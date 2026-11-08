package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访客实体类
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visitor")
public class Visitor extends BaseEntity {
    
    /**
     * 访客ID
     */
    @TableId(type = IdType.AUTO)
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
     * 学生宿舍（楼栋-房间号）
     */
    private String studentRoom;
    
    /**
     * 楼栋ID（用于权限过滤）
     */
    private Long buildingId;
    
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
    private LocalDate visitDate;
    
    /**
     * 来访时段
     */
    private String visitTime;
    
    // ========== 审批信息 ==========
    
    /**
     * 状态：PENDING-待审核，APPROVED-已批准，REJECTED-已拒绝，
     * COMPLETED-已完成，CANCELLED-已取消
     */
    private String status;
    
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
    private LocalDateTime approvedAt;
    
    // ========== 签到签退信息 ==========
    
    /**
     * 签到时间
     */
    private LocalDateTime checkedInAt;
    
    /**
     * 签退时间
     */
    private LocalDateTime checkedOutAt;
    
    // ========== 其他 ==========
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 覆盖父类的 updatedAt 字段，标记为非持久化
     * visitor 表没有 updated_at 字段
     */
    @TableField(exist = false)
    private LocalDateTime updatedAt;
}

