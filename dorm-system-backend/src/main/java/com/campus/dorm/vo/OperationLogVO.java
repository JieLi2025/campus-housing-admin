package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志VO
 */
@Data
public class OperationLogVO {
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 操作时间
     */
    private LocalDateTime time;
    
    /**
     * 操作内容
     */
    private String action;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作状态
     */
    private String status;
    
    /**
     * 操作人
     */
    private String operator;
}

