/**
 * 物资管理API
 * 学生端物资查看和申报接口
 * 
 * @author Campus Dorm System
 * @date 2025-10-28
 */
import request from '@/utils/request'

/**
 * 获取我的宿舍物资配置（从物资模板获取）
 * @returns {Promise}
 */
export function getMyRoomMaterialTemplate() {
  return request({
    url: '/student/materials/my-template',
    method: 'get'
  })
}

/**
 * 提交物品申报
 * @param {Object} data - 申报数据
 * @param {String} data.materialName - 物品名称
 * @param {String} data.issueType - 问题类型（损坏/缺失/故障/其他）
 * @param {String} data.issue - 问题描述
 * @returns {Promise}
 */
export function submitMaterialReport(data) {
  return request({
    url: '/student/materials/report',
    method: 'post',
    data
  })
}

/**
 * 获取我的物品申报记录
 * @returns {Promise}
 */
export function getMyMaterialReports() {
  return request({
    url: '/student/materials/my-reports',
    method: 'get'
  })
}

/**
 * 撤回物品申报
 * @param {Number} id - 申报ID
 * @returns {Promise}
 */
export function withdrawMaterialReport(id) {
  return request({
    url: `/student/materials/report/${id}`,
    method: 'delete'
  })
}

/**
 * 修改物品申报
 * @param {Number} id - 申报ID
 * @param {Object} data - 申报数据
 * @returns {Promise}
 */
export function updateMaterialReport(id, data) {
  return request({
    url: `/student/materials/report/${id}`,
    method: 'put',
    data
  })
}
