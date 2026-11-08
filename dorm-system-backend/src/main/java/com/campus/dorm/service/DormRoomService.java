package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.BatchAssignRoomsDTO;
import com.campus.dorm.dto.BatchCreateRoomDTO;
import com.campus.dorm.dto.DormRoomDTO;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.vo.DormRoomVO;
import com.campus.dorm.vo.RoomWithResidentsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 宿舍房间服务
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Service
@Slf4j
public class DormRoomService {
    
    @Autowired
    private DormRoomMapper roomMapper;
    
    @Autowired
    private BuildingMapper buildingMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private com.campus.dorm.mapper.SysUserMapper sysUserMapper;
    
    @Autowired
    private com.campus.dorm.mapper.DormGroupMapper dormGroupMapper;
    
    /**
     * 获取房间列表（分页）
     */
    public Result<IPage<DormRoomVO>> getRoomList(Integer currentPage, Integer pageSize, Long buildingId, String keyword) {
        Page<DormRoom> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<DormRoom> wrapper = new LambdaQueryWrapper<>();
        
        if (buildingId != null) {
            wrapper.eq(DormRoom::getBuildingId, buildingId);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DormRoom::getRoomNumber, keyword);
        }
        
        wrapper.orderByAsc(DormRoom::getBuildingId, DormRoom::getRoomNumber);
        
        IPage<DormRoom> roomPage = roomMapper.selectPage(page, wrapper);
        IPage<DormRoomVO> voPage = roomPage.convert(room -> {
            DormRoomVO vo = new DormRoomVO();
            BeanUtils.copyProperties(room, vo);
            Building building = buildingMapper.selectById(room.getBuildingId());
            if (building != null) {
                vo.setBuildingName(building.getBuildingName());
            }
            return vo;
        });
        
