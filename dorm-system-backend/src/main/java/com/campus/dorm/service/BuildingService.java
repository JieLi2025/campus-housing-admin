package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.BuildingDTO;
import com.campus.dorm.entity.Admin;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.AdminMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.campus.dorm.vo.BuildingStatisticsVO;
import com.campus.dorm.vo.BuildingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 宿舍楼管理服务
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingService {
    
    private final BuildingMapper buildingMapper;
    private final AdminMapper adminMapper;
    private final DormRoomMapper dormRoomMapper;
    private final SysUserMapper sysUserMapper;
    
    /**
     * 获取宿舍楼列表（分页）
     */
    public Result<Page<BuildingVO>> getBuildingList(Integer currentPage, Integer pageSize, 
                                                     String keyword, String buildingType) {
        log.info("获取宿舍楼列表：currentPage={}, pageSize={}, keyword={}, buildingType={}", 
                currentPage, pageSize, keyword, buildingType);
        
        // 构建查询条件（不再需要检查deleted字段，因为使用物理删除）
        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（宿舍楼名称或编码）
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(Building::getBuildingName, keyword)
                .or()
                .like(Building::getBuildingCode, keyword)
            );
        }
        
        // 类型筛选
        if (StringUtils.hasText(buildingType)) {
            queryWrapper.eq(Building::getBuildingType, buildingType);
        }
        
        // 按ID倒序（替代原来的created_at排序）
        queryWrapper.orderByDesc(Building::getId);
        
        // 分页查询
        Page<Building> page = new Page<>(currentPage, pageSize);
        Page<Building> buildingPage = buildingMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        Page<BuildingVO> voPage = new Page<>(buildingPage.getCurrent(), buildingPage.getSize(), buildingPage.getTotal());
        List<BuildingVO> buildingVOS = buildingPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(buildingVOS);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取所有宿舍楼（不分页，用于下拉选择）
     */
    public Result<List<BuildingVO>> getAllBuildings() {
        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Building::getBuildingCode);
        
        List<Building> buildings = buildingMapper.selectList(queryWrapper);
        List<BuildingVO> buildingVOS = buildings.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return Result.success(buildingVOS);
    }
    
    /**
     * 获取宿舍楼详情
     */
    public Result<BuildingVO> getBuildingDetail(Long buildingId) {
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("宿舍楼不存在");
        }
        
        BuildingVO vo = convertToVO(building);
        return Result.success(vo);
    }
    
    /**
     * 创建宿舍楼
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createBuilding(BuildingDTO dto) {
        log.info("创建宿舍楼：{}", dto);
        
        // 检查宿舍楼名称是否已存在
        LambdaQueryWrapper<Building> nameQueryWrapper = new LambdaQueryWrapper<>();
        nameQueryWrapper.eq(Building::getBuildingName, dto.getBuildingName());
        Long nameCount = buildingMapper.selectCount(nameQueryWrapper);
        
        if (nameCount > 0) {
            return Result.error("楼栋名称已存在，请使用其他名称");
        }
        
        // 检查宿舍楼编码是否已存在
        LambdaQueryWrapper<Building> codeQueryWrapper = new LambdaQueryWrapper<>();
        codeQueryWrapper.eq(Building::getBuildingCode, dto.getBuildingCode());
        Long codeCount = buildingMapper.selectCount(codeQueryWrapper);
        
        if (codeCount > 0) {
            return Result.error("宿舍楼编码已存在");
        }
        
        // 创建宿舍楼
        Building building = new Building();
        BeanUtils.copyProperties(dto, building);
        
        int result = buildingMapper.insert(building);
        if (result > 0) {
            log.info("宿舍楼创建成功：id={}", building.getId());
            return Result.success("创建成功");
        }
        
        return Result.error("创建失败");
    }
    
    /**
     * 更新宿舍楼
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateBuilding(BuildingDTO dto) {
        log.info("更新宿舍楼：{}", dto);
        
        if (dto.getId() == null) {
            return Result.error("宿舍楼ID不能为空");
        }
        
        Building building = buildingMapper.selectById(dto.getId());
        if (building == null) {
            return Result.error("宿舍楼不存在");
        }
        
        // 检查名称是否与其他宿舍楼重复
        if (StringUtils.hasText(dto.getBuildingName()) && !dto.getBuildingName().equals(building.getBuildingName())) {
            LambdaQueryWrapper<Building> nameQueryWrapper = new LambdaQueryWrapper<>();
            nameQueryWrapper.eq(Building::getBuildingName, dto.getBuildingName())
                    .ne(Building::getId, dto.getId());
            Long nameCount = buildingMapper.selectCount(nameQueryWrapper);
            if (nameCount > 0) {
                return Result.error("楼栋名称已存在，请使用其他名称");
            }
        }
        
        // 检查编码是否与其他宿舍楼重复
        if (StringUtils.hasText(dto.getBuildingCode()) && !dto.getBuildingCode().equals(building.getBuildingCode())) {
            LambdaQueryWrapper<Building> codeQueryWrapper = new LambdaQueryWrapper<>();
            codeQueryWrapper.eq(Building::getBuildingCode, dto.getBuildingCode())
                    .ne(Building::getId, dto.getId());
            Long codeCount = buildingMapper.selectCount(codeQueryWrapper);
            if (codeCount > 0) {
                return Result.error("宿舍楼编码已存在");
            }
        }
        
        // 更新宿舍楼信息
        BeanUtils.copyProperties(dto, building);
        int result = buildingMapper.updateById(building);
        
        if (result > 0) {
            log.info("宿舍楼更新成功：id={}", building.getId());
            return Result.success("更新成功");
        }
        
        return Result.error("更新失败");
    }
    
    /**
     * 删除宿舍楼（物理删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteBuilding(Long buildingId) {
        log.info("删除宿舍楼：buildingId={}", buildingId);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("宿舍楼不存在");
        }
        
        // 检查该楼栋的所有房间是否有人入住
        LambdaQueryWrapper<DormRoom> roomQueryWrapper = new LambdaQueryWrapper<>();
        roomQueryWrapper.eq(DormRoom::getBuildingId, buildingId);
        List<DormRoom> rooms = dormRoomMapper.selectList(roomQueryWrapper);
        
        // 统计总入住人数
        int totalOccupied = 0;
        for (DormRoom room : rooms) {
            totalOccupied += room.getCurrentCount();
        }
        
        if (totalOccupied > 0) {
            return Result.error(String.format("该宿舍楼还有 %d 人入住，无法删除", totalOccupied));
        }
        
        // 如果没有人入住，可以删除楼栋（同时会级联物理删除所有房间）
        // 先物理删除所有房间
        if (!rooms.isEmpty()) {
            for (DormRoom room : rooms) {
                dormRoomMapper.physicalDeleteById(room.getId());  // 使用物理删除
            }
            log.info("物理删除宿舍楼下的 {} 个房间", rooms.size());
        }
        
        // 物理删除楼栋（真正从数据库删除）
        int result = buildingMapper.physicalDeleteById(buildingId);  // 使用物理删除
        
        if (result > 0) {
            log.info("宿舍楼物理删除成功：buildingId={}", buildingId);
            return Result.success("删除成功");
        }
        
        return Result.error("删除失败");
    }
    
    /**
     * 为楼栋分配管理员
     * @param buildingId 楼栋ID
     * @param adminName 管理员姓名（为null表示取消分配）
     * @param adminPhone 管理员电话（可选）
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignManager(Long buildingId, String adminName, String adminPhone) {
        log.info("为楼栋{}分配管理员：{}", buildingId, adminName);
        
        // 1. 验证楼栋存在
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 2. 如果是分配管理员（不是取消分配），允许重新分配
        if (StringUtils.hasText(adminName)) {
            // 如果楼栋已有管理员且不是同一个人，记录重新分配操作
            if (StringUtils.hasText(building.getManagerName()) && !building.getManagerName().equals(adminName)) {
                log.info("楼栋{}将从管理员{}重新分配给管理员{}", buildingId, building.getManagerName(), adminName);
            }
        }
        
        // 3. 更新楼栋的管理员信息
        building.setManagerName(adminName);
        building.setManagerPhone(adminPhone);
        int result = buildingMapper.updateById(building);
        
        if (result > 0) {
            String action = StringUtils.hasText(adminName) ? "分配" : "取消分配";
            log.info("楼栋管理员{}成功：buildingId={}, managerName={}", action, buildingId, adminName);
            return Result.success(action + "成功");
        }
        
        return Result.error("操作失败");
    }
    
    /**
     * 批量为管理员分配楼栋
     * @param adminName 管理员姓名
     * @param adminPhone 管理员电话（可选）
     * @param buildingIds 楼栋ID列表
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignBuildingsToManager(String adminName, String adminPhone, List<Long> buildingIds) {
        log.info("为管理员{}分配楼栋：{}", adminName, buildingIds);
        
        if (!StringUtils.hasText(adminName)) {
            return Result.error("管理员姓名不能为空");
        }
        
        if (buildingIds == null || buildingIds.isEmpty()) {
            // 如果楼栋列表为空，清除该管理员的所有楼栋分配
            LambdaQueryWrapper<Building> query = new LambdaQueryWrapper<>();
            query.eq(Building::getManagerName, adminName);
            List<Building> buildings = buildingMapper.selectList(query);
            
            for (Building building : buildings) {
                building.setManagerName(null);
                building.setManagerPhone(null);
                buildingMapper.updateById(building);
            }
            
            log.info("清除管理员{}的所有楼栋分配", adminName);
            return Result.success("清除成功");
        }
        
        // 检查要分配的楼栋是否存在，并允许重新分配
        List<Building> buildingsToAssign = new ArrayList<>();
        for (Long buildingId : buildingIds) {
            Building building = buildingMapper.selectById(buildingId);
            if (building == null) {
                return Result.error("楼栋ID " + buildingId + " 不存在");
            }
            
            // 如果楼栋已有管理员且不是同一个人，记录重新分配操作
            if (StringUtils.hasText(building.getManagerName()) && !building.getManagerName().equals(adminName)) {
                log.info("楼栋{}将从管理员{}重新分配给管理员{}", buildingId, building.getManagerName(), adminName);
            }
            
            buildingsToAssign.add(building);
        }
        
        // 为每个楼栋分配管理员（不清除已有分配）
        int successCount = 0;
        for (Building building : buildingsToAssign) {
            building.setManagerName(adminName);
            building.setManagerPhone(adminPhone);
            int result = buildingMapper.updateById(building);
            if (result > 0) {
                successCount++;
                log.info("为管理员{}分配楼栋{}成功", adminName, building.getBuildingName());
            }
        }
        
        return Result.success("分配成功");
    }
    
    /**
     * 获取管理员管理的楼栋列表
     * @param userId sys_user表的用户ID
     * @return 楼栋统计信息列表
     */
    public Result<List<BuildingStatisticsVO>> getAdminBuildings(Long userId) {
        log.info("获取管理员管理的楼栋：userId={}", userId);
        
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("管理员不存在：userId={}", userId);
            return Result.success(new ArrayList<>());
        }
        
        // 1. 获取管理员信息（用于匹配admin_id）
        LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(Admin::getUserId, userId);
        Admin admin = adminMapper.selectOne(adminWrapper);
        Long adminId = (admin != null) ? admin.getId() : null;
        
        // 2. 查询楼栋：匹配 manager_name 或 admin_id
        LambdaQueryWrapper<Building> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(w -> {
                    // 匹配用户名
                    w.eq(Building::getManagerName, user.getUsername());
                    // 匹配管理员ID
                    if (adminId != null) {
                        w.or().eq(Building::getAdminId, adminId);
                    }
                })
                .orderByAsc(Building::getBuildingCode);
        
        List<Building> buildings = buildingMapper.selectList(queryWrapper);
        
        log.info("管理员{}（username={}, adminId={}）管理的楼栋数量：{}", 
                userId, user.getUsername(), adminId, buildings.size());
        
        // 转换为统计VO
        List<BuildingStatisticsVO> statisticsVOS = buildings.stream()
                .map(this::convertToStatisticsVO)
                .collect(Collectors.toList());
        
        return Result.success(statisticsVOS);
    }
    
    /**
     * 获取楼栋统计信息
     * @param buildingId 楼栋ID
     * @return 楼栋统计信息
     */
    public Result<BuildingStatisticsVO> getBuildingStatistics(Long buildingId) {
        log.info("获取楼栋统计信息：buildingId={}", buildingId);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        BuildingStatisticsVO vo = convertToStatisticsVO(building);
        return Result.success(vo);
    }
    
    /**
     * 分配管理员到楼栋
     * @param buildingId 楼栋ID
     * @param adminId 管理员ID
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> assignAdminToBuilding(Long buildingId, Long adminId) {
        log.info("分配管理员到楼栋：buildingId={}, adminId={}", buildingId, adminId);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 如果已有其他管理员，提示将重新分配
        if (building.getAdminId() != null && !building.getAdminId().equals(adminId)) {
            log.info("楼栋{}将从管理员{}重新分配给管理员{}", buildingId, building.getAdminId(), adminId);
        }
        
        building.setAdminId(adminId);
        int result = buildingMapper.updateById(building);
        
        if (result > 0) {
            return Result.success("分配成功");
        }
        
        return Result.error("分配失败");
    }
    
    // ==================== 阶段配置管理 ====================
    
    /**
     * 获取楼栋的阶段配置
     * @param buildingId 楼栋ID
     * @return 阶段配置信息
     */
    public Result<?> getPhaseConfig(Long buildingId) {
        log.info("获取楼栋阶段配置：buildingId={}", buildingId);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("buildingId", building.getId());
        config.put("buildingName", building.getBuildingName());
        config.put("currentPhase", building.getCurrentPhase());
        config.put("roomSelectionEnd", building.getRoomSelectionEnd());
        
        return Result.success(config);
    }
    
    /**
     * 更新楼栋的阶段配置
     * @param buildingId 楼栋ID
     * @param currentPhase 当前阶段
     * @param roomSelectionEnd 选房截止时间
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updatePhaseConfig(Long buildingId, String currentPhase, 
                                       LocalDateTime roomSelectionEnd) {
        log.info("更新楼栋阶段配置：buildingId={}, phase={}", buildingId, currentPhase);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        if (currentPhase != null) {
            building.setCurrentPhase(currentPhase);
        }
        building.setRoomSelectionEnd(roomSelectionEnd); // 允许设置为null
        
        int result = buildingMapper.updateById(building);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }
    
    /**
     * 切换楼栋的分配阶段
     * @param buildingId 楼栋ID
     * @param newPhase 新阶段
     * @return 切换结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> switchPhase(Long buildingId, String newPhase) {
        log.info("切换楼栋阶段：buildingId={}, newPhase={}", buildingId, newPhase);
        
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        String currentPhase = building.getCurrentPhase();
        
        // 验证阶段切换的合法性
        if (!isValidPhaseTransition(currentPhase, newPhase)) {
            return Result.error("不允许从 " + currentPhase + " 切换到 " + newPhase);
        }
        
        building.setCurrentPhase(newPhase);
        
        // 根据新阶段更新相关时间
        LocalDateTime now = LocalDateTime.now();
        switch (newPhase) {
            case "admin_allocation":
                if (building.getRoomSelectionEnd() == null) {
                    building.setRoomSelectionEnd(now);
                }
                break;
        }
        
        int result = buildingMapper.updateById(building);
        if (result > 0) {
            log.info("阶段切换成功：{} -> {}", currentPhase, newPhase);
            return Result.success("阶段切换成功");
        }
        return Result.error("阶段切换失败");
    }
    
    /**
     * 验证阶段切换的合法性
     */
    private boolean isValidPhaseTransition(String currentPhase, String newPhase) {
        if (currentPhase == null || currentPhase.equals(newPhase)) {
            return true;
        }
        
        switch (currentPhase) {
            case "forming_teams":
                return "room_selection".equals(newPhase);
            case "room_selection":
                return "admin_allocation".equals(newPhase) || "forming_teams".equals(newPhase);
            case "admin_allocation":
                return "completed".equals(newPhase) || "room_selection".equals(newPhase);
            case "completed":
                return false;
            default:
                return true;
        }
    }
    
    /**
     * 根据学生ID获取其所属楼栋的阶段配置
     * @param studentId 学生ID
     * @return 阶段配置
     */
    public Result<?> getPhaseConfigByStudent(Long studentId) {
        log.info("根据学生获取阶段配置：studentId={}", studentId);
        
        // 查询学生所属的楼栋（通过bed表关联）
        // 这里简化处理，返回第一个有阶段配置的楼栋
        LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Building::getCurrentPhase)
               .orderByDesc(Building::getId)
               .last("LIMIT 1");
        
        Building building = buildingMapper.selectOne(wrapper);
        if (building == null) {
            // 如果没有配置，返回默认值
            java.util.Map<String, Object> defaultConfig = new java.util.HashMap<>();
            defaultConfig.put("currentPhase", "forming_teams");
            defaultConfig.put("roomSelectionEnd", null);
            return Result.success(defaultConfig);
        }
        
        return getPhaseConfig(building.getId());
    }
    
    /**
     * 转换为统计VO
     */
    private BuildingStatisticsVO convertToStatisticsVO(Building building) {
        BuildingStatisticsVO vo = new BuildingStatisticsVO();
        
        vo.setId(building.getId());
        vo.setName(building.getBuildingName());
        vo.setCode(building.getBuildingCode());
        vo.setBuildingType(building.getBuildingType());
        vo.setDescription(building.getDescription());
        vo.setAdminId(building.getAdminId());
        vo.setManagerName(building.getManagerName());
        vo.setManagerPhone(building.getManagerPhone());
        
        // buildingType已经是中文了（男/女），直接使用
        vo.setGender(building.getBuildingType());
        
        // 统计房间和床位信息
        LambdaQueryWrapper<DormRoom> roomQueryWrapper = new LambdaQueryWrapper<>();
        roomQueryWrapper.eq(DormRoom::getBuildingId, building.getId());
        List<DormRoom> rooms = dormRoomMapper.selectList(roomQueryWrapper);
        
        int totalRooms = rooms.size();
        int totalBeds = 0;
        int occupiedBeds = 0;
        int availableBeds = 0;
        
        for (DormRoom room : rooms) {
            if (room.getBedCount() != null) {
                totalBeds += room.getBedCount();
            }
            if (room.getAvailableBeds() != null) {
                availableBeds += room.getAvailableBeds();
            }
        }
        
        occupiedBeds = totalBeds - availableBeds;
        
        vo.setTotalRooms(totalRooms);
        vo.setTotalBeds(totalBeds);
        vo.setOccupiedBeds(occupiedBeds);
        vo.setAvailableBeds(availableBeds);
        
        // 计算入住率
        if (totalBeds > 0) {
            int occupancyRate = (int) Math.round(occupiedBeds * 100.0 / totalBeds);
            vo.setOccupancyRate(occupancyRate);
        } else {
            vo.setOccupancyRate(0);
        }
        
        return vo;
    }
    
    /**
     * 转换为VO
     */
    private BuildingVO convertToVO(Building building) {
        BuildingVO vo = new BuildingVO();
        BeanUtils.copyProperties(building, vo);
        
        // 统计房间和床位信息
        LambdaQueryWrapper<DormRoom> roomQueryWrapper = new LambdaQueryWrapper<>();
        roomQueryWrapper.eq(DormRoom::getBuildingId, building.getId());
        List<DormRoom> rooms = dormRoomMapper.selectList(roomQueryWrapper);
        
        int totalBeds = 0;
        int occupiedBeds = 0;
        int allocatedRooms = 0;
        
        for (DormRoom room : rooms) {
            totalBeds += room.getCapacity();
            occupiedBeds += room.getCurrentCount();
            if (room.getCurrentCount() > 0) {
                allocatedRooms++;
            }
        }
        
        vo.setAllocatedRooms(allocatedRooms);
        vo.setTotalBeds(totalBeds);
        vo.setOccupiedBeds(occupiedBeds);
        
        // 计算入住率
        if (totalBeds > 0) {
            vo.setOccupancyRate(Math.round(occupiedBeds * 10000.0 / totalBeds) / 100.0);
        } else {
            vo.setOccupancyRate(0.0);
        }
        
        return vo;
    }
}

