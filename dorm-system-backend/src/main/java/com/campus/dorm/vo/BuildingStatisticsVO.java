package com.campus.dorm.vo;

import lombok.Data;

/**
 * 楼栋统计信息VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class BuildingStatisticsVO {
    
    /**
     * 楼栋ID
     */
    private Long id;
    
    /**
     * 楼栋名称
     */
    private String name;
    
    /**
     * 楼栋编码
     */
    private String code;
    
    /**
     * 楼栋类型：male-男生宿舍，female-女生宿舍，mixed-混合宿舍
     */
    private String buildingType;
    
    /**
     * 性别（前端显示用：男/女/混合）
     */
    private String gender;
    
    /**
     * 总房间数
     */
    private Integer totalRooms;
    
    /**
     * 总床位数
     */
    private Integer totalBeds;
    
    /**
     * 已占用床位数
     */
    private Integer occupiedBeds;
    
    /**
     * 可用床位数
     */
    private Integer availableBeds;
    
    /**
     * 入住率（百分比）
     */
    private Integer occupancyRate;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 管理员ID
     */
    private Long adminId;
    
    /**
     * 管理员姓名
     */
    private String managerName;
    
    /**
     * 管理员电话
     */
    private String managerPhone;
}

