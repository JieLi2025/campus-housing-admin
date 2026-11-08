package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 宿舍物资模板DTO
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Data
public class MaterialTemplateDTO {
    
    /**
     * 模板ID（更新时需要）
     */
    private Long id;
    
    /**
     * 楼栋ID（用于按具体楼栋隔离配置）
     */
    private Long buildingId;
    
    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    /**
     * 楼栋类型（保留，向后兼容）
     */
    private String buildingType;
    
    /**
     * 房间类型
     */
    private String roomType;
    
    /**
     * 物资列表（JSON格式）
     */
    private String materials;
}

