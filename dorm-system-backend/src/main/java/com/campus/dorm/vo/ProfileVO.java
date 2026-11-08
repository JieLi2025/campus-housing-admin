package com.campus.dorm.vo;

import lombok.Data;

/**
 * 个人信息VO
 */
@Data
public class ProfileVO {
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 角色
     */
    private String role;
    
    /**
     * 学号
     */
    private String studentId;
    
    /**
     * 性别
     */
    private String gender;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 班级
     */
    private String className;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 个人标签（JSON数组字符串）
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
    
    /**
     * 宿舍位置（格式：楼栋-房间号）
     */
    private String dormLocation;
    
    /**
     * 房间ID（用于跳转）
     */
    private Long roomId;
    
    /**
     * 床位号
     */
    private String bedNumber;
    
    // ========== 管理员特有字段 ==========
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 职位
     */
    private String position;
    
    /**
     * 办公地点
     */
    private String officeLocation;
    
    /**
     * 工作时间（完整格式，如：08:30-17:30）
     */
    private String workHours;
    
    /**
     * 上班时间（格式：HH:mm）
     */
    private String workStartTime;
    
    /**
     * 下班时间（格式：HH:mm）
     */
    private String workEndTime;
}

