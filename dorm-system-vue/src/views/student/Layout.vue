<template>
  <div class="student-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="sidebar">
      <div class="sidebar-header">
        <div v-if="!isCollapse" class="logo-container">
          <el-icon class="logo-icon"><School /></el-icon>
          <span class="logo-text">学生宿舍系统</span>
        </div>
        <el-icon class="logo-icon" v-else><School /></el-icon>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        router
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-sub-menu index="dorm-allocation">
          <template #title>
            <el-icon><Connection /></el-icon>
            <span>宿舍分配</span>
          </template>
          <el-menu-item index="/student/allocation-flow">
            <el-icon><Connection /></el-icon>
            <template #title>分配流程</template>
          </el-menu-item>
          <el-menu-item index="/student/roommate-match">
            <el-icon><Connection /></el-icon>
            <template #title>室友匹配</template>
          </el-menu-item>

        </el-sub-menu>
        
        <el-sub-menu index="dorm-service">
          <template #title>
            <el-icon><House /></el-icon>
            <span>宿舍服务</span>
          </template>
          <el-menu-item index="/student/dorm-management">
            <el-icon><Document /></el-icon>
            <template #title>宿舍申请</template>
          </el-menu-item>
          <el-menu-item index="/student/material-management">
            <el-icon><Box /></el-icon>
            <template #title>用品管理</template>
          </el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="messages">
          <template #title>
            <el-icon><Message /></el-icon>
            <span>消息中心</span>
          </template>
          <el-menu-item index="/student/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>公告</template>
          </el-menu-item>
          <el-menu-item index="/student/dorm-chat">
            <el-icon><ChatLineRound /></el-icon>
            <template #title>宿舍群聊</template>
          </el-menu-item>
          <el-menu-item index="/student/building-info">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>楼栋动态</template>
          </el-menu-item>
          <el-menu-item index="/student/chat-messages">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>聊天消息</template>
          </el-menu-item>
        </el-sub-menu>
        

      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-button
            type="text"
            @click="toggleCollapse"
            class="collapse-btn"
          >
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>学生端</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleNotificationCommand" trigger="click" placement="bottom-end" class="notification-dropdown">
            <div class="notification-trigger">
              <el-icon class="notification-icon"><Bell /></el-icon>
              <div v-if="notificationCount > 0" class="notification-count">
                {{ notificationCount > 99 ? '99+' : notificationCount }}
              </div>
            </div>
            <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="notifications">
                    <div class="dropdown-item-content">
                      <div class="dropdown-item-left">
                        <el-icon><Bell /></el-icon>
                        <span>通知</span>
                      </div>
                      <el-badge 
                        v-if="systemNotificationCount > 0" 
                        :value="systemNotificationCount" 
                        :max="99" 
                        class="dropdown-badge"
                      />
                    </div>
                  </el-dropdown-item>
                  <el-dropdown-item command="chat-messages">
                    <div class="dropdown-item-content">
                      <div class="dropdown-item-left">
                        <el-icon><ChatDotRound /></el-icon>
                        <span>聊天消息</span>
                      </div>
                      <el-badge 
                        v-if="chatMessageCount > 0" 
                        :value="chatMessageCount" 
                        :max="99" 
                        class="dropdown-badge"
                      />
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="currentUserAvatar" />
              <span class="username">{{ currentUserName || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>


    <!-- 个人设置对话框 -->
    <el-dialog v-model="showSettingsDialog" title="个人设置" width="900px">
      <el-tabs type="border-card" v-model="activeTab">
        <el-tab-pane label="安全设置" name="security">
          <div class="security-settings">
            <div class="security-item">
              <div class="security-item-header">
                <el-icon class="security-icon"><Message /></el-icon>
                <div class="security-item-info">
                  <h4>邮箱设置</h4>
                  <p class="current-email">当前邮箱：{{ settingsForm.email || '未绑定' }}</p>
                </div>
                <el-button type="primary" plain @click="showEmailDialog = true">
                  {{ settingsForm.email ? '修改邮箱' : '绑定邮箱' }}
                </el-button>
              </div>
            </div>
            
            <div class="security-item">
              <div class="security-item-header">
                <el-icon class="security-icon"><Lock /></el-icon>
                <div class="security-item-info">
                  <h4>密码设置</h4>
                  <p>定期修改密码，保护账户安全</p>
                </div>
                <el-button type="danger" plain @click="showPasswordDialog = true">修改密码</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="showSettingsDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改邮箱对话框 -->
    <el-dialog v-model="showEmailDialog" title="修改邮箱" width="500px">
      <el-form :model="emailForm" label-width="80px">
        <el-form-item label="新邮箱" required>
          <el-input v-model="emailForm.newEmail" type="email" placeholder="请输入新邮箱地址" />
        </el-form-item>
        <el-form-item label="邀请码" required>
          <el-input v-model="emailForm.invitationCode" placeholder="请输入邀请码" />
        </el-form-item>
        <el-form-item>
          <div class="email-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>使用原始邀请码验证身份，修改后邀请码将更新</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" @click="changeEmail">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="450px">
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="旧密码" required>
          <el-input 
            v-model="passwordForm.oldPassword" 
            type="password" 
            show-password 
            placeholder="请输入当前密码"
          />
        </el-form-item>
        
        <el-form-item label="新密码" required>
          <el-input 
            v-model="passwordForm.newPassword" 
            type="password" 
            show-password 
            placeholder="请输入新密码（至少8位）"
          />
          <!-- 简化的密码强度显示 -->
          <div class="simple-password-strength">
            <span class="strength-label" :style="{ color: getPasswordStrengthColor() }">
              密码强度{{ getPasswordStrengthText() }}
            </span>
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ 
                  width: getPasswordStrengthPercent() + '%',
                  backgroundColor: getPasswordStrengthColor()
                }"
              ></div>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="确认密码" required>
          <el-input 
            v-model="passwordForm.confirmPassword" 
            type="password" 
            show-password 
            placeholder="请再次输入新密码"
          />
          <div v-if="passwordForm.confirmPassword && passwordForm.newPassword !== passwordForm.confirmPassword" class="password-mismatch">
            <el-icon><WarningFilled /></el-icon>
            两次输入的密码不一致
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="changePassword"
          :disabled="!isPasswordFormValid"
        >
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getUnreadCount as getNotificationUnreadCount } from '@/api/notification.js'
import { getConversations } from '@/api/chat.js'
import { getUserSettings, updateUserSettings } from '@/api/userSettings.js'
import { getProfile, updateProfile, updatePassword } from '@/api/profile.js'
import { getStudentAnnouncementList } from '@/api/announcement.js'
import { getAssignedArea } from '@/api/studentAllocation.js'
import { clearCurrentUser, getCurrentUser, setCurrentUser } from '@/utils/auth.js'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'

