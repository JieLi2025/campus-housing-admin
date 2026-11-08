package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼栋专业分配VO
 * 用于返回楼栋专业分配信息
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Data
public class BuildingMajorAssignmentVO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 楼栋名称（关联查询）
     */
    private String buildingName;
    
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
     * 分配模式
     */
    private String assignmentMode;
    
    /**
     * 记录状态（软删除标记）
     * - 1 = 生效（正常使用中的专业分配）
     * - 0 = 失效（已删除但保留记录，用于历史追溯和数据恢复）
     */
    private Integer status;
}

