package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 通知创建/编辑DTO
 */
@Data
public class NotificationDTO {
    
    /**
     * 通知ID（编辑时需要）
     */
    private Long id;
    
    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;
    
    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String message;
    
    /**
     * 通知类型: info/warning/success/error
     */
    @NotBlank(message = "通知类型不能为空")
    private String type;
    
    /**
     * 图标名称
     */
    private String icon;
    
    /**
     * 目标角色: SUPER_ADMIN/ADMIN/STUDENT，NULL表示全部
     */
    private String targetRole;
    
    /**
     * 目标用户ID，NULL表示群发
     */
    private Long targetUserId;
    
    /**
     * 目标楼栋ID，用于楼栋公告（只发给该楼栋的学生）
     */
    private Long targetBuildingId;
    
    /**
     * 关联业务ID（如申请ID、访客ID、公告ID）
     */
    private Long relatedId;
}

