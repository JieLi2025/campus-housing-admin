package com.campus.dorm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 分配结果DTO
 * 
 * @author Campus Dorm System
 * @date 2025-12-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分配结果")
public class AllocationResultDTO {
    
    @Schema(description = "成功分配的学生数量")
    private Integer successCount;
    
    @Schema(description = "失败的学生数量")
    private Integer failedCount;
    
    @Schema(description = "跳过的学生数量")
    private Integer skippedCount;
    
    @Schema(description = "成功分配的学生详情")
    private List<StudentAssignmentDetail> successDetails;
    
    @Schema(description = "失败的学生详情")
    private List<StudentAssignmentDetail> failedDetails;
    
    @Schema(description = "警告信息")
    private List<String> warnings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAssignmentDetail {
        @Schema(description = "学生ID")
        private Long studentId;
        
        @Schema(description = "学生姓名")
        private String studentName;
        
        @Schema(description = "学号")
        private String studentNumber;
        
        @Schema(description = "分配到的宿舍组ID")
        private Long groupId;
        
        @Schema(description = "分配到的宿舍组名称")
        private String groupName;
        
        @Schema(description = "分配到的房间ID")
        private Long roomId;
        
        @Schema(description = "分配到的房间号")
        private String roomNumber;
        
        @Schema(description = "失败原因")
        private String failureReason;
    }
    
    public void addSuccess(StudentAssignmentDetail detail) {
        if (successDetails == null) {
            successDetails = new ArrayList<>();
        }
        successDetails.add(detail);
        successCount = successDetails.size();
    }
    
    public void addFailure(StudentAssignmentDetail detail) {
        if (failedDetails == null) {
            failedDetails = new ArrayList<>();
        }
        failedDetails.add(detail);
        failedCount = failedDetails.size();
    }
    
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
    
    public void incrementSkippedCount() {
        if (skippedCount == null) {
            skippedCount = 0;
        }
        skippedCount++;
    }
}
