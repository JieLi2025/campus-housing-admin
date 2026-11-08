/**
 * 快捷回复数据管理
 */

const STORAGE_KEY = 'quick_replies_data'

// 默认快捷回复
const defaultQuickReplies = [
  { id: 1, content: '好的，已收到您的反馈' },
  { id: 2, content: '请提供更详细的信息' },
  { id: 3, content: '我们会尽快处理这个问题' },
  { id: 4, content: '请按照规定执行' },
  { id: 5, content: '有其他问题随时联系' },
  { id: 6, content: '好的，我会尽快处理' },
  { id: 7, content: '收到，感谢反馈' },
  { id: 8, content: '已经安排维修师傅上门' },
  { id: 9, content: '请保持宿舍整洁' },
  { id: 10, content: '注意用电安全' }
]

/**
 * 获取所有快捷回复
 */
export function getQuickReplies() {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    if (data) {
      return JSON.parse(data)
    }
    // 首次加载时保存默认数据
    saveQuickReplies(defaultQuickReplies)
    return defaultQuickReplies
  } catch (error) {
    console.error('获取快捷回复失败:', error)
    return defaultQuickReplies
  }
}

/**
 * 保存快捷回复
 */
export function saveQuickReplies(replies) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(replies))
    return true
  } catch (error) {
    console.error('保存快捷回复失败:', error)
    return false
  }
}

/**
 * 添加快捷回复
 */
export function addQuickReply(content) {
  try {
    const replies = getQuickReplies()
    const newId = replies.length > 0 ? Math.max(...replies.map(r => r.id)) + 1 : 1
    const newReply = {
      id: newId,
      content: content.trim(),
      createTime: new Date().toISOString()
    }
    replies.push(newReply)
    saveQuickReplies(replies)
    return newReply
  } catch (error) {
    console.error('添加快捷回复失败:', error)
    return null
  }
}

/**
 * 删除快捷回复
 */
export function deleteQuickReply(id) {
  try {
    const replies = getQuickReplies()
    const filteredReplies = replies.filter(r => r.id !== id)
    saveQuickReplies(filteredReplies)
    return true
  } catch (error) {
    console.error('删除快捷回复失败:', error)
    return false
  }
}

/**
 * 更新快捷回复
 */
export function updateQuickReply(id, content) {
  try {
    const replies = getQuickReplies()
    const reply = replies.find(r => r.id === id)
    if (reply) {
      reply.content = content.trim()
      reply.updateTime = new Date().toISOString()
      saveQuickReplies(replies)
      return true
    }
    return false
  } catch (error) {
    console.error('更新快捷回复失败:', error)
    return false
  }
}

/**
 * 重置为默认快捷回复
 */
export function resetQuickReplies() {
  try {
    saveQuickReplies(defaultQuickReplies)
    return true
  } catch (error) {
    console.error('重置快捷回复失败:', error)
    return false
  }
}

