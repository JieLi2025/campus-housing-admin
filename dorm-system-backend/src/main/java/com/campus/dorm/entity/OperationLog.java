package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("operation_log")
public class OperationLog {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作人ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 操作人用户名 (对应数据库 user_name 字段)
     */
    @TableField("user_name")
    private String userName;
    
    /**
     * 操作类型 (对应数据库 operation_type 字段)
     */
    @TableField("operation_type")
    private String operationType;
    
    /**
     * 操作描述 (对应数据库 operation_desc 字段)
     */
    @TableField("operation_desc")
    private String operationDesc;
    
    /**
     * 操作状态: 1-成功, 0-失败
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 错误信息 (对应数据库 error_message 字段)
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 创建时间 (对应数据库 created_at 字段)
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

