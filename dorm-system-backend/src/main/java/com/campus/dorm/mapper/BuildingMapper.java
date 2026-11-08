package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Building;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 宿舍楼Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {
    
    /**
     * 物理删除楼栋（真正从数据库删除）
     * @param buildingId 楼栋ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM building WHERE id = #{buildingId}")
    int physicalDeleteById(Long buildingId);
    
    /**
     * 根据楼栋ID列表获取楼栋的性别类型
     * @param buildingIds 楼栋ID列表
     * @return 性别列表（male/female）
     */
    @Select("<script>" +
            "SELECT DISTINCT building_type FROM building " +
            "WHERE id IN " +
            "<foreach item='id' collection='buildingIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<String> getBuildingGenders(@Param("buildingIds") List<Long> buildingIds);
    
}

