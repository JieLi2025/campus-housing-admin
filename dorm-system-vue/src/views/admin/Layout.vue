<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '240px'" class="sidebar">
      <div class="sidebar-header">
        <div v-if="!isCollapse" class="logo-container">
          <el-icon class="logo-icon"><Setting /></el-icon>
          <span class="logo-text">宿舍管理系统</span>
        </div>
        <el-icon class="logo-icon" v-else><Setting /></el-icon>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        router
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-sub-menu index="student-management">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>学生管理</span>
          </template>
          <el-menu-item index="/admin/student-allocation">
            <el-icon><Position /></el-icon>
            <template #title>学生分配</template>
          </el-menu-item>
          <el-menu-item index="/admin/student-info">
            <el-icon><User /></el-icon>
            <template #title>学生信息</template>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="building-management">
          <template #title>
            <el-icon><House /></el-icon>
            <span>楼栋管理</span>
          </template>
          <el-menu-item index="/admin/dorm-management">
            <el-icon><OfficeBuilding /></el-icon>
            <template #title>宿舍管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/announcement">
            <el-icon><Bell /></el-icon>
            <template #title>公告管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/application-review">
            <el-icon><Document /></el-icon>
            <template #title>申请审核</template>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/admin/visitor-management">
          <el-icon><Suitcase /></el-icon>
          <template #title>访客管理</template>
        </el-menu-item>
        
        <el-sub-menu index="messages">
          <template #title>
            <el-icon><Message /></el-icon>
            <span>消息中心</span>
          </template>
          <el-menu-item index="/admin/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>消息通知</template>
          </el-menu-item>
          <el-menu-item index="/admin/building-group-chat">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>楼栋动态</template>
          </el-menu-item>
          <el-menu-item index="/admin/chat-messages">
            <el-icon><ChatLineRound /></el-icon>
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
            <el-breadcrumb-item>管理员端</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <div class="notification-trigger" @click="showNotifications">
            <el-icon class="notification-icon"><Bell /></el-icon>
            <div v-if="notificationCount > 0" class="notification-count">
              {{ notificationCount > 99 ? '99+' : notificationCount }}
            </div>
          </div>
          
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="defaultAvatar" />
              <span class="username">{{ currentUser ? currentUser.realName : '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
  </div>
</template>

<script>
import { getCurrentUser, clearCurrentUser, setCurrentUser } from '@/utils/auth.js'
import { getTotalUnreadCount, setupDataChangeListener, setupStorageListener } from '@/utils/adminMessageData.js'
import { getUnreadCount } from '@/api/notification.js'
import { getCurrentUserInfo } from '@/api/auth.js'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'
import { 
  Fold, 
  Expand, 
  Bell, 
  ArrowDown,
  House,
  ChatDotRound,
  ChatLineRound,
  Message,
  UserFilled,
  User,
  Position,
  Document,
  OfficeBuilding,
  Suitcase
} from '@element-plus/icons-vue'

export default {
  name: 'AdminLayout',
  components: {
    Fold,
    Expand,
    Bell,
    ArrowDown,
    House,
    ChatDotRound,
    ChatLineRound,
    Message,
    UserFilled,
    User,
    Position,
    Document,
    OfficeBuilding,
    Suitcase
  },
  data() {
    return {
      isCollapse: false,
      notificationCount: 0,
      currentUser: null,  // 添加响应式用户数据
      defaultAvatar: defaultAvatar,  // 添加默认头像
      pageTitles: {
        '/admin/dorm-management': '宿舍管理',
        '/admin/student-allocation': '学生分配',
        '/admin/application-review': '申请审核',
        '/admin/announcement': '公告管理',
        '/admin/building-group-chat': '楼栋动态',
        '/admin/visitor-management': '访客管理',
        '/admin/student-info': '学生信息',
        '/admin/profile': '个人设置',
        '/admin/notifications': '消息通知',
        '/admin/chat-messages': '聊天消息'
      }
    }
  },
  mounted() {
    // 检查登录状态
    this.checkLoginStatus();
    // 加载当前用户信息
    this.loadCurrentUser();
    // 不再自动初始化默认测试数据，避免清空后又重新出现
    // initializeDefaultData();
    // 计算实际未读消息数量
    this.calculateUnreadCount();
    // 设置通知监听器
    this.setupNotificationListener();
    // 模拟实时更新通知数量
    this.startNotificationTimer();
    // 监听 localStorage 变化（跨标签页或页面刷新）
    window.addEventListener('storage', this.handleStorageChange);
    // 监听通知已读事件，实时更新未读计数
    this.notificationReadHandler = () => {
      this.calculateUnreadCount();
    };
    window.addEventListener('notificationRead', this.notificationReadHandler);
  },
  beforeUnmount() {
    if (this.notificationTimer) {
      clearInterval(this.notificationTimer);
    }
    window.removeEventListener('storage', this.handleStorageChange);
    // 移除通知已读事件监听器
    if (this.notificationReadHandler) {
      window.removeEventListener('notificationRead', this.notificationReadHandler);
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    },
    currentPageTitle() {
      // 优先使用路由 meta.title，否则使用 pageTitles 映射
      if (this.$route.meta && this.$route.meta.title) {
        return this.$route.meta.title
      }
      const path = this.$route.path
      return this.pageTitles[path] || '未知页面'
    },
  },
  methods: {
    checkLoginStatus() {
      // 检查是否已登录
      // 🔒 使用会话隔离的 token
      const token = localStorage.getItem('admin_token')
      const currentUser = getCurrentUser()
      
      if (!token || !currentUser) {
        // 未登录，重定向到登录页
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return false
      }
      
      // 检查用户类型是否正确
      if (currentUser.userType !== 'admin' && currentUser.userType !== 'superadmin') {
        // 用户类型不对，清除登录状态并跳转
        localStorage.removeItem('token')
        localStorage.removeItem('currentUser')
        this.$message.error('当前用户不是管理员，请使用管理员账号登录')
        this.$router.push('/login')
        return false
      }
      
      return true
    },
    async loadCurrentUser() {
      // 先检查是否有token
      const token = localStorage.getItem('admin_token')
      if (!token) {
        this.$router.push('/login')
        return
      }
      
      // 直接从后端获取用户信息
      try {
        const res = await getCurrentUserInfo()
        if (res.code === 200 && res.data) {
          this.currentUser = res.data
          // 同步更新本地存储（用于其他地方的快速访问）
          setCurrentUser(res.data)
        } else {
          // 获取失败，跳转到登录页
          this.$router.push('/login')
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 网络错误时，尝试使用本地缓存
        this.currentUser = getCurrentUser()
        if (!this.currentUser) {
          this.$router.push('/login')
        }
      }
    },
    handleStorageChange(event) {
      // 当 localStorage 变化时重新加载用户信息
      if (event.key === 'currentUser' || event.key === 'token') {
        if (!event.newValue) {
          // localStorage 被清空，跳转到登录页
          this.$router.push('/login')
        } else {
          this.loadCurrentUser()
        }
      }
    },
    async calculateUnreadCount() {
      try {
        // 优先从后端API获取未读数量
        const res = await getUnreadCount()
        if (res.code === 200) {
          this.notificationCount = res.data || 0
        } else {
          // 后端获取失败时，使用本地数据作为备用
          this.notificationCount = getTotalUnreadCount()
        }
      } catch (error) {
        console.error('获取未读通知数量失败:', error)
        // 出错时使用本地数据作为备用
        this.notificationCount = getTotalUnreadCount()
      }
    },
    // 监听数据变化，实时更新未读消息数量
    setupNotificationListener() {
      // 监听同页面内的数据变化
      setupDataChangeListener(() => {
        this.calculateUnreadCount();
      });
      // 监听跨标签页的数据变化
      setupStorageListener(() => {
        this.calculateUnreadCount();
      });
    },
    toggleCollapse() {
      this.isCollapse = !this.isCollapse
    },
    showNotifications() {
      this.$router.push('/admin/notifications')
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
          clearCurrentUser()
          this.$router.push('/login')
        })
      } else if (command === 'profile') {
        // 添加错误处理，防止路由跳转问题
        this.$router.push('/admin/profile').catch(err => {
          console.error('跳转到个人设置失败:', err);
          // 如果路由跳转失败，尝试强制刷新
          if (err.name !== 'NavigationDuplicated') {
            window.location.href = '/admin/profile';
          }
        });
      }
    }
  }
}
</script>

