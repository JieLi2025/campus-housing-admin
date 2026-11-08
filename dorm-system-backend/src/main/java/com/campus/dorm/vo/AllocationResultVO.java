package com.campus.dorm.vo;

import lombok.Data;

import java.util.List;

/**
 * 分配结果VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class AllocationResultVO {
    
    /**
     * 是否成功分配
     */
    private Boolean success;
    
    /**
     * 提示消息
     */
    private String message;
    
    /**
     * 楼栋名称
     */
    private String building;
    
    /**
     * 房间号
     */
    private String roomNumber;
    
    /**
     * 楼层
     */
    private Integer floor;
    
    /**
     * 床位号
     */
    private String bedNumber;
    
    /**
     * 室友列表（包含姓名和头像）
     */
    private List<RoommateVO> roommates;
    
    /**
     * 室友信息VO
     */
    @Data
    public static class RoommateVO {
        /**
         * 室友姓名
         */
        private String name;
        
        /**
         * 室友头像URL
         */
        private String avatar;
        
        /**
         * 室友学号
         */
        private String studentId;
    }
    
    /**
     * 分配阶段：adjustable-可调整期，confirmed-已确认
     */
    private String allocationPhase;
    
    /**
     * 调整截止时间（ISO格式字符串）
     */
    private String adjustmentDeadline;
    
    /**
     * 是否为舍长
     */
    private Boolean isLeader;
    
    /**
     * 宿舍设施信息
     */
    private FacilitiesVO facilities;
    
    /**
     * 物资配置JSON（从material_template动态获取）
     */
    private String materials;
    
    @Data
    public static class FacilitiesVO {
        @com.fasterxml.jackson.annotation.JsonProperty("阳台")
        private Boolean yangTai;
        
        @com.fasterxml.jackson.annotation.JsonProperty("空调")
        private Boolean kongTiao;
        
        @com.fasterxml.jackson.annotation.JsonProperty("独立卫浴")
        private Boolean duLiWeiYu;
        
        @com.fasterxml.jackson.annotation.JsonProperty("WiFi")
        private Boolean wifi;
        
        @com.fasterxml.jackson.annotation.JsonProperty("暖气")
        private Boolean nuanQi;
        
        @com.fasterxml.jackson.annotation.JsonProperty("热水器")
        private Boolean reShuiQi;
    }
}

