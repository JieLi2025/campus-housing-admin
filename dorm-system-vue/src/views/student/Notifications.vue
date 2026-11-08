<template>
  <div class="notifications-page">
    <div class="page-header">
      <h2>公告</h2>
      <p class="page-description">查看重要公告</p>
    </div>

    <div class="notifications-container">
      <!-- 通知筛选 -->
      <div class="filter-section">
        <el-radio-group v-model="filterType" @change="filterNotifications">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="announcement">公告</el-radio-button>
          <el-radio-button label="unread">未读</el-radio-button>
        </el-radio-group>
        
        <el-button type="primary" @click="markAllAsRead" :disabled="!hasUnread">
          全部标记为已读
        </el-button>
      </div>

      <!-- 通知列表 -->
      <div class="notifications-list">
        <el-card 
          v-for="notification in filteredNotifications" 
          :key="notification.id"
          class="notification-card"
          :class="{ 'unread': !notification.isRead }"
          @click="markAsRead(notification.id)"
        >
          <div class="notification-header">
            <div class="notification-type">
              <el-icon class="announcement-icon"><Bell /></el-icon>
              <span class="type-text">公告</span>
            </div>
            <div class="notification-time">
              {{ formatTime(notification.createTime) }}
            </div>
          </div>
          
          <div class="notification-content">
            <h4 class="notification-title">{{ notification.title }}</h4>
            <p class="notification-message">{{ notification.message }}</p>
          </div>
          
          <div class="notification-footer" v-if="notification.actionUrl || notification.chatId">
            <el-button type="primary" size="small" @click.stop="handleAction(notification)">
              {{ notification.chatId ? '查看消息' : '查看详情' }}
            </el-button>
          </div>
        </el-card>
        
        <el-empty v-if="filteredNotifications.length === 0" description="暂无通知" />
      </div>
    </div>
  </div>
</template>

<script>
import { getNotificationList, markAsRead as markNotificationAsRead, markAllAsRead as markAllNotificationsAsRead } from '@/api/notification.js'
import { getStudentAnnouncementList } from '@/api/announcement.js'
import { getAssignedArea } from '@/api/studentAllocation.js'

