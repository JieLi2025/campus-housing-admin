package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * 宿舍群聊服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DormGroupChatService {
    
    @Resource
    private ConversationMapper conversationMapper;
    
    @Resource
    private ChatMessageMapper chatMessageMapper;
    
    @Resource
    private GroupChatMemberMapper groupChatMemberMapper;
    
    @Resource
    private DormGroupMapper dormGroupMapper;
    
    @Resource
    private StudentMapper studentMapper;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private WebSocketService webSocketService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
    @Resource
    private ObjectMapper objectMapper;
    
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
     * 获取或创建宿舍群聊会话
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> getOrCreateDormGroupChat(Long dormGroupId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证队伍是否存在
        DormGroup dormGroup = dormGroupMapper.selectById(dormGroupId);
        if (dormGroup == null) {
            return Result.error("队伍不存在");
        }
        
        // 验证用户是否是队伍成员
        if (!isDormGroupMember(dormGroup, userId)) {
            return Result.error("您不是该队伍的成员");
        }
        
        // 查找是否已存在该队伍的群聊
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "dorm_group")
               .eq(Conversation::getDormGroupId, dormGroupId)
               .eq(Conversation::getDeleted, 0);
        
        Conversation conversation = conversationMapper.selectOne(wrapper);
        
        if (conversation == null) {
            // 创建新群聊
            conversation = new Conversation();
            conversation.setConversationType("dorm_group");
            conversation.setDormGroupId(dormGroupId);
            conversation.setGroupName(dormGroup.getGroupName() + "群聊");
            conversation.setUser1Id(null); // 群聊时user1_id设为null
            conversation.setUser2Id(null); // 群聊时user2_id设为null
            conversationMapper.insert(conversation);
            
            log.info("创建宿舍群聊：dormGroupId={}, conversationId={}", dormGroupId, conversation.getId());
            
            // 添加所有队伍成员到群聊
            addAllMembersToGroupChat(conversation.getId(), dormGroup);
        } else {
            // 确保用户是群成员
            addUserToGroupChat(conversation.getId(), userId);
        }
        
        return Result.success(conversation.getId());
    }
    
    /**
     * 发送群聊消息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<ChatMessageVO> sendGroupMessage(Long dormGroupId, SendMessageDTO dto) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取或创建群聊会话
        Result<Long> convResult = getOrCreateDormGroupChat(dormGroupId);
        if (!convResult.isSuccess()) {
            return Result.error(convResult.getMessage());
        }
        
        Long conversationId = convResult.getData();
        
        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(null); // 群聊消息receiver_id为NULL
        message.setContent(dto.getContent());
        message.setFileUrl(dto.getFileUrl());
        message.setQuotedMessageId(dto.getQuotedMessageId());
        message.setIsRecalled(false);
        message.setIsAnonymous(dto.getIsAnonymous() != null ? dto.getIsAnonymous() : false);
        
        chatMessageMapper.insert(message);
        
        // 更新会话的最后消息信息
        Conversation conversation = conversationMapper.selectById(conversationId);
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationMapper.updateById(conversation);
        
        // 转换为VO
        ChatMessageVO vo = convertToChatMessageVO(message, userId);
        
        // 通过WebSocket推送消息给所有群成员
        pushGroupMessage(dormGroupId, vo);
        
        return Result.success(vo);
    }
    
    /**
     * 获取群聊消息历史
     */
    public Result<List<ChatMessageVO>> getGroupMessages(Long dormGroupId, Integer pageNum, Integer pageSize) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "dorm_group")
               .eq(Conversation::getDormGroupId, dormGroupId)
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
    public Result<List<Map<String, Object>>> getGroupMembers(Long dormGroupId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "dorm_group")
               .eq(Conversation::getDormGroupId, dormGroupId)
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
        
        // 转换为成员信息列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupChatMember member : members) {
            Map<String, Object> memberInfo = new HashMap<>();
            SysUser user = sysUserMapper.selectById(member.getUserId());
            if (user != null) {
                memberInfo.put("userId", user.getId());
                memberInfo.put("userName", user.getRealName());
                memberInfo.put("avatar", user.getAvatar());
                memberInfo.put("role", member.getRole());
                
                // 获取学生信息
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getUserId, user.getId());
                Student student = studentMapper.selectOne(studentWrapper);
                if (student != null) {
                    memberInfo.put("id", student.getId()); // 学生表ID
                    memberInfo.put("studentId", student.getStudentId()); // 学号
                    memberInfo.put("studentNumber", student.getStudentId()); // 学号（兼容）
                    memberInfo.put("major", student.getMajor());
                    memberInfo.put("className", student.getClassName());
                }
                
                result.add(memberInfo);
            }
        }
        
        return Result.success(result);
    }
    
    /**
     * 当学生加入队伍时，自动加入群聊
     */
    @Transactional(rollbackFor = Exception.class)
    public void onStudentJoinDormGroup(Long dormGroupId, Long studentId) {
        try {
            // 获取或创建群聊
            DormGroup dormGroup = dormGroupMapper.selectById(dormGroupId);
            if (dormGroup == null) {
                return;
            }
            
            LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Conversation::getConversationType, "dorm_group")
                   .eq(Conversation::getDormGroupId, dormGroupId)
                   .eq(Conversation::getDeleted, 0);
            
            Conversation conversation = conversationMapper.selectOne(wrapper);
            
            if (conversation == null) {
                // 创建新群聊
                conversation = new Conversation();
                conversation.setConversationType("dorm_group");
                conversation.setDormGroupId(dormGroupId);
                conversation.setGroupName(dormGroup.getGroupName() + "群聊");
                conversation.setUser1Id(null);
                conversation.setUser2Id(null);
                conversationMapper.insert(conversation);
                
                // 添加所有成员
                addAllMembersToGroupChat(conversation.getId(), dormGroup);
            } else {
                // 添加新成员
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getId, studentId);
                Student student = studentMapper.selectOne(studentWrapper);
                if (student != null && student.getUserId() != null) {
                    addUserToGroupChat(conversation.getId(), student.getUserId());
                }
            }
            
            log.info("学生{}加入队伍{}的群聊", studentId, dormGroupId);
        } catch (Exception e) {
            log.error("学生加入群聊失败", e);
        }
    }
    
    /**
     * 当学生退出队伍时，自动退出群聊
     */
    @Transactional(rollbackFor = Exception.class)
    public void onStudentLeaveDormGroup(Long dormGroupId, Long studentId) {
        try {
            LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Conversation::getConversationType, "dorm_group")
                   .eq(Conversation::getDormGroupId, dormGroupId)
                   .eq(Conversation::getDeleted, 0);
            
            Conversation conversation = conversationMapper.selectOne(wrapper);
            if (conversation == null) {
                return;
            }
            
            // 获取学生的userId
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getId, studentId);
            Student student = studentMapper.selectOne(studentWrapper);
            if (student == null || student.getUserId() == null) {
                return;
            }
            
            // 标记成员为非活跃
            LambdaQueryWrapper<GroupChatMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(GroupChatMember::getConversationId, conversation.getId())
                        .eq(GroupChatMember::getUserId, student.getUserId());
            
            GroupChatMember member = groupChatMemberMapper.selectOne(memberWrapper);
            if (member != null) {
                member.setIsActive(false);
                groupChatMemberMapper.updateById(member);
            }
            
            log.info("学生{}退出队伍{}的群聊", studentId, dormGroupId);
        } catch (Exception e) {
            log.error("学生退出群聊失败", e);
        }
    }
    
    /**
     * 检查用户是否是队伍成员
     */
    private boolean isDormGroupMember(DormGroup dormGroup, Long userId) {
        try {
            // 获取学生ID
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getUserId, userId);
            Student student = studentMapper.selectOne(studentWrapper);
            if (student == null) {
                return false;
            }
            
            // 检查是否是组长
            if (dormGroup.getLeaderId().equals(student.getId())) {
                return true;
            }
            
            // 检查是否在成员列表中
            List<Map<String, Object>> members = objectMapper.readValue(
                dormGroup.getMembers(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            return members.stream()
                .anyMatch(m -> student.getId().equals(((Number) m.get("id")).longValue()));
        } catch (Exception e) {
            log.error("检查队伍成员失败", e);
            return false;
        }
    }
    
    /**
     * 添加所有队伍成员到群聊
     */
    private void addAllMembersToGroupChat(Long conversationId, DormGroup dormGroup) {
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                dormGroup.getMembers(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> member : members) {
                Long studentId = ((Number) member.get("id")).longValue();
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getId, studentId);
                Student student = studentMapper.selectOne(studentWrapper);
                
                if (student != null && student.getUserId() != null) {
                    addUserToGroupChat(conversationId, student.getUserId());
                }
            }
        } catch (Exception e) {
            log.error("添加所有成员到群聊失败", e);
        }
    }
    
    /**
     * 添加用户到群聊
     */
    private void addUserToGroupChat(Long conversationId, Long userId) {
        LambdaQueryWrapper<GroupChatMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GroupChatMember::getConversationId, conversationId)
               .eq(GroupChatMember::getUserId, userId);
        
        GroupChatMember existing = groupChatMemberMapper.selectOne(wrapper);
        if (existing == null) {
            GroupChatMember member = new GroupChatMember();
            member.setConversationId(conversationId);
            member.setUserId(userId);
            member.setRole("member");
            member.setIsActive(true);
            groupChatMemberMapper.insert(member);
        } else if (!existing.getIsActive()) {
            // 重新激活
            existing.setIsActive(true);
            groupChatMemberMapper.updateById(existing);
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
     * 推送群聊消息给所有成员
     */
    private void pushGroupMessage(Long dormGroupId, ChatMessageVO message) {
        // 获取群聊会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getConversationType, "dorm_group")
               .eq(Conversation::getDormGroupId, dormGroupId)
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
        
        // 推送消息给每个成员（除了发送者）
        for (GroupChatMember member : members) {
            if (!member.getUserId().equals(message.getSenderId())) {
                webSocketService.sendNewMessage(member.getUserId(), message);
            }
        }
        
        // 同时通过topic推送（用于实时订阅）
        webSocketService.sendDormGroupMessage(dormGroupId, message);
    }
    
    /**
     * 转换为消息VO
     */
    private ChatMessageVO convertToChatMessageVO(ChatMessage message, Long currentUserId) {
        ChatMessageVO vo = new ChatMessageVO();
        BeanUtils.copyProperties(message, vo);
        
        vo.setIsOwn(message.getSenderId().equals(currentUserId));
        vo.setIsAnonymous(message.getIsAnonymous() != null ? message.getIsAnonymous() : false);
        
        // 获取发送者信息
        // 如果是匿名消息，不显示发送者名称和头像
        if (Boolean.TRUE.equals(message.getIsAnonymous())) {
            vo.setSenderName(""); // 匿名时不显示名称
            vo.setSenderAvatar(""); // 匿名时不显示头像
        } else {
            SysUser sender = sysUserMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getRealName());
                vo.setSenderAvatar(sender.getAvatar());
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

