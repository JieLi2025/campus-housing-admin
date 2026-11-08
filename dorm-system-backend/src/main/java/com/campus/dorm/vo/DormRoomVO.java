package com.campus.dorm.vo;

import lombok.Data;

import java.util.List;

/**
 * 宿舍房间VO
 * 用于返回给前端的房间详细信息
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Data
public class DormRoomVO {
    
    /**
     * 房间ID
     */
    private Long id;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 楼栋名称
     */
    private String buildingName;
    
    /**
     * 楼栋类型
     */
    private String buildingType;
    
    /**
     * 房间号
     */
    private String roomNumber;
    
    /**
     * 楼层
     */
    private Integer floor;
    
    /**
     * 房间类型
     */
    private String roomType;
    
    /**
     * 床位数
     */
    private Integer bedCount;
    
    /**
     * 可用床位数
     */
    private Integer availableBeds;
    
    /**
     * 已占用床位数（计算得出）
     */
    private Integer occupiedBeds;
    
    /**
     * 设施配置（JSON格式，保留向后兼容）
     */
    private String facilities;
    
    /**
     * 物资配置（JSON格式，从material_template动态获取）
     */
    private String materials;
    
    /**
     * 归属专业
     */
    private String majorName;
    
    /**
     * 归属班级
     */
    private String className;
    
    /**
     * 班级分片标识
     */
    private String classSegment;
    
    /**
     * 房间状态：available(可用)、full(已满)
     */
    private String status;
    
    /**
     * 入住学生列表
     */
    private List<RoomStudentVO> students;
    
    // ===== 辅助方法 =====
    
    /**
     * 计算已占用床位数
     */
    public Integer getOccupiedBeds() {
        if (bedCount != null && availableBeds != null) {
            return bedCount - availableBeds;
        }
        return 0;
    }
    
    /**
     * 根据可用床位数自动计算房间状态
     */
    public String getStatus() {
        if (status != null) {
            return status;
        }
        // 如果未设置status，根据可用床位数自动计算
        if (availableBeds != null && bedCount != null) {
            if (availableBeds == 0) {
                return "full";
            } else if (availableBeds > 0) {
                return "available";
            }
        }
        return "available";
    }
}
