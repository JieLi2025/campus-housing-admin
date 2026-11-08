package com.campus.dorm.dto;

import lombok.Data;

/**
 * 房间选择DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class RoomSelectionDTO {
    
    /**
     * 房间ID
     */
    private Long roomId;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 宿舍组ID
     */
    private Long groupId;
}

