package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 登录请求DTO
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Data
public class LoginDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户名/学号
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 用户类型：student/admin
     */
    @NotBlank(message = "用户类型不能为空")
    private String userType;
}

