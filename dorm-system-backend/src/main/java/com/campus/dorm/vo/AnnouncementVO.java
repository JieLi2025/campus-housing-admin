package com.campus.dorm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告VO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class AnnouncementVO {
    
    /**
     * 公告ID
     */
    private Long id;
    
    /**
     * 公告标题
     */
    private String title;
    
    /**
     * 公告副标题
     */
    private String subtitle;
    
    /**
     * 公告内容
     */
    private String content;
    
    /**
     * 额外要点
     */
    private String extraPoints;
    
    /**
     * 发布者ID
     */
    private Long publisherId;
    
    /**
     * 发布者姓名
     */
    private String publisherName;
    
    /**
     * 发布者类型
     */
    private String publisherType;
    
    /**
     * 目标楼栋类型
     */
    private String buildingType;
    
    /**
     * 目标楼栋ID列表
     */
    private List<Long> buildingIds;
    
    /**
     * 楼栋名称（用于前端显示）
     */
    private String building;
    
    /**
     * 状态（统一使用中文：已发布、已归档）
     */
    private String status;
    
    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;
    
    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireDate;
    
    /**
     * 展示选项
     */
    private List<String> displayOptions;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 发布时间（格式：yyyy-MM-dd）
     */
    private String publishTime;
    
    /**
     * 是否已过期
     */
    private Boolean isExpired;
}

