package com.campus.dorm.service.impl;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.*;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormGroupMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.service.IStudentAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学生分配统计Service实现类
 * 
 * @author Campus Dorm System
 * @date 2025-10-31
 * 
 * 注意：此类已被 StudentAllocationServiceImplExtended 替代
 * 保留此文件作为参考，如需恢复旧版本，取消下面 @Service 注解的注释即可
 */
@Slf4j
// @Service  // 已注释：使用 StudentAllocationServiceImplExtended 替代
@RequiredArgsConstructor
public class StudentAllocationServiceImpl implements IStudentAllocationService {
    
    private final StudentMapper studentMapper;
    private final DormGroupMapper dormGroupMapper;
    private final DormRoomMapper dormRoomMapper;
    private final BuildingMapper buildingMapper;
    
    @Override
    public AllocationStatisticsDTO getAllocationStatistics(List<Long> buildingIds) {
        log.info("开始计算学生分配统计数据，楼栋IDs: {}", buildingIds);
        
        AllocationStatisticsDTO dto = new AllocationStatisticsDTO();
        
        try {
            // 获取楼栋的性别类型（male/female）
            List<String> genders = null;
            if (buildingIds != null && !buildingIds.isEmpty()) {
                genders = buildingMapper.getBuildingGenders(buildingIds);
                log.info("楼栋对应的性别类型: {}", genders);
            }
            
            // 1. 待分配学生数量（按楼栋性别过滤）
            Long waitingStudents;
            if (genders != null && !genders.isEmpty()) {
                waitingStudents = studentMapper.countUnassignedStudentsByGenders(genders);
            } else {
                waitingStudents = studentMapper.countUnassignedStudents();
            }
            dto.setWaitingStudentsCount(waitingStudents != null ? waitingStudents : 0L);
            log.info("待分配学生数: {}", waitingStudents);
            
            // 2-7. 宿舍组和房间统计（按楼栋ID过滤）
            if (buildingIds != null && !buildingIds.isEmpty()) {
                // 按楼栋过滤
                Long incompleteGroups = dormGroupMapper.countIncompleteGroupsByBuildings(buildingIds);
                dto.setIncompleteGroupsCount(incompleteGroups != null ? incompleteGroups : 0L);
                log.info("未满宿舍组数: {}", incompleteGroups);
                
                Integer incompleteGroupBeds = dormGroupMapper.sumAvailableBedsInIncompleteGroupsByBuildings(buildingIds);
                dto.setIncompleteGroupBeds(incompleteGroupBeds != null ? incompleteGroupBeds : 0);
                log.info("未满组可用床位: {}", incompleteGroupBeds);
                
                Long completeGroups = dormGroupMapper.countCompleteGroupsByBuildings(buildingIds);
                dto.setCompleteGroupsCount(completeGroups != null ? completeGroups : 0L);
                log.info("已满员宿舍组数: {}", completeGroups);
                
                Long vacantRooms = dormRoomMapper.countVacantRoomsByBuildings(buildingIds);
                dto.setVacantRoomsCount(vacantRooms != null ? vacantRooms : 0L);
                log.info("空置房间数: {}", vacantRooms);
                
                Integer vacantRoomBeds = dormRoomMapper.sumVacantRoomBedsByBuildings(buildingIds);
                dto.setVacantRoomBeds(vacantRoomBeds != null ? vacantRoomBeds : 0);
                log.info("空置房间床位: {}", vacantRoomBeds);
            } else {
                // 全局统计
                Long incompleteGroups = dormGroupMapper.countIncompleteGroups();
                dto.setIncompleteGroupsCount(incompleteGroups != null ? incompleteGroups : 0L);
                log.info("未满宿舍组数: {}", incompleteGroups);
                
                Integer incompleteGroupBeds = dormGroupMapper.sumAvailableBedsInIncompleteGroups();
                dto.setIncompleteGroupBeds(incompleteGroupBeds != null ? incompleteGroupBeds : 0);
                log.info("未满组可用床位: {}", incompleteGroupBeds);
                
                Long completeGroups = dormGroupMapper.countCompleteGroups();
                dto.setCompleteGroupsCount(completeGroups != null ? completeGroups : 0L);
                log.info("已满员宿舍组数: {}", completeGroups);
                
                Long vacantRooms = dormRoomMapper.countVacantRooms();
                dto.setVacantRoomsCount(vacantRooms != null ? vacantRooms : 0L);
                log.info("空置房间数: {}", vacantRooms);
                
                Integer vacantRoomBeds = dormRoomMapper.sumVacantRoomBeds();
                dto.setVacantRoomBeds(vacantRoomBeds != null ? vacantRoomBeds : 0);
                log.info("空置房间床位: {}", vacantRoomBeds);
            }
            
            // 7. 总可用床位（未满组 + 空置房间）
            int totalAvailableBeds = dto.getIncompleteGroupBeds() + dto.getVacantRoomBeds();
            dto.setTotalAvailableBeds(totalAvailableBeds);
            log.info("总可用床位: {}", totalAvailableBeds);
            
        } catch (Exception e) {
            log.error("计算学生分配统计数据失败", e);
            // 出错时返回默认值
            dto.setWaitingStudentsCount(0L);
            dto.setIncompleteGroupsCount(0L);
            dto.setIncompleteGroupBeds(0);
            dto.setCompleteGroupsCount(0L);
            dto.setVacantRoomsCount(0L);
            dto.setVacantRoomBeds(0);
            dto.setTotalAvailableBeds(0);
        }
        
        return dto;
    }
    
    @Override
    public AllocationResultDTO intelligentAssign(IntelligentAssignRequest request) {
        throw new UnsupportedOperationException("请使用 StudentAllocationServiceImplExtended");
    }
    
    @Override
    public AllocationResultDTO assignToGroup(AssignToGroupRequest request) {
        throw new UnsupportedOperationException("请使用 StudentAllocationServiceImplExtended");
    }
    
    @Override
    public AllocationResultDTO assignToRoom(AssignToRoomRequest request) {
        throw new UnsupportedOperationException("请使用 StudentAllocationServiceImplExtended");
    }
    
    @Override
    public AllocationResultDTO batchProcessByClass(String className, List<Long> buildingIds) {
        throw new UnsupportedOperationException("请使用 StudentAllocationServiceImplExtended");
    }
    
    @Override
    public AllocationValidationDTO validateAllocation(List<Long> buildingIds) {
        throw new UnsupportedOperationException("请使用 StudentAllocationServiceImplExtended");
    }
    
    @Override
    public Result<?> completeAllocation(List<Long> buildingIds) {
        throw new UnsupportedOperationException("请使用 StudentAllocationServiceImplExtended");
    }
}

