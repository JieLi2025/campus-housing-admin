package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 更新联系人备注DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class UpdateContactRemarkDTO {
    
    /**
     * 备注名称
     */
    @NotBlank(message = "备注名称不能为空")
    private String remark;
}