        return Result.success(voPage);
    }
    
    /**
     * 创建房间
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createRoom(DormRoomDTO dto) {
        log.info("创建房间：buildingId={}, roomNumber={}", dto.getBuildingId(), dto.getRoomNumber());
        
        // 1. 验证楼栋存在
        Building building = buildingMapper.selectById(dto.getBuildingId());
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 2. 检查房间号是否重复
        LambdaQueryWrapper<DormRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DormRoom::getBuildingId, dto.getBuildingId())
               .eq(DormRoom::getRoomNumber, dto.getRoomNumber());
        Long count = roomMapper.selectCount(wrapper);
        
        if (count > 0) {
            return Result.error("房间号已存在");
        }
        
        // 3. 创建房间
        DormRoom room = new DormRoom();
        BeanUtils.copyProperties(dto, room);
        
        // 设置默认值
        if (room.getAvailableBeds() == null) {
            room.setAvailableBeds(room.getBedCount());
        }
        
        int result = roomMapper.insert(room);
        if (result > 0) {
            log.info("房间创建成功：id={}", room.getId());
            return Result.success("房间创建成功", room.getId());
        }
        
        return Result.error("房间创建失败");
    }
    
    /**
     * 批量创建房间
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Integer>> batchCreateRooms(BatchCreateRoomDTO dto) {
        log.info("批量创建房间：buildingId={}, 楼层{}-{}, 每层{}间", 
                dto.getBuildingId(), dto.getStartFloor(), dto.getEndFloor(), dto.getRoomsPerFloor());
        
        // 1. 验证楼栋存在
        Building building = buildingMapper.selectById(dto.getBuildingId());
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 2. 验证参数
        if (dto.getEndFloor() < dto.getStartFloor()) {
            return Result.error("结束楼层不能小于起始楼层");
        }
        
        // 3. 生成房间列表
        List<DormRoom> rooms = new ArrayList<>();
        int successCount = 0;
        int skipCount = 0;
        
        for (int floor = dto.getStartFloor(); floor <= dto.getEndFloor(); floor++) {
            for (int roomNum = 1; roomNum <= dto.getRoomsPerFloor(); roomNum++) {
                String roomNumber = String.valueOf(floor * 100 + roomNum);
                
                // 检查房间是否已存在
                LambdaQueryWrapper<DormRoom> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DormRoom::getBuildingId, dto.getBuildingId())
                       .eq(DormRoom::getRoomNumber, roomNumber);
                Long count = roomMapper.selectCount(wrapper);
                
                if (count > 0) {
                    skipCount++;
                    continue;
                }
                
                // 创建房间对象
                DormRoom room = new DormRoom();
                room.setBuildingId(dto.getBuildingId());
                room.setRoomNumber(roomNumber);
                room.setFloor(floor);
                room.setRoomType(dto.getRoomType());
                room.setBedCount(dto.getBedCount());
                room.setAvailableBeds(dto.getBedCount());
                
                rooms.add(room);
            }
        }
        
        // 4. 批量插入
        if (!rooms.isEmpty()) {
            int inserted = roomMapper.batchInsertRooms(rooms);
            successCount = inserted;
            log.info("批量创建房间完成：成功{}间，跳过{}间", successCount, skipCount);
        }
        
        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("successCount", successCount);
        resultMap.put("skipCount", skipCount);
        resultMap.put("totalCount", successCount + skipCount);
        
        return Result.success(resultMap);
    }
    
    /**
     * 更新房间
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateRoom(DormRoomDTO dto) {
        log.info("更新房间：id={}", dto.getId());
        
        if (dto.getId() == null) {
            return Result.error("房间ID不能为空");
        }
        
        DormRoom existingRoom = roomMapper.selectById(dto.getId());
        if (existingRoom == null) {
            return Result.error("房间不存在");
        }
        
        // 更新字段
        if (StringUtils.hasText(dto.getRoomNumber())) {
            // 检查新房间号是否重复
            if (!dto.getRoomNumber().equals(existingRoom.getRoomNumber())) {
                LambdaQueryWrapper<DormRoom> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(DormRoom::getBuildingId, existingRoom.getBuildingId())
                       .eq(DormRoom::getRoomNumber, dto.getRoomNumber())
                       .ne(DormRoom::getId, dto.getId());
                Long count = roomMapper.selectCount(wrapper);
                
                if (count > 0) {
                    return Result.error("新房间号已存在");
                }
            }
            existingRoom.setRoomNumber(dto.getRoomNumber());
        }
        
        if (dto.getFloor() != null) {
            existingRoom.setFloor(dto.getFloor());
        }
        if (StringUtils.hasText(dto.getRoomType())) {
            existingRoom.setRoomType(dto.getRoomType());
        }
        if (dto.getBedCount() != null) {
            // 如果总床位数变化，需要重新计算可用床位（保持已入住人数不变）
            if (dto.getAvailableBeds() == null) {
                // 1. 计算当前已入住人数
                int currentOccupied = existingRoom.getBedCount() - existingRoom.getAvailableBeds();
                
                // 2. 验证新床位数是否足够
                if (dto.getBedCount() < currentOccupied) {
                    log.warn("房间{}的新床位数{}小于已入住人数{}，更新失败", 
                            existingRoom.getRoomNumber(), dto.getBedCount(), currentOccupied);
                    return Result.error("新床位数(" + dto.getBedCount() + ")小于已入住人数(" + currentOccupied + ")，无法更新");
                }
                
                // 3. 更新床位数和可用床位数
                existingRoom.setBedCount(dto.getBedCount());
                existingRoom.setAvailableBeds(dto.getBedCount() - currentOccupied);
                
                log.info("房间{}床位数更新：已入住{}人，新床位数{}，可用床位{}", 
                        existingRoom.getRoomNumber(), currentOccupied, dto.getBedCount(), existingRoom.getAvailableBeds());
            } else {
                // 如果明确指定了可用床位数，则直接使用
                existingRoom.setBedCount(dto.getBedCount());
            }
        }
        if (dto.getAvailableBeds() != null) {
            existingRoom.setAvailableBeds(dto.getAvailableBeds());
        }
        
        int result = roomMapper.updateById(existingRoom);
        if (result > 0) {
            log.info("房间更新成功：id={}", dto.getId());
            return Result.success("房间更新成功");
        }
        
        return Result.error("房间更新失败");
    }
    
    /**
     * 删除房间
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteRoom(Long roomId) {
        log.info("删除房间：id={}", roomId);
        
        DormRoom room = roomMapper.selectById(roomId);
        if (room == null) {
            return Result.error("房间不存在");
        }
        
        // 检查房间是否有学生入住
        int occupiedBeds = room.getBedCount() - room.getAvailableBeds();
        if (occupiedBeds > 0) {
            return Result.error("房间有学生入住，无法删除");
        }
        
        // 物理删除
        int result = roomMapper.deleteById(roomId);
        if (result > 0) {
            log.info("房间删除成功：id={}", roomId);
            return Result.success("房间删除成功");
        }
        
        return Result.error("房间删除失败");
    }
    
    /**
     * 批量删除房间
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Integer>> batchDeleteRooms(List<Long> roomIds) {
        log.info("批量删除房间：ids={}", roomIds);
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long roomId : roomIds) {
            DormRoom room = roomMapper.selectById(roomId);
            if (room == null) {
                failCount++;
                errors.add("房间" + roomId + "不存在");
                continue;
            }
            
            // 检查是否有学生
            int occupiedBeds = room.getBedCount() - room.getAvailableBeds();
            if (occupiedBeds > 0) {
                failCount++;
                errors.add("房间" + room.getRoomNumber() + "有学生入住");
                continue;
            }
            
            int result = roomMapper.deleteById(roomId);
            if (result > 0) {
                successCount++;
            } else {
                failCount++;
            }
        }
        
        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("successCount", successCount);
        resultMap.put("failCount", failCount);
        
        log.info("批量删除完成：成功{}间，失败{}间", successCount, failCount);
        
        if (failCount > 0 && !errors.isEmpty()) {
            return Result.error(String.join("; ", errors), resultMap);
        }
        
        return Result.success(resultMap);
    }
    
    /**
     * 批量更新房间类型
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Integer>> batchUpdateRoomType(List<Long> roomIds, String newRoomType, Integer newBedCount) {
        log.info("批量更新房间类型：ids={}, roomType={}, bedCount={}", roomIds, newRoomType, newBedCount);
        
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long roomId : roomIds) {
            DormRoom room = roomMapper.selectById(roomId);
            if (room == null) {
                failCount++;
                errors.add("房间ID " + roomId + " 不存在");
                continue;
            }
            
            // 更新房间类型
            room.setRoomType(newRoomType);
            
            // 如果需要更新床位数
            if (newBedCount != null) {
                // 1. 先计算当前已入住人数
                int currentOccupied = room.getBedCount() - room.getAvailableBeds();
                
                // 2. 验证新床位数是否足够容纳已入住的学生
                if (newBedCount < currentOccupied) {
                    failCount++;
                    String errorMsg = "房间 " + room.getRoomNumber() + " 新床位数(" + newBedCount 
                            + ")小于已入住人数(" + currentOccupied + ")，跳过更新";
                    errors.add(errorMsg);
                    log.warn(errorMsg);
                    continue;
                }
                
                // 3. 更新床位数和可用床位数（保持已入住人数不变）
                room.setBedCount(newBedCount);
                room.setAvailableBeds(newBedCount - currentOccupied);
                
                log.info("房间 {} 床位数更新：{} -> {}，已入住：{}，可用：{}", 
                        room.getRoomNumber(), room.getBedCount(), newBedCount, currentOccupied, room.getAvailableBeds());
            }
            
            int result = roomMapper.updateById(room);
            if (result > 0) {
                successCount++;
            } else {
                failCount++;
                errors.add("房间 " + room.getRoomNumber() + " 更新失败");
            }
        }
        
        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("successCount", successCount);
        resultMap.put("failCount", failCount);
        
        log.info("批量更新完成：成功{}间，失败{}间", successCount, failCount);
        
        // 如果有失败的情况，返回错误信息
        if (failCount > 0 && !errors.isEmpty()) {
            return Result.error(String.join("; ", errors.subList(0, Math.min(3, errors.size()))), resultMap);
        }
        
        return Result.success(resultMap);
    }
    
    /**
     * 获取楼栋的所有房间
     */
    public Result<List<DormRoomVO>> getRoomsByBuilding(Long buildingId) {
        log.info("获取楼栋{}的所有房间", buildingId);
        
        List<DormRoomVO> rooms = roomMapper.getRoomsByBuildingId(buildingId);
        
        // 为每个房间加载入住学生信息
        for (DormRoomVO room : rooms) {
            List<com.campus.dorm.vo.RoomStudentVO> students = getStudentsByRoomId(room.getId());
            room.setStudents(students);
        }
        
        log.info("查询到{}间房间", rooms.size());
        return Result.success(rooms);
    }
    
    /**
     * 获取房间的入住学生列表
     * 从dorm_group表查询
     */
    private List<com.campus.dorm.vo.RoomStudentVO> getStudentsByRoomId(Long roomId) {
        List<com.campus.dorm.vo.RoomStudentVO> students = new ArrayList<>();
        Set<Long> addedStudentIds = new HashSet<>();
        
        // 从dorm_group表查询选择了该房间的宿舍组
        LambdaQueryWrapper<com.campus.dorm.entity.DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(com.campus.dorm.entity.DormGroup::getSelectedRoomId, roomId);
        com.campus.dorm.entity.DormGroup group = dormGroupMapper.selectOne(groupWrapper);
        
        if (group != null && group.getMembers() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> members = objectMapper.readValue(
                    group.getMembers(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> member : members) {
                    Object idObj = member.get("id");
                    Long memberId = null;
                    if (idObj instanceof Number) {
                        memberId = ((Number) idObj).longValue();
                    }
                    
                    if (memberId != null && !addedStudentIds.contains(memberId)) {
                        Student student = studentMapper.selectById(memberId);
                        if (student != null) {
                            com.campus.dorm.vo.RoomStudentVO studentVO = new com.campus.dorm.vo.RoomStudentVO();
                            studentVO.setStudentId(student.getId());
                            studentVO.setStudentNo(student.getStudentId());
                            studentVO.setRealName(student.getRealName());
                            
                            // 从members JSON获取床位号
                            Object bedNumberObj = member.get("bedNumber");
                            if (bedNumberObj != null) {
                                studentVO.setBedNumber(bedNumberObj.toString());
                            }
                            
                            students.add(studentVO);
                            addedStudentIds.add(memberId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析宿舍组成员失败：roomId={}", roomId, e);
            }
        }
        
        // 备选方案：从student表直接查询（通过roomId字段关联的学生）
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getRoomId, roomId);
        List<Student> directStudents = studentMapper.selectList(studentWrapper);
        
        int bedCounter = students.size() + 1;
        for (Student student : directStudents) {
            if (!addedStudentIds.contains(student.getId())) {
                com.campus.dorm.vo.RoomStudentVO studentVO = new com.campus.dorm.vo.RoomStudentVO();
                studentVO.setStudentId(student.getId());
                studentVO.setStudentNo(student.getStudentId());
                studentVO.setRealName(student.getRealName());
                studentVO.setBedNumber(String.valueOf(bedCounter++));
                students.add(studentVO);
                addedStudentIds.add(student.getId());
            }
        }
        
        return students;
    }
    
    /**
     * 获取房间详情
     */
    public Result<DormRoomVO> getRoomDetail(Long roomId) {
        log.info("获取房间详情：id={}", roomId);
        
        DormRoomVO room = roomMapper.getRoomDetailById(roomId);
        if (room == null) {
            return Result.error("房间不存在");
        }
        
        return Result.success(room);
    }
    
    /**
     * 获取楼栋所有房间及入住学生信息
     * @param buildingId 楼栋ID
     * @return 房间及入住学生列表
     */
    public Result<List<RoomWithResidentsVO>> getRoomsWithResidents(Long buildingId) {
        log.info("获取楼栋{}的所有房间及入住学生", buildingId);
        
        // 获取楼栋信息
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 获取楼栋所有房间
        LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(DormRoom::getBuildingId, buildingId)
                .orderByAsc(DormRoom::getFloor, DormRoom::getRoomNumber);
        List<DormRoom> rooms = roomMapper.selectList(roomWrapper);
        
        // 转换为VO并填充入住学生信息
        List<RoomWithResidentsVO> voList = rooms.stream()
                .map(room -> convertToRoomWithResidentsVO(room, building))
                .collect(Collectors.toList());
        
        return Result.success(voList);
    }
    
    /**
     * 获取房间入住学生列表
     * @param roomId 房间ID
     * @return 入住学生列表
     */
    public Result<List<RoomWithResidentsVO.ResidentInfo>> getRoomResidents(Long roomId) {
        log.info("获取房间{}的入住学生", roomId);
        
        DormRoom room = roomMapper.selectById(roomId);
        if (room == null) {
            return Result.error("房间不存在");
        }
        
        List<RoomWithResidentsVO.ResidentInfo> residents = new ArrayList<>();
        Set<Long> addedStudentIds = new HashSet<>();
        
        // 从dorm_group表查询选择了该房间的宿舍组
        LambdaQueryWrapper<com.campus.dorm.entity.DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(com.campus.dorm.entity.DormGroup::getSelectedRoomId, roomId);
        com.campus.dorm.entity.DormGroup group = dormGroupMapper.selectOne(groupWrapper);
        
        if (group != null && group.getMembers() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> members = objectMapper.readValue(
                    group.getMembers(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> member : members) {
                    Object idObj = member.get("id");
                    Long memberId = null;
                    if (idObj instanceof Number) {
                        memberId = ((Number) idObj).longValue();
                    }
                    
                    if (memberId != null && !addedStudentIds.contains(memberId)) {
                        Student student = studentMapper.selectById(memberId);
                        if (student != null) {
                            RoomWithResidentsVO.ResidentInfo info = new RoomWithResidentsVO.ResidentInfo();
                            info.setId(student.getId());
                            info.setStudentId(student.getStudentId());
                            info.setName(student.getRealName());
                            info.setMajor(student.getMajor());
                            info.setClassName(student.getClassName());
                            
                            // 从members JSON获取床位号
                            Object bedNumberObj = member.get("bedNumber");
                            if (bedNumberObj != null) {
                                info.setBedNumber(bedNumberObj.toString());
                            }
                            
                            // 获取邮箱（从sys_user表）
                            if (student.getUserId() != null) {
                                info.setEmail(getEmailByUserId(student.getUserId()));
                            }
                            
                            residents.add(info);
                            addedStudentIds.add(memberId);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析宿舍组成员失败：roomId={}", roomId, e);
            }
        }
        
        // 备选方案：从student表直接查询（通过roomId字段关联的学生）
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getRoomId, roomId);
        List<Student> directStudents = studentMapper.selectList(studentWrapper);
        
        int bedCounter = residents.size() + 1;
        for (Student student : directStudents) {
            if (!addedStudentIds.contains(student.getId())) {
                RoomWithResidentsVO.ResidentInfo info = new RoomWithResidentsVO.ResidentInfo();
                info.setId(student.getId());
                info.setStudentId(student.getStudentId());
                info.setName(student.getRealName());
                info.setMajor(student.getMajor());
                info.setClassName(student.getClassName());
                info.setBedNumber(String.valueOf(bedCounter++));
                
                // 获取邮箱（从sys_user表）
                if (student.getUserId() != null) {
                    info.setEmail(getEmailByUserId(student.getUserId()));
                }
                
                residents.add(info);
                addedStudentIds.add(student.getId());
            }
        }
        
        log.info("房间{}共有{}名入住学生", roomId, residents.size());
        return Result.success(residents);
    }
    
    /**
     * 根据用户ID获取邮箱
     */
    private String getEmailByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        com.campus.dorm.entity.SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * 批量设置房间归属
     * @param dto 批量分配数据
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchAssignRooms(BatchAssignRoomsDTO dto) {
        log.info("======== 批量设置房间归属 - 开始 ========");
        log.info("🔍 接收到的DTO完整内容: {}", dto);
        log.info("🔍 操作类型: {}", dto.getAction());
        log.info("🔍 楼栋ID: {}", dto.getBuildingId());
        log.info("🔍 专业名称: {}", dto.getMajorName());
        log.info("🔍 班级名称: {}", dto.getClassName());
        log.info("🔍 房间ID列表: {}", dto.getRoomIds());
        log.info("🔍 房间数量: {}", dto.getRoomIds() != null ? dto.getRoomIds().size() : 0);
        
        if (dto.getRoomIds() == null || dto.getRoomIds().isEmpty()) {
            log.error("❌ 房间ID列表为空");
            return Result.error("房间ID列表不能为空");
        }
        
        int successCount = 0;
        int failCount = 0;
        List<String> successRooms = new ArrayList<>();
        List<String> failRooms = new ArrayList<>();
        
        for (Long roomId : dto.getRoomIds()) {
            log.info("--- 处理房间 ID: {} ---", roomId);
            
            DormRoom room = roomMapper.selectById(roomId);
            if (room == null) {
                log.warn("⚠️ 房间不存在: roomId={}", roomId);
                failCount++;
                failRooms.add("ID:" + roomId + "(不存在)");
                continue;
            }
            
            log.info("🔍 更新前房间状态: roomId={}, roomNumber={}, buildingId={}, majorName={}, className={}", 
                    room.getId(), room.getRoomNumber(), room.getBuildingId(), 
                    room.getMajorName(), room.getClassName());
            
            if (!room.getBuildingId().equals(dto.getBuildingId())) {
                log.warn("⚠️ 房间楼栋ID不匹配: room.buildingId={}, dto.buildingId={}", 
                        room.getBuildingId(), dto.getBuildingId());
                failCount++;
                failRooms.add(room.getRoomNumber() + "(楼栋不匹配)");
                continue;
            }
            
            int result;
            if ("unassign".equals(dto.getAction())) {
                log.info("🔄 执行取消分配操作");
                // 🔥 修复：使用 UpdateWrapper 显式设置 null 值
                // 因为 MyBatis-Plus 的 update-strategy 配置为 not_null，直接 set null 不会更新
                LambdaUpdateWrapper<DormRoom> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(DormRoom::getId, roomId)
                        .set(DormRoom::getMajorName, null)
                        .set(DormRoom::getClassName, null);
                result = roomMapper.update(null, updateWrapper);
                log.info("🔍 准备更新房间: roomId={}, 新的majorName=null, 新的className=null");
            } else {
                log.info("🔄 执行分配操作");
                // 分配
                room.setMajorName(dto.getMajorName());
                
                // 🔥 防护：如果 className 等于 majorName，则设置为 null
                // 避免出现 "人工智能 - 人工智能" 这样的重复显示
                String className = dto.getClassName();
                if (className != null && className.equals(dto.getMajorName())) {
                    log.warn("⚠️ className({})等于majorName({})，自动设置为null", className, dto.getMajorName());
                    className = null;
                }
                room.setClassName(className);
                
                log.info("🔍 准备更新房间: roomId={}, 新的majorName={}, 新的className={}", 
                        room.getId(), room.getMajorName(), room.getClassName());
                result = roomMapper.updateById(room);
            }
            
            log.info("🔍 SQL执行结果: 影响行数={}", result);
            
            if (result > 0) {
                successCount++;
                successRooms.add(room.getRoomNumber());
                
                // 验证更新结果
                DormRoom updatedRoom = roomMapper.selectById(roomId);
                log.info("✅ 更新成功 - 验证: roomId={}, roomNumber={}, majorName={}, className={}", 
                        updatedRoom.getId(), updatedRoom.getRoomNumber(), 
                        updatedRoom.getMajorName(), updatedRoom.getClassName());
            } else {
                failCount++;
                failRooms.add(room.getRoomNumber() + "(更新失败)");
                log.error("❌ 更新失败: roomId={}, roomNumber={}", roomId, room.getRoomNumber());
            }
        }
        
        log.info("======== 批量设置房间归属 - 完成 ========");
        log.info("📊 统计: 总数={}, 成功={}, 失败={}", dto.getRoomIds().size(), successCount, failCount);
        log.info("✅ 成功的房间: {}", successRooms);
        if (!failRooms.isEmpty()) {
            log.warn("❌ 失败的房间: {}", failRooms);
        }
        
        String message = "成功处理 " + successCount + " 间房间";
        if (failCount > 0) {
            message += "，失败 " + failCount + " 间";
        }
        
        return Result.success(message);
    }
    
    /**
     * 转换为RoomWithResidentsVO
     */
    private RoomWithResidentsVO convertToRoomWithResidentsVO(DormRoom room, Building building) {
        RoomWithResidentsVO vo = new RoomWithResidentsVO();
        
        vo.setId(room.getId());
        vo.setBuildingId(room.getBuildingId());
        vo.setBuilding(building.getBuildingName());
        vo.setRoom(room.getRoomNumber());
        vo.setFloor(room.getFloor());
        vo.setType(room.getRoomType());
        vo.setTotalBeds(room.getBedCount());
        vo.setAvailableBeds(room.getAvailableBeds());
        vo.setOccupiedBeds(room.getBedCount() - room.getAvailableBeds());
        vo.setMajorName(room.getMajorName());
        vo.setClassName(room.getClassName());
        
        // 设置归属对象信息
        if (room.getMajorName() != null) {
            RoomWithResidentsVO.BelongToInfo belongTo = new RoomWithResidentsVO.BelongToInfo();
            belongTo.setBuildingId(room.getBuildingId());
            belongTo.setMajorName(room.getMajorName());
            belongTo.setClassName(room.getClassName());
            vo.setBelongTo(belongTo);
        }
        
        // 从dorm_group表获取入住学生列表
        List<RoomWithResidentsVO.ResidentInfo> residents = new ArrayList<>();
        
        LambdaQueryWrapper<com.campus.dorm.entity.DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.eq(com.campus.dorm.entity.DormGroup::getSelectedRoomId, room.getId());
        com.campus.dorm.entity.DormGroup group = dormGroupMapper.selectOne(groupWrapper);
        
        if (group != null && group.getMembers() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> members = objectMapper.readValue(
                    group.getMembers(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
                );
                
                for (Map<String, Object> member : members) {
                    Object idObj = member.get("id");
                    Long memberId = null;
                    if (idObj instanceof Number) {
                        memberId = ((Number) idObj).longValue();
                    }
                    
                    if (memberId != null) {
                        Student student = studentMapper.selectById(memberId);
                        if (student != null) {
                            RoomWithResidentsVO.ResidentInfo info = new RoomWithResidentsVO.ResidentInfo();
                            info.setId(student.getId());
                            info.setStudentId(student.getStudentId());
                            info.setName(student.getRealName());
                            info.setMajor(student.getMajor());
                            info.setClassName(student.getClassName());
                            
                            // 从members JSON获取床位号
                            Object bedNumberObj = member.get("bedNumber");
                            if (bedNumberObj != null) {
                                info.setBedNumber(bedNumberObj.toString());
                            }
                            
                            // 获取邮箱
                            if (student.getUserId() != null) {
                                info.setEmail(getEmailByUserId(student.getUserId()));
                            }
                            
                            residents.add(info);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析宿舍组成员失败：roomId={}", room.getId(), e);
            }
        }
        
        vo.setResidents(residents);
        
        return vo;
    }
    
}
