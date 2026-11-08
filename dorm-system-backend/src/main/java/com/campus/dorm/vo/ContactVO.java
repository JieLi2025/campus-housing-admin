package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class ContactVO {
    
    /**
     * 联系人ID
     */
    private Long id;
    
    /**
     * 联系人用户ID
     */
    private Long userId;
    
    /**
     * 联系人名称
     */
    private String userName;
    
    /**
     * 联系人头像
     */
    private String avatar;
    
    /**
     * 联系人类型：student/admin
     */
    private String userType;
    
    /**
     * 学生学号（仅当userType为student时有效）
     */
    private String studentId;
    
    /**
     * 备注名称
     */
    private String remark;
    
    /**
     * 显示名称（优先显示备注，没有则显示用户名）
     */
    private String displayName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

