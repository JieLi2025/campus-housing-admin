/**
 * 宿舍分配相关 API
 */
import request from '@/utils/request'

/**
 * 获取分配记录列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.pageSize - 每页数量
 * @param {number} params.buildingId - 宿舍楼ID
 * @param {string} params.status - 分配状态
 * @param {string} params.keyword - 搜索关键字
 * @returns {Promise}
 */
export function getAllocationList(params) {
  return request({
    url: '/admin/allocations',
    method: 'get',
    params
  })
}

/**
 * 分配宿舍
 * @param {Object} data - 分配数据
 * @param {number} data.studentId - 学生ID
 * @param {number} data.roomId - 房间ID
 * @param {string} data.bedNumber - 床位号
 * @param {string} data.allocationDate - 分配日期
 * @param {string} data.allocationType - 分配方式
 * @param {string} data.remarks - 备注
 * @returns {Promise}
 */
export function allocateDorm(data) {
  return request({
    url: '/admin/allocations',
    method: 'post',
    data
  })
}

/**
 * 学生入住
 * @param {number} allocationId - 分配记录ID
 * @returns {Promise}
 */
export function checkIn(allocationId) {
  return request({
    url: `/admin/allocations/${allocationId}/check-in`,
    method: 'post'
  })
}

/**
 * 学生退宿
 * @param {number} allocationId - 分配记录ID
 * @returns {Promise}
 */
export function checkOut(allocationId) {
  return request({
    url: `/admin/allocations/${allocationId}/check-out`,
    method: 'post'
  })
}

/**
 * 取消分配
 * @param {number} allocationId - 分配记录ID
 * @returns {Promise}
 */
export function cancelAllocation(allocationId) {
  return request({
    url: `/admin/allocations/${allocationId}/cancel`,
    method: 'post'
  })
}

/**
 * 获取我的宿舍信息（学生端）
 * @returns {Promise}
 */
export function getMyDorm() {
  return request({
    url: '/student/dorm/my-dorm',
    method: 'get'
  })
}

/**
 * 获取指定学生的宿舍信息
 * @param {number} studentId - 学生ID
 * @returns {Promise}
 */
export function getStudentDorm(studentId) {
  return request({
    url: `/student/dorm/${studentId}`,
    method: 'get'
  })
}

// ========== 剩余处理阶段API ==========

/**
 * 智能分配学生
 * @param {Object} data - 分配请求
 * @param {Array<number>} data.studentIds - 学生ID列表（可选）
 * @param {Array<number>} data.buildingIds - 楼栋ID列表
 * @param {string} data.strategy - 分配策略（same_class/same_major/auto）
 * @param {boolean} data.allowCrossMajor - 是否允许跨专业
 * @param {boolean} data.allowCrossClass - 是否允许跨班级
 * @returns {Promise}
 */
export function intelligentAssign(data) {
  return request({
    url: '/admin/allocation/intelligent-assign',
    method: 'post',
    data
  })
}

/**
 * 手动分配学生到宿舍组
 * @param {Object} data - 分配请求
 * @param {number} data.studentId - 学生ID
 * @param {number} data.groupId - 宿舍组ID
 * @param {boolean} data.force - 是否强制分配
 * @returns {Promise}
 */
export function assignToGroup(data) {
  return request({
    url: '/admin/allocation/assign-to-group',
    method: 'post',
    data
  })
}

/**
 * 手动分配学生到房间
 * @param {Object} data - 分配请求
 * @param {Array<number>} data.studentIds - 学生ID列表
 * @param {number} data.roomId - 房间ID
 * @param {boolean} data.createNewGroup - 是否创建新宿舍组
 * @returns {Promise}
 */
export function assignToRoom(data) {
  return request({
    url: '/admin/allocation/assign-to-room',
    method: 'post',
    data
  })
}

/**
 * 按班级批量处理
 * @param {Object} params - 请求参数
 * @param {string} params.className - 班级名称
 * @param {string} params.buildingIds - 楼栋ID列表（逗号分隔）
 * @returns {Promise}
 */
export function batchProcessByClass(params) {
  return request({
    url: '/admin/allocation/batch-process-by-class',
    method: 'post',
    params
  })
}

/**
 * 验证分配结果
 * @param {Object} params - 请求参数
 * @param {string} params.buildingIds - 楼栋ID列表（逗号分隔）
 * @returns {Promise}
 */
export function validateAllocation(params) {
  return request({
    url: '/admin/allocation/validate',
    method: 'get',
    params
  })
}

/**
 * 完成分配，将宿舍信息同步到学生表
 * @param {Array<number>} buildingIds 楼栋ID列表（可选，如果不传则处理所有楼栋）
 * @returns {Promise}
 */
export function completeAllocation(buildingIds) {
  return request({
    url: '/admin/allocation/complete',
    method: 'post',
    params: buildingIds && buildingIds.length > 0 ? { buildingIds } : {}
  })
}
