package com.campus.dorm.dto;

import lombok.Data;

/**
 * 申请查询DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class ApplicationQueryDTO {
    
    /**
     * 当前页码
     */
    private Integer currentPage = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
    
    /**
     * 搜索关键词（学号或姓名）
     */
    private String keyword;
    
    /**
     * 申请类型：TRANSFER-调宿申请，CHECKOUT-退宿申请
     */
    private String type;
    
    /**
     * 状态筛选
     */
    private String status;
    
    /**
     * 学生ID（查询指定学生的申请）
     */
    private Long studentId;
}

