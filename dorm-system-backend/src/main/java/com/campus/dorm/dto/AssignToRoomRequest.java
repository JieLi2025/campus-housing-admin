package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 分配学生到房间请求DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-01
 */
@Data
@Schema(description = "分配学生到房间请求")
public class AssignToRoomRequest {
    
    @NotEmpty(message = "学生ID列表不能为空")
    @Schema(description = "学生ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> studentIds;
    
    @Schema(description = "房间ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long roomId;
    
    @Schema(description = "是否创建新宿舍组（true）或加入房间现有宿舍组（false）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean createNewGroup = true;
}
