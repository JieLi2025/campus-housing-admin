package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.GroupChatMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群聊成员Mapper
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Mapper
public interface GroupChatMemberMapper extends BaseMapper<GroupChatMember> {
}

