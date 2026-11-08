package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.*;
import com.campus.dorm.service.IStudentAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 学生分配统计Controller
 * 
 * @author Campus Dorm System
 * @date 2025-10-31
 */
@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/admin/allocation")
@RequiredArgsConstructor
@Tag(name = "学生分配统计管理", description = "学生分配统计相关接口")
public class StudentAllocationController {
    
    private final IStudentAllocationService allocationService;
    
    /**
     * 获取学生分配统计数据（按管理员负责的楼栋过滤）
     * @param buildingIds 楼栋ID列表（逗号分隔），为空则统计所有楼栋
     * @return 统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取学生分配统计数据", description = "获取待分配学生、未满宿舍组、可用床位等统计信息，支持按楼栋过滤")
    public Result<AllocationStatisticsDTO> getStatistics(@RequestParam(required = false) String buildingIds) {
        log.info("请求获取学生分配统计数据，楼栋IDs: {}", buildingIds);
        
        List<Long> buildingIdList = parseBuildingIds(buildingIds);
        AllocationStatisticsDTO statistics = allocationService.getAllocationStatistics(buildingIdList);
        return Result.success(statistics);
    }
    
    /**
     * 智能分配学生
     */
    @PostMapping("/intelligent-assign")
    @Operation(summary = "智能分配学生", description = "根据策略自动为学生分配宿舍")
    public Result<AllocationResultDTO> intelligentAssign(@RequestBody IntelligentAssignRequest request) {
        System.out.println("==========================================");
        System.out.println("🎯🎯🎯 Controller收到批量智能分配请求 🎯🎯🎯");
        System.out.println("请求参数: " + request);
        System.out.println("📋 studentIds: " + (request.getStudentIds() != null ? request.getStudentIds().size() + "个" : "null"));
        System.out.println("🏢 buildingIds: " + request.getBuildingIds());
        System.out.println("🏢 buildingIds是否为空: " + (request.getBuildingIds() == null || request.getBuildingIds().isEmpty()));
        System.out.println("==========================================");
        log.info("智能分配请求: {}", request);
        AllocationResultDTO result = allocationService.intelligentAssign(request);
        System.out.println("📊📊📊 Service返回结果: " + result);
        return Result.success(result);
    }
    
    /**
     * 手动分配学生到宿舍组
     */
    @PostMapping("/assign-to-group")
    @Operation(summary = "分配学生到宿舍组", description = "管理员手动将学生添加到未满的宿舍组")
    public Result<AllocationResultDTO> assignToGroup(@RequestBody @Valid AssignToGroupRequest request) {
        log.info("分配学生到宿舍组请求: {}", request);
        AllocationResultDTO result = allocationService.assignToGroup(request);
        return Result.success(result);
    }
    
    /**
     * 手动分配学生到房间
     */
    @PostMapping("/assign-to-room")
    @Operation(summary = "分配学生到房间", description = "管理员直接将学生分配到指定房间")
    public Result<AllocationResultDTO> assignToRoom(@RequestBody @Valid AssignToRoomRequest request) {
        log.info("分配学生到房间请求: {}", request);
        AllocationResultDTO result = allocationService.assignToRoom(request);
        return Result.success(result);
    }
    
    /**
     * 按班级批量处理
     */
    @PostMapping("/batch-process-by-class")
    @Operation(summary = "按班级批量处理", description = "自动处理指定班级的所有未分配学生")
    public Result<AllocationResultDTO> batchProcessByClass(
            @RequestParam String className,
            @RequestParam(required = false) String buildingIds) {
        log.info("按班级批量处理，班级: {}, 楼栋IDs: {}", className, buildingIds);
        
        List<Long> buildingIdList = parseBuildingIds(buildingIds);
        AllocationResultDTO result = allocationService.batchProcessByClass(className, buildingIdList);
        return Result.success(result);
    }
    
    /**
     * 验证分配结果
     */
    @GetMapping("/validate")
    @Operation(summary = "验证分配结果", description = "检查分配是否完整，是否存在问题")
    public Result<AllocationValidationDTO> validateAllocation(
            @RequestParam(required = false) String buildingIds) {
        log.info("验证分配结果，楼栋IDs: {}", buildingIds);
        
        List<Long> buildingIdList = parseBuildingIds(buildingIds);
        AllocationValidationDTO result = allocationService.validateAllocation(buildingIdList);
        return Result.success(result);
    }
    
    /**
     * 完成分配
     * @param buildingIds 楼栋ID列表（可选，如果不传则处理所有楼栋）
     */
    @PostMapping("/complete")
    @Operation(summary = "完成分配", description = "将宿舍信息同步到学生表")
    public Result<?> completeAllocation(@RequestParam(required = false) List<Long> buildingIds) {
        log.info("完成分配，同步宿舍信息到学生表，楼栋ID列表: {}", buildingIds);
        return allocationService.completeAllocation(buildingIds);
    }
    
    /**
     * 解析楼栋ID列表
     */
    private List<Long> parseBuildingIds(String buildingIds) {
        if (buildingIds == null || buildingIds.trim().isEmpty()) {
            return null;
        }
        
        return java.util.Arrays.stream(buildingIds.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .collect(java.util.stream.Collectors.toList());
    }
}

