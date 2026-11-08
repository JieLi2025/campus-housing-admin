package com.campus.dorm.dto;

import lombok.Data;

/**
 * 公告查询DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class AnnouncementQueryDTO {
    
    /**
     * 当前页码
     */
    private Integer currentPage = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
    
    /**
     * 搜索关键词（标题或内容）
     */
    private String keyword;
    
    /**
     * 状态筛选：已发布/已归档
     */
    private String status;
    
    /**
     * 楼栋ID筛选（学生端使用）
     */
    private Long buildingId;
    
    /**
     * 是否只查询未过期的（学生端使用）
     */
    private Boolean onlyValid;
}

