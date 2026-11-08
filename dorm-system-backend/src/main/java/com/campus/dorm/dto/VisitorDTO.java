package com.campus.dorm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 访客预约DTO（学生提交预约）
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
@Data
public class VisitorDTO {
    
    /**
     * 访客姓名
     */
    @NotBlank(message = "访客姓名不能为空")
    private String visitorName;
    
    /**
     * 访客电话
     */
    @NotBlank(message = "访客电话不能为空")
    private String visitorPhone;
    
    /**
     * 访客身份证号（可选）
     */
    private String visitorIdCard;
    
    /**
     * 与学生关系
     */
    private String relationship;
    
    /**
     * 来访事由
     */
    @NotBlank(message = "来访事由不能为空")
    private String visitReason;
    
    /**
     * 来访日期
     */
    @NotNull(message = "来访日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
    
    /**
     * 来访时段
     */
    private String visitTime;
    
    /**
     * 预计时长（小时）
     */
    private Integer duration;
    
    /**
     * 学生联系电话
     */
    private String studentPhone;
    
    /**
     * 学生ID（管理员创建时可选）
     */
    private Long studentId;
    
    /**
     * 学生姓名（管理员创建时可选）
     */
    private String studentName;
    
    /**
     * 学号（管理员创建时可选）
     */
    private String studentNumber;
    
    /**
     * 学生宿舍（管理员创建时可选）
     */
    private String studentRoom;
    
    /**
     * 楼栋ID（用于验证学生是否在该楼栋）
     */
    private Long buildingId;
    
    /**
     * 备注
     */
    private String remarks;
}

