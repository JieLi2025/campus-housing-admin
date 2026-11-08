<template>
  <div class="chat-messages-page">
    <div class="chat-container">
      <!-- 左侧聊天列表 -->
      <div class="chat-list-panel">
        <div class="chat-list-header">
          <div class="search-area">
            <div class="search-box">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索联系人（姓名或学号）"
                size="small"
                clearable
                @input="handleSearchInput"
                @keyup.enter="handleSearchEnter"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
          </div>
        </div>
        
        <div class="chat-list">
          <!-- 搜索结果显示（当搜索关键词存在且有搜索结果时） -->
          <div v-if="searchKeyword && searchResultsInList.length > 0" class="search-results-section">
            <div class="search-results-header">搜索结果</div>
            <div 
              v-for="user in searchResultsInList" 
              :key="user.id"
              class="chat-item search-result-item"
            >
              <div class="chat-avatar">
                <el-avatar :size="40" :src="user.avatar">
                  <el-icon v-if="user.type === 'admin'"><UserFilled /></el-icon>
                  <el-icon v-else><User /></el-icon>
                </el-avatar>
              </div>
              <div class="chat-info">
                <div class="chat-header">
                  <span class="chat-name">{{ user.name }}</span>
                  <el-button 
                    v-if="user.isContact" 
                    type="primary" 
                    size="small" 
                    @click.stop="goToChat(user)"
                  >
                    聊天
                  </el-button>
                  <el-button 
                    v-else
                    type="primary" 
                    size="small" 
                    @click.stop="addNewContactFromSearch(user)"
                  >
                    添加
                  </el-button>
                </div>
                <div class="chat-preview">
                  <span class="user-type">{{ user.type === 'admin' || user.type === 'superadmin' ? '管理员' : '学生' }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 联系人列表 -->
          <div 
            v-for="chat in filteredChatList" 
            :key="chat.id"
            class="chat-item"
            :class="{ active: selectedChatId === chat.id }"
            @click="selectChat(chat.id)"
            @contextmenu.prevent="showContactContextMenu($event, chat)"
          >
            <div class="chat-avatar">
              <el-avatar :size="40" :src="chat.avatar">
                <el-icon v-if="chat.type === 'admin'"><UserFilled /></el-icon>
                <el-icon v-else><User /></el-icon>
              </el-avatar>
              <div v-if="chat.unreadCount > 0" class="unread-badge">{{ chat.unreadCount }}</div>
            </div>
            
            <div class="chat-info">
              <div class="chat-header">
                <span class="chat-name">{{ chat.displayName || chat.name }}</span>
                <span class="chat-time">{{ formatTime(chat.lastMessageTime) }}</span>
              </div>
              <div class="chat-preview">
                <span class="last-message">{{ chat.lastMessage }}</span>
              </div>
            </div>
          </div>
          
          <div v-if="filteredChatList.length === 0 && (!searchKeyword || searchResultsInList.length === 0)" class="empty-chat-list">
            <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            <p>未找到联系人</p>
          </div>
        </div>
      </div>
      
      <!-- 右侧聊天内容 -->
      <div class="chat-content-panel">
        <div v-if="selectedChat" class="chat-content">
          <!-- 聊天头部 -->
          <div class="chat-content-header">
            <div class="chat-user-info">
              <!-- 智能返回按钮（仅当从外部跳转时显示） -->
              <el-button 
                v-if="fromRoute" 
                type="text" 
                size="small" 
                @click="goBack"
                class="back-btn"
              >
                <el-icon><ArrowLeft /></el-icon>
                返回
              </el-button>
              
              <el-avatar :size="32" :src="selectedChat.avatar">
                <el-icon v-if="selectedChat.type === 'admin'"><UserFilled /></el-icon>
                <el-icon v-else><User /></el-icon>
              </el-avatar>
              <span class="chat-user-name">{{ selectedChat.displayName || selectedChat.name }}</span>
            </div>
            <div class="chat-actions">
              <el-dropdown 
                @command="(command) => handleContactAction(command, selectedChat)"
                trigger="click"
                placement="bottom-end"
              >
                <el-button type="text" size="large" class="more-actions-btn">
                  <el-icon><More /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <!-- 查看资料选项 -->
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      查看资料
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除联系人
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div class="messages-container" ref="messagesContainer">
            <div class="messages-list">
              <div 
                v-for="message in selectedChat.messages" 
                :key="message.id"
                :data-message-id="message.id"
                class="message-item"
                :class="{ 
                  'own-message': message.isOwn && !message.isRecalled,
                  'recalled': message.isRecalled 
                }"
              >
                <!-- 对方的头像（左侧） -->
                <div class="message-avatar" v-if="!message.isOwn && !message.isRecalled">
                  <el-avatar :size="32" :src="selectedChat.avatar">
                    <el-icon v-if="selectedChat.type === 'admin'"><UserFilled /></el-icon>
                    <el-icon v-else><User /></el-icon>
                  </el-avatar>
                </div>
                
                <!-- 消息内容 -->
                <div class="message-content" :class="{ 'own-content': message.isOwn }">
                  <!-- 普通消息 -->
                  <div v-if="!message.isRecalled" class="message-bubble-wrapper">
                      <div 
                        v-if="message.isOwn"
                        class="message-bubble" 
                        :class="{ 'own-bubble': message.isOwn }"
                        @contextmenu.prevent="openMessageContextMenu($event, message)"
                      >
                        <!-- 引用消息显示 -->
                        <div v-if="message.quotedMessage" class="quoted-message-in-bubble" @click="jumpToQuotedMessage(message.quotedMessage)">
                          <div class="quoted-line"></div>
                          <div class="quoted-content-wrapper">
                            <div class="quoted-sender-name">{{ message.quotedMessage.senderName }}</div>
                            <div class="quoted-message-content">{{ message.quotedMessage.content }}</div>
                          </div>
                        </div>
                        
                        <!-- 文本消息 -->
                        <span v-if="message.type === 'text' || !message.type" class="message-text">{{ message.content }}</span>
                      
                        <!-- 图片消息 -->
                      <div v-else-if="message.type === 'image'" class="message-image">
                        <img
                          :src="getImageUrl(message.url)"
                          :alt="message.fileName"
                          class="chat-image"
                          @click="openImagePreview(getImageUrl(message.url), message.fileName)"
                          @error="handleImageError"
                        />
                      </div>
                      
                                              <!-- 文件消息 -->
                        <div v-else-if="message.type === 'file'" class="message-file">
                          <div class="file-info">
                            <el-icon class="file-icon" :class="getFileIconClass(message.fileName)"><Document /></el-icon>
                            <div class="file-details">
                              <div class="file-name">{{ message.fileName }}</div>
                              <div class="file-size">{{ formatFileSize(message.fileSize) }}</div>
                            </div>
                            <el-button type="text" size="small" @click="downloadFile(message)" class="download-btn">
                              <el-icon><Download /></el-icon>
                            </el-button>
                          </div>
                        </div>
                        
                        <!-- 室友邀请消息 -->
                        <div v-else-if="message.type === 'invite'" class="invite-message">
                          <div class="invite-header">
                            <el-icon class="invite-icon">
                              <House v-if="message.inviteData && message.inviteData.type === 'dormmate'" />
                              <User v-else />
                            </el-icon>
                            <span class="invite-title">
                              {{ message.inviteData && message.inviteData.type === 'dormmate' ? '宿舍队伍邀请' : '室友邀请' }}
                            </span>
                            <el-tag 
                              v-if="message.inviteData" 
                              :type="getInviteStatusType(message.inviteData.status)"
                              size="small"
                            >
                              {{ getInviteStatusText(message.inviteData.status) }}
                            </el-tag>
                          </div>
                          <div class="invite-content">
                            <div v-if="message.inviteData && message.inviteData.dormInfo" class="invite-dorm-info">
                              <strong>宿舍信息：</strong>{{ message.inviteData.dormInfo }}
                            </div>
                            <div v-if="message.content" class="invite-message-text">
                              {{ message.content }}
                            </div>
                          </div>
                          <div v-if="!message.isOwn && message.inviteData && message.inviteData.status === 'pending'" class="invite-actions">
                            <el-button size="small" type="success" @click="acceptInvite(message)">接受</el-button>
                            <el-button size="small" type="danger" @click="rejectInvite(message)">拒绝</el-button>
                          </div>
                          <div v-if="message.isOwn && message.inviteData && message.inviteData.status === 'pending'" class="invite-actions">
                            <el-button size="small" type="warning" @click="recallInvite(message)">撤回邀请</el-button>
                          </div>
                        </div>
                        
                      </div>
                    <div 
                      v-else
                      class="message-bubble" 
                      :class="{ 'own-bubble': message.isOwn }"
                      @contextmenu.prevent="openMessageContextMenu($event, message)"
                    >
                        <!-- 引用消息显示 -->
                        <div v-if="message.quotedMessage" class="quoted-message-in-bubble" @click="jumpToQuotedMessage(message.quotedMessage)">
                          <div class="quoted-line"></div>
                          <div class="quoted-content-wrapper">
                            <div class="quoted-sender-name">{{ message.quotedMessage.senderName }}</div>
                            <div class="quoted-message-content">{{ message.quotedMessage.content }}</div>
                    </div>
                        </div>
                        
                        <!-- 文本消息 -->
                        <span v-if="message.type === 'text' || !message.type" class="message-text">{{ message.content }}</span>
                        
                        <!-- 图片消息 -->
                        <div v-else-if="message.type === 'image'" class="message-image">
                          <img
                            :src="getImageUrl(message.url)"
                            :alt="message.fileName"
                            class="chat-image"
                            @click="openImagePreview(getImageUrl(message.url), message.fileName)"
                            @error="handleImageError"
                          />
                        </div>
                        
                        <!-- 文件消息 -->
                        <div v-else-if="message.type === 'file'" class="message-file">
                          <div class="file-info">
                            <el-icon class="file-icon" :class="getFileIconClass(message.fileName)"><Document /></el-icon>
                            <div class="file-details">
                              <div class="file-name">{{ message.fileName }}</div>
                              <div class="file-size">{{ formatFileSize(message.fileSize) }}</div>
                            </div>
                            <el-button type="text" size="small" @click="downloadFile(message)" class="download-btn">
                              <el-icon><Download /></el-icon>
                            </el-button>
                          </div>
                        </div>
                        
                        <!-- 室友邀请消息 -->
                        <div v-else-if="message.type === 'invite'" class="invite-message">
                          <div class="invite-header">
                            <el-icon class="invite-icon">
                              <House v-if="message.inviteData && message.inviteData.type === 'dormmate'" />
                              <User v-else />
                            </el-icon>
                            <span class="invite-title">
                              {{ message.inviteData && message.inviteData.type === 'dormmate' ? '宿舍队伍邀请' : '室友邀请' }}
                            </span>
                            <el-tag 
                              v-if="message.inviteData" 
                              :type="getInviteStatusType(message.inviteData.status)"
                              size="small"
                            >
                              {{ getInviteStatusText(message.inviteData.status) }}
                            </el-tag>
                          </div>
                          <div class="invite-content">
                            <div v-if="message.inviteData && message.inviteData.dormInfo" class="invite-dorm-info">
                              <strong>宿舍信息：</strong>{{ message.inviteData.dormInfo }}
                            </div>
                            <div v-if="message.content" class="invite-message-text">
                              {{ message.content }}
                            </div>
                          </div>
                          <div v-if="!message.isOwn && message.inviteData && message.inviteData.status === 'pending'" class="invite-actions">
                            <el-button size="small" type="success" @click="acceptInvite(message)">接受</el-button>
                            <el-button size="small" type="danger" @click="rejectInvite(message)">拒绝</el-button>
                          </div>
                          <div v-if="message.isOwn && message.inviteData && message.inviteData.status === 'pending'" class="invite-actions">
                            <el-button size="small" type="warning" @click="recallInvite(message)">撤回邀请</el-button>
                          </div>
                        </div>
                        
                      </div>
                    <div class="message-time" :class="{ 'own-time': message.isOwn }">{{ formatMessageTime(message.time) }}</div>
                  </div>
                  
                  <!-- 撤回消息提示 -->
                  <div v-else class="recalled-message">
                    <el-icon class="recall-icon"><InfoFilled /></el-icon>
                    <span class="recall-text">
                      {{ message.isOwn ? '你撤回了一条消息' : `${selectedChat.displayName || selectedChat.name}撤回了一条消息` }}
                    </span>
                  </div>
                </div>
                
                <!-- 自己的头像（右侧） -->
                <div class="message-avatar" v-if="message.isOwn && !message.isRecalled">
                  <el-avatar :size="32" :src="currentUserAvatar">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 输入框 -->
          <div class="message-input-panel">
            <!-- 引用消息显示 -->
            <div v-if="quotedMessage" class="quoted-message-display">
              <div class="quoted-content">
                <div class="quoted-header">
                  <el-icon><ChatDotSquare /></el-icon>
                  <span class="quoted-sender">引用 {{ quotedMessage.senderName }} 的消息</span>
                  <el-button type="text" size="small" @click="cancelQuote" class="cancel-quote-btn">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <div class="quoted-text">{{ quotedMessage.content }}</div>
              </div>
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
                <el-button type="text" size="small" title="上传文件或图片" class="upload-file-btn">
                  <el-icon class="upload-icon"><Paperclip /></el-icon>
              </el-button>
              </el-upload>
            </div>
            <div class="input-area">
              <el-input
                v-model="messageInput"
                type="textarea"
                :rows="3"
                placeholder="输入消息（Enter发送，Shift+Enter换行）..."
                @keydown.enter.exact.prevent="sendMessage"
                @keydown.ctrl.enter="sendMessage"
              />
              <el-button 
                type="primary" 
                size="small" 
                @click="sendMessage"
                :disabled="!messageInput.trim()"
                class="send-btn"
              >
                发送
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 未选择聊天时的占位图 -->
        <div v-else class="empty-chat-content">
          <el-icon class="empty-icon" :size="64"><ChatDotSquare /></el-icon>
          <h3>选择一个联系人开始聊天</h3>
          <p>点击左侧联系人列表查看消息</p>
        </div>
      </div>
    </div>
    
    <!-- 联系人资料对话框 -->
    <!-- 用户详情对话框 - 支持学生和管理员不同显示 -->
    <el-dialog 
      v-model="profileDialogVisible" 
      :title="selectedUserDetail?.userType === 'student' ? '学生详情' : '管理员详情'" 
      width="900px"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
      <div v-if="selectedUserDetail">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="profile-info">
              <div class="avatar-section">
                <el-avatar :size="120" :src="selectedUserDetail.avatar" class="profile-avatar" />
              </div>
              <div class="user-name-section">
                <h2 class="user-name">{{ selectedUserDetail.name }}</h2>
              </div>
              <div class="basic-info">
                <!-- 学生显示学号，管理员显示手机号 -->
                <div class="info-row">
                  <el-icon class="info-icon"><Document /></el-icon>
                  <span class="info-text" v-if="selectedUserDetail.userType === 'student'">
                    {{ selectedUserDetail.studentId || '未设置学号' }}
                  </span>
                  <span class="info-text" v-else>
                    {{ selectedUserDetail.phone || '未设置手机号' }}
                  </span>
                </div>
                <div class="info-row">
                  <el-icon class="info-icon"><Message /></el-icon>
                  <el-tooltip 
                    :content="selectedUserDetail.email || '未绑定邮箱'" 
                    placement="top"
                    :disabled="!selectedUserDetail.email"
                  >
                    <span class="info-text">{{ selectedUserDetail.email || '未绑定邮箱' }}</span>
                  </el-tooltip>
                </div>
                <!-- 学生显示宿舍信息，管理员显示办公地点 -->
                <div class="info-row">
                  <el-icon class="info-icon"><House /></el-icon>
                  <span class="info-text" v-if="selectedUserDetail.userType === 'student'">
                    {{ selectedUserDetail.dormInfo || '暂未分配宿舍' }}
                  </span>
                  <span class="info-text" v-else>
                    {{ selectedUserDetail.officeLocation || '暂无办公地点' }}
                  </span>
                </div>
              </div>
            </div>
          </el-col>
          
          <el-col :span="16">
            <!-- 学生显示个人标签，管理员显示部门 -->
            <div class="profile-tags">
              <h4>
                <el-icon><CollectionTag /></el-icon> 
                {{ selectedUserDetail.userType === 'student' ? '个人标签' : '部门' }}
              </h4>
              <div class="tags-container" v-if="selectedUserDetail.userType === 'student'">
                <el-tag 
                  v-for="tag in selectedUserDetail.tags" 
                  :key="tag" 
                  style="margin: 5px;"
                >
                  {{ tag }}
                </el-tag>
                <span v-if="!selectedUserDetail.tags || selectedUserDetail.tags.length === 0" class="no-tags-text">
                  该用户暂未设置个人标签
                </span>
              </div>
              <div class="tags-container" v-else>
                <el-tag type="success" style="margin: 5px;">
                  {{ selectedUserDetail.department || '宿舍管理部' }}
                </el-tag>
              </div>
            </div>
            
            <div class="profile-bio">
              <h4><el-icon><Document /></el-icon> 个人简介</h4>
              <div class="bio-content">
                <p>{{ selectedUserDetail.bio || '这个人很懒什么都没写' }}</p>
              </div>
            </div>
            
            <!-- 学生显示作息时间，管理员显示工作时间 -->
            <div class="schedule-settings">
              <h4>
                <el-icon><Setting /></el-icon> 
                {{ selectedUserDetail.userType === 'student' ? '作息时间' : '工作时间' }}
              </h4>
              <el-form label-width="80px" v-if="selectedUserDetail.userType === 'student'">
                <el-form-item label="就寝时间">
                  <span>{{ selectedUserDetail.bedTime || '未设置' }}</span>
                </el-form-item>
                <el-form-item label="起床时间">
                  <span>{{ selectedUserDetail.wakeTime || '未设置' }}</span>
                </el-form-item>
              </el-form>
              <el-form label-width="80px" v-else>
                <el-form-item label="上班时间">
                  <span>{{ selectedUserDetail.workStartTime || '未设置' }}</span>
                </el-form-item>
                <el-form-item label="下班时间">
                  <span>{{ selectedUserDetail.workEndTime || '未设置' }}</span>
                </el-form-item>
              </el-form>
            </div>
          </el-col>
        </el-row>
      </div>
      <div v-else class="loading-profile">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <template #footer>
        <el-button type="primary" @click="closeProfileDialog">关闭</el-button>
      </template>
    </el-dialog>
    
    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="imagePreviewVisible"
      :title="previewImageName || '图片预览'"
      width="80%"
      top="5vh"
      :before-close="closeImagePreview"
      class="image-preview-dialog"
    >
      <div class="preview-image-container">
        <img 
          :src="previewImageUrl" 
          :alt="previewImageName"
          class="preview-image"
          @error="handlePreviewImageError"
        />
      </div>
      <template #footer>
        <div class="preview-footer">
          <span class="image-name">{{ previewImageName }}</span>
          <div class="preview-actions">
            <el-button @click="downloadImage" type="primary" size="small">
              <el-icon><Download /></el-icon>
              下载图片
            </el-button>
            <el-button @click="closeImagePreview" size="small">
              关闭
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
    

    
    <!-- 右键菜单 -->
    <div 
      v-if="contextMenuVisible" 
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click.stop
    >
      <div class="context-menu-list">
        <div 
          class="context-menu-item"
          @click="handleContextMenuAction('profile')"
        >
          <el-icon><User /></el-icon>
          <span>查看资料</span>
        </div>
        <div 
          class="context-menu-item context-menu-divider"
          @click="handleContextMenuAction('delete')"
        >
          <el-icon><Delete /></el-icon>
          <span>删除联系人</span>
        </div>
      </div>
    </div>
    
    <!-- 右键菜单背景遮罩 -->
    <div 
      v-if="contextMenuVisible" 
      class="context-menu-overlay"
      @click="hideContextMenu"
    ></div>
  </div>
