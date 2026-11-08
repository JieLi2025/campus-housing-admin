package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.MaterialTemplateDTO;
import com.campus.dorm.entity.MaterialTemplate;
import com.campus.dorm.service.BuildingService;
import com.campus.dorm.service.DormGroupService;
import com.campus.dorm.service.MaterialTemplateService;
import com.campus.dorm.vo.BuildingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置Controller
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Slf4j
@RestController
@RequestMapping("/super-admin/config")
@Tag(name = "系统配置管理", description = "超级管理员系统配置接口")
public class SystemConfigController {
    
    @Autowired
    private MaterialTemplateService materialTemplateService;
    
    @Autowired
    private BuildingService buildingService;
    
    @Autowired
    private DormGroupService dormGroupService;
    
    // ==================== 宿舍物资模板配置 ====================
    
    @GetMapping("/materials")
    @Operation(summary = "获取所有物资模板")
    public Result<List<MaterialTemplate>> getAllMaterialTemplates() {
        return materialTemplateService.getAllTemplates();
    }
    
    @GetMapping("/materials/by-type")
    @Operation(summary = "根据楼栋ID和房间类型获取物资模板")
    public Result<MaterialTemplate> getMaterialTemplateByType(
            @Parameter(description = "楼栋ID（优先使用）") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "楼栋类型（向后兼容）") @RequestParam(required = false) String buildingType,
            @Parameter(description = "房间类型") @RequestParam(required = false) String roomType) {
        return materialTemplateService.getTemplateByType(buildingId, buildingType, roomType);
    }
    
    @PostMapping("/materials")
    @Operation(summary = "创建物资模板")
    public Result<?> createMaterialTemplate(
            @Parameter(description = "物资模板DTO") @RequestBody @Validated MaterialTemplateDTO dto) {
        return materialTemplateService.createTemplate(dto);
    }
    
    @PutMapping("/materials")
    @Operation(summary = "更新物资模板")
    public Result<?> updateMaterialTemplate(
            @Parameter(description = "物资模板DTO") @RequestBody @Validated MaterialTemplateDTO dto) {
        return materialTemplateService.updateTemplate(dto);
    }
    
    @DeleteMapping("/materials/{id}")
    @Operation(summary = "删除物资模板")
    public Result<?> deleteMaterialTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        return materialTemplateService.deleteTemplate(id);
    }
    
    // ==================== 楼栋数据查询（用于配置页面） ====================
    
    @GetMapping("/buildings")
    @Operation(summary = "获取所有楼栋列表", description = "用于系统配置页面的楼栋下拉选择")
    public Result<List<BuildingVO>> getAllBuildings() {
        return buildingService.getAllBuildings();
    }
    
    // ==================== 数据迁移 ====================
    
    @PostMapping("/migrate-bed-numbers")
    @Operation(summary = "迁移床位号数据", description = "为现有宿舍组成员添加 bedNumber 字段（按位置分配 1, 2, 3...）")
    public Result<?> migrateBedNumbers() {
        log.info("开始执行床位号数据迁移");
        return dormGroupService.migrateBedNumbers();
    }
}

