/**
 * 统一的邀请服务
 * 处理各种邀请功能，避免代码重复
 */

/**
 * 发送宿舍队伍邀请
 * @param {Object} targetUser 目标用户对象
 * @param {string} message 邀请消息（可选）
 * @returns {Promise<boolean>} 是否发送成功
 */
export async function sendDormInvite(targetUser, message = '') {
  try {
    // 导入聊天消息工具函数
    const { addPrivateChatMessage, getPrivateChats, savePrivateChats } = await import('@/utils/studentMessageData.js')
    
    // 生成邀请消息ID
    const messageId = Date.now().toString()
    const currentTime = new Date().toISOString()
    
    // 创建宿舍邀请消息对象
    const inviteMessage = {
      id: messageId,
      type: 'dorm_invite',
      content: message || '邀请你组成宿舍队伍，一起申请宿舍分配。',
      time: currentTime,
      isOwn: true,
      inviteStatus: 'pending'
    }
    
    // 检查是否已存在该联系人的聊天记录
    // 使用 userId（系统用户ID）而不是 id（学生表主键），确保聊天对象正确
    const privateChats = getPrivateChats()
    const chatUserId = targetUser.userId || targetUser.id
    const contactId = `student_${chatUserId}`
    
    if (!privateChats[contactId]) {
      privateChats[contactId] = {
        messages: [],
        unreadCount: 0,
        contactInfo: {
          id: chatUserId,
          name: targetUser.name,
          avatar: targetUser.avatar,
          type: 'student',
          major: targetUser.major,
          className: targetUser.className
        }
      }
      savePrivateChats(privateChats)
    }
    
    // 添加邀请消息到聊天记录
    addPrivateChatMessage(contactId, inviteMessage)
    
    return true
  } catch (error) {
    console.error('发送宿舍邀请失败:', error)
    return false
  }
}

/**
 * 跳转到聊天页面并选中指定联系人
 * @param {string} contactId 联系人ID
 * @param {Object} router Vue路由对象
 * @param {string} fromRoute 来源路由
 */
export function navigateToChat(contactId, router, fromRoute = 'roommate-match') {
  // 使用sessionStorage存储跳转数据，提高安全性
  const redirectData = {
    chatId: contactId,
    from: fromRoute,
    timestamp: Date.now()
  }
  
  sessionStorage.setItem('chatRedirect', JSON.stringify(redirectData))
  
  // 跳转到聊天页面
  router.push({
    path: '/student/chat-messages',
    query: { redirect: 'true' }
  })
}

/**
 * 处理邀请结果通知
 * @param {Object} messageInstance Vue消息实例
 * @param {Object} notifyInstance Vue通知实例
 * @param {string} targetUserName 目标用户名
 * @param {boolean} success 是否成功
 */
export function handleInviteNotification(messageInstance, notifyInstance, targetUserName, success) {
  if (success) {
    messageInstance.success(`已向${targetUserName}发送宿舍队伍邀请`)
    
    notifyInstance({
      title: '邀请已发送',
      message: `已向${targetUserName}发送邀请，您可以在聊天消息页面查看和管理邀请`,
      type: 'success',
      duration: 4000
    })
  } else {
    messageInstance.error('发送邀请失败，请重试')
  }
}