</template>

<script>
import { Search, ChatDotRound, User, UserFilled, More, Picture, Paperclip, MoreFilled, Delete, RefreshLeft, InfoFilled, DocumentCopy, Document, Download, ChatDotSquare, Close, Warning, ArrowLeft, Message, CollectionTag, Setting, Loading } from '@element-plus/icons-vue'
import * as chatApi from '@/api/chat'
import webSocketManager from '@/utils/websocket'
import { getToken, getCurrentUser } from '@/utils/auth'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'

export default {
  name: 'StudentChatMessages',
  components: {
    Search,
    ChatDotRound,
    User,
    UserFilled,
    More,
    Picture,
    Paperclip,
    MoreFilled,
    Delete,
    RefreshLeft,
    InfoFilled,
    DocumentCopy,
    Document,
    Download,
    ChatDotSquare,
    Close,
    Warning,
    ArrowLeft,
    Message,
    CollectionTag,
    Setting,
    Loading
  },
  data() {
    return {
      currentMessageContextMenu: null, // 当前打开的消息右键菜单
      searchKeyword: '',
      selectedChatId: null,
      messageInput: '',
      profileDialogVisible: false,
      currentContact: null,
      selectedUserDetail: null, // 用户详情数据
      searchResultsInList: [], // 搜索框中的搜索结果
      searchTimer: null, // 搜索防抖定时器
      quotedMessage: null, // 引用的消息
      imagePreviewVisible: false,
      previewImageUrl: '',
      previewImageName: '',
      fromRoute: null, // 来源路由信息，用于返回逻辑
      // 右键菜单相关数据
      contextMenuVisible: false,
      contextMenuPosition: { x: 0, y: 0 },
      contextMenuChat: null,
      chatList: [],
      currentUserAvatar: '', // 当前用户头像（从后端获取）
    }
  },
  computed: {
    filteredChatList() {
      if (!this.searchKeyword) {
        return this.chatList
      }
      // 如果有搜索关键词，先过滤已有联系人
      return this.chatList.filter(chat => 
        chat.name.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        (chat.displayName && chat.displayName.toLowerCase().includes(this.searchKeyword.toLowerCase())) ||
        chat.lastMessage.toLowerCase().includes(this.searchKeyword.toLowerCase())
      )
    },
    selectedChat() {
      return this.chatList.find(chat => chat.id === this.selectedChatId)
    }
  },
  async mounted() {
    // 加载私聊数据
    await this.loadPrivateChats()
    
    // 触发自动添加管理员联系人（调用getContacts接口）
    await this.ensureManagerContact()
    
    // 处理跳转参数（优先从sessionStorage读取，提高安全性）
    let chatId = null
    let fromRoute = null
    
    // 检查是否是通过redirect参数跳转的
    if (this.$route.query.redirect === 'true') {
      const redirectData = this.getRedirectData()
      if (redirectData) {
        chatId = redirectData.chatId
        fromRoute = redirectData.from
        // 使用后立即清除，防止重复使用
        sessionStorage.removeItem('chatRedirect')
      }
    } else {
      // 兼容旧的URL参数方式（向后兼容）
      chatId = this.$route.query.chatId
      fromRoute = this.$route.query.from
    }
    
    if (chatId) {
      // 如果指定了chatId，优先选择该聊天
      await this.selectSpecificChat(chatId)
    } else if (this.chatList.length > 0) {
      // 默认选择第一个聊天
      this.selectedChatId = this.chatList[0].id
      await this.selectChat(this.selectedChatId)
    }
    
    // 存储来源路由信息，用于返回逻辑
    this.fromRoute = fromRoute
    
    // 初始化滚动条智能显示
    this.initScrollbarBehavior()
    
    // 监听私聊数据变化，实时更新未读消息
    this.setupPrivateChatListener()
    
    // 连接WebSocket
    this.connectWebSocket()
  },
  beforeUnmount() {
    // 断开WebSocket连接
    this.disconnectWebSocket()
    
    // 清除搜索定时器
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
    }
  },
  methods: {
    // 获取重定向数据（从sessionStorage）
    getRedirectData() {
      try {
        const data = sessionStorage.getItem('chatRedirect')
        if (!data) return null
        
        const redirectData = JSON.parse(data)
        
        // 检查数据是否过期（5分钟内有效）
        const maxAge = 5 * 60 * 1000 // 5分钟
        if (Date.now() - redirectData.timestamp > maxAge) {
          sessionStorage.removeItem('chatRedirect')
          return null
        }
        
        return redirectData
      } catch (error) {
        console.error('解析重定向数据失败:', error)
        sessionStorage.removeItem('chatRedirect')
        return null
      }
    },
    
    // 加载私聊数据（从API获取）
    async loadPrivateChats() {
      try {
        const res = await chatApi.getConversations()
        if (res.code === 200 && res.data) {
          // 将API返回的会话列表转换为组件需要的格式
          this.chatList = res.data.map(conv => ({
            id: `user_${conv.userId}`,
            conversationId: conv.id,
            name: conv.userName || '未知用户',
            displayName: conv.remark || conv.userName || '未知用户',
            type: conv.userType || 'student',
            avatar: conv.avatar || '',
            lastMessage: conv.lastMessage || '暂无消息',
            lastMessageTime: conv.lastMessageTime || new Date().toISOString(),
            unreadCount: conv.unreadCount || 0,
            messages: [], // 消息列表在selectChat时加载
            userId: conv.userId
          }))
        }
        
        // 获取当前用户头像
        const currentUserId = this.getCurrentUserId()
        if (currentUserId) {
          const profileRes = await chatApi.getUserProfile(currentUserId)
          if (profileRes.code === 200 && profileRes.data && profileRes.data.avatar) {
            this.currentUserAvatar = profileRes.data.avatar
          }
        }
      } catch (error) {
        console.error('加载会话列表失败:', error)
        this.$message.error('加载会话列表失败')
      }
    },
    
    // 确保管理员联系人已添加（触发后端自动添加逻辑）
    async ensureManagerContact() {
      try {
        // 调用getContacts接口，触发后端的autoAddBuildingManagerContact逻辑
        const res = await chatApi.getContacts()
        console.log('已触发自动添加管理员联系人检查')
        
        if (res.code === 200 && res.data) {
          // 将联系人转换为会话格式，并合并到chatList中
          const contactChats = res.data
            .filter(contact => {
              // 过滤掉已经在会话列表中的联系人
              return !this.chatList.some(chat => chat.userId === contact.userId)
            })
            .map(contact => ({
              id: `user_${contact.userId}`,
              conversationId: null, // 联系人可能还没有会话
              name: contact.userName || '未知用户',
              displayName: contact.displayName || contact.userName || '未知用户',
              type: contact.userType || 'student',
              avatar: contact.avatar || defaultAvatar,
              lastMessage: '点击开始聊天',
              lastMessageTime: contact.createdAt || new Date().toISOString(),
              unreadCount: 0,
              messages: [],
              userId: contact.userId
            }))
          
          // 将联系人添加到会话列表顶部
          if (contactChats.length > 0) {
            this.chatList = [...contactChats, ...this.chatList]
            console.log(`添加了 ${contactChats.length} 个联系人到会话列表`)
          }
        }
      } catch (error) {
        console.error('检查管理员联系人失败:', error)
        // 不显示错误消息，避免影响用户体验
      }
    },

    // 监听私聊数据变化（使用WebSocket实时推送）
    async setupPrivateChatListener() {
      // WebSocket连接后会自动接收消息，这里不需要轮询
    },
    
    // 连接WebSocket
    connectWebSocket() {
      // 获取当前用户ID（从token中解析或从store获取）
      // 这里需要根据实际情况获取userId
      const token = getToken()
      if (!token) {
        console.warn('未找到Token，无法连接WebSocket')
        return
      }
      
      // 从token中解析userId（需要根据实际情况调整）
      // 这里假设可以从某个地方获取userId，实际可能需要从API获取
      // 暂时使用一个占位符，实际使用时需要替换
      const userId = this.getCurrentUserId()
      
      if (!userId) {
        console.warn('无法获取用户ID，WebSocket连接失败，但页面可以正常使用')
        return
      }
      
      console.log('学生端开始连接WebSocket，用户ID:', userId)
      
      try {
        webSocketManager.connect(
          userId,
          () => {
            console.log('学生端WebSocket连接成功回调执行')
            // 注册消息处理器
            console.log('开始注册消息处理器')
            this.setupWebSocketHandlers()
            console.log('消息处理器注册完成')
          },
          (error) => {
            console.error('学生端WebSocket连接失败:', error)
            // 不显示错误提示，避免干扰用户，页面可以正常使用（通过API轮询）
          }
        )
      } catch (error) {
        console.error('学生端WebSocket连接异常:', error)
        // 静默处理，不影响页面使用
      }
    },
    
    // 断开WebSocket
    disconnectWebSocket() {
      webSocketManager.disconnect()
    },
    
    // 设置WebSocket消息处理器
    setupWebSocketHandlers() {
      console.log('学生端注册WebSocket消息处理器')
      
      // 先清除旧的处理器（避免重复注册）
      if (this.newMessageHandler) {
        webSocketManager.off('newMessage', this.newMessageHandler)
      }
      if (this.messageRecalledHandler) {
        webSocketManager.off('messageRecalled', this.messageRecalledHandler)
      }
      if (this.conversationUpdatedHandler) {
        webSocketManager.off('conversationUpdated', this.conversationUpdatedHandler)
      }
      
      // 创建处理器函数（保存引用以便后续清除）
      // 使用箭头函数确保this上下文正确
      this.newMessageHandler = (messageData) => {
        console.log('学生端收到newMessage事件，数据:', messageData)
        this.handleWebSocketMessage(messageData)
      }
      
      this.messageRecalledHandler = (data) => {
        this.handleMessageRecalledViaWebSocket(data)
      }
      
      this.conversationUpdatedHandler = (data) => {
        this.handleConversationUpdatedViaWebSocket(data)
      }
      
      // 注册新的处理器
      webSocketManager.on('newMessage', this.newMessageHandler)
      webSocketManager.on('messageRecalled', this.messageRecalledHandler)
      webSocketManager.on('conversationUpdated', this.conversationUpdatedHandler)
      
      console.log('学生端WebSocket消息处理器注册完成')
    },
    
    // 处理WebSocket接收到的消息
    handleWebSocketMessage(messageData) {
      console.log('学生端收到WebSocket消息:', messageData)
            // 转换消息格式
      const message = {
        id: messageData.id,
        type: messageData.messageType || 'text',
        content: messageData.content,
        time: messageData.createdAt,
        isOwn: messageData.isOwn,
        isRecalled: messageData.isRecalled,
        fileName: messageData.fileName,
        fileSize: messageData.fileSize,
        url: messageData.fileUrl,
        quotedMessage: messageData.quotedMessage
      }
      
      // 找到对应的会话
      const conversationId = messageData.conversationId
      const chat = this.chatList.find(c => c.conversationId === conversationId)
      
      if (chat) {
        // 检查消息是否已存在（避免重复添加）
        const existingMessage = chat.messages.find(m => m.id === message.id)
        if (!existingMessage) {
          // 确保messages数组存在
          if (!chat.messages) {
            // Vue 3 中直接赋值即可，响应式系统会自动处理
            chat.messages = []
          }
          
          // 使用Vue的响应式更新
          chat.messages.push(message)
          chat.lastMessage = this.getMessageDisplayText(message)
          chat.lastMessageTime = message.time
          
          // 如果不是自己发送的消息
          if (!message.isOwn) {
            // 如果当前正在查看这个聊天，不增加未读数，并标记为已读
            if (this.selectedChatId === chat.id) {
              // 标记为已读
              if (chat.conversationId) {
                chatApi.markAsRead(chat.conversationId).catch(err => console.error(err))
              }
            } else {
              // 否则增加未读计数
              chat.unreadCount = (chat.unreadCount || 0) + 1
            }
          }
          
          // 如果当前正在查看这个聊天，滚动到底部
          if (this.selectedChatId === chat.id) {
            this.$nextTick(() => {
              this.scrollToBottom()
            })
          }
          
          console.log('消息已添加到聊天列表:', message)
        } else {
          // 如果消息已存在，更新它（可能是状态更新）
          console.log('消息已存在，更新:', message.id)
          Object.assign(existingMessage, message)
        }
      } else {
        console.log('未找到对应的会话，重新加载会话列表')
        // 如果会话不存在，重新加载会话列表
        this.loadPrivateChats()
      }
    },
    
    // 通过WebSocket处理消息撤回
    handleMessageRecalledViaWebSocket(data) {
      const conversationId = data.conversationId
      const messageId = data.messageId
      
      const chat = this.chatList.find(c => c.conversationId === conversationId)
      if (chat) {
        const message = chat.messages.find(m => m.id === messageId)
        if (message) {
          message.isRecalled = true
          message.recallTime = new Date().toISOString()
        }
      }
    },
    
    // 通过WebSocket处理会话更新
    handleConversationUpdatedViaWebSocket(data) {
      console.log('学生端收到会话更新通知:', data)
      // 只更新会话信息，不重新加载整个列表（避免丢失当前消息）
      const conversationId = data.conversationId
      const chat = this.chatList.find(c => c.conversationId === conversationId)
      
      if (chat) {
        // 只更新最后消息时间和未读计数，不重新加载消息列表
        // 这样可以避免丢失当前正在查看的消息
        console.log('更新会话信息，不重新加载消息')
      } else {
        // 如果会话不在列表中，重新加载
        console.log('会话不在列表中，重新加载会话列表')
        this.loadPrivateChats()
      }
    },
    
    // 获取当前用户ID（需要根据实际情况实现）
    getCurrentUserId() {
      const user = getCurrentUser()
      if (user && (user.id || user.userId)) {
        return user.id || user.userId
      }
      return null
    },

    async selectChat(chatId) {
      // 更新当前选中的聊天ID
      this.selectedChatId = chatId
      
      // 查找对应的聊天对象
      const chat = this.chatList.find(c => c.id === chatId)
      if (!chat) return
      
      // 如果是重新选择，且已经加载过消息，则不重复加载
      if (chat.messages && chat.messages.length > 0 && this.selectedChatId === chatId && this.selectedChat && this.selectedChat.id === chatId) {
        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom()
        })
        return
      }
      
      // 如果没有消息或者为空数组，尝试加载
      if (!chat.messages || chat.messages.length === 0) {
        // 如果有conversationId，加载消息历史
        if (chat.conversationId) {
          try {
            const res = await chatApi.getMessages(chat.conversationId, 1, 100)
            if (res.code === 200 && res.data) {
              // 转换消息格式
              chat.messages = res.data.map(msg => ({
                id: msg.id,
                type: msg.messageType || 'text',
                content: msg.content,
                time: msg.createdAt,
                isOwn: msg.isOwn,
                isRecalled: msg.isRecalled,
                fileName: msg.fileName,
                fileSize: msg.fileSize,
                url: msg.fileUrl,
                quotedMessage: msg.quotedMessage
              }))
            }
          } catch (error) {
            console.error('加载消息历史失败:', error)
          }
        }
      }
      
      // 标记为已读
      if (chat.conversationId && chat.unreadCount > 0) {
        try {
          await chatApi.markAsRead(chat.conversationId)
          chat.unreadCount = 0
        } catch (error) {
          console.error('标记聊天为已读失败:', error)
        }
      }
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    
    // 选择特定聊天（支持从外部跳转）
    async selectSpecificChat(chatId) {
      // 提取userId用于匹配（支持 user_123 或 student_123 格式）
      const userId = chatId.replace(/^(user_|student_)/, '')
      
      // 检查聊天是否存在（同时匹配 user_ 和 student_ 前缀）
      const existingChat = this.chatList.find(chat => {
        const chatUserId = chat.id.replace(/^(user_|student_)/, '')
        return chatUserId === userId
      })
      
      if (existingChat) {
        // 如果聊天已存在，直接选择
        await this.selectChat(existingChat.id)
      } else {
        // 如果聊天不存在，尝试创建新的聊天
        await this.createChatFromId(chatId)
      }
    },
    
    // 根据chatId创建新聊天
    async createChatFromId(chatId) {
      try {
        // 从chatId中提取userId（支持格式：user_123 或 student_123）
        const userId = chatId.replace(/^(user_|student_)/, '')
        
        // 先通过精确查找API获取用户信息（避免模糊搜索导致的错误匹配）
        const userRes = await chatApi.getUserById(parseInt(userId))
        if (userRes.code !== 200 || !userRes.data) {
          this.$message.error('无法找到该用户')
          return
        }
        
        const chatUser = userRes.data
        
        // 创建会话
        const convRes = await chatApi.createConversation(parseInt(userId))
        if (convRes.code === 200) {
          const conversationId = convRes.data
          
          const newChat = {
            id: `user_${chatUser.userId}`,
            conversationId: conversationId,
            userId: chatUser.userId,
            name: chatUser.userName,
            type: chatUser.userType || 'student',
            avatar: chatUser.avatar || defaultAvatar,
            lastMessage: '开始聊天',
            lastMessageTime: new Date().toISOString(),
            unreadCount: 0,
            messages: []
          }
          
          this.chatList.unshift(newChat)
          await this.selectChat(newChat.id)
          this.$message.success(`已创建与${chatUser.userName}的聊天`)
        } else {
          this.$message.error(convRes.message || '创建会话失败')
        }
      } catch (error) {
        console.error('创建聊天失败:', error)
        this.$message.error('无法创建聊天，请稍后再试')
      }
    },
    
    // 智能返回逻辑
    goBack() {
      if (this.fromRoute === 'notifications') {
        this.$router.push('/student/notifications')
      } else if (this.fromRoute) {
        // 如果有明确的来源路由，返回到该路由
        this.$router.push(`/student/${this.fromRoute}`)
      } else {
        // 默认返回上一页
        this.$router.go(-1)
      }
    },
    
    async addNewContact(user) {
      try {
        // 添加联系人
        await chatApi.addContact(user.userId)
        
        // 创建会话
        const convRes = await chatApi.createConversation(user.userId)
        if (convRes.code === 200) {
          const conversationId = convRes.data
          
          const newChat = {
            id: `user_${user.userId}`,
            conversationId: conversationId,
            userId: user.userId,
            name: user.name,
            displayName: user.name,
            type: user.type,
            avatar: user.avatar,
            lastMessage: '开始聊天',
            lastMessageTime: new Date().toISOString(),
            unreadCount: 0,
            messages: []
          }
          
          // 添加到联系人列表
          this.chatList.unshift(newChat)
          
          // 清空搜索关键词和搜索结果，确保新添加的联系人可见
          this.searchKeyword = ''
          this.searchResultsInList = []
          
          // 自动选择新添加的联系人
          this.selectedChatId = newChat.id
          await this.selectChat(newChat.id)
          
          this.$message.success(`已添加联系人：${user.name}`)
        }
      } catch (error) {
        console.error('添加联系人失败:', error)
        // request拦截器已经显示了错误提示，这里只记录日志
      }
    },
    
    // 从搜索框添加联系人
    async addNewContactFromSearch(user) {
      // 检查是否已经是联系人
      const existingChat = this.chatList.find(chat => chat.userId === user.userId)
      if (existingChat) {
        // 如果已经是联系人，直接选择
        this.searchKeyword = ''
        this.searchResultsInList = []
        this.selectedChatId = existingChat.id
        await this.selectChat(existingChat.id)
        return
      }
      
      // 调用添加联系人方法
      await this.addNewContact(user)
    },
    
    // 处理搜索框输入（防抖搜索）
    handleSearchInput() {
      // 清除之前的定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      
      // 如果搜索关键词为空，清空搜索结果
      if (!this.searchKeyword || !this.searchKeyword.trim()) {
        this.searchResultsInList = []
        return
      }
      
      // 防抖：500ms后执行搜索
      this.searchTimer = setTimeout(() => {
        this.performSearch()
      }, 500)
    },
    
    // 处理搜索框回车
    handleSearchEnter() {
      // 清除防抖定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      // 立即执行搜索
      this.performSearch()
    },
    
    // 执行搜索
    async performSearch() {
      if (!this.searchKeyword || !this.searchKeyword.trim()) {
        this.searchResultsInList = []
        return
      }
      
      try {
        const res = await chatApi.searchContacts(this.searchKeyword.trim())
        if (res.code === 200 && res.data) {
          // 转换搜索结果格式，标记是否已经是联系人
          this.searchResultsInList = res.data.map(user => {
            const existingChat = this.chatList.find(chat => chat.userId === user.userId)
            return {
              id: `user_${user.userId}`,
              userId: user.userId,
              name: user.userName,
              type: user.userType || 'student',
              avatar: user.avatar || defaultAvatar,
              studentId: user.studentId || '',
              isContact: !!existingChat, // 标记是否已经是联系人
              chatId: existingChat ? existingChat.id : null // 如果是联系人，保存chatId
            }
          })
        } else {
          this.searchResultsInList = []
        }
      } catch (error) {
        console.error('搜索联系人失败:', error)
        this.searchResultsInList = []
        // request拦截器已经显示了错误提示，这里只记录日志
      }
    },
    
    // 跳转到聊天页面（用于已添加的联系人）
    async goToChat(user) {
      if (user.chatId) {
        // 清空搜索关键词
        this.searchKeyword = ''
        this.searchResultsInList = []
        // 选择该联系人
        this.selectedChatId = user.chatId
        await this.selectChat(user.chatId)
      }
    },
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
        // 处理图片
        const reader = new FileReader()
        reader.onload = (e) => {
          const imageUrl = e.target.result
          this.sendImageMessage(file.name, imageUrl, file.size)
        }
        reader.readAsDataURL(file)
      } else {
        // 处理其他文件
        const fileUrl = URL.createObjectURL(file)
        this.sendFileMessage(file.name, fileUrl, file.size)
      }
      
      // 清空上传组件
      this.$refs.fileUpload.clearFiles()
    },
    async sendImageMessage(fileName, imageUrl, fileSize) {
      if (!this.selectedChat) return
      
      try {
        // 如果imageUrl是base64，需要先上传到服务器
        let fileUrl = imageUrl
        if (imageUrl.startsWith('data:')) {
          // 将base64转换为File对象并上传
          const blob = await fetch(imageUrl).then(r => r.blob())
          const file = new File([blob], fileName, { type: blob.type })
          const uploadRes = await chatApi.uploadChatFile(file)
          if (uploadRes.code === 200 && uploadRes.data) {
            fileUrl = uploadRes.data.fileUrl
          }
        }
        
        const messageData = {
          conversationId: this.selectedChat.conversationId,
          receiverId: this.selectedChat.userId,
          content: '',
          messageType: 'image',
          fileUrl: fileUrl,
          fileName: fileName,
          fileSize: fileSize
        }
        
        const res = await chatApi.sendMessage(messageData)
        if (res.code === 200 && res.data) {
          const msg = res.data
          
          // 更新会话ID（如果后端创建了新会话）
          if (!this.selectedChat.conversationId && msg.conversationId) {
            this.selectedChat.conversationId = msg.conversationId
          }
          
          const newMessage = {
            id: msg.id,
            type: 'image',
            fileName: msg.fileName,
            url: msg.fileUrl,
            fileSize: msg.fileSize,
            time: msg.createdAt,
            isOwn: true
          }
          
          this.selectedChat.messages.push(newMessage)
          this.selectedChat.lastMessage = '[图片]'
          this.selectedChat.lastMessageTime = newMessage.time
          
          this.$nextTick(() => {
            this.scrollToBottom()
          })
          
          this.$message.success('图片发送成功')
        } else {
          this.$message.error(res.message || '发送图片失败')
        }
      } catch (error) {
        console.error('发送图片失败:', error)
        this.$message.error('发送图片失败')
      }
    },
    async sendFileMessage(fileName, fileUrl, fileSize) {
      if (!this.selectedChat) return
      
      try {
        // 如果fileUrl是blob URL，需要先上传到服务器
        let finalFileUrl = fileUrl
        if (fileUrl.startsWith('blob:')) {
          const response = await fetch(fileUrl)
          const blob = await response.blob()
          const file = new File([blob], fileName, { type: blob.type })
          const uploadRes = await chatApi.uploadChatFile(file)
          if (uploadRes.code === 200 && uploadRes.data) {
            finalFileUrl = uploadRes.data.fileUrl
            fileSize = uploadRes.data.fileSize
          }
        }
        
        const messageData = {
          conversationId: this.selectedChat.conversationId,
          receiverId: this.selectedChat.userId,
          content: '',
          messageType: 'file',
          fileUrl: finalFileUrl,
          fileName: fileName,
          fileSize: fileSize
        }
        
        const res = await chatApi.sendMessage(messageData)
        if (res.code === 200 && res.data) {
          const msg = res.data
          
          // 更新会话ID（如果后端创建了新会话）
          if (!this.selectedChat.conversationId && msg.conversationId) {
            this.selectedChat.conversationId = msg.conversationId
          }
          
          const newMessage = {
            id: msg.id,
            type: 'file',
            fileName: msg.fileName,
            url: msg.fileUrl,
            fileSize: msg.fileSize,
            time: msg.createdAt,
            isOwn: true
          }
          
          this.selectedChat.messages.push(newMessage)
          this.selectedChat.lastMessage = `[文件] ${fileName}`
          this.selectedChat.lastMessageTime = newMessage.time
          
          this.$nextTick(() => {
            this.scrollToBottom()
          })
          
          this.$message.success('文件发送成功')
        } else {
          this.$message.error(res.message || '发送文件失败')
        }
      } catch (error) {
        console.error('发送文件失败:', error)
        this.$message.error('发送文件失败')
      }
    },
    openImagePreview(imageUrl, imageName) {
      console.log('预览图片URL:', imageUrl)
      
      // 处理相对路径URL（添加baseURL）
      let finalUrl = imageUrl
      if (imageUrl && !imageUrl.startsWith('http') && !imageUrl.startsWith('data:')) {
        finalUrl = `http://localhost:8080${imageUrl}`
      }
      
      // 确保图片URL是有效的
      if (!finalUrl || finalUrl === '#') {
        this.$message.warning('图片链接无效')
        return
      }
      
      this.previewImageUrl = finalUrl
      this.previewImageName = imageName || '图片'
      this.imagePreviewVisible = true
    },
    closeImagePreview() {
      this.imagePreviewVisible = false
      this.previewImageUrl = ''
      this.previewImageName = ''
    },
    downloadImage() {
      if (this.previewImageUrl) {
        const link = document.createElement('a')
        link.href = this.previewImageUrl
        link.download = this.previewImageName || 'image'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        this.$message.success('图片下载开始')
      }
    },
    handleImageError(event) {
      console.warn('图片加载失败:', event.target.src)
      event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWbvueJh+WKoOi9veWksei0pTwvdGV4dD48L3N2Zz4='
    },
    handlePreviewImageError(event) {
      // 静默处理，不显示任何错误信息
      console.warn('预览图片加载失败，已静默处理:', event.target.src)
    },
    downloadFile(message) {
      if (message.url) {
        // 处理相对路径URL
        let fileUrl = message.url
        if (!fileUrl.startsWith('http') && !fileUrl.startsWith('data:')) {
          fileUrl = `http://localhost:8080${fileUrl}`
        }
        
        const link = document.createElement('a')
        link.href = fileUrl
        link.download = message.fileName
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        this.$message.success('文件下载开始')
      }
    },
    getImageUrl(url) {
      if (!url) return ''
      // 如果是相对路径，添加baseURL
      if (!url.startsWith('http') && !url.startsWith('data:')) {
        return `http://localhost:8080${url}`
      }
      return url
    },
    getFileIconClass(fileName) {
      const ext = fileName.split('.').pop()?.toLowerCase()
      switch (ext) {
        case 'pdf':
          return 'file-pdf'
        case 'doc':
        case 'docx':
          return 'file-word'
        case 'xls':
        case 'xlsx':
          return 'file-excel'
        case 'ppt':
        case 'pptx':
          return 'file-powerpoint'
        case 'zip':
        case 'rar':
        case '7z':
          return 'file-archive'
        case 'txt':
          return 'file-text'
        default:
          return 'file-default'
      }
    },
    formatFileSize(bytes) {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    handleContactAction(command, contact) {
      this.currentContact = contact
      switch (command) {
        case 'profile':
          this.showContactProfile()
          break
        case 'delete':
          this.deleteContact(contact)
          break
      }
    },
    async showContactProfile() {
      this.profileDialogVisible = true
      this.selectedUserDetail = null
      
      // 从后端获取用户详细信息
      try {
        const userId = this.currentContact?.userId || this.currentContact?.id?.replace('user_', '')
        if (userId) {
          // 使用新的getUserProfile API获取用户详细资料
          const { getUserProfile } = await import('@/api/chat.js')
          const res = await getUserProfile(userId)
          if (res.code === 200 && res.data) {
            const data = res.data
            
            // 根据用户类型设置不同的字段
            if (data.userType === 'student') {
              // 学生用户
              // 解析标签
              let tags = []
              if (data.tags) {
                try {
                  tags = typeof data.tags === 'string' ? JSON.parse(data.tags) : data.tags
                } catch (e) {
                  console.error('解析标签失败:', e)
                  tags = []
                }
              }
              
              this.selectedUserDetail = {
                userType: 'student',
                name: data.name || this.currentContact?.name,
                avatar: data.avatar || this.currentContact?.avatar,
                studentId: data.studentId || '未设置',
                email: data.email || '未绑定邮箱',
                dormInfo: data.dormInfo || '暂未分配宿舍',
                tags: tags,
                bio: data.bio || '这个人很懒什么都没写',
                bedTime: data.bedTime || '未设置',
                wakeTime: data.wakeTime || '未设置'
              }
            } else {
              // 管理员用户
              this.selectedUserDetail = {
                userType: data.userType || 'admin',
                name: data.name || this.currentContact?.name,
                avatar: data.avatar || this.currentContact?.avatar,
                phone: data.phone || '未设置',
                email: data.email || '未绑定邮箱',
                officeLocation: data.officeLocation || '暂无办公地点',
                department: data.department || '宿舍管理部',
                bio: data.bio || '这个人很懒什么都没写',
                workStartTime: data.workStartTime || '08:30',
                workEndTime: data.workEndTime || '17:30'
              }
            }
          } else {
            // 如果获取失败，使用基本信息（默认为学生）
            this.selectedUserDetail = {
              userType: this.currentContact?.type || 'student',
              name: this.currentContact?.name,
              avatar: this.currentContact?.avatar,
              studentId: '未设置',
              email: '未绑定邮箱',
              dormInfo: '暂未分配宿舍',
              tags: [],
              bio: '这个人很懒什么都没写',
              bedTime: '未设置',
              wakeTime: '未设置'
            }
          }
        }
      } catch (error) {
        console.error('获取用户详情失败:', error)
        // 使用基本信息（根据联系人类型判断）
        const isAdmin = this.currentContact?.type === 'admin' || this.currentContact?.type === 'superadmin'
        if (isAdmin) {
          this.selectedUserDetail = {
            userType: this.currentContact?.type || 'admin',
            name: this.currentContact?.name,
            avatar: this.currentContact?.avatar,
            phone: '未设置',
            email: '未绑定邮箱',
            officeLocation: '暂无办公地点',
            department: '宿舍管理部',
            bio: '这个人很懒什么都没写',
            workStartTime: '08:30',
            workEndTime: '17:30'
          }
        } else {
          this.selectedUserDetail = {
            userType: 'student',
            name: this.currentContact?.name,
            avatar: this.currentContact?.avatar,
            studentId: '未设置',
            email: '未绑定邮箱',
            dormInfo: '暂未分配宿舍',
            tags: [],
            bio: '这个人很懒什么都没写',
            bedTime: '未设置',
            wakeTime: '未设置'
          }
        }
      }
    },
    closeProfileDialog() {
      this.profileDialogVisible = false
      this.currentContact = null
      this.selectedUserDetail = null
    },
    async deleteContact(chat) {
      this.$confirm(`确定要删除联系人 ${chat.displayName || chat.name} 吗？删除后将清空与该用户的聊天记录。`, '删除联系人', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 确保使用正确的userId，避免误删其他联系人
          if (!chat.userId) {
            console.error('联系人缺少userId字段:', chat)
            this.$message.error('联系人数据异常，无法删除')
            return
          }
          
          // 调用删除联系人接口，传递对方的userId
          await chatApi.deleteContact(chat.userId)
          
          // 从本地列表中移除（使用userId精确匹配，避免误删）
          const index = this.chatList.findIndex(c => c.userId === chat.userId)
          if (index !== -1) {
            this.chatList.splice(index, 1)
          }
          
          // 如果当前选中的是该联系人，清空选中状态
          if (this.selectedChatId === chat.id) {
            this.selectedChatId = null
          }
          
          // 重新加载会话和联系人列表，确保状态一致
          await this.loadPrivateChats()
          await this.ensureManagerContact()
          
          this.$message.success('已删除联系人')
        } catch (error) {
          console.error('删除联系人失败:', error)
          this.$message.error('删除联系人失败，请重试')
        }
      }).catch(() => {
        // 用户取消
      })
    },
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
        case 'quote':
          this.quoteMessage(message)
          break
      }
    },
    canRecallMessage(message) {
      // 只有自己的消息且发送时间在2分钟内才能撤回
      if (!message.isOwn) return false
      const now = new Date()
      const messageTime = new Date(message.time)
      const diffMinutes = (now - messageTime) / (1000 * 60)
      return diffMinutes <= 2 
    },
    async recallMessage(message) {
      this.$confirm('确定要撤回这条消息吗？', '撤回消息', {
        confirmButtonText: '撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await chatApi.recallMessage(message.id)
          if (res.code === 200) {
            message.isRecalled = true
            message.recallTime = new Date().toISOString()
            this.$message.success('消息已撤回')
          }
        } catch (error) {
          console.error('撤回消息失败:', error)
          this.$message.error(error.message || '撤回消息失败')
        }
      }).catch(() => {
        // 用户取消撤回
      })
    },
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

    quoteMessage(message) {
      // 设置引用的消息
      this.quotedMessage = {
        id: message.id,
        content: this.getMessageDisplayText(message),
        senderName: message.isOwn ? '我' : (this.selectedChat?.displayName || this.selectedChat?.name || '对方'),
        time: message.time
      }
      
      // 聚焦到输入框
      this.$nextTick(() => {
        const textareaEl = this.$el.querySelector('.message-input-panel textarea')
        if (textareaEl) {
          textareaEl.focus()
        }
      })
      
      this.$message.success('消息已引用')
    },
    getMessageDisplayText(message) {
      // 根据消息类型返回显示文本
      switch (message.type) {
        case 'image':
          return '[图片]'
        case 'file':
          return `[文件] ${message.fileName}`
        case 'invite':
          return message.inviteData && message.inviteData.type === 'dormmate' ? '[宿舍队伍邀请]' : '[室友邀请]'
        case 'dorm_invite':
          return '[宿舍邀请]'
        default:
          return message.content
      }
    },

    // 获取邀请状态类型（用于标签颜色）
    getInviteStatusType(status) {
      switch (status) {
        case 'pending':
          return 'warning'
        case 'accepted':
          return 'success'
        case 'rejected':
          return 'danger'
        case 'recalled':
          return 'info'
        default:
          return 'info'
      }
    },

    // 获取邀请状态文本
    getInviteStatusText(status) {
      switch (status) {
        case 'pending':
          return '等待回复'
        case 'accepted':
          return '已接受'
        case 'rejected':
          return '已拒绝'
        case 'recalled':
          return '已撤回'
        default:
          return '未知状态'
      }
    },

    // 接受邀请
    async acceptInvite(message) {
      try {
        const { getPrivateChats, savePrivateChats } = await import('@/utils/studentMessageData.js')
        
        // 更新邀请状态
        message.inviteData.status = 'accepted'
        
        // 保存更新
        const privateChats = getPrivateChats()
        const contactId = Object.keys(privateChats).find(key => {
          const chat = privateChats[key]
          return chat.messages && chat.messages.some(msg => msg.id === message.id)
        })
        
        if (contactId) {
          savePrivateChats(privateChats)
        }
        
        this.$message.success('已接受邀请')
        
        // 可以在这里添加其他逻辑，比如跳转到宿舍管理页面
        
      } catch (error) {
        console.error('接受邀请失败:', error)
        this.$message.error('接受邀请失败')
      }
    },

    // 拒绝邀请
    async rejectInvite(message) {
      try {
        const { getPrivateChats, savePrivateChats } = await import('@/utils/studentMessageData.js')
        
        // 更新邀请状态
        message.inviteData.status = 'rejected'
        
        // 保存更新
        const privateChats = getPrivateChats()
        const contactId = Object.keys(privateChats).find(key => {
          const chat = privateChats[key]
          return chat.messages && chat.messages.some(msg => msg.id === message.id)
        })
        
        if (contactId) {
          savePrivateChats(privateChats)
        }
        
        this.$message.success('已拒绝邀请')
        
      } catch (error) {
        console.error('拒绝邀请失败:', error)
        this.$message.error('拒绝邀请失败')
      }
    },

    // 撤回邀请
    async recallInvite(message) {
      try {
        const { recallInviteMessage } = await import('@/utils/studentMessageData.js')
        
        // 找到对应的联系人ID
        const privateChats = await import('@/utils/studentMessageData.js').then(module => module.getPrivateChats())
        const contactId = Object.keys(privateChats).find(key => {
          const chat = privateChats[key]
          return chat.messages && chat.messages.some(msg => msg.id === message.id)
        })
        
        if (contactId && recallInviteMessage(contactId, message.id)) {
          this.$message.success('已撤回邀请')
          
          // 更新本地消息状态
          message.inviteData.status = 'recalled'
          message.inviteData.recalled = true
        } else {
          this.$message.error('撤回邀请失败')
        }
        
      } catch (error) {
        console.error('撤回邀请失败:', error)
        this.$message.error('撤回邀请失败')
      }
    },
    cancelQuote() {
      this.quotedMessage = null
    },
    jumpToQuotedMessage(quotedMessage) {
      // 查找被引用的消息
      const targetMessage = this.selectedChat?.messages.find(msg => msg.id === quotedMessage.id)
      
      if (targetMessage) {
        // 如果是图片消息，直接打开预览
        if (targetMessage.type === 'image' && targetMessage.url) {
          this.openImagePreview(targetMessage.url, targetMessage.fileName)
          return
        }
        
        // 如果是文件消息，触发下载
        if (targetMessage.type === 'file' && targetMessage.url) {
          this.downloadFile(targetMessage)
          return
        }
        
        // 对于文本消息，高亮显示（可选功能）
        this.highlightMessage(quotedMessage.id)
      } else {
        this.$message.info('原消息已被删除或撤回')
      }
    },
    highlightMessage(messageId) {
      // 高亮显示目标消息
      this.$nextTick(() => {
        const messageElement = document.querySelector(`[data-message-id="${messageId}"]`)
        if (messageElement) {
          messageElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
          messageElement.classList.add('highlight-message')
          
          // 3秒后移除高亮效果
          setTimeout(() => {
            messageElement.classList.remove('highlight-message')
          }, 3000)
        }
      })
    },
    async sendMessage() {
      if (!this.messageInput.trim() || !this.selectedChat) return
      
      try {
        // 准备消息数据
        const messageData = {
          conversationId: this.selectedChat.conversationId,
          receiverId: this.selectedChat.userId,
          content: this.messageInput.trim(),
          messageType: 'text',
          quotedMessageId: this.quotedMessage ? this.quotedMessage.id : null
        }
        
        // 发送消息到后端
        const res = await chatApi.sendMessage(messageData)
        if (res.code === 200 && res.data) {
          const msg = res.data
          
          // 更新会话ID（如果是新创建的会话）
          if (!this.selectedChat.conversationId && msg.conversationId) {
            this.selectedChat.conversationId = msg.conversationId
          }
          
          // 创建消息对象，格式与WebSocket消息一致
          const newMessage = {
            id: msg.id,
            type: msg.messageType || 'text',
            content: msg.content,
            time: msg.createdAt,
            isOwn: true,
            isRecalled: msg.isRecalled || false,
            fileName: msg.fileName,
            fileSize: msg.fileSize,
            url: msg.fileUrl,
            quotedMessage: msg.quotedMessage,
            conversationId: msg.conversationId
          }
          
          // 检查消息是否已存在（避免WebSocket重复添加）
          const existingMessage = this.selectedChat.messages.find(m => m.id === newMessage.id)
          if (!existingMessage) {
            this.selectedChat.messages.push(newMessage)
            this.selectedChat.lastMessage = this.getMessageDisplayText(newMessage)
            this.selectedChat.lastMessageTime = newMessage.time
            
            // 滚动到底部
            this.$nextTick(() => {
              this.scrollToBottom()
            })
          }
          
          this.messageInput = ''
          this.quotedMessage = null
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        this.$message.error('发送消息失败')
      }
    },
    scrollToBottom() {
      // 使用requestAnimationFrame和延迟确保DOM更新后再滚动
      const messagesList = this.$refs.messagesContainer?.querySelector('.messages-list')
      if (!messagesList) {
        console.warn('学生端：消息列表元素不存在')
        return
      }
      
      console.log('学生端：开始滚动到底部', {
        scrollTop: messagesList.scrollTop,
        scrollHeight: messagesList.scrollHeight,
        clientHeight: messagesList.clientHeight
      })
      
      // 立即尝试滚动
      messagesList.scrollTop = messagesList.scrollHeight
      
      // 使用requestAnimationFrame确保在下一帧执行
      requestAnimationFrame(() => {
        messagesList.scrollTop = messagesList.scrollHeight
        console.log('学生端：requestAnimationFrame后滚动', {
          scrollTop: messagesList.scrollTop,
          scrollHeight: messagesList.scrollHeight
        })
        
        // 延迟再次设置，确保内容高度已更新
        setTimeout(() => {
          messagesList.scrollTop = messagesList.scrollHeight
          console.log('学生端：setTimeout后滚动', {
            scrollTop: messagesList.scrollTop,
            scrollHeight: messagesList.scrollHeight
          })
        }, 200)
      })
    },
    formatTime(timeStr) {
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) { // 1分钟内
        return '刚刚'
      } else if (diff < 3600000) { // 1小时内
        return `${Math.floor(diff / 60000)}分钟前`
      } else if (diff < 86400000) { // 1天内
        return `${Math.floor(diff / 3600000)}小时前`
      } else if (diff < 86400000 * 7) { // 1周内
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        return weekdays[date.getDay()]
      } else {
        return date.toLocaleDateString()
      }
    },
    formatMessageTime(timeStr) {
      const date = new Date(timeStr)
      return date.toLocaleTimeString('zh-CN', { 
        hour12: false, 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    },
    initScrollbarBehavior() {
      this.$nextTick(() => {
        const messagesList = this.$refs.messagesContainer?.querySelector('.messages-list')
        const chatList = document.querySelector('.chat-list')
        
        if (messagesList) {
          // 监听滚动事件，滚动时显示滚动条
          let scrollTimer = null
          messagesList.addEventListener('scroll', () => {
            messagesList.classList.add('scrolling')
            
            // 清除之前的定时器
            if (scrollTimer) {
              clearTimeout(scrollTimer)
            }
            
            // 停止滚动2秒后隐藏滚动条
            scrollTimer = setTimeout(() => {
              messagesList.classList.remove('scrolling')
            }, 2000)
          })
        }
        
        if (chatList) {
          // 同样为联系人列表添加滚动行为
          let chatScrollTimer = null
          chatList.addEventListener('scroll', () => {
            chatList.classList.add('scrolling')
            
            if (chatScrollTimer) {
              clearTimeout(chatScrollTimer)
            }
            
            chatScrollTimer = setTimeout(() => {
              chatList.classList.remove('scrolling')
            }, 2000)
          })
        }
      })
    },
    
    // 右键菜单相关方法
    showContactContextMenu(event, chat) {
      this.contextMenuChat = chat
      this.contextMenuPosition.x = event.clientX
      this.contextMenuPosition.y = event.clientY
      this.contextMenuVisible = true
      
      // 添加全局点击监听，点击其他地方关闭菜单
      this.$nextTick(() => {
        document.addEventListener('click', this.hideContextMenu)
      })
    },
    
    hideContextMenu() {
      this.contextMenuVisible = false
      this.contextMenuChat = null
      document.removeEventListener('click', this.hideContextMenu)
    },
    
    handleContextMenuAction(command) {
      if (this.contextMenuChat) {
        // 设置为当前联系人
        this.currentContact = this.contextMenuChat
        
        // 如果不是当前选中的聊天，先选中它
        if (this.selectedChatId !== this.contextMenuChat.id) {
          this.selectChat(this.contextMenuChat.id)
        }
        
        // 执行对应的操作
        switch (command) {
          case 'profile':
            this.showContactProfile()
            break
          case 'delete':
            this.deleteContact(this.contextMenuChat)
            break
        }
      }
      
      // 关闭右键菜单
      this.hideContextMenu()
    },

    // 处理消息操作
    handleMessageAction(command, message) {
      switch (command) {
        case 'recall':
          this.recallMessage(message)
          break
        case 'copy':
          this.copyMessage(message)
          break
        case 'quote':
          this.quoteMessage(message)
          break
        case 'delete':
          this.deleteMessage(message)
          break
      }
    },
    
    deleteMessage(message) {
      this.$confirm('确定要删除这条消息吗？', '删除消息', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await chatApi.deleteMessage(message.id)
          if (res.code === 200) {
            // 从列表中移除
            const index = this.selectedChat.messages.findIndex(m => m.id === message.id)
            if (index !== -1) {
              this.selectedChat.messages.splice(index, 1)
            }
            this.$message.success('消息已删除')
          }
        } catch (error) {
          console.error('删除消息失败:', error)
          this.$message.error('删除消息失败')
        }
      }).catch(() => {
        // 用户取消
      })
    },

    // 打开消息右键菜单
    openMessageContextMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'message-context-menu'
      
      // 菜单项
      const menuItems = []
      
      // 检查是否是邀请类消息
      const isInviteMessage = ['dorm_invite', 'dorm_invite_accepted', 'dorm_invite_rejected', 'invite'].includes(message.type)
      
      if (message.isOwn) {
        // 自己的消息
        if (this.canRecallMessage(message)) {
          menuItems.push({ text: '撤回', command: 'recall' })
        }
        
        // 邀请消息不显示复制，但显示删除
        if (!isInviteMessage) {
          menuItems.push({ text: '复制', command: 'copy' })
        }
        
        // 所有自己的消息都可以删除
        menuItems.push({ text: '删除', command: 'delete' })
      } else {
        // 别人的消息
        if (!isInviteMessage) {
          menuItems.push({ text: '复制', command: 'copy' })
          menuItems.push({ text: '引用', command: 'quote' })
        } else {
          // 别人的邀请消息，也许可以删除（本地删除，但这里只实现了后端删除接口，且后端限制只能删除自己的）
          // 如果后端限制只能删除自己的，这里就不能显示删除。
          // 目前后端实现确实限制了 senderId == currentUserId。
          // 所以别人的消息不能删除（除非实现一个 removeLocalMessage 接口，或者 modify backend to allow deleting received messages from own view）
          // 暂时不给别人的消息显示删除选项
        }
      }
      
      if (menuItems.length === 0) return
      
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
    }
  },

  beforeUnmount() {
    this.closeMessageContextMenu()
  }
}
</script>

