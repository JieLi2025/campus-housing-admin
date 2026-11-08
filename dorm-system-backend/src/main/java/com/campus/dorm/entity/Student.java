package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 学生信息实体类
 * 对应数据库表：student
 * 注意：student表没有updated_at字段
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student")
public class Student extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 覆盖父类的 updatedAt 字段，标记为非持久化
     * student 表没有 updated_at 字段
     */
    @TableField(exist = false)
    private LocalDateTime updatedAt;
    
    /**
     * 学生ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联用户ID
     */
    private Long userId;
    
    /**
     * 学号
     */
    private String studentId;
    
    /**
     * 真实姓名（学生业务数据的核心属性，导入时保存）
     */
    private String realName;
    
    /**
     * 性别：男/女
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
     * 班级分片标识（用于拆分班级时的归属，如A/B/C，未拆分为NULL）
     */
    private String classSegment;
    
    /**
     * 宿舍组ID
     */
    private Long dormGroupId;
    
    /**
     * 房间ID
     */
    private Long roomId;
    
    /**
     * 房间号（冗余字段，方便显示）
     */
    private String roomNumber;
    
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
     * 是否已注册：0-未注册，1-已注册
     */
    private Integer isRegistered;
    
    /**
     * 注册时间
     */
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
     * 就寝时间（格式：HH:mm）
     */
    private String bedTime;
    
    /**
     * 起床时间（格式：HH:mm）
     */
    private String wakeTime;
}

