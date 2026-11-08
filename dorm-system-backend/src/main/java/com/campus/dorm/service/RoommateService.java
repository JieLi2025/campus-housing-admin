package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.RoommateInviteDTO;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.vo.RoommateInviteVO;
import com.campus.dorm.vo.RoommateVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateService {
    
    private final RoommateInviteMapper inviteMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final DormGroupMapper dormGroupMapper;
    private final BuildingMajorAssignmentMapper buildingMajorAssignmentMapper;
    private final ObjectMapper objectMapper;
    
    private static final Map<String, String> STATUS_MAP = new HashMap<>();
    
    static {
        STATUS_MAP.put("PENDING", "待处理");
        STATUS_MAP.put("ACCEPTED", "已接受");
        STATUS_MAP.put("REJECTED", "已拒绝");
        STATUS_MAP.put("EXPIRED", "已过期");
    }
    
    /**
     * 获取热门标签统计（从student.tags字段统计出现频率最高的标签）
     * @param limit 返回的标签数量限制
     */
    public Result<List<Map<String, Object>>> getPopularTags(int limit) {
        // 查询所有已注册学生的标签
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getIsRegistered, 1)
               .eq(Student::getDeleted, 0)
               .isNotNull(Student::getTags);
        
        List<Student> students = studentMapper.selectList(wrapper);
        
        // 统计标签出现次数
        Map<String, Integer> tagCount = new HashMap<>();
        for (Student student : students) {
            List<String> tags = parseTagsFromJson(student.getTags());
            for (String tag : tags) {
                tagCount.merge(tag, 1, Integer::sum);
            }
        }
        
        // 按出现次数排序，取前N个
        List<Map<String, Object>> result = tagCount.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(limit)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("tag", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        
        return Result.success(result);
    }
    
    public Result<List<RoommateVO>> searchRoommates(Long studentId, List<String> preferredTags, List<String> excludedTags, boolean random) {
        // 获取当前学生信息
        Student currentStudent = studentMapper.selectById(studentId);
        if (currentStudent == null) {
            return Result.error("学生不存在");
        }
        
        // 获取当前学生的楼栋分配信息
        String currentGrade = currentStudent.getGrade();
        String currentMajor = currentStudent.getMajor();
        String currentGender = currentStudent.getGender();
        
        // 验证性别信息是否存在（必须有性别才能搜索室友，确保宿舍安全）
        if (currentGender == null || currentGender.trim().isEmpty()) {
            log.error("学生{}的性别信息为空，无法搜索室友", studentId);
            return Result.error("您的性别信息不完整，无法搜索室友。请联系管理员完善信息。");
        }
        
        // 查询当前学生所属的楼栋（通过building_major_assignment表）
        Long assignedBuildingId = getAssignedBuildingId(currentGrade, currentMajor, currentGender);
        
        if (assignedBuildingId == null) {
            log.warn("学生{}未找到楼栋分配信息: grade={}, major={}, gender={}", 
                studentId, currentGrade, currentMajor, currentGender);
            // 如果没有楼栋分配，返回空列表（不应该出现这种情况）
            return Result.success(new ArrayList<>());
        }
        
        log.debug("学生{}的楼栋分配: buildingId={}", studentId, assignedBuildingId);
        
        // 查询同楼栋、同性别、同专业、同班级的其他学生（已注册的）
        // 性别和楼栋过滤是必须的，确保宿舍安全和管理规范
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Student::getId, studentId)
               .eq(Student::getGender, currentGender)  // 必须同性别
               .eq(Student::getMajor, currentMajor)    // 必须同专业
               .eq(Student::getClassName, currentStudent.getClassName())  // 必须同班级
               .eq(Student::getIsRegistered, 1)        // 必须已注册
               .eq(Student::getDeleted, 0);            // 未删除
        
        List<Student> students = studentMapper.selectList(wrapper);
        
        // 进一步过滤：只保留同楼栋的学生
        // 通过每个学生的年级、专业、性别查询其楼栋分配，确保在同一楼栋
        students = students.stream()
            .filter(student -> {
                Long studentBuildingId = getAssignedBuildingId(
                    student.getGrade(), 
                    student.getMajor(), 
                    student.getGender()
                );
                boolean sameBuilding = assignedBuildingId.equals(studentBuildingId);
                if (!sameBuilding) {
                    log.debug("过滤掉不同楼栋的学生: studentId={}, buildingId={}", 
                        student.getId(), studentBuildingId);
                }
                return sameBuilding;
            })
            .collect(Collectors.toList());
        
        if (students.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        
        // 转换为VO并计算匹配度
        List<RoommateVO> voList = students.stream()
            .map(student -> convertStudentToVO(student, preferredTags, excludedTags))
            .collect(Collectors.toList());
        
        if (random) {
            // 随机模式：打乱顺序
            Collections.shuffle(voList);
        } else {
            // 标签匹配模式：先过滤掉有排除标签的学生，再按匹配度排序
            if (excludedTags != null && !excludedTags.isEmpty()) {
                Set<String> excludedTagSet = new HashSet<>(excludedTags);
                voList = voList.stream()
                    .filter(vo -> {
                        List<String> studentTags = vo.getTags();
                        if (studentTags == null || studentTags.isEmpty()) {
                            return true; // 没有标签的学生不过滤
                        }
                        // 检查学生是否有任何排除标签
                        return studentTags.stream().noneMatch(excludedTagSet::contains);
                    })
                    .collect(Collectors.toList());
            }
            voList.sort((a, b) -> b.getRate() - a.getRate());
        }
        
        return Result.success(voList);
    }
    
    /**
     * 将Student转换为RoommateVO，并计算匹配度
     */
    private RoommateVO convertStudentToVO(Student student, List<String> preferredTags, List<String> excludedTags) {
        RoommateVO vo = new RoommateVO();
        vo.setId(student.getId());
        vo.setStudentId(student.getId());
        vo.setUserId(student.getUserId());  // 设置关联的系统用户ID，用于聊天等功能
        vo.setStudentNumber(student.getStudentId());  // 设置学号（字符串）
        vo.setName(student.getRealName());
        vo.setMajor(student.getMajor());
        vo.setClassName(student.getClassName());
        // 从sys_user表获取头像
        if (student.getUserId() != null) {
            SysUser user = sysUserMapper.selectById(student.getUserId());
            vo.setAvatar(user != null ? user.getAvatar() : null);
        } else {
            vo.setAvatar(null);
        }
        vo.setBio(student.getBio());
        
        // 解析学生标签
        List<String> studentTags = parseTagsFromJson(student.getTags());
        vo.setTags(studentTags);
        
        // 计算匹配度
        int rate = calculateMatchRate(studentTags, preferredTags, excludedTags);
        vo.setRate(rate);
        
        return vo;
    }
    
    /**
     * 解析JSON格式的标签
     */
    private List<String> parseTagsFromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析标签JSON失败: {}", tagsJson);
            return new ArrayList<>();
        }
    }
    
    /**
     * 计算匹配度（0-100）
     */
    private int calculateMatchRate(List<String> studentTags, List<String> preferredTags, List<String> excludedTags) {
        if (studentTags == null || studentTags.isEmpty()) {
            return 50; // 没有标签的学生给50分基础分
        }
        
        if ((preferredTags == null || preferredTags.isEmpty()) && 
            (excludedTags == null || excludedTags.isEmpty())) {
            return 50; // 没有设置偏好时给50分
        }
        
        int score = 50; // 基础分
        Set<String> studentTagSet = new HashSet<>(studentTags);
        
        // 匹配偏好标签加分
        if (preferredTags != null) {
            for (String tag : preferredTags) {
                if (studentTagSet.contains(tag)) {
                    score += 15; // 每匹配一个偏好标签加15分
                }
            }
        }
        
        // 匹配排除标签减分
        if (excludedTags != null) {
            for (String tag : excludedTags) {
                if (studentTagSet.contains(tag)) {
                    score -= 20; // 每匹配一个排除标签减20分
                }
            }
        }
        
        // 限制在0-100范围内
        return Math.max(0, Math.min(100, score));
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<?> sendInvite(Long senderId, RoommateInviteDTO dto) {
        Student sender = studentMapper.selectById(senderId);
        Student receiver = studentMapper.selectById(dto.getReceiverId());
        if (sender == null || receiver == null) return Result.error("用户不存在");
        
        // 检查发送者是否已经完成分配（已选房或已分配）
        if (isStudentAllocated(senderId)) {
            return Result.error("您已完成宿舍分配，无法再发送邀请");
        }
        
        // 检查接收者是否已经完成分配
        if (isStudentAllocated(dto.getReceiverId())) {
            return Result.error("该学生已完成宿舍分配，无法接收邀请");
        }
        
        // 检查是否已发送过待处理的邀请
        LambdaQueryWrapper<RoommateInvite> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(RoommateInvite::getSenderId, senderId)
                   .eq(RoommateInvite::getReceiverId, dto.getReceiverId())
                   .eq(RoommateInvite::getStatus, "PENDING");
        Long existCount = inviteMapper.selectCount(existWrapper);
        if (existCount > 0) {
            return Result.error("已向该学生发送过邀请，请勿重复发送");
        }
        
        RoommateInvite invite = new RoommateInvite();
        invite.setSenderId(senderId);
        invite.setSenderName(sender.getRealName());
        invite.setSenderNumber(sender.getStudentId());
        invite.setReceiverId(dto.getReceiverId());
        invite.setReceiverName(receiver.getRealName());
        invite.setReceiverNumber(receiver.getStudentId());
        invite.setStatus("PENDING");
        
        inviteMapper.insert(invite);
        return Result.success("邀请发送成功");
    }
    
    /**
     * 检查学生是否已完成宿舍分配（已选房或已分配）
     */
    private boolean isStudentAllocated(Long studentId) {
        // 方法1：检查学生是否是已完成分配的宿舍组的组长
        LambdaQueryWrapper<DormGroup> leaderWrapper = new LambdaQueryWrapper<>();
        leaderWrapper.eq(DormGroup::getLeaderId, studentId)
                    .in(DormGroup::getStatus, "room_selected", "allocated");
        Long leaderCount = dormGroupMapper.selectCount(leaderWrapper);
        if (leaderCount > 0) {
            log.debug("学生{}是已完成分配的宿舍组组长", studentId);
            return true;
        }
        
        // 方法2：检查学生是否在已完成分配的宿舍组的成员列表中
        // 查询所有已完成分配的宿舍组，然后检查成员列表
        LambdaQueryWrapper<DormGroup> groupWrapper = new LambdaQueryWrapper<>();
        groupWrapper.in(DormGroup::getStatus, "room_selected", "allocated")
                   .isNotNull(DormGroup::getMembers);
        List<DormGroup> groups = dormGroupMapper.selectList(groupWrapper);
        
        for (DormGroup group : groups) {
            try {
                String membersJson = group.getMembers();
                if (membersJson != null && !membersJson.trim().isEmpty()) {
                    List<Map<String, Object>> members = objectMapper.readValue(
                        membersJson, 
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}
                    );
                    boolean isMember = members.stream()
                        .anyMatch(m -> {
                            Object idObj = m.get("id");
                            if (idObj instanceof Number) {
                                return studentId.equals(((Number) idObj).longValue());
                            }
                            return false;
                        });
                    if (isMember) {
                        log.debug("学生{}在已完成分配的宿舍组{}的成员列表中", studentId, group.getId());
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("解析宿舍组{}的成员列表失败: {}", group.getId(), e.getMessage());
            }
        }
        
        return false;
    }
    
    public Result<List<RoommateInviteVO>> getSentInvites(Long studentId) {
        LambdaQueryWrapper<RoommateInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoommateInvite::getSenderId, studentId);
        wrapper.orderByDesc(RoommateInvite::getCreatedAt);
        
        List<RoommateInvite> list = inviteMapper.selectList(wrapper);
        List<RoommateInviteVO> voList = list.stream()
            .map(this::convertInviteToVO)
            .collect(Collectors.toList());
        
        return Result.success(voList);
    }
    
    public Result<List<RoommateInviteVO>> getReceivedInvites(Long studentId) {
        LambdaQueryWrapper<RoommateInvite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoommateInvite::getReceiverId, studentId);
        wrapper.orderByDesc(RoommateInvite::getCreatedAt);
        
        List<RoommateInvite> list = inviteMapper.selectList(wrapper);
        List<RoommateInviteVO> voList = list.stream()
            .map(this::convertInviteToVO)
            .collect(Collectors.toList());
        
        return Result.success(voList);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<?> acceptInvite(Long inviteId, Long studentId) {
        RoommateInvite invite = inviteMapper.selectById(inviteId);
        if (invite == null) return Result.error("邀请不存在");
        if (!invite.getReceiverId().equals(studentId)) return Result.error("无权操作");
        if (!"PENDING".equals(invite.getStatus())) return Result.error("邀请已处理");
        
        invite.setStatus("ACCEPTED");
        inviteMapper.updateById(invite);
        
        return Result.success("已接受邀请");
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rejectInvite(Long inviteId, Long studentId) {
        RoommateInvite invite = inviteMapper.selectById(inviteId);
        if (invite == null) return Result.error("邀请不存在");
        if (!invite.getReceiverId().equals(studentId)) return Result.error("无权操作");
        if (!"PENDING".equals(invite.getStatus())) return Result.error("邀请已处理");
        
        invite.setStatus("REJECTED");
        inviteMapper.updateById(invite);
        
        return Result.success("已拒绝邀请");
    }
    
    private RoommateInviteVO convertInviteToVO(RoommateInvite invite) {
        RoommateInviteVO vo = new RoommateInviteVO();
        BeanUtils.copyProperties(invite, vo);
        vo.setStatusLabel(STATUS_MAP.getOrDefault(invite.getStatus(), invite.getStatus()));
        return vo;
    }
    
    /**
     * 获取学生分配的楼栋ID
     * 根据年级、专业、性别查询building_major_assignment表
     * 
     * @param grade 年级
     * @param major 专业
     * @param gender 性别
     * @return 楼栋ID，如果未找到返回null
     */
    private Long getAssignedBuildingId(String grade, String major, String gender) {
        try {
            LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BuildingMajorAssignment::getGrade, grade)
                   .eq(BuildingMajorAssignment::getMajorName, major)
                   .eq(BuildingMajorAssignment::getGender, gender)
                   .eq(BuildingMajorAssignment::getStatus, 1)
                   .last("LIMIT 1");
            
            BuildingMajorAssignment assignment = buildingMajorAssignmentMapper.selectOne(wrapper);
            return assignment != null ? assignment.getBuildingId() : null;
        } catch (Exception e) {
            log.error("查询楼栋分配失败: grade={}, major={}, gender={}, error={}", 
                grade, major, gender, e.getMessage());
            return null;
        }
    }
}

