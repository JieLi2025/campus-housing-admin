package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.common.ResultCode;
import com.campus.dorm.dto.SendMessageDTO;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.ChatMessageVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 楼栋群聊服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingGroupChatService {
    
    @Resource
    private ConversationMapper conversationMapper;
    
    @Resource
    private ChatMessageMapper chatMessageMapper;
    
    @Resource
    private GroupChatMemberMapper groupChatMemberMapper;
    
    @Resource
    private BuildingMapper buildingMapper;
    
    @Resource
    private StudentMapper studentMapper;
    
    @Resource
    private AdminMapper adminMapper;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private StudentBuildingAssignmentService studentBuildingAssignmentService;
    
    @Resource
    private BuildingService buildingService;
    
    @Resource
    private WebSocketService webSocketService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
    @Resource
    private ObjectMapper objectMapper;
    
    @Resource
    private DormRoomMapper dormRoomMapper;
    
    @Resource
    private BuildingMajorAssignmentMapper buildingMajorAssignmentMapper;
    
    @Resource
    private DormGroupMapper dormGroupMapper;
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取或创建楼栋群聊会话
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> getOrCreateBuildingGroupChat(Long buildingId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证楼栋是否存在
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 验证用户权限
        Result<?> authResult = verifyUserBuildingAccess(userId, buildingId);
        if (!authResult.isSuccess()) {
            return Result.error(authResult.getMessage());
        }
        
        // 查找是否已存在该楼栋的群聊
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        
        if (conversation == null) {
            // 创建新群聊
            conversation = new Conversation();
            conversation.setConversationType("building_group");
            conversation.setBuildingId(buildingId);
            conversation.setGroupName(building.getBuildingName() + "群聊");
            conversation.setUser1Id(null); // 群聊时user1_id设为null，避免唯一键冲突
            conversation.setUser2Id(null); // 群聊时user2_id设为null，避免唯一键冲突
            conversationMapper.insert(conversation);
            
            log.info("创建楼栋群聊：buildingId={}, conversationId={}", buildingId, conversation.getId());
        }
        
        // 确保用户是群成员
        addUserToGroupChat(conversation.getId(), userId);
        
        return Result.success(conversation.getId());
    }
    
    /**
     * 发送群聊消息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<ChatMessageVO> sendGroupMessage(Long buildingId, SendMessageDTO dto) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取或创建群聊会话
        Result<Long> convResult = getOrCreateBuildingGroupChat(buildingId);
        if (!convResult.isSuccess()) {
            return Result.error(convResult.getMessage());
        }
        
        Long conversationId = convResult.getData();
        
        // 验证用户是群成员
        if (!isGroupMember(conversationId, userId)) {
            return Result.error("您不是该群聊的成员");
        }
        
        // 获取会话信息，检查全体禁言状态
        Conversation conversation = conversationMapper.selectById(conversationId);
        GroupChatMember member = getGroupChatMember(conversationId, userId);
        
        // 检查全体禁言（管理员不受限制）
        if (Boolean.TRUE.equals(conversation.getMuteAll())) {
            if (member == null || !"admin".equals(member.getRole())) {
                return Result.error("当前已开启全体禁言，只有管理员可以发言");
            }
        }
        
        // 检查用户是否被单独禁言
        if (member != null && Boolean.TRUE.equals(member.getIsMuted())) {
            return Result.error("您已被禁言，无法发送消息");
        }
        
        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(null); // 群聊消息receiver_id为NULL
        message.setContent(dto.getContent());
        message.setFileUrl(dto.getFileUrl());
        message.setQuotedMessageId(dto.getQuotedMessageId());
        message.setIsRecalled(false);
        
        chatMessageMapper.insert(message);
        
        // 更新会话的最后消息信息
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationMapper.updateById(conversation);
        
        // 转换为VO
        ChatMessageVO vo = convertToChatMessageVO(message, userId);
        
        // 通过WebSocket推送消息给所有群成员
        pushGroupMessage(buildingId, vo);
        
        return Result.success(vo);
    }
    
    /**
     * 获取群聊消息历史
     */
    public Result<List<ChatMessageVO>> getGroupMessages(Long buildingId, Integer pageNum, Integer pageSize) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            return Result.success(new ArrayList<>());
        }
        
        // 验证用户是群成员
        if (!isGroupMember(conversation.getId(), userId)) {
            return Result.error("您不是该群聊的成员");
        }
        
        // 查询消息（分页）
        LambdaQueryWrapper<ChatMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(ChatMessage::getConversationId, conversation.getId())
                     .orderByDesc(ChatMessage::getCreatedAt)
                     .last("LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize);
        
        List<ChatMessage> messages = chatMessageMapper.selectList(messageWrapper);
        
        // 反转顺序（最新的在前）
        Collections.reverse(messages);
        
        // 转换为VO
        List<ChatMessageVO> result = messages.stream()
                .map(msg -> convertToChatMessageVO(msg, userId))
                .collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    /**
     * 获取群成员列表
     */
    public Result<List<Map<String, Object>>> getGroupMembers(Long buildingId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            return Result.success(new ArrayList<>());
        }
        
        // 查询所有活跃成员
        LambdaQueryWrapper<GroupChatMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupChatMember::getConversationId, conversation.getId())
                    .eq(GroupChatMember::getIsActive, true);
        
        List<GroupChatMember> members = groupChatMemberMapper.selectList(memberWrapper);
        
        log.info("楼栋{}群聊总成员数: {}", buildingId, members.size());
        
        // 获取该楼栋的管理员列表（用于过滤超级管理员）
        Set<Long> buildingAdminIds = new HashSet<>();
        // 直接查询楼栋信息获取管理员ID，而不是调用 getAdminBuildings
        Building building = buildingMapper.selectById(buildingId);
        if (building != null && building.getAdminId() != null) {
            buildingAdminIds.add(building.getAdminId());
        }
        
        // 转换为成员信息列表，只包含该楼栋的普通管理员和学生，完全排除超级管理员
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupChatMember member : members) {
            Map<String, Object> memberInfo = new HashMap<>();
            SysUser user = sysUserMapper.selectById(member.getUserId());
            if (user != null) {
                log.info("检查成员: userId={}, userName={}, userType={}", user.getId(), user.getRealName(), user.getUserType());
                
                // 完全排除超级管理员
                if ("superadmin".equals(user.getUserType())) {
                    log.info("排除超级管理员: {}", user.getRealName());
                    continue;
                }
                
                // 如果是普通管理员，检查是否是该楼栋的管理员
                if ("admin".equals(user.getUserType())) {
                    if (!buildingAdminIds.contains(user.getId())) {
                        log.info("排除非该楼栋管理员: {}", user.getRealName());
                        // 不是该楼栋的管理员，跳过
                        continue;
                    }
                    log.info("保留该楼栋管理员: {}", user.getRealName());
                }
                
                memberInfo.put("userId", user.getId());
                memberInfo.put("userName", user.getRealName());
                memberInfo.put("avatar", user.getAvatar());
                memberInfo.put("userType", user.getUserType());
                memberInfo.put("role", member.getRole());
                memberInfo.put("isMuted", Boolean.TRUE.equals(member.getIsMuted()));
                
                // 如果是学生，获取房间信息
                if ("student".equals(user.getUserType())) {
                    LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                    studentWrapper.eq(Student::getUserId, user.getId());
                    Student student = studentMapper.selectOne(studentWrapper);
                    if (student != null) {
                        memberInfo.put("studentId", student.getStudentId());
                        memberInfo.put("room", getStudentRoom(student.getId()));
                    }
                }
                
                result.add(memberInfo);
            }
        }
        
        log.info("楼栋{}群聊过滤后成员数: {}", buildingId, result.size());
        
        return Result.success(result);
    }
    
    /**
     * 禁言/解除禁言用户
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleMuteUser(Long buildingId, Long targetUserId, Boolean mute) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证权限：只有管理员可以禁言
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser == null || (!"admin".equals(currentUser.getUserType()) && !"superadmin".equals(currentUser.getUserType()))) {
            return Result.error("只有管理员可以禁言用户");
        }
        
        // 获取群聊会话
        Result<Long> convResult = getOrCreateBuildingGroupChat(buildingId);
        if (!convResult.isSuccess()) {
            return Result.error(convResult.getMessage());
        }
        
        Long conversationId = convResult.getData();
        
        // 获取目标用户成员信息
        GroupChatMember member = getGroupChatMember(conversationId, targetUserId);
        if (member == null) {
            return Result.error("用户不是该群聊的成员");
        }
        
        // 设置禁言状态
        member.setIsMuted(mute);
        groupChatMemberMapper.updateById(member);
        
        return Result.success();
    }
    
    /**
     * 切换全体禁言状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> toggleMuteAll(Long buildingId, Boolean muteAll) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证权限：只有管理员可以设置全体禁言
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser == null || (!"admin".equals(currentUser.getUserType()) && !"superadmin".equals(currentUser.getUserType()))) {
            return Result.error("只有管理员可以设置全体禁言");
        }
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            return Result.error("群聊不存在");
        }
        
        // 设置全体禁言状态
        conversation.setMuteAll(muteAll);
        conversationMapper.updateById(conversation);
        
        log.info("管理员{}{}楼栋{}的全体禁言", userId, muteAll ? "开启" : "关闭", buildingId);
        
        return Result.success(muteAll);
    }
    
    /**
     * 获取全体禁言状态
     */
    public Result<Boolean> getMuteAllStatus(Long buildingId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            return Result.success(false);
        }
        
        return Result.success(Boolean.TRUE.equals(conversation.getMuteAll()));
    }
    
    /**
     * 获取楼栋学生统计信息
     * @param buildingId 楼栋ID
     * @return 包含总学生数和在线学生数的Map
     */
    public Result<Map<String, Object>> getBuildingStudentStats(Long buildingId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证权限
        Result<?> authResult = verifyUserBuildingAccess(userId, buildingId);
        if (!authResult.isSuccess()) {
            return Result.error(authResult.getMessage());
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 1. 统计该楼栋的所有学生（基于楼栋分配记录）
        int totalStudents = 0;
        
        // 查询该楼栋的专业分配记录
        LambdaQueryWrapper<BuildingMajorAssignment> assignmentWrapper = new LambdaQueryWrapper<>();
        assignmentWrapper.eq(BuildingMajorAssignment::getBuildingId, buildingId)
                        .eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> assignments = buildingMajorAssignmentMapper.selectList(assignmentWrapper);
        
        // 根据分配记录统计学生数
        Set<Long> studentIds = new HashSet<>();
        for (BuildingMajorAssignment assignment : assignments) {
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getDeleted, 0)
                         .eq(Student::getMajor, assignment.getMajorName());
            
            // 注意：不匹配年级字段，因为班级名称已经包含了年级信息（如"人工智能2301"中的"23"表示2023级）
            // 如果分配记录中的年级字段与学生表中的年级字段不一致，会导致统计不准确
            // 因此只通过专业和班级名称来匹配学生，这样更可靠
            
            // 匹配班级：如果分配记录有班级，只匹配该班级；如果没有班级，匹配所有该专业的学生
            if (assignment.getClassName() != null && !assignment.getClassName().isEmpty()) {
                // 分配记录指定了班级，精确匹配该班级
                studentWrapper.eq(Student::getClassName, assignment.getClassName());
            }
            // 如果分配记录没有指定班级（null或空），则不添加班级条件，匹配所有该专业的学生
            
            // 匹配班级分片：如果分配记录有分片，只匹配该分片；如果没有分片，只匹配没有分片的学生
            if (assignment.getClassSegment() != null && !assignment.getClassSegment().isEmpty()) {
                // 分配记录指定了分片，精确匹配该分片
                studentWrapper.eq(Student::getClassSegment, assignment.getClassSegment());
            } else {
                // 分配记录没有分片，只匹配没有分片的学生
                studentWrapper.and(w -> w.isNull(Student::getClassSegment)
                        .or()
                        .eq(Student::getClassSegment, ""));
            }
            
            // 匹配性别
            if (assignment.getGender() != null) {
                String assignmentGender = assignment.getGender();
                String studentGender;
                if ("male".equalsIgnoreCase(assignmentGender)) {
                    studentGender = "男";
                } else if ("female".equalsIgnoreCase(assignmentGender)) {
                    studentGender = "女";
                } else {
                    studentGender = assignmentGender;
                }
                studentWrapper.eq(Student::getGender, studentGender);
            }
            
            List<Student> students = studentMapper.selectList(studentWrapper);
            studentIds.addAll(students.stream().map(Student::getId).collect(Collectors.toSet()));
        }
        
        totalStudents = studentIds.size();
        
        // 2. 统计群聊中的学生数（作为在线学生的近似值）
        int onlineStudents = 0;
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation != null) {
            // 查询所有活跃成员
            LambdaQueryWrapper<GroupChatMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(GroupChatMember::getConversationId, conversation.getId())
                        .eq(GroupChatMember::getIsActive, true);
            
            List<GroupChatMember> members = groupChatMemberMapper.selectList(memberWrapper);
            
            // 统计学生数量（排除管理员）
            for (GroupChatMember member : members) {
                SysUser user = sysUserMapper.selectById(member.getUserId());
                if (user != null && "student".equals(user.getUserType())) {
                    onlineStudents++;
                }
            }
        }
        
        stats.put("totalStudents", totalStudents);
        stats.put("onlineStudents", onlineStudents);
        
        return Result.success(stats);
    }
    
    /**
     * 验证用户是否有权限访问该楼栋群聊
     */
    private Result<?> verifyUserBuildingAccess(Long userId, Long buildingId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 管理员：检查是否管理该楼栋
        if ("admin".equals(user.getUserType()) || "superadmin".equals(user.getUserType())) {
            Result<List<com.campus.dorm.vo.BuildingStatisticsVO>> buildingsResult = buildingService.getAdminBuildings(userId);
            if (buildingsResult.isSuccess() && buildingsResult.getData() != null) {
                boolean hasAccess = buildingsResult.getData().stream()
                    .anyMatch(b -> b.getId().equals(buildingId));
                if (hasAccess) {
                    return Result.success();
                }
            }
            return Result.error("您没有管理该楼栋的权限");
        }
        
        // 学生：检查是否属于该楼栋
        if ("student".equals(user.getUserType())) {
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getUserId, userId);
            Student student = studentMapper.selectOne(studentWrapper);
            if (student == null) {
                return Result.error("学生信息不存在");
            }
            
            Result<com.campus.dorm.vo.StudentBuildingAssignmentVO> assignmentResult = 
                studentBuildingAssignmentService.getMyBuildingAssignment(student.getId());
            if (assignmentResult.isSuccess() && assignmentResult.getData() != null) {
                com.campus.dorm.vo.StudentBuildingAssignmentVO assignment = assignmentResult.getData();
                if (assignment.getAssignments() != null) {
                    boolean hasAccess = assignment.getAssignments().stream()
                        .anyMatch(a -> a.getBuildingId().equals(buildingId));
                    if (hasAccess) {
                        return Result.success();
                    }
                }
            }
            return Result.error("您不属于该楼栋");
        }
        
        return Result.error("用户类型不支持");
    }
    
    /**
     * 添加用户到群聊
     */
    private void addUserToGroupChat(Long conversationId, Long userId) {
        LambdaQueryWrapper<GroupChatMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupChatMember::getConversationId, conversationId)
               .eq(GroupChatMember::getUserId, userId)
               .eq(GroupChatMember::getIsActive, true);
        
        GroupChatMember existing = groupChatMemberMapper.selectOne(wrapper);
        if (existing == null) {
            GroupChatMember member = new GroupChatMember();
            member.setConversationId(conversationId);
            member.setUserId(userId);
            
            // 判断角色：管理员为admin，学生为member
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && ("admin".equals(user.getUserType()) || "superadmin".equals(user.getUserType()))) {
                member.setRole("admin");
            } else {
                member.setRole("member");
            }
            
            member.setIsActive(true);
            member.setIsMuted(false); // 默认未禁言
            groupChatMemberMapper.insert(member);
        }
    }
    
    /**
     * 检查用户是否是群成员
     */
    private boolean isGroupMember(Long conversationId, Long userId) {
        LambdaQueryWrapper<GroupChatMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupChatMember::getConversationId, conversationId)
               .eq(GroupChatMember::getUserId, userId)
               .eq(GroupChatMember::getIsActive, true);
        
        return groupChatMemberMapper.selectOne(wrapper) != null;
    }
    
    /**
     * 获取群聊成员信息
     */
    private GroupChatMember getGroupChatMember(Long conversationId, Long userId) {
        LambdaQueryWrapper<GroupChatMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupChatMember::getConversationId, conversationId)
               .eq(GroupChatMember::getUserId, userId)
               .eq(GroupChatMember::getIsActive, true);
        
        return groupChatMemberMapper.selectOne(wrapper);
    }
    
    /**
     * 推送群聊消息给所有成员
     */
    private void pushGroupMessage(Long buildingId, ChatMessageVO message) {
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "building_group")
               .eq(Conversation::getBuildingId, buildingId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        if (conversation == null) {
            return;
        }
        
        // 获取所有活跃成员
        LambdaQueryWrapper<GroupChatMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GroupChatMember::getConversationId, conversation.getId())
                    .eq(GroupChatMember::getIsActive, true);
        
        List<GroupChatMember> members = groupChatMemberMapper.selectList(memberWrapper);
        
        // 推送消息给每个成员（包括发送者，用于多标签页同步）
        for (GroupChatMember member : members) {
            webSocketService.sendNewMessage(member.getUserId(), message);
        }
        
        // 同时通过topic推送（用于实时订阅，所有订阅该topic的用户都会收到）
        webSocketService.sendGroupMessage(buildingId, message);
    }
    
    /**
     * 获取学生房间信息（从dorm_group获取）
     */
    private String getStudentRoom(Long studentId) {
        try {
            // 从dorm_group获取学生的房间ID
            Long roomId = getStudentRoomIdFromDormGroup(studentId);
            if (roomId == null) {
                return "";
            }
            
            // 查询房间信息
            DormRoom room = dormRoomMapper.selectById(roomId);
            if (room == null) {
                return "";
            }
            
            // 查询楼栋信息
            Building building = buildingMapper.selectById(room.getBuildingId());
            if (building == null) {
                return room.getRoomNumber();
            }
            
            // 返回格式：楼栋名-房间号
            return building.getBuildingName() + "-" + room.getRoomNumber();
        } catch (Exception e) {
            log.error("获取学生房间信息失败：studentId={}", studentId, e);
            return "";
        }
    }
    
    /**
     * 从dorm_group获取学生的房间ID
     */
    private Long getStudentRoomIdFromDormGroup(Long studentId) {
        try {
            // 查询所有已选房间的宿舍组
            LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(DormGroup::getSelectedRoomId);
            List<DormGroup> groups = dormGroupMapper.selectList(wrapper);
            
            for (DormGroup group : groups) {
                if (group.getMembers() == null || group.getMembers().isEmpty()) {
                    continue;
                }
                try {
                    List<Map<String, Object>> members = objectMapper.readValue(
                        group.getMembers(), 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    for (Map<String, Object> member : members) {
                        Object idObj = member.get("id");
                        Long memberId = null;
                        if (idObj instanceof Number) {
                            memberId = ((Number) idObj).longValue();
                        }
                        if (memberId != null && memberId.equals(studentId)) {
                            return group.getSelectedRoomId();
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析宿舍组成员失败：groupId={}", group.getId());
                }
            }
        } catch (Exception e) {
            log.error("获取学生房间ID失败：studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 转换为消息VO
     */
    private ChatMessageVO convertToChatMessageVO(ChatMessage message, Long currentUserId) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        
        vo.setIsOwn(message.getSenderId().equals(currentUserId));
        
        // 获取发送者信息
        SysUser sender = sysUserMapper.selectById(message.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getRealName());
            vo.setSenderAvatar(sender.getAvatar());
            vo.setUserType(sender.getUserType());
            
            // 如果是学生，获取房间信息
            if ("student".equals(sender.getUserType())) {
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getUserId, sender.getId());
                Student student = studentMapper.selectOne(studentWrapper);
                if (student != null) {
                    vo.setRoom(getStudentRoom(student.getId()));
                }
            }
        }
        
        // 获取引用的消息
        if (message.getQuotedMessageId() != null) {
            ChatMessage quoted = chatMessageMapper.selectById(message.getQuotedMessageId());
            if (quoted != null) {
                vo.setQuotedMessage(convertToChatMessageVO(quoted, currentUserId));
            }
        }
        
        // 格式化时间
        vo.setTime(formatMessageTime(message.getCreatedAt()));
        
        return vo;
    }
    
    /**
     * 格式化消息时间
     */
    private String formatMessageTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.toLocalTime().toString().substring(0, 5);
    }
}

