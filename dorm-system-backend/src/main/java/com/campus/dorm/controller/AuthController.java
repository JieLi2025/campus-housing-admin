package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.CreateUserDTO;
import com.campus.dorm.dto.ForgotPasswordDTO;
import com.campus.dorm.dto.LoginDTO;
import com.campus.dorm.dto.RegisterDTO;
import com.campus.dorm.dto.ResetPasswordByInviteCodeDTO;
import com.campus.dorm.service.AuthService;
import com.campus.dorm.vo.AdminContactVO;
import com.campus.dorm.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理登录、注册、登出等认证相关请求
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、登出等接口")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     * 
     * @param loginDTO 登录信息
     * @return 登录结果（Token + 用户信息）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持学生、管理员、超级管理员登录")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        log.info("接收到登录请求：{}", loginDTO.getUsername());
        return authService.login(loginDTO);
    }
    
    /**
     * 学生注册
     * 
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "学生注册", description = "学生使用学号、姓名、邮箱和密码进行注册")
    public Result<?> register(@Validated @RequestBody RegisterDTO registerDTO) {
        log.info("接收到注册请求：studentId={}, realName={}", 
            registerDTO.getStudentId(), registerDTO.getRealName());
        return authService.register(registerDTO);
    }
    
    /**
     * 测试接口
     */
    @GetMapping("/test")
    @Operation(summary = "测试接口", description = "用于测试后端服务是否正常运行")
    public Result<String> test() {
        return Result.success("后端服务运行正常！当前时间：" + System.currentTimeMillis());
    }
    
    /**
     * 创建新用户（仅供超级管理员使用）
     * 
     * @param createUserDTO 用户信息
     * @return 创建结果
     */
    @PostMapping("/create-user")
    @Operation(summary = "创建用户", description = "超级管理员创建新的管理员或超级管理员")
    public Result<?> createUser(@Validated @RequestBody CreateUserDTO createUserDTO) {
        log.info("接收到创建用户请求：username={}, userType={}", 
            createUserDTO.getUsername(), createUserDTO.getUserType());
        return authService.createUser(createUserDTO);
    }
    
    /**
     * 忘记密码 - 重置密码
     * 前端验证邮箱验证码后调用此接口重置密码
     * 
     * @param forgotPasswordDTO 重置密码信息（包含邮箱、验证码、新密码）
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "学生通过邮箱验证后重置密码")
    public Result<?> resetPassword(@Validated @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        log.info("接收到重置密码请求：email={}", forgotPasswordDTO.getEmail());
        return authService.resetPasswordByEmail(forgotPasswordDTO);
    }
    
    /**
     * 验证邀请码用于重置密码
     * 
     * @param studentId 学号
     * @param inviteCode 邀请码
     * @return 验证结果
     */
    @PostMapping("/verify-invite-code")
    @Operation(summary = "验证邀请码", description = "验证学号和邀请码是否匹配，用于忘记密码功能")
    public Result<?> verifyInviteCodeForReset(@RequestBody java.util.Map<String, String> params) {
        String studentId = params.get("studentId");
        String inviteCode = params.get("inviteCode");
        log.info("接收到验证邀请码请求：studentId={}", studentId);
        return authService.verifyInviteCodeForReset(studentId, inviteCode);
    }
    
    /**
     * 通过邀请码重置密码
     * 
     * @param dto 重置密码信息（包含学号、邀请码、新密码）
     * @return 重置结果
     */
    @PostMapping("/reset-password-by-invite-code")
    @Operation(summary = "通过邀请码重置密码", description = "学生通过邀请码验证后重置密码")
    public Result<?> resetPasswordByInviteCode(@Validated @RequestBody ResetPasswordByInviteCodeDTO dto) {
        log.info("接收到通过邀请码重置密码请求：studentId={}", dto.getStudentId());
        return authService.resetPasswordByInviteCode(dto);
    }
    
    /**
     * 获取系统管理员联系方式（公开接口，用于登录页面展示）
     * 
     * @return 管理员联系方式
     */
    @GetMapping("/admin-contact")
    @Operation(summary = "获取管理员联系方式", description = "公开接口，获取超级管理员的联系方式用于登录页面展示")
    public Result<AdminContactVO> getAdminContact() {
        log.info("获取系统管理员联系方式");
        return authService.getAdminContact();
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * @param token 认证令牌
     * @return 用户信息
     */
    @GetMapping("/user-info")
    @Operation(summary = "获取当前用户信息", description = "根据Token获取当前登录用户的最新信息")
    public Result<?> getCurrentUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        log.info("获取当前用户信息");
        return authService.getCurrentUserInfo(token);
    }
    
}
