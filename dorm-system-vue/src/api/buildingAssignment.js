import request from '@/utils/request'

/**
 * 楼栋分配管理API
 * 用于超级管理员为普通管理员分配楼栋
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */

/**
 * 为管理员分配楼栋
 * @param {Object} data - { adminId, buildingIds: [], remark }
 * @returns {Promise}
 */
export function assignBuildings(data) {
  return request({
    url: '/super-admin/building-assignments',
    method: 'post',
    data
  })
}

/**
 * 获取管理员的楼栋分配列表
 * @param {Number} adminId - 管理员ID
 * @returns {Promise}
 */
export function getAdminAssignments(adminId) {
  return request({
    url: `/super-admin/building-assignments/admin/${adminId}`,
    method: 'get'
  })
}

/**
 * 获取所有楼栋分配（Map格式）
 * 返回格式：{ adminId: [buildingId1, buildingId2, ...] }
 * @returns {Promise}
 */
export function getAllBuildingAssignments() {
  return request({
    url: '/super-admin/building-assignments/all',
    method: 'get'
  })
}

/**
 * 获取楼栋的分配信息
 * @param {Number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function getBuildingAssignment(buildingId) {
  return request({
    url: `/super-admin/building-assignments/building/${buildingId}`,
    method: 'get'
  })
}

/**
 * 取消楼栋分配
 * @param {Number} adminId - 管理员ID
 * @param {Number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function removeAssignment(adminId, buildingId) {
  return request({
    url: `/super-admin/building-assignments/admin/${adminId}/building/${buildingId}`,
    method: 'delete'
  })
}
