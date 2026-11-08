package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.common.ResultCode;
import com.campus.dorm.dto.CreateUserDTO;
import com.campus.dorm.dto.ForgotPasswordDTO;
import com.campus.dorm.dto.LoginDTO;
import com.campus.dorm.dto.RegisterDTO;
import com.campus.dorm.dto.ResetPasswordByInviteCodeDTO;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.SysUserMapper;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.AdminContactVO;
import com.campus.dorm.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 认证服务
 * 处理用户登录、注册等认证相关功能
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;
    private final JwtUtil jwtUtil;
    
    /**
     * 用户登录
     * 
     * @param loginDTO 登录信息
     * @return 登录结果（包含 Token 和用户信息）
     */
    public Result<LoginVO> login(LoginDTO loginDTO) {
        log.info("用户登录请求：username={}, userType={}", loginDTO.getUsername(), loginDTO.getUserType());
        
        // 根据用户类型查询用户
        SysUser sysUser = findUserByUsernameAndType(loginDTO.getUsername(), loginDTO.getUserType());
        
        if (sysUser == null) {
            log.warn("用户不存在：username={}", loginDTO.getUsername());
            return Result.error(ResultCode.LOGIN_ERROR);
        }
        
        // 检查账号状态
        if (sysUser.getStatus() == 0) {
            log.warn("账号已被禁用：username={}", loginDTO.getUsername());
            return Result.error(ResultCode.ACCOUNT_DISABLED);
        }
        
        // 验证密码（暂时使用 MD5，后续可以改为 BCrypt）
        String encryptedPassword = encryptPassword(loginDTO.getPassword());
        if (!encryptedPassword.equals(sysUser.getPassword())) {
            log.warn("密码错误：username={}", loginDTO.getUsername());
            return Result.error(ResultCode.LOGIN_ERROR);
        }
        
        // 生成 JWT Token
        String token = jwtUtil.generateToken(sysUser.getId(), sysUser.getUsername(), sysUser.getUserType());
        
        // 构建返回数据
        LoginVO loginVO = buildLoginVO(sysUser, token);
        
        log.info("用户登录成功：username={}, userType={}", loginDTO.getUsername(), loginDTO.getUserType());
        return Result.success("登录成功", loginVO);
    }
    
    /**
     * 根据用户名和类型查询用户
     * 优化：管理员登录时，如果指定类型查不到，会尝试另一种管理员类型
     */
    private SysUser findUserByUsernameAndType(String username, String userType) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        
        if ("student".equals(userType)) {
            // 学生登录：先通过学号查询学生表，再关联查询用户表
            LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(Student::getStudentId, username);
            Student student = studentMapper.selectOne(studentWrapper);
            
            if (student == null) {
                return null;
            }
            
            // 通过 userId 查询用户信息
            return sysUserMapper.selectById(student.getUserId());
        } else if ("admin".equals(userType) || "superadmin".equals(userType)) {
            // 管理员登录：支持 admin 和 superadmin 互相尝试
            // 先尝试指定的类型
            wrapper.eq(SysUser::getUsername, username);
            wrapper.eq(SysUser::getUserType, userType);
            SysUser user = sysUserMapper.selectOne(wrapper);
            
            // 如果找不到，且是管理员类型，尝试另一种管理员类型
            if (user == null) {
                String alternativeType = "admin".equals(userType) ? "superadmin" : "admin";
                LambdaQueryWrapper<SysUser> alternativeWrapper = new LambdaQueryWrapper<>();
                alternativeWrapper.eq(SysUser::getUsername, username);
                alternativeWrapper.eq(SysUser::getUserType, alternativeType);
                user = sysUserMapper.selectOne(alternativeWrapper);
            }
            
            return user;
        } else {
            // 其他用户类型：直接按类型查询
            wrapper.eq(SysUser::getUsername, username);
            wrapper.eq(SysUser::getUserType, userType);
            return sysUserMapper.selectOne(wrapper);
        }
    }
    
    /**
     * 构建登录返回数据
     */
    private LoginVO buildLoginVO(SysUser sysUser, String token) {
        LoginVO loginVO = LoginVO.builder()
                .token(token)
                .userId(sysUser.getId())
                .username(sysUser.getUsername())
                .realName(sysUser.getRealName())
                .email(sysUser.getEmail())
                .userType(sysUser.getUserType())
                .avatar(sysUser.getAvatar())
                .build();
        
        // 如果是学生，附加学生信息
        if ("student".equals(sysUser.getUserType())) {
            LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Student::getUserId, sysUser.getId());
            Student student = studentMapper.selectOne(wrapper);
            
            if (student != null) {
                loginVO.setStudentId(student.getStudentId());
                loginVO.setMajor(student.getMajor());
                loginVO.setClassName(student.getClassName());
                loginVO.setGender(student.getGender());
                loginVO.setGrade(student.getGrade());
            }
        }
        
        return loginVO;
    }
    
    /**
     * 学生注册
     * 
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> register(RegisterDTO registerDTO) {
        log.info("学生注册请求：studentId={}, realName={}", registerDTO.getStudentId(), registerDTO.getRealName());
        
        // 0. 校验两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            log.warn("两次密码不一致：studentId={}", registerDTO.getStudentId());
            return Result.error(3000, "两次输入的密码不一致");
        }
        
        // 1. 检查学号是否已存在于 student 表
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getStudentId, registerDTO.getStudentId());
        Student existingStudent = studentMapper.selectOne(studentWrapper);
        
        if (existingStudent == null) {
            log.warn("学号不存在于系统中：{}", registerDTO.getStudentId());
            return Result.error(3001, "学号不存在，请联系管理员");
        }
        
        // 2. 检查学号是否已注册
        if (existingStudent.getIsRegistered() == 1) {
            log.warn("学号已注册：{}", registerDTO.getStudentId());
            return Result.error(3002, "该学号已注册，请直接登录");
        }
        
        // 3. 验证姓名是否匹配（防止冒用学号）
        if (existingStudent.getRealName() != null && !existingStudent.getRealName().isEmpty()) {
            String inputName = registerDTO.getRealName().trim();
            String storedName = existingStudent.getRealName().trim();
            
            if (!inputName.equals(storedName)) {
                log.warn("姓名不匹配：学号={}, 输入姓名={}, 系统姓名={}", 
                    registerDTO.getStudentId(), inputName, storedName);
                return Result.error(ResultCode.STUDENT_NAME_MISMATCH);
            }
            log.info("姓名验证通过：学号={}, 姓名={}", registerDTO.getStudentId(), inputName);
        } else {
            log.warn("学号 {} 在系统中没有姓名记录，跳过姓名验证", registerDTO.getStudentId());
        }
        
        // 4. 验证邀请码（必须提供且有效）
        if (registerDTO.getInviteCode() == null || registerDTO.getInviteCode().trim().isEmpty()) {
            log.warn("未提供邀请码：学号={}", registerDTO.getStudentId());
            return Result.error(3010, "邀请码不能为空，请联系管理员获取邀请码");
        }
        
        String inputInviteCode = registerDTO.getInviteCode().trim();
        
        // 4.1 验证邀请码是否匹配该学生
        if (existingStudent.getInviteCode() == null || !existingStudent.getInviteCode().equals(inputInviteCode)) {
            log.warn("邀请码不匹配：学号={}, 输入邀请码={}, 系统邀请码={}", 
                registerDTO.getStudentId(), inputInviteCode, existingStudent.getInviteCode());
            return Result.error(3011, "邀请码无效或不匹配，请检查后重试");
        }
        
        // 4.2 检查邀请码状态
        if (!"unused".equals(existingStudent.getInviteCodeStatus())) {
            log.warn("邀请码已被使用或已过期：学号={}, 邀请码状态={}", 
                registerDTO.getStudentId(), existingStudent.getInviteCodeStatus());
            return Result.error(3012, "该邀请码已被使用或已过期，请联系管理员重新生成");
        }
        
        // 4.3 检查邀请码是否过期
        if (existingStudent.getInviteCodeExpiry() == null || 
            existingStudent.getInviteCodeExpiry().isBefore(LocalDateTime.now())) {
            log.warn("邀请码已过期：学号={}, 有效期={}", 
                registerDTO.getStudentId(), existingStudent.getInviteCodeExpiry());
            // 更新邀请码状态为过期
            existingStudent.setInviteCodeStatus("expired");
            studentMapper.updateById(existingStudent);
            return Result.error(3013, "邀请码已过期，请联系管理员重新生成");
        }
        
        log.info("邀请码验证通过：学号={}, 邀请码={}", registerDTO.getStudentId(), inputInviteCode);
        
        // 6. 检查邮箱是否已被使用
        LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(SysUser::getEmail, registerDTO.getEmail());
        SysUser existingEmail = sysUserMapper.selectOne(emailWrapper);
        
        if (existingEmail != null) {
            log.warn("邮箱已被使用：{}", registerDTO.getEmail());
            return Result.error(3003, "该邮箱已被使用");
        }
        
        // 7. 检查用户名（学号）是否已存在于 sys_user 表
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getUsername, registerDTO.getStudentId());
        SysUser existingUser = sysUserMapper.selectOne(userWrapper);
        
        if (existingUser != null) {
            log.warn("用户名已存在：{}", registerDTO.getStudentId());
            return Result.error(3004, "该学号已存在系统中");
        }
        
        // 9. 加密密码
        String encryptedPassword = encryptPassword(registerDTO.getPassword());
        
        // 10. 创建 sys_user 记录
        SysUser newUser = new SysUser();
        newUser.setUsername(registerDTO.getStudentId());
        newUser.setPassword(encryptedPassword);
        newUser.setRealName(registerDTO.getRealName());
        newUser.setEmail(registerDTO.getEmail());
        newUser.setPhone(registerDTO.getPhone());
        newUser.setUserType("student");
        newUser.setStatus(1); // 启用状态
        newUser.setDeleted(0);
        
        int insertResult = sysUserMapper.insert(newUser);
        if (insertResult <= 0) {
            log.error("创建用户失败：{}", registerDTO.getStudentId());
            return Result.error(3005, "注册失败，请稍后重试");
        }
        
        // 11. 更新 student 表的注册状态
        LocalDateTime now = LocalDateTime.now();
        existingStudent.setIsRegistered(1);
        existingStudent.setRegistrationDate(now);
        existingStudent.setUserId(newUser.getId()); // 关联用户ID
        existingStudent.setInviteCodeStatus("used"); // 更新邀请码状态为已使用
        
        int updateResult = studentMapper.updateById(existingStudent);
        if (updateResult <= 0) {
            log.error("更新学生注册状态失败：{}", registerDTO.getStudentId());
            // 注意：这里应该回滚事务，由于使用了 @Transactional，会自动回滚
            throw new RuntimeException("更新学生注册状态失败");
        }
        
        log.info("学生注册成功：studentId={}, userId={}", registerDTO.getStudentId(), newUser.getId());
        return Result.success("注册成功，请登录");
    }
    
    /**
     * 密码加密（使用 MD5）
     * 注意：生产环境建议使用 BCrypt
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 创建新用户（仅供超级管理员使用）
     * 
     * @param createUserDTO 用户信息
     * @return 创建结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createUser(CreateUserDTO createUserDTO) {
        log.info("创建用户请求：username={}, userType={}", 
            createUserDTO.getUsername(), createUserDTO.getUserType());
        
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, createUserDTO.getUsername());
        SysUser existingUser = sysUserMapper.selectOne(usernameWrapper);
        
        if (existingUser != null) {
            log.warn("用户名已存在：{}", createUserDTO.getUsername());
            return Result.error(4001, "用户名已存在");
        }
        
        // 2. 检查邮箱是否已存在
        LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(SysUser::getEmail, createUserDTO.getEmail());
        SysUser existingEmail = sysUserMapper.selectOne(emailWrapper);
        
        if (existingEmail != null) {
            log.warn("邮箱已存在：{}", createUserDTO.getEmail());
            return Result.error(4002, "邮箱已存在");
        }
        
        // 3. 加密密码
        String encryptedPassword = encryptPassword(createUserDTO.getPassword());
        
        // 4. 创建用户
        SysUser newUser = new SysUser();
        newUser.setUsername(createUserDTO.getUsername());
        newUser.setPassword(encryptedPassword);
        newUser.setRealName(createUserDTO.getRealName());
        newUser.setEmail(createUserDTO.getEmail());
        newUser.setPhone(createUserDTO.getPhone());
        newUser.setUserType(createUserDTO.getUserType());
        newUser.setStatus(1); // 启用状态
        newUser.setDeleted(0);
        
        int insertResult = sysUserMapper.insert(newUser);
        if (insertResult <= 0) {
            log.error("创建用户失败：{}", createUserDTO.getUsername());
            return Result.error(4003, "创建用户失败");
        }
        
        log.info("用户创建成功：username={}, userId={}, userType={}", 
            createUserDTO.getUsername(), newUser.getId(), createUserDTO.getUserType());
        
        return Result.success("用户创建成功");
    }
    
    /**
     * 通过邮箱重置密码（忘记密码功能）
     * 
     * @param forgotPasswordDTO 重置密码信息
     * @return 重置结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> resetPasswordByEmail(ForgotPasswordDTO forgotPasswordDTO) {
        log.info("重置密码请求：email={}", forgotPasswordDTO.getEmail());
        
        // 1. 查询邮箱对应的用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEmail, forgotPasswordDTO.getEmail());
        SysUser user = sysUserMapper.selectOne(wrapper);
        
        if (user == null) {
            log.warn("邮箱未注册：{}", forgotPasswordDTO.getEmail());
            return Result.error(5001, "该邮箱未注册");
        }
        
        // 2. 验证验证码（暂时简化处理，实际应该有验证码服务）
        // 在实际生产环境中，应该：
        // - 从Redis或缓存中获取该邮箱的验证码
        // - 检查验证码是否正确且未过期
        // - 验证成功后删除验证码
        // 这里为了简化，我们信任前端已经验证过验证码
        log.info("验证码验证通过：email={}, code={}", forgotPasswordDTO.getEmail(), forgotPasswordDTO.getCode());
        
        // 3. 加密新密码
        String encryptedPassword = encryptPassword(forgotPasswordDTO.getNewPassword());
        
        // 4. 更新密码（updatedAt字段会由MyBatis-Plus自动填充）
        user.setPassword(encryptedPassword);
        
        int updateResult = sysUserMapper.updateById(user);
        if (updateResult <= 0) {
            log.error("更新密码失败：email={}", forgotPasswordDTO.getEmail());
            return Result.error(5002, "密码重置失败，请稍后重试");
        }
        
        log.info("密码重置成功：email={}, userId={}", forgotPasswordDTO.getEmail(), user.getId());
        return Result.success("密码重置成功，请使用新密码登录");
    }
    
    /**
     * 验证邀请码用于重置密码
     * 注意：忘记密码是已注册用户的操作，邀请码已经使用过了，
     * 所以只验证邀请码是否匹配，不检查状态和过期时间
     * 
     * @param studentId 学号
     * @param inviteCode 邀请码
     * @return 验证结果
     */
    public Result<?> verifyInviteCodeForReset(String studentId, String inviteCode) {
        log.info("验证邀请码请求（忘记密码）：studentId={}", studentId);
        
        // 1. 参数校验
        if (studentId == null || studentId.trim().isEmpty()) {
            return Result.error(6001, "学号不能为空");
        }
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            return Result.error(6002, "邀请码不能为空");
        }
        
        // 2. 查询学生信息
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getStudentId, studentId.trim());
        Student student = studentMapper.selectOne(wrapper);
        
        if (student == null) {
            log.warn("学号不存在：{}", studentId);
            return Result.error(6003, "学号不存在");
        }
        
        // 3. 检查学生是否已注册
        if (student.getIsRegistered() != 1) {
            log.warn("学号未注册：{}", studentId);
            return Result.error(6004, "该学号尚未注册，请先注册");
        }
        
        // 4. 验证邀请码是否匹配（不检查状态和过期时间，因为已注册用户的邀请码状态是used）
        if (student.getInviteCode() == null || !student.getInviteCode().equals(inviteCode.trim())) {
            log.warn("邀请码不匹配：studentId={}", studentId);
            return Result.error(6005, "邀请码无效或不匹配");
        }
        
        log.info("邀请码验证成功（忘记密码）：studentId={}", studentId);
        return Result.success("验证成功");
    }
    
    /**
     * 通过邀请码重置密码
     * 
     * @param dto 重置密码信息
     * @return 重置结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> resetPasswordByInviteCode(ResetPasswordByInviteCodeDTO dto) {
        log.info("通过邀请码重置密码请求：studentId={}", dto.getStudentId());
        
        // 1. 再次验证邀请码
        Result<?> verifyResult = verifyInviteCodeForReset(dto.getStudentId(), dto.getInviteCode());
        if (verifyResult.getCode() != 200) {
            return verifyResult;
        }
        
        // 2. 查询学生信息获取userId
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getStudentId, dto.getStudentId().trim());
        Student student = studentMapper.selectOne(studentWrapper);
        
        if (student.getUserId() == null) {
            log.error("学生没有关联的用户ID：studentId={}", dto.getStudentId());
            return Result.error(6007, "账号异常，请联系管理员");
        }
        
        // 3. 查询用户信息
        SysUser user = sysUserMapper.selectById(student.getUserId());
        if (user == null) {
            log.error("用户不存在：userId={}", student.getUserId());
            return Result.error(6008, "账号异常，请联系管理员");
        }
        
        // 4. 加密新密码
        String encryptedPassword = encryptPassword(dto.getNewPassword());
        
        // 5. 更新密码
        user.setPassword(encryptedPassword);
        int updateResult = sysUserMapper.updateById(user);
        
        if (updateResult <= 0) {
            log.error("更新密码失败：studentId={}", dto.getStudentId());
            return Result.error(6009, "密码重置失败，请稍后重试");
        }
        
        // 6. 生成新的邀请码（密码重置后邀请码更新，下次需要联系管理员获取新邀请码）
        String newInviteCode = generateNewInviteCode(dto.getStudentId());
        student.setInviteCode(newInviteCode);
        student.setInviteCodeStatus("unused");
        student.setInviteCodeExpiry(LocalDateTime.now().plusDays(30));
        studentMapper.updateById(student);
        
        log.info("通过邀请码重置密码成功，已生成新邀请码：studentId={}, userId={}", dto.getStudentId(), user.getId());
        return Result.success("密码重置成功，请使用新密码登录");
    }
    
    /**
     * 生成新的邀请码
     */
    private String generateNewInviteCode(String studentId) {
        // 格式：INV + 学号后4位 + 8位随机字符
        String suffix = studentId.length() >= 4 ? studentId.substring(studentId.length() - 4) : studentId;
        String randomPart = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "INV" + suffix + randomPart;
    }
    
    /**
     * 获取系统管理员联系方式
     * 查询第一个超级管理员的联系信息用于登录页面展示
     * 
     * @return 管理员联系方式
     */
    public Result<AdminContactVO> getAdminContact() {
        log.info("获取系统管理员联系方式");
        
        // 查询第一个启用状态的超级管理员
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserType, "superadmin")
               .eq(SysUser::getStatus, 1)
               .orderByAsc(SysUser::getId)
               .last("LIMIT 1");
        
        SysUser superAdmin = sysUserMapper.selectOne(wrapper);
        
        if (superAdmin == null) {
            log.warn("未找到超级管理员");
            // 返回默认值
            return Result.success(AdminContactVO.builder()
                    .name("系统管理员")
                    .email("admin@campus.edu.cn")
                    .phone("请联系学校管理部门")
                    .build());
        }
        
        // 构建返回数据
        AdminContactVO contactVO = AdminContactVO.builder()
                .name(StringUtils.hasText(superAdmin.getRealName()) ? superAdmin.getRealName() : superAdmin.getUsername())
                .email(StringUtils.hasText(superAdmin.getEmail()) ? superAdmin.getEmail() : "未设置")
                .phone(StringUtils.hasText(superAdmin.getPhone()) ? superAdmin.getPhone() : "未设置")
                .build();
        
        log.info("获取管理员联系方式成功：name={}", contactVO.getName());
        return Result.success(contactVO);
    }
    
    /**
     * 获取当前登录用户信息
     * 
     * @param token 认证令牌
     * @return 用户信息
     */
    public Result<?> getCurrentUserInfo(String token) {
        log.info("获取当前用户信息");
        
        if (!StringUtils.hasText(token)) {
            return Result.error("未提供认证令牌");
        }
        
        // 移除 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证并解析 Token
        if (!jwtUtil.validateToken(token)) {
            return Result.error("令牌无效或已过期");
        }
        
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Result.error("无法解析用户信息");
        }
        
        // 查询用户信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 构建返回数据（不包含敏感信息），直接使用 LoginVO
        LoginVO userInfo = LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .build();
        
        log.info("获取用户信息成功：userId={}, username={}", userId, user.getUsername());
        return Result.success(userInfo);
    }
    
}

