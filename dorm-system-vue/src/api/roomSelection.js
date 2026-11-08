import request from '@/utils/request'

/**
 * 获取房间选择配置
 */
export function getRoomSelectionConfig() {
  return request({
    url: '/admin/room-selection/config',
    method: 'get'
  })
}

/**
 * 开放房间选择
 * @param {Object} data - { deadline, batchName, description, adminUsername }
 */
export function openRoomSelection(data) {
  return request({
    url: '/admin/room-selection/open',
    method: 'post',
    data
  })
}

/**
 * 关闭房间选择
 * @param {string} adminUsername - 管理员用户名
 */
export function closeRoomSelection(adminUsername) {
  return request({
    url: '/admin/room-selection/close',
    method: 'post',
    data: { adminUsername }
  })
}

/**
 * 修改截止时间
 * @param {Object} data - { deadline, adminUsername }
 */
export function updateDeadline(data) {
  return request({
    url: '/admin/room-selection/deadline',
    method: 'put',
    data
  })
}

/**
 * 获取选择记录列表
 * @param {Object} params - { currentPage, pageSize, keyword, status }
 */
export function getRoomSelectionRecords(params) {
  return request({
    url: '/admin/room-selection/records',
    method: 'get',
    params
  })
}

/**
 * 创建选择记录（管理员代选）
 * @param {Object} data - 选择记录数据
 */
export function createSelectionRecord(data) {
  return request({
    url: '/admin/room-selection/records',
    method: 'post',
    data
  })
}

/**
 * 确认选择记录
 * @param {number} recordId - 记录ID
 * @param {string} adminUsername - 管理员用户名
 */
export function confirmSelectionRecord(recordId, adminUsername) {
  return request({
    url: `/admin/room-selection/records/${recordId}/confirm`,
    method: 'put',
    data: { adminUsername }
  })
}

/**
 * 取消选择记录
 * @param {number} recordId - 记录ID
 * @param {string} reason - 取消原因
 * @param {string} adminUsername - 管理员用户名
 */
export function cancelSelectionRecord(recordId, reason, adminUsername) {
  return request({
    url: `/admin/room-selection/records/${recordId}/cancel`,
    method: 'put',
    data: { reason, adminUsername }
  })
}

/**
 * 获取统计数据
 */
export function getRoomSelectionStatistics() {
  return request({
    url: '/admin/room-selection/statistics',
    method: 'get'
  })
}

/**
 * 切换选房阶段（全局，影响所有楼栋）
 * @param {string} newPhase 新阶段
 * @deprecated 建议使用 switchBuildingPhase 按楼栋切换
 */
export function switchAllocationPhase(newPhase) {
  return request({
    url: '/admin/room-selection/switch-phase',
    method: 'post',
    params: { newPhase }
  })
}

/**
 * 按楼栋切换选房阶段
 * @param {number} buildingId 楼栋ID
 * @param {string} newPhase 新阶段
 */
export function switchBuildingPhase(buildingId, newPhase) {
  return request({
    url: `/admin/room-selection/building/${buildingId}/switch-phase`,
    method: 'post',
    params: { newPhase }
  })
}


