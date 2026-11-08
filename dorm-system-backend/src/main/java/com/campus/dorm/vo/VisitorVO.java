package com.campus.dorm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访客VO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class VisitorVO {
    
    /**
     * 访客ID
     */
    private Long id;
    
    // ========== 预约学生信息 ==========
    
    /**
     * 预约学生ID
     */
    private Long studentId;
    
    /**
     * 预约学生姓名
     */
    private String studentName;
    
    /**
     * 学号
     */
    private String studentNumber;
    
    /**
     * 学生宿舍
     */
    private String studentRoom;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 楼栋名称
     */
    private String buildingName;
    
    // ========== 访客信息 ==========
    
    /**
     * 访客姓名
     */
    private String visitorName;
    
    /**
     * 访客电话
     */
    private String visitorPhone;
    
    /**
     * 访客身份证号
     */
    private String visitorIdCard;
    
    // ========== 来访信息 ==========
    
    /**
     * 来访事由
     */
    private String visitReason;
    
    /**
     * 来访日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
    
    /**
     * 来访时段
     */
    private String visitTime;
    
    // ========== 审批信息 ==========
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态（中文）
     */
    private String statusLabel;
    
    /**
     * 审核管理员ID
     */
    private Long adminId;
    
    /**
     * 审核管理员姓名
     */
    private String adminName;
    
    /**
     * 管理员审批意见
     */
    private String adminComment;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedAt;
    
    // ========== 签到签退信息 ==========
    
    /**
     * 签到时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedInAt;
    
    /**
     * 签退时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedOutAt;
    
    // ========== 其他 ==========
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 预约时间（格式：yyyy-MM-dd）
     */
    private String applyTime;
    
    /**
     * 是否可以签到（已批准且未签到）
     */
    private Boolean canCheckIn;
    
    /**
     * 是否可以签退（已签到且未签退）
     */
    private Boolean canCheckOut;
}

