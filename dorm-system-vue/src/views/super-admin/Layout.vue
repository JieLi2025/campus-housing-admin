<template>
  <div class="super-admin-layout">
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
      >
        <el-menu-item index="/super-admin/student-management">
          <el-icon><User /></el-icon>
          <template #title>学生管理</template>
        </el-menu-item>
        
        <el-menu-item index="/super-admin/admin-management">
          <el-icon><UserFilled /></el-icon>
          <template #title>管理员管理</template>
        </el-menu-item>
        
        <el-menu-item index="/super-admin/building-management">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>楼栋管理</template>
        </el-menu-item>
        
        <el-menu-item index="/super-admin/announcement">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>
        
        <el-menu-item index="/super-admin/config-management">
          <el-icon><Setting /></el-icon>
          <template #title>配置管理</template>
        </el-menu-item>
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
            <el-breadcrumb-item>超级管理员端</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleNotificationCommand" trigger="click" placement="bottom-end" class="notification-dropdown">
            <div class="notification-trigger">
              <el-icon class="notification-icon"><Bell /></el-icon>
              <div v-if="unreadCount > 0" class="notification-count">
                {{ unreadCount > 99 ? '99+' : unreadCount }}
              </div>
            </div>
            <template #dropdown>
                             <div class="notification-panel">
                 <div class="notification-header">
                   <span class="notification-title">通知管理</span>
                   <div class="notification-actions">
                     <el-button type="text" size="small" @click="goToAnnouncement">发布公告</el-button>
                     <el-button type="text" size="small" @click="handleMarkAllAsRead">全部已读</el-button>
                   </div>
                 </div>
                <div class="notification-list">
                 <div 
                   v-for="notification in notifications" 
                   :key="notification.id"
                   class="notification-item"
                   :class="{ unread: !notification.read }"
                   @click="handleMarkAsRead(notification.id)"
                 >
                   <div class="notification-icon">
                     <el-icon :color="notification.type === 'warning' ? '#e6a23c' : '#409eff'">
                       <component :is="notification.icon" />
                     </el-icon>
                   </div>
                                        <div class="notification-content">
                      <div class="notification-title">{{ notification.title }}</div>
                      <div class="notification-message">{{ notification.message }}</div>
                      <div class="notification-time">{{ notification.time }}</div>
                    </div>
                    <div class="notification-actions-item">
                      <el-button type="text" size="small" @click.stop="handleDeleteNotification(notification.id)" style="color: #f56c6c;">删除</el-button>
                    </div>
                 </div>
                 <div v-if="notifications.length === 0" class="no-notifications">
                   暂无通知
                 </div>
               </div>
              </div>
            </template>
          </el-dropdown>
          
          <el-dropdown @command="handleUserCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="defaultAvatar" />
              <span class="username">{{ currentUserName }}</span>
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

      <!-- 个人设置对话框 -->
      <el-dialog v-model="profileDialogVisible" title="个人设置" width="1000px">
         <el-tabs v-model="activeTab" type="border-card">
           <!-- 基本信息 -->
           <el-tab-pane label="基本信息" name="basic">
             <div class="profile-container">
               <div class="avatar-section">
                <el-upload
                  class="avatar-uploader"
                  action="#"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                >
                  <div class="avatar-wrapper">
                    <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" />
                    <div v-else class="avatar-placeholder">
                      <el-icon class="avatar-icon"><User /></el-icon>
                      <span class="avatar-text">点击上传头像</span>
                    </div>
                  </div>
                </el-upload>
              </div>
              <div class="form-section">
                <el-form :model="profileForm" label-width="100px" :rules="profileRules" ref="profileFormRef">
                  <el-form-item label="姓名" prop="realName">
                    <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
                  </el-form-item>
                  <el-form-item label="用户名" prop="username">
                    <el-input v-model="profileForm.username" disabled />
                  </el-form-item>
                  <el-form-item label="邮箱" prop="email">
                    <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                  </el-form-item>
                  <el-form-item label="手机号" prop="phone">
                    <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                  </el-form-item>
                </el-form>
              </div>
             </div>
           </el-tab-pane>

           <!-- 密码修改 -->
           <el-tab-pane label="密码修改" name="password">
             <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
               <el-form-item label="当前密码" prop="currentPassword">
                 <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" show-password />
               </el-form-item>
               <el-form-item label="新密码" prop="newPassword">
                 <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
               </el-form-item>
               <el-form-item label="确认密码" prop="confirmPassword">
                 <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
               </el-form-item>
             </el-form>
           </el-tab-pane>

           <!-- 操作日志 -->
           <el-tab-pane label="操作日志" name="operations">
             <div class="operations-header">
               <h3>操作记录</h3>
               <el-button type="primary" size="small" @click="exportOperations">导出记录</el-button>
             </div>
             <el-table :data="paginatedOperationLogs" style="width: 100%" :header-cell-style="{ background: '#f5f7fa' }">
               <el-table-column prop="time" label="操作时间" width="180" align="center" />
               <el-table-column prop="action" label="操作内容" min-width="200" align="center" />
               <el-table-column prop="module" label="操作模块" width="150" align="center" />
               <el-table-column prop="status" label="状态" width="100" align="center">
                 <template #default="scope">
                   <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'" size="small">
                     {{ scope.row.status === 'success' ? '成功' : '失败' }}
                   </el-tag>
                 </template>
               </el-table-column>
             </el-table>
             <div class="pagination-wrapper">
               <el-pagination
                 v-model:current-page="currentPage"
                 v-model:page-size="pageSize"
                 :page-sizes="[10, 20, 50, 100]"
                 :total="totalOperations"
                 layout="total, sizes, prev, pager, next, jumper"
                 @size-change="handleSizeChange"
                 @current-change="handleCurrentChange"
               />
             </div>
           </el-tab-pane>
         </el-tabs>
         <template #footer>
           <span class="dialog-footer">
             <el-button @click="profileDialogVisible = false">取消</el-button>
             <el-button type="primary" @click="saveProfile">保存设置</el-button>
           </span>
         </template>
       </el-dialog>
  </div>
