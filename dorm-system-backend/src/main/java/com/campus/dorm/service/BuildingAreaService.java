package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.BuildingAreaDTO;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.BuildingMajorAssignment;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.BuildingMajorAssignmentMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.vo.BuildingAreaVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 楼栋区域管理服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BuildingAreaService {
    
    private final BuildingMapper buildingMapper;
    private final BuildingMajorAssignmentMapper assignmentMapper;
    private final DormRoomMapper dormRoomMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 获取楼栋的所有区域
     * @param buildingId 楼栋ID
     * @return 区域列表
     */
    public Result<List<BuildingAreaVO>> getAreasByBuilding(Long buildingId) {
        log.info("获取楼栋{}的所有区域", buildingId);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 查询该楼栋的所有分配记录（按区域分组）
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getBuildingId, buildingId)
                .eq(BuildingMajorAssignment::getStatus, 1)
                .isNotNull(BuildingMajorAssignment::getAreaId);
        List<BuildingMajorAssignment> assignments = assignmentMapper.selectList(wrapper);
        
        // 按区域ID分组
        Map<String, List<BuildingMajorAssignment>> areaMap = new HashMap<>();
        for (BuildingMajorAssignment assignment : assignments) {
            String areaId = assignment.getAreaId();
            areaMap.computeIfAbsent(areaId, k -> new ArrayList<>()).add(assignment);
        }
        
        // 转换为VO
        List<BuildingAreaVO> areaVOS = new ArrayList<>();
        for (Map.Entry<String, List<BuildingMajorAssignment>> entry : areaMap.entrySet()) {
            String areaId = entry.getKey();
            List<BuildingMajorAssignment> areaAssignments = entry.getValue();
            
            if (!areaAssignments.isEmpty()) {
                BuildingAreaVO vo = convertToAreaVO(buildingId, areaId, areaAssignments);
                areaVOS.add(vo);
            }
        }
        
        // 按区域ID排序
        areaVOS.sort(Comparator.comparing(BuildingAreaVO::getId));
        
        return Result.success(areaVOS);
    }
    
    /**
     * 创建区域
     * @param dto 区域数据
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createArea(BuildingAreaDTO dto) {
        log.info("创建区域：{}", dto);
        
        // 验证楼栋存在
        Building building = buildingMapper.selectById(dto.getBuildingId());
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 生成区域ID
        String areaId = generateAreaId(dto.getBuildingId());
        
        // 将楼层列表转为JSON
        String floorsJson = convertFloorsToJson(dto.getFloors());
        
        // 🔥 关键修复：查找该楼栋中已有的专业分配记录（area_id 为 NULL 的记录）
        LambdaQueryWrapper<BuildingMajorAssignment> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(BuildingMajorAssignment::getBuildingId, dto.getBuildingId())
                      .isNull(BuildingMajorAssignment::getAreaId)
                      .eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> existingAssignments = assignmentMapper.selectList(existingWrapper);
        
        log.info("🔍 找到 {} 条已有的专业分配记录（area_id 为 NULL）", existingAssignments.size());
        
        // 如果有专业班级分配，处理分配记录
        if (dto.getAssignments() != null && !dto.getAssignments().isEmpty()) {
            for (BuildingAreaDTO.MajorClassAssignment assignment : dto.getAssignments()) {
                // 🔥 查找是否有匹配的现有记录
                BuildingMajorAssignment existingRecord = existingAssignments.stream()
                    .filter(e -> e.getMajorName().equals(assignment.getMajorName()) 
                              && (e.getClassName() == null && assignment.getClassName() == null 
                                  || e.getClassName() != null && e.getClassName().equals(assignment.getClassName())))
                    .findFirst()
                    .orElse(null);
                
                if (existingRecord != null) {
                    // 🔥 更新现有记录，添加区域信息
                    log.info("✏️ 更新现有记录 ID={}: {}-{}", existingRecord.getId(), assignment.getMajorName(), assignment.getClassName());
                    existingRecord.setAreaId(areaId);
                    existingRecord.setAreaName(dto.getAreaName());
                    existingRecord.setFloors(floorsJson);
                    existingRecord.setAllocatedRooms(assignment.getAllocatedRooms());
                    assignmentMapper.updateById(existingRecord);
                } else {
                    // 🔥 创建新记录（如果没有匹配的现有记录）
                    log.info("➕ 创建新记录: {}-{}", assignment.getMajorName(), assignment.getClassName());
                    BuildingMajorAssignment entity = new BuildingMajorAssignment();
                    entity.setBuildingId(dto.getBuildingId());
                    entity.setAreaId(areaId);
                    entity.setAreaName(dto.getAreaName());
                    entity.setFloors(floorsJson);
                    entity.setMajorName(assignment.getMajorName());
                    entity.setClassName(assignment.getClassName());
                    entity.setGrade(assignment.getGrade());
                    entity.setGender(building.getBuildingType());
                    entity.setAssignmentMode(assignment.getMode());
                    entity.setAllocatedStudents(assignment.getAllocatedStudents());
                    entity.setAllocatedRooms(assignment.getAllocatedRooms());
                    entity.setStatus(1);
                    
                    assignmentMapper.insert(entity);
                }
            }
        } else {
            // 如果没有分配，创建一个空的区域记录
            BuildingMajorAssignment entity = new BuildingMajorAssignment();
            entity.setBuildingId(dto.getBuildingId());
            entity.setAreaId(areaId);
            entity.setAreaName(dto.getAreaName());
            entity.setFloors(floorsJson);
            entity.setMajorName("");
            entity.setClassName("");
            entity.setGender(building.getBuildingType());
            entity.setAssignmentMode("unassigned");
            entity.setStatus(1);
            
            assignmentMapper.insert(entity);
        }
        
        return Result.success("创建成功");
    }
    
    /**
     * 更新区域
     * @param areaId 区域ID
     * @param dto 区域数据
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateArea(String areaId, BuildingAreaDTO dto) {
        log.info("更新区域：areaId={}, dto={}", areaId, dto);
        
        // 查询该区域的所有记录
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getAreaId, areaId)
                .eq(BuildingMajorAssignment::getBuildingId, dto.getBuildingId());
        List<BuildingMajorAssignment> existingAssignments = assignmentMapper.selectList(wrapper);
        
        if (existingAssignments.isEmpty()) {
            return Result.error("区域不存在");
        }
        
        // 获取楼栋信息（用于获取性别）
        Building building = buildingMapper.selectById(dto.getBuildingId());
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 删除旧的分配记录
        for (BuildingMajorAssignment assignment : existingAssignments) {
            assignmentMapper.deleteById(assignment.getId());
        }
        
        // 创建新的分配记录
        String floorsJson = convertFloorsToJson(dto.getFloors());
        
        if (dto.getAssignments() != null && !dto.getAssignments().isEmpty()) {
            for (BuildingAreaDTO.MajorClassAssignment assignment : dto.getAssignments()) {
                BuildingMajorAssignment entity = new BuildingMajorAssignment();
                entity.setBuildingId(dto.getBuildingId());
                entity.setAreaId(areaId);
                entity.setAreaName(dto.getAreaName());
                entity.setFloors(floorsJson);
                entity.setMajorName(assignment.getMajorName());
                entity.setClassName(assignment.getClassName());
                entity.setGrade(assignment.getGrade());  // 设置年级
                entity.setGender(building.getBuildingType());  // 设置性别，从楼栋类型获取
                entity.setAssignmentMode(assignment.getMode());
                entity.setAllocatedStudents(assignment.getAllocatedStudents());
                entity.setAllocatedRooms(assignment.getAllocatedRooms());
                entity.setStatus(1);
                
                assignmentMapper.insert(entity);
            }
        } else {
            // 如果没有分配，创建一个空的区域记录
            BuildingMajorAssignment entity = new BuildingMajorAssignment();
            entity.setBuildingId(dto.getBuildingId());
            entity.setAreaId(areaId);
            entity.setAreaName(dto.getAreaName());
            entity.setFloors(floorsJson);
            entity.setMajorName("");  // ✅ 设置空字符串，避免数据库NOT NULL错误
            entity.setClassName("");  // ✅ 设置空字符串
            entity.setGender(building.getBuildingType());  // ✅ 设置性别，从楼栋类型获取
            entity.setAssignmentMode("unassigned");  // ✅ 标记为未分配状态
            entity.setStatus(1);
            
            assignmentMapper.insert(entity);
        }
        
        return Result.success("更新成功");
    }
    
    /**
     * 删除区域
     * @param areaId 区域ID
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteArea(String areaId) {
        log.info("删除区域：areaId={}", areaId);
        
        // 查询该区域的所有记录
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getAreaId, areaId);
        List<BuildingMajorAssignment> assignments = assignmentMapper.selectList(wrapper);
        
        if (assignments.isEmpty()) {
            return Result.error("区域不存在");
        }
        
        // 获取区域信息（楼栋ID和楼层列表）
        BuildingMajorAssignment firstAssignment = assignments.get(0);
        Long buildingId = firstAssignment.getBuildingId();
        String floorsJson = firstAssignment.getFloors();
        
        // 解析楼层列表
        if (floorsJson != null && !floorsJson.trim().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Integer> floors = objectMapper.readValue(floorsJson, new TypeReference<List<Integer>>() {});
                
                // 清空该区域楼层的房间归属信息
                log.info("清空楼栋{}的楼层{}的房间归属信息", buildingId, floors);
                LambdaUpdateWrapper<DormRoom> roomUpdateWrapper = new LambdaUpdateWrapper<>();
                roomUpdateWrapper.eq(DormRoom::getBuildingId, buildingId)
                        .in(DormRoom::getFloor, floors)
                        .set(DormRoom::getMajorName, null)
                        .set(DormRoom::getClassName, null);
                dormRoomMapper.update(null, roomUpdateWrapper);
            } catch (Exception e) {
                log.error("解析楼层列表失败", e);
            }
        }
        
        // 删除所有分配记录
        for (BuildingMajorAssignment assignment : assignments) {
            assignmentMapper.deleteById(assignment.getId());
        }
        
        return Result.success("删除成功");
    }
    
    /**
     * 获取区域统计信息
     * @param buildingId 楼栋ID
     * @param areaId 区域ID
     * @return 区域统计信息
     */
    public Result<BuildingAreaVO> getAreaStatistics(Long buildingId, String areaId) {
        log.info("获取区域统计信息：buildingId={}, areaId={}", buildingId, areaId);
        
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getBuildingId, buildingId)
                .eq(BuildingMajorAssignment::getAreaId, areaId)
                .eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> assignments = assignmentMapper.selectList(wrapper);
        
        if (assignments.isEmpty()) {
            return Result.error("区域不存在");
        }
        
        BuildingAreaVO vo = convertToAreaVO(buildingId, areaId, assignments);
        return Result.success(vo);
    }
    
    /**
     * 转换为区域VO
     */
    private BuildingAreaVO convertToAreaVO(Long buildingId, String areaId, List<BuildingMajorAssignment> assignments) {
        BuildingAreaVO vo = new BuildingAreaVO();
        
        if (assignments.isEmpty()) {
            return vo;
        }
        
        BuildingMajorAssignment first = assignments.get(0);
        vo.setId(areaId);
        vo.setBuildingId(buildingId);
        vo.setName(first.getAreaName());
        
        // 解析楼层列表
        List<Integer> floors = parseFloors(first.getFloors());
        vo.setFloors(floors);
        
        // 提取专业列表
        List<BuildingAreaVO.MajorInfo> majors = new ArrayList<>();
        Set<String> majorNames = new HashSet<>();
        
        for (BuildingMajorAssignment assignment : assignments) {
            if (assignment.getMajorName() != null && !majorNames.contains(assignment.getMajorName())) {
                majorNames.add(assignment.getMajorName());
                
                BuildingAreaVO.MajorInfo majorInfo = new BuildingAreaVO.MajorInfo();
                majorInfo.setMajorName(assignment.getMajorName());
                majorInfo.setMode(assignment.getAssignmentMode());
                majorInfo.setAllocatedStudents(assignment.getAllocatedStudents());
                majorInfo.setAllocatedRooms(assignment.getAllocatedRooms());
                
                majors.add(majorInfo);
            }
        }
        vo.setMajors(majors);
        
        // 提取班级列表
        List<BuildingAreaVO.ClassInfo> classes = new ArrayList<>();
        for (BuildingMajorAssignment assignment : assignments) {
            if (assignment.getClassName() != null) {
                BuildingAreaVO.ClassInfo classInfo = new BuildingAreaVO.ClassInfo();
                classInfo.setMajorName(assignment.getMajorName());
                classInfo.setClassName(assignment.getClassName());
                classInfo.setClassKey(assignment.getMajorName() + "-" + assignment.getClassName());
                classInfo.setAllocatedStudents(assignment.getAllocatedStudents());
                classInfo.setAllocatedRooms(assignment.getAllocatedRooms());
                
                classes.add(classInfo);
            }
        }
        vo.setClasses(classes);
        
        // 统计房间数和学生数
        int totalRooms = 0;
        int totalStudents = 0;
        
        // 根据楼层统计房间
        if (!floors.isEmpty()) {
            LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.eq(DormRoom::getBuildingId, buildingId)
                    .in(DormRoom::getFloor, floors);
            Long roomCount = dormRoomMapper.selectCount(roomWrapper);
            totalRooms = roomCount.intValue();
        }
        
        // 累计分配的学生数
        for (BuildingMajorAssignment assignment : assignments) {
            if (assignment.getAllocatedStudents() != null) {
                totalStudents += assignment.getAllocatedStudents();
            }
        }
        
        vo.setRoomCount(totalRooms);
        vo.setStudentCount(totalStudents);
        
        return vo;
    }
    
    /**
     * 生成区域ID
     */
    private String generateAreaId(Long buildingId) {
        // 查询该楼栋已有的最大区域ID
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getBuildingId, buildingId)
                .isNotNull(BuildingMajorAssignment::getAreaId)
                .orderByDesc(BuildingMajorAssignment::getAreaId)
                .last("LIMIT 1");
        
        BuildingMajorAssignment last = assignmentMapper.selectOne(wrapper);
        
        if (last == null || last.getAreaId() == null) {
            return "area-1";
        }
        
        // 提取数字部分并递增
        String lastId = last.getAreaId();
        String[] parts = lastId.split("-");
        if (parts.length == 2) {
            try {
                int num = Integer.parseInt(parts[1]);
                return "area-" + (num + 1);
            } catch (NumberFormatException e) {
                return "area-1";
            }
        }
        
        return "area-1";
    }
    
    /**
     * 将楼层列表转为JSON
     */
    private String convertFloorsToJson(List<Integer> floors) {
        try {
            return objectMapper.writeValueAsString(floors);
        } catch (JsonProcessingException e) {
            log.error("楼层列表转JSON失败", e);
            return "[]";
        }
    }
    
    /**
     * 解析楼层JSON
     */
    private List<Integer> parseFloors(String floorsJson) {
        if (floorsJson == null || floorsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(floorsJson, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析楼层JSON失败：{}", floorsJson, e);
            return new ArrayList<>();
        }
    }
}

