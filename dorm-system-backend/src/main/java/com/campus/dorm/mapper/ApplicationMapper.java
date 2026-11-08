package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Application;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 申请Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
    
    /**
     * 物理删除申请记录（真正从数据库删除）
     * @param id 申请ID
     * @return 影响的行数
     */
    @Delete("DELETE FROM application WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}

