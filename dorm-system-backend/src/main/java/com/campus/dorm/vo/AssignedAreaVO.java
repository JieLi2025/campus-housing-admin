package com.campus.dorm.vo;

import lombok.Data;

/**
 * 分配区域信息VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class AssignedAreaVO {
    
    /**
     * 楼栋名称
     */
    private String building;
    
    /**
     * 分配楼层（如"3-5楼"）
     */
    private String floors;
    
    /**
     * 可用房间数量
     */
    private Integer availableRooms;
    
    /**
     * 房间类型（如"4人间"）
     */
    private String roomType;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 班级
     */
    private String className;
    
    /**
     * 班级分片标识（用于拆分班级，如A/B/C）
     */
    private String classSegment;
    
    /**
     * 学号范围（拆分班级时显示，如"2023004001~2023004050"）
     */
    private String studentIdRange;
    
    /**
     * 区域是否已分配（楼栋管理员是否已完成区域划分）
     * true: 已分配具体房间，学生可以进行选房
     * false: 等待楼栋管理员划分区域
     */
    private Boolean areaAssigned;
}

