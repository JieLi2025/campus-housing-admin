package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.AnnouncementQueryDTO;
import com.campus.dorm.service.AnnouncementService;
import com.campus.dorm.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 学生公告Controller
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@RestController
@RequestMapping("/student/announcements")
@RequiredArgsConstructor
@Tag(name = "学生公告管理", description = "学生查看公告的接口")
public class StudentAnnouncementController {
    
    private final AnnouncementService announcementService;
    
    /**
     * 获取公告列表（学生端）
     */
    @GetMapping
    @Operation(summary = "获取公告列表", description = "学生查看公告列表（只显示已发布且未过期的）")
    public Result<IPage<AnnouncementVO>> getAnnouncementList(AnnouncementQueryDTO queryDTO) {
        log.info("学生获取公告列表：queryDTO={}", queryDTO);
        
        // 默认只查询有效的公告
        if (queryDTO.getOnlyValid() == null) {
            queryDTO.setOnlyValid(true);
        }
        
        return announcementService.getStudentAnnouncementList(queryDTO);
    }
    
    /**
     * 获取公告详情（学生端，自动增加浏览数）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情", description = "查看公告详情，自动增加浏览数")
    public Result<AnnouncementVO> getAnnouncementDetail(
            @Parameter(description = "公告ID") @PathVariable Long id) {
        log.info("学生查看公告详情：id={}", id);
        return announcementService.getAnnouncementDetailWithView(id);
    }
    
    /**
     * 获取最新公告（用于登录弹窗）
     */
    @GetMapping("/latest")
    @Operation(summary = "获取最新公告", description = "获取最新的登录弹窗公告")
    public Result<AnnouncementVO> getLatestAnnouncement(
            @Parameter(description = "楼栋ID") @RequestParam(required = false) Long buildingId) {
        log.info("学生获取最新公告：buildingId={}", buildingId);
        return announcementService.getLatestAnnouncement(buildingId);
    }
}

