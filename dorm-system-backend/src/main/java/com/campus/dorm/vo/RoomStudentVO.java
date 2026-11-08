package com.campus.dorm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 房间学生信息VO
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStudentVO {
    
    /**
     * 学生ID
     */
    private Long studentId;
    
    /**
     * 学号
     */
    private String studentNo;
    
    /**
     * 学生姓名
     */
    private String realName;
    
    /**
     * 床位号
     */
    private String bedNumber;
    
    /**
     * 入住时间
     */
    private String assignedAt;
}

