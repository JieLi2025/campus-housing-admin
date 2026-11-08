package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宿舍楼VO
 * 用于返回宿舍楼信息
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Data
public class BuildingVO {
    
    /**
     * 宿舍楼ID
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
     * 宿舍楼类型
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
     * 状态
     */
    private Integer status;
    
    /**
     * 已分配房间数
     */
    private Integer allocatedRooms;
    
    /**
     * 总床位数
     */
    private Integer totalBeds;
    
    /**
     * 已占用床位数
     */
    private Integer occupiedBeds;
    
    /**
     * 入住率
     */
    private Double occupancyRate;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

