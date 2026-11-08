<template>

  <div class="building-info-page">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>🏢 楼栋动态</h3>
            <el-select 
              v-model="selectedBuilding" 
              placeholder="选择楼栋" 
              style="width: 150px;" 
              size="default"
              @change="loadGroupChat"
            >
              <el-option 
                v-for="building in managedBuildings" 
                :key="building.id"
                :label="building.name" 
                :value="building.id.toString()" 
              />
            </el-select>
          </div>
          <div class="header-right">
            <el-tag type="success">
              <el-icon><User /></el-icon>
              楼栋学生 {{ studentCount }} 人
            </el-tag>
            <el-tooltip :content="muteAll ? '取消全体禁言' : '全体禁言'" placement="bottom">
              <el-button 
                :type="muteAll ? 'danger' : 'warning'" 
                size="small" 
                @click="toggleMuteAll"
              >
                <el-icon><TurnOff v-if="muteAll" /><Bell v-else /></el-icon>
                {{ muteAll ? '已全体禁言' : '全体禁言' }}
              </el-button>
            </el-tooltip>
            <el-button type="primary" size="small" @click="showPublishDialog = true">
              <el-icon><Bell /></el-icon>
              发布公告
            </el-button>
          </div>
        </div>
      </template>

      <!-- 全体禁言提示 -->
      <div v-if="muteAll" class="mute-all-notice">
        <el-icon class="notice-icon"><InfoFilled /></el-icon>
        <span>当前已开启全体禁言，只有管理员可以发言</span>
      </div>

      <!-- 消息区域 -->
      <div class="messages-container">
        <div class="messages-wrapper" ref="messagesWrapper">
          <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ 'recalled-item': msg.isRecalled }">
            <!-- 管理员消息（我发送的） -->
            <div v-if="msg.role === 'admin' && msg.isMe && !msg.isRecalled" class="my-message">
              <div class="message-content">
                <div class="message-header">
                  <span class="message-name">我（管理员）</span>
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                </div>
                <!-- 公告消息 -->
                <div v-if="msg.isAnnouncement" class="message-bubble my-bubble announcement-bubble">
                  <div class="announcement-header">
                    <div class="announcement-icon">
                      <el-icon><Bell /></el-icon>
                    </div>
                    <span class="announcement-label">楼栋公告</span>
                  </div>
                  <div class="announcement-title-text">
                    {{ msg.announcementTitle }}
                  </div>
                  <div class="announcement-content">
                    {{ msg.content }}
                  </div>
                </div>
                <!-- 普通消息（可撤回和复制） -->
                <div 
                  v-else
                  class="message-bubble my-bubble"
                  @contextmenu.prevent="openMessageContextMenu($event, msg)"
                >
                  {{ msg.content }}
                </div>
              </div>
              <div class="message-avatar">
                <el-avatar :size="32">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <el-tag size="small" type="warning" class="role-badge">管理员</el-tag>
              </div>
            </div>

            <!-- 撤回消息提示 -->
            <div v-else-if="msg.role === 'admin' && msg.isMe && msg.isRecalled" class="recalled-message-wrapper">
              <div class="recalled-message">
                <el-icon class="recall-icon"><InfoFilled /></el-icon>
                <span class="recall-text">你撤回了一条消息</span>
              </div>
            </div>

            <!-- 学生消息 -->
            <div v-else-if="!msg.isRecalled" class="student-message">
              <div class="message-avatar">
                <el-avatar :size="32" :src="msg.avatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <el-tag 
                  v-if="isUserMuted(msg.senderId)" 
                  size="small" 
                  type="danger" 
                  class="mute-badge"
                >
                  已禁言
                </el-tag>
              </div>
              <div class="message-content">
                <div class="message-header-top">
                  <span class="message-name">{{ msg.senderName }}</span>
                  <span class="message-room">{{ msg.room }}</span>
                </div>
                <div 
                  class="message-bubble student-bubble"
                  @contextmenu.prevent="openStudentMessageContextMenu($event, msg)"
                >
                  {{ msg.content }}
                </div>
                <div class="message-header-bottom">
                  <span class="message-time">{{ formatTime(msg.time) }}</span>
                  <el-button class="reply-btn" size="small" @click="replyToStudent(msg)">
                    <el-icon><ChatDotRound /></el-icon>
                    回复
                  </el-button>
                  <el-button 
                    class="mute-btn" 
                    size="small" 
                    :type="isUserMuted(msg.senderId) ? 'success' : 'warning'"
                    @click="toggleMuteUser(msg)"
                  >
                    <el-icon><Microphone v-if="isUserMuted(msg.senderId)" /><TurnOff v-else /></el-icon>
                    {{ isUserMuted(msg.senderId) ? '解除禁言' : '禁言' }}
                  </el-button>
                </div>
              </div>
            </div>
      </div>


          <!-- 空状态 -->
          <el-empty v-if="messages.length === 0" description="暂无消息" :image-size="100" />
          </div>
        </div>


      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-toolbar">
          <el-button size="small" @click="showQuickReplies">
            <el-icon><Collection /></el-icon>
            快捷回复
          </el-button>
          <el-button size="small" @click="handleFileUpload">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <el-button size="small" @click="showMutedList" v-if="mutedUsers.length > 0">
            <el-icon><User /></el-icon>
            禁言列表 ({{ mutedUsers.length }})
          </el-button>
        </div>
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="发送消息给楼栋学生（前端演示）"
            @keydown="handleKeydown"
            resize="none"
          />
          <el-button 
            type="primary" 
            :disabled="!inputMessage.trim()" 
            @click="sendMessage"
            size="small"
            class="send-message-btn"
          >
            发送
          </el-button>
        </div>
      </div>
    </el-card>


    <!-- 发布公告对话框 -->
    <el-dialog v-model="showPublishDialog" title="📢 发布楼栋公告" width="600px">
      <el-form :model="announcementForm" label-width="100px">
        <el-form-item label="公告标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容" required>
          <el-input 
            v-model="announcementForm.content" 
            type="textarea" 
            :rows="6"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker
            v-model="announcementForm.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="publishAnnouncement">发布公告</el-button>
      </template>
    </el-dialog>

    <!-- 快捷回复对话框 -->
    <el-dialog v-model="showQuickReplyDialog" title="快捷回复管理" width="500px">
      <div class="quick-reply-header">
        <el-button type="primary" size="small" @click="showAddReplyDialog">
          <el-icon><Plus /></el-icon>
          添加快捷回复
        </el-button>
      </div>
      <div class="quick-replies">
        <div 
          v-for="reply in quickReplies" 
          :key="reply.id"
          class="quick-reply-item"
        >
          <span class="reply-content" @click="useQuickReply(reply.content)">
            {{ reply.content }}
          </span>
          <el-button 
            type="danger" 
            size="small"
            circle
            @click="confirmDeleteReply(reply)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <div v-if="quickReplies.length === 0" class="empty-placeholder">
        <el-empty description="暂无快捷回复，点击上方按钮添加" :image-size="80" />
      </div>
    </el-dialog>

    <!-- 添加快捷回复对话框 -->
    <el-dialog v-model="addReplyDialogVisible" title="添加快捷回复" width="400px">
      <el-form :model="addReplyForm" label-width="80px">
        <el-form-item label="回复内容" required>
          <el-input
            v-model="addReplyForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入快捷回复内容"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addReplyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddReply">确定</el-button>
      </template>
    </el-dialog>

    <!-- 禁言列表对话框 -->
    <el-dialog v-model="showMutedListDialog" title="禁言列表" width="500px">
      <div v-if="mutedUsers.length === 0" class="empty-placeholder">
        <el-empty description="暂无禁言用户" :image-size="80" />
      </div>
      <div v-else class="muted-users-list">
        <div 
          v-for="user in mutedUsers" 
          :key="user.id"
          class="muted-user-item"
        >
          <div class="muted-user-info">
            <el-avatar :size="32" :src="user.avatar">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div class="muted-user-details">
              <span class="muted-user-name">{{ user.name }}</span>
              <span class="muted-user-room">{{ user.room }}</span>
            </div>
          </div>
          <el-button 
            type="success" 
            size="small"
            @click="unmuteUserById(user.id)"
          >
            <el-icon><Microphone /></el-icon>
            解除禁言
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showMutedListDialog = false">关闭</el-button>
        <el-button 
          type="warning" 
          @click="clearAllMutedUsers"
          v-if="mutedUsers.length > 0"
        >
          清空禁言列表
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>