<style scoped>
.chat-messages-page {
  height: calc(100vh - 70px);
  background: #f5f5f5;
  margin: 0;
  padding: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.chat-container {
  display: flex;
  height: 100%;
  background: #fff;
  overflow: hidden;
  position: relative;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 左侧聊天列表 */
.chat-list-panel {
  width: 350px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fa;
  flex-shrink: 0;
}

.chat-list-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
  height: 60px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.search-area {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-box {
  flex: 1;
}

.search-box :deep(.el-input) {
  border-radius: 20px;
}

.search-box :deep(.el-input__wrapper) {
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
}

.search-box :deep(.el-input__prefix-inner) {
  color: #909399;
}

.chat-list {
  flex: 1;
  background: #f8f9fa;
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 禁用滚动条 */
  height: auto; /* 使用flex自动计算高度 */
  min-height: 0; /* 允许flex收缩 */
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin: 2px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  position: relative;
  background: #fff;
  border: 1px solid transparent;
  min-height: 70px;
  flex-shrink: 0;
}

.chat-item:hover {
  background: #f0f9ff;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.chat-item.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.chat-avatar {
  position: relative;
  margin-right: 12px;
  margin-top: 20px;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  height: 16px;
  line-height: 12px;
  text-align: center;
}

.chat-info {
  flex: 1;
  min-width: 0;
  overflow: hidden; /* 防止内容溢出 */
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}



.chat-name {
  font-weight: 500;
  font-size: 14px;
  color: #000; /* 联系人名字改为黑色 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px; /* 限制最大宽度 */
}

.chat-time {
  font-size: 11px;
  color: #999;
  flex-shrink: 0; /* 防止时间被压缩 */
}

.chat-item.active .chat-time {
  color: rgba(255, 255, 255, 0.8);
}

.chat-preview {
  display: flex;
  align-items: center;
}

.last-message {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-item.active .last-message {
  color: rgba(255, 255, 255, 0.9);
}

.empty-chat-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #999;
  background: #fafafa;
  margin: 12px;
  border-radius: 12px;
  border: 2px dashed #e0e0e0;
  gap: 16px; /* 添加间距 */
}

/* 搜索结果区域 */
.search-results-section {
  padding: 8px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 8px;
}

.search-results-header {
  font-size: 12px;
  color: #909399;
  padding: 8px 16px;
  font-weight: 500;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin: 2px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
}

.search-result-item:hover {
  background: #f0f9ff;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.search-result-item .chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.search-result-item .user-type {
  font-size: 12px;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
  color: #ddd;
  margin: 0; /* 移除margin，使用gap统一间距 */
}

/* 右侧聊天内容 */
.chat-content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative; /* 确保定位正确 */
  min-height: 0; /* 允许flex收缩 */
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative; /* 确保子元素定位正确 */
}

.chat-content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  height: 60px;
  position: relative;
  z-index: 10;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0; /* 允许收缩 */
  overflow: hidden; /* 防止内容溢出 */
}

.back-btn {
  color: #409eff;
  padding: 4px 8px;
  margin-right: 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.chat-user-name {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
  margin: 0;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-container {
  flex: 1;
  background: #f8f9fa;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0; /* 允许flex收缩 */
  position: relative; /* 确保定位正确 */
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
  min-height: 0; /* 允许flex收缩 */
}

.message-item {
  display: flex;
  align-items: flex-start; /* 改为顶部对齐，让头像与文本第一行齐平 */
  gap: 8px;
}

.message-item.own-message {
  justify-content: flex-end; /* 自己的消息右对齐 */
}

.message-item.recalled {
  justify-content: center; /* 撤回消息居中 */
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 60%;
  min-width: 0; /* 允许内容收缩 */
  word-wrap: break-word; /* 长文本自动换行 */
}

.message-content.own-content {
  align-items: flex-end; /* 自己的消息内容右对齐 */
}

.message-bubble {
  background: #fff;
  padding: 8px 12px;
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  word-break: break-word; /* 强制长单词换行 */
  max-width: 100%; /* 确保不超出容器 */
  width: fit-content; /* 气泡宽度根据内容自适应 */
  overflow-wrap: break-word; /* 现代浏览器的换行属性 */
}

.own-bubble {
  background: #409eff;
  color: #fff;
}

.message-text {
  line-height: 1.4;
  font-size: 14px;
  color: #000; /* 对方的聊天文字改为黑色 */
}

.own-bubble .message-text {
  color: #fff; /* 自己的聊天文字保持白色 */
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 8px;
}

.message-time.own-time {
  text-align: right; /* 自己的时间右对齐 */
}

/* 撤回消息样式 - 微信风格 */
.recalled-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 12px;
  margin: 8px auto;
  background: linear-gradient(135deg, #f5f5f5 0%, #ececec 100%);
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  max-width: 200px;
  align-self: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.recalled-message:hover {
  background: linear-gradient(135deg, #f0f0f0 0%, #e8e8e8 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

.recall-icon {
  color: #bbb;
  font-size: 12px;
  flex-shrink: 0;
}

.recall-text {
  color: #888;
  font-size: 12px;
  line-height: 1.3;
  font-weight: 400;
  text-align: center;
  white-space: nowrap;
}

.message-bubble-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.message-input-panel {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 12px 24px;
  flex-shrink: 0;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.06);
  min-height: 100px;
  display: flex;
  flex-direction: column;
  position: sticky; /* 使用sticky定位确保在底部 */
  bottom: 0;
  z-index: 10;
  min-width: 0; /* 防止水平溢出 */
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  height: 32px;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  height: 60px;
  min-width: 0; /* 防止水平溢出 */
}

.input-area .el-textarea {
  flex: 1;
  min-width: 0; /* 允许收缩 */
}

.input-area :deep(.el-textarea__inner) {
  resize: none;
  border-radius: 16px;
  padding: 10px 16px;
  border: 1px solid #e4e7ed;
  background: #f8f9fa;
  min-height: 60px !important;
  max-height: 60px !important;
  overflow-y: auto;
  overflow-x: hidden; /* 防止水平滚动 */
  word-wrap: break-word; /* 长文本自动换行 */
}

.send-btn {
  height: 36px;
  border-radius: 18px;
  padding: 0 16px;
  font-weight: 500;
  flex-shrink: 0;
}

.empty-chat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  background: #f8f9fa;
}

.empty-chat-content .empty-icon {
  color: #ddd;
  margin-bottom: 24px;
}

.empty-chat-content h3 {
  color: #666;
  margin-bottom: 8px;
}

.empty-chat-content p {
  color: #999;
  font-size: 14px;
}

/* 添加更多交互效果 */
.chat-item:hover::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #409eff;
  transition: all 0.3s ease;
}

.chat-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #409eff;
}

.message-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-messages-page {
    height: 100vh;
    margin: 0;
    padding: 8px;
  }
  
  .chat-container {
    height: calc(100vh - 16px);
  }
  
  .chat-list-panel {
    width: 280px;
    min-width: 0; /* 防止水平溢出 */
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .message-input-panel {
    padding: 16px;
  }
  
  .input-area {
    flex-direction: column;
    gap: 12px;
  }
  
  .send-btn {
    align-self: flex-end;
    width: 80px;
  }
}

/* 保持外层容器不滚动，只允许内容区域滚动 */
.chat-messages-page, 
.chat-container,
.chat-content-panel,
.chat-list-panel,
.messages-container,
.message-input-panel {
  overflow: hidden !important;
}

/* 隐藏不需要的滚动条 */
.chat-messages-page::-webkit-scrollbar,
.chat-container::-webkit-scrollbar,
.chat-content-panel::-webkit-scrollbar,
.message-input-panel::-webkit-scrollbar {
  display: none !important;
  width: 0 !important;
}

.chat-content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 0;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.empty-chat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  background: #f8f9fa;
}
.messages-list::-webkit-scrollbar,
.chat-list::-webkit-scrollbar {
  width: 6px;
}

.messages-list::-webkit-scrollbar-track,
.chat-list::-webkit-scrollbar-track {
  background: transparent;
}

.messages-list::-webkit-scrollbar-thumb,
.chat-list::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 3px;
  transition: all 0.3s ease;
}

