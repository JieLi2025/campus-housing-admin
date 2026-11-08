package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.RoomSelectionDTO;
import com.campus.dorm.dto.TeamInviteDTO;
import com.campus.dorm.dto.TransferApplicationDTO;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.vo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学生分配流程Service
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentAllocationFlowService {
    
    private final DormGroupMapper dormGroupMapper;
    private final RoommateInviteMapper roommateInviteMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final DormRoomMapper dormRoomMapper;
    private final BuildingMapper buildingMapper;
    private final BuildingMajorAssignmentMapper majorAssignmentMapper;
    private final ApplicationMapper applicationMapper;
    private final MaterialTemplateMapper materialTemplateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Resource
    private DormGroupChatService dormGroupChatService;
    
    @Resource
    private WebSocketService webSocketService;
    
    /**
     * 根据userId获取studentId（student表的id）
     */
    private Long getStudentIdByUserId(Long userId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(wrapper);
        return student != null ? student.getId() : null;
    }
    
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
    
    // ==================== 楼栋独立阶段控制 ====================
    
    /**
     * 获取学生所属楼栋ID
     * 通过 student -> building_major_assignment -> building 链路查询
     * 
     * @param userId 用户ID（sys_user表的id）
     * @return 楼栋ID，如果未分配则返回null
     */
    public Long getStudentBuildingId(Long userId) {
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            log.warn("获取学生楼栋ID失败：学生不存在，userId={}", userId);
            return null;
        }
        
        return getStudentBuildingIdByStudentId(studentId);
    }
    
    /**
     * 根据学生ID获取楼栋ID
     * 优先从学生的房间信息获取，如果没有则从专业分配获取
     */
    private Long getStudentBuildingIdByStudentId(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            log.warn("获取学生楼栋ID失败：学生信息不存在，studentId={}", studentId);
            return null;
        }
        
        // 优先从学生的房间信息获取楼栋ID
        if (student.getRoomId() != null) {
            DormRoom room = dormRoomMapper.selectById(student.getRoomId());
            if (room != null && room.getBuildingId() != null) {
                return room.getBuildingId();
            }
        }
        
        // 如果没有房间信息，从专业分配获取
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getMajorName, student.getMajor())
               .eq(BuildingMajorAssignment::getClassName, student.getClassName())
               .eq(BuildingMajorAssignment::getStatus, 1);
        
        // 精确匹配班级分片
        if (student.getClassSegment() != null && !student.getClassSegment().isEmpty()) {
            wrapper.eq(BuildingMajorAssignment::getClassSegment, student.getClassSegment());
        } else {
            wrapper.and(w -> w.isNull(BuildingMajorAssignment::getClassSegment)
                           .or().eq(BuildingMajorAssignment::getClassSegment, ""));
        }
        
        BuildingMajorAssignment assignment = majorAssignmentMapper.selectOne(wrapper);
        if (assignment == null) {
            log.info("学生尚未分配楼栋：studentId={}, major={}, className={}", 
                    studentId, student.getMajor(), student.getClassName());
            return null;
        }
        
        return assignment.getBuildingId();
    }
    
    /**
     * 获取学生所属楼栋的阶段配置
     * 数据源：building 表的阶段配置字段
     * 
     * @param userId 用户ID（sys_user表的id）
     * @return 阶段配置
     */
    public Result<?> getStudentPhase(Long userId) {
        // 1. 获取学生所属楼栋ID
        Long buildingId = getStudentBuildingId(userId);
        
        // 2. 如果学生未分配楼栋，尝试返回第一个有配置的楼栋
        if (buildingId == null) {
            log.info("学生未分配楼栋，尝试返回全局配置：userId={}", userId);
            LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Building::getCurrentPhase)
                   .orderByDesc(Building::getId)
                   .last("LIMIT 1");
            Building building = buildingMapper.selectOne(wrapper);
            
            if (building == null) {
                // 返回默认配置
                Map<String, Object> defaultConfig = new HashMap<>();
                defaultConfig.put("currentPhase", "forming_teams");
                defaultConfig.put("roomSelectionEnd", null);
                return Result.success(defaultConfig);
            }
            buildingId = building.getId();
        }
        
        // 3. 获取该楼栋的阶段配置
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("currentPhase", "forming_teams");
            defaultConfig.put("roomSelectionEnd", null);
            return Result.success(defaultConfig);
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("buildingId", building.getId());
        config.put("buildingName", building.getBuildingName());
        config.put("currentPhase", building.getCurrentPhase());
        config.put("roomSelectionEnd", building.getRoomSelectionEnd());
        
        return Result.success(config);
    }
    
    /**
     * 检查并自动切换学生所属楼栋的阶段
     * 基于截止时间自动切换阶段
     * 
     * @param userId 用户ID（sys_user表的id）
     */
    public void checkAndAutoSwitchStudentPhase(Long userId) {
        Long buildingId = getStudentBuildingId(userId);
        if (buildingId == null) {
            // 如果学生未分配楼栋，获取第一个有配置的楼栋
            LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Building::getCurrentPhase)
                   .orderByDesc(Building::getId)
                   .last("LIMIT 1");
            Building building = buildingMapper.selectOne(wrapper);
            if (building != null) {
                buildingId = building.getId();
            }
        }
        
        if (buildingId != null) {
            checkAndAutoSwitchPhaseForBuilding(buildingId);
        }
    }
    
    /**
     * 检查并自动切换指定楼栋的阶段
     * 
     * @param buildingId 楼栋ID
     */
    private void checkAndAutoSwitchPhaseForBuilding(Long buildingId) {
        Building building = buildingMapper.selectById(buildingId);
        if (building == null || building.getCurrentPhase() == null) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        String currentPhase = building.getCurrentPhase();
        boolean needUpdate = false;
        
        // 检查选房阶段是否已过期（组队阶段由管理员手动切换，不再自动切换）
        if ("room_selection".equals(currentPhase) && building.getRoomSelectionEnd() != null) {
            if (now.isAfter(building.getRoomSelectionEnd())) {
                building.setCurrentPhase("admin_allocation");
                needUpdate = true;
                log.info("楼栋{}选房阶段已过期，自动切换到管理员分配阶段", buildingId);
            }
        }
        
        if (needUpdate) {
            buildingMapper.updateById(building);
        }
    }
    
    /**
     * 检查当前阶段是否允许组队操作
     * 在管理员进入剩余处理阶段（admin_allocation）后禁止发送邀请、接受邀请等组队操作
     * forming_teams 和 room_selection 阶段都允许组队
     * 
     * @param userId 用户ID
     * @return 如果允许返回 Result.success()，否则返回错误信息
     */
    private Result<?> checkPhaseAllowsTeaming(Long userId) {
        Long buildingId = getStudentBuildingId(userId);
        
        // 如果学生未分配楼栋，尝试获取第一个有配置的楼栋
        if (buildingId == null) {
            LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Building::getCurrentPhase)
                   .orderByDesc(Building::getId)
                   .last("LIMIT 1");
            Building building = buildingMapper.selectOne(wrapper);
            if (building != null) {
                buildingId = building.getId();
            }
        }
        
        if (buildingId == null) {
            // 没有配置阶段，默认允许组队
            return Result.success();
        }
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.success();
        }
        
        String phase = building.getCurrentPhase();
        
        // forming_teams 和 room_selection 阶段都允许组队操作
        if (phase == null || "forming_teams".equals(phase) || "room_selection".equals(phase)) {
            return Result.success();
        }
        
        // admin_allocation 和 completed 阶段禁止组队操作
        String phaseText;
        switch (phase) {
            case "admin_allocation":
                phaseText = "管理员剩余处理阶段";
                break;
            case "completed":
                phaseText = "分配已完成";
                break;
            default:
                phaseText = phase;
        }
        
        log.info("组队操作被拒绝：userId={}, 当前阶段={}", userId, phase);
        return Result.error("当前处于" + phaseText + "，不允许组队操作");
    }
    
    // ==================== 原有方法 ====================
    
    /**
     * 获取学生的分配区域信息
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<AssignedAreaVO> getAssignedArea(Long userId) {
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 获取学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 根据专业班级查询分配的楼栋（支持班级分片精确匹配）
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getMajorName, student.getMajor())
               .eq(BuildingMajorAssignment::getClassName, student.getClassName())
               .eq(BuildingMajorAssignment::getStatus, 1);
        
        // 🔑 关键：精确匹配班级分片
        if (student.getClassSegment() != null && !student.getClassSegment().isEmpty()) {
            wrapper.eq(BuildingMajorAssignment::getClassSegment, student.getClassSegment());
        } else {
            wrapper.and(w -> w.isNull(BuildingMajorAssignment::getClassSegment)
                           .or().eq(BuildingMajorAssignment::getClassSegment, ""));
        }
        
        BuildingMajorAssignment assignment = majorAssignmentMapper.selectOne(wrapper);
        if (assignment == null) {
            return Result.error("该专业班级暂未分配宿舍楼栋");
        }
        
        // 3. 获取楼栋信息
        Building building = buildingMapper.selectById(assignment.getBuildingId());
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 4. 查询该班级分配的房间（用于判断楼栋管理员是否已完成区域划分）
        // 🔑 关键：只查询该楼栋下、该专业班级的房间，确保数据隔离
        LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(DormRoom::getBuildingId, building.getId())
                   .eq(DormRoom::getClassName, student.getClassName())
                   .and(w -> w.eq(DormRoom::getMajorName, student.getMajor())
                             .or()
                             .like(DormRoom::getMajorName, student.getMajor() + " - " + student.getClassName()));
        
        List<DormRoom> allRooms = dormRoomMapper.selectList(roomWrapper);
        
        // 5. 判断楼栋管理员是否已完成区域划分
        boolean isAreaAssigned = !allRooms.isEmpty();
        
        // 6. 根据分配状态确定楼层范围和可用房间
        String roomType = null;
        String floorRange = null;
        int availableRoomCount = 0;
        
        if (isAreaAssigned) {
            // 楼栋管理员已划分区域，从房间数据中获取信息
            List<DormRoom> availableRooms = allRooms.stream()
                .filter(r -> r.getAvailableBeds() != null && r.getAvailableBeds() > 0)
                .collect(Collectors.toList());
            availableRoomCount = availableRooms.size();
            
            // 从房间推断楼层范围
            List<Integer> floors = allRooms.stream()
                .map(DormRoom::getFloor)
                .filter(f -> f != null)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
            
            if (!floors.isEmpty()) {
                if (floors.size() == 1) {
                    floorRange = floors.get(0) + "楼";
                } else {
                    floorRange = floors.get(0) + "-" + floors.get(floors.size() - 1) + "楼";
                }
            } else {
                floorRange = "等待分配";
            }
            
            // 从房间获取房间类型
            if (!allRooms.isEmpty() && allRooms.get(0).getRoomType() != null) {
                roomType = convertRoomTypeToDisplay(allRooms.get(0).getRoomType());
            }
        } else {
            // 楼栋管理员尚未划分区域
            floorRange = "等待分配";
            availableRoomCount = 0;
            
            // 尝试从 building_major_assignment 的 floors 字段获取预分配楼层（如果有）
            if (assignment.getFloors() != null && !assignment.getFloors().trim().isEmpty()) {
                try {
                    List<Integer> preAssignedFloors = objectMapper.readValue(
                        assignment.getFloors(), 
                        new TypeReference<List<Integer>>() {}
                    );
                    if (!preAssignedFloors.isEmpty()) {
                        Collections.sort(preAssignedFloors);
                        if (preAssignedFloors.size() == 1) {
                            floorRange = preAssignedFloors.get(0) + "楼";
                        } else {
                            floorRange = preAssignedFloors.get(0) + "-" + preAssignedFloors.get(preAssignedFloors.size() - 1) + "楼";
                        }
                    }
                } catch (Exception e) {
                    // 解析失败，保持"等待分配"
                }
            }
        }
        
        // 如果房间类型为空，尝试从分配记录获取，否则使用默认值
        if (roomType == null) {
            if (assignment.getRoomType() != null && !assignment.getRoomType().isEmpty()) {
                roomType = convertRoomTypeToDisplay(assignment.getRoomType());
            } else {
                roomType = "等待分配";
            }
        }
        
        // 7. 构建返回数据（包含分片信息和分配状态）
        AssignedAreaVO vo = new AssignedAreaVO();
        vo.setBuilding(building.getBuildingName());
        vo.setBuildingId(building.getId());
        vo.setFloors(floorRange);
        vo.setAvailableRooms(availableRoomCount);
        vo.setRoomType(roomType);
        vo.setMajor(student.getMajor());
        vo.setClassName(student.getClassName());
        
        // 返回班级分片信息（用于前端显示）
        vo.setClassSegment(assignment.getClassSegment());
        vo.setStudentIdRange(assignment.getStudentIdRange());
        
        // 新增：返回区域分配状态，前端可据此显示不同提示
        vo.setAreaAssigned(isAreaAssigned);
        
        return Result.success(vo);
    }
    
    /**
     * 获取或创建学生的团队
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<TeamInfoVO> getOrCreateTeam(Long userId) {
        try {
            // 1. 根据userId获取studentId
            Long studentId = getStudentIdByUserId(userId);
            if (studentId == null) {
                return Result.error("学生不存在");
            }
            
            // 2. 查找学生是否已加入团队
            // 先查询是否是组长
            LambdaQueryWrapper<DormGroup> leaderWrapper = new LambdaQueryWrapper<>();
            leaderWrapper.eq(DormGroup::getLeaderId, studentId);
            DormGroup group = dormGroupMapper.selectOne(leaderWrapper);
            
            // 如果不是组长，查询是否是成员（使用LIKE查询JSON字符串，兼容低版本MySQL）
            if (group == null) {
                LambdaQueryWrapper<DormGroup> memberWrapper = new LambdaQueryWrapper<>();
                // 使用LIKE查询JSON字符串中包含该studentId的团队
                // JSON格式: [{"id":101,...}] 或 [{"id":101,...},{"id":102,...}]
                memberWrapper.like(DormGroup::getMembers, "\"id\":" + studentId);
                group = dormGroupMapper.selectOne(memberWrapper);
                
                // 如果LIKE查询找到结果，验证JSON中确实包含该studentId（防止误匹配）
                if (group != null) {
                    try {
                        String membersJson = group.getMembers();
                        if (membersJson != null && !membersJson.trim().isEmpty()) {
                            List<Map<String, Object>> membersData = objectMapper.readValue(
                                membersJson, 
                                new TypeReference<List<Map<String, Object>>>() {}
                            );
                            boolean found = membersData.stream()
                                .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
                            if (!found) {
                                group = null; // 如果JSON中不包含该studentId，设为null
                            }
                        }
                    } catch (Exception e) {
                        log.warn("验证团队成员JSON失败，继续使用LIKE查询结果：groupId={}", group.getId(), e);
                    }
                }
            }
            
            // 3. 检查是否存在已接受邀请但未加入团队的情况（数据修复）
            if (group == null) {
                group = checkAndRepairMembership(studentId);
            }

            // 4. 如果不存在，创建新团队
            if (group == null) {
                Student student = studentMapper.selectById(studentId);
                if (student == null) {
                    return Result.error("学生不存在");
                }
                
                // 验证：检查该学生是否已经存在未完成的宿舍组
                LambdaQueryWrapper<DormGroup> checkWrapper = new LambdaQueryWrapper<>();
                checkWrapper.eq(DormGroup::getLeaderId, studentId)
                           .in(DormGroup::getStatus, "forming", "complete"); // 只检查未选择房间的组
                
                List<DormGroup> existingGroups = dormGroupMapper.selectList(checkWrapper);
                if (existingGroups != null && !existingGroups.isEmpty()) {
                    // 过滤掉已选择房间的组
                    List<DormGroup> activeGroups = existingGroups.stream()
                        .filter(g -> g.getSelectedRoomId() == null)
                        .collect(Collectors.toList());
                    
                    if (!activeGroups.isEmpty()) {
                        String errorMsg = String.format("您已经存在未完成的宿舍组（%s），无法重复创建", activeGroups.get(0).getGroupName());
                        log.warn("学生{}尝试创建重复的宿舍组", studentId);
                        return Result.error(errorMsg);
                    }
                }
                
                // 获取房间类型（添加异常处理和状态检查）
                Result<AssignedAreaVO> areaResult = getAssignedArea(userId);
                if (areaResult.getCode() != 200 || areaResult.getData() == null) {
                    String errorMsg = areaResult.getMessage() != null ? areaResult.getMessage() : "未找到分配区域信息";
                    log.warn("获取分配区域失败：userId={}, studentId={}, error={}", userId, studentId, errorMsg);
                    return Result.error(errorMsg);
                }
                
                AssignedAreaVO areaVO = areaResult.getData();
                
                // 从房间类型中提取人数
                int maxMembers = extractMaxMembers(areaVO.getRoomType());
                
                // 将显示格式的房间类型转换为数据库格式
                String dbRoomType = convertDisplayToRoomType(areaVO.getRoomType());
                log.info("房间类型转换：显示格式={}, 数据库格式={}", areaVO.getRoomType(), dbRoomType);
                
                group = new DormGroup();
                group.setGroupName(student.getRealName() + "的团队");
                group.setLeaderId(studentId);
                group.setCurrentMembers(1);
                group.setMaxMembers(maxMembers);
                group.setMajor(student.getMajor());
                group.setClassName(student.getClassName());
                group.setGrade(student.getGrade());
                group.setGender(student.getGender());
                group.setTargetRoomType(dbRoomType); // 使用数据库格式
                group.setStatus("forming");
                
                // 创建成员列表
                List<Map<String, Object>> members = new ArrayList<>();
                Map<String, Object> memberMap = Map.of(
                    "id", studentId,
                    "name", student.getRealName(),
                    "studentId", student.getStudentId(),
                    "major", student.getMajor(),
                    "className", student.getClassName() != null ? student.getClassName() : "",
                    "isLeader", true,
                    "avatar", getStudentAvatar(student)
                );
                members.add(memberMap);
                
                try {
                    group.setMembers(objectMapper.writeValueAsString(members));
                } catch (Exception e) {
                    log.error("序列化成员列表失败", e);
                    return Result.error("创建团队失败");
                }
                
                dormGroupMapper.insert(group);
                log.info("创建新团队成功：groupId={}", group.getId());
            }
            
            // 3. 构建返回VO
            TeamInfoVO vo = buildTeamInfoVO(group);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取或创建团队失败：userId={}", userId, e);
            return Result.error("获取团队信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 发送室友邀请
     * @param senderId 发送者用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> sendInvite(Long senderId, TeamInviteDTO dto) {
        // 0. 检查当前阶段是否允许组队操作
        Result<?> phaseCheck = checkPhaseAllowsTeaming(senderId);
        if (phaseCheck.getCode() != 200) {
            return phaseCheck;
        }
        
        // 1. 根据userId获取studentId
        Long senderStudentId = getStudentIdByUserId(senderId);
        if (senderStudentId == null) {
            return Result.error("发送者不存在");
        }
        
        // 2. 获取发送者信息
        Student sender = studentMapper.selectById(senderStudentId);
        if (sender == null) {
            return Result.error("发送者不存在");
        }
        
        // 3. 获取接收者信息
        Student receiver;
        if (dto.getReceiverId() != null) {
            receiver = studentMapper.selectById(dto.getReceiverId());
        } else {
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getStudentId, dto.getStudentId());
            receiver = studentMapper.selectOne(wrapper);
        }
        
        if (receiver == null) {
            return Result.error("被邀请的学生不存在");
        }
        
        // 3. 检查是否邀请自己
        if (senderStudentId.equals(receiver.getId())) {
            return Result.error("不能邀请自己");
        }
        
        // 4. 检查发送者的团队状态
        LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(DormGroup::getLeaderId, senderStudentId);
        DormGroup senderGroup = dormGroupMapper.selectOne(groupWrapper);
        
        if (senderGroup != null) {
            // 4.1 检查团队是否已满
            if (senderGroup.getCurrentMembers() >= senderGroup.getMaxMembers()) {
                return Result.error("您的团队已满员，无法继续邀请");
            }
            
            // 4.2 检查接收者是否已经在当前团队中
            try {
                String membersJson = senderGroup.getMembers();
                if (membersJson != null && !membersJson.trim().isEmpty()) {
                    List<Map<String, Object>> members = objectMapper.readValue(
                        membersJson, 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    boolean isMember = members.stream()
                        .anyMatch(m -> receiver.getId().equals(((Number) m.get("id")).longValue()));
                    if (isMember) {
                        return Result.error("该学生已在您的团队中");
                    }
                }
            } catch (Exception e) {
                // 解析失败，继续检查
            }
        }
        
        // 5. 检查是否已发送过邀请
        LambdaQueryWrapper<RoommateInvite> inviteWrapper = new LambdaQueryWrapper<>();
        inviteWrapper.eq(RoommateInvite::getSenderId, senderStudentId)
                    .eq(RoommateInvite::getReceiverId, receiver.getId())
                    .eq(RoommateInvite::getStatus, "PENDING");
        
        Long existingCount = roommateInviteMapper.selectCount(inviteWrapper);
        if (existingCount > 0) {
            return Result.error("已向该学生发送过邀请，请勿重复发送");
        }
        
        // 6. 创建邀请记录
        RoommateInvite invite = new RoommateInvite();
        invite.setSenderId(senderStudentId);
        invite.setSenderName(sender.getRealName());
        invite.setSenderNumber(sender.getStudentId());
        invite.setReceiverId(receiver.getId());
        invite.setReceiverName(receiver.getRealName());
        invite.setReceiverNumber(receiver.getStudentId());
        invite.setStatus("PENDING");
        invite.setCreatedAt(LocalDateTime.now());
        
        roommateInviteMapper.insert(invite);
        
        return Result.success("邀请已发送", invite.getId());
    }
    
    /**
     * 撤回邀请
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelInvite(Long senderId, Long inviteId) {
        // 根据userId获取studentId
        Long senderStudentId = getStudentIdByUserId(senderId);
        if (senderStudentId == null) {
            return Result.error("发送者不存在");
        }
        
        RoommateInvite invite = roommateInviteMapper.selectById(inviteId);
        if (invite == null) {
            return Result.error("邀请不存在");
        }
        
        if (!invite.getSenderId().equals(senderStudentId)) {
            return Result.error("无权操作此邀请");
        }
        
        // 如果是待处理状态，则撤回
        if ("PENDING".equals(invite.getStatus())) {
            invite.setStatus("CANCELLED");
            roommateInviteMapper.updateById(invite);
            return Result.success("邀请已撤回");
        }
        
        // 如果是已撤回、已拒绝或已接受状态，则删除记录
        if ("CANCELLED".equals(invite.getStatus()) || "REJECTED".equals(invite.getStatus()) || "ACCEPTED".equals(invite.getStatus())) {
            roommateInviteMapper.deleteById(inviteId);
            return Result.success("邀请记录已删除");
        }
        
        return Result.error("无法操作此邀请");
    }
    
    /**
     * 获取收到的邀请列表
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<List<RoommateInvite>> getReceivedInvites(Long userId) {
        // 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        LambdaQueryWrapper<RoommateInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoommateInvite::getReceiverId, studentId)
               .eq(RoommateInvite::getStatus, "PENDING")
               .orderByDesc(RoommateInvite::getCreatedAt);
        
        List<RoommateInvite> invites = roommateInviteMapper.selectList(wrapper);
        return Result.success(invites);
    }
    
    /**
     * 获取已发送的邀请列表
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<List<RoommateInvite>> getSentInvites(Long userId) {
        // 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        LambdaQueryWrapper<RoommateInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoommateInvite::getSenderId, studentId)
               .orderByDesc(RoommateInvite::getCreatedAt);
        
        List<RoommateInvite> invites = roommateInviteMapper.selectList(wrapper);
        
        // 填充接收者的专业和班级信息
        for (RoommateInvite invite : invites) {
            if (invite.getReceiverId() != null) {
                Student receiver = studentMapper.selectById(invite.getReceiverId());
                if (receiver != null) {
                    invite.setReceiverMajor(receiver.getMajor());
                    invite.setReceiverClass(receiver.getClassName());
                }
            }
        }
        
        return Result.success(invites);
    }
    
    /**
     * 接受邀请
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> acceptInvite(Long userId, Long inviteId) {
        // 0. 检查当前阶段是否允许组队操作
        Result<?> phaseCheck = checkPhaseAllowsTeaming(userId);
        if (phaseCheck.getCode() != 200) {
            return phaseCheck;
        }
        
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 获取邀请信息
        RoommateInvite invite = roommateInviteMapper.selectById(inviteId);
        if (invite == null) {
            return Result.error("邀请不存在");
        }
        
        if (!invite.getReceiverId().equals(studentId)) {
            return Result.error("无权处理此邀请");
        }
        
        if (!"PENDING".equals(invite.getStatus())) {
            return Result.error("该邀请已被处理");
        }
        
        // 2. 检查学生是否已在其他团队（使用LIKE查询，兼容低版本MySQL）
        LambdaQueryWrapper<DormGroup> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.like(DormGroup::getMembers, "\"id\":" + studentId);
        DormGroup existingGroup = dormGroupMapper.selectOne(checkWrapper);
        
        // 验证JSON中确实包含该studentId（防止误匹配）
        if (existingGroup != null) {
            try {
                String membersJson = existingGroup.getMembers();
                if (membersJson != null && !membersJson.trim().isEmpty()) {
                    List<Map<String, Object>> membersData = objectMapper.readValue(
                        membersJson, 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    boolean found = membersData.stream()
                        .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
                    if (!found) {
                        existingGroup = null; // 如果JSON中不包含该studentId，设为null
                    }
                }
            } catch (Exception e) {
                log.warn("验证团队成员JSON失败：groupId={}", existingGroup != null ? existingGroup.getId() : null, e);
                existingGroup = null; // 解析失败，设为null
            }
        }
        
        // 3. 检查学生是否已在其他团队
        if (existingGroup != null) {
            // 如果学生已经在邀请人的团队中（作为成员），直接返回成功
            if (existingGroup.getLeaderId().equals(invite.getSenderId())) {
                // 已经在邀请人的团队中，直接返回成功
                invite.setStatus("ACCEPTED");
                roommateInviteMapper.updateById(invite);
                return Result.success("您已在该团队中");
            }
            
            // 如果学生是自己团队的leader且只有1个人，允许接受邀请（后面会删除旧团队）
            if (existingGroup.getLeaderId().equals(studentId) && existingGroup.getCurrentMembers() == 1) {
                // 允许接受邀请，后面会删除旧团队
                log.info("学生{}是只有1人的团队leader，允许接受邀请并删除旧团队", studentId);
            } else if (!existingGroup.getLeaderId().equals(studentId)) {
                // 学生是其他团队的普通成员，允许接受邀请（后面会从旧团队移除）
                log.info("学生{}是其他团队的普通成员，允许接受邀请并从旧团队移除", studentId);
            } else {
                // 学生是自己团队的leader但团队有多人，需要先转让舍长
                return Result.error("您是当前团队的舍长，请先转让舍长后再接受邀请");
            }
        }
        
        // 4. 获取邀请人的团队
        LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(DormGroup::getLeaderId, invite.getSenderId());
        DormGroup group = dormGroupMapper.selectOne(groupWrapper);
        
        if (group == null) {
            return Result.error("邀请人的团队不存在");
        }
        
        // 5. 检查团队是否已满
        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            return Result.error("该团队已满员");
        }
        
        // 6. 将学生添加到团队
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 检查是否已在成员列表中
            boolean exists = members.stream()
                .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
            
            if (!exists) {
                Map<String, Object> newMember = Map.of(
                    "id", studentId,
                    "name", student.getRealName(),
                    "studentId", student.getStudentId(),
                    "major", student.getMajor(),
                    "className", student.getClassName() != null ? student.getClassName() : "",
                    "isLeader", false,
                    "avatar", getStudentAvatar(student)
                );
                members.add(newMember);
                
                group.setMembers(objectMapper.writeValueAsString(members));
                group.setCurrentMembers(members.size());
                
                // 如果满员，更新状态
                if (group.getCurrentMembers() >= group.getMaxMembers()) {
                    group.setStatus("full");
                }
                
                dormGroupMapper.updateById(group);
            }
        } catch (Exception e) {
            log.error("更新团队成员失败", e);
            return Result.error("加入团队失败");
        }
        
        // 7. 更新邀请状态
        invite.setStatus("ACCEPTED");
        roommateInviteMapper.updateById(invite);
        
        // 8. 处理旧团队：如果学生之前有自己的团队（只有自己），删除旧团队
        if (existingGroup != null && existingGroup.getLeaderId().equals(studentId) 
            && existingGroup.getCurrentMembers() == 1) {
            // 退出旧团队的群聊
            try {
                dormGroupChatService.onStudentLeaveDormGroup(existingGroup.getId(), studentId);
            } catch (Exception e) {
                log.error("退出旧团队群聊失败", e);
            }
            dormGroupMapper.deleteById(existingGroup.getId());
            log.info("已删除学生{}的旧团队（只有自己）", studentId);
        } 
        // 9. 如果学生是其他团队的普通成员，从旧团队中移除
        else if (existingGroup != null && !existingGroup.getLeaderId().equals(studentId)) {
            try {
                // 从旧团队的成员列表中移除该学生
                String oldMembersJson = existingGroup.getMembers();
                if (oldMembersJson != null && !oldMembersJson.trim().isEmpty()) {
                    List<Map<String, Object>> oldMembers = objectMapper.readValue(
                        oldMembersJson, 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    
                    // 移除该学生
                    oldMembers.removeIf(m -> studentId.equals(((Number) m.get("id")).longValue()));
                    
                    existingGroup.setMembers(objectMapper.writeValueAsString(oldMembers));
                    existingGroup.setCurrentMembers(oldMembers.size());
                    
                    // 如果不满员，更新状态
                    if (existingGroup.getCurrentMembers() < existingGroup.getMaxMembers()) {
                        existingGroup.setStatus("forming");
                    }
                    
                    dormGroupMapper.updateById(existingGroup);
                    
                    // 退出旧团队的群聊
                    try {
                        dormGroupChatService.onStudentLeaveDormGroup(existingGroup.getId(), studentId);
                    } catch (Exception e) {
                        log.error("退出旧团队群聊失败", e);
                    }
                    
                    log.info("已从旧团队{}中移除学生{}", existingGroup.getId(), studentId);
                }
            } catch (Exception e) {
                log.error("从旧团队移除成员失败", e);
                // 不阻止流程继续，因为已经加入新团队了
            }
        }
        
        // 10. 加入新团队的群聊
        try {
            dormGroupChatService.onStudentJoinDormGroup(group.getId(), studentId);
        } catch (Exception e) {
            log.error("加入团队群聊失败", e);
        }
        
        return Result.success("已加入团队");
    }
    
    /**
     * 拒绝邀请
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rejectInvite(Long userId, Long inviteId) {
        log.info("拒绝邀请：userId={}, inviteId={}", userId, inviteId);
        
        // 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            log.warn("用户{}对应的学生记录不存在", userId);
            return Result.error("学生不存在");
        }
        
        RoommateInvite invite = roommateInviteMapper.selectById(inviteId);
        if (invite == null) {
            return Result.error("邀请不存在");
        }
        
        if (!invite.getReceiverId().equals(studentId)) {
            return Result.error("无权处理此邀请");
        }
        
        if (!"PENDING".equals(invite.getStatus())) {
            return Result.error("该邀请已被处理");
        }
        
        invite.setStatus("REJECTED");
        roommateInviteMapper.updateById(invite);
        
        return Result.success("已拒绝邀请");
    }
    
    /**
     * 搜索同班同学
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<List<StudentVO>> searchClassmates(Long userId, String keyword) {
        log.info("搜索同班同学：userId={}, keyword={}", userId, keyword);
        
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            log.warn("用户{}对应的学生记录不存在", userId);
            return Result.error("学生不存在");
        }
        
        // 2. 获取当前学生信息
        Student currentStudent = studentMapper.selectById(studentId);
        if (currentStudent == null) {
            return Result.error("学生不存在");
        }
        
        // 验证性别信息（必须有性别才能搜索同学，确保宿舍安全）
        String currentGender = currentStudent.getGender();
        if (currentGender == null || currentGender.trim().isEmpty()) {
            log.error("学生{}的性别信息为空，无法搜索同学", studentId);
            return Result.error("您的性别信息不完整，无法搜索同学。请联系管理员完善信息。");
        }
        
        // 查询同专业、同班级、同性别的学生（必须同性别，确保宿舍安全）
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getMajor, currentStudent.getMajor())
               .eq(Student::getClassName, currentStudent.getClassName())
               .eq(Student::getGender, currentGender)  // 必须同性别
               .ne(Student::getId, studentId);
        
        // 关键字搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Student::getRealName, keyword)
                            .or().like(Student::getStudentId, keyword));
        }
        
        List<Student> students = studentMapper.selectList(wrapper);
        
        // 转换为VO
        List<StudentVO> vos = students.stream().map(s -> {
            StudentVO vo = new StudentVO();
            vo.setId(s.getId());
            vo.setRealName(s.getRealName());
            vo.setStudentId(s.getStudentId());
            vo.setMajor(s.getMajor());
            vo.setClassName(s.getClassName());
            vo.setGender(s.getGender());
            vo.setAvatar(getStudentAvatar(s));
            vo.setBio(s.getBio());
            vo.setTags(s.getTags());
            return vo;
        }).collect(Collectors.toList());
        
        return Result.success(vos);
    }
    
    /**
     * 移除团队成员（仅舍长可操作）
     * @param leaderId 舍长用户ID（sys_user表的id）
     * @param memberId 成员学生ID（student表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> removeMember(Long leaderId, Long memberId) {
        log.info("移除团队成员：leaderId={}, memberId={}", leaderId, memberId);
        
        // 根据userId获取studentId
        Long leaderStudentId = getStudentIdByUserId(leaderId);
        if (leaderStudentId == null) {
            log.warn("用户{}对应的学生记录不存在", leaderId);
            return Result.error("舍长不存在");
        }
        
        // 获取舍长的团队
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, leaderStudentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        if (group == null) {
            return Result.error("团队不存在");
        }
        
        // 不能移除自己
        if (leaderStudentId.equals(memberId)) {
            return Result.error("不能移除自己");
        }
        
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 移除成员
            members.removeIf(m -> memberId.equals(((Number) m.get("id")).longValue()));
            
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            
            // 如果不满员，更新状态
            if (group.getCurrentMembers() < group.getMaxMembers()) {
                group.setStatus("forming");
            }
            
            dormGroupMapper.updateById(group);
            
            // 退出群聊
            try {
                dormGroupChatService.onStudentLeaveDormGroup(group.getId(), memberId);
            } catch (Exception e) {
                log.error("退出群聊失败", e);
            }
            
            // 为被移除的学生直接创建新的单人宿舍组
            // 注意：不能调用getOrCreateTeam，因为它会查询到刚更新的宿舍组
            try {
                Student removedStudent = studentMapper.selectById(memberId);
                if (removedStudent != null) {
                    // 检查是否已有宿舍组（作为舍长）
                    LambdaQueryWrapper<DormGroup> checkWrapper = new LambdaQueryWrapper<>();
                    checkWrapper.eq(DormGroup::getLeaderId, memberId);
                    DormGroup existingGroup = dormGroupMapper.selectOne(checkWrapper);
                    
                    if (existingGroup == null) {
                        // 获取被移除学生的分配区域信息
                        Result<AssignedAreaVO> areaResult = getAssignedArea(removedStudent.getUserId());
                        String roomType = "二人间"; // 默认值
                        int maxMembers = 2; // 默认值
                        
                        if (areaResult.getCode() == 200 && areaResult.getData() != null) {
                            roomType = areaResult.getData().getRoomType();
                            maxMembers = extractMaxMembers(roomType);
                        } else {
                            log.warn("获取被移除学生的分配区域失败，使用默认值二人间: studentId={}", memberId);
                        }
                        
                        // 创建新的单人宿舍组
                        DormGroup newGroup = new DormGroup();
                        newGroup.setGroupName(removedStudent.getRealName() + "的团队");
                        newGroup.setLeaderId(memberId);
                        newGroup.setCurrentMembers(1);
                        newGroup.setMaxMembers(maxMembers);
                        newGroup.setMajor(removedStudent.getMajor());
                        newGroup.setClassName(removedStudent.getClassName());
                        newGroup.setGrade(removedStudent.getGrade());
                        newGroup.setGender(removedStudent.getGender());
                        newGroup.setTargetRoomType(convertDisplayToRoomType(roomType));
                        newGroup.setStatus("forming");
                        
                        // 创建成员列表
                        List<Map<String, Object>> newMembers = new ArrayList<>();
                        Map<String, Object> memberMap = new HashMap<>();
                        memberMap.put("id", memberId);
                        memberMap.put("name", removedStudent.getRealName());
                        memberMap.put("studentId", removedStudent.getStudentId());
                        memberMap.put("major", removedStudent.getMajor());
                        memberMap.put("className", removedStudent.getClassName() != null ? removedStudent.getClassName() : "");
                        memberMap.put("isLeader", true);
                        memberMap.put("avatar", getStudentAvatar(removedStudent));
                        newMembers.add(memberMap);
                        
                        newGroup.setMembers(objectMapper.writeValueAsString(newMembers));
                        dormGroupMapper.insert(newGroup);
                        log.info("已为被移除的学生创建新的单人宿舍组: studentId={}, newGroupId={}", memberId, newGroup.getId());
                    } else {
                        log.info("被移除的学生已有宿舍组: studentId={}, groupId={}", memberId, existingGroup.getId());
                    }
                    
                    // 通过WebSocket通知被移除的成员
                    if (removedStudent.getUserId() != null) {
                        webSocketService.sendTeamUpdate(
                            removedStudent.getUserId(), 
                            "REMOVED", 
                            "您已被舍长移出队伍，系统已为您创建新的队伍"
                        );
                        log.info("已通知被移除的学生: studentId={}, userId={}", memberId, removedStudent.getUserId());
                    }
                }
            } catch (Exception e) {
                log.error("为被移除的学生创建新宿舍组失败", e);
            }
            
            return Result.success("成员已移除");
        } catch (Exception e) {
            log.error("移除成员失败", e);
            return Result.error("移除成员失败");
        }
    }
    
    /**
     * 转让舍长
     * @param currentLeaderId 当前舍长用户ID（sys_user表的id）
     * @param newLeaderId 新舍长学生ID（student表的id，从前端members中获取）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> transferLeadership(Long currentLeaderId, Long newLeaderId) {
        log.info("转让舍长：currentLeaderId={}, newLeaderId={}", currentLeaderId, newLeaderId);
        
        // 根据userId获取studentId
        Long currentLeaderStudentId = getStudentIdByUserId(currentLeaderId);
        if (currentLeaderStudentId == null) {
            log.warn("用户{}对应的学生记录不存在", currentLeaderId);
            return Result.error("当前舍长不存在");
        }
        
        // newLeaderId 已经是 student.id，不需要转换
        Long newLeaderStudentId = newLeaderId;
        
        // 获取团队
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, currentLeaderStudentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        if (group == null) {
            return Result.error("团队不存在");
        }
        
        // 检查新舍长是否在团队中
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            boolean newLeaderExists = members.stream()
                .anyMatch(m -> newLeaderStudentId.equals(((Number) m.get("id")).longValue()));
            
            if (!newLeaderExists) {
                return Result.error("新舍长不在团队中");
            }
            
            // 更新成员的isLeader标记
            for (Map<String, Object> member : members) {
                Long memberId = ((Number) member.get("id")).longValue();
                member.put("isLeader", memberId.equals(newLeaderStudentId));
            }
            
            // 获取新舍长姓名
            Student newLeader = studentMapper.selectById(newLeaderStudentId);
            if (newLeader == null) {
                return Result.error("新舍长信息不存在");
            }
            
            group.setLeaderId(newLeaderStudentId);
            group.setMembers(objectMapper.writeValueAsString(members));
            
            dormGroupMapper.updateById(group);
            
            return Result.success("舍长已转让");
        } catch (Exception e) {
            log.error("转让舍长失败", e);
            return Result.error("转让舍长失败");
        }
    }
    
    /**
     * 转让舍长并离开团队（用于舍长调宿申请）
     * 流程：1.转让舍长给新成员 2.将自己从宿舍组移除 3.释放自己的床位
     * @param currentLeaderId 当前舍长用户ID（sys_user表的id）
     * @param newLeaderId 新舍长学生ID（student表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> transferLeaderAndLeave(Long currentLeaderId, Long newLeaderId) {
        log.info("转让舍长并离开团队：currentLeaderId={}, newLeaderId={}", currentLeaderId, newLeaderId);
        
        // 1. 根据userId获取studentId
        Long currentLeaderStudentId = getStudentIdByUserId(currentLeaderId);
        if (currentLeaderStudentId == null) {
            log.warn("用户{}对应的学生记录不存在", currentLeaderId);
            return Result.error("当前舍长不存在");
        }
        
        // 2. 获取团队
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, currentLeaderStudentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        if (group == null) {
            return Result.error("团队不存在");
        }
        
        // 3. 检查新舍长是否在团队中，并执行转让+移除
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            boolean newLeaderExists = members.stream()
                .anyMatch(m -> newLeaderId.equals(((Number) m.get("id")).longValue()));
            
            if (!newLeaderExists) {
                return Result.error("新舍长不在团队中");
            }
            
            // 移除当前舍长，并更新新舍长的isLeader标记
            members.removeIf(m -> currentLeaderStudentId.equals(((Number) m.get("id")).longValue()));
            for (Map<String, Object> member : members) {
                Long memberId = ((Number) member.get("id")).longValue();
                member.put("isLeader", memberId.equals(newLeaderId));
            }
            
            // 获取新舍长姓名
            Student newLeader = studentMapper.selectById(newLeaderId);
            if (newLeader == null) {
                return Result.error("新舍长信息不存在");
            }
            
            // 更新团队信息
            group.setLeaderId(newLeaderId);
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            
            // 如果不满员，更新状态为forming
            if (group.getCurrentMembers() < group.getMaxMembers()) {
                group.setStatus("forming");
            }
            
            dormGroupMapper.updateById(group);
            
            // 4. 释放当前舍长的床位（床位信息已存储在dorm_group.members JSON中）
            // 增加房间可用床位数
            if (group.getSelectedRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                if (room != null) {
                    room.setAvailableBeds(room.getAvailableBeds() + 1);
                    dormRoomMapper.updateById(room);
                    log.info("已释放床位：roomId={}", room.getId());
                }
            }
            
            // 5. 退出群聊
            try {
                dormGroupChatService.onStudentLeaveDormGroup(group.getId(), currentLeaderStudentId);
            } catch (Exception e) {
                log.error("退出群聊失败", e);
            }
            
            log.info("舍长转让并离开成功：原舍长studentId={}, 新舍长studentId={}", currentLeaderStudentId, newLeaderId);
            return Result.success("已转让舍长并离开团队");
        } catch (Exception e) {
            log.error("转让舍长并离开团队失败", e);
            return Result.error("操作失败：" + e.getMessage());
        }
    }
    
    /**
     * 舍长解散宿舍组并离开（用于舍长是唯一成员时的调宿申请）
     * 流程：1.解散宿舍组 2.释放房间 3.释放床位
     * @param userId 舍长用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> disbandGroupAndLeave(Long userId) {
        log.info("解散宿舍组并离开：userId={}", userId);
        
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            log.warn("用户{}对应的学生记录不存在", userId);
            return Result.error("学生不存在");
        }
        
        // 2. 获取团队（必须是舍长）
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, studentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        if (group == null) {
            return Result.error("您不是任何团队的舍长");
        }
        
        // 3. 检查是否只有一个成员
        if (group.getCurrentMembers() > 1) {
            return Result.error("团队还有其他成员，请先转让舍长或移除成员");
        }
        
        try {
            // 4. 释放床位（床位信息已存储在dorm_group.members JSON中）
            // 增加房间可用床位数
            if (group.getSelectedRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                if (room != null) {
                    room.setAvailableBeds(room.getAvailableBeds() + 1);
                    dormRoomMapper.updateById(room);
                    log.info("已释放床位：roomId={}", room.getId());
                }
            }
            
            // 5. 释放房间（清除宿舍组的房间选择）
            if (group.getSelectedRoomId() != null) {
                group.setSelectedRoomId(null);
            }
            
            // 6. 退出群聊
            try {
                dormGroupChatService.onStudentLeaveDormGroup(group.getId(), studentId);
            } catch (Exception e) {
                log.error("退出群聊失败", e);
            }
            
            // 7. 删除宿舍组
            dormGroupMapper.deleteById(group.getId());
            
            log.info("宿舍组解散成功：groupId={}, studentId={}", group.getId(), studentId);
            return Result.success("已解散宿舍组并离开");
        } catch (Exception e) {
            log.error("解散宿舍组失败", e);
            return Result.error("操作失败：" + e.getMessage());
        }
    }
    
    /**
     * 退出团队
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> leaveTeam(Long userId) {
        log.info("退出团队：userId={}", userId);
        
        // 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            log.warn("用户{}对应的学生记录不存在", userId);
            return Result.error("学生不存在");
        }
        
        // 查找学生所在的团队（使用LIKE查询，兼容低版本MySQL）
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(DormGroup::getMembers, "\"id\":" + studentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        // 验证JSON中确实包含该studentId（防止误匹配）
        if (group != null) {
            try {
                String membersJson = group.getMembers();
                if (membersJson != null && !membersJson.trim().isEmpty()) {
                    List<Map<String, Object>> membersData = objectMapper.readValue(
                        membersJson, 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    boolean found = membersData.stream()
                        .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
                    if (!found) {
                        group = null; // 如果JSON中不包含该studentId，设为null
                    }
                }
            } catch (Exception e) {
                log.warn("验证团队成员JSON失败：groupId={}", group != null ? group.getId() : null, e);
                group = null; // 解析失败，设为null
            }
        }
        
        if (group == null) {
            return Result.error("您未加入任何团队");
        }
        
        // 舍长不能直接退出
        if (group.getLeaderId().equals(studentId)) {
            return Result.error("舍长不能直接退出，请先转让舍长或解散团队");
        }
        
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 移除成员
            members.removeIf(m -> studentId.equals(((Number) m.get("id")).longValue()));
            
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            
            // 如果不满员，更新状态
            if (group.getCurrentMembers() < group.getMaxMembers()) {
                group.setStatus("forming");
            }
            
            dormGroupMapper.updateById(group);
            
            // 退出群聊
            try {
                dormGroupChatService.onStudentLeaveDormGroup(group.getId(), studentId);
            } catch (Exception e) {
                log.error("退出群聊失败", e);
            }
            
            // 创建新的单人团队
            getOrCreateTeam(studentId);
            
            return Result.success("已退出团队");
        } catch (Exception e) {
            log.error("退出团队失败", e);
            return Result.error("退出团队失败");
        }
    }
    
    /**
     * 获取可选房间列表
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<List<DormRoomVO>> getAvailableRooms(Long userId) {
        // 1. 获取学生的分配区域
        Result<AssignedAreaVO> areaResult = getAssignedArea(userId);
        if (areaResult.getData() == null) {
            return Result.error("未找到分配区域信息");
        }
        
        AssignedAreaVO area = areaResult.getData();
        
        // 2. 获取学生信息（用于专业班级过滤）
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        
        // 3. 查询可用房间（只根据专业班级过滤，不限制楼层和房间范围）
        LambdaQueryWrapper<DormRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormRoom::getBuildingId, area.getBuildingId())
               .eq(DormRoom::getClassName, student.getClassName())
               .and(w -> w.eq(DormRoom::getMajorName, student.getMajor())
                         .or()
                         .like(DormRoom::getMajorName, student.getMajor() + " - " + student.getClassName()))
               .gt(DormRoom::getAvailableBeds, 0);
        
        // 根据房间类型格式进行匹配
        String roomTypePattern = extractRoomTypePattern(area.getRoomType());
        if (roomTypePattern != null) {
            wrapper.and(w -> {
                w.like(DormRoom::getRoomType, roomTypePattern);
                if ("4".equals(roomTypePattern)) {
                    w.or().eq(DormRoom::getRoomType, "four_bed").or().eq(DormRoom::getRoomType, "four_person");
                } else if ("6".equals(roomTypePattern)) {
                    w.or().eq(DormRoom::getRoomType, "six_bed").or().eq(DormRoom::getRoomType, "six_person");
                } else if ("2".equals(roomTypePattern)) {
                    w.or().eq(DormRoom::getRoomType, "two_bed").or().eq(DormRoom::getRoomType, "double_bed");
                } else if ("8".equals(roomTypePattern)) {
                    w.or().eq(DormRoom::getRoomType, "eight_bed");
                }
            });
        }
        
        wrapper.orderByAsc(DormRoom::getFloor, DormRoom::getRoomNumber);
        
        List<DormRoom> rooms = dormRoomMapper.selectList(wrapper);
        
        // 3. 转换为VO，并从material_template动态获取物资配置
        List<DormRoomVO> vos = rooms.stream().map(room -> {
            Building building = buildingMapper.selectById(room.getBuildingId());
            
            DormRoomVO vo = new DormRoomVO();
            vo.setId(room.getId());
            vo.setBuildingId(room.getBuildingId());
            vo.setBuildingName(building != null ? building.getBuildingName() : "");
            vo.setRoomNumber(room.getRoomNumber());
            vo.setFloor(room.getFloor());
            vo.setRoomType(room.getRoomType());
            vo.setBedCount(room.getBedCount());
            vo.setAvailableBeds(room.getAvailableBeds());
            vo.setOccupiedBeds(room.getBedCount() - room.getAvailableBeds());
            
            // 从material_template动态获取物资配置（按buildingId+roomType查询）
            String materials = getMaterialsForRoom(room.getBuildingId(), room.getRoomType());
            vo.setMaterials(materials);
            vo.setFacilities(materials);
            
            return vo;
        }).collect(Collectors.toList());
        
        return Result.success(vos);
    }
    
    /**
     * 根据楼栋ID和房型获取物资配置
     */
    private String getMaterialsForRoom(Long buildingId, String roomType) {
        if (buildingId == null) {
            log.debug("获取物资配置：buildingId为空");
            return null;
        }
        
        try {
            // 标准化房型名称
            String normalizedRoomType = normalizeRoomTypeForQuery(roomType);
            log.debug("获取物资配置：buildingId={}, 原始roomType={}, 标准化roomType={}", 
                     buildingId, roomType, normalizedRoomType);
            
            LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MaterialTemplate::getBuildingId, buildingId);
            if (normalizedRoomType != null) {
                wrapper.eq(MaterialTemplate::getRoomType, normalizedRoomType);
            }
            wrapper.last("LIMIT 1");
            
            MaterialTemplate template = materialTemplateMapper.selectOne(wrapper);
            if (template != null) {
                log.debug("找到物资配置：templateId={}, materials={}", template.getId(), template.getMaterials());
                return template.getMaterials();
            } else {
                log.debug("未找到物资配置：buildingId={}, roomType={}", buildingId, normalizedRoomType);
            }
        } catch (Exception e) {
            log.warn("获取物资配置失败：buildingId={}, roomType={}", buildingId, roomType, e);
        }
        
        return null;
    }
    
    /**
     * 标准化房型名称用于查询
     * 统一使用中文数字格式：二人间、四人间、六人间、八人间
     */
    private String normalizeRoomTypeForQuery(String roomType) {
        if (roomType == null) return null;
        
        // 统一转换为标准中文数字格式
        if (roomType.contains("2") || roomType.contains("双") || roomType.contains("二") ||
            roomType.equalsIgnoreCase("two_bed") || roomType.equalsIgnoreCase("double_bed")) {
            return "二人间";
        } else if (roomType.contains("4") || roomType.contains("四") ||
                   roomType.equalsIgnoreCase("four_bed") || roomType.equalsIgnoreCase("four_person")) {
            return "四人间";
        } else if (roomType.contains("6") || roomType.contains("六") ||
                   roomType.equalsIgnoreCase("six_bed") || roomType.equalsIgnoreCase("six_person")) {
            return "六人间";
        } else if (roomType.contains("8") || roomType.contains("八") ||
                   roomType.equalsIgnoreCase("eight_bed")) {
            return "八人间";
        }
        
        return roomType;
    }
    
    /**
     * 确认房间选择（舍长操作）
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> confirmRoomSelection(Long userId, RoomSelectionDTO dto) {
        
        // 1. 根据userId获取studentId
        Long leaderStudentId = getStudentIdByUserId(userId);
        if (leaderStudentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 获取舍长的团队
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, leaderStudentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        if (group == null) {
            return Result.error("团队不存在");
        }
        
        // 允许的状态：full（满员待选房）、complete（完成组队）、room_selected（已选房，重新选房场景）
        if (!"full".equals(group.getStatus()) && !"complete".equals(group.getStatus()) && !"room_selected".equals(group.getStatus())) {
            return Result.error("团队未满员，无法选择房间");
        }
        
        // 3. 如果是重新选房，先释放原来的房间和分配记录
        if ("room_selected".equals(group.getStatus()) && group.getSelectedRoomId() != null) {
            // 释放原房间床位
            DormRoom oldRoom = dormRoomMapper.selectById(group.getSelectedRoomId());
            if (oldRoom != null) {
                oldRoom.setAvailableBeds(oldRoom.getAvailableBeds() + group.getCurrentMembers());
                dormRoomMapper.updateById(oldRoom);
            }
            
            // 床位信息已存储在dorm_group.members JSON中，无需额外释放
            log.info("重新选房，原床位信息将被覆盖：roomId={}", group.getSelectedRoomId());
        }
        
        // 4. 检查房间是否可用
        DormRoom room = dormRoomMapper.selectById(dto.getRoomId());
        if (room == null) {
            return Result.error("房间不存在");
        }
        
        if (room.getAvailableBeds() < group.getCurrentMembers()) {
            return Result.error("房间床位不足");
        }
        
        // 3. 更新团队信息
        group.setSelectedRoomId(dto.getRoomId());
        group.setStatus("room_selected");
        dormGroupMapper.updateById(group);
        
        // 4. 更新房间可用床位
        room.setAvailableBeds(room.getAvailableBeds() - group.getCurrentMembers());
        dormRoomMapper.updateById(room);
        
        // 5. 为每个成员分配床位号（存储在dorm_group.members JSON中）
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 为每个成员分配床位号
            for (int i = 0; i < members.size(); i++) {
                Map<String, Object> member = members.get(i);
                member.put("bedNumber", String.valueOf(i + 1));
            }
            
            // 更新dorm_group.members JSON
            group.setMembers(objectMapper.writeValueAsString(members));
            dormGroupMapper.updateById(group);
            
            log.info("床位分配成功：roomId={}, 成员数={}", dto.getRoomId(), members.size());
        } catch (Exception e) {
            log.error("分配床位失败", e);
            return Result.error("选房失败：" + e.getMessage());
        }
        
        return Result.success("选房成功");
    }
    
    /**
     * 取消房间选择（舍长重新选房）
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelRoomSelection(Long userId) {
        // 1. 根据userId获取studentId
        Long leaderStudentId = getStudentIdByUserId(userId);
        if (leaderStudentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 获取舍长的团队
        LambdaQueryWrapper<DormGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormGroup::getLeaderId, leaderStudentId);
        DormGroup group = dormGroupMapper.selectOne(wrapper);
        
        if (group == null) {
            return Result.error("团队不存在");
        }
        
        // 3. 检查团队是否已选房
        if (!"room_selected".equals(group.getStatus()) || group.getSelectedRoomId() == null) {
            return Result.error("团队尚未选择房间");
        }
        
        Long oldRoomId = group.getSelectedRoomId();
        
        // 4. 释放原房间床位
        DormRoom oldRoom = dormRoomMapper.selectById(oldRoomId);
        if (oldRoom != null) {
            oldRoom.setAvailableBeds(oldRoom.getAvailableBeds() + group.getCurrentMembers());
            dormRoomMapper.updateById(oldRoom);
            log.info("释放房间床位：roomId={}, 释放数量={}", oldRoomId, group.getCurrentMembers());
        }
        
        // 5. 删除所有成员的分配记录
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            // 床位信息已存储在dorm_group.members JSON中，无需额外删除分配记录
            log.info("取消选房，床位信息将在重新选房时更新：roomId={}, 成员数={}", oldRoomId, members.size());
        } catch (Exception e) {
            log.error("删除分配记录失败", e);
        }
        
        // 6. 更新团队状态（回到满员待选房状态）
        group.setSelectedRoomId(null);
        group.setStatus("full");
        dormGroupMapper.updateById(group);
        
        log.info("取消房间选择成功：groupId={}, oldRoomId={}", group.getId(), oldRoomId);
        return Result.success("已取消房间选择，请重新选择房间");
    }
    
    /**
     * 获取分配结果
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<AllocationResultVO> getAllocationResult(Long userId) {
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 查询学生所在的宿舍组（从dorm_group表查询）
        // 使用LIKE查询members字段，兼容旧版MySQL
        // 注意：members中存储的是student表的id，不是studentId（学号）
        LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.and(wrapper -> wrapper
                        .like(DormGroup::getMembers, "\"id\":" + studentId)
                        .or()
                        .like(DormGroup::getMembers, "\"studentId\":\"" + studentId + "\""))
                    .or()
                    .eq(DormGroup::getLeaderId, studentId);
        
        List<DormGroup> groups = dormGroupMapper.selectList(groupWrapper);
        
        log.info("查询学生{}的宿舍组，找到{}个", studentId, groups.size());
        if (!groups.isEmpty()) {
            for (DormGroup g : groups) {
                log.info("  宿舍组ID: {}, 名称: {}, 房间ID: {}, members: {}", 
                    g.getId(), g.getGroupName(), g.getSelectedRoomId(), 
                    g.getMembers() != null ? g.getMembers().substring(0, Math.min(100, g.getMembers().length())) : "null");
            }
        }
        
        // 过滤出已选择房间的宿舍组
        DormGroup group = null;
        for (DormGroup g : groups) {
            if (g.getSelectedRoomId() != null) {
                group = g;
                break;
            }
        }
        
        if (group == null || group.getSelectedRoomId() == null) {
            // 没有分配房间，需要重新开始分配流程
            AllocationResultVO vo = new AllocationResultVO();
            vo.setSuccess(false);
            vo.setMessage("暂未分配宿舍，请重新组建团队");
            return Result.success(vo);
        }
        
        // 3. 获取房间和楼栋信息
        DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
        if (room == null) {
            return Result.error("房间信息不存在");
        }
        
        Building building = buildingMapper.selectById(room.getBuildingId());
        if (building == null) {
            return Result.error("楼栋信息不存在");
        }
        
        // 4. 从宿舍组的members字段获取室友信息和床位号
        List<AllocationResultVO.RoommateVO> roommates = new ArrayList<>();
        Integer bedNumber = null;
        try {
            if (group.getMembers() != null && !group.getMembers().trim().isEmpty()) {
                List<Map<String, Object>> members = objectMapper.readValue(
                    group.getMembers(), 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // 根据成员在列表中的位置分配床位号（从1开始）
                int position = 1;
                for (Map<String, Object> member : members) {
                    Object memberIdObj = member.get("id");
                    if (memberIdObj != null) {
                        Long memberId = Long.valueOf(memberIdObj.toString());
                        
                        if (memberId.equals(studentId)) {
                            // 找到当前学生，记录床位号
                            bedNumber = position;
                        } else {
                            // 其他成员是室友，获取完整信息（包括头像）
                            Student roommate = studentMapper.selectById(memberId);
                            if (roommate != null) {
                                AllocationResultVO.RoommateVO roommateVO = new AllocationResultVO.RoommateVO();
                                roommateVO.setName(roommate.getRealName());
                                roommateVO.setStudentId(roommate.getStudentId());
                                // 从sys_user表获取头像
                                if (roommate.getUserId() != null) {
                                    SysUser user = sysUserMapper.selectById(roommate.getUserId());
                                    if (user != null) {
                                        roommateVO.setAvatar(user.getAvatar());
                                    }
                                }
                                roommates.add(roommateVO);
                            }
                        }
                        position++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析宿舍组成员JSON失败", e);
        }
        
        // 5. 检查是否为舍长
        boolean isLeader = studentId.equals(group.getLeaderId());
        
        // 6. 构建返回VO
        AllocationResultVO vo = new AllocationResultVO();
        vo.setSuccess(true);
        vo.setMessage("恭喜！您已成功分配到宿舍");
        vo.setBuilding(building.getBuildingName());
        vo.setRoomNumber(room.getRoomNumber());
        vo.setFloor(room.getFloor());
        vo.setBedNumber(bedNumber != null ? bedNumber.toString() : null); // 根据成员顺序分配床位号
        vo.setRoommates(roommates);
        vo.setIsLeader(isLeader);
        
        // 7. 根据系统阶段和截止时间判断分配阶段
        String systemPhase = getCurrentSystemPhase();
        LocalDateTime deadline = getSystemAdjustmentDeadline();
        
        // 判断分配阶段的逻辑：
        // 1. 如果系统阶段是 admin_allocation、result_viewing 或 completed，则为已确认
        // 2. 如果截止时间已过，则为已确认
        // 3. 否则为可调整
        if ("admin_allocation".equals(systemPhase) || 
            "result_viewing".equals(systemPhase) || 
            "completed".equals(systemPhase)) {
            vo.setAllocationPhase("confirmed");
        } else if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
            vo.setAllocationPhase("confirmed");
        } else {
            vo.setAllocationPhase("adjustable");
        }
        
        // 设置调整截止时间
        if (deadline != null) {
            vo.setAdjustmentDeadline(deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            // 如果没有设置系统截止时间，使用宿舍组创建时间+7天作为默认值
            if (group.getCreatedAt() != null) {
                LocalDateTime defaultDeadline = group.getCreatedAt().plusDays(7);
                vo.setAdjustmentDeadline(defaultDeadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            // 如果创建时间也为空，不设置截止时间，前端会显示"未设置"
        }
        
        // 7. 设置设施信息（从material_template动态获取）
        String materialsJson = getMaterialsForRoom(room.getBuildingId(), room.getRoomType());
        if (materialsJson != null && !materialsJson.trim().isEmpty()) {
            // 使用物资配置
            vo.setMaterials(materialsJson);
            AllocationResultVO.FacilitiesVO facilities = parseFacilitiesFromMaterials(materialsJson);
            vo.setFacilities(facilities);
        } else {
            // 没有配置物资模板，设置空设施
            AllocationResultVO.FacilitiesVO facilities = new AllocationResultVO.FacilitiesVO();
            vo.setFacilities(facilities);
        }
        
        return Result.success(vo);
    }
    
    /**
     * 提交调宿申请
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> submitTransferApplication(Long userId, TransferApplicationDTO dto) {
        log.info("提交调宿申请：userId={}, dto={}", userId, dto);
        log.info("目标房间：{}, 目标室友：{}", dto.getTargetRoom(), dto.getTargetRoommateId());
        
        // 1. 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 获取学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 2. 从dorm_group表获取当前宿舍信息
        LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.and(w -> w
                        .like(DormGroup::getMembers, "\"id\":" + studentId)
                        .or()
                        .eq(DormGroup::getLeaderId, studentId))
                    .isNotNull(DormGroup::getSelectedRoomId);
        DormGroup currentGroup = dormGroupMapper.selectOne(groupWrapper);
        
        // 3. 创建申请记录
        Application application = new Application();
        application.setStudentId(studentId);
        application.setBuildingId(getStudentBuildingIdByStudentId(studentId));  // 设置楼栋ID用于权限过滤
        application.setType("调宿申请");
        
        // 映射原因
        String reasonText = mapReason(dto.getReason());
        application.setReason(reasonText + ": " + dto.getDescription());
        application.setStatus("待审核");
        application.setContactPhone(dto.getContactPhone());
        
        // 目标宿舍信息（换房间）
        if (dto.getTargetRoom() != null && !dto.getTargetRoom().trim().isEmpty()) {
            application.setTargetDormInfo(dto.getTargetRoom());
        }
        
        // 目标室友信息（换室友）
        if (dto.getTargetRoommateId() != null && !dto.getTargetRoommateId().trim().isEmpty()) {
            application.setTargetDormInfo("目标室友学号: " + dto.getTargetRoommateId());
        }
        
        applicationMapper.insert(application);
        
        log.info("调宿申请已保存：id={}, targetDormInfo={}", application.getId(), application.getTargetDormInfo());
        
        // 发送通知给楼栋管理员
        try {
            notifyBuildingAdminForApplication(application, student, userId);
        } catch (Exception e) {
            log.error("发送申请通知失败：applicationId={}", application.getId(), e);
            // 不影响主业务流程
        }
        
        return Result.success("申请已提交，请等待审核", application.getId());
    }
    
    @Resource
    private NotificationService notificationService;
    
    /**
     * 发送申请通知给楼栋管理员
     */
    private void notifyBuildingAdminForApplication(Application application, Student student, Long userId) {
        try {
            log.info("开始发送申请通知：applicationId={}, studentId={}", application.getId(), student.getId());
            
            // 获取学生所在楼栋ID
            Long buildingId = null;
            
            // 从student.roomId获取楼栋ID
            if (student.getRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(student.getRoomId());
                if (room != null) {
                    buildingId = room.getBuildingId();
                }
            }
            
            // 如果还是没有，从building_major_assignment获取
            if (buildingId == null) {
                buildingId = getStudentBuildingId(userId);
            }
            
            log.info("获取学生楼栋ID：studentId={}, buildingId={}", student.getId(), buildingId);
            
            if (buildingId == null) {
                log.warn("无法发送申请通知：学生未分配宿舍，studentId={}", student.getId());
                return;
            }
            
            // 创建通知
            com.campus.dorm.dto.NotificationDTO notificationDTO = new com.campus.dorm.dto.NotificationDTO();
            notificationDTO.setTitle("新的调宿申请待审核");
            notificationDTO.setMessage("学生" + student.getRealName() + "提交了调宿申请，请及时处理");
            notificationDTO.setType("application");
            notificationDTO.setRelatedId(application.getId());
            notificationDTO.setTargetBuildingId(buildingId);
            
            notificationService.createBuildingNotification(notificationDTO, buildingId, userId);
            log.info("申请通知已发送：applicationId={}, buildingId={}", application.getId(), buildingId);
        } catch (Exception e) {
            log.error("发送申请通知失败：applicationId={}", application.getId(), e);
        }
    }
    
    /**
     * 获取申请历史
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<List<Application>> getApplicationHistory(Long userId) {
        // 根据userId获取studentId
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        log.info("获取申请历史：studentId={}", studentId);
        
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getStudentId, studentId)
               .eq(Application::getDeleted, 0)  // 过滤已删除的申请
               .orderByDesc(Application::getCreatedAt);
        
        List<Application> applications = applicationMapper.selectList(wrapper);
        return Result.success(applications);
    }
    
    /**
     * 检查并修复数据一致性：
     * 如果存在状态为ACCEPTED的收到的邀请，但学生不在发送者的团队中，则自动加入
     */
    private DormGroup checkAndRepairMembership(Long studentId) {
        try {
            // 1. 查询最近一条已接受的邀请
            LambdaQueryWrapper<RoommateInvite> inviteWrapper = new LambdaQueryWrapper<>();
            inviteWrapper.eq(RoommateInvite::getReceiverId, studentId)
                         .eq(RoommateInvite::getStatus, "ACCEPTED")
                         .orderByDesc(RoommateInvite::getCreatedAt);
            List<RoommateInvite> acceptedInvites = roommateInviteMapper.selectList(inviteWrapper);
            
            if (acceptedInvites.isEmpty()) {
                return null;
            }
            
            // 遍历检查（防止最近的一条对应的团队已解散）
            for (RoommateInvite invite : acceptedInvites) {
                // 2. 获取邀请人的团队
                LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
                groupWrapper.eq(DormGroup::getLeaderId, invite.getSenderId());
                DormGroup senderGroup = dormGroupMapper.selectOne(groupWrapper);
                
                if (senderGroup != null) {
                    // 3. 检查学生是否在团队中
                    boolean isMember = false;
                    List<Map<String, Object>> members = new ArrayList<>();
                    
                    String membersJson = senderGroup.getMembers();
                    if (membersJson != null && !membersJson.trim().isEmpty()) {
                        try {
                            members = objectMapper.readValue(
                                membersJson, 
                                new TypeReference<List<Map<String, Object>>>() {}
                            );
                            isMember = members.stream()
                                .anyMatch(m -> studentId.equals(((Number) m.get("id")).longValue()));
                        } catch (Exception e) {
                            log.warn("解析团队成员失败", e);
                            continue;
                        }
                    }
                    
                    // 4. 如果不在团队中，执行修复
                    if (!isMember) {
                        log.info("检测到数据不一致：学生{}接受了邀请{}但不在团队{}中，执行自动修复", 
                                studentId, invite.getId(), senderGroup.getId());
                                
                        Student student = studentMapper.selectById(studentId);
                        if (student != null) {
                            Map<String, Object> newMember = Map.of(
                                "id", studentId,
                                "name", student.getRealName(),
                                "studentId", student.getStudentId(),
                                "major", student.getMajor(),
                                "className", student.getClassName() != null ? student.getClassName() : "",
                                "isLeader", false,
                                "avatar", getStudentAvatar(student)
                            );
                            members.add(newMember);
                            
                            senderGroup.setMembers(objectMapper.writeValueAsString(members));
                            senderGroup.setCurrentMembers(members.size());
                            
                            // 如果满员，更新状态
                            if (senderGroup.getCurrentMembers() >= senderGroup.getMaxMembers()) {
                                senderGroup.setStatus("full");
                            }
                            
                            dormGroupMapper.updateById(senderGroup);
                            return senderGroup;
                        }
                    } else {
                        // 如果已经在团队中，直接返回该团队
                        return senderGroup;
                    }
                }
            }
        } catch (Exception e) {
            log.error("执行数据修复失败", e);
        }
        return null;
    }

    // ===== 辅助方法 =====
    
    /**
     * 获取系统配置的调整截止时间
     * 数据源：building 表的 room_selection_end 字段
     */
    private LocalDateTime getSystemAdjustmentDeadline() {
        try {
            LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Building::getRoomSelectionEnd)
                   .orderByDesc(Building::getId)
                   .last("LIMIT 1");
            Building building = buildingMapper.selectOne(wrapper);
            if (building != null && building.getRoomSelectionEnd() != null) {
                return building.getRoomSelectionEnd();
            }
        } catch (Exception e) {
            log.warn("获取系统调整截止时间失败", e);
        }
        return null;
    }
    
    /**
     * 获取当前系统阶段
     * 数据源：building 表的 current_phase 字段
     */
    private String getCurrentSystemPhase() {
        try {
            LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(Building::getCurrentPhase)
                   .orderByDesc(Building::getId)
                   .last("LIMIT 1");
            Building building = buildingMapper.selectOne(wrapper);
            if (building != null && building.getCurrentPhase() != null) {
                return building.getCurrentPhase();
            }
        } catch (Exception e) {
            log.warn("获取系统阶段失败", e);
        }
        return "forming_teams"; // 默认返回组队阶段
    }
    
    /**
     * 从房间类型中提取最大人数
     */
    private int extractMaxMembers(String roomType) {
        if (roomType == null) return 4;
        
        if (roomType.contains("2")) return 2;
        if (roomType.contains("4")) return 4;
        if (roomType.contains("6")) return 6;
        if (roomType.contains("8")) return 8;
        
        return 4; // 默认4人间
    }
    
    /**
     * 构建TeamInfoVO
     */
    private TeamInfoVO buildTeamInfoVO(DormGroup group) {
        TeamInfoVO vo = new TeamInfoVO();
        vo.setId(group.getId());
        vo.setGroupName(group.getGroupName());
        vo.setLeaderId(group.getLeaderId());
        vo.setMemberCount(group.getCurrentMembers());
        vo.setMaxMembers(group.getMaxMembers());
        vo.setMajor(group.getMajor());
        vo.setClassName(group.getClassName());
        vo.setGender(group.getGender());
        vo.setRoomType(group.getTargetRoomType());
        vo.setStatus(group.getStatus());
        vo.setRoomId(group.getSelectedRoomId());
        vo.setBuildingId(null); // 字段已移除，设为null
        vo.setCreatedAt(group.getCreatedAt());
        
        // 获取房间和楼栋名称
        if (group.getSelectedRoomId() != null) {
            DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
            if (room != null) {
                vo.setRoomNumber(room.getRoomNumber());
                Building building = buildingMapper.selectById(room.getBuildingId());
                if (building != null) {
                    vo.setBuildingName(building.getBuildingName());
                }
            }
        }
        
        // 解析成员列表
        String[] leaderNameRef = new String[1]; // 使用数组来存储，因为数组引用是final的
        try {
            String membersJson = group.getMembers();
            if (membersJson == null || membersJson.trim().isEmpty()) {
                // 如果 members 字段为空（不在数据库中），创建默认成员列表（只包含组长）
                if (group.getLeaderId() != null) {
                    try {
                        Student leader = studentMapper.selectById(group.getLeaderId());
                        if (leader != null) {
                            TeamInfoVO.TeamMemberVO leaderVO = new TeamInfoVO.TeamMemberVO();
                            leaderVO.setId(leader.getId());
                            leaderVO.setName(leader.getRealName());
                            leaderVO.setStudentId(leader.getStudentId());
                            leaderVO.setMajor(leader.getMajor());
                            leaderVO.setClassName(leader.getClassName());
                            leaderVO.setIsLeader(true);
                            leaderVO.setAvatar(getStudentAvatar(leader));
                            leaderNameRef[0] = leader.getRealName();
                            vo.setMembers(Collections.singletonList(leaderVO));
                        } else {
                            vo.setMembers(new ArrayList<>());
                        }
                    } catch (Exception e) {
                        log.warn("查询组长信息失败：leaderId={}", group.getLeaderId(), e);
                        vo.setMembers(new ArrayList<>());
                    }
                } else {
                    vo.setMembers(new ArrayList<>());
                }
            } else {
                List<Map<String, Object>> membersData = objectMapper.readValue(
                    membersJson, 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                
                List<TeamInfoVO.TeamMemberVO> members = membersData.stream().map(m -> {
                    TeamInfoVO.TeamMemberVO memberVO = new TeamInfoVO.TeamMemberVO();
                    memberVO.setId(((Number) m.get("id")).longValue());
                    memberVO.setName((String) m.get("name"));
                    memberVO.setStudentId((String) m.get("studentId"));
                    memberVO.setMajor((String) m.get("major"));
                    memberVO.setClassName((String) m.get("className"));
                    memberVO.setIsLeader((Boolean) m.get("isLeader"));
                    memberVO.setAvatar((String) m.get("avatar"));
                    
                    // 如果是组长，记录组长姓名
                    if (Boolean.TRUE.equals(memberVO.getIsLeader()) || 
                        group.getLeaderId() != null && group.getLeaderId().equals(memberVO.getId())) {
                        leaderNameRef[0] = memberVO.getName();
                    }
                    
                    return memberVO;
                }).collect(Collectors.toList());
                
                vo.setMembers(members);
            }
        } catch (Exception e) {
            log.error("解析成员列表失败：groupId={}", group.getId(), e);
            // 如果解析失败，尝试创建默认成员列表
            if (group.getLeaderId() != null) {
                try {
                    Student leader = studentMapper.selectById(group.getLeaderId());
                    if (leader != null) {
                        TeamInfoVO.TeamMemberVO leaderVO = new TeamInfoVO.TeamMemberVO();
                        leaderVO.setId(leader.getId());
                        leaderVO.setName(leader.getRealName());
                        leaderVO.setStudentId(leader.getStudentId());
                        leaderVO.setMajor(leader.getMajor());
                        leaderVO.setClassName(leader.getClassName());
                        leaderVO.setIsLeader(true);
                        leaderVO.setAvatar(getStudentAvatar(leader));
                        leaderNameRef[0] = leader.getRealName();
                        vo.setMembers(Collections.singletonList(leaderVO));
                    } else {
                        vo.setMembers(new ArrayList<>());
                    }
                } catch (Exception ex) {
                    log.warn("查询组长信息失败：leaderId={}", group.getLeaderId(), ex);
                    vo.setMembers(new ArrayList<>());
                }
            } else {
                vo.setMembers(new ArrayList<>());
            }
        }
        
        // 设置组长姓名：优先使用非持久化字段，如果为空则从成员列表中获取
        if (group.getLeaderName() != null && !group.getLeaderName().isEmpty()) {
            vo.setLeaderName(group.getLeaderName());
        } else if (leaderNameRef[0] != null && !leaderNameRef[0].isEmpty()) {
            vo.setLeaderName(leaderNameRef[0]);
        } else if (group.getLeaderId() != null) {
            // 如果成员列表解析失败，尝试从数据库查询组长信息
            try {
                Student leader = studentMapper.selectById(group.getLeaderId());
                if (leader != null) {
                    vo.setLeaderName(leader.getRealName());
                }
            } catch (Exception e) {
                log.warn("查询组长信息失败：leaderId={}", group.getLeaderId(), e);
            }
        }
        
        return vo;
    }
    
    /**
     * 从物资配置JSON解析设施信息
     */
    private AllocationResultVO.FacilitiesVO parseFacilitiesFromMaterials(String materialsJson) {
        AllocationResultVO.FacilitiesVO vo = new AllocationResultVO.FacilitiesVO();
        vo.setYangTai(false);
        vo.setKongTiao(false);
        vo.setDuLiWeiYu(false);
        vo.setWifi(false);
        vo.setNuanQi(false);
        vo.setReShuiQi(false);
        
        if (materialsJson == null || materialsJson.trim().isEmpty()) {
            return vo;
        }
        
        try {
            // 物资配置是数组格式 [{"name":"空调","quantity":1,"unit":"个"},...]
            List<Map<String, Object>> materials = objectMapper.readValue(
                materialsJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            for (Map<String, Object> item : materials) {
                String name = (String) item.get("name");
                if (name == null) continue;
                
                if (name.contains("阳台")) vo.setYangTai(true);
                if (name.contains("空调")) vo.setKongTiao(true);
                if (name.contains("独立卫浴") || name.contains("卫浴")) vo.setDuLiWeiYu(true);
                if (name.contains("WiFi") || name.contains("wifi") || name.contains("网络")) vo.setWifi(true);
                if (name.contains("暖气")) vo.setNuanQi(true);
                if (name.contains("热水器")) vo.setReShuiQi(true);
            }
        } catch (Exception e) {
            log.warn("解析物资配置失败，尝试作为对象格式解析", e);
            // 如果不是数组格式，尝试作为对象格式解析
            return parseFacilities(materialsJson);
        }
        
        return vo;
    }
    
    /**
     * 解析设施信息
     * 统一使用中文 key，同时兼容旧的英文 key 格式
     */
    private AllocationResultVO.FacilitiesVO parseFacilities(String facilitiesJson) {
        AllocationResultVO.FacilitiesVO vo = new AllocationResultVO.FacilitiesVO();
        vo.setYangTai(false);
        vo.setKongTiao(false);
        vo.setDuLiWeiYu(false);
        vo.setWifi(false);
        vo.setNuanQi(false);
        vo.setReShuiQi(false);
        
        if (facilitiesJson == null || facilitiesJson.trim().isEmpty()) {
            return vo;
        }
        
        try {
            Map<String, Object> facilities = objectMapper.readValue(
                facilitiesJson, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // 统一使用中文 key，同时兼容旧的英文 key 格式
            vo.setYangTai(hasFacilityValue(facilities, "阳台", "hasBalcony", "has阳台"));
            vo.setKongTiao(hasFacilityValue(facilities, "空调", "hasAirConditioner", "has空调"));
            vo.setDuLiWeiYu(hasFacilityValue(facilities, "独立卫浴", "hasPrivateBathroom", "has独立卫浴"));
            vo.setWifi(hasFacilityValue(facilities, "WiFi", "wifi", "hasWifi", "hasWiFi", "has WiFi"));
            vo.setNuanQi(hasFacilityValue(facilities, "暖气", "hasHeating", "has暖气"));
            vo.setReShuiQi(hasFacilityValue(facilities, "热水器", "hasWaterHeater", "has热水器"));
        } catch (Exception e) {
            log.error("解析设施信息失败", e);
        }
        
        return vo;
    }
    
    /**
     * 检查设施是否存在（支持多种 key 格式）
     */
    private boolean hasFacilityValue(Map<String, Object> facilities, String... keys) {
        for (String key : keys) {
            if (Boolean.TRUE.equals(facilities.get(key))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 映射申请原因
     */
    private String mapReason(String reason) {
        switch (reason) {
            case "health": return "身体健康原因";
            case "study": return "学习环境需要";
            case "roommate_conflict": return "室友关系问题";
            case "family": return "家庭特殊情况";
            case "other": return "其他原因";
            default: return "其他原因";
        }
    }
    
    /**
     * 转换房间类型为显示格式
     * 统一使用中文数字格式：二人间、四人间、六人间、八人间
     */
    private String convertRoomTypeToDisplay(String roomType) {
        if (roomType == null) return "四人间";
        
        // 统一转换为标准中文数字格式
        if (roomType.contains("2") || roomType.contains("双") || roomType.contains("二") ||
            roomType.equalsIgnoreCase("two_bed") || roomType.equalsIgnoreCase("double_bed")) {
            return "二人间";
        } else if (roomType.contains("4") || roomType.contains("四") ||
                   roomType.equalsIgnoreCase("four_bed") || roomType.equalsIgnoreCase("four_person")) {
            return "四人间";
        } else if (roomType.contains("6") || roomType.contains("六") ||
                   roomType.equalsIgnoreCase("six_bed") || roomType.equalsIgnoreCase("six_person")) {
            return "六人间";
        } else if (roomType.contains("8") || roomType.contains("八") ||
                   roomType.equalsIgnoreCase("eight_bed")) {
            return "八人间";
        }
        
        return "四人间"; // 默认
    }
    
    /**
     * 将显示格式的房间类型转换为统一的中文数字格式
     * 统一使用：二人间、四人间、六人间、八人间
     */
    private String convertDisplayToRoomType(String displayRoomType) {
        if (displayRoomType == null) {
            return "四人间";
        }
        
        // 统一转换为标准中文数字格式
        if (displayRoomType.contains("2") || displayRoomType.contains("双") || displayRoomType.contains("二") ||
            displayRoomType.equalsIgnoreCase("double_bed") || displayRoomType.equalsIgnoreCase("two_bed")) {
            return "二人间";
        } else if (displayRoomType.contains("4") || displayRoomType.contains("四") ||
                   displayRoomType.equalsIgnoreCase("four_bed") || displayRoomType.equalsIgnoreCase("four_person")) {
            return "四人间";
        } else if (displayRoomType.contains("6") || displayRoomType.contains("六") ||
                   displayRoomType.equalsIgnoreCase("six_bed") || displayRoomType.equalsIgnoreCase("six_person")) {
            return "六人间";
        } else if (displayRoomType.contains("8") || displayRoomType.contains("八") ||
                   displayRoomType.equalsIgnoreCase("eight_bed")) {
            return "八人间";
        }
        
        // 默认返回四人间
        return "四人间";
    }
    
    /**
     * 提取房间类型的匹配模式
     */
    private String extractRoomTypePattern(String displayRoomType) {
        if (displayRoomType == null) return null;
        
        if (displayRoomType.contains("2")) {
            return "2";
        } else if (displayRoomType.contains("4")) {
            return "4";
        } else if (displayRoomType.contains("6")) {
            return "6";
        } else if (displayRoomType.contains("8")) {
            return "8";
        }
        
        return null;
    }
    
}

