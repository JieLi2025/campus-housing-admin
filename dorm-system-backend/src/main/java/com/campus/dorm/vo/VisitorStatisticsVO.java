package com.campus.dorm.vo;

import lombok.Data;

/**
 * 访客统计VO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class VisitorStatisticsVO {
    
    /**
     * 待审核数量
     */
    private Integer pendingCount;
    
    /**
     * 已批准数量
     */
    private Integer approvedCount;
    
    /**
     * 已拒绝数量
     */
    private Integer rejectedCount;
    
    /**
     * 已完成数量
     */
    private Integer completedCount;
    
    /**
     * 今日访客数量
     */
    private Integer todayCount;
    
    /**
     * 本周访客数量
     */
    private Integer weekCount;
    
    /**
     * 本月访客数量
     */
    private Integer monthCount;
    
    /**
     * 当前在访数量（已签到未签退）
     */
    private Integer currentVisitorCount;
}

