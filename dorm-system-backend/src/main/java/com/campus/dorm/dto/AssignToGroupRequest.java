package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 分配学生到宿舍组请求DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-01
 */
@Data
@Schema(description = "分配学生到宿舍组请求")
public class AssignToGroupRequest {
    
    @NotNull(message = "学生ID不能为空")
    @Schema(description = "学生ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long studentId;
    
    @Schema(description = "宿舍组ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long groupId;
    
    @Schema(description = "是否强制分配（忽略警告）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean force = false;
}
