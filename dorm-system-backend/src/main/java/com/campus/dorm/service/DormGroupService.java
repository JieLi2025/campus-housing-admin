package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.dorm.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 宿舍组Service
 * 
 * @author Campus Dorm System
 * @date 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DormGroupService {
    
    private final DormGroupMapper dormGroupMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Resource
    private ConversationMapper conversationMapper;
    
    @Resource
    private ChatMessageMapper chatMessageMapper;
    
    @Resource
    private GroupChatMemberMapper groupChatMemberMapper;
    
    @Resource
    private RoommateInviteMapper roommateInviteMapper;
    
    @Resource
    private StudentMapper studentMapper;
    
    @Resource
    private BuildingMajorAssignmentMapper majorAssignmentMapper;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private BuildingMapper buildingMapper;
    
    @Resource
    private DormRoomMapper dormRoomMapper;
    
    @Resource
    private NotificationMapper notificationMapper;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
    /**
     * 获取当前用户ID（从JWT Token）
     */
    private Long getCurrentUserId() {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前用户类型
     */
    private String getCurrentUserType() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getUserType() : null;
    }
    
    /**
     * 获取宿舍组列表（分页）
     * 普通管理员只能看到自己负责楼栋对应专业/班级的学生创建的宿舍组
     */
    public Result<?> getGroupList(Integer currentPage, Integer pageSize, String keyword, String status) {
        log.info("获取宿舍组列表：currentPage={}, pageSize={}, keyword={}, status={}", 
            currentPage, pageSize, keyword, status);
        
        Page<DormGroup> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        
        // 关键字搜索（组名）
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(DormGroup::getGroupName, keyword);
        }
        
        // 状态筛选
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(DormGroup::getStatus, status);
        }
        
        // 权限过滤：普通管理员只能看到自己负责楼栋的学生创建的宿舍组
        String userType = getCurrentUserType();
        Long currentUserId = getCurrentUserId();
        Set<Long> allowedStudentIds = null;
        
        if ("admin".equals(userType) && currentUserId != null) {
            log.info("普通管理员查询宿舍组列表，需要进行权限过滤，管理员ID：{}", currentUserId);
            
            // 1. 获取当前管理员的用户名
            SysUser currentUser = sysUserMapper.selectById(currentUserId);
            if (currentUser == null) {
                log.warn("管理员{}不存在，返回空列表", currentUserId);
                Map<String, Object> emptyData = new HashMap<>();
                emptyData.put("records", new ArrayList<>());
                emptyData.put("total", 0L);
                emptyData.put("currentPage", currentPage);
                emptyData.put("pageSize", pageSize);
                return Result.success(emptyData);
            }
            
            // 2. 查询管理员负责的楼栋
            LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
            buildingWrapper.eq(Building::getManagerName, currentUser.getUsername());
            List<Building> buildings = buildingMapper.selectList(buildingWrapper);
            
            if (buildings.isEmpty()) {
                log.warn("管理员{}未分配任何楼栋，返回空列表", currentUser.getUsername());
                Map<String, Object> emptyData = new HashMap<>();
                emptyData.put("records", new ArrayList<>());
                emptyData.put("total", 0L);
                emptyData.put("currentPage", currentPage);
                emptyData.put("pageSize", pageSize);
                return Result.success(emptyData);
            }
            
            List<Long> buildingIds = buildings.stream()
                .map(Building::getId)
                .collect(Collectors.toList());
            log.info("管理员{}负责的楼栋ID列表：{}", currentUser.getUsername(), buildingIds);
            
            // 3. 查询这些楼栋对应的专业/班级分配
            LambdaQueryWrapper<BuildingMajorAssignment> majorWrapper = new LambdaQueryWrapper<>();
            majorWrapper.in(BuildingMajorAssignment::getBuildingId, buildingIds)
                       .eq(BuildingMajorAssignment::getStatus, 1);
            List<BuildingMajorAssignment> majorAssignments = majorAssignmentMapper.selectList(majorWrapper);
            
            // 4. 构建允许的学生ID集合
            allowedStudentIds = new HashSet<>();
            
            // 4a. 根据专业/班级/年级/性别过滤学生
            if (!majorAssignments.isEmpty()) {
                for (BuildingMajorAssignment majorAssignment : majorAssignments) {
                    LambdaQueryWrapper<Student> studentByMajorWrapper = new LambdaQueryWrapper<>();
                    studentByMajorWrapper.eq(Student::getDeleted, 0)
                        .eq(Student::getMajor, majorAssignment.getMajorName())
                        .eq(Student::getClassName, majorAssignment.getClassName())
                        .eq(Student::getGrade, majorAssignment.getGrade());
                    
                    // 兼容性别字段格式
                    String assignmentGender = majorAssignment.getGender();
                    if (assignmentGender != null) {
                        String studentGender;
                        if ("male".equalsIgnoreCase(assignmentGender)) {
                            studentGender = "男";
                        } else if ("female".equalsIgnoreCase(assignmentGender)) {
                            studentGender = "女";
                        } else {
                            studentGender = assignmentGender;
                        }
                        studentByMajorWrapper.eq(Student::getGender, studentGender);
                    }
                    
                    List<Student> students = studentMapper.selectList(studentByMajorWrapper);
                    allowedStudentIds.addAll(students.stream()
                        .map(Student::getId)
                        .collect(Collectors.toSet()));
                }
            }
            
            // 4b. 查询已分配到这些楼栋宿舍的学生（从dorm_group表获取）
            List<Long> roomIds = new ArrayList<>();
            for (Long buildingId : buildingIds) {
                LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
                roomWrapper.eq(DormRoom::getBuildingId, buildingId);
                List<DormRoom> rooms = dormRoomMapper.selectList(roomWrapper);
                roomIds.addAll(rooms.stream()
                    .map(DormRoom::getId)
                    .collect(Collectors.toList()));
            }
            
            if (!roomIds.isEmpty()) {
                // 从dorm_group表查询已选择这些房间的宿舍组，提取成员ID
                LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
                groupWrapper.in(DormGroup::getSelectedRoomId, roomIds);
                List<DormGroup> groups = dormGroupMapper.selectList(groupWrapper);
                for (DormGroup group : groups) {
                    List<Long> memberIds = extractMemberIds(group);
                    allowedStudentIds.addAll(memberIds);
                }
            }
            
            log.info("管理员{}允许查看的学生ID数量：{}", currentUser.getUsername(), allowedStudentIds.size());
            
            // 5. 只查询这些学生创建的宿舍组（通过leaderId过滤）
            if (!allowedStudentIds.isEmpty()) {
                wrapper.in(DormGroup::getLeaderId, allowedStudentIds);
            } else {
                // 如果没有允许的学生，返回空列表
                log.warn("管理员{}没有允许查看的学生，返回空列表", currentUser.getUsername());
                Map<String, Object> emptyData = new HashMap<>();
                emptyData.put("records", new ArrayList<>());
                emptyData.put("total", 0L);
                emptyData.put("currentPage", currentPage);
                emptyData.put("pageSize", pageSize);
                return Result.success(emptyData);
            }
        }
        
        // 按ID倒序（ID自增，效果等同于按创建时间倒序）
        wrapper.orderByDesc(DormGroup::getId);
        
        IPage<DormGroup> result = dormGroupMapper.selectPage(page, wrapper);
        
        // 填充组长姓名
        List<DormGroup> groups = result.getRecords();
        if (groups != null && !groups.isEmpty()) {
            // 收集所有leaderId
            List<Long> leaderIds = groups.stream()
                .filter(g -> g.getLeaderId() != null)
                .map(DormGroup::getLeaderId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
            
            // 批量查询学生信息
            if (!leaderIds.isEmpty()) {
                List<Student> students = studentMapper.selectBatchIds(leaderIds);
                Map<Long, String> leaderNameMap = students.stream()
                    .filter(s -> s != null && s.getRealName() != null)
                    .collect(java.util.stream.Collectors.toMap(
                        Student::getId,
                        Student::getRealName,
                        (v1, v2) -> v1
                    ));
                
                // 填充leaderName，如果查询不到则尝试从members JSON中提取
                for (DormGroup group : groups) {
                    if (group.getLeaderId() != null) {
                        // 先尝试从学生表查询
                        String leaderName = leaderNameMap.get(group.getLeaderId());
                        if (leaderName != null) {
                            group.setLeaderName(leaderName);
                        } else {
                            // 如果查询不到，尝试从members JSON中提取
                            try {
                                String membersJson = group.getMembers();
                                if (membersJson != null && !membersJson.trim().isEmpty()) {
                                    List<Map<String, Object>> members = objectMapper.readValue(
                                        membersJson,
                                        new TypeReference<List<Map<String, Object>>>() {}
                                    );
                                    for (Map<String, Object> member : members) {
                                        Object idObj = member.get("id");
                                        Long memberId = null;
                                        if (idObj instanceof Number) {
                                            memberId = ((Number) idObj).longValue();
                                        }
                                        if (memberId != null && memberId.equals(group.getLeaderId())) {
                                            Object nameObj = member.get("name");
                                            if (nameObj == null) {
                                                nameObj = member.get("realName");
                                            }
                                            if (nameObj != null) {
                                                group.setLeaderName(nameObj.toString());
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("从members JSON中提取组长姓名失败：groupId={}", group.getId(), e);
                            }
                        }
                    }
                }
            }
            
            // 填充房间信息（房间号、楼栋名、楼层）
            List<Long> roomIds = groups.stream()
                .filter(g -> g.getSelectedRoomId() != null)
                .map(DormGroup::getSelectedRoomId)
                .distinct()
                .collect(Collectors.toList());
            
            if (!roomIds.isEmpty()) {
                List<DormRoom> rooms = dormRoomMapper.selectBatchIds(roomIds);
                Map<Long, DormRoom> roomMap = rooms.stream()
                    .collect(Collectors.toMap(DormRoom::getId, r -> r, (v1, v2) -> v1));
                
                // 获取楼栋信息
                List<Long> buildingIds = rooms.stream()
                    .filter(r -> r.getBuildingId() != null)
                    .map(DormRoom::getBuildingId)
                    .distinct()
                    .collect(Collectors.toList());
                
                Map<Long, String> buildingNameMap = new HashMap<>();
                if (!buildingIds.isEmpty()) {
                    List<Building> buildings = buildingMapper.selectBatchIds(buildingIds);
                    buildingNameMap = buildings.stream()
                        .collect(Collectors.toMap(Building::getId, Building::getBuildingName, (v1, v2) -> v1));
                }
                
                // 填充房间信息到宿舍组
                for (DormGroup group : groups) {
                    if (group.getSelectedRoomId() != null) {
                        DormRoom room = roomMap.get(group.getSelectedRoomId());
                        if (room != null) {
                            group.setRoomNumber(room.getRoomNumber());
                            group.setFloor(room.getFloor());
                            if (room.getBuildingId() != null) {
                                group.setBuildingName(buildingNameMap.get(room.getBuildingId()));
                            }
                        }
                    }
                }
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", groups);
        data.put("total", result.getTotal());
        data.put("currentPage", result.getCurrent());
        data.put("pageSize", result.getSize());
        
        return Result.success(data);
    }
    
    /**
     * 根据ID获取宿舍组详情
     */
    public Result<?> getGroupById(Long id) {
        log.info("获取宿舍组详情：id={}", id);
        
        DormGroup group = dormGroupMapper.selectById(id);
        if (group == null) {
            return Result.error("宿舍组不存在");
        }
        
        return Result.success(group);
    }
    
    /**
     * 根据组长ID获取宿舍组
     */
    public Result<?> getGroupsByLeaderId(Long leaderId) {
        log.info("根据组长ID获取宿舍组：leaderId={}", leaderId);
        
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, leaderId);
        wrapper.orderByDesc(DormGroup::getId);
        
        List<DormGroup> groups = dormGroupMapper.selectList(wrapper);
        return Result.success(groups);
    }
    
    /**
     * 获取学生所在的宿舍组（包括作为组长或成员）
     */
    public Result<?> getMyDormGroup(Long studentId) {
        log.info("获取学生所在的宿舍组：studentId={}", studentId);
        
        try {
            // 查询所有宿舍组
            List<DormGroup> allGroups = dormGroupMapper.selectList(null);
            
            // 查找学生所在的宿舍组
            for (DormGroup group : allGroups) {
                // 检查是否是组长
                if (group.getLeaderId() != null && group.getLeaderId().equals(studentId)) {
                    log.info("学生{}是宿舍组{}的组长", studentId, group.getId());
                    return Result.success(group);
                }
                
                // 检查是否在成员列表中
                if (StringUtils.isNotBlank(group.getMembers())) {
                    try {
                        List<Map<String, Object>> members = objectMapper.readValue(
                            group.getMembers(), 
                            new TypeReference<List<Map<String, Object>>>() {}
                        );
                        
                        for (Map<String, Object> member : members) {
                            Object memberIdObj = member.get("id");
                            Long memberId = null;
                            
                            if (memberIdObj instanceof Integer) {
                                memberId = ((Integer) memberIdObj).longValue();
                            } else if (memberIdObj instanceof Long) {
                                memberId = (Long) memberIdObj;
                            } else if (memberIdObj instanceof String) {
                                try {
                                    memberId = Long.parseLong((String) memberIdObj);
                                } catch (NumberFormatException e) {
                                    log.warn("无法解析成员ID：{}", memberIdObj);
                                }
                            }
                            
                            if (memberId != null && memberId.equals(studentId)) {
                                log.info("学生{}是宿舍组{}的成员", studentId, group.getId());
                                return Result.success(group);
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析宿舍组{}的成员列表失败", group.getId(), e);
                    }
                }
            }
            
            log.info("学生{}未加入任何宿舍组", studentId);
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("获取学生宿舍组失败", e);
            return Result.error("获取宿舍组失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建宿舍组
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createGroup(DormGroup dormGroup) {
        log.info("创建宿舍组：{}", dormGroup);
        
        // 验证：检查该leaderId是否已经存在未完成的宿舍组
        if (dormGroup.getLeaderId() != null) {
            LambdaQueryWrapper<DormGroup> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(DormGroup::getLeaderId, dormGroup.getLeaderId())
                       .in(DormGroup::getStatus, "forming", "complete"); // 只检查未选择房间的组
            
            List<DormGroup> existingGroups = dormGroupMapper.selectList(checkWrapper);
            if (existingGroups != null && !existingGroups.isEmpty()) {
                // 过滤掉已选择房间的组
                List<DormGroup> activeGroups = existingGroups.stream()
                    .filter(group -> group.getSelectedRoomId() == null)
                    .collect(Collectors.toList());
                
                if (!activeGroups.isEmpty()) {
                    String errorMsg = String.format("该学生（ID: %d）已经存在未完成的宿舍组，无法重复创建", dormGroup.getLeaderId());
                    log.warn(errorMsg);
                    return Result.error(errorMsg);
                }
            }
        }
        
        // 设置默认值
        if (dormGroup.getCurrentMembers() == null) {
            dormGroup.setCurrentMembers(1);
        }
        
        // 如果maxMembers未设置，尝试根据leaderId和专业分配来设置
        if (dormGroup.getMaxMembers() == null) {
            if (dormGroup.getLeaderId() != null) {
                try {
                    // 尝试根据学生的专业分配来设置maxMembers
                    Student student = studentMapper.selectById(dormGroup.getLeaderId());
                    if (student != null && student.getMajor() != null) {
                        // 查询该专业的房间类型分配
                        LambdaQueryWrapper<BuildingMajorAssignment> assignmentWrapper = new LambdaQueryWrapper<>();
                        assignmentWrapper.eq(BuildingMajorAssignment::getMajorName, student.getMajor())
                                       .eq(BuildingMajorAssignment::getStatus, 1);
                        if (student.getClassName() != null) {
                            assignmentWrapper.and(w -> w.eq(BuildingMajorAssignment::getClassName, student.getClassName())
                                                       .or().isNull(BuildingMajorAssignment::getClassName)
                                                       .or().eq(BuildingMajorAssignment::getClassName, ""));
                        }
                        BuildingMajorAssignment assignment = majorAssignmentMapper.selectOne(assignmentWrapper);
                        if (assignment != null && assignment.getBuildingId() != null) {
                            // 从分配的楼栋中查询房间类型
                            // 查询该专业班级在该楼栋的房间，获取房间类型
                            LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
                            roomWrapper.eq(DormRoom::getBuildingId, assignment.getBuildingId())
                                     .eq(DormRoom::getClassName, student.getClassName())
                                     .and(w -> w.eq(DormRoom::getMajorName, student.getMajor())
                                               .or()
                                               .like(DormRoom::getMajorName, student.getMajor() + " - " + student.getClassName()))
                                     .last("LIMIT 1"); // 只取第一个房间
                            
                            DormRoom sampleRoom = dormRoomMapper.selectOne(roomWrapper);
                            if (sampleRoom != null && sampleRoom.getRoomType() != null) {
                                String roomType = sampleRoom.getRoomType();
                                // 从房间类型中提取人数（支持中文格式：二人间、四人间等）
                                if (roomType.contains("二") || roomType.contains("2") || "two_bed".equalsIgnoreCase(roomType) || "double_bed".equalsIgnoreCase(roomType)) {
                                    dormGroup.setMaxMembers(2);
                                    dormGroup.setTargetRoomType("二人间");
                                } else if (roomType.contains("四") || roomType.contains("4") || "four_bed".equalsIgnoreCase(roomType)) {
                                    dormGroup.setMaxMembers(4);
                                    dormGroup.setTargetRoomType("四人间");
                                } else if (roomType.contains("六") || roomType.contains("6") || "six_bed".equalsIgnoreCase(roomType)) {
                                    dormGroup.setMaxMembers(6);
                                    dormGroup.setTargetRoomType("六人间");
                                } else if (roomType.contains("八") || roomType.contains("8") || "eight_bed".equalsIgnoreCase(roomType)) {
                                    dormGroup.setMaxMembers(8);
                                    dormGroup.setTargetRoomType("八人间");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("根据专业分配设置maxMembers失败，使用默认值4：leaderId={}", dormGroup.getLeaderId(), e);
                }
            }
            // 如果仍然为null，使用默认值4
            if (dormGroup.getMaxMembers() == null) {
                dormGroup.setMaxMembers(4);
                log.warn("宿舍组maxMembers未设置，使用默认值4：groupId={}, leaderId={}", dormGroup.getId(), dormGroup.getLeaderId());
            }
        }
        
        if (dormGroup.getStatus() == null) {
            dormGroup.setStatus("forming");
        }
        
        // 检查成员数量，自动设置状态
        if (dormGroup.getCurrentMembers() >= dormGroup.getMaxMembers()) {
            dormGroup.setStatus("complete");
        }
        
        int result = dormGroupMapper.insert(dormGroup);
        if (result > 0) {
            return Result.success(dormGroup);
        }
        
        return Result.error("创建宿舍组失败");
    }
    
    /**
     * 更新宿舍组信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateGroup(Long id, DormGroup dormGroup) {
        log.info("更新宿舍组：id={}, data={}", id, dormGroup);
        
        // 检查是否存在
        DormGroup existing = dormGroupMapper.selectById(id);
        if (existing == null) {
            return Result.error("宿舍组不存在");
        }
        
        dormGroup.setId(id);
        
        // 根据成员数量自动更新状态
        if (dormGroup.getCurrentMembers() != null && dormGroup.getMaxMembers() != null) {
            if (dormGroup.getCurrentMembers() >= dormGroup.getMaxMembers()) {
                dormGroup.setStatus("complete");
            } else if ("complete".equals(existing.getStatus())) {
                dormGroup.setStatus("forming");
            }
        }
        
        int result = dormGroupMapper.updateById(dormGroup);
        if (result > 0) {
            return Result.success(dormGroupMapper.selectById(id));
        }
        
        return Result.error("更新宿舍组失败");
    }
    
    /**
     * 更新宿舍组公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateAnnouncement(Long id, String announcement) {
        log.info("更新宿舍组公告：id={}, announcement={}", id, announcement);
        
        // 检查是否存在
        DormGroup existing = dormGroupMapper.selectById(id);
        if (existing == null) {
            return Result.error("宿舍组不存在");
        }
        
        // 更新公告
        existing.setAnnouncement(announcement);
        int result = dormGroupMapper.updateById(existing);
        
        if (result > 0) {
            return Result.success("公告更新成功");
        }
        
        return Result.error("更新公告失败");
    }
    
    /**
     * 删除宿舍组
     * 自动清理所有关联数据：聊天群组、群聊成员、聊天消息、待处理邀请
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteGroup(Long id) {
        log.info("删除宿舍组：id={}", id);
        
        // 检查是否存在
        DormGroup existing = dormGroupMapper.selectById(id);
        if (existing == null) {
            return Result.error("宿舍组不存在");
        }
        
        // 如果已选择房间，不允许删除
        if (existing.getSelectedRoomId() != null) {
            return Result.error("该宿舍组已选择房间，无法删除");
        }
        
        try {
            // 1. 清理聊天群组相关数据
            cleanupChatGroupData(id);
            
            // 2. 清理室友邀请记录（仅待处理状态）
            cleanupPendingInvites(existing);
            
            // 3. 删除宿舍组
            int result = dormGroupMapper.deleteById(id);
            if (result > 0) {
                log.info("宿舍组删除成功：id={}", id);
                return Result.success("删除成功");
            }
            
            return Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除宿舍组失败：id={}", id, e);
            throw e; // 抛出异常，触发事务回滚
        }
    }
    
    /**
     * 清理聊天群组相关数据
     */
    private void cleanupChatGroupData(Long dormGroupId) {
        try {
            // 查找关联的聊天会话
            LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Conversation::getConversationType, "dorm_group")
                   .eq(Conversation::getDormGroupId, dormGroupId)
                   .eq(Conversation::getDeleted, 0);
            
            Conversation conversation = conversationMapper.selectOne(wrapper);
            if (conversation != null) {
                Long conversationId = conversation.getId();
                
                // 1. 删除聊天消息
                LambdaQueryWrapper<ChatMessage> messageWrapper = new LambdaQueryWrapper<>();
                messageWrapper.eq(ChatMessage::getConversationId, conversationId);
                int deletedMessages = chatMessageMapper.delete(messageWrapper);
                log.info("删除聊天消息：{} 条", deletedMessages);
                
                // 2. 删除群聊成员
                LambdaQueryWrapper<GroupChatMember> memberWrapper = new LambdaQueryWrapper<>();
                memberWrapper.eq(GroupChatMember::getConversationId, conversationId);
                int deletedMembers = groupChatMemberMapper.delete(memberWrapper);
                log.info("删除群聊成员：{} 条", deletedMembers);
                
                // 3. 逻辑删除聊天会话
                conversation.setDeleted(1);
                conversationMapper.updateById(conversation);
                log.info("逻辑删除聊天会话：conversationId={}", conversationId);
            }
        } catch (Exception e) {
            log.error("清理聊天群组数据失败：dormGroupId={}", dormGroupId, e);
            // 不抛出异常，继续执行其他清理操作
        }
    }
    
    /**
     * 清理待处理的室友邀请记录
     */
    private void cleanupPendingInvites(DormGroup group) {
        try {
            // 解析成员列表，获取所有成员ID
            List<Long> memberIds = extractMemberIds(group);
            
            if (memberIds.isEmpty()) {
                return;
            }
            
            // 删除发送者的待处理邀请
            LambdaQueryWrapper<RoommateInvite> senderWrapper = new LambdaQueryWrapper<>();
            senderWrapper.in(RoommateInvite::getSenderId, memberIds)
                        .eq(RoommateInvite::getStatus, "PENDING");
            int deletedSenderInvites = roommateInviteMapper.delete(senderWrapper);
            log.info("删除发送者待处理邀请：{} 条", deletedSenderInvites);
            
            // 删除接收者的待处理邀请
            LambdaQueryWrapper<RoommateInvite> receiverWrapper = new LambdaQueryWrapper<>();
            receiverWrapper.in(RoommateInvite::getReceiverId, memberIds)
                          .eq(RoommateInvite::getStatus, "PENDING");
            int deletedReceiverInvites = roommateInviteMapper.delete(receiverWrapper);
            log.info("删除接收者待处理邀请：{} 条", deletedReceiverInvites);
        } catch (Exception e) {
            log.error("清理邀请记录失败：groupId={}", group.getId(), e);
            // 不抛出异常，继续执行其他清理操作
        }
    }
    
    /**
     * 从宿舍组中提取所有成员ID（包括组长）
     */
    private List<Long> extractMemberIds(DormGroup group) {
        List<Long> memberIds = new ArrayList<>();
        
        // 添加组长ID
        if (group.getLeaderId() != null) {
            memberIds.add(group.getLeaderId());
        }
        
        // 解析members JSON字段，提取成员ID
        try {
            String membersJson = group.getMembers();
            if (membersJson != null && !membersJson.trim().isEmpty()) {
                List<Map<String, Object>> members = objectMapper.readValue(
                    membersJson, 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> member : members) {
                    Object idObj = member.get("id");
                    if (idObj != null) {
                        Long memberId = ((Number) idObj).longValue();
                        if (!memberIds.contains(memberId)) {
                            memberIds.add(memberId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析成员列表失败：groupId={}", group.getId(), e);
        }
        
        return memberIds;
    }
    
    /**
     * 计算下一个可用的床位号
     * 床位号从1开始递增，找到当前成员中最大的床位号+1
     * 
     * @param members 当前成员列表
     * @return 下一个可用的床位号字符串
     */
    public static String getNextBedNumber(List<Map<String, Object>> members) {
        if (members == null || members.isEmpty()) {
            return "1";
        }
        
        int maxBedNumber = 0;
        for (Map<String, Object> member : members) {
            Object bedNumberObj = member.get("bedNumber");
            if (bedNumberObj != null) {
                try {
                    int bedNumber = Integer.parseInt(bedNumberObj.toString());
                    if (bedNumber > maxBedNumber) {
                        maxBedNumber = bedNumber;
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的床位号
                }
            }
        }
        
        return String.valueOf(maxBedNumber + 1);
    }
    
    /**
     * 为成员分配床位号
     * 
     * @param member 成员Map
     * @param bedNumber 床位号
     */
    public static void assignBedNumberToMember(Map<String, Object> member, String bedNumber) {
        if (member != null && bedNumber != null) {
            member.put("bedNumber", bedNumber);
        }
    }
    
    /**
     * 从成员列表中获取指定学生的床位号
     * 
     * @param membersJson members JSON字符串
     * @param studentId 学生ID
     * @return 床位号，如果未找到返回null
     */
    public String getBedNumberForStudent(String membersJson, Long studentId) {
        if (membersJson == null || membersJson.trim().isEmpty() || studentId == null) {
            return null;
        }
        
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                membersJson,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> member : members) {
                Object idObj = member.get("id");
                if (idObj != null) {
                    Long memberId = ((Number) idObj).longValue();
                    if (memberId.equals(studentId)) {
                        Object bedNumberObj = member.get("bedNumber");
                        return bedNumberObj != null ? bedNumberObj.toString() : null;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析成员列表获取床位号失败：studentId={}", studentId, e);
        }
        
        return null;
    }
    
    /**
     * 选择房间
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> selectRoom(Long id, Long roomId, Long buildingId) {
        log.info("选择房间：id={}, roomId={}, buildingId={}", id, roomId, buildingId);
        
        // 检查宿舍组是否存在
        DormGroup group = dormGroupMapper.selectById(id);
        if (group == null) {
            return Result.error("宿舍组不存在");
        }
        
        // 检查是否已满员
        if (!"complete".equals(group.getStatus())) {
            return Result.error("宿舍组未满员，无法选择房间");
        }
        
        // 更新房间信息
        group.setSelectedRoomId(roomId);
        group.setStatus("room_selected");
        
        int result = dormGroupMapper.updateById(group);
        if (result > 0) {
            return Result.success(dormGroupMapper.selectById(id));
        }
        
        return Result.error("选择房间失败");
    }
    
    /**
     * 获取统计数据
     */
    public Result<?> getStatistics() {
        log.info("获取宿舍组统计数据");
        
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        
        // 总数
        Long total = dormGroupMapper.selectCount(wrapper);
        
        // 未满员数量
        wrapper.clear();
        wrapper.eq(DormGroup::getStatus, "forming");
        Long incompleteCount = dormGroupMapper.selectCount(wrapper);
        
        // 已满员数量
        wrapper.clear();
        wrapper.eq(DormGroup::getStatus, "complete");
        Long completeCount = dormGroupMapper.selectCount(wrapper);
        
        // 已选房间数量
        wrapper.clear();
        wrapper.eq(DormGroup::getStatus, "room_selected");
        Long roomSelectedCount = dormGroupMapper.selectCount(wrapper);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("incomplete", incompleteCount);
        stats.put("complete", completeCount);
        stats.put("roomSelected", roomSelectedCount);
        
        return Result.success(stats);
    }
    
    /**
     * 取消房间选择（管理员操作）
     * @param groupId 宿舍组ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelRoomSelection(Long groupId) {
        log.info("管理员取消房间选择：groupId={}", groupId);
        
        // 1. 获取宿舍组
        DormGroup group = dormGroupMapper.selectById(groupId);
        if (group == null) {
            return Result.error("宿舍组不存在");
        }
        
        // 2. 检查宿舍组是否已选房
        if (!"room_selected".equals(group.getStatus()) || group.getSelectedRoomId() == null) {
            return Result.error("该宿舍组尚未选择房间");
        }
        
        Long oldRoomId = group.getSelectedRoomId();
        
        // 3. 释放原房间床位
        DormRoom oldRoom = dormRoomMapper.selectById(oldRoomId);
        if (oldRoom != null) {
            int releaseBeds = group.getCurrentMembers() != null ? group.getCurrentMembers() : 0;
            oldRoom.setAvailableBeds(oldRoom.getAvailableBeds() + releaseBeds);
            dormRoomMapper.updateById(oldRoom);
            log.info("释放房间床位：roomId={}, 释放数量={}", oldRoomId, releaseBeds);
        }
        
        // 4. 床位信息已存储在dorm_group.members JSON中，无需额外删除分配记录
        // 床位号会在重新选房时自动重新分配
        log.info("取消房间选择，床位信息将在重新选房时更新：roomId={}", oldRoomId);
        
        // 5. 更新宿舍组状态（回到满员待选房状态）
        group.setSelectedRoomId(null);
        group.setStatus("full"); // 回到满员状态，可以重新选房（数据库ENUM值：forming/full/room_selected/allocated/disbanded）
        dormGroupMapper.updateById(group);
        
        log.info("取消房间选择成功：groupId={}, oldRoomId={}", groupId, oldRoomId);
        return Result.success("已取消房间选择，该宿舍组可以重新选择房间");
    }
    
    /**
     * 数据迁移：为现有宿舍组成员添加 bedNumber
     * 按成员在数组中的位置分配床位号（1, 2, 3...）
     * 
     * @return 迁移结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> migrateBedNumbers() {
        log.info("开始迁移床位号数据");
        
        int totalGroups = 0;
        int successGroups = 0;
        int failedGroups = 0;
        int totalMembers = 0;
        
        try {
            // 查询所有宿舍组
            List<DormGroup> allGroups = dormGroupMapper.selectList(null);
            totalGroups = allGroups.size();
            
            for (DormGroup group : allGroups) {
                try {
                    String membersJson = group.getMembers();
                    if (membersJson == null || membersJson.trim().isEmpty() || "[]".equals(membersJson.trim())) {
                        log.debug("宿舍组 {} 没有成员，跳过", group.getId());
                        successGroups++;
                        continue;
                    }
                    
                    // 解析成员列表
                    List<Map<String, Object>> members = objectMapper.readValue(
                        membersJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    
                    // 检查是否已有 bedNumber
                    boolean hasBedNumber = members.stream()
                        .anyMatch(m -> m.get("bedNumber") != null);
                    
                    if (hasBedNumber) {
                        log.debug("宿舍组 {} 已有床位号，跳过", group.getId());
                        successGroups++;
                        continue;
                    }
                    
                    // 为每个成员分配床位号（按数组位置）
                    for (int i = 0; i < members.size(); i++) {
                        Map<String, Object> member = members.get(i);
                        assignBedNumberToMember(member, String.valueOf(i + 1));
                        totalMembers++;
                    }
                    
                    // 更新宿舍组
                    group.setMembers(objectMapper.writeValueAsString(members));
                    dormGroupMapper.updateById(group);
                    
                    successGroups++;
                    log.debug("宿舍组 {} 迁移成功，成员数: {}", group.getId(), members.size());
                    
                } catch (Exception e) {
                    failedGroups++;
                    log.error("迁移宿舍组 {} 失败", group.getId(), e);
                    // 继续处理其他记录
                }
            }
            
            log.info("床位号迁移完成：总宿舍组={}, 成功={}, 失败={}, 总成员数={}", 
                     totalGroups, successGroups, failedGroups, totalMembers);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalGroups", totalGroups);
            result.put("successGroups", successGroups);
            result.put("failedGroups", failedGroups);
            result.put("totalMembers", totalMembers);
            result.put("message", String.format("迁移完成：%d 个宿舍组成功，%d 个失败，共 %d 名成员", 
                                                successGroups, failedGroups, totalMembers));
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("床位号迁移失败", e);
            return Result.error("迁移失败：" + e.getMessage());
        }
    }
}

