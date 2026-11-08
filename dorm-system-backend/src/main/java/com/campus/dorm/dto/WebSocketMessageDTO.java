package com.campus.dorm.dto;

import lombok.Data;

/**
 * WebSocket消息DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class WebSocketMessageDTO {
    
    /**
     * 消息类型：NEW_MESSAGE, MESSAGE_RECALLED, CONVERSATION_UPDATED, TYPING
     */
    private String type;
    
    /**
     * 消息数据（JSON字符串或对象）
     */
    private Object data;
    
    /**
     * 目标用户ID（用于点对点推送）
     */
    private Long targetUserId;
    
    /**
     * 会话ID
     */
    private Long conversationId;
}

