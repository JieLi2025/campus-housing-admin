package com.campus.dorm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.*;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormGroup;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.entity.BuildingMajorAssignment;
import com.campus.dorm.mapper.*;
import com.campus.dorm.service.DormGroupService;
import com.campus.dorm.service.IStudentAllocationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生分配Service实现类（扩展版）
 * 
 * @author Campus Dorm System
 * @date 2025-12-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAllocationServiceImplExtended implements IStudentAllocationService {
    
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final DormGroupMapper dormGroupMapper;
    private final DormRoomMapper dormRoomMapper;
    private final BuildingMapper buildingMapper;
    private final BuildingMajorAssignmentMapper buildingMajorAssignmentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 从sys_user表获取学生头像
     */
    private String getStudentAvatar(Student student) {
        if (student == null || student.getUserId() == null) {
            return "";
        }
        SysUser user = sysUserMapper.selectById(student.getUserId());
        return user != null && user.getAvatar() != null ? user.getAvatar() : "";
    }
    
    @Override
    public AllocationStatisticsDTO getAllocationStatistics(List<Long> buildingIds) {
        log.info("开始计算学生分配统计数据，楼栋IDs: {}", buildingIds);
        
        AllocationStatisticsDTO dto = new AllocationStatisticsDTO();
        
        try {
            // 1. 待分配学生数量
            // 🔧 修复：按楼栋对应的专业/班级分配范围统计，与学生信息页面保持一致
            Long waitingStudents = countUnassignedStudentsByBuildings(buildingIds);
            dto.setWaitingStudentsCount(waitingStudents != null ? waitingStudents : 0L);
            log.info("待分配学生数: {}", waitingStudents);
            
            // 2-7. 宿舍组和房间统计（按楼栋ID过滤）
            if (buildingIds != null && !buildingIds.isEmpty()) {
                // 按楼栋过滤
                Long incompleteGroups = dormGroupMapper.countIncompleteGroupsByBuildings(buildingIds);
                dto.setIncompleteGroupsCount(incompleteGroups != null ? incompleteGroups : 0L);
                
                Integer incompleteGroupBeds = dormGroupMapper.sumAvailableBedsInIncompleteGroupsByBuildings(buildingIds);
                dto.setIncompleteGroupBeds(incompleteGroupBeds != null ? incompleteGroupBeds : 0);
                
                Long completeGroups = dormGroupMapper.countCompleteGroupsByBuildings(buildingIds);
                dto.setCompleteGroupsCount(completeGroups != null ? completeGroups : 0L);
                
                Long vacantRooms = dormRoomMapper.countVacantRoomsByBuildings(buildingIds);
                dto.setVacantRoomsCount(vacantRooms != null ? vacantRooms : 0L);
                
                Integer vacantRoomBeds = dormRoomMapper.sumVacantRoomBedsByBuildings(buildingIds);
                dto.setVacantRoomBeds(vacantRoomBeds != null ? vacantRoomBeds : 0);
            } else {
                // 全局统计
                Long incompleteGroups = dormGroupMapper.countIncompleteGroups();
                dto.setIncompleteGroupsCount(incompleteGroups != null ? incompleteGroups : 0L);
                
                Integer incompleteGroupBeds = dormGroupMapper.sumAvailableBedsInIncompleteGroups();
                dto.setIncompleteGroupBeds(incompleteGroupBeds != null ? incompleteGroupBeds : 0);
                
                Long completeGroups = dormGroupMapper.countCompleteGroups();
                dto.setCompleteGroupsCount(completeGroups != null ? completeGroups : 0L);
                
                Long vacantRooms = dormRoomMapper.countVacantRooms();
                dto.setVacantRoomsCount(vacantRooms != null ? vacantRooms : 0L);
                
                Integer vacantRoomBeds = dormRoomMapper.sumVacantRoomBeds();
                dto.setVacantRoomBeds(vacantRoomBeds != null ? vacantRoomBeds : 0);
            }
            
            // 7. 总可用床位（未满组 + 空置房间）
            int totalAvailableBeds = dto.getIncompleteGroupBeds() + dto.getVacantRoomBeds();
            dto.setTotalAvailableBeds(totalAvailableBeds);
            
        } catch (Exception e) {
            log.error("计算学生分配统计数据失败", e);
            dto.setWaitingStudentsCount(0L);
            dto.setIncompleteGroupsCount(0L);
            dto.setIncompleteGroupBeds(0);
            dto.setCompleteGroupsCount(0L);
            dto.setVacantRoomsCount(0L);
            dto.setVacantRoomBeds(0);
            dto.setTotalAvailableBeds(0);
        }
        
        return dto;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllocationResultDTO intelligentAssign(IntelligentAssignRequest request) {
        System.out.println("========================================");
        System.out.println("🎯 批量智能分配开始");
        System.out.println("请求参数: " + request);
        System.out.println("📋 studentIds: " + request.getStudentIds());
        System.out.println("🏢 buildingIds: " + request.getBuildingIds());
        System.out.println("📌 strategy: " + request.getStrategy());
        System.out.println("========================================");
        log.info("开始智能分配，请求参数: {}", request);
        
        AllocationResultDTO result = AllocationResultDTO.builder()
                .successCount(0)
                .failedCount(0)
                .skippedCount(0)
                .build();
        
        try {
            // 1. 获取待分配学生列表
            List<Student> students = getUnassignedStudents(request.getStudentIds(), request.getBuildingIds());
            System.out.println("📊 查询到的学生数: " + students.size());
            if (students.isEmpty()) {
                System.out.println("⚠️ 没有找到待分配的学生");
                result.addWarning("没有找到待分配的学生");
                return result;
            }
            
            // 1.1 过滤掉已经分配的学生，防止重复分配
            List<Student> unassignedStudents = new ArrayList<>();
            for (Student student : students) {
                if (isStudentAssigned(student.getId())) {
                    System.out.println("⚠️ 学生 " + student.getRealName() + " (ID=" + student.getId() + ") 已经在宿舍组中，跳过");
                    result.addWarning("学生 " + student.getRealName() + " 已经在宿舍组中，跳过");
                    result.incrementSkippedCount();
                } else {
                    unassignedStudents.add(student);
                }
            }
            
            if (unassignedStudents.isEmpty()) {
                System.out.println("⚠️ 所有学生都已分配");
                result.addWarning("所有学生都已分配");
                return result;
            }
            
            students = unassignedStudents;
            log.info("找到 {} 名待分配学生", students.size());
            
            // 2. 获取未满的宿舍组列表
            List<DormGroup> incompleteGroups = getIncompleteGroups(request.getBuildingIds());
            log.info("找到 {} 个未满宿舍组", incompleteGroups.size());
            
            // 3. 按策略进行智能匹配
            List<Student> remainingStudents = new ArrayList<>();
            
            for (Student student : students) {
                try {
                    DormGroup matchedGroup = findBestMatchGroup(student, incompleteGroups, request);
                    
                    if (matchedGroup != null) {
                        // 添加学生到宿舍组
                        boolean success = addStudentToGroup(student, matchedGroup);
                        if (success) {
                            result.addSuccess(AllocationResultDTO.StudentAssignmentDetail.builder()
                                    .studentId(student.getId())
                                    .studentName(student.getRealName())
                                    .studentNumber(student.getStudentId())
                                    .groupId(matchedGroup.getId())
                                    .groupName(matchedGroup.getGroupName())
                                    .build());
                        } else {
                            // 添加失败，可能是并发导致满了，加入剩余列表
                            remainingStudents.add(student);
                        }
                    } else {
                        // 未找到合适的现有宿舍组，加入剩余列表待新建
                        remainingStudents.add(student);
                    }
                } catch (Exception e) {
                    log.error("分配学生失败: {}", student.getRealName(), e);
                    result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                            .studentId(student.getId())
                            .studentName(student.getRealName())
                            .failureReason("系统错误: " + e.getMessage())
                            .build());
                }
            }
            
            // 4. 为剩余学生创建新宿舍组
            if (!remainingStudents.isEmpty()) {
                log.info("为 {} 名未分配学生创建新宿舍组", remainingStudents.size());
                createNewGroupsForRemainingStudents(remainingStudents, result, request);
            }
            
            log.info("智能分配完成，成功: {}, 失败: {}", result.getSuccessCount(), result.getFailedCount());
            
        } catch (Exception e) {
            log.error("智能分配失败", e);
            result.addWarning("智能分配过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllocationResultDTO assignToGroup(AssignToGroupRequest request) {
        log.info("手动分配学生到宿舍组，请求: {}", request);
        
        AllocationResultDTO result = AllocationResultDTO.builder()
                .successCount(0)
                .failedCount(0)
                .build();
        
        try {
            // 1. 获取学生信息
            Student student = studentMapper.selectById(request.getStudentId());
            if (student == null) {
                result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(request.getStudentId())
                        .failureReason("学生不存在")
                        .build());
                return result;
            }
            
            // 2. 获取宿舍组信息
            DormGroup group = dormGroupMapper.selectById(request.getGroupId());
            if (group == null) {
                result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(student.getId())
                        .studentName(student.getRealName())
                        .failureReason("宿舍组不存在")
                        .build());
                return result;
            }
            
            // 3. 验证是否可以分配
            String validationError = validateAssignment(student, group, request.getForce());
            if (validationError != null && !request.getForce()) {
                result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(student.getId())
                        .studentName(student.getRealName())
                        .failureReason(validationError)
                        .build());
                return result;
            }
            
            // 4. 执行分配
            boolean success = addStudentToGroup(student, group);
            if (success) {
                result.addSuccess(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(student.getId())
                        .studentName(student.getRealName())
                        .studentNumber(student.getStudentId())
                        .groupId(group.getId())
                        .groupName(group.getGroupName())
                        .build());
                
                if (validationError != null) {
                    result.addWarning("强制分配成功，但存在警告: " + validationError);
                }
            } else {
                result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(student.getId())
                        .studentName(student.getRealName())
                        .failureReason("添加到宿舍组失败")
                        .build());
            }
            
        } catch (Exception e) {
            log.error("手动分配失败", e);
            result.addWarning("分配过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllocationResultDTO assignToRoom(AssignToRoomRequest request) {
        log.info("分配学生到房间，请求: {}", request);
        
        AllocationResultDTO result = AllocationResultDTO.builder()
                .successCount(0)
                .failedCount(0)
                .build();
        
        try {
            // 1. 获取房间信息
            DormRoom room = dormRoomMapper.selectById(request.getRoomId());
            if (room == null) {
                result.addWarning("房间不存在");
                return result;
            }
            
            // 2. 检查房间可用床位
            int availableBeds = room.getAvailableBeds() != null ? room.getAvailableBeds() : 0;
            if (availableBeds < request.getStudentIds().size()) {
                result.addWarning(String.format("房间可用床位不足，需要 %d 个，实际只有 %d 个", 
                        request.getStudentIds().size(), availableBeds));
                return result;
            }
            
            // 3. 获取学生列表
            List<Student> students = studentMapper.selectBatchIds(request.getStudentIds());
            if (students.isEmpty()) {
                result.addWarning("未找到学生信息");
                return result;
            }
            
            // 3.1 验证待分配学生的一致性（性别、专业、班级必须一致）
            String studentGender = students.get(0).getGender();
            String studentMajor = students.get(0).getMajor();
            String studentClass = students.get(0).getClassName();
            
            for (Student student : students) {
                if (!student.getGender().equals(studentGender)) {
                    result.addWarning("待分配学生性别不一致，无法分配到同一房间");
                    return result;
                }
                if (!student.getMajor().equals(studentMajor)) {
                    result.addWarning("待分配学生专业不一致，无法分配到同一房间");
                    return result;
                }
                if (!student.getClassName().equals(studentClass)) {
                    result.addWarning("待分配学生班级不一致，无法分配到同一房间");
                    return result;
                }
            }
            
            // 3.2 检查房间是否已有专业/班级限制
            if (room.getMajorName() != null && !room.getMajorName().isEmpty()) {
                if (!room.getMajorName().equals(studentMajor)) {
                    result.addWarning(String.format("房间已分配给专业 [%s]，无法分配 [%s] 专业的学生", 
                            room.getMajorName(), studentMajor));
                    return result;
                }
            }
            
            if (room.getClassName() != null && !room.getClassName().isEmpty()) {
                if (!room.getClassName().equals(studentClass)) {
                    result.addWarning(String.format("房间已分配给班级 [%s]，无法分配 [%s] 班级的学生", 
                            room.getClassName(), studentClass));
                    return result;
                }
            }
            
            // 4. 创建或获取宿舍组
            DormGroup group;
            if (request.getCreateNewGroup()) {
                // 创建新宿舍组
                group = createNewGroupForRoom(students, room);
                if (group == null) {
                    result.addWarning("创建宿舍组失败");
                    return result;
                }
            } else {
                // 查找房间现有宿舍组
                group = findGroupByRoom(request.getRoomId());
                if (group == null) {
                    // 如果房间没有宿舍组，创建一个新的
                    log.info("房间 {} 没有关联的宿舍组，创建新宿舍组", room.getRoomNumber());
                    group = createNewGroupForRoom(students, room);
                    if (group == null) {
                        result.addWarning("创建宿舍组失败");
                        return result;
                    }
                } else {
                    // 4.1 验证学生是否与现有宿舍组匹配
                    if (!group.getGender().equals(studentGender)) {
                        result.addWarning(String.format("房间现有宿舍组性别为 [%s]，无法分配 [%s] 性别的学生", 
                                group.getGender(), studentGender));
                        return result;
                    }
                    
                    if (!group.getMajor().equals(studentMajor)) {
                        result.addWarning(String.format("房间现有宿舍组专业为 [%s]，无法分配 [%s] 专业的学生", 
                                group.getMajor(), studentMajor));
                        return result;
                    }
                    
                    if (!group.getClassName().equals(studentClass)) {
                        result.addWarning(String.format("房间现有宿舍组班级为 [%s]，无法分配 [%s] 班级的学生", 
                                group.getClassName(), studentClass));
                        return result;
                    }
                    
                    // 确保宿舍组关联到房间
                    if (group.getSelectedRoomId() == null) {
                        group.setSelectedRoomId(room.getId());
                        dormGroupMapper.updateById(group);
                        log.info("更新宿舍组 {} 的房间ID为 {}", group.getGroupName(), room.getId());
                    }
                }
            }
            
            // 5. 添加学生到宿舍组
            for (Student student : students) {
                try {
                    boolean success = addStudentToGroup(student, group);
                    if (success) {
                        result.addSuccess(AllocationResultDTO.StudentAssignmentDetail.builder()
                                .studentId(student.getId())
                                .studentName(student.getRealName())
                                .studentNumber(student.getStudentId())
                                .groupId(group.getId())
                                .groupName(group.getGroupName())
                                .roomId(room.getId())
                                .roomNumber(room.getRoomNumber())
                                .build());
                    } else {
                        result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                                .studentId(student.getId())
                                .studentName(student.getRealName())
                                .failureReason("添加到宿舍组失败")
                                .build());
                    }
                } catch (Exception e) {
                    log.error("添加学生到宿舍组失败: {}", student.getRealName(), e);
                    result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                            .studentId(student.getId())
                            .studentName(student.getRealName())
                            .failureReason("系统错误: " + e.getMessage())
                            .build());
                }
            }
            
            // 6. 更新房间占用状态和专业/班级信息
            int newAvailableBeds = room.getAvailableBeds() - result.getSuccessCount();
            room.setAvailableBeds(newAvailableBeds);
            
            // 如果房间之前没有设置专业/班级，现在设置上
            if (result.getSuccessCount() > 0) {
                if (room.getMajorName() == null || room.getMajorName().isEmpty()) {
                    room.setMajorName(studentMajor);
                    log.info("设置房间 {} 的专业为: {}", room.getRoomNumber(), studentMajor);
                }
                if (room.getClassName() == null || room.getClassName().isEmpty()) {
                    room.setClassName(studentClass);
                    log.info("设置房间 {} 的班级为: {}", room.getRoomNumber(), studentClass);
                }
            }
            
            dormRoomMapper.updateById(room);
            
        } catch (Exception e) {
            log.error("分配学生到房间失败", e);
            result.addWarning("分配过程中发生错误: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AllocationResultDTO batchProcessByClass(String className, List<Long> buildingIds) {
        log.info("按班级批量处理，班级: {}, 楼栋IDs: {}", className, buildingIds);
        
        // 创建智能分配请求
        IntelligentAssignRequest request = new IntelligentAssignRequest();
        request.setBuildingIds(buildingIds);
        request.setStrategy("same_class");
        request.setAllowCrossClass(false);
        
        // 获取该班级的所有未分配学生
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getClassName, className);
        List<Student> classStudents = studentMapper.selectList(wrapper);
        
        if (classStudents.isEmpty()) {
            AllocationResultDTO result = AllocationResultDTO.builder()
                    .successCount(0)
                    .failedCount(0)
                    .build();
            result.addWarning("该班级没有学生");
            return result;
        }
        
        // 过滤出未分配的学生
        List<Long> unassignedStudentIds = classStudents.stream()
                .filter(s -> !isStudentAssigned(s.getId()))
                .map(Student::getId)
                .collect(Collectors.toList());
        
        request.setStudentIds(unassignedStudentIds);
        
        // 调用智能分配
        return intelligentAssign(request);
    }
    
    @Override
    public AllocationValidationDTO validateAllocation(List<Long> buildingIds) {
        log.info("验证分配结果，楼栋IDs: {}", buildingIds);
        
        AllocationValidationDTO validation = AllocationValidationDTO.builder()
                .isValid(true)
                .build();
        
        try {
            // 1. 统计总学生数和已分配学生数
            AllocationStatisticsDTO stats = getAllocationStatistics(buildingIds);
            
            int totalStudents = stats.getWaitingStudentsCount().intValue() + 
                    (stats.getCompleteGroupsCount().intValue() * 4); // 简化计算
            int assignedStudents = totalStudents - stats.getWaitingStudentsCount().intValue();
            
            validation.setTotalStudents(totalStudents);
            validation.setAssignedStudents(assignedStudents);
            validation.setUnassignedStudents(stats.getWaitingStudentsCount().intValue());
            
            // 2. 计算完成率
            double completionRate = totalStudents > 0 ? 
                    (double) assignedStudents / totalStudents * 100 : 0;
            validation.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);
            
            // 3. 检查问题
            if (stats.getWaitingStudentsCount() > 0) {
                validation.setIsValid(false);
                validation.addIssue(String.format("还有 %d 名学生未分配宿舍", 
                        stats.getWaitingStudentsCount()));
            }
            
            if (stats.getIncompleteGroupsCount() > 0) {
                validation.addWarning(String.format("有 %d 个宿舍组未满员", 
                        stats.getIncompleteGroupsCount()));
            }
            
            // 4. 检查性别混住（这里简化处理）
            // 实际应该查询数据库检查每个宿舍组的性别一致性
            
            log.info("验证完成，完成率: {}%, 是否通过: {}", 
                    validation.getCompletionRate(), validation.getIsValid());
            
        } catch (Exception e) {
            log.error("验证分配结果失败", e);
            validation.setIsValid(false);
            validation.addIssue("验证过程中发生错误: " + e.getMessage());
        }
        
        return validation;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 根据楼栋ID统计未分配学生数量
     * 🔧 修复：按楼栋对应的专业/班级分配范围统计，与学生信息页面保持一致
     * 
     * @param buildingIds 楼栋ID列表
     * @return 未分配学生数量
     */
    private Long countUnassignedStudentsByBuildings(List<Long> buildingIds) {
        log.info("📊 开始统计未分配学生数量，楼栋IDs: {}", buildingIds);
        
        if (buildingIds == null || buildingIds.isEmpty()) {
            // 没有指定楼栋，统计所有未分配学生
            Long count = studentMapper.countUnassignedStudents();
            log.info("📊 未指定楼栋，统计所有未分配学生: {}", count);
            return count;
        }
        
        // 1. 查询这些楼栋对应的专业/班级分配
        LambdaQueryWrapper<BuildingMajorAssignment> majorWrapper = new LambdaQueryWrapper<>();
        majorWrapper.in(BuildingMajorAssignment::getBuildingId, buildingIds)
                   .eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> majorAssignments = buildingMajorAssignmentMapper.selectList(majorWrapper);
        
        log.info("📊 楼栋 {} 的专业分配规则数量: {}", buildingIds, majorAssignments.size());
        
        if (majorAssignments.isEmpty()) {
            log.warn("📊 楼栋 {} 没有专业分配规则，返回0", buildingIds);
            return 0L;
        }
        
        // 2. 根据专业/班级/年级/性别查询符合条件的学生ID
        Set<Long> allowedStudentIds = new HashSet<>();
        for (BuildingMajorAssignment assignment : majorAssignments) {
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getDeleted, 0)
                .eq(Student::getMajor, assignment.getMajorName())
                .eq(Student::getClassName, assignment.getClassName())
                .eq(Student::getGrade, assignment.getGrade());
            
            // 性别匹配
            if (assignment.getGender() != null) {
                studentWrapper.eq(Student::getGender, assignment.getGender());
            }
            
            List<Student> students = studentMapper.selectList(studentWrapper);
            log.info("📊 专业分配规则 [专业={}, 班级={}, 年级={}, 性别={}] 匹配学生数: {}", 
                assignment.getMajorName(), assignment.getClassName(), 
                assignment.getGrade(), assignment.getGender(), students.size());
            
            allowedStudentIds.addAll(students.stream()
                .map(Student::getId)
                .collect(Collectors.toSet()));
        }
        
        if (allowedStudentIds.isEmpty()) {
            log.warn("📊 楼栋 {} 的专业分配范围内没有学生", buildingIds);
            return 0L;
        }
        
        log.info("📊 楼栋 {} 的专业分配范围内共有 {} 名学生（去重后）", buildingIds, allowedStudentIds.size());
        
        // 3. 统计这些学生中未分配的数量（不在任何宿舍组中）
        long unassignedCount = 0;
        long assignedCount = 0;
        for (Long studentId : allowedStudentIds) {
            if (!isStudentAssigned(studentId)) {
                unassignedCount++;
            } else {
                assignedCount++;
            }
        }
        
        log.info("📊 楼栋 {} 统计结果: 总学生={}, 已分配={}, 未分配={}", 
            buildingIds, allowedStudentIds.size(), assignedCount, unassignedCount);
        return unassignedCount;
    }
    
    /**
     * 获取未分配的学生列表
     * 🔧 修复：不再限制只查询已注册学生，因为管理员可能在学生注册前就给其分配宿舍
     */
    private List<Student> getUnassignedStudents(List<Long> studentIds, List<Long> buildingIds) {
        System.out.println("🔍 查询未分配学生 - studentIds: " + studentIds + ", buildingIds: " + buildingIds);
        
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        
        // ⚠️ 关键修复：如果指定了学生ID，只查询这些学生（必须放在最前面）
        if (studentIds != null && !studentIds.isEmpty()) {
            wrapper.in(Student::getId, studentIds);
            System.out.println("   限定学生ID: " + studentIds);
        }
        // 只查询未删除的学生
        wrapper.eq(Student::getDeleted, 0);
        
        // 如果指定了楼栋，根据楼栋的性别类型过滤学生
        if (buildingIds != null && !buildingIds.isEmpty()) {
            List<String> buildingTypes = buildingMapper.getBuildingGenders(buildingIds);
            System.out.println("   楼栋类型: " + buildingTypes);
            
            if (buildingTypes != null && !buildingTypes.isEmpty()) {
                Set<String> targetGenders = new HashSet<>();
                for (String type : buildingTypes) {
                    // buildingType现在已经是中文了：男/女
                    if ("男".equals(type)) {
                        targetGenders.add("男");
                    } else if ("女".equals(type)) {
                        targetGenders.add("女");
                    }
                }
                
                if (!targetGenders.isEmpty()) {
                    wrapper.in(Student::getGender, targetGenders);
                    System.out.println("   转换后的目标性别: " + targetGenders);
                }
            }
        }
        
        List<Student> allStudents = studentMapper.selectList(wrapper);
        System.out.println("   查询到学生: " + allStudents.size() + " 人");
        
        // 过滤出未分配的学生（不在complete/room_selected/full状态的宿舍组中）
        List<Student> unassignedStudents = new ArrayList<>();
        for (Student student : allStudents) {
            boolean isAssigned = isStudentAssigned(student.getId());
            System.out.println("   学生 " + student.getRealName() + " (ID=" + student.getId() + ") 已分配: " + isAssigned);
            if (!isAssigned) {
                unassignedStudents.add(student);
            }
        }
        
        System.out.println("✅ 未分配学生: " + unassignedStudents.size() + " 人");
        return unassignedStudents;
    }
    
    /**
     * 获取未满的宿舍组列表
     */
    private List<DormGroup> getIncompleteGroups(List<Long> buildingIds) {
        log.info("🔍 获取未满宿舍组，楼栋IDs: {}", buildingIds);
        
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getStatus, "forming");
        
        // 如果指定了楼栋，需要过滤出这些楼栋的宿舍组
        if (buildingIds != null && !buildingIds.isEmpty()) {
            // 通过关联查询房间和楼栋来过滤宿舍组
            // 获取这些楼栋下的所有房间ID
            LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.in(DormRoom::getBuildingId, buildingIds);
            List<DormRoom> rooms = dormRoomMapper.selectList(roomWrapper);
            
            // 提取房间ID列表
            List<Long> roomIds = new ArrayList<>();
            if (!rooms.isEmpty()) {
                roomIds = rooms.stream()
                        .map(DormRoom::getId)
                        .collect(Collectors.toList());
            }
            
            // 映射性别
            List<String> buildingTypes = buildingMapper.getBuildingGenders(buildingIds);
            List<String> targetGenders = new ArrayList<>();
            if (buildingTypes != null) {
                for (String type : buildingTypes) {
                    // buildingType现在已经是中文了：男/女
                    if ("男".equals(type)) targetGenders.add("男");
                    else if ("女".equals(type)) targetGenders.add("女");
                }
            }
            
            // 构建复杂查询：
            // (selectedRoomId IN roomIds) OR (selectedRoomId IS NULL AND gender IN targetGenders)
            // 注意：MyBatis-Plus的and/or嵌套写法
            final List<Long> finalRoomIds = roomIds;
            final List<String> finalTargetGenders = targetGenders;
            
            wrapper.and(w -> {
                if (!finalRoomIds.isEmpty()) {
                    w.in(DormGroup::getSelectedRoomId, finalRoomIds);
                }
                if (!finalTargetGenders.isEmpty()) {
                    w.or(sub -> sub.isNull(DormGroup::getSelectedRoomId)
                                   .in(DormGroup::getGender, finalTargetGenders));
                }
            });
        }
        
        List<DormGroup> groups = dormGroupMapper.selectList(wrapper);
        log.info("🔍 找到 {} 个未满宿舍组", groups.size());
        for (DormGroup g : groups) {
            log.info("🔍   - 宿舍组: {} (ID={}), 性别={}, 专业={}, 班级={}, 人数={}/{}, 房间ID={}", 
                g.getGroupName(), g.getId(), g.getGender(), g.getMajor(), g.getClassName(),
                g.getCurrentMembers(), g.getMaxMembers(), g.getSelectedRoomId());
        }
        return groups;
    }
    
    /**
     * 查找最佳匹配的宿舍组
     */
    private DormGroup findBestMatchGroup(Student student, List<DormGroup> groups, IntelligentAssignRequest request) {
        log.info("🎯 为学生 {} 查找匹配宿舍组，性别={}, 专业={}, 班级={}", 
            student.getRealName(), student.getGender(), student.getMajor(), student.getClassName());
        
        if (groups.isEmpty()) {
            log.info("🎯 没有可用的未满宿舍组");
            return null;
        }
        
        // 按策略排序
        List<DormGroup> sortedGroups = new ArrayList<>(groups);
        
        if ("same_class".equals(request.getStrategy())) {
            // 同班级优先
            sortedGroups.sort((g1, g2) -> {
                boolean g1Match = student.getClassName().equals(g1.getClassName());
                boolean g2Match = student.getClassName().equals(g2.getClassName());
                if (g1Match && !g2Match) return -1;
                if (!g1Match && g2Match) return 1;
                return 0;
            });
        } else if ("same_major".equals(request.getStrategy())) {
            // 同专业优先
            sortedGroups.sort((g1, g2) -> {
                boolean g1Match = student.getMajor().equals(g1.getMajor());
                boolean g2Match = student.getMajor().equals(g2.getMajor());
                if (g1Match && !g2Match) return -1;
                if (!g1Match && g2Match) return 1;
                return 0;
            });
        }
        
        // 返回第一个匹配的宿舍组
        for (DormGroup group : sortedGroups) {
            log.info("🎯   检查宿舍组: {} (ID={}), 性别={}, 专业={}, 班级={}, 人数={}/{}", 
                group.getGroupName(), group.getId(), group.getGender(), group.getMajor(), 
                group.getClassName(), group.getCurrentMembers(), group.getMaxMembers());
            
            // 检查是否还有空床位
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                log.info("🎯     ❌ 已满员，跳过");
                continue;
            }
            
            // 检查性别匹配
            if (!student.getGender().equals(group.getGender())) {
                log.info("🎯     ❌ 性别不匹配: 学生={}, 宿舍组={}", student.getGender(), group.getGender());
                continue;
            }
            
            // 严格检查班级（如果不允许跨班级）
            if (!Boolean.TRUE.equals(request.getAllowCrossClass()) && 
                !student.getClassName().equals(group.getClassName())) {
                log.info("🎯     ❌ 班级不匹配: 学生={}, 宿舍组={}", student.getClassName(), group.getClassName());
                continue;
            }
            
            // 严格检查专业（如果不允许跨专业）
            if (!Boolean.TRUE.equals(request.getAllowCrossMajor()) && 
                !student.getMajor().equals(group.getMajor())) {
                log.info("🎯     ❌ 专业不匹配: 学生={}, 宿舍组={}", student.getMajor(), group.getMajor());
                continue;
            }
            
            log.info("🎯     ✅ 找到匹配的宿舍组: {}", group.getGroupName());
            return group;
        }
        
        log.info("🎯 没有找到匹配的宿舍组");
        return null;
    }
    
    /**
     * 验证学生是否可以分配到宿舍组
     */
    private String validateAssignment(Student student, DormGroup group, Boolean force) {
        // 1. 检查宿舍组是否已满
        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            return "宿舍组已满员";
        }
        
        // 2. 检查性别匹配
        if (!student.getGender().equals(group.getGender())) {
            return "性别不匹配";
        }
        
        // 3. 检查专业匹配（警告级别）
        if (!force && !student.getMajor().equals(group.getMajor())) {
            return "专业不匹配（可强制分配）";
        }
        
        // 4. 检查班级匹配（警告级别）
        if (!force && !student.getClassName().equals(group.getClassName())) {
            return "班级不匹配（可强制分配）";
        }
        
        return null;
    }
    
    /**
     * 添加学生到宿舍组
     * 🔧 修复：同步更新房间的 availableBeds 字段
     * 🔧 新增：自动分配 bedNumber
     */
    private boolean addStudentToGroup(Student student, DormGroup group) {
        try {
            // 1. 解析现有成员列表
            List<Map<String, Object>> members;
            try {
                members = objectMapper.readValue(group.getMembers(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                members = new ArrayList<>();
            }
            
            // 2. 检查学生是否已在组中
            boolean exists = members.stream()
                    .anyMatch(m -> student.getId().equals(((Number) m.get("id")).longValue()));
            
            if (exists) {
                log.warn("学生已在宿舍组中: {}", student.getRealName());
                return false;
            }
            
            // 3. 计算下一个可用的床位号
            String nextBedNumber = DormGroupService.getNextBedNumber(members);
            
            // 4. 添加新成员（包含 bedNumber）
            Map<String, Object> newMember = new HashMap<>();
            newMember.put("id", student.getId());
            newMember.put("name", student.getRealName());
            newMember.put("studentId", student.getStudentId());
            newMember.put("major", student.getMajor());
            newMember.put("className", student.getClassName());
            newMember.put("isLeader", false);
            newMember.put("avatar", getStudentAvatar(student));
            DormGroupService.assignBedNumberToMember(newMember, nextBedNumber);
            
            members.add(newMember);
            
            // 4. 更新宿舍组
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            
            // 如果满员，更新状态
            if (group.getCurrentMembers() >= group.getMaxMembers()) {
                group.setStatus("full");
            }
            
            // 5. 🔧 修复：同步更新房间的 availableBeds
            if (group.getSelectedRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                if (room != null && room.getAvailableBeds() != null && room.getAvailableBeds() > 0) {
                    room.setAvailableBeds(room.getAvailableBeds() - 1);
                    dormRoomMapper.updateById(room);
                    log.info("更新房间 {} 的可用床位: {}", room.getRoomNumber(), room.getAvailableBeds());
                }
            } else {
                // 6. 🔧 新增：如果宿舍组没有房间且已满员，尝试自动分配房间
                if (group.getCurrentMembers() >= group.getMaxMembers()) {
                    log.info("🏠 宿舍组 {} 已满员但没有房间，尝试自动分配房间", group.getGroupName());
                    DormRoom assignedRoom = tryAssignRoomToGroup(group);
                    if (assignedRoom != null) {
                        group.setSelectedRoomId(assignedRoom.getId());
                        group.setTargetRoomType(assignedRoom.getRoomType());
                        log.info("🏠 ✅ 成功为宿舍组 {} 分配房间 {}", group.getGroupName(), assignedRoom.getRoomNumber());
                    } else {
                        log.warn("🏠 ❌ 未能为宿舍组 {} 找到合适的房间", group.getGroupName());
                    }
                }
            }
            
            dormGroupMapper.updateById(group);
            
            log.info("成功添加学生 {} 到宿舍组 {}", student.getRealName(), group.getGroupName());
            return true;
            
        } catch (Exception e) {
            log.error("添加学生到宿舍组失败", e);
            return false;
        }
    }
    
    /**
     * 尝试为宿舍组分配房间
     * @param group 宿舍组
     * @return 分配的房间，如果没有找到合适的房间则返回null
     */
    private DormRoom tryAssignRoomToGroup(DormGroup group) {
        log.info("🏠 tryAssignRoomToGroup: 为宿舍组 {} 寻找房间 (专业={}, 班级={}, 性别={}, 人数={})", 
            group.getGroupName(), group.getMajor(), group.getClassName(), group.getGender(), group.getCurrentMembers());
        
        // 1. 根据宿舍组的专业/班级找到对应的楼栋
        LambdaQueryWrapper<BuildingMajorAssignment> assignmentWrapper = new LambdaQueryWrapper<>();
        assignmentWrapper.eq(BuildingMajorAssignment::getMajorName, group.getMajor())
                        .eq(BuildingMajorAssignment::getClassName, group.getClassName())
                        .eq(BuildingMajorAssignment::getStatus, 1);
        BuildingMajorAssignment assignment = buildingMajorAssignmentMapper.selectOne(assignmentWrapper);
        
        if (assignment == null) {
            log.warn("🏠 未找到专业 {} 班级 {} 的楼栋分配", group.getMajor(), group.getClassName());
            return null;
        }
        
        Long buildingId = assignment.getBuildingId();
        log.info("🏠 找到楼栋分配: buildingId={}", buildingId);
        
        // 2. 查询该楼栋中符合条件的房间
        LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(DormRoom::getBuildingId, buildingId)
                   .ge(DormRoom::getAvailableBeds, group.getCurrentMembers()) // 可用床位 >= 宿舍组人数
                   .and(w -> w.eq(DormRoom::getMajorName, group.getMajor())
                             .or().isNull(DormRoom::getMajorName)
                             .or().eq(DormRoom::getMajorName, ""))
                   .and(w -> w.eq(DormRoom::getClassName, group.getClassName())
                             .or().isNull(DormRoom::getClassName)
                             .or().eq(DormRoom::getClassName, ""))
                   .orderByAsc(DormRoom::getAvailableBeds); // 优先选择刚好够用的房间
        
        List<DormRoom> candidateRooms = dormRoomMapper.selectList(roomWrapper);
        log.info("🏠 找到 {} 个候选房间", candidateRooms.size());
        
        if (candidateRooms.isEmpty()) {
            return null;
        }
        
        // 3. 选择第一个合适的房间
        DormRoom selectedRoom = candidateRooms.get(0);
        log.info("🏠 选择房间: {} (可用床位={})", selectedRoom.getRoomNumber(), selectedRoom.getAvailableBeds());
        
        // 4. 更新房间信息
        selectedRoom.setAvailableBeds(selectedRoom.getAvailableBeds() - group.getCurrentMembers());
        if (selectedRoom.getMajorName() == null || selectedRoom.getMajorName().isEmpty()) {
            selectedRoom.setMajorName(group.getMajor());
        }
        if (selectedRoom.getClassName() == null || selectedRoom.getClassName().isEmpty()) {
            selectedRoom.setClassName(group.getClassName());
        }
        dormRoomMapper.updateById(selectedRoom);
        
        log.info("🏠 更新房间 {} 可用床位: {}", selectedRoom.getRoomNumber(), selectedRoom.getAvailableBeds());
        
        return selectedRoom;
    }
    
    /**
     * 为房间创建新宿舍组
     */
    private DormGroup createNewGroupForRoom(List<Student> students, DormRoom room) {
        if (students.isEmpty()) {
            return null;
        }
        
        try {
            Student leader = students.get(0);
            
            DormGroup group = new DormGroup();
            group.setGroupName(leader.getRealName() + "的团队");  // 统一命名为"的团队"
            group.setLeaderId(leader.getId());
            group.setCurrentMembers(students.size());  // 设置实际成员数
            group.setMaxMembers(room.getCapacity());
            group.setMajor(leader.getMajor());
            group.setClassName(leader.getClassName());
            group.setGrade(leader.getGrade());
            group.setGender(leader.getGender());
            group.setTargetRoomType(room.getRoomType());
            group.setSelectedRoomId(room.getId());
            // 根据人数设置状态
            group.setStatus(students.size() >= room.getCapacity() ? "full" : "forming");
            
            // 构建完整的成员列表JSON（包含 bedNumber）
            List<Map<String, Object>> members = new ArrayList<>();
            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                Map<String, Object> memberMap = new HashMap<>();
                memberMap.put("id", student.getId());
                memberMap.put("name", student.getRealName());
                memberMap.put("studentId", student.getStudentId());
                memberMap.put("major", student.getMajor());
                memberMap.put("className", student.getClassName() != null ? student.getClassName() : "");
                memberMap.put("isLeader", i == 0);  // 第一个是舍长
                memberMap.put("avatar", getStudentAvatar(student));
                // 按数组位置分配床位号：1, 2, 3...
                DormGroupService.assignBedNumberToMember(memberMap, String.valueOf(i + 1));
                members.add(memberMap);
            }
            
            group.setMembers(objectMapper.writeValueAsString(members));
            
            dormGroupMapper.insert(group);
            
            log.info("为房间 {} 创建新宿舍组: {}，成员数: {}/{}", 
                     room.getRoomNumber(), group.getGroupName(), students.size(), room.getCapacity());
            return group;
            
        } catch (Exception e) {
            log.error("创建宿舍组失败", e);
            return null;
        }
    }
    
    /**
     * 根据房间ID查找宿舍组
     */
    private DormGroup findGroupByRoom(Long roomId) {
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getSelectedRoomId, roomId);
        return dormGroupMapper.selectOne(wrapper);
    }
    
    /**
     * 检查学生是否已分配
     * 只有在已满员或已分配房间的宿舍组中的学生才算"已分配"
     * 在未满宿舍组中的学生仍然算"待分配"，可以被重新分配
     */
    private boolean isStudentAssigned(Long studentId) {
        // 检查学生是否在任何宿舍组中（包括 forming 状态）
        // 这样可以防止学生被重复添加到多个宿舍组
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        // 使用更严格的JSON查询（MySQL 5.7+支持 JSON_CONTAINS，但为了兼容性，我们优化LIKE）
        // 匹配 "id":123, 或 "id": 123, 或 "id":123} 或 "id": 123}
        wrapper.and(w -> w
                .like(DormGroup::getMembers, "\"id\":" + studentId + ",")
                .or()
                .like(DormGroup::getMembers, "\"id\": " + studentId + ",")
                .or()
                .like(DormGroup::getMembers, "\"id\":" + studentId + "}")
                .or()
                .like(DormGroup::getMembers, "\"id\": " + studentId + "}")
        );
        
        // 检查所有状态的宿舍组，包括 forming
        // 注意：不检查 disbanded（已解散）状态
        wrapper.notIn(DormGroup::getStatus, "disbanded");
        
        Long count = dormGroupMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
    
    /**
     * 查找匹配的未满宿舍组
     * 匹配条件：性别、专业、班级相同，且有足够的空余床位
     */
    private DormGroup findMatchingIncompleteGroup(Student student, int requiredBeds) {
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getStatus, "forming")
               .eq(DormGroup::getGender, student.getGender())
               .eq(DormGroup::getMajor, student.getMajor())
               .eq(DormGroup::getClassName, student.getClassName());
        
        List<DormGroup> groups = dormGroupMapper.selectList(wrapper);
        
        // 找到有足够空余床位的宿舍组
        for (DormGroup group : groups) {
            int availableBeds = group.getMaxMembers() - group.getCurrentMembers();
            if (availableBeds >= requiredBeds) {
                return group;
            }
        }
        
        return null;
    }

    /**
     * 为剩余未分配学生创建新宿舍组
     */
    private void createNewGroupsForRemainingStudents(List<Student> students, AllocationResultDTO result, IntelligentAssignRequest request) {
        System.out.println("\n========== 🏠 createNewGroupsForRemainingStudents 开始 ==========");
        System.out.println("📋 待分配学生数: " + students.size());
        for (Student s : students) {
            System.out.println("   - " + s.getRealName() + " (专业=" + s.getMajor() + ", 班级=" + s.getClassName() + ", 性别=" + s.getGender() + ")");
        }
        
        // 预先获取所有可用房间，避免循环中多次查询
        List<DormRoom> availableRooms = new ArrayList<>();
        Map<Long, String> buildingGenderMap = new HashMap<>();
        
        if (request.getBuildingIds() != null && !request.getBuildingIds().isEmpty()) {
            System.out.println("\n📍 楼栋IDs: " + request.getBuildingIds());
            
            // 1. 获取楼栋性别信息
            List<Building> buildings = buildingMapper.selectBatchIds(request.getBuildingIds());
            for (Building b : buildings) {
                buildingGenderMap.put(b.getId(), b.getBuildingType());
                System.out.println("   楼栋 " + b.getId() + " (" + b.getBuildingName() + "): 类型=" + b.getBuildingType());
            }
            
            // 2. 获取房间信息（包括空房间和有可用床位的房间）
            LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.in(DormRoom::getBuildingId, request.getBuildingIds())
                       .gt(DormRoom::getAvailableBeds, 0) // 查询有可用床位的房间
                       .orderByDesc(DormRoom::getAvailableBeds); // 优先使用床位多的房间
            availableRooms = dormRoomMapper.selectList(roomWrapper);
            
            System.out.println("\n🏠 查询到可用房间数: " + availableRooms.size());
            System.out.println("📊 可用房间详情 (availableBeds > 0):");
            for (DormRoom r : availableRooms) {
                System.out.println("   房间 " + r.getRoomNumber() + ": 楼栋ID=" + r.getBuildingId() 
                    + ", 专业=" + r.getMajorName() + ", 班级=" + r.getClassName() 
                    + ", 容量=" + r.getBedCount() + ", 可用床位=" + r.getAvailableBeds());
            }
            log.info("找到 {} 个可用房间", availableRooms.size());
        } else {
            System.out.println("⚠️ 没有指定楼栋IDs!");
        }
        
        // 按性别分组
        Map<String, List<Student>> studentsByGender = students.stream()
                .collect(Collectors.groupingBy(Student::getGender));
        
        for (Map.Entry<String, List<Student>> genderEntry : studentsByGender.entrySet()) {
            String gender = genderEntry.getKey(); // "男" 或 "女"
            List<Student> genderStudents = genderEntry.getValue();
            
            // 过滤出该性别的可用房间（通过楼栋类型判断）
            System.out.println("\n🔍 过滤性别=" + gender + " 的房间:");
            List<DormRoom> genderRooms = new ArrayList<>();
            for (DormRoom room : availableRooms) {
                String buildingType = buildingGenderMap.get(room.getBuildingId());
                boolean match = false;
                
                // buildingType现在已经是中文了：男/女
                if ("男".equals(buildingType) && "男".equals(gender)) {
                    match = true;
                } else if ("女".equals(buildingType) && "女".equals(gender)) {
                    match = true;
                }
                
                if (match) {
                    genderRooms.add(room);
                }
            }
            System.out.println("   性别匹配的房间数: " + genderRooms.size());
            
            // 按策略分组
            Map<String, List<Student>> studentsByStrategy;
            if ("same_major".equals(request.getStrategy())) {
                studentsByStrategy = genderStudents.stream()
                        .collect(Collectors.groupingBy(Student::getMajor));
            } else {
                // 默认按班级分组
                studentsByStrategy = genderStudents.stream()
                        .collect(Collectors.groupingBy(Student::getClassName));
            }
            
            for (List<Student> groupStudents : studentsByStrategy.values()) {
                List<Student> remainingStudentsInSubGroup = new ArrayList<>(groupStudents);
                
                while (!remainingStudentsInSubGroup.isEmpty()) {
                    Student sampleStudent = remainingStudentsInSubGroup.get(0);
                    
                    // 1. 寻找最佳房间并确定分组大小
                    DormRoom selectedRoom = null;
                    int targetSize = 4; // 默认大小
                    
                    log.info("🔍 为学生 {} (专业={}, 班级={}, 性别={}) 寻找房间", 
                        sampleStudent.getRealName(), sampleStudent.getMajor(), 
                        sampleStudent.getClassName(), sampleStudent.getGender());
                    log.info("🔍 可用的性别匹配房间数: {}", genderRooms.size());
                    
                    if (!genderRooms.isEmpty()) {
                         // 过滤出符合专业/班级限制的房间
                         List<DormRoom> candidateRooms = genderRooms.stream()
                                .filter(r -> {
                                    // 如果房间指定了专业，必须匹配
                                    if (r.getMajorName() != null && !r.getMajorName().isEmpty() && 
                                        !r.getMajorName().equals(sampleStudent.getMajor())) {
                                        log.info("🔍   房间 {} 专业不匹配: 房间={}, 学生={}", 
                                            r.getRoomNumber(), r.getMajorName(), sampleStudent.getMajor());
                                        return false;
                                    }
                                    // 如果房间指定了班级，必须匹配
                                    if (r.getClassName() != null && !r.getClassName().isEmpty() && 
                                        !r.getClassName().equals(sampleStudent.getClassName())) {
                                        log.info("🔍   房间 {} 班级不匹配: 房间={}, 学生={}", 
                                            r.getRoomNumber(), r.getClassName(), sampleStudent.getClassName());
                                        return false;
                                    }
                                    log.info("🔍   房间 {} 匹配成功, 可用床位={}", r.getRoomNumber(), r.getAvailableBeds());
                                    return true;
                                })
                                .collect(Collectors.toList());
                         
                         log.info("🔍 符合专业/班级限制的候选房间数: {}", candidateRooms.size());
                         
                         final int remainingCount = remainingStudentsInSubGroup.size();
                         
                         // 尝试找正好匹配的
                         selectedRoom = candidateRooms.stream()
                                .filter(r -> r.getAvailableBeds() == remainingCount)
                                .findFirst()
                                .orElse(null);
                         
                         // 如果没找到，找能容纳的最小房间
                         if (selectedRoom == null) {
                             selectedRoom = candidateRooms.stream()
                                    .filter(r -> r.getAvailableBeds() >= remainingCount)
                                    .findFirst()
                                    .orElse(null);
                         }
                         
                         // 如果还没找到，找最大的房间填满它
                         if (selectedRoom == null) {
                             selectedRoom = candidateRooms.stream()
                                    .max(Comparator.comparingInt(DormRoom::getAvailableBeds))
                                    .orElse(null);
                         }
                         
                         // 策略4: 如果还没找到匹配的房间，尝试找一个空房间（无专业/班级限制）
                         if (selectedRoom == null) {
                             log.info("🔍 策略4: 寻找空房间（无专业/班级限制）");
                             selectedRoom = genderRooms.stream()
                                    .filter(r -> r.getAvailableBeds() >= remainingCount)
                                    .filter(r -> (r.getMajorName() == null || r.getMajorName().isEmpty()) 
                                              && (r.getClassName() == null || r.getClassName().isEmpty()))
                                    .findFirst()
                                    .orElse(null);
                             
                             if (selectedRoom != null) {
                                 // 将房间分配给该专业/班级
                                 selectedRoom.setMajorName(sampleStudent.getMajor());
                                 selectedRoom.setClassName(sampleStudent.getClassName());
                                 dormRoomMapper.updateById(selectedRoom);
                                 log.info("🔍 ✅ 找到空房间 {}，设置专业={}, 班级={}", 
                                     selectedRoom.getRoomNumber(), sampleStudent.getMajor(), sampleStudent.getClassName());
                             }
                         }
                         
                         if (selectedRoom != null) {
                             log.info("🔍 ✅ 找到房间: {} (可用床位={})", selectedRoom.getRoomNumber(), selectedRoom.getAvailableBeds());
                         } else {
                             log.info("🔍 ❌ 未找到匹配的房间");
                         }
                    } else {
                        log.info("🔍 ❌ 没有可用的性别匹配房间");
                    }
                    
                    if (selectedRoom != null) {
                        targetSize = selectedRoom.getAvailableBeds();
                    }
                    
                    // 2. 切分学生
                    int actualSize = Math.min(targetSize, remainingStudentsInSubGroup.size());
                    if (actualSize <= 0) actualSize = 4; // 防止死循环
                    
                    List<Student> chunk = new ArrayList<>(remainingStudentsInSubGroup.subList(0, actualSize));
                    remainingStudentsInSubGroup.subList(0, actualSize).clear();
                    
                    // 3. 创建分组
                    createGroupForChunk(chunk, result, availableRooms);
                }
            }
        }
    }

    /**
     * 为一组学生创建新宿舍组或添加到现有未满宿舍组
     */
    private void createGroupForChunk(List<Student> chunk, AllocationResultDTO result, List<DormRoom> availableRooms) {
        if (chunk.isEmpty()) return;

        System.out.println("\n========== 🔧 createGroupForChunk 开始 ==========");
        System.out.println("📋 本次分组学生数: " + chunk.size());
        for (Student s : chunk) {
            System.out.println("   - " + s.getRealName() + " (专业=" + s.getMajor() + ", 班级=" + s.getClassName() + ")");
        }
        System.out.println("📦 传入的可用房间数: " + (availableRooms != null ? availableRooms.size() : "null"));

        try {
            Student leader = chunk.get(0);
            System.out.println("👤 组长: " + leader.getRealName() + " (专业=" + leader.getMajor() + ", 班级=" + leader.getClassName() + ", 性别=" + leader.getGender() + ")");
            
            // 1. 首先尝试找到匹配的未满宿舍组
            DormGroup existingGroup = findMatchingIncompleteGroup(leader, chunk.size());
            System.out.println("🔍 查找现有未满宿舍组: " + (existingGroup != null ? "找到 " + existingGroup.getGroupName() : "未找到"));
            
            if (existingGroup != null) {
                // 找到了匹配的未满宿舍组，将学生添加进去
                log.info("找到匹配的未满宿舍组: {}，将 {} 名学生添加进去", existingGroup.getGroupName(), chunk.size());
                for (Student student : chunk) {
                    boolean success = addStudentToGroup(student, existingGroup);
                    if (success) {
                        result.addSuccess(AllocationResultDTO.StudentAssignmentDetail.builder()
                                .studentId(student.getId())
                                .studentName(student.getRealName())
                                .studentNumber(student.getStudentId())
                                .groupId(existingGroup.getId())
                                .groupName(existingGroup.getGroupName())
                                .roomId(existingGroup.getSelectedRoomId())
                                .build());
                    } else {
                        result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                                .studentId(student.getId())
                                .studentName(student.getRealName())
                                .failureReason("添加到现有宿舍组失败")
                                .build());
                    }
                }
                return;
            }
            
            // 2. 没有找到合适的现有宿舍组，创建新的
            DormGroup group = new DormGroup();
            group.setGroupName(leader.getRealName() + "的自动分组");
            group.setLeaderId(leader.getId());
            group.setCurrentMembers(chunk.size());
            group.setMajor(leader.getMajor());
            group.setClassName(leader.getClassName());
            group.setGrade(leader.getGrade());
            group.setGender(leader.getGender());
            
            DormRoom selectedRoom = null;
            
            // 尝试寻找合适的房间
            System.out.println("\n🏠 开始寻找房间...");
            if (availableRooms != null && !availableRooms.isEmpty()) {
                System.out.println("   可用房间列表 (" + availableRooms.size() + " 个):");
                for (DormRoom r : availableRooms) {
                    System.out.println("      房间 " + r.getRoomNumber() + ": 专业=" + r.getMajorName() 
                        + ", 班级=" + r.getClassName() + ", 容量=" + r.getBedCount() 
                        + ", 当前入住=" + r.getCurrentCount() + ", 可用床位=" + r.getAvailableBeds());
                }
                
                // 策略1: 找空房间且容量正好匹配的，并且专业班级匹配
                System.out.println("\n   📌 策略1: 找空房间且容量=" + chunk.size() + "，专业=" + leader.getMajor() + "，班级=" + leader.getClassName());
                selectedRoom = availableRooms.stream()
                        .filter(r -> {
                            boolean empty = r.getCurrentCount() == 0;
                            boolean capacityMatch = r.getCapacity() == chunk.size();
                            boolean majorMatch = r.getMajorName() == null || r.getMajorName().isEmpty() || r.getMajorName().equals(leader.getMajor());
                            boolean classMatch = r.getClassName() == null || r.getClassName().isEmpty() || r.getClassName().equals(leader.getClassName());
                            System.out.println("      检查房间 " + r.getRoomNumber() + ": 空=" + empty + ", 容量匹配=" + capacityMatch + ", 专业匹配=" + majorMatch + ", 班级匹配=" + classMatch);
                            return empty && capacityMatch && majorMatch && classMatch;
                        })
                        .findFirst()
                        .orElse(null);
                System.out.println("   策略1结果: " + (selectedRoom != null ? "找到房间 " + selectedRoom.getRoomNumber() : "未找到"));
                
                // 策略2: 找可用床位足够的房间，并且专业班级匹配
                if (selectedRoom == null) {
                    System.out.println("\n   📌 策略2: 找可用床位>=" + chunk.size() + "，专业=" + leader.getMajor() + "，班级=" + leader.getClassName());
                    selectedRoom = availableRooms.stream()
                            .filter(r -> {
                                boolean bedsEnough = r.getAvailableBeds() >= chunk.size();
                                boolean majorMatch = r.getMajorName() == null || r.getMajorName().isEmpty() || r.getMajorName().equals(leader.getMajor());
                                boolean classMatch = r.getClassName() == null || r.getClassName().isEmpty() || r.getClassName().equals(leader.getClassName());
                                System.out.println("      检查房间 " + r.getRoomNumber() + ": 床位足够=" + bedsEnough + ", 专业匹配=" + majorMatch + ", 班级匹配=" + classMatch);
                                return bedsEnough && majorMatch && classMatch;
                            })
                            .findFirst()
                            .orElse(null);
                    System.out.println("   策略2结果: " + (selectedRoom != null ? "找到房间 " + selectedRoom.getRoomNumber() : "未找到"));
                }
                
                // 策略3: 如果还没找到，找一个空房间（没有专业/班级限制的），并设置其专业/班级
                if (selectedRoom == null) {
                    System.out.println("\n   📌 策略3: 寻找空房间（无专业/班级限制）");
                    log.info("🔍 策略3: 寻找空房间（无专业/班级限制）");
                    selectedRoom = availableRooms.stream()
                            .filter(r -> {
                                boolean bedsEnough = r.getAvailableBeds() >= chunk.size();
                                boolean noMajor = r.getMajorName() == null || r.getMajorName().isEmpty();
                                boolean noClass = r.getClassName() == null || r.getClassName().isEmpty();
                                System.out.println("      检查房间 " + r.getRoomNumber() + ": 床位足够=" + bedsEnough + ", 无专业限制=" + noMajor + ", 无班级限制=" + noClass);
                                return bedsEnough && noMajor && noClass;
                            })
                            .findFirst()
                            .orElse(null);
                    System.out.println("   策略3结果: " + (selectedRoom != null ? "找到房间 " + selectedRoom.getRoomNumber() : "未找到"));
                    
                    if (selectedRoom != null) {
                        // 将房间分配给该专业/班级
                        selectedRoom.setMajorName(leader.getMajor());
                        selectedRoom.setClassName(leader.getClassName());
                        log.info("🔍 ✅ 找到空房间 {}，设置专业={}, 班级={}", 
                            selectedRoom.getRoomNumber(), leader.getMajor(), leader.getClassName());
                    }
                }
            }
            
            System.out.println("\n🎯 最终房间选择结果: " + (selectedRoom != null ? "房间 " + selectedRoom.getRoomNumber() : "❌ 未找到任何房间!"));
            
            if (selectedRoom != null) {
                System.out.println("✅ 分配房间 " + selectedRoom.getRoomNumber() + " 给宿舍组");
                group.setMaxMembers(selectedRoom.getCapacity());
                group.setSelectedRoomId(selectedRoom.getId());
                group.setTargetRoomType(selectedRoom.getRoomType());
                
                // 更新房间状态
                int oldBeds = selectedRoom.getAvailableBeds();
                selectedRoom.setAvailableBeds(selectedRoom.getAvailableBeds() - chunk.size());
                dormRoomMapper.updateById(selectedRoom);
                System.out.println("   更新房间可用床位: " + oldBeds + " -> " + selectedRoom.getAvailableBeds());
                
                // 设置组状态
                if (group.getCurrentMembers() >= group.getMaxMembers()) {
                    group.setStatus("full"); 
                } else {
                    group.setStatus("forming");
                }
                System.out.println("   宿舍组状态: " + group.getStatus());
                
                // 从可用列表中移除（如果已满）
                if (selectedRoom.getAvailableBeds() <= 0) {
                    availableRooms.remove(selectedRoom);
                    System.out.println("   房间已满，从可用列表移除");
                }
            } else {
                System.out.println("未找到房间! 创建无房间的宿舍组");
                // 没找到房间，根据学生人数设置合理的容量
                // 向上取整到标准容量：2人间、4人间或6人间
                int chunkSize = chunk.size();
                int maxMembers;
                if (chunkSize <= 2) {
                    maxMembers = 2;
                } else if (chunkSize <= 4) {
                    maxMembers = 4;
                } else {
                    maxMembers = 6;
                }
                group.setMaxMembers(maxMembers);
                group.setStatus("forming");
                group.setSelectedRoomId(null);
                System.out.println("   宿舍组容量设为: " + maxMembers + ", 状态: forming, 房间ID: null");
                log.info("未找到房间，根据学生人数 {} 设置宿舍组容量为 {}", chunkSize, maxMembers);
            }
            
            // 构建成员JSON（包含 bedNumber）
            List<Map<String, Object>> membersList = new ArrayList<>();
            int bedNumber = 1;
            for (Student student : chunk) {
                Map<String, Object> member = new HashMap<>();
                member.put("id", student.getId());
                member.put("name", student.getRealName());
                member.put("studentId", student.getStudentId());
                member.put("major", student.getMajor());
                member.put("className", student.getClassName());
                member.put("isLeader", student.getId().equals(leader.getId()));
                member.put("avatar", getStudentAvatar(student));
                // 按顺序分配床位号
                DormGroupService.assignBedNumberToMember(member, String.valueOf(bedNumber++));
                membersList.add(member);
            }
            
            group.setMembers(objectMapper.writeValueAsString(membersList));
            
            dormGroupMapper.insert(group);
            log.info("创建自动分组: {}, 房间: {}", group.getGroupName(), selectedRoom != null ? selectedRoom.getRoomNumber() : "无");
            
            // 记录成功结果
            for (Student student : chunk) {
                result.addSuccess(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(student.getId())
                        .studentName(student.getRealName())
                        .studentNumber(student.getStudentId())
                        .groupId(group.getId())
                        .groupName(group.getGroupName())
                        .roomId(selectedRoom != null ? selectedRoom.getId() : null)
                        .roomNumber(selectedRoom != null ? selectedRoom.getRoomNumber() : null)
                        .build());
            }
            
        } catch (Exception e) {
            log.error("创建自动分组失败", e);
            for (Student student : chunk) {
                result.addFailure(AllocationResultDTO.StudentAssignmentDetail.builder()
                        .studentId(student.getId())
                        .studentName(student.getRealName())
                        .failureReason("创建分组失败: " + e.getMessage())
                        .build());
            }
        }
    }
    
    /**
     * 完成分配，将宿舍信息同步到学生表
     * @param buildingIds 楼栋ID列表（可选，如果为null或空则处理所有楼栋）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> completeAllocation(List<Long> buildingIds) {
        log.info("开始完成分配，同步宿舍信息到学生表，楼栋ID列表: {}", buildingIds);
        
        try {
            // 1. 查询已分配房间的宿舍组
            LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(DormGroup::getSelectedRoomId)
                   .in(DormGroup::getStatus, "forming", "full", "room_selected", "complete");
            
            List<DormGroup> allGroups = dormGroupMapper.selectList(wrapper);
            log.info("找到 {} 个已分配房间的宿舍组", allGroups.size());
            
            // 如果指定了楼栋ID，通过房间的楼栋ID过滤宿舍组
            List<DormGroup> groups;
            if (buildingIds != null && !buildingIds.isEmpty()) {
                log.info("限定楼栋ID: {}", buildingIds);
                groups = new ArrayList<>();
                for (DormGroup group : allGroups) {
                    DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                    if (room != null && buildingIds.contains(room.getBuildingId())) {
                        groups.add(group);
                    }
                }
                log.info("过滤后剩余 {} 个宿舍组", groups.size());
            } else {
                groups = allGroups;
            }
            
            int totalStudents = 0;
            int successCount = 0;
            int failedCount = 0;
            
            // 2. 遍历每个宿舍组
            for (DormGroup group : groups) {
                try {
                    // 获取房间信息
                    DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                    if (room == null) {
                        log.warn("宿舍组 {} 关联的房间不存在，跳过", group.getId());
                        continue;
                    }
                    
                    // 解析成员列表
                    String membersJson = group.getMembers();
                    if (membersJson == null || membersJson.trim().isEmpty() || "[]".equals(membersJson.trim())) {
                        log.warn("宿舍组 {} 没有成员，跳过", group.getId());
                        continue;
                    }
                    
                    List<Map<String, Object>> members = objectMapper.readValue(
                        membersJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    
                    // 3. 更新每个学生的宿舍信息
                    for (Map<String, Object> member : members) {
                        totalStudents++;
                        try {
                            Long studentId = ((Number) member.get("id")).longValue();
                            
                            Student student = studentMapper.selectById(studentId);
                            if (student == null) {
                                log.warn("学生 {} 不存在，跳过", studentId);
                                failedCount++;
                                continue;
                            }
                            
                            // 更新学生的宿舍信息
                            student.setDormGroupId(group.getId());
                            student.setRoomId(room.getId());
                            student.setRoomNumber(room.getRoomNumber());
                            
                            studentMapper.updateById(student);
                            successCount++;
                            
                            log.debug("更新学生 {} 的宿舍信息：宿舍组={}, 房间={}", 
                                     student.getRealName(), group.getGroupName(), room.getRoomNumber());
                            
                        } catch (Exception e) {
                            log.error("更新学生宿舍信息失败：{}", member, e);
                            failedCount++;
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("处理宿舍组 {} 失败", group.getId(), e);
                }
            }
            
            log.info("完成分配完成：总宿舍组={}, 总学生={}, 成功={}, 失败={}", 
                     groups.size(), totalStudents, successCount, failedCount);
            
            // 4. 更新指定楼栋的阶段为 completed
            if (buildingIds != null && !buildingIds.isEmpty()) {
                // 只更新指定的楼栋
                for (Long buildingId : buildingIds) {
                    Building building = buildingMapper.selectById(buildingId);
                    if (building != null) {
                        building.setCurrentPhase("completed");
                        buildingMapper.updateById(building);
                        log.info("更新楼栋 {} 阶段为 completed", building.getBuildingName());
                    }
                }
            } else {
                // 更新所有楼栋
                LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
                buildingWrapper.isNotNull(Building::getCurrentPhase);
                List<Building> buildings = buildingMapper.selectList(buildingWrapper);
                for (Building building : buildings) {
                    building.setCurrentPhase("completed");
                    buildingMapper.updateById(building);
                    log.info("更新楼栋 {} 阶段为 completed", building.getBuildingName());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalGroups", groups.size());
            result.put("totalStudents", totalStudents);
            result.put("successCount", successCount);
            result.put("failedCount", failedCount);
            
            return Result.success("分配完成，已同步 " + successCount + " 个学生的宿舍信息", result);
            
        } catch (Exception e) {
            log.error("完成分配失败", e);
            return Result.error("完成分配失败：" + e.getMessage());
        }
    }
}
