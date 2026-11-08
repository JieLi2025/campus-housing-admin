package com.campus.dorm.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 楼栋区域数据传输对象
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class BuildingAreaDTO {
    
    /**
     * 区域ID（编辑时需要）
     */
    private String areaId;
    
    /**
     * 楼栋ID
     */
    @NotNull(message = "楼栋ID不能为空")
    private Long buildingId;
    
    /**
     * 区域名称
     */
    @NotBlank(message = "区域名称不能为空")
    private String areaName;
    
    /**
     * 楼层列表
     */
    @NotNull(message = "楼层列表不能为空")
    private List<Integer> floors;
    
    /**
     * 专业班级分配列表
     */
    private List<MajorClassAssignment> assignments;
    
    /**
     * 专业班级分配内部类
     */
    @Data
    public static class MajorClassAssignment {
        /**
         * 专业名称
         */
        private String majorName;
        
        /**
         * 班级名称（可选，仅当mode为class-only时需要）
         */
        private String className;
        
        /**
         * 年级
         */
        private String grade;
        
        /**
         * 分配模式：full-major或class-only
         */
        private String mode;
        
        /**
         * 分配学生数
         */
        private Integer allocatedStudents;
        
        /**
         * 分配房间数
         */
        private Integer allocatedRooms;
    }
}

