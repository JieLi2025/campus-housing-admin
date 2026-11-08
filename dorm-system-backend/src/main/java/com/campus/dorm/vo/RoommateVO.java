package com.campus.dorm.vo;

import lombok.Data;
import java.util.List;

@Data
public class RoommateVO {
    private Long id;
    private Long studentId;
    private Long userId;  // 关联的系统用户ID，用于聊天等功能
    private String studentNumber;  // 学号（字符串）
    private String name;
    private String major;
    private String className;
    private String avatar;
    private String bio;
    private List<String> tags;
    private Integer rate;  // 匹配度百分比
    
    // 偏好设置相关字段（用于获取偏好设置接口）
    private List<String> preferredTags;
    private List<String> excludedTags;
    private String sleepTime;
    private Integer cleanliness;
    private String studyHabit;
    private String hobbies;
    private String personality;
    private String otherRequirements;
}

