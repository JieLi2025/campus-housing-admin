package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 批量创建房间DTO
 * 用于批量创建连续的房间号
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Data
public class BatchCreateRoomDTO {
    
    /**
     * 楼栋ID
     */
    @NotNull(message = "楼栋ID不能为空")
    private Long buildingId;
    
    /**
     * 起始楼层
     */
    @NotNull(message = "起始楼层不能为空")
    @Min(value = 1, message = "起始楼层必须大于0")
    private Integer startFloor;
    
    /**
     * 结束楼层
     */
    @NotNull(message = "结束楼层不能为空")
    @Min(value = 1, message = "结束楼层必须大于0")
    private Integer endFloor;
    
    /**
     * 每层房间数
     */
    @NotNull(message = "每层房间数不能为空")
    @Min(value = 1, message = "每层房间数必须大于0")
    private Integer roomsPerFloor;
    
    /**
     * 房间类型
     */
    @NotBlank(message = "房间类型不能为空")
    private String roomType;
    
    /**
     * 床位数
     */
    @NotNull(message = "床位数不能为空")
    @Min(value = 1, message = "床位数必须大于0")
    private Integer bedCount;
    
    /**
     * 设施信息（JSON格式）
     */
    private String facilities;
}
