<template>
  <div class="notifications-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>消息通知</h2>
          <div class="header-buttons">
            <el-button type="primary" @click="handleMarkAllAsRead">全部标记为已读</el-button>
            <el-button @click="refreshNotifications" type="info">刷新</el-button>
          </div>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <el-tab-pane label="全部通知" name="all">
          <div class="notification-list" v-loading="loading">
            <div v-if="notifications.length === 0" class="empty-state">
              <el-empty description="暂无通知" />
            </div>
            <div v-for="item in notifications" :key="item.id" class="notification-item" :class="{ unread: !item.read }" @click="handleNotificationClick(item)">
              <div class="notification-icon">
                <el-icon :class="getIconClass(item.type)">
                  <Setting v-if="item.type === 'system'" />
                  <Document v-else-if="item.type === 'application'" />
                  <User v-else />
                </el-icon>
              </div>
              <div class="notification-content">
                <div class="notification-title">{{ item.title }}</div>
                <div class="notification-message">{{ item.message }}</div>
                <div class="notification-meta">
                  <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
                </div>
              </div>
              <div class="notification-actions" v-if="item.type !== 'system'">
                <el-button type="text" size="small" @click.stop="handleDeleteNotification(item.id)">删除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="系统通知" name="system">
          <div class="notification-list" v-loading="loading">
            <div v-if="notifications.length === 0" class="empty-state">
              <el-empty description="暂无系统通知" />
            </div>
            <div v-for="item in notifications" :key="item.id" class="notification-item" :class="{ unread: !item.read }" @click="handleNotificationClick(item)">
              <div class="notification-icon"><el-icon class="icon-system"><Setting /></el-icon></div>
              <div class="notification-content">
                <div class="notification-title">{{ item.title }}</div>
                <div class="notification-message">{{ item.message }}</div>
                <div class="notification-meta"><span class="notification-time">{{ formatTime(item.createdAt) }}</span></div>
              </div>
              <!-- 系统通知不显示删除按钮，只有超级管理员可以删除 -->
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="申请通知" name="application">
          <div class="notification-list" v-loading="loading">
            <div v-if="notifications.length === 0" class="empty-state"><el-empty description="暂无申请通知" /></div>
            <div v-for="item in notifications" :key="item.id" class="notification-item" :class="{ unread: !item.read }" @click="handleNotificationClick(item)">
              <div class="notification-icon"><el-icon class="icon-application"><Document /></el-icon></div>
              <div class="notification-content">
                <div class="notification-title">{{ item.title }}</div>
                <div class="notification-message">{{ item.message }}</div>
                <div class="notification-meta">
                  <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
                </div>
              </div>
              <div class="notification-actions"><el-button type="text" size="small" @click.stop="handleDeleteNotification(item.id)">删除</el-button></div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="访客通知" name="visitor">
          <div class="notification-list" v-loading="loading">
            <div v-if="notifications.length === 0" class="empty-state"><el-empty description="暂无访客通知" /></div>
            <div v-for="item in notifications" :key="item.id" class="notification-item" :class="{ unread: !item.read }" @click="handleNotificationClick(item)">
              <div class="notification-icon"><el-icon class="icon-visitor"><User /></el-icon></div>
              <div class="notification-content">
                <div class="notification-title">{{ item.title }}</div>
                <div class="notification-message">{{ item.message }}</div>
                <div class="notification-meta">
                  <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
                </div>
              </div>
              <div class="notification-actions"><el-button type="text" size="small" @click.stop="handleDeleteNotification(item.id)">删除</el-button></div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <div class="pagination-container" v-if="total > pageSize">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="handlePageChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Document, User } from '@element-plus/icons-vue'
import { getAdminNotifications, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'

const router = useRouter()
const activeTab = ref('all')
const notifications = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const getIconClass = (type) => {
  const classMap = { system: 'icon-system', application: 'icon-application', visitor: 'icon-visitor' }
  return classMap[type] || 'icon-system'
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString()
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const type = activeTab.value === 'all' ? null : activeTab.value
    const res = await getAdminNotifications({ type, page: currentPage.value, size: pageSize.value })
    if (res.code === 200) {
      notifications.value = res.data.records || res.data || []
      total.value = res.data.total || notifications.value.length
    }
  } catch (error) {
    console.error('加载通知失败:', error)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => { currentPage.value = 1; loadNotifications() }
const handlePageChange = (page) => { currentPage.value = page; loadNotifications() }
const refreshNotifications = () => { loadNotifications() }

const handleNotificationClick = async (notification) => {
  if (!notification.read) {
    try {
      await markAsRead(notification.id)
      notification.read = true
      // 通知父组件更新未读计数
      window.dispatchEvent(new CustomEvent('notificationRead'))
      ElMessage.success('已标记为已读')
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  // 根据通知类型跳转到对应页面
  switch (notification.type) {
    case 'application':
      router.push('/admin/application-review')
      break
    case 'visitor':
      router.push('/admin/visitor-management')
      break
    // 其他类型的通知只标记已读，不弹窗
  }
}

const handleMarkAllAsRead = async () => {
  try {
    await markAllAsRead()
    notifications.value.forEach(n => n.read = true)
    // 通知父组件更新未读计数
    window.dispatchEvent(new CustomEvent('notificationRead'))
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDeleteNotification = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    ElMessage.success('删除成功')
  } catch (error) { if (error !== 'cancel') { console.error('删除失败:', error); ElMessage.error('删除失败') } }
}

onMounted(() => { loadNotifications() })
</script>

<style scoped>
.notifications-page { padding: 20px; }
.header-actions { display: flex; justify-content: space-between; align-items: center; }
.header-actions h2 { margin: 0; }
.header-buttons { display: flex; gap: 10px; }
.notification-list { min-height: 200px; }
.notification-item { display: flex; align-items: flex-start; padding: 15px; border-bottom: 1px solid #ebeef5; cursor: pointer; transition: background-color 0.3s; }
.notification-item:hover { background-color: #f5f7fa; }
.notification-item.unread { background-color: #ecf5ff; }
.notification-icon { margin-right: 15px; font-size: 24px; }
.icon-system { color: #909399; }
.icon-application { color: #409eff; }
.icon-visitor { color: #67c23a; }
.notification-content { flex: 1; }
.notification-title { font-weight: bold; margin-bottom: 5px; color: #303133; }
.notification-message { color: #606266; font-size: 14px; margin-bottom: 5px; }
.notification-meta { display: flex; gap: 15px; font-size: 12px; color: #909399; }
.notification-actions { margin-left: 10px; }
.empty-state { padding: 40px 0; }
.pagination-container { margin-top: 20px; display: flex; justify-content: center; }
</style>
