/**
 * 操作日志API
 */
import request from '@/utils/request'

/**
 * 获取操作日志列表（当前用户）
 */
export function getOperationLogList(pageNum = 1, pageSize = 10) {
  return request({
    url: '/operation-logs',
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 获取所有操作日志（超级管理员）
 */
export function getAllOperationLogList(pageNum = 1, pageSize = 10) {
  return request({
    url: '/operation-logs/all',
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 记录操作日志
 */
export function logOperation(data) {
  return request({
    url: '/operation-logs',
    method: 'post',
    data
  })
}
