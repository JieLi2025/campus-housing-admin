package com.campus.dorm.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学生信息VO
 * 用于返回给前端的学生信息
 * 
 * @author Campus Dorm System
 * @date 2024-10-22
 */
@Data
public class StudentVO {
    
    /**
     * 学生ID
     */
    private Long id;
    
    /**
     * 关联用户ID（学生注册后关联）
     */
    private Long userId;
    
    /**
     * 学号
     */
    private String studentId;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 性别
     */
    private String gender;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 年级
     */
    private String grade;
    
    /**
     * 班级
     */
    private String className;
    
    /**
     * 班级分片标识（用于拆分班级时的归属）
     */
    private String classSegment;
    
    /**
     * 邀请码
     */
    private String inviteCode;
    
    /**
     * 邀请码状态：unused/used/expired
     */
    private String inviteCodeStatus;
    
    /**
     * 邀请码有效期
     */
    private LocalDateTime inviteCodeExpiry;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 是否已注册
     */
    private Boolean isRegistered;
    
    /**
     * 注册时间
     * 前端使用 registeredAt 字段名
     */
    @JsonProperty("registeredAt")
    private LocalDateTime registrationDate;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 个人标签（JSON数组）
     */
    private String tags;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 宿舍号（兼容旧字段名）
     */
    private String dormNumber;
    
    /**
     * 房间号（与Student实体的roomNumber字段对应）
     */
    private String roomNumber;
    
    /**
     * 宿舍位置信息（楼栋-房间号）
     */
    private String dormLocation;
    
    /**
     * 床位号
     */
    private String bedNumber;
    
    /**
     * 就寝时间（格式：HH:mm）
     */
    private String bedTime;
    
    /**
     * 起床时间（格式：HH:mm）
     */
    private String wakeTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

