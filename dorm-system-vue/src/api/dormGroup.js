/**
 * 宿舍组管理API
 * 用于学生自组宿舍功能
 */
import request from '@/utils/request'

/**
 * 获取宿舍组列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.currentPage - 当前页码
 * @param {number} params.pageSize - 每页数量
 * @param {string} params.keyword - 搜索关键字（组名、组长姓名）
 * @param {string} params.status - 状态筛选（incomplete/complete/room_selected）
 * @returns {Promise}
 */
export function getDormGroupList(params) {
  return request({
    url: '/admin/dorm-groups',
    method: 'get',
    params
  })
}

/**
 * 根据ID获取宿舍组详情
 * @param {number} id - 宿舍组ID
 * @returns {Promise}
 */
export function getDormGroupById(id) {
  return request({
    url: `/admin/dorm-groups/${id}`,
    method: 'get'
  })
}

/**
 * 根据组长ID获取宿舍组
 * @param {number} leaderId - 组长ID（学生ID）
 * @returns {Promise}
 */
export function getDormGroupsByLeaderId(leaderId) {
  return request({
    url: `/admin/dorm-groups/by-leader/${leaderId}`,
    method: 'get'
  })
}

/**
 * 创建宿舍组
 * @param {Object} data - 宿舍组数据
 * @param {string} data.groupName - 组名
 * @param {string} data.leaderName - 组长姓名
 * @param {number} data.leaderId - 组长ID
 * @param {number} data.memberCount - 成员数量
 * @param {number} data.maxMembers - 最大成员数
 * @param {string} data.major - 专业
 * @param {string} data.className - 班级
 * @param {string} data.gender - 性别
 * @param {string} data.roomType - 房间类型
 * @param {string} data.members - 成员列表（JSON字符串）
 * @returns {Promise}
 */
export function createDormGroup(data) {
  return request({
    url: '/admin/dorm-groups',
    method: 'post',
    data
  })
}

/**
 * 更新宿舍组信息
 * @param {number} id - 宿舍组ID
 * @param {Object} data - 更新的数据
 * @returns {Promise}
 */
export function updateDormGroup(id, data) {
  return request({
    url: `/admin/dorm-groups/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除宿舍组
 * @param {number} id - 宿舍组ID
 * @returns {Promise}
 */
export function deleteDormGroup(id) {
  return request({
    url: `/admin/dorm-groups/${id}`,
    method: 'delete'
  })
}

/**
 * 选择房间
 * @param {number} id - 宿舍组ID
 * @param {Object} data - 房间数据
 * @param {number} data.roomId - 房间ID
 * @param {number} data.buildingId - 楼栋ID
 * @returns {Promise}
 */
export function selectRoom(id, data) {
  return request({
    url: `/admin/dorm-groups/${id}/select-room`,
    method: 'post',
    data
  })
}

/**
 * 获取宿舍组统计数据
 * @returns {Promise}
 */
export function getDormGroupStatistics() {
  return request({
    url: '/admin/dorm-groups/statistics',
    method: 'get'
  })
}

/**
 * 取消房间选择（管理员操作）
 * @param {number} id - 宿舍组ID
 * @returns {Promise}
 */
export function cancelRoomSelection(id) {
  return request({
    url: `/admin/dorm-groups/${id}/cancel-room-selection`,
    method: 'post'
  })
}

/**
 * 获取当前学生的宿舍组信息（学生端）
 * @returns {Promise}
 */
export function getMyDormGroup() {
  return request({
    url: '/student/dorm/my-dorm-group',
    method: 'get'
  })
}


/**
 * 更新宿舍组公告
 * @param {number} id - 宿舍组ID
 * @param {string} announcement - 公告内容
 * @returns {Promise}
 */
export function updateDormGroupAnnouncement(id, announcement) {
  return request({
    url: `/admin/dorm-groups/${id}/announcement`,
    method: 'put',
    data: { announcement }
  })
}
