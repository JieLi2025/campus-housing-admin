package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.MaterialTemplateDTO;
import com.campus.dorm.entity.MaterialTemplate;
import com.campus.dorm.mapper.MaterialTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 宿舍物资模板服务
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Slf4j
@Service
public class MaterialTemplateService {
    
    @Autowired
    private MaterialTemplateMapper materialTemplateMapper;
    
    /**
     * 获取所有物资模板
     */
    public Result<List<MaterialTemplate>> getAllTemplates() {
        log.info("获取所有物资模板");
        
        List<MaterialTemplate> templates = materialTemplateMapper.selectList(null);
        
        log.info("查询到物资模板数量：{}", templates.size());
        return Result.success(templates);
    }
    
    /**
     * 根据楼栋ID和房间类型获取模板（新版本，支持楼栋隔离）
     * 当传入buildingId时，严格按buildingId查询，不回退到buildingType（确保楼栋隔离）
     * 仅当buildingId为空时，才按buildingType查询（向后兼容旧版本）
     * 注意：找不到模板不视为错误，返回空数据即可（允许首次配置的情况）
     */
    public Result<MaterialTemplate> getTemplateByType(Long buildingId, String buildingType, String roomType) {
        log.info("获取物资模板：buildingId={}, buildingType={}, roomType={}", buildingId, buildingType, roomType);
        
        MaterialTemplate template = null;
        
        // 当传入 buildingId 时，严格按 buildingId + roomType 查询（不回退，确保隔离）
        if (buildingId != null) {
            LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MaterialTemplate::getBuildingId, buildingId);
            if (StringUtils.hasText(roomType)) {
                wrapper.eq(MaterialTemplate::getRoomType, roomType);
            }
            wrapper.last("LIMIT 1");
            template = materialTemplateMapper.selectOne(wrapper);
            
            if (template != null) {
                log.info("按buildingId找到物资模板：id={}", template.getId());
            } else {
                log.info("按buildingId未找到物资模板（首次配置），buildingId={}, roomType={}", buildingId, roomType);
            }
            // 无论是否找到，都直接返回，不回退到buildingType查询
            return Result.success(template);
        }
        
        // 仅当 buildingId 为空时，才按 buildingType + roomType 查询（向后兼容旧版本）
        if (StringUtils.hasText(buildingType)) {
            LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MaterialTemplate::getBuildingType, buildingType);
            if (StringUtils.hasText(roomType)) {
                wrapper.eq(MaterialTemplate::getRoomType, roomType);
            }
            wrapper.last("LIMIT 1");
            template = materialTemplateMapper.selectOne(wrapper);
            
            if (template != null) {
                log.info("按buildingType找到物资模板（向后兼容）：id={}", template.getId());
                return Result.success(template);
            }
        }
        
        log.info("未找到物资模板（首次配置），buildingId={}, buildingType={}, roomType={}", buildingId, buildingType, roomType);
        return Result.success(null);
    }
    
    /**
     * 根据楼栋类型和房间类型获取模板（旧版本，向后兼容）
     */
    public Result<MaterialTemplate> getTemplateByType(String buildingType, String roomType) {
        return getTemplateByType(null, buildingType, roomType);
    }
    
    /**
     * 创建或更新物资模板（按buildingId+roomType判断）
     * 如果已存在相同buildingId+roomType的记录，则更新；否则新增
     */
    @Transactional
    public Result<?> createTemplate(MaterialTemplateDTO dto) {
        log.info("创建/更新物资模板：buildingId={}, roomType={}", dto.getBuildingId(), dto.getRoomType());
        
        // 检查是否已存在相同 buildingId + roomType 的记录
        if (dto.getBuildingId() != null && StringUtils.hasText(dto.getRoomType())) {
            LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MaterialTemplate::getBuildingId, dto.getBuildingId())
                   .eq(MaterialTemplate::getRoomType, dto.getRoomType());
            MaterialTemplate existing = materialTemplateMapper.selectOne(wrapper);
            
            if (existing != null) {
                // 已存在，执行更新
                log.info("已存在相同配置，执行更新：id={}", existing.getId());
                existing.setTemplateName(dto.getTemplateName());
                existing.setMaterials(dto.getMaterials());
                if (dto.getBuildingType() != null) {
                    existing.setBuildingType(dto.getBuildingType());
                }
                
                int result = materialTemplateMapper.updateById(existing);
                if (result > 0) {
                    log.info("物资模板更新成功：id={}", existing.getId());
                    return Result.success("物资模板更新成功", existing.getId());
                }
                return Result.error("物资模板更新失败");
            }
        }
        
        // 不存在，执行新增
        MaterialTemplate template = new MaterialTemplate();
        BeanUtils.copyProperties(dto, template);
        
        int result = materialTemplateMapper.insert(template);
        if (result > 0) {
            log.info("物资模板创建成功：id={}", template.getId());
            return Result.success("物资模板创建成功", template.getId());
        }
        
        return Result.error("物资模板创建失败");
    }
    
    /**
     * 更新物资模板
     */
    @Transactional
    public Result<?> updateTemplate(MaterialTemplateDTO dto) {
        log.info("更新物资模板：id={}, buildingId={}", dto.getId(), dto.getBuildingId());
        
        if (dto.getId() == null) {
            return Result.error("模板ID不能为空");
        }
        
        MaterialTemplate existingTemplate = materialTemplateMapper.selectById(dto.getId());
        if (existingTemplate == null) {
            return Result.error("物资模板不存在");
        }
        
        // 更新字段
        if (StringUtils.hasText(dto.getTemplateName())) {
            existingTemplate.setTemplateName(dto.getTemplateName());
        }
        if (dto.getBuildingId() != null) {
            existingTemplate.setBuildingId(dto.getBuildingId());
        }
        if (StringUtils.hasText(dto.getBuildingType())) {
            existingTemplate.setBuildingType(dto.getBuildingType());
        }
        if (StringUtils.hasText(dto.getRoomType())) {
            existingTemplate.setRoomType(dto.getRoomType());
        }
        if (StringUtils.hasText(dto.getMaterials())) {
            existingTemplate.setMaterials(dto.getMaterials());
        }
        
        int result = materialTemplateMapper.updateById(existingTemplate);
        if (result > 0) {
            log.info("物资模板更新成功：id={}", dto.getId());
            return Result.success("物资模板更新成功");
        }
        
        return Result.error("物资模板更新失败");
    }
    
    /**
     * 删除物资模板（硬删除）
     */
    @Transactional
    public Result<?> deleteTemplate(Long id) {
        log.info("删除物资模板：id={}", id);
        
        LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTemplate::getId, id);
        
        int result = materialTemplateMapper.delete(wrapper);
        if (result > 0) {
            log.info("物资模板删除成功：id={}", id);
            return Result.success("物资模板删除成功");
        }
        
        return Result.error("物资模板删除失败");
    }
}

