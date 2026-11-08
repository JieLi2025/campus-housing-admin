package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 学生信息更新DTO
 * 
 * @author Campus Dorm System
 * @date 2024-10-24
 */
@Data
@Schema(description = "学生信息更新DTO")
public class StudentUpdateDTO {
    
    @NotNull(message = "学生ID不能为空")
    @Schema(description = "学生ID", required = true, example = "1")
    private Long id;
    
    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", required = true, example = "张三")
    private String realName;
    
    @Pattern(regexp = "^(男|女)$", message = "性别只能是男或女")
    @Schema(description = "性别", required = true, example = "男", allowableValues = {"男", "女"})
    private String gender;
    
    @NotBlank(message = "专业不能为空")
    @Schema(description = "专业", required = true, example = "计算机科学与技术")
    private String major;
    
    @NotBlank(message = "年级不能为空")
    @Schema(description = "年级", required = true, example = "2023级")
    private String grade;
    
    @NotBlank(message = "班级不能为空")
    @Schema(description = "班级", required = true, example = "计科2301班")
    private String className;
    
    @Schema(description = "个人简介", example = "热爱编程，喜欢运动")
    private String bio;
    
    @Schema(description = "个人标签（JSON数组）", example = "[\"运动\", \"音乐\"]")
    private String tags;
}

