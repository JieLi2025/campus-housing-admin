package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员信息实体类
 * 对应数据库表：admin
 * 与 sys_user 表通过 user_id 关联，采用最简设计
 * 暂不存储业务字段，仅作为扩展预留，后续可按需添加字段
 * 注意：admin表已删除created_at和updated_at字段，不继承BaseEntity
 * 
 * @author Campus Dorm System
 * @date 2025-11-02
 */
@Data
@TableName("admin")
public class Admin implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 管理员ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联用户ID（sys_user表的主键）
     */
    private Long userId;
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 职位
     */
    private String position;
    
    /**
     * 办公地点
     */
    private String officeLocation;
    
    /**
     * 工作时间
     */
    private String workHours;
    
    /**
     * 个人简介
     */
    private String bio;
    
    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}

