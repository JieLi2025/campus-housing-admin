package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 审批访客DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class ApproveVisitorDTO {
    
    /**
     * 是否批准（true-批准，false-拒绝）
     */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;
    
    /**
     * 管理员审批意见
     */
    private String adminComment;
    
    /**
     * 拒绝原因（拒绝时必填）
     */
    private String rejectReason;
}