/* 悬停时显示滚动条 */
.messages-list:hover::-webkit-scrollbar-thumb,
.chat-list:hover::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

/* 滚动时显示滚动条 */
.messages-list.scrolling::-webkit-scrollbar-thumb,
.chat-list.scrolling::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.4);
}

/* 悬停和滚动时加深滚动条 */
.messages-list:hover::-webkit-scrollbar-thumb:hover,
.chat-list:hover::-webkit-scrollbar-thumb:hover,
.messages-list.scrolling::-webkit-scrollbar-thumb:hover,
.chat-list.scrolling::-webkit-scrollbar-thumb:hover,
.messages-list::-webkit-scrollbar-thumb:active,
.chat-list::-webkit-scrollbar-thumb:active {
  background: rgba(0, 0, 0, 0.6);
}

/* 为消息容器添加悬停状态检测 */
.messages-container:hover .messages-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

.messages-container:hover .messages-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

/* 滚动条淡入淡出动画 */
.messages-list::-webkit-scrollbar-thumb,
.chat-list::-webkit-scrollbar-thumb {
  transition: background 0.3s ease, opacity 0.3s ease;
}

/* 恢复消息列表滚动功能 */
.messages-list {
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px; /* 为滚动条留出空间 */
}

/* 恢复联系人列表滚动功能 */
.chat-list {
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px; /* 为滚动条留出空间 */
}

