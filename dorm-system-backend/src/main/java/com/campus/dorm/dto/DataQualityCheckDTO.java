package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量检查结果DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据质量检查结果")
public class DataQualityCheckDTO {
    
    @Schema(description = "是否通过检查")
    private Boolean passed;
    
    @Schema(description = "总宿舍组数")
    private Integer totalGroups;
    
    @Schema(description = "总学生数")
    private Integer totalStudents;
    
    @Schema(description = "已分配学生数")
    private Integer assignedStudents;
    
    @Schema(description = "未分配学生数")
    private Integer unassignedStudents;
    
    @Schema(description = "男女混住宿舍组数")
    private Integer mixedGenderGroups;
    
    @Schema(description = "不同班级混住宿舍组数")
    private Integer mixedClassGroups;
    
    @Schema(description = "不在正确区域的宿舍组数")
    private Integer wrongAreaGroups;
    
    @Schema(description = "重复分配的学生数")
    private Integer duplicateAssignments;
    
    @Schema(description = "未分配房间的宿舍组数")
    private Integer groupsWithoutRoom;
    
    @Schema(description = "未满员的宿舍组数")
    private Integer incompleteGroups;
    
    @Schema(description = "人数不一致的宿舍组数")
    private Integer inconsistentCountGroups;
    
    @Schema(description = "问题详情列表")
    private List<IssueDetail> issues;
    
    @Schema(description = "警告列表")
    private List<String> warnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueDetail {
        @Schema(description = "问题类型")
        private String issueType;
        
        @Schema(description = "严重程度：critical(严重), warning(警告), info(信息)")
        private String severity;
        
        @Schema(description = "宿舍组ID")
        private Long groupId;
        
        @Schema(description = "宿舍组名称")
        private String groupName;
        
        @Schema(description = "问题描述")
        private String description;
        
        @Schema(description = "相关数据")
        private String details;
    }
    
    public void addIssue(IssueDetail issue) {
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
