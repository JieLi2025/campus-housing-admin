package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宿舍房间实体类
 * 对应数据库表：room
 * 
 * @author Campus Dorm System
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("room")
public class DormRoom {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 房间ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 所属宿舍楼ID
     */
    @TableField("building_id")
    private Long buildingId;
    
    /**
     * 房间号
     */
    @TableField("room_number")
    private String roomNumber;
    
    /**
     * 楼层
     */
    private Integer floor;
    
    /**
     * 房型：4人间，6人间，2人间等
     */
    @TableField("room_type")
    private String roomType;
    
    /**
     * 床位数
     */
    @TableField("bed_count")
    private Integer bedCount;
    
    /**
     * 可用床位数
     */
    @TableField("available_beds")
    private Integer availableBeds;
    
    /**
     * 归属专业
     */
    @TableField("major_name")
    private String majorName;
    
    /**
     * 归属班级
     */
    @TableField("class_name")
    private String className;
    
    /**
     * 班级分片标识（用于拆分班级时的房间归属）
     */
    @TableField("class_segment")
    private String classSegment;
    
    // ===== 以下是辅助方法（不对应数据库字段） =====
    
    /**
     * 获取容纳人数（即床位数）
     */
    public Integer getCapacity() {
        return this.bedCount;
    }
    
    /**
     * 设置容纳人数
     */
    public void setCapacity(Integer capacity) {
        this.bedCount = capacity;
    }
    
    /**
     * 获取当前入住人数（通过计算得出）
     */
    public Integer getCurrentCount() {
        if (bedCount == null || availableBeds == null) {
            return 0;
        }
        return bedCount - availableBeds;
    }
    
    /**
     * 设置当前入住人数（会更新可用床位数）
     */
    public void setCurrentCount(Integer currentCount) {
        if (bedCount != null && currentCount != null) {
            this.availableBeds = bedCount - currentCount;
        }
    }
}
