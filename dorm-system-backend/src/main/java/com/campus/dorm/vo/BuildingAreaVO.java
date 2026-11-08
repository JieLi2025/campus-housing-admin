package com.campus.dorm.vo;

import lombok.Data;
import java.util.List;

/**
 * 楼栋区域展示对象
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class BuildingAreaVO {
    
    /**
     * 区域ID
     */
    private String id;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 区域名称
     */
    private String name;
    
    /**
     * 楼层列表
     */
    private List<Integer> floors;
    
    /**
     * 专业列表
     */
    private List<MajorInfo> majors;
    
    /**
     * 班级列表
     */
    private List<ClassInfo> classes;
    
    /**
     * 房间数
     */
    private Integer roomCount;
    
    /**
     * 学生数
     */
    private Integer studentCount;
    
    /**
     * 专业信息内部类
     */
    @Data
    public static class MajorInfo {
        private String majorName;
        private String mode; // full-major 或 class-only
        private Integer allocatedStudents;
        private Integer allocatedRooms;
    }
    
    /**
     * 班级信息内部类
     */
    @Data
    public static class ClassInfo {
        private String majorName;
        private String className;
        private String classKey; // majorName + className组合key
        private Integer allocatedStudents;
        private Integer allocatedRooms;
    }
}

