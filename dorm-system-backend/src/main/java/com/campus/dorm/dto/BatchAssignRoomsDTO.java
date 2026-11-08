package com.campus.dorm.dto;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量分配房间DTO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class BatchAssignRoomsDTO {
    
    /**
     * 房间ID列表
     */
    @NotEmpty(message = "房间ID列表不能为空")
    private List<Long> roomIds;
    
    /**
     * 楼栋ID
     */
    @NotNull(message = "楼栋ID不能为空")
    private Long buildingId;
    
    /**
     * 归属专业
     */
    private String majorName;
    
    /**
     * 归属班级
     */
    private String className;
    
    /**
     * 操作类型：assign-分配，unassign-取消分配
     */
    private String action;
}

