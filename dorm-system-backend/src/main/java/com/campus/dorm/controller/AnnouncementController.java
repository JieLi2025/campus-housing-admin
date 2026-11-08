package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.AnnouncementDTO;
import com.campus.dorm.dto.AnnouncementQueryDTO;
import com.campus.dorm.service.AnnouncementService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 公告管理Controller（管理员端）
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
@Tag(name = "公告管理（管理员端）", description = "管理员发布和管理公告的接口")
public class AnnouncementController {
    
    private final AnnouncementService announcementService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    
    /**
     * 获取当前用户ID（从JWT Token）
     */
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取当前用户类型（从JWT Token）
     */
    private String getUserType() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserTypeFromToken(token);
    }
    
    /**
     * 创建公告
     */
    @PostMapping
    @Operation(summary = "创建公告", description = "管理员创建新公告（草稿或直接发布）")
    public Result<?> createAnnouncement(@Validated @RequestBody AnnouncementDTO dto) {
        log.info("管理员创建公告：dto={}", dto);
        Long publisherId = getUserId();
        return announcementService.createAnnouncement(publisherId, dto);
    }
    
    /**
     * 获取公告列表（分页、筛选）
     * 超级管理员：可以看到所有公告
     * 普通管理员：只能看到自己发布的 + 自己管理楼栋相关的公告
     */
    @GetMapping
    @Operation(summary = "获取公告列表", description = "支持分页、关键词搜索、状态筛选")
    public Result<IPage<AnnouncementVO>> getAnnouncementList(AnnouncementQueryDTO queryDTO) {
        Long userId = getUserId();
        String userType = getUserType();
        log.info("获取公告列表：userId={}, userType={}, queryDTO={}", userId, userType, queryDTO);
        
        // 超级管理员可以看到所有公告
        if ("superadmin".equals(userType)) {
            return announcementService.getAnnouncementList(queryDTO);
        }
        
        // 普通管理员只能看到自己发布的 + 自己管理楼栋相关的公告
        return announcementService.getAnnouncementListForAdmin(queryDTO, userId);
    }
    
    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情", description = "根据公告ID获取详细信息")
    public Result<AnnouncementVO> getAnnouncementDetail(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("管理员获取公告详情：id={}", id);
        return announcementService.getAnnouncementDetail(id);
    }
    
    /**
     * 更新公告
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新公告", description = "更新公告的标题、内容等信息")
    public Result<?> updateAnnouncement(
            @Parameter(description = "公告ID") @PathVariable Long id,
            @Validated @RequestBody AnnouncementDTO dto) {
        log.info("管理员更新公告：id={}, dto={}", id, dto);
        return announcementService.updateAnnouncement(id, dto);
    }
    
    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告", description = "软删除公告记录")
    public Result<?> deleteAnnouncement(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("管理员删除公告：id={}", id);
        return announcementService.deleteAnnouncement(id);
    }
    
    /**
     * 发布公告
     */
    @PutMapping("/{id}/publish")
    @Operation(summary = "发布公告", description = "将草稿状态的公告发布")
    public Result<?> publishAnnouncement(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("管理员发布公告：id={}", id);
        return announcementService.publishAnnouncement(id);
    }
    
    /**
     * 归档公告
     */
    @PutMapping("/{id}/archive")
    @Operation(summary = "归档公告", description = "将公告归档（不再对学生可见）")
    public Result<?> archiveAnnouncement(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("管理员归档公告：id={}", id);
        return announcementService.archiveAnnouncement(id);
    }
}

