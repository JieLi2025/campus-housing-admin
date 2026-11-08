/**
 * 聊天API
 */
import request from '@/utils/request'

/**
 * 获取会话列表
 */
export function getConversations() {
  return request({
    url: '/chat/conversations',
    method: 'get'
  })
}

/**
 * 获取消息历史
 */
export function getMessages(conversationId, pageNum = 1, pageSize = 50) {
  return request({
    url: `/chat/conversations/${conversationId}/messages`,
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 创建会话
 */
export function createConversation(userId) {
  return request({
    url: '/chat/conversations',
    method: 'post',
    data: { userId }
  })
}

/**
 * 发送消息
 */
export function sendMessage(data) {
  return request({
    url: '/chat/messages',
    method: 'post',
    data
  })
}

/**
 * 撤回消息
 */
export function recallMessage(messageId) {
  return request({
    url: `/chat/messages/${messageId}/recall`,
    method: 'put'
  })
}

/**
 * 更新消息类型（用于更新邀请状态等）
 */
export function updateMessageType(messageId, newType) {
  return request({
    url: `/chat/messages/${messageId}/type`,
    method: 'put',
    params: { newType }
  })
}

/**
 * 删除消息
 */
export function deleteMessage(messageId) {
  return request({
    url: `/chat/messages/${messageId}`,
    method: 'delete'
  })
}

/**
 * 标记会话为已读
 */
export function markAsRead(conversationId) {
  return request({
    url: `/chat/conversations/${conversationId}/read`,
    method: 'put'
  })
}

/**
 * 获取联系人列表
 */
export function getContacts() {
  return request({
    url: '/chat/contacts',
    method: 'get'
  })
}

/**
 * 添加联系人
 */
export function addContact(contactId, remark) {
  return request({
    url: '/chat/contacts',
    method: 'post',
    data: { contactId, remark }
  })
}

/**
 * 更新联系人备注
 */
export function updateContactRemark(contactId, remark) {
  return request({
    url: `/chat/contacts/${contactId}`,
    method: 'put',
    data: { remark }
  })
}

/**
 * 删除联系人
 */
export function deleteContact(contactId) {
  return request({
    url: `/chat/contacts/${contactId}`,
    method: 'delete'
  })
}

/**
 * 搜索联系人
 */
export function searchContacts(keyword) {
  return request({
    url: '/chat/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 通过用户ID获取用户信息（用于聊天跳转时精确查找用户）
 */
export function getUserById(userId) {
  return request({
    url: `/chat/user/${userId}`,
    method: 'get'
  })
}

/**
 * 获取用户详细资料（用于查看资料弹窗）
 * 支持学生和管理员，返回不同的字段
 */
export function getUserProfile(userId) {
  return request({
    url: `/chat/user/${userId}/profile`,
    method: 'get'
  })
}

/**
 * 上传聊天文件（图片或文件）
 */
export function uploadChatFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/chat/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// ========== 楼栋群聊API ==========

/**
 * 获取楼栋群聊会话
 */
export function getBuildingGroupChat(buildingId) {
  return request({
    url: `/chat/building/${buildingId}/group`,
    method: 'get'
  })
}

/**
 * 发送楼栋群聊消息
 */
export function sendBuildingGroupMessage(buildingId, data) {
  return request({
    url: `/chat/building/${buildingId}/group/messages`,
    method: 'post',
    data
  })
}

/**
 * 获取楼栋群聊消息历史
 */
export function getBuildingGroupMessages(buildingId, pageNum = 1, pageSize = 50) {
  return request({
    url: `/chat/building/${buildingId}/group/messages`,
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 获取楼栋群聊成员列表
 */
export function getBuildingGroupMembers(buildingId) {
  return request({
    url: `/chat/building/${buildingId}/group/members`,
    method: 'get'
  })
}

/**
 * 获取楼栋学生统计信息
 */
export function getBuildingStudentStats(buildingId) {
  return request({
    url: `/chat/building/${buildingId}/group/stats`,
    method: 'get'
  })
}

/**
 * 获取楼栋在线用户数
 */
export function getBuildingOnlineCount(buildingId) {
  return request({
    url: `/chat/building/${buildingId}/group/online-count`,
    method: 'get'
  })
}

/**
 * 禁言/解除禁言用户
 */
export function toggleMuteUser(buildingId, userId, mute) {
  return request({
    url: `/chat/building/${buildingId}/group/mute/${userId}`,
    method: 'put',
    params: { mute }
  })
}

/**
 * 切换全体禁言状态
 */
export function toggleMuteAll(buildingId, muteAll) {
  return request({
    url: `/chat/building/${buildingId}/group/mute-all`,
    method: 'put',
    params: { muteAll }
  })
}

/**
 * 获取全体禁言状态
 */
export function getMuteAllStatus(buildingId) {
  return request({
    url: `/chat/building/${buildingId}/group/mute-all`,
    method: 'get'
  })
}

// ========== 宿舍群聊API ==========

/**
 * 获取宿舍群聊会话
 */
export function getDormGroupChat(dormGroupId) {
  return request({
    url: `/chat/dorm-group/${dormGroupId}/group`,
    method: 'get'
  })
}

/**
 * 发送宿舍群聊消息
 */
export function sendDormGroupMessage(dormGroupId, data) {
  return request({
    url: `/chat/dorm-group/${dormGroupId}/group/messages`,
    method: 'post',
    data
  })
}

/**
 * 获取宿舍群聊消息历史
 */
export function getDormGroupMessages(dormGroupId, pageNum = 1, pageSize = 50) {
  return request({
    url: `/chat/dorm-group/${dormGroupId}/group/messages`,
    method: 'get',
    params: { pageNum, pageSize }
  })
}

/**
 * 获取宿舍群聊成员列表
 */
export function getDormGroupMembers(dormGroupId) {
  return request({
    url: `/chat/dorm-group/${dormGroupId}/group/members`,
    method: 'get'
  })
}

