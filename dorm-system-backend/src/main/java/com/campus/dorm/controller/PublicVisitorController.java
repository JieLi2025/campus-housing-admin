package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.StudentValidationDTO;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 公开访客API（无需认证）
 * 用于访客登记页面的验证
 * 
 * @author Campus Dorm System
 * @date 2025-11-06
 */
@Slf4j
@RestController
@RequestMapping("/public/visitor")
@RequiredArgsConstructor
@Tag(name = "公开访客API", description = "访客登记页面使用的公开接口，无需认证")
public class PublicVisitorController {
    
    private final StudentMapper studentMapper;
    private final DormRoomMapper dormRoomMapper;
    private final BuildingMapper buildingMapper;
    private final com.campus.dorm.mapper.VisitorMapper visitorMapper;
    private final com.campus.dorm.service.NotificationService notificationService;
    private final com.campus.dorm.mapper.DormGroupMapper dormGroupMapper;
    private final HttpServletRequest request;
    
    // 🔒 简单的限流机制：记录每个IP的请求次数
    // Key: IP地址, Value: 请求次数计数器
    private static final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();
    
    // 限流配置：每分钟最多10次请求
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long RATE_LIMIT_WINDOW = 60 * 1000; // 1分钟（毫秒）
    
    /**
     * 验证学生信息是否匹配楼栋和宿舍号
     * 用于访客登记页面的实时验证
     * 
     * 🔒 安全措施：
     * 1. 输入验证：使用 @Validated 和 DTO 进行参数校验
     * 2. 限流保护：每个IP每分钟最多10次请求
     * 3. 日志记录：记录所有验证请求，便于审计
     * 4. 信息最小化：不返回敏感信息（如学生ID）
     */
    @PostMapping("/validate-student")
    @Operation(summary = "验证学生信息", description = "验证学生姓名、楼栋和宿舍号是否匹配（公开API，有限流保护）")
    public Result<StudentValidationVO> validateStudent(
            @Validated @RequestBody StudentValidationDTO dto) {
        
        // 🔒 限流检查
        String clientIp = getClientIp();
        if (!checkRateLimit(clientIp)) {
            log.warn("IP {} 请求过于频繁，触发限流", clientIp);
            return Result.error("请求过于频繁，请稍后再试");
        }
        
        String studentName = dto.getStudentName();
        Long buildingId = dto.getBuildingId();
        String studentRoom = dto.getStudentRoom();
        
        log.info("验证学生信息：IP={}, studentName={}, buildingId={}, studentRoom={}", 
            clientIp, studentName, buildingId, studentRoom);
        
        StudentValidationVO vo = new StudentValidationVO();
        
        // 1. 通过学生姓名查找学生
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getRealName, studentName)
                     .eq(Student::getDeleted, 0);
        Student student = studentMapper.selectOne(studentWrapper);
        
        if (student == null) {
            vo.setValid(false);
            vo.setMessage("未找到该学生，请确认学生姓名是否正确");
            return Result.success(vo);
        }
        
        // 2. 从dorm_group表查询学生的宿舍分配信息
        DormRoom room = getStudentRoomFromDormGroup(student.getId());
        
        if (room == null) {
            vo.setValid(false);
            vo.setMessage("该学生暂未分配宿舍，无法进行访客登记");
            return Result.success(vo);
        }
        
        Building building = buildingMapper.selectById(room.getBuildingId());
        if (building == null) {
            vo.setValid(false);
            vo.setMessage("楼栋信息不存在");
            return Result.success(vo);
        }
        
        // 4. 验证宿舍号是否匹配
        String actualRoomNumber = room.getRoomNumber();
        String inputRoomNumber = studentRoom;
        
        // 提取数字进行比较（可能格式不同，如"101" vs "101室"）
        String actualRoomNum = actualRoomNumber != null ? actualRoomNumber.replaceAll("[^0-9]", "") : "";
        String inputRoomNum = inputRoomNumber != null ? inputRoomNumber.replaceAll("[^0-9]", "") : "";
        
        if (!actualRoomNum.equals(inputRoomNum)) {
            vo.setValid(false);
            // 🔒 安全：不暴露学生实际宿舍信息，只提示信息不符
            vo.setMessage("学生信息与宿舍号不匹配，请核实后重新填写");
            return Result.success(vo);
        }
        
