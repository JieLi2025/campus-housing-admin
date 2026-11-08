package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 发送消息DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class SendMessageDTO {
    
    /**
     * 会话ID（如果已有会话）
     */
    private Long conversationId;
    
    /**
     * 接收者ID（创建新会话时需要）
     */
    private Long receiverId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：text/image/file/invite/dorm_invite
     */
    @NotNull(message = "消息类型不能为空")
    private String messageType;
    
    /**
     * 文件URL（图片/文件消息时使用）
     */
    private String fileUrl;
    
    /**
     * 文件名（图片/文件消息时使用）
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 引用的消息ID
     */
    private Long quotedMessageId;
    
    /**
     * 是否匿名发送
     */
    private Boolean isAnonymous;
}

