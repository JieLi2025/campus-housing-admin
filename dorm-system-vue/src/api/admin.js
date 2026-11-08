import request from '@/utils/request'

/**
 * 获取所有管理员列表（普通管理员使用）
 */
export function getAllAdmins() {
  return request({
    url: '/admin/list',
    method: 'get'
  })
}

/**
 * 获取管理员列表（分页，超级管理员使用）
 */
export function getAdminList(params) {
  return request({
    url: '/super-admin/admins',
    method: 'get',
    params
  })
}

/**
 * 获取管理员详情
 */
export function getAdminDetail(id) {
  return request({
    url: `/super-admin/admins/${id}`,
    method: 'get'
  })
}

/**
 * 创建管理员
 */
export function createAdmin(data) {
  return request({
    url: '/super-admin/admins',
    method: 'post',
    data
  })
}

/**
 * 更新管理员信息
 */
export function updateAdmin(data) {
  return request({
    url: '/super-admin/admins',
    method: 'put',
    data
  })
}

/**
 * 删除管理员
 */
export function deleteAdmin(id) {
  return request({
    url: `/super-admin/admins/${id}`,
    method: 'delete'
  })
}

/**
 * 切换管理员状态
 */
export function toggleAdminStatus(id) {
  return request({
    url: `/super-admin/admins/${id}/status`,
    method: 'put'
  })
}

/**
 * 重置管理员密码
 */
export function resetAdminPassword(id, data) {
  return request({
    url: `/super-admin/admins/${id}/password`,
    method: 'put',
    data
  })
}
