package com.campus.dorm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.service.StudentBuildingAssignmentService;
import com.campus.dorm.service.DormGroupService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.StudentBuildingAssignmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 学生宿舍查看Controller
 * 
 * @author Campus Dorm System
 * @date 2024-10-23
 */
@Slf4j
@RestController
@RequestMapping("/student/dorm")
@RequiredArgsConstructor
public class StudentDormController {
    
    private final StudentBuildingAssignmentService studentBuildingAssignmentService;
    private final StudentMapper studentMapper;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    private final DormGroupService dormGroupService;
    
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取当前学生的宿舍信息（从宿舍组获取）
     */
    @GetMapping("/my-dorm")
    public Result<?> getMyDorm() {
        try {
            Long userId = getUserId();
            if (userId == null) {
                return Result.error("未登录");
            }

            // 获取学生信息
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getUserId, userId);
            Student student = studentMapper.selectOne(wrapper);
            if (student == null) {
                return Result.error("非学生用户");
            }
            
            log.info("查询学生{}的宿舍信息", student.getId());
            return dormGroupService.getMyDormGroup(student.getId());
            
        } catch (Exception e) {
            log.error("查询宿舍信息失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取当前学生的宿舍信息（简化路径）
     */
    @GetMapping
    public Result<?> getMyDormSimple() {
        return getMyDorm();
    }
    
    /**
     * 根据学生ID获取宿舍信息（管理员使用）
     */
    @GetMapping("/{studentId}")
    public Result<?> getStudentDorm(@PathVariable Long studentId) {
        return dormGroupService.getMyDormGroup(studentId);
    }
    
    /**
     * 获取当前学生的班级楼栋分配信息
     */
    @GetMapping("/my-building-assignment")
    public Result<StudentBuildingAssignmentVO> getMyBuildingAssignment(HttpServletRequest request) {
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
            
            // 获取学生信息
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getUserId, userId);
            Student student = studentMapper.selectOne(wrapper);
            if (student == null) {
                return Result.error("非学生用户");
            }
            
            log.info("查询学生{}的班级楼栋分配", student.getId());
            return studentBuildingAssignmentService.getMyBuildingAssignment(student.getId());
            
        } catch (Exception e) {
            log.error("查询班级楼栋分配失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取当前学生的宿舍组信息
     */
    @GetMapping("/my-dorm-group")
    public Result<?> getMyDormGroup() {
        return getMyDorm();
    }
}
