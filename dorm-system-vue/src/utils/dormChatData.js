// 宿舍群聊数据管理工具

// 数据存储键
const DORM_CHAT_DATA_KEY = 'dormChatData'
const DORM_CHAT_MEMBERS_KEY = 'dormChatMembers'
const USER_REMARKS_KEY = 'userRemarks'
const MESSAGE_QUOTES_KEY = 'messageQuotes'

// 默认群聊数据
const defaultDormChats = {
  'dorm_101': {
    id: 'dorm_101',
    name: '1号楼101室',
    building: '1号楼',
    room: '101',
    avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    description: '欢迎来到101宿舍群聊！',
    announcement: '请大家保持宿舍卫生，按时熄灯。',
    createTime: '2024-01-10T10:00:00Z',
    memberCount: 4,
    isActive: true,
    settings: {
      allowInvite: true,
      muteAll: false,
      allowAnonymous: true,
      showMemberList: true
    },
    messages: [
      {
        id: 1,
        senderId: 'system',
        senderName: '系统通知',
        senderAvatar: '',
        content: '欢迎来到101宿舍群聊！',
        type: 'system',
        timestamp: '2024-01-10T10:00:00Z',
        isAnonymous: false,
        isRecalled: false,
        quotedMessage: null
      }
    ]
  }
}

// 默认群成员数据
const defaultDormChatMembers = {
  'dorm_101': [
    {
      id: 'student_001',
      name: '张三',
      avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      role: 'admin', // admin: 管理员, member: 普通成员
      joinTime: '2024-01-10T10:00:00Z',
      lastActiveTime: '2024-01-15T15:30:00Z',
      isOnline: true,
      isMuted: false,
      major: '计算机科学与技术',
      grade: '2023级',
      studentId: '2023001001'
    },
    {
      id: 'student_002',
      name: '李四',
      avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      role: 'member',
      joinTime: '2024-01-10T10:00:00Z',
      lastActiveTime: '2024-01-15T14:30:00Z',
      isOnline: false,
      isMuted: false,
      major: '软件工程',
      grade: '2023级',
      studentId: '2023001002'
    },
    {
      id: 'student_003',
      name: '王五',
      avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c4d9a9e13f1aa8fdb734a2png.png',
      role: 'member',
      joinTime: '2024-01-10T10:00:00Z',
      lastActiveTime: '2024-01-15T14:35:00Z',
      isOnline: true,
      isMuted: false,
      major: '电子信息工程',
      grade: '2023级',
      studentId: '2023001003'
    },
    {
      id: 'student_004',
      name: '赵六',
      avatar: 'https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg',
      role: 'member',
      joinTime: '2024-01-10T10:00:00Z',
      lastActiveTime: '2024-01-15T14:40:00Z',
      isOnline: true,
      isMuted: false,
      major: '机械工程',
      grade: '2023级',
      studentId: '2023001004'
    }
  ]
}

// 获取群聊数据
export function getDormChatData() {
  const data = localStorage.getItem(DORM_CHAT_DATA_KEY)
  if (data) {
    try {
      return JSON.parse(data)
    } catch (error) {
      console.error('解析群聊数据失败:', error)
      setDormChatData(defaultDormChats)
      return defaultDormChats
    }
  }
  setDormChatData(defaultDormChats)
  return defaultDormChats
}

// 保存群聊数据
export function setDormChatData(chatData) {
  localStorage.setItem(DORM_CHAT_DATA_KEY, JSON.stringify(chatData))
}

// 获取群成员数据
export function getDormChatMembers() {
  const data = localStorage.getItem(DORM_CHAT_MEMBERS_KEY)
  if (data) {
    try {
      return JSON.parse(data)
    } catch (error) {
      console.error('解析群成员数据失败:', error)
      setDormChatMembers(defaultDormChatMembers)
      return defaultDormChatMembers
    }
  }
  setDormChatMembers(defaultDormChatMembers)
  return defaultDormChatMembers
}

