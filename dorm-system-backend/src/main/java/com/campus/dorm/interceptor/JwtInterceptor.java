package com.campus.dorm.interceptor;

import com.campus.dorm.common.ResultCode;
import com.campus.dorm.exception.BusinessException;
import com.campus.dorm.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器
 * 
 * @author Campus Housing Team
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("${app.jwt.header}")
    private String tokenHeader;
    
    @Value("${app.jwt.prefix}")
    private String tokenPrefix;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 获取 Token
        String token = getTokenFromRequest(request);
        
        if (!StringUtils.hasText(token)) {
            log.warn("请求未携带 Token: {}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        // 验证 Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token 验证失败: {}", token);
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        
        // Token 已过期
        if (jwtUtil.isTokenExpired(token)) {
            log.warn("Token 已过期: {}", token);
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        
        // 将用户信息存入请求属性中，方便后续使用
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String userType = jwtUtil.getUserTypeFromToken(token);
        
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("userType", userType);
        
        log.debug("Token 验证成功: userId={}, username={}, userType={}", userId, username, userType);
        
        return true;
    }
    
    /**
     * 从请求中获取 Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(tokenHeader);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenPrefix)) {
            return bearerToken.substring(tokenPrefix.length()).trim();
        }
        
        return null;
    }
}

