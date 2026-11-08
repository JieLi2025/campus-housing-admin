package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告实体类
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcement")
public class Announcement extends BaseEntity {
    
    /**
     * 公告ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 公告标题
     */
    private String title;
    
    /**
     * 公告副标题
     */
    @TableField(exist = false)
    private String subtitle;
    
    /**
     * 公告内容
     */
    private String content;
    
    /**
     * 额外要点（每行一条）
     */
    @TableField(exist = false)
    private String extraPoints;
    
    // ========== 发布信息 ==========
    
    /**
     * 发布者ID
     */
    private Long publisherId;
    
    /**
     * 发布者姓名
     * 注意：数据库表中不存在此字段，标记为非持久化字段
     */
    @TableField(exist = false)
    private String publisherName;
    
    /**
     * 发布者类型：ADMIN-管理员，SUPER_ADMIN-超级管理员
     * 注意：数据库表中不存在此字段，标记为非持久化字段
     */
    @TableField(exist = false)
    private String publisherType;
    
    // ========== 目标和状态 ==========
    
    /**
     * 目标楼栋类型（如果按楼栋类型发布）
     * 注意：数据库表中不存在此字段，标记为非持久化字段
     */
    @TableField(exist = false)
    private String buildingType;
    
    /**
     * 目标楼栋ID列表（逗号分隔，ALL表示全部）
     */
    private String buildingIds;
    
    /**
     * 状态：已发布、已归档（统一使用中文）
     */
    private String status;
    
    // ========== 时间信息 ==========
    
    /**
     * 发布时间
     * 注意：数据库表中不存在此字段，标记为非持久化字段
     * 发布时间可以通过 created_at 字段获取（当状态为 PUBLISHED 时）
     */
    @TableField(exist = false)
    private LocalDateTime publishedAt;
    
    /**
     * 过期时间（映射到数据库的deadline字段）
     */
    @TableField("deadline")
    private LocalDateTime expireDate;
    
    // ========== 展示设置（已废弃，保留字段兼容性） ==========
    
    /**
     * 展示选项（已废弃，不再使用）
     * 所有公告统一在公告中心展示
     */
    @TableField(exist = false)
    private String displayOptions;
    
    /**
     * 覆盖父类的 updatedAt 字段，标记为非持久化
     * announcement 表已移除 updated_at 字段
     */
    @TableField(exist = false)
    private LocalDateTime updatedAt;
    
}

