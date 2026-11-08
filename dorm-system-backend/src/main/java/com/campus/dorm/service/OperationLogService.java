package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.dto.OperationLogDTO;
import com.campus.dorm.entity.OperationLog;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.OperationLogMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.campus.dorm.vo.OperationLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 操作日志服务
 */
@Slf4j
@Service
public class OperationLogService {
    
    @Resource
    private OperationLogMapper operationLogMapper;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    /**
     * 获取操作日志列表（分页）
     */
    public IPage<OperationLogVO> getOperationLogList(Long userId, Integer pageNum, Integer pageSize) {
        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        
        IPage<OperationLog> logPage = operationLogMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<OperationLogVO> result = logPage.convert(this::convertToVO);
        return result;
    }
    
    /**
     * 记录操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void logOperation(Long userId, OperationLogDTO dto) {
        OperationLog log = new OperationLog();
        BeanUtils.copyProperties(dto, log);
        
        // 设置用户信息
        log.setUserId(userId);
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            log.setUserName(user.getUsername());
        }
        
        // 默认状态为成功
        if (log.getStatus() == null) {
            log.setStatus(1);
        }
        
        operationLogMapper.insert(log);
        OperationLogService.log.info("记录操作日志: 用户={}, 操作={}, 描述={}", userId, dto.getOperationType(), dto.getOperationDesc());
    }
    
    /**
     * 转换为VO
     */
    private OperationLogVO convertToVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setTime(log.getCreatedAt());
        vo.setAction(log.getOperationType());
        vo.setModule(log.getOperationDesc());
        vo.setStatus(log.getStatus() != null && log.getStatus() == 1 ? "success" : "fail");
        vo.setOperator(log.getUserName());
        return vo;
    }
    
}

