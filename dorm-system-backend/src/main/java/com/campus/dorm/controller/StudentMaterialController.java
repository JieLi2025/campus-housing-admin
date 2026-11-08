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
 * 学生端物资管理Controller
 * 
 * @author Campus Dorm System
 * @date 2025-11-29
 */
@Slf4j
@RestController
@RequestMapping("/student/materials")
@RequiredArgsConstructor
@Tag(name = "物资管理（学生端）", description = "学生查看宿舍物资的接口")
public class StudentMaterialController {
    
    private final StudentMapper studentMapper;
    private final DormRoomMapper dormRoomMapper;
    private final DormGroupMapper dormGroupMapper;
    private final MaterialTemplateMapper materialTemplateMapper;
    private final ApplicationMapper applicationMapper;
    private final BuildingMapper buildingMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
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
     * 从dorm_group获取学生的房间ID
     * @param studentId 学生ID
     * @return 房间ID，如果未分配则返回null
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
     * 获取学生的宿舍信息字符串（楼栋名-房间号）
     */
    private String getStudentDormInfo(Long studentId) {
        Long roomId = getStudentRoomIdFromDormGroup(studentId);
        if (roomId == null) {
            return "未分配";
        }
        
        DormRoom room = dormRoomMapper.selectById(roomId);
        if (room == null) {
            return "未分配";
        }
        
        Building building = buildingMapper.selectById(room.getBuildingId());
        return (building != null ? building.getBuildingName() : "") + "-" + room.getRoomNumber();
    }
    
