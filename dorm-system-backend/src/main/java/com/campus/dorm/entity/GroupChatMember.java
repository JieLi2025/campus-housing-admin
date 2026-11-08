package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群聊成员实体类
 * 对应数据库表：group_chat_member
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("group_chat_member")
public class GroupChatMember {
    
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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 角色：member-普通成员，admin-管理员
     */
    @TableField("role")
    private String role;
    
    /**
     * 是否活跃：0-已离开，1-活跃
     */
    @TableField("is_active")
    private Boolean isActive;
    
    /**
     * 是否被禁言：0-未禁言，1-已禁言
     */
    @TableField("is_muted")
    private Boolean isMuted;
}

