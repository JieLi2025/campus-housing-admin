package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.*;
import com.campus.dorm.service.ChatService;
import com.campus.dorm.service.FileUploadService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.ChatMessageVO;
import com.campus.dorm.vo.ContactVO;
import com.campus.dorm.vo.ConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天控制器
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "聊天功能")
public class ChatController {
    
    private final ChatService chatService;
    
    private final FileUploadService fileUploadService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public Result<List<ConversationVO>> getConversations() {
        return chatService.getConversations();
    }
    
    /**
     * 获取消息历史
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "获取消息历史")
    public Result<List<ChatMessageVO>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return chatService.getMessages(conversationId, pageNum, pageSize);
    }
    
    /**
     * 创建会话
     */
    @PostMapping("/conversations")
    @Operation(summary = "创建会话")
    public Result<Long> createConversation(@Validated @RequestBody CreateConversationDTO dto) {
        return chatService.createConversation(dto);
    }
    
    /**
     * 发送消息
     */
    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    public Result<ChatMessageVO> sendMessage(@Validated @RequestBody SendMessageDTO dto) {
        return chatService.sendMessage(dto);
    }
    
    /**
     * 撤回消息
     */
    @PutMapping("/messages/{messageId}/recall")
    @Operation(summary = "撤回消息")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        return chatService.recallMessage(messageId);
    }
    
    /**
     * 更新消息类型（用于更新邀请状态等）
     */
    @PutMapping("/messages/{messageId}/type")
    @Operation(summary = "更新消息类型")
    public Result<Void> updateMessageType(
            @PathVariable Long messageId,
            @RequestParam String newType) {
        return chatService.updateMessageType(messageId, newType);
    }
    
    /**
     * 删除消息
     */
    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "删除消息")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        return chatService.deleteMessage(messageId);
    }
    
    /**
     * 标记会话为已读
     */
    @PutMapping("/conversations/{conversationId}/read")
    @Operation(summary = "标记会话为已读")
    public Result<Void> markAsRead(@PathVariable Long conversationId) {
        return chatService.markAsRead(conversationId);
    }
    
    /**
     * 获取联系人列表
     */
    @GetMapping("/contacts")
    @Operation(summary = "获取联系人列表")
    public Result<List<ContactVO>> getContacts() {
        return chatService.getContacts();
    }
    
    /**
     * 添加联系人
     */
    @PostMapping("/contacts")
    @Operation(summary = "添加联系人")
    public Result<Void> addContact(@Validated @RequestBody AddContactDTO dto) {
        return chatService.addContact(dto);
    }
    
    /**
     * 更新联系人备注
     */
    /**
     * 删除联系人
     */
    @DeleteMapping("/contacts/{contactId}")
    @Operation(summary = "删除联系人")
    public Result<Void> deleteContact(@PathVariable Long contactId) {
        return chatService.deleteContact(contactId);
    }
    
    /**
     * 搜索联系人
     */
    @GetMapping("/search")
    @Operation(summary = "搜索联系人")
    public Result<List<ContactVO>> searchContacts(@RequestParam String keyword) {
        return chatService.searchContacts(keyword);
    }
    
    /**
     * 通过用户ID获取用户信息（用于聊天跳转时精确查找用户）
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "通过用户ID获取用户信息")
    public Result<ContactVO> getUserById(@PathVariable Long userId) {
        return chatService.getUserById(userId);
    }
    
    /**
     * 获取用户详细资料（用于查看资料弹窗）
     * 支持学生和管理员，返回不同的字段
     */
    @GetMapping("/user/{userId}/profile")
    @Operation(summary = "获取用户详细资料")
    public Result<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        return chatService.getUserProfile(userId);
    }
    
    /**
     * 上传聊天文件（图片或文件）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传聊天文件")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileUploadService.ChatFileUploadResult result = fileUploadService.uploadChatFile(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("fileUrl", result.getFileUrl());
            response.put("fileName", result.getFileName());
            response.put("fileSize", result.getFileSize());
            response.put("messageType", result.getMessageType());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    // ========== 楼栋群聊API ==========
    
    @Resource
    private com.campus.dorm.service.BuildingGroupChatService buildingGroupChatService;
    
    /**
     * 获取楼栋群聊会话
     */
    @GetMapping("/building/{buildingId}/group")
    @Operation(summary = "获取楼栋群聊会话")
    public Result<Long> getBuildingGroupChat(@PathVariable Long buildingId) {
        return buildingGroupChatService.getOrCreateBuildingGroupChat(buildingId);
    }
    
    /**
     * 发送楼栋群聊消息
     */
    @PostMapping("/building/{buildingId}/group/messages")
    @Operation(summary = "发送楼栋群聊消息")
    public Result<ChatMessageVO> sendBuildingGroupMessage(
            @PathVariable Long buildingId,
            @Validated @RequestBody SendMessageDTO dto) {
        return buildingGroupChatService.sendGroupMessage(buildingId, dto);
    }
    
    /**
     * 获取楼栋群聊消息历史
     */
    @GetMapping("/building/{buildingId}/group/messages")
    @Operation(summary = "获取楼栋群聊消息历史")
    public Result<List<ChatMessageVO>> getBuildingGroupMessages(
            @PathVariable Long buildingId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return buildingGroupChatService.getGroupMessages(buildingId, pageNum, pageSize);
    }
    
    /**
     * 获取楼栋群聊成员列表
     */
    @GetMapping("/building/{buildingId}/group/members")
    @Operation(summary = "获取楼栋群聊成员列表")
    public Result<List<Map<String, Object>>> getBuildingGroupMembers(@PathVariable Long buildingId) {
        return buildingGroupChatService.getGroupMembers(buildingId);
    }
    
    /**
     * 获取楼栋学生统计信息
     */
    @GetMapping("/building/{buildingId}/group/stats")
    @Operation(summary = "获取楼栋学生统计信息")
    public Result<Map<String, Object>> getBuildingStudentStats(@PathVariable Long buildingId) {
        return buildingGroupChatService.getBuildingStudentStats(buildingId);
    }
    
    /**
     * 禁言/解除禁言用户
     */
    @PutMapping("/building/{buildingId}/group/mute/{userId}")
    @Operation(summary = "禁言/解除禁言用户")
    public Result<Void> toggleMuteUser(
            @PathVariable Long buildingId,
            @PathVariable Long userId,
            @RequestParam Boolean mute) {
        return buildingGroupChatService.toggleMuteUser(buildingId, userId, mute);
    }
    
    /**
     * 切换全体禁言状态
     */
    @PutMapping("/building/{buildingId}/group/mute-all")
    @Operation(summary = "切换全体禁言状态")
    public Result<Boolean> toggleMuteAll(
            @PathVariable Long buildingId,
            @RequestParam Boolean muteAll) {
        return buildingGroupChatService.toggleMuteAll(buildingId, muteAll);
    }
    
    /**
     * 获取全体禁言状态
     */
    @GetMapping("/building/{buildingId}/group/mute-all")
    @Operation(summary = "获取全体禁言状态")
    public Result<Boolean> getMuteAllStatus(@PathVariable Long buildingId) {
        return buildingGroupChatService.getMuteAllStatus(buildingId);
    }
    
    // ========== 宿舍群聊API ==========
    
    @Resource
    private com.campus.dorm.service.DormGroupChatService dormGroupChatService;
    
    /**
     * 获取宿舍群聊会话
     */
    @GetMapping("/dorm-group/{dormGroupId}/group")
    @Operation(summary = "获取宿舍群聊会话")
    public Result<Long> getDormGroupChat(@PathVariable Long dormGroupId) {
        return dormGroupChatService.getOrCreateDormGroupChat(dormGroupId);
    }
    
    /**
     * 发送宿舍群聊消息
     */
    @PostMapping("/dorm-group/{dormGroupId}/group/messages")
    @Operation(summary = "发送宿舍群聊消息")
    public Result<ChatMessageVO> sendDormGroupMessage(
            @PathVariable Long dormGroupId,
            @Validated @RequestBody SendMessageDTO dto) {
        return dormGroupChatService.sendGroupMessage(dormGroupId, dto);
    }
    
    /**
     * 获取宿舍群聊消息历史
     */
    @GetMapping("/dorm-group/{dormGroupId}/group/messages")
    @Operation(summary = "获取宿舍群聊消息历史")
    public Result<List<ChatMessageVO>> getDormGroupMessages(
            @PathVariable Long dormGroupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return dormGroupChatService.getGroupMessages(dormGroupId, pageNum, pageSize);
    }
    
    /**
     * 获取宿舍群聊成员列表
     */
    @GetMapping("/dorm-group/{dormGroupId}/group/members")
    @Operation(summary = "获取宿舍群聊成员列表")
    public Result<List<Map<String, Object>>> getDormGroupMembers(@PathVariable Long dormGroupId) {
        return dormGroupChatService.getGroupMembers(dormGroupId);
    }
}