// 保存群成员数据
export function setDormChatMembers(membersData) {
  localStorage.setItem(DORM_CHAT_MEMBERS_KEY, JSON.stringify(membersData))
}

// 获取当前用户的宿舍群聊
export function getCurrentUserDormChat() {
  // 这里假设当前用户是student_001，在实际应用中应该从认证系统获取
  const currentUserId = 'student_001'
  const chatData = getDormChatData()
  const membersData = getDormChatMembers()
  
  // 查找用户所在的宿舍群
  for (const [chatId, members] of Object.entries(membersData)) {
    if (members.some(member => member.id === currentUserId)) {
      return {
        ...chatData[chatId],
        members: members
      }
    }
  }
  
  return null
}

// 发送消息到群聊
export function sendMessageToDormChat(chatId, message) {
  const chatData = getDormChatData()
  
  // 如果群聊不存在，自动创建一个新的群聊数据结构
  if (!chatData[chatId]) {
    console.log('群聊数据不存在，自动创建:', chatId)
    chatData[chatId] = {
      id: chatId,
      name: '宿舍群聊',
      building: '',
      room: '',
      avatar: '',
      description: '',
      announcement: '',
      createTime: new Date().toISOString(),
      memberCount: 0,
      isActive: true,
      settings: {
        allowInvite: true,
        muteAll: false,
        allowAnonymous: true,
        showMemberList: true
      },
      messages: []
    }
    setDormChatData(chatData)
  }
  
  const newMessage = {
    id: Date.now(),
    senderId: message.senderId || 'student_001',
    senderName: message.senderName || '当前用户',
    senderAvatar: message.senderAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    content: message.content,
    type: message.type || 'text',
    timestamp: new Date().toISOString(),
    isAnonymous: message.isAnonymous || false,
    isRecalled: false,
    quotedMessage: message.quotedMessage || null,
    fileName: message.fileName,
    fileSize: message.fileSize,
    url: message.url
  }
  
  chatData[chatId].messages.push(newMessage)
  setDormChatData(chatData)
  
  return newMessage
}

// 获取群聊消息
export function getDormChatMessages(chatId, limit = 50, offset = 0) {
  const chatData = getDormChatData()
  
  if (!chatData[chatId]) {
    return []
  }
  
  const messages = chatData[chatId].messages || []
  return messages.slice(offset, offset + limit)
}

// 更新群聊设置
export function updateDormChatSettings(chatId, settings) {
  const chatData = getDormChatData()
  
  if (!chatData[chatId]) {
    throw new Error('群聊不存在')
  }
  
  chatData[chatId].settings = {
    ...chatData[chatId].settings,
    ...settings
  }
  
  setDormChatData(chatData)
  return chatData[chatId].settings
}

// 更新群公告
export function updateDormChatAnnouncement(chatId, announcement) {
  const chatData = getDormChatData()
  
  if (!chatData[chatId]) {
    throw new Error('群聊不存在')
  }
  
  chatData[chatId].announcement = announcement
  setDormChatData(chatData)
  
  // 添加系统消息
  const systemMessage = {
    id: Date.now(),
    senderId: 'system',
    senderName: '系统通知',
    senderAvatar: '',
    content: `群公告已更新：${announcement}`,
    type: 'system',
    timestamp: new Date().toISOString(),
    isAnonymous: false
  }
  
  chatData[chatId].messages.push(systemMessage)
  setDormChatData(chatData)
  
  return announcement
}

// 获取群成员列表
export function getDormChatMemberList(chatId) {
  const membersData = getDormChatMembers()
  return membersData[chatId] || []
}

