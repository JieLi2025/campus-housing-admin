package com.campus.dorm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队信息VO
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Data
public class TeamInfoVO {
    
    /**
     * 团队ID
     */
    private Long id;
    
    /**
     * 团队名称
     */
    private String groupName;
    
    /**
     * 组长ID
     */
    private Long leaderId;
    
    /**
     * 组长姓名
     */
    private String leaderName;
    
    /**
     * 成员数量
     */
    private Integer memberCount;
    
    /**
     * 最大成员数
     */
    private Integer maxMembers;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 班级
     */
    private String className;
    
    /**
     * 性别
     */
    private String gender;
    
    /**
     * 房间类型
     */
    private String roomType;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 选择的房间ID
     */
    private Long roomId;
    
    /**
     * 选择的房间号
     */
    private String roomNumber;
    
    /**
     * 楼栋ID
     */
    private Long buildingId;
    
    /**
     * 楼栋名称
     */
    private String buildingName;
    
    /**
     * 成员列表
     */
    private List<TeamMemberVO> members;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 团队成员VO
     */
    @Data
    public static class TeamMemberVO {
        private Long id;
        private String name;
        private String studentId;
        private String major;
        private String className;
        private Boolean isLeader;
        private String avatar;
    }
}

