<template>
  <div class="announcement-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>公告管理</h2>
          <el-button type="primary" @click="showPublishDialog">发布公告</el-button>
        </div>
      </template>
      
      <!-- 公告列表 -->
      <el-table :data="announcements" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="150" align="center" header-align="center" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip min-width="200" align="center" header-align="center" />
        <el-table-column prop="building" label="目标楼栋" width="120" align="center" header-align="center" />
        <el-table-column prop="publishTime" label="发布时间" width="120" align="center" header-align="center" />
        <el-table-column prop="expireDate" label="有效期" width="120" align="center" header-align="center">
          <template #default="scope">
            {{ formatExpireDate(scope.row.expireDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" header-align="center">
          <template #default="scope">
            <el-button size="small" @click="viewAnnouncement(scope.row)">查看</el-button>
            <el-button size="small" @click="editAnnouncement(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteAnnouncement(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发布公告抽屉 -->
    <el-drawer v-model="publishDialogVisible" title="发布公告" size="600px" direction="rtl" :close-on-click-modal="false">
      <el-form :model="announcementForm" label-width="100px" style="padding-right: 20px;">
        <el-form-item label="目标楼栋" required>
          <el-select v-model="announcementForm.buildingId" placeholder="请选择楼栋" style="width: 100%" :loading="loadingBuildings">
            <el-option label="全部楼栋" value="" />
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="building.buildingName" 
              :value="building.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="公告标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入标题" />
        </el-form-item>

        <el-form-item label="公告内容" required>
          <el-input v-model="announcementForm.content" type="textarea" :rows="6" placeholder="请输入内容" />
        </el-form-item>

        <el-form-item label="有效期限">
          <el-date-picker v-model="announcementForm.expireDate" type="datetime" placeholder="选择过期时间" style="width: 100%" />
        </el-form-item>

        <el-form-item>
          <el-button @click="closePublishDialog">取消</el-button>
          <el-button type="primary" @click="publishAnnouncement">发布公告</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>

    <!-- 查看公告详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="公告详情" width="700px" :close-on-click-modal="false">
      <el-descriptions :column="1" border v-if="viewForm.id">
        <el-descriptions-item label="目标楼栋">
          <el-tag type="primary">{{ viewForm.building || '全部楼栋' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="公告标题">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="公告内容">
          <div style="white-space: pre-wrap; line-height: 1.6;">{{ viewForm.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="有效期限">
          <span v-if="viewForm.expireDate">{{ formatExpireDate(viewForm.expireDate) }}</span>
          <span v-else style="color: #909399;">无限制</span>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">
          {{ formatPublishTime(viewForm.publishTime || viewForm.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(viewForm.status)">{{ viewForm.status || '未知' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑公告对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑公告" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="editForm.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="editForm.expireDate" type="datetime" placeholder="选择过期日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="已发布" value="已发布" />
            <el-option label="已归档" value="已归档" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeEditDialog">取消</el-button>
          <el-button type="primary" @click="saveEdit">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getAnnouncementList,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement
} from '@/api/announcement.js'
import { getBuildingList } from '@/api/building.js'

export default {
  name: 'SuperAdminAnnouncement',
  data() {
    return {
      publishDialogVisible: false,
      editDialogVisible: false,
      viewDialogVisible: false,
      loading: false,
      loadingBuildings: false,
      availableBuildings: [],
      announcementForm: {
        buildingId: '',
        title: '',
        content: '',
        expireDate: null
      },
      editForm: {
        id: null,
        title: '',
        content: '',
        expireDate: '',
        status: '已发布'
      },
      viewForm: {},
      announcements: [],
      currentPage: 1,
      pageSize: 20,
      total: 0
    }
  },
  mounted() {
    this.loadAnnouncements()
    this.loadAvailableBuildings()
  },
  methods: {
    async loadAnnouncements() {
      try {
        this.loading = true
        const res = await getAnnouncementList({
          currentPage: this.currentPage,
          pageSize: this.pageSize
        })
        if (res.code === 200) {
          this.announcements = res.data.records || res.data || []
          this.total = res.data.total || this.announcements.length
        }
      } catch (error) {
        console.error('加载公告失败:', error)
        this.$message.error('加载公告列表失败')
      } finally {
        this.loading = false
      }
    },
    async loadAvailableBuildings() {
      try {
        this.loadingBuildings = true
        const res = await getBuildingList({ currentPage: 1, pageSize: 1000 })
        if (res.code === 200) {
          this.availableBuildings = res.data.records || res.data || []
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
      } finally {
        this.loadingBuildings = false
      }
    },
    showPublishDialog() {
      this.announcementForm = {
        buildingId: '',
        title: '',
        content: '',
        expireDate: null
      }
      this.publishDialogVisible = true
    },
    closePublishDialog() {
      this.publishDialogVisible = false
    },
    // 格式化日期为本地时间字符串（避免时区问题）
    formatDateToLocal(date) {
      if (!date) return null
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
    },
    async publishAnnouncement() {
      if (!this.announcementForm.title || !this.announcementForm.content) {
        this.$message.warning('请填写标题和内容')
        return
      }

      try {
        this.loading = true
        
        const payload = {
          title: this.announcementForm.title,
          content: this.announcementForm.content,
          buildingIds: this.announcementForm.buildingId ? [this.announcementForm.buildingId] : [],
          expireDate: this.formatDateToLocal(this.announcementForm.expireDate),
          status: '已发布'
        }

        const res = await createAnnouncement(payload)
        
        if (res.code === 200) {
          this.$message.success('公告发布成功')
          this.loadAnnouncements()
          this.closePublishDialog()
        } else {
          this.$message.error(res.message || '发布失败')
        }
      } catch (error) {
        console.error('发布公告失败:', error)
        this.$message.error('发布失败：' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },
    viewAnnouncement(announcement) {
      this.viewForm = { ...announcement }
      this.viewDialogVisible = true
    },
    editAnnouncement(announcement) {
      this.editForm = {
        id: announcement.id,
        title: announcement.title,
        content: announcement.content,
        expireDate: announcement.expireDate ? new Date(announcement.expireDate) : null,
        status: announcement.status || '已发布'
      }
      this.editDialogVisible = true
    },
    closeEditDialog() {
      this.editDialogVisible = false
    },
    async saveEdit() {
      if (!this.editForm.title || !this.editForm.content) {
        this.$message.warning('请填写完整信息')
        return
      }
      
      try {
        this.loading = true
        let expireDateValue = null
        if (this.editForm.expireDate) {
          if (this.editForm.expireDate instanceof Date) {
            expireDateValue = this.editForm.expireDate.toISOString()
          } else {
            expireDateValue = this.editForm.expireDate
          }
        }
        
        const updatedData = {
          title: this.editForm.title,
          content: this.editForm.content,
          expireDate: expireDateValue,
          status: this.editForm.status
        }
        
        const res = await updateAnnouncement(this.editForm.id, updatedData)
        
        if (res.code === 200) {
          this.$message.success('公告编辑成功')
          this.loadAnnouncements()
          this.closeEditDialog()
        } else {
          this.$message.error(res.message || '编辑失败')
        }
      } catch (error) {
        console.error('编辑公告失败:', error)
        this.$message.error('编辑失败：' + (error.message || '网络错误'))
      } finally {
        this.loading = false
      }
    },
    deleteAnnouncement(announcement) {
      this.$confirm('确定要删除这个公告吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteAnnouncement(announcement.id)
          if (res.code === 200) {
            this.$message.success('公告已删除')
            this.loadAnnouncements()
          } else {
            this.$message.error(res.message || '删除失败')
          }
        } catch (error) {
          console.error('删除公告失败:', error)
          this.$message.error('删除失败：' + (error.message || '网络错误'))
        }
      }).catch(() => {})
    },
    formatPublishTime(time) {
      if (!time) return '未知'
      try {
        const date = new Date(time)
        if (isNaN(date.getTime())) return '未知'
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
      } catch (e) {
        return '未知'
      }
    },
    getStatusType(status) {
      const types = {
        '已发布': 'success',
        '已归档': 'info'
      }
      return types[status] || 'info'
    },
    formatExpireDate(date) {
      if (!date) return '无限制'
      if (typeof date === 'string') {
        if (date.includes('T')) return date.split('T')[0]
        if (/^\d{4}-\d{2}-\d{2}$/.test(date)) return date
        try {
          const parsedDate = new Date(date)
          if (!isNaN(parsedDate.getTime())) return parsedDate.toISOString().split('T')[0]
        } catch (e) {}
      }
      if (date instanceof Date) return date.toISOString().split('T')[0]
      return '无限制'
    }
  }
}
</script>

<style scoped>
.announcement-page {
  padding: 20px;
}
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions h2 {
  margin: 0;
}
</style>
