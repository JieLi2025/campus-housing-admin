package com.campus.dorm.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知VO
 */
@Data
public class NotificationVO {
    
    /**
     * 通知ID
     */
    private Long id;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知内容
     */
    private String message;
    
    /**
     * 通知类型: system/application/visitor
     */
    private String type;
    
    /**
     * 图标名称
     */
    private String icon;
    
    /**
     * 是否已读
     */
    private Boolean read;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 时间显示（如：刚刚、1小时前）
     */
    private String time;
    
    /**
     * 关联业务ID（申请ID、访客ID等）
     */
    private Long relatedId;
    
    /**
     * 跳转信息
     */
    private JumpInfo jumpTo;
    
    /**
     * 跳转信息内部类
     */
    @Data
    public static class JumpInfo {
        /**
         * 跳转路由
         */
        private String route;
        
        /**
         * 跳转类型
         */
        private String type;
        
        /**
         * 关联ID
         */
        private Long relatedId;
    }
}

