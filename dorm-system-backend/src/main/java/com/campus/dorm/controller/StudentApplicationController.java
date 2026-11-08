package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.ApplicationDTO;
import com.campus.dorm.dto.StudentUpdateApplicationDTO;
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
 * 学生申请Controller
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/student/applications")
@RequiredArgsConstructor
@Tag(name = "学生申请管理", description = "学生提交和查看申请的接口")
public class StudentApplicationController {
    
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
     * 提交申请
     */
    @PostMapping
    @Operation(summary = "提交申请", description = "学生提交调宿或退宿申请")
    public Result<?> submitApplication(@Validated @RequestBody ApplicationDTO dto) {
        log.info("学生提交申请：dto={}", dto);
        Long studentId = getUserId();
        return applicationService.submitApplication(studentId, dto);
    }
    
    /**
     * 获取我的申请列表
     */
    @GetMapping
    @Operation(summary = "获取我的申请列表", description = "查询当前学生的所有申请记录")
    public Result<IPage<ApplicationVO>> getMyApplications(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("学生获取我的申请列表");
        Long studentId = getUserId();
        return applicationService.getMyApplications(studentId, currentPage, pageSize);
    }
    
    /**
     * 获取申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取申请详情", description = "查看申请的详细信息")
    public Result<ApplicationVO> getApplicationDetail(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        log.info("学生获取申请详情：id={}", id);
        return applicationService.getApplicationDetail(id);
    }
    
    /**
     * 撤回申请
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "撤回申请", description = "撤回待审核的申请")
    public Result<?> withdrawApplication(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        log.info("学生撤回申请：id={}", id);
        Long studentId = getUserId();
        return applicationService.withdrawApplication(id, studentId);
    }
    
    /**
     * 修改申请（仅待审核状态可修改）
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改申请", description = "修改待审核的申请信息")
    public Result<?> updateApplication(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody StudentUpdateApplicationDTO dto) {
        log.info("学生修改申请：id={}, dto={}", id, dto);
        Long studentId = getUserId();
        return applicationService.updateStudentApplication(id, studentId, dto);
    }
}

