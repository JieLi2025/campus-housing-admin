/**
 * 宿舍楼管理相关 API
 */
import request from '@/utils/request'

/**
 * 获取宿舍楼列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.buildingType - 宿舍楼类型
 * @param {number} params.status - 状态
 * @param {string} params.keyword - 搜索关键字
 * @returns {Promise}
 */
export function getBuildingList(params) {
  return request({
    url: '/admin/buildings',
    method: 'get',
    params
  })
}

/**
 * 获取所有宿舍楼（不分页）
 * @returns {Promise}
 */
export function getAllBuildings() {
  return request({
    url: '/admin/buildings/all',
    method: 'get'
  })
}

/**
 * 获取宿舍楼详情
 * @param {number} buildingId - 宿舍楼ID
 * @returns {Promise}
 */
export function getBuildingDetail(buildingId) {
  return request({
    url: `/admin/buildings/${buildingId}`,
    method: 'get'
  })
}

/**
 * 创建宿舍楼
 * @param {Object} data - 宿舍楼数据
 * @returns {Promise}
 */
export function createBuilding(data) {
  return request({
    url: '/admin/buildings',
    method: 'post',
    data
  })
}

/**
 * 更新宿舍楼
 * @param {Object} data - 宿舍楼数据
 * @returns {Promise}
 */
export function updateBuilding(data) {
  return request({
    url: '/admin/buildings',
    method: 'put',
    data
  })
}

/**
 * 删除宿舍楼
 * @param {number} buildingId - 宿舍楼ID
 * @returns {Promise}
 */
export function deleteBuilding(buildingId) {
  return request({
    url: `/admin/buildings/${buildingId}`,
    method: 'delete'
  })
}

/**
 * 为楼栋分配管理员（简化版）
 * @param {number} buildingId - 楼栋ID
 * @param {string} adminName - 管理员姓名
 * @param {string} adminPhone - 管理员电话（可选）
 * @returns {Promise}
 */
export function assignBuildingManager(buildingId, adminName, adminPhone = '') {
  return request({
    url: `/admin/buildings/${buildingId}/assign-manager`,
    method: 'post',
    params: {
      adminName,
      adminPhone
    }
  })
}

/**
 * 为管理员批量分配楼栋（简化版）
 * @param {string} adminName - 管理员姓名
 * @param {string} adminPhone - 管理员电话（可选）
 * @param {Array<number>} buildingIds - 楼栋ID列表
 * @returns {Promise}
 */
export function assignBuildingsToManager(adminName, adminPhone, buildingIds) {
  return request({
    url: '/admin/buildings/assign-buildings',
    method: 'post',
    params: {
      adminName,
      adminPhone
    },
    data: buildingIds
  })
}
