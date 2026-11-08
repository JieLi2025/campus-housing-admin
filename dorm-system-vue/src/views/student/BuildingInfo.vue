<template>
  <div class="building-info-page">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>🏢 楼栋动态</h3>
            <el-tag type="primary" effect="plain">{{ currentBuilding }}</el-tag>
          </div>
          <div class="header-right">
            <!-- 在线人数功能已移除 -->
          </div>
        </div>
      </template>

      <!-- 全体禁言提示 - 浮于消息上方 -->
      <div v-if="muteAll" class="mute-all-notice">
        <el-icon class="notice-icon"><InfoFilled /></el-icon>
        <span>当前已开启全体禁言，只有管理员可以发言</span>
      </div>
      <div v-else-if="isMuted" class="mute-notice">
        <el-icon class="notice-icon"><InfoFilled /></el-icon>
        <span>您已被禁言，无法发送消息</span>
      </div>

      <!-- 消息区域 -->
      <div class="messages-container">
        <div class="messages-wrapper" ref="messagesWrapper">
          <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
            <!-- 管理员消息 -->
            <div v-if="msg.role === 'admin'" class="admin-message">
              <div class="message-avatar" @contextmenu.prevent="openAdminInfoMenu($event, msg)">
                <el-avatar :size="40" :src="msg.avatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <el-tag size="small" type="warning" class="role-badge">管理员</el-tag>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-name">{{ msg.senderName }}</span>
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                </div>
                
                <div 
                  class="message-bubble admin-bubble"
                  @contextmenu.prevent="openMessageContextMenu($event, msg)"
                >
                  <span class="message-text">{{ msg.content }}</span>
                </div>
                
                <!-- 引用消息 - 移到气泡下方 -->
                <div v-if="msg.quotedMessage" class="quoted-message">
                  <div class="quoted-text">
                    <span class="quoted-sender">{{ msg.quotedMessage.senderName }}：</span>
                    <span class="quoted-content">{{ msg.quotedMessage.content }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 学生消息 -->
            <div v-else-if="msg.role === 'student'" class="student-message">
              <div class="message-avatar" @contextmenu.prevent="openStudentInfoMenu($event, msg)">
                <el-avatar :size="40" :src="msg.avatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-name">{{ msg.senderName }}</span>
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                </div>
                
                <div 
                  class="message-bubble student-bubble"
                  @contextmenu.prevent="openMessageContextMenu($event, msg)"
                >
                  <span class="message-text">{{ msg.content }}</span>
                </div>
                
                <!-- 引用消息 - 移到气泡下方 -->
                <div v-if="msg.quotedMessage" class="quoted-message">
                  <div class="quoted-text">
                    <span class="quoted-sender">{{ msg.quotedMessage.senderName }}：</span>
                    <span class="quoted-content">{{ msg.quotedMessage.content }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 自己的消息 -->
            <div v-else-if="msg.role === 'me'" class="my-message">
              <div class="message-content">
                <div class="message-header">
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                  <span class="message-name">{{ msg.senderName }}</span>
                </div>
                
                <div 
                  class="message-bubble my-bubble"
                  @contextmenu.prevent="openMessageContextMenu($event, msg)"
                >
                  <span class="message-text">{{ msg.content }}</span>
                </div>
                
                <!-- 引用消息 - 移到气泡下方 -->
                <div v-if="msg.quotedMessage" class="quoted-message">
                  <div class="quoted-text">
                    <span class="quoted-sender">{{ msg.quotedMessage.senderName }}：</span>
                    <span class="quoted-content">{{ msg.quotedMessage.content }}</span>
                  </div>
                </div>
              </div>
              <div class="message-avatar">
                <el-avatar :size="40" :src="currentUserAvatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <el-empty v-if="messages.length === 0" description="暂无消息，开始交流吧" :image-size="100" />
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <!-- 引用消息显示 -->
        <div v-if="quotedMessage" class="quote-preview">
          <div class="quote-info">
            <div class="quote-text-preview">
              <span class="quote-sender">{{ quotedMessage.senderName }}：</span>
              <span class="quote-content">{{ quotedMessage.content }}</span>
            </div>
          </div>
          <el-icon class="close-btn" @click="clearQuote"><Close /></el-icon>
        </div>
        
        <div class="input-toolbar">
          <el-upload
            ref="fileUpload"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            accept="image/*,.pdf,.doc,.docx,.txt"
            style="display: inline-block;"
          >
            <el-button circle size="small" :disabled="isUserMuted">
              <el-icon><Paperclip /></el-icon>
            </el-button>
          </el-upload>
        </div>
        
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            :placeholder="isUserMuted ? '您当前无法发送消息' : '输入消息，按 Enter 发送，Shift+Enter 换行'"
            @keydown="handleKeydown"
            resize="none"
            :disabled="isUserMuted"
          />
          <el-button type="primary" :disabled="!inputMessage.trim() || isUserMuted" @click="sendMessage">
            发送
          </el-button>
        </div>
      </div>
    </el-card>



    <!-- 管理员信息对话框 -->
    <el-dialog v-model="showAdminInfoDialog" title="👨‍💼 管理员信息" width="500px">
      <div class="admin-info">
        <div class="admin-avatar-section">
          <el-avatar :size="60" class="admin-avatar">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="admin-basic-info">
            <h3>张管理员</h3>
            <p class="admin-role">楼栋管理员</p>
          </div>
        </div>
        <div class="admin-details">
          <div class="detail-item">
            <el-icon class="detail-icon"><Location /></el-icon>
            <div class="detail-content">
              <div class="detail-label">办公地点</div>
              <div class="detail-value">1号楼101室</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><Clock /></el-icon>
            <div class="detail-content">
              <div class="detail-label">工作时间</div>
              <div class="detail-value">周一至周五 8:00-18:00</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><Phone /></el-icon>
            <div class="detail-content">
              <div class="detail-label">联系电话</div>
              <div class="detail-value">138-0000-0000</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><Message /></el-icon>
            <div class="detail-content">
              <div class="detail-label">微信</div>
              <div class="detail-value">zhang_admin_001</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showAdminInfoDialog = false">我知道了</el-button>
      </template>
    </el-dialog>

    <!-- 学生信息对话框 -->
    <el-dialog v-model="showStudentInfoDialog" title="👨‍🎓 学生信息" width="500px">
      <div class="student-info">
        <div class="student-avatar-section">
          <el-avatar :size="60" class="student-avatar">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="student-basic-info">
            <h3>{{ currentStudentInfo.name || '学生姓名' }}</h3>
            <p class="student-role">学生</p>
          </div>
        </div>
        <div class="student-details">
          <div class="detail-item">
            <el-icon class="detail-icon"><User /></el-icon>
            <div class="detail-content">
              <div class="detail-label">学号</div>
              <div class="detail-value">{{ currentStudentInfo.studentId || '2024001' }}</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><School /></el-icon>
            <div class="detail-content">
              <div class="detail-label">专业</div>
              <div class="detail-value">{{ currentStudentInfo.major || '计算机科学与技术' }}</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><OfficeBuilding /></el-icon>
            <div class="detail-content">
              <div class="detail-label">班级</div>
              <div class="detail-value">{{ currentStudentInfo.class || '计科2024-1班' }}</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><Location /></el-icon>
            <div class="detail-content">
              <div class="detail-label">宿舍</div>
              <div class="detail-value">{{ currentStudentInfo.dormitory || '1号楼101室' }}</div>
            </div>
          </div>
          <div class="detail-item">
            <el-icon class="detail-icon"><Phone /></el-icon>
            <div class="detail-content">
              <div class="detail-label">联系电话</div>
              <div class="detail-value">{{ currentStudentInfo.phone || '138-0000-0000' }}</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showStudentInfoDialog = false">我知道了</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script>
import { DocumentCopy } from '@element-plus/icons-vue'
import { getCurrentUser, getCurrentUserId } from '@/utils/auth.js'
import { 
  getBuildingGroupChat, 
  sendBuildingGroupMessage, 
  getBuildingGroupMessages,
  getBuildingGroupMembers,
  getMuteAllStatus,
  getUserProfile
} from '@/api/chat.js'
import { getMyBuildingAssignment } from '@/api/adminDormManagement.js'
import webSocketManager from '@/utils/websocket.js'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'

export default {
  name: 'StudentBuildingInfo',
  components: {
    DocumentCopy
  },
  data() {
    return {
      currentMessageContextMenu: null, // 当前打开的消息右键菜单
      currentBuilding: '1号楼',
      currentBuildingId: null, // 当前楼栋ID
      onlineCount: 0,
      inputMessage: '',
      messages: [],
      showAdminInfoDialog: false,
      showStudentInfoDialog: false,
      currentStudentInfo: {},
      quotedMessage: null,
      currentUserAvatar: defaultAvatar,
      loading: false,
      groupMembers: [], // 群成员列表
      // 禁言状态
      muteAll: false, // 全体禁言状态
      isMuted: false // 当前用户是否被禁言
    }
  },
  computed: {
    // 是否被禁言（全体禁言或个人禁言）
    isUserMuted() {
      return this.muteAll || this.isMuted
    },
  },
  async mounted() {
    await this.loadBuildingInfo()
    await this.loadGroupChat()
    this.scrollToBottom()
    this.connectWebSocket()
  },
  beforeUnmount() {
    // 断开WebSocket订阅
    if (this.currentBuildingId) {
      webSocketManager.unsubscribeFromBuildingGroup(this.currentBuildingId)
    }
    
    this.closeMessageContextMenu()
  },
  methods: {
    // 加载楼栋信息
    async loadBuildingInfo() {
      try {
        const response = await getMyBuildingAssignment()
        if (response.code === 200 && response.data && response.data.assignments && response.data.assignments.length > 0) {
          const assignment = response.data.assignments[0]
          this.currentBuildingId = assignment.buildingId
          this.currentBuilding = assignment.buildingName || '1号楼'
        } else {
          this.$message.warning('未找到您的楼栋分配信息')
        }
      } catch (error) {
        console.error('加载楼栋信息失败:', error)
        this.$message.error('加载楼栋信息失败')
      }
    },
    
    // 加载群聊
    async loadGroupChat() {
      if (!this.currentBuildingId) {
        return
      }
      
      this.loading = true
      try {
        // 获取或创建群聊会话
        const convResponse = await getBuildingGroupChat(this.currentBuildingId)
        if (convResponse.code !== 200) {
          this.$message.error('获取群聊失败：' + convResponse.message)
          return
        }
        
        // 加载消息历史
        const messagesResponse = await getBuildingGroupMessages(this.currentBuildingId, 1, 50)
        if (messagesResponse.code === 200 && messagesResponse.data) {
          this.messages = messagesResponse.data.map(msg => this.convertMessageToDisplay(msg))
        }
        
        // 加载成员列表
        const membersResponse = await getBuildingGroupMembers(this.currentBuildingId)
        if (membersResponse.code === 200 && membersResponse.data) {
          this.groupMembers = membersResponse.data
          
          // 检查当前用户是否被禁言，并获取当前用户头像
          const currentUser = getCurrentUser()
          if (currentUser) {
            const currentMember = membersResponse.data.find(m => m.userId === currentUser.id)
            this.isMuted = currentMember?.isMuted || false
            // 从成员列表中获取当前用户头像
            if (currentMember?.avatar) {
              this.currentUserAvatar = currentMember.avatar
            } else {
              // 如果成员列表中没有头像，从后端API获取
              try {
                const profileRes = await getUserProfile(currentUser.id)
                if (profileRes.code === 200 && profileRes.data && profileRes.data.avatar) {
                  this.currentUserAvatar = profileRes.data.avatar
                }
              } catch (e) {
                console.warn('从后端获取头像失败:', e)
              }
            }
          }
        }
        
        // 加载全体禁言状态
        await this.loadMuteAllStatus()
        
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } catch (error) {
        console.error('加载群聊失败:', error)
        this.$message.error('加载群聊失败')
      } finally {
        this.loading = false
      }
    },
    
    // 连接WebSocket
    connectWebSocket() {
      if (!this.currentBuildingId) {
        return
      }
      
      const userId = getCurrentUserId()
      if (!userId) {
        console.warn('无法获取用户ID，WebSocket连接失败')
        return
      }
      
      // 连接WebSocket
      webSocketManager.connect(
        userId,
        () => {
          console.log('WebSocket连接成功')
          // 订阅楼栋群聊
          webSocketManager.subscribeToBuildingGroup(this.currentBuildingId, (message) => {
            this.handleGroupMessage(message)
          })
          
        },
        (error) => {
          console.error('WebSocket连接失败:', error)
        }
      )
    },
    
    // 处理群聊消息
    handleGroupMessage(message) {
      // 检查消息是否已存在
      if (!this.messages.find(m => m.id === message.id)) {
        this.messages.push(this.convertMessageToDisplay(message))
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    
    // 转换消息格式为显示格式
    convertMessageToDisplay(msg) {
      const currentUser = getCurrentUser()
      // 判断是否是自己发送的消息：只根据senderId判断，不依赖isOwn（因为后端可能设置错误）
      const isOwn = currentUser && msg.senderId && msg.senderId === currentUser.id
      
      return {
        id: msg.id,
        role: isOwn ? 'me' : (msg.userType === 'admin' ? 'admin' : 'student'),
        senderId: msg.senderId,
        senderName: msg.senderName || '未知用户',
        avatar: msg.senderAvatar || '',
        content: msg.content || '',
        time: msg.createdAt ? new Date(msg.createdAt) : new Date(),
        quotedMessage: msg.quotedMessage ? {
          senderName: msg.quotedMessage.senderName,
          content: msg.quotedMessage.content
        } : null
      }
    },
    
    // 加载全体禁言状态
    async loadMuteAllStatus() {
      if (!this.currentBuildingId) {
        return
      }
      
      try {
        const response = await getMuteAllStatus(this.currentBuildingId)
        if (response.code === 200) {
          this.muteAll = response.data || false
        }
      } catch (error) {
        console.error('加载全体禁言状态失败:', error)
      }
    },
    
    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim() || !this.currentBuildingId) return
      
      try {
        const messageData = {
          content: this.inputMessage.trim(),
          messageType: 'text',
          quotedMessageId: this.quotedMessage ? this.quotedMessage.id : null
        }
        
        const response = await sendBuildingGroupMessage(this.currentBuildingId, messageData)
        // 如果成功，添加消息到列表
        const msg = response.data
        
        // 创建消息对象，格式与WebSocket消息一致
        const newMessage = this.convertMessageToDisplay(msg)
        
        // 检查消息是否已存在（避免WebSocket重复添加）
        const existingMessage = this.messages.find(m => m.id === newMessage.id)
        if (!existingMessage) {
          this.messages.push(newMessage)
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
        
        // 清空输入框和引用
        this.inputMessage = ''
        this.quotedMessage = null
        // 注意：如果后端返回错误，request拦截器已经显示了错误提示，这里不需要再显示
      } catch (error) {
        // request拦截器已经显示了错误提示，这里只记录日志，不再显示提示
        console.error('发送消息失败:', error)
      }
    },
    
    // 处理键盘事件
    handleKeydown(e) {
      // Enter 发送消息，Shift+Enter 换行
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },
    
    // 滚动到底部
    scrollToBottom() {
      const wrapper = this.$refs.messagesWrapper
      if (!wrapper) {
        return
      }
      
      // 使用requestAnimationFrame和延迟确保DOM更新后再滚动
      requestAnimationFrame(() => {
        wrapper.scrollTop = wrapper.scrollHeight
        setTimeout(() => {
          wrapper.scrollTop = wrapper.scrollHeight
        }, 200)
      })
    },
    
    // 格式化时间
    formatTime(time) {
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      
      return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
    },

    // 复制消息
    copyMessage(message) {
      // 复制消息内容到剪贴板
      if (navigator.clipboard) {
        navigator.clipboard.writeText(message.content).then(() => {
          this.$message.success('消息已复制到剪贴板')
        }).catch(() => {
          this.$message.error('复制失败')
        })
      } else {
        // 兼容老版本浏览器
        const textArea = document.createElement('textarea')
        textArea.value = message.content
        document.body.appendChild(textArea)
        textArea.select()
        try {
          document.execCommand('copy')
          this.$message.success('消息已复制到剪贴板')
        } catch (err) {
          this.$message.error('复制失败')
        }
        document.body.removeChild(textArea)
      }
    },

    // 引用消息
    quoteMessage(message) {
      this.quotedMessage = {
        id: message.id,
        senderName: message.senderName,
        content: message.content,
        time: message.time
      }
      
      // 聚焦到输入框
      this.$nextTick(() => {
        const textareaEl = this.$el.querySelector('.input-wrapper textarea')
        if (textareaEl) {
          textareaEl.focus()
        }
      })
      
      this.$message.success('消息已引用')
    },

    // 清除引用
    clearQuote() {
      this.quotedMessage = null
    },
    
    // 格式化截止时间
    formatDeadline(deadline) {
      if (!deadline) return '长期有效'
      const end = new Date(deadline)
      if (isNaN(end.getTime())) return '长期有效'
      
      const now = new Date()
      const diffMs = end - now
      
      if (diffMs <= 0) return `已到期`
      
      const days = Math.floor(diffMs / 86400000)
      const hours = Math.floor((diffMs % 86400000) / 3600000)
      
      if (days > 0) return `剩余 ${days} 天`
      if (hours > 0) return `剩余 ${hours} 小时`
      
      const minutes = Math.floor(diffMs / 60000)
      return `剩余 ${minutes} 分钟`
    },
    
    // 处理文件上传
    handleFileChange(file) {
      if (file.size > 10 * 1024 * 1024) { // 10MB限制
        this.$message.error('文件大小不能超过10MB')
        return
      }
      
      const fileType = file.raw.type
      if (fileType.startsWith('image/')) {
        this.$message.success('图片文件已选择，可以发送')
        // 这里可以添加图片预览或直接发送逻辑
      } else {
        this.$message.success('文件已选择，可以发送')
        // 这里可以添加文件发送逻辑
      }
    },

    // 打开管理员信息菜单
    openAdminInfoMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'admin-context-menu'
      
      // 查看管理员信息选项
      const infoItem = document.createElement('div')
      infoItem.style.cssText = `
        padding: 8px 16px;
        cursor: pointer;
        font-size: 14px;
        color: #606266;
        transition: background-color 0.2s;
        white-space: nowrap;
      `
      infoItem.textContent = '查看管理员信息'
      
      infoItem.addEventListener('mouseenter', () => {
        infoItem.style.backgroundColor = '#f5f7fa'
      })
      
      infoItem.addEventListener('mouseleave', () => {
        infoItem.style.backgroundColor = 'transparent'
      })
      
      infoItem.addEventListener('click', () => {
        this.showAdminInfoDialog = true
        this.closeMessageContextMenu()
      })
      
      menu.appendChild(infoItem)
      
      // 基础样式
      menu.style.cssText = `
        position: fixed;
        background: white;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        padding: 4px 0;
      `
      
      // 临时添加到页面以获取宽高
      menu.style.visibility = 'hidden'
      document.body.appendChild(menu)
      
      const menuRect = menu.getBoundingClientRect()
      const menuWidth = menuRect.width
      const menuHeight = menuRect.height
      
      // 计算菜单位置
      const x = event.clientX
      const y = event.clientY
      const windowWidth = window.innerWidth
      const windowHeight = window.innerHeight
      
      let finalX = x
      let finalY = y
      
      // 防止菜单超出屏幕右边界
      if (x + menuWidth > windowWidth) {
        finalX = windowWidth - menuWidth - 10
      }
      
      // 防止菜单超出屏幕下边界
      if (y + menuHeight > windowHeight) {
        finalY = y - menuHeight - 5
      }
      
      // 设置最终位置
      menu.style.left = finalX + 'px'
      menu.style.top = finalY + 'px'
      menu.style.visibility = 'visible'
      
      // 点击其他地方关闭菜单
      const closeMenu = (e) => {
        if (!menu.contains(e.target)) {
          this.closeMessageContextMenu()
          document.removeEventListener('click', closeMenu)
        }
      }
      
      setTimeout(() => {
        document.addEventListener('click', closeMenu)
      }, 100)
      
      // 保存菜单引用
      this.currentContextMenu = menu
    },

    // 打开学生信息菜单
    openStudentInfoMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 设置当前学生信息
      this.currentStudentInfo = {
        name: message.senderName,
        studentId: '2024001',
        major: '计算机科学与技术',
        class: '计科2024-1班',
        dormitory: '1号楼101室',
        phone: '138-0000-0000'
      }
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'student-context-menu'
      
      // 查看学生信息选项
      const infoItem = document.createElement('div')
      infoItem.style.cssText = `
        padding: 8px 16px;
        cursor: pointer;
        font-size: 14px;
        color: #606266;
        transition: background-color 0.2s;
        white-space: nowrap;
      `
      infoItem.textContent = '查看学生信息'
      
      infoItem.addEventListener('mouseenter', () => {
        infoItem.style.backgroundColor = '#f5f7fa'
      })
      
      infoItem.addEventListener('mouseleave', () => {
        infoItem.style.backgroundColor = 'transparent'
      })
      
      infoItem.addEventListener('click', () => {
        this.showStudentInfoDialog = true
        this.closeMessageContextMenu()
      })
      
      menu.appendChild(infoItem)
      
      // 基础样式
      menu.style.cssText = `
        position: fixed;
        background: white;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        padding: 4px 0;
      `
      
      // 临时添加到页面以获取宽高
      menu.style.visibility = 'hidden'
      document.body.appendChild(menu)
      
      const menuRect = menu.getBoundingClientRect()
      const menuWidth = menuRect.width
      const menuHeight = menuRect.height
      
      // 计算菜单位置
      const x = event.clientX
      const y = event.clientY
      const windowWidth = window.innerWidth
      const windowHeight = window.innerHeight
      
      let finalX = x
      let finalY = y
      
      // 防止菜单超出屏幕右边界
      if (x + menuWidth > windowWidth) {
        finalX = windowWidth - menuWidth - 10
      }
      
      // 防止菜单超出屏幕下边界
      if (y + menuHeight > windowHeight) {
        finalY = y - menuHeight - 5
      }
      
      // 设置最终位置
      menu.style.left = finalX + 'px'
      menu.style.top = finalY + 'px'
      menu.style.visibility = 'visible'
      
      // 点击其他地方关闭菜单
      const closeMenu = (e) => {
        if (!menu.contains(e.target)) {
          this.closeMessageContextMenu()
          document.removeEventListener('click', closeMenu)
        }
      }
      
      setTimeout(() => {
        document.addEventListener('click', closeMenu)
      }, 100)
      
      // 保存菜单引用
      this.currentContextMenu = menu
    },

    // 打开消息右键菜单
    openMessageContextMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'message-context-menu'
      
      // 复制功能
      const copyItem = document.createElement('div')
      copyItem.style.cssText = `
        padding: 8px 16px;
        cursor: pointer;
        font-size: 14px;
        color: #606266;
        transition: background-color 0.2s;
        white-space: nowrap;
      `
      copyItem.textContent = '复制'
      
      copyItem.addEventListener('mouseenter', () => {
        copyItem.style.backgroundColor = '#f5f7fa'
      })
      
      copyItem.addEventListener('mouseleave', () => {
        copyItem.style.backgroundColor = 'transparent'
      })
      
      copyItem.addEventListener('click', () => {
        this.copyMessage(message)
        this.closeMessageContextMenu()
      })
      
      menu.appendChild(copyItem)
      
      // 引用功能
      const quoteItem = document.createElement('div')
      quoteItem.style.cssText = `
        padding: 8px 16px;
        cursor: pointer;
        font-size: 14px;
        color: #606266;
        transition: background-color 0.2s;
        white-space: nowrap;
        border-top: 1px solid #f0f0f0;
      `
      quoteItem.textContent = '引用'
      
      quoteItem.addEventListener('mouseenter', () => {
        quoteItem.style.backgroundColor = '#f5f7fa'
      })
      
      quoteItem.addEventListener('mouseleave', () => {
        quoteItem.style.backgroundColor = 'transparent'
      })
      
      quoteItem.addEventListener('click', () => {
        this.quoteMessage(message)
        this.closeMessageContextMenu()
      })
      
      menu.appendChild(quoteItem)
      
      
      // 基础样式
      menu.style.cssText = `
        position: fixed;
        background: white;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        padding: 4px 0;
      `
      
      // 临时添加到页面以获取宽高
      menu.style.visibility = 'hidden'
      document.body.appendChild(menu)
      
      const menuWidth = menu.offsetWidth
      const menuHeight = menu.offsetHeight
      const windowWidth = window.innerWidth
      const windowHeight = window.innerHeight
      
      // 智能定位：避免菜单超出屏幕或覆盖内容
      let left = event.clientX + 5  // 默认在鼠标右侧
      let top = event.clientY + 5   // 默认在鼠标下方
      
      // 如果右侧空间不够，显示在左侧
      if (left + menuWidth > windowWidth) {
        left = event.clientX - menuWidth - 5
      }
      
      // 如果下方空间不够，显示在上方
      if (top + menuHeight > windowHeight) {
        top = event.clientY - menuHeight - 5
      }
      
      // 确保不超出屏幕边界
      left = Math.max(5, Math.min(left, windowWidth - menuWidth - 5))
      top = Math.max(5, Math.min(top, windowHeight - menuHeight - 5))
      
      menu.style.left = left + 'px'
      menu.style.top = top + 'px'
      menu.style.visibility = 'visible'
      
      this.currentMessageContextMenu = menu
      
      // 点击其他地方关闭菜单
      const closeMenu = (e) => {
        if (!menu.contains(e.target)) {
          this.closeMessageContextMenu()
          document.removeEventListener('click', closeMenu)
        }
      }
      
      setTimeout(() => {
        document.addEventListener('click', closeMenu)
      }, 100)
    },

    // 关闭消息右键菜单
    closeMessageContextMenu() {
      // 关闭消息右键菜单
      if (this.currentMessageContextMenu) {
        try {
          document.body.removeChild(this.currentMessageContextMenu)
        } catch (e) {
          // 菜单可能已经被移除
        }
        this.currentMessageContextMenu = null
      }
      
      // 关闭管理员/学生信息菜单
      if (this.currentContextMenu) {
        try {
          document.body.removeChild(this.currentContextMenu)
        } catch (e) {
          // 菜单可能已经被移除
        }
        this.currentContextMenu = null
      }
    }
  },

  beforeUnmount() {
    this.closeMessageContextMenu()
  }
}
</script>