import { Plus, Delete, Close, RefreshLeft, DocumentCopy, InfoFilled, TurnOff, Microphone } from '@element-plus/icons-vue'
import { getQuickReplies, addQuickReply, deleteQuickReply } from '@/utils/quickReplyData.js'
import { 
  getBuildingGroupChat, 
  sendBuildingGroupMessage, 
  getBuildingGroupMessages,
  getBuildingGroupMembers,
  toggleMuteUser,
  toggleMuteAll as toggleMuteAllApi,
  getMuteAllStatus
} from '@/api/chat.js'
import { getMyBuildings } from '@/api/adminDormManagement.js'
import { createNotification } from '@/api/notification.js'
import { createAnnouncement } from '@/api/announcement.js'
import { addNotification as addAdminNotification } from '@/utils/adminMessageData.js'
import webSocketManager from '@/utils/websocket.js'
import { getCurrentUserId } from '@/utils/auth.js'

export default {

  components: {
    Plus,
    Delete,
    Close,
    RefreshLeft,
    DocumentCopy,
    InfoFilled,
    TurnOff,
    Microphone
  },
  name: 'AdminBuildingInfo',
  data() {
    return {
      currentMessageContextMenu: null, // 当前打开的消息右键菜单
      selectedBuilding: '1', // 楼栋ID（字符串，用于选择器）
      selectedBuildingId: null, // 楼栋ID（数字，用于API）
      managedBuildings: [], // 管理的楼栋列表
      inputMessage: '',
      muteAll: false, // 全体禁言状态
      mutedUsers: [], // 禁言用户列表 [{id, name, room, avatar, mutedAt}]
      showMutedListDialog: false, // 显示禁言列表对话框
      messages: [],
      showPublishDialog: false,
      announcementForm: {
        title: '',
        content: '',
        deadline: null
      },
      showQuickReplyDialog: false,
      quickReplies: [],
      addReplyDialogVisible: false,
      addReplyForm: {
        content: ''
      },
      loading: false,
      publishingAnnouncement: false // 防止重复发布公告
    }
  },
  watch: {
    selectedBuilding(newVal) {
      // 当选择的楼栋改变时，重新加载群聊
      const building = this.managedBuildings.find(b => b.id.toString() === newVal)
      if (building) {
        this.selectedBuildingId = building.id
        this.loadGroupChat()
      }
    }
  },
  async mounted() {
    await this.loadManagedBuildings()
    this.scrollToBottom()
    this.loadQuickReplies()
    this.loadMutedUsers()
    this.connectWebSocket()
  },
  beforeUnmount() {
    // 组件销毁前清理右键菜单和WebSocket订阅
    if (this.selectedBuildingId) {
      webSocketManager.unsubscribeFromBuildingGroup(this.selectedBuildingId)
    }
    this.closeMessageContextMenu()
  },
  methods: {
    // 加载管理的楼栋列表
    async loadManagedBuildings() {
      try {
        const response = await getMyBuildings()
        if (response.code === 200 && response.data && response.data.length > 0) {
          this.managedBuildings = response.data.map(b => ({
            id: b.id,
            name: b.name ,
            label: b.name 
          }))
          
          // 设置默认选中的楼栋
          if (this.managedBuildings.length > 0) {
            this.selectedBuilding = this.managedBuildings[0].id.toString()
            this.selectedBuildingId = this.managedBuildings[0].id
            await this.loadGroupChat()
          }
        } else {
          this.$message.warning('您没有管理的楼栋')
        }
      } catch (error) {
        console.error('加载管理的楼栋失败:', error)
        this.$message.error('加载楼栋列表失败')
      }
    },
    
    // 加载群聊
    async loadGroupChat() {
      if (!this.selectedBuildingId) {
        return
      }
      
      this.loading = true
      try {
        // 获取或创建群聊会话
        const convResponse = await getBuildingGroupChat(this.selectedBuildingId)
        if (convResponse.code !== 200) {
          this.$message.error('获取群聊失败：' + convResponse.message)
          return
        }
        
        // 加载消息历史
        const messagesResponse = await getBuildingGroupMessages(this.selectedBuildingId, 1, 50)
        if (messagesResponse.code === 200 && messagesResponse.data) {
          this.messages = messagesResponse.data.map(msg => this.convertMessageToDisplay(msg))
        }
        
        // 加载成员列表
        const membersResponse = await getBuildingGroupMembers(this.selectedBuildingId)
        if (membersResponse.code === 200 && membersResponse.data) {
          // 成员列表用于其他用途（如显示成员信息）
        }
        
        // 加载禁言用户列表
        await this.loadMutedUsers()
        
        // 加载全体禁言状态
        await this.loadMuteAllStatus()
        
        // 重新订阅WebSocket（取消所有旧的订阅，然后订阅新的）
        // 取消所有楼栋群聊订阅
        this.managedBuildings.forEach(building => {
          webSocketManager.unsubscribeFromBuildingGroup(building.id)
        })
        
        // 订阅当前选中的楼栋群聊
        if (this.selectedBuildingId) {
          webSocketManager.subscribeToBuildingGroup(this.selectedBuildingId, (message) => {
            this.handleGroupMessage(message)
          })
        }
        
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
      const userId = getCurrentUserId('admin')
      if (!userId) {
        console.warn('无法获取用户ID，WebSocket连接失败')
        return
      }
      
      webSocketManager.connect(
        userId,
        () => {
          console.log('WebSocket连接成功')
          if (this.selectedBuildingId) {
            webSocketManager.subscribeToBuildingGroup(this.selectedBuildingId, (message) => {
              this.handleGroupMessage(message)
            })
          }
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
      const currentUserId = getCurrentUserId('admin')
      // 判断是否是自己发送的消息：只根据senderId判断，不依赖isOwn（因为后端可能设置错误）
      const isOwn = currentUserId && msg.senderId && msg.senderId === currentUserId
      
      return {
        id: msg.id,
        role: isOwn ? 'admin' : (msg.userType === 'admin' ? 'admin' : 'student'),
        isMe: isOwn,
        senderId: msg.senderId,
        senderName: msg.senderName || '未知用户',
        avatar: msg.senderAvatar || '',
        content: msg.content || '',
        time: msg.createdAt ? new Date(msg.createdAt) : new Date(),
        isAnnouncement: false,
        room: msg.room || ''
      }
    },
    
    // 加载禁言用户列表（从后端）
    async loadMutedUsers() {
      if (!this.selectedBuildingId) {
        return
      }
      
      try {
        const response = await getBuildingGroupMembers(this.selectedBuildingId)
        if (response.code === 200 && response.data) {
          // 从后端获取成员列表，筛选出被禁言的用户
          this.mutedUsers = response.data
            .filter(member => member.isMuted)
            .map(member => ({
              id: member.userId,
              name: member.userName,
              room: member.room || '',
              avatar: member.avatar || '',
              mutedAt: new Date().toISOString()
            }))
        }
      } catch (e) {
        console.error('加载禁言列表失败:', e)
      }
    },

    // 切换全体禁言
    async toggleMuteAll() {
      if (!this.selectedBuildingId) {
        this.$message.warning('请先选择楼栋')
        return
      }
      
      const newMuteAllStatus = !this.muteAll
      try {
        const response = await toggleMuteAllApi(this.selectedBuildingId, newMuteAllStatus)
        if (response.code === 200) {
          this.muteAll = newMuteAllStatus
          if (this.muteAll) {
            this.$message.success('已开启全体禁言')
          } else {
            this.$message.info('已取消全体禁言')
          }
        }
      } catch (error) {
        console.error('切换全体禁言失败:', error)
      }
    },
    
    // 加载全体禁言状态
    async loadMuteAllStatus() {
      if (!this.selectedBuildingId) {
        return
      }
      
      try {
        const response = await getMuteAllStatus(this.selectedBuildingId)
        if (response.code === 200) {
          this.muteAll = response.data || false
        }
      } catch (error) {
        console.error('加载全体禁言状态失败:', error)
      }
    },

    // 判断用户是否被禁言
    isUserMuted(userId) {
      return this.mutedUsers.some(user => user.id === userId)
    },

    // 切换单个用户禁言状态
    async toggleMuteUser(message) {
      const userId = message.senderId
      const userName = message.senderName
      const userRoom = message.room
      const userAvatar = message.avatar
      const isCurrentlyMuted = this.isUserMuted(userId)

      try {
        // 调用后端API设置禁言状态
        await toggleMuteUser(this.selectedBuildingId, userId, !isCurrentlyMuted)
        // API调用成功后才更新本地状态和显示提示
        if (isCurrentlyMuted) {
          // 解除禁言：只更新本地状态
          const userIndex = this.mutedUsers.findIndex(user => user.id === userId)
          if (userIndex > -1) {
            this.mutedUsers.splice(userIndex, 1)
          }
          this.$message.success(`已解除 ${userName} 的禁言`)
        } else {
          // 禁言
          this.mutedUsers.push({
            id: userId,
            name: userName,
            room: userRoom,
            avatar: userAvatar,
            mutedAt: new Date().toISOString()
          })
          this.$message.success(`已禁言 ${userName}`)
        }
        // 注意：如果后端返回错误，request拦截器已经显示了错误提示，这里不需要再显示
      } catch (error) {
        // request拦截器已经显示了错误提示，这里只记录日志，不再显示提示
        console.error('切换禁言状态失败:', error)
      }
    },

    // 通过ID解除用户禁言（用于禁言列表对话框）
    async unmuteUserById(userId) {
      try {
        await toggleMuteUser(this.selectedBuildingId, userId, false)
        // API调用成功后才更新本地状态和显示提示
        const userIndex = this.mutedUsers.findIndex(user => user.id === userId)
        if (userIndex > -1) {
          const userName = this.mutedUsers[userIndex].name
          this.mutedUsers.splice(userIndex, 1)
          this.$message.success(`已解除 ${userName} 的禁言`)
        }
        // 注意：如果后端返回错误，request拦截器已经显示了错误提示，这里不需要再显示
      } catch (error) {
        // request拦截器已经显示了错误提示，这里只记录日志，不再显示提示
        console.error('解除禁言失败:', error)
      }
    },

    // 显示禁言列表
    showMutedList() {
      this.showMutedListDialog = true
    },

    // 清空禁言列表
    async clearAllMutedUsers() {
      this.$confirm('确定要清空所有禁言用户吗？', '清空确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        // 批量解除所有用户的禁言
        const promises = this.mutedUsers.map(user => 
          toggleMuteUser(this.selectedBuildingId, user.id, false)
        )
        
        try {
          await Promise.all(promises)
          this.mutedUsers = []
          this.$message.success('已清空禁言列表')
          this.showMutedListDialog = false
        } catch (error) {
          console.error('清空禁言列表失败:', error)
          this.$message.error('操作失败')
        }
      }).catch(() => {
        // 用户取消
      })
    },

    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim() || !this.selectedBuildingId) return
      
      try {
        const messageData = {
          content: this.inputMessage.trim(),
          messageType: 'text'
        }
        
        const response = await sendBuildingGroupMessage(this.selectedBuildingId, messageData)
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
        
        // 清空输入框
        this.inputMessage = ''
        // 注意：如果后端返回错误，request拦截器已经显示了错误提示，这里不需要再显示
      } catch (error) {
        // request拦截器已经显示了错误提示，这里只记录日志，不再显示提示
        console.error('发送消息失败:', error)
      }
    },
    
    // 处理键盘事件
    handleKeydown(e) {
      if (e.ctrlKey && e.key === 'Enter') {
        e.preventDefault()
        this.sendMessage()
      }
    },
    
    // 回复学生
    replyToStudent(msg) {
      this.inputMessage = `@${msg.senderName} `
      // 聚焦输入框
      this.$nextTick(() => {
        const textarea = this.$el.querySelector('textarea')
        if (textarea) textarea.focus()
      })
    },
    
    // 上传文件
    handleFileUpload() {
      this.$message.info('文件上传功能（前端演示）')
    },
    
    // 发布公告
    async publishAnnouncement() {
      if (!this.announcementForm.title || !this.announcementForm.content) {
        this.$message.warning('请填写完整的公告信息')
        return
      }
      
      // 防止重复点击
      if (this.publishingAnnouncement) {
        return
      }
      this.publishingAnnouncement = true
      
      const buildingName = this.managedBuildings.find(b => b.id === this.selectedBuildingId)?.name || '楼栋'
      
      try {
        // 调用后端API创建公告
        const announcementPayload = {
          title: this.announcementForm.title,
          subtitle: '',
          content: this.announcementForm.content,
          extraPoints: '',
          buildingIds: [this.selectedBuildingId],
          expireDate: this.announcementForm.deadline || null,
          status: '已发布'
        }
        
        const res = await createAnnouncement(announcementPayload)
        if (res.code !== 200) {
          this.$message.error(res.message || '公告发布失败')
          this.publishingAnnouncement = false
          return
        }
        
        this.showPublishDialog = false
        this.announcementForm = { title: '', content: '', deadline: null }
        this.$message.success('公告发布成功')
        
      } catch (error) {
        console.error('发布公告失败:', error)
        this.$message.error('发布公告失败：' + (error.message || '网络错误'))
      } finally {
        this.publishingAnnouncement = false
      }
    },
    
    // 加载快捷回复
    loadQuickReplies() {
      this.quickReplies = getQuickReplies()
    },
    
    // 显示快捷回复
    showQuickReplies() {
      this.showQuickReplyDialog = true
    },
    
    // 使用快捷回复
    useQuickReply(reply) {
      this.inputMessage = reply
      this.showQuickReplyDialog = false
    },
    
    // 显示添加快捷回复对话框
    showAddReplyDialog() {
      this.addReplyForm.content = ''
      this.addReplyDialogVisible = true
    },
    
    // 确认添加快捷回复
    confirmAddReply() {
      if (!this.addReplyForm.content.trim()) {
        this.$message.warning('请输入快捷回复内容')
        return
      }
      
      const result = addQuickReply(this.addReplyForm.content)
      if (result) {
        this.$message.success('添加成功')
        this.loadQuickReplies()
        this.addReplyDialogVisible = false
        this.addReplyForm.content = ''
      } else {
        this.$message.error('添加失败')
      }
    },
    
    // 确认删除快捷回复
    confirmDeleteReply(reply) {
      this.$confirm(`确定要删除快捷回复"${reply.content}"吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const result = deleteQuickReply(reply.id)
        if (result) {
          this.$message.success('删除成功')
          this.loadQuickReplies()
        } else {
          this.$message.error('删除失败')
        }
      }).catch(() => {
        // 用户取消删除
      })
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

    // 处理消息操作
    handleMessageAction(command, message) {
      switch (command) {
        case 'recall':
          this.recallMessage(message)
          break
        case 'recall-disabled':
          this.$message.warning('消息发送超过2分钟，无法撤回')
          break
        case 'copy':
          this.copyMessage(message)
          break
      }
    },

    // 判断消息是否可以撤回
    canRecallMessage(message) {
      // 只有自己发送的消息且发送时间在2分钟内才能撤回
      const now = new Date()
      const messageTime = new Date(message.time)
      const diffMinutes = (now - messageTime) / (1000 * 60)
      return diffMinutes <= 120 // 120分钟内可以撤回（方便测试虚拟数据）
    },

    // 撤回消息
    recallMessage(message) {
      this.$confirm('确定要撤回这条消息吗？', '撤回消息', {
        confirmButtonText: '撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 标记消息为已撤回
        message.isRecalled = true
        message.recallTime = new Date().toISOString()
        this.$message.success('消息已撤回')
      }).catch(() => {
        // 用户取消撤回
      })
    },

    // 打开学生消息右键菜单
    openStudentMessageContextMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'message-context-menu'
      
      // 菜单项
      const menuItems = []
      
      // 学生消息可以禁言
      const isMuted = this.isUserMuted(message.senderId)
      menuItems.push({ 
        text: isMuted ? '解除禁言' : '禁言用户', 
        command: 'mute',
        color: isMuted ? '#67c23a' : '#f56c6c'
      })
      
      // 所有消息都可以复制
      menuItems.push({ text: '复制', command: 'copy' })
      
      menuItems.forEach(item => {
        const menuItem = document.createElement('div')
        menuItem.style.cssText = `
          padding: 8px 16px;
          cursor: pointer;
          font-size: 14px;
          color: ${item.color || '#606266'};
          transition: background-color 0.2s;
          white-space: nowrap;
        `
        menuItem.textContent = item.text
        
        menuItem.addEventListener('mouseenter', () => {
          menuItem.style.backgroundColor = '#f5f7fa'
        })
        
        menuItem.addEventListener('mouseleave', () => {
          menuItem.style.backgroundColor = 'transparent'
        })
        
        menuItem.addEventListener('click', () => {
          if (item.command === 'mute') {
            this.toggleMuteUser(message)
          } else {
            this.handleMessageAction(item.command, message)
          }
          this.closeMessageContextMenu()
        })
        
        menu.appendChild(menuItem)
      })
      
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
      
      // 智能定位
      let left = event.clientX + 5
      let top = event.clientY + 5
      
      if (left + menuWidth > windowWidth) {
        left = event.clientX - menuWidth - 5
      }
      
      if (top + menuHeight > windowHeight) {
        top = event.clientY - menuHeight - 5
      }
      
      left = Math.max(5, Math.min(left, windowWidth - menuWidth - 5))
      top = Math.max(5, Math.min(top, windowHeight - menuHeight - 5))
      
      menu.style.left = left + 'px'
      menu.style.top = top + 'px'
      menu.style.visibility = 'visible'
      
      this.currentMessageContextMenu = menu
      
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

    // 打开管理员消息右键菜单
    openMessageContextMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'message-context-menu'
      
      // 菜单项
      const menuItems = []
      
      // 管理员自己的消息可以撤回
      if (message.role === 'admin' && message.isMe && this.canRecallMessage(message)) {
        menuItems.push({ text: '撤回', command: 'recall' })
      }
      
      // 所有消息都可以复制
      menuItems.push({ text: '复制', command: 'copy' })
      
      menuItems.forEach(item => {
        const menuItem = document.createElement('div')
        menuItem.style.cssText = `
          padding: 8px 16px;
          cursor: pointer;
          font-size: 14px;
          color: #606266;
          transition: background-color 0.2s;
          white-space: nowrap;
        `
        menuItem.textContent = item.text
        
        menuItem.addEventListener('mouseenter', () => {
          menuItem.style.backgroundColor = '#f5f7fa'
        })
        
        menuItem.addEventListener('mouseleave', () => {
          menuItem.style.backgroundColor = 'transparent'
        })
        
        menuItem.addEventListener('click', () => {
          this.handleMessageAction(item.command, message)
          this.closeMessageContextMenu()
        })
        
        menu.appendChild(menuItem)
      })
      
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
      if (this.currentMessageContextMenu) {
        try {
          document.body.removeChild(this.currentMessageContextMenu)
        } catch (e) {
          // 菜单可能已经被移除
        }
        this.currentMessageContextMenu = null
      }
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
    }
  }
}
</script>

<style scoped>

.building-info-page {
  height: calc(100vh - 70px);
  background: #f5f5f5;
  margin: 0;
  padding: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  box-shadow: none;
  overflow: hidden;
  margin: 0;
}

:deep(.el-card__header) {
  padding: 12px 24px;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  height: 60px;
  box-sizing: border-box;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 10;
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  min-height: 0;
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
  gap: 12px;
}

/* 消息容器 */
.messages-container {
  flex: 1;
  background: #f8f9fa;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.messages-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
  min-height: 0;
}

/* 消息项 */
.message-item {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.student-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  width: 100%;
}

.my-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  width: 100%;
  justify-content: flex-end;
}

.message-avatar {
  position: relative;
  flex-shrink: 0;
  margin-top: 20px;
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
  word-wrap: break-word;
  max-width: 60%;
  min-width: 100px;
}

.my-message .message-content {
  align-items: flex-end;
}

.student-message .message-content {
  align-items: flex-start;
}

.student-message .message-avatar {
  margin-top: 20px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.my-message .message-header {
  justify-content: flex-end;
}

/* 学生消息顶部信息（姓名和房间） */
.message-header-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}

/* 学生消息底部信息（时间、回复、禁言按钮） */
.message-header-bottom {
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.message-header-bottom .message-time {
  margin-right: 4px;
}

.message-header-bottom .reply-btn {
  margin-left: 0;
  margin-right: 2px;
}

.message-header-bottom .mute-btn {
  margin-left: 0;
}

.message-name {
  font-weight: 500;
  color: #303133;
  font-size: 13px;
}

.message-room {
  padding: 2px 6px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 11px;
  color: #606266;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-bubble {
  padding: 8px 12px;
  border-radius: 16px;
  line-height: 1.5;
  word-wrap: break-word;
  white-space: pre-wrap;
  position: relative;
  font-size: 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-break: break-word;
  max-width: 100%;
  overflow-wrap: break-word;
  transition: all 0.2s ease;
}

/* 学生消息气泡下方需要一些间距 */
.student-message .message-bubble {
  margin-bottom: 0;
}

.student-bubble {
  background: #ffffff;
  color: #303133;
}

.my-bubble {
  background: #409eff;
  color: #ffffff;
}

/* 公告消息特殊样式 */
.announcement-bubble {
  min-width: 300px;
  max-width: 500px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  padding: 0 !important;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4) !important;
}

.announcement-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.15);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.announcement-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  font-size: 13px;
}

.announcement-label {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.5px;
}

.announcement-title-text {
  padding: 14px 16px 10px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  line-height: 1.4;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.2);
}

.announcement-content {
  padding: 14px 16px 16px;
  line-height: 1.6;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-btn {
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
  background: transparent;
  border: none;
  color: #409eff;
  transition: all 0.3s;
}

.reply-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.reply-btn .el-icon {
  color: inherit;
  font-size: 12px;
}

/* 输入区域 */
.input-area {
  border-top: 1px solid #e4e7ed;
  padding: 10px 24px;
  background: #ffffff;
  flex-shrink: 0;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
  max-height: 120px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  min-width: 0;
}

.input-wrapper .el-input {
  flex: 1;
  min-width: 0;
}

.input-wrapper :deep(.el-textarea__inner) {
  resize: none;
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  background: #f8f9fa;
  overflow-y: auto;
  overflow-x: hidden;
  word-wrap: break-word;
}

.send-message-btn {
  height: 36px;
  border-radius: 18px;
  padding: 0 24px;
  font-weight: 500;
  flex-shrink: 0;
  align-self: flex-end;
}

/* 快捷回复 */
.quick-reply-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.quick-replies {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
}

.quick-reply-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: background-color 0.3s;
  gap: 12px;
}

.quick-reply-item:hover {
  background: #e8f4ff;
}

.quick-reply-item .reply-content {
  flex: 1;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}

.quick-reply-item .reply-content:hover {
  color: #409eff;
}

.quick-reply-item .el-button {
  flex-shrink: 0;
}

.empty-placeholder {
  padding: 20px 0;
  text-align: center;
}

/* 滚动条样式 - 智能显示隐藏 */
.messages-wrapper::-webkit-scrollbar {
  width: 6px;
}

.messages-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.messages-wrapper::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 3px;
  transition: all 0.3s ease;
}

/* 悬停时显示滚动条 */
.messages-wrapper:hover::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

/* 滚动时显示滚动条 */
.messages-wrapper.scrolling::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.4);
}

/* 悬停和滚动时加深滚动条 */
.messages-wrapper:hover::-webkit-scrollbar-thumb:hover,
.messages-wrapper.scrolling::-webkit-scrollbar-thumb:hover,
.messages-wrapper::-webkit-scrollbar-thumb:active {
  background: rgba(0, 0, 0, 0.6);
}

/* 为消息容器添加悬停状态检测 */
.messages-container:hover .messages-wrapper::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

.messages-container:hover .messages-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

/* 消息气泡悬停效果 */
.message-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 撤回消息样式 */
.recalled-message-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.recalled-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 16px;
  color: #909399;
  font-size: 13px;
}

.recall-icon {
  font-size: 16px;
  color: #909399;
}

.recall-text {
  font-style: italic;
}

.recalled-item {
  opacity: 0.8;
}

/* 全体禁言提示 */
.mute-all-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 24px;
  background: #fef0f0;
  border-bottom: 1px solid #fbc4c4;
  color: #f56c6c;
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
}

.mute-all-notice .notice-icon {
  font-size: 16px;
}

/* 禁言徽章 */
.mute-badge {
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  padding: 0 4px;
  height: 16px;
  line-height: 16px;
}

/* 禁言按钮 */
.mute-btn {
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
  background: transparent;
  border: none;
  color: #f56c6c;
  transition: all 0.3s;
}

.mute-btn:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.mute-btn.el-button--success {
  color: #67c23a;
}

.mute-btn.el-button--success:hover {
  background: #f0f9ff;
  color: #67c23a;
}

.mute-btn .el-icon {
  color: inherit;
  font-size: 12px;
}

/* 禁言列表对话框 */
.muted-users-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px;
}

.muted-user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s;
  gap: 12px;
}

.muted-user-item:hover {
  background: #fef0f0;
  transform: translateX(2px);
}

.muted-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.muted-user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.muted-user-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.muted-user-room {
  font-size: 12px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .announcement-bubble {
    min-width: 260px;
    max-width: 400px;
  }
  
  .announcement-header {
    padding: 8px 12px;
  }
  
  .announcement-title-text {
    padding: 12px;
    font-size: 14px;
  }
  
  .announcement-content {
    padding: 12px;
    font-size: 13px;
  }
  
  .input-area {
    min-height: 100px;
    max-height: 100px;
    padding: 8px 16px;
  }
}
</style>

