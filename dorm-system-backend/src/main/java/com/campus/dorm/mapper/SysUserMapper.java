package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户 Mapper
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 物理删除用户（真正删除，绕过软删除）
     * 警告：此操作不可恢复！
     */
    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int hardDelete(@Param("id") Long id);
}

