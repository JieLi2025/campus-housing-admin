package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.entity.DormGroup;
import com.campus.dorm.service.DormGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 宿舍组管理Controller（管理员端）
 * 
 * @author Campus Dorm System
 * @date 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/admin/dorm-groups")
@RequiredArgsConstructor
@Tag(name = "宿舍组管理（管理员端）", description = "管理员管理学生自组宿舍的接口")
public class DormGroupController {
    
    private final DormGroupService dormGroupService;
    
    /**
     * 获取宿舍组列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取宿舍组列表", description = "分页查询宿舍组，支持关键字搜索和状态筛选")
    public Result<?> getGroupList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        log.info("获取宿舍组列表：currentPage={}, pageSize={}, keyword={}, status={}", 
            currentPage, pageSize, keyword, status);
        return dormGroupService.getGroupList(currentPage, pageSize, keyword, status);
    }
    
    /**
     * 根据ID获取宿舍组详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取宿舍组详情", description = "根据ID查询宿舍组的详细信息")
    public Result<?> getGroupById(@PathVariable Long id) {
        log.info("获取宿舍组详情：id={}", id);
        return dormGroupService.getGroupById(id);
    }
    
    /**
     * 根据组长ID获取宿舍组
     */
    @GetMapping("/by-leader/{leaderId}")
    @Operation(summary = "根据组长ID获取宿舍组", description = "查询某个学生作为组长创建的宿舍组")
    public Result<?> getGroupsByLeaderId(@PathVariable Long leaderId) {
        log.info("根据组长ID获取宿舍组：leaderId={}", leaderId);
        return dormGroupService.getGroupsByLeaderId(leaderId);
    }
    
    /**
     * 创建宿舍组
     */
    @PostMapping
    @Operation(summary = "创建宿舍组", description = "创建新的宿舍组")
    public Result<?> createGroup(@RequestBody DormGroup dormGroup) {
        log.info("创建宿舍组：{}", dormGroup);
        return dormGroupService.createGroup(dormGroup);
    }
    
    /**
     * 更新宿舍组信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新宿舍组", description = "更新宿舍组的基本信息")
    public Result<?> updateGroup(
            @PathVariable Long id,
            @RequestBody DormGroup dormGroup) {
        log.info("更新宿舍组：id={}, data={}", id, dormGroup);
        return dormGroupService.updateGroup(id, dormGroup);
    }
    
    /**
     * 删除宿舍组
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除宿舍组", description = "删除指定的宿舍组")
    public Result<?> deleteGroup(@PathVariable Long id) {
        log.info("删除宿舍组：id={}", id);
        return dormGroupService.deleteGroup(id);
    }
    
    /**
     * 选择房间
     */
    @PostMapping("/{id}/select-room")
    @Operation(summary = "选择房间", description = "为宿舍组选择具体的房间")
    public Result<?> selectRoom(
            @PathVariable Long id,
            @RequestBody Map<String, Long> params) {
        Long roomId = params.get("roomId");
        Long buildingId = params.get("buildingId");
        log.info("选择房间：id={}, roomId={}, buildingId={}", id, roomId, buildingId);
        return dormGroupService.selectRoom(id, roomId, buildingId);
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据", description = "获取宿舍组的统计信息")
    public Result<?> getStatistics() {
        log.info("获取宿舍组统计数据");
        return dormGroupService.getStatistics();
    }
    
    /**
     * 取消房间选择（管理员操作）
     */
    @PostMapping("/{id}/cancel-room-selection")
    @Operation(summary = "取消房间选择", description = "管理员取消宿舍组的房间选择，释放房间以便重新选择")
    public Result<?> cancelRoomSelection(@PathVariable Long id) {
        log.info("管理员取消房间选择：groupId={}", id);
        return dormGroupService.cancelRoomSelection(id);
    }
    
    /**
     * 更新宿舍组公告
     */
    @PutMapping("/{id}/announcement")
    @Operation(summary = "更新宿舍组公告", description = "更新宿舍组的群公告")
    public Result<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        String announcement = params.get("announcement");
        log.info("更新宿舍组公告：id={}, announcement={}", id, announcement);
        return dormGroupService.updateAnnouncement(id, announcement);
    }
}