<style scoped>
/* 桌面端全屏布局 */
.admin-layout {
  width: 100vw;
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.sidebar {
  background: linear-gradient(180deg, #2d3250 0%, #1f2335 50%, #1a1d2e 100%);
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
  position: relative;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.08) 0%, transparent 100%);
  pointer-events: none;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2d3250 0%, #1f2844 100%);
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  position: relative;
  z-index: 1;
}

.sidebar-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.5), transparent);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  color: #667eea;
  filter: drop-shadow(0 2px 8px rgba(102, 126, 234, 0.4));
}

.logo-text {
  color: #e6e9f0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.8px;
  background: linear-gradient(135deg, #e6e9f0 0%, #c5d0e6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-menu {
  border: none;
  background: transparent;
  height: calc(100vh - 64px);
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item) {
  color: #c5d0e6;
  height: 50px;
  line-height: 50px;
  font-size: 14px;
  margin: 4px 12px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

:deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 0;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 4px 4px 0;
  transition: height 0.3s ease;
  box-shadow: 0 0 10px rgba(102, 126, 234, 0.5);
}

:deep(.el-menu-item:hover) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: #fff;
}

:deep(.el-menu-item:hover::before) {
  height: 60%;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.2) 100%);
  color: #fff;
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.2);
}

:deep(.el-menu-item.is-active::before) {
  height: 70%;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

/* 子菜单样式 */
:deep(.el-sub-menu) {
  color: #c5d0e6;
  margin: 4px 12px;
  border-radius: 8px;
}

:deep(.el-sub-menu__title) {
  color: #c5d0e6 !important;
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
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 4px 4px 0;
  transition: height 0.3s ease;
  box-shadow: 0 0 10px rgba(102, 126, 234, 0.5);
}

:deep(.el-sub-menu__title:hover) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.1) 100%) !important;
  color: #fff !important;
}

