package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.OperationLogDTO;
import com.campus.dorm.service.OperationLogService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.OperationLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/operation-logs")
public class OperationLogController {
    
    @Resource
    private OperationLogService operationLogService;
    
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
     * 获取操作日志列表（当前用户）
     */
    @GetMapping
    public Result<IPage<OperationLogVO>> getOperationLogList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = getUserId();
        IPage<OperationLogVO> page = operationLogService.getOperationLogList(userId, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 获取所有操作日志列表（超级管理员）
     */
    @GetMapping("/all")
    public Result<IPage<OperationLogVO>> getAllOperationLogList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<OperationLogVO> page = operationLogService.getOperationLogList(null, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 记录操作日志
     */
    @PostMapping
    public Result<Void> logOperation(@Validated @RequestBody OperationLogDTO dto) {
        Long userId = getUserId();
        operationLogService.logOperation(userId, dto);
        return Result.success();
    }
}

