/**
 * 申请管理API
 * 包括学生提交申请和管理员审批
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
import request from '@/utils/request'

// ==================== 管理员端API ====================

/**
 * 获取申请列表（管理员端）
 * @param {Object} data - 查询参数
 * @param {Number} data.currentPage - 当前页码
 * @param {Number} data.pageSize - 每页数量
 * @param {String} data.keyword - 搜索关键词（学号或姓名）
 * @param {String} data.type - 申请类型：TRANSFER/CHECKOUT
 * @param {String} data.status - 状态筛选：PENDING/APPROVED/REJECTED
 * @returns {Promise}
 */
export function getApplicationList(data) {
  return request({
    url: '/admin/applications/list',
    method: 'post',
    data
  })
}

/**
 * 获取申请详情
 * @param {Number} id - 申请ID
 * @returns {Promise}
 */
export function getApplicationDetail(id) {
  return request({
    url: `/admin/applications/${id}`,
    method: 'get'
  })
}

/**
 * 审批申请
 * @param {Number} id - 申请ID
 * @param {Object} data - 审批数据
 * @param {Boolean} data.approved - 是否通过（true-通过，false-拒绝）
 * @param {String} data.comment - 审批意见
 * @param {String} data.rejectReason - 拒绝原因（拒绝时必填）
 * @param {Number} data.assignedBuildingId - 分配的楼栋ID（调宿申请通过时）
 * @param {Number} data.assignedRoomId - 分配的房间ID（调宿申请通过时）
 * @param {String} data.assignedDormInfo - 分配的宿舍信息（调宿申请通过时）
 * @param {String} data.assignmentType - 分配方式：DIRECT/EXCHANGE
 * @param {Number} data.exchangeStudentId - 调换对象学生ID（调换时）
 * @returns {Promise}
 */
export function approveApplication(id, data) {
  return request({
    url: `/admin/applications/${id}/approve`,
    method: 'put',
    data
  })
}

/**
 * 删除申请（管理员）
 * @param {Number} id - 申请ID
 * @returns {Promise}
 */
export function deleteApplication(id) {
  return request({
    url: `/admin/applications/${id}`,
    method: 'delete'
  })
}

/**
 * 更新申请信息（管理员）
 * @param {Number} id - 申请ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export function updateApplication(id, data) {
  return request({
    url: `/admin/applications/${id}`,
    method: 'put',
    data
  })
}

/**
 * 获取待审核申请数量
 * @returns {Promise}
 */
export function getPendingCount() {
  return request({
    url: '/admin/applications/pending-count',
    method: 'get'
  })
}

// ==================== 学生端API ====================

/**
 * 提交申请（学生端）
 * @param {Object} data - 申请数据
 * @param {String} data.type - 申请类型：TRANSFER-调宿申请，CHECKOUT-退宿申请
 * @param {String} data.reason - 申请原因
 * @param {String} data.currentDormInfo - 当前宿舍信息
 * @param {String} data.targetDormInfo - 目标宿舍信息（可选）
 * @param {Number} data.targetRoomId - 目标房间ID（可选）
 * @param {String} data.contactPhone - 联系电话（退宿申请需要）
 * @param {String} data.allocationPhase - 分配阶段
 * @param {Boolean} data.wasLeader - 是否曾为舍长
 * @returns {Promise}
 */
export function submitApplication(data) {
  return request({
    url: '/student/applications',
    method: 'post',
    data
  })
}

/**
 * 获取我的申请列表（学生端）
 * @param {Number} currentPage - 当前页码
 * @param {Number} pageSize - 每页数量
 * @returns {Promise}
 */
export function getMyApplications(currentPage = 1, pageSize = 10) {
  return request({
    url: '/student/applications',
    method: 'get',
    params: {
      currentPage,
      pageSize
    }
  })
}

/**
 * 获取申请详情（学生端）
 * @param {Number} id - 申请ID
 * @returns {Promise}
 */
export function getMyApplicationDetail(id) {
  return request({
    url: `/student/applications/${id}`,
    method: 'get'
  })
}

/**
 * 撤回申请（学生端）
 * @param {Number} id - 申请ID
 * @returns {Promise}
 */
export function withdrawApplication(id) {
  return request({
    url: `/student/applications/${id}`,
    method: 'delete'
  })
}

/**
 * 修改申请（学生端，仅待审核状态可修改）
 * @param {Number} id - 申请ID
 * @param {Object} data - 申请数据
 * @param {String} data.reason - 申请原因
 * @param {String} data.targetDormInfo - 目标宿舍信息（可选）
 * @param {String} data.contactPhone - 联系电话（可选）
 * @returns {Promise}
 */
export function updateStudentApplication(id, data) {
  return request({
    url: `/student/applications/${id}`,
    method: 'put',
    data
  })
}
