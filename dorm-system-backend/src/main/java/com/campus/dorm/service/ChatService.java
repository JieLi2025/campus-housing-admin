package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.common.ResultCode;
import com.campus.dorm.dto.*;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.ChatMessageVO;
import com.campus.dorm.vo.ContactVO;
import com.campus.dorm.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    @Resource
    private ConversationMapper conversationMapper;
    
    @Resource
    private ChatMessageMapper chatMessageMapper;
    
    @Resource
    private ContactMapper contactMapper;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private StudentMapper studentMapper;
    
    @Resource
    private AdminMapper adminMapper;
    
    @Resource
    private BuildingMapper buildingMapper;
    
    @Resource
    private DormRoomMapper dormRoomMapper;
    
    @Resource
    private BuildingMajorAssignmentMapper buildingMajorAssignmentMapper;
    
    @Resource
    private DormGroupMapper dormGroupMapper;
    
    @Resource
    private WebSocketService webSocketService;
    
    @Resource
    private UnreadCacheService unreadCacheService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
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
     * 获取会话列表
     * 只返回联系人列表中的会话（已删除的联系人不会出现在会话列表中）
     */
    public Result<List<ConversationVO>> getConversations() {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 1. 先获取联系人列表
        LambdaQueryWrapper<Contact> contactWrapper = new LambdaQueryWrapper<>();
        contactWrapper.eq(Contact::getUserId, userId);
        List<Contact> contacts = contactMapper.selectList(contactWrapper);
        Set<Long> contactUserIds = contacts.stream()
                .map(Contact::getContactId)
                .collect(Collectors.toSet());
        
        // 2. 查询用户参与的所有会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Conversation::getUser1Id, userId)
                        .or()
                        .eq(Conversation::getUser2Id, userId));
        wrapper.orderByDesc(Conversation::getLastMessageTime);
        
        List<Conversation> conversations = conversationMapper.selectList(wrapper);
        
        // 3. 过滤出联系人列表中的会话
        List<ConversationVO> result = conversations.stream()
                .filter(conv -> {
                    // 获取会话中的另一个用户ID
                    Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
                    // 只返回在联系人列表中的会话
                    return contactUserIds.contains(otherUserId);
                })
                .map(conv -> convertToConversationVO(conv, userId))
                .collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    /**
     * 获取消息历史
     */
    public Result<List<ChatMessageVO>> getMessages(Long conversationId, Integer pageNum, Integer pageSize) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证会话是否存在且用户有权限
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || 
            (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId))) {
            return Result.error("会话不存在或无权限");
        }
        
        // 查询消息（分页）
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);
        wrapper.orderByDesc(ChatMessage::getCreatedAt);
        wrapper.last("LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize);
        
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        
        // 反转顺序（最新的在前）
        Collections.reverse(messages);
        
        // 转换为VO
        List<ChatMessageVO> result = messages.stream()
                .map(msg -> convertToChatMessageVO(msg, userId))
                .collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    /**
     * 创建会话
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createConversation(CreateConversationDTO dto) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        Long otherUserId = dto.getUserId();
        if (otherUserId.equals(userId)) {
            return Result.error("不能与自己创建会话");
        }
        
        // 检查是否已存在会话
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Conversation::getUser1Id, userId).eq(Conversation::getUser2Id, otherUserId)
                        .or()
                        .eq(Conversation::getUser1Id, otherUserId).eq(Conversation::getUser2Id, userId));
        
        Conversation existing = conversationMapper.selectOne(wrapper);
        if (existing != null) {
            // 如果会话已存在，确保双方都在联系人列表中
            ensureContactExists(userId, otherUserId);
            ensureContactExists(otherUserId, userId);
            return Result.success(existing.getId());
        }
        
        // 创建新会话
        Conversation conversation = new Conversation();
        conversation.setUser1Id(userId);
        conversation.setUser2Id(otherUserId);
        conversationMapper.insert(conversation);
        
        // 自动为双方添加联系人（如果不存在）
        ensureContactExists(userId, otherUserId);
        ensureContactExists(otherUserId, userId);
        
        return Result.success(conversation.getId());
    }
    
    /**
     * 确保联系人存在（智能判断是否需要双向添加）
     * 学生 -> 管理员：单向添加（学生端显示）
     * 管理员 -> 学生：只有学生发消息后才添加（管理员端显示）
     */
    private void ensureContactExists(Long senderId, Long receiverId) {
        try {
            // 获取发送者和接收者信息
            SysUser sender = sysUserMapper.selectById(senderId);
            SysUser receiver = sysUserMapper.selectById(receiverId);
            
            if (sender == null || receiver == null) {
                log.warn("发送者或接收者不存在: senderId={}, receiverId={}", senderId, receiverId);
                return;
            }
            
            boolean senderIsStudent = "student".equals(sender.getUserType());
            boolean receiverIsStudent = "student".equals(receiver.getUserType());
            boolean senderIsAdmin = "admin".equals(sender.getUserType()) || "superadmin".equals(sender.getUserType());
            boolean receiverIsAdmin = "admin".equals(receiver.getUserType()) || "superadmin".equals(receiver.getUserType());
            
            // 情况1：学生发送消息给管理员
            if (senderIsStudent && receiverIsAdmin) {
                // 学生端：确保管理员在学生的联系人列表中（自动添加）
                ensureContactExistsOneWay(senderId, receiverId, true);
                
                // 管理员端：添加学生到管理员的联系人列表（手动添加，因为学生发了消息）
                ensureContactExistsOneWay(receiverId, senderId, false);
                
                log.debug("学生{}发送消息给管理员{}，双向添加联系人", senderId, receiverId);
            }
            // 情况2：管理员发送消息给学生
            else if (senderIsAdmin && receiverIsStudent) {
                // 管理员端：确保学生在管理员的联系人列表中
                ensureContactExistsOneWay(senderId, receiverId, false);
                
                // 学生端：确保管理员在学生的联系人列表中（自动添加）
                ensureContactExistsOneWay(receiverId, senderId, true);
                
                log.debug("管理员{}发送消息给学生{}，双向添加联系人", senderId, receiverId);
            }
            // 情况3：学生之间互发消息
            else if (senderIsStudent && receiverIsStudent) {
                // 双向添加（都是手动添加）
                ensureContactExistsOneWay(senderId, receiverId, false);
                ensureContactExistsOneWay(receiverId, senderId, false);
                
                log.debug("学生{}和学生{}互发消息，双向添加联系人", senderId, receiverId);
            }
            // 情况4：管理员之间互发消息
            else if (senderIsAdmin && receiverIsAdmin) {
                // 双向添加（都是手动添加）
                ensureContactExistsOneWay(senderId, receiverId, false);
                ensureContactExistsOneWay(receiverId, senderId, false);
                
                log.debug("管理员{}和管理员{}互发消息，双向添加联系人", senderId, receiverId);
            }
        } catch (Exception e) {
            log.error("确保联系人存在失败: senderId={}, receiverId={}, error={}", 
                senderId, receiverId, e.getMessage(), e);
        }
    }
    
    /**
     * 单向确保联系人存在
     * @param userId 用户ID
     * @param contactId 联系人ID
     * @param isAutoAdded 是否自动添加（true表示学生自动添加的管理员）
     */
    private void ensureContactExistsOneWay(Long userId, Long contactId, boolean isAutoAdded) {
        try {
            LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Contact::getUserId, userId)
                   .eq(Contact::getContactId, contactId);
            Contact existingContact = contactMapper.selectOne(wrapper);
            
            if (existingContact == null) {
                Contact contact = new Contact();
                contact.setUserId(userId);
                contact.setContactId(contactId);
                contact.setIsAutoAdded(isAutoAdded ? 1 : 0);
                contactMapper.insert(contact);
                
                log.info("添加联系人关系: userId={}, contactId={}, isAutoAdded={}", 
                    userId, contactId, isAutoAdded);
            }
        } catch (Exception e) {
            log.error("单向添加联系人失败: userId={}, contactId={}, error={}", 
                userId, contactId, e.getMessage(), e);
        }
    }
    
    /**
     * 为学生自动添加楼栋管理员联系人
     * 🔧 修复：优先使用 adminId 字段查找管理员，避免因用户名修改导致找不到管理员
     * @param studentUserId 学生的用户ID
     */
    private void autoAddBuildingManagerContact(Long studentUserId) {
        try {
            // 1. 获取学生信息
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getUserId, studentUserId);
            Student student = studentMapper.selectOne(studentWrapper);
            
            if (student == null) {
                log.debug("学生不存在，无法添加管理员联系人: userId={}", studentUserId);
                return;
            }
            
            // 2. 查询学生所在楼栋（通过building_major_assignment表）
            Long buildingId = getBuildingIdForStudent(student);
            
            if (buildingId == null) {
                log.debug("学生{}未找到楼栋分配，无法添加管理员联系人", studentUserId);
                return;
            }
            
            // 3. 获取楼栋信息
            Building building = buildingMapper.selectById(buildingId);
            if (building == null) {
                log.debug("楼栋{}不存在，无法添加联系人", buildingId);
                return;
            }
            
            // 4. 🔧 修复：优先通过 adminId 查找管理员（更可靠，不受用户名修改影响）
            SysUser manager = null;
            
            // 方式1：通过 adminId 查找（推荐）
            if (building.getAdminId() != null) {
                // 先从 admin 表获取 userId
                Admin admin = adminMapper.selectById(building.getAdminId());
                if (admin != null && admin.getUserId() != null) {
                    manager = sysUserMapper.selectById(admin.getUserId());
                    if (manager != null && manager.getStatus() != 1) {
                        log.debug("管理员账号已禁用: adminId={}, userId={}", building.getAdminId(), admin.getUserId());
                        manager = null;
                    }
                }
            }
            
            // 方式2：如果 adminId 方式失败，回退到 managerName 方式（兼容旧数据）
            if (manager == null && building.getManagerName() != null && !building.getManagerName().trim().isEmpty()) {
                LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.eq(SysUser::getUsername, building.getManagerName())
                          .in(SysUser::getUserType, Arrays.asList("admin", "superadmin"))
                          .eq(SysUser::getStatus, 1);
                manager = sysUserMapper.selectOne(userWrapper);
                
                if (manager == null) {
                    log.debug("通过managerName未找到管理员: buildingId={}, managerName={}", buildingId, building.getManagerName());
                }
            }
            
            // 如果两种方式都找不到管理员，静默返回（不再打印警告，因为可能是正常情况）
            if (manager == null) {
                log.debug("楼栋{}没有配置有效的管理员，跳过自动添加联系人", buildingId);
                return;
            }
            
            // 5. 检查是否已添加
            LambdaQueryWrapper<Contact> contactWrapper = new LambdaQueryWrapper<>();
            contactWrapper.eq(Contact::getUserId, studentUserId)
                         .eq(Contact::getContactId, manager.getId());
            Contact existingContact = contactMapper.selectOne(contactWrapper);
            
            if (existingContact == null) {
                // 6. 添加联系人（单向：学生 -> 管理员）
                Contact contact = new Contact();
                contact.setUserId(studentUserId);
                contact.setContactId(manager.getId());
                contact.setIsAutoAdded(1); // 标记为自动添加
                contactMapper.insert(contact);
                
                log.info("自动为学生{}添加楼栋管理员{}联系人: buildingId={}", 
                    studentUserId, manager.getId(), buildingId);
            } else {
                log.debug("学生{}已有管理员{}联系人，无需重复添加", studentUserId, manager.getId());
            }
        } catch (Exception e) {
            log.error("自动添加管理员联系人失败: studentUserId={}, error={}", studentUserId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取学生所在楼栋ID
     * 通过building_major_assignment表查询
     */
    private Long getBuildingIdForStudent(Student student) {
        try {
            // 处理性别字段（兼容中英文）
            String gender = student.getGender();
            
            LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BuildingMajorAssignment::getGrade, student.getGrade())
                   .eq(BuildingMajorAssignment::getMajorName, student.getMajor())
                   .eq(BuildingMajorAssignment::getGender, gender)
                   .eq(BuildingMajorAssignment::getStatus, 1)
                   .last("LIMIT 1");
            
            BuildingMajorAssignment assignment = buildingMajorAssignmentMapper.selectOne(wrapper);
            return assignment != null ? assignment.getBuildingId() : null;
        } catch (Exception e) {
            log.error("查询学生楼栋失败: studentId={}, error={}", student.getId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<ChatMessageVO> sendMessage(SendMessageDTO dto) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        Long conversationId = dto.getConversationId();
        Long receiverId = dto.getReceiverId();
        
        // 如果没有会话ID，需要创建会话
        if (conversationId == null) {
            if (receiverId == null) {
                return Result.error("接收者ID不能为空");
            }
            
            CreateConversationDTO createDto = new CreateConversationDTO();
            createDto.setUserId(receiverId);
            Result<Long> createResult = createConversation(createDto);
            if (!createResult.isSuccess()) {
                return Result.error(createResult.getMessage());
            }
            conversationId = createResult.getData();
        }
        
        // 验证会话是否存在并获取会话信息
        // 使用查询条件确保会话存在且未被逻辑删除
        Conversation conversation = conversationMapper.selectById(conversationId);
        boolean needCreateConversation = false;
        
        // 如果会话不存在或已被逻辑删除，需要创建新会话
        if (conversation == null || (conversation.getDeleted() != null && conversation.getDeleted() == 1)) {
            needCreateConversation = true;
        } else {
            // 验证用户是否有权限访问此会话
            if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
                // 权限验证失败，可能需要创建新会话
                if (receiverId != null) {
                    needCreateConversation = true;
                } else {
                    return Result.error("无权访问此会话");
                }
            }
        }
        
        // 如果需要创建会话
        if (needCreateConversation) {
            if (receiverId == null) {
                return Result.error("会话不存在且接收者ID不能为空");
            }
            
            // 创建新会话
            CreateConversationDTO createDto = new CreateConversationDTO();
            createDto.setUserId(receiverId);
            Result<Long> createResult = createConversation(createDto);
            if (!createResult.isSuccess()) {
                return Result.error("会话不存在且无法创建新会话: " + createResult.getMessage());
            }
            conversationId = createResult.getData();
            conversation = conversationMapper.selectById(conversationId);
            
            if (conversation == null) {
                return Result.error("创建会话失败");
            }
        }
        
        // 确定接收者ID
        if (receiverId == null) {
            receiverId = conversation.getUser1Id().equals(userId) ? 
                        conversation.getUser2Id() : conversation.getUser1Id();
        }
        
        // 如果是宿舍邀请消息，检查发送者和接收者的分配状态
        if ("dorm_invite".equals(dto.getMessageType())) {
            log.info("检查宿舍邀请：发送者userId={}, 接收者userId={}", userId, receiverId);
            // 检查发送者是否已完成分配
            boolean senderAllocated = isUserAllocated(userId);
            log.info("发送者分配状态：userId={}, isAllocated={}", userId, senderAllocated);
            if (senderAllocated) {
                return Result.error("您已完成宿舍分配，无法再发送邀请");
            }
            // 检查接收者是否已完成分配
            boolean receiverAllocated = isUserAllocated(receiverId);
            log.info("接收者分配状态：userId={}, isAllocated={}", receiverId, receiverAllocated);
            if (receiverAllocated) {
                return Result.error("该学生已完成宿舍分配，无法接收邀请");
            }
        }
        
        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setContent(dto.getContent());
        message.setFileUrl(dto.getFileUrl());
        message.setQuotedMessageId(dto.getQuotedMessageId());
        message.setIsRecalled(false);
        
        // 尝试插入消息，如果外键约束失败，说明会话不存在，则创建新会话并重试
        try {
            chatMessageMapper.insert(message);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 外键约束失败，说明conversationId不存在，创建新会话并重试
            if (receiverId == null) {
                return Result.error("会话不存在且接收者ID不能为空");
            }
            
            log.warn("插入消息时外键约束失败，conversationId={}，尝试创建新会话", conversationId);
            
            // 创建新会话
            CreateConversationDTO createDto = new CreateConversationDTO();
            createDto.setUserId(receiverId);
            Result<Long> createResult = createConversation(createDto);
            if (!createResult.isSuccess()) {
                return Result.error("会话不存在且无法创建新会话: " + createResult.getMessage());
            }
            conversationId = createResult.getData();
            conversation = conversationMapper.selectById(conversationId);
            
            if (conversation == null) {
                return Result.error("创建会话失败");
            }
            
            // 更新消息的conversationId并重试插入
            message.setConversationId(conversationId);
            chatMessageMapper.insert(message);
        }
        
        // 更新会话的最后消息信息
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversationMapper.updateById(conversation);
        
        // 更新接收者的未读数缓存（增加1）
        try {
            unreadCacheService.incrementUnreadCount(receiverId, conversationId, 1);
            log.debug("发送消息，增加接收者未读数: receiverId={}, conversationId={}", receiverId, conversationId);
        } catch (Exception e) {
            log.warn("更新Redis缓存失败，忽略: {}", e.getMessage());
        }
        
        // 确保双方都在联系人列表中（如果不存在则自动创建）
        ensureContactExists(userId, receiverId);
        ensureContactExists(receiverId, userId);
        
        // 转换为VO（用于返回给发送者）
        ChatMessageVO vo = convertToChatMessageVO(message, userId);
        
        // 通过WebSocket推送消息给接收者（使用接收者的视角转换VO）
        ChatMessageVO receiverVo = convertToChatMessageVO(message, receiverId);
        webSocketService.sendNewMessage(receiverId, receiverVo);
        
        // 通过WebSocket推送消息给发送者自己（用于多标签页同步）
        webSocketService.sendNewMessage(userId, vo);
        
        // 推送会话更新通知给双方
        webSocketService.sendConversationUpdated(userId, conversationId);
        webSocketService.sendConversationUpdated(receiverId, conversationId);
        
        return Result.success(vo);
    }
    
    /**
     * 撤回消息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> recallMessage(Long messageId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            return Result.error("消息不存在");
        }
        
        // 只能撤回自己发送的消息
        if (!message.getSenderId().equals(userId)) {
            return Result.error("只能撤回自己发送的消息");
        }
        
        // 检查是否超过2分钟
        Duration duration = Duration.between(message.getCreatedAt(), LocalDateTime.now());
        if (duration.toMinutes() > 2) {
            return Result.error("消息发送超过2分钟，无法撤回");
        }
        
        // 标记为已撤回
        message.setIsRecalled(true);
        chatMessageMapper.updateById(message);
        
        // 通过WebSocket推送消息撤回通知给接收者
        webSocketService.sendMessageRecalled(message.getReceiverId(), messageId, message.getConversationId());
        
        // 推送会话更新通知给双方
        webSocketService.sendConversationUpdated(message.getSenderId(), message.getConversationId());
        webSocketService.sendConversationUpdated(message.getReceiverId(), message.getConversationId());
        
        return Result.success();
    }

    /**
     * 更新消息类型（主要用于更新邀请状态）
     * 允许接收者修改类型（例如从 dorm_invite 修改为 dorm_invite_accepted）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMessageType(Long messageId, String newType) {
        // ... (existing code) ...
        
        return Result.success();
    }
    
    /**
     * 删除消息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteMessage(Long messageId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            return Result.error("消息不存在");
        }
        
        // 只能删除自己发送的消息
        if (!message.getSenderId().equals(userId)) {
            return Result.error("只能删除自己发送的消息");
        }
        
        // 逻辑删除
        chatMessageMapper.deleteById(messageId);
        
        // 推送消息删除通知（可选，这里只做本地删除的话不需要推送，但通常应该同步）
        // 这里我们可以选择不推送，让前端自己处理移除，或者推送一个 "messageDeleted" 事件
        // 为了简单起见，这里只做后端删除，前端调用成功后自己移除
        
        return Result.success();
    }
    
    /**
     * 标记会话为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> markAsRead(Long conversationId) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 验证会话
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || 
            (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId))) {
            return Result.error("会话不存在或无权限");
        }
        
        // 标记已读：更新联系人的updated_at时间戳
        // 实时计算时会基于这个时间戳来判断未读消息
        Long otherUserId = conversation.getUser1Id().equals(userId) ? 
                          conversation.getUser2Id() : conversation.getUser1Id();
        
        LambdaQueryWrapper<Contact> contactWrapper = new LambdaQueryWrapper<>();
        contactWrapper.eq(Contact::getUserId, userId);
        contactWrapper.eq(Contact::getContactId, otherUserId);
        Contact contact = contactMapper.selectOne(contactWrapper);
        
        if (contact != null) {
            // 更新updated_at时间戳，标记为已读
            contact.setUpdatedAt(LocalDateTime.now());
            contactMapper.updateById(contact);
            
            // 清除Redis缓存
            try {
                unreadCacheService.clearUnreadCount(userId, conversationId);
                log.debug("标记已读，清除缓存: userId={}, conversationId={}", userId, conversationId);
            } catch (Exception e) {
                log.warn("清除Redis缓存失败，忽略: {}", e.getMessage());
            }
        }
        
        return Result.success();
    }
    
    /**
     * 获取联系人列表
     */
    public Result<List<ContactVO>> getContacts() {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 检查用户类型
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }
        
        // 如果是学生，自动添加楼栋管理员（如果未添加）
        if ("student".equals(currentUser.getUserType())) {
            autoAddBuildingManagerContact(userId);
        }
        
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.orderByDesc(Contact::getCreatedAt);
        
        List<Contact> contacts = contactMapper.selectList(wrapper);
        
        // 转换为VO
        List<ContactVO> result = contacts.stream()
                .map(this::convertToContactVO)
                .collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    /**
     * 添加联系人
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addContact(AddContactDTO dto) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        Long contactId = dto.getContactId();
        if (contactId.equals(userId)) {
            return Result.error("不能添加自己为联系人");
        }
        
        // 检查是否已存在
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getUserId, userId);
        wrapper.eq(Contact::getContactId, contactId);
        
        Contact existing = contactMapper.selectOne(wrapper);
        if (existing != null) {
            return Result.error("联系人已存在");
        }
        
        // 创建联系人
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactId(contactId);
        contactMapper.insert(contact);
        
        return Result.success();
    }
    
    /**
     * 删除联系人
     * 支持通过主键ID或联系人用户ID删除
     * 注意：使用物理删除而不是逻辑删除，避免唯一索引冲突
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteContact(Long id) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        // 1. 尝试将参数作为联系人ID（对方的用户ID）进行物理删除
        int deleted = contactMapper.physicalDeleteByUserAndContact(userId, id);
        if (deleted > 0) {
            log.debug("物理删除联系人成功：userId={}, contactId={}", userId, id);
            return Result.success();
        }
        
        // 2. 尝试将参数作为主键ID查找并删除
        Contact contactById = contactMapper.selectById(id);
        if (contactById != null && contactById.getUserId().equals(userId)) {
            contactMapper.physicalDeleteById(id);
            log.debug("物理删除联系人成功：id={}", id);
            return Result.success();
        }
        
        // 联系人不存在，可能已经被删除，返回成功（幂等性）
        log.debug("联系人不存在或已删除：userId={}, id={}", userId, id);
        return Result.success();
    }
    
    /**
     * 搜索联系人（搜索学生和管理员）
     * 对于管理员用户，只搜索其管理的楼栋中的学生
     */
    public Result<List<ContactVO>> searchContacts(String keyword) {
        Long userId = getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        
        List<ContactVO> result = new ArrayList<>();
        String searchKeyword = keyword.trim();
        
        // 检查当前用户是否是管理员
        SysUser currentUser = sysUserMapper.selectById(userId);
        boolean isAdmin = currentUser != null && 
                         ("admin".equals(currentUser.getUserType()) || 
                          "superadmin".equals(currentUser.getUserType()));
        
        Set<Long> allowedStudentIds = new HashSet<>();
        
        // 如果是管理员，需要过滤出管理的楼栋中的学生
        // 逻辑与StudentService保持一致：包括符合专业分配的学生和已分配到宿舍的学生
        if (isAdmin && "admin".equals(currentUser.getUserType())) {
            // 1. 获取管理员管理的楼栋列表
            LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
            buildingWrapper.eq(Building::getManagerName, currentUser.getUsername());
            List<Building> buildings = buildingMapper.selectList(buildingWrapper);
            
            log.info("管理员{}管理的楼栋数量：{}", currentUser.getUsername(), buildings.size());
            
            if (!buildings.isEmpty()) {
                List<Long> buildingIds = buildings.stream()
                    .map(Building::getId)
                    .collect(Collectors.toList());
                
                log.info("管理员{}管理的楼栋ID：{}", currentUser.getUsername(), buildingIds);
                
                // 2. 查询这些楼栋对应的专业/班级分配
                LambdaQueryWrapper<BuildingMajorAssignment> majorWrapper = new LambdaQueryWrapper<>();
                majorWrapper.in(BuildingMajorAssignment::getBuildingId, buildingIds)
                           .eq(BuildingMajorAssignment::getStatus, 1);
                List<BuildingMajorAssignment> majorAssignments = buildingMajorAssignmentMapper.selectList(majorWrapper);
                
                // 3a. 根据专业/班级/年级/性别过滤学生（与StudentService逻辑一致）
                if (!majorAssignments.isEmpty()) {
                    for (BuildingMajorAssignment assignment : majorAssignments) {
                        LambdaQueryWrapper<Student> studentQuery = new LambdaQueryWrapper<>();
                        studentQuery.eq(Student::getDeleted, 0)
                                   .isNotNull(Student::getUserId); // 只搜索已注册的学生
                        
                        if (assignment.getMajorName() != null) {
                            studentQuery.eq(Student::getMajor, assignment.getMajorName());
                        }
                        if (assignment.getClassName() != null) {
                            studentQuery.eq(Student::getClassName, assignment.getClassName());
                        }
                        if (assignment.getGrade() != null) {
                            studentQuery.eq(Student::getGrade, assignment.getGrade());
                        }
                        // 处理性别字段（兼容英文和中文）
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
                            studentQuery.eq(Student::getGender, studentGender);
                        }
                        
                        List<Student> students = studentMapper.selectList(studentQuery);
                        allowedStudentIds.addAll(students.stream()
                            .map(Student::getId)
                            .collect(Collectors.toSet()));
                    }
                    log.info("管理员{}管理的楼栋中符合专业分配的学生数量：{}", currentUser.getUsername(), allowedStudentIds.size());
                }
                
                // 3b. 查询已分配到这些楼栋宿舍的学生
                LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
                roomWrapper.in(DormRoom::getBuildingId, buildingIds);
                List<DormRoom> rooms = dormRoomMapper.selectList(roomWrapper);
                List<Long> roomIds = rooms.stream()
                    .map(DormRoom::getId)
                    .collect(Collectors.toList());
                
                log.info("管理员{}管理的楼栋中的房间数量：{}", currentUser.getUsername(), roomIds.size());
                
                if (!roomIds.isEmpty()) {
                    // 从dorm_group表查询已选择这些房间的宿舍组，提取成员ID
                    LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
                    groupWrapper.in(DormGroup::getSelectedRoomId, roomIds);
                    List<DormGroup> groups = dormGroupMapper.selectList(groupWrapper);
                    Set<Long> allocatedStudentIds = new HashSet<>();
                    for (DormGroup group : groups) {
                        List<Long> memberIds = extractMemberIds(group);
                        allocatedStudentIds.addAll(memberIds);
                    }
                    
                    allowedStudentIds.addAll(allocatedStudentIds);
                    log.info("管理员{}管理的楼栋中已分配宿舍的学生数量：{}", currentUser.getUsername(), allocatedStudentIds.size());
                }
                
                log.info("管理员{}可搜索的学生ID列表总大小：{}", currentUser.getUsername(), allowedStudentIds.size());
            } else {
                log.warn("管理员{}没有管理的楼栋", currentUser.getUsername());
            }
        }
        
        // 搜索学生（通过学号或姓名）
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.and(w -> w.like(Student::getRealName, searchKeyword)
                        .or()
                        .like(Student::getStudentId, searchKeyword));
        studentWrapper.eq(Student::getDeleted, 0);
        studentWrapper.isNotNull(Student::getUserId); // 只搜索已注册的学生
        
        // 如果是管理员，添加楼栋过滤（只搜索已分配到该楼栋的学生）
        if (isAdmin && "admin".equals(currentUser.getUserType())) {
            // 如果管理的楼栋中有学生，添加过滤条件
            if (!allowedStudentIds.isEmpty()) {
                studentWrapper.in(Student::getId, allowedStudentIds);
            } else {
                // 如果管理的楼栋中没有学生，返回空结果（不添加过滤条件会导致查询所有学生）
                // 注意：这里返回空结果，因为管理员只能搜索其管理的楼栋中的学生
                // 如果allowedStudentIds为空，说明该管理员管理的楼栋中确实没有学生
                return Result.success(result);
            }
        }
        
        List<Student> students = studentMapper.selectList(studentWrapper);
        for (Student student : students) {
            if (student.getUserId() != null && !student.getUserId().equals(userId)) {
                // 如果是管理员，再次验证学生是否在管理的楼栋中
                if (isAdmin && "admin".equals(currentUser.getUserType())) {
                    if (!allowedStudentIds.contains(student.getId())) {
                        continue; // 跳过不在管理楼栋中的学生
                    }
                }
                
                SysUser user = sysUserMapper.selectById(student.getUserId());
                if (user != null) {
                    ContactVO vo = new ContactVO();
                    vo.setUserId(student.getUserId());
                    vo.setUserName(student.getRealName());
                    vo.setUserType("student");
                    vo.setStudentId(student.getStudentId()); // 添加学号信息
                    vo.setAvatar(user.getAvatar());
                    result.add(vo);
                }
            }
        }
        
        // 搜索管理员（通过用户名或真实姓名）- 所有管理员都可以搜索其他管理员
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.and(w -> w.like(SysUser::getRealName, searchKeyword)
                        .or()
                        .like(SysUser::getUsername, searchKeyword));
        userWrapper.in(SysUser::getUserType, Arrays.asList("admin", "superadmin"));
        userWrapper.eq(SysUser::getStatus, 1); // 只搜索启用的管理员
        
        List<SysUser> adminUsers = sysUserMapper.selectList(userWrapper);
        for (SysUser user : adminUsers) {
            if (!user.getId().equals(userId)) {
                ContactVO vo = new ContactVO();
                vo.setUserId(user.getId());
                vo.setUserName(user.getRealName());
                vo.setUserType(user.getUserType());
                vo.setAvatar(user.getAvatar());
                result.add(vo);
            }
        }
        
        return Result.success(result);
    }
    
    /**
     * 通过用户ID精确获取用户信息（用于聊天跳转时精确查找用户）
     */
    public Result<ContactVO> getUserById(Long targetUserId) {
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        if (targetUserId == null) {
            return Result.error("用户ID不能为空");
        }
        
        // 先查询SysUser表
        SysUser user = sysUserMapper.selectById(targetUserId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        ContactVO vo = new ContactVO();
        vo.setUserId(user.getId());
        vo.setAvatar(user.getAvatar());
        
        // 根据用户类型获取详细信息
        if ("student".equals(user.getUserType())) {
            // 查询学生信息
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getUserId, targetUserId);
            Student student = studentMapper.selectOne(wrapper);
            if (student != null) {
                vo.setUserName(student.getRealName());
                vo.setStudentId(student.getStudentId());
                vo.setUserType("student");
            } else {
                vo.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                vo.setUserType("student");
            }
        } else {
            // 管理员
            vo.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            vo.setUserType(user.getUserType());
        }
        
        return Result.success(vo);
    }
    
    /**
     * 获取用户详细资料（用于查看资料弹窗）
     * 支持学生和管理员，返回不同的字段
     */
    public Result<Map<String, Object>> getUserProfile(Long targetUserId) {
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        if (targetUserId == null) {
            return Result.error("用户ID不能为空");
        }
        
        // 先查询SysUser表
        SysUser user = sysUserMapper.selectById(targetUserId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("avatar", user.getAvatar());
        profile.put("userType", user.getUserType());
        
        // 根据用户类型获取详细信息
        if ("student".equals(user.getUserType())) {
            // 查询学生信息
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getUserId, targetUserId);
            Student student = studentMapper.selectOne(wrapper);
            
            if (student != null) {
                profile.put("name", student.getRealName());
                profile.put("studentId", student.getStudentId());
                profile.put("email", user.getEmail()); // 邮箱从SysUser获取
                profile.put("bio", student.getBio());
                profile.put("tags", student.getTags());
                profile.put("bedTime", student.getBedTime());
                profile.put("wakeTime", student.getWakeTime());
                
                // 从dorm_group获取宿舍信息
                String dormInfo = null;
                if (student.getId() != null) {
                    Long roomId = getStudentRoomIdFromDormGroup(student.getId());
                    if (roomId != null) {
                        DormRoom room = dormRoomMapper.selectById(roomId);
                        if (room != null) {
                            Building building = buildingMapper.selectById(room.getBuildingId());
                            if (building != null) {
                                dormInfo = building.getBuildingName() + " " + room.getRoomNumber();
                            }
                        }
                    }
                }
                profile.put("dormInfo", dormInfo);
            } else {
                profile.put("name", user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        } else {
            // 管理员
            profile.put("name", user.getRealName() != null ? user.getRealName() : user.getUsername());
            profile.put("phone", user.getPhone());
            profile.put("email", user.getEmail());
            
            // 从Admin表获取管理员详细信息
            LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
            adminWrapper.eq(Admin::getUserId, targetUserId);
            Admin admin = adminMapper.selectOne(adminWrapper);
            
            if (admin != null) {
                // 使用Admin表中的数据
                profile.put("officeLocation", admin.getOfficeLocation());
                profile.put("department", admin.getDepartment());
                profile.put("bio", admin.getBio());
                
                // 解析工作时间（格式：08:30-17:30）
                String workHours = admin.getWorkHours();
                if (workHours != null && workHours.contains("-")) {
                    String[] times = workHours.split("-");
                    if (times.length >= 2) {
                        profile.put("workStartTime", times[0].trim());
                        profile.put("workEndTime", times[1].trim());
                    } else {
                        profile.put("workStartTime", null);
                        profile.put("workEndTime", null);
                    }
                } else {
                    profile.put("workStartTime", null);
                    profile.put("workEndTime", null);
                }
            } else {
                // Admin表中没有记录，返回null
                profile.put("officeLocation", null);
                profile.put("department", null);
                profile.put("bio", null);
                profile.put("workStartTime", null);
                profile.put("workEndTime", null);
            }
        }
        
        return Result.success(profile);
    }
    
    /**
     * 检查用户是否已完成宿舍分配（已选房或已分配）
     * @param userId 系统用户ID（sys_user表的id）
     */
    private boolean isUserAllocated(Long userId) {
        log.info("开始检查用户分配状态：userId={}", userId);
        
        // 1. 根据userId获取studentId
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(studentWrapper);
        
        if (student == null) {
            log.info("用户{}不是学生，跳过检查", userId);
            return false;
        }
        
        Long studentId = student.getId();
        log.info("用户{}对应的学生ID={}", userId, studentId);
        
        // 2. 检查学生是否是已完成分配的宿舍组的组长
        LambdaQueryWrapper<DormGroup> leaderWrapper = new LambdaQueryWrapper<>();
        leaderWrapper.eq(DormGroup::getLeaderId, studentId)
                    .in(DormGroup::getStatus, "room_selected", "allocated");
        Long leaderCount = dormGroupMapper.selectCount(leaderWrapper);
        log.info("学生{}作为组长的已分配宿舍组数量：{}", studentId, leaderCount);
        if (leaderCount > 0) {
            log.info("用户{}（学生ID:{}）是已完成分配的宿舍组组长，返回true", userId, studentId);
            return true;
        }
        
        // 3. 检查学生是否在已完成分配的宿舍组的成员列表中
        LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(DormGroup::getStatus, "room_selected", "allocated")
                   .isNotNull(DormGroup::getMembers);
        List<DormGroup> groups = dormGroupMapper.selectList(groupWrapper);
        log.info("已完成分配的宿舍组总数：{}", groups.size());
        
        for (DormGroup group : groups) {
            try {
                String membersJson = group.getMembers();
                log.info("检查宿舍组{}的成员列表：{}", group.getId(), membersJson);
                if (membersJson != null && !membersJson.trim().isEmpty()) {
                    // 简单的JSON字符串匹配
                    if (membersJson.contains("\"id\":" + studentId) || 
                        membersJson.contains("\"id\": " + studentId)) {
                        log.info("用户{}（学生ID:{}）在已完成分配的宿舍组{}的成员列表中，返回true", userId, studentId, group.getId());
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("检查宿舍组{}的成员列表失败: {}", group.getId(), e.getMessage());
            }
        }
        
        log.info("用户{}（学生ID:{}）未在任何已分配的宿舍组中，返回false", userId, studentId);
        return false;
    }
    
    /**
     * 实时计算未读消息数（带Redis缓存）
     * 优先从Redis缓存获取，缓存未命中时从数据库计算
     * Redis不可用时自动降级到数据库计算
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 未读消息数量
     */
    private int calculateUnreadCount(Long conversationId, Long userId) {
        try {
            // 1. 尝试从Redis缓存获取
            Integer cachedCount = unreadCacheService.getUnreadCount(userId, conversationId);
            if (cachedCount != null) {
                return cachedCount;
            }
            
            // 2. 缓存未命中，从数据库计算
            int count = calculateUnreadCountFromDB(conversationId, userId);
            
            // 3. 写入缓存
            unreadCacheService.setUnreadCount(userId, conversationId, count);
            
            return count;
        } catch (Exception e) {
            // Redis不可用时，直接从数据库计算
            log.warn("Redis缓存不可用，降级到数据库计算: {}", e.getMessage());
            return calculateUnreadCountFromDB(conversationId, userId);
        }
    }
    
    /**
     * 从数据库实时计算未读消息数
     * 基于chat_message表直接COUNT计算，无需维护额外的未读消息表
     * 
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 未读消息数量
     */
    private int calculateUnreadCountFromDB(Long conversationId, Long userId) {
        // 获取会话信息，确定对方用户ID
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return 0;
        }
        
        // 确定对方用户ID（发送者）
        Long otherUserId = conversation.getUser1Id().equals(userId) ? 
                          conversation.getUser2Id() : conversation.getUser1Id();
        
        // 查询联系人记录，获取最后阅读时间
        LambdaQueryWrapper<Contact> contactWrapper = new LambdaQueryWrapper<>();
        contactWrapper.eq(Contact::getUserId, userId);
        contactWrapper.eq(Contact::getContactId, otherUserId);
        Contact contact = contactMapper.selectOne(contactWrapper);
        
        // 如果没有联系人记录，返回0（不应该出现这种情况）
        if (contact == null) {
            return 0;
        }
        
        // 统计最后阅读时间之后，对方发送的消息数量
        LambdaQueryWrapper<ChatMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(ChatMessage::getConversationId, conversationId);
        messageWrapper.eq(ChatMessage::getSenderId, otherUserId); // 只统计对方发送的消息
        
        // 如果有更新时间，则统计该时间之后的消息
        if (contact.getUpdatedAt() != null) {
            messageWrapper.gt(ChatMessage::getCreatedAt, contact.getUpdatedAt());
        }
        
        Long count = chatMessageMapper.selectCount(messageWrapper);
        return count != null ? count.intValue() : 0;
    }
    
    /**
     * 转换为会话VO
     */
    private ConversationVO convertToConversationVO(Conversation conv, Long currentUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conv.getId());
        
        // 确定对方用户ID
        Long otherUserId = conv.getUser1Id().equals(currentUserId) ? 
                          conv.getUser2Id() : conv.getUser1Id();
        vo.setUserId(otherUserId);
        
        // 获取对方用户信息
        SysUser otherUser = sysUserMapper.selectById(otherUserId);
        if (otherUser != null) {
            vo.setUserName(otherUser.getRealName());
            vo.setAvatar(otherUser.getAvatar());
            vo.setUserType(otherUser.getUserType());
            
            // 如果是学生，获取学生的宿舍信息
            if ("student".equals(otherUser.getUserType())) {
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getUserId, otherUserId);
                Student student = studentMapper.selectOne(studentWrapper);
                if (student != null) {
                    vo.setStudentId(student.getStudentId());
                    vo.setMajor(student.getMajor());
                    // 从dorm_group获取宿舍信息
                    Long roomId = getStudentRoomIdFromDormGroup(student.getId());
                    if (roomId != null) {
                        DormRoom room = dormRoomMapper.selectById(roomId);
                        if (room != null) {
                            Building building = buildingMapper.selectById(room.getBuildingId());
                            String buildingName = building != null ? building.getBuildingName() : "";
                            vo.setDormInfo(buildingName + "-" + room.getRoomNumber());
                        }
                    }
                }
            }
        }
        
        // 获取备注
        LambdaQueryWrapper<Contact> contactWrapper = new LambdaQueryWrapper<>();
        contactWrapper.eq(Contact::getUserId, currentUserId);
        contactWrapper.eq(Contact::getContactId, otherUserId);
        Contact contact = contactMapper.selectOne(contactWrapper);
        // 获取最后一条消息
        if (conv.getLastMessageId() != null) {
            ChatMessage lastMessage = chatMessageMapper.selectById(conv.getLastMessageId());
            if (lastMessage != null) {
                vo.setLastMessage(getMessageDisplayText(lastMessage));
                vo.setLastMessageTime(lastMessage.getCreatedAt());
            }
        }
        
        // 实时计算未读消息数
        int unreadCount = calculateUnreadCount(conv.getId(), currentUserId);
        vo.setUnreadCount(unreadCount);
        
        // 格式化时间
        if (conv.getLastMessageTime() != null) {
            vo.setTime(formatTime(conv.getLastMessageTime()));
        }
        
        return vo;
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
     * 转换为联系人VO
     */
    private ContactVO convertToContactVO(Contact contact) {
        ContactVO vo = new ContactVO();
        vo.setId(contact.getId());
        vo.setUserId(contact.getContactId());
        
        // 获取联系人用户信息
        SysUser user = sysUserMapper.selectById(contact.getContactId());
        if (user != null) {
            vo.setUserName(user.getRealName());
            vo.setAvatar(user.getAvatar());
            vo.setUserType(user.getUserType());
        }
        
        vo.setDisplayName(vo.getUserName());
        vo.setCreatedAt(contact.getCreatedAt());
        
        return vo;
    }
    
    /**
     * 获取消息显示文本
     */
    private String getMessageDisplayText(ChatMessage message) {
        if (message.getIsRecalled()) {
            return "[消息已撤回]";
        }
        return message.getContent() != null ? message.getContent() : "";
    }
    
    /**
     * 格式化时间
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else if (seconds < 604800) {
            return (seconds / 86400) + "天前";
        } else {
            return dateTime.toLocalDate().toString();
        }
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
                List<Long> memberIds = extractMemberIds(group);
                if (memberIds.contains(studentId)) {
                    return group.getSelectedRoomId();
                }
            }
        } catch (Exception e) {
            log.error("获取学生房间ID失败：studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 从宿舍组中提取成员ID列表
     */
    private List<Long> extractMemberIds(DormGroup group) {
        List<Long> memberIds = new ArrayList<>();
        if (group.getMembers() == null || group.getMembers().isEmpty()) {
            return memberIds;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
            );
            for (Map<String, Object> member : members) {
                Object idObj = member.get("id");
                if (idObj instanceof Number) {
                    memberIds.add(((Number) idObj).longValue());
                }
            }
        } catch (Exception e) {
            log.warn("解析宿舍组成员失败：groupId={}", group.getId());
        }
        return memberIds;
    }
}

