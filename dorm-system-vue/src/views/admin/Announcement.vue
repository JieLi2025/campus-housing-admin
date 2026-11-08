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
      <el-table :data="announcements" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="150" align="center" header-align="center" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip min-width="200" align="center" header-align="center" />
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

    <!-- 发布公告抽屉（楼栋公告表单） -->
    <el-drawer v-model="publishDialogVisible" title="发布楼栋公告" size="600px" direction="rtl" :close-on-click-modal="false">
      <el-form :model="announcementForm" label-width="100px" style="padding-right: 20px;">
        <el-form-item label="选择楼栋" required>
          <el-select v-model="announcementForm.buildingId" placeholder="请选择楼栋" style="width: 100%" :loading="loadingBuildings">
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="building.name" 
              :value="building.id" 
            />
          </el-select>
          <div v-if="availableBuildings.length === 0 && !loadingBuildings" style="color: #909399; font-size: 12px; margin-top: 4px;">
            暂无可管理的楼栋
          </div>
        </el-form-item>

        <el-form-item label="公告标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入标题" />
        </el-form-item>

        <el-form-item label="公告副标题">
          <el-input v-model="announcementForm.subtitle" placeholder="可选" />
        </el-form-item>

        <el-form-item label="公告内容" required>
          <el-input v-model="announcementForm.content" type="textarea" :rows="6" placeholder="请输入内容" />
        </el-form-item>

        <el-form-item label="公告要点">
          <el-input v-model="announcementForm.extraPoints" type="textarea" :rows="3" placeholder="每行一条要点（可选）" />
        </el-form-item>

        <el-form-item label="有效期限">
          <el-date-picker 
            v-model="announcementForm.range" 
            type="datetimerange" 
            start-placeholder="开始时间" 
            end-placeholder="结束时间" 
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%" 
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="closePublishDialog">取消</el-button>
          <el-button type="primary" @click="publishAnnouncement">发布公告</el-button>
        </el-form-item>
      </el-form>
    </el-drawer>

    <!-- 查看公告详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="公告详情" width="700px" :close-on-click-modal="false">
      <el-descriptions :column="1" border v-if="viewForm.id" label-class-name="detail-label">
        <el-descriptions-item label="选择楼栋" label-width="100px">
          <el-tag type="primary">{{ viewForm.building || viewForm.buildingName || '全部楼栋' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="公告标题" label-width="100px">{{ viewForm.title }}</el-descriptions-item>
        <el-descriptions-item label="公告副标题" label-width="100px" v-if="viewForm.subtitle">
          <span style="color: #909399;">{{ viewForm.subtitle }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="公告内容" label-width="100px">
          <div style="white-space: pre-wrap; line-height: 1.6;">{{ viewForm.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="公告要点" label-width="100px" v-if="viewForm.extraPoints">
          <div style="white-space: pre-wrap; line-height: 1.6; color: #e6a23c;">{{ viewForm.extraPoints }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="有效期限" label-width="100px">
          <span v-if="viewForm.expireDate">{{ formatExpireDate(viewForm.expireDate) }}</span>
          <span v-else style="color: #909399;">未设置</span>
        </el-descriptions-item>
        <el-descriptions-item label="发布时间" label-width="100px">
          {{ formatPublishTime(viewForm.publishTime || viewForm.createTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="状态" label-width="100px">
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
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="editForm.expireDate"
            type="date"
            placeholder="选择过期日期"
            style="width: 100%"
          />
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
  deleteAnnouncement,
  publishAnnouncement as publishAnnouncementAPI
} from '@/api/announcement.js'
import { getMyBuildings } from '@/api/adminDormManagement.js'

export default {
  name: 'Announcement',
  data() {
    return {
      publishDialogVisible: false,
      editDialogVisible: false,
      viewDialogVisible: false,
      loading: false,
      loadingBuildings: false,
      availableBuildings: [], // 当前管理员可管理的楼栋列表
      announcementForm: {
        buildingId: null, // 改为楼栋ID
        title: '',
        subtitle: '',
        content: '',
        extraPoints: '',
        range: []
      },
      editForm: {
        id: null,
        title: '',
        content: '',
        expireDate: '',
        status: 'active'
      },
      viewForm: {},
      announcements: [],
      currentPage: 1,
      pageSize: 20,
      total: 0
    }
  },
  mounted() {
    this.loadAnnouncements();
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
    async showPublishDialog() {
      // 加载管理员可管理的楼栋列表
      await this.loadAvailableBuildings()
      
      // 初始化表单，如果有楼栋则默认选择第一个
      this.announcementForm = {
        buildingId: this.availableBuildings.length > 0 ? this.availableBuildings[0].id : null,
        title: '',
        subtitle: '',
        content: '',
        extraPoints: '',
        range: []
      }
      this.publishDialogVisible = true
    },
    async loadAvailableBuildings() {
      try {
        this.loadingBuildings = true
        const res = await getMyBuildings()
        if (res.code === 200) {
          this.availableBuildings = res.data || []
          if (this.availableBuildings.length === 0) {
            this.$message.warning('您暂无可管理的楼栋，无法发布公告')
          }
        } else {
          this.$message.error(res.message || '获取楼栋列表失败')
          this.availableBuildings = []
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
        this.$message.error('加载楼栋列表失败：' + (error.message || '网络错误'))
        this.availableBuildings = []
      } finally {
        this.loadingBuildings = false
      }
    },
    closePublishDialog() {
      this.publishDialogVisible = false
      this.announcementForm = {
        buildingId: null,
        title: '',
        subtitle: '',
        content: '',
        extraPoints: '',
        range: []
      }
    },
    async publishAnnouncement() {
      if (!this.announcementForm.title || !this.announcementForm.content) {
        this.$message.warning('请填写标题和内容')
        return
      }
      
      if (!this.announcementForm.buildingId) {
        this.$message.warning('请选择楼栋')
        return
      }

      try {
        this.loading = true
        
        const payload = {
          title: this.announcementForm.title,
          subtitle: this.announcementForm.subtitle || '',
          content: this.announcementForm.content,
          extraPoints: this.announcementForm.extraPoints || '',
          buildingIds: [this.announcementForm.buildingId], // 传递楼栋ID数组
          expireDate: this.announcementForm.range?.[1] || null,
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
      // 查看公告详情
      this.viewForm = { ...announcement }
      this.viewDialogVisible = true
    },
    editAnnouncement(announcement) {
      // 复制公告数据到编辑表单
      // 后端现在统一返回中文状态值
      this.editForm = {
        id: announcement.id,
        title: announcement.title,
        content: announcement.content,
        expireDate: announcement.expireDate ? new Date(announcement.expireDate) : null,
        status: announcement.status || '已发布' // 直接使用中文状态值
      }
      this.editDialogVisible = true
    },
    closeEditDialog() {
      this.editDialogVisible = false
      this.editForm = {
        id: null,
        title: '',
        content: '',
        expireDate: '',
        status: '已发布'
      }
    },
    async saveEdit() {
      if (!this.editForm.title || !this.editForm.content) {
        this.$message.warning('请填写完整信息')
        return
      }
      
      try {
        this.loading = true
        // 转换日期格式
        let expireDateValue = null
        if (this.editForm.expireDate) {
          if (this.editForm.expireDate instanceof Date) {
            // 转换为 ISO 字符串格式
            expireDateValue = this.editForm.expireDate.toISOString()
          } else {
            expireDateValue = this.editForm.expireDate
          }
        }
        
        const updatedData = {
          title: this.editForm.title,
          content: this.editForm.content,
          expireDate: expireDateValue,
          status: this.editForm.status // 直接使用中文状态值
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
      }).catch(() => {
        // 用户取消
      })
    },
    formatPublishTime(time) {
      if (!time) return '未知';
      
      try {
        const date = new Date(time);
        if (isNaN(date.getTime())) return '未知';
        
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      } catch (e) {
        return '未知';
      }
    },
    getStatusType(status) {
      // 统一使用中文状态值
      const types = {
        '已发布': 'success',
        '已归档': 'info'
      }
      return types[status] || 'info'
    },
    getStatusText(status) {
      // 后端直接返回中文，直接使用即可
      return status || '未知'
    },
    formatExpireDate(date) {
      if (!date) {
        return '无限制';
      }
      
      // 如果是字符串格式的日期
      if (typeof date === 'string') {
        // 如果是ISO格式，提取日期部分
        if (date.includes('T')) {
          return date.split('T')[0];
        }
        // 如果已经是YYYY-MM-DD格式，直接返回
        if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
          return date;
        }
        // 尝试解析其他格式
        try {
          const parsedDate = new Date(date);
          if (!isNaN(parsedDate.getTime())) {
            return parsedDate.toISOString().split('T')[0];
          }
        } catch (e) {
          console.error('日期解析失败:', e);
        }
      }
      
      // 如果是Date对象
      if (date instanceof Date) {
        return date.toISOString().split('T')[0];
      }
      
      return '无限制';
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

.dialog-footer {
  text-align: right;
}

.dialog-footer .el-button {
  margin-left: 10px;
}

@media (max-width: 768px) {
  .announcement-page {
    padding: 10px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}

/* 楼栋公告表单样式 */
:deep(.el-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

:deep(.el-checkbox) {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  transition: all 0.3s;
  margin: 0;
}

:deep(.el-checkbox:hover) {
  border-color: #409eff;
  background: #f5f9ff;
}

:deep(.el-checkbox.is-checked) {
  border-color: #409eff;
  background: #ecf5ff;
}

:deep(.el-checkbox.is-disabled) {
  background: #f5f7fa;
  cursor: not-allowed;
}

.option-desc {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

:deep(.el-divider) {
  margin: 24px 0;
}

:deep(.el-divider__text) {
  background: #fff;
  color: #606266;
  font-weight: 600;
  font-size: 14px;
}

/* 抽屉样式优化 */
:deep(.el-drawer__header) {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-drawer__title) {
  font-weight: 600;
  color: #303133;
  font-size: 18px;
}

:deep(.el-drawer__body) {
  padding: 20px;
}

/* 公告详情标签样式 */
:deep(.detail-label) {
  white-space: nowrap !important;
  min-width: 80px !important;
}

:deep(.el-descriptions__label) {
  white-space: nowrap !important;
  min-width: 80px !important;
}
</style>
