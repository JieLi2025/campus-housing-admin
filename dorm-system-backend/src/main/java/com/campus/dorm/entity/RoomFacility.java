package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 房间设施实体
 * 对应数据库表：facility_template
 * 
 * @author Campus Dorm System
 * @date 2025-11-01
 */
@Data
@TableName("facility_template")
public class RoomFacility {
    
    /**
     * 设施ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 设施名称（唯一标识）
     */
    private String facilityName;
    
    /**
     * 默认数量
     */
    private Integer defaultQuantity;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 排序号
     */
    private Integer sortOrder;
    
    /**
     * 默认房型列表（逗号分隔，如：二人间,四人间,六人间,八人间）
     */
    private String defaultRoomTypes;
}

