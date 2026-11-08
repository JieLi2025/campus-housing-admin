import request from '@/utils/request'

/**
 * 宿舍房间管理API
 * 用于管理员和超级管理员管理宿舍房间
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */

/**
 * 创建房间
 * @param {Object} data - { buildingId, roomNumber, floor, roomType, bedCount, ... }
 * @returns {Promise}
 */
export function createRoom(data) {
  return request({
    url: '/admin/rooms',
    method: 'post',
    data
  })
}

/**
 * 批量创建房间
 * @param {Object} data - { buildingId, startFloor, endFloor, roomsPerFloor, roomType, bedCount, ... }
 * @returns {Promise}
 */
export function batchCreateRooms(data) {
  return request({
    url: '/admin/rooms/batch',
    method: 'post',
    data
  })
}

/**
 * 更新房间
 * @param {Number} id - 房间ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export function updateRoom(id, data) {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除房间
 * @param {Number} id - 房间ID
 * @returns {Promise}
 */
export function deleteRoom(id) {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除房间
 * @param {Array<Number>} roomIds - 房间ID列表
 * @returns {Promise}
 */
export function batchDeleteRooms(roomIds) {
  return request({
    url: '/admin/rooms/batch',
    method: 'delete',
    data: roomIds
  })
}

/**
 * 批量更新房间类型
 * @param {Array<Number>} roomIds - 房间ID列表
 * @param {String} roomType - 新房间类型
 * @param {Number} bedCount - 新床位数（可选）
 * @returns {Promise}
 */
export function batchUpdateRoomType(roomIds, roomType, bedCount = null) {
  return request({
    url: '/admin/rooms/batch-update-type',
    method: 'put',
    params: {
      roomIds: roomIds.join(','),
      roomType,
      bedCount
    }
  })
}

/**
 * 获取楼栋的所有房间
 * @param {Number} buildingId - 楼栋ID
 * @returns {Promise}
 */
export function getRoomsByBuilding(buildingId) {
  return request({
    url: `/admin/rooms/building/${buildingId}`,
    method: 'get',
    params: {
      _t: Date.now() // 添加时间戳避免缓存
    }
  })
}

/**
 * 获取房间详情
 * @param {Number} id - 房间ID
 * @returns {Promise}
 */
export function getRoomDetail(id) {
  return request({
    url: `/admin/rooms/${id}`,
    method: 'get'
  })
}
