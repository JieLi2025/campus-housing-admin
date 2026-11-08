package com.campus.dorm.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 学生信息验证DTO
 * 用于公开API的学生信息验证
 *
 * @author Campus Dorm System
 * @date 2025-11-08
 */
@Data
public class StudentValidationDTO {
    
    /**
     * 学生姓名
     */
    @NotBlank(message = "学生姓名不能为空")
    @Size(min = 2, max = 10, message = "学生姓名长度应在2-10个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z\\s]+$", message = "学生姓名只能包含中文、英文字母和空格")
    private String studentName;
    
    /**
     * 楼栋ID（可选）
     */
    private Long buildingId;
    
    /**
     * 宿舍号
     */
    @NotBlank(message = "宿舍号不能为空")
    @Size(min = 1, max = 20, message = "宿舍号长度应在1-20个字符之间")
    @Pattern(regexp = "^[0-9\\-\\u4e00-\\u9fa5]+$", message = "宿舍号格式不正确")
    private String studentRoom;
}

