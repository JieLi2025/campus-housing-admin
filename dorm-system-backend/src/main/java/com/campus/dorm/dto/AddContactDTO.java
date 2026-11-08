package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 添加联系人DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class AddContactDTO {
    
    /**
     * 联系人ID
     */
    @NotNull(message = "联系人ID不能为空")
    private Long contactId;
    
    /**
     * 备注名称（可选）
     */
    private String remark;
}