// 添加群成员
export function addDormChatMember(chatId, member) {
  const membersData = getDormChatMembers()
  const chatData = getDormChatData()
  
  if (!membersData[chatId]) {
    membersData[chatId] = []
  }
  
  // 检查成员是否已存在
  const existingMember = membersData[chatId].find(m => m.id === member.id)
  if (existingMember) {
    throw new Error('成员已存在')
  }
  
  const newMember = {
    ...member,
    role: member.role || 'member',
    joinTime: new Date().toISOString(),
    lastActiveTime: new Date().toISOString(),
    isOnline: true,
    isMuted: false
  }
  
  membersData[chatId].push(newMember)
  setDormChatMembers(membersData)
  
  // 更新群聊成员数量
  if (chatData[chatId]) {
    chatData[chatId].memberCount = membersData[chatId].length
    setDormChatData(chatData)
  }
  
  // 添加系统消息
  const systemMessage = {
    id: Date.now(),
    senderId: 'system',
    senderName: '系统通知',
    senderAvatar: '',
    content: `${member.name}加入了群聊`,
    type: 'system',
    timestamp: new Date().toISOString(),
    isAnonymous: false
  }
  
  if (chatData[chatId]) {
    chatData[chatId].messages.push(systemMessage)
    setDormChatData(chatData)
  }
  
  return newMember
}

// 移除群成员
export function removeDormChatMember(chatId, memberId) {
  const membersData = getDormChatMembers()
  const chatData = getDormChatData()
  
  if (!membersData[chatId]) {
    throw new Error('群聊不存在')
  }
  
  const memberIndex = membersData[chatId].findIndex(m => m.id === memberId)
  if (memberIndex === -1) {
    throw new Error('成员不存在')
  }
  
  const removedMember = membersData[chatId][memberIndex]
  membersData[chatId].splice(memberIndex, 1)
  setDormChatMembers(membersData)
  
  // 更新群聊成员数量
  if (chatData[chatId]) {
    chatData[chatId].memberCount = membersData[chatId].length
    setDormChatData(chatData)
    
    // 添加系统消息
    const systemMessage = {
      id: Date.now(),
      senderId: 'system',
      senderName: '系统通知',
      senderAvatar: '',
      content: `${removedMember.name}退出了群聊`,
      type: 'system',
      timestamp: new Date().toISOString(),
      isAnonymous: false
    }
    
    chatData[chatId].messages.push(systemMessage)
    setDormChatData(chatData)
  }
  
  return removedMember
}

// 更新成员角色
export function updateMemberRole(chatId, memberId, newRole) {
  const membersData = getDormChatMembers()
  
  if (!membersData[chatId]) {
    throw new Error('群聊不存在')
  }
  
  const member = membersData[chatId].find(m => m.id === memberId)
  if (!member) {
    throw new Error('成员不存在')
  }
  
  const oldRole = member.role
  member.role = newRole
  setDormChatMembers(membersData)
  
  // 添加系统消息
  const chatData = getDormChatData()
  if (chatData[chatId]) {
    const roleText = newRole === 'admin' ? '管理员' : '普通成员'
    const systemMessage = {
      id: Date.now(),
      senderId: 'system',
      senderName: '系统通知',
      senderAvatar: '',
      content: `${member.name}被设为${roleText}`,
      type: 'system',
      timestamp: new Date().toISOString(),
      isAnonymous: false
    }
    
    chatData[chatId].messages.push(systemMessage)
    setDormChatData(chatData)
  }
  
  return member
}

// 更新成员在线状态
export function updateMemberOnlineStatus(chatId, memberId, isOnline) {
  const membersData = getDormChatMembers()
  
  if (!membersData[chatId]) {
    return false
  }
  
  const member = membersData[chatId].find(m => m.id === memberId)
  if (!member) {
    return false
  }
  
  member.isOnline = isOnline
  member.lastActiveTime = new Date().toISOString()
  setDormChatMembers(membersData)
  
  return true
}

// 禁言/取消禁言成员
export function toggleMemberMute(chatId, memberId) {
  const membersData = getDormChatMembers()
  
  if (!membersData[chatId]) {
    throw new Error('群聊不存在')
  }
  
  const member = membersData[chatId].find(m => m.id === memberId)
  if (!member) {
    throw new Error('成员不存在')
  }
  
  member.isMuted = !member.isMuted
  setDormChatMembers(membersData)
  
  // 添加系统消息
  const chatData = getDormChatData()
  if (chatData[chatId]) {
    const action = member.isMuted ? '被禁言' : '被取消禁言'
    const systemMessage = {
      id: Date.now(),
      senderId: 'system',
      senderName: '系统通知',
      senderAvatar: '',
      content: `${member.name}${action}`,
      type: 'system',
      timestamp: new Date().toISOString(),
      isAnonymous: false
    }
    
    chatData[chatId].messages.push(systemMessage)
    setDormChatData(chatData)
  }
  
  return member
}

