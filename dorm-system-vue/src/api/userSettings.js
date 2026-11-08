/**
 * 用户设置API
 */
import request from '@/utils/request'

/**
 * 获取用户设置
 */
export function getUserSettings() {
  return request({
    url: '/user-settings',
    method: 'get'
  })
}

/**
 * 更新用户设置
 */
export function updateUserSettings(data) {
  return request({
    url: '/user-settings',
    method: 'put',
    data
  })
}
