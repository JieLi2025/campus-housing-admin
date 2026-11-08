package com.campus.dorm.dto;

import lombok.Data;

/**
 * 团队邀请DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class TeamInviteDTO {
    
    /**
     * 被邀请学生的学号
     */
    private String studentId;
    
    /**
     * 被邀请学生的ID
     */
    private Long receiverId;
    
    /**
     * 邀请留言
     */
    private String message;
}