export default {
  name: 'StudentNotifications',
  data() {
    return {
      filterType: 'all',
      notifications: [],
      loading: false,
      studentBuildingId: null // 学生所在楼栋ID
    }
  },
  mounted() {
    this.loadStudentBuildingAndNotifications()
  },
  computed: {
    filteredNotifications() {
      switch (this.filterType) {
        case 'announcement':
          return this.notifications.filter(n => n.type === 'announcement')
        case 'unread':
          return this.notifications.filter(n => !n.isRead)
        default:
          return this.notifications
      }
    },
    hasUnread() {
      return this.notifications.some(n => !n.isRead)
    }
  },
  methods: {
    // 先获取学生楼栋信息，再加载公告
    async loadStudentBuildingAndNotifications() {
      try {
        // 获取学生分配区域信息（包含楼栋ID）
        const areaRes = await getAssignedArea()
        console.log('获取学生分配区域信息:', areaRes)
        if (areaRes.code === 200 && areaRes.data) {
          this.studentBuildingId = areaRes.data.buildingId
          console.log('学生楼栋ID:', this.studentBuildingId)
        }
      } catch (e) {
        console.error('获取学生楼栋信息失败:', e)
      }
      // 无论是否获取到楼栋ID，都加载公告
      this.loadNotifications()
    },
    
    async loadNotifications() {
      try {
        this.loading = true
        const allAnnouncements = []
        
        // 1. 从通知API获取数据
        try {
          const notificationRes = await getNotificationList({ currentPage: 1, pageSize: 50 })
          const notifications = (notificationRes.data?.records || []).map(n => ({
            id: n.id,
            type: 'announcement',
            title: n.title,
            message: n.message || n.content,
            createTime: n.createdTime || n.createTime || n.time,
            isRead: n.read || n.isRead || false,
            source: 'notification'
          }))
          allAnnouncements.push(...notifications)
        } catch (e) {
          console.error('加载通知失败:', e)
        }
        
        // 2. 从公告API获取数据（学生端公告接口，传递楼栋ID只看本楼栋公告）
        try {
          const params = { currentPage: 1, pageSize: 50 }
          // 如果有楼栋ID，只获取本楼栋相关的公告
          if (this.studentBuildingId) {
            params.buildingId = this.studentBuildingId
          }
          console.log('请求公告列表参数:', params)
          const announcementRes = await getStudentAnnouncementList(params)
          console.log('公告列表响应:', announcementRes)
          if (announcementRes.code === 200) {
            // 获取本地存储的已读公告ID列表
            const readAnnouncementIds = JSON.parse(localStorage.getItem('read_announcement_ids') || '[]')
            
            const announcements = (announcementRes.data?.records || announcementRes.data || [])
              .map(a => ({
                id: `announcement_${a.id}`,
                originalId: a.id,
                type: 'announcement',
                title: a.title,
                message: a.content,
                createTime: a.publishTime || a.createTime,
                isRead: readAnnouncementIds.includes(a.id), // 从本地存储判断是否已读
                source: 'announcement'
              }))
            allAnnouncements.push(...announcements)
          }
        } catch (e) {
          console.error('加载公告失败:', e)
        }
        
        // 按时间排序（最新的在前）
        allAnnouncements.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
        
        this.notifications = allAnnouncements
      } catch (error) {
        console.error('加载公告失败:', error)
      } finally {
        this.loading = false
      }
    },
    getTypeText(type) {
      return '公告'
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
      } else {
        return date.toLocaleDateString()
      }
    },
    async markAsRead(id) {
      const notification = this.notifications.find(n => n.id === id)
      if (notification && !notification.isRead) {
        try {
          // 根据数据来源选择不同的标记已读方式
          if (notification.source === 'announcement') {
            // 公告来源：保存到localStorage持久化已读状态
            const readAnnouncementIds = JSON.parse(localStorage.getItem('read_announcement_ids') || '[]')
            if (!readAnnouncementIds.includes(notification.originalId)) {
              readAnnouncementIds.push(notification.originalId)
              localStorage.setItem('read_announcement_ids', JSON.stringify(readAnnouncementIds))
            }
            notification.isRead = true
            this.$message.success('已标记为已读')
            this.notifyParentToUpdateCount()
          } else {
            // 通知来源：调用通知API标记已读
            const res = await markNotificationAsRead(id)
            if (res.code === 200) {
              notification.isRead = true
              this.$message.success('已标记为已读')
              this.notifyParentToUpdateCount()
            }
          }
        } catch (error) {
          console.error('标记已读失败:', error)
          this.$message.error('标记已读失败')
        }
      }
    },
    async markAllAsRead() {
      const unreadCount = this.notifications.filter(n => !n.isRead).length
      if (unreadCount === 0) {
        this.$message.info('没有未读通知')
        return
      }
      
      try {
        // 1. 标记所有系统通知为已读
        const systemNotifications = this.notifications.filter(n => n.source === 'notification' && !n.isRead)
        if (systemNotifications.length > 0) {
          const res = await markAllNotificationsAsRead()
          if (res.code === 200) {
            systemNotifications.forEach(n => n.isRead = true)
          }
        }
        
        // 2. 标记所有公告为已读（保存到localStorage）
        const unreadAnnouncements = this.notifications.filter(n => n.source === 'announcement' && !n.isRead)
        if (unreadAnnouncements.length > 0) {
          const readAnnouncementIds = JSON.parse(localStorage.getItem('read_announcement_ids') || '[]')
          unreadAnnouncements.forEach(a => {
            if (!readAnnouncementIds.includes(a.originalId)) {
              readAnnouncementIds.push(a.originalId)
            }
            a.isRead = true
          })
          localStorage.setItem('read_announcement_ids', JSON.stringify(readAnnouncementIds))
        }
        
        this.$message.success(`已将 ${unreadCount} 条通知标记为已读`)
        // 触发父组件更新未读计数
        this.notifyParentToUpdateCount()
      } catch (error) {
        console.error('批量标记已读失败:', error)
        this.$message.error('标记已读失败')
      }
    },
    filterNotifications() {
      // 筛选逻辑已在computed中处理
    },
    // 通知父组件更新未读计数
    notifyParentToUpdateCount() {
      // 触发自定义事件，让Layout组件重新计算未读数
      window.dispatchEvent(new CustomEvent('notificationRead'))
    },
    handleAction(notification) {
      if (notification.chatId) {
        // 使用sessionStorage存储跳转参数，提高安全性
        sessionStorage.setItem('chatRedirect', JSON.stringify({
          chatId: notification.chatId,
          from: 'notifications',
          timestamp: Date.now() // 添加时间戳，防止过期数据
        }));
        
        // 跳转到聊天页面，不在URL中暴露敏感信息
        this.$router.push({
          path: '/student/chat-messages',
          query: { redirect: 'true' }
        });
      } else if (notification.actionUrl) {
        this.$router.push(notification.actionUrl)
      }
    }
  }
}
</script>

<style scoped>
.notifications-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
  text-align: center;
}

.page-header h2 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.page-description {
  color: #909399;
  font-size: 14px;
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
}

.notification-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.notification-card.unread {
  border-left: 4px solid #409eff;
  background: #f8f9ff;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notification-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.announcement-icon {
  color: #e6a23c;
  font-size: 16px;
}

.type-text {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.notification-time {
  font-size: 12px;
  color: #c0c4cc;
}

.notification-content {
  margin-bottom: 16px;
}

.notification-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 8px;
  font-weight: 600;
}

.notification-message {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.notification-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-section {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .notification-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
