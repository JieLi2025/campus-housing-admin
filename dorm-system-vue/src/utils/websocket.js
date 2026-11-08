/**
 * WebSocket工具类
 * 用于管理聊天WebSocket连接
 */
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { getToken } from '@/utils/auth'

class WebSocketManager {
  constructor() {
    this.stompClient = null
    this.socket = null
    this.connected = false
    this.subscriptions = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.messageHandlers = new Map()
    this.pendingSubscriptions = new Map() // 待订阅队列：key -> {type, id, callback}
  }

  /**
   * 连接WebSocket
   */
  connect(userId, onConnected, onError) {
    // 如果已经连接，确保处理器已注册
    if (this.connected && this.stompClient && this.stompClient.connected) {
      console.log('WebSocket已连接，确保订阅和处理器已注册')
      // 确保订阅已建立
      if (!this.subscriptions.has('messages')) {
        this.subscribeToMessages(userId)
      }
      if (!this.subscriptions.has('conversations')) {
        this.subscribeToConversations(userId)
      }
      // 调用onConnected回调，让组件注册处理器
      if (onConnected) {
        onConnected()
      }
      return
    }

    const token = getToken()
    if (!token) {
      console.error('未找到Token，无法连接WebSocket')
      return
    }

    // 创建SockJS连接
    this.socket = new SockJS('http://localhost:8080/api/ws/chat')
    
    // 创建STOMP客户端（使用新的@stomp/stompjs库）
    this.stompClient = new Client({
      webSocketFactory: () => this.socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => {
        // 禁用调试日志
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket连接成功')
        this.connected = true
        this.reconnectAttempts = 0
        
        console.log(`开始订阅消息队列，用户ID: ${userId}`)
        // 订阅用户消息队列
        this.subscribeToMessages(userId)
        
        // 订阅会话更新队列
        this.subscribeToConversations(userId)
        
        // 订阅团队更新队列
        this.subscribeToTeamUpdates(userId)
        
        // 处理待订阅的群聊
        this.processPendingSubscriptions()
        
        if (onConnected) {
          console.log('调用onConnected回调')
          onConnected()
        } else {
          console.warn('onConnected回调未定义')
        }
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP错误:', frame)
        this.connected = false
        
        if (onError) {
          onError(new Error(frame.headers['message'] || 'STOMP连接失败'))
        }
        
        // 尝试重连
        this.attemptReconnect(userId, onConnected, onError)
      },
      onWebSocketError: (event) => {
        console.error('WebSocket连接失败:', event)
        this.connected = false
        
        if (onError) {
          onError(event)
        }
        
        // 尝试重连
        this.attemptReconnect(userId, onConnected, onError)
      }
    })
    
    // 激活连接
    this.stompClient.activate()
  }