        // 5. 如果提供了buildingId，验证楼栋是否匹配
        if (buildingId != null) {
            if (!buildingId.equals(building.getId())) {
                vo.setValid(false);
                // 🔒 安全：不暴露学生实际楼栋信息，只提示信息不符
                vo.setMessage("学生信息与所选楼栋不匹配，请核实后重新填写");
                return Result.success(vo);
            }
        }
        
        // 所有验证通过
        vo.setValid(true);
        vo.setMessage("验证通过");
        // 🔒 安全：验证通过后才返回楼栋和宿舍信息（用于确认显示）
        vo.setActualBuildingName(building.getBuildingName());
        vo.setActualRoomNumber(actualRoomNumber);
        
        return Result.success(vo);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果存在多个IP，取第一个
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return StringUtils.hasText(ip) ? ip : "unknown";
    }
    
    /**
     * 检查限流
     * @param ip 客户端IP
     * @return true表示允许请求，false表示触发限流
     */
    private boolean checkRateLimit(String ip) {
        long currentTime = System.currentTimeMillis();
        
        // 获取或创建计数器
        AtomicInteger count = requestCounts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        Long lastReset = lastResetTime.computeIfAbsent(ip, k -> currentTime);
        
        // 如果超过时间窗口，重置计数器
        if (currentTime - lastReset > RATE_LIMIT_WINDOW) {
            count.set(0);
            lastResetTime.put(ip, currentTime);
        }
        
        // 检查是否超过限制
        int currentCount = count.incrementAndGet();
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 公开访客登记接口（无需认证）
     * 用于访客在登录页面提交访客登记
     * 
     * 🔒 安全措施：
     * 1. 限流保护
     * 2. 不暴露学生实际宿舍信息
     */
    @PostMapping("/register")
    @Operation(summary = "访客登记", description = "访客提交登记信息（公开API，有限流保护）")
    public Result<?> registerVisitor(@Validated @RequestBody com.campus.dorm.dto.VisitorDTO dto) {
        // 🔒 限流检查
        String clientIp = getClientIp();
        if (!checkRateLimit(clientIp)) {
            log.warn("IP {} 请求过于频繁，触发限流", clientIp);
            return Result.error("请求过于频繁，请稍后再试");
        }
        
        log.info("访客登记：IP={}, visitorName={}, studentName={}, buildingId={}, studentRoom={}", 
            clientIp, dto.getVisitorName(), dto.getStudentName(), dto.getBuildingId(), dto.getStudentRoom());
        
        // 验证必填字段
        if (dto.getStudentName() == null || dto.getStudentName().trim().isEmpty()) {
            return Result.error("请填写被访学生姓名");
        }
        if (dto.getStudentRoom() == null || dto.getStudentRoom().trim().isEmpty()) {
            return Result.error("请填写宿舍号");
        }
        if (dto.getBuildingId() == null) {
            return Result.error("请选择楼栋");
        }
        
        // 1. 通过学生姓名查找学生
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getRealName, dto.getStudentName().trim())
                     .eq(Student::getDeleted, 0);
        java.util.List<Student> students = studentMapper.selectList(studentWrapper);
        
        if (students.isEmpty()) {
            // 🔒 安全：不暴露是否存在该学生
            return Result.error("学生信息验证失败，请核实后重新填写");
        }
        
        // 如果有多个同名学生，尝试通过宿舍号筛选
        Student student = null;
        if (students.size() > 1) {
            String inputRoomNum = dto.getStudentRoom().replaceAll("[^0-9]", "");
            for (Student s : students) {
                // 从dorm_group表查询该学生的宿舍分配
                DormRoom checkRoom = getStudentRoomFromDormGroup(s.getId());
                if (checkRoom != null) {
                    String roomNum = checkRoom.getRoomNumber().replaceAll("[^0-9]", "");
                    if (roomNum.equals(inputRoomNum) && checkRoom.getBuildingId().equals(dto.getBuildingId())) {
                        student = s;
                        break;
                    }
                }
            }
            if (student == null) {
                // 🔒 安全：不暴露具体原因
                return Result.error("学生信息与宿舍不匹配，请核实后重新填写");
            }
        } else {
            student = students.get(0);
        }
        
        // 2. 从dorm_group表查询学生的宿舍分配信息
        DormRoom room = getStudentRoomFromDormGroup(student.getId());
        
        if (room == null) {
            // 🔒 安全：不暴露学生未分配宿舍的信息
            return Result.error("学生信息验证失败，请核实后重新填写");
        }
        
        Building building = buildingMapper.selectById(room.getBuildingId());
        if (building == null) {
            return Result.error("系统错误，请稍后重试");
        }
        
        // 4. 验证宿舍号和楼栋是否匹配
        String actualRoomNum = room.getRoomNumber() != null ? room.getRoomNumber().replaceAll("[^0-9]", "") : "";
        String inputRoomNum = dto.getStudentRoom() != null ? dto.getStudentRoom().replaceAll("[^0-9]", "") : "";
        
        if (!actualRoomNum.equals(inputRoomNum)) {
            // 🔒 安全：不暴露学生实际宿舍信息
            return Result.error("学生信息与宿舍不匹配，请核实后重新填写");
        }
        
        if (!dto.getBuildingId().equals(building.getId())) {
            // 🔒 安全：不暴露学生实际楼栋信息
            return Result.error("学生信息与所选楼栋不匹配，请核实后重新填写");
        }
        
        // 5. 创建访客记录
        com.campus.dorm.entity.Visitor visitor = new com.campus.dorm.entity.Visitor();
        org.springframework.beans.BeanUtils.copyProperties(dto, visitor);
        
        visitor.setStudentId(student.getId());
        visitor.setStudentName(student.getRealName());
        visitor.setStudentNumber(student.getStudentId());
        visitor.setStudentRoom(room.getRoomNumber());
        visitor.setBuildingId(building.getId()); // 设置楼栋ID（用于权限过滤）
        visitor.setStatus("PENDING"); // 访客登记需要审核
        
        visitorMapper.insert(visitor);
        
        // 6. 发送通知给楼栋管理员
        try {
            com.campus.dorm.dto.NotificationDTO notificationDTO = new com.campus.dorm.dto.NotificationDTO();
            notificationDTO.setTitle("新的访客预约申请");
            notificationDTO.setMessage("访客" + visitor.getVisitorName() + "预约访问学生" + visitor.getStudentName() + "，请及时审批");
            notificationDTO.setType("visitor");
            notificationDTO.setRelatedId(visitor.getId());
            
            notificationService.createBuildingNotification(notificationDTO, building.getId(), null);
            log.info("访客通知已发送：visitorId={}, buildingId={}", visitor.getId(), building.getId());
        } catch (Exception e) {
            log.error("发送访客通知失败：visitorId={}", visitor.getId(), e);
            // 不影响主业务流程
        }
        
        log.info("访客登记成功：visitorId={}, visitorName={}", visitor.getId(), visitor.getVisitorName());
        return Result.success("访客登记提交成功，请等待管理员审核");
    }
    
    /**
     * 查询访客状态（公开API，无需认证）
     * 用于访客查询自己的登记状态
     */
    @GetMapping("/status")
    @Operation(summary = "查询访客状态", description = "根据身份证号和来访日期查询访客登记状态（公开API，有限流保护）")
    public Result<VisitorStatusVO> getVisitorStatus(
            @Parameter(description = "访客身份证号") @RequestParam String visitorIdCard,
            @Parameter(description = "来访日期（yyyy-MM-dd）") @RequestParam String visitDate) {
        
        // 🔒 限流检查
        String clientIp = getClientIp();
        if (!checkRateLimit(clientIp)) {
            log.warn("IP {} 请求过于频繁，触发限流", clientIp);
            return Result.error("请求过于频繁，请稍后再试");
        }
        
        log.info("查询访客状态：IP={}, visitorIdCard={}, visitDate={}", clientIp, visitorIdCard, visitDate);
        
        if (!StringUtils.hasText(visitorIdCard) || !StringUtils.hasText(visitDate)) {
            return Result.error("请提供身份证号和来访日期");
        }
        
        // 解析日期（只取前10位，格式：yyyy-MM-dd）
        java.time.LocalDate parsedDate;
        try {
            String dateStr = visitDate.length() > 10 ? visitDate.substring(0, 10) : visitDate;
            parsedDate = java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.error("日期格式解析失败：visitDate={}", visitDate, e);
            return Result.error("日期格式错误，请使用yyyy-MM-dd格式");
        }
        
        // 查询访客记录
        LambdaQueryWrapper<com.campus.dorm.entity.Visitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.campus.dorm.entity.Visitor::getVisitorIdCard, visitorIdCard)
               .eq(com.campus.dorm.entity.Visitor::getVisitDate, parsedDate)
               .eq(com.campus.dorm.entity.Visitor::getDeleted, 0)
               .orderByDesc(com.campus.dorm.entity.Visitor::getCreatedAt)
               .last("LIMIT 1");
        
        com.campus.dorm.entity.Visitor visitor = visitorMapper.selectOne(wrapper);
        
        if (visitor == null) {
            return Result.error("未找到访客登记记录");
        }
        
        // 查询楼栋名称
        String buildingName = "";
        if (visitor.getBuildingId() != null) {
            Building building = buildingMapper.selectById(visitor.getBuildingId());
            if (building != null) {
                buildingName = building.getBuildingName();
            }
        }
        
        // 构建返回结果
        VisitorStatusVO vo = new VisitorStatusVO();
        vo.setStatus(visitor.getStatus());
        vo.setStatusLabel(getStatusLabel(visitor.getStatus()));
        vo.setVisitorName(visitor.getVisitorName());
        vo.setVisitorPhone(visitor.getVisitorPhone());
        vo.setVisitorIdCard(visitor.getVisitorIdCard());
        vo.setStudentName(visitor.getStudentName());
        vo.setStudentRoom(visitor.getStudentRoom());
        vo.setBuildingName(buildingName);
        vo.setVisitDate(visitor.getVisitDate() != null ? visitor.getVisitDate().toString() : "");
        vo.setVisitTime(visitor.getVisitTime());
        vo.setVisitReason(visitor.getVisitReason());
        vo.setApprovedAt(visitor.getApprovedAt() != null ? visitor.getApprovedAt().toString() : null);
        vo.setCheckedInAt(visitor.getCheckedInAt() != null ? visitor.getCheckedInAt().toString() : null);
        vo.setCheckedOutAt(visitor.getCheckedOutAt() != null ? visitor.getCheckedOutAt().toString() : null);
        
        return Result.success(vo);
    }
    
    /**
     * 获取状态中文标签
     */
    private String getStatusLabel(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "PENDING": return "待审核";
            case "APPROVED": return "已通过";
            case "REJECTED": return "已拒绝";
            case "CHECKED_IN": return "已登记";
            case "COMPLETED": return "已离开";
            case "CANCELLED": return "已取消";
            default: return status;
        }
    }
    
    /**
     * 访客状态VO
     */
    @lombok.Data
    public static class VisitorStatusVO {
        private String status;
        private String statusLabel;
        private String visitorName;
        private String visitorPhone;
        private String visitorIdCard;
        private String studentName;
        private String studentRoom;
        private String buildingName;
        private String visitDate;
        private String visitTime;
        private String visitReason;
        private String approvedAt;
        private String checkedInAt;
        private String checkedOutAt;
    }
    
    /**
     * 验证结果VO
     */
    @lombok.Data
    public static class StudentValidationVO {
        /**
         * 是否有效
         */
        private Boolean valid;
        
        /**
         * 验证消息
         */
        private String message;
        
        /**
         * 实际楼栋名称
         */
        private String actualBuildingName;
        
        /**
         * 实际宿舍号
         */
        private String actualRoomNumber;
        
        // 🔒 安全：不返回学生ID，避免信息泄露
        // private Long studentId;
    }
    
    /**
     * 从dorm_group表获取学生的宿舍房间信息
     */
    private DormRoom getStudentRoomFromDormGroup(Long studentId) {
        try {
            // 查询学生所在的宿舍组
            LambdaQueryWrapper<com.campus.dorm.entity.DormGroup> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.and(wrapper -> wrapper
                            .like(com.campus.dorm.entity.DormGroup::getMembers, "\"id\":" + studentId)
                            .or()
                            .eq(com.campus.dorm.entity.DormGroup::getLeaderId, studentId))
                        .isNotNull(com.campus.dorm.entity.DormGroup::getSelectedRoomId);
            
            com.campus.dorm.entity.DormGroup group = dormGroupMapper.selectOne(groupWrapper);
            
            if (group != null && group.getSelectedRoomId() != null) {
                return dormRoomMapper.selectById(group.getSelectedRoomId());
            }
        } catch (Exception e) {
            log.warn("从DormGroup获取宿舍信息失败: studentId={}", studentId, e);
        }
        return null;
    }
}

