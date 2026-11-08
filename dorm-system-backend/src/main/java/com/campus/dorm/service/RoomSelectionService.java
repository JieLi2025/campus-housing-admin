package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormGroupMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 房间选择Service
 * 数据源：building 表的阶段配置字段 和 dorm_group 表的统计
 * 
 * @author Campus Dorm System
 * @date 2025-10-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomSelectionService {
    
    private final BuildingMapper buildingMapper;
    private final DormGroupMapper dormGroupMapper;
    private final DormRoomMapper dormRoomMapper;
    private final StudentMapper studentMapper;
    
    /**
     * 获取统计数据
     * 数据源：building 表的阶段配置字段 和 dorm_group 表的统计
     */
    public Result<?> getStatistics() {
        log.info("获取房间选择统计数据");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 1. 获取第一个有阶段配置的楼栋
            LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
            buildingWrapper.isNotNull(Building::getCurrentPhase)
                          .orderByDesc(Building::getId)
                          .last("LIMIT 1");
            Building building = buildingMapper.selectOne(buildingWrapper);
            
            // 2. 统计总房间数（从room表）
            LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
            Long totalRooms = dormRoomMapper.selectCount(roomWrapper);
            stats.put("totalRooms", totalRooms != null ? totalRooms : 0L);
            
            // 3. 统计已选房间数（从dorm_group表，status='room_selected'）
            Long selectedRooms = dormGroupMapper.countSelectedRooms();
            stats.put("selectedRooms", selectedRooms != null ? selectedRooms : 0L);
            
            // 4. 统计总学生数（从student表统计）
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getDeleted, 0);
            Long studentCount = studentMapper.selectCount(studentWrapper);
            Integer totalStudents = studentCount != null ? studentCount.intValue() : 0;
            stats.put("totalStudents", totalStudents);
            
            // 5. 统计已分配学生数（从dorm_group表统计）
            Integer allocatedStudents = dormGroupMapper.sumAllocatedStudents();
            stats.put("allocatedStudents", allocatedStudents != null ? allocatedStudents : 0);
            
            // 6. 计算未分配学生数
            int unallocatedStudents = Math.max(0, totalStudents - (allocatedStudents != null ? allocatedStudents : 0));
            stats.put("unallocatedStudents", unallocatedStudents);
            
            // 7. 计算选房率（已选房间数 / 总房间数）
            double selectionRate = 0.0;
            if (totalRooms != null && totalRooms > 0) {
                selectionRate = (selectedRooms != null ? selectedRooms : 0L) * 100.0 / totalRooms;
            }
            stats.put("selectionRate", Math.round(selectionRate * 100.0) / 100.0);
            
            // 8. 计算分配率（已分配学生数 / 总学生数）
            double allocationRate = 0.0;
            if (totalStudents > 0) {
                allocationRate = (allocatedStudents != null ? allocatedStudents : 0) * 100.0 / totalStudents;
            }
            stats.put("allocationRate", Math.round(allocationRate * 100.0) / 100.0);
            
            // 9. 添加当前阶段信息（从building表获取）
            if (building != null) {
                stats.put("currentPhase", building.getCurrentPhase());
                stats.put("buildingName", building.getBuildingName());
            }
            
            log.info("房间选择统计数据获取成功：总房间数={}, 已选房间数={}, 总学生数={}, 已分配学生数={}", 
                    totalRooms, selectedRooms, totalStudents, allocatedStudents);
            
        } catch (Exception e) {
            log.error("获取房间选择统计数据失败", e);
            // 返回默认值
            stats.put("totalRooms", 0);
            stats.put("selectedRooms", 0);
            stats.put("totalStudents", 0);
            stats.put("allocatedStudents", 0);
            stats.put("unallocatedStudents", 0);
            stats.put("selectionRate", 0.0);
            stats.put("allocationRate", 0.0);
            stats.put("error", "统计数据获取失败：" + e.getMessage());
        }
        
        return Result.success(stats);
    }
}
