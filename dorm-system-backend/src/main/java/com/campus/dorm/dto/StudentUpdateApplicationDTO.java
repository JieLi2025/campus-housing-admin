package com.campus.dorm.dto;

import lombok.Data;

/**
 * 学生修改申请DTO（仅修改申请内容，不修改类型）
 * 
 * @author Campus Dorm System
 * @date 2025-11-30
 */
@Data
public class StudentUpdateApplicationDTO {
    
    /**
     * 申请原因
     */
    private String reason;
    
    /**
     * 目标宿舍信息（可选）
     */
    private String targetDormInfo;
    
    /**
     * 联系电话
     */
    private String contactPhone;
}
