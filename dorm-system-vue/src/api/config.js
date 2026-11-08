import request from '@/utils/request'

// ==================== 系统参数配置 ====================

/**
 * 获取所有系统配置
 */
export function getAllSystemConfigsAPI() {
  return request({
    url: '/super-admin/config/system',
    method: 'get'
  })
}

/**
 * 获取单个系统配置
 */
export function getSystemConfigAPI(key) {
  return request({
    url: `/super-admin/config/system/${key}`,
    method: 'get'
  })
}

/**
 * 保存系统配置
 */
export function saveSystemConfigAPI(data) {
  return request({
    url: '/super-admin/config/system',
    method: 'post',
    data
  })
}

/**
 * 批量更新系统配置
 */
export function batchSaveSystemConfigsAPI(configs) {
  return request({
    url: '/super-admin/config/system/batch',
    method: 'put',
    data: configs
  })
}

/**
 * 删除系统配置
 */
export function deleteSystemConfigAPI(key) {
  return request({
    url: `/super-admin/config/system/${key}`,
    method: 'delete'
  })
}

// ==================== 宿舍物资模板配置 ====================

/**
 * 获取所有物资模板
 */
export function getAllMaterialTemplatesAPI() {
  return request({
    url: '/super-admin/config/materials',
    method: 'get'
  })
}

/**
 * 根据楼栋ID和房间类型获取物资模板
 * @param {number} buildingId - 楼栋ID（优先使用）
 * @param {string} buildingType - 楼栋类型（向后兼容）
 * @param {string} roomType - 房间类型
 */
export function getMaterialTemplateByTypeAPI(buildingId, buildingType, roomType) {
  return request({
    url: '/super-admin/config/materials/by-type',
    method: 'get',
    params: {
      buildingId,
      buildingType,
      roomType
    }
  })
}

/**
 * 创建物资模板
 */
export function createMaterialTemplateAPI(data) {
  return request({
    url: '/super-admin/config/materials',
    method: 'post',
    data
  })
}

/**
 * 更新物资模板
 */
export function updateMaterialTemplateAPI(data) {
  return request({
    url: '/super-admin/config/materials',
    method: 'put',
    data
  })
}

/**
 * 删除物资模板
 */
export function deleteMaterialTemplateAPI(id) {
  return request({
    url: `/super-admin/config/materials/${id}`,
    method: 'delete'
  })
}

// ==================== 楼栋数据 ====================

/**
 * 获取所有楼栋列表（用于系统配置页面）
 */
export function getAllBuildingsAPI() {
  return request({
    url: '/super-admin/config/buildings',
    method: 'get'
  })
}


