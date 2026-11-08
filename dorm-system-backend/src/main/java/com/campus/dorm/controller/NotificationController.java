package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.NotificationDTO;
import com.campus.dorm.service.NotificationService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 消息通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    
    @Resource
    private NotificationService notificationService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取通知列表
     */
    @GetMapping
    public Result<IPage<NotificationVO>> getNotificationList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getUserId();
        IPage<NotificationVO> page = notificationService.getNotificationList(userId, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 获取管理员通知列表（带类型过滤）
     * @param type 通知类型（可选）：system/application/visitor
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GetMapping("/admin")
    public Result<IPage<NotificationVO>> getAdminNotificationList(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = getUserId();
        IPage<NotificationVO> page = notificationService.getAdminNotificationList(userId, type, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 获取系统通知列表（超级管理员发送的通知）
     * 用于超级管理员的通知管理功能
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GetMapping("/system")
    public Result<IPage<NotificationVO>> getSystemNotificationList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        IPage<NotificationVO> page = notificationService.getSystemNotificationList(pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = getUserId();
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }
    
    /**
     * 创建通知（仅超级管理员）
     */
    @PostMapping
    public Result<Void> createNotification(@Validated @RequestBody NotificationDTO dto) {
        Long userId = getUserId();
        notificationService.createNotification(dto, userId);
        return Result.success();
    }
    
    /**
     * 更新通知（仅超级管理员）
     */
    @PutMapping("/{id}")
    public Result<Void> updateNotification(
            @PathVariable Long id,
            @Validated @RequestBody NotificationDTO dto) {
        dto.setId(id);
        notificationService.updateNotification(dto);
        return Result.success();
    }
    
    /**
     * 标记为已读
     */
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return Result.success();
    }
    
    /**
     * 全部标记为已读
     */
    @PutMapping("/mark-all-read")
    public Result<Void> markAllAsRead() {
        Long userId = getUserId();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
    
    /**
     * 删除通知
     * 系统通知只能由超级管理员删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        Long userId = getUserId();
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }
}

