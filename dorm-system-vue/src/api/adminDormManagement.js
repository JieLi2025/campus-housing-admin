/**
 * 管理员宿舍管理API
 * 用于管理员管理楼栋、房间、区域等功能
 */
import request from '@/utils/request'

/**
 * 获取当前管理员管理的楼栋
 * @returns {Promise}
 */
export function getMyBuildings() {
  return request({
    url: '/admin/my-buildings',
    method: 'get'
  })
}

/**
 * 获取楼栋统计信息
 * @param {number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function getBuildingStatistics(buildingId) {
  return request({
    url: `/admin/buildings/${buildingId}/statistics`,
    method: 'get'
  })
}

/**
 * 获取楼栋所有房间（含入住学生）
 * @param {number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function getBuildingRooms(buildingId) {
  return request({
    url: `/admin/buildings/${buildingId}/rooms`,
    method: 'get'
  })
}

/**
 * 获取房间入住学生
 * @param {number} roomId - 房间ID
 * @returns {Promise}
 */
export function getRoomResidents(roomId) {
  return request({
    url: `/admin/rooms/${roomId}/residents`,
    method: 'get'
  })
}

/**
 * 获取楼栋区域列表
 * @param {number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function getBuildingAreas(buildingId) {
  return request({
    url: `/admin/buildings/${buildingId}/areas`,
    method: 'get'
  })
}

/**
 * 创建区域
 * @param {number} buildingId - 楼栋ID
 * @param {Object} data - 区域数据
 * @returns {Promise}
 */
export function createArea(buildingId, data) {
  return request({
    url: `/admin/buildings/${buildingId}/areas`,
    method: 'post',
    data
  })
}

/**
 * 更新区域
 * @param {string} areaId - 区域ID
 * @param {Object} data - 区域数据
 * @returns {Promise}
 */
export function updateArea(areaId, data) {
  return request({
    url: `/admin/areas/${areaId}`,
    method: 'put',
    data
  })
}

/**
 * 删除区域
 * @param {string} areaId - 区域ID
 * @returns {Promise}
 */
export function deleteArea(areaId) {
  return request({
    url: `/admin/areas/${areaId}`,
    method: 'delete'
  })
}

/**
 * 批量设置房间归属
 * @param {Object} data - 批量分配数据
 * @param {Array<number>} data.roomIds - 房间ID列表
 * @param {number} data.buildingId - 楼栋ID
 * @param {string} data.majorName - 专业名称
 * @param {string} data.className - 班级名称
 * @param {string} data.action - 操作类型：assign/unassign
 * @returns {Promise}
 */
export function batchAssignRooms(data) {
  return request({
    url: '/admin/rooms/batch-assign',
    method: 'put',
    data
  })
}

/**
 * 获取学生的班级楼栋分配信息（学生端使用）
 * @returns {Promise}
 */
export function getMyBuildingAssignment() {
  return request({
    url: '/student/dorm/my-building-assignment',
    method: 'get'
  })
}