:deep(.el-sub-menu__title:hover::before) {
  height: 60%;
}

:deep(.el-sub-menu.is-opened .el-sub-menu__title) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.08) 100%) !important;
  color: #fff !important;
}

:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.2) 100%) !important;
  color: #fff !important;
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.2);
}

:deep(.el-sub-menu.is-active .el-sub-menu__title::before) {
  height: 70%;
}

:deep(.el-sub-menu__title .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

/* 子菜单展开区域 */
:deep(.el-menu--inline) {
  background: transparent !important;
  margin-top: 4px;
  margin-bottom: 4px;
  padding: 4px 0;
}

/* 子菜单项样式 */
:deep(.el-sub-menu .el-menu-item) {
  color: #b0bcd4;
  height: 42px;
  line-height: 42px;
  font-size: 13px;
  padding-left: 56px !important;
  background: rgba(255, 255, 255, 0.03);
  margin: 3px 12px;
  border-radius: 6px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-left: 3px solid transparent;
}

:deep(.el-sub-menu .el-menu-item::before) {
  display: none;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.18) 0%, rgba(118, 75, 162, 0.12) 100%) !important;
  color: #fff !important;
  transform: translateX(4px);
  border-left-color: rgba(102, 126, 234, 0.7);
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(102, 126, 234, 0.35) 0%, rgba(118, 75, 162, 0.25) 100%) !important;
  color: #fff !important;
  font-weight: 500;
  border-left-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
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
  background: #f5f6f8;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 21, 41, 0.05);
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
  background: #e4e7ed;
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 通知铃铛容器 */
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
.notification-trigger .notification-icon {
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

:deep(.el-badge__content.is-fixed) {
  top: 16px;
  right: 8px;
  transform: translate(50%, -50%);
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
  background: #e4e7ed;
}

.username {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.main-content {
  background: #f0f2f5;
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

/* 面包屑样式优化 */
:deep(.el-breadcrumb) {
  font-size: 14px;
}

:deep(.el-breadcrumb__item) {
  font-weight: 500;
  color: #8590a8;
}

:deep(.el-breadcrumb__item:last-child) {
  color: #409eff;
  font-weight: 600;
}

:deep(.el-breadcrumb__separator) {
  color: #d0d5e0;
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
  background: #ecf5ff;
  color: #409eff;
}

/* 全局组件样式优化 */
:deep(.el-card) {
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

:deep(.el-table) {
  background-color: transparent;
}

:deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #606266;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-table tr) {
  background-color: #fff;
}

:deep(.el-table tbody tr:hover > td) {
  background-color: #f5f7fa !important;
}

:deep(.el-table__body tr.current-row > td) {
  background-color: #ecf5ff !important;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-pagination) {
  padding: 16px 0;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next),
:deep(.el-pagination .el-pager li) {
  background: #fff;
  border: 1px solid #dcdfe6;
}

:deep(.el-pagination .el-pager li:hover),
:deep(.el-pagination .btn-prev:hover),
:deep(.el-pagination .btn-next:hover) {
  color: #409eff;
  background: #ecf5ff;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

:deep(.el-button--primary) {
  background: #409eff;
  border-color: #409eff;
}

:deep(.el-button--primary:hover) {
  background: #66b1ff;
  border-color: #66b1ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

:deep(.el-form-item__label) {
  color: #606266;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  background-color: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: #c0c4cc;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

:deep(.el-textarea__inner) {
  background-color: #fff;
  border: 1px solid #dcdfe6;
}

:deep(.el-textarea__inner:hover) {
  border-color: #c0c4cc;
}

:deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

:deep(.el-select__wrapper) {
  background-color: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: none;
}

:deep(.el-select__wrapper:hover) {
  border-color: #c0c4cc;
}

:deep(.el-select__wrapper.is-focused) {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

:deep(.el-dialog) {
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

:deep(.el-dialog__header) {
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
  padding: 20px;
}

:deep(.el-drawer) {
  background: #fff;
}

:deep(.el-drawer__header) {
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
  padding: 20px;
  margin-bottom: 0;
}

:deep(.el-descriptions) {
  background-color: transparent;
}

:deep(.el-descriptions__label) {
  background: #fafafa;
  color: #606266;
  font-weight: 500;
}

:deep(.el-descriptions__content) {
  background-color: #fff;
}
</style>
