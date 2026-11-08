package com.campus.dorm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录返回VO
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * 用户ID
     */
    private Long userId;
    
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
     * 用户类型：student/admin/superadmin
     */
    private String userType;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 学号（学生用户）
     */
    private String studentId;
    
    /**
     * 专业（学生用户）
     */
    private String major;
    
    /**
     * 班级（学生用户）
     */
    private String className;
    
    /**
     * 性别（学生用户）：male/female
     */
    private String gender;
    
    /**
     * 年级（学生用户）
     */
    private String grade;
}

