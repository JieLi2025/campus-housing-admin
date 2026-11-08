package com.campus.dorm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.CreateAdminDTO;
import com.campus.dorm.dto.ResetPasswordDTO;
import com.campus.dorm.dto.UpdateAdminDTO;
import com.campus.dorm.service.AdminService;
import com.campus.dorm.vo.AdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 超级管理员控制器
 * 用于管理普通管理员账号
 * 
 * @author Campus Dorm System
 * @date 2024-10-26
 */
@Slf4j
@RestController
@RequestMapping("/super-admin/admins")
@RequiredArgsConstructor
@Tag(name = "超级管理员-管理员管理", description = "超级管理员对普通管理员的管理接口")
public class SuperAdminController {
    
    private final AdminService adminService;
    
    /**
     * 获取管理员列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取管理员列表", description = "分页查询管理员列表，支持关键词搜索和状态筛选")
    public Result<Page<AdminVO>> getAdminList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        log.info("超级管理员获取管理员列表：currentPage={}, pageSize={}, keyword={}, status={}", 
                currentPage, pageSize, keyword, status);
        return adminService.getAdminList(currentPage, pageSize, keyword, status);
    }
    
    /**
     * 获取管理员详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取管理员详情", description = "根据ID获取管理员详细信息")
    public Result<AdminVO> getAdminDetail(@PathVariable Long id) {
        log.info("超级管理员获取管理员详情：id={}", id);
        return adminService.getAdminDetail(id);
    }
    
    /**
     * 创建管理员
     */
    @PostMapping
    @Operation(summary = "创建管理员", description = "创建新的普通管理员账号")
    public Result<?> createAdmin(@Validated @RequestBody CreateAdminDTO dto) {
        log.info("超级管理员创建管理员：username={}", dto.getUsername());
        return adminService.createAdmin(dto);
    }
    
    /**
     * 更新管理员信息
     */
    @PutMapping
    @Operation(summary = "更新管理员信息", description = "更新管理员的基本信息（不包括密码）")
    public Result<?> updateAdmin(@Validated @RequestBody UpdateAdminDTO dto) {
        log.info("超级管理员更新管理员：id={}", dto.getId());
        return adminService.updateAdmin(dto);
    }
    
    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除管理员", description = "软删除管理员账号")
    public Result<?> deleteAdmin(@PathVariable Long id) {
        log.info("超级管理员删除管理员：id={}", id);
        return adminService.deleteAdmin(id);
    }
    
    /**
     * 切换管理员状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "切换管理员状态", description = "启用或禁用管理员账号")
    public Result<?> toggleAdminStatus(@PathVariable Long id) {
        log.info("超级管理员切换管理员状态：id={}", id);
        return adminService.toggleAdminStatus(id);
    }
    
    /**
     * 重置管理员密码
     */
    @PutMapping("/{id}/password")
    @Operation(summary = "重置管理员密码", description = "重置管理员的登录密码")
    public Result<?> resetPassword(@PathVariable Long id, 
                                   @Validated @RequestBody ResetPasswordDTO dto) {
        log.info("超级管理员重置管理员密码：id={}", id);
        return adminService.resetPassword(id, dto);
    }
}

