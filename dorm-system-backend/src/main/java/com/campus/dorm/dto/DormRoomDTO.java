package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 宿舍房间DTO
 * 用于创建和更新宿舍房间
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Data
public class DormRoomDTO {
    
    /**
     * 房间ID（更新时必填）
     */
    private Long id;
    
    /**
     * 楼栋ID
     */
    @NotNull(message = "楼栋ID不能为空")
    private Long buildingId;
    
    /**
     * 房间号
     */
    @NotBlank(message = "房间号不能为空")
    private String roomNumber;
    
    /**
     * 楼层
     */
    @NotNull(message = "楼层不能为空")
    @Min(value = 1, message = "楼层必须大于0")
    private Integer floor;
    
    /**
     * 房间类型（如：four_bed, six_bed）
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
     * 可用床位数
     */
    private Integer availableBeds;
    
    /**
     * 状态（available/full/maintenance）
     */
    private String status;
}