<style scoped>
.building-info-page {
  padding: 0;
  margin: 0;
  height: calc(100vh - 70px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 公告横幅 */
.announcement-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f5f5f5 100%);
  border-bottom: 1px solid #e4e7ed;
}

.banner-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.banner-content {
  flex: 1;
  min-width: 0;
}

.banner-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.banner-text {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 消息容器 */
.messages-container {
  flex: 1;
  overflow: hidden;
  padding: 20px;
  background: #f8f9fa;
}

.messages-wrapper {
  height: 100%;
  overflow-y: auto;
  padding-right: 8px;
}

/* 消息项 */
.message-item {
  margin-bottom: 20px;
}

/* 自己的消息需要靠右对齐 */
.message-item.me {
  display: flex;
  justify-content: flex-end;
}

.admin-message,
.student-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.my-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  justify-content: flex-end;
  width: 100%;
}

.message-avatar {
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 0.2s ease;
  margin-top: 20px;
}

.message-avatar:hover {
  transform: scale(1.05);
}

/* 管理员头像特殊样式 */
.admin-message .message-avatar {
  position: relative;
}

/* 学生头像特殊样式 */
.student-message .message-avatar {
  position: relative;
}

.role-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
}

.message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  max-width: 85%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.my-message .message-content {
  align-items: flex-end;
}

