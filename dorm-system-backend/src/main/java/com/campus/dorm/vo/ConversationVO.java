package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class ConversationVO {
    
    /**
     * 会话ID
     */
    private Long id;
    
    /**
     * 对方用户ID
     */
    private Long userId;
    
    /**
     * 对方用户名称
     */
    private String userName;
    
    /**
     * 对方用户头像
     */
    private String avatar;
    
    /**
     * 对方用户类型：student/admin
     */
    private String userType;
    
    /**
     * 备注名称
     */
    private String remark;
    
    /**
     * 最后一条消息内容
     */
    private String lastMessage;
    
    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 未读消息数量
     */
    private Integer unreadCount;
    
    /**
     * 格式化后的时间显示
     */
    private String time;
    
    /**
     * 学生宿舍信息（仅学生类型有效）
     */
    private String dormInfo;
    
    /**
     * 学生学号（仅学生类型有效）
     */
    private String studentId;
    
    /**
     * 学生专业（仅学生类型有效）
     */
    private String major;
}

