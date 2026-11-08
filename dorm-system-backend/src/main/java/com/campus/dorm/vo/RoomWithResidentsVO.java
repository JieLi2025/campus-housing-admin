package com.campus.dorm.vo;

import lombok.Data;
import java.util.List;

/**
 * 房间及入住学生信息VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class RoomWithResidentsVO {
    
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
    private String building;
    
    /**
     * 房间号
     */
    private String room;
    
    /**
     * 楼层
     */
    private Integer floor;
    
    /**
     * 房型
     */
    private String type;
    
    /**
     * 总床位数
     */
    private Integer totalBeds;
    
    /**
     * 已住人数
     */
    private Integer occupiedBeds;
    
    /**
     * 可用床位数
     */
    private Integer availableBeds;
    
    /**
     * 归属专业
     */
    private String majorName;
    
    /**
     * 归属班级
     */
    private String className;
    
    /**
     * 归属对象信息（用于前端显示）
     */
    private BelongToInfo belongTo;
    
    /**
     * 入住学生列表
     */
    private List<ResidentInfo> residents;
    
    /**
     * 归属对象信息内部类
     */
    @Data
    public static class BelongToInfo {
        private Long buildingId;
        private String majorName;
        private String className;
    }
    
    /**
     * 入住学生信息内部类
     */
    @Data
    public static class ResidentInfo {
        private Long id;
        private String studentId;
        private String name;
        private String major;
        private String className;
        private String email;
        private String bedNumber;
    }
}

