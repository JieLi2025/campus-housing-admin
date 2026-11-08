package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.ApplicationQueryDTO;
import com.campus.dorm.dto.ApproveApplicationDTO;
import com.campus.dorm.dto.UpdateApplicationDTO;
import com.campus.dorm.service.ApplicationService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.ApplicationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 申请管理Controller（管理员端）
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/admin/applications")
@RequiredArgsConstructor
@Tag(name = "申请管理（管理员端）", description = "管理员审批学生申请的接口")
public class ApplicationController {
    
    private final ApplicationService applicationService;
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
     * 获取申请列表（分页、筛选）
     * 管理员只能看到自己管理楼栋的申请
     */
    @PostMapping("/list")
    @Operation(summary = "获取申请列表", description = "支持分页、关键词搜索、类型筛选、状态筛选")
    public Result<IPage<ApplicationVO>> getApplicationList(@RequestBody ApplicationQueryDTO queryDTO) {
        Long adminUserId = getUserId();
        log.info("管理员获取申请列表：adminUserId={}, queryDTO={}", adminUserId, queryDTO);
        return applicationService.getApplicationList(queryDTO, adminUserId);
    }
    
    /**
     * 获取申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取申请详情", description = "根据申请ID获取详细信息")
    public Result<ApplicationVO> getApplicationDetail(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        log.info("管理员获取申请详情：id={}", id);
        return applicationService.getApplicationDetail(id);
    }
    
    /**
     * 审批申请
     */
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批申请", description = "管理员审批申请（通过或拒绝）")
    public Result<?> approveApplication(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @Validated @RequestBody ApproveApplicationDTO dto) {
        log.info("管理员审批申请：id={}, dto={}", id, dto);
        Long adminId = getUserId();
        return applicationService.approveApplication(id, adminId, dto);
    }
    
    /**
     * 更新申请信息（管理员更新分配信息）
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新申请信息", description = "管理员更新申请的分配信息（仅限待审核的调宿申请）")
    public Result<?> updateApplication(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @Validated @RequestBody UpdateApplicationDTO dto) {
        log.info("管理员更新申请信息：id={}, dto={}", id, dto);
        return applicationService.updateApplication(id, dto);
    }
    
    /**
     * 删除申请
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除申请", description = "软删除申请记录")
    public Result<?> deleteApplication(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        log.info("管理员删除申请：id={}", id);
        return applicationService.deleteApplication(id);
    }
    
    /**
     * 获取待审核申请数量
     */
    @GetMapping("/pending-count")
    @Operation(summary = "获取待审核申请数量", description = "用于首页待办提醒")
    public Result<Long> getPendingCount() {
        Long adminUserId = getUserId();
        return applicationService.getPendingCount(adminUserId);
    }
}

