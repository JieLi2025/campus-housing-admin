package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告DTO（创建/更新）
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class AnnouncementDTO {
    
    /**
     * 公告标题
     */
    @NotBlank(message = "公告标题不能为空")
    private String title;
    
    /**
     * 公告副标题
     */
    private String subtitle;
    
    /**
     * 公告内容
     */
    @NotBlank(message = "公告内容不能为空")
    private String content;
    
    /**
     * 额外要点
     */
    private String extraPoints;
    
    /**
     * 目标楼栋类型
     */
    private String buildingType;
    
    /**
     * 目标楼栋ID列表（如果为空或包含"ALL"表示全部楼栋）
     */
    private List<Long> buildingIds;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireDate;
    
    /**
     * 展示选项
     */
    private List<String> displayOptions;
    
    /**
     * 状态：已发布、已归档（统一使用中文）
     */
    private String status;
}

