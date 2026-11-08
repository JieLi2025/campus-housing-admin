package com.campus.dorm.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.dorm.entity.Announcement;
import com.campus.dorm.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公告定时任务
 * 自动归档过期公告
 * 
 * @author Campus Dorm System
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementScheduledTask {
    
    private final AnnouncementMapper announcementMapper;
    
    /**
     * 自动归档过期公告
     * 每小时执行一次（整点执行）
     * 将已发布且已过期的公告状态改为"已归档"
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void archiveExpiredAnnouncements() {
        log.info("开始执行过期公告自动归档任务...");
        
        LocalDateTime now = LocalDateTime.now();
        
        LambdaUpdateWrapper<Announcement> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
            .eq(Announcement::getStatus, "已发布")
            .eq(Announcement::getDeleted, 0)
            .isNotNull(Announcement::getExpireDate)
            .lt(Announcement::getExpireDate, now)
            .set(Announcement::getStatus, "已归档");
        
        int count = announcementMapper.update(null, updateWrapper);
        
        if (count > 0) {
            log.info("过期公告自动归档完成，共归档 {} 条公告", count);
        } else {
            log.debug("没有需要归档的过期公告");
        }
    }
}
