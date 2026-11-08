package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.RoomFacility;
import com.campus.dorm.mapper.RoomFacilityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 房间设施服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomFacilityService {
    
    private final RoomFacilityMapper roomFacilityMapper;
    
    /**
     * 获取所有房间设施
     */
    public Result<List<RoomFacility>> getAllFacilities() {
        log.info("获取所有房间设施");
        
        LambdaQueryWrapper<RoomFacility> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(RoomFacility::getSortOrder);
        
        List<RoomFacility> list = roomFacilityMapper.selectList(wrapper);
        log.info("查询到{}个房间设施", list.size());
        
        return Result.success(list);
    }
    
    /**
     * 根据ID获取设施
     */
    public Result<RoomFacility> getFacilityById(Long id) {
        log.info("根据ID获取设施：id={}", id);
        
        RoomFacility facility = roomFacilityMapper.selectById(id);
        if (facility == null) {
            return Result.error("设施不存在");
        }
        
        return Result.success(facility);
    }
    
    /**
     * 新增房间设施
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> addFacility(RoomFacility facility) {
        log.info("新增房间设施：{}", facility);
        
        // 检查设施名称是否已存在
        LambdaQueryWrapper<RoomFacility> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomFacility::getFacilityName, facility.getFacilityName());
        Long count = roomFacilityMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.error("设施名称已存在，请使用不同的名称");
        }
        
        // 如果没有设置排序号，设为最大值+1
        if (facility.getSortOrder() == null) {
            LambdaQueryWrapper<RoomFacility> maxWrapper = new LambdaQueryWrapper<>();
            maxWrapper.orderByDesc(RoomFacility::getSortOrder);
            maxWrapper.last("LIMIT 1");
            RoomFacility maxFacility = roomFacilityMapper.selectOne(maxWrapper);
            facility.setSortOrder(maxFacility != null ? maxFacility.getSortOrder() + 1 : 1);
        }
        
        roomFacilityMapper.insert(facility);
        return Result.success("新增成功");
    }
    
    /**
     * 更新房间设施
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateFacility(RoomFacility facility) {
        log.info("更新房间设施：{}", facility);
        
        // 检查设施是否存在
        RoomFacility existFacility = roomFacilityMapper.selectById(facility.getId());
        if (existFacility == null) {
            return Result.error("设施不存在");
        }
        
        // 如果修改了设施名称，检查新名称是否已被占用
        if (!existFacility.getFacilityName().equals(facility.getFacilityName())) {
            LambdaQueryWrapper<RoomFacility> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RoomFacility::getFacilityName, facility.getFacilityName());
            wrapper.ne(RoomFacility::getId, facility.getId());
            Long count = roomFacilityMapper.selectCount(wrapper);
            if (count > 0) {
                return Result.error("设施名称已存在，请使用不同的名称");
            }
        }
        
        roomFacilityMapper.updateById(facility);
        return Result.success("更新成功");
    }
    
    /**
     * 删除房间设施
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteFacility(Long id) {
        log.info("删除房间设施：id={}", id);
        
        RoomFacility facility = roomFacilityMapper.selectById(id);
        if (facility == null) {
            return Result.error("设施不存在");
        }
        
        roomFacilityMapper.deleteById(id);
        return Result.success("删除成功");
    }
    
    /**
     * 批量更新排序
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateSortOrder(List<RoomFacility> facilities) {
        log.info("批量更新设施排序：共{}个设施", facilities.size());
        
        for (RoomFacility facility : facilities) {
            RoomFacility updateFacility = new RoomFacility();
            updateFacility.setId(facility.getId());
            updateFacility.setSortOrder(facility.getSortOrder());
            roomFacilityMapper.updateById(updateFacility);
        }
        
        return Result.success("排序更新成功");
    }
    
    /**
     * 获取指定房型的默认设施列表
     */
    public Result<List<RoomFacility>> getFacilitiesByRoomType(String roomType) {
        log.info("获取房型{}的默认设施", roomType);
        
        LambdaQueryWrapper<RoomFacility> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(RoomFacility::getDefaultRoomTypes, roomType);
        wrapper.orderByAsc(RoomFacility::getSortOrder);
        
        List<RoomFacility> list = roomFacilityMapper.selectList(wrapper);
        log.info("房型{}有{}个默认设施", roomType, list.size());
        
        return Result.success(list);
    }
    
    /**
     * 保存房型默认模板配置
     * @param roomType 房型
     * @param facilityIds 设施ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveRoomTypeTemplate(String roomType, List<Long> facilityIds) {
        log.info("保存房型{}的默认模板，设施数量：{}", roomType, facilityIds.size());
        
        // 获取所有设施
        List<RoomFacility> allFacilities = roomFacilityMapper.selectList(null);
        
        for (RoomFacility facility : allFacilities) {
            String currentRoomTypes = facility.getDefaultRoomTypes();
            boolean shouldInclude = facilityIds.contains(facility.getId());
            boolean currentlyIncluded = currentRoomTypes != null && currentRoomTypes.contains(roomType);
            
            if (shouldInclude && !currentlyIncluded) {
                // 添加房型
                if (currentRoomTypes == null || currentRoomTypes.isEmpty()) {
                    facility.setDefaultRoomTypes(roomType);
                } else {
                    facility.setDefaultRoomTypes(currentRoomTypes + "," + roomType);
                }
                roomFacilityMapper.updateById(facility);
            } else if (!shouldInclude && currentlyIncluded) {
                // 移除房型
                String newRoomTypes = currentRoomTypes
                    .replace(roomType + ",", "")
                    .replace("," + roomType, "")
                    .replace(roomType, "");
                facility.setDefaultRoomTypes(newRoomTypes.isEmpty() ? null : newRoomTypes);
                roomFacilityMapper.updateById(facility);
            }
        }
        
        return Result.success("保存成功");
    }
}

