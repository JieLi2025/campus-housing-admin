package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话Mapper
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}

