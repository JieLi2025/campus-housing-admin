package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.UpdatePasswordDTO;
import com.campus.dorm.dto.UpdateProfileDTO;
import com.campus.dorm.service.FileUploadService;
import com.campus.dorm.service.ProfileService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.ProfileVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 个人设置控制器
 */
@Slf4j
@RestController
@RequestMapping("/profile")
public class ProfileController {
    
    @Resource
    private ProfileService profileService;
    
    @Resource
    private FileUploadService fileUploadService;
    
    @Resource
    private JwtUtil jwtUtil;
    
    @Resource
    private HttpServletRequest request;
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取个人信息
     */
    @GetMapping
    public Result<ProfileVO> getProfile() {
        Long userId = getUserId();
        ProfileVO profile = profileService.getProfile(userId);
        return Result.success(profile);
    }
    
    /**
     * 更新个人信息
     */
    @PutMapping
    public Result<Void> updateProfile(@Validated @RequestBody UpdateProfileDTO dto) {
        Long userId = getUserId();
        profileService.updateProfile(userId, dto);
        return Result.success();
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Validated @RequestBody UpdatePasswordDTO dto) {
        Long userId = getUserId();
        profileService.updatePassword(userId, dto);
        return Result.success();
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = fileUploadService.uploadAvatar(file);
            log.info("头像上传成功，URL: {}", avatarUrl);
            return Result.success(avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error(e.getMessage());
        }
    }
}