</template>

<script>
import { getSystemNotifications, getUnreadCount, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'
import { getOperationLogList, logOperation as logOperationAPI } from '@/api/operationLog'
import { getProfile, updateProfile, updatePassword } from '@/api/profile'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'

export default {
  name: 'SuperAdminLayout',
  provide() {
    return {
      logOperation: this.logOperation
    }
  },
  data() {
    return {
      isCollapse: false,
      defaultAvatar: defaultAvatar,  // 添加默认头像
      currentUserName: '超级管理员',  // 右上角显示的用户名
      pageTitles: {
        '/super-admin/student-management': '学生管理',
        '/super-admin/admin-management': '管理员管理',
        '/super-admin/building-management': '楼栋管理',
        '/super-admin/config-management': '配置管理',
        '/super-admin/announcement': '公告管理'
      },
      notifications: [],
      unreadCount: 0,
      profileDialogVisible: false,
      activeTab: 'basic',
      profileForm: {
        avatar: '',
        realName: '',
        username: '',
        email: '',
        phone: ''
      },
      profileRules: {
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
        phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: this.validateConfirmPassword, trigger: 'blur' }]
      },
      // 操作日志相关数据
      operationLogs: [],
      currentPage: 1,
      pageSize: 10,
      totalOperations: 0
    }
  },
  mounted() {
    // 检查是否已登录
    // 🔒 使用会话隔离的 token
    const token = localStorage.getItem('superadmin_token')
    if (token) {
      // 只在已登录时加载数据
      this.loadNotifications()
      this.loadUnreadCount()
      this.loadProfile()  // 加载个人信息，更新右上角用户名
    } else {
      console.warn('未登录，跳过加载通知数据')
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    },
    currentPageTitle() {
      return this.pageTitles[this.$route.path] || '未知页面'
    },
    paginatedOperationLogs() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.operationLogs.slice(start, end);
    }
  },
  methods: {
    // ==================== 数据加载方法 ====================
    async loadNotifications() {
      try {
        const res = await getSystemNotifications(1, 100)
        if (res.code === 200) {
          this.notifications = res.data.records || []
        }
      } catch (error) {
        console.error('加载通知失败:', error)
      }
    },
    async loadUnreadCount() {
      try {
        const res = await getUnreadCount()
        if (res.code === 200) {
          this.unreadCount = res.data
        }
      } catch (error) {
        console.error('加载未读数量失败:', error)
      }
    },
    async loadOperationLogs() {
      try {
        const res = await getOperationLogList(this.currentPage, this.pageSize)
        if (res.code === 200) {
          this.operationLogs = res.data.records.map(log => ({
            id: log.id,
            time: log.time,
            action: log.action,
            module: log.module,
            ip: log.ip,
            status: log.status
          }))
          this.totalOperations = res.data.total
        }
      } catch (error) {
        console.error('加载操作日志失败:', error)
      }
    },
    async loadProfile() {
      try {
        const res = await getProfile()
        if (res.code === 200) {
          this.profileForm = {
            avatar: res.data.avatar || '',
            realName: res.data.realName || '',
            username: res.data.username || '',
            email: res.data.email || '',
            phone: res.data.phone || ''
          }
          // 更新右上角显示的用户名
          this.currentUserName = res.data.realName || res.data.username || '超级管理员'
        }
      } catch (error) {
        console.error('加载个人信息失败:', error)
      }
    },
    
    // ==================== 通用方法 ====================
    toggleCollapse() {
      this.isCollapse = !this.isCollapse
    },
    showNotifications() {
      // 通知按钮点击事件，下拉面板会自动显示
      this.loadNotifications()
    },
    handleUserCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.logOperation('退出登录', '系统');
          // 🔒 清除当前角色的登录信息（会话隔离）
          // 动态导入避免循环依赖
          import('@/utils/auth.js').then(({ clearCurrentUser }) => {
            clearCurrentUser()
            this.$router.push('/login')
          })
        })
      } else if (command === 'profile') {
        this.loadProfile()
        this.loadOperationLogs()
        this.profileDialogVisible = true;
        this.activeTab = 'basic';
      }
    },
    handleNotificationCommand(command) {
      if (command === 'markAllAsRead') {
        this.handleMarkAllAsRead()
      }
    },
    
    // ==================== 通知相关方法 ====================
    async handleMarkAsRead(id) {
      try {
        await markAsRead(id)
        const notification = this.notifications.find(n => n.id === id);
        if (notification) {
          notification.read = true;
          this.$message.success('通知已标记为已读');
          this.logOperation(`标记通知为已读：${notification.title}`, '通知管理');
          this.loadUnreadCount()
        }
      } catch (error) {
        this.$message.error('操作失败：' + (error.message || '未知错误'))
      }
    },
    async handleMarkAllAsRead() {
      try {
        await markAllAsRead()
        this.notifications.forEach(n => n.read = true);
        this.$message.success('所有通知已标记为已读');
        this.logOperation('全部标记通知为已读', '通知管理');
        this.loadUnreadCount()
      } catch (error) {
        this.$message.error('操作失败：' + (error.message || '未知错误'))
      }
    },
    goToAnnouncement() {
      this.$router.push('/super-admin/announcement');
    },

    async handleDeleteNotification(id) {
      this.$confirm('确定要删除这条通知吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const notification = this.notifications.find(n => n.id === id);
          await deleteNotification(id)
          this.$message.success('通知删除成功');
          this.logOperation(`删除通知：${notification.title}`, '通知管理');
          this.loadNotifications()
          this.loadUnreadCount()
        } catch (error) {
          this.$message.error('删除失败：' + (error.message || '未知错误'))
        }
      });
    },
    getNotificationIcon(type) {
      const iconMap = {
        'info': 'InfoFilled',
        'warning': 'Warning',
        'success': 'Check',
        'error': 'CircleClose'
      };
      return iconMap[type] || 'InfoFilled';
    },
    
    // ==================== 个人设置相关方法 ====================
    beforeAvatarUpload(rawFile) {
      const isJPG = rawFile.type === 'image/jpeg';
      const isPNG = rawFile.type === 'image/png';
      const isLt2M = rawFile.size / 1024 / 1024 < 2;

      if (!isJPG && !isPNG) {
        this.$message.error('图片只能是 JPG/PNG 格式!');
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过 2MB!');
      }
      return (isJPG || isPNG) && isLt2M;
    },
    validateConfirmPassword(rule, value, callback) {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致!'));
      } else {
        callback();
      }
    },
    async saveProfile() {
      // 根据当前标签页智能保存
      if (this.activeTab === 'basic') {
        // 基本信息标签页：只保存基本信息
        await this.$refs.profileFormRef.validate(async (valid) => {
          if (valid) {
            try {
              await updateProfile({
                realName: this.profileForm.realName,
                email: this.profileForm.email,
                phone: this.profileForm.phone
              })
              this.$notify({
                title: '保存成功',
                message: '个人信息已更新',
                type: 'success',
                duration: 2000
              });
              this.logOperation('保存基本信息', '个人设置');
              this.loadProfile();
            } catch (error) {
              this.$notify({
                title: '保存失败',
                message: error.message || '未知错误',
                type: 'error',
                duration: 3000
              });
            }
          } else {
            this.$notify({
              title: '验证失败',
              message: '请检查表单信息',
              type: 'warning',
              duration: 2000
            });
          }
        });
      } else if (this.activeTab === 'password') {
        // 密码修改标签页：只修改密码
        if (!this.passwordForm.newPassword) {
          this.$notify({
            title: '提示',
            message: '请输入新密码',
            type: 'warning',
            duration: 2000
          });
          return;
        }
        
        await this.$refs.passwordFormRef.validate(async (valid) => {
          if (valid) {
            try {
              await updatePassword({
                currentPassword: this.passwordForm.currentPassword,
                newPassword: this.passwordForm.newPassword,
                confirmPassword: this.passwordForm.confirmPassword
              })
              this.$notify({
                title: '修改成功',
                message: '密码已更新，请妥善保管',
                type: 'success',
                duration: 3000
              });
              this.logOperation('修改密码', '个人设置');
              // 清空密码表单
              this.passwordForm = {
                currentPassword: '',
                newPassword: '',
                confirmPassword: ''
              };
            } catch (error) {
              this.$notify({
                title: '修改失败',
                message: error.message || '未知错误',
                type: 'error',
                duration: 3000
              });
            }
          } else {
            this.$notify({
              title: '验证失败',
              message: '请检查密码要求',
              type: 'warning',
              duration: 2000
            });
          }
        });
      } else if (this.activeTab === 'operations') {
        // 操作记录标签页：不需要保存
        this.$notify({
          title: '提示',
          message: '操作记录无需保存',
          type: 'info',
          duration: 2000
        });
      }
    },
    
    // ==================== 操作日志相关方法 ====================
    exportOperations() {
      // 创建导出对话框
      this.$confirm('请选择导出格式和位置', '导出操作记录', {
        confirmButtonText: '导出Excel',
        cancelButtonText: '导出CSV',
        distinguishCancelAndClose: true,
        type: 'info',
        showCancelButton: true,
        showClose: true,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        beforeClose: (action, instance, done) => {
          if (action === 'confirm') {
            this.exportToExcel();
            done();
          } else if (action === 'cancel') {
            this.exportToCSV();
            done();
          } else {
            done();
          }
        }
      }).then(() => {
        // 导出完成后不关闭个人设置对话框
      }).catch(() => {
        // 用户取消导出，不关闭对话框
      });
    },
    exportToExcel() {
      // 生成Excel文件内容
      const headers = ['操作时间', '操作内容', '操作模块', '状态'];
      const data = this.operationLogs.map(log => [
        `"${log.time}"`,
        `"${log.action}"`,
        `"${log.module}"`,
        `"${log.status === 'success' ? '成功' : '失败'}"`
      ]);
      
      // 创建CSV内容（Excel可以打开CSV）
      const csvContent = [headers.join(','), ...data.map(row => row.join(','))].join('\n');
      
      // 创建Blob对象
      const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
      
      // 创建下载链接
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `操作记录_${new Date().toISOString().slice(0, 10)}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      this.$message.success('Excel文件导出成功！');
      this.logOperation('导出操作记录(Excel)', '个人设置');
    },
    exportToCSV() {
      // 生成CSV文件内容
      const headers = ['操作时间', '操作内容', '操作模块', '状态'];
      const data = this.operationLogs.map(log => [
        `"${log.time}"`,
        `"${log.action}"`,
        `"${log.module}"`,
        `"${log.status === 'success' ? '成功' : '失败'}"`
      ]);
      
      // 创建CSV内容
      const csvContent = [headers.join(','), ...data.map(row => row.join(','))].join('\n');
      
      // 创建Blob对象
      const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
      
      // 创建下载链接
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `操作记录_${new Date().toISOString().slice(0, 10)}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      this.$message.success('CSV文件导出成功！');
      this.logOperation('导出操作记录(CSV)', '个人设置');
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.loadOperationLogs();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.loadOperationLogs();
    },
    
    // 记录操作日志的方法（供其他方法调用）
    async logOperation(action, module, status = 'success') {
      console.log('Layout.logOperation received:', { action, module, status });
      
      try {
        await logOperationAPI({
          operationType: action,        // 后端字段名: operationType
          operationDesc: module,        // 后端字段名: operationDesc
          status: status === 'success' ? 1 : 0  // 后端使用数字: 1-成功, 0-失败
        })
        console.log('Layout: 操作记录已保存到后端');
        
        // 如果当前在个人设置的操作日志标签页，刷新日志列表
        if (this.profileDialogVisible && this.activeTab === 'operations') {
          this.loadOperationLogs();
        }
      } catch (error) {
        console.error('记录操作日志失败:', error)
      }
    }
  }
}
</script>

