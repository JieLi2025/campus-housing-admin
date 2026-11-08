package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会话实体类
 * 对应数据库表：conversation
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("conversation")
public class Conversation {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户1 ID
     */
    @TableField("user1_id")
    private Long user1Id;
    
    /**
     * 用户2 ID（私聊时使用，群聊时为NULL）
     */
    @TableField("user2_id")
    private Long user2Id;
    
    /**
     * 会话类型：private-私聊，building_group-楼栋群聊，dorm_group-宿舍群聊
     */
    @TableField("conversation_type")
    private String conversationType;
    
    /**
     * 楼栋ID（楼栋群聊时使用）
     */
    @TableField("building_id")
    private Long buildingId;
    
    /**
     * 宿舍队伍ID（宿舍群聊时使用）
     */
    @TableField("dorm_group_id")
    private Long dormGroupId;
    
    /**
     * 群聊名称
     */
    @TableField("group_name")
    private String groupName;
    
    /**
     * 最后一条消息ID
     */
    @TableField("last_message_id")
    private Long lastMessageId;
    
    /**
     * 最后消息时间
     */
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 全体禁言：0-未禁言，1-已禁言（群聊时使用）
     */
    @TableField("mute_all")
    private Boolean muteAll;
}

