package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统通知实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("notification")
public class Notification {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 接收用户ID (对应数据库 user_id 字段)
     * NULL表示广播通知（所有人可见）
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 通知内容 (对应数据库 content 字段)
     */
    @TableField("content")
    private String content;
    
    /**
     * 通知类型: system/application/visitor
     */
    @TableField("type")
    private String type;
    
    /**
     * 关联ID (如申请ID、访客ID)
     */
    @TableField("related_id")
    private Long relatedId;
    
    /**
     * 是否已读: 0-未读, 1-已读
     */
    @TableField("is_read")
    private Boolean isRead;
    
    /**
     * 创建时间 (对应数据库 created_at 字段)
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

