package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.entity.RoomFacility;
import com.campus.dorm.service.RoomFacilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 房间设施控制器
 * 
 * @author Campus Dorm System
 * @date 2025-11-01
 */
@Slf4j
@RestController
@RequestMapping("/facility")
@RequiredArgsConstructor
public class RoomFacilityController {
    
    private final RoomFacilityService roomFacilityService;
    
    /**
     * 获取所有房间设施
     */
    @GetMapping("/list")
    public Result<List<RoomFacility>> getAllFacilities() {
        return roomFacilityService.getAllFacilities();
    }
    
    /**
     * 根据ID获取设施
     */
    @GetMapping("/{id}")
    public Result<RoomFacility> getFacilityById(@PathVariable Long id) {
        return roomFacilityService.getFacilityById(id);
    }
    
    /**
     * 新增房间设施
     */
    @PostMapping
    public Result<String> addFacility(@RequestBody RoomFacility facility) {
        return roomFacilityService.addFacility(facility);
    }
    
    /**
     * 更新房间设施
     */
    @PutMapping
    public Result<String> updateFacility(@RequestBody RoomFacility facility) {
        return roomFacilityService.updateFacility(facility);
    }
    
    /**
     * 删除房间设施
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteFacility(@PathVariable Long id) {
        return roomFacilityService.deleteFacility(id);
    }
    
    /**
     * 批量更新排序
     */
    @PutMapping("/sort")
    public Result<String> updateSortOrder(@RequestBody List<RoomFacility> facilities) {
        return roomFacilityService.updateSortOrder(facilities);
    }
    
    /**
     * 获取指定房型的默认设施列表
     */
    @GetMapping("/room-type/{roomType}")
    public Result<List<RoomFacility>> getFacilitiesByRoomType(@PathVariable String roomType) {
        return roomFacilityService.getFacilitiesByRoomType(roomType);
    }
    
    /**
     * 保存房型默认模板配置
     */
    @PostMapping("/room-type-template")
    public Result<String> saveRoomTypeTemplate(@RequestBody RoomTypeTemplateDTO dto) {
        return roomFacilityService.saveRoomTypeTemplate(dto.getRoomType(), dto.getFacilityIds());
    }
    
    /**
     * 房型模板DTO
     */
    @lombok.Data
    public static class RoomTypeTemplateDTO {
        private String roomType;
        private List<Long> facilityIds;
    }
}

