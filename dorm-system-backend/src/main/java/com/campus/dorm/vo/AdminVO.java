package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员VO
 * 
 * @author Campus Dorm System
 * @date 2024-10-24
 */
@Data
public class AdminVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 用户类型：admin/superadmin
     */
    private String userType;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 状态文本：active/inactive
     */
    private String statusText;
    
    /**
     * 管理楼栋名称
     */
    private String managedBuilding;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

