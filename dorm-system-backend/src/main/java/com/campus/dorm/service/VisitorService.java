package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.ApproveVisitorDTO;
import com.campus.dorm.dto.VisitorDTO;
import com.campus.dorm.dto.VisitorQueryDTO;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.Visitor;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Building;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.VisitorMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.vo.VisitorStatisticsVO;
import com.campus.dorm.vo.VisitorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 访客管理Service
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorService {
    
    private final VisitorMapper visitorMapper;
    private final StudentMapper studentMapper;
    private final DormRoomMapper dormRoomMapper;
    private final BuildingMapper buildingMapper;
    private final NotificationService notificationService;
    private final com.campus.dorm.mapper.DormGroupMapper dormGroupMapper;
    private final com.campus.dorm.mapper.SysUserMapper sysUserMapper;
    
    // 状态映射
    private static final Map<String, String> STATUS_MAP = new HashMap<>();
    
    static {
        STATUS_MAP.put("PENDING", "待审核");
        STATUS_MAP.put("APPROVED", "已批准");
        STATUS_MAP.put("REJECTED", "已拒绝");
        STATUS_MAP.put("COMPLETED", "已完成");
        STATUS_MAP.put("CANCELLED", "已取消");
    }
    
    /**
     * 学生提交访客预约
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> submitVisitor(Long studentId, VisitorDTO dto) {
        log.info("学生提交访客预约：studentId={}, dto={}", studentId, dto);
        
        // 查询学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        
        // 检查来访日期是否为未来日期
        if (dto.getVisitDate().isBefore(LocalDate.now())) {
            return Result.error("来访日期不能早于今天");
        }
        
        // 创建访客记录
        Visitor visitor = new Visitor();
        BeanUtils.copyProperties(dto, visitor);
        
        visitor.setStudentId(studentId);
        visitor.setStudentName(student.getRealName());
        visitor.setStudentNumber(student.getStudentId());
        // 设置学生宿舍信息（TODO: 需要从宿舍分配记录中查询）
        visitor.setStudentRoom("待完善"); // 暂时留空，后续从宿舍分配记录中获取
        visitor.setStatus("PENDING");
        
        visitorMapper.insert(visitor);
        
        // 发送通知给楼栋管理员
        notifyBuildingAdminForVisitor(visitor, studentId);
        
        log.info("访客预约提交成功：visitorId={}", visitor.getId());
        return Result.success("预约提交成功，请等待管理员审批");
    }
    
    /**
     * 发送访客通知给楼栋管理员
     */
    private void notifyBuildingAdminForVisitor(Visitor visitor, Long studentId) {
        try {
            // 获取学生所在楼栋ID
            Long buildingId = getStudentBuildingId(studentId);
            if (buildingId == null) {
                log.warn("无法发送访客通知：学生未分配宿舍，studentId={}", studentId);
                return;
            }
            
            // 创建通知
            com.campus.dorm.dto.NotificationDTO notificationDTO = new com.campus.dorm.dto.NotificationDTO();
            notificationDTO.setTitle("新的访客预约申请");
            notificationDTO.setMessage("访客" + visitor.getVisitorName() + "预约访问学生" + visitor.getStudentName() + "，请及时审批");
            notificationDTO.setType("visitor");
            notificationDTO.setRelatedId(visitor.getId());
            notificationDTO.setTargetBuildingId(buildingId);
            
            notificationService.createBuildingNotification(notificationDTO, buildingId, null);
            log.info("访客通知已发送：visitorId={}, buildingId={}", visitor.getId(), buildingId);
        } catch (Exception e) {
            log.error("发送访客通知失败：visitorId={}", visitor.getId(), e);
            // 不影响主业务流程
        }
    }
    
    /**
     * 获取学生所在楼栋ID（从dorm_group表获取）
     */
    private Long getStudentBuildingId(Long studentId) {
        try {
            // 从dorm_group表查询学生所在的宿舍组
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.dorm.entity.DormGroup> groupWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            groupWrapper.and(wrapper -> wrapper
                            .like(com.campus.dorm.entity.DormGroup::getMembers, "\"id\":" + studentId)
                            .or()
                            .eq(com.campus.dorm.entity.DormGroup::getLeaderId, studentId))
                        .isNotNull(com.campus.dorm.entity.DormGroup::getSelectedRoomId);
            com.campus.dorm.entity.DormGroup group = dormGroupMapper.selectOne(groupWrapper);
            
            if (group != null && group.getSelectedRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                if (room != null) {
                    return room.getBuildingId();
                }
            }
        } catch (Exception e) {
            log.warn("获取学生楼栋ID失败：studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 管理员创建访客记录（允许学生信息为空）
     * 如果提供了学生姓名和宿舍号，必须验证这些信息是否匹配
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createVisitorByAdmin(VisitorDTO dto) {
        log.info("管理员创建访客记录：dto={}", dto);
        
        // 检查来访日期是否为未来日期
        if (dto.getVisitDate() != null && dto.getVisitDate().isBefore(LocalDate.now())) {
            return Result.error("来访日期不能早于今天");
        }
        
        // 🔥 强制验证：访客登记必须提供学生姓名、楼栋和宿舍号，且必须验证通过
        // 这是访客登记的核心要求，不能跳过
        if (StringUtils.isBlank(dto.getStudentName())) {
            log.warn("访客登记缺少学生姓名：dto={}", dto);
            return Result.error("访客登记必须提供被访学生姓名");
        }
        
        if (StringUtils.isBlank(dto.getStudentRoom())) {
            log.warn("访客登记缺少宿舍号：dto={}", dto);
            return Result.error("访客登记必须提供宿舍号");
        }
        
        if (dto.getBuildingId() == null) {
            log.warn("访客登记缺少楼栋ID：dto={}", dto);
            return Result.error("访客登记必须选择楼栋");
        }
        
        // 通过学生姓名查找学生（精确匹配）
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getRealName, dto.getStudentName())
                     .eq(Student::getDeleted, 0);
        List<Student> students = studentMapper.selectList(studentWrapper);
        
        if (students.isEmpty()) {
            log.warn("未找到学生：studentName={}", dto.getStudentName());
            return Result.error("未找到该学生，请确认学生姓名是否正确");
        }
        
        if (students.size() > 1) {
            log.warn("存在多个同名学生：studentName={}, count={}", dto.getStudentName(), students.size());
            // 如果提供了宿舍号，尝试通过宿舍号进一步筛选
            if (StringUtils.isNotBlank(dto.getStudentRoom())) {
                String inputRoomNum = dto.getStudentRoom().replaceAll("[^0-9]", "");
                for (Student s : students) {
                    // 从dorm_group表查询该学生的宿舍分配
                    DormRoom checkRoom = getStudentRoomFromDormGroup(s.getId());
                    if (checkRoom != null) {
                        String roomNum = checkRoom.getRoomNumber().replaceAll("[^0-9]", "");
                        if (roomNum.equals(inputRoomNum) && checkRoom.getBuildingId().equals(dto.getBuildingId())) {
                            // 找到匹配的学生
                            students = List.of(s);
                            break;
                        }
                    }
                }
            }
            
            if (students.size() > 1) {
                return Result.error("存在多个同名学生（" + dto.getStudentName() + "），请提供正确的宿舍号以区分");
            }
        }
        
        Student student = students.get(0);
        log.info("找到学生：studentId={}, studentName={}", student.getId(), student.getRealName());
        
        // 从dorm_group表查询学生的宿舍分配信息
        DormRoom room = getStudentRoomFromDormGroup(student.getId());
        
        if (room == null) {
            log.warn("学生未分配宿舍：studentId={}, studentName={}", student.getId(), student.getRealName());
            return Result.error("该学生暂未分配宿舍，无法进行访客登记");
        }
        
        Building actualBuilding = buildingMapper.selectById(room.getBuildingId());
        if (actualBuilding == null) {
            log.error("楼栋不存在：buildingId={}", room.getBuildingId());
            return Result.error("楼栋信息不存在");
        }
        
        String actualRoomNumber = room.getRoomNumber();
        String inputRoomNumber = dto.getStudentRoom();
        
        log.info("验证宿舍号：学生={}, 输入宿舍号={}, 实际宿舍号={}, 实际楼栋={}", 
            dto.getStudentName(), inputRoomNumber, actualRoomNumber, actualBuilding.getBuildingName());
        
        // 提取数字进行比较（可能格式不同，如"101" vs "101室"）
        String actualRoomNum = actualRoomNumber != null ? actualRoomNumber.replaceAll("[^0-9]", "") : "";
        String inputRoomNum = inputRoomNumber != null ? inputRoomNumber.replaceAll("[^0-9]", "") : "";
        
        if (!actualRoomNum.equals(inputRoomNum)) {
            log.warn("宿舍号不匹配：学生={}, 输入={}, 实际={}", dto.getStudentName(), inputRoomNum, actualRoomNum);
            return Result.error(String.format("学生%s不在%s宿舍，实际住在%s-%s", 
                dto.getStudentName(), inputRoomNumber, actualBuilding.getBuildingName(), actualRoomNumber));
        }
        
        // 强制验证楼栋是否匹配
        if (!dto.getBuildingId().equals(actualBuilding.getId())) {
            Building selectedBuilding = buildingMapper.selectById(dto.getBuildingId());
            String selectedBuildingName = selectedBuilding != null ? selectedBuilding.getBuildingName() : "所选楼栋";
            log.warn("楼栋不匹配：学生={}, 选择楼栋={}, 实际楼栋={}", 
                dto.getStudentName(), selectedBuildingName, actualBuilding.getBuildingName());
            return Result.error(String.format("学生%s不在%s，实际住在%s", 
                dto.getStudentName(), selectedBuildingName, actualBuilding.getBuildingName()));
        }
        
        log.info("验证通过：学生={}, 楼栋={}, 宿舍号={}", 
            dto.getStudentName(), actualBuilding.getBuildingName(), actualRoomNumber);
        
        // 创建访客记录
        Visitor visitor = new Visitor();
        BeanUtils.copyProperties(dto, visitor);
        
        // 设置正确的学生信息（验证已通过）
        visitor.setStudentId(student.getId());
        visitor.setStudentName(student.getRealName());
        visitor.setStudentNumber(student.getStudentId());
        visitor.setStudentRoom(actualRoomNumber);
        // 设置楼栋ID（用于权限过滤）
        visitor.setBuildingId(actualBuilding.getId());
        
        // 管理员创建的访客默认状态为已批准（APPROVED）
        visitor.setStatus("APPROVED");
        // 管理员创建时自动设置审核时间为当前时间
        visitor.setApprovedAt(LocalDateTime.now());
        
        visitorMapper.insert(visitor);
        
        log.info("管理员创建访客记录成功：visitorId={}", visitor.getId());
        return Result.success("访客记录创建成功");
    }
    
    /**
     * 获取访客列表（管理员端，支持分页和筛选）
     * 普通管理员只能看到自己管理楼栋的访客
     * @param queryDTO 查询条件
     * @param adminUserId 管理员用户ID（用于权限过滤）
     */
    public Result<IPage<VisitorVO>> getVisitorList(VisitorQueryDTO queryDTO, Long adminUserId) {
        log.info("获取访客列表：queryDTO={}, adminUserId={}", queryDTO, adminUserId);
        
        Page<Visitor> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<Visitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visitor::getDeleted, 0);
        
        // 按楼栋过滤（管理员只能看到自己管理楼栋的访客）
        List<Long> managedBuildingIds = getAdminManagedBuildingIds(adminUserId);
        if (managedBuildingIds.isEmpty()) {
            log.warn("管理员没有管理任何楼栋：adminUserId={}", adminUserId);
            return Result.success(new Page<>());
        }
        wrapper.in(Visitor::getBuildingId, managedBuildingIds);
        
        // 关键词搜索（访客姓名、学生姓名或学号）
        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            wrapper.and(w -> w
                .like(Visitor::getVisitorName, queryDTO.getKeyword())
                .or()
                .like(Visitor::getStudentName, queryDTO.getKeyword())
                .or()
                .like(Visitor::getStudentNumber, queryDTO.getKeyword())
            );
        }
        
        // 状态筛选
        if (StringUtils.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(Visitor::getStatus, queryDTO.getStatus());
        }
        
        // 日期筛选
        if (queryDTO.getVisitDate() != null) {
            wrapper.eq(Visitor::getVisitDate, queryDTO.getVisitDate());
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Visitor::getCreatedAt);
        
        IPage<Visitor> visitorPage = visitorMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<VisitorVO> voPage = visitorPage.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取管理员管理的楼栋ID列表
     */
    private List<Long> getAdminManagedBuildingIds(Long adminUserId) {
        try {
            // 获取用户信息
            com.campus.dorm.entity.SysUser user = sysUserMapper.selectById(adminUserId);
            if (user == null) {
                log.warn("管理员用户不存在：userId={}", adminUserId);
                return List.of();
            }
            
            // 查询楼栋：匹配 manager_name（username）
            LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
            buildingWrapper.eq(Building::getManagerName, user.getUsername());
            List<Building> buildings = buildingMapper.selectList(buildingWrapper);
            
            log.info("管理员{}（username={}）管理的楼栋数量：{}", adminUserId, user.getUsername(), buildings.size());
            
            return buildings.stream()
                    .map(Building::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取管理员管理的楼栋失败：adminUserId={}", adminUserId, e);
            return List.of();
        }
    }
    
    /**
     * 获取我的访客列表（学生端）
     */
    public Result<IPage<VisitorVO>> getMyVisitorList(Long studentId, Integer currentPage, Integer pageSize) {
        log.info("学生获取访客列表：studentId={}, currentPage={}, pageSize={}", studentId, currentPage, pageSize);
        
        Page<Visitor> page = new Page<>(currentPage, pageSize);
        
        LambdaQueryWrapper<Visitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.eq(Visitor::getStudentId, studentId);
        wrapper.orderByDesc(Visitor::getCreatedAt);
        
        IPage<Visitor> visitorPage = visitorMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<VisitorVO> voPage = visitorPage.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取访客详情
     */
    public Result<VisitorVO> getVisitorDetail(Long id) {
        log.info("获取访客详情：id={}", id);
        
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor == null || visitor.getDeleted() == 1) {
            return Result.error("访客记录不存在");
        }
        
        VisitorVO vo = convertToVO(visitor);
        return Result.success(vo);
    }
    
    /**
     * 审批访客（批准/拒绝）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> approveVisitor(Long id, Long adminId, ApproveVisitorDTO dto) {
        log.info("审批访客：id={}, adminId={}, dto={}", id, adminId, dto);
        
        Visitor visitor = visitorMapper.selectById(id);
        log.info("查询访客结果：visitor={}, deleted={}", visitor, visitor != null ? visitor.getDeleted() : "null");
        if (visitor == null || visitor.getDeleted() == 1) {
            log.warn("访客记录不存在或已删除：id={}", id);
            return Result.error("访客记录不存在");
        }
        
        if (!"PENDING".equals(visitor.getStatus())) {
            return Result.error("该访客预约已处理，无法重复审批");
        }
        
        // 更新审批信息
        visitor.setAdminId(adminId);
        // 从数据库获取管理员真实姓名
        String adminName = "管理员";
        com.campus.dorm.entity.SysUser adminUser = sysUserMapper.selectById(adminId);
        if (adminUser != null && org.springframework.util.StringUtils.hasText(adminUser.getRealName())) {
            adminName = adminUser.getRealName();
        }
        visitor.setAdminName(adminName);
        visitor.setAdminComment(dto.getAdminComment());
        visitor.setApprovedAt(LocalDateTime.now());
        
        if (dto.getApproved()) {
            // 批准
            visitor.setStatus("APPROVED");
        } else {
            // 拒绝
            if (StringUtils.isBlank(dto.getRejectReason())) {
                return Result.error("拒绝时必须填写拒绝原因");
            }
            visitor.setStatus("REJECTED");
            visitor.setRejectReason(dto.getRejectReason());
        }
        
        visitorMapper.updateById(visitor);
        
        log.info("访客审批成功：visitorId={}, status={}", id, visitor.getStatus());
        return Result.success(dto.getApproved() ? "访客预约已批准" : "访客预约已拒绝");
    }
    
    /**
     * 访客签到
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> checkIn(Long id) {
        log.info("访客签到：id={}", id);
        
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor == null || visitor.getDeleted() == 1) {
            return Result.error("访客记录不存在");
        }
        
        if (!"APPROVED".equals(visitor.getStatus())) {
            return Result.error("只有已批准的访客才能签到");
        }
        
        if (visitor.getCheckedInAt() != null) {
            return Result.error("该访客已签到，请勿重复签到");
        }
        
        // 记录签到时间并更新状态
        visitor.setCheckedInAt(LocalDateTime.now());
        visitor.setStatus("CHECKED_IN"); // 更新状态为已登记
        visitorMapper.updateById(visitor);
        
        log.info("访客签到成功：visitorId={}, checkedInAt={}", id, visitor.getCheckedInAt());
        return Result.success("签到成功");
    }
    
    /**
     * 访客签退
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> checkOut(Long id) {
        log.info("访客签退：id={}", id);
        
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor == null || visitor.getDeleted() == 1) {
            return Result.error("访客记录不存在");
        }
        
        if (visitor.getCheckedInAt() == null) {
            return Result.error("该访客未签到，无法签退");
        }
        
        if (visitor.getCheckedOutAt() != null) {
            return Result.error("该访客已签退，请勿重复签退");
        }
        
        // 记录签退时间
        visitor.setCheckedOutAt(LocalDateTime.now());
        
        // 更新状态为已完成
        visitor.setStatus("COMPLETED");
        
        visitorMapper.updateById(visitor);
        
        log.info("访客签退成功：visitorId={}", id);
        return Result.success("签退成功");
    }
    
    /**
     * 删除/取消访客预约
     * 同时删除消息通知中对应的访客通知
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteVisitor(Long id, Long studentId) {
        log.info("删除访客预约：id={}, studentId={}", id, studentId);
        
        Visitor visitor = visitorMapper.selectById(id);
        if (visitor == null) {
            log.warn("访客记录不存在：id={}", id);
            return Result.error("访客记录不存在");
        }
        
        if (visitor.getDeleted() != null && visitor.getDeleted() == 1) {
            log.warn("访客记录已删除：id={}", id);
            return Result.error("访客记录不存在");
        }
        
        // 学生只能取消自己的预约
        if (studentId != null && visitor.getStudentId() != null && !visitor.getStudentId().equals(studentId)) {
            log.warn("无权操作他人的访客预约：id={}, studentId={}, visitorStudentId={}", id, studentId, visitor.getStudentId());
            return Result.error("无权操作他人的访客预约");
        }
        
        // 只有待审核的预约可以取消（只更新状态，不删除）
        if ("PENDING".equals(visitor.getStatus())) {
            visitor.setStatus("CANCELLED");
            visitorMapper.updateById(visitor);
            // 级联删除消息通知中对应的访客通知
            notificationService.deleteNotificationsByRelatedId("visitor", id);
            log.info("预约已取消：id={}", id);
            return Result.success("预约已取消");
        } else {
            // 其他状态执行软删除（使用 deleteById 会自动处理 @TableLogic）
            int deleteCount = visitorMapper.deleteById(id);
            log.info("访客记录已删除：id={}, deleteCount={}", id, deleteCount);
            if (deleteCount > 0) {
                // 级联删除消息通知中对应的访客通知
                notificationService.deleteNotificationsByRelatedId("visitor", id);
                return Result.success("访客记录已删除");
            } else {
                return Result.error("删除失败");
            }
        }
    }
    
    /**
     * 访客统计
     */
    public Result<VisitorStatisticsVO> getStatistics() {
        log.info("获取访客统计");
        
        VisitorStatisticsVO statistics = new VisitorStatisticsVO();
        
        LambdaQueryWrapper<Visitor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Visitor::getDeleted, 0);
        
        // 各状态数量统计
        wrapper.eq(Visitor::getStatus, "PENDING");
        statistics.setPendingCount(visitorMapper.selectCount(wrapper).intValue());
        
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.eq(Visitor::getStatus, "APPROVED");
        statistics.setApprovedCount(visitorMapper.selectCount(wrapper).intValue());
        
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.eq(Visitor::getStatus, "REJECTED");
        statistics.setRejectedCount(visitorMapper.selectCount(wrapper).intValue());
        
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.eq(Visitor::getStatus, "COMPLETED");
        statistics.setCompletedCount(visitorMapper.selectCount(wrapper).intValue());
        
        // 今日访客数量
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.eq(Visitor::getVisitDate, LocalDate.now());
        statistics.setTodayCount(visitorMapper.selectCount(wrapper).intValue());
        
        // 本周访客数量（简化为最近7天）
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.ge(Visitor::getVisitDate, weekAgo);
        statistics.setWeekCount(visitorMapper.selectCount(wrapper).intValue());
        
        // 本月访客数量
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.ge(Visitor::getVisitDate, monthStart);
        statistics.setMonthCount(visitorMapper.selectCount(wrapper).intValue());
        
        // 当前在访数量（已签到未签退）
        wrapper.clear();
        wrapper.eq(Visitor::getDeleted, 0);
        wrapper.isNotNull(Visitor::getCheckedInAt);
        wrapper.isNull(Visitor::getCheckedOutAt);
        statistics.setCurrentVisitorCount(visitorMapper.selectCount(wrapper).intValue());
        
        return Result.success(statistics);
    }
    
    /**
     * 转换为VisitorVO
     */
    private VisitorVO convertToVO(Visitor visitor) {
        VisitorVO vo = new VisitorVO();
        BeanUtils.copyProperties(visitor, vo);
        
        // 设置状态中文
        vo.setStatusLabel(STATUS_MAP.getOrDefault(visitor.getStatus(), visitor.getStatus()));
        
        // 设置预约时间
        if (visitor.getCreatedAt() != null) {
            vo.setApplyTime(visitor.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        
        // 设置是否可以签到/签退
        vo.setCanCheckIn("APPROVED".equals(visitor.getStatus()) && visitor.getCheckedInAt() == null);
        vo.setCanCheckOut(visitor.getCheckedInAt() != null && visitor.getCheckedOutAt() == null);
        
        // 设置楼栋名称
        if (visitor.getBuildingId() != null) {
            Building building = buildingMapper.selectById(visitor.getBuildingId());
            if (building != null) {
                vo.setBuildingName(building.getBuildingName());
            }
        }
        
        return vo;
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

