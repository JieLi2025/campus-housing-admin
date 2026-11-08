package com.campus.dorm.dto;

import lombok.Data;

/**
 * 学生分配统计数据DTO
 */
@Data
public class AllocationStatisticsDTO {
    
    /**
     * 待分配学生数量（未加入任何宿舍组的学生）
     */
    private Long waitingStudentsCount;
    
    /**
     * 未满宿舍组数量（member_count < max_members）
     */
    private Long incompleteGroupsCount;
    
    /**
     * 未满宿舍组的可用床位总数
     */
    private Integer incompleteGroupBeds;
    
    /**
     * 已满员宿舍组数量（member_count = max_members）
     */
    private Long completeGroupsCount;
    
    /**
     * 完全空置的房间数量（没有被任何宿舍组选择）
     */
    private Long vacantRoomsCount;
    
    /**
     * 空置房间的床位总数
     */
    private Integer vacantRoomBeds;
    
    /**
     * 总可用床位数（未满组床位 + 空置房间床位）
     */
    private Integer totalAvailableBeds;
}

