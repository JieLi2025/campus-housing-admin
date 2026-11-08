package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.ApplicationDTO;
import com.campus.dorm.dto.ApplicationQueryDTO;
import com.campus.dorm.dto.ApproveApplicationDTO;
import com.campus.dorm.dto.UpdateApplicationDTO;
import com.campus.dorm.dto.StudentUpdateApplicationDTO;
import com.campus.dorm.entity.Application;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.DormGroup;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.entity.Admin;
import com.campus.dorm.mapper.AdminMapper;
import com.campus.dorm.mapper.ApplicationMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.DormGroupMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campus.dorm.vo.ApplicationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 申请Service
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {
    
    private final ApplicationMapper applicationMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final DormRoomMapper dormRoomMapper;
    private final BuildingMapper buildingMapper;
    private final DormGroupMapper dormGroupMapper;
    private final AdminMapper adminMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 类型常量（中文显示）
    public static final String TYPE_TRANSFER = "调宿申请";
    public static final String TYPE_CHECKOUT = "退宿申请";
    public static final String TYPE_MATERIAL_REPORT = "物品申报";
    
    // 类型映射（用于兼容英文和中文）
    private static final Map<String, String> TYPE_MAP = new HashMap<>();
    
    // 状态映射（用于显示中文状态）
    private static final Map<String, String> STATUS_MAP = new HashMap<>();
    
    // 状态常量（中文显示）
    public static final String STATUS_PENDING = "待审核";
    public static final String STATUS_APPROVED = "已通过";
    public static final String STATUS_REJECTED = "已拒绝";
    public static final String STATUS_COMPENSATION_PENDING = "待赔偿处理";
    
    // 可审批状态集合（待审核和待赔偿处理都可以审批）
    private static final java.util.Set<String> APPROVABLE_STATUS_SET = new java.util.HashSet<>();
    
    static {
        // 类型映射（兼容英文和中文）
        TYPE_MAP.put("TRANSFER", TYPE_TRANSFER);
        TYPE_MAP.put("CHECKOUT", TYPE_CHECKOUT);
        TYPE_MAP.put("MATERIAL_REPORT", TYPE_MATERIAL_REPORT);
        TYPE_MAP.put(TYPE_TRANSFER, TYPE_TRANSFER);
        TYPE_MAP.put(TYPE_CHECKOUT, TYPE_CHECKOUT);
        TYPE_MAP.put(TYPE_MATERIAL_REPORT, TYPE_MATERIAL_REPORT);
        
        // 状态映射（兼容英文和中文）
        STATUS_MAP.put("PENDING", "待审核");
        STATUS_MAP.put("APPROVED", "已通过");
        STATUS_MAP.put("REJECTED", "已拒绝");
        STATUS_MAP.put(STATUS_PENDING, "待审核");
        STATUS_MAP.put(STATUS_APPROVED, "已通过");
        STATUS_MAP.put(STATUS_REJECTED, "已拒绝");
        STATUS_MAP.put(STATUS_COMPENSATION_PENDING, "待赔偿处理");
        STATUS_MAP.put("COMPENSATION_PENDING", "待赔偿处理");
        
        // 可审批状态集合（待审核和待赔偿处理）
        APPROVABLE_STATUS_SET.add("PENDING");
        APPROVABLE_STATUS_SET.add(STATUS_PENDING);
        APPROVABLE_STATUS_SET.add("COMPENSATION_PENDING");
        APPROVABLE_STATUS_SET.add(STATUS_COMPENSATION_PENDING);
    }
    
    /**
     * 判断状态是否可审批（待审核或待赔偿处理）
     */
    private boolean isApprovableStatus(String status) {
        return status != null && APPROVABLE_STATUS_SET.contains(status);
    }
    
    /**
     * 判断状态是否为待审核
     */
    private boolean isPendingStatus(String status) {
        return STATUS_PENDING.equals(status) || "PENDING".equalsIgnoreCase(status);
    }
    
    /**
     * 判断是否为调宿申请（兼容英文和中文）
     */
    private boolean isTransferType(String type) {
        return TYPE_TRANSFER.equals(type) || "TRANSFER".equalsIgnoreCase(type);
    }
    
    /**
     * 判断是否为退宿申请（兼容英文和中文）
     */
    private boolean isCheckoutType(String type) {
        return TYPE_CHECKOUT.equals(type) || "CHECKOUT".equalsIgnoreCase(type);
    }
    
    /**
     * 判断是否为物品申报（兼容英文和中文）
     */
    private boolean isMaterialReportType(String type) {
        return TYPE_MATERIAL_REPORT.equals(type) || "MATERIAL_REPORT".equalsIgnoreCase(type);
    }
    
    /**
     * 学生提交申请
     * @param userId 用户ID（sys_user表的id）
     * @param dto 申请数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> submitApplication(Long userId, ApplicationDTO dto) {
        log.info("学生提交申请：userId={}, dto={}", userId, dto);
        
        // 根据userId获取studentId
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(studentWrapper);
        if (student == null) {
            log.warn("提交申请失败：学生不存在，userId={}", userId);
            return Result.error("学生不存在");
        }
        Long studentId = student.getId();
        
        // 创建申请
        Application application = new Application();
        BeanUtils.copyProperties(dto, application);
        
        application.setStudentId(studentId);
        application.setStatus(STATUS_PENDING);
        
        // 设置楼栋ID（用于权限过滤）
        Long buildingId = getStudentBuildingId(studentId);
        application.setBuildingId(buildingId);
        
        // 设置联系电话（如果有）
        if (dto.getContactPhone() != null) {
            application.setContactPhone(dto.getContactPhone());
        }
        
        applicationMapper.insert(application);
        System.out.println("【调试】申请已插入数据库：applicationId=" + application.getId());
        
        // 发送通知给楼栋管理员
        try {
            System.out.println("【调试】准备发送通知...");
            notifyBuildingAdminForApplication(application, student, userId);
            System.out.println("【调试】通知发送完成");
        } catch (Exception e) {
            System.out.println("【调试】发送通知时发生异常：" + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("【调试】申请提交成功：applicationId=" + application.getId());
        return Result.success("申请提交成功");
    }
    
    /**
     * 发送申请通知给楼栋管理员
     */
    private void notifyBuildingAdminForApplication(Application application, Student student, Long userId) {
        try {
            System.out.println("【调试】开始发送申请通知：applicationId=" + application.getId() + 
                             ", studentId=" + student.getId() + ", applicationType=" + application.getType());
            
            // 获取学生所在楼栋ID
            Long buildingId = getStudentBuildingId(student.getId());
            System.out.println("【调试】获取学生楼栋ID：studentId=" + student.getId() + ", buildingId=" + buildingId);
            
            if (buildingId == null) {
                System.out.println("【调试】无法发送申请通知：学生未分配宿舍，studentId=" + student.getId());
                return;
            }
            
            // 创建通知
            com.campus.dorm.dto.NotificationDTO notificationDTO = new com.campus.dorm.dto.NotificationDTO();
            String typeLabel = TYPE_MAP.get(application.getType());
            if (typeLabel == null) {
                typeLabel = application.getType(); // 如果映射不到，直接使用原始类型
            }
            log.info("申请类型映射：原始类型={}, 映射后={}", application.getType(), typeLabel);
            
            notificationDTO.setTitle("新的" + typeLabel + "待审核");
            notificationDTO.setMessage("学生" + student.getRealName() + "提交了" + typeLabel + "，请及时处理");
            notificationDTO.setType("application");
            notificationDTO.setRelatedId(application.getId());
            notificationDTO.setTargetBuildingId(buildingId);
            
            notificationService.createBuildingNotification(notificationDTO, buildingId, userId);
            log.info("申请通知已发送：applicationId={}, buildingId={}", application.getId(), buildingId);
        } catch (Exception e) {
            log.error("发送申请通知失败：applicationId={}", application.getId(), e);
            // 不影响主业务流程
        }
    }
    
    /**
     * 获取学生所在楼栋ID
     * 优先从student表的roomId获取，如果没有则从dorm_group表获取
     */
    private Long getStudentBuildingId(Long studentId) {
        try {
            // 方法1：直接从student表的roomId获取（优先）
            Student student = studentMapper.selectById(studentId);
            if (student != null && student.getRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(student.getRoomId());
                if (room != null && room.getBuildingId() != null) {
                    log.info("通过student.roomId获取楼栋ID：studentId={}, roomId={}, buildingId={}", 
                             studentId, student.getRoomId(), room.getBuildingId());
                    return room.getBuildingId();
                }
            }
            
            // 方法2：从dorm_group表查询学生所在的宿舍组（兼容旧逻辑）
            LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.and(wrapper -> wrapper
                            .like(DormGroup::getMembers, "\"id\":" + studentId)
                            .or()
                            .eq(DormGroup::getLeaderId, studentId))
                        .isNotNull(DormGroup::getSelectedRoomId);
            DormGroup group = dormGroupMapper.selectOne(groupWrapper);
            
            if (group != null && group.getSelectedRoomId() != null) {
                DormRoom room = dormRoomMapper.selectById(group.getSelectedRoomId());
                if (room != null && room.getBuildingId() != null) {
                    log.info("通过dorm_group获取楼栋ID：studentId={}, groupId={}, roomId={}, buildingId={}", 
                             studentId, group.getId(), group.getSelectedRoomId(), room.getBuildingId());
                    return room.getBuildingId();
                }
            }
            
            log.warn("无法获取学生楼栋ID：studentId={}, student.roomId={}", 
                     studentId, student != null ? student.getRoomId() : "null");
        } catch (Exception e) {
            log.error("获取学生楼栋ID异常：studentId={}", studentId, e);
        }
        return null;
    }
    
    /**
     * 获取申请列表（管理员端，支持分页和筛选）
     * @param queryDTO 查询条件
     * @param adminUserId 管理员用户ID（用于权限过滤）
     */
    public Result<IPage<ApplicationVO>> getApplicationList(ApplicationQueryDTO queryDTO, Long adminUserId) {
        log.info("获取申请列表：queryDTO={}, adminUserId={}", queryDTO, adminUserId);
        
        Page<Application> page = new Page<>(queryDTO.getCurrentPage(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getDeleted, 0);
        
        // 按楼栋过滤（管理员只能看到自己管理楼栋的申请）
        List<Long> managedBuildingIds = getAdminManagedBuildingIds(adminUserId);
        if (managedBuildingIds.isEmpty()) {
            log.warn("管理员没有管理任何楼栋：adminUserId={}", adminUserId);
            return Result.success(new Page<>());
        }
        wrapper.in(Application::getBuildingId, managedBuildingIds);
        
        // 关键词搜索（学号或姓名）- 通过关联 Student 表查询
        if (StringUtils.isNotBlank(queryDTO.getKeyword())) {
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.like(Student::getStudentId, queryDTO.getKeyword())
                         .or()
                         .like(Student::getRealName, queryDTO.getKeyword());
            List<Student> matchedStudents = studentMapper.selectList(studentWrapper);
            List<Long> studentIds = matchedStudents.stream()
                    .map(Student::getId)
                    .collect(Collectors.toList());
            if (studentIds.isEmpty()) {
                // 没有匹配的学生，返回空结果
                return Result.success(new Page<>());
            }
            wrapper.in(Application::getStudentId, studentIds);
        }
        
        // 类型筛选
        if (StringUtils.isNotBlank(queryDTO.getType())) {
            wrapper.eq(Application::getType, queryDTO.getType());
        }
        
        // 状态筛选
        if (StringUtils.isNotBlank(queryDTO.getStatus())) {
            wrapper.eq(Application::getStatus, queryDTO.getStatus());
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Application::getCreatedAt);
        
        IPage<Application> result = applicationMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<ApplicationVO> voPage = result.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取管理员管理的楼栋ID列表
     */
    private List<Long> getAdminManagedBuildingIds(Long adminUserId) {
        // 1. 获取用户信息
        SysUser user = sysUserMapper.selectById(adminUserId);
        if (user == null) {
            log.warn("管理员用户不存在：userId={}", adminUserId);
            return List.of();
        }
        
        // 2. 获取管理员信息
        LambdaQueryWrapper<Admin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(Admin::getUserId, adminUserId);
        Admin admin = adminMapper.selectOne(adminWrapper);
        Long adminId = (admin != null) ? admin.getId() : null;
        
        // 3. 查询楼栋：匹配 manager_name 或 admin_id
        LambdaQueryWrapper<Building> buildingWrapper = new LambdaQueryWrapper<>();
        buildingWrapper.and(w -> {
            w.eq(Building::getManagerName, user.getUsername());
            if (adminId != null) {
                w.or().eq(Building::getAdminId, adminId);
            }
        });
        List<Building> buildings = buildingMapper.selectList(buildingWrapper);
        
        log.info("管理员{}（username={}, adminId={}）管理的楼栋数量：{}", 
                adminUserId, user.getUsername(), adminId, buildings.size());
        
        return buildings.stream()
                .map(Building::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取我的申请列表（学生端）
     * @param userId 用户ID（sys_user表的id）
     */
    public Result<IPage<ApplicationVO>> getMyApplications(Long userId, Integer currentPage, Integer pageSize) {
        log.info("获取我的申请列表：userId={}", userId);
        
        // 根据userId获取studentId
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(studentWrapper);
        if (student == null) {
            log.warn("获取申请列表失败：学生不存在，userId={}", userId);
            return Result.success(new Page<>());
        }
        Long studentId = student.getId();
        
        Page<Application> page = new Page<>(currentPage, pageSize);
        
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getStudentId, studentId);
        wrapper.eq(Application::getDeleted, 0);
        wrapper.orderByDesc(Application::getCreatedAt);
        
        IPage<Application> result = applicationMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<ApplicationVO> voPage = result.convert(this::convertToVO);
        
        return Result.success(voPage);
    }
    
    /**
     * 获取申请详情
     */
    public Result<ApplicationVO> getApplicationDetail(Long id) {
        log.info("获取申请详情：id={}", id);
        
        Application application = applicationMapper.selectById(id);
        if (application == null || application.getDeleted() == 1) {
            return Result.error("申请不存在");
        }
        
        ApplicationVO vo = convertToVO(application);
        return Result.success(vo);
    }
    
    /**
     * 审批申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> approveApplication(Long id, Long adminId, ApproveApplicationDTO dto) {
        log.info("审批申请：id={}, adminId={}, dto={}", id, adminId, dto);
        
        Application application = applicationMapper.selectById(id);
        if (application == null || application.getDeleted() == 1) {
            return Result.error("申请不存在");
        }
        
        if (!isApprovableStatus(application.getStatus())) {
            return Result.error("该申请状态不允许审批");
        }
        
        // 更新申请状态
        if (dto.getApproved()) {
            // 通过申请 - 如果有自定义状态则使用自定义状态，否则使用已通过
            if (StringUtils.isNotBlank(dto.getStatus())) {
                application.setStatus(dto.getStatus());
            } else {
                application.setStatus(STATUS_APPROVED);
            }
            application.setAdminComment(dto.getComment());
            
            // 如果是调宿申请，保存分配的房间ID
            if (TYPE_TRANSFER.equals(application.getType()) || "TRANSFER".equalsIgnoreCase(application.getType())) {
                if (dto.getAssignedRoomId() != null) {
                    application.setAssignedRoomId(dto.getAssignedRoomId());
                }
                if (dto.getExchangeStudentId() != null) {
                    application.setExchangeStudentId(dto.getExchangeStudentId());
                }
            }
            
            // 如果是退宿申请且状态为已通过（不是待赔偿处理），执行退宿操作
            if (isCheckoutType(application.getType()) && STATUS_APPROVED.equals(application.getStatus())) {
                releaseStudentDorm(application.getStudentId());
            }
            
            // 如果是调宿申请且状态为已通过，执行实际的宿舍调换
            if (isTransferType(application.getType()) && STATUS_APPROVED.equals(application.getStatus())) {
                executeTransfer(application);
            }
        } else {
            // 拒绝申请
            application.setStatus(STATUS_REJECTED);
            
            if (StringUtils.isBlank(dto.getRejectReason())) {
                return Result.error("拒绝申请必须填写拒绝原因");
            }
            
            application.setRejectReason(dto.getRejectReason());
            application.setAdminComment(dto.getComment());
        }
        
        applicationMapper.updateById(application);
        
        log.info("申请审批成功：id={}, status={}", id, application.getStatus());
        return Result.success(dto.getApproved() ? "申请已通过" : "申请已拒绝");
    }
    
    /**
     * 更新申请信息（管理员更新分配信息）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateApplication(Long id, UpdateApplicationDTO dto) {
        log.info("更新申请信息：id={}, dto={}", id, dto);
        
        Application application = applicationMapper.selectById(id);
        if (application == null || application.getDeleted() == 1) {
            return Result.error("申请不存在");
        }
        
        if (!isPendingStatus(application.getStatus())) {
            return Result.error("只能更新待审核申请的分配信息");
        }
        
        // 只允许更新调宿申请的分配信息
        if (!isTransferType(application.getType())) {
            return Result.error("只能更新调宿申请的分配信息");
        }
        
        // 更新分配信息
        if (dto.getAssignedRoomId() != null) {
            application.setAssignedRoomId(dto.getAssignedRoomId());
        }
        if (dto.getExchangeStudentId() != null) {
            application.setExchangeStudentId(dto.getExchangeStudentId());
        }
        
        applicationMapper.updateById(application);
        
        log.info("申请信息更新成功：id={}", id);
        return Result.success("分配信息已保存");
    }
    
    /**
     * 学生修改申请（仅待审核状态可修改）
     * @param id 申请ID
     * @param userId 用户ID（sys_user表的id）
     * @param dto 申请数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateStudentApplication(Long id, Long userId, StudentUpdateApplicationDTO dto) {
        log.info("学生修改申请：id={}, userId={}, dto={}", id, userId, dto);
        
        // 根据userId获取studentId
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(studentWrapper);
        if (student == null) {
            log.warn("修改申请失败：学生不存在，userId={}", userId);
            return Result.error("学生不存在");
        }
        Long studentId = student.getId();
        
        Application application = applicationMapper.selectById(id);
        if (application == null || application.getDeleted() == 1) {
            return Result.error("申请不存在");
        }
        
        if (!application.getStudentId().equals(studentId)) {
            return Result.error("无权修改该申请");
        }
        
        // 状态判断：只能修改待审核的申请（兼容英文和中文状态值）
        String status = application.getStatus();
        if (!isPendingStatus(status)) {
            return Result.error("只能修改待审核的申请");
        }
        
        // 更新申请信息
        if (StringUtils.isNotBlank(dto.getReason())) {
            application.setReason(dto.getReason());
        }
        if (StringUtils.isNotBlank(dto.getTargetDormInfo())) {
            application.setTargetDormInfo(dto.getTargetDormInfo());
        }
        if (StringUtils.isNotBlank(dto.getContactPhone())) {
            application.setContactPhone(dto.getContactPhone());
        }
        
        applicationMapper.updateById(application);
        
        log.info("申请修改成功：id={}", id);
        return Result.success("申请修改成功");
    }
    
    /**
     * 撤回申请（学生端）
     * @param id 申请ID
     * @param userId 用户ID（sys_user表的id）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> withdrawApplication(Long id, Long userId) {
        log.info("撤回申请：id={}, userId={}", id, userId);
        
        // 根据userId获取studentId
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getUserId, userId);
        Student student = studentMapper.selectOne(studentWrapper);
        if (student == null) {
            log.warn("撤回申请失败：学生不存在，userId={}", userId);
            return Result.error("学生不存在");
        }
        Long studentId = student.getId();
        log.info("撤回申请：找到学生，studentId={}, realName={}", studentId, student.getRealName());
        
        Application application = applicationMapper.selectById(id);
        if (application == null || application.getDeleted() == 1) {
            log.warn("撤回申请失败：申请不存在或已删除，id={}", id);
            return Result.error("申请不存在");
        }
        
        log.info("撤回申请：申请信息，applicationStudentId={}, status={}, type={}", 
                 application.getStudentId(), application.getStatus(), application.getType());
        
        if (!application.getStudentId().equals(studentId)) {
            log.warn("撤回申请失败：无权撤回，申请studentId={}, 当前studentId={}", 
                     application.getStudentId(), studentId);
            return Result.error("无权撤回该申请");
        }
        
        // 状态判断：兼容英文和中文状态值
        String status = application.getStatus();
        if (!isPendingStatus(status)) {
            log.warn("撤回申请失败：状态不是待审核，当前状态={}", status);
            return Result.error("只能撤回待审核的申请");
        }
        
        // 物理删除 - 真正从数据库删除记录
        int rows = applicationMapper.physicalDeleteById(id);
        log.info("申请撤回执行结果：id={}, affectedRows={}", id, rows);
        
        if (rows > 0) {
            log.info("申请撤回成功（已从数据库删除）：id={}", id);
            return Result.success("申请已撤回");
        } else {
            log.warn("申请撤回失败：删除行数为0，id={}", id);
            return Result.error("撤回失败，请重试");
        }
    }
    
    /**
     * 删除申请（管理员端）
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteApplication(Long id) {
        log.info("删除申请：id={}", id);
        
        Application application = applicationMapper.selectById(id);
        if (application == null || application.getDeleted() == 1) {
            return Result.error("申请不存在");
        }
        
        // 软删除
        application.setDeleted(1);
        applicationMapper.updateById(application);
        
        log.info("申请删除成功：id={}", id);
        return Result.success("申请已删除");
    }
    
    /**
     * 获取待审核申请数量（按管理员楼栋过滤）
     */
    public Result<Long> getPendingCount(Long adminUserId) {
        // 获取管理员管理的楼栋
        List<Long> managedBuildingIds = getAdminManagedBuildingIds(adminUserId);
        if (managedBuildingIds.isEmpty()) {
            return Result.success(0L);
        }
        
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Application::getBuildingId, managedBuildingIds);
        // 兼容英文和中文状态值
        wrapper.and(w -> w.eq(Application::getStatus, "PENDING")
                         .or()
                         .eq(Application::getStatus, STATUS_PENDING));
        wrapper.eq(Application::getDeleted, 0);
        
        Long count = applicationMapper.selectCount(wrapper);
        return Result.success(count);
    }
    
    /**
     * 转换为VO
     */
    private ApplicationVO convertToVO(Application application) {
        ApplicationVO vo = new ApplicationVO();
        BeanUtils.copyProperties(application, vo);
        
        // 设置中文标签
        vo.setTypeLabel(TYPE_MAP.get(application.getType()));
        vo.setStatusLabel(STATUS_MAP.get(application.getStatus()));
        
        // 设置申请时间（yyyy-MM-dd格式）
        if (application.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            vo.setApplyTime(application.getCreatedAt().format(formatter));
        }
        
        // 从 Student 表查询学生信息
        if (application.getStudentId() != null) {
            Student student = studentMapper.selectById(application.getStudentId());
            if (student != null) {
                vo.setStudentName(student.getRealName());
                vo.setStudentNumber(student.getStudentId());
                
                // 从学生的 roomId 获取当前宿舍信息
                if (student.getRoomId() != null) {
                    DormRoom room = dormRoomMapper.selectById(student.getRoomId());
                    if (room != null) {
                        Building building = buildingMapper.selectById(room.getBuildingId());
                        String dormInfo = (building != null ? building.getBuildingName() : "") + "-" + room.getRoomNumber();
                        vo.setCurrentDormInfo(dormInfo);
                    }
                }
            }
        }
        
        // 从 assignedRoomId 获取分配的宿舍信息
        if (application.getAssignedRoomId() != null) {
            DormRoom room = dormRoomMapper.selectById(application.getAssignedRoomId());
            if (room != null) {
                Building building = buildingMapper.selectById(room.getBuildingId());
                String dormInfo = (building != null ? building.getBuildingName() : "") + "-" + room.getRoomNumber();
                vo.setAssignedDormInfo(dormInfo);
            }
        }
        
        // 从 exchangeStudentId 获取调换对象信息
        if (application.getExchangeStudentId() != null) {
            Student exchangeStudent = studentMapper.selectById(application.getExchangeStudentId());
            if (exchangeStudent != null) {
                vo.setExchangeStudentName(exchangeStudent.getRealName());
                vo.setExchangeStudentNumber(exchangeStudent.getStudentId());
                // 获取调换对象的宿舍信息
                if (exchangeStudent.getRoomId() != null) {
                    DormRoom room = dormRoomMapper.selectById(exchangeStudent.getRoomId());
                    if (room != null) {
                        Building building = buildingMapper.selectById(room.getBuildingId());
                        String dormInfo = (building != null ? building.getBuildingName() : "") + "-" + room.getRoomNumber();
                        vo.setExchangeDormInfo(dormInfo);
                    }
                }
            }
        }
        
        return vo;
    }
    
    /**
     * 执行调宿操作
     * @param application 申请记录
     */
    private void executeTransfer(Application application) {
        log.info("执行调宿操作：applicationId={}, studentId={}, assignedRoomId={}", 
                 application.getId(), application.getStudentId(), application.getAssignedRoomId());
        
        Long studentId = application.getStudentId();
        Long newRoomId = application.getAssignedRoomId();
        
        if (newRoomId == null) {
            log.warn("调宿申请没有分配新房间：applicationId={}", application.getId());
            return;
        }
        
        try {
            // 1. 查找学生当前所在的宿舍组
            LambdaQueryWrapper<DormGroup> oldGroupWrapper = new LambdaQueryWrapper<>();
            oldGroupWrapper.like(DormGroup::getMembers, "\"id\":" + studentId)
                          .or()
                          .eq(DormGroup::getLeaderId, studentId);
            List<DormGroup> oldGroups = dormGroupMapper.selectList(oldGroupWrapper);
            
            Long oldRoomId = null;
            
            // 2. 从旧宿舍组移除学生
            for (DormGroup oldGroup : oldGroups) {
                if (oldGroup.getSelectedRoomId() != null && !oldGroup.getSelectedRoomId().equals(newRoomId)) {
                    oldRoomId = oldGroup.getSelectedRoomId();
                    removeStudentFromGroup(studentId, oldGroup);
                    log.info("从旧宿舍组移除学生：groupId={}, studentId={}", oldGroup.getId(), studentId);
                    
                    // 更新旧房间的可用床位
                    DormRoom oldRoom = dormRoomMapper.selectById(oldGroup.getSelectedRoomId());
                    if (oldRoom != null) {
                        oldRoom.setAvailableBeds(Math.min(oldRoom.getAvailableBeds() + 1, oldRoom.getBedCount()));
                        dormRoomMapper.updateById(oldRoom);
                        log.info("旧房间可用床位已更新：roomId={}, availableBeds={}", oldRoom.getId(), oldRoom.getAvailableBeds());
                    }
                }
            }
            
            // 3. 获取新房间信息
            DormRoom newRoom = dormRoomMapper.selectById(newRoomId);
            if (newRoom == null) {
                log.error("新房间不存在：roomId={}", newRoomId);
                return;
            }
            
            // 4. 检查新房间是否有可用床位
            if (newRoom.getAvailableBeds() == null || newRoom.getAvailableBeds() <= 0) {
                log.error("新房间没有可用床位：roomId={}, availableBeds={}", newRoomId, newRoom.getAvailableBeds());
                return;
            }
            
            // 5. 查找新房间的宿舍组
            LambdaQueryWrapper<DormGroup> newGroupWrapper = new LambdaQueryWrapper<>();
            newGroupWrapper.eq(DormGroup::getSelectedRoomId, newRoomId);
            DormGroup newGroup = dormGroupMapper.selectOne(newGroupWrapper);
            
            // 6. 获取学生信息
            Student student = studentMapper.selectById(studentId);
            if (student == null) {
                log.error("学生不存在：studentId={}", studentId);
                return;
            }
            
            // 7. 如果新房间没有宿舍组，创建一个
            if (newGroup == null) {
                newGroup = new DormGroup();
                newGroup.setGroupName(student.getRealName() + "的宿舍");
                newGroup.setLeaderId(studentId);
                newGroup.setCurrentMembers(0);
                newGroup.setMaxMembers(newRoom.getBedCount());
                newGroup.setMajor(student.getMajor());
                newGroup.setClassName(student.getClassName());
                newGroup.setGrade(student.getGrade());
                newGroup.setGender(student.getGender());
                newGroup.setSelectedRoomId(newRoomId);
                newGroup.setStatus("forming");
                newGroup.setMembers("[]");
                dormGroupMapper.insert(newGroup);
                log.info("为新房间创建宿舍组：groupId={}, roomId={}", newGroup.getId(), newRoomId);
            }
            
            // 8. 将学生添加到新宿舍组
            addStudentToGroup(student, newGroup);
            log.info("学生已添加到新宿舍组：groupId={}, studentId={}", newGroup.getId(), studentId);
            
            // 9. 更新新房间的可用床位
            newRoom.setAvailableBeds(newRoom.getAvailableBeds() - 1);
            dormRoomMapper.updateById(newRoom);
            log.info("新房间可用床位已更新：roomId={}, availableBeds={}", newRoomId, newRoom.getAvailableBeds());
            
            log.info("调宿操作完成：studentId={}, oldRoomId={}, newRoomId={}", studentId, oldRoomId, newRoomId);
        } catch (Exception e) {
            log.error("执行调宿操作失败：applicationId={}", application.getId(), e);
            throw new RuntimeException("调宿操作失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从宿舍组移除学生
     */
    private void removeStudentFromGroup(Long studentId, DormGroup group) {
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            members.removeIf(m -> {
                Object idObj = m.get("id");
                if (idObj != null) {
                    Long memberId = Long.valueOf(idObj.toString());
                    return memberId.equals(studentId);
                }
                return false;
            });
            
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            
            if (members.isEmpty()) {
                // 宿舍组为空，清除房间关联
                group.setSelectedRoomId(null);
                group.setStatus("forming");
                log.info("宿舍组已清空，清除房间关联：groupId={}", group.getId());
            } else if (group.getCurrentMembers() < group.getMaxMembers()) {
                // 宿舍组未满
                group.setStatus("forming");
            }
            
            dormGroupMapper.updateById(group);
            log.info("从宿舍组移除学生成功：groupId={}, studentId={}, 剩余成员数={}", 
                group.getId(), studentId, members.size());
        } catch (Exception e) {
            log.error("从宿舍组移除学生失败：groupId={}, studentId={}", group.getId(), studentId, e);
            throw new RuntimeException("从宿舍组移除学生失败", e);
        }
    }
    
    /**
     * 将学生添加到宿舍组
     */
    private void addStudentToGroup(Student student, DormGroup group) {
        try {
            List<Map<String, Object>> members = objectMapper.readValue(
                group.getMembers(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 检查是否已存在
            boolean exists = members.stream()
                .anyMatch(m -> {
                    Object idObj = m.get("id");
                    if (idObj != null) {
                        Long memberId = Long.valueOf(idObj.toString());
                        return memberId.equals(student.getId());
                    }
                    return false;
                });
            
            if (!exists) {
                Map<String, Object> newMember = new HashMap<>();
                newMember.put("id", student.getId());
                newMember.put("name", student.getRealName());
                newMember.put("studentId", student.getStudentId());
                newMember.put("major", student.getMajor());
                newMember.put("className", student.getClassName());
                newMember.put("isLeader", false);
                // 从sys_user表获取头像
                String avatar = "";
                if (student.getUserId() != null) {
                    SysUser user = sysUserMapper.selectById(student.getUserId());
                    avatar = user != null && user.getAvatar() != null ? user.getAvatar() : "";
                }
                newMember.put("avatar", avatar);
                
                members.add(newMember);
                
                group.setMembers(objectMapper.writeValueAsString(members));
                group.setCurrentMembers(members.size());
                
                if (group.getCurrentMembers() >= group.getMaxMembers()) {
                    group.setStatus("full");
                } else {
                    group.setStatus("forming");
                }
                
                dormGroupMapper.updateById(group);
                log.info("添加学生到宿舍组成功：groupId={}, studentId={}, 当前成员数={}", 
                    group.getId(), student.getId(), members.size());
            } else {
                log.info("学生已在宿舍组中：groupId={}, studentId={}", group.getId(), student.getId());
            }
        } catch (Exception e) {
            log.error("添加学生到宿舍组失败：groupId={}, studentId={}", group.getId(), student.getId(), e);
            throw new RuntimeException("添加学生到宿舍组失败", e);
        }
    }
    
    /**
     * 释放学生宿舍（退宿时调用）
     * @param studentId 学生ID（student表的id）
     */
    private void releaseStudentDorm(Long studentId) {
        log.info("释放学生宿舍：studentId={}", studentId);
        
        try {
            // 1. 从dorm_group表查找学生所在的宿舍组
            LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
            groupWrapper.and(wrapper -> wrapper
                            .like(DormGroup::getMembers, "\"id\":" + studentId)
                            .or()
                            .eq(DormGroup::getLeaderId, studentId))
                        .isNotNull(DormGroup::getSelectedRoomId);
            List<DormGroup> groups = dormGroupMapper.selectList(groupWrapper);
            
            if (groups.isEmpty()) {
                log.warn("学生没有有效的宿舍分配记录：studentId={}", studentId);
                return;
            }
            
            for (DormGroup group : groups) {
                Long roomId = group.getSelectedRoomId();
                
                // 2. 更新房间的可用床位数（availableBeds + 1）
                if (roomId != null) {
                    DormRoom room = dormRoomMapper.selectById(roomId);
                    if (room != null && room.getAvailableBeds() != null && room.getBedCount() != null) {
                        // 可用床位数增加1，但不能超过总床位数
                        int newAvailableBeds = Math.min(room.getAvailableBeds() + 1, room.getBedCount());
                        room.setAvailableBeds(newAvailableBeds);
                        dormRoomMapper.updateById(room);
                        log.info("房间可用床位已更新：roomId={}, availableBeds={}", roomId, newAvailableBeds);
                    }
                }
                
                // 3. 从宿舍组中移除学生
                removeStudentFromGroup(studentId, group);
                
                log.info("学生宿舍释放成功：studentId={}, roomId={}", studentId, roomId);
            }
        } catch (Exception e) {
            log.error("释放学生宿舍失败：studentId={}", studentId, e);
            // 不抛出异常，避免影响主流程
        }
    }
    
    /**
     * 从宿舍组中移除学生
     * @param studentId 学生ID
     */
    private void removeStudentFromDormGroup(Long studentId) {
        log.info("从宿舍组中移除学生：studentId={}", studentId);
        
        try {
            // 1. 查找学生所在的宿舍组（作为组长）
            LambdaQueryWrapper<DormGroup> leaderWrapper = new LambdaQueryWrapper<>();
            leaderWrapper.eq(DormGroup::getLeaderId, studentId);
            DormGroup groupAsLeader = dormGroupMapper.selectOne(leaderWrapper);
            
            if (groupAsLeader != null) {
                // 学生是组长，需要特殊处理
                handleLeaderCheckout(groupAsLeader, studentId);
                return;
            }
            
            // 2. 查找学生所在的宿舍组（作为成员）
            LambdaQueryWrapper<DormGroup> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.like(DormGroup::getMembers, "\"id\":" + studentId);
            DormGroup groupAsMember = dormGroupMapper.selectOne(memberWrapper);
            
            if (groupAsMember != null) {
                // 从成员列表中移除
                removeFromMembersList(groupAsMember, studentId);
            } else {
                log.info("学生不在任何宿舍组中：studentId={}", studentId);
            }
        } catch (Exception e) {
            log.error("从宿舍组移除学生失败：studentId={}", studentId, e);
        }
    }
    
    /**
     * 处理组长退宿
     */
    private void handleLeaderCheckout(DormGroup group, Long studentId) {
        log.info("处理组长退宿：groupId={}, studentId={}", group.getId(), studentId);
        
        try {
            String membersJson = group.getMembers();
            if (membersJson == null || membersJson.trim().isEmpty()) {
                // 没有成员，直接解散宿舍组
                group.setStatus("disbanded");
                dormGroupMapper.updateById(group);
                log.info("宿舍组已解散（无其他成员）：groupId={}", group.getId());
                return;
            }
            
            List<Map<String, Object>> members = objectMapper.readValue(
                membersJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 移除退宿的组长
            members.removeIf(m -> studentId.equals(((Number) m.get("id")).longValue()));
            
            if (members.isEmpty()) {
                // 没有其他成员，解散宿舍组
                group.setStatus("disbanded");
                group.setMembers("[]");
                group.setCurrentMembers(0);
                dormGroupMapper.updateById(group);
                log.info("宿舍组已解散（组长退宿后无其他成员）：groupId={}", group.getId());
            } else {
                // 将第一个成员设为新组长
                Map<String, Object> newLeader = members.get(0);
                Long newLeaderId = ((Number) newLeader.get("id")).longValue();
                newLeader.put("isLeader", true);
                
                group.setLeaderId(newLeaderId);
                group.setMembers(objectMapper.writeValueAsString(members));
                group.setCurrentMembers(members.size());
                dormGroupMapper.updateById(group);
                
                log.info("宿舍组组长已转让：groupId={}, newLeaderId={}", group.getId(), newLeaderId);
            }
        } catch (Exception e) {
            log.error("处理组长退宿失败：groupId={}", group.getId(), e);
        }
    }
    
    /**
     * 从成员列表中移除学生
     */
    private void removeFromMembersList(DormGroup group, Long studentId) {
        log.info("从成员列表中移除学生：groupId={}, studentId={}", group.getId(), studentId);
        
        try {
            String membersJson = group.getMembers();
            if (membersJson == null || membersJson.trim().isEmpty()) {
                return;
            }
            
            List<Map<String, Object>> members = objectMapper.readValue(
                membersJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // 移除该学生
            members.removeIf(m -> studentId.equals(((Number) m.get("id")).longValue()));
            
            // 更新宿舍组
            group.setMembers(objectMapper.writeValueAsString(members));
            group.setCurrentMembers(members.size());
            dormGroupMapper.updateById(group);
            
            log.info("学生已从宿舍组移除：groupId={}, studentId={}, remainingMembers={}", 
                     group.getId(), studentId, members.size());
        } catch (Exception e) {
            log.error("从成员列表移除学生失败：groupId={}, studentId={}", group.getId(), studentId, e);
        }
    }
}

