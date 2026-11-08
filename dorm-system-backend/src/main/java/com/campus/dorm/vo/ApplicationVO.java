package com.campus.dorm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请VO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class ApplicationVO {
    
    /**
     * 申请ID
     */
    private Long id;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 学生姓名
     */
    private String studentName;
    
    /**
     * 学号
     */
    private String studentNumber;
    
    /**
     * 申请类型
     */
    private String type;
    
    /**
     * 申请类型（中文）
     */
    private String typeLabel;
    
    /**
     * 申请原因
     */
    private String reason;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态（中文）
     */
    private String statusLabel;
    
    /**
     * 当前宿舍信息
     */
    private String currentDormInfo;
    
    /**
     * 目标宿舍信息
     */
    private String targetDormInfo;
    
    /**
     * 分配的宿舍信息
     */
    private String assignedDormInfo;
    
    /**
     * 调换对象学生ID
     */
    private Long exchangeStudentId;
    
    /**
     * 调换对象姓名
     */
    private String exchangeStudentName;
    
    /**
     * 调换对象学号
     */
    private String exchangeStudentNumber;
    
    /**
     * 调换对象宿舍信息
     */
    private String exchangeDormInfo;
    
    /**
     * 审批意见
     */
    private String adminComment;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 申请时间（格式：yyyy-MM-dd HH:mm）
     */
    private String applyTime;
    
    /**
     * 创建时间（原始字段）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
}

