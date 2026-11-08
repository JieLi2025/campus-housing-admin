package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.entity.Building;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.service.BuildingService;
import com.campus.dorm.service.RoomSelectionService;
import com.campus.dorm.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房间选择Controller（管理员端）
 * 数据源：building 表的阶段配置字段
 * 
 * @author Campus Dorm System
 * @date 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/admin/room-selection")
@RequiredArgsConstructor
@Tag(name = "房间选择管理", description = "管理员管理房间选择功能")
public class RoomSelectionController {
    
    private final RoomSelectionService roomSelectionService;
    private final BuildingService buildingService;
    private final BuildingMapper buildingMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    
    /**
     * 获取当前用户ID（从JWT Token）
     */
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取房间选择配置
     * 兼容前端期望的格式：{ isOpen: 1/0, deadline: "..." }
     */
    @GetMapping("/config")
    @Operation(summary = "获取房间选择配置", description = "获取当前房间选择配置信息")
    public Result<?> getConfig(@RequestParam(required = false) Long buildingId) {
        log.info("获取房间选择配置：buildingId={}", buildingId);

        Building building = null;
        
        // 如果指定了楼栋ID，获取该楼栋配置
        if (buildingId != null) {
            building = buildingMapper.selectById(buildingId);
        }
        
        // 如果没有指定楼栋或楼栋不存在，获取第一个有配置的楼栋
        if (building == null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.isNotNull(Building::getCurrentPhase)
                   .orderByDesc(Building::getId)
                   .last("LIMIT 1");
            building = buildingMapper.selectOne(wrapper);
        }
        
        Map<String, Object> config = new HashMap<>();
        
        if (building == null || building.getCurrentPhase() == null) {
            // 如果没有配置，返回默认值
            config.put("isOpen", 0);
            config.put("deadline", null);
            return Result.success(config);
        }
        
        // 判断是否开放：如果当前阶段是 room_selection，则认为是开放的
        boolean isOpen = "room_selection".equals(building.getCurrentPhase());
        config.put("isOpen", isOpen ? 1 : 0);
        
        // 设置截止时间
        LocalDateTime deadline = building.getRoomSelectionEnd();
        if (deadline != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            config.put("deadline", deadline.format(formatter));
        } else {
            config.put("deadline", null);
        }
        
        config.put("buildingId", building.getId());
        config.put("buildingName", building.getBuildingName());
        config.put("currentPhase", building.getCurrentPhase());
        
        return Result.success(config);
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据", description = "获取房间选择的统计信息")
    public Result<?> getStatistics() {
        log.info("管理员获取房间选择统计");
        return roomSelectionService.getStatistics();
    }

    @PostMapping("/open")
    @Operation(summary = "开放房间选择", description = "开放房间选择并设置截止时间")
    public Result<?> openRoomSelection(@RequestBody Map<String, Object> params) {
        log.info("开放房间选择：{}", params);
        String deadlineStr = (String) params.get("deadline");
        Long buildingId = params.get("buildingId") != null ? 
            Long.valueOf(params.get("buildingId").toString()) : null;
        
        // 如果没有指定楼栋，获取第一个楼栋
        if (buildingId == null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.last("LIMIT 1");
            Building firstBuilding = buildingMapper.selectOne(wrapper);
            if (firstBuilding != null) {
                buildingId = firstBuilding.getId();
            } else {
                return Result.error("没有可用的楼栋");
            }
        }
        
        LocalDateTime deadline = null;
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                deadline = LocalDateTime.parse(deadlineStr, formatter);
            } catch (Exception e) {
                try {
                    deadline = LocalDateTime.parse(deadlineStr);
                } catch (Exception ex) {
                    log.error("无法解析截止时间", ex);
                }
            }
        }
        
        return buildingService.updatePhaseConfig(buildingId, "room_selection", deadline);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭房间选择", description = "关闭房间选择")
    public Result<?> closeRoomSelection(@RequestBody Map<String, Object> params) {
        log.info("关闭房间选择：{}", params);
        Long buildingId = params.get("buildingId") != null ? 
            Long.valueOf(params.get("buildingId").toString()) : null;
        
        // 如果没有指定楼栋，获取当前处于room_selection阶段的楼栋
        if (buildingId == null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(Building::getCurrentPhase, "room_selection")
                   .last("LIMIT 1");
            Building building = buildingMapper.selectOne(wrapper);
            if (building != null) {
                buildingId = building.getId();
            } else {
                return Result.error("没有处于选房阶段的楼栋");
            }
        }
        
        // 关闭选房，进入管理员分配阶段，清除截止时间
        return buildingService.updatePhaseConfig(buildingId, "admin_allocation", null);
    }

