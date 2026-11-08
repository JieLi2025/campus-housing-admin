import request from '@/utils/request'

/**
 * 楼栋专业分配管理API
 * 用于超级管理员管理楼栋与专业的分配关系
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */

/**
 * 批量设置楼栋的专业分配
 * @param {Number} buildingId - 楼栋ID
 * @param {Array} assignments - 专业分配列表 [{ majorName, capacity, ... }]
 * @returns {Promise}
 */
export function saveMajorAssignments(buildingId, assignments) {
  return request({
    url: `/super-admin/major-assignments/building/${buildingId}`,
    method: 'post',
    data: assignments
  })
}

/**
 * 获取楼栋的专业分配列表
 * @param {Number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function getMajorAssignments(buildingId) {
  return request({
    url: `/super-admin/major-assignments/building/${buildingId}`,
    method: 'get'
  })
}

/**
 * 获取所有楼栋的专业分配（Map格式）
 * 返回格式：{ buildingId: [{majorName, capacity, ...}, ...] }
 * @returns {Promise}
 */
export function getAllMajorAssignments() {
  return request({
    url: '/super-admin/major-assignments/all',
    method: 'get'
  })
}

/**
 * 删除专业分配
 * @param {Number} id - 分配ID
 * @returns {Promise}
 */
export function deleteMajorAssignment(id) {
  return request({
    url: `/super-admin/major-assignments/${id}`,
    method: 'delete'
  })
}

/**
 * 获取各专业剩余学生数
 * 返回格式：{ "男": { "专业名": 剩余人数, ... }, "女": { "专业名": 剩余人数, ... } }
 * @returns {Promise}
 */
export function getRemainingStudentsByMajor() {
  return request({
    url: '/super-admin/major-assignments/remaining-students',
    method: 'get'
  })
}
