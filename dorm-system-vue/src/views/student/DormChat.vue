<template>
  <div class="dorm-chat-page">
    <div class="chat-layout">
      <!-- 左侧聊天区域 -->
      <div class="chat-main">
        <el-card class="chat-card">
          <!-- 群聊头部 -->
          <template #header>
            <div class="chat-header">
              <div class="chat-info">
                <div class="chat-avatar">
                  <el-avatar :size="40" :src="dormChat?.avatar">
                    <el-icon><House /></el-icon>
                  </el-avatar>
                </div>
                <div class="chat-details">
                  <h3 class="chat-name">{{ dormChat?.name || '宿舍群聊' }}</h3>
                  <span class="member-count">{{ dormChat?.memberCount || 0 }}人</span>
                </div>
              </div>
              <div class="chat-controls">
                <el-switch v-model="anonymousMode" active-text="匿名模式" />
                <el-button size="small" @click="showMemberList">
                  <el-icon><User /></el-icon>
                  成员
                </el-button>
                <el-button size="small" @click="showSettings">
                  <el-icon><Bell /></el-icon>
                  公告
                </el-button>
              </div>
            </div>
          </template>
      
          <!-- 无群聊状态 -->
          <div v-if="!dormChat" class="no-chat-state">
            <div class="no-chat-content">
              <el-icon class="no-chat-icon" size="64"><House /></el-icon>
              <h3>暂无宿舍群聊</h3>
              <p>您还没有加入宿舍群聊，请联系宿管或室友邀请您加入</p>
              <div class="no-chat-actions">
                <el-button type="primary" @click="loadDormChatData">刷新</el-button>
              </div>
            </div>
          </div>
          
          <div v-else class="chat-container">
            <!-- 群公告滚动条 - 浮动在消息上方 -->
            <div v-if="currentAnnouncement" class="announcement-wrapper">
              <div class="announcement-marquee">
                <div class="marquee-icon">
                  <el-icon><Bell /></el-icon>
                </div>
                <div class="marquee-content">
                  <div class="marquee-text">
                    <span>{{ currentAnnouncement }}</span>
                  </div>
                </div>
                <div class="marquee-close" @click="closeAnnouncement">
                  <el-icon><Close /></el-icon>
                </div>
              </div>
            </div>
            
        <!-- 聊天记录 -->
        <div class="chat-messages" ref="messageContainer">
              <div v-for="message in messages" :key="message.id" class="message-item" :class="{ 'own-message': isOwnMessage(message), 'system-message': message.type === 'system' }">
                <!-- 系统消息 -->
                <div v-if="message.type === 'system'" class="system-message-content">
                  <span class="system-text">{{ message.content }}</span>
                </div>
                
                <!-- 普通消息 -->
                <template v-else>
                  <template v-if="isOwnMessage(message)">
                    <!-- 自己的消息：内容在左，头像在右 -->
                    <div class="message-content">
                      <div class="message-header">
                        <span v-if="!message.isAnonymous" class="message-name">
                          {{ message.senderName }}
                        </span>
                        <span class="message-time">{{ formatMessageTime(message.timestamp) }}</span>
                      </div>
                      
                      <!-- 文本消息 -->
                      <div 
                        v-if="message.type === 'text'"
                        class="message-bubble own-bubble"
                        :class="{ 'recalled-message': message.isRecalled }"
                        @contextmenu.prevent="showContextMenu($event, message)"
                      >
                        <!-- 撤回消息显示 -->
                        <div v-if="message.isRecalled" class="recalled-content">
                          <el-icon class="recalled-icon"><RefreshLeft /></el-icon>
                          <span class="recalled-text">消息已撤回</span>
                        </div>
                        
                        <!-- 正常消息显示 -->
                        <span v-else class="message-text">{{ message.content }}</span>
                      </div>
                      
                      <!-- 引用消息 - 移到气泡下方 -->
                      <div v-if="message.type === 'text' && !message.isRecalled && message.quotedMessage" class="quoted-message">
                        <div class="quoted-text">
                          <span class="quoted-sender">{{ message.quotedMessage.isAnonymous ? '匿名用户' : message.quotedMessage.senderName }}：</span>
                          <span class="quoted-content" :class="{ 'quoted-recalled': message.quotedMessage.isRecalled }">
                            {{ message.quotedMessage.isRecalled ? '原消息已撤回' : message.quotedMessage.content }}
                          </span>
                        </div>
                      </div>
                      
                      <!-- 图片消息 -->
                      <div 
                        v-else-if="message.type === 'image'" 
                        class="message-bubble image-bubble own-bubble"
                        @contextmenu.prevent="showContextMenu($event, message)"
                      >
                        <img
                          :src="message.url"
                          :alt="message.fileName"
                          class="chat-image"
                          @click="previewImage(message.url, message.fileName)"
                          @error="handleImageError"
                        />
                      </div>
                      
                      <!-- 文件消息 -->
                      <div 
                        v-else-if="message.type === 'file'" 
                        class="message-bubble file-bubble own-bubble"
                        @contextmenu.prevent="showContextMenu($event, message)"
                      >
                        <div class="file-info">
                          <el-icon class="file-icon"><Document /></el-icon>
                          <div class="file-details">
                            <div class="file-name">{{ message.fileName }}</div>
                            <div class="file-size">{{ formatFileSize(message.fileSize) }}</div>
                          </div>
                          <el-button type="text" size="small" @click="downloadFile(message)">
                            <el-icon><Download /></el-icon>
                          </el-button>
                        </div>
                      </div>
                    </div>
                    <!-- 自己的头像：匿名时显示默认头像 -->
                    <div class="message-avatar" :class="{ 'anonymous-avatar': message.isAnonymous }">
                      <el-avatar v-if="!message.isAnonymous" :size="40" :src="currentUserAvatar">
                        <el-icon><User /></el-icon>
                      </el-avatar>
                      <el-avatar v-else :size="40">
                        <el-icon><User /></el-icon>
                      </el-avatar>
                    </div>
                  </template>
                  
                  <template v-else>
                    <!-- 别人的消息：头像在左，内容在右 -->
                    <!-- 匿名消息不显示头像，不可点击查看资料 -->
                    <div v-if="!message.isAnonymous" class="message-avatar" @click="showUserProfile(message.senderId)">
                      <el-avatar :size="40" :src="message.senderAvatar">
                        <el-icon><User /></el-icon>
                      </el-avatar>
                    </div>
                    <div v-else class="message-avatar anonymous-avatar">
                      <el-avatar :size="40">
                        <el-icon><User /></el-icon>
                      </el-avatar>
                    </div>
            <div class="message-content">
              <div class="message-header">
                        <span v-if="!message.isAnonymous" class="message-name">
                          {{ message.senderName }}
                        </span>
                        <span class="message-time">{{ formatMessageTime(message.timestamp) }}</span>
                      </div>
                      
                      <!-- 文本消息 -->
                      <div 
                        v-if="message.type === 'text'"
                        class="message-bubble"
                        :class="{ 'recalled-message': message.isRecalled }"
                        @contextmenu.prevent="showContextMenu($event, message)"
                      >
                        <!-- 撤回消息显示 -->
                        <div v-if="message.isRecalled" class="recalled-content">
                          <el-icon class="recalled-icon"><RefreshLeft /></el-icon>
                          <span class="recalled-text">消息已撤回</span>
                        </div>
                        
                        <!-- 正常消息显示 -->
                        <span v-else class="message-text">{{ message.content }}</span>
                      </div>
                      
                      <!-- 引用消息 - 移到气泡下方 -->
                      <div v-if="message.type === 'text' && !message.isRecalled && message.quotedMessage" class="quoted-message">
                        <div class="quoted-text">
                          <span class="quoted-sender">{{ message.quotedMessage.isAnonymous ? '匿名用户' : message.quotedMessage.senderName }}：</span>
                          <span class="quoted-content" :class="{ 'quoted-recalled': message.quotedMessage.isRecalled }">
                            {{ message.quotedMessage.isRecalled ? '原消息已撤回' : message.quotedMessage.content }}
                          </span>
                        </div>
                      </div>
                      
                      <!-- 图片消息 -->
                      <div 
                        v-else-if="message.type === 'image'" 
                        class="message-bubble image-bubble"
                        @contextmenu.prevent="showContextMenu($event, message)"
                      >
                        <img
                          :src="message.url"
                          :alt="message.fileName"
                          class="chat-image"
                          @click="previewImage(message.url, message.fileName)"
                          @error="handleImageError"
                        />
                      </div>
                      
                      <!-- 文件消息 -->
                      <div 
                        v-else-if="message.type === 'file'" 
                        class="message-bubble file-bubble"
                        @contextmenu.prevent="showContextMenu($event, message)"
                      >
                        <div class="file-info">
                          <el-icon class="file-icon"><Document /></el-icon>
                          <div class="file-details">
                            <div class="file-name">{{ message.fileName }}</div>
                            <div class="file-size">{{ formatFileSize(message.fileSize) }}</div>
                          </div>
                          <el-button type="text" size="small" @click="downloadFile(message)">
                            <el-icon><Download /></el-icon>
                          </el-button>
                        </div>
              </div>
            </div>
                  </template>
                </template>
          </div>
        </div>
        
        <!-- 输入框 -->
        <div class="chat-input">
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
                  accept=".doc,.docx,.pdf,.txt,.zip,.rar,.xlsx,.xls,.ppt,.pptx,image/*"
                  :on-change="handleFileSelect"
                  :limit="1"
                >
                  <el-button type="text" size="small" title="发送文件">
                    <el-icon><Paperclip /></el-icon>
                  </el-button>
                </el-upload>
                <el-button type="text" size="small" title="发送图片" @click="selectImage">
                  <el-icon><Picture /></el-icon>
                </el-button>
              </div>
              
              <div class="input-area">
            <el-input
              ref="messageInput"
              v-model="newMessage"
              type="textarea"
              :rows="1"
              placeholder="输入消息..."
              @keydown.enter.prevent="sendMessage"
              :disabled="isUserMuted"
              :autosize="{ minRows: 1, maxRows: 3 }"
            />
                <el-button 
                  type="primary" 
                  @click="sendMessage" 
                  :disabled="!newMessage.trim() || isUserMuted"
                  class="send-btn"
                >
              发送
            </el-button>
              </div>
              
              <div v-if="isUserMuted" class="mute-notice">
                <el-alert title="您已被禁言，无法发送消息" type="warning" :closable="false" />
              </div>
              
              <div class="input-tips">
            <span class="tip">Enter 发送</span>
          </div>
        </div>
      </div>
    </el-card>
      </div>
      
      <!-- 右侧成员列表（可收缩） -->
      <div v-if="showMembers" class="member-panel">
        <el-card class="member-card">
          <template #header>
            <div class="member-header">
              <span class="member-title">群成员 ({{ members.length }})</span>
              <el-button type="text" @click="showMembers = false">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </template>
          
          <div class="member-list">
            <div v-for="member in members" :key="member.id" class="member-item">
              <div class="member-avatar" @click="showUserProfile(member.id)">
                <el-badge :is-dot="member.isOnline" :type="member.isOnline ? 'success' : 'info'">
                  <el-avatar :size="36" :src="member.avatar">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </el-badge>
              </div>
              
              <div class="member-info" @click="showUserProfile(member.id)">
                <div class="member-name">
                  {{ member.name }}
                  <el-tag v-if="member.role === 'admin'" size="small" type="warning">管理员</el-tag>
                  <el-tag v-if="member.isMuted" size="small" type="danger">禁言</el-tag>
                </div>
                <div class="member-details">
                  <span class="member-major">{{ member.major }}</span>
                  <span class="member-grade">{{ member.grade }}</span>
                </div>
              </div>
              
              <div class="member-actions">
                <el-dropdown @command="(command) => handleMemberAction(command, member)">
                  <el-button type="text" size="small">
                    <el-icon><More /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="profile">查看资料</el-dropdown-item>
                      <el-dropdown-item divided v-if="canManageMembers && member.role !== 'admin'" command="promote">设为管理员</el-dropdown-item>
                      <el-dropdown-item v-if="canManageMembers && member.role === 'admin'" command="demote">取消管理员</el-dropdown-item>
                      <el-dropdown-item v-if="canManageMembers" :command="member.isMuted ? 'unmute' : 'mute'">
                        {{ member.isMuted ? '取消禁言' : '禁言' }}
                      </el-dropdown-item>
                      <el-dropdown-item v-if="canManageMembers" command="remove" divided>移除群聊</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
    
    <!-- 发布公告对话框 -->
    <el-dialog
      v-model="settingsDialogVisible"
      title="发布公告"
      width="500px"
      :before-close="closeSettingsDialog"
    >
      <div class="settings-content">
        <el-form :model="settingsForm" label-width="80px">
          <el-form-item label="群公告">
            <el-input
              v-model="settingsForm.announcement"
              type="textarea"
              :rows="5"
              placeholder="请输入群公告内容，所有群成员都能看到"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeSettingsDialog">取消</el-button>
          <el-button type="primary" @click="publishAnnouncement">发布公告</el-button>
        </div>
      </template>
    </el-dialog>
    
    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="imagePreviewVisible"
      :title="previewImageName"
      width="80%"
      top="5vh"
      class="image-preview-dialog"
    >
      <div class="preview-container">
        <img :src="previewImageUrl" :alt="previewImageName" class="preview-image" />
      </div>
      <template #footer>
        <el-button @click="downloadImage" type="primary">下载图片</el-button>
        <el-button @click="imagePreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 消息右键菜单 -->
    <div 
      v-if="contextMenuVisible" 
      class="context-menu"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="copyMessage">
        <el-icon><CopyDocument /></el-icon>
        <span>复制</span>
      </div>
      <div v-if="canRecallMessage" class="menu-item" @click="recallMessage">
        <el-icon><RefreshLeft /></el-icon>
        <span>撤回</span>
      </div>
      <div class="menu-item" @click="quoteMessage">
        <el-icon><ChatLineSquare /></el-icon>
        <span>引用</span>
      </div>
    </div>

    <!-- 用户详情对话框 - 一比一复刻个人中心样式（只读） -->
    <el-dialog 
      v-model="userDetailVisible" 
      title="用户详情" 
      width="900px"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
      <div v-if="selectedUser">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="profile-info">
              <div class="avatar-section">
                <el-avatar :size="120" :src="selectedUser.avatar" class="profile-avatar" />
              </div>
              <div class="user-name-section">
                <h2 class="user-name">{{ selectedUser.name }}</h2>
              </div>
              <div class="basic-info">
                <div class="info-row">
                  <el-icon class="info-icon"><Document /></el-icon>
                  <span class="info-text">{{ selectedUser.studentId || '未设置' }}</span>
                </div>
                <div class="info-row">
                  <el-icon class="info-icon"><Message /></el-icon>
                  <span class="info-text">{{ selectedUser.email || '未绑定邮箱' }}</span>
                </div>
                <div class="info-row">
                  <el-icon class="info-icon"><House /></el-icon>
                  <span class="info-text">{{ selectedUser.dormInfo || '暂未分配宿舍' }}</span>
                </div>
              </div>
            </div>
          </el-col>
          
          <el-col :span="16">
            <div class="profile-tags">
              <h4><el-icon><CollectionTag /></el-icon> 个人标签</h4>
              <div class="tags-container">
                <el-tag 
                  v-for="tag in selectedUser.tags" 
                  :key="tag" 
                  style="margin: 5px;"
                >
                  {{ tag }}
                </el-tag>
                <span v-if="!selectedUser.tags || selectedUser.tags.length === 0" class="no-tags-text">
                  该用户暂未设置个人标签
                </span>
              </div>
            </div>
            
            <div class="profile-bio">
              <h4><el-icon><Document /></el-icon> 个人简介</h4>
              <div class="bio-content">
                <p>{{ selectedUser.bio || '这个人很懒什么都没写' }}</p>
              </div>
            </div>
            
            <div class="schedule-settings">
              <h4><el-icon><Setting /></el-icon> 作息时间</h4>
              <el-form label-width="80px">
                <el-form-item label="就寝时间">
                  <span>{{ selectedUser.bedTime || '未设置' }}</span>
                </el-form-item>
                <el-form-item label="起床时间">
                  <span>{{ selectedUser.wakeTime || '未设置' }}</span>
                </el-form-item>
              </el-form>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <template #footer>
        <el-button type="primary" @click="userDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { House, User, Setting, Close, More, Paperclip, Picture, Document, Download, ChatLineSquare, CopyDocument, RefreshLeft, CollectionTag, Message, Bell } from '@element-plus/icons-vue'