export default {
  name: 'StudentLayout',
  data() {
    return {
      isCollapse: false,
      currentUserName: '',
      currentUserAvatar: defaultAvatar,
      notificationCount: 0,
      systemNotificationCount: 0, // 系统通知未读数
      chatMessageCount: 0, // 聊天消息未读数
      showSettingsDialog: false,
      activeTab: 'security',
      settingsForm: {
        email: '',
        avatar: ''
      },
      showEmailDialog: false,
      emailForm: {
        newEmail: '',
        invitationCode: ''
      },
      showPasswordDialog: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      studentBuildingId: null, // 学生所在楼栋ID（用于过滤公告）

      pageTitles: {
        '/student/profile': '个人中心',
        '/student/allocation-flow': '分配流程',
        '/student/roommate-match': '室友匹配',
        '/student/dorm-management': '宿舍申请',
        '/student/material-management': '用品管理',
        '/student/dorm-chat': '宿舍群聊',
        '/student/building-info': '楼栋动态',
        '/student/notifications': '公告',
        '/student/chat-messages': '聊天消息'
      }
    }
  },
  mounted() {
    // 确保有默认的currentUser（前端演示用）
    this.ensureCurrentUser();
    // 加载当前用户名
    this.loadCurrentUserName();
    // 初始化默认数据
    // initializeDefaultData();
    // 计算实际未读消息数量
    this.calculateUnreadCount();
    // 模拟实时更新通知数量
    this.startNotificationTimer();
    
    // 监听通知已读事件，实时更新未读计数
    this.notificationReadHandler = () => {
      this.calculateUnreadCount();
    };
    window.addEventListener('notificationRead', this.notificationReadHandler);
    
    // 监听头像更新事件，实时更新右上角头像
    this.avatarUpdateHandler = (event) => {
      if (event.detail && event.detail.newAvatar) {
        this.currentUserAvatar = event.detail.newAvatar
      }
    };
    window.addEventListener('avatarUpdated', this.avatarUpdateHandler);


  },
  beforeUnmount() {
    if (this.notificationTimer) {
      clearInterval(this.notificationTimer);
    }
    // 移除事件监听器
    if (this.notificationReadHandler) {
      window.removeEventListener('notificationRead', this.notificationReadHandler);
    }
    if (this.avatarUpdateHandler) {
      window.removeEventListener('avatarUpdated', this.avatarUpdateHandler);
    }
  },
  computed: {
    // 密码表单是否有效
    isPasswordFormValid() {
      return this.passwordForm.oldPassword &&
             this.passwordForm.newPassword &&
             this.passwordForm.confirmPassword &&
             this.passwordForm.newPassword === this.passwordForm.confirmPassword &&
             this.passwordForm.newPassword.length >= 8
    },
    activeMenu() {
      return this.$route.path
    },
    currentPageTitle() {
      return this.pageTitles[this.$route.path] || '未知页面'
    },
    deadlineText() {
      if (!this.latestNotice || !this.latestNotice.deadline) return '由学院统一发布，请关注通知中心'
      const end = new Date(this.latestNotice.deadline)
      if (isNaN(end.getTime())) return '由学院统一发布，请关注通知中心'
      const now = new Date()
      const diffMs = end - now
      if (diffMs <= 0) return `${end.toLocaleString()}（已到期）`
      const days = Math.floor(diffMs / 86400000)
      const hours = Math.floor((diffMs % 86400000) / 3600000)
      const minutes = Math.floor((diffMs % 3600000) / 60000)
      const parts = []
      if (days) parts.push(`${days}天`)
      if (hours) parts.push(`${hours}小时`)
      parts.push(`${minutes}分钟`)
      return `${end.toLocaleString()}（剩余 ${parts.join(' ')}）`
    },
    extraPointsList() {
      const points = (this.latestNotice && this.latestNotice.extraPoints) || ''
      if (!points) return []
      return points
        .split(/\r?\n/)
        .map(s => s.trim())
        .filter(Boolean)
    }
  },
  watch: {
    // 监听路由变化，确保菜单状态正确更新
    '$route'(to, from) {
      // 强制更新组件状态
      this.$forceUpdate()
    }
  },
  methods: {
    // 确保有默认的currentUser（前端演示用）
    ensureCurrentUser() {
      try {
        // 🔒 使用会话隔离的 getCurrentUser
        const currentUser = getCurrentUser()
        if (!currentUser) {
          // 创建默认演示用户（张三 - student_001）
          const defaultUser = {
            studentId: '2024001',
            realName: '张三',
            username: 'student_001',
            role: 'student',
            userType: 'student'  // 🔒 必须有 userType 字段
          }
          setCurrentUser(defaultUser)
          console.log('已创建默认演示用户')
        }
      } catch (error) {
        console.error('创建默认用户失败:', error)
      }
    },
    
    // 加载当前登录用户的姓名和头像（从后端API获取，与个人中心保持一致）
    async loadCurrentUserName() {
      try {
        // 🔒 使用会话隔离的 getCurrentUser
        const currentUser = getCurrentUser()
        if (currentUser) {
          // 从后端API获取用户资料
          try {
            const response = await getProfile()
            if (response.code === 200 && response.data) {
              const profile = response.data
              // 设置用户名
              this.currentUserName = profile.realName || currentUser.studentId || '用户'
              // 设置头像（与个人中心页面使用相同的数据源）
              this.currentUserAvatar = profile.avatar || defaultAvatar
              // 同时更新 settingsForm 中的数据
              this.settingsForm.email = profile.email || ''
              this.settingsForm.avatar = profile.avatar || defaultAvatar
            } else {
              // API调用失败，使用默认值
              this.currentUserName = currentUser.realName || currentUser.studentId || '用户'
              this.currentUserAvatar = defaultAvatar
            }
          } catch (apiError) {
            console.error('从API加载用户信息失败:', apiError)
            // API调用失败，使用默认值
            this.currentUserName = currentUser.realName || currentUser.studentId || '用户'
            this.currentUserAvatar = defaultAvatar
          }
        } else {
          this.currentUserName = '用户'
          this.currentUserAvatar = defaultAvatar
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
        this.currentUserName = '用户'
        this.currentUserAvatar = defaultAvatar
      }
    },
    
    async calculateUnreadCount() {
      // 计算系统通知未读数
      try {
        const res = await getNotificationUnreadCount()
        if (res.code === 200) {
          this.systemNotificationCount = res.data || 0
        }
      } catch (error) {
        console.error('获取系统通知未读数失败:', error)
      }

      // 先获取学生楼栋ID（如果还没有获取过）
      if (!this.studentBuildingId) {
        try {
          const areaRes = await getAssignedArea()
          if (areaRes.code === 200 && areaRes.data) {
            this.studentBuildingId = areaRes.data.buildingId
          }
        } catch (e) {
          console.error('获取学生楼栋信息失败:', e)
        }
      }

      // 计算公告未读数（从公告API获取，结合localStorage判断已读状态，只获取本楼栋公告）
      try {
        const params = { currentPage: 1, pageSize: 50 }
        // 如果有楼栋ID，只获取本楼栋相关的公告
        if (this.studentBuildingId) {
          params.buildingId = this.studentBuildingId
        }
        const res = await getStudentAnnouncementList(params)
        if (res.code === 200) {
          const announcements = res.data?.records || res.data || []
          // 获取本地存储的已读公告ID列表
          const readAnnouncementIds = JSON.parse(localStorage.getItem('read_announcement_ids') || '[]')
          // 统计未读公告数量（排除已在localStorage中标记为已读的）
          const unreadAnnouncements = announcements.filter(a => !readAnnouncementIds.includes(a.id)).length
          this.systemNotificationCount += unreadAnnouncements
        }
      } catch (error) {
        console.error('获取公告未读数失败:', error)
      }

      // 计算聊天消息未读数
      try {
        const res = await getConversations()
        if (res.code === 200 && res.data) {
          this.chatMessageCount = res.data.reduce((sum, chat) => sum + (chat.unreadCount || 0), 0)
        }
      } catch (error) {
        console.error('获取聊天未读数失败:', error)
      }
      
      // 更新总未读数
      this.notificationCount = this.systemNotificationCount + this.chatMessageCount
    },
    
    // 监听数据变化，实时更新未读消息数量
    setupNotificationListener() {
      // 移除旧的localStorage监听
    },
    toggleCollapse() {
      this.isCollapse = !this.isCollapse
    },
    showNotifications() {
      this.$router.push('/student/notifications')
    },
    
    handleNotificationCommand(command) {
      if (command === 'notifications') {
        this.$router.push('/student/notifications')
      } else if (command === 'chat-messages') {
        this.$router.push('/student/chat-messages')
      }
    },
    startNotificationTimer() {
      // 每30秒重新计算未读消息数量
      this.notificationTimer = setInterval(() => {
        this.calculateUnreadCount();
      }, 30000);
    },
    handleMenuSelect(index) {
      // 手动处理路由跳转，确保跳转正常
      if (index && index !== this.$route.path) {
        this.$router.push(index).catch(err => {
          console.error('路由跳转失败:', err)
        })
      }
    },
    handleUserCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 🔒 清除当前角色的登录信息（会话隔离）
          clearCurrentUser()
          this.$router.push('/login')
        })
      } else if (command === 'profile') {
        this.$router.push('/student/profile')
      } else if (command === 'settings') {
        this.loadSettingsData()
        this.showSettingsDialog = true
      }
    },
    
    // 统一获取用户邮箱的方法
    getCurrentUserEmail() {
      try {
        // 🔒 使用会话隔离的 getCurrentUser
        const currentUser = getCurrentUser()
        if (!currentUser) return ''
        
        // 优先从注册用户信息中获取邮箱（这是最权威的来源）
        const registeredUsers = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
        const user = registeredUsers.find(u => u.studentId === currentUser.studentId)
        if (user && user.email) {
          return user.email
        }
        
        // 备用：从当前用户信息获取
        return currentUser.email || ''
      } catch (error) {
        console.error('获取用户邮箱失败:', error)
        return ''
      }
    },
    
    // 统一更新用户邮箱的方法
    updateUserEmail(newEmail) {
      try {
        // 🔒 使用会话隔离的 getCurrentUser
        const currentUser = getCurrentUser()
        if (!currentUser) return false
        
        // 1. 更新 registeredUsers 中的邮箱
        const registeredUsers = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
        const userIndex = registeredUsers.findIndex(u => u.studentId === currentUser.studentId)
        if (userIndex !== -1) {
          registeredUsers[userIndex].email = newEmail
          localStorage.setItem('registeredUsers', JSON.stringify(registeredUsers))
        }
        
        // 2. 更新 userProfile 中的邮箱
        const profileKey = `userProfile_${currentUser.studentId}`
        const userProfile = JSON.parse(localStorage.getItem(profileKey) || '{}')
        userProfile.email = newEmail
        localStorage.setItem(profileKey, JSON.stringify(userProfile))
        
        // 3. 更新当前用户信息中的邮箱
        if (currentUser.email !== undefined) {
          currentUser.email = newEmail
          const storageKey = `currentUser_${window.location.pathname.split('/')[1] || 'default'}`
          localStorage.setItem(storageKey, JSON.stringify(currentUser))
        }
        
        return true
      } catch (error) {
        console.error('更新用户邮箱失败:', error)
        return false
      }
    },
    
    // 加载设置数据
    async loadSettingsData() {
      try {
        // 从后端API获取最新的用户信息
        const response = await getProfile()
        if (response.code === 200 && response.data) {
          this.settingsForm.email = response.data.email || ''
          this.settingsForm.avatar = response.data.avatar || ''
        } else {
          // 如果API调用失败，使用本地缓存
          const currentUser = getCurrentUser()
          if (currentUser) {
            this.settingsForm.email = this.getCurrentUserEmail()
            const userProfile = JSON.parse(localStorage.getItem(`userProfile_${currentUser.studentId}`) || '{}')
            this.settingsForm.avatar = userProfile.avatar || ''
          }
        }
      } catch (error) {
        console.error('加载设置数据失败:', error)
        // 出错时使用本地缓存
        const currentUser = getCurrentUser()
        if (currentUser) {
          this.settingsForm.email = this.getCurrentUserEmail()
          const userProfile = JSON.parse(localStorage.getItem(`userProfile_${currentUser.studentId}`) || '{}')
          this.settingsForm.avatar = userProfile.avatar || ''
        }
      }
    },
    
    // 保存设置
    async saveSettings() {
      try {
        // 🔒 使用会话隔离的 getCurrentUser
        const currentUser = getCurrentUser()
        if (currentUser) {
          // 保存个人信息到本地存储（头像等）
          const userProfile = JSON.parse(localStorage.getItem(`userProfile_${currentUser.studentId}`) || '{}')
          userProfile.email = this.settingsForm.email
          userProfile.avatar = this.settingsForm.avatar
          localStorage.setItem(`userProfile_${currentUser.studentId}`, JSON.stringify(userProfile))
          
          this.showSettingsDialog = false
          this.$message.success('设置保存成功')
          
          // 更新头像显示
          this.currentUserAvatar = this.settingsForm.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        }
      } catch (error) {
        console.error('保存设置失败:', error)
        this.$message.error('保存设置失败')
      }
    },
    
    // 修改邮箱
    async changeEmail() {
      if (!this.emailForm.newEmail || !this.emailForm.invitationCode) {
        this.$message.error('请填写完整信息')
        return
      }
      
      // 验证邀请码
      if (!this.emailForm.invitationCode) {
        this.$message.error('请输入邀请码')
        return
      }
      
      try {
        // 调用后端API更新邮箱
        const response = await updateProfile({
          email: this.emailForm.newEmail,
          invitationCode: this.emailForm.invitationCode
        })
        
        if (response.code === 200) {
          // 保存新邮箱到临时变量（因为表单会被重置）
          const newEmail = this.emailForm.newEmail
          
          // 更新设置表单中的邮箱显示
          this.settingsForm.email = newEmail
          
          // 同时更新localStorage（用于本地缓存）
          this.updateUserEmail(newEmail)
          
          this.showEmailDialog = false
          this.emailForm = {
            newEmail: '',
            invitationCode: ''
          }
          
          // 触发自定义事件通知其他组件更新
          window.dispatchEvent(new CustomEvent('emailUpdated', { 
            detail: { newEmail: newEmail } 
          }))
          
          this.$message.success('邮箱修改成功')
        } else {
          this.$message.error(response.message || '邮箱修改失败')
        }
      } catch (error) {
        console.error('修改邮箱失败:', error)
        // 不再显示错误消息，由全局拦截器统一处理
      }
    },
    
    // 修改密码
    async changePassword() {
      if (!this.passwordForm.oldPassword || !this.passwordForm.newPassword || !this.passwordForm.confirmPassword) {
        this.$message.warning('请填写完整的密码信息')
        return
      }
      
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.error('两次输入的新密码不一致')
        return
      }
      
      if (this.passwordForm.newPassword.length < 8) {
        this.$message.error('新密码长度至少8位')
        return
      }
      
      try {
        // 调用后端API修改密码
        const response = await updatePassword({
          currentPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword,
          confirmPassword: this.passwordForm.confirmPassword
        })
        
        if (response.code === 200) {
          // 清空表单
          this.passwordForm = {
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
          }
          
          this.showPasswordDialog = false
          this.$message.success('密码修改成功')
        } else {
          this.$message.error(response.message || '密码修改失败')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        // 不再显示错误消息，由全局拦截器统一处理
      }
    },
    
    // 获取密码强度等级
    getPasswordStrength() {
      const password = this.passwordForm.newPassword
      if (!password) return 'weak'
      
      let score = 0
      
      // 长度检查
      if (password.length >= 8) score += 1
      if (password.length >= 12) score += 1
      
      // 复杂度检查
      if (/[a-z]/.test(password)) score += 1  // 小写字母
      if (/[A-Z]/.test(password)) score += 1  // 大写字母
      if (/[0-9]/.test(password)) score += 1  // 数字
      if (/[^A-Za-z0-9]/.test(password)) score += 1  // 特殊字符
      
      if (score <= 2) return 'weak'
      if (score <= 4) return 'medium'
      return 'strong'
    },
    
    // 获取密码强度文本
    getPasswordStrengthText() {
      const strength = this.getPasswordStrength()
      const texts = {
        weak: '弱',
        medium: '中等',
        strong: '高'
      }
      return texts[strength] || '弱'
    },
    
    // 获取密码强度百分比
    getPasswordStrengthPercent() {
      if (!this.passwordForm.newPassword) return 0
      const strength = this.getPasswordStrength()
      const percents = {
        weak: 33,
        medium: 66,
        strong: 100
      }
      return percents[strength] || 0
    },
    
    // 获取密码强度颜色
    getPasswordStrengthColor() {
      const strength = this.getPasswordStrength()
      const colors = {
        weak: '#f56c6c',
        medium: '#e6a23c',
        strong: '#67c23a'
      }
      return colors[strength] || '#f56c6c'
    },
    
    // 获取密码强度等级数字
    getPasswordStrengthLevel() {
      const strength = this.getPasswordStrength()
      const levels = {
        weak: 1,
        medium: 2,
        strong: 3
      }
      return levels[strength] || 0
    },
    
    // 获取密码强度描述
    getPasswordStrengthDesc() {
      const strength = this.getPasswordStrength()
      const descs = {
        weak: '密码强度较低',
        medium: '密码强度中等',
        strong: '密码强度很好'
      }
      return descs[strength] || ''
    }
  }
}
</script>

