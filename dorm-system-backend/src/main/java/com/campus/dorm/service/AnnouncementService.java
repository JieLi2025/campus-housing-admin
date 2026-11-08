package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.AnnouncementDTO;
import com.campus.dorm.dto.AnnouncementQueryDTO;
import com.campus.dorm.dto.NotificationDTO;
import com.campus.dorm.entity.Announcement;
import com.campus.dorm.mapper.AnnouncementMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.entity.Building;
import com.campus.dorm.vo.AnnouncementVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 公告Service
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {
    
    private final AnnouncementMapper announcementMapper;
    private final BuildingMapper buildingMapper;
    private final NotificationService notificationService;
    private final com.campus.dorm.mapper.SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createAnnouncement(Long publisherId, AnnouncementDTO dto) {
        log.info("创建公告：publisherId={}, dto={}", publisherId, dto);
        
        Announcement announcement = new Announcement();
        BeanUtils.copyProperties(dto, announcement);
        
        announcement.setPublisherId(publisherId);
        // publisherType 字段已标记为非持久化字段，不需要设置
        
        // 处理楼栋ID列表
        if (dto.getBuildingIds() != null && !dto.getBuildingIds().isEmpty()) {
            announcement.setBuildingIds(dto.getBuildingIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        } else {
            announcement.setBuildingIds("ALL");
        }
        
        // 处理展示选项
        if (dto.getDisplayOptions() != null && !dto.getDisplayOptions().isEmpty()) {
            try {
                announcement.setDisplayOptions(objectMapper.writeValueAsString(dto.getDisplayOptions()));
            } catch (Exception e) {
                log.error("展示选项序列化失败", e);
                announcement.setDisplayOptions("[]");
            }
        }
        
        // 设置状态（统一使用中文，默认已发布）
        if (StringUtils.isBlank(dto.getStatus())) {
            announcement.setStatus("已发布");
        }
        
        // publishedAt 字段已标记为非持久化，不需要设置
        // 发布时间可以通过 created_at 字段获取
        
        announcementMapper.insert(announcement);
        
        log.info("公告创建成功：announcementId={}", announcement.getId());
        
        // 如果公告状态为已发布，创建通知提醒（在新线程中执行，不影响主事务）
        if ("已发布".equals(announcement.getStatus())) {
            final Announcement finalAnnouncement = announcement;
            new Thread(() -> {
                try {
                    createAnnouncementNotification(finalAnnouncement);
                } catch (Exception e) {
                    log.error("创建公告通知失败（异步）：announcementId={}", finalAnnouncement.getId(), e);
                }
            }).start();
        }
        
        return Result.success("公告创建成功");
    }
    
    /**
     * 为公告创建通知（折中方案）
     * - 普通管理员发布公告：不创建通知，学生通过公告列表查看
     * - 超级管理员发布指定楼栋公告：只通知该楼栋的管理员（1条记录）
     * - 超级管理员发布全部楼栋公告：不创建通知
     */
    private void createAnnouncementNotification(Announcement announcement) {
        try {
            // 只有超级管理员发布指定楼栋的公告时，才通知对应楼栋的管理员
            String buildingIds = announcement.getBuildingIds();
            if (!"ALL".equals(buildingIds) && StringUtils.isNotBlank(buildingIds)) {
                if (isSuperAdmin(announcement.getPublisherId())) {
                    String[] ids = buildingIds.split(",");
                    notifyBuildingAdmins(announcement, ids);
                    log.info("已通知楼栋管理员：announcementId={}, buildingIds={}", 
                        announcement.getId(), buildingIds);
                } else {
                    log.info("普通管理员发布公告，不创建通知：announcementId={}", announcement.getId());
                }
            } else {
                log.info("全部楼栋公告，不创建通知：announcementId={}", announcement.getId());
            }
        } catch (Exception e) {
            log.error("创建公告通知失败：announcementId={}", announcement.getId(), e);
            // 不抛出异常，避免影响公告发布
        }
    }
    
    /**
     * 检查用户是否为超级管理员
     */
    private boolean isSuperAdmin(Long userId) {
        try {
            com.campus.dorm.entity.SysUser user = sysUserMapper.selectById(userId);
            return user != null && "superadmin".equals(user.getUserType());
        } catch (Exception e) {
            log.error("检查用户类型失败：userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 通知楼栋管理员（超级管理员发布公告时）
     * 通过 manager_name 字段查找管理员用户
     */
    private void notifyBuildingAdmins(Announcement announcement, String[] buildingIdStrs) {
        for (String idStr : buildingIdStrs) {
            try {
                Long buildingId = Long.parseLong(idStr.trim());
                Building building = buildingMapper.selectById(buildingId);
                if (building == null || StringUtils.isBlank(building.getManagerName())) {
                    log.warn("楼栋不存在或没有管理员：buildingId={}", buildingId);
                    continue;
                }
                
                // 通过 manager_name（用户名）查找管理员用户
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.dorm.entity.SysUser> wrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                wrapper.eq(com.campus.dorm.entity.SysUser::getUsername, building.getManagerName())
                       .eq(com.campus.dorm.entity.SysUser::getUserType, "admin");
                com.campus.dorm.entity.SysUser adminUser = sysUserMapper.selectOne(wrapper);
                
                if (adminUser == null) {
                    log.warn("楼栋管理员用户不存在：buildingId={}, managerName={}", buildingId, building.getManagerName());
                    continue;
                }
                
                NotificationDTO adminNotification = new NotificationDTO();
                adminNotification.setTitle("新公告：" + announcement.getTitle());
                // 显示公告内容，超过100字截断
                String content = announcement.getContent();
                adminNotification.setMessage(content.length() > 100 
                    ? content.substring(0, 100) + "..." 
                    : content);
                adminNotification.setType("system");
                adminNotification.setRelatedId(announcement.getId());
                adminNotification.setTargetUserId(adminUser.getId());
                notificationService.createNotification(adminNotification, announcement.getPublisherId());
                log.info("已通知楼栋管理员：buildingId={}, adminUserId={}", buildingId, adminUser.getId());
            } catch (NumberFormatException e) {
                log.warn("无效的楼栋ID：{}", idStr);
            } catch (Exception e) {
                log.error("通知单个楼栋管理员失败：idStr={}", idStr, e);
                // 继续处理其他楼栋，不中断
            }
        }
    }
    
    /**
     * 获取公告列表（超级管理员端，可以看到所有公告）
     */
    public Result<IPage<AnnouncementVO>> getAnnouncementList(AnnouncementQueryDTO queryDTO) {
        log.info("获取公告列表：queryDTO={}", queryDTO);
        
        Page<Announcement> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getDeleted, 0);
        
        // 关键词搜索（标题或内容）
        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                .like(Announcement::getTitle, queryDTO.getKeyword())
                .or()
                .like(Announcement::getContent, queryDTO.getKeyword())
            );
        }
        
        // 状态筛选
        if (StringUtils.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(Announcement::getStatus, queryDTO.getStatus());
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Announcement::getCreatedAt);
        
        IPage<Announcement> result = announcementMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<AnnouncementVO> voPage = result.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取公告列表（普通管理员端）
     * 只能看到：自己发布的 + 自己管理楼栋相关的公告
     */
    public Result<IPage<AnnouncementVO>> getAnnouncementListForAdmin(AnnouncementQueryDTO queryDTO, Long adminUserId) {
        log.info("管理员获取公告列表：adminUserId={}, queryDTO={}", adminUserId, queryDTO);
        
        // 查询该管理员管理的楼栋
        LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
        buildingWrapper.eq(Building::getAdminId, adminUserId);
        List<Building> managedBuildings = buildingMapper.selectList(buildingWrapper);
        List<Long> managedBuildingIds = managedBuildings.stream()
            .map(Building::getId)
            .collect(Collectors.toList());
        
        log.info("管理员管理的楼栋IDs：{}", managedBuildingIds);
        
        Page<Announcement> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getDeleted, 0);
        
        // 权限筛选：自己发布的 OR 目标楼栋包含自己管理的楼栋 OR 全部楼栋
        wrapper.and(w -> {
            // 自己发布的
            w.eq(Announcement::getPublisherId, adminUserId);
            
            // 或者目标楼栋包含自己管理的楼栋
            if (!managedBuildingIds.isEmpty()) {
                for (Long buildingId : managedBuildingIds) {
                    w.or().like(Announcement::getBuildingIds, buildingId.toString());
                }
            }
            
            // 或者是全部楼栋的公告
            w.or().eq(Announcement::getBuildingIds, "ALL");
        });
        
        // 关键词搜索（标题或内容）
        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                .like(Announcement::getTitle, queryDTO.getKeyword())
                .or()
                .like(Announcement::getContent, queryDTO.getKeyword())
            );
        }
        
        // 状态筛选
        if (StringUtils.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(Announcement::getStatus, queryDTO.getStatus());
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Announcement::getCreatedAt);
        
        IPage<Announcement> result = announcementMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<AnnouncementVO> voPage = result.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取公告列表（学生端）
     * 
     * 楼栋过滤逻辑：
     * - 如果学生有分配楼栋（buildingId != null）：显示"全部楼栋"公告 + 包含该楼栋的公告
     * - 如果学生没有分配楼栋（buildingId == null）：只显示"全部楼栋"公告
     */
    public Result<IPage<AnnouncementVO>> getStudentAnnouncementList(AnnouncementQueryDTO queryDTO) {
        log.info("学生获取公告列表：queryDTO={}, buildingId={}", queryDTO, queryDTO.getBuildingId());
        
        // 调试：先查询所有已发布的公告
        LambdaQueryWrapper<Announcement> debugWrapper = new LambdaQueryWrapper<>();
        debugWrapper.eq(Announcement::getDeleted, 0);
        debugWrapper.eq(Announcement::getStatus, "已发布");
        List<Announcement> allAnnouncements = announcementMapper.selectList(debugWrapper);
        log.info("所有已发布公告数量：{}，详情：{}", allAnnouncements.size(), 
            allAnnouncements.stream().map(a -> "id=" + a.getId() + ",buildingIds=" + a.getBuildingIds() + ",deadline=" + a.getExpireDate()).collect(java.util.stream.Collectors.toList()));
        
        Page<Announcement> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getDeleted, 0);
        wrapper.eq(Announcement::getStatus, "已发布"); // 只查询已发布的
        
        // 楼栋筛选
        if (queryDTO.getBuildingId() != null) {
            // 学生有分配楼栋：显示"全部楼栋"公告 + 包含该楼栋的公告
            wrapper.and(w -> w
                .eq(Announcement::getBuildingIds, "ALL")
                .or()
                .like(Announcement::getBuildingIds, queryDTO.getBuildingId().toString())
            );
        } else {
            // 学生没有分配楼栋：只显示"全部楼栋"公告
            wrapper.eq(Announcement::getBuildingIds, "ALL");
        }
        
        // 只查询未过期的
        if (Boolean.TRUE.equals(queryDTO.getOnlyValid())) {
            wrapper.and(w -> w
                .isNull(Announcement::getExpireDate)
                .or()
                .gt(Announcement::getExpireDate, LocalDateTime.now())
            );
        }
        
        // 按创建时间倒序（发布时间通过 created_at 获取）
        wrapper.orderByDesc(Announcement::getCreatedAt);
        
        IPage<Announcement> result = announcementMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<AnnouncementVO> voPage = result.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取公告详情
     */
    public Result<AnnouncementVO> getAnnouncementDetail(Long id) {
        log.info("获取公告详情：id={}", id);
        
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            return Result.error("公告不存在");
        }
        
        AnnouncementVO vo = convertToVO(announcement);
        return Result.success(vo);
    }
    
    /**
     * 获取公告详情（学生端，自动增加浏览数）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<AnnouncementVO> getAnnouncementDetailWithView(Long id) {
        log.info("学生查看公告详情：id={}", id);
        
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            return Result.error("公告不存在");
        }
        
        AnnouncementVO vo = convertToVO(announcement);
        return Result.success(vo);
    }
    
    /**
     * 更新公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateAnnouncement(Long id, AnnouncementDTO dto) {
        log.info("更新公告：id={}, dto={}", id, dto);
        
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            return Result.error("公告不存在");
        }
        
        BeanUtils.copyProperties(dto, announcement);
        announcement.setId(id); // 确保ID不变
        
        // 处理楼栋ID列表
        if (dto.getBuildingIds() != null && !dto.getBuildingIds().isEmpty()) {
            announcement.setBuildingIds(dto.getBuildingIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        } else {
            announcement.setBuildingIds("ALL");
        }
        
        // 处理展示选项
        if (dto.getDisplayOptions() != null && !dto.getDisplayOptions().isEmpty()) {
            try {
                announcement.setDisplayOptions(objectMapper.writeValueAsString(dto.getDisplayOptions()));
            } catch (Exception e) {
                log.error("展示选项序列化失败", e);
            }
        }
        
        announcementMapper.updateById(announcement);
        
        log.info("公告更新成功：id={}", id);
        return Result.success("公告更新成功");
    }
    
    /**
     * 删除公告
     * 同时删除消息通知中对应的公告通知
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteAnnouncement(Long id) {
        log.info("删除公告：id={}", id);
        
        // 先查询公告是否存在
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            log.warn("公告不存在：id={}", id);
            return Result.error("公告不存在");
        }
        
        // 检查是否已删除
        if (announcement.getDeleted() != null && announcement.getDeleted() == 1) {
            log.warn("公告已删除：id={}", id);
            return Result.error("公告已删除");
        }
        
        // 使用 LambdaUpdateWrapper 只更新 deleted 字段
        LambdaUpdateWrapper<Announcement> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Announcement::getId, id)
                     .eq(Announcement::getDeleted, 0)  // 确保只更新未删除的记录
                     .set(Announcement::getDeleted, 1);
        
        int result = announcementMapper.update(null, updateWrapper);
        
        if (result > 0) {
            // 级联删除消息通知中对应的公告通知
            notificationService.deleteNotificationsByRelatedId("system", id);
            log.info("公告删除成功：id={}, result={}", id, result);
            return Result.success("公告删除成功");
        } else {
            log.warn("公告删除失败：id={}, result={}", id, result);
            return Result.error("公告删除失败，可能已被删除");
        }
    }
    
    /**
     * 发布公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> publishAnnouncement(Long id) {
        log.info("发布公告：id={}", id);
        
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            return Result.error("公告不存在");
        }
        
        announcement.setStatus("已发布");
        // publishedAt 字段已标记为非持久化，不需要设置
        // 发布时间可以通过 created_at 字段获取
        
        announcementMapper.updateById(announcement);
        
        log.info("公告发布成功：id={}", id);
        return Result.success("公告发布成功");
    }
    
    /**
     * 归档公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> archiveAnnouncement(Long id) {
        log.info("归档公告：id={}", id);
        
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || announcement.getDeleted() == 1) {
            return Result.error("公告不存在");
        }
        
        announcement.setStatus("已归档");
        
        announcementMapper.updateById(announcement);
        
        log.info("公告归档成功：id={}", id);
        return Result.success("公告归档成功");
    }
    
    /**
     * 获取最新公告（已废弃）
     * 登录弹窗功能已移除，此方法保留接口兼容性，始终返回null
     */
    @Deprecated
    public Result<AnnouncementVO> getLatestAnnouncement(Long buildingId) {
        log.info("获取最新公告（已废弃）：buildingId={}", buildingId);
        return Result.success(null);
    }
    
    /**
     * 转换为VO
     */
    private AnnouncementVO convertToVO(Announcement announcement) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(announcement, vo);
        
        // 实时计算状态：如果已发布且已过期，显示为"已归档"
        String status = announcement.getStatus();
        if ("已发布".equals(status) && announcement.getExpireDate() != null 
                && announcement.getExpireDate().isBefore(LocalDateTime.now())) {
            status = "已归档";
            // 同时更新数据库状态（异步，不影响查询性能）
            asyncUpdateAnnouncementStatus(announcement.getId(), "已归档");
        }
        vo.setStatus(status);
        
        // 设置发布时间：如果状态为"已发布"，使用 created_at 作为发布时间
        LocalDateTime publishTime = null;
        if ("已发布".equals(announcement.getStatus()) && announcement.getCreatedAt() != null) {
            publishTime = announcement.getCreatedAt();
        } else if (announcement.getPublishedAt() != null) {
            publishTime = announcement.getPublishedAt();
        }
        
        if (publishTime != null) {
            vo.setPublishedAt(publishTime);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            vo.setPublishTime(publishTime.format(formatter));
        } else if (announcement.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            vo.setPublishTime(announcement.getCreatedAt().format(formatter));
        }
        
        // 设置是否已过期
        if (announcement.getExpireDate() != null) {
            vo.setIsExpired(announcement.getExpireDate().isBefore(LocalDateTime.now()));
        } else {
            vo.setIsExpired(false);
        }
        
        // 解析楼栋ID列表并获取楼栋名称
        if (StringUtils.isNotBlank(announcement.getBuildingIds()) && !"ALL".equals(announcement.getBuildingIds())) {
            try {
                List<Long> buildingIds = Arrays.stream(announcement.getBuildingIds().split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
                vo.setBuildingIds(buildingIds);
                // 查询楼栋名称
                List<Building> buildings = buildingMapper.selectBatchIds(buildingIds);
                if (buildings != null && !buildings.isEmpty()) {
                    vo.setBuilding(buildings.stream()
                        .map(Building::getBuildingName)
                        .collect(Collectors.joining("、")));
                } else {
                    vo.setBuilding("未知楼栋");
                }
            } catch (Exception e) {
                log.error("解析楼栋ID失败", e);
                vo.setBuilding("全部楼栋");
            }
        } else {
            vo.setBuilding("全部楼栋");
        }
        
        // 解析展示选项
        if (StringUtils.isNotBlank(announcement.getDisplayOptions())) {
            try {
                List<String> displayOptions = objectMapper.readValue(
                    announcement.getDisplayOptions(), 
                    new TypeReference<List<String>>() {}
                );
                vo.setDisplayOptions(displayOptions);
            } catch (Exception e) {
                log.error("解析展示选项失败", e);
                vo.setDisplayOptions(new ArrayList<>());
            }
        } else {
            vo.setDisplayOptions(new ArrayList<>());
        }
        
        return vo;
    }
    
    /**
     * 异步更新公告状态（在查询时发现过期后自动归档）
     */
    private void asyncUpdateAnnouncementStatus(Long id, String status) {
        try {
            LambdaUpdateWrapper<Announcement> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Announcement::getId, id)
                    .eq(Announcement::getStatus, "已发布")  // 只更新已发布状态的
                    .set(Announcement::getStatus, status);
            announcementMapper.update(null, updateWrapper);
            log.debug("自动归档过期公告：id={}", id);
        } catch (Exception e) {
            log.warn("自动归档公告失败：id={}, error={}", id, e.getMessage());
        }
    }
}

