package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.BatchAssignRoomsDTO;
import com.campus.dorm.dto.BuildingAreaDTO;
import com.campus.dorm.dto.UpdateRoomStatusDTO;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.AdminMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.service.BuildingAreaService;
import com.campus.dorm.service.BuildingService;
import com.campus.dorm.service.DormRoomService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.BuildingAreaVO;
import com.campus.dorm.vo.BuildingStatisticsVO;
import com.campus.dorm.vo.RoomWithResidentsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 管理员宿舍管理Controller
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理员宿舍管理", description = "管理员管理楼栋、房间、区域等功能")
@Slf4j
public class AdminDormManagementController {
    
    private final BuildingService buildingService;
    private final DormRoomService dormRoomService;
    private final BuildingAreaService buildingAreaService;
    private final AdminMapper adminMapper;
    private final SysUserMapper sysUserMapper;
    private final BuildingMapper buildingMapper;
    private final DormRoomMapper dormRoomMapper;
    private final JwtUtil jwtUtil;
    
    /**
     * 验证当前管理员是否有权限访问指定楼栋
     * @param request HTTP请求
     * @param buildingId 楼栋ID
     * @return 管理员ID，如果无权限返回null
     */
    private Long validateBuildingAccess(HttpServletRequest request, Long buildingId) {
        try {
            // 从token获取当前用户ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return null;
            }
            
            // 获取管理员信息
            LambdaQueryWrapper<com.campus.dorm.entity.Admin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(com.campus.dorm.entity.Admin::getUserId, userId);
            com.campus.dorm.entity.Admin admin = adminMapper.selectOne(wrapper);
            if (admin == null) {
                return null;
            }
            
            // 检查该楼栋是否属于当前管理员
            Building building = buildingMapper.selectById(buildingId);
            if (building == null) {
                return null;
            }
            
            // 验证方式1：通过admin_id匹配
            boolean adminIdMatch = building.getAdminId() != null && building.getAdminId().equals(admin.getId());
            
            // 验证方式2：通过manager_name (username) 匹配
            boolean managerNameMatch = false;
            if (!adminIdMatch) {
                SysUser sysUser = sysUserMapper.selectById(userId);
                if (sysUser != null && building.getManagerName() != null && building.getManagerName().equals(sysUser.getUsername())) {
                    managerNameMatch = true;
                }
            }
            
            if (adminIdMatch || managerNameMatch) {
                return admin.getId();
            }
            
            log.warn("管理员{}尝试访问非其管理的楼栋{}", admin.getId(), buildingId);
            return null;
        } catch (Exception e) {
            log.error("权限验证失败", e);
            return null;
        }
    }
    
    /**
     * 通过房间ID验证权限
     * @param request HTTP请求
     * @param roomId 房间ID
     * @return 管理员ID，如果无权限返回null
     */
    private Long validateRoomAccess(HttpServletRequest request, Long roomId) {
        try {
            DormRoom room = dormRoomMapper.selectById(roomId);
            if (room == null) {
                return null;
            }
            return validateBuildingAccess(request, room.getBuildingId());
        } catch (Exception e) {
            log.error("房间权限验证失败", e);
            return null;
        }
    }
    
    /**
     * 批量验证房间权限
     * @param request HTTP请求
     * @param roomIds 房间ID列表
     * @return 管理员ID，如果任何一个房间无权限则返回null
     */
    private Long validateBatchRoomAccess(HttpServletRequest request, List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return null;
        }
        
        Long adminId = null;
        for (Long roomId : roomIds) {
            Long currentAdminId = validateRoomAccess(request, roomId);
            if (currentAdminId == null) {
                return null;
            }
            if (adminId == null) {
                adminId = currentAdminId;
            } else if (!adminId.equals(currentAdminId)) {
                // 所有房间必须属于同一个管理员
                log.warn("批量操作包含属于不同管理员的房间");
                return null;
            }
        }
        return adminId;
    }
    
    /**
     * 获取当前管理员管理的楼栋列表
     */
    @GetMapping("/my-buildings")
    @Operation(summary = "获取管理员管理的楼栋", description = "获取当前登录管理员管理的所有楼栋及统计信息")
    public Result<List<BuildingStatisticsVO>> getMyBuildings(HttpServletRequest request) {
        try {
            // 从token获取当前用户ID
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return Result.error("未登录");
            }
            
            // 获取管理员信息
            LambdaQueryWrapper<com.campus.dorm.entity.Admin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(com.campus.dorm.entity.Admin::getUserId, userId);
            com.campus.dorm.entity.Admin admin = adminMapper.selectOne(wrapper);
            if (admin == null) {
                return Result.error("非管理员用户");
            }
            
            log.info("获取管理员{}（userId={}）管理的楼栋", admin.getId(), userId);
            // 🔥 修复：传递userId而不是admin.id，因为BuildingService需要通过userId查询sys_user表
            return buildingService.getAdminBuildings(userId);
            
        } catch (Exception e) {
            log.error("获取管理员楼栋失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取楼栋统计信息
     */
    @GetMapping("/buildings/{id}/statistics")
    @Operation(summary = "获取楼栋统计信息", description = "获取指定楼栋的详细统计信息")
    public Result<BuildingStatisticsVO> getBuildingStatistics(
            @Parameter(description = "楼栋ID") @PathVariable Long id,
            HttpServletRequest request) {
        log.info("获取楼栋统计信息：buildingId={}", id);
        
        // 权限验证
        Long adminId = validateBuildingAccess(request, id);
        if (adminId == null) {
            return Result.error("无权访问该楼栋或楼栋不存在");
        }
        
        return buildingService.getBuildingStatistics(id);
    }
    
    /**
     * 获取楼栋所有房间（含入住学生）
     */
    @GetMapping("/buildings/{id}/rooms")
    @Operation(summary = "获取楼栋所有房间", description = "获取指定楼栋的所有房间及入住学生信息")
    public Result<List<RoomWithResidentsVO>> getBuildingRooms(
            @Parameter(description = "楼栋ID") @PathVariable Long id,
            HttpServletRequest request) {
        log.info("获取楼栋房间：buildingId={}", id);
        
        // 权限验证
        Long adminId = validateBuildingAccess(request, id);
        if (adminId == null) {
            return Result.error("无权访问该楼栋或楼栋不存在");
        }
        
        return dormRoomService.getRoomsWithResidents(id);
    }
    
    /**
     * 获取房间入住学生
     */
    @GetMapping("/rooms/{id}/residents")
    @Operation(summary = "获取房间入住学生", description = "获取指定房间的入住学生列表")
    public Result<List<RoomWithResidentsVO.ResidentInfo>> getRoomResidents(
            @Parameter(description = "房间ID") @PathVariable Long id,
            HttpServletRequest request) {
        log.info("获取房间入住学生：roomId={}", id);
        
        // 权限验证
        Long adminId = validateRoomAccess(request, id);
        if (adminId == null) {
            return Result.error("无权访问该房间或房间不存在");
        }
        
        return dormRoomService.getRoomResidents(id);
    }
    
    /**
     * 获取楼栋区域列表
     */
    @GetMapping("/buildings/{id}/areas")
    @Operation(summary = "获取楼栋区域列表", description = "获取指定楼栋的所有楼层区域")
    public Result<List<BuildingAreaVO>> getBuildingAreas(
            @Parameter(description = "楼栋ID") @PathVariable Long id,
            HttpServletRequest request) {
        log.info("获取楼栋区域列表：buildingId={}", id);
        
        // 权限验证
        Long adminId = validateBuildingAccess(request, id);
        if (adminId == null) {
            return Result.error("无权访问该楼栋或楼栋不存在");
        }
        
        return buildingAreaService.getAreasByBuilding(id);
    }
    
    /**
     * 创建区域
     */
    @PostMapping("/buildings/{id}/areas")
    @Operation(summary = "创建区域", description = "为楼栋创建新的楼层区域")
    public Result<?> createArea(
            @Parameter(description = "楼栋ID") @PathVariable Long id,
            @Validated @RequestBody BuildingAreaDTO dto,
            HttpServletRequest request) {
        log.info("创建区域：buildingId={}, dto={}", id, dto);
        
        // 权限验证
        Long adminId = validateBuildingAccess(request, id);
        if (adminId == null) {
            return Result.error("无权访问该楼栋或楼栋不存在");
        }
        
        dto.setBuildingId(id);
        return buildingAreaService.createArea(dto);
    }
    
    /**
     * 更新区域
     */
    @PutMapping("/areas/{id}")
    @Operation(summary = "更新区域", description = "更新楼层区域信息")
    public Result<?> updateArea(
            @Parameter(description = "区域ID") @PathVariable String id,
            @Validated @RequestBody BuildingAreaDTO dto) {
        log.info("更新区域：areaId={}, dto={}", id, dto);
        return buildingAreaService.updateArea(id, dto);
    }
    
    /**
     * 删除区域
     */
    @DeleteMapping("/areas/{id}")
    @Operation(summary = "删除区域", description = "删除楼层区域")
    public Result<?> deleteArea(
            @Parameter(description = "区域ID") @PathVariable String id) {
        log.info("删除区域：areaId={}", id);
        return buildingAreaService.deleteArea(id);
    }
    
    /**
     * 批量设置房间归属
     */
    @PutMapping("/rooms/batch-assign")
    @Operation(summary = "批量设置房间归属", description = "批量设置房间的归属专业和班级")
    public Result<?> batchAssignRooms(@Validated @RequestBody BatchAssignRoomsDTO dto,
                                       HttpServletRequest request) {
        log.info("批量设置房间归属：{}", dto);
        
        // 权限验证：验证所有房间是否都属于当前管理员
        Long adminId = validateBatchRoomAccess(request, dto.getRoomIds());
        if (adminId == null) {
            return Result.error("无权访问这些房间或房间不存在");
        }
        
        return dormRoomService.batchAssignRooms(dto);
    }
}