import { getUserProfile } from '@/api/chat.js'

export default {
  name: 'DormChat',
  components: {
    House,
    User,
    Setting,
    Close,
    More,
    Paperclip,
    Picture,
    Document,
    Download,
    ChatLineSquare,
    CopyDocument,
    RefreshLeft,
    CollectionTag,
    Message,
    Bell
  },
  data() {
    return {
      // 基本状态
      anonymousMode: false,
      newMessage: '',
      showMembers: false,
      currentUserAvatar: '', // 当前用户头像（从后端API或成员列表获取）
      
      // 群聊数据
      dormChat: null,
      messages: [],
      members: [],
      
      // 当前用户信息（从登录信息获取）
      currentUser: null,
      
      // 对话框状态
      settingsDialogVisible: false,
      imagePreviewVisible: false,
      previewImageUrl: '',
      previewImageName: '',
      
      // 公告表单
      settingsForm: {
        announcement: ''
      },
      
      // 消息操作相关
      contextMenuVisible: false,
      contextMenuX: 0,
      contextMenuY: 0,
      selectedMessage: null,
      quotedMessage: null,
      
      // 用户详情对话框
      userDetailVisible: false,
      selectedUser: null,
      
      // 消息轮询定时器
      messagePollingTimer: null,
      
      // 公告是否被关闭
      announcementClosed: false
    }
  },
  computed: {
    // 当前显示的公告（从 localStorage 读取）
    currentAnnouncement() {
      if (this.announcementClosed) return null
      return this.dormChat?.announcement || null
    },
    // 检查当前用户是否被禁言
    isUserMuted() {
      if (!this.currentUser || !this.currentUser.id) return false
      const currentMember = this.members.find(m => m.id === this.currentUser.id)
      return currentMember?.isMuted || (this.dormChat?.settings?.muteAll && currentMember?.role !== 'admin')
    },
    
    // 检查当前用户是否可以管理成员
    canManageMembers() {
      if (!this.currentUser || !this.currentUser.id) return false
      const currentMember = this.members.find(m => m.id === this.currentUser.id)
      return currentMember?.role === 'admin'
    },
    
    // 检查是否可以撤回消息
    canRecallMessage() {
      if (!this.selectedMessage || !this.currentUser) return false
      
      // 只能撤回自己的消息
      if (this.selectedMessage.senderId !== this.currentUser.id) return false
      
      // 系统消息不能撤回
      if (this.selectedMessage.type === 'system') return false
      
      // 已撤回的消息不能再撤回
      if (this.selectedMessage.isRecalled) return false
      
      // 检查时间限制（2分钟内）
      const messageTime = new Date(this.selectedMessage.timestamp)
      const currentTime = new Date()
      const timeDiff = (currentTime - messageTime) / 1000 / 60
      
      return timeDiff <= 2
    }
  },
  async mounted() {
    // 初始化当前用户信息（必须先完成）
    await this.initCurrentUser()
    
    // 加载宿舍群聊数据
    await this.loadDormChatData()
    this.scrollToBottom()
    
    // 连接 WebSocket 并订阅群聊消息
    this.connectWebSocket()
    
    // 启动消息轮询作为备用（每10秒刷新一次）
    this.startMessagePolling()
  },
  beforeUnmount() {
    // 清理轮询定时器
    this.stopMessagePolling()
    // 取消 WebSocket 订阅
    this.disconnectWebSocket()
  },
  methods: {
    // 初始化当前用户信息
    async initCurrentUser() {
      try {
        const { getCurrentUser } = await import('@/utils/auth.js')
        const user = getCurrentUser()
        if (user) {
          this.currentUser = {
            id: user.id || user.userId,
            name: user.realName || user.username,
            avatar: user.avatar || ''
          }
          console.log('当前用户信息:', this.currentUser)
          
          // 从后端API获取最新的头像信息
          try {
            const profileRes = await getUserProfile(this.currentUser.id)
            if (profileRes.code === 200 && profileRes.data && profileRes.data.avatar) {
              this.currentUserAvatar = profileRes.data.avatar
              this.currentUser.avatar = profileRes.data.avatar
            }
          } catch (e) {
            console.warn('从后端获取头像失败，将使用成员列表中的头像:', e)
          }
        } else {
          console.warn('未获取到用户信息')
          // 设置默认用户信息，避免null错误
          this.currentUser = {
            id: 0,
            name: '游客',
            avatar: ''
          }
        }
      } catch (error) {
        console.error('获取当前用户信息失败:', error)
        // 设置默认用户信息，避免null错误
        this.currentUser = {
          id: 0,
          name: '游客',
          avatar: ''
        }
      }
    },
    
    // 加载宿舍群聊数据
    async loadDormChatData() {
      try {
        // 从后端API获取宿舍组信息
        const { getMyDormGroup } = await import('@/api/dormGroup.js')
        const response = await getMyDormGroup()
        
        if (response.code === 200 && response.data) {
          const dormGroup = response.data
          
          // 解析成员列表
          let memberList = []
          if (dormGroup.members) {
            try {
              memberList = JSON.parse(dormGroup.members)
            } catch (e) {
              console.error('解析成员列表失败:', e)
            }
          }
          
          // 构建群聊数据（公告从后端 dormGroup.announcement 获取）
          this.dormChat = {
            id: `dorm_group_${dormGroup.id}`,
            dormGroupId: dormGroup.id,
            name: dormGroup.groupName || '宿舍群聊',
            building: dormGroup.buildingName || '',
            room: dormGroup.roomNumber || '',
            avatar: '',
            description: `${dormGroup.groupName}`,
            announcement: dormGroup.announcement || '',
            createTime: dormGroup.createdAt,
            memberCount: dormGroup.currentMembers || memberList.length,
            isActive: dormGroup.status !== 'disbanded',
            settings: {
              allowInvite: true,
              muteAll: false,
              allowAnonymous: true,
              showMemberList: true
            },
            messages: []
          }
          
          // 重置公告关闭状态
          this.announcementClosed = false
          
          // 设置成员列表（从 JSON 解析，作为初始数据）
          // 注意：这里的 member.id 是学生表ID，member.userId 是用户表ID
          this.members = memberList.map(member => ({
            id: member.userId || member.id, // 优先使用 userId（用户表ID）
            studentTableId: member.id, // 学生表ID（用于查询学生详情）
            name: member.name || member.realName,
            avatar: member.avatar || '',
            role: member.id === dormGroup.leaderId ? 'admin' : 'member',
            joinTime: dormGroup.createdAt,
            lastActiveTime: new Date().toISOString(),
            isOnline: false,
            isMuted: false,
            major: member.major || '',
            grade: member.grade || '',
            studentId: member.studentNumber || ''
          }))
          
          // 初始化公告表单
          this.settingsForm = {
            announcement: this.dormChat.announcement || ''
          }
          
          // 从后端 API 加载消息
          try {
            const { getDormGroupMessages, getDormGroupMembers } = await import('@/api/chat.js')
            
            // 加载消息历史
            const msgResponse = await getDormGroupMessages(dormGroup.id, 1, 100)
            if (msgResponse.code === 200 && msgResponse.data) {
              this.messages = msgResponse.data.map(msg => ({
                id: msg.id,
                senderId: msg.senderId,
                senderName: msg.senderName || '未知用户',
                senderAvatar: msg.senderAvatar || '',
                content: msg.content,
                type: msg.messageType || 'text',
                timestamp: msg.createdAt,
                isAnonymous: msg.isAnonymous || false,
                isRecalled: msg.isRecalled || false,
                quotedMessage: msg.quotedMessage || null,
                fileName: msg.fileName,
                fileSize: msg.fileSize,
                url: msg.fileUrl
              }))
              console.log('从后端加载消息数量:', this.messages.length)
            }
            
            // 加载成员列表（从后端获取更准确的在线状态等）
            const memberResponse = await getDormGroupMembers(dormGroup.id)
            if (memberResponse.code === 200 && memberResponse.data) {
              console.log('后端返回的成员数据:', memberResponse.data)
              this.members = memberResponse.data.map(member => ({
                id: member.userId, // 用户表ID（用于消息发送者匹配）
                studentTableId: member.id, // 学生表ID（用于查询学生详情）
                name: member.userName || member.name || member.realName || '未知用户',
                avatar: member.avatar || '',
                role: member.role === 'admin' ? 'admin' : 'member',
                joinTime: member.joinTime || dormGroup.createdAt,
                lastActiveTime: member.lastActiveTime || new Date().toISOString(),
                isOnline: member.isOnline || false,
                isMuted: member.isMuted || false,
                major: member.major || '',
                grade: member.grade || member.className || '',
                studentId: member.studentId || member.studentNumber || ''
              }))
              
              // 从成员列表中更新当前用户的头像
              if (this.currentUser && this.currentUser.id) {
                const currentMember = memberResponse.data.find(m => m.userId === this.currentUser.id)
                if (currentMember && currentMember.avatar) {
                  this.currentUserAvatar = currentMember.avatar
                }
              }
            }
          } catch (e) {
            console.error('从后端加载消息失败:', e)
            this.messages = []
          }
          
          console.log('宿舍群聊数据加载成功:', this.dormChat)
          console.log('成员列表:', this.members)
        } else {
          this.dormChat = null
          this.members = []
          console.log('未找到宿舍组信息')
        }
      } catch (error) {
        console.error('加载宿舍群聊数据失败:', error)
        this.dormChat = null
        this.members = []
      }
    },
    
    // 连接 WebSocket 并订阅群聊消息
    async connectWebSocket() {
      if (!this.currentUser || !this.currentUser.id || !this.dormChat) return
      
      try {
        const webSocketManager = (await import('@/utils/websocket.js')).default
        
        // 连接 WebSocket
        webSocketManager.connect(
          this.currentUser.id,
          () => {
            console.log('WebSocket 连接成功，订阅宿舍群聊')
            // 订阅宿舍群聊消息
            if (this.dormChat && this.dormChat.dormGroupId) {
              webSocketManager.subscribeToDormGroup(this.dormChat.dormGroupId, (message) => {
                console.log('收到宿舍群聊消息:', message)
                this.handleWebSocketMessage(message)
              })
            }
          },
          (error) => {
            console.error('WebSocket 连接失败:', error)
          }
        )
      } catch (error) {
        console.error('初始化 WebSocket 失败:', error)
      }
    },
    
    // 断开 WebSocket 连接
    async disconnectWebSocket() {
      if (!this.dormChat) return
      
      try {
        const webSocketManager = (await import('@/utils/websocket.js')).default
        if (this.dormChat.dormGroupId) {
          webSocketManager.unsubscribeFromDormGroup(this.dormChat.dormGroupId)
        }
      } catch (error) {
        console.error('断开 WebSocket 失败:', error)
      }
    },
    
    // 处理 WebSocket 收到的消息
    handleWebSocketMessage(message) {
      // 将消息添加到列表
      const newMessage = {
        id: message.id,
        senderId: message.senderId,
        senderName: message.senderName || '未知用户',
        senderAvatar: message.senderAvatar || '',
        content: message.content,
        type: message.messageType || 'text',
        timestamp: message.createdAt,
        isAnonymous: message.isAnonymous || false,
        isRecalled: message.isRecalled || false,
        quotedMessage: message.quotedMessage || null,
        fileName: message.fileName,
        fileSize: message.fileSize,
        url: message.fileUrl
      }
      
      // 检查消息是否已存在（避免重复）
      const exists = this.messages.some(m => m.id === newMessage.id)
      if (!exists) {
        this.messages.push(newMessage)
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      }
    },
    
    // 启动消息轮询
    startMessagePolling() {
      // 每10秒刷新一次消息（作为 WebSocket 的备用）
      this.messagePollingTimer = setInterval(async () => {
        if (this.dormChat && this.dormChat.dormGroupId) {
          await this.refreshMessages()
        }
      }, 10000)
    },
    
    // 停止消息轮询
    stopMessagePolling() {
      if (this.messagePollingTimer) {
        clearInterval(this.messagePollingTimer)
        this.messagePollingTimer = null
      }
    },
    
    // 刷新消息（不刷新整个页面数据）
    async refreshMessages() {
      if (!this.dormChat || !this.dormChat.dormGroupId) return
      
      try {
        const { getDormGroupMessages } = await import('@/api/chat.js')
        const msgResponse = await getDormGroupMessages(this.dormChat.dormGroupId, 1, 100)
        
        if (msgResponse.code === 200 && msgResponse.data) {
          const newMessages = msgResponse.data.map(msg => ({
            id: msg.id,
            senderId: msg.senderId,
            senderName: msg.senderName || '未知用户',
            senderAvatar: msg.senderAvatar || '',
            content: msg.content,
            type: msg.messageType || 'text',
            timestamp: msg.createdAt,
            isAnonymous: msg.isAnonymous || false,
            isRecalled: msg.isRecalled || false,
            quotedMessage: msg.quotedMessage || null,
            fileName: msg.fileName,
            fileSize: msg.fileSize,
            url: msg.fileUrl
          }))
          
          // 检查是否有新消息
          if (newMessages.length !== this.messages.length) {
            const hadNewMessages = newMessages.length > this.messages.length
            this.messages = newMessages
            
            // 如果有新消息，滚动到底部
            if (hadNewMessages) {
              this.$nextTick(() => {
                this.scrollToBottom()
              })
            }
          }
        }
      } catch (e) {
        console.error('刷新消息失败:', e)
      }
    },
    
    // 判断是否为自己的消息
    isOwnMessage(message) {
      if (!this.currentUser || !this.currentUser.id) return false
      return message.senderId === this.currentUser.id
    },
    
    // 发送消息
    async sendMessage() {
      if (!this.newMessage.trim() || !this.dormChat || this.isUserMuted || !this.currentUser) return
      
      try {
        const { sendDormGroupMessage } = await import('@/api/chat.js')
        
        const messageData = {
          content: this.newMessage.trim(),
          messageType: 'text',
          isAnonymous: this.anonymousMode
        }
        
        // 如果有引用消息，添加引用信息
        if (this.quotedMessage) {
          messageData.quotedMessageId = this.quotedMessage.id
          this.quotedMessage = null // 清除引用
        }
        
        console.log('发送消息数据:', messageData)
        
        const response = await sendDormGroupMessage(this.dormChat.dormGroupId, messageData)
        
        if (response.code === 200) {
          this.newMessage = ''
          // 重新加载消息
          await this.loadDormChatData()
          this.scrollToBottom()
        } else {
          this.$message.error(response.message || '发送消息失败')
        }
        
      } catch (error) {
        console.error('发送消息失败:', error)
        this.$message.error('发送消息失败')
      }
    },
    
    // 处理文件选择
    handleFileSelect(uploadFile) {
      const file = uploadFile.raw
      if (!file) return
      
      // 验证文件大小（10MB）
      if (file.size > 10 * 1024 * 1024) {
        this.$message.error('文件大小不能超过10MB')
        return
      }
      
      // 判断是图片还是文件
      if (file.type.startsWith('image/')) {
        this.handleImageUpload(file)
      } else {
        this.handleFileUpload(file)
      }
      
      // 清空上传组件
      this.$refs.fileUpload.clearFiles()
    },
    
    // 处理图片上传
    async handleImageUpload(file) {
      const reader = new FileReader()
      reader.onload = async (e) => {
        try {
          const { sendMessageToDormChat } = await import('@/utils/dormChatData.js')
          
          const messageData = {
            senderId: this.currentUser.id,
            senderName: this.currentUser.name,
            senderAvatar: this.currentUser.avatar,
            content: '[图片]',
            type: 'image',
            fileName: file.name,
            fileSize: file.size,
            url: e.target.result,
            isAnonymous: this.anonymousMode
          }
          
          await sendMessageToDormChat(this.dormChat.id, messageData)
          await this.loadDormChatData()
          this.scrollToBottom()
          this.$message.success('图片发送成功')
          
        } catch (error) {
          console.error('发送图片失败:', error)
          this.$message.error('发送图片失败')
        }
      }
      reader.readAsDataURL(file)
    },
    
    // 处理文件上传
    async handleFileUpload(file) {
      try {
        const { sendMessageToDormChat } = await import('@/utils/dormChatData.js')
        
        const fileUrl = URL.createObjectURL(file)
        const messageData = {
          senderId: this.currentUser.id,
          senderName: this.currentUser.name,
          senderAvatar: this.currentUser.avatar,
          content: `[文件] ${file.name}`,
          type: 'file',
          fileName: file.name,
          fileSize: file.size,
          url: fileUrl,
          isAnonymous: this.anonymousMode
        }
        
        await sendMessageToDormChat(this.dormChat.id, messageData)
        await this.loadDormChatData()
        this.scrollToBottom()
        this.$message.success('文件发送成功')
        
      } catch (error) {
        console.error('发送文件失败:', error)
        this.$message.error('发送文件失败')
      }
    },
    
    // 选择图片
    selectImage() {
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = 'image/*'
      input.onchange = (e) => {
        const file = e.target.files[0]
        if (file) {
          this.handleImageUpload(file)
        }
      }
      input.click()
    },
    
    // 预览图片
    previewImage(url, name) {
      this.previewImageUrl = url
      this.previewImageName = name
      this.imagePreviewVisible = true
    },
    
    // 下载图片
    downloadImage() {
      const link = document.createElement('a')
      link.href = this.previewImageUrl
      link.download = this.previewImageName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      this.$message.success('图片下载开始')
    },
    
    // 下载文件
    downloadFile(message) {
      const link = document.createElement('a')
      link.href = message.url
      link.download = message.fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      this.$message.success('文件下载开始')
    },
    
    // 处理图片加载错误
    handleImageError(event) {
      event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWbvueJh+WKoOi9veWksei0pTwvdGV4dD48L3N2Zz4='
    },
    
    // 格式化文件大小
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    // 格式化消息时间
    formatMessageTime(timestamp) {
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', { 
        hour12: false, 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    },
    
    // 显示成员列表
    showMemberList() {
      this.showMembers = !this.showMembers
    },
    
    // 处理成员操作
    async handleMemberAction(command, member) {
      // 查看资料不需要管理员权限
      if (command === 'profile') {
        await this.showUserProfile(member.id)
        return
      }
      
      // 其他操作需要管理员权限
      if (!this.canManageMembers || member.id === this.currentUser.id) {
        this.$message.warning('没有权限执行此操作')
        return
      }
      
      try {
        const { updateMemberRole, toggleMemberMute, removeDormChatMember } = await import('@/utils/dormChatData.js')
        
        switch (command) {
          case 'promote':
            await updateMemberRole(this.dormChat.id, member.id, 'admin')
            this.$message.success(`${member.name}已设为管理员`)
            break
            
          case 'demote':
            await updateMemberRole(this.dormChat.id, member.id, 'member')
            this.$message.success(`${member.name}已取消管理员`)
            break
            
          case 'mute':
          case 'unmute':
            await toggleMemberMute(this.dormChat.id, member.id)
            const action = command === 'mute' ? '禁言' : '取消禁言'
            this.$message.success(`${member.name}已${action}`)
            break
            
          case 'remove':
            await this.$confirm(`确定要将${member.name}移出群聊吗？`, '确认操作', {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            })
            await removeDormChatMember(this.dormChat.id, member.id)
            this.$message.success(`${member.name}已被移出群聊`)
            break
        }
        
        // 重新加载数据
        await this.loadDormChatData()
        
      } catch (error) {
        if (error === 'cancel') return
        console.error('成员操作失败:', error)
        this.$message.error('操作失败')
      }
    },
    
    // 显示公告对话框
    showSettings() {
      // 加载当前公告
      this.settingsForm.announcement = this.dormChat?.announcement || ''
      this.settingsDialogVisible = true
    },
    
    // 关闭设置对话框
    closeSettingsDialog() {
      this.settingsDialogVisible = false
    },
    
    // 发布公告
    async publishAnnouncement() {
      if (!this.settingsForm.announcement.trim()) {
        this.$message.warning('请输入公告内容')
        return
      }
      
      try {
        const announcement = this.settingsForm.announcement.trim()
        
        // 调用后端 API 保存公告
        const { updateDormGroupAnnouncement } = await import('@/api/dormGroup.js')
        const response = await updateDormGroupAnnouncement(this.dormChat.dormGroupId, announcement)
        
        if (response.code === 200) {
          // 更新本地群聊公告
          if (this.dormChat) {
            this.dormChat.announcement = announcement
          }
          
          // 重置关闭状态，显示新公告
          this.announcementClosed = false
          
          this.$message.success('公告发布成功')
          this.settingsDialogVisible = false
        } else {
          this.$message.error(response.message || '发布公告失败')
        }
        
      } catch (error) {
        console.error('发布公告失败:', error)
        this.$message.error('发布公告失败')
      }
    },
    
    // 关闭公告
    closeAnnouncement() {
      this.announcementClosed = true
    },
      
      // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messageContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    // ========== 消息右键菜单相关方法 ==========
    
    // 显示右键菜单
    showContextMenu(event, message) {
      if (message.type === 'system') return // 系统消息不显示菜单
      
      this.selectedMessage = message
      this.contextMenuX = event.clientX
      this.contextMenuY = event.clientY
      this.contextMenuVisible = true
      
      // 点击其他地方关闭菜单
      document.addEventListener('click', this.hideContextMenu, { once: true })
    },
    
    // 隐藏右键菜单
    hideContextMenu() {
      this.contextMenuVisible = false
      this.selectedMessage = null
    },
    
    // 复制消息
    async copyMessage() {
      if (!this.selectedMessage) return
      
      try {
        const { copyMessageContent } = await import('@/utils/dormChatData.js')
        await copyMessageContent(this.selectedMessage)
        this.$message.success('消息已复制到剪贴板')
      } catch (error) {
        console.error('复制消息失败:', error)
        this.$message.error('复制失败')
      }
      
      this.hideContextMenu()
    },
    
    // 撤回消息
    async recallMessage() {
      if (!this.selectedMessage) return
      
      try {
        const { recallMessage } = await import('@/utils/dormChatData.js')
        const success = recallMessage(this.dormChat.id, this.selectedMessage.id)
        
        if (success) {
          this.$message.success('消息已撤回')
          await this.loadDormChatData()
        } else {
          this.$message.error('撤回失败，可能超过时间限制')
        }
      } catch (error) {
        console.error('撤回消息失败:', error)
        this.$message.error('撤回失败')
      }
      
      this.hideContextMenu()
    },
    
    // 引用消息
    quoteMessage() {
      if (!this.selectedMessage) return
      
      // 根据是否匿名显示正确的发送人名称
      const displayName = this.selectedMessage.isAnonymous ? '匿名用户' : this.selectedMessage.senderName
      
      this.quotedMessage = {
        id: this.selectedMessage.id,
        senderName: displayName,
        content: this.selectedMessage.content,
        type: this.selectedMessage.type,
        isAnonymous: this.selectedMessage.isAnonymous
      }
      
      this.hideContextMenu()
      
      // 聚焦到输入框
      this.$nextTick(() => {
        const input = this.$refs.messageInput?.$refs?.textarea
        if (input) {
          input.focus()
        }
      })
    },
    
    // 清除引用
    clearQuote() {
      this.quotedMessage = null
    },
    
    // ========== 用户资料相关方法 ==========
    
    // 显示用户详情
    async showUserProfile(userId) {
      if (userId === this.currentUser.id) {
        this.$message.info('这是您自己')
        return
      }
      
      try {
        // 从成员列表中查找基本信息
        const member = this.members.find(m => m.id === userId)
        
        if (!member) {
          this.$message.error('用户不存在')
          return
        }
        
        // 调用后端API获取完整的用户信息
        const { getStudentById, getStudentByUserId } = await import('@/api/student.js')
        
        let response
        // 优先使用 studentTableId（学生表ID）查询
        // 如果没有 studentTableId，尝试用 id 作为学生表ID查询（从 JSON 解析的数据中 id 就是学生表ID）
        // 最后尝试用 userId 查询
        const studentTableId = member.studentTableId || member.id
        console.log('查询学生详情，studentTableId:', studentTableId, 'userId:', userId)
        
        try {
          // 首先尝试用学生表ID查询
          response = await getStudentById(studentTableId)
        } catch (e) {
          console.log('用学生表ID查询失败，尝试用用户ID查询')
          // 如果失败，尝试用用户ID查询
          response = await getStudentByUserId(userId)
        }
        
        if (response.code === 200 && response.data) {
          const studentData = response.data
          
          // 解析标签
          let tags = []
          if (studentData.tags) {
            try {
              tags = JSON.parse(studentData.tags)
            } catch (e) {
              console.error('解析标签失败:', e)
              tags = []
            }
          }
          
          // 构建完整的用户详情数据
          this.selectedUser = {
            id: member.id,
            name: member.name,
            avatar: member.avatar,
            studentId: studentData.studentId || member.studentId || '未设置',
            email: studentData.email || '未绑定邮箱',
            dormInfo: studentData.dormLocation || member.dormInfo || '暂未分配宿舍',
            tags: tags,
            bio: studentData.bio || '这个人很懒什么都没写',
            bedTime: studentData.bedTime || '未设置',
            wakeTime: studentData.wakeTime || '未设置',
            major: member.major,
            grade: member.grade
          }
          
          this.userDetailVisible = true
          console.log('用户详情加载成功:', this.selectedUser)
        } else {
          this.$message.error(response.message || '获取用户详情失败')
        }
      } catch (error) {
        console.error('获取用户详情失败:', error)
        this.$message.error('获取用户详情失败')
      }
    }
  }
}
</script>

