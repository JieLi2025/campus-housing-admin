package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 宿舍组实体类
 * 用于学生自组宿舍功能
 * 
 * @author Campus Dorm System
 * @date 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dorm_group")
public class DormGroup extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 逻辑删除标志（数据库表中不存在此字段，禁用逻辑删除）
     */
    @TableField(exist = false)
    private Integer deleted;
    
    /**
     * 更新时间（覆盖父类字段，数据库表中不存在此字段）
     */
    @TableField(exist = false)
    private LocalDateTime updatedAt;
    
    /**
     * 创建时间（覆盖父类字段，数据库表中不存在此字段）
     */
    @TableField(exist = false)
    private LocalDateTime createdAt;
    
    /**
     * 宿舍组名称
     */
    private String groupName;
    
    /**
     * 组长ID（学生ID）
     */
    private Long leaderId;
    
    /**
     * 组长姓名（非持久化字段，仅用于查询结果展示）
     */
    @TableField(exist = false)
    private String leaderName;
    
    /**
     * 成员列表（JSON格式存储）
     */
    private String members;
    
    /**
     * 目标房间类型（four_bed/six_bed）
     */
    @TableField("target_room_type")
    private String targetRoomType;
    
    /**
     * 最大成员数（根据房间类型：4/6人间）
     */
    private Integer maxMembers;
    
    /**
     * 当前成员数
     */
    @TableField("current_members")
    private Integer currentMembers;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 班级
     */
    private String className;
    
    /**
     * 年级
     */
    private String grade;
    
    /**
     * 性别限制（male/female）
     */
    private String gender;
    
    /**
     * 已选择的房间ID
     */
    @TableField(value = "selected_room_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long selectedRoomId;
    
    /**
     * 状态：forming-组建中，full-已满员，room_selected-已选房，allocated-已分配，disbanded-已解散
     */
    private String status;
    
    /**
     * 群公告
     */
    private String announcement;
    
    // ========== 非持久化字段，用于查询结果展示 ==========
    
    /**
     * 房间号（非持久化字段）
     */
    @TableField(exist = false)
    private String roomNumber;
    
    /**
     * 楼栋名称（非持久化字段）
     */
    @TableField(exist = false)
    private String buildingName;
    
    /**
     * 楼层（非持久化字段）
     */
    @TableField(exist = false)
    private Integer floor;
}

