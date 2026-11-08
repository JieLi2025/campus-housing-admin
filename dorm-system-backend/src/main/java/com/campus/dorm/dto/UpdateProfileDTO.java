package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;

/**
 * 更新个人信息DTO
 */
@Data
public class UpdateProfileDTO {
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 个人标签（JSON数组字符串）
     */
    private String tags;
    
    /**
     * 就寝时间（格式：HH:mm）
     */
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "就寝时间格式不正确，应为HH:mm")
    private String bedTime;
    
    /**
     * 起床时间（格式：HH:mm）
     */
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "起床时间格式不正确，应为HH:mm")
    private String wakeTime;
    
    // ========== 管理员特有字段 ==========
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 职位
     */
    private String position;
    
    /**
     * 办公地点
     */
    private String officeLocation;
    
    /**
     * 工作时间
     */
    private String workHours;

    /**
     * 邀请码（用于修改敏感信息如邮箱时的验证）
     */
    private String invitationCode;
}

