package com.campus.dorm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请码生成结果VO
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeGenerateResultVO {
    
    /**
     * 总学生数
     */
    private Integer totalCount;
    
    /**
     * 成功生成数
     */
    private Integer successCount;
    
    /**
     * 已有邀请码数（跳过）
     */
    private Integer skippedCount;
    
    /**
     * 失败数
     */
    private Integer failureCount;
    
    /**
     * 提示信息
     */
    private String message;
}

