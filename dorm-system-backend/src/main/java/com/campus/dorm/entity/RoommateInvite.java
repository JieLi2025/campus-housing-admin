package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 室友邀请实体类
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
@TableName("roommate_invite")
public class RoommateInvite {
    
    /**
     * 邀请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // ========== 发送者信息 ==========
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者姓名
     */
    private String senderName;
    
    /**
     * 发送者学号
     */
    private String senderNumber;
    
    /**
     * 发送者宿舍
     */
    @TableField(exist = false)
    private String senderRoom;
    
    // ========== 接收者信息 ==========
    
    /**
     * 接收者ID
     */
    private Long receiverId;
    
    /**
     * 接收者姓名
     */
    private String receiverName;
    
    /**
     * 接收者学号
     */
    private String receiverNumber;
    
    /**
     * 接收者宿舍
     */
    @TableField(exist = false)
    private String receiverRoom;
    
    /**
     * 接收者专业（非数据库字段，用于返回数据）
     */
    @TableField(exist = false)
    private String receiverMajor;
    
    /**
     * 接收者班级（非数据库字段，用于返回数据）
     */
    @TableField(exist = false)
    private String receiverClass;
    
    // ========== 邀请信息 ==========
    
    /**
     * 目标房间ID
     */
    @TableField(exist = false)
    private Long roomId;
    
    /**
     * 目标房间号
     */
    @TableField(exist = false)
    private String roomNumber;
    
    /**
     * 目标楼栋名称
     */
    @TableField(exist = false)
    private String buildingName;
    
    // ========== 状态信息 ==========
    
    /**
     * 状态：PENDING-待处理，ACCEPTED-已接受，REJECTED-已拒绝，EXPIRED-已过期
     */
    private String status;
    
    /**
     * 匹配度分数
     */
    @TableField(exist = false)
    private Integer matchScore;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

