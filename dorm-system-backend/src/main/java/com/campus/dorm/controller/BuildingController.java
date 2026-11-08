package com.campus.dorm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.BuildingDTO;
import com.campus.dorm.service.BuildingService;
import com.campus.dorm.vo.BuildingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宿舍楼管理Controller
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Slf4j
@RestController
@RequestMapping("/admin/buildings")
@RequiredArgsConstructor
public class BuildingController {
    
    private final BuildingService buildingService;
    
    /**
     * 获取宿舍楼列表（分页）
     */
    @GetMapping
    public Result<Page<BuildingVO>> getBuildingList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String buildingType) {
        return buildingService.getBuildingList(currentPage, pageSize, keyword, buildingType);
    }
    
    /**
     * 获取所有宿舍楼（不分页，用于下拉选择）
     */
    @GetMapping("/all")
    public Result<List<BuildingVO>> getAllBuildings() {
        return buildingService.getAllBuildings();
    }
    
    /**
     * 获取宿舍楼详情
     */
    @GetMapping("/{buildingId}")
    public Result<BuildingVO> getBuildingDetail(@PathVariable Long buildingId) {
        return buildingService.getBuildingDetail(buildingId);
    }
    
    /**
     * 创建宿舍楼
     */
    @PostMapping
    public Result<?> createBuilding(@RequestBody BuildingDTO dto) {
        return buildingService.createBuilding(dto);
    }
    
    /**
     * 更新宿舍楼
     */
    @PutMapping
    public Result<?> updateBuilding(@RequestBody BuildingDTO dto) {
        return buildingService.updateBuilding(dto);
    }
    
    /**
     * 删除宿舍楼
     */
    @DeleteMapping("/{buildingId}")
    public Result<?> deleteBuilding(@PathVariable Long buildingId) {
        return buildingService.deleteBuilding(buildingId);
    }
    
    /**
     * 为楼栋分配管理员
     * @param buildingId 楼栋ID
     * @param adminName 管理员姓名
     * @param adminPhone 管理员电话（可选）
     */
    @PostMapping("/{buildingId}/assign-manager")
    public Result<?> assignManager(
            @PathVariable Long buildingId,
            @RequestParam String adminName,
            @RequestParam(required = false) String adminPhone) {
        log.info("为楼栋{}分配管理员：adminName={}", buildingId, adminName);
        return buildingService.assignManager(buildingId, adminName, adminPhone);
    }
    
    /**
     * 为管理员批量分配楼栋
     * @param adminName 管理员姓名
     * @param adminPhone 管理员电话
     * @param buildingIds 楼栋ID列表
     */
    @PostMapping("/assign-buildings")
    public Result<?> assignBuildingsToManager(
            @RequestParam String adminName,
            @RequestParam(required = false) String adminPhone,
            @RequestBody List<Long> buildingIds) {
        log.info("为管理员{}分配楼栋：buildingIds={}", adminName, buildingIds);
        return buildingService.assignBuildingsToManager(adminName, adminPhone, buildingIds);
    }
}

