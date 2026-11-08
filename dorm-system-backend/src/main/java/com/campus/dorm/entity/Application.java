package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 申请实体类
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("application")
public class Application extends BaseEntity {
    
    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 排除 BaseEntity 中的 updatedAt 字段（数据库已删除该字段）
     */
    @TableField(exist = false)
    private LocalDateTime updatedAt;
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 楼栋ID（用于权限过滤，确保管理员只能看到自己楼栋的申请）
     */
    private Long buildingId;
    
    /**
     * 申请类型：TRANSFER-调宿申请，CHECKOUT-退宿申请
     */
    private String type;
    
    /**
     * 申请原因
     */
    private String reason;
    
    /**
     * 状态：PENDING-待审核，APPROVED-已通过，REJECTED-已拒绝，
     * INVENTORY_PASSED-物资清点通过，COMPENSATION_PENDING-待赔偿处理
     */
    private String status;
    
    // ========== 宿舍信息 ==========
    
    /**
     * 目标宿舍信息（学生意愿描述）
     */
    private String targetDormInfo;
    
    // ========== 分配信息（管理员分配的宿舍） ==========
    
    /**
     * 分配的房间ID
     */
    private Long assignedRoomId;
    
    // ========== 调换相关 ==========
    
    /**
     * 调换对象学生ID
     */
    private Long exchangeStudentId;
    
    // ========== 审批信息 ==========
    
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
}

