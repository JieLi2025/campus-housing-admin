<template>
  <div class="student-info-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>学生信息管理</h2>
          <p class="page-description">查看和管理学生基础信息，监控学生注册状态</p>
        </div>
      </template>
      
      <!-- 搜索和筛选区域 -->
      <div class="search-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学号、姓名"
              clearable
              @input="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="4">
            <el-select
              v-model="filterMajor"
              placeholder="选择专业"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部专业" value="" />
              <el-option
                v-for="major in availableMajors"
                :key="major"
                :label="major"
                :value="major"
              />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select
              v-model="filterStatus"
              placeholder="注册状态"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部状态" value="" />
              <el-option label="已注册" value="registered" />
              <el-option label="未注册" value="unregistered" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select
              v-model="filterGender"
              placeholder="性别"
              clearable
              @change="handleSearch"
            >
              <el-option label="全部性别" value="" />
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <div class="action-buttons">
              <el-button @click="resetFilters">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
              <el-button type="primary" @click="exportData">
                <el-icon><Download /></el-icon>
                导出数据
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 统计信息 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="学生总数" :value="stats.total" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已注册" :value="stats.registered" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="未注册" :value="stats.unregistered" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="注册率" :value="stats.registrationRate" suffix="%" />
          </el-col>
        </el-row>
      </div>
      
      <!-- 学生列表表格 -->
      <div class="table-section">
        <el-table
          :data="students"
          style="width: 100%"
          v-loading="loading"
          @row-click="viewStudentDetail"
          row-class-name="clickable-row"
        >
          <el-table-column prop="studentId" label="学号" min-width="120" sortable align="center" />
          <el-table-column prop="realName" label="姓名" min-width="100" align="center" />
          <el-table-column prop="gender" label="性别" min-width="80" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.gender === '男' ? 'primary' : 'success'" size="small">
                {{ scope.row.gender }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="major" label="专业" min-width="150" align="center" />
          <el-table-column prop="className" label="班级" min-width="120" align="center" />
          <el-table-column label="注册状态" min-width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isRegistered ? 'success' : 'warning'" size="small">
                {{ scope.row.isRegistered ? '已注册' : '未注册' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="260" align="center" show-overflow-tooltip>
            <template #default="scope">
              <span class="email-cell">{{ scope.row.email || '未绑定' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="宿舍号" min-width="120" align="center">
            <template #default="scope">
              {{ scope.row.roomNumber || '暂未分配' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="scope">
              <div class="action-buttons">
                <el-button type="primary" size="small" @click.stop="viewStudentDetail(scope.row)">
                  查看详情
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
    
    <!-- 学生详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="学生详细信息"
      width="800px"
      :before-close="closeDetailDialog"
    >
      <div v-if="selectedStudent" class="student-detail">
        <el-row :gutter="20">
          <!-- 左侧：基础信息 -->
          <el-col :span="12">
            <div class="detail-section">
              <h4><el-icon><User /></el-icon> 基础信息（不可修改）</h4>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">学号：</span>
                  <span class="value">{{ selectedStudent.studentId }}</span>
                </div>
                <div class="info-item">
                  <span class="label">姓名：</span>
                  <span class="value">{{ selectedStudent.realName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">性别：</span>
                  <span class="value">{{ selectedStudent.gender }}</span>
                </div>
                <div class="info-item">
                  <span class="label">班级：</span>
                  <span class="value">{{ selectedStudent.className }}</span>
                </div>
                <div class="info-item">
                  <span class="label">专业：</span>
                  <span class="value">{{ selectedStudent.major }}</span>
                </div>
                <div class="info-item">
                  <span class="label">宿舍号：</span>
                  <span class="value" :class="{ 'empty-value': !selectedStudent.roomNumber && !selectedStudent.dormNumber }">{{ selectedStudent.roomNumber || selectedStudent.dormNumber || '未分配' }}</span>
                </div>
              </div>
            </div>
            
            <div class="detail-section">
              <h4><el-icon><Calendar /></el-icon> 系统信息</h4>
              <div class="info-grid">
                <div class="info-item">
                  <span class="label">注册状态：</span>
                  <el-tag :type="selectedStudent.isRegistered ? 'success' : 'warning'" size="small">
                    {{ selectedStudent.isRegistered ? '已注册' : '未注册' }}
                  </el-tag>
                </div>
                <div class="info-item">
                  <span class="label">注册时间：</span>
                  <span class="value" :class="{ 'empty-value': !selectedStudent.registeredAt }">{{ selectedStudent.registeredAt ? formatDateTime(selectedStudent.registeredAt) : '未注册' }}</span>
                </div>
              </div>
            </div>
          </el-col>
          
          <!-- 右侧：个人资料 -->
          <el-col :span="12">
            <div class="detail-section">
              <h4><el-icon><EditPen /></el-icon> 个人资料（学生可编辑）</h4>
              <div class="profile-info">
                <div class="avatar-section">
                  <el-avatar :size="80" :src="studentProfile.avatar">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </div>
                <div class="info-grid-personal">
                  <div class="info-item full-width">
                    <span class="label">邮箱：</span>
                    <span class="value" :class="{ 'empty-value': !selectedStudent.email }">{{ selectedStudent.email || '未绑定' }}</span>
                  </div>
                  <div class="info-item full-width bio-item">
                    <span class="label">个人简介：</span>
                    <span class="value bio-value" :class="{ 'empty-value': !studentProfile.bio }">{{ studentProfile.bio || '暂无简介' }}</span>
                  </div>
                </div>
                
                <div class="tags-section">
                  <span class="label">个人标签：</span>
                  <div class="tags-list">
                    <template v-if="studentTags && studentTags.length">
                      <el-tag v-for="tag in visibleTags" :key="tag" size="small" style="margin: 2px;">{{ tag }}</el-tag>
                      <el-tag v-if="hiddenTagsCount > 0" type="info" size="small" style="margin: 2px;">+{{ hiddenTagsCount }}</el-tag>
                    </template>
                    <span v-else class="no-tags">暂无标签</span>
                  </div>
                </div>
              </div>
            </div>
            
          </el-col>
        </el-row>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetailDialog">关闭</el-button>
          <el-button type="primary" @click="contactStudent(selectedStudent)">
            <el-icon><ChatDotRound /></el-icon>
            联系学生
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Search, Refresh, Download, User, EditPen, Calendar, House, ChatDotRound } from '@element-plus/icons-vue'
import { getStudentList } from '@/api/student'

export default {
  name: 'AdminStudentInfo',
  components: {
    Search,
    Refresh,
    Download,
    User,
    EditPen,
    Calendar,
    House,
    ChatDotRound
  },
  data() {
    return {
      loading: false,
      searchKeyword: '',
      filterMajor: '',
      filterStatus: '',
      filterGender: '',
      currentPage: 1,
      pageSize: 20,
      
      students: [],
      total: 0,
      availableMajors: [],  // 从后端获取的所有可用专业列表
      
      detailDialogVisible: false,
      selectedStudent: null,
      studentProfile: {},
      studentTags: [],
      
      stats: {
        total: 0,
        registered: 0,
        unregistered: 0,
        registrationRate: 0
      }
    }
  },
  computed: {
    visibleTags() {
      if (!this.studentTags) {
        return [];
      }
      const maxVisible = 3;
      return this.studentTags.slice(0, maxVisible);
    },
    hiddenTagsCount() {
      if (!this.studentTags) {
        return 0;
      }
      const maxVisible = 3;
      const totalTags = this.studentTags.length;
      return totalTags > maxVisible ? totalTags - maxVisible : 0;
    }
  },
  mounted() {
    this.loadStudentsData()
  },
  methods: {
    // 加载学生数据（从后端API）
    async loadStudentsData() {
      this.loading = true
      try {
        const queryParams = {
          keyword: this.searchKeyword,
          major: this.filterMajor,
          gender: this.filterGender,
          registrationStatus: this.filterStatus,
          currentPage: this.currentPage,
          pageSize: this.pageSize
        }
        
        const response = await getStudentList(queryParams)
        
        if (response.code === 200) {
          this.students = response.data.students
          this.total = response.data.total
          this.stats = response.data.stats
          // 从后端获取所有可用的专业列表
          if (response.data.availableMajors) {
            this.availableMajors = response.data.availableMajors
          }
          
          console.log('学生数据加载成功：', response.data)
        } else {
          this.$message.error(response.message || '加载学生数据失败')
        }
      } catch (error) {
        console.error('加载学生数据失败:', error)
        this.$message.error('加载学生数据失败，请检查后端服务')
      } finally {
        this.loading = false
      }
    },
    
    // 搜索和筛选（重新加载数据）
    handleSearch() {
      this.currentPage = 1
      this.loadStudentsData()
    },
    
    // 重置筛选条件
    resetFilters() {
      this.searchKeyword = ''
      this.filterMajor = ''
      this.filterStatus = ''
      this.filterGender = ''
      this.currentPage = 1
      this.loadStudentsData()
      this.$message.success('筛选条件已重置')
    },
    
    // 查看学生详情
    viewStudentDetail(student) {
      this.selectedStudent = student
      
      // 加载学生的个人资料信息
      this.loadStudentProfile(student.studentId)
      
      this.detailDialogVisible = true
    },
    
    // 加载学生个人资料（从后端API获取）
    async loadStudentProfile(studentId) {
      try {
        // 调用后端API获取学生详细信息
        const { getStudentByStudentId } = await import('@/api/student.js')
        const response = await getStudentByStudentId(studentId)
        
        if (response.code === 200 && response.data) {
          // 从后端数据中提取个人资料字段
          const studentData = response.data
          this.studentProfile = {
            avatar: studentData.avatar || '',
            bio: studentData.bio || '',
            dormLocation: studentData.roomNumber || studentData.dormNumber || '',
            dormPreference: ''  // 这个字段后端暂无，保持为空
          }
          
          // 更新selectedStudent的宿舍号（如果后端返回了更详细的信息）
          if (this.selectedStudent && (studentData.roomNumber || studentData.dormNumber)) {
            this.selectedStudent.roomNumber = studentData.roomNumber || studentData.dormNumber
          }
          
          // 处理标签数据（后端返回的是JSON字符串）
          if (studentData.tags) {
            try {
              this.studentTags = JSON.parse(studentData.tags)
            } catch (e) {
              console.error('解析学生标签失败:', e)
              this.studentTags = []
            }
          } else {
            this.studentTags = []
          }
        } else {
          // API调用失败，使用空数据（基本信息已在selectedStudent中）
          console.warn('获取学生详细信息失败，使用基本信息:', response.message)
          this.studentProfile = {}
          this.studentTags = []
        }
      } catch (error) {
        console.error('加载学生资料失败:', error)
        // API调用失败时，使用空数据（基本信息已在selectedStudent中）
        // 对于未注册学生，这是正常情况，不显示错误提示
        this.studentProfile = {}
        this.studentTags = []
      }
    },
    
    // 关闭详情对话框
    closeDetailDialog() {
      this.detailDialogVisible = false
      this.selectedStudent = null
      this.studentProfile = {}
      this.studentTags = []
    },
    
    // 联系学生
    contactStudent(student) {
      // 跳转到管理员聊天页面
      const redirectData = {
        chatId: `student_${student.studentId}`,
        from: 'student-info',
        timestamp: Date.now()
      }
      
      sessionStorage.setItem('chatRedirect', JSON.stringify(redirectData))
      
      this.$router.push({
        path: '/admin/chat-messages',
        query: { redirect: 'true' }
      })
    },
    
    // 导出数据（导出全部符合筛选条件的数据）
    async exportData() {
      const loadingMsg = this.$message({
        message: '正在获取全部数据...',
        type: 'info',
        duration: 0  // 不自动关闭
      })
      
      try {
        // 请求全部数据（不分页）
        const queryParams = {
          keyword: this.searchKeyword,
          major: this.filterMajor,
          gender: this.filterGender,
          registrationStatus: this.filterStatus,
          currentPage: 1,
          pageSize: 99999  // 获取全部数据
        }
        
        const response = await getStudentList(queryParams)
        loadingMsg.close()  // 关闭加载提示
        
        if (response.code !== 200 || !response.data.students || response.data.students.length === 0) {
          this.$message.warning('暂无数据可导出')
          return
        }
        
        const allStudents = response.data.students
        
        const exportData = allStudents.map(student => ({
          学号: student.studentId,
          姓名: student.realName,
          性别: student.gender,
          专业: student.major,
          班级: student.className,
          注册状态: student.isRegistered ? '已注册' : '未注册',
          邮箱: student.email || '未绑定',
          宿舍号: student.roomNumber || student.dormNumber || '暂未分配'
        }))
        
        // 转换为CSV格式（学号作为文本处理，避免科学计数法）
        const csvContent = this.convertToCSV(exportData, ['学号'])
        
        // 添加 UTF-8 BOM 标记，解决 Excel 打开中文乱码问题
        const BOM = '\uFEFF'
        const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
        const link = document.createElement('a')
        const url = URL.createObjectURL(blob)
        link.setAttribute('href', url)
        link.setAttribute('download', `学生信息_${new Date().toLocaleDateString().replace(/\//g, '-')}.csv`)
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
        
        this.$message.success(`成功导出 ${allStudents.length} 条数据`)
      } catch (error) {
        console.error('导出数据失败:', error)
        this.$message.error('导出数据失败')
      }
    },
    
    // 转换为CSV格式
    // textFields: 需要作为文本处理的字段（避免Excel科学计数法）
    convertToCSV(data, textFields = []) {
      if (!data.length) return ''
      
      const headers = Object.keys(data[0])
      const csvRows = []
      
      // 添加表头
      csvRows.push(headers.join(','))
      
      // 添加数据行
      data.forEach(row => {
        const values = headers.map(header => {
          let value = row[header] || ''
          // 对于需要作为文本的字段，添加制表符前缀防止Excel转为科学计数法
          if (textFields.includes(header) && value) {
            value = '\t' + value
          }
          return `"${value}"`
        })
        csvRows.push(values.join(','))
      })
      
      return csvRows.join('\n')
    },
    
    // 格式化日期时间
    formatDateTime(dateString) {
      if (!dateString) return '未知'
      return new Date(dateString).toLocaleString('zh-CN')
    },
    
    // 分页处理
    handleSizeChange(newSize) {
      this.pageSize = newSize
      this.currentPage = 1
      this.loadStudentsData()
    },
    
    handleCurrentChange(newPage) {
      this.currentPage = newPage
      this.loadStudentsData()
    }
  }
}
</script>

<style scoped>
.student-info-page {
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-description {
  margin: 0;
  color: #909399;
  font-size: 14px;
  line-height: 1.5;
}

.search-section {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.action-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
}

.email-cell {
  white-space: nowrap;
  display: inline-block;
}

.stats-section {
  margin-bottom: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.table-section {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: #f5f7fa;
}

.pagination-container {
  padding: 20px;
  text-align: center;
  border-top: 1px solid #e4e7ed;
}

/* 学生详情对话框样式 */
.student-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-section h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 0;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-grid-personal .info-item {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
}

.info-grid-personal .info-item .label {
  min-width: 70px; /* Adjust label width */
  flex-shrink: 0;
}

.info-grid-personal .bio-item {
  flex-direction: column;
  margin-top: 16px;
}

.info-grid-personal .bio-item .bio-value {
  margin-top: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

.tags-section {
  display: flex;
  align-items: flex-start; /* Align label and tags to the top */
}

.tags-section .label {
    min-width: 70px; /* Match other labels */
    flex-shrink: 0;
    padding-top: 5px; /* Align with the center of the tags */
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: #606266;
  font-weight: 500;
  min-width: 60px;
  font-size: 14px;
}

.info-item .value {
  color: #303133;
  font-size: 14px;
  flex: 1;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.tags-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-height: 32px;
  align-items: center;
}

.no-tags,
.empty-value {
  color: #c0c4cc;
  font-style: italic;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .student-info-page {
    padding: 10px;
  }
  
  .search-section .el-row {
    flex-direction: column;
    gap: 10px;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .student-detail {
    max-height: 400px;
  }
}
</style>




