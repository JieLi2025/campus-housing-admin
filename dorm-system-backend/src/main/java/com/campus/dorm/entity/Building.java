package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 宿舍楼实体类
 * 对应数据库表：building
 * 
 * @author Campus Dorm System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("building")
public class Building {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 宿舍楼ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 宿舍楼名称
     */
    @TableField("building_name")
    private String buildingName;
    
    /**
     * 宿舍楼编码（唯一）
     */
    @TableField("building_code")
    private String buildingCode;
    
    /**
     * 宿舍楼类型：男-男生宿舍，女-女生宿舍
     */
    @TableField("building_type")
    private String buildingType;
    
    /**
     * 楼层数
     */
    @TableField("floor_count")
    private Integer floorCount;
    
    /**
     * 总房间数
     */
    @TableField("total_rooms")
    private Integer totalRooms;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 管理员姓名
     */
    @TableField("manager_name")
    private String managerName;
    
    /**
     * 管理员电话
     */
    @TableField("manager_phone")
    private String managerPhone;
    
    /**
     * 管理员ID（关联admin表，一栋楼只能有一个管理员）
     */
    @TableField("admin_id")
    private Long adminId;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 当前分配阶段
     * - forming_teams: 组建团队阶段（学生可以组队、发送邀请、接受邀请）
     * - room_selection: 选择房间阶段（舍长选房，学生仍可组队）
     * - admin_allocation: 管理员剩余处理阶段（禁止组队，管理员分配未组队学生）
     * - completed: 分配已完成
     */
    @TableField("current_phase")
    private String currentPhase;
    
    /**
     * 选房阶段截止时间
     */
    @TableField("room_selection_end")
    private LocalDateTime roomSelectionEnd;
}
