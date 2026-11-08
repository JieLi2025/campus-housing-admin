package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 楼栋专业分配实体类
 * 对应数据库表：building_major_assignment
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("building_major_assignment")
public class BuildingMajorAssignment {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 楼栋ID
     */
    @TableField("building_id")
    private Long buildingId;
    
    /**
     * 区域ID（用于标识同一区域的多条记录）
     */
    @TableField("area_id")
    private String areaId;
    
    /**
     * 区域名称
     */
    @TableField("area_name")
    private String areaName;
    
    /**
     * 楼层列表（JSON格式，如"[1,2,3]"）
     */
    @TableField("floors")
    private String floors;
    
    /**
     * 专业名称
     */
    @TableField("major_name")
    private String majorName;
    
    /**
     * 班级名称
     */
    @TableField("class_name")
    private String className;
    
    /**
     * 班级分片标识（用于拆分班级）：A/B/C等，未拆分为NULL
     */
    @TableField("class_segment")
    private String classSegment;
    
    /**
     * 年级
     */
    @TableField("grade")
    private String grade;
    
    /**
     * 性别要求
     */
    @TableField("gender")
    private String gender;
    
    /**
     * 分配学生数
     */
    @TableField("allocated_students")
    private Integer allocatedStudents;
    
    /**
     * 分配房间数
     */
    @TableField("allocated_rooms")
    private Integer allocatedRooms;
    
    /**
     * 分配模式：full-major-整专业，class-only-整班组合
     */
    @TableField("assignment_mode")
    private String assignmentMode;
    
    /**
     * 学号范围：当一个班级被拆分到多个区域时，记录每个分片包含的学号范围，帮助学生了解自己属于哪个分片
     * 格式示例：2023001001~2023001050
     */
    @TableField("student_id_range")
    private String studentIdRange;
    
    /**
     * 房间类型：二人间、四人间、六人间、八人间
     */
    @TableField("room_type")
    private String roomType;
    
    /**
     * 记录状态（软删除标记）
     * - 1 = 生效（正常使用中的专业分配）
     * - 0 = 失效（已删除但保留记录，用于历史追溯和数据恢复）
     * 
     * 说明：删除专业分配时不会真正删除数据，而是将 status 改为 0
     * 这样可以：1) 保留历史记录 2) 支持数据恢复 3) 避免破坏数据关联
     */
    private Integer status;
}

