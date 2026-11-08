package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 申请DTO（学生提交申请）
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class ApplicationDTO {
    
    /**
     * 申请类型：调宿申请，退宿申请
     */
    @NotBlank(message = "申请类型不能为空")
    private String type;
    
    /**
     * 申请原因
     */
    @NotBlank(message = "申请原因不能为空")
    private String reason;
    
    /**
     * 目标宿舍信息（可选，学生意愿描述）
     */
    private String targetDormInfo;
    
    /**
     * 联系电话（退宿申请需要）
     */
    private String contactPhone;
}