    @PutMapping("/deadline")
    @Operation(summary = "修改截止时间", description = "修改房间选择截止时间")
    public Result<?> updateDeadline(@RequestBody Map<String, Object> params) {
        log.info("修改截止时间：{}", params);
        String deadlineStr = (String) params.get("deadline");
        Long buildingId = params.get("buildingId") != null ? 
            Long.valueOf(params.get("buildingId").toString()) : null;
        
        if (buildingId == null) {
            return Result.error("请指定楼栋ID");
        }
        
        LocalDateTime deadline = null;
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                deadline = LocalDateTime.parse(deadlineStr, formatter);
            } catch (Exception e) {
                try {
                    deadline = LocalDateTime.parse(deadlineStr);
                } catch (Exception ex) {
                    log.error("时间格式错误", ex);
                    return Result.error("时间格式错误");
                }
            }
        }
        
        return buildingService.updatePhaseConfig(buildingId, null, deadline);
    }

    // ==================== 楼栋阶段控制接口 ====================
    
    /**
     * 按楼栋获取阶段配置
     */
    @GetMapping("/building/{buildingId}/phase")
    @Operation(summary = "按楼栋获取阶段配置", description = "获取指定楼栋的选房阶段配置")
    public Result<?> getBuildingPhase(@PathVariable Long buildingId) {
        log.info("获取楼栋阶段配置：buildingId={}", buildingId);
        return buildingService.getPhaseConfig(buildingId);
    }
    
    /**
     * 按楼栋更新阶段配置
     */
    @PutMapping("/building/{buildingId}/phase")
    @Operation(summary = "按楼栋更新阶段配置", description = "更新指定楼栋的选房阶段配置")
    public Result<?> updateBuildingPhase(@PathVariable Long buildingId, 
                                          @RequestBody Map<String, Object> params) {
        log.info("更新楼栋阶段配置：buildingId={}, params={}", buildingId, params);
        
        String currentPhase = (String) params.get("currentPhase");
        String roomSelectionEndStr = (String) params.get("roomSelectionEnd");
        
        LocalDateTime roomSelectionEnd = parseDateTime(roomSelectionEndStr);
        
        return buildingService.updatePhaseConfig(buildingId, currentPhase, roomSelectionEnd);
    }
    
    /**
     * 按楼栋切换阶段
     */
    @PostMapping("/building/{buildingId}/switch-phase")
    @Operation(summary = "按楼栋切换阶段", description = "切换指定楼栋的选房阶段")
    public Result<?> switchBuildingPhase(@PathVariable Long buildingId, 
                                          @RequestParam String newPhase) {
        log.info("切换楼栋阶段：buildingId={}, newPhase={}", buildingId, newPhase);
        return buildingService.switchPhase(buildingId, newPhase);
    }
    
    /**
     * 获取所有楼栋的阶段配置列表
     */
    @GetMapping("/all-phases")
    @Operation(summary = "获取所有阶段配置", description = "获取所有楼栋的阶段配置列表")
    public Result<?> getAllPhases() {
        log.info("获取所有阶段配置");
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByAsc(Building::getBuildingCode);
        
        List<Building> buildings = buildingMapper.selectList(wrapper);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Building building : buildings) {
            Map<String, Object> config = new HashMap<>();
            config.put("buildingId", building.getId());
            config.put("buildingName", building.getBuildingName());
            config.put("currentPhase", building.getCurrentPhase());
            config.put("roomSelectionEnd", building.getRoomSelectionEnd());
            result.add(config);
        }
        
        return Result.success(result);
    }
    
    /**
     * 切换选房阶段（全局，应用到所有楼栋）
     */
    @PostMapping("/switch-phase")
    @Operation(summary = "切换选房阶段", description = "切换选房阶段（应用到所有楼栋）")
    public Result<?> switchPhase(@RequestParam String newPhase) {
        log.info("管理员切换选房阶段：{}", newPhase);
        
        // 获取所有楼栋
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Building> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        List<Building> buildings = buildingMapper.selectList(wrapper);
        
        int successCount = 0;
        for (Building building : buildings) {
            Result<?> result = buildingService.switchPhase(building.getId(), newPhase);
            if (result.getCode() == 200) {
                successCount++;
            }
        }
        
        return Result.success("已切换 " + successCount + " 个楼栋的阶段");
    }
    
    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (Exception ex) {
                log.warn("无法解析时间：{}", dateTimeStr);
                return null;
            }
        }
    }
}