  /**
   * 订阅消息队列
   */
  subscribeToMessages(userId) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('WebSocket未连接，无法订阅消息')
      return
    }
    
    // 重要：使用 /user/queue/messages 而不是 /user/{userId}/queue/messages
    // Spring会自动将 /user/queue/messages 转换为 /user/{principalName}/queue/messages
    const destination = `/user/queue/messages`
    console.log(`准备订阅消息队列: ${destination} (用户ID: ${userId})`)
    
    try {
      const subscription = this.stompClient.subscribe(
        destination,
        (message) => {
          try {
            console.log('========== 收到WebSocket消息 ==========')
            console.log('原始消息body:', message.body)
            console.log('消息头信息:', message.headers)
            console.log('消息命令:', message.command)
            console.log('订阅目标:', destination)
            
            if (!message.body) {
              console.warn('消息body为空，跳过处理')
              return
            }
            
            const data = JSON.parse(message.body)
            console.log('解析后的消息数据:', data)
            console.log('消息类型:', data.type)
            
            // 处理不同类型的消息
            if (data.type === 'MESSAGE_RECALLED') {
              // 消息撤回事件
              console.log('处理消息撤回事件')
              this.handleMessageRecalled(data)
            } else if (data.type === 'NEW_MESSAGE') {
              // 新消息
              console.log('处理新消息事件，类型: NEW_MESSAGE')
              this.handleNewMessage(data)
            } else {
              // 未知类型，也尝试作为新消息处理
              console.log('未知消息类型，尝试作为新消息处理:', data.type)
              this.handleNewMessage(data)
            }
            console.log('========== 消息处理完成 ==========')
          } catch (error) {
            console.error('处理WebSocket消息失败:', error)
            console.error('原始消息body:', message.body)
            console.error('错误堆栈:', error.stack)
          }
        },
        {
          // 添加额外的订阅选项
          id: `sub-${userId}-messages-${Date.now()}`
        }
      )
      
      if (subscription) {
        this.subscriptions.set('messages', subscription)
        console.log(`✅ 成功订阅消息队列: ${destination}, subscription ID: ${subscription.id || 'N/A'}`)
      } else {
        console.error(`❌ 订阅消息队列失败: ${destination}`)
      }
    } catch (error) {
      console.error(`❌ 订阅消息队列异常: ${destination}`, error)
      console.error('错误堆栈:', error.stack)
    }
  }

  /**
   * 订阅会话更新队列
   */
  subscribeToConversations(userId) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('WebSocket未连接，无法订阅会话更新')
      return
    }
    
    // 重要：使用 /user/queue/conversations 而不是 /user/{userId}/queue/conversations
    // Spring会自动将 /user/queue/conversations 转换为 /user/{principalName}/queue/conversations
    const destination = `/user/queue/conversations`
    console.log(`准备订阅会话更新队列: ${destination} (用户ID: ${userId})`)
    
    const subscription = this.stompClient.subscribe(
      destination,
      (message) => {
        try {
          console.log('收到会话更新消息:', message.body)
          const data = JSON.parse(message.body)
          this.handleConversationUpdated(data)
        } catch (error) {
          console.error('处理会话更新失败:', error)
        }
      }
    )
    
    this.subscriptions.set('conversations', subscription)
    console.log(`✅ 成功订阅会话更新队列: ${destination}`)
  }

  /**
   * 处理新消息
   */
  handleNewMessage(messageData) {
    console.log('WebSocket收到新消息（原始数据）:', messageData)
    
    // 如果消息数据包含type和data字段，提取data
    let actualMessage = messageData
    if (messageData.type === 'NEW_MESSAGE' && messageData.data) {
      actualMessage = messageData.data
      console.log('提取消息数据:', actualMessage)
    }
    
    // 触发消息处理器
    const handlers = this.messageHandlers.get('newMessage') || []
    console.log(`触发 ${handlers.length} 个消息处理器`)
    handlers.forEach(handler => {
      try {
        handler(actualMessage)
      } catch (error) {
        console.error('消息处理器执行失败:', error)
      }
    })
  }

  /**
   * 处理消息撤回
   */
  handleMessageRecalled(data) {
    const handlers = this.messageHandlers.get('messageRecalled') || []
    handlers.forEach(handler => {
      try {
        handler(data)
      } catch (error) {
        console.error('消息撤回处理器执行失败:', error)
      }
    })
  }

  /**
   * 处理会话更新
   */
  handleConversationUpdated(data) {
    const handlers = this.messageHandlers.get('conversationUpdated') || []
    handlers.forEach(handler => {
      try {
        handler(data)
      } catch (error) {
        console.error('会话更新处理器执行失败:', error)
      }
    })
  }

  /**
   * 订阅团队更新队列
   */
  subscribeToTeamUpdates(userId) {
    if (!this.stompClient || !this.connected) {
      console.warn('WebSocket未连接，无法订阅团队更新队列')
      return
    }
    
    const destination = `/user/queue/team-updates`
    console.log(`准备订阅团队更新队列: ${destination} (用户ID: ${userId})`)
    
    const subscription = this.stompClient.subscribe(
      destination,
      (message) => {
        try {
          console.log('收到团队更新消息:', message.body)
          const data = JSON.parse(message.body)
          this.handleTeamUpdate(data)
        } catch (error) {
          console.error('处理团队更新失败:', error)
        }
      }
    )
    
    this.subscriptions.set('team-updates', subscription)
    console.log(`✅ 成功订阅团队更新队列: ${destination}`)
  }

  /**
   * 处理团队更新
   */
  handleTeamUpdate(data) {
    const handlers = this.messageHandlers.get('team-updates') || []
    handlers.forEach(handler => {
      try {
        handler(data)
      } catch (error) {
        console.error('团队更新处理器执行失败:', error)
      }
    })
  }

  /**
   * 注册消息处理器
   */
  on(event, handler) {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, [])
    }
    this.messageHandlers.get(event).push(handler)
  }

  /**
   * 注册消息处理器（别名）
   */
  registerMessageHandler(event, handler) {
    this.on(event, handler)
  }

  /**
   * 移除消息处理器
   */
  off(event, handler) {
    if (this.messageHandlers.has(event)) {
      const handlers = this.messageHandlers.get(event)
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  /**
   * 尝试重连
   */
  attemptReconnect(userId, onConnected, onError) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('WebSocket重连次数已达上限')
      return
    }

    this.reconnectAttempts++
    console.log(`WebSocket尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

    setTimeout(() => {
      this.disconnect()
      this.connect(userId, onConnected, onError)
    }, this.reconnectDelay)
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.subscriptions) {
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe()
      })
      this.subscriptions.clear()
    }

    if (this.stompClient) {
      this.stompClient.deactivate()
      this.stompClient = null
    }

    if (this.socket) {
      this.socket.close()
      this.socket = null
    }

    this.connected = false
    this.messageHandlers.clear()
    this.pendingSubscriptions.clear()
  }

  /**
   * 检查连接状态
   */
  isConnected() {
    return this.connected && this.stompClient && this.stompClient.connected
  }
  
  /**
   * 订阅楼栋群聊消息
   * 如果WebSocket未连接，会加入待订阅队列，连接成功后自动订阅
   */
  subscribeToBuildingGroup(buildingId, callback) {
    const key = `building_group_${buildingId}`
    
    // 如果已经订阅过，先取消订阅
    const existingSubscription = this.subscriptions.get(key)
    if (existingSubscription) {
      existingSubscription.unsubscribe()
    }
    
    // 如果已连接，直接订阅
    if (this.isConnected()) {
      return this._doSubscribeToBuildingGroup(buildingId, callback)
    }
    
    // 如果未连接，加入待订阅队列
    console.log('WebSocket未连接，加入待订阅队列')
    this.pendingSubscriptions.set(key, {
      type: 'building_group',
      id: buildingId,
      callback: callback
    })
    
    return null
  }
  
  /**
   * 处理待订阅队列
   */
  processPendingSubscriptions() {
    if (!this.isConnected()) {
      return
    }
    
    this.pendingSubscriptions.forEach((pending, key) => {
      if (pending.type === 'building_group') {
        this._doSubscribeToBuildingGroup(pending.id, pending.callback)
      } else if (pending.type === 'dorm_group') {
        this._doSubscribeToDormGroup(pending.id, pending.callback)
      }
    })
    
    // 清空待订阅队列
    this.pendingSubscriptions.clear()
  }
  
  /**
   * 执行订阅楼栋群聊（内部方法）
   */
  _doSubscribeToBuildingGroup(buildingId, callback) {
    if (!this.stompClient || !this.stompClient.connected) {
      return null
    }
    
    const topic = `/topic/building/${buildingId}/group`
    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        if (data.type === 'GROUP_MESSAGE' && data.data) {
          callback(data.data)
        } else {
          // 如果没有type字段，直接使用data
          callback(data)
        }
      } catch (error) {
        console.error('处理楼栋群聊消息失败:', error)
      }
    })
    
    const key = `building_group_${buildingId}`
    this.subscriptions.set(key, subscription)
    console.log(`已订阅楼栋群聊: ${topic}`)
    return subscription
  }
  
  /**
   * 订阅宿舍群聊消息
   * 如果WebSocket未连接，会加入待订阅队列，连接成功后自动订阅
   */
  subscribeToDormGroup(dormGroupId, callback) {
    const key = `dorm_group_${dormGroupId}`
    
    // 如果已经订阅过，先取消订阅
    const existingSubscription = this.subscriptions.get(key)
    if (existingSubscription) {
      existingSubscription.unsubscribe()
    }
    
    // 如果已连接，直接订阅
    if (this.isConnected()) {
      return this._doSubscribeToDormGroup(dormGroupId, callback)
    }
    
    // 如果未连接，加入待订阅队列
    console.log('WebSocket未连接，加入待订阅队列')
    this.pendingSubscriptions.set(key, {
      type: 'dorm_group',
      id: dormGroupId,
      callback: callback
    })
    
    return null
  }
  
  /**
   * 执行订阅宿舍群聊（内部方法）
   */
  _doSubscribeToDormGroup(dormGroupId, callback) {
    if (!this.stompClient || !this.stompClient.connected) {
      return null
    }
    
    const topic = `/topic/dorm-group/${dormGroupId}/group`
    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        if (data.type === 'GROUP_MESSAGE' && data.data) {
          callback(data.data)
        } else {
          // 如果没有type字段，直接使用data
          callback(data)
        }
      } catch (error) {
        console.error('处理宿舍群聊消息失败:', error)
      }
    })
    
    const key = `dorm_group_${dormGroupId}`
    this.subscriptions.set(key, subscription)
    console.log(`已订阅宿舍群聊: ${topic}`)
    return subscription
  }
  
  /**
   * 取消订阅楼栋群聊
   */
  unsubscribeFromBuildingGroup(buildingId) {
    const key = `building_group_${buildingId}`
    const subscription = this.subscriptions.get(key)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(key)
    }
  }
  
  /**
   * 取消订阅宿舍群聊
   */
  unsubscribeFromDormGroup(dormGroupId) {
    const key = `dorm_group_${dormGroupId}`
    const subscription = this.subscriptions.get(key)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(key)
    }
  }
}

// 创建单例
const webSocketManager = new WebSocketManager()

export default webSocketManager

