package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 审批申请DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class ApproveApplicationDTO {
    
    /**
     * 是否通过：true-通过，false-拒绝
     */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;
    
    /**
     * 审批意见
     */
    private String comment;
    
    /**
     * 拒绝原因（approved=false时必填）
     */
    private String rejectReason;
    
    /**
     * 分配的房间ID（调宿申请通过时）
     */
    private Long assignedRoomId;
    
    /**
     * 调换对象学生ID（调宿时与其他学生交换）
     */
    private Long exchangeStudentId;
    
    /**
     * 自定义状态（用于退宿物资清点等特殊流程）
     * 如：COMPENSATION_PENDING-待赔偿处理
     */
    private String status;
}

