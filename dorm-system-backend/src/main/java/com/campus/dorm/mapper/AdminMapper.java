package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员信息Mapper
 * 
 * @author Campus Dorm System
 * @date 2025-11-02
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    
    /**
     * 根据用户ID查询管理员信息
     * 
     * @param userId 用户ID
     * @return 管理员信息
     */
    @Select("SELECT * FROM admin WHERE user_id = #{userId} AND deleted = 0")
    Admin selectByUserId(@Param("userId") Long userId);
}

