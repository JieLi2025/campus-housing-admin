package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 操作日志创建DTO
 */
@Data
public class OperationLogDTO {
    
    /**
     * 操作类型
     */
    @NotBlank(message = "操作类型不能为空")
    private String operationType;
    
    /**
     * 操作描述
     */
    @NotBlank(message = "操作描述不能为空")
    private String operationDesc;
    
    /**
     * 请求方法: GET/POST/PUT/DELETE
     */
    private String requestMethod;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 操作状态: 1-成功, 0-失败
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}

