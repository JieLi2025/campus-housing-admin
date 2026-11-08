package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.StudentQueryDTO;
import com.campus.dorm.dto.StudentUpdateDTO;
import com.campus.dorm.service.StudentService;
import com.campus.dorm.vo.InviteCodeGenerateResultVO;
import com.campus.dorm.vo.StudentImportResultVO;
import com.campus.dorm.vo.StudentListVO;
import com.campus.dorm.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生管理控制器
 * 
 * @author Campus Dorm System
 * @date 2024-10-22
 */
@Slf4j
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "学生管理", description = "学生信息管理相关接口")
public class StudentController {
    
    private final StudentService studentService;
    
    /**
     * 获取学生列表（分页、搜索、筛选）
     */
    @PostMapping("/list")
    @Operation(summary = "获取学生列表", description = "支持分页、搜索和多条件筛选")
    public Result<StudentListVO> getStudentList(@RequestBody StudentQueryDTO queryDTO) {
        log.info("接收到学生列表查询请求：{}", queryDTO);
        return studentService.getStudentList(queryDTO);
    }
    
    /**
     * 获取学生详情（通过ID）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取学生详情", description = "根据学生ID获取详细信息")
    public Result<StudentVO> getStudentDetail(@PathVariable Long id) {
        log.info("接收到学生详情查询请求，ID：{}", id);
        return studentService.getStudentDetail(id);
    }
    
    /**
     * 根据学号获取学生详情
     */
    @GetMapping("/by-student-id/{studentId}")
    @Operation(summary = "根据学号获取学生详情", description = "根据学号查询学生信息")
    public Result<StudentVO> getStudentByStudentId(@PathVariable String studentId) {
        log.info("接收到学号查询请求，学号：{}", studentId);
        return studentService.getStudentByStudentId(studentId);
    }
    
    /**
     * 根据用户ID获取学生详情
     */
    @GetMapping("/by-user-id/{userId}")
    @Operation(summary = "根据用户ID获取学生详情", description = "根据用户ID查询学生信息")
    public Result<StudentVO> getStudentByUserId(@PathVariable Long userId) {
        log.info("接收到用户ID查询请求，userId：{}", userId);
        return studentService.getStudentByUserId(userId);
    }
    
    /**
     * 导入学生信息（支持Excel和CSV）
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入学生信息", description = "批量导入学生基础信息，支持.xlsx和.csv格式")
    public Result<StudentImportResultVO> importStudents(
            @Parameter(description = "Excel文件（.xlsx格式）或CSV文件（.csv格式）", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file) {
        log.info("接收到学生导入请求，文件名：{}", file.getOriginalFilename());
        
        // 验证文件
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return Result.error("文件名不能为空");
        }
        
        // 检查文件格式
        String lowerFilename = filename.toLowerCase();
        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".csv")) {
            return Result.error("请上传xlsx或csv格式的文件");
        }
        
        return studentService.importStudents(file);
    }
    
    /**
     * 批量生成邀请码
     */
    @PostMapping("/generate-invite-codes")
    @Operation(summary = "批量生成邀请码", description = "为未注册的学生批量生成邀请码")
    public Result<InviteCodeGenerateResultVO> generateInviteCodes(@RequestBody(required = false) List<Long> studentIds) {
        log.info("接收到批量生成邀请码请求，学生ID列表：{}", studentIds);
        return studentService.generateInviteCodes(studentIds);
    }
    
    /**
     * 更新学生信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新学生信息", description = "更新指定学生的详细信息")
    public Result<?> updateStudent(
            @PathVariable Long id,
            @Validated @RequestBody StudentUpdateDTO updateDTO) {
        log.info("接收到更新学生信息请求，学生ID：{}，更新内容：{}", id, updateDTO);
        
        // 确保路径参数和DTO中的ID一致
        updateDTO.setId(id);
        
        return studentService.updateStudent(updateDTO);
    }
    
    /**
     * 删除单个学生（硬删除）
     * 警告：此操作会永久删除学生及所有关联数据，不可恢复！
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学生", description = "永久删除指定学生及其所有关联数据（室友邀请、队伍、床位、申请、通知等），此操作不可恢复！")
    public Result<?> deleteStudent(@PathVariable Long id) {
        log.warn("⚠️ 接收到删除学生请求（硬删除，不可恢复），学生ID：{}", id);
        return studentService.hardDeleteStudent(id);
    }
    
    /**
     * 批量删除学生（硬删除）
     * 警告：此操作会永久删除学生及所有关联数据，不可恢复！
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除学生", description = "永久删除学生及其所有关联数据（室友邀请、队伍、床位、申请、通知等），此操作不可恢复！")
    public Result<?> batchDeleteStudents(@RequestBody List<Long> studentIds) {
        log.warn("⚠️ 接收到批量删除学生请求（硬删除，不可恢复），学生ID列表：{}", studentIds);
        return studentService.batchHardDeleteStudents(studentIds);
    }
    
    /**
     * 获取学生的所有关联数据（删除前预览）
     */
    @GetMapping("/{id}/related-data")
    @Operation(summary = "查看学生关联数据", description = "查看学生的所有关联数据，用于删除前预览")
    public Result<?> getStudentRelatedData(@PathVariable Long id) {
        log.info("接收到查看学生关联数据请求，学生ID：{}", id);
        return studentService.getStudentRelatedData(id);
    }
    
    /**
     * 调试接口：对比楼栋专业分配与实际学生数据
     */
    @GetMapping("/debug/compare-assignment")
    @Operation(summary = "调试：对比专业分配与学生数据", description = "检查哪些学生不在专业分配范围内")
    public Result<?> debugCompareAssignment() {
        log.info("🔍 调试：对比专业分配与学生数据");
        return studentService.debugCompareAssignment();
    }
    
    /**
     * 分析接口：检测专业分配数据不匹配问题
     */
    @GetMapping("/debug/analyze-mismatch")
    @Operation(summary = "分析：检测专业分配数据不匹配", description = "检测哪些班级的实际学生数与分配规则不一致")
    public Result<?> analyzeAssignmentMismatch() {
        log.info("🔍 分析：检测专业分配数据不匹配");
        return studentService.analyzeAssignmentMismatch();
    }
}

