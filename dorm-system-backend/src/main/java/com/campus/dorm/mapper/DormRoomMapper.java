package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.vo.DormRoomVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 宿舍房间Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Mapper
public interface DormRoomMapper extends BaseMapper<DormRoom> {
    
    /**
     * 获取楼栋的所有房间（带楼栋信息）
     * @param buildingId 楼栋ID
     * @return 房间列表
     */
    List<DormRoomVO> getRoomsByBuildingId(@Param("buildingId") Long buildingId);
    
    /**
     * 获取房间详情（带楼栋信息）
     * @param roomId 房间ID
     * @return 房间详情
     */
    DormRoomVO getRoomDetailById(@Param("roomId") Long roomId);
    
    /**
     * 批量插入房间
     * @param rooms 房间列表
     * @return 插入数量
     */
    int batchInsertRooms(@Param("list") List<DormRoom> rooms);
    
    /**
     * 物理删除房间（真正从数据库删除，用于删除楼栋时级联删除房间）
     * @param roomId 房间ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM room WHERE id = #{roomId}")
    int physicalDeleteById(Long roomId);
    
    /**
     * 统计完全空置的房间数量（没有被任何宿舍组选择）
     */
    @Select("SELECT COUNT(*) FROM room r " +
            "WHERE r.available_beds = r.bed_count " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM dorm_group dg " +
            "    WHERE dg.selected_room_id = r.id " +
            ")")
    Long countVacantRooms();
    
    /**
     * 统计空置房间的床位总数
     */
    @Select("SELECT COALESCE(SUM(r.available_beds), 0) FROM room r " +
            "WHERE NOT EXISTS ( " +
            "    SELECT 1 FROM dorm_group dg " +
            "    WHERE dg.selected_room_id = r.id " +
            ")")
    Integer sumVacantRoomBeds();
    
    /**
     * 统计空置房间数量（按楼栋过滤）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM room r " +
            "WHERE r.available_beds = r.bed_count " +
            "AND r.building_id IN " +
            "<foreach item='id' collection='buildingIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM dorm_group dg " +
            "    WHERE dg.selected_room_id = r.id " +
            ")" +
            "</script>")
    Long countVacantRoomsByBuildings(@Param("buildingIds") List<Long> buildingIds);
    
    /**
     * 统计空置房间床位总数（按楼栋过滤）
     */
    @Select("<script>" +
            "SELECT COALESCE(SUM(r.available_beds), 0) FROM room r " +
            "WHERE r.building_id IN " +
            "<foreach item='id' collection='buildingIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM dorm_group dg " +
            "    WHERE dg.selected_room_id = r.id " +
            ")" +
            "</script>")
    Integer sumVacantRoomBedsByBuildings(@Param("buildingIds") List<Long> buildingIds);
    
}
