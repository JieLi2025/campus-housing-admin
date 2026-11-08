package com.campus.dorm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量操作结果VO
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResultVO {
    
    /**
     * 总数
     */
    private Integer totalCount;
    
    /**
     * 成功数量
     */
    private Integer successCount;
    
    /**
     * 失败数量
     */
    private Integer failureCount;
    
    /**
     * 失败详情列表
     */
    private List<String> errors;
    
    /**
     * 操作描述
     */
    private String message;
    
    /**
     * 有学生入住的房间号列表（批量删除时使用）
     */
    private List<String> occupiedRoomNumbers;
}

