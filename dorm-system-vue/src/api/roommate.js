import request from '@/utils/request'

export function searchRoommates(params = {}) {
  // 手动构建查询字符串，避免数组参数格式问题
  const searchParams = new URLSearchParams()
  
  if (params.preferredTags && params.preferredTags.length > 0) {
    params.preferredTags.forEach(tag => searchParams.append('preferredTags', tag))
  }
  if (params.excludedTags && params.excludedTags.length > 0) {
    params.excludedTags.forEach(tag => searchParams.append('excludedTags', tag))
  }
  if (params.random) {
    searchParams.append('random', 'true')
  }
  
  const queryString = searchParams.toString()
  const url = queryString ? `/student/roommates/search?${queryString}` : '/student/roommates/search'
  
  return request({ 
    url: url, 
    method: 'get'
  })
}

export function getPopularTags(limit = 20) {
  return request({ url: `/student/roommates/popular-tags?limit=${limit}`, method: 'get' })
}

export function sendRoommateInvite(data) {
  return request({ url: '/student/roommates/invite', method: 'post', data })
}

export function getSentInvites() {
  return request({ url: '/student/roommates/invites/sent', method: 'get' })
}

export function getReceivedInvites() {
  return request({ url: '/student/roommates/invites/received', method: 'get' })
}

export function acceptInvite(id) {
  return request({ url: `/student/roommates/invite/${id}/accept`, method: 'put' })
}

export function rejectInvite(id) {
  return request({ url: `/student/roommates/invite/${id}/reject`, method: 'put' })
}
