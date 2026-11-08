package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.DormGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 宿舍组Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-29
 */
@Mapper
public interface DormGroupMapper extends BaseMapper<DormGroup> {
    
    /**
     * 统计未满员的宿舍组数量
     * 注意：dorm_group 表没有 deleted 字段，因此不添加逻辑删除条件
     */
    @Select("SELECT COUNT(*) FROM dorm_group " +
            "WHERE status = 'forming' " +
            "AND current_members < max_members")
    Long countIncompleteGroups();
    
    /**
     * 统计未满员宿舍组的可用床位总数
     * 注意：dorm_group 表没有 deleted 字段，因此不添加逻辑删除条件
     */
    @Select("SELECT COALESCE(SUM(max_members - current_members), 0) FROM dorm_group " +
            "WHERE status = 'forming' " +
            "AND current_members < max_members")
    Integer sumAvailableBedsInIncompleteGroups();
    
    /**
     * 统计已满员的宿舍组数量
     * 注意：dorm_group 表没有 deleted 字段，因此不添加逻辑删除条件
     */
    @Select("SELECT COUNT(*) FROM dorm_group " +
            "WHERE status IN ('complete', 'room_selected') " +
            "AND current_members >= max_members")
    Long countCompleteGroups();
    
    /**
     * 统计未满员的宿舍组数量（按楼栋过滤）
     * 注意：dorm_group 表没有 building_id 字段，需要通过 room 表关联
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM dorm_group dg " +
            "LEFT JOIN room r ON dg.selected_room_id = r.id " +
            "WHERE dg.status = 'forming' " +
            "AND dg.current_members &lt; dg.max_members " +
            "AND (dg.selected_room_id IS NULL OR r.building_id IN " +
            "<foreach item='id' collection='buildingIds' open='(' separator=',' close=')'> " +
            "#{id}" +
            "</foreach>" +
            ")" +
            "</script>")
    Long countIncompleteGroupsByBuildings(@Param("buildingIds") List<Long> buildingIds);
    
    /**
     * 统计未满员宿舍组的可用床位总数（按楼栋过滤）
     * 注意：dorm_group 表没有 building_id 字段，需要通过 room 表关联
     */
    @Select("<script>" +
            "SELECT COALESCE(SUM(dg.max_members - dg.current_members), 0) FROM dorm_group dg " +
            "LEFT JOIN room r ON dg.selected_room_id = r.id " +
            "WHERE dg.status = 'forming' " +
            "AND dg.current_members &lt; dg.max_members " +
            "AND (dg.selected_room_id IS NULL OR r.building_id IN " +
            "<foreach item='id' collection='buildingIds' open='(' separator=',' close=')'> " +
            "#{id}" +
            "</foreach>" +
            ")" +
            "</script>")
    Integer sumAvailableBedsInIncompleteGroupsByBuildings(@Param("buildingIds") List<Long> buildingIds);
    
    /**
     * 统计已满员的宿舍组数量（按楼栋过滤）
     * 注意：dorm_group 表没有 building_id 字段，需要通过 room 表关联
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM dorm_group dg " +
            "LEFT JOIN room r ON dg.selected_room_id = r.id " +
            "WHERE dg.status IN ('complete', 'room_selected') " +
            "AND dg.current_members &gt;= dg.max_members " +
            "AND (dg.selected_room_id IS NULL OR r.building_id IN " +
            "<foreach item='id' collection='buildingIds' open='(' separator=',' close=')'> " +
            "#{id}" +
            "</foreach>" +
            ")" +
            "</script>")
    Long countCompleteGroupsByBuildings(@Param("buildingIds") List<Long> buildingIds);
    
    /**
     * 统计已选房间的宿舍组数量（status='room_selected'）
     * 注意：dorm_group 表没有 deleted 字段，因此不添加逻辑删除条件
     */
    @Select("SELECT COUNT(*) FROM dorm_group " +
            "WHERE status = 'room_selected'")
    Long countSelectedRooms();
    
    /**
     * 统计已分配的学生总数（从所有宿舍组的members字段中统计）
     * 注意：dorm_group 表没有 deleted 字段，因此不添加逻辑删除条件
     */
    @Select("SELECT COALESCE(SUM(current_members), 0) FROM dorm_group " +
            "WHERE status IN ('complete', 'room_selected', 'allocated')")
    Integer sumAllocatedStudents();
}

