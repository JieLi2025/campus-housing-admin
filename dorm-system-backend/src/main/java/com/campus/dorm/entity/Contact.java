package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 联系人实体类
 * 对应数据库表：contact
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("contact")
public class Contact {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 联系人ID
     */
    @TableField("contact_id")
    private Long contactId;
    
    /**
     * 是否自动添加：0-否，1-是（学生自动添加的管理员）
     */
    @TableField("is_auto_added")
    private Integer isAutoAdded;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}

