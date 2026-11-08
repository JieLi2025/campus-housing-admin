package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 宿舍物资模板实体
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Data
@TableName("material_template")
public class MaterialTemplate {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 楼栋ID（用于按具体楼栋隔离配置）
     */
    private Long buildingId;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 楼栋类型（如：男生楼、女生楼）
     */
    private String buildingType;
    
    /**
     * 房间类型（如：4人间、6人间）
     */
    private String roomType;
    
    /**
     * 物资列表（JSON格式）
     */
    private String materials;
}

