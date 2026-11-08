package com.campus.dorm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学生导入结果VO
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportResultVO {
    
    /**
     * 总记录数
     */
    private Integer totalCount;
    
    /**
     * 成功导入数
     */
    private Integer successCount;
    
    /**
     * 失败记录数
     */
    private Integer failureCount;
    
    /**
     * 错误详情列表
     */
    private List<ErrorDetail> errors;
    
    /**
     * 错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        /**
         * 行号
         */
        private Integer rowIndex;
        
        /**
         * 学号
         */
        private String studentId;
        
        /**
         * 姓名
         */
        private String realName;
        
        /**
         * 错误原因
         */
        private String errorMessage;
    }
}

