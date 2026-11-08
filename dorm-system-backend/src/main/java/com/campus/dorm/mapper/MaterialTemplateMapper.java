package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.MaterialTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宿舍物资模板Mapper接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Mapper
public interface MaterialTemplateMapper extends BaseMapper<MaterialTemplate> {
    // MyBatis-Plus提供基础CRUD方法
}