/* 联系人资料对话框样式 */
.contact-profile {
  text-align: center;
}

.profile-avatar {
  margin-bottom: 20px;
}

.profile-info h3 {
  margin: 0 0 20px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.profile-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 0;
  text-align: left;
}

.profile-item .label {
  color: #909399;
  font-weight: 500;
  min-width: 80px;
}

.profile-item span:last-child {
  color: #303133;
  flex: 1;
  text-align: right;
}

/* 添加联系人对话框样式 */
.add-contact-dialog {
  max-height: 400px;
  overflow-y: auto;
}

.search-results {
  margin-top: 16px;
}

.results-header {
  font-size: 14px;
  font-weight: 500;
  color: #909399;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-result-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.user-avatar {
  margin-right: 12px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.user-type {
  font-size: 12px;
  color: #909399;
}

.no-results, .search-tips {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
  text-align: center;
}

.no-results-icon, .tips-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 16px;
}

/* 图片消息样式 */
.message-image {
  max-width: 200px;
  border-radius: 8px;
  overflow: hidden;
}

.chat-image {
  width: 100%;
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.chat-image:hover {
  transform: scale(1.02);
}

/* 文件消息样式 */
.message-file {
  min-width: 200px;
  max-width: 300px;
}

.file-info {
  display: flex;
  align-items: center;
  padding: 8px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  gap: 12px;
}

.own-bubble .file-info {
  background: rgba(255, 255, 255, 0.2);
}

.file-icon {
  font-size: 24px;
  color: #606266;
  flex-shrink: 0;
}

.own-bubble .file-icon {
  color: rgba(255, 255, 255, 0.8);
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.own-bubble .file-name {
  color: #fff;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.own-bubble .file-size {
  color: rgba(255, 255, 255, 0.7);
}

.download-btn {
  color: #409eff;
  padding: 4px;
  flex-shrink: 0;
}

.own-bubble .download-btn {
  color: rgba(255, 255, 255, 0.9);
}

.download-btn:hover {
  background: rgba(64, 158, 255, 0.1);
  border-radius: 4px;
}

.own-bubble .download-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 文件类型图标样式 */
.file-pdf {
  color: #e74c3c;
}

.file-word {
  color: #2980b9;
}

.file-excel {
  color: #27ae60;
}

.file-powerpoint {
  color: #e67e22;
}

.file-archive {
  color: #9b59b6;
}

.file-text {
  color: #34495e;
}

.file-default {
  color: #95a5a6;
}

/* 上传按钮样式优化 */
.input-toolbar .el-upload {
  display: inline-block;
}

.input-toolbar .el-upload .el-button {
  padding: 4px;
  border: none;
  background: transparent;
  color: #606266;
  transition: all 0.3s ease;
}

.input-toolbar .el-upload .el-button:hover {
  color: #409eff;
  background: #f0f9ff;
  border-radius: 4px;
  transform: scale(1.1);
}

/* 文件上传按钮图标放大 */
.upload-file-btn .upload-icon {
  font-size: 18px !important;
}

/* 引用消息样式 */
.quoted-message-display {
  background: #f8f9fa;
  border-left: 4px solid #409eff;
  margin-bottom: 12px;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

.quoted-content {
  padding: 8px 12px;
}

.quoted-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.quoted-sender {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

.cancel-quote-btn {
  margin-left: auto;
  color: #909399;
}

.cancel-quote-btn:hover {
  color: #f56c6c;
}

.quoted-text {
  font-size: 13px;
  color: #666;
  background: #fff;
  padding: 6px 8px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 消息气泡中的引用样式 - 微信风格 */
.quoted-message-in-bubble {
  position: relative;
  background: rgba(0, 0, 0, 0.03);
  padding: 8px 10px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.quoted-message-in-bubble:hover {
  background: rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.own-bubble .quoted-message-in-bubble {
  background: rgba(255, 255, 255, 0.15);
}

.own-bubble .quoted-message-in-bubble:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* 左侧竖线 */
.quoted-line {
  width: 3px;
  background: #409eff;
  border-radius: 2px;
  flex-shrink: 0;
}

.own-bubble .quoted-line {
  background: rgba(255, 255, 255, 0.8);
}

/* 引用内容区域 */
.quoted-content-wrapper {
  flex: 1;
  min-width: 0;
}

.quoted-sender-name {
  font-size: 11px;
  color: #409eff;
  font-weight: 600;
  margin-bottom: 2px;
  letter-spacing: 0.2px;
}

.own-bubble .quoted-sender-name {
  color: rgba(255, 255, 255, 0.95);
}

.quoted-message-content {
  font-size: 12px;
  color: #666;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 400;
}

.own-bubble .quoted-message-content {
  color: rgba(255, 255, 255, 0.85);
}

/* 消息高亮动画效果 */
.highlight-message {
  animation: highlight-pulse 3s ease-in-out;
}

@keyframes highlight-pulse {
  0% {
    background-color: transparent;
    transform: scale(1);
  }
  10% {
    background-color: rgba(64, 158, 255, 0.1);
    transform: scale(1.02);
  }
  90% {
    background-color: rgba(64, 158, 255, 0.05);
    transform: scale(1.01);
  }
  100% {
    background-color: transparent;
    transform: scale(1);
  }
}

/* 图片预览对话框样式 */
.image-preview-dialog :deep(.el-dialog) {
  margin-top: 5vh !important;
  margin-bottom: 5vh !important;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.image-preview-dialog :deep(.el-dialog__body) {
  padding: 10px 20px;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.preview-image-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 8px;
  min-height: 400px;
  max-height: 70vh;
  overflow: hidden;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
  cursor: zoom-in;
}

.preview-image:hover {
  transform: scale(1.02);
}

.preview-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.image-name {
  color: #606266;
  font-size: 14px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 20px;
}

.preview-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 聊天中的图片样式优化 */
.chat-image {
  width: 100%;
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  object-fit: cover;
  display: block;
}

.chat-image:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 图片加载错误时的样式 */
.chat-image[src*="data:image/svg+xml"] {
  background: #f5f5f5;
  border: 1px dashed #ddd;
}

/* 响应式图片预览 */
@media (max-width: 768px) {
  .image-preview-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 2vh auto !important;
  }
  
  .preview-image-container {
    min-height: 300px;
    max-height: 60vh;
  }
  
  .preview-footer {
    flex-direction: column;
    gap: 12px;
  }
  
  .image-name {
    text-align: center;
    margin-right: 0;
  }
}


/* 更多操作按钮样式 */
.more-actions-btn {
  color: #606266;
  transition: all 0.3s ease;
  padding: 8px;
  border-radius: 50%;
}

.more-actions-btn:hover {
  color: #409eff;
  background: #f5f7fa;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.more-actions-btn .el-icon {
  font-size: 18px;
}

/* 宿舍邀请对话框样式 */
.dorm-invite-dialog {
  max-height: 600px;
  overflow-y: auto;
}

.invite-user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  text-align: center;
}

.user-details h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.user-type {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.invite-info-section {
  padding: 16px 0;
}

.invite-description {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #b3e5fc;
}

.desc-icon {
  color: #409eff;
  font-size: 18px;
  margin-top: 2px;
  flex-shrink: 0;
}

.invite-description p {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.invite-message-section {
  margin-top: 8px;
}

/* 室友邀请消息样式 */
.invite-message {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #b3e5fc;
  border-radius: 12px;
  padding: 16px;
  max-width: 350px;
  min-width: 300px;
  box-shadow: 0 2px 8px rgba(3, 169, 244, 0.1);
  transition: all 0.3s ease;
}

.own-bubble .invite-message {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  border-color: #ffcc02;
  box-shadow: 0 2px 8px rgba(255, 204, 2, 0.1);
}

.invite-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.invite-icon {
  color: #1976d2;
  font-size: 18px;
}

.own-bubble .invite-icon {
  color: #f57c00;
}

.invite-title {
  font-weight: 600;
  color: #1976d2;
  flex: 1;
}

.own-bubble .invite-title {
  color: #f57c00;
}

.invite-content {
  margin-bottom: 12px;
}

.invite-dorm-info {
  background: rgba(255, 255, 255, 0.6);
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #424242;
}

.invite-message-text {
  color: #424242;
  line-height: 1.5;
  font-size: 14px;
}

.invite-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
}

.invite-actions .el-button {
  min-width: 60px;
}

/* 宿舍邀请消息样式 */
.dorm-invite-message {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #b3e5fc;
  border-radius: 12px;
  padding: 16px;
  max-width: 320px;
  min-width: 280px;
  box-shadow: 0 2px 8px rgba(3, 169, 244, 0.1);
  transition: all 0.3s ease;
}

.own-bubble .dorm-invite-message {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  border-color: #ffcc02;
  box-shadow: 0 2px 8px rgba(255, 204, 2, 0.1);
}

.dorm-invite-message:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(3, 169, 244, 0.15);
}

.own-bubble .dorm-invite-message:hover {
  box-shadow: 0 4px 16px rgba(255, 204, 2, 0.15);
}

.invite-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(3, 169, 244, 0.2);
}

.own-bubble .invite-header {
  border-bottom-color: rgba(255, 204, 2, 0.3);
}

.invite-icon {
  color: #03a9f4;
  font-size: 18px;
}

.own-bubble .invite-icon {
  color: #ff8f00;
}

.invite-title {
  font-weight: 600;
  color: #0277bd;
  font-size: 14px;
}

.own-bubble .invite-title {
  color: #e65100;
}

.invite-content {
  margin-bottom: 12px;
}


.invite-message-text {
  color: #37474f;
  font-size: 13px;
  line-height: 1.4;
  background: rgba(255, 255, 255, 0.6);
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid rgba(3, 169, 244, 0.1);
}

.own-bubble .invite-message-text {
  color: #3e2723;
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 204, 2, 0.2);
}

.invite-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(3, 169, 244, 0.2);
}

.own-bubble .invite-actions {
  border-top-color: rgba(255, 204, 2, 0.3);
}

.pending-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.received-invite-actions {
  display: flex;
  gap: 8px;
}

.received-invite-actions .el-button {
  font-size: 12px;
  padding: 4px 12px;
  height: 28px;
  border-radius: 14px;
  font-weight: 500;
}

.own-invite-status .el-tag,
.accepted-status .el-tag,
.rejected-status .el-tag {
  font-weight: 500;
  border-radius: 12px;
  font-size: 11px;
  padding: 2px 8px;
}

/* 响应式宿舍邀请样式 */
@media (max-width: 768px) {
  .dorm-invite-message {
    max-width: 280px;
    min-width: 240px;
  }
  
  .dorm-info-row {
    font-size: 12px;
  }
  
  .invite-message-text {
    font-size: 12px;
  }
  
  .received-invite-actions .el-button {
    font-size: 11px;
    padding: 3px 10px;
    height: 26px;
  }
}

/* 右键菜单样式 */
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #e4e7ed;
  overflow: hidden;
  min-width: 160px;
  animation: contextMenuFadeIn 0.2s ease-out;
}

@keyframes contextMenuFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-5px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.context-menu-list {
  padding: 6px 0;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  user-select: none;
}

.context-menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.context-menu-item.context-menu-divider {
  border-top: 1px solid #e4e7ed;
  margin-top: 6px;
  padding-top: 16px;
}

.context-menu-item.context-menu-divider:hover {
  color: #f56c6c;
}

.context-menu-item .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.context-menu-item span {
  flex: 1;
}

.context-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  background: transparent;
}

/* 联系人列表项右键效果 */
.chat-item {
  user-select: none;
}

.chat-item:active {
  transform: scale(0.98);
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
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.loading-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;
}

.loading-profile .is-loading {
  font-size: 32px;
  margin-bottom: 12px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
