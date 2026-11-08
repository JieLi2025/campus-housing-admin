package com.campus.dorm.dto;

import lombok.Data;

/**
 * 宿舍楼DTO
 * 用于创建和更新宿舍楼
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Data
public class BuildingDTO {
    
    /**
     * 宿舍楼ID（更新时使用）
     */
    private Long id;
    
    /**
     * 宿舍楼名称
     */
    private String buildingName;
    
    /**
     * 宿舍楼编码
     */
    private String buildingCode;
    
    /**
     * 宿舍楼类型：male/female/mixed
     */
    private String buildingType;
    
    /**
     * 楼层数
     */
    private Integer floorCount;
    
    /**
     * 总房间数
     */
    private Integer totalRooms;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 管理员姓名
     */
    private String managerName;
    
    /**
     * 管理员电话
     */
    private String managerPhone;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 状态：0-停用，1-启用
     */
    private Integer status;
}

