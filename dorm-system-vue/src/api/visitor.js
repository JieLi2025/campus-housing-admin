/**
 * 访客管理API
 * 包括管理员审批访客和学生预约访客
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
import request from '@/utils/request'

// ==================== 管理员端API ====================

/**
 * 获取访客列表（管理员端）
 * @param {Object} params - 查询参数
 * @param {Number} params.currentPage - 当前页码
 * @param {Number} params.pageSize - 每页数量
 * @param {String} params.keyword - 搜索关键词（访客姓名、学生姓名或学号）
 * @param {String} params.status - 状态筛选：PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED
 * @param {String} params.visitDate - 来访日期筛选（yyyy-MM-dd）
 * @returns {Promise}
 */
export function getVisitorList(params) {
  return request({
    url: '/admin/visitors',
    method: 'get',
    params
  })
}

/**
 * 获取访客详情（管理员端）
 * @param {Number} id - 访客ID
 * @returns {Promise}
 */
export function getVisitorDetail(id) {
  return request({
    url: `/admin/visitors/${id}`,
    method: 'get'
  })
}

/**
 * 审批访客
 * @param {Number} id - 访客ID
 * @param {Object} data - 审批数据
 * @param {Boolean} data.approved - 是否批准（true-批准/false-拒绝）
 * @param {String} data.adminComment - 管理员审批意见（可选）
 * @param {String} data.rejectReason - 拒绝原因（拒绝时必填）
 * @returns {Promise}
 */
export function approveVisitor(id, data) {
  return request({
    url: `/admin/visitors/${id}/approve`,
    method: 'put',
    data
  })
}

/**
 * 访客签到
 * @param {Number} id - 访客ID
 * @returns {Promise}
 */
export function checkInVisitor(id) {
  return request({
    url: `/admin/visitors/${id}/checkin`,
    method: 'post'
  })
}

/**
 * 访客签退
 * @param {Number} id - 访客ID
 * @returns {Promise}
 */
export function checkOutVisitor(id) {
  return request({
    url: `/admin/visitors/${id}/checkout`,
    method: 'post'
  })
}

/**
 * 删除访客记录
 * @param {Number} id - 访客ID
 * @returns {Promise}
 */
export function deleteVisitor(id) {
  return request({
    url: `/admin/visitors/${id}`,
    method: 'delete'
  })
}

/**
 * 获取访客统计
 * @returns {Promise}
 */
export function getVisitorStatistics() {
  return request({
    url: '/admin/visitors/statistics',
    method: 'get'
  })
}

/**
 * 创建访客记录（管理员端）
 * @param {Object} data - 访客数据
 * @param {String} data.visitorName - 访客姓名
 * @param {String} data.visitorPhone - 访客电话
 * @param {String} data.visitorIdCard - 访客身份证号（可选）
 * @param {String} data.visitReason - 来访事由
 * @param {String} data.visitDate - 来访日期（yyyy-MM-dd）
 * @param {String} data.visitTime - 来访时段（可选）
 * @param {String} data.studentRoom - 访问宿舍（可选）
 * @param {String} data.remarks - 备注（可选）
 * @returns {Promise}
 */
export function createVisitor(data) {
  return request({
    url: '/admin/visitors',
    method: 'post',
    data
  })
}

// ==================== 公开API（无需认证）====================

/**
 * 验证学生信息（公开API，无需认证）
 * @param {String} studentName - 学生姓名
 * @param {Number} buildingId - 楼栋ID（可选）
 * @param {String} studentRoom - 宿舍号
 * @returns {Promise}
 */
export function validateStudentInfo(studentName, buildingId, studentRoom) {
  return request({
    url: '/public/visitor/validate-student',
    method: 'post',
    data: {
      studentName,
      buildingId,
      studentRoom
    }
  })
}

/**
 * 访客登记（公开API，无需认证）
 * 用于访客在登录页面提交登记信息
 * @param {Object} data - 访客数据
 * @param {String} data.visitorName - 访客姓名
 * @param {String} data.visitorPhone - 访客电话
 * @param {String} data.visitorIdCard - 访客身份证号（可选）
 * @param {String} data.visitReason - 来访事由
 * @param {String} data.visitDate - 来访日期（yyyy-MM-dd）
 * @param {String} data.visitTime - 来访时段（可选）
 * @param {String} data.studentName - 被访学生姓名
 * @param {String} data.studentRoom - 访问宿舍
 * @param {Number} data.buildingId - 楼栋ID
 * @returns {Promise}
 */
export function registerVisitor(data) {
  return request({
    url: '/public/visitor/register',
    method: 'post',
    data
  })
}

/**
 * 查询访客状态（公开API，无需认证）
 * 用于访客查询自己的登记状态
 * @param {String} visitorIdCard - 访客身份证号
 * @param {String} visitDate - 来访日期（yyyy-MM-dd）
 * @returns {Promise}
 */
export function getVisitorStatus(visitorIdCard, visitDate) {
  return request({
    url: '/public/visitor/status',
    method: 'get',
    params: {
      visitorIdCard,
      visitDate
    }
  })
}


