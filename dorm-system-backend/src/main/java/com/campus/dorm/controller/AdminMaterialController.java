package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.util.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员端物资管理Controller
 * 
 * @author Campus Dorm System
 * @date 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/admin/materials")
@RequiredArgsConstructor
@Tag(name = "物资管理（管理员端）", description = "管理员物资清点和管理接口")
public class AdminMaterialController {
    
    private final MaterialTemplateMapper materialTemplateMapper;
    private final DormRoomMapper dormRoomMapper;
    private final DormGroupMapper dormGroupMapper;
    private final BuildingMapper buildingMapper;
    private final ApplicationMapper applicationMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取指定宿舍的物资配置模板
     */
    @GetMapping("/room-template")
    @Operation(summary = "获取宿舍物资配置", description = "根据宿舍号获取物资配置模板")
    public Result<Map<String, Object>> getRoomMaterialTemplate(@RequestParam String dormNumber) {
        log.info("管理员获取宿舍物资配置：dormNumber={}", dormNumber);
        
        // 查找房间信息
        LambdaQueryWrapper<DormRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.like(DormRoom::getRoomNumber, dormNumber)
                   .last("LIMIT 1");
        DormRoom room = dormRoomMapper.selectOne(roomWrapper);
        
        Map<String, Object> result = new HashMap<>();
        
        if (room == null) {
            result.put("materials", "[]");
            result.put("message", "未找到该宿舍");
            return Result.success(result);
        }
        
        // 获取物资模板
        String roomType = normalizeRoomType(room.getRoomType());
        
        LambdaQueryWrapper<MaterialTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(MaterialTemplate::getBuildingId, room.getBuildingId())
                       .eq(MaterialTemplate::getRoomType, roomType)
                       .last("LIMIT 1");
        
        MaterialTemplate template = materialTemplateMapper.selectOne(templateWrapper);
        
        if (template != null && template.getMaterials() != null) {
            result.put("materials", template.getMaterials());
            result.put("roomType", roomType);
            result.put("buildingId", room.getBuildingId());
        } else {
            result.put("materials", "[]");
            result.put("message", "该楼栋房型暂未配置物资");
        }
        
        return Result.success(result);
    }
    
    /**
     * 提交物资清点结果
     */
    @PostMapping("/inventory")
    @Operation(summary = "提交物资清点结果", description = "管理员提交退宿物资清点结果")
    public Result<?> submitInventoryResult(@RequestBody Map<String, Object> inventoryData) {
        log.info("管理员提交物资清点结果：{}", inventoryData);
        
        Long applicationId = Long.valueOf(inventoryData.get("applicationId").toString());
        
        // 查询申请
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return Result.error("申请不存在");
        }
        
        // 保存清点结果到申请的备注字段
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> materials = (List<Map<String, Object>>) inventoryData.get("materials");
        Double totalCompensation = Double.valueOf(inventoryData.get("totalCompensation").toString());
        
        StringBuilder inventoryResult = new StringBuilder();
        inventoryResult.append("【物资清点结果】\n");
        
        int damagedCount = 0;
        int missingCount = 0;
        
        for (Map<String, Object> material : materials) {
            String name = (String) material.get("name");
            String status = (String) material.get("status");
            Double compensation = material.get("compensation") != null ? 
                Double.valueOf(material.get("compensation").toString()) : 0.0;
            String remark = (String) material.get("remark");
            
            if ("damaged".equals(status)) {
                damagedCount++;
                inventoryResult.append(String.format("- %s: 损坏，赔偿 ¥%.2f", name, compensation));
                if (remark != null && !remark.isEmpty()) {
                    inventoryResult.append("（").append(remark).append("）");
                }
                inventoryResult.append("\n");
            } else if ("missing".equals(status)) {
                missingCount++;
                inventoryResult.append(String.format("- %s: 缺失，赔偿 ¥%.2f", name, compensation));
                if (remark != null && !remark.isEmpty()) {
                    inventoryResult.append("（").append(remark).append("）");
                }
                inventoryResult.append("\n");
            }
        }
        
        inventoryResult.append(String.format("\n损坏: %d项, 缺失: %d项, 赔偿总额: ¥%.2f", 
            damagedCount, missingCount, totalCompensation));
        
        // 更新申请的备注
        application.setAdminComment(inventoryResult.toString());
        applicationMapper.updateById(application);
        