<style scoped>
.dorm-chat-page {
  padding: 20px 20px 8px 20px;
  height: calc(100vh - 68px);
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.chat-layout {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}

.chat-main {
  flex: 1;
  min-width: 0;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  min-height: 0;
  overflow: hidden;
}

/* 群聊头部 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  flex-shrink: 0;
}

.chat-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-details {
  display: flex;
  flex-direction: column;
}

.chat-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.member-count {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.chat-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 群公告 */
.announcement-bar {
  padding: 0 20px 8px;
  flex-shrink: 0;
}

.announcement-bar :deep(.el-alert) {
  border-radius: 8px;
}

.announcement-bar :deep(.el-alert__content) {
  padding: 6px 0;
}

/* 聊天容器 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 20px;
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}

.message-item.own-message {
  justify-content: flex-end;
}

.message-item.system-message {
  justify-content: center;
}

.system-message-content {
  background: rgba(0, 0, 0, 0.05);
  padding: 8px 16px;
  border-radius: 16px;
  text-align: center;
}

.system-text {
  font-size: 12px;
  color: #909399;
}

.message-avatar {
  flex-shrink: 0;
  margin-top: 20px;
}

.message-content {
  max-width: 60%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.own-message .message-content {
  align-items: flex-end;
}

.message-header {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.own-message .message-header {
  justify-content: flex-end;
}

.message-name {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
}

.message-bubble {
  background: #fff;
  padding: 8px 12px;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  max-width: 100%;
  width: fit-content;
  position: relative;
}

.own-bubble {
  background: #409eff;
  color: #fff;
}

/* 消息气泡尾巴效果 */
.message-bubble::before {
  content: '';
  position: absolute;
  width: 0;
  height: 0;
  border: 6px solid transparent;
}

/* 别人的消息气泡尾巴（左侧） */
.message-item:not(.own-message) .message-bubble::before {
  left: -6px;
  top: 12px;
  border-right-color: #fff;
}

/* 自己的消息气泡尾巴（右侧） */
.own-bubble::before {
  right: -6px;
  top: 12px;
  border-left-color: #409eff;
}

.message-text {
  line-height: 1.4;
  font-size: 14px;
}

.image-bubble {
  padding: 4px;
  overflow: hidden;
}

.chat-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.chat-image:hover {
  transform: scale(1.02);
}

.file-bubble {
  padding: 8px 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  font-size: 24px;
  color: #606266;
}

.own-bubble .file-icon {
  color: rgba(255, 255, 255, 0.8);
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  opacity: 0.7;
}

/* 输入区域 */
.chat-input {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 8px 20px;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area :deep(.el-textarea) {
  flex: 1;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 16px;
  padding: 8px 12px;
  resize: none;
  border: 1px solid #e4e7ed;
  background: #f8f9fa;
  line-height: 1.4;
  min-height: 36px;
}

.send-btn {
  height: 36px;
  border-radius: 18px;
  padding: 0 16px;
  flex-shrink: 0;
  font-size: 14px;
}

.mute-notice {
  margin: 12px 0;
}

.input-tips {
  margin-top: 2px;
  text-align: right;
}

.tip {
  font-size: 11px;
  color: #c0c4cc;
}

/* 成员面板 */
.member-panel {
  width: 280px;
  flex-shrink: 0;
}

.member-card {
  height: 100%;
}

.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.member-title {
  font-weight: 600;
  color: #303133;
}

.member-list {
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f2f5;
  gap: 12px;
}

.member-item:last-child {
  border-bottom: none;
}

.member-avatar {
  position: relative;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.member-details {
  font-size: 12px;
  color: #909399;
  display: flex;
}

.member-actions {
  flex-shrink: 0;
}

/* 设置对话框 */
.settings-content {
  max-height: 500px;
  overflow-y: auto;
}

.setting-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-right {
  display: flex;
  gap: 12px;
}

/* 图片预览 */
.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: #f8f9fa;
  border-radius: 8px;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar,
.member-list::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track,
.member-list::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb,
.member-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.member-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dorm-chat-page {
    padding: 8px;
  }
  
  .chat-layout {
    flex-direction: column;
  }
  
  .member-panel {
    width: 100%;
    height: 200px;
  }
  
  .message-content {
    max-width: 80%;
  }
  
  .input-area {
    flex-direction: column;
    gap: 8px;
  }
  
  .send-btn {
    align-self: flex-end;
  }
}

/* 动画效果 */
.message-item {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-image:hover,
.message-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 在线状态指示 */
.member-avatar :deep(.el-badge__content) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* ========== 引用消息样式 - 独立显示 ========== */
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

.quoted-recalled {
  color: #c0c4cc !important;
  font-style: italic;
}

/* 撤回消息样式 */
.recalled-message {
  background: #f5f7fa !important;
  border: 1px dashed #c0c4cc !important;
  color: #909399 !important;
}

.recalled-content {
  display: flex;
  align-items: center;
  gap: 6px;
  font-style: italic;
}

.recalled-icon {
  font-size: 14px;
  color: #c0c4cc;
}

.recalled-text {
  color: #909399;
  font-size: 13px;
}

/* 引用预览区域 - 参考微信/QQ设计 */
.quote-preview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.05);
  border-left: 3px solid #409eff;
  border-radius: 4px;
  margin-bottom: 8px;
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

.quote-preview .quote-content {
  color: #606266;
  font-weight: 400;
}

.quote-preview .close-btn {
  color: #c0c4cc;
  cursor: pointer;
  font-size: 16px;
  flex-shrink: 0;
}

.quote-preview .close-btn:hover {
  color: #409eff;
}

/* ========== 右键菜单样式 ========== */
.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 2000;
  min-width: 120px;
  padding: 4px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.menu-item .el-icon {
  font-size: 16px;
}

/* ========== 用户详情对话框样式（一比一复刻个人中心） ========== */
.profile-info {
  text-align: center;
  padding: 30px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  color: white;
  position: relative;
  overflow: hidden;
}

.profile-info::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255,255,255,0.1), transparent);
  transform: rotate(45deg);
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
  100% { transform: translateX(100%) translateY(100%) rotate(45deg); }
}

