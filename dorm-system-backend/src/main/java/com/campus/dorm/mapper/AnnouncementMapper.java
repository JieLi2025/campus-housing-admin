package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}

