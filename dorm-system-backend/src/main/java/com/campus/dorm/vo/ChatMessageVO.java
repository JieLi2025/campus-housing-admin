package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class ChatMessageVO {
    
    /**
     * 消息ID
     */
    private Long id;
    
    /**
     * 会话ID
     */
    private Long conversationId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者名称
     */
    private String senderName;
    
    /**
     * 发送者头像
     */
    private String senderAvatar;
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：text/image/file/invite/dorm_invite
     */
    private String messageType;
    
    /**
     * 文件URL
     */
    private String fileUrl;
    
    /**
     * 文件名
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
     * 引用的消息内容
     */
    private ChatMessageVO quotedMessage;
    
    /**
     * 是否已撤回
     */
    private Boolean isRecalled;
    
    /**
     * 是否匿名发送
     */
    private Boolean isAnonymous;
    
    /**
     * 是否是自己发送的消息
     */
    private Boolean isOwn;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 格式化后的时间显示
     */
    private String time;
    
    /**
     * 用户类型：student/admin/superadmin
     */
    private String userType;
    
    /**
     * 房间号（学生用户显示）
     */
    private String room;
}

