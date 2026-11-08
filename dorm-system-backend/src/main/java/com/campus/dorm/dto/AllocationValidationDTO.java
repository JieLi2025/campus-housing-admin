package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 分配验证结果DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分配验证结果")
public class AllocationValidationDTO {
    
    @Schema(description = "是否验证通过")
    private Boolean isValid;
    
    @Schema(description = "完成率（百分比）")
    private Double completionRate;
    
    @Schema(description = "总学生数")
    private Integer totalStudents;
    
    @Schema(description = "已分配学生数")
    private Integer assignedStudents;
    
    @Schema(description = "未分配学生数")
    private Integer unassignedStudents;
    
    @Schema(description = "问题列表")
    private List<String> issues;
    
    @Schema(description = "警告列表")
    private List<String> warnings;
    
    public void addIssue(String issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }
    
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
}
