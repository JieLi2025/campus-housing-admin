package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Contact;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 联系人Mapper
 * 
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
    
    /**
     * 物理删除联系人（绕过逻辑删除）
     * @param id 联系人记录ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM contact WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
    
    /**
     * 根据用户ID和联系人ID物理删除
     * @param userId 用户ID
     * @param contactId 联系人ID
     * @return 删除的行数
     */
    @Delete("DELETE FROM contact WHERE user_id = #{userId} AND contact_id = #{contactId}")
    int physicalDeleteByUserAndContact(@Param("userId") Long userId, @Param("contactId") Long contactId);
}

