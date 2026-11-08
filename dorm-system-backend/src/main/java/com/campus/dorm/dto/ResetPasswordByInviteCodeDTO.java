package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 通过邀请码重置密码DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-07
 */
@Data
public class ResetPasswordByInviteCodeDTO {
    
    /**
     * 学号
     */
    @NotBlank(message = "学号不能为空")
    private String studentId;
    
    /**
     * 邀请码
     */
    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
    
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8位")
    private String newPassword;
}
