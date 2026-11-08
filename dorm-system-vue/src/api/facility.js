import request from '@/utils/request'

/**
 * 房间设施API
 */

// 获取所有房间设施
export function getAllFacilitiesAPI() {
  return request({
    url: '/facility/list',
    method: 'get'
  })
}

// 根据ID获取设施
export function getFacilityByIdAPI(id) {
  return request({
    url: `/facility/${id}`,
    method: 'get'
  })
}

// 新增房间设施
export function addFacilityAPI(data) {
  return request({
    url: '/facility',
    method: 'post',
    data
  })
}

// 更新房间设施
export function updateFacilityAPI(data) {
  return request({
    url: '/facility',
    method: 'put',
    data
  })
}

// 删除房间设施
export function deleteFacilityAPI(id) {
  return request({
    url: `/facility/${id}`,
    method: 'delete'
  })
}

// 批量更新排序
export function updateSortOrderAPI(facilities) {
  return request({
    url: '/facility/sort',
    method: 'put',
    data: facilities
  })
}

// 获取指定房型的默认设施列表
export function getFacilitiesByRoomTypeAPI(roomType) {
  return request({
    url: `/facility/room-type/${roomType}`,
    method: 'get'
  })
}

// 保存房型默认模板配置
export function saveRoomTypeTemplateAPI(roomType, facilityIds) {
  return request({
    url: '/facility/room-type-template',
    method: 'post',
    data: { roomType, facilityIds }
  })
}
