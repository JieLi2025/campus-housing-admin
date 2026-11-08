// 学生端消息数据管理
const STORAGE_KEY = 'studentMessageData'

// 自定义事件系统
const eventBus = {
  listeners: {},
  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = []
    }
    this.listeners[event].push(callback)
  },
  emit(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(callback => callback(data))
    }
  },
  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    }
  }
}

// 获取存储的消息数据
function getMessageData() {
  const data = localStorage.getItem(STORAGE_KEY)
  return data ? JSON.parse(data) : {
    notifications: [],
    privateChats: {},
    unreadCounts: {
      notifications: 0,
      privateChats: 0
    }
  }
}

// 保存消息数据
function saveMessageData(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
  // 触发数据更新事件
  eventBus.emit('messageDataChanged', data)
}

// 获取通知数据
function getNotifications() {
  const data = getMessageData()
  return data.notifications || []
}

// 保存通知数据
function saveNotifications(notifications) {
  const data = getMessageData()
  data.notifications = notifications
  // 计算未读通知数量
  data.unreadCounts.notifications = notifications.filter(n => !n.isRead).length
  saveMessageData(data)
}

// 获取私聊数据
function getPrivateChats() {
  const data = getMessageData()
  return data.privateChats || {}
}

// 保存私聊数据
function savePrivateChats(privateChats) {
  const data = getMessageData()
  data.privateChats = privateChats
  // 计算未读私聊数量
  let totalUnread = 0
  Object.values(privateChats).forEach(chat => {
    totalUnread += chat.unreadCount || 0
  })
  data.unreadCounts.privateChats = totalUnread
  saveMessageData(data)
}

// 获取总未读消息数量
function getTotalUnreadCount() {
  const data = getMessageData()
  return (data.unreadCounts?.notifications || 0) + (data.unreadCounts?.privateChats || 0)
}

// 标记通知为已读
function markNotificationAsRead(notificationId) {
  const notifications = getNotifications()
  const notification = notifications.find(n => n.id === notificationId)
  if (notification && !notification.isRead) {
    notification.isRead = true
    saveNotifications(notifications)
  }
}

// 标记所有通知为已读
function markAllNotificationsAsRead() {
  const notifications = getNotifications()
  notifications.forEach(n => {
    n.isRead = true
  })
  saveNotifications(notifications)
}

// 标记私聊消息为已读
function markPrivateChatAsRead(contactId) {
  const privateChats = getPrivateChats()
  if (privateChats[contactId]) {
    privateChats[contactId].unreadCount = 0
    savePrivateChats(privateChats)
  }
}

// 添加私聊消息
function addPrivateChatMessage(contactId, message) {
  const privateChats = getPrivateChats()
  if (!privateChats[contactId]) {
    privateChats[contactId] = {
      messages: [],
      unreadCount: 0
    }
  }
  
  privateChats[contactId].messages.push(message)
  
  // 如果不是自己发送的消息，增加未读计数
  if (!message.isOwn) {
    privateChats[contactId].unreadCount = (privateChats[contactId].unreadCount || 0) + 1
  }
  
  savePrivateChats(privateChats)
}

// 撤回私聊消息
function recallPrivateChatMessage(contactId, messageId) {
  const privateChats = getPrivateChats()
  if (privateChats[contactId] && privateChats[contactId].messages) {
    const messageIndex = privateChats[contactId].messages.findIndex(msg => msg.id === messageId)
    if (messageIndex > -1) {
      const message = privateChats[contactId].messages[messageIndex]
      // 标记消息为已撤回
      message.recalled = true
      message.originalContent = message.content
      message.content = null
      // 保存更新
      savePrivateChats(privateChats)
      return true
    }
  }
  return false
}

// 撤回邀请消息
function recallInviteMessage(contactId, messageId) {
  const privateChats = getPrivateChats()
  if (privateChats[contactId] && privateChats[contactId].messages) {
    const messageIndex = privateChats[contactId].messages.findIndex(msg => msg.id === messageId)
    if (messageIndex > -1) {
      const message = privateChats[contactId].messages[messageIndex]
      // 标记邀请为已撤回
      message.recalled = true
      message.originalType = message.type
      message.originalContent = message.content
      message.type = 'recalled'
      if (message.inviteData) {
        message.inviteData.recalled = true
      }
      // 保存更新
      savePrivateChats(privateChats)
      return true
    }
  }
  return false
}

// 添加新通知
function addNotification(notification) {
  const notifications = getNotifications()
  notifications.unshift(notification)
  saveNotifications(notifications)
}


// 默认通知数据
const defaultNotifications = []

// 默认私聊数据（恢复私聊功能）
const defaultPrivateChats = {}

// 初始化默认数据
function initializeDefaultData() {
  const data = getMessageData()
  
  // 只有在没有数据时才初始化
  if (!data.notifications || data.notifications.length === 0) {
    console.log('初始化通知数据...')
    saveNotifications(defaultNotifications)
  }
   
  // 初始化私聊数据（恢复私聊功能）
  if (!data.privateChats || Object.keys(data.privateChats).length === 0) {
    console.log('初始化私聊数据...')
    savePrivateChats(defaultPrivateChats)
  }
   
  console.log('数据初始化检查完成！')
  console.log('当前总未读数量:', getTotalUnreadCount())
}

// 监听数据变化事件
function setupDataChangeListener(callback) {
  eventBus.on('messageDataChanged', callback)
}

// 监听localStorage变化（跨标签页）
function setupStorageListener(callback) {
  window.addEventListener('storage', (e) => {
    if (e.key === STORAGE_KEY) {
      callback()
    }
  })
}

// 清除所有数据（用于测试）
function clearAllData() {
  localStorage.removeItem(STORAGE_KEY)
  eventBus.emit('messageDataChanged', null)
}

// 强制重置为测试数据（用于测试）
function resetToTestData() {
  localStorage.removeItem(STORAGE_KEY)
  initializeDefaultData()
  console.log('已强制重置为测试数据！')
}

export {
  getNotifications,
  saveNotifications,
  getPrivateChats,
  savePrivateChats,
  getTotalUnreadCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  markPrivateChatAsRead,
  addPrivateChatMessage,
  recallPrivateChatMessage,
  recallInviteMessage,
  addNotification,
  initializeDefaultData,
  setupDataChangeListener,
  setupStorageListener,
  clearAllData,
  resetToTestData
}
