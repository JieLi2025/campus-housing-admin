/**
 * 公告管理API
 * 包括管理员发布公告和学生查看公告
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
import request from '@/utils/request'

// ==================== 管理员端API ====================

/**
 * 创建公告
 * @param {Object} data - 公告数据
 * @param {String} data.title - 公告标题
 * @param {String} data.subtitle - 公告副标题（可选）
 * @param {String} data.content - 公告内容
 * @param {String} data.extraPoints - 额外要点（可选）
 * @param {String} data.buildingType - 目标楼栋类型（可选）
 * @param {Array<Number>} data.buildingIds - 目标楼栋ID列表（可选，为空表示全部）
 * @param {String} data.expireDate - 过期时间（可选）
 * @param {Array<String>} data.displayOptions - 展示选项
 * @param {String} data.status - 状态：DRAFT-草稿，PUBLISHED-已发布
 * @returns {Promise}
 */
export function createAnnouncement(data) {
  return request({
    url: '/admin/announcements',
    method: 'post',
    data
  })
}

/**
 * 获取公告列表（管理员端）
 * @param {Object} params - 查询参数
 * @param {Number} params.currentPage - 当前页码
 * @param {Number} params.pageSize - 每页数量
 * @param {String} params.keyword - 搜索关键词（标题或内容）
 * @param {String} params.status - 状态筛选：DRAFT/PUBLISHED/ARCHIVED
 * @returns {Promise}
 */
export function getAnnouncementList(params) {
  return request({
    url: '/admin/announcements',
    method: 'get',
    params
  })
}

/**
 * 获取公告详情（管理员端）
 * @param {Number} id - 公告ID
 * @returns {Promise}
 */
export function getAnnouncementDetail(id) {
  return request({
    url: `/admin/announcements/${id}`,
    method: 'get'
  })
}

/**
 * 更新公告
 * @param {Number} id - 公告ID
 * @param {Object} data - 公告数据
 * @returns {Promise}
 */
export function updateAnnouncement(id, data) {
  return request({
    url: `/admin/announcements/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除公告
 * @param {Number} id - 公告ID
 * @returns {Promise}
 */
export function deleteAnnouncement(id) {
  return request({
    url: `/admin/announcements/${id}`,
    method: 'delete'
  })
}

/**
 * 发布公告
 * @param {Number} id - 公告ID
 * @returns {Promise}
 */
export function publishAnnouncement(id) {
  return request({
    url: `/admin/announcements/${id}/publish`,
    method: 'put'
  })
}

/**
 * 归档公告
 * @param {Number} id - 公告ID
 * @returns {Promise}
 */
export function archiveAnnouncement(id) {
  return request({
    url: `/admin/announcements/${id}/archive`,
    method: 'put'
  })
}

// ==================== 学生端API ====================

/**
 * 获取公告列表（学生端）
 * @param {Object} params - 查询参数
 * @param {Number} params.currentPage - 当前页码
 * @param {Number} params.pageSize - 每页数量
 * @param {Number} params.buildingId - 楼栋ID筛选（可选）
 * @param {Boolean} params.onlyValid - 是否只查询未过期的（默认true）
 * @returns {Promise}
 */
export function getStudentAnnouncementList(params) {
  return request({
    url: '/student/announcements',
    method: 'get',
    params
  })
}

/**
 * 获取公告详情（学生端，自动增加浏览数）
 * @param {Number} id - 公告ID
 * @returns {Promise}
 */
export function getStudentAnnouncementDetail(id) {
  return request({
    url: `/student/announcements/${id}`,
    method: 'get'
  })
}

/**
 * 获取最新公告（已废弃，登录弹窗功能已移除）
 * @deprecated 此功能已移除，保留接口兼容性
 * @param {Number} buildingId - 楼栋ID（可选）
 * @returns {Promise} 始终返回 null
 */
export function getLatestAnnouncement(buildingId) {
  // 登录弹窗功能已移除，直接返回空结果
  return Promise.resolve({ code: 200, data: null })
}
