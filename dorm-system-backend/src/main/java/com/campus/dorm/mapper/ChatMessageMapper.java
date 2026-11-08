package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}