// 撤回消息
export function recallDormChatMessage(chatId, messageId) {
  const chatData = getDormChatData()
  
  if (!chatData[chatId]) {
    throw new Error('群聊不存在')
  }
  
  const message = chatData[chatId].messages.find(m => m.id === messageId)
  if (!message) {
    throw new Error('消息不存在')
  }
  
  // 检查是否可以撤回（2分钟内）
  const now = new Date()
  const messageTime = new Date(message.timestamp)
  const diffMinutes = (now - messageTime) / (1000 * 60)
  
  if (diffMinutes > 2) {
    throw new Error('消息发送超过2分钟，无法撤回')
  }
  
  message.isRecalled = true
  message.recallTime = new Date().toISOString()
  setDormChatData(chatData)
  
  return message
}

// 重置为测试数据
export function resetToTestData() {
  setDormChatData(defaultDormChats)
  setDormChatMembers(defaultDormChatMembers)
}

// 模拟接收消息（用于测试）
export function simulateReceiveMessage(chatId) {
  const replies = [
    '好的，收到了',
    '明白，谢谢',
    '没问题',
    '我知道了',
    '好的呢',
    '今天天气不错',
    '大家晚上一起吃饭吗？',
    '明天有课吗？',
    '宿舍网络怎么样？'
  ]
  
  const randomReply = replies[Math.floor(Math.random() * replies.length)]
  const membersData = getDormChatMembers()
  const members = membersData[chatId] || []
  
  // 随机选择一个成员发送消息
  const availableMembers = members.filter(m => m.id !== 'student_001') // 排除当前用户
  if (availableMembers.length === 0) {
    return null
  }
  
  const randomMember = availableMembers[Math.floor(Math.random() * availableMembers.length)]
  
  return sendMessageToDormChat(chatId, {
    senderId: randomMember.id,
    senderName: randomMember.name,
    senderAvatar: randomMember.avatar,
    content: randomReply,
    type: 'text',
    isAnonymous: false
  })
}

// 数据变化监听器
let dataChangeListeners = []

export function setupDataChangeListener(callback) {
  dataChangeListeners.push(callback)
}

export function notifyDataChange() {
  dataChangeListeners.forEach(callback => {
    try {
      callback()
    } catch (error) {
      console.error('数据变化监听器执行失败:', error)
    }
  })
}

// 在数据修改时调用通知函数
const originalSetDormChatData = setDormChatData
export function setDormChatDataWithNotify(chatData) {
  originalSetDormChatData(chatData)
  notifyDataChange()
}

const originalSetDormChatMembers = setDormChatMembers
export function setDormChatMembersWithNotify(membersData) {
  originalSetDormChatMembers(membersData)
  notifyDataChange()
}

// ========== 用户备注功能 ==========

// 获取用户备注数据
export function getUserRemarks() {
  try {
    const data = localStorage.getItem(USER_REMARKS_KEY)
    return data ? JSON.parse(data) : {}
  } catch (error) {
    console.error('获取用户备注数据失败:', error)
    return {}
  }
}

// 设置用户备注数据
export function setUserRemarks(remarksData) {
  try {
    localStorage.setItem(USER_REMARKS_KEY, JSON.stringify(remarksData))
  } catch (error) {
    console.error('保存用户备注数据失败:', error)
  }
}

// 设置特定用户的备注
export function setUserRemark(userId, remark) {
  const remarks = getUserRemarks()
  if (remark && remark.trim()) {
    remarks[userId] = remark.trim()
  } else {
    delete remarks[userId]
  }
  setUserRemarks(remarks)
  notifyDataChange()
}

