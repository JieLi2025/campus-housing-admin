package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.dto.RoommateInviteDTO;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.service.RoommateService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.RoommateInviteVO;
import com.campus.dorm.vo.RoommateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/student/roommates")
@RequiredArgsConstructor
@Tag(name = "室友招募（学生端）")
public class RoommateController {
    
    private final RoommateService roommateService;
    private final StudentMapper studentMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    
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
    
    @GetMapping("/search")
    @Operation(summary = "搜索室友")
    public Result<List<RoommateVO>> searchRoommates(
            @RequestParam(required = false) List<String> preferredTags,
            @RequestParam(required = false) List<String> excludedTags,
            @RequestParam(required = false, defaultValue = "false") boolean random) {
        Long userId = getUserId();
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        return roommateService.searchRoommates(studentId, preferredTags, excludedTags, random);
    }
    
    @GetMapping("/popular-tags")
    @Operation(summary = "获取热门标签统计")
    public Result<List<Map<String, Object>>> getPopularTags(
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return roommateService.getPopularTags(limit);
    }
    
    @PostMapping("/invite")
    @Operation(summary = "发送邀请")
    public Result<?> sendInvite(@Validated @RequestBody RoommateInviteDTO dto) {
        Long userId = getUserId();
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        return roommateService.sendInvite(studentId, dto);
    }
    
    @GetMapping("/invites/sent")
    @Operation(summary = "我发送的邀请")
    public Result<List<RoommateInviteVO>> getSentInvites() {
        Long userId = getUserId();
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        return roommateService.getSentInvites(studentId);
    }
    
    @GetMapping("/invites/received")
    @Operation(summary = "我收到的邀请")
    public Result<List<RoommateInviteVO>> getReceivedInvites() {
        Long userId = getUserId();
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        return roommateService.getReceivedInvites(studentId);
    }
    
    @PutMapping("/invite/{id}/accept")
    @Operation(summary = "接受邀请")
    public Result<?> acceptInvite(@PathVariable Long id) {
        Long userId = getUserId();
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        return roommateService.acceptInvite(id, studentId);
    }
    
    @PutMapping("/invite/{id}/reject")
    @Operation(summary = "拒绝邀请")
    public Result<?> rejectInvite(@PathVariable Long id) {
        Long userId = getUserId();
        Long studentId = getStudentIdByUserId(userId);
        if (studentId == null) {
            return Result.error("学生不存在");
        }
        return roommateService.rejectInvite(id, studentId);
    }
}

