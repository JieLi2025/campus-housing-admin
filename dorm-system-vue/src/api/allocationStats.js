import request from '@/utils/request'

/**
 * 获取学生分配统计数据
 * @param {Array} buildingIds 楼栋ID数组
 */
export function getAllocationStatistics(buildingIds) {
  return request({
    url: '/admin/allocation/statistics',
    method: 'get',
    params: {
      buildingIds: buildingIds && buildingIds.length > 0 ? buildingIds.join(',') : undefined
    }
  })
}

