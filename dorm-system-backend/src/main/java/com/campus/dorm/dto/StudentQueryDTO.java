package com.campus.dorm.dto;

import lombok.Data;

/**
 * 学生查询DTO
 * 用于学生列表查询的参数封装
 * 
 * @author Campus Dorm System
 * @date 2024-10-22
 */
@Data
public class StudentQueryDTO {
    
    /**
     * 搜索关键词（学号、姓名）
     */
    private String keyword;
    
    /**
     * 专业筛选
     */
    private String major;
    
    /**
     * 性别筛选：男/女
     */
    private String gender;
    
    /**
     * 年级筛选
     */
    private String grade;
    
    /**
     * 注册状态筛选：registered/unregistered
     */
    private String registrationStatus;
    
    /**
     * 宿舍分配状态筛选：allocated/unallocated
     */
    private String allocationStatus;
    
    /**
     * 当前页码
     */
    private Integer currentPage = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}

