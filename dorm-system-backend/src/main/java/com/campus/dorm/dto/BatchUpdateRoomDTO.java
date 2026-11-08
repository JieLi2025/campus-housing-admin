package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量更新宿舍DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Data
public class BatchUpdateRoomDTO {
    
    /**
     * 要更新的房间ID列表
     */
    @NotEmpty(message = "房间ID列表不能为空")
    private List<Long> roomIds;
    
    /**
     * 房间类型（可选）
     */
    private String roomType;
    
    /**
     * 容纳人数（可选）
     */
    private Integer capacity;
    
    /**
     * 设施配置（可选，JSON格式）
     */
    private String facilities;
    
    /**
     * 房间状态（可选）
     */
    private String status;
}