        log.info("物资清点结果已保存：applicationId={}", applicationId);
        return Result.success("清点结果已保存");
    }
    
    /**
     * 获取物资清点记录
     */
    @GetMapping("/inventory/{applicationId}")
    @Operation(summary = "获取物资清点记录", description = "获取指定申请的物资清点结果")
    public Result<Map<String, Object>> getInventoryResult(@PathVariable Long applicationId) {
        log.info("获取物资清点记录：applicationId={}", applicationId);
        
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return Result.error("申请不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("applicationId", applicationId);
        result.put("inventoryResult", application.getAdminComment());
        result.put("status", application.getStatus());
        
        return Result.success(result);
    }
    
    /**
     * 确认赔偿完成
     */
    @PostMapping("/compensation/{applicationId}/confirm")
    @Operation(summary = "确认赔偿完成", description = "确认学生已完成赔偿，通过退宿申请")
    public Result<?> confirmCompensation(@PathVariable Long applicationId) {
        log.info("确认赔偿完成：applicationId={}", applicationId);
        
        Application application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return Result.error("申请不存在");
        }
        
        if (!"COMPENSATION_PENDING".equals(application.getStatus()) && !"待赔偿处理".equals(application.getStatus())) {
            return Result.error("该申请不在待赔偿状态");
        }
        
        // 更新状态为已通过
        application.setStatus("已通过");
        String comment = application.getAdminComment();
        application.setAdminComment(comment + "\n\n【赔偿已完成】");
        applicationMapper.updateById(application);
        
        // 释放学生宿舍
        releaseStudentDorm(application.getStudentId());
        
        log.info("赔偿确认完成，退宿申请已通过：applicationId={}", applicationId);
        return Result.success("赔偿已确认，退宿申请已通过");
    }
    
    /**
     * 释放学生宿舍（退宿时调用）
     * 从dorm_group中移除学生，并更新房间可用床位
     */
    private void releaseStudentDorm(Long studentId) {
        log.info("释放学生宿舍：studentId={}", studentId);
        
        try {
            // 从dorm_group获取学生的房间ID
            Long roomId = getStudentRoomIdFromDormGroup(studentId);
            
            // 从宿舍组中移除学生
            removeStudentFromDormGroup(studentId);
            
            // 更新房间的可用床位数（availableBeds + 1）
            if (roomId != null) {
                DormRoom room = dormRoomMapper.selectById(roomId);
                if (room != null && room.getAvailableBeds() != null && room.getBedCount() != null) {
                    int newAvailableBeds = Math.min(room.getAvailableBeds() + 1, room.getBedCount());
                    room.setAvailableBeds(newAvailableBeds);
                    dormRoomMapper.updateById(room);
                    log.info("房间可用床位已更新：roomId={}, availableBeds={}", roomId, newAvailableBeds);
                }
            }
            
            log.info("学生宿舍释放成功：studentId={}, roomId={}", studentId, roomId);
        } catch (Exception e) {
            log.error("释放学生宿舍失败：studentId={}", studentId, e);
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
     * 从宿舍组中移除学生
     */
    private void removeStudentFromDormGroup(Long studentId) {
        log.info("从宿舍组中移除学生：studentId={}", studentId);
        
        try {
            // 查找学生所在的宿舍组（作为组长）
            LambdaQueryWrapper<DormGroup> leaderWrapper = new LambdaQueryWrapper<>();
            leaderWrapper.eq(DormGroup::getLeaderId, studentId);
            DormGroup groupAsLeader = dormGroupMapper.selectOne(leaderWrapper);
            
            if (groupAsLeader != null) {
                handleLeaderCheckout(groupAsLeader, studentId);
                return;
            }
            
            // 查找学生所在的宿舍组（作为成员）
            LambdaQueryWrapper<DormGroup> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.like(DormGroup::getMembers, "\"id\":" + studentId);
            DormGroup groupAsMember = dormGroupMapper.selectOne(memberWrapper);
            
            if (groupAsMember != null) {
                removeFromMembersList(groupAsMember, studentId);
            }
        } catch (Exception e) {
            log.error("从宿舍组移除学生失败：studentId={}", studentId, e);
        }
    }
    
    /**
     * 处理组长退宿
     */
    private void handleLeaderCheckout(DormGroup group, Long studentId) {
        try {
            String membersJson = group.getMembers();
            if (membersJson == null || membersJson.trim().isEmpty()) {
                group.setStatus("disbanded");
                dormGroupMapper.updateById(group);
                return;
            }
            
            List<Map<String, Object>> members = objectMapper.readValue(
                membersJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            members.removeIf(m -> studentId.equals(((Number) m.get("id")).longValue()));
            
            if (members.isEmpty()) {
                group.setStatus("disbanded");
                group.setMembers("[]");
                group.setCurrentMembers(0);
            } else {
                Map<String, Object> newLeader = members.get(0);
                Long newLeaderId = ((Number) newLeader.get("id")).longValue();
                newLeader.put("isLeader", true);
                
                group.setLeaderId(newLeaderId);
                group.setMembers(objectMapper.writeValueAsString(members));
                group.setCurrentMembers(members.size());
            }
            dormGroupMapper.updateById(group);
        } catch (Exception e) {
            log.error("处理组长退宿失败：groupId={}", group.getId(), e);
        }
    }
    
    /**
     * 从成员列表中移除学生
     */
    private void removeFromMembersList(DormGroup group, Long studentId) {
        try {
            String membersJson = group.getMembers();
            if (membersJson == null || membersJson.trim().isEmpty()) {
                return;
            }
            
            List<Map<String, Object>> members = objectMapper.readValue(
                membersJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            members.removeIf(m -> studentId.equals(((Number) m.get("id")).longValue()));
            
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            dormGroupMapper.updateById(group);
        } catch (Exception e) {
            log.error("从成员列表移除学生失败：groupId={}", group.getId(), e);
        }
    }
    
    /**
     * 标准化房型名称
     * 统一使用中文数字格式：二人间、四人间、六人间、八人间
     */
    private String normalizeRoomType(String roomType) {
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
        
        return roomType;
    }
}
