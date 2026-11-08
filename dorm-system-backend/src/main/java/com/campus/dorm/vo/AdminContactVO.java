package com.campus.dorm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员联系方式VO
 * 用于登录页面展示系统管理员联系信息
 * 
 * @author Campus Dorm System
 * @date 2024-12-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContactVO {
    
    /**
     * 管理员姓名
     */
    private String name;
    
    /**
     * 管理员邮箱
     */
    private String email;
    
    /**
     * 管理员电话
     */
    private String phone;
}
