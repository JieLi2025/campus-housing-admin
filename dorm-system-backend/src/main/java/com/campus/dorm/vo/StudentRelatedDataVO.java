package com.campus.dorm.vo;

import lombok.Data;
import java.util.List;

/**
 * 学生关联数据VO
 * 用于展示学生的所有关联数据，方便删除前预览
 * 
 * @author Campus Dorm System
 * @date 2024-11-04
 */
@Data
public class StudentRelatedDataVO {
    
    /**
     * 学生基本信息
     */
    private StudentBasicInfo studentInfo;
    
    /**
     * 用户账号信息
     */
    private UserAccountInfo userAccount;
    
    /**
     * 室友邀请统计
     */
    private InviteStatistics inviteStats;
    
    /**
     * 队伍信息
     */
    private TeamInfo teamInfo;
    
    /**
     * 床位分配信息
     */
    private BedAllocationInfo bedInfo;
    
    /**
     * 申请记录统计
     */
    private ApplicationStatistics applicationStats;
    
    /**
     * 通知记录统计
     */
    private NotificationStatistics notificationStats;
    
    /**
     * 物资分配统计
     */
    private MaterialStatistics materialStats;
    
    /**
     * 数据完整性检查
     */
    private DataIntegrityCheck integrityCheck;
    
    // ===== 内部类定义 =====
    
    @Data
    public static class StudentBasicInfo {
        private Long id;
        private String studentId;
        private String realName;
        private String gender;
        private String major;
        private String grade;
        private String className;
        private String bio;
        private String tags;
        private Integer isRegistered;
        private String registrationDate;
    }
    
    @Data
    public static class UserAccountInfo {
        private Long userId;
        private String username;
        private String role;
        private String status;
    }
    
    @Data
    public static class InviteStatistics {
        private Integer sentCount;          // 发送的邀请数
        private Integer receivedCount;      // 收到的邀请数
        private Integer pendingCount;       // 待处理的邀请数
        private List<InviteDetail> recentInvites; // 最近的邀请记录
    }
    
    @Data
    public static class InviteDetail {
        private Long id;
        private String type;  // sent/received
        private String otherPartyName;
        private String status;
        private String createdAt;
    }
    
    @Data
    public static class TeamInfo {
        private Long teamId;
        private String teamName;
        private String role;            // leader/member
        private Integer memberCount;
        private String status;
        private List<String> memberNames;
        private String warning;         // 如果是队长，警告将解散队伍
    }
    
    @Data
    public static class BedAllocationInfo {
        private Long bedId;
        private String buildingName;
        private String roomNumber;
        private String bedNumber;
        private String status;
        private String allocatedAt;
        private Integer roommateCount;  // 室友数量
        private List<String> roommateNames;
    }
    
    @Data
    public static class ApplicationStatistics {
        private Integer totalCount;
        private Integer pendingCount;
        private Integer approvedCount;
        private List<ApplicationDetail> recentApplications;
    }
    
    @Data
    public static class ApplicationDetail {
        private Long id;
        private String type;
        private String status;
        private String reason;
        private String createdAt;
    }
    
    @Data
    public static class NotificationStatistics {
        private Integer totalCount;
        private Integer unreadCount;
        private Integer recentCount;
    }
    
    @Data
    public static class MaterialStatistics {
        private Integer totalCount;
        private Integer allocatedCount;
        private List<MaterialDetail> items;
    }
    
    @Data
    public static class MaterialDetail {
        private String materialName;
        private Integer quantity;
        private String status;
    }
    
    @Data
    public static class DataIntegrityCheck {
        private Boolean hasUser;
        private Boolean hasInvites;
        private Boolean hasTeam;
        private Boolean hasBed;
        private Boolean hasApplications;
        private Boolean hasNotifications;
        private Boolean hasMaterials;
        private String summary;  // 数据摘要
        private String warning;  // 删除警告
    }
}

