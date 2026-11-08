package com.campus.dorm.vo;

import lombok.Data;
import java.util.List;

/**
 * 学生班级楼栋分配信息VO
 * 用于学生查询自己班级分配到哪栋楼、哪些房间
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class StudentBuildingAssignmentVO {
    
    /**
     * 学生专业
     */
    private String major;
    
    /**
     * 学生班级
     */
    private String className;
    
    /**
     * 是否已分配楼栋
     */
    private Boolean hasAssignment;
    
    /**
     * 分配信息列表（可能有多个楼栋）
     */
    private List<AssignmentInfo> assignments;
    
    /**
     * 分配信息内部类
     */
    @Data
    public static class AssignmentInfo {
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
         * 分配模式：full-major或class-only
         */
        private String assignmentMode;
        
        /**
         * 分配房间数
         */
        private Integer allocatedRooms;
        
        /**
         * 分配学生数
         */
        private Integer allocatedStudents;
        
        /**
         * 楼层列表
         */
        private List<Integer> floors;
        
        /**
         * 区域名称
         */
        private String areaName;
        
        /**
         * 班级分片标识（用于拆分班级，如A/B/C）
         */
        private String classSegment;
        
        /**
         * 学号范围：当一个班级被拆分到多个区域时，记录每个分片包含的学号范围，帮助学生了解自己属于哪个分片
         * 格式示例：2023004001~2023004050
         */
        private String studentIdRange;
    }
}

