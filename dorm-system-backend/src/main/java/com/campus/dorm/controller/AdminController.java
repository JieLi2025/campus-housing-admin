package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.service.AdminService;
import com.campus.dorm.vo.AdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 * 
 * @author Campus Dorm System
 * @date 2024-10-24
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员信息查询接口（仅普通管理员）")
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * 获取所有管理员列表
     * 
     * @return 管理员列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取管理员列表", description = "获取所有类型为admin和superadmin的用户")
    public Result<List<AdminVO>> getAllAdmins() {
        log.info("获取所有管理员列表");
        return adminService.getAllAdmins();
    }
}

