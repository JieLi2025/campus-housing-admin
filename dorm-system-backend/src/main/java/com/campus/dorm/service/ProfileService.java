package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.UpdatePasswordDTO;
import com.campus.dorm.dto.UpdateProfileDTO;
import com.campus.dorm.entity.Admin;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormGroup;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.AdminMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormGroupMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.campus.dorm.vo.ProfileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 个人设置服务
 */
@Slf4j
@Service
public class ProfileService {
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private StudentMapper studentMapper;
    
    @Resource
    private AdminMapper adminMapper;
    
    @Resource
    private DormRoomMapper dormRoomMapper;
    
    @Resource
    private BuildingMapper buildingMapper;
    
    @Resource
    private DormGroupMapper dormGroupMapper;

    // Remove SystemConfigService as it is no longer used for invitation code
    // @Resource
    // private SystemConfigService systemConfigService;
    
    /**
     * 获取个人信息
     */
    public ProfileVO getProfile(Long userId) {
        // 1. 查询用户基础信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 构建基础VO
        ProfileVO vo = new ProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getUserType());
        
        // 3. 查询学生详细信息（如果是学生用户）
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(studentWrapper);
        
        if (student != null) {
            // 填充学生特有信息
            vo.setStudentId(student.getStudentId());
            vo.setGender(student.getGender());
            vo.setMajor(student.getMajor());
            vo.setClassName(student.getClassName());
            vo.setBio(student.getBio());
            vo.setTags(student.getTags());
            vo.setBedTime(student.getBedTime() != null ? student.getBedTime() : "23:00");
            vo.setWakeTime(student.getWakeTime() != null ? student.getWakeTime() : "07:00");
            // 头像：统一从sys_user表获取
            vo.setAvatar(user.getAvatar());
            
            // 4. 从dorm_group表查询宿舍分配信息
            DormRoom room = getDormRoomFromDormGroup(student.getId());
            if (room != null) {
                Building building = buildingMapper.selectById(room.getBuildingId());
                if (building != null) {
                    vo.setDormLocation(building.getBuildingName() + "-" + room.getRoomNumber());
                    vo.setRoomId(room.getId());
                    // 从dorm_group.members JSON中获取床位号
                    String bedNumber = getBedNumberFromDormGroup(student.getId());
                    vo.setBedNumber(bedNumber);
                    log.info("从dorm_group表获取到学生{}的宿舍信息: {}, 床位号: {}", student.getId(), vo.getDormLocation(), bedNumber);
                }
            }
        } else {
            // 非学生用户，头像也从sys_user表获取
            vo.setAvatar(user.getAvatar());
            
            // 查询管理员详细信息
            if ("admin".equals(user.getUserType()) || "superadmin".equals(user.getUserType())) {
                LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
                adminWrapper.eq(Admin::getUserId, userId);
                Admin admin = adminMapper.selectOne(adminWrapper);
                
                if (admin != null) {
                    vo.setDepartment(admin.getDepartment());
                    vo.setPosition(admin.getPosition());
                    vo.setOfficeLocation(admin.getOfficeLocation());
                    vo.setWorkHours(admin.getWorkHours());
                    vo.setBio(admin.getBio());
                    
                    // 解析工作时间为上班/下班时间
                    String workHours = admin.getWorkHours();
                    if (workHours != null && workHours.contains("-")) {
                        String[] times = workHours.split("-");
                        if (times.length >= 2) {
                            vo.setWorkStartTime(times[0].trim());
                            vo.setWorkEndTime(times[1].trim());
                        }
                    }
                }
            }
        }
        
