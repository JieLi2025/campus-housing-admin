package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.StudentImportDTO;
import com.campus.dorm.dto.StudentQueryDTO;
import com.campus.dorm.dto.StudentUpdateDTO;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.InviteCodeGenerateResultVO;
import com.campus.dorm.vo.StudentImportResultVO;
import com.campus.dorm.vo.StudentListVO;
import com.campus.dorm.vo.StudentRelatedDataVO;
import com.campus.dorm.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 学生管理服务
 * 
 * @author Campus Dorm System
 * @date 2024-10-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {
    
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final BuildingMapper buildingMapper;
    private final BuildingMajorAssignmentMapper buildingMajorAssignmentMapper;
    private final DormRoomMapper dormRoomMapper;
    private final RoommateInviteMapper roommateInviteMapper;
    private final DormGroupMapper dormGroupMapper;
    private final ApplicationMapper applicationMapper;
    private final NotificationMapper notificationMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取当前用户ID（从JWT Token）
     */
    private Long getCurrentUserId() {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("获取当前用户ID失败", e);
            return null;
        }
    }
    
    /**
     * 获取当前用户类型
     */
    private String getCurrentUserType() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getUserType() : null;
    }
    
    /**
     * 获取学生列表（分页、搜索、筛选）
     * 普通管理员只能看到自己负责楼栋对应专业/班级的学生
     */
    public Result<StudentListVO> getStudentList(StudentQueryDTO queryDTO) {
        log.info("获取学生列表，查询条件：{}", queryDTO);
        
        // 获取当前用户信息
        String userType = getCurrentUserType();
        Long currentUserId = getCurrentUserId();
        
        // 构建查询条件
        LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
        
        // 只查询未删除的学生
        queryWrapper.eq(Student::getDeleted, 0);
        
        // 声明allowedStudentIds变量，用于权限过滤和统计
        Set<Long> allowedStudentIds = null;
        
        // 权限过滤：普通管理员只能看到自己负责楼栋的学生
        if ("admin".equals(userType) && currentUserId != null) {
            log.info("普通管理员查询学生列表，需要进行权限过滤，管理员ID：{}", currentUserId);
            
            // 1. 获取当前管理员的用户名
            SysUser currentUser = sysUserMapper.selectById(currentUserId);
            if (currentUser == null) {
                log.warn("管理员{}不存在，返回空列表", currentUserId);
                StudentListVO emptyResult = new StudentListVO();
                emptyResult.setStudents(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setCurrentPage(queryDTO.getCurrentPage());
                emptyResult.setPageSize(queryDTO.getPageSize());
                emptyResult.setTotalPages(0L);
                emptyResult.setStats(new StudentListVO.StudentStatsVO());
                return Result.success(emptyResult);
            }
            
            // 2. 🔥 修复：通过 building 表的 manager_name 字段查询管理员负责的楼栋（manager_name存储的是username）
            LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
            buildingWrapper.eq(Building::getManagerName, currentUser.getUsername());
            List<Building> buildings = buildingMapper.selectList(buildingWrapper);
            
            if (buildings.isEmpty()) {
                log.warn("管理员{}未分配任何楼栋，返回空列表", currentUser.getUsername());
                // 未分配楼栋，返回空列表
                StudentListVO emptyResult = new StudentListVO();
                emptyResult.setStudents(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setCurrentPage(queryDTO.getCurrentPage());
                emptyResult.setPageSize(queryDTO.getPageSize());
                emptyResult.setTotalPages(0L);
                emptyResult.setStats(new StudentListVO.StudentStatsVO());
                return Result.success(emptyResult);
            }
            
            List<Long> buildingIds = buildings.stream()
                .map(Building::getId)
                .collect(Collectors.toList());
            log.info("管理员{}负责的楼栋ID列表：{}", currentUser.getUsername(), buildingIds);
            
            // 2. 查询这些楼栋对应的专业/班级分配
            LambdaQueryWrapper<BuildingMajorAssignment> majorWrapper = new LambdaQueryWrapper<>();
            majorWrapper.in(BuildingMajorAssignment::getBuildingId, buildingIds)
                       .eq(BuildingMajorAssignment::getStatus, 1);
            List<BuildingMajorAssignment> majorAssignments = buildingMajorAssignmentMapper.selectList(majorWrapper);
            
            // 3. 构建学生过滤条件
            // 过滤条件包括：
            // a. 学生的专业/班级/年级/性别匹配楼栋专业分配
            // b. 或者学生已分配到这些楼栋的宿舍
            
            allowedStudentIds = new HashSet<>();
            
            // 3a. 根据专业/班级/年级/性别过滤
            if (!majorAssignments.isEmpty()) {
                log.info("根据{}条专业分配规则筛选学生", majorAssignments.size());
                for (BuildingMajorAssignment majorAssignment : majorAssignments) {
                    LambdaQueryWrapper<Student> studentByMajorWrapper = new LambdaQueryWrapper<>();
                    studentByMajorWrapper.eq(Student::getDeleted, 0)
                        .eq(Student::getMajor, majorAssignment.getMajorName())
                        .eq(Student::getClassName, majorAssignment.getClassName())
                        .eq(Student::getGrade, majorAssignment.getGrade());
                    
                    // 性别匹配（统一使用中文：男/女）
                    String assignmentGender = majorAssignment.getGender();
                    if (assignmentGender != null) {
                        studentByMajorWrapper.eq(Student::getGender, assignmentGender);
                        log.info("查询条件：专业={}, 班级={}, 年级={}, 性别={}", 
                            majorAssignment.getMajorName(), 
                            majorAssignment.getClassName(), 
                            majorAssignment.getGrade(), 
                            assignmentGender);
                    }
                    
                    List<Student> students = studentMapper.selectList(studentByMajorWrapper);
                    log.info("专业分配规则：{}-{}-{}-{}，匹配学生数：{}", 
                        majorAssignment.getMajorName(), 
                        majorAssignment.getClassName(), 
                        majorAssignment.getGrade(),
                        assignmentGender,
                        students.size());
                    
                    allowedStudentIds.addAll(students.stream()
                        .map(Student::getId)
                        .collect(Collectors.toSet()));
                }
                log.info("根据专业分配规则，共筛选出{}名学生", allowedStudentIds.size());
            }
            
            // 如果没有任何匹配的学生，返回空列表
            if (allowedStudentIds.isEmpty()) {
                log.warn("⚠️ 管理员{}负责的楼栋没有匹配的学生，返回空列表", currentUserId);
                log.warn("⚠️ 请检查：1.是否已保存专业分配 2.专业/班级/年级/性别是否与学生表一致");
                log.warn("⚠️ 专业分配规则数量：{}", majorAssignments.size());
                if (!majorAssignments.isEmpty()) {
                    log.warn("⚠️ 第一条分配规则示例：专业={}, 班级={}, 年级={}, 性别={}", 
                        majorAssignments.get(0).getMajorName(),
                        majorAssignments.get(0).getClassName(),
                        majorAssignments.get(0).getGrade(),
                        majorAssignments.get(0).getGender());
                }
                StudentListVO emptyResult = new StudentListVO();
                emptyResult.setStudents(Collections.emptyList());
                emptyResult.setTotal(0L);
                emptyResult.setCurrentPage(queryDTO.getCurrentPage());
                emptyResult.setPageSize(queryDTO.getPageSize());
                emptyResult.setTotalPages(0L);
                emptyResult.setStats(new StudentListVO.StudentStatsVO());
                return Result.success(emptyResult);
            }
            
            // 添加学生ID过滤条件
            queryWrapper.in(Student::getId, allowedStudentIds);
            log.info("管理员{}可查看的学生ID数量：{}", currentUserId, allowedStudentIds.size());
        } else if ("superadmin".equals(userType)) {
            log.info("超级管理员查询学生列表，无需权限过滤");
        } else {
            log.warn("无法识别的用户类型或未登录：{}", userType);
        }
        
        // 保存allowedStudentIds用于后续统计（如果是管理员）
        Set<Long> allowedStudentIdsForStats = null;
        if ("admin".equals(userType) && currentUserId != null && allowedStudentIds != null && !allowedStudentIds.isEmpty()) {
            allowedStudentIdsForStats = new HashSet<>(allowedStudentIds);
        }
        
        // 关键词搜索（学号或姓名）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            queryWrapper.and(wrapper -> wrapper
                .like(Student::getStudentId, keyword)
                .or()
                .like(Student::getRealName, keyword)
            );
        }
        
        // 专业筛选
        if (StringUtils.hasText(queryDTO.getMajor())) {
            queryWrapper.eq(Student::getMajor, queryDTO.getMajor());
        }
        
        // 性别筛选
        if (StringUtils.hasText(queryDTO.getGender())) {
            queryWrapper.eq(Student::getGender, queryDTO.getGender());
        }
        
        // 年级筛选
        if (StringUtils.hasText(queryDTO.getGrade())) {
            queryWrapper.eq(Student::getGrade, queryDTO.getGrade());
        }
        
        // 注册状态筛选
        if (StringUtils.hasText(queryDTO.getRegistrationStatus())) {
            if ("registered".equals(queryDTO.getRegistrationStatus())) {
                queryWrapper.eq(Student::getIsRegistered, 1);
            } else if ("unregistered".equals(queryDTO.getRegistrationStatus())) {
                queryWrapper.eq(Student::getIsRegistered, 0);
            }
        }
        
        // 宿舍分配状态筛选
        if (StringUtils.hasText(queryDTO.getAllocationStatus())) {
            if ("allocated".equals(queryDTO.getAllocationStatus())) {
                // 已分配：roomNumber不为空
                queryWrapper.isNotNull(Student::getRoomNumber);
            } else if ("unallocated".equals(queryDTO.getAllocationStatus())) {
                // 未分配：roomNumber为空
                queryWrapper.isNull(Student::getRoomNumber);
            }
        }
        
        // 排序规则：
        // 1. 已分配宿舍的学生按房间号排序在前
        // 2. 未分配宿舍的学生按专业、班级、学号排序在后
        // 使用 CASE WHEN 让 NULL 值排在最后
        queryWrapper.last("ORDER BY CASE WHEN room_number IS NULL THEN 1 ELSE 0 END, " +
                         "room_number ASC, major ASC, class_name ASC, student_id ASC");
        
        // 分页查询
        Page<Student> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        Page<Student> studentPage = studentMapper.selectPage(page, queryWrapper);
        
        // 获取所有学生的用户ID
        List<Long> userIds = studentPage.getRecords().stream()
            .map(Student::getUserId)
            .collect(Collectors.toList());
        
        // 批量查询用户信息
        Map<Long, SysUser> userMap = null;
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(SysUser::getId, user -> user));
        }
        
        // 转换为VO
        Map<Long, SysUser> finalUserMap = userMap;
        List<StudentVO> studentVOS = studentPage.getRecords().stream()
            .map(student -> convertToVO(student, finalUserMap))
            .collect(Collectors.toList());
        
        // 构建返回结果
        StudentListVO result = new StudentListVO();
        result.setStudents(studentVOS);
        result.setTotal(studentPage.getTotal());
        result.setCurrentPage(queryDTO.getCurrentPage());
        result.setPageSize(queryDTO.getPageSize());
        result.setTotalPages(studentPage.getPages());
        
        // 获取统计信息（根据权限过滤）
        result.setStats(getStudentStats(allowedStudentIdsForStats));
        
        // 获取所有可用的专业列表（根据权限过滤）
        result.setAvailableMajors(getAvailableMajors(allowedStudentIdsForStats));
        
        log.info("获取学生列表成功，共 {} 条记录", studentPage.getTotal());
        return Result.success(result);
    }
    
    /**
     * 获取学生详情
     */
    public Result<StudentVO> getStudentDetail(Long studentId) {
        log.info("获取学生详情，studentId：{}", studentId);
        
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getDeleted() == 1) {
            log.warn("学生不存在，studentId：{}", studentId);
            return Result.error("学生不存在");
        }
        
        // 查询用户信息（如果学生已注册）
        Map<Long, SysUser> userMap = new HashMap<>();
        if (student.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(student.getUserId());
            if (user != null) {
                userMap.put(user.getId(), user);
                log.info("找到用户信息，userId：{}", student.getUserId());
            } else {
                log.warn("用户信息不存在，userId：{}，但学生记录存在，返回基本信息", student.getUserId());
            }
        } else {
            log.info("学生未注册，userId为null，返回基本信息");
        }
        
        // 即使未注册，也返回学生基本信息
        StudentVO studentVO = convertToVO(student, userMap);
        
        log.info("获取学生详情成功：{}", studentVO.getStudentId());
        return Result.success(studentVO);
    }
    
    /**
     * 根据学号获取学生详情
     */
    public Result<StudentVO> getStudentByStudentId(String studentNo) {
        log.info("根据学号获取学生详情，学号：{}", studentNo);
        
        LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Student::getStudentId, studentNo)
                    .eq(Student::getDeleted, 0);
        Student student = studentMapper.selectOne(queryWrapper);
        
        if (student == null) {
            log.warn("学生不存在，学号：{}", studentNo);
            return Result.error("学生不存在");
        }
        
        // 查询用户信息（如果学生已注册）
        Map<Long, SysUser> userMap = new HashMap<>();
        if (student.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(student.getUserId());
            if (user != null) {
                userMap.put(user.getId(), user);
                log.info("找到用户信息，userId：{}", student.getUserId());
            } else {
                log.warn("用户信息不存在，userId：{}，但学生记录存在，返回基本信息", student.getUserId());
            }
        } else {
            log.info("学生未注册，userId为null，返回基本信息");
        }
        
        // 即使未注册，也返回学生基本信息
        StudentVO studentVO = convertToVO(student, userMap);
        
        log.info("根据学号获取学生详情成功：{}", studentNo);
        return Result.success(studentVO);
    }
    
    /**
     * 根据用户ID获取学生详情
     */
    public Result<StudentVO> getStudentByUserId(Long userId) {
        log.info("根据用户ID获取学生详情，userId：{}", userId);
        
        LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Student::getUserId, userId)
                    .eq(Student::getDeleted, 0);
        Student student = studentMapper.selectOne(queryWrapper);
        
        if (student == null) {
            log.warn("学生不存在，userId：{}", userId);
            return Result.error("学生不存在");
        }
        
        // 查询用户信息（如果学生已注册）
        Map<Long, SysUser> userMap = new HashMap<>();
        if (student.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(student.getUserId());
            if (user != null) {
                userMap.put(user.getId(), user);
                log.info("找到用户信息，userId：{}", student.getUserId());
            } else {
                log.warn("用户信息不存在，userId：{}，但学生记录存在，返回基本信息", student.getUserId());
            }
        } else {
            log.info("学生未注册，userId为null，返回基本信息");
        }
        
        // 即使未注册，也返回学生基本信息
        StudentVO studentVO = convertToVO(student, userMap);
        
        log.info("根据用户ID获取学生详情成功 - userId: {}, bio: {}, tags: {}, bedTime: {}, wakeTime: {}, dormLocation: {}", 
                 userId, studentVO.getBio(), studentVO.getTags(), 
                 studentVO.getBedTime(), studentVO.getWakeTime(), studentVO.getDormLocation());
        
        return Result.success(studentVO);
    }
    
    /**
     * 获取学生统计信息
     * @param allowedStudentIds 允许统计的学生ID集合（null表示统计所有学生，用于超级管理员）
     */
    private StudentListVO.StudentStatsVO getStudentStats(Set<Long> allowedStudentIds) {
        // 查询总数（只统计未删除的）
        LambdaQueryWrapper<Student> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(Student::getDeleted, 0);
        // 如果有限定的学生ID集合，只统计这些学生
        if (allowedStudentIds != null && !allowedStudentIds.isEmpty()) {
            totalWrapper.in(Student::getId, allowedStudentIds);
        }
        Long total = studentMapper.selectCount(totalWrapper);
        
        // 查询已注册数（只统计未删除的）
        LambdaQueryWrapper<Student> registeredWrapper = new LambdaQueryWrapper<>();
        registeredWrapper.eq(Student::getIsRegistered, 1)
                        .eq(Student::getDeleted, 0);
        // 如果有限定的学生ID集合，只统计这些学生
        if (allowedStudentIds != null && !allowedStudentIds.isEmpty()) {
            registeredWrapper.in(Student::getId, allowedStudentIds);
        }
        Long registered = studentMapper.selectCount(registeredWrapper);
        
        // 计算未注册数和注册率
        Long unregistered = total - registered;
        Double registrationRate = total > 0 ? (registered * 100.0 / total) : 0.0;
        
        StudentListVO.StudentStatsVO stats = new StudentListVO.StudentStatsVO();
        stats.setTotal(total);
        stats.setRegistered(registered);
        stats.setUnregistered(unregistered);
        stats.setRegistrationRate(Math.round(registrationRate * 100.0) / 100.0); // 保留两位小数
        
        return stats;
    }
    
    /**
     * 获取所有可用的专业列表
     * @param allowedStudentIds 允许的学生ID集合（null表示所有学生，用于超级管理员）
     */
    private List<String> getAvailableMajors(Set<Long> allowedStudentIds) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getDeleted, 0)
               .isNotNull(Student::getMajor)
               .select(Student::getMajor);
        
        // 如果有限定的学生ID集合，只查询这些学生的专业
        if (allowedStudentIds != null && !allowedStudentIds.isEmpty()) {
            wrapper.in(Student::getId, allowedStudentIds);
        }
        
        List<Student> students = studentMapper.selectList(wrapper);
        
        // 提取去重并排序的专业列表
        return students.stream()
                .map(Student::getMajor)
                .filter(major -> major != null && !major.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 将Student实体转换为StudentVO
     */
    private StudentVO convertToVO(Student student, Map<Long, SysUser> userMap) {
        StudentVO vo = new StudentVO();
        BeanUtils.copyProperties(student, vo);
        
        // 设置注册状态为Boolean
        vo.setIsRegistered(student.getIsRegistered() != null && student.getIsRegistered() == 1);
        
        // 确保注册时间被正确复制（BeanUtils.copyProperties 会自动复制同名字段，但显式设置更安全）
        vo.setRegistrationDate(student.getRegistrationDate());
        
        // 显式设置个人信息字段（确保不被覆盖，null值保持为null，前端会处理）
        vo.setBio(student.getBio());
        vo.setTags(student.getTags());
        vo.setBedTime(student.getBedTime());
        vo.setWakeTime(student.getWakeTime());
        
        // 统一设置宿舍号字段（roomNumber和dormNumber保持一致）
        String roomNum = student.getRoomNumber();
        vo.setRoomNumber(roomNum);
        vo.setDormNumber(roomNum);
        
        log.info("学生个人信息 - ID: {}, bio: {}, tags: {}, bedTime: {}, wakeTime: {}", 
                  student.getId(), student.getBio(), student.getTags(), 
                  student.getBedTime(), student.getWakeTime());
        
        // 从用户表获取信息
        if (userMap != null && userMap.containsKey(student.getUserId())) {
            SysUser user = userMap.get(student.getUserId());
            vo.setRealName(user.getRealName());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setAvatar(user.getAvatar());
        }
        
        // 从DormGroup表获取宿舍信息和床位号
        DormGroupRoomInfo groupRoomInfo = getDormRoomAndBedNumberFromDormGroup(student.getId());
        if (groupRoomInfo != null && groupRoomInfo.room != null) {
            Building building = buildingMapper.selectById(groupRoomInfo.room.getBuildingId());
            if (building != null) {
                String dormLocation = building.getBuildingName() + "-" + groupRoomInfo.room.getRoomNumber();
                vo.setDormLocation(dormLocation);
                vo.setDormNumber(groupRoomInfo.room.getRoomNumber());
                vo.setRoomNumber(groupRoomInfo.room.getRoomNumber());
                vo.setBedNumber(groupRoomInfo.bedNumber);
                log.info("从dorm_group表获取到学生{}的宿舍信息: {}, 床位号: {}", student.getId(), dormLocation, groupRoomInfo.bedNumber);
            }
        } else {
            log.info("学生未分配宿舍 - 学生ID: {}", student.getId());
        }
        
        return vo;
    }
    
    /**
     * 宿舍组房间信息（包含房间和床位号）
     */
    private static class DormGroupRoomInfo {
        DormRoom room;
        String bedNumber;
        
        DormGroupRoomInfo(DormRoom room, String bedNumber) {
            this.room = room;
            this.bedNumber = bedNumber;
        }
    }
    
    /**
     * 从DormGroup表获取学生的宿舍房间信息和床位号
     * 用于解决bed表记录不一致的问题
     */
    private DormGroupRoomInfo getDormRoomAndBedNumberFromDormGroup(Long studentId) {
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
                DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                
                // 从 members JSON 中提取床位号
                String bedNumber = null;
                if (group.getMembers() != null && !group.getMembers().trim().isEmpty()) {
                    try {
                        List<Map<String, Object>> members = objectMapper.readValue(
                            group.getMembers(),
                            new TypeReference<List<Map<String, Object>>>() {}
                        );
                        for (Map<String, Object> member : members) {
                            Object idObj = member.get("id");
                            if (idObj != null) {
                                Long memberId = ((Number) idObj).longValue();
                                if (memberId.equals(studentId)) {
                                    Object bedNumberObj = member.get("bedNumber");
                                    bedNumber = bedNumberObj != null ? bedNumberObj.toString() : null;
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析members JSON获取床位号失败: studentId={}", studentId, e);
                    }
                }
                
                return new DormGroupRoomInfo(room, bedNumber);
            }
        } catch (Exception e) {
            log.warn("从DormGroup获取宿舍信息失败: studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 导入学生信息（支持Excel和CSV）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<StudentImportResultVO> importStudents(MultipartFile file) {
        log.info("开始导入学生信息，文件名：{}", file.getOriginalFilename());
        
        List<StudentImportDTO> importDataList = new ArrayList<>();
        List<StudentImportResultVO.ErrorDetail> errors = new ArrayList<>();
        
        // 根据文件类型选择解析方式
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return Result.error("文件名不能为空");
        }
        
        try {
            if (filename.toLowerCase().endsWith(".csv")) {
                // 解析CSV文件
                parseCSVFile(file, importDataList, errors);
            } else if (filename.toLowerCase().endsWith(".xlsx")) {
                // 解析Excel文件
                parseExcelFile(file, importDataList, errors);
            } else {
                return Result.error("不支持的文件格式");
            }
        } catch (Exception e) {
            log.error("读取文件失败", e);
            return Result.error("读取文件失败：" + e.getMessage());
        }
        
        log.info("成功解析 {} 条学生数据", importDataList.size());
        
        // 批量插入数据库
        int successCount = 0;
        for (StudentImportDTO dto : importDataList) {
            try {
                // 检查学号是否已存在（只检查未删除的）
                LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Student::getStudentId, dto.getStudentId())
                       .eq(Student::getDeleted, 0);
                Student existingStudent = studentMapper.selectOne(wrapper);
                
                if (existingStudent != null) {
                    errors.add(StudentImportResultVO.ErrorDetail.builder()
                        .studentId(dto.getStudentId())
                        .realName(dto.getRealName())
                        .errorMessage("学号已存在")
                        .build());
                    continue;
                }
                
                // 创建学生记录
                Student student = new Student();
                student.setStudentId(dto.getStudentId());
                student.setRealName(dto.getRealName());
                student.setGender(normalizeGender(dto.getGender()));
                student.setMajor(dto.getMajor());
                student.setGrade(dto.getGrade());
                student.setClassName(dto.getClassName());
                student.setIsRegistered(0);
                student.setDeleted(0);
                
                int result = studentMapper.insert(student);
                if (result > 0) {
                    successCount++;
                }
                
            } catch (Exception e) {
                log.error("插入学生数据失败：{}", dto.getStudentId(), e);
                errors.add(StudentImportResultVO.ErrorDetail.builder()
                    .studentId(dto.getStudentId())
                    .realName(dto.getRealName())
                    .errorMessage("插入失败：" + e.getMessage())
                    .build());
            }
        }
        
        // 构建返回结果
        StudentImportResultVO result = StudentImportResultVO.builder()
            .totalCount(importDataList.size())
            .successCount(successCount)
            .failureCount(errors.size())
            .errors(errors)
            .build();
        
        log.info("学生导入完成，成功：{}，失败：{}", successCount, errors.size());
        return Result.success(result);
    }
    
    /**
     * 批量生成邀请码
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<InviteCodeGenerateResultVO> generateInviteCodes(List<Long> studentIds) {
        log.info("开始批量生成邀请码，学生数：{}", studentIds != null ? studentIds.size() : "全部");
        
        // 查询需要生成邀请码的学生
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        
        // 如果指定了学生ID，则只生成指定学生的邀请码
        if (studentIds != null && !studentIds.isEmpty()) {
            wrapper.in(Student::getId, studentIds);
        }
        
        // 只为未注册且未删除的学生生成邀请码
        wrapper.eq(Student::getIsRegistered, 0)
               .eq(Student::getDeleted, 0);
        
        List<Student> students = studentMapper.selectList(wrapper);
        int totalCount = students.size();
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        
        // 邀请码有效期：30天
        LocalDateTime expiry = LocalDateTime.now().plusDays(30);
        
        for (Student student : students) {
            try {
                // 如果已有邀请码且未过期，跳过
                if (student.getInviteCode() != null && !student.getInviteCode().isEmpty()) {
                    if (student.getInviteCodeExpiry() != null && 
                        student.getInviteCodeExpiry().isAfter(LocalDateTime.now())) {
                        log.info("学生 {} 已有有效邀请码，跳过", student.getStudentId());
                        skippedCount++;
                        continue;
                    }
                }
                
                // 生成唯一邀请码
                String inviteCode = generateUniqueInviteCode(student.getStudentId());
                
                // 更新邀请码
                student.setInviteCode(inviteCode);
                student.setInviteCodeStatus("unused");
                student.setInviteCodeExpiry(expiry);
                
                int result = studentMapper.updateById(student);
                if (result > 0) {
                    successCount++;
                } else {
                    failureCount++;
                }
                
            } catch (Exception e) {
                log.error("为学生 {} 生成邀请码失败", student.getStudentId(), e);
                failureCount++;
            }
        }
        
        // 构建返回结果
        InviteCodeGenerateResultVO result = InviteCodeGenerateResultVO.builder()
            .totalCount(totalCount)
            .successCount(successCount)
            .skippedCount(skippedCount)
            .failureCount(failureCount)
            .message(String.format("成功生成 %d 个邀请码，跳过 %d 个，失败 %d 个", 
                successCount, skippedCount, failureCount))
            .build();
        
        log.info("邀请码生成完成：{}", result.getMessage());
        return Result.success(result);
    }
    
    /**
     * 解析Excel文件
     */
    private void parseExcelFile(MultipartFile file, List<StudentImportDTO> importDataList, 
                                List<StudentImportResultVO.ErrorDetail> errors) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            
            log.info("Excel总行数：{}", lastRowNum + 1);
            
            // 从第2行开始读取数据（第1行是表头）
            for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                
                try {
                    StudentImportDTO dto = parseRow(row, rowIndex);
                    if (dto != null) {
                        importDataList.add(dto);
                    }
                } catch (Exception e) {
                    log.error("解析第 {} 行失败：{}", rowIndex + 1, e.getMessage());
                    errors.add(StudentImportResultVO.ErrorDetail.builder()
                        .rowIndex(rowIndex + 1)
                        .errorMessage("解析失败：" + e.getMessage())
                        .build());
                }
            }
        }
    }
    
    /**
     * 解析CSV文件
     */
    private void parseCSVFile(MultipartFile file, List<StudentImportDTO> importDataList,
                             List<StudentImportResultVO.ErrorDetail> errors) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            
            // 读取表头
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("CSV文件为空");
            }
            
            // 跳过BOM（如果存在）
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }
            
            String[] headers = headerLine.split(",");
            log.info("CSV表头：{}", String.join(", ", headers));
            
            // 读取数据行
            String line;
            int rowIndex = 1;
            while ((line = reader.readLine()) != null) {
                rowIndex++;
                line = line.trim();
                
                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    String[] values = line.split(",", -1); // -1 保留空值
                    StudentImportDTO dto = parseCSVRow(headers, values, rowIndex);
                    if (dto != null) {
                        importDataList.add(dto);
                    }
                } catch (Exception e) {
                    log.error("解析第 {} 行失败：{}", rowIndex, e.getMessage());
                    errors.add(StudentImportResultVO.ErrorDetail.builder()
                        .rowIndex(rowIndex)
                        .errorMessage("解析失败：" + e.getMessage())
                        .build());
                }
            }
            
            log.info("CSV总行数（含表头）：{}", rowIndex);
        }
    }
    
    /**
     * 解析CSV行数据
     */
    private StudentImportDTO parseCSVRow(String[] headers, String[] values, int rowIndex) {
        // 检查是否为空行
        boolean isEmpty = true;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) {
            return null;
        }
        
        StudentImportDTO dto = new StudentImportDTO();
        
        // 根据表头映射数据
        for (int i = 0; i < headers.length && i < values.length; i++) {
            String header = headers[i].trim();
            String value = values[i] != null ? values[i].trim() : "";
            
            switch (header) {
                case "学号":
                    dto.setStudentId(value);
                    break;
                case "姓名":
                    dto.setRealName(value);
                    break;
                case "性别":
                    dto.setGender(value);
                    break;
                case "录取专业":
                    dto.setMajor(value);
                    break;
                case "年级":
                    dto.setGrade(value);
                    break;
                case "班级":
                    dto.setClassName(value);
                    break;
                case "邀请码":
                    // 邀请码可选，忽略
                    break;
                default:
                    log.debug("未识别的列：{}", header);
            }
        }
        
        // 验证必填字段
        if (dto.getStudentId() == null || dto.getStudentId().isEmpty()) {
            throw new RuntimeException("学号不能为空");
        }
        if (dto.getRealName() == null || dto.getRealName().isEmpty()) {
            throw new RuntimeException("姓名不能为空");
        }
        if (dto.getGender() == null || dto.getGender().isEmpty()) {
            throw new RuntimeException("性别不能为空");
        }
        if (dto.getMajor() == null || dto.getMajor().isEmpty()) {
            throw new RuntimeException("专业不能为空");
        }
        if (dto.getGrade() == null || dto.getGrade().isEmpty()) {
            throw new RuntimeException("年级不能为空");
        }
        if (dto.getClassName() == null || dto.getClassName().isEmpty()) {
            throw new RuntimeException("班级不能为空");
        }
        
        return dto;
    }
    
    /**
     * 解析Excel行数据
     */
    private StudentImportDTO parseRow(Row row, int rowIndex) {
        // 检查是否为空行
        if (isRowEmpty(row)) {
            return null;
        }
        
        StudentImportDTO dto = new StudentImportDTO();
        
        // 列映射：0-学号, 1-姓名, 2-性别, 3-专业, 4-年级, 5-班级
        dto.setStudentId(getCellValueAsString(row.getCell(0)));
        dto.setRealName(getCellValueAsString(row.getCell(1)));
        dto.setGender(getCellValueAsString(row.getCell(2)));
        dto.setMajor(getCellValueAsString(row.getCell(3)));
        dto.setGrade(getCellValueAsString(row.getCell(4)));
        dto.setClassName(getCellValueAsString(row.getCell(5)));
        
        // 验证必填字段
        if (dto.getStudentId() == null || dto.getStudentId().trim().isEmpty()) {
            throw new RuntimeException("学号不能为空");
        }
        if (dto.getRealName() == null || dto.getRealName().trim().isEmpty()) {
            throw new RuntimeException("姓名不能为空");
        }
        if (dto.getGender() == null || dto.getGender().trim().isEmpty()) {
            throw new RuntimeException("性别不能为空");
        }
        if (dto.getMajor() == null || dto.getMajor().trim().isEmpty()) {
            throw new RuntimeException("专业不能为空");
        }
        if (dto.getGrade() == null || dto.getGrade().trim().isEmpty()) {
            throw new RuntimeException("年级不能为空");
        }
        if (dto.getClassName() == null || dto.getClassName().trim().isEmpty()) {
            throw new RuntimeException("班级不能为空");
        }
        
        return dto;
    }
    
    /**
     * 检查行是否为空
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取单元格值（统一转为字符串）
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // 数字类型转字符串（处理学号等可能被识别为数字的情况）
                double numericValue = cell.getNumericCellValue();
                // 如果是整数，不显示小数点
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 标准化性别
     * 统一转换为中文：男/女
     */
    private String normalizeGender(String gender) {
        if (gender == null) {
            return null;
        }
        gender = gender.trim();
        // 统一转换为中文
        if ("男".equals(gender) || "Male".equalsIgnoreCase(gender) || "M".equalsIgnoreCase(gender) || "male".equalsIgnoreCase(gender)) {
            return "男";
        } else if ("女".equals(gender) || "Female".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender) || "female".equalsIgnoreCase(gender)) {
            return "女";
        }
        return gender;
    }
    
    /**
     * 生成唯一邀请码
     * 格式：YYYY + 6位随机字母数字
     */
    private String generateUniqueInviteCode(String studentId) {
        // 使用学号的年份前缀
        String yearPrefix = studentId.length() >= 4 ? studentId.substring(0, 4) : "";
        
        // 生成6位随机字母数字
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉容易混淆的字符
        SecureRandom random = new SecureRandom();
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
     * 更新学生信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStudent(StudentUpdateDTO updateDTO) {
        log.info("开始更新学生信息，学生ID：{}", updateDTO.getId());
        
        // 检查学生是否存在且未被删除
        Student student = studentMapper.selectById(updateDTO.getId());
        if (student == null || student.getDeleted() == 1) {
            log.warn("学生不存在或已被删除，ID：{}", updateDTO.getId());
            return Result.error("学生不存在");
        }
        
        // 更新学生信息
        student.setRealName(updateDTO.getRealName());
        student.setGender(updateDTO.getGender());
        student.setMajor(updateDTO.getMajor());
        student.setGrade(updateDTO.getGrade());
        student.setClassName(updateDTO.getClassName());
        student.setBio(updateDTO.getBio());
        student.setTags(updateDTO.getTags());
        
        int result = studentMapper.updateById(student);
        if (result > 0) {
            // 如果学生关联了用户，同步更新用户表的部分信息
            if (student.getUserId() != null) {
                SysUser user = sysUserMapper.selectById(student.getUserId());
                if (user != null) {
                    user.setRealName(updateDTO.getRealName());
                    sysUserMapper.updateById(user);
                }
            }
            
            log.info("学生信息更新成功，学生ID：{}", updateDTO.getId());
            return Result.success("学生信息更新成功");
        } else {
            log.error("学生信息更新失败，学生ID：{}", updateDTO.getId());
            return Result.error("学生信息更新失败");
        }
    }
    
    /**
     * 批量删除学生（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchDeleteStudents(List<Long> studentIds) {
        log.info("开始批量删除学生，学生数：{}", studentIds.size());
        
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("学生ID列表不能为空");
        }
        
        int successCount = 0;
        int failureCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        for (Long studentId : studentIds) {
            try {
                Student student = studentMapper.selectById(studentId);
                // MyBatis Plus 会自动过滤已删除的记录（deleted=1）
                if (student == null) {
                    errorMessages.add("学生ID " + studentId + " 不存在或已删除");
                    failureCount++;
                    continue;
                }
                
                // 检查是否已分配宿舍
                // TODO: 添加宿舍分配检查逻辑
                // if (student.getDormRoomId() != null) {
                //     errorMessages.add("学生 " + student.getRealName() + "（" + student.getStudentId() + "）已分配宿舍，不能删除");
                //     failureCount++;
                //     continue;
                // }
                
                // 软删除学生记录（使用MyBatis Plus的deleteById，会自动处理@TableLogic）
                int result = studentMapper.deleteById(student.getId());
                
                if (result > 0) {
                    // 如果关联了用户账号，也需要软删除用户
                    if (student.getUserId() != null) {
                        sysUserMapper.deleteById(student.getUserId());
                    }
                    successCount++;
                } else {
                    errorMessages.add("删除学生ID " + studentId + " 失败");
                    failureCount++;
                }
                
            } catch (Exception e) {
                log.error("删除学生ID {} 失败", studentId, e);
                errorMessages.add("删除学生ID " + studentId + " 失败：" + e.getMessage());
                failureCount++;
            }
        }
        
        // 构建返回结果
        if (failureCount == 0) {
            log.info("批量删除学生成功，共删除 {} 个学生", successCount);
            return Result.success("成功删除 " + successCount + " 个学生");
        } else if (successCount == 0) {
            log.error("批量删除学生全部失败");
            return Result.error("删除失败：" + String.join("；", errorMessages));
        } else {
            log.warn("批量删除学生部分成功，成功：{}，失败：{}", successCount, failureCount);
            return Result.success(String.format("成功删除 %d 个学生，失败 %d 个：%s", 
                successCount, failureCount, String.join("；", errorMessages)));
        }
    }
    
    /**
     * 删除单个学生（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteStudent(Long studentId) {
        log.info("开始删除学生，学生ID：{}", studentId);
        return batchDeleteStudents(Collections.singletonList(studentId));
    }
    
    /**
     * 批量硬删除学生（永久删除，包含所有关联数据）
     * 警告：此操作不可恢复！
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchHardDeleteStudents(List<Long> studentIds) {
        log.warn("⚠️ 开始批量硬删除学生（不可恢复），学生数：{}", studentIds.size());
        
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("学生ID列表不能为空");
        }
        
        int successCount = 0;
        int failureCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        for (Long studentId : studentIds) {
            try {
                // 查询学生信息（包括软删除的记录）
                LambdaQueryWrapper<Student> studentQuery = new LambdaQueryWrapper<>();
                studentQuery.eq(Student::getId, studentId);
                // 使用selectList并指定deleted条件以绕过软删除过滤
                studentQuery.apply("(deleted = 0 OR deleted = 1)");
                List<Student> students = studentMapper.selectList(studentQuery);
                
                if (students.isEmpty()) {
                    errorMessages.add("学生ID " + studentId + " 不存在");
                    failureCount++;
                    continue;
                }
                
                Student student = students.get(0);
                log.info("硬删除学生：{} ({})", student.getRealName(), student.getStudentId());
                
                // 1. 删除室友邀请记录（作为发送者）
                try {
                    LambdaQueryWrapper<RoommateInvite> senderInviteQuery = new LambdaQueryWrapper<>();
                    senderInviteQuery.eq(RoommateInvite::getSenderId, studentId);
                    int senderInviteCount = roommateInviteMapper.delete(senderInviteQuery);
                    log.info("删除学生作为发送者的邀请记录：{} 条", senderInviteCount);
                } catch (Exception e) {
                    log.warn("删除学生作为发送者的邀请记录失败（可能是表结构不匹配）：{}", e.getMessage());
                }
                
                // 2. 删除室友邀请记录（作为接收者）
                try {
                    LambdaQueryWrapper<RoommateInvite> receiverInviteQuery = new LambdaQueryWrapper<>();
                    receiverInviteQuery.eq(RoommateInvite::getReceiverId, studentId);
                    int receiverInviteCount = roommateInviteMapper.delete(receiverInviteQuery);
                    log.info("删除学生作为接收者的邀请记录：{} 条", receiverInviteCount);
                } catch (Exception e) {
                    log.warn("删除学生作为接收者的邀请记录失败（可能是表结构不匹配）：{}", e.getMessage());
                }
                
                // 3. 处理队伍数据
                try {
                    // 3.1 检查是否是队长
                    LambdaQueryWrapper<DormGroup> leaderGroupQuery = new LambdaQueryWrapper<>();
                    leaderGroupQuery.eq(DormGroup::getLeaderId, studentId);
                    DormGroup leaderGroup = dormGroupMapper.selectOne(leaderGroupQuery);
                    
                    if (leaderGroup != null) {
                        // 如果是队长，直接删除整个队伍
                        // 🔧 修复：如果宿舍组已选择房间，需要释放房间的床位
                        if (leaderGroup.getSelectedRoomId() != null) {
                            DormRoom room = dormRoomMapper.selectById(leaderGroup.getSelectedRoomId());
                            if (room != null) {
                                // 恢复可用床位数（释放整个宿舍组占用的床位）
                                int releasedBeds = leaderGroup.getCurrentMembers() != null ? leaderGroup.getCurrentMembers() : 0;
                                room.setAvailableBeds((room.getAvailableBeds() != null ? room.getAvailableBeds() : 0) + releasedBeds);
                                dormRoomMapper.updateById(room);
                                log.info("释放房间床位：房间ID={}, 释放床位数={}", room.getId(), releasedBeds);
                            }
                        }
                        dormGroupMapper.deleteById(leaderGroup.getId());
                        log.info("删除学生作为队长的队伍：{}", leaderGroup.getId());
                    } else {
                        // 3.2 检查是否是队员（使用LIKE查询JSON字符串，兼容低版本MySQL）
                        LambdaQueryWrapper<DormGroup> memberGroupQuery = new LambdaQueryWrapper<>();
                        memberGroupQuery.like(DormGroup::getMembers, "\"id\":" + studentId);
                        DormGroup memberGroup = dormGroupMapper.selectOne(memberGroupQuery);
                        
                        // 验证JSON中确实包含该studentId
                        if (memberGroup != null) {
                            try {
                                String membersJson = memberGroup.getMembers();
                                if (membersJson != null && !membersJson.trim().isEmpty()) {
                                    List<Map<String, Object>> membersData = objectMapper.readValue(
                                        membersJson, 
                                        new TypeReference<List<Map<String, Object>>>() {}
                                    );
                                    boolean found = membersData.stream()
                                        .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
                                    if (!found) {
                                        memberGroup = null; // 如果JSON中不包含该studentId，设为null
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("验证团队成员JSON失败：groupId={}", memberGroup != null ? memberGroup.getId() : null, e);
                                memberGroup = null; // 解析失败，设为null
                            }
                        }
                        
                        if (memberGroup != null) {
                            // 从队伍成员中移除该学生
                            try {
                                String membersJson = memberGroup.getMembers();
                                if (membersJson != null && !membersJson.isEmpty()) {
                                    // 🔧 修复：从members JSON中真正移除该学生
                                    List<Map<String, Object>> membersData = objectMapper.readValue(
                                        membersJson, 
                                        new TypeReference<List<Map<String, Object>>>() {}
                                    );
                                    
                                    // 移除该学生
                                    membersData.removeIf(m -> {
                                        Object idObj = m.get("id");
                                        if (idObj == null) return false;
                                        return studentId.equals(((Number) idObj).longValue());
                                    });
                                    
                                    // 更新members JSON和成员数
                                    memberGroup.setMembers(objectMapper.writeValueAsString(membersData));
                                    memberGroup.setCurrentMembers(membersData.size());
                                    
                                    // 如果队伍只剩下队长一个人或没人了，删除队伍
                                    if (membersData.size() <= 1) {
                                        // 🔧 修复：如果宿舍组已选择房间，需要释放房间的床位
                                        if (memberGroup.getSelectedRoomId() != null) {
                                            DormRoom room = dormRoomMapper.selectById(memberGroup.getSelectedRoomId());
                                            if (room != null) {
                                                // 恢复可用床位数
                                                int releasedBeds = membersData.size(); // 剩余成员数就是要释放的床位数
                                                room.setAvailableBeds((room.getAvailableBeds() != null ? room.getAvailableBeds() : 0) + releasedBeds);
                                                dormRoomMapper.updateById(room);
                                                log.info("释放房间床位：房间ID={}, 释放床位数={}", room.getId(), releasedBeds);
                                            }
                                        }
                                        dormGroupMapper.deleteById(memberGroup.getId());
                                        log.info("队伍成员不足，删除队伍：{}", memberGroup.getId());
                                    } else {
                                        dormGroupMapper.updateById(memberGroup);
                                        log.info("从队伍中移除学生，剩余成员数：{}", membersData.size());
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("处理队伍成员时出错，将尝试继续删除", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("处理队伍数据失败（可能是表结构不匹配）：{}", e.getMessage());
                }
                
                // 4. 从dorm_group中移除学生并更新房间床位（床位信息现在存储在dorm_group.members中）
                Long roomId = getStudentRoomIdFromDormGroup(studentId);
                if (roomId != null) {
                    // 更新房间的可用床位数
                    DormRoom room = dormRoomMapper.selectById(roomId);
                    if (room != null && room.getAvailableBeds() != null && room.getBedCount() != null) {
                        int newAvailableBeds = Math.min(room.getAvailableBeds() + 1, room.getBedCount());
                        room.setAvailableBeds(newAvailableBeds);
                        dormRoomMapper.updateById(room);
                        log.info("更新房间可用床位：roomId={}, availableBeds={}", roomId, newAvailableBeds);
                    }
                }
                // 注意：从宿舍组中移除学生的逻辑已在上面的队伍处理中完成
                
                // 5. 删除申请记录
                LambdaQueryWrapper<Application> applicationQuery = new LambdaQueryWrapper<>();
                applicationQuery.eq(Application::getStudentId, studentId);
                int applicationCount = applicationMapper.delete(applicationQuery);
                log.info("删除申请记录：{} 条", applicationCount);
                
                // 6. 删除通知记录
                LambdaQueryWrapper<Notification> notificationQuery = new LambdaQueryWrapper<>();
                notificationQuery.eq(Notification::getUserId, studentId);
                int notificationCount = notificationMapper.delete(notificationQuery);
                log.info("删除通知记录：{} 条", notificationCount);
                
                // 7. 硬删除用户账号（物理删除）
                if (student.getUserId() != null) {
                    // 使用自定义的 hardDelete 方法进行物理删除
                    int userDeleted = sysUserMapper.hardDelete(student.getUserId());
                    if (userDeleted > 0) {
                        log.info("✅ 硬删除用户账号：ID={}", student.getUserId());
                    } else {
                        log.warn("⚠️ 用户账号不存在或已被删除：ID={}", student.getUserId());
                    }
                }
                
                // 9. 硬删除学生记录（最后删除）
                // 注意：不能使用 deleteById，因为有 @TableLogic 会执行软删除
                // 必须使用自定义的 hardDelete 方法进行物理删除
                int deletedRows = studentMapper.hardDelete(student.getId());
                if (deletedRows > 0) {
                    log.info("✅ 成功硬删除学生记录：{} ({})", student.getRealName(), student.getStudentId());
                } else {
                    log.warn("⚠️ 学生记录可能已被删除：ID={}", student.getId());
                }
                
                // 10. 更新专业分配规则的allocated_students字段
                try {
                    updateMajorAssignmentAfterDelete(student);
                } catch (Exception e) {
                    log.warn("⚠️ 更新专业分配规则失败（不影响删除操作）：{}", e.getMessage());
                }
                
                successCount++;
                
            } catch (Exception e) {
                log.error("❌ 硬删除学生ID {} 失败", studentId, e);
                errorMessages.add("删除学生ID " + studentId + " 失败：" + e.getMessage());
                failureCount++;
            }
        }
        
        // 构建返回结果
        if (failureCount == 0) {
            log.info("✅ 批量硬删除学生成功，共删除 {} 个学生", successCount);
            return Result.success("成功删除 " + successCount + " 个学生及其所有关联数据");
        } else if (successCount == 0) {
            log.error("❌ 批量硬删除学生全部失败");
            return Result.error("删除失败：" + String.join("；", errorMessages));
        } else {
            log.warn("⚠️ 批量硬删除学生部分成功，成功：{}，失败：{}", successCount, failureCount);
            return Result.success(String.format("成功删除 %d 个学生，失败 %d 个：%s", 
                successCount, failureCount, String.join("；", errorMessages)));
        }
    }
    
    /**
     * 硬删除单个学生（永久删除，包含所有关联数据）
     * 警告：此操作不可恢复！
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> hardDeleteStudent(Long studentId) {
        log.warn("⚠️ 开始硬删除学生（不可恢复），学生ID：{}", studentId);
        return batchHardDeleteStudents(Collections.singletonList(studentId));
    }
    
    /**
     * 获取学生的所有关联数据（删除前预览）
     */
    public Result<StudentRelatedDataVO> getStudentRelatedData(Long studentId) {
        log.info("查询学生关联数据，学生ID：{}", studentId);
        
        // 查询学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        
        StudentRelatedDataVO vo = new StudentRelatedDataVO();
        
        // 1. 学生基本信息
        StudentRelatedDataVO.StudentBasicInfo basicInfo = new StudentRelatedDataVO.StudentBasicInfo();
        basicInfo.setId(student.getId());
        basicInfo.setStudentId(student.getStudentId());
        basicInfo.setRealName(student.getRealName());
        basicInfo.setGender(student.getGender());
        basicInfo.setMajor(student.getMajor());
        basicInfo.setGrade(student.getGrade());
        basicInfo.setClassName(student.getClassName());
        basicInfo.setBio(student.getBio());
        basicInfo.setTags(student.getTags());
        basicInfo.setIsRegistered(student.getIsRegistered());
        basicInfo.setRegistrationDate(student.getRegistrationDate() != null ? 
            student.getRegistrationDate().toString() : null);
        vo.setStudentInfo(basicInfo);
        
        // 2. 用户账号信息
        if (student.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(student.getUserId());
            if (user != null) {
                StudentRelatedDataVO.UserAccountInfo userInfo = new StudentRelatedDataVO.UserAccountInfo();
                userInfo.setUserId(user.getId());
                userInfo.setUsername(user.getUsername());
                userInfo.setRole(user.getUserType());
                userInfo.setStatus(user.getDeleted() == 1 ? "已删除" : "正常");
                vo.setUserAccount(userInfo);
            }
        }
        
        // 3. 室友邀请统计
        StudentRelatedDataVO.InviteStatistics inviteStats = new StudentRelatedDataVO.InviteStatistics();
        
        try {
            // 发送的邀请
            LambdaQueryWrapper<RoommateInvite> sentQuery = new LambdaQueryWrapper<>();
            sentQuery.eq(RoommateInvite::getSenderId, studentId);
            List<RoommateInvite> sentInvites = roommateInviteMapper.selectList(sentQuery);
            inviteStats.setSentCount(sentInvites.size());
            
            // 接收的邀请
            LambdaQueryWrapper<RoommateInvite> receivedQuery = new LambdaQueryWrapper<>();
            receivedQuery.eq(RoommateInvite::getReceiverId, studentId);
            List<RoommateInvite> receivedInvites = roommateInviteMapper.selectList(receivedQuery);
            inviteStats.setReceivedCount(receivedInvites.size());
            
            // 待处理的邀请
            long pendingCount = receivedInvites.stream()
                .filter(invite -> "PENDING".equals(invite.getStatus()))
                .count();
            inviteStats.setPendingCount((int) pendingCount);
        } catch (Exception e) {
            log.warn("查询邀请统计失败（可能是表结构不匹配）：{}", e.getMessage());
            inviteStats.setSentCount(0);
            inviteStats.setReceivedCount(0);
            inviteStats.setPendingCount(0);
        }
        
        vo.setInviteStats(inviteStats);
        
        // 4. 队伍信息
        try {
            // 检查是否是队长
            LambdaQueryWrapper<DormGroup> leaderQuery = new LambdaQueryWrapper<>();
            leaderQuery.eq(DormGroup::getLeaderId, studentId);
            DormGroup leaderGroup = dormGroupMapper.selectOne(leaderQuery);
            
            if (leaderGroup != null) {
                StudentRelatedDataVO.TeamInfo teamInfo = new StudentRelatedDataVO.TeamInfo();
                teamInfo.setTeamId(leaderGroup.getId());
                teamInfo.setTeamName(leaderGroup.getGroupName());
                teamInfo.setRole("队长");
                teamInfo.setMemberCount(leaderGroup.getCurrentMembers());
                teamInfo.setStatus(leaderGroup.getStatus());
                teamInfo.setWarning("⚠️ 删除该学生将解散整个队伍！");
                vo.setTeamInfo(teamInfo);
            } else {
                // 检查是否是队员（使用LIKE查询JSON字符串，兼容低版本MySQL）
                LambdaQueryWrapper<DormGroup> memberQuery = new LambdaQueryWrapper<>();
                memberQuery.like(DormGroup::getMembers, "\"id\":" + studentId);
                DormGroup memberGroup = dormGroupMapper.selectOne(memberQuery);
                
                // 验证JSON中确实包含该studentId
                if (memberGroup != null) {
                    try {
                        String membersJson = memberGroup.getMembers();
                        if (membersJson != null && !membersJson.trim().isEmpty()) {
                            List<Map<String, Object>> membersData = objectMapper.readValue(
                                membersJson, 
                                new TypeReference<List<Map<String, Object>>>() {}
                            );
                            boolean found = membersData.stream()
                                .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
                            if (!found) {
                                memberGroup = null; // 如果JSON中不包含该studentId，设为null
                            }
                        }
                    } catch (Exception e) {
                        log.warn("验证团队成员JSON失败：groupId={}", memberGroup != null ? memberGroup.getId() : null, e);
                        memberGroup = null; // 解析失败，设为null
                    }
                }
                
                if (memberGroup != null) {
                    StudentRelatedDataVO.TeamInfo teamInfo = new StudentRelatedDataVO.TeamInfo();
                    teamInfo.setTeamId(memberGroup.getId());
                    teamInfo.setTeamName(memberGroup.getGroupName());
                    teamInfo.setRole("队员");
                    teamInfo.setMemberCount(memberGroup.getCurrentMembers());
                    teamInfo.setStatus(memberGroup.getStatus());
                    if (memberGroup.getCurrentMembers() <= 2) {
                        teamInfo.setWarning("⚠️ 删除该学生后队伍将只剩队长，队伍可能被解散！");
                    }
                    vo.setTeamInfo(teamInfo);
                }
            }
        } catch (Exception e) {
            log.warn("查询队伍信息失败（可能是表结构不匹配）：{}", e.getMessage());
        }
        
        // 5. 床位分配信息（从dorm_group获取）
        Long roomId = getStudentRoomIdFromDormGroup(studentId);
        String bedNumber = getStudentBedNumberFromDormGroup(studentId);
        
        if (roomId != null) {
            StudentRelatedDataVO.BedAllocationInfo bedInfo = new StudentRelatedDataVO.BedAllocationInfo();
            bedInfo.setBedNumber(bedNumber);
            bedInfo.setStatus("occupied");
            
            // 获取房间信息
            DormRoom room = dormRoomMapper.selectById(roomId);
            if (room != null) {
                bedInfo.setRoomNumber(room.getRoomNumber());
                bedInfo.setRoommateCount(room.getBedCount() - room.getAvailableBeds() - 1); // 减去自己
                
                // 获取楼栋信息
                if (room.getBuildingId() != null) {
                    Building building = buildingMapper.selectById(room.getBuildingId());
                    if (building != null) {
                        bedInfo.setBuildingName(building.getBuildingName());
                    }
                }
            }
            vo.setBedInfo(bedInfo);
        }
        
        // 6. 申请记录统计
        LambdaQueryWrapper<Application> appQuery = new LambdaQueryWrapper<>();
        appQuery.eq(Application::getStudentId, studentId);
        List<Application> applications = applicationMapper.selectList(appQuery);
        
        StudentRelatedDataVO.ApplicationStatistics appStats = new StudentRelatedDataVO.ApplicationStatistics();
        appStats.setTotalCount(applications.size());
        appStats.setPendingCount((int) applications.stream()
            .filter(app -> "PENDING".equals(app.getStatus()))
            .count());
        appStats.setApprovedCount((int) applications.stream()
            .filter(app -> "APPROVED".equals(app.getStatus()))
            .count());
        vo.setApplicationStats(appStats);
        
        // 7. 通知记录统计
        LambdaQueryWrapper<Notification> notifQuery = new LambdaQueryWrapper<>();
        notifQuery.eq(Notification::getUserId, studentId);
        List<Notification> notifications = notificationMapper.selectList(notifQuery);
        
        StudentRelatedDataVO.NotificationStatistics notifStats = new StudentRelatedDataVO.NotificationStatistics();
        notifStats.setTotalCount(notifications.size());
        notifStats.setUnreadCount((int) notifications.stream()
            .filter(n -> !n.getIsRead())
            .count());
        vo.setNotificationStats(notifStats);
        
        // 8. 数据完整性检查
        StudentRelatedDataVO.DataIntegrityCheck integrityCheck = new StudentRelatedDataVO.DataIntegrityCheck();
        integrityCheck.setHasUser(student.getUserId() != null);
        integrityCheck.setHasInvites(inviteStats.getSentCount() + inviteStats.getReceivedCount() > 0);
        integrityCheck.setHasTeam(vo.getTeamInfo() != null);
        integrityCheck.setHasBed(vo.getBedInfo() != null);
        integrityCheck.setHasApplications(appStats.getTotalCount() > 0);
        integrityCheck.setHasNotifications(notifStats.getTotalCount() > 0);
        
        // 生成摘要
        StringBuilder summary = new StringBuilder();
        summary.append("将删除：");
        if (integrityCheck.getHasUser()) summary.append("用户账号、");
        if (integrityCheck.getHasInvites()) summary.append(inviteStats.getSentCount() + inviteStats.getReceivedCount()).append("条邀请、");
        if (integrityCheck.getHasTeam()) summary.append("队伍关系、");
        if (integrityCheck.getHasBed()) summary.append("床位分配、");
        if (integrityCheck.getHasApplications()) summary.append(appStats.getTotalCount()).append("条申请、");
        if (integrityCheck.getHasNotifications()) summary.append(notifStats.getTotalCount()).append("条通知、");
        
        String summaryText = summary.toString();
        if (summaryText.endsWith("、")) {
            summaryText = summaryText.substring(0, summaryText.length() - 1);
        }
        integrityCheck.setSummary(summaryText);
        
        // 生成警告
        if (vo.getTeamInfo() != null && "队长".equals(vo.getTeamInfo().getRole())) {
            integrityCheck.setWarning("⚠️ 该学生是队长，删除将解散整个队伍！");
        } else if (appStats.getPendingCount() > 0) {
            integrityCheck.setWarning("⚠️ 该学生有待处理的申请！");
        }
        
        vo.setIntegrityCheck(integrityCheck);
        
        log.info("查询学生关联数据完成：{}", summaryText);
        return Result.success(vo);
    }
    
    /**
     * 调试方法：对比楼栋专业分配与实际学生数据
     * 找出哪些学生不在专业分配范围内
     */
    public Result<?> debugCompareAssignment() {
        log.info("🔍 开始对比专业分配与学生数据");
        
        // 获取当前用户信息
        String userType = getCurrentUserType();
        Long currentUserId = getCurrentUserId();
        
        if (!"admin".equals(userType) || currentUserId == null) {
            return Result.error("此接口仅供楼栋管理员使用");
        }
        
        // 1. 获取管理员负责的楼栋
        SysUser currentUser = sysUserMapper.selectById(currentUserId);
        if (currentUser == null) {
            return Result.error("管理员不存在");
        }
        
        LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
        buildingWrapper.eq(Building::getManagerName, currentUser.getUsername());
        List<Building> buildings = buildingMapper.selectList(buildingWrapper);
        
        if (buildings.isEmpty()) {
            return Result.error("未分配任何楼栋");
        }
        
        List<Long> buildingIds = buildings.stream()
            .map(Building::getId)
            .collect(Collectors.toList());
        
        log.info("管理员{}负责的楼栋：{}", currentUser.getUsername(), 
            buildings.stream().map(Building::getBuildingName).collect(Collectors.joining(", ")));
        
        // 2. 获取这些楼栋的专业分配规则
        LambdaQueryWrapper<BuildingMajorAssignment> majorWrapper = new LambdaQueryWrapper<>();
        majorWrapper.in(BuildingMajorAssignment::getBuildingId, buildingIds)
                   .eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> majorAssignments = buildingMajorAssignmentMapper.selectList(majorWrapper);
        
        log.info("专业分配规则数量：{}", majorAssignments.size());
        
        // 3. 根据专业分配规则筛选学生
        Set<Long> allowedStudentIds = new HashSet<>();
        Map<String, Object> assignmentStats = new LinkedHashMap<>();
        int totalAllocatedStudents = 0; // 超级管理员分配的总学生数
        
        for (BuildingMajorAssignment assignment : majorAssignments) {
            log.info("处理分配规则：专业={}, 班级={}, 年级={}, 性别={}, 分配数={}", 
                assignment.getMajorName(), 
                assignment.getClassName(), 
                assignment.getGrade(),
                assignment.getGender(),
                assignment.getAllocatedStudents());
            
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getDeleted, 0)
                .eq(Student::getMajor, assignment.getMajorName())
                .eq(Student::getClassName, assignment.getClassName())
                .eq(Student::getGrade, assignment.getGrade());
            
            // 处理性别
            String assignmentGender = assignment.getGender();
            if (assignmentGender != null) {
                String studentGender;
                if ("male".equalsIgnoreCase(assignmentGender)) {
                    studentGender = "男";
                } else if ("female".equalsIgnoreCase(assignmentGender)) {
                    studentGender = "女";
                } else {
                    studentGender = assignmentGender;
                }
                studentWrapper.eq(Student::getGender, studentGender);
            }
            
            List<Student> students = studentMapper.selectList(studentWrapper);
            log.info("查询到{}名学生", students.size());
            
            // 如果查询到学生，打印前3个学生的信息用于调试
            if (!students.isEmpty()) {
                for (int i = 0; i < Math.min(3, students.size()); i++) {
                    Student s = students.get(i);
                    log.info("  学生{}：{} - {} - {} - {} - {}", 
                        i+1, s.getStudentId(), s.getRealName(), s.getMajor(), s.getClassName(), s.getGrade());
                }
            }
            
            String key = String.format("%s-%s-%s-%s", 
                assignment.getMajorName(), 
                assignment.getClassName(), 
                assignment.getGrade(),
                assignmentGender);
            
            // 记录详细信息
            Map<String, Object> detail = new HashMap<>();
            detail.put("allocated", assignment.getAllocatedStudents() != null ? assignment.getAllocatedStudents() : 0);
            detail.put("actual", students.size());
            detail.put("diff", students.size() - (assignment.getAllocatedStudents() != null ? assignment.getAllocatedStudents() : 0));
            
            // 添加学生列表（前5个）用于调试
            List<Map<String, String>> studentList = new ArrayList<>();
            for (int i = 0; i < Math.min(5, students.size()); i++) {
                Student s = students.get(i);
                Map<String, String> studentInfo = new HashMap<>();
                studentInfo.put("studentNumber", s.getStudentId());
                studentInfo.put("name", s.getRealName());
                studentInfo.put("major", s.getMajor());
                studentInfo.put("className", s.getClassName());
                studentInfo.put("grade", s.getGrade());
                studentList.add(studentInfo);
            }
            detail.put("sampleStudents", studentList);
            
            assignmentStats.put(key, detail);
            
            totalAllocatedStudents += (assignment.getAllocatedStudents() != null ? assignment.getAllocatedStudents() : 0);
            
            allowedStudentIds.addAll(students.stream()
                .map(Student::getId)
                .collect(Collectors.toSet()));
        }
        
        log.info("根据专业分配规则，应该有{}名学生", allowedStudentIds.size());
        
        // 4. 获取符合专业分配规则的实际学生（已注册的）
        List<Student> matchedStudents = new ArrayList<>();
        if (!allowedStudentIds.isEmpty()) {
            LambdaQueryWrapper<Student> matchedWrapper = new LambdaQueryWrapper<>();
            matchedWrapper.in(Student::getId, allowedStudentIds)
                         .eq(Student::getDeleted, 0)
                         .eq(Student::getIsRegistered, 1);
            matchedStudents = studentMapper.selectList(matchedWrapper);
        }
        
        log.info("符合专业分配规则且已注册的学生数：{}", matchedStudents.size());
        
        // 5. 获取当前管理员能看到的所有学生（通过getStudentList的逻辑）
        LambdaQueryWrapper<Student> visibleStudentsWrapper = new LambdaQueryWrapper<>();
        visibleStudentsWrapper.eq(Student::getDeleted, 0)
                             .eq(Student::getIsRegistered, 1)
                             .in(Student::getId, allowedStudentIds);
        List<Student> visibleStudents = studentMapper.selectList(visibleStudentsWrapper);
        
        log.info("管理员能看到的学生总数：{}", visibleStudents.size());
        
        // 6. 找出多出来的学生（能看到但不在专业分配规则内的）
        // 注意：由于我们已经注释掉了"已分配学生"的逻辑，理论上不应该有多出的学生
        // 但如果有，说明数据有问题
        List<Map<String, Object>> extraStudents = new ArrayList<>();
        Set<Long> matchedStudentIds = matchedStudents.stream()
            .map(Student::getId)
            .collect(Collectors.toSet());
        
        for (Student student : visibleStudents) {
            if (!matchedStudentIds.contains(student.getId())) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", student.getId());
                info.put("studentNumber", student.getStudentId());
                info.put("name", student.getRealName());
                info.put("gender", student.getGender());
                info.put("major", student.getMajor());
                info.put("className", student.getClassName());
                info.put("grade", student.getGrade());
                info.put("isRegistered", student.getIsRegistered());
                extraStudents.add(info);
            }
        }
        
        // 7. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("buildingNames", buildings.stream()
            .map(Building::getBuildingName)
            .collect(Collectors.joining(", ")));
        result.put("assignmentRulesCount", majorAssignments.size());
        result.put("assignmentStats", assignmentStats);
        result.put("allocatedByAdmin", totalAllocatedStudents); // 超级管理员分配的总数
        result.put("actualInDatabase", allowedStudentIds.size()); // 数据库中实际符合条件的学生数
        result.put("matchedRegisteredCount", matchedStudents.size());
        result.put("visibleStudentCount", visibleStudents.size());
        result.put("extraStudentCount", extraStudents.size());
        result.put("extraStudents", extraStudents);
        result.put("dataDiff", allowedStudentIds.size() - totalAllocatedStudents); // 数据差异
        
        // 添加详细说明
        String explanation;
        if (allowedStudentIds.size() > totalAllocatedStudents) {
            explanation = String.format(
                "⚠️ 数据异常：超级管理员分配了%d名学生，但数据库中有%d名学生符合条件，多出%d人。" +
                "这些多出的学生可能是数据导入错误或班级信息不准确。",
                totalAllocatedStudents,
                allowedStudentIds.size(),
                allowedStudentIds.size() - totalAllocatedStudents
            );
        } else if (allowedStudentIds.size() < totalAllocatedStudents) {
            explanation = String.format(
                "⚠️ 数据异常：超级管理员分配了%d名学生，但数据库中只有%d名学生符合条件，少了%d人。" +
                "部分学生可能未导入或信息不匹配。",
                totalAllocatedStudents,
                allowedStudentIds.size(),
                totalAllocatedStudents - allowedStudentIds.size()
            );
        } else {
            explanation = String.format(
                "✅ 数据正常：超级管理员分配了%d名学生，数据库中也有%d名学生符合条件，数据一致。",
                totalAllocatedStudents,
                allowedStudentIds.size()
            );
        }
        result.put("explanation", explanation);
        
        log.info("对比完成：管理员分配{}人，数据库实际{}人，差异{}人", 
            totalAllocatedStudents, allowedStudentIds.size(), allowedStudentIds.size() - totalAllocatedStudents);
        
        return Result.success(result);
    }
    
    /**
     * 分析专业分配数据不匹配问题
     * 检测哪些班级的实际学生数与分配规则不一致，但不自动修改
     * 需要超级管理员根据楼栋房间容量手动调整分配规则
     */
    public Result<?> analyzeAssignmentMismatch() {
        log.info("🔍 开始分析专业分配数据不匹配问题");
        
        // 获取当前用户信息
        String userType = getCurrentUserType();
        Long currentUserId = getCurrentUserId();
        
        if (!"admin".equals(userType) || currentUserId == null) {
            return Result.error("此接口仅供楼栋管理员使用");
        }
        
        // 1. 获取管理员负责的楼栋
        SysUser currentUser = sysUserMapper.selectById(currentUserId);
        if (currentUser == null) {
            return Result.error("管理员不存在");
        }
        
        LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
        buildingWrapper.eq(Building::getManagerName, currentUser.getUsername());
        List<Building> buildings = buildingMapper.selectList(buildingWrapper);
        
        if (buildings.isEmpty()) {
            return Result.error("未分配任何楼栋");
        }
        
        List<Long> buildingIds = buildings.stream()
            .map(Building::getId)
            .collect(Collectors.toList());
        
        // 2. 获取这些楼栋的专业分配规则
        LambdaQueryWrapper<BuildingMajorAssignment> majorWrapper = new LambdaQueryWrapper<>();
        majorWrapper.in(BuildingMajorAssignment::getBuildingId, buildingIds)
                   .eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> majorAssignments = buildingMajorAssignmentMapper.selectList(majorWrapper);
        
        int mismatchCount = 0;
        int matchCount = 0;
        List<Map<String, Object>> mismatchDetails = new ArrayList<>();
        List<Map<String, Object>> excessStudents = new ArrayList<>();
        
        // 3. 遍历每个分配规则，统计实际学生数并对比
        for (BuildingMajorAssignment assignment : majorAssignments) {
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getDeleted, 0)
                .eq(Student::getMajor, assignment.getMajorName())
                .eq(Student::getClassName, assignment.getClassName())
                .eq(Student::getGrade, assignment.getGrade());
            
            // 处理性别
            String assignmentGender = assignment.getGender();
            if (assignmentGender != null) {
                String studentGender;
                if ("male".equalsIgnoreCase(assignmentGender)) {
                    studentGender = "男";
                } else if ("female".equalsIgnoreCase(assignmentGender)) {
                    studentGender = "女";
                } else {
                    studentGender = assignmentGender;
                }
                studentWrapper.eq(Student::getGender, studentGender);
            }
            
            List<Student> students = studentMapper.selectList(studentWrapper);
            int actualCount = students.size();
            int allocatedCount = assignment.getAllocatedStudents() != null ? assignment.getAllocatedStudents() : 0;
            
            // 如果数量不一致，记录但不修改
            if (actualCount != allocatedCount) {
                mismatchCount++;
                
                Map<String, Object> detail = new HashMap<>();
                detail.put("major", assignment.getMajorName());
                detail.put("className", assignment.getClassName());
                detail.put("grade", assignment.getGrade());
                detail.put("gender", assignmentGender);
                detail.put("allocatedCount", allocatedCount);
                detail.put("actualCount", actualCount);
                detail.put("diff", actualCount - allocatedCount);
                detail.put("buildingId", assignment.getBuildingId());
                mismatchDetails.add(detail);
                
                // 如果实际学生多于分配数，记录多出的学生
                if (actualCount > allocatedCount) {
                    int excessCount = actualCount - allocatedCount;
                    // 获取多出的学生（简单取后面的学生）
                    for (int i = allocatedCount; i < Math.min(actualCount, allocatedCount + 5); i++) {
                        if (i < students.size()) {
                            Student s = students.get(i);
                            Map<String, Object> excessInfo = new HashMap<>();
                            excessInfo.put("studentId", s.getStudentId());
                            excessInfo.put("realName", s.getRealName());
                            excessInfo.put("major", s.getMajor());
                            excessInfo.put("className", s.getClassName());
                            excessInfo.put("grade", s.getGrade());
                            excessStudents.add(excessInfo);
                        }
                    }
                }
                
                log.warn("⚠️ 数据不匹配：{}-{}-{}-{}, 分配{}人但实际{}人，差异{}", 
                    assignment.getMajorName(), assignment.getClassName(), 
                    assignment.getGrade(), assignmentGender, allocatedCount, actualCount, actualCount - allocatedCount);
            } else {
                matchCount++;
            }
        }
        
        // 4. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("totalRules", majorAssignments.size());
        result.put("mismatchCount", mismatchCount);
        result.put("matchCount", matchCount);
        result.put("mismatchDetails", mismatchDetails);
        result.put("excessStudents", excessStudents);
        
        String message;
        String suggestion;
        if (mismatchCount > 0) {
            message = String.format("⚠️ 发现数据不匹配！共检查%d条规则，其中%d条不匹配，%d条正常", 
                majorAssignments.size(), mismatchCount, matchCount);
            suggestion = "建议：请联系超级管理员，根据楼栋房间容量重新调整专业分配规则。" +
                        "不要直接修改allocated_students字段，因为这可能导致房间容量超额。";
        } else {
            message = String.format("✅ 数据检查完成！共检查%d条规则，所有数据都匹配，无问题", 
                majorAssignments.size());
            suggestion = "数据正常，无需处理。";
        }
        result.put("suggestion", suggestion);
        
        log.info(message);
        return Result.success(message, result);
    }
    
    /**
     * 删除学生后更新专业分配规则的allocated_students字段
     * 自动减少对应专业班级的分配人数
     */
    private void updateMajorAssignmentAfterDelete(Student student) {
        log.info("🔄 更新专业分配规则：学生={}, 专业={}, 班级={}, 年级={}, 性别={}", 
            student.getRealName(), student.getMajor(), student.getClassName(), 
            student.getGrade(), student.getGender());
        
        // 查找匹配的专业分配规则
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getMajorName, student.getMajor())
               .eq(BuildingMajorAssignment::getClassName, student.getClassName())
               .eq(BuildingMajorAssignment::getGrade, student.getGrade())
               .eq(BuildingMajorAssignment::getStatus, 1);
        
        // 处理性别匹配
        String studentGender = student.getGender();
        if ("男".equals(studentGender)) {
            wrapper.eq(BuildingMajorAssignment::getGender, "male");
        } else if ("女".equals(studentGender)) {
            wrapper.eq(BuildingMajorAssignment::getGender, "female");
        }
        
        List<BuildingMajorAssignment> assignments = buildingMajorAssignmentMapper.selectList(wrapper);
        
        if (assignments.isEmpty()) {
            log.info("未找到匹配的专业分配规则，无需更新");
            return;
        }
        
        // 更新每个匹配的分配规则
        for (BuildingMajorAssignment assignment : assignments) {
            int oldCount = assignment.getAllocatedStudents() != null ? assignment.getAllocatedStudents() : 0;
            if (oldCount > 0) {
                int newCount = oldCount - 1;
                assignment.setAllocatedStudents(newCount);
                buildingMajorAssignmentMapper.updateById(assignment);
                log.info("✅ 更新专业分配规则：楼栋ID={}, {}-{}-{}, {}人 → {}人", 
                    assignment.getBuildingId(), assignment.getMajorName(), 
                    assignment.getClassName(), assignment.getGrade(), oldCount, newCount);
            } else {
                log.warn("⚠️ 专业分配规则的allocated_students已经是0，无法再减少");
            }
        }
    }
    
    /**
     * 从dorm_group获取学生的房间ID
     */
    private Long getStudentRoomIdFromDormGroup(Long studentId) {
        try {
            // 查询所有已选房间的宿舍组
            LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(DormGroup::getSelectedRoomId);
            List<DormGroup> groups = dormGroupMapper.selectList(wrapper);
            
            for (DormGroup group : groups) {
                if (group.getMembers() == null || group.getMembers().isEmpty()) {
                    continue;
                }
                try {
                    List<Map<String, Object>> members = objectMapper.readValue(
                        group.getMembers(), 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    for (Map<String, Object> member : members) {
                        Object idObj = member.get("id");
                        Long memberId = null;
                        if (idObj instanceof Number) {
                            memberId = ((Number) idObj).longValue();
                        }
                        if (memberId != null && memberId.equals(studentId)) {
                            return group.getSelectedRoomId();
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析宿舍组成员失败：groupId={}", group.getId());
                }
            }
        } catch (Exception e) {
            log.error("获取学生房间ID失败：studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 从dorm_group获取学生的床位号
     */
    private String getStudentBedNumberFromDormGroup(Long studentId) {
        try {
            // 查询所有已选房间的宿舍组
            LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(DormGroup::getSelectedRoomId);
            List<DormGroup> groups = dormGroupMapper.selectList(wrapper);
            
            for (DormGroup group : groups) {
                if (group.getMembers() == null || group.getMembers().isEmpty()) {
                    continue;
                }
                try {
                    List<Map<String, Object>> members = objectMapper.readValue(
                        group.getMembers(), 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    for (Map<String, Object> member : members) {
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
                            return null;
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析宿舍组成员失败：groupId={}", group.getId());
                }
            }
        } catch (Exception e) {
            log.error("获取学生床位号失败：studentId={}", studentId, e);
        }
        return null;
    }
}

