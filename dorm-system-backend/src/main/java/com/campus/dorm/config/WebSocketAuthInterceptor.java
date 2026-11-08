package com.campus.dorm.config;

import com.campus.dorm.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket认证拦截器
 * 用于在WebSocket连接时验证JWT Token
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从请求头获取Token
            String token = accessor.getFirstNativeHeader("Authorization");
            
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            if (token != null && !token.isEmpty()) {
                try {
                    // 验证Token并获取用户ID
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    if (userId != null && jwtUtil.validateToken(token)) {
                        // 将用户ID设置到WebSocket会话中
                        // 创建一个简单的Principal对象，其name就是userId的字符串形式
                        final String userIdStr = userId.toString();
                        accessor.setUser(new java.security.Principal() {
                            @Override
                            public String getName() {
                                return userIdStr;
                            }
                        });
                        log.info("WebSocket连接认证成功，用户ID: {}, Principal name: {}", userId, userIdStr);
                    } else {
                        log.warn("WebSocket连接认证失败：Token无效或已过期");
                        // 拒绝连接
                        throw new SecurityException("WebSocket连接认证失败：Token无效");
                    }
                } catch (Exception e) {
                    log.error("WebSocket连接认证失败", e);
                    // 拒绝连接
                    throw new SecurityException("WebSocket连接认证失败：" + e.getMessage());
                }
            } else {
                log.warn("WebSocket连接认证失败：未提供Token");
                // 拒绝连接
                throw new SecurityException("WebSocket连接认证失败：未提供Token");
            }
        }
        
        return message;
    }
}

