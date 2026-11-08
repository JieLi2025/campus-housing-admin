package com.campus.dorm.controller;

import com.campus.dorm.common.Result;
import com.campus.dorm.dto.RoomSelectionDTO;
import com.campus.dorm.dto.TeamInviteDTO;
import com.campus.dorm.dto.TransferApplicationDTO;
import com.campus.dorm.entity.Application;
import com.campus.dorm.entity.RoommateInvite;
import com.campus.dorm.service.StudentAllocationFlowService;
import com.campus.dorm.util.JwtUtil;
import com.campus.dorm.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 学生分配流程Controller
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Slf4j
@RestController
@RequestMapping("/student/allocation-flow")
@RequiredArgsConstructor
@Tag(name = "学生分配流程", description = "学生端宿舍分配流程相关接口")
public class StudentAllocationFlowController {
    
    private final StudentAllocationFlowService allocationFlowService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;
    
    /**
     * 获取当前用户ID（从JWT Token）
     */
    private Long getUserId() {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 获取系统选房阶段（按学生所属楼栋）
     */
    @GetMapping("/phase")
    @Operation(summary = "获取系统选房阶段", description = "获取当前学生所属楼栋的选房阶段配置")
    public Result<?> getSystemPhase() {
        Long userId = getUserId();
        
        // 先检查是否需要自动切换该学生所属楼栋的阶段
        allocationFlowService.checkAndAutoSwitchStudentPhase(userId);
        
        // 获取该学生所属楼栋的阶段配置
        return allocationFlowService.getStudentPhase(userId);
    }
    
    // ========== 步骤1：组建团队 ==========
    
    /**
     * 获取分配区域信息
     */
    @GetMapping("/assigned-area")
    @Operation(summary = "获取分配区域信息", description = "获取学生被分配的楼栋、楼层、房间类型等信息")
    public Result<AssignedAreaVO> getAssignedArea() {
        Long studentId = getUserId();
        return allocationFlowService.getAssignedArea(studentId);
    }
    
    /**
     * 获取当前团队信息
     */
    @GetMapping("/team")
    @Operation(summary = "获取当前团队信息", description = "获取学生当前所在的宿舍团队信息，如不存在则自动创建")
    public Result<TeamInfoVO> getTeam() {
        Long studentId = getUserId();
        return allocationFlowService.getOrCreateTeam(studentId);
    }
    
    /**
     * 发送室友邀请
     */
    @PostMapping("/invites/send")
    @Operation(summary = "发送室友邀请", description = "向其他学生发送室友邀请")
    public Result<?> sendInvite(@RequestBody TeamInviteDTO dto) {
        Long senderId = getUserId();
        return allocationFlowService.sendInvite(senderId, dto);
    }
    
    /**
     * 撤回邀请
     */
    @DeleteMapping("/invites/{inviteId}")
    @Operation(summary = "撤回邀请", description = "撤回已发送的邀请")
    public Result<?> cancelInvite(@PathVariable Long inviteId) {
        Long senderId = getUserId();
        return allocationFlowService.cancelInvite(senderId, inviteId);
    }
    
    /**
     * 获取收到的邀请列表
     */
    @GetMapping("/invites/received")
    @Operation(summary = "获取收到的邀请", description = "获取当前学生收到的室友邀请列表")
    public Result<List<RoommateInvite>> getReceivedInvites() {
        Long studentId = getUserId();
        return allocationFlowService.getReceivedInvites(studentId);
    }
    
    /**
     * 获取已发送的邀请列表
     */
    @GetMapping("/invites/sent")
    @Operation(summary = "获取已发送的邀请", description = "获取当前学生已发送的邀请列表")
    public Result<List<RoommateInvite>> getSentInvites() {
        Long studentId = getUserId();
        return allocationFlowService.getSentInvites(studentId);
    }
    
    /**
     * 接受邀请
     */
    @PostMapping("/invites/{inviteId}/accept")
    @Operation(summary = "接受邀请", description = "接受室友邀请并加入团队")
    public Result<?> acceptInvite(@PathVariable Long inviteId) {
        Long studentId = getUserId();
        return allocationFlowService.acceptInvite(studentId, inviteId);
    }
    
    /**
     * 拒绝邀请
     */
    @PostMapping("/invites/{inviteId}/reject")
    @Operation(summary = "拒绝邀请", description = "拒绝室友邀请")
    public Result<?> rejectInvite(@PathVariable Long inviteId) {
        Long studentId = getUserId();
        return allocationFlowService.rejectInvite(studentId, inviteId);
    }
    
    /**
     * 搜索同班同学
     */
    @GetMapping("/classmates/search")
    @Operation(summary = "搜索同班同学", description = "根据关键字搜索同专业同班级的学生")
    public Result<List<StudentVO>> searchClassmates(@RequestParam(required = false) String keyword) {
        Long studentId = getUserId();
        return allocationFlowService.searchClassmates(studentId, keyword);
    }
    
    /**
     * 移除团队成员（仅舍长）
     */
    @DeleteMapping("/team/members/{memberId}")
    @Operation(summary = "移除团队成员", description = "舍长移除团队中的成员")
    public Result<?> removeMember(@PathVariable Long memberId) {
        Long leaderId = getUserId();
        return allocationFlowService.removeMember(leaderId, memberId);
    }
    
    /**
     * 转让舍长
     */
    @PostMapping("/team/transfer-leadership")
    @Operation(summary = "转让舍长", description = "将舍长权限转让给团队中的其他成员")
    public Result<?> transferLeadership(@RequestParam Long newLeaderId) {
        Long currentLeaderId = getUserId();
        return allocationFlowService.transferLeadership(currentLeaderId, newLeaderId);
    }
    
    /**
     * 退出团队
     */
    @PostMapping("/team/leave")
    @Operation(summary = "退出团队", description = "普通成员退出当前团队")
    public Result<?> leaveTeam() {
        Long studentId = getUserId();
        return allocationFlowService.leaveTeam(studentId);
    }
    
    /**
     * 转让舍长并离开团队（用于舍长调宿申请）
     */
    @PostMapping("/team/transfer-and-leave")
    @Operation(summary = "转让舍长并离开", description = "舍长转让权限给其他成员后离开团队，用于调宿申请场景")
    public Result<?> transferLeaderAndLeave(@RequestParam Long newLeaderId) {
        Long currentLeaderId = getUserId();
        return allocationFlowService.transferLeaderAndLeave(currentLeaderId, newLeaderId);
    }
    
    /**
     * 解散宿舍组并离开（用于舍长是唯一成员时的调宿申请）
     */
    @PostMapping("/team/disband-and-leave")
    @Operation(summary = "解散宿舍组并离开", description = "舍长解散宿舍组并离开，用于舍长是唯一成员时的调宿申请场景")
    public Result<?> disbandGroupAndLeave() {
        Long userId = getUserId();
        return allocationFlowService.disbandGroupAndLeave(userId);
    }
    
    // ========== 步骤2：选择房间 ==========
    
    /**
     * 获取可选房间列表
     */
    @GetMapping("/available-rooms")
    @Operation(summary = "获取可选房间列表", description = "获取学生可以选择的宿舍房间列表")
    public Result<List<DormRoomVO>> getAvailableRooms() {
        Long studentId = getUserId();
        return allocationFlowService.getAvailableRooms(studentId);
    }
    
    /**
     * 确认房间选择（舍长操作）
     */
    @PostMapping("/select-room")
    @Operation(summary = "确认房间选择", description = "舍长为团队选择宿舍房间")
    public Result<?> confirmRoomSelection(@RequestBody RoomSelectionDTO dto) {
        Long leaderId = getUserId();
        return allocationFlowService.confirmRoomSelection(leaderId, dto);
    }
    
    /**
     * 取消房间选择（舍长重新选房）
     */
    @PostMapping("/cancel-room-selection")
    @Operation(summary = "取消房间选择", description = "舍长取消当前房间选择，释放房间以便重新选择")
    public Result<?> cancelRoomSelection() {
        Long leaderId = getUserId();
        return allocationFlowService.cancelRoomSelection(leaderId);
    }
    
    // ========== 步骤3：查看结果 ==========
    
    /**
     * 获取分配结果
     */
    @GetMapping("/allocation-result")
    @Operation(summary = "获取分配结果", description = "获取学生的宿舍分配结果")
    public Result<AllocationResultVO> getAllocationResult() {
        Long studentId = getUserId();
        return allocationFlowService.getAllocationResult(studentId);
    }
    
    /**
     * 提交调宿申请
     */
    @PostMapping("/transfer-application")
    @Operation(summary = "提交调宿申请", description = "学生提交宿舍调换申请")
    public Result<?> submitTransferApplication(@RequestBody TransferApplicationDTO dto) {
        Long studentId = getUserId();
        return allocationFlowService.submitTransferApplication(studentId, dto);
    }
    
    /**
     * 获取申请历史
     */
    @GetMapping("/transfer-applications")
    @Operation(summary = "获取申请历史", description = "获取学生的调宿申请历史记录")
    public Result<List<Application>> getApplicationHistory() {
        Long studentId = getUserId();
        return allocationFlowService.getApplicationHistory(studentId);
    }
}