.my-message .message-header {
  flex-direction: row-reverse;
}

.message-name {
  font-weight: 500;
  color: #606266;
}

.message-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
  max-width: 100%;
  width: fit-content;
  box-sizing: border-box;
}

.admin-bubble {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #303133;
  border: 1px solid #ffecb3;
}

.student-bubble {
  background: #ffffff;
  color: #303133;
  border: 1px solid #e4e7ed;
}

.my-bubble {
  background: linear-gradient(135deg, #409eff 0%, #5cb3ff 100%);
  color: #ffffff;
}

/* 输入区域 */
.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 16px 20px;
  background: #ffffff;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-wrapper .el-input {
  flex: 1;
}

.input-wrapper .el-button {
  height: 40px;
  padding: 0 24px;
}

/* 禁言提示样式 */
/* 禁言提示样式 - 浮于消息上方 */
.mute-all-notice,
.mute-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: #fef0f0;
  border-bottom: 1px solid #fde2e2;
  color: #f56c6c;
  font-size: 14px;
}

.mute-all-notice .notice-icon,
.mute-notice .notice-icon {
  font-size: 16px;
  color: #909399;
}

/* 管理员信息对话框 */
.admin-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.admin-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.admin-avatar {
  flex-shrink: 0;
}

.admin-basic-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.admin-role {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.admin-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.detail-icon {
  font-size: 16px;
  color: #409eff;
  flex-shrink: 0;
}

.detail-content {
  flex: 1;
}

.detail-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

/* 学生信息对话框 */
.student-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.student-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.student-avatar {
  flex-shrink: 0;
}

.student-basic-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.student-role {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

/* 引用消息样式 - 参考微信/QQ设计，发送人和内容在同一行，显示在消息气泡下方 */
.quoted-message {
  background: rgba(255, 255, 255, 0.95);
  border-left: 4px solid #409eff;
  padding: 8px 12px;
  margin-top: 6px;
  border-radius: 6px;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(64, 158, 255, 0.3);
  max-width: 100%;
  width: fit-content;
}

.quoted-text {
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.quoted-sender {
  color: #409eff;
  font-weight: 600;
}

.quoted-content {
  color: #606266;
  font-weight: 400;
}

/* 引用预览区域 - 参考微信/QQ设计 */
.quote-preview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10px 14px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 1px solid #409eff;
  border-left: 4px solid #409eff;
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.quote-info {
  flex: 1;
  min-width: 0;
}

.quote-text-preview {
  font-size: 13px;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.quote-sender {
  color: #409eff;
  font-weight: 600;
}

.quote-content {
  color: #606266;
  font-weight: 400;
}

.close-btn {
  font-size: 16px;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #606266;
}

.message-text {
  line-height: 1.4;
  font-size: 14px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.contact-icon {
  font-size: 24px;
  color: #409eff;
}

.contact-details {
  flex: 1;
}

.contact-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.contact-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 滚动条样式 */
.messages-wrapper::-webkit-scrollbar {
  width: 6px;
}

.messages-wrapper::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.messages-wrapper::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* 公告弹窗样式 - 简约版 */
:deep(.announcement-popup-dialog) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.announcement-popup-dialog .el-dialog__header) {
  padding: 16px 20px;
  margin: 0;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.announcement-popup-dialog .el-dialog__body) {
  padding: 20px;
}

:deep(.announcement-popup-dialog .el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}

.popup-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.popup-icon {
  font-size: 18px;
  color: #409eff;
}

.popup-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.popup-content {
  padding: 0;
}

.announcement-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.announcement-content {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 16px;
}

.announcement-time {
  display: flex;
}

.popup-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.dont-show-checkbox {
  font-size: 13px;
  color: #909399;
}
</style>
