package com.campus.dorm.dto;

import lombok.Data;

/**
 * 学生导入数据DTO
 * 用于Excel导入时的数据承载
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Data
public class StudentImportDTO {
    
    /**
     * 学号（必填）
     */
    private String studentId;
    
    /**
     * 真实姓名（必填）
     */
    private String realName;
    
    /**
     * 性别（必填）：男/女（导入时支持多种格式，会自动转换为中文）
     */
    private String gender;
    
    /**
     * 专业（必填）
     */
    private String major;
    
    /**
     * 年级（必填）
     */
    private String grade;
    
    /**
     * 班级（必填）
     */
    private String className;
}