<style scoped>
/* 桌面端全屏布局 */
.super-admin-layout {
  width: 100vw;
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.sidebar {
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.3);
  position: relative;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200px;
  background: linear-gradient(180deg, rgba(6, 182, 212, 0.06) 0%, transparent 100%);
  pointer-events: none;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2937 0%, #1a2332 100%);
  border-bottom: 1px solid rgba(6, 182, 212, 0.15);
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
  background: linear-gradient(90deg, transparent, rgba(6, 182, 212, 0.4), transparent);
}

 .logo-container {
   display: flex;
   align-items: center;
   gap: 12px;
}

.logo-icon {
  font-size: 28px;
  color: #06b6d4;
  filter: drop-shadow(0 2px 8px rgba(6, 182, 212, 0.5));
}

 .logo-text {
   color: #f0f9ff;
   font-size: 18px;
   font-weight: 600;
   letter-spacing: 0.5px;
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
  color: #cbd5e1;
  height: 56px;
  line-height: 56px;
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
  background: linear-gradient(180deg, #06b6d4 0%, #0891b2 100%);
  border-radius: 0 4px 4px 0;
  transition: height 0.3s ease;
  box-shadow: 0 0 12px rgba(6, 182, 212, 0.6);
}

:deep(.el-menu-item:hover) {
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.12) 0%, rgba(8, 145, 178, 0.08) 100%);
  color: #fff;
}