<style scoped>
/* 登录公告弹窗美化 */
:deep(.welcome-dialog .el-dialog) { border-radius: 14px; overflow: hidden; }
:deep(.welcome-dialog .el-dialog__header) { text-align: center; border-bottom: 1px solid #ebeef5; margin-right: 0; }
:deep(.welcome-dialog .el-dialog__title) { font-weight: 700; color: #303133; letter-spacing: 1px; }
:deep(.welcome-dialog .el-dialog__body) { background: linear-gradient(180deg, #f8fafc 0%, #ffffff 40%); }
.notice-hero { display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: #ecf5ff; border: 1px solid #e3f2ff; border-radius: 10px; }
.hero-icon { color: #409eff; font-size: 22px; }
.hero-title { font-size: 16px; font-weight: 700; color: #1f2d3d; }
.hero-sub { font-size: 12px; color: #606266; }
.notice-body { margin-top: 12px; color: #606266; line-height: 1.7; }
.content-text { white-space: pre-wrap; background: #f8f9fa; border: 1px solid #edf2f7; padding: 10px 12px; border-radius: 8px; }
.bullet { margin: 8px 0 0 18px; padding: 0; }
.bullet li { list-style: disc; margin: 6px 0; }
.tips-row { display: flex; align-items: center; gap: 6px; margin-top: 10px; color: #909399; }
.tips-icon { color: #409eff; }
.welcome-footer { display: flex; align-items: center; }
.welcome-footer .spacer { flex: 1; }
/* 桌面端全屏布局 */
.student-layout {
  width: 100vw;
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.sidebar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  color: #fff;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
}

.sidebar-menu {
  border: none;
  background: transparent;
  height: calc(100vh - 64px);
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.8);
  height: 56px;
  line-height: 56px;
  font-size: 14px;
  margin: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  transform: translateX(4px);
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

/* 子菜单样式 - 使用管理员端的大小和间距，保持学生端颜色 */
:deep(.el-sub-menu) {
  color: rgba(255, 255, 255, 0.8);
  margin: 4px 12px;
  border-radius: 8px;
}

:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.8) !important;
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

:deep(.el-sub-menu__title::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 0;
  background: linear-gradient(to bottom, #a8b7ff, #d4a5ff);
  border-radius: 0 4px 4px 0;
  transition: height 0.3s ease;
  box-shadow: 0 0 10px rgba(168, 183, 255, 0.5);
}

:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
}

:deep(.el-sub-menu__title:hover::before) {
  height: 60%;
}

:deep(.el-sub-menu.is-opened .el-sub-menu__title) {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #fff !important;
}

:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  background: rgba(255, 255, 255, 0.25) !important;
  color: #fff !important;
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(168, 183, 255, 0.3);
}

:deep(.el-sub-menu.is-active .el-sub-menu__title::before) {
  height: 70%;
}

:deep(.el-sub-menu__title .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

/* 子菜单展开区域 */
:deep(.el-sub-menu .el-menu) {
  background: transparent !important;
  border-radius: 6px;
  margin-bottom: 4px;
  padding: 4px 0;
}

/* 子菜单项样式 - 使用管理员端的大小和间距 */
:deep(.el-sub-menu .el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
  height: 42px;
  line-height: 42px;
  font-size: 13px;
  padding-left: 56px !important;
  margin: 2px 12px;
  margin-right: 12px;
  border-radius: 6px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-left: 3px solid transparent;
}

:deep(.el-sub-menu .el-menu-item::before) {
  display: none;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.18) !important;
  color: #fff !important;
  transform: translateX(4px);
  border-left-color: rgba(168, 183, 255, 0.7);
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
  font-weight: 500;
  border-left-color: #a8b7ff;
  box-shadow: 0 2px 8px rgba(168, 183, 255, 0.25);
}

:deep(.el-sub-menu .el-menu-item .el-icon) {
  font-size: 16px;
  margin-right: 8px;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 21, 41, 0.08);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-btn {
  font-size: 20px;
  color: #606266;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.3s;
}

.collapse-btn:hover {
  background: #f5f7fa;
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 通知下拉框 */
.notification-dropdown {
  margin-right: 8px;
}

/* 通知触发器 */
.notification-trigger {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(64, 158, 255, 0.05);
  border: 1px solid rgba(64, 158, 255, 0.1);
}

.notification-trigger:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.2);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

/* 铃铛图标 */
.notification-icon {
  font-size: 18px;
  color: #606266;
  transition: all 0.3s ease;
}

.notification-trigger:hover .notification-icon {
  color: #409eff;
  transform: rotate(-15deg);
}

/* 未读数量徽章 */
.notification-count {
  position: absolute;
  top: -3px;
  right: -3px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
  color: white;
  font-size: 10px;
  font-weight: 600;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  padding: 0 4px;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
  border: 2px solid #ffffff;
  z-index: 10;
  animation: pulse 2s infinite;
  box-sizing: border-box;
}

/* 脉冲动画 */
@keyframes pulse {
  0% {
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3), 0 0 0 0 rgba(255, 107, 107, 0.7);
  }
  70% {
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3), 0 0 0 6px rgba(255, 107, 107, 0);
  }
  100% {
    box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3), 0 0 0 0 rgba(255, 107, 107, 0);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.main-content {
  background: #f8fafc;
  padding: 32px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-header) {
  height: 64px;
  line-height: 64px;
  padding: 0;
}

:deep(.el-aside) {
  height: 100vh;
}

:deep(.el-main) {
  padding: 0;
}

/* 下拉菜单项样式 */
.dropdown-item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 20px;
  min-height: 20px;
}

.dropdown-item-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

/* 设置描述文字样式 */
.setting-desc {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

/* 头像上传区域样式 */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-avatar-preview {
  flex-shrink: 0;
}

.dropdown-item-left span {
  line-height: 1;
}

.dropdown-badge {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 20px;
}

/* 下拉菜单Badge样式 */
.dropdown-badge :deep(.el-badge__content) {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a52 100%);
  color: white;
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  display: flex !important;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border-radius: 8px;
  transform: none;
  position: static;
  box-shadow: 0 1px 4px rgba(255, 107, 107, 0.3);
  box-sizing: border-box;
  line-height: 1;
}

/* 确保下拉菜单项内容正确显示 */
:deep(.el-dropdown-menu__item) {
  padding: 10px 16px;
  min-width: 160px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  line-height: 1.4;
}

:deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4fd 100%);
  color: #409eff;
}

/* 下拉菜单整体样式 */
:deep(.el-dropdown-menu) {
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(64, 158, 255, 0.1);
  padding: 4px 0;
}

/* 面包屑样式优化 */
:deep(.el-breadcrumb) {
  font-size: 14px;
}

:deep(.el-breadcrumb__item) {
  font-weight: 500;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

:deep(.el-dropdown-menu__item) {
  padding: 12px 20px;
  font-size: 14px;
}

:deep(.el-dropdown-menu__item:hover) {
  background: #f5f7fa;
  color: #409eff;
}

/* 安全设置样式 */
.security-settings {
  padding: 20px 0;
}

.security-item {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #e0e6ed;
  transition: all 0.3s ease;
}

.security-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
}

.security-item-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.security-icon {
  color: #409eff;
  font-size: 24px;
  flex-shrink: 0;
}

.security-item-info {
  flex: 1;
}

.security-item-info h4 {
  margin: 0 0 4px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.security-item-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.current-email {
  color: #409eff !important;
  font-weight: 500;
}

/* 验证码输入样式 */
.verification-code-input {
  display: flex;
  gap: 12px;
  align-items: center;
}

.verification-code-input .el-input {
  flex: 1;
}

.verification-code-input .el-button {
  white-space: nowrap;
  min-width: 120px;
}

.email-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.email-hint .el-icon {
  color: #409eff;
  flex-shrink: 0;
}

.verification-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.verification-actions .email-hint {
  margin-top: 0;
}

/* 简化的密码强度显示 */
.simple-password-strength {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.simple-password-strength .strength-label {
  font-size: 12px;
  font-weight: 500;
  min-width: 80px;
  white-space: nowrap;
}

.simple-password-strength .progress-bar {
  flex: 1;
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  min-width: 100px;
}

.simple-password-strength .progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: all 0.3s ease;
  min-height: 8px;
}
</style>
