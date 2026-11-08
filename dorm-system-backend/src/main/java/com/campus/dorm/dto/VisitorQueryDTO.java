package com.campus.dorm.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 访客查询DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class VisitorQueryDTO {
    
    /**
     * 当前页
     */
    private Integer currentPage = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 10;
    
    /**
     * 搜索关键词（访客姓名或学生姓名或学号）
     */
    private String keyword;
    
    /**
     * 状态筛选：PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED
     */
    private String status;
    
    /**
     * 来访日期筛选
     */
    private LocalDate visitDate;
    
    /**
     * 学生ID（学生端查询自己的访客）
     */
    private Long studentId;
}

