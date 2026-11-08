package com.campus.dorm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的消息代理，用于向客户端发送消息
        // 客户端订阅路径：/user/{userId}/queue/messages
        config.enableSimpleBroker("/queue", "/topic");
        // 客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
        // 用户目标前缀
        config.setUserDestinationPrefix("/user");
    }
    
    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点，客户端通过此端点连接
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*") // 允许所有来源（生产环境应限制）
                .withSockJS(); // 启用SockJS支持，提供降级方案
    }
    
    /**
     * 配置客户端入站通道拦截器（用于认证）
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}

