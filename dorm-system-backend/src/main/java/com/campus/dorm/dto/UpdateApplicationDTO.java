package com.campus.dorm.dto;

import lombok.Data;

/**
 * 更新申请DTO（管理员更新分配信息）
 * 
 * @author Campus Dorm System
 * @date 2025-11-07
 */
@Data
public class UpdateApplicationDTO {
    
    /**
     * 分配的房间ID（调宿申请）
     */
    private Long assignedRoomId;
    
    /**
     * 调换对象学生ID（调宿时与其他学生交换）
     */
    private Long exchangeStudentId;
}

