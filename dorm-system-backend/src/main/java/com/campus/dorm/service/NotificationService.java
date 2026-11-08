package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.dto.NotificationDTO;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.Notification;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.NotificationMapper;
import com.campus.dorm.vo.NotificationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知服务
 */
@Slf4j
@Service
public class NotificationService {
    
    @Resource
    private NotificationMapper notificationMapper;
    
    @Resource
    private BuildingMapper buildingMapper;
    
    /**
     * 获取当前用户的通知列表（分页）
     */
    public IPage<NotificationVO> getNotificationList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .isNull(Notification::getUserId));
        wrapper.orderByDesc(Notification::getCreatedAt);
        
        IPage<Notification> notificationPage = notificationMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<NotificationVO> result = notificationPage.convert(this::convertToVO);
        return result;
    }
    
    /**
     * 获取管理员的通知列表（带类型过滤）
     * @param userId 管理员用户ID
     * @param type 通知类型（可选）：system/application/visitor
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    public IPage<NotificationVO> getAdminNotificationList(Long userId, String type, Integer pageNum, Integer pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        
        // 通知是发给该用户的，或者是广播通知（userId为NULL）
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .isNull(Notification::getUserId));
        
        // 类型过滤
        if (StringUtils.hasText(type)) {
            wrapper.eq(Notification::getType, type);
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(Notification::getCreatedAt);
        
        IPage<Notification> notificationPage = notificationMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<NotificationVO> result = notificationPage.convert(this::convertToAdminVO);
        return result;
    }
    
    /**
     * 获取系统通知列表（超级管理员发送的通知）
     * 用于超级管理员的通知管理功能
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    public IPage<NotificationVO> getSystemNotificationList(Integer pageNum, Integer pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询系统通知（type='system'）
        wrapper.eq(Notification::getType, "system");
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(Notification::getCreatedAt);
        
        IPage<Notification> notificationPage = notificationMapper.selectPage(page, wrapper);
        
        // 转换为VO
        IPage<NotificationVO> result = notificationPage.convert(this::convertToAdminVO);
        return result;
    }
    
    /**
     * 转换为管理员VO（带跳转信息）
     */
    private NotificationVO convertToAdminVO(Notification notification) {
        NotificationVO vo = convertToVO(notification);
        
        // 设置关联ID
        vo.setRelatedId(notification.getRelatedId());
        
        // 设置跳转信息
        NotificationVO.JumpInfo jumpInfo = new NotificationVO.JumpInfo();
        String type = notification.getType();
        if ("application".equals(type)) {
            jumpInfo.setRoute("/admin/application-review");
            jumpInfo.setType("application");
            jumpInfo.setRelatedId(notification.getRelatedId());
            vo.setJumpTo(jumpInfo);
        } else if ("visitor".equals(type)) {
            jumpInfo.setRoute("/admin/visitor-management");
            jumpInfo.setType("visitor");
            jumpInfo.setRelatedId(notification.getRelatedId());
            vo.setJumpTo(jumpInfo);
        }
        
        return vo;
    }
    
    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .isNull(Notification::getUserId));
        wrapper.eq(Notification::getIsRead, false);
        
        return notificationMapper.selectCount(wrapper);
    }
    
    @Resource
    private com.campus.dorm.mapper.SysUserMapper sysUserMapper;
    
    @Resource
    private com.campus.dorm.mapper.GroupChatMemberMapper groupChatMemberMapper;
    
    @Resource
    private com.campus.dorm.mapper.ConversationMapper conversationMapper;
    
    /**
     * 创建通知
     * 系统通知（type=system）：
     *   - 指定targetUserId：发给指定用户
     *   - 指定targetRole=admin：发给所有管理员
     *   - 其他：广播给所有人（user_id=NULL）
     * 其他通知：为每个目标用户创建一条记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(NotificationDTO dto, Long createdBy) {
        // 系统通知的处理
        if ("system".equals(dto.getType())) {
            // 如果指定了目标用户，只发给该用户
            if (dto.getTargetUserId() != null) {
                createSingleNotification(dto, dto.getTargetUserId());
                log.info("创建系统通知成功（指定用户）: {}, 用户ID: {}", dto.getTitle(), dto.getTargetUserId());
                return;
            }
            
            // 如果指定了目标角色为admin，发给所有管理员
            if ("admin".equals(dto.getTargetRole())) {
                LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysUser::getUserType, "admin");
                List<SysUser> admins = sysUserMapper.selectList(wrapper);
                for (SysUser admin : admins) {
                    createSingleNotification(dto, admin.getId());
                }
                log.info("创建系统通知成功（所有管理员）: {}, 管理员数: {}", dto.getTitle(), admins.size());
                return;
            }
            
            // 其他情况：广播给所有人
            createBroadcastNotification(dto);
            log.info("创建系统广播通知成功: {}", dto.getTitle());
            return;
        }
        
        // 如果指定了目标用户，只发给该用户
        if (dto.getTargetUserId() != null) {
            createSingleNotification(dto, dto.getTargetUserId());
            return;
        }
        
        // 如果指定了目标楼栋，只发给该楼栋的学生
        if (dto.getTargetBuildingId() != null) {
            java.util.List<Long> buildingUserIds = getBuildingStudentUserIds(dto.getTargetBuildingId());
            for (Long userId : buildingUserIds) {
                createSingleNotification(dto, userId);
            }
            log.info("创建楼栋公告通知成功: {}, 楼栋ID: {}, 用户数: {}", dto.getTitle(), dto.getTargetBuildingId(), buildingUserIds.size());
            return;
        }
        
        // 如果指定了目标角色，发给该角色的所有用户
        if (dto.getTargetRole() != null && !dto.getTargetRole().isEmpty()) {
            String targetRole = dto.getTargetRole().toLowerCase();
            
            // "all" 表示发给所有用户
            if ("all".equals(targetRole)) {
                java.util.List<com.campus.dorm.entity.SysUser> allUsers = sysUserMapper.selectList(null);
                for (com.campus.dorm.entity.SysUser user : allUsers) {
                    createSingleNotification(dto, user.getId());
                }
                log.info("创建全体通知成功: {}, 用户数: {}", dto.getTitle(), allUsers.size());
                return;
            }
            
            // 转换角色名称
            String userType;
            switch (targetRole) {
                case "student":
                    userType = "student";
                    break;
                case "admin":
                    userType = "admin";
                    break;
                case "super_admin":
                case "superadmin":
                    userType = "superadmin";
                    break;
                default:
                    userType = targetRole;
            }
            
            // 查询该角色的所有用户
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.dorm.entity.SysUser> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(com.campus.dorm.entity.SysUser::getUserType, userType);
            java.util.List<com.campus.dorm.entity.SysUser> users = sysUserMapper.selectList(wrapper);
            
            for (com.campus.dorm.entity.SysUser user : users) {
                createSingleNotification(dto, user.getId());
            }
            log.info("创建群发通知成功: {}, 目标角色: {}, 用户数: {}", dto.getTitle(), targetRole, users.size());
            return;
        }
        
        // 否则发给所有用户
        java.util.List<com.campus.dorm.entity.SysUser> allUsers = sysUserMapper.selectList(null);
        for (com.campus.dorm.entity.SysUser user : allUsers) {
            createSingleNotification(dto, user.getId());
        }
        log.info("创建全体通知成功: {}, 用户数: {}", dto.getTitle(), allUsers.size());
    }
    
    @Resource
    private com.campus.dorm.mapper.DormRoomMapper dormRoomMapper;
    
    @Resource
    private com.campus.dorm.mapper.StudentMapper studentMapper;
    
    /**
     * 获取楼栋内所有学生的用户ID
     * 通过 DormRoom 表查找该楼栋的所有房间，再通过 Student 表查找住在这些房间的学生
     */
    private java.util.List<Long> getBuildingStudentUserIds(Long buildingId) {
        java.util.List<Long> userIds = new java.util.ArrayList<>();
        
        try {
            // 查找该楼栋的所有房间
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.dorm.entity.DormRoom> roomWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            roomWrapper.eq(com.campus.dorm.entity.DormRoom::getBuildingId, buildingId);
            java.util.List<com.campus.dorm.entity.DormRoom> rooms = dormRoomMapper.selectList(roomWrapper);
            
            if (rooms.isEmpty()) {
                log.warn("楼栋没有房间：buildingId={}", buildingId);
                return userIds;
            }
            
            // 获取所有房间ID
            java.util.List<Long> roomIds = rooms.stream()
                .map(com.campus.dorm.entity.DormRoom::getId)
                .collect(java.util.stream.Collectors.toList());
            
            // 查找住在这些房间的学生
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.dorm.entity.Student> studentWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            studentWrapper.in(com.campus.dorm.entity.Student::getRoomId, roomIds)
                         .isNotNull(com.campus.dorm.entity.Student::getUserId);
            java.util.List<com.campus.dorm.entity.Student> students = studentMapper.selectList(studentWrapper);
            
            // 收集学生的用户ID
            for (com.campus.dorm.entity.Student student : students) {
                if (student.getUserId() != null) {
                    userIds.add(student.getUserId());
                }
            }
            
            log.info("查找楼栋学生：buildingId={}, 房间数={}, 学生数={}", buildingId, rooms.size(), userIds.size());
        } catch (Exception e) {
            log.error("查找楼栋学生失败：buildingId={}", buildingId, e);
        }
        
        return userIds;
    }
    
    /**
     * 创建单条通知
     */
    private void createSingleNotification(NotificationDTO dto, Long userId) {
        Notification notification = new Notification();
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getMessage());
        notification.setType(dto.getType());
        notification.setUserId(userId);
        notification.setIsRead(false);
        notification.setRelatedId(dto.getRelatedId());
        
        notificationMapper.insert(notification);
    }
    
    /**
     * 创建广播通知（user_id = NULL，所有人可见）
     * 用于系统通知，只创建一条记录
     */
    private void createBroadcastNotification(NotificationDTO dto) {
        Notification notification = new Notification();
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getMessage());
        notification.setType(dto.getType());
        notification.setUserId(null);  // NULL表示广播，所有人可见
        notification.setIsRead(false);
        notification.setRelatedId(dto.getRelatedId());
        
        notificationMapper.insert(notification);
    }
    
    /**
     * 创建楼栋级别通知（发送给楼栋管理员）
     * @param dto 通知数据
     * @param buildingId 楼栋ID
     * @param senderId 发送者ID（未使用，保留接口兼容）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createBuildingNotification(NotificationDTO dto, Long buildingId, Long senderId) {
        System.out.println("【通知调试】createBuildingNotification开始：buildingId=" + buildingId + ", title=" + dto.getTitle());
        try {
            // 验证楼栋存在
            Building building = buildingMapper.selectById(buildingId);
            System.out.println("【通知调试】查询楼栋结果：building=" + (building != null ? building.getBuildingName() + ", adminId=" + building.getAdminId() : "null"));
            if (building == null) {
                log.warn("创建通知失败：楼栋不存在，buildingId={}", buildingId);
                return;
            }
            
            // 获取楼栋管理员
            List<Long> adminUserIds = getAdminUserIdsByBuilding(buildingId);
            System.out.println("【通知调试】获取管理员ID列表：adminUserIds=" + adminUserIds);
            if (adminUserIds.isEmpty()) {
                log.warn("创建通知失败：楼栋无管理员，buildingId={}", buildingId);
                System.out.println("【通知调试】楼栋无管理员，无法发送通知！buildingId=" + buildingId);
                return;
            }
            
            // 为每个管理员创建通知
            for (Long adminUserId : adminUserIds) {
                Notification notification = new Notification();
                notification.setTitle(dto.getTitle());
                notification.setContent(dto.getMessage());
                notification.setType(dto.getType());
                notification.setUserId(adminUserId);
                notification.setRelatedId(dto.getRelatedId());
                notification.setIsRead(false);
                
                notificationMapper.insert(notification);
                System.out.println("【通知调试】通知已插入数据库：notificationId=" + notification.getId() + ", userId=" + adminUserId);
            }
            
            log.info("创建楼栋通知成功: title={}, buildingId={}, 管理员数={}", 
                    dto.getTitle(), buildingId, adminUserIds.size());
            System.out.println("【通知调试】createBuildingNotification完成：成功创建" + adminUserIds.size() + "条通知");
        } catch (Exception e) {
            log.error("创建楼栋通知异常：buildingId={}", buildingId, e);
            System.out.println("【通知调试】createBuildingNotification异常：" + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免影响主业务流程
        }
    }
    
    @Resource
    private com.campus.dorm.mapper.AdminMapper adminMapper;
    
    /**
     * 获取楼栋管理员的用户ID列表
     * 注意：building.admin_id 关联的是 admin 表的 id，需要通过 admin.user_id 获取 sys_user.id
     */
    private List<Long> getAdminUserIdsByBuilding(Long buildingId) {
        System.out.println("【管理员查找】开始查找楼栋管理员：buildingId=" + buildingId);
        List<Long> adminUserIds = new ArrayList<>();
        
        Building building = buildingMapper.selectById(buildingId);
        System.out.println("【管理员查找】楼栋信息：" + (building != null ? 
            "name=" + building.getBuildingName() + ", adminId=" + building.getAdminId() + ", managerName=" + building.getManagerName() 
            : "null"));
        
        if (building == null) {
            log.warn("楼栋不存在：buildingId={}", buildingId);
            return adminUserIds;
        }
        
        // 优先使用 adminId 字段（关联 admin 表的 id）
        if (building.getAdminId() != null) {
            System.out.println("【管理员查找】使用adminId查找：adminId=" + building.getAdminId());
            
            // building.adminId 是 admin 表的 id，需要查询 admin 表获取 user_id
            com.campus.dorm.entity.Admin admin = adminMapper.selectById(building.getAdminId());
            System.out.println("【管理员查找】admin表查询结果：" + (admin != null ? "userId=" + admin.getUserId() : "null"));
            
            if (admin != null && admin.getUserId() != null) {
                // 验证对应的 sys_user 是否有效
                SysUser sysUser = sysUserMapper.selectById(admin.getUserId());
                System.out.println("【管理员查找】sys_user查询结果：" + (sysUser != null ? 
                    "id=" + sysUser.getId() + ", username=" + sysUser.getUsername() + ", userType=" + sysUser.getUserType() + ", status=" + sysUser.getStatus()
                    : "null"));
                
                if (sysUser != null && "admin".equals(sysUser.getUserType()) && sysUser.getStatus() == 1) {
                    adminUserIds.add(sysUser.getId());
                    System.out.println("【管理员查找】成功找到管理员：userId=" + sysUser.getId());
                } else {
                    System.out.println("【管理员查找】sys_user不符合条件（不是admin或状态不为1）");
                }
            } else {
                System.out.println("【管理员查找】admin表中找不到记录或userId为空");
            }
        } else {
            System.out.println("【管理员查找】building.adminId为空");
        }
        
        // 如果通过adminId没找到，尝试通过managerName查找（兼容旧数据）
        if (adminUserIds.isEmpty() && building.getManagerName() != null) {
            System.out.println("【管理员查找】尝试通过managerName查找：managerName=" + building.getManagerName());
            
            // 先尝试通过真实姓名(realName)匹配
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getRealName, building.getManagerName())
                   .eq(SysUser::getUserType, "admin")
                   .eq(SysUser::getStatus, 1);
            
            List<SysUser> admins = sysUserMapper.selectList(wrapper);
            System.out.println("【管理员查找】通过realName查找结果：" + admins.size() + "条");
            
            // 如果通过realName没找到，再尝试通过username匹配
            if (admins.isEmpty()) {
                wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysUser::getUsername, building.getManagerName())
                       .eq(SysUser::getUserType, "admin")
                       .eq(SysUser::getStatus, 1);
                admins = sysUserMapper.selectList(wrapper);
                System.out.println("【管理员查找】通过username查找结果：" + admins.size() + "条");
            }
            
            adminUserIds = admins.stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
        }
        
        System.out.println("【管理员查找】最终结果：adminUserIds=" + adminUserIds);
        return adminUserIds;
    }
    
    /**
     * 更新通知
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateNotification(NotificationDTO dto) {
        Notification notification = notificationMapper.selectById(dto.getId());
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getMessage());
        notification.setType(dto.getType());
        
        notificationMapper.updateById(notification);
        log.info("更新通知成功: ID={}", notification.getId());
    }
    
    /**
     * 标记为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && !notification.getIsRead()) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
            log.info("标记通知为已读: ID={}", notificationId);
        }
    }
    
    /**
     * 全部标记为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Notification::getUserId, userId)
                        .or()
                        .isNull(Notification::getUserId));
        wrapper.eq(Notification::getIsRead, false);
        
        List<Notification> notifications = notificationMapper.selectList(wrapper);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notificationMapper.updateById(notification);
        }
        
        log.info("全部标记为已读: 用户ID={}, 数量={}", userId, notifications.size());
    }
    
    /**
     * 删除通知
     * 系统通知（type=system）只能由超级管理员删除
     * 其他通知可以由接收者删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        
        // 系统通知只能由超级管理员删除（通过检查user_id是否为NULL来判断是否为广播通知）
        if ("system".equals(notification.getType()) && notification.getUserId() == null) {
            // 检查当前用户是否为超级管理员
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || !"superadmin".equals(user.getUserType())) {
                throw new RuntimeException("只有超级管理员才能删除系统通知");
            }
        }
        
        notificationMapper.deleteById(notificationId);
        log.info("删除通知: ID={}, 操作用户: {}", notificationId, userId);
    }
    
    /**
     * 删除通知（无权限检查，用于内部调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId) {
        notificationMapper.deleteById(notificationId);
        log.info("删除通知: ID={}", notificationId);
    }
    
    /**
     * 根据关联ID删除通知（用于级联删除）
     * @param type 通知类型（announcement/visitor/application）
     * @param relatedId 关联的业务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotificationsByRelatedId(String type, Long relatedId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getType, type)
               .eq(Notification::getRelatedId, relatedId);
        
        int count = notificationMapper.delete(wrapper);
        log.info("级联删除通知: type={}, relatedId={}, 删除数量={}", type, relatedId, count);
    }
    
    /**
     * 转换为VO
     */
    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setTitle(notification.getTitle());
        vo.setMessage(notification.getContent());
        vo.setType(notification.getType());
        vo.setIcon("InfoFilled"); // 默认图标
        vo.setRead(notification.getIsRead());
        vo.setCreatedTime(notification.getCreatedAt());
        vo.setTime(formatTime(notification.getCreatedAt()));
        return vo;
    }
    
    /**
     * 格式化时间（刚刚、1小时前、1天前）
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return "刚刚";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else if (seconds < 2592000) {
            return (seconds / 86400) + "天前";
        } else {
            return dateTime.toString();
        }
    }
    
    /**
     * 根据类型获取默认图标
     */
    private String getDefaultIcon(String type) {
        switch (type) {
            case "info":
                return "InfoFilled";
            case "warning":
                return "Warning";
            case "success":
                return "Check";
            case "error":
                return "CircleClose";
            default:
                return "InfoFilled";
        }
    }
}

