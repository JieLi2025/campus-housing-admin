package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.BatchCreateRoomDTO;
import com.campus.dorm.dto.DormRoomDTO;
import com.campus.dorm.service.DormRoomService;
import com.campus.dorm.vo.DormRoomVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 宿舍房间管理Controller
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@RestController
@RequestMapping("/admin/rooms")
@RequiredArgsConstructor
@Tag(name = "宿舍房间管理", description = "宿舍房间CRUD接口")
@Slf4j
public class DormRoomController {
    
    private final DormRoomService roomService;
    
    /**
     * 获取房间列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取房间列表", description = "分页获取所有房间列表，支持关键词搜索")
    public Result<?> getRoomList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String keyword) {
        log.info("获取房间列表：currentPage={}, pageSize={}, buildingId={}, keyword={}", 
            currentPage, pageSize, buildingId, keyword);
        return roomService.getRoomList(currentPage, pageSize, buildingId, keyword);
    }
    
    /**
     * 创建房间
     */
    @PostMapping
    @Operation(summary = "创建房间", description = "创建单个宿舍房间")
    public Result<?> createRoom(@Validated @RequestBody DormRoomDTO dto) {
        log.info("接收到创建房间请求：{}", dto);
        return roomService.createRoom(dto);
    }
    
    /**
     * 批量创建房间
     */
    @PostMapping("/batch")
    @Operation(summary = "批量创建房间", description = "批量创建连续房间号的宿舍")
    public Result<Map<String, Integer>> batchCreateRooms(@Validated @RequestBody BatchCreateRoomDTO dto) {
        log.info("接收到批量创建房间请求：{}", dto);
        return roomService.batchCreateRooms(dto);
    }
    
    /**
     * 更新房间
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新房间", description = "更新房间信息")
    public Result<?> updateRoom(
            @Parameter(description = "房间ID") @PathVariable Long id,
            @Validated @RequestBody DormRoomDTO dto) {
        log.info("接收到更新房间请求：id={}, dto={}", id, dto);
        dto.setId(id);
        return roomService.updateRoom(dto);
    }
    
    /**
     * 删除房间
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除房间", description = "删除指定房间（房间必须为空）")
    public Result<?> deleteRoom(@Parameter(description = "房间ID") @PathVariable Long id) {
        log.info("接收到删除房间请求：id={}", id);
        return roomService.deleteRoom(id);
    }
    
    /**
     * 批量删除房间
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除房间", description = "批量删除房间（房间必须为空）")
    public Result<Map<String, Integer>> batchDeleteRooms(@RequestBody List<Long> roomIds) {
        log.info("接收到批量删除房间请求：ids={}", roomIds);
        return roomService.batchDeleteRooms(roomIds);
    }
    
    /**
     * 批量更新房间类型
     */
    @PutMapping("/batch-update-type")
    @Operation(summary = "批量更新房间类型", description = "批量修改房间的类型和床位数")
    public Result<Map<String, Integer>> batchUpdateRoomType(
            @RequestParam List<Long> roomIds,
            @RequestParam String roomType,
            @RequestParam(required = false) Integer bedCount) {
        log.info("接收到批量更新房间类型请求：ids={}, roomType={}, bedCount={}", roomIds, roomType, bedCount);
        return roomService.batchUpdateRoomType(roomIds, roomType, bedCount);
    }
    
    /**
     * 获取楼栋的所有房间
     */
    @GetMapping("/building/{buildingId}")
    @Operation(summary = "获取楼栋的所有房间", description = "查询指定楼栋的所有房间列表")
    public Result<List<DormRoomVO>> getRoomsByBuilding(
            @Parameter(description = "楼栋ID") @PathVariable Long buildingId) {
        log.info("接收到获取楼栋房间请求：buildingId={}", buildingId);
        return roomService.getRoomsByBuilding(buildingId);
    }
    
    /**
     * 获取房间详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取房间详情", description = "查询指定房间的详细信息")
    public Result<DormRoomVO> getRoomDetail(@Parameter(description = "房间ID") @PathVariable Long id) {
        log.info("接收到获取房间详情请求：id={}", id);
        return roomService.getRoomDetail(id);
    }
}
