package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能分配请求DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-01
 */
@Data
@Schema(description = "智能分配请求")
public class IntelligentAssignRequest {
    
    @Schema(description = "学生ID列表，为空则处理所有待分配学生")
    private List<Long> studentIds;
    
    @Schema(description = "楼栋ID列表，限定分配范围")
    private List<Long> buildingIds;
    
    @Schema(description = "分配策略：same_class(同班级优先), same_major(同专业优先), auto(自动选择)")
    private String strategy = "same_class";
    
    @Schema(description = "是否允许跨专业分配")
    private Boolean allowCrossMajor = false;
    
    @Schema(description = "是否允许跨班级分配")
    private Boolean allowCrossClass = true;
}
