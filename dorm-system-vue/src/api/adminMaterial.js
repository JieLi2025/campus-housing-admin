/**
 * 管理员端物资管理API
 * 
 * @author Campus Dorm System
 * @date 2025-11-30
 */
import request from '@/utils/request'

/**
 * 获取指定宿舍的物资配置模板
 * @param {String} dormNumber - 宿舍号
 * @returns {Promise}
 */
export function getRoomMaterialTemplate(dormNumber) {
  return request({
    url: '/admin/materials/room-template',
    method: 'get',
    params: { dormNumber }
  })
}

/**
 * 提交物资清点结果
 * @param {Object} data - 清点数据
 * @param {Number} data.applicationId - 申请ID
 * @param {Array} data.materials - 物资清点列表
 * @param {Number} data.totalCompensation - 赔偿总额
 * @returns {Promise}
 */
export function submitInventoryResult(data) {
  return request({
    url: '/admin/materials/inventory',
    method: 'post',
    data
  })
}

/**
 * 获取物资清点记录
 * @param {Number} applicationId - 申请ID
 * @returns {Promise}
 */
export function getInventoryResult(applicationId) {
  return request({
    url: `/admin/materials/inventory/${applicationId}`,
    method: 'get'
  })
}

/**
 * 确认赔偿完成
 * @param {Number} applicationId - 申请ID
 * @returns {Promise}
 */
export function confirmCompensation(applicationId) {
  return request({
    url: `/admin/materials/compensation/${applicationId}/confirm`,
    method: 'post'
  })
}
