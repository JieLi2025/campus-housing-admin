package com.campus.dorm.dto;

import lombok.Data;

import java.util.List;

/**
 * 楼栋专业分配DTO
 * 用于创建和更新楼栋专业分配
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Data
public class BuildingMajorAssignmentDTO {
    
    /**
     * 楼栋ID（必填）
     */
    private Long buildingId;
    
    /**
     * 专业分配列表
     */
    private List<MajorAssignmentItem> assignments;
    
    /**
     * 操作人姓名（用于记录历史）
     */
    private String operatorName;
    
    /**
     * 操作人ID（用于记录历史）
     */
    private Long operatorId;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 专业分配项
     */
    @Data
    public static class MajorAssignmentItem {
        /**
         * 专业名称
         */
        private String majorName;
        
        /**
         * 专业代码
         */
        private String majorCode;
        
        /**
         * 分配学生数
         */
        private Integer allocatedStudents;
        
        /**
         * 分配房间数
         */
        private Integer allocatedRooms;
        
        /**
         * 起始房间号
         */
        private String startRoom;
        
        /**
         * 结束房间号
         */
        private String endRoom;
        
        /**
         * 分配模式：full-major 或 class-only
         */
        private String assignmentMode;
    }
}

