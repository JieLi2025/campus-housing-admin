package com.campus.dorm.vo;

import lombok.Data;
import java.util.List;

/**
 * 学生列表VO
 * 包含分页信息和统计数据
 * 
 * @author Campus Dorm System
 * @date 2024-10-22
 */
@Data
public class StudentListVO {
    
    /**
     * 学生列表
     */
    private List<StudentVO> students;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer currentPage;
    
    /**
     * 每页大小
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Long totalPages;
    
    /**
     * 统计信息
     */
    private StudentStatsVO stats;
    
    /**
     * 所有可用的专业列表（用于筛选下拉框）
     */
    private List<String> availableMajors;
    
    /**
     * 学生统计信息VO
     */
    @Data
    public static class StudentStatsVO {
        /**
         * 学生总数
         */
        private Long total;
        
        /**
         * 已注册数
         */
        private Long registered;
        
        /**
         * 未注册数
         */
        private Long unregistered;
        
        /**
         * 注册率（百分比）
         */
        private Double registrationRate;
    }
}

