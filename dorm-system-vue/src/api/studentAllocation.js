/**
 * 学生分配流程相关 API
 */
import request from '@/utils/request'

/**
 * 获取系统选房阶段
 * @returns {Promise}
 */
export function getSystemPhase() {
  return request({
    url: '/student/allocation-flow/phase',
    method: 'get'
  })
}

/**
 * 获取分配区域信息
 * @returns {Promise}
 */
export function getAssignedArea() {
  return request({
    url: '/student/allocation-flow/assigned-area',
    method: 'get'
  })
}

/**
 * 获取当前团队信息
 * @returns {Promise}
 */
export function getCurrentTeam() {
  return request({
    url: '/student/allocation-flow/team',
    method: 'get'
  })
}

/**
 * 发送室友邀请
 * @param {Object} data
 * @param {string} data.studentId - 被邀请学生的学号
 * @param {number} data.receiverId - 被邀请学生的ID
 * @param {string} data.message - 邀请留言
 * @returns {Promise}
 */
export function sendInvite(data) {
  return request({
    url: '/student/allocation-flow/invites/send',
    method: 'post',
    data
  })
}

/**
 * 撤回邀请
 * @param {number} inviteId - 邀请ID
 * @returns {Promise}
 */
export function cancelInvite(inviteId) {
  return request({
    url: `/student/allocation-flow/invites/${inviteId}`,
    method: 'delete'
  })
}

/**
 * 获取收到的邀请列表
 * @returns {Promise}
 */
export function getReceivedInvites() {
  return request({
    url: '/student/allocation-flow/invites/received',
    method: 'get'
  })
}

/**
 * 获取已发送的邀请列表
 * @returns {Promise}
 */
export function getSentInvites() {
  return request({
    url: '/student/allocation-flow/invites/sent',
    method: 'get'
  })
}

/**
 * 接受邀请
 * @param {number} inviteId - 邀请ID
 * @returns {Promise}
 */
export function acceptInvite(inviteId) {
  return request({
    url: `/student/allocation-flow/invites/${inviteId}/accept`,
    method: 'post'
  })
}

/**
 * 拒绝邀请
 * @param {number} inviteId - 邀请ID
 * @returns {Promise}
 */
export function rejectInvite(inviteId) {
  return request({
    url: `/student/allocation-flow/invites/${inviteId}/reject`,
    method: 'post'
  })
}

/**
 * 搜索同班同学
 * @param {string} keyword - 搜索关键字
 * @returns {Promise}
 */
export function searchClassmates(keyword) {
  return request({
    url: '/student/allocation-flow/classmates/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 移除团队成员（仅舍长）
 * @param {number} memberId - 成员ID
 * @returns {Promise}
 */
export function removeMember(memberId) {
  return request({
    url: `/student/allocation-flow/team/members/${memberId}`,
    method: 'delete'
  })
}

/**
 * 转让舍长
 * @param {number} newLeaderId - 新舍长ID
 * @returns {Promise}
 */
export function transferLeadership(newLeaderId) {
  return request({
    url: '/student/allocation-flow/team/transfer-leadership',
    method: 'post',
    params: { newLeaderId }
  })
}

/**
 * 退出团队
 * @returns {Promise}
 */
export function leaveTeam() {
  return request({
    url: '/student/allocation-flow/team/leave',
    method: 'post'
  })
}

/**
 * 转让舍长并离开团队（用于舍长调宿申请）
 * @param {number} newLeaderId - 新舍长ID（student表的id）
 * @returns {Promise}
 */
export function transferLeaderAndLeave(newLeaderId) {
  return request({
    url: '/student/allocation-flow/team/transfer-and-leave',
    method: 'post',
    params: { newLeaderId }
  })
}

/**
 * 解散宿舍组并离开（用于舍长是唯一成员时的调宿申请）
 * @returns {Promise}
 */
export function disbandGroupAndLeave() {
  return request({
    url: '/student/allocation-flow/team/disband-and-leave',
    method: 'post'
  })
}

/**
 * 获取可选房间列表
 * @returns {Promise}
 */
export function getAvailableRooms() {
  return request({
    url: '/student/allocation-flow/available-rooms',
    method: 'get'
  })
}

/**
 * 确认房间选择（舍长操作）
 * @param {Object} data
 * @param {number} data.roomId - 房间ID
 * @param {number} data.buildingId - 楼栋ID
 * @param {number} data.groupId - 宿舍组ID
 * @returns {Promise}
 */
export function confirmRoomSelection(data) {
  return request({
    url: '/student/allocation-flow/select-room',
    method: 'post',
    data
  })
}

/**
 * 获取分配结果
 * @returns {Promise}
 */
export function getAllocationResult() {
  return request({
    url: '/student/allocation-flow/allocation-result',
    method: 'get'
  })
}

/**
 * 提交调宿申请
 * @param {Object} data
 * @param {string} data.transferType - 申请类型（room/roommate）
 * @param {string} data.targetBuilding - 目标楼栋
 * @param {string} data.targetRoom - 目标房间号
 * @param {string} data.reason - 申请原因
 * @param {string} data.description - 详细说明
 * @param {string} data.allocationPhase - 分配阶段
 * @param {boolean} data.isLeader - 是否为舍长
 * @param {string} data.contactPhone - 联系电话
 * @returns {Promise}
 */
export function submitTransferApplication(data) {
  return request({
    url: '/student/allocation-flow/transfer-application',
    method: 'post',
    data
  })
}

/**
 * 获取申请历史
 * @returns {Promise}
 */
export function getApplicationHistory() {
  return request({
    url: '/student/allocation-flow/transfer-applications',
    method: 'get'
  })
}


/**
 * 取消房间选择（舍长重新选房）
 * @returns {Promise}
 */
export function cancelRoomSelection() {
  return request({
    url: '/student/allocation-flow/cancel-room-selection',
    method: 'post'
  })
}
