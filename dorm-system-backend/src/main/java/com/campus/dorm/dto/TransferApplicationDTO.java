package com.campus.dorm.dto;

import lombok.Data;

/**
 * 调宿申请DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class TransferApplicationDTO {
    
    /**
     * 申请类型：room-换房间，roommate-换室友
     */
    private String transferType;
    
    /**
     * 目标楼栋
     */
    private String targetBuilding;
    
    /**
     * 目标房间号
     */
    private String targetRoom;
    
    /**
     * 目标室友学号（换室友时使用）
     */
    private String targetRoommateId;
    
    /**
     * 申请原因：health-身体健康原因，study-学习环境需要，
     * roommate_conflict-室友关系问题，family-家庭特殊情况，other-其他原因
     */
    private String reason;
    
    /**
     * 详细说明
     */
    private String description;
    
    /**
     * 分配阶段：self-selection-自主分配期，confirmed-正式入住后
     */
    private String allocationPhase;
    
    /**
     * 是否为舍长
     */
    private Boolean isLeader;
    
    /**
     * 联系电话
     */
    private String contactPhone;
}

