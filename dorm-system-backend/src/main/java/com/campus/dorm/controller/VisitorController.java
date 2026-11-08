package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.ApproveVisitorDTO;
import com.campus.dorm.dto.VisitorDTO;
import com.campus.dorm.dto.VisitorQueryDTO;
import com.campus.dorm.service.VisitorService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.VisitorStatisticsVO;
import com.campus.dorm.vo.VisitorVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 访客管理Controller（管理员端）
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/admin/visitors")
@RequiredArgsConstructor
@Tag(name = "访客管理（管理员端）", description = "管理员审批和管理访客的接口")
public class VisitorController {
    
    private final VisitorService visitorService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取访客列表（分页、筛选）
     * 普通管理员只能看到自己管理楼栋的访客
     */
    @GetMapping
    @Operation(summary = "获取访客列表", description = "支持分页、关键词搜索、状态筛选、日期筛选")
    public Result<IPage<VisitorVO>> getVisitorList(VisitorQueryDTO queryDTO) {
        Long adminUserId = getUserId();
        log.info("管理员获取访客列表：adminUserId={}, queryDTO={}", adminUserId, queryDTO);
        return visitorService.getVisitorList(queryDTO, adminUserId);
    }
    
    /**
     * 获取访客详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取访客详情", description = "根据访客ID获取详细信息")
    public Result<VisitorVO> getVisitorDetail(
            @Parameter(description = "访客ID") @PathVariable Long id) {
        log.info("管理员获取访客详情：id={}", id);
        return visitorService.getVisitorDetail(id);
    }
    
    /**
     * 审批访客（批准/拒绝）
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批访客", description = "批准或拒绝访客预约")
    public Result<?> approveVisitor(
            @Parameter(description = "访客ID") @PathVariable Long id,
            @Validated @RequestBody ApproveVisitorDTO dto) {
        log.info("管理员审批访客：id={}, dto={}", id, dto);
        Long adminId = getUserId();
        return visitorService.approveVisitor(id, adminId, dto);
    }
    
    /**
     * 访客签到
     */
    @PostMapping("/{id}/checkin")
    @Operation(summary = "访客签到", description = "访客到访时签到")
    public Result<?> checkIn(
            @Parameter(description = "访客ID") @PathVariable Long id) {
        log.info("管理员处理访客签到：id={}", id);
        return visitorService.checkIn(id);
    }
    
    /**
     * 访客签退
     */
    @PostMapping("/{id}/checkout")
    @Operation(summary = "访客签退", description = "访客离开时签退")
    public Result<?> checkOut(
            @Parameter(description = "访客ID") @PathVariable Long id) {
        log.info("管理员处理访客签退：id={}", id);
        return visitorService.checkOut(id);
    }
    
    /**
     * 删除访客记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除访客记录", description = "软删除访客记录")
    public Result<?> deleteVisitor(
            @Parameter(description = "访客ID") @PathVariable Long id) {
        log.info("管理员删除访客记录：id={}", id);
        return visitorService.deleteVisitor(id, null);
    }
    
    /**
     * 创建访客记录（管理员端）
     */
    @PostMapping
    @Operation(summary = "创建访客记录", description = "管理员直接创建访客记录，学生信息可选")
    public Result<?> createVisitor(@Validated @RequestBody VisitorDTO dto) {
        log.info("管理员创建访客记录：dto={}", dto);
        return visitorService.createVisitorByAdmin(dto);
    }
    
    /**
     * 访客统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "访客统计", description = "获取访客统计信息（待审核、今日、本周、本月等）")
    public Result<VisitorStatisticsVO> getStatistics() {
        log.info("管理员获取访客统计");
        return visitorService.getStatistics();
    }
}

