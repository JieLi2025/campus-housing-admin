package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.common.ResultCode;
import com.campus.dorm.dto.CreateAdminDTO;
import com.campus.dorm.dto.ResetPasswordDTO;
import com.campus.dorm.dto.UpdateAdminDTO;
import com.campus.dorm.entity.Admin;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.AdminMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.campus.dorm.vo.AdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员服务
 * 
 * @author Campus Dorm System
 * @date 2024-10-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final SysUserMapper sysUserMapper;
    private final AdminMapper adminMapper;
    private final BuildingMapper buildingMapper;
    
    /**
     * 获取所有普通管理员列表（不包括超级管理员）
     */
    public Result<List<AdminVO>> getAllAdmins() {
        log.info("查询所有普通管理员");
        
        // 只查询 admin 类型的用户，不包括 superadmin
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserType, "admin")
                .eq(SysUser::getStatus, 1)  // 只查询启用状态的
                .orderByAsc(SysUser::getId);
        
        List<SysUser> adminUsers = sysUserMapper.selectList(queryWrapper);
        
        // 转换为VO
        List<AdminVO> adminVOS = adminUsers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        log.info("查询到 {} 个普通管理员", adminVOS.size());
        return Result.success(adminVOS);
    }
    
    /**
     * 获取管理员列表（分页，超级管理员使用）
     */
    public Result<Page<AdminVO>> getAdminList(Integer currentPage, Integer pageSize, 
                                               String keyword, Integer status) {
        log.info("获取管理员列表：currentPage={}, pageSize={}, keyword={}, status={}", 
                currentPage, pageSize, keyword, status);
        
        // 构建查询条件
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserType, "admin");
        
        // 关键词搜索（用户名、真实姓名、邮箱、手机号）
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(SysUser::getUsername, keyword)
                .or().like(SysUser::getRealName, keyword)
                .or().like(SysUser::getEmail, keyword)
                .or().like(SysUser::getPhone, keyword)
            );
        }
        
        // 状态筛选
        if (status != null) {
            queryWrapper.eq(SysUser::getStatus, status);
        }
        
        // 按ID倒序（新创建的管理员ID更大）
        queryWrapper.orderByDesc(SysUser::getId);
        
        // 分页查询
        Page<SysUser> page = new Page<>(currentPage, pageSize);
        Page<SysUser> adminPage = sysUserMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<AdminVO> voPage = new Page<>(adminPage.getCurrent(), adminPage.getSize(), adminPage.getTotal());
        List<AdminVO> adminVOS = adminPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(adminVOS);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取管理员详情
     */
    public Result<AdminVO> getAdminDetail(Long adminId) {
        log.info("获取管理员详情：adminId={}", adminId);
        
        SysUser sysUser = sysUserMapper.selectById(adminId);
        if (sysUser == null) {
            return Result.error("管理员不存在");
        }
        
        if (!"admin".equals(sysUser.getUserType())) {
            return Result.error("该用户不是管理员");
        }
        
        AdminVO vo = convertToVO(sysUser);
        return Result.success(vo);
    }
    
    /**
     * 创建管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createAdmin(CreateAdminDTO dto) {
        log.info("创建管理员：username={}", dto.getUsername());
        
        // 1. 检查用户名是否已存在（只检查未删除的记录）
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, dto.getUsername())
                       .eq(SysUser::getDeleted, 0); // 只检查未删除的记录
        Long usernameCount = sysUserMapper.selectCount(usernameWrapper);
        
        if (usernameCount > 0) {
            return Result.error(ResultCode.ADMIN_USERNAME_EXISTS.getCode(), "用户名已存在，请使用其他用户名");
        }
        
        // 检查用户名是否在已删除记录中存在
        LambdaQueryWrapper<SysUser> deletedWrapper = new LambdaQueryWrapper<>();
        deletedWrapper.eq(SysUser::getUsername, dto.getUsername())
                      .eq(SysUser::getDeleted, 1); // 检查已删除的记录
        Long deletedCount = sysUserMapper.selectCount(deletedWrapper);
        
        if (deletedCount > 0) {
            return Result.error(ResultCode.ADMIN_USERNAME_EXISTS.getCode(), 
                "该用户名已被删除的账户使用，请使用其他用户名或联系管理员物理删除旧记录");
        }
        
        // 2. 检查邮箱是否已存在
        if (StringUtils.hasText(dto.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, dto.getEmail());
            Long emailCount = sysUserMapper.selectCount(emailWrapper);
            
            if (emailCount > 0) {
                return Result.error(ResultCode.ADMIN_EMAIL_EXISTS.getCode(), "邮箱已存在，请使用其他邮箱");
            }
        }
        
        // 3. 检查手机号是否已存在
        if (StringUtils.hasText(dto.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, dto.getPhone());
            Long phoneCount = sysUserMapper.selectCount(phoneWrapper);
            
            if (phoneCount > 0) {
                return Result.error(ResultCode.ADMIN_PHONE_EXISTS.getCode(), "手机号已存在，请使用其他手机号");
            }
        }
        
        // 4. 加密密码
        String encryptedPassword = encryptPassword(dto.getPassword());
        
        // 5. 创建管理员
        SysUser newAdmin = new SysUser();
        newAdmin.setUsername(dto.getUsername());
        newAdmin.setPassword(encryptedPassword);
        newAdmin.setRealName(dto.getRealName());
        newAdmin.setEmail(dto.getEmail());
        newAdmin.setPhone(dto.getPhone());
        newAdmin.setUserType("admin"); // 固定为普通管理员
        newAdmin.setStatus(1); // 默认启用
        
        int result = sysUserMapper.insert(newAdmin);
        if (result > 0) {
            log.info("管理员创建成功：id={}, username={}", newAdmin.getId(), newAdmin.getUsername());
            
            // 6. 插入 admin 表（保持数据一致性）
            Admin admin = new Admin();
            admin.setUserId(newAdmin.getId());
            
            int adminResult = adminMapper.insert(admin);
            if (adminResult <= 0) {
                log.error("admin 表插入失败，user_id={}", newAdmin.getId());
                throw new RuntimeException("管理员信息创建失败");
            }
            
            log.info("管理员信息表创建成功：admin_id={}, user_id={}", admin.getId(), admin.getUserId());
            
            // 返回新创建的管理员信息（包含ID）
            AdminVO adminVO = convertToVO(newAdmin);
            return Result.success("创建成功", adminVO);
        }
        
        return Result.error("创建失败");
    }
    
    /**
     * 更新管理员信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateAdmin(UpdateAdminDTO dto) {
        log.info("更新管理员：id={}", dto.getId());
        
        // 1. 检查管理员是否存在
        SysUser admin = sysUserMapper.selectById(dto.getId());
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        
        if (!"admin".equals(admin.getUserType())) {
            return Result.error("该用户不是管理员");
        }
        
        // 2. 检查用户名是否与其他用户重复
        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(admin.getUsername())) {
            LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(SysUser::getUsername, dto.getUsername())
                    .ne(SysUser::getId, dto.getId());
            Long usernameCount = sysUserMapper.selectCount(usernameWrapper);
            
            if (usernameCount > 0) {
                return Result.error("用户名已存在，请使用其他用户名");
            }
        }
        
        // 3. 检查邮箱是否与其他用户重复
        if (StringUtils.hasText(dto.getEmail()) && !dto.getEmail().equals(admin.getEmail())) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, dto.getEmail())
                    .ne(SysUser::getId, dto.getId());
            Long emailCount = sysUserMapper.selectCount(emailWrapper);
            
            if (emailCount > 0) {
                return Result.error("邮箱已存在，请使用其他邮箱");
            }
        }
        
        // 4. 检查手机号是否与其他用户重复
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(admin.getPhone())) {
            LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(SysUser::getPhone, dto.getPhone())
                    .ne(SysUser::getId, dto.getId());
            Long phoneCount = sysUserMapper.selectCount(phoneWrapper);
            
            if (phoneCount > 0) {
                return Result.error("手机号已存在，请使用其他手机号");
            }
        }
        
        // 5. 更新管理员信息（只更新非空字段）
        // 保存旧值，用于同步更新building表
        String oldUsername = admin.getUsername();
        String oldPhone = admin.getPhone();
        String oldRealName = admin.getRealName(); // 用于兼容旧数据（building.manager_name 可能存储的是 realName）
        
        if (StringUtils.hasText(dto.getUsername())) {
            admin.setUsername(dto.getUsername());
        }
        if (StringUtils.hasText(dto.getRealName())) {
            admin.setRealName(dto.getRealName());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            admin.setEmail(dto.getEmail());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            admin.setPhone(dto.getPhone());
        }
        if (dto.getStatus() != null) {
            admin.setStatus(dto.getStatus());
        }
        
        int result = sysUserMapper.updateById(admin);
        if (result > 0) {
            log.info("管理员更新成功：id={}", admin.getId());
            
            // 🔧 修复：同步更新building表的manager_name和manager_phone字段
            // 通过旧的 manager_name 查询楼栋（不依赖 admin_id，因为外键关系复杂）
            log.info("开始同步更新楼栋管理员信息：userId={}, oldUsername={}, oldRealName={}, newUsername={}, newPhone={}", 
                dto.getId(), oldUsername, oldRealName, admin.getUsername(), admin.getPhone());
            
            // 查询该管理员负责的所有楼栋（通过旧的 username 或 realName 匹配）
            LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
            buildingWrapper.and(w -> w
                .eq(Building::getManagerName, oldUsername)
                .or()
                .eq(Building::getManagerName, oldRealName)
            );
            List<Building> buildings = buildingMapper.selectList(buildingWrapper);
            
            log.info("查询到 {} 个楼栋需要同步更新（manager_name={} 或 {}）", 
                buildings.size(), oldUsername, oldRealName);
            
            for (Building building : buildings) {
                String oldManagerName = building.getManagerName();
                String oldManagerPhone = building.getManagerPhone();
                
                // 只更新 manager_name 和 manager_phone，不修改 admin_id
                building.setManagerName(admin.getUsername());
                building.setManagerPhone(admin.getPhone());
                
                buildingMapper.updateById(building);
                log.info("同步更新楼栋管理员信息：buildingId={}, buildingName={}, " +
                    "oldManagerName={}, newManagerName={}, oldPhone={}, newPhone={}", 
                    building.getId(), building.getBuildingName(),
                    oldManagerName, building.getManagerName(),
                    oldManagerPhone, building.getManagerPhone());
            }
            
            return Result.success("更新成功");
        }
        
        return Result.error("更新失败");
    }
    
    /**
     * 删除管理员（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteAdmin(Long adminId) {
        log.info("删除管理员：adminId={}", adminId);
        
        // 1. 检查管理员是否存在
        SysUser admin = sysUserMapper.selectById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        
        if (!"admin".equals(admin.getUserType())) {
            return Result.error("该用户不是管理员");
        }
        
        // 2. 软删除 admin 表（需要先查询 admin 记录）
        Admin adminInfo = adminMapper.selectByUserId(adminId);
        if (adminInfo != null) {
            adminMapper.deleteById(adminInfo.getId());
            log.info("管理员信息表删除成功：admin_id={}", adminInfo.getId());
        }
        
        // 3. 软删除管理员（MyBatis-Plus会自动处理软删除）
        int result = sysUserMapper.deleteById(adminId);
        
        if (result > 0) {
            log.info("管理员删除成功：adminId={}", adminId);
            return Result.success("删除成功");
        }
        
        return Result.error("删除失败");
    }
    
    /**
     * 切换管理员状态（启用/禁用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> toggleAdminStatus(Long adminId) {
        log.info("切换管理员状态：adminId={}", adminId);
        
        // 1. 检查管理员是否存在
        SysUser admin = sysUserMapper.selectById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        
        if (!"admin".equals(admin.getUserType())) {
            return Result.error("该用户不是管理员");
        }
        
        // 2. 切换状态
        Integer newStatus = admin.getStatus() == 1 ? 0 : 1;
        admin.setStatus(newStatus);
        
        int result = sysUserMapper.updateById(admin);
        if (result > 0) {
            String statusText = newStatus == 1 ? "启用" : "禁用";
            log.info("管理员状态切换成功：adminId={}, newStatus={}", adminId, statusText);
            return Result.success(statusText + "成功");
        }
        
        return Result.error("状态切换失败");
    }
    
    /**
     * 重置管理员密码
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> resetPassword(Long adminId, ResetPasswordDTO dto) {
        log.info("重置管理员密码：adminId={}", adminId);
        
        // 1. 检查管理员是否存在
        SysUser admin = sysUserMapper.selectById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        
        if (!"admin".equals(admin.getUserType())) {
            return Result.error("该用户不是管理员");
        }
        
        // 2. 加密新密码
        String encryptedPassword = encryptPassword(dto.getNewPassword());
        
        // 3. 更新密码
        admin.setPassword(encryptedPassword);
        int result = sysUserMapper.updateById(admin);
        
        if (result > 0) {
            log.info("管理员密码重置成功：adminId={}", adminId);
            return Result.success("密码重置成功");
        }
        
        return Result.error("密码重置失败");
    }
    
    /**
     * 密码加密（使用 MD5）
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 转换为VO
     */
    private AdminVO convertToVO(SysUser sysUser) {
        AdminVO vo = new AdminVO();
        BeanUtils.copyProperties(sysUser, vo);
        
        // 设置状态为字符串
        vo.setStatusText(sysUser.getStatus() == 1 ? "active" : "inactive");
        
        // 查询管理员对应的楼栋（使用manager_name字段，manager_name存储的是username）
        LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
        buildingWrapper.eq(Building::getManagerName, sysUser.getUsername());
        List<Building> buildings = buildingMapper.selectList(buildingWrapper);
        
        if (buildings != null && !buildings.isEmpty()) {
            // 如果管理多栋楼，用逗号分隔显示
            String buildingNames = buildings.stream()
                    .map(Building::getBuildingName)
                    .collect(Collectors.joining(", "));
            vo.setManagedBuilding(buildingNames);
        } else {
            vo.setManagedBuilding("未分配");
        }
        
        return vo;
    }
}

