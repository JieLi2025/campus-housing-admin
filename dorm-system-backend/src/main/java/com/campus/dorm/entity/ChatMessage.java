package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 对应数据库表：chat_message
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("chat_message")
public class ChatMessage {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;
    
    /**
     * 发送者ID
     */
    @TableField("sender_id")
    private Long senderId;
    
    /**
     * 接收者ID
     */
    @TableField("receiver_id")
    private Long receiverId;
    
    /**
     * 消息内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 文件URL
     */
    @TableField("file_url")
    private String fileUrl;
    
    /**
     * 引用的消息ID
     */
    @TableField("quoted_message_id")
    private Long quotedMessageId;
    
    /**
     * 是否已撤回：0-未撤回，1-已撤回
     */
    @TableField("is_recalled")
    private Boolean isRecalled;
    
    /**
     * 是否匿名发送：0-否，1-是
     */
    @TableField("is_anonymous")
    private Boolean isAnonymous;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}

