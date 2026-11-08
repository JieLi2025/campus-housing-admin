package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.entity.BuildingMajorAssignment;
import com.campus.dorm.service.BuildingMajorAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 楼栋专业分配管理Controller
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@RestController
@RequestMapping("/super-admin/major-assignments")
@RequiredArgsConstructor
@Tag(name = "楼栋专业分配管理", description = "超级管理员专业分配相关接口")
@Slf4j
public class BuildingMajorAssignmentController {
    
    private final BuildingMajorAssignmentService assignmentService;
    
    /**
     * 批量设置楼栋的专业分配
     */
    @PostMapping("/building/{buildingId}")
    @Operation(summary = "设置楼栋的专业分配", description = "为指定楼栋批量设置专业分配，自动覆盖旧分配")
    public Result<?> batchSaveAssignments(
            @Parameter(description = "楼栋ID") @PathVariable Long buildingId,
            @RequestBody List<Map<String, Object>> assignments) {
        log.info("批量保存楼栋{}的专业分配", buildingId);
        return assignmentService.batchSaveAssignments(buildingId, assignments);
    }
    
    /**
     * 获取楼栋的专业分配列表
     */
    @GetMapping("/building/{buildingId}")
    @Operation(summary = "获取楼栋的专业分配", description = "查询指定楼栋的所有专业分配")
    public Result<List<BuildingMajorAssignment>> getAssignmentsByBuilding(
            @Parameter(description = "楼栋ID") @PathVariable Long buildingId) {
        log.info("查询楼栋{}的专业分配", buildingId);
        return assignmentService.getAssignmentsByBuilding(buildingId);
    }
    
    /**
     * 获取所有专业分配（Map格式）
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有专业分配", description = "以Map格式返回所有楼栋的专业分配")
    public Result<Map<Long, List<Map<String, Object>>>> getAllAssignments() {
        log.info("查询所有专业分配");
        return assignmentService.getAllAssignmentsMap();
    }
    
    /**
     * 删除专业分配
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除专业分配", description = "删除指定的专业分配记录")
    public Result<?> deleteAssignment(@Parameter(description = "分配ID") @PathVariable Long id) {
        log.info("删除专业分配：id={}", id);
        return assignmentService.deleteAssignment(id);
    }
    
    /**
     * 获取各专业剩余学生数
     */
    @GetMapping("/remaining-students")
    @Operation(summary = "获取专业剩余学生数", description = "返回各专业（按性别）的剩余可分配学生数")
    public Result<Map<String, Map<String, Integer>>> getRemainingStudents() {
        log.info("查询专业剩余学生数");
        return assignmentService.getRemainingStudentsByMajor();
    }
}
