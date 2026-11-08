package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 重置密码DTO
 * 
 * @author Campus Dorm System
 * @date 2024-10-26
 */
@Data
public class ResetPasswordDTO {
    
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}

