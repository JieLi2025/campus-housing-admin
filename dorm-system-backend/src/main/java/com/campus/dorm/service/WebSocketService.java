package com.campus.dorm.service;

import com.campus.dorm.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * WebSocket消息推送服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Slf4j
@Service
public class WebSocketService {
    
    @Resource
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 发送新消息通知给接收者
     */
    public void sendNewMessage(Long receiverId, ChatMessageVO message) {
        try {
            // 创建一个包含类型信息的消息对象
            java.util.Map<String, Object> messageWrapper = new java.util.HashMap<>();
            messageWrapper.put("type", "NEW_MESSAGE");
            messageWrapper.put("data", message);
            
            String destination = "/user/" + receiverId.toString() + "/queue/messages";
            log.info("准备推送消息到: {}, 消息内容: {}", destination, messageWrapper);
            log.info("消息序列化测试 - receiverId: {}, messageId: {}, content: {}", 
                    receiverId, message.getId(), message.getContent());
            
            // 使用convertAndSendToUser推送消息
            // 第一个参数是Principal的name（字符串形式的userId）
            // 第二个参数是目标路径（不包含/user前缀）
            messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                messageWrapper
            );
            log.info("WebSocket推送新消息给用户: {}, 目标路径: {}, Principal name应该是: {}", 
                    receiverId, destination, receiverId.toString());
        } catch (Exception e) {
            log.error("WebSocket推送消息失败，用户ID: {}", receiverId, e);
        }
    }
    
    /**
     * 发送消息撤回通知
     */
    public void sendMessageRecalled(Long receiverId, Long messageId, Long conversationId) {
        try {
            messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                new MessageRecalledEvent(messageId, conversationId)
            );
            log.info("WebSocket推送消息撤回通知给用户: {}", receiverId);
        } catch (Exception e) {
            log.error("WebSocket推送消息撤回通知失败", e);
        }
    }
    
    /**
     * 发送会话更新通知（新消息、已读等）
     */
    public void sendConversationUpdated(Long userId, Long conversationId) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/conversations",
                new ConversationUpdatedEvent(conversationId)
            );
            log.info("WebSocket推送会话更新通知给用户: {}", userId);
        } catch (Exception e) {
            log.error("WebSocket推送会话更新通知失败", e);
        }
    }
    
    /**
     * 消息撤回事件
     */
    public static class MessageRecalledEvent {
        private String type = "MESSAGE_RECALLED";
        private Long messageId;
        private Long conversationId;
        
        public MessageRecalledEvent(Long messageId, Long conversationId) {
            this.messageId = messageId;
            this.conversationId = conversationId;
        }
        
        public String getType() {
            return type;
        }
        
        public Long getMessageId() {
            return messageId;
        }
        
        public Long getConversationId() {
            return conversationId;
        }
    }
    
    /**
     * 会话更新事件
     */
    public static class ConversationUpdatedEvent {
        private String type = "CONVERSATION_UPDATED";
        private Long conversationId;
        
        public ConversationUpdatedEvent(Long conversationId) {
            this.conversationId = conversationId;
        }
        
        public String getType() {
            return type;
        }
        
        public Long getConversationId() {
            return conversationId;
        }
    }
    
    /**
     * 发送群聊消息（楼栋群聊）
     */
    public void sendGroupMessage(Long buildingId, ChatMessageVO message) {
        try {
            java.util.Map<String, Object> messageWrapper = new java.util.HashMap<>();
            messageWrapper.put("type", "GROUP_MESSAGE");
            messageWrapper.put("data", message);
            
            messagingTemplate.convertAndSend(
                "/topic/building/" + buildingId + "/group",
                messageWrapper
            );
            log.info("WebSocket推送群聊消息到楼栋: {}", buildingId);
        } catch (Exception e) {
            log.error("WebSocket推送群聊消息失败", e);
        }
    }
    
    /**
     * 发送群聊消息（宿舍群聊）
     */
    public void sendDormGroupMessage(Long dormGroupId, ChatMessageVO message) {
        try {
            java.util.Map<String, Object> messageWrapper = new java.util.HashMap<>();
            messageWrapper.put("type", "GROUP_MESSAGE");
            messageWrapper.put("data", message);
            
            messagingTemplate.convertAndSend(
                "/topic/dorm-group/" + dormGroupId + "/group",
                messageWrapper
            );
            log.info("WebSocket推送群聊消息到宿舍队伍: {}", dormGroupId);
        } catch (Exception e) {
            log.error("WebSocket推送群聊消息失败", e);
        }
    }
    
    /**
     * 发送团队更新通知（成员被移除、舍长转让等）
     */
    public void sendTeamUpdate(Long userId, String action, String message) {
        try {
            java.util.Map<String, Object> notification = new java.util.HashMap<>();
            notification.put("type", "TEAM_UPDATE");
            notification.put("action", action);  // REMOVED, LEADER_CHANGED, etc.
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/team-updates",
                notification
            );
            log.info("WebSocket推送团队更新通知给用户: {}, action: {}", userId, action);
        } catch (Exception e) {
            log.error("WebSocket推送团队更新通知失败", e);
        }
    }
}