        log.info("获取个人信息成功: 用户ID={}, 用户名={}", userId, user.getUsername());
        return vo;
    }
    
    /**
     * 更新个人信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        // 1. 更新用户表（sys_user）中的字段
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 保存旧值，用于同步更新 building 表
        String oldUsername = user.getUsername();
        String oldRealName = user.getRealName();
        
        // 验证邀请码（如果修改了邮箱）- 仅对学生用户生效
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            // 只有学生用户才需要邀请码验证
            if ("student".equals(user.getUserType())) {
                // 查找学生记录
                LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
                studentWrapper.eq(Student::getUserId, userId);
                Student student = studentMapper.selectOne(studentWrapper);
                
                if (student != null) {
                    // 验证邀请码是否正确
                    if (student.getInviteCode() == null || !student.getInviteCode().equals(dto.getInvitationCode())) {
                        throw new RuntimeException("邀请码错误");
                    }
                    
                    // 验证通过，生成新的邀请码并保存
                    String newInviteCode = generateUniqueInviteCode(student.getStudentId());
                    student.setInviteCode(newInviteCode);
                    studentMapper.updateById(student);
                }
            }
            // 管理员和超级管理员可以直接修改邮箱，无需邀请码验证
        }
        
        boolean userUpdated = false;
        // 更新用户表的非空字段
        if (dto.getRealName() != null && !dto.getRealName().isEmpty()) {
            user.setRealName(dto.getRealName());
            userUpdated = true;
        }
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            user.setEmail(dto.getEmail());
            userUpdated = true;
        }
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            user.setPhone(dto.getPhone());
            userUpdated = true;
        }
        
        if (userUpdated) {
            sysUserMapper.updateById(user);
        }
        
        // 2. 更新学生表（student）中的字段
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(wrapper);
        
        if (student != null) {
            boolean studentUpdated = false;
            
            // 更新头像到sys_user表
            if (dto.getAvatar() != null && !dto.getAvatar().isEmpty()) {
                user.setAvatar(dto.getAvatar());
                sysUserMapper.updateById(user);
            }
            if (dto.getBio() != null) {
                student.setBio(dto.getBio());
                studentUpdated = true;
            }
            if (dto.getTags() != null) {
                student.setTags(dto.getTags());
                studentUpdated = true;
            }
            if (dto.getBedTime() != null && !dto.getBedTime().isEmpty()) {
                student.setBedTime(dto.getBedTime());
                studentUpdated = true;
            }
            if (dto.getWakeTime() != null && !dto.getWakeTime().isEmpty()) {
                student.setWakeTime(dto.getWakeTime());
                studentUpdated = true;
            }
            
            if (studentUpdated) {
                studentMapper.updateById(student);
            }
        } else {
            // 3. 更新管理员表（admin）中的字段
            SysUser currentUser = sysUserMapper.selectById(userId);
            if (currentUser != null && ("admin".equals(currentUser.getUserType()) || "superadmin".equals(currentUser.getUserType()))) {
                LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
                adminWrapper.eq(Admin::getUserId, userId);
                Admin admin = adminMapper.selectOne(adminWrapper);
                
                boolean adminUpdated = false;
                boolean isNewAdmin = false;
                
                if (admin == null) {
                    // 如果管理员记录不存在，创建新记录
                    admin = new Admin();
                    admin.setUserId(userId);
                    isNewAdmin = true;
                }
                
                if (dto.getDepartment() != null) {
                    admin.setDepartment(dto.getDepartment());
                    adminUpdated = true;
                }
                if (dto.getPosition() != null) {
                    admin.setPosition(dto.getPosition());
                    adminUpdated = true;
                }
                if (dto.getOfficeLocation() != null) {
                    admin.setOfficeLocation(dto.getOfficeLocation());
                    adminUpdated = true;
                }
                if (dto.getWorkHours() != null) {
                    admin.setWorkHours(dto.getWorkHours());
                    adminUpdated = true;
                }
                if (dto.getBio() != null) {
                    admin.setBio(dto.getBio());
                    adminUpdated = true;
                }
                
                if (adminUpdated) {
                    if (isNewAdmin) {
                        adminMapper.insert(admin);
                    } else {
                        adminMapper.updateById(admin);
                    }
                }
                
                // 🔧 修复：同步更新 building 表中的管理员信息
                // 注意：building.admin_id 外键指向 admin.id，不是 sys_user.id
                Long adminId = admin != null ? admin.getId() : null;
                
                log.info("开始同步更新楼栋管理员信息：userId={}, adminId={}, oldUsername={}, oldRealName={}, newUsername={}, newPhone={}", 
                    userId, adminId, oldUsername, oldRealName, user.getUsername(), user.getPhone());
                
                // 查询该管理员负责的所有楼栋（通过 manager_name 匹配）
                LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
                buildingWrapper.and(w -> w
                    .eq(Building::getManagerName, oldUsername)
                    .or()
                    .eq(Building::getManagerName, oldRealName)
                );
                // 如果有 adminId，也加入查询条件
                if (adminId != null) {
                    buildingWrapper.or().eq(Building::getAdminId, adminId);
                }
                List<Building> buildings = buildingMapper.selectList(buildingWrapper);
                
                log.info("查询到 {} 个楼栋需要同步更新", buildings.size());
                
                for (Building building : buildings) {
                    String oldManagerName = building.getManagerName();
                    String oldManagerPhone = building.getManagerPhone();
                    
                    // 只更新 manager_name 和 manager_phone，不修改 admin_id
                    building.setManagerName(user.getUsername());
                    building.setManagerPhone(user.getPhone());
                    
                    buildingMapper.updateById(building);
                    log.info("同步更新楼栋管理员信息：buildingId={}, buildingName={}, " +
                        "oldManagerName={}, newManagerName={}, oldPhone={}, newPhone={}", 
                        building.getId(), building.getBuildingName(),
                        oldManagerName, building.getManagerName(),
                        oldManagerPhone, building.getManagerPhone());
                }
            }
        }
        
        log.info("更新个人信息成功: 用户ID={}", userId);
    }
    
    /**
     * 修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        // 验证两次密码是否一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证当前密码
        String encryptedCurrentPassword = DigestUtils.md5DigestAsHex(dto.getCurrentPassword().getBytes());
        if (!encryptedCurrentPassword.equals(user.getPassword())) {
            throw new RuntimeException("当前密码不正确");
        }
        
        // 更新密码
        String encryptedNewPassword = DigestUtils.md5DigestAsHex(dto.getNewPassword().getBytes());
        user.setPassword(encryptedNewPassword);
        sysUserMapper.updateById(user);
        
        log.info("修改密码成功: 用户ID={}", userId);
    }

    /**
     * 生成唯一邀请码
     * 格式：YYYY + 6位随机字母数字（与StudentService保持一致）
     */
    private String generateUniqueInviteCode(String studentId) {
        // 使用学号的年份前缀
        String yearPrefix = studentId.length() >= 4 ? studentId.substring(0, 4) : "";
        
        // 生成6位随机字母数字
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉容易混淆的字符
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder code = new StringBuilder(yearPrefix);
        
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 检查是否重复
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getInviteCode, code.toString());
        Student existing = studentMapper.selectOne(wrapper);
        
        // 如果重复，递归重新生成
        if (existing != null) {
            return generateUniqueInviteCode(studentId);
        }
        
        return code.toString();
    }
    
    /**
     * 从DormGroup表获取学生的床位号
     */
    private String getBedNumberFromDormGroup(Long studentId) {
        try {
            // 查询学生所在的宿舍组
            LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.and(wrapper -> wrapper
                            .like(DormGroup::getMembers, "\"id\":" + studentId)
                            .or()
                            .eq(DormGroup::getLeaderId, studentId))
                        .isNotNull(DormGroup::getSelectedRoomId);
            
            DormGroup group = dormGroupMapper.selectOne(groupWrapper);
            
            if (group != null && group.getMembers() != null) {
                // 解析members JSON获取床位号
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<java.util.Map<String, Object>> members = objectMapper.readValue(
                    group.getMembers(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>() {}
                );
                
                for (java.util.Map<String, Object> member : members) {
                    Object idObj = member.get("id");
                    Long memberId = null;
                    if (idObj instanceof Number) {
                        memberId = ((Number) idObj).longValue();
                    }
                    if (memberId != null && memberId.equals(studentId)) {
                        Object bedNumberObj = member.get("bedNumber");
                        if (bedNumberObj != null) {
                            return bedNumberObj.toString();
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从DormGroup获取床位号失败: studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 从DormGroup表获取学生的宿舍房间信息
     */
    private DormRoom getDormRoomFromDormGroup(Long studentId) {
        try {
            // 查询学生所在的宿舍组（使用LIKE查询members字段）
            LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.and(wrapper -> wrapper
                            .like(DormGroup::getMembers, "\"id\":" + studentId)
                            .or()
                            .eq(DormGroup::getLeaderId, studentId))
                        .isNotNull(DormGroup::getSelectedRoomId);
            
            DormGroup group = dormGroupMapper.selectOne(groupWrapper);
            
            if (group != null && group.getSelectedRoomId() != null) {
                return dormRoomMapper.selectById(group.getSelectedRoomId());
            }
        } catch (Exception e) {
            log.warn("从DormGroup获取宿舍信息失败: studentId={}", studentId, e);
        }
        return null;
    }
}

