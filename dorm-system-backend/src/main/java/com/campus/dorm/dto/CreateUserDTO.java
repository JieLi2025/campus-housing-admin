package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建用户请求DTO（管理员使用）
 * 用于超级管理员创建新的管理员或超级管理员
 * 
 * @author Campus Dorm System
 * @date 2024-10-22
 */
@Data
public class CreateUserDTO {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度应为4-20个字符")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度应为8-20个字符")
    private String password;
    
    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度应为2-50个字符")
    private String realName;
    
    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号（可选）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确", groups = {})
    private String phone;
    
    /**
     * 用户类型：admin（管理员）或 superadmin（超级管理员）
     */
    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "^(admin|superadmin)$", message = "用户类型只能是admin或superadmin")
    private String userType;
}

