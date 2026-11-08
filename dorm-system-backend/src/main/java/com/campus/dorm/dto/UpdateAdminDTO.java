package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 更新管理员DTO
 * 
 * @author Campus Dorm System
 * @date 2024-10-26
 */
@Data
public class UpdateAdminDTO {
    
    /**
     * 管理员ID
     */
    @NotNull(message = "管理员ID不能为空")
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
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
}

