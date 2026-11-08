package com.campus.dorm.common;

import lombok.Getter;

/**
 * 返回状态码枚举
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Getter
public enum ResultCode {
    
    // ========== 通用状态码 ==========
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    
    // 常用别名（为了兼容）
    BAD_REQUEST(400, "请求参数错误"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    
    // ========== 认证相关 1000-1099 ==========
    UNAUTHORIZED(1001, "未登录或登录已过期"),
    LOGIN_ERROR(1002, "用户名或密码错误"),
    TOKEN_INVALID(1003, "Token无效"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    PERMISSION_DENIED(1005, "权限不足"),
    ACCOUNT_DISABLED(1006, "账号已被禁用"),
    
    // ========== 学生相关 2000-2099 ==========
    STUDENT_NOT_FOUND(2001, "学生不存在"),
    STUDENT_ID_EXISTS(2002, "学号已存在"),
    STUDENT_NOT_REGISTERED(2003, "学生未注册"),
    STUDENT_ALREADY_REGISTERED(2004, "学生已注册"),
    INVITE_CODE_INVALID(2005, "邀请码无效"),
    INVITE_CODE_EXPIRED(2006, "邀请码已过期"),
    STUDENT_NAME_MISMATCH(2007, "学号与姓名不匹配"),
    
    // ========== 管理员相关 3000-3099 ==========
    ADMIN_NOT_FOUND(3001, "管理员不存在"),
    ADMIN_USERNAME_EXISTS(3002, "用户名已存在"),
    ADMIN_EMAIL_EXISTS(3003, "邮箱已存在"),
    ADMIN_PHONE_EXISTS(3004, "手机号已存在"),
    
    // ========== 宿舍相关 4000-4099 ==========
    BUILDING_NOT_FOUND(4001, "楼栋不存在"),
    ROOM_NOT_FOUND(4002, "房间不存在"),
    BED_NOT_FOUND(4003, "床位不存在"),
    BED_OCCUPIED(4004, "床位已被占用"),
    ROOM_FULL(4005, "房间已满"),
    
    // ========== 申请相关 5000-5099 ==========
    APPLICATION_NOT_FOUND(5001, "申请不存在"),
    APPLICATION_REVIEWED(5002, "申请已审核，无法重复操作"),
    
    // ========== 邮件相关 6000-6099 ==========
    EMAIL_SEND_FAILED(6001, "邮件发送失败"),
    VERIFICATION_CODE_INVALID(6002, "验证码无效"),
    VERIFICATION_CODE_EXPIRED(6003, "验证码已过期"),
    VERIFICATION_CODE_ERROR(6004, "验证码错误"),
    
    // ========== 文件相关 7000-7099 ==========
    FILE_UPLOAD_FAILED(7001, "文件上传失败"),
    FILE_TYPE_ERROR(7002, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(7003, "文件大小超过限制");
    
    /**
     * 状态码
     */
    private final Integer code;
    
    /**
     * 返回消息
     */
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