// 获取特定用户的备注
export function getUserRemark(userId) {
  const remarks = getUserRemarks()
  return remarks[userId] || ''
}

// ========== 消息操作功能 ==========

// 撤回消息
export function recallMessage(chatId, messageId) {
  const chatData = getDormChatData()
  const chat = chatData[chatId]
  
  if (!chat) return false
  
  const messageIndex = chat.messages.findIndex(msg => msg.id === messageId)
  if (messageIndex === -1) return false
  
  const message = chat.messages[messageIndex]
  
  // 检查是否可以撤回（只能撤回自己的消息，且在2分钟内）
  const currentUserId = 'student_001' // 当前用户ID
  if (message.senderId !== currentUserId) return false
  
  const messageTime = new Date(message.timestamp)
  const currentTime = new Date()
  const timeDiff = (currentTime - messageTime) / 1000 / 60 // 转换为分钟
  
  if (timeDiff > 2) return false // 超过2分钟不能撤回
  
  // 标记消息为已撤回
  message.isRecalled = true
  message.content = '消息已被撤回'
  
  setDormChatDataWithNotify(chatData)
  return true
}

// 复制消息内容
export function copyMessageContent(message) {
  if (message.isRecalled) return false
  
  let content = ''
  switch (message.type) {
    case 'text':
      content = message.content
      break
    case 'image':
      content = `[图片] ${message.content}`
      break
    case 'file':
      content = `[文件] ${message.fileName || message.content}`
      break
    default:
      content = message.content
  }
  
  // 使用现代浏览器的 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(content)
  } else {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = content
    document.body.appendChild(textArea)
    textArea.select()
    const success = document.execCommand('copy')
    document.body.removeChild(textArea)
    return Promise.resolve(success)
  }
}

// 引用消息发送新消息
export function sendQuotedMessage(chatId, quotedMessage, newContent, isAnonymous = false, currentUserInfo = null) {
  let senderInfo = null
  
  // 优先使用传入的当前用户信息
  if (currentUserInfo && currentUserInfo.id) {
    senderInfo = {
      id: currentUserInfo.id,
      name: currentUserInfo.name || '当前用户',
      avatar: currentUserInfo.avatar || ''
    }
  } else {
    // 降级：从成员列表中查找
    const membersData = getDormChatMembers()
    const members = membersData[chatId] || []
    const currentUser = members.find(m => m.id === 'student_001')
    if (currentUser) {
      senderInfo = {
        id: currentUser.id,
        name: currentUser.name,
        avatar: currentUser.avatar
      }
    }
  }
  
  // 如果仍然没有用户信息，使用默认值
  if (!senderInfo) {
    senderInfo = {
      id: 'unknown',
      name: '当前用户',
      avatar: ''
    }
  }
  
  const messageData = {
    senderId: senderInfo.id,
    senderName: isAnonymous ? '匿名用户' : senderInfo.name,
    senderAvatar: isAnonymous ? '' : senderInfo.avatar,
    content: newContent,
    type: 'text',
    isAnonymous: isAnonymous,
    quotedMessage: {
      id: quotedMessage.id,
      senderName: quotedMessage.senderName,
      content: quotedMessage.content,
      type: quotedMessage.type,
      isAnonymous: quotedMessage.isAnonymous
    }
  }
  
  return sendMessageToDormChat(chatId, messageData)
}

// ========== 用户资料功能 ==========

// 获取用户详细资料
export function getUserProfile(userId, chatId) {
  const membersData = getDormChatMembers()
  const members = membersData[chatId] || []
  const user = members.find(member => member.id === userId)
  
  if (!user) return null
  
  // 获取用户备注
  const remark = getUserRemark(userId)
  
  return {
    ...user,
    remark: remark,
    // 扩展资料信息
    profile: {
      phone: user.phone || '未设置',
      email: user.email || '未设置',
      qq: user.qq || '未设置',
      wechat: user.wechat || '未设置',
      hometown: user.hometown || '未设置',
      hobby: user.hobby || '未设置',
      motto: user.motto || '未设置'
    }
  }
}
