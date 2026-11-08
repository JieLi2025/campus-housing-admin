package com.campus.dorm.dto;

import lombok.Data;

/**
 * 更新房间备注DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class UpdateRoomStatusDTO {
    
    /**
     * 房间ID
     */
    private Long roomId;
    
    /**
     * 备注说明
     */
    private String notes;
}

