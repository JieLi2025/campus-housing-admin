package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.BuildingMajorAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 楼栋专业分配Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-25
 */
@Mapper
public interface BuildingMajorAssignmentMapper extends BaseMapper<BuildingMajorAssignment> {
    
    /**
     * 根据楼栋ID查询所有有效的专业分配
     * @param buildingId 楼栋ID
     * @return 专业分配列表
     */
    @Select("SELECT * FROM building_major_assignment WHERE building_id = #{buildingId} AND status = 1 ORDER BY id DESC")
    List<BuildingMajorAssignment> selectByBuildingId(Long buildingId);
    
    /**
     * 根据楼栋ID和专业名称查询分配记录
     * @param buildingId 楼栋ID
     * @param majorName 专业名称
     * @return 专业分配记录
     */
    @Select("SELECT * FROM building_major_assignment WHERE building_id = #{buildingId} AND major_name = #{majorName} LIMIT 1")
    BuildingMajorAssignment selectByBuildingAndMajor(Long buildingId, String majorName);
}

