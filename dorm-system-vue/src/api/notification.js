/**
 * 消息通知API
 */
import request from '@/utils/request'

/**
 * 获取通知列表（分页）
 */
export function getNotificationList(pageNum = 1, pageSize = 10) {
  // Support object parameter style (consistent with other APIs)
  if (typeof pageNum === 'object') {
    const params = pageNum
    return request({
      url: '/notifications',
      method: 'get',
      params: {
        pageNum: params.currentPage || params.pageNum || 1,
        pageSize: params.pageSize || 10
      }
    })
  }

  return request({
    url: '/notifications',
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 获取未读通知数量
 */
export function getUnreadCount() {
  return request({
    url: '/notifications/unread-count',
    method: 'get'
  })
}

/**
 * 创建通知
 */
export function createNotification(data) {
  return request({
    url: '/notifications',
    method: 'post',
    data
  })
}

/**
 * 更新通知
 */
export function updateNotification(id, data) {
  return request({
    url: `/notifications/${id}`,
    method: 'put',
    data
  })
}

/**
 * 标记为已读
 */
export function markAsRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'put'
  })
}

/**
 * 全部标记为已读
 */
export function markAllAsRead() {
  return request({
    url: '/notifications/mark-all-read',
    method: 'put'
  })
}

/**
 * 删除通知
 */
export function deleteNotification(id) {
  return request({
    url: `/notifications/${id}`,
    method: 'delete'
  })
}

/**
 * 获取管理员通知列表（带类型过滤）
 * @param {Object} params - 查询参数
 * @param {string} params.type - 通知类型（可选）：system/application/visitor
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页数量
 */
export function getAdminNotifications(params = {}) {
  return request({
    url: '/notifications/admin',
    method: 'get',
    params: {
      type: params.type || null,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20
    }
  })
}

/**
 * 获取系统通知列表（超级管理员发送的通知）
 * @param {number} pageNum - 页码
 * @param {number} pageSize - 每页数量
 */
export function getSystemNotifications(pageNum = 1, pageSize = 100) {
  return request({
    url: '/notifications/system',
    method: 'get',
    params: { pageNum, pageSize }
  })
}