:deep(.el-menu-item:hover::before) {
  height: 60%;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(6, 182, 212, 0.25) 0%, rgba(8, 145, 178, 0.15) 100%);
  color: #fff;
  font-weight: 500;
  box-shadow: 0 2px 12px rgba(6, 182, 212, 0.15);
}

:deep(.el-menu-item.is-active::before) {
  height: 70%;
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 12px;
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
  background: #ecfeff;
  color: #06b6d4;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 通知下拉菜单 */
.notification-dropdown {
  margin-right: 8px;
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
  background: #ecfeff;
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
  color: #06b6d4;
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
  background: #ecfeff;
  color: #06b6d4;
}

/* 全局组件样式优化 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
  border-color: transparent;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #22d3ee 0%, #06b6d4 100%);
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.4);
}

:deep(.el-tag--primary) {
  background-color: rgba(6, 182, 212, 0.1);
  border-color: rgba(6, 182, 212, 0.2);
  color: #0e7490;
}

/* 通知面板样式 */
.notification-panel {
  width: 400px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

 .notification-header {
   display: flex;
   justify-content: space-between;
   align-items: center;
   padding: 16px 20px;
   border-bottom: 1px solid #ebeef5;
   background: #f5f7fa;
 }

 .notification-header .notification-title {
   font-size: 16px;
   font-weight: 600;
   color: #303133;
 }

 .notification-actions {
   display: flex;
   gap: 8px;
 }

.notification-list {
  max-height: 400px;
  overflow-y: auto;
  padding: 0;
}

 .notification-item {
   display: flex;
   align-items: flex-start;
   padding: 16px 20px;
   cursor: pointer;
   border-bottom: 1px solid #f0f0f0;
   transition: background-color 0.3s;
   position: relative;
 }

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.unread {
  background-color: #ecf5ff;
  border-left: 4px solid #409eff;
}

.notification-item .notification-icon {
  margin-right: 16px;
  font-size: 20px;
  margin-top: 2px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-content .notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.4;
}

.notification-message {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 6px;
  word-wrap: break-word;
  white-space: normal;
}

 .notification-time {
   font-size: 12px;
   color: #909399;
 }

 .notification-actions-item {
   display: flex;
   gap: 8px;
   margin-left: 12px;
   flex-shrink: 0;
 }

 .notification-actions-item .el-button {
   padding: 4px 8px;
   font-size: 12px;
 }

 .no-notifications {
   text-align: center;
   padding: 40px 20px;
   color: #c0c4cc;
   font-size: 14px;
 }

   /* 个人设置对话框样式 */
  .profile-container {
    display: flex;
    gap: 30px;
    align-items: flex-start;
  }

  .avatar-section {
    flex-shrink: 0;
  }

  .form-section {
    flex: 1;
  }

  .avatar-uploader .el-upload {
    border: none;
    border-radius: 0;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: all 0.3s;
  }

  .avatar-wrapper {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    overflow: hidden;
    border: 3px solid #e4e7ed;
    transition: all 0.3s;
    position: relative;
  }

  .avatar-wrapper:hover {
    border-color: #409eff;
    transform: scale(1.05);
  }

  .avatar {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #909399;
  }

  .avatar-icon {
    font-size: 32px;
    margin-bottom: 8px;
  }

  .avatar-text {
    font-size: 12px;
    text-align: center;
  }

  /* 操作日志样式 */
  .operations-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .operations-header h3 {
    margin: 0;
    color: #303133;
    font-size: 16px;
    font-weight: 600;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}
</style>
