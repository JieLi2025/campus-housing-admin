package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建会话DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class CreateConversationDTO {
    
    /**
     * 对方用户ID
     */
    @NotNull(message = "对方用户ID不能为空")
    private Long userId;
}

