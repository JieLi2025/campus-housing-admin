package com.campus.dorm.service;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.*;

import java.util.List;

/**
 * 学生分配统计Service接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-31
 */
public interface IStudentAllocationService {
    
    /**
     * 获取学生分配统计数据（按管理员负责的楼栋过滤）
     * @param buildingIds 楼栋ID列表，如果为空或null则统计所有楼栋
     * @return 统计数据DTO
     */
    AllocationStatisticsDTO getAllocationStatistics(List<Long> buildingIds);
    
    /**
     * 智能分配学生到宿舍
     * @param request 智能分配请求
     * @return 分配结果
     */
    AllocationResultDTO intelligentAssign(IntelligentAssignRequest request);
    
    /**
     * 手动分配学生到宿舍组
     * @param request 分配请求
     * @return 分配结果
     */
    AllocationResultDTO assignToGroup(AssignToGroupRequest request);
    
    /**
     * 手动分配学生到房间
     * @param request 分配请求
     * @return 分配结果
     */
    AllocationResultDTO assignToRoom(AssignToRoomRequest request);
    
    /**
     * 按班级批量处理
     * @param className 班级名称
     * @param buildingIds 楼栋ID列表
     * @return 分配结果
     */
    AllocationResultDTO batchProcessByClass(String className, List<Long> buildingIds);
    
    /**
     * 验证分配结果
     * @param buildingIds 楼栋ID列表
     * @return 验证结果
     */
    AllocationValidationDTO validateAllocation(List<Long> buildingIds);
    
    /**
     * 完成分配，将宿舍信息同步到学生表
     * @param buildingIds 楼栋ID列表（可选，如果为null或空则处理所有楼栋）
     * @return 处理结果
     */
    Result<?> completeAllocation(List<Long> buildingIds);
}