    /**
     * 获取学生所在楼栋ID
     */
    private Long getStudentBuildingId(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student != null && student.getRoomId() != null) {
            DormRoom room = dormRoomMapper.selectById(student.getRoomId());
            if (room != null) {
                return room.getBuildingId();
            }
        }
        return null;
    }
    
    /**
     * 获取当前学生宿舍的物资配置模板
     */
    @GetMapping("/my-template")
    @Operation(summary = "获取我的宿舍物资配置", description = "根据学生所在楼栋和房型获取物资配置模板")
    public Result<Map<String, Object>> getMyRoomMaterialTemplate() {
        Long userId = getUserId();
        log.info("学生获取宿舍物资配置：userId={}", userId);
        
        // 1. 获取学生ID
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 2. 从dorm_group获取学生的宿舍分配信息
        Long roomId = getStudentRoomIdFromDormGroup(studentId);
        if (roomId == null) {
            // 学生尚未分配宿舍
            Map<String, Object> result = new HashMap<>();
            result.put("materials", "[]");
            result.put("message", "您尚未分配宿舍");
            return Result.success(result);
        }
        
        // 3. 获取房间信息（楼栋ID和房型）
        DormRoom room = dormRoomMapper.selectById(roomId);
        if (room == null) {
            return Result.error("房间信息不存在");
        }
        
        // 4. 根据楼栋ID和房型查询物资配置模板
        String roomType = normalizeRoomType(room.getRoomType());
        
        LambdaQueryWrapper<MaterialTemplate> templateWrapper = new LambdaQueryWrapper<>();
        templateWrapper.eq(MaterialTemplate::getBuildingId, room.getBuildingId())
                       .eq(MaterialTemplate::getRoomType, roomType)
                       .last("LIMIT 1");
        
        MaterialTemplate template = materialTemplateMapper.selectOne(templateWrapper);
        
        Map<String, Object> result = new HashMap<>();
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
    
    /**
     * 提交物品申报
     */
    @PostMapping("/report")
    @Operation(summary = "提交物品申报", description = "学生提交物品损坏/缺失等问题申报")
    public Result<?> submitMaterialReport(@RequestBody Map<String, Object> reportData) {
        Long userId = getUserId();
        log.info("学生提交物品申报：userId={}, reportData={}", userId, reportData);
        
        // 1. 获取学生信息
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生信息不存在");
        }
        
        // 2. 从dorm_group获取学生的宿舍信息
        String dormInfo = getStudentDormInfo(studentId);
        
        // 3. 构建申报原因
        String materialName = (String) reportData.get("materialName");
        String issueType = (String) reportData.get("issueType");
        String issue = (String) reportData.get("issue");
        
        String reason = String.format("【%s】%s - %s：%s", issueType, materialName, dormInfo, issue);
        
        // 4. 创建申请记录
        Application application = new Application();
        application.setStudentId(studentId);
        application.setBuildingId(getStudentBuildingId(studentId));  // 设置楼栋ID用于权限过滤
        application.setType("物品申报");  // 物品申报类型
        application.setReason(reason);
        application.setStatus("待审核");
        
        applicationMapper.insert(application);
        
        log.info("物品申报提交成功：applicationId={}", application.getId());
        return Result.success("申报提交成功，请等待管理员处理");
    }
    
    /**
     * 获取我的物品申报记录
     */
    @GetMapping("/my-reports")
    @Operation(summary = "获取我的物品申报记录", description = "获取当前学生的物品申报历史")
    public Result<List<Application>> getMyMaterialReports() {
        Long userId = getUserId();
        log.info("学生获取物品申报记录：userId={}", userId);
        
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getStudentId, studentId)
               .and(w -> w.eq(Application::getType, "物品申报").or().eq(Application::getType, "MATERIAL_REPORT"))
               .eq(Application::getDeleted, 0)
               .orderByDesc(Application::getCreatedAt);
        
        List<Application> reports = applicationMapper.selectList(wrapper);
        return Result.success(reports);
    }
    
    /**
     * 撤回物品申报（物理删除）
     */
    @DeleteMapping("/report/{id}")
    @Operation(summary = "撤回物品申报", description = "撤回待处理的物品申报，从数据库中删除")
    public Result<?> withdrawMaterialReport(@PathVariable Long id) {
        Long userId = getUserId();
        log.info("学生撤回物品申报：userId={}, reportId={}", userId, id);
        
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 查询申报记录
        Application application = applicationMapper.selectById(id);
        if (application == null) {
            return Result.error("申报记录不存在");
        }
        
        // 验证是否是本人的申报
        if (!application.getStudentId().equals(studentId)) {
            return Result.error("无权操作此申报");
        }
        
        // 验证是否是物品申报类型
        if (!"MATERIAL_REPORT".equals(application.getType()) && !"物品申报".equals(application.getType())) {
            return Result.error("只能撤回物品申报");
        }
        
        // 验证状态是否为待处理
        if (!"PENDING".equalsIgnoreCase(application.getStatus()) && !"待审核".equals(application.getStatus())) {
            return Result.error("只能撤回待处理的申报");
        }
        
        // 物理删除（真正从数据库删除）
        int rows = applicationMapper.physicalDeleteById(id);
        if (rows > 0) {
            log.info("物品申报撤回成功：reportId={}", id);
            return Result.success("申报已撤回");
        } else {
            return Result.error("撤回失败，请重试");
        }
    }
    
    /**
     * 修改物品申报
     */
    @PutMapping("/report/{id}")
    @Operation(summary = "修改物品申报", description = "修改待处理的物品申报")
    public Result<?> updateMaterialReport(@PathVariable Long id, @RequestBody Map<String, Object> reportData) {
        Long userId = getUserId();
        log.info("学生修改物品申报：userId={}, reportId={}, data={}", userId, id, reportData);
        
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        
        // 查询申报记录
        Application application = applicationMapper.selectById(id);
        if (application == null) {
            return Result.error("申报记录不存在");
        }
        
        // 验证是否是本人的申报
        if (!application.getStudentId().equals(studentId)) {
            return Result.error("无权操作此申报");
        }
        
        // 验证是否是物品申报类型
        if (!"MATERIAL_REPORT".equals(application.getType()) && !"物品申报".equals(application.getType())) {
            return Result.error("只能修改物品申报");
        }
        
        // 验证状态是否为待处理
        if (!"PENDING".equalsIgnoreCase(application.getStatus()) && !"待审核".equals(application.getStatus())) {
            return Result.error("只能修改待处理的申报");
        }
        
        // 获取宿舍信息（从学生信息关联查询）
        String dormInfo = "";
        Student student = studentMapper.selectById(studentId);
        if (student != null && student.getRoomId() != null) {
            DormRoom room = dormRoomMapper.selectById(student.getRoomId());
            if (room != null) {
                Building building = buildingMapper.selectById(room.getBuildingId());
                dormInfo = (building != null ? building.getBuildingName() : "") + "-" + room.getRoomNumber();
            }
        }
        
        // 更新申报内容
        String materialName = (String) reportData.get("materialName");
        String issueType = (String) reportData.get("issueType");
        String issue = (String) reportData.get("issue");
        
        String reason = String.format("【%s】%s - %s：%s", issueType, materialName, dormInfo, issue);
        application.setReason(reason);
        
        applicationMapper.updateById(application);
        
        log.info("物品申报修改成功：reportId={}", id);
        return Result.success("申报已修改");
    }
}
