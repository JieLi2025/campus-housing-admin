package com.campus.dorm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 未读消息缓存服务
 * 使用Redis缓存未读消息数，提升性能
 * 
 * @author Campus Dorm System
 * @date 2025-12-03
 */
@Slf4j
@Service
public class UnreadCacheService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String UNREAD_KEY_PREFIX = "unread:";
    private static final int CACHE_EXPIRE_MINUTES = 5; // 缓存过期时间5分钟
    
    /**
     * 获取未读消息数（从缓存）
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return 未读消息数，null表示缓存未命中
     */
    public Integer getUnreadCount(Long userId, Long conversationId) {
        try {
            String key = buildKey(userId);
            Object value = redisTemplate.opsForHash().get(key, conversationId.toString());
            
            if (value != null) {
                log.debug("缓存命中: userId={}, conversationId={}, unreadCount={}", 
                    userId, conversationId, value);
                return Integer.parseInt(value.toString());
            }
            
            log.debug("缓存未命中: userId={}, conversationId={}", userId, conversationId);
            return null;
        } catch (Exception e) {
            log.error("获取未读数缓存失败: userId={}, conversationId={}, error={}", 
                userId, conversationId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 设置未读消息数（写入缓存）
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param unreadCount 未读消息数
     */
    public void setUnreadCount(Long userId, Long conversationId, int unreadCount) {
        try {
            String key = buildKey(userId);
            redisTemplate.opsForHash().put(key, conversationId.toString(), unreadCount);
            
            // 设置过期时间
            redisTemplate.expire(key, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.debug("写入缓存: userId={}, conversationId={}, unreadCount={}", 
                userId, conversationId, unreadCount);
        } catch (Exception e) {
            log.error("写入未读数缓存失败: userId={}, conversationId={}, error={}", 
                userId, conversationId, e.getMessage());
        }
    }
    
    /**
     * 增加未读消息数
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @param increment 增量（通常为1）
     */
    public void incrementUnreadCount(Long userId, Long conversationId, int increment) {
        try {
            String key = buildKey(userId);
            redisTemplate.opsForHash().increment(key, conversationId.toString(), increment);
            
            // 设置过期时间
            redisTemplate.expire(key, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            
            log.debug("增加未读数: userId={}, conversationId={}, increment={}", 
                userId, conversationId, increment);
        } catch (Exception e) {
            log.error("增加未读数缓存失败: userId={}, conversationId={}, error={}", 
                userId, conversationId, e.getMessage());
        }
    }
    
    /**
     * 清除未读消息数（标记已读）
     * 
     * @param userId 用户ID
     * @param conversationId 会话ID
     */
    public void clearUnreadCount(Long userId, Long conversationId) {
        try {
            String key = buildKey(userId);
            redisTemplate.opsForHash().delete(key, conversationId.toString());
            
            log.debug("清除未读数: userId={}, conversationId={}", userId, conversationId);
        } catch (Exception e) {
            log.error("清除未读数缓存失败: userId={}, conversationId={}, error={}", 
                userId, conversationId, e.getMessage());
        }
    }
    
    /**
     * 清除用户的所有未读消息缓存
     * 
     * @param userId 用户ID
     */
    public void clearAllUnreadCount(Long userId) {
        try {
            String key = buildKey(userId);
            redisTemplate.delete(key);
            
            log.debug("清除用户所有未读数缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户所有未读数缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    /**
     * 获取用户的总未读消息数
     * 
     * @param userId 用户ID
     * @return 总未读消息数
     */
    public int getTotalUnreadCount(Long userId) {
        try {
            String key = buildKey(userId);
            Map<Object, Object> allUnread = redisTemplate.opsForHash().entries(key);
            
            int total = allUnread.values().stream()
                .mapToInt(v -> Integer.parseInt(v.toString()))
                .sum();
            
            log.debug("获取总未读数: userId={}, total={}", userId, total);
            return total;
        } catch (Exception e) {
            log.error("获取总未读数失败: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 构建Redis Key
     * 
     * @param userId 用户ID
     * @return Redis Key
     */
    private String buildKey(Long userId) {
        return UNREAD_KEY_PREFIX + userId;
    }
    
    /**
     * 检查Redis是否可用
     * 
     * @return true=可用，false=不可用
     */
    public boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().set("health_check", "ok", 1, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Redis不可用: {}", e.getMessage());
            return false;
        }
    }
}
