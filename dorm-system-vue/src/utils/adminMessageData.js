// 管理员端消息数据管理
const STORAGE_KEY = 'adminMessageData'

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
    unreadCount: 0
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
  data.unreadCount = notifications.filter(n => !n.isRead).length
  saveMessageData(data)
}

// 获取总未读消息数量
function getTotalUnreadCount() {
  const data = getMessageData()
  return data.unreadCount || 0
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

// 添加新通知
function addNotification(notification) {
  const notifications = getNotifications()
  notifications.unshift(notification)
  saveNotifications(notifications)
}

// 添加个人信息通知
function addPrivateChatNotification(fromUserId, fromUserName, message, chatId) {
  const notification = {
    id: Date.now(),
    type: 'private_chat',
    title: `来自${fromUserName}的个人信息`,
    message: message.length > 50 ? message.substring(0, 50) + '...' : message,
    createTime: new Date().toISOString(),
    isRead: false,
    chatId: chatId,
    fromUserId: fromUserId,
    fromUserName: fromUserName
  }
  addNotification(notification)
  return notification
}

// 删除通知
function deleteNotification(notificationId) {
  const notifications = getNotifications()
  const filteredNotifications = notifications.filter(n => n.id !== notificationId)
  saveNotifications(filteredNotifications)
}

// 初始化默认数据（已移除模拟数据，通知现在从后端API获取）
function initializeDefaultData() {
  // 不再初始化模拟数据，通知数据现在从后端API获取
  // 保留此函数以保持向后兼容性
  console.log('管理员通知数据初始化完成（数据从后端API获取）')
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

// 清除所有数据
function clearAllData() {
  // 保存空的数据结构
  const emptyData = {
    notifications: [],
    unreadCount: 0
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(emptyData))
  eventBus.emit('messageDataChanged', emptyData)
}

// 强制重置为测试数据（用于测试）
function resetToTestData() {
  localStorage.removeItem(STORAGE_KEY)
  initializeDefaultData()
  console.log('已强制重置为管理员测试数据！')
}

export {
  getNotifications,
  saveNotifications,
  getTotalUnreadCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  addNotification,
  addPrivateChatNotification,
  deleteNotification,
  initializeDefaultData,
  setupDataChangeListener,
  setupStorageListener,
  clearAllData
}