.avatar-section {
  margin-bottom: 20px;
  position: relative;
}

.profile-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.user-name-section {
  margin-bottom: 25px;
}

.user-name {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  color: white;
}

.user-title {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
  font-weight: 300;
  color: white;
}

.basic-info {
  text-align: left;
  margin: 0;
  padding: 0;
  background: transparent;
}

.info-row {
  display: flex;
  align-items: center;
  margin: 12px 0;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.info-row:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateX(5px);
}

.info-icon {
  margin-right: 12px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  width: 20px;
  display: flex;
  justify-content: center;
}

.info-text {
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  font-weight: 400;
  flex: 1;
  word-break: break-all;
  overflow-wrap: break-word;
  line-height: 1.5;
}

.profile-tags, .profile-bio, .schedule-settings {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.profile-tags h4, .profile-bio h4, .schedule-settings h4 {
  margin: 0 0 15px 0;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.no-tags-text {
  color: #c0c4cc;
  font-style: italic;
  font-size: 14px;
}

.bio-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bio-content p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
  min-height: 24px;
}

/* 头像可点击样式 */
/* 匿名头像样式 */
.anonymous-avatar {
  cursor: default !important;
}

.anonymous-avatar .el-avatar {
  background: linear-gradient(135deg, #a0a0a0 0%, #808080 100%);
}
.message-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.message-avatar:hover {
  transform: scale(1.05);
}

/* 无群聊状态 */
.no-chat-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: #f8f9fa;
}

.no-chat-content {
  text-align: center;
  max-width: 400px;
}

.no-chat-icon {
  color: #c0c4cc;
  margin-bottom: 20px;
}

.no-chat-content h3 {
  margin: 0 0 12px 0;
  color: #606266;
  font-size: 18px;
  font-weight: 600;
}

.no-chat-content p {
  margin: 0 0 24px 0;
  color: #909399;
  font-size: 14px;
  line-height: 1.5;
}

.no-chat-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* ========== 滚动公告样式 ========== */
.chat-container {
  position: relative;
}

.announcement-wrapper {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  display: flex;
  justify-content: center;
}

.announcement-marquee {
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, rgba(255, 247, 230, 0.95) 0%, rgba(255, 251, 230, 0.95) 50%, rgba(255, 247, 230, 0.95) 100%);
  border: 1px solid #ffd666;
  border-radius: 20px;
  padding: 6px 16px;
  overflow: hidden;
  width: 50vw;
  max-width: 600px;
  min-width: 300px;
  box-shadow: 0 2px 12px rgba(250, 173, 20, 0.3);
  backdrop-filter: blur(8px);
}

.marquee-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: #faad14;
  border-radius: 50%;
  margin-right: 10px;
  color: white;
  font-size: 12px;
}

.marquee-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.marquee-close {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin-left: 10px;
  color: #d48806;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.marquee-close:hover {
  color: #ad6800;
  transform: scale(1.1);
}

.marquee-text {
  display: inline-block;
  white-space: nowrap;
  animation: marquee 15s linear infinite;
  padding-left: 100%;
}

.marquee-text span {
  color: #d48806;
  font-size: 14px;
  font-weight: 500;
}

@keyframes marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

/* 鼠标悬停时暂停滚动 */
.announcement-marquee:hover .marquee-text {
  animation-play-state: paused;
}
</style>
