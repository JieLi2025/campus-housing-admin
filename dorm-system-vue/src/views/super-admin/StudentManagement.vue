<template>
  <div class="student-management">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-header-content">
        <el-icon class="page-icon"><User /></el-icon>
        <div class="page-title-group">
          <h2 class="page-title">学生信息管理</h2>
          <p class="page-description">导入和管理学生基础信息，系统将根据导入的信息控制学生注册和权限</p>
        </div>
      </div>
    </div>

    <!-- 功能卡片区域 -->
    <div class="content-grid">
      <!-- Excel导入卡片 -->
      <el-card class="import-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Upload /></el-icon>
            <span class="header-title">Excel导入</span>
          </div>
        </template>
        
        <div class="import-section">
          <el-alert 
            type="info" 
            :closable="false"
            class="import-description"
          >
            <template #title>
              <div class="alert-title">导入说明</div>
            </template>
            <div class="import-info-grid">
              <div class="info-item">
                <el-icon class="info-icon"><Document /></el-icon>
                <div class="info-content">
                  <div class="info-label">支持格式</div>
                  <div class="info-value">.xlsx / .csv 文件</div>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><List /></el-icon>
                <div class="info-content">
                  <div class="info-label">必需字段</div>
                  <div class="info-value">学号、姓名、性别、录取专业、年级、班级</div>
                </div>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><Warning /></el-icon>
                <div class="info-content">
                  <div class="info-label">注意事项</div>
                  <div class="info-value">学号必须唯一，邀请码自动生成（年份+8位随机字符+校验码），有效期60天</div>
                </div>
              </div>
            </div>
          </el-alert>
          
          <!-- 文件上传区域 -->
          <el-upload
            ref="uploadRef"
            class="upload-demo"
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".xlsx,.xls,.csv"
            :limit="1"
            :file-list="fileList"
            :show-file-list="false"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将Excel或CSV文件拖拽到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 .xlsx 和 .csv 格式，文件不超过10MB
              </div>
            </template>
          </el-upload>
          
          <!-- 已上传文件显示 -->
          <div v-if="selectedFile" class="uploaded-file-card">
            <div class="file-info">
              <el-icon class="file-icon" :class="getFileIconClass(selectedFile.name)">
                <Document />
              </el-icon>
              <div class="file-details">
                <div class="file-name">{{ selectedFile.name }}</div>
                <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
              </div>
              <button 
                class="delete-file-btn"
                @click="removeFile"
                title="删除文件"
              >
                <el-icon class="delete-icon"><Close /></el-icon>
              </button>
            </div>
          </div>
          
          <!-- 操作按钮 -->
          <div class="action-buttons">
            <el-button 
              type="primary" 
              :loading="isProcessing"
              :disabled="!selectedFile"
              @click="previewFile"
            >
              <el-icon><View /></el-icon>
              预览数据
            </el-button>
            <el-button 
              type="success"
              :loading="isImporting"
              :disabled="!previewData || !previewData.length"
              @click="importData"
            >
              <el-icon><Check /></el-icon>
              确认导入
            </el-button>
            <el-button @click="downloadTemplate">
              <el-icon><Download /></el-icon>
              下载模板
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 数据统计卡片 -->
      <el-card class="stats-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><DataAnalysis /></el-icon>
            <span class="header-title">数据统计</span>
          </div>
        </template>
        
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-number">{{ totalStudents }}</div>
            <div class="stat-label">学生总数</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ totalMajors }}</div>
            <div class="stat-label">专业数量</div>
          </div>
          <div class="stat-item clickable" @click="filterStudents('male')" :class="{ active: currentFilter === 'male' }">
            <div class="stat-number text-primary">{{ maleStudents }}</div>
            <div class="stat-label">男生人数</div>
          </div>
          <div class="stat-item clickable" @click="filterStudents('female')" :class="{ active: currentFilter === 'female' }">
            <div class="stat-number text-danger">{{ femaleStudents }}</div>
            <div class="stat-label">女生人数</div>
          </div>
          <div class="stat-item clickable" @click="filterStudents('registered')" :class="{ active: currentFilter === 'registered' }">
            <div class="stat-number">{{ registeredStudents }}</div>
            <div class="stat-label">已注册</div>
          </div>
          <div class="stat-item clickable" @click="filterStudents('unregistered')" :class="{ active: currentFilter === 'unregistered' }">
            <div class="stat-number">{{ unregisteredStudents }}</div>
            <div class="stat-label">未注册</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 数据预览区域 -->
    <el-card v-if="previewData && previewData.length" class="preview-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><View /></el-icon>
          <span class="header-title">数据预览</span>
          <span class="preview-count">（共 {{ previewData.length }} 条记录）</span>
        </div>
      </template>
      
      <el-table 
        :data="paginatedPreviewData" 
        style="width: 100%"
        max-height="400"
        stripe
        border
      >
        <el-table-column prop="studentId" label="学号" min-width="120" align="center" />
        <el-table-column prop="realName" label="姓名" min-width="100" align="center" />
        <el-table-column label="性别" min-width="80" align="center">
          <template #default="scope">
            {{ scope.row.gender || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="major" label="录取专业" min-width="150" />
        <el-table-column prop="grade" label="年级" min-width="80" align="center" />
        <el-table-column prop="className" label="班级" min-width="130" />
        <el-table-column label="邀请码" min-width="180" align="center" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.inviteCode">{{ scope.row.inviteCode }}</span>
            <el-tag v-else type="warning" size="small">待生成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="120" align="center">
          <template #default="scope">
            <span v-if="scope.row.inviteCodeExpiry" style="font-size: 12px;">
              {{ formatInviteCodeExpiry(scope.row.inviteCodeExpiry) }}
            </span>
            <span v-else style="color: #909399; font-size: 12px;">60天</span>
          </template>
        </el-table-column>
        <el-table-column label="数据状态" min-width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.isValid ? 'success' : 'danger'" size="small">
              {{ scope.row.isValid ? '有效' : '无效' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="previewCurrentPage"
          :page-size="previewPageSize"
          :total="previewData.length"
          layout="prev, pager, next, total"
          @current-change="handlePreviewPageChange"
        />
      </div>
    </el-card>

    <!-- 已导入学生列表 -->
    <el-card class="students-list-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><User /></el-icon>
          <span class="header-title">学生列表</span>
          <div class="header-actions">
            <div class="search-filter-group">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索学号、姓名或专业"
                style="width: 300px"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-tag v-if="currentFilter !== 'all'" type="info" closable @close="clearFilter" class="filter-tag">
                筛选: {{ getFilterLabel(currentFilter) }}
              </el-tag>
            </div>
            <div class="action-button-group">
              <el-button 
                v-if="studentsWithoutInviteCode > 0"
                type="success" 
                @click="batchGenerateInviteCodes"
              >
                <el-icon><Tickets /></el-icon>
                批量生成邀请码 ({{ studentsWithoutInviteCode }})
              </el-button>
              <el-button 
                v-if="!selectAllPages"
                type="warning" 
                plain
                @click="selectAllPagesData"
              >
                <el-icon><Select /></el-icon>
                全选所有 {{ filteredStudentsList.length }} 条
              </el-button>
              <el-button 
                v-if="selectAllPages"
                type="info" 
                plain
                @click="cancelSelectAll"
              >
                <el-icon><Close /></el-icon>
                取消全选
              </el-button>
              <el-button 
                v-if="(selectedStudents && selectedStudents.length > 0) || selectAllPages"
                type="danger" 
                @click="batchDeleteStudents"
              >
                <el-icon><Delete /></el-icon>
                批量删除 ({{ selectAllPages ? filteredStudentsList.length : selectedStudents.length }})
              </el-button>
              <el-button type="primary" @click="exportStudentsList">
                <el-icon><Download /></el-icon>
                导出列表
              </el-button>
            </div>
          </div>
        </div>
      </template>
      
      <div class="table-container">
        <el-table 
          :data="paginatedStudentsList"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column prop="studentId" label="学号" min-width="120" align="center" />
          <el-table-column prop="realName" label="姓名" min-width="100" align="center" />
          <el-table-column label="性别" min-width="80" align="center">
            <template #default="scope">
              {{ scope.row.gender || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="major" label="录取专业" min-width="200" show-overflow-tooltip />
          <el-table-column prop="grade" label="年级" min-width="80" align="center" />
          <el-table-column prop="className" label="班级" min-width="130" />
          <el-table-column label="邀请码" min-width="180" align="center" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.inviteCode">{{ scope.row.inviteCode }}</span>
              <el-tag v-else type="warning" size="small">未生成</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册状态" min-width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.isRegistered ? 'success' : 'info'" size="small">
                {{ scope.row.isRegistered ? '已注册' : '未注册' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="scope">
              <el-button 
                type="primary" 
                size="small"
                @click="viewStudentDetail(scope.row)"
              >
                查看
              </el-button>
              <el-popconfirm
                title="确定要删除这条学生信息吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="deleteStudent(scope.row)"
              >
                <template #reference>
                  <el-button type="danger" size="small">
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 表格提示信息 -->
      <div v-if="selectAllPages || (selectedStudents && selectedStudents.length > 0)" class="table-tips">
        <el-alert 
          :type="selectAllPages ? 'warning' : 'info'" 
          :closable="false" 
          show-icon
        >
          <template #title>
            <span v-if="selectAllPages">
              已选择 <strong>全部 {{ filteredStudentsList.length }}</strong> 条记录（所有页）
            </span>
            <span v-else>
              已选择 <strong>{{ selectedStudents.length }}</strong> 条记录（当前页）
            </span>
          </template>
        </el-alert>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="studentsCurrentPage"
          :page-size="studentsPageSize"
          :total="filteredStudentsList.length"
          layout="total, prev, pager, next, sizes, jumper"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleStudentsPageSizeChange"
          @current-change="handleStudentsPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import * as XLSX from 'xlsx'
import { importStudents, generateInviteCodes, getStudentList, 
         deleteStudent as deleteStudentApi, 
         batchDeleteStudents as batchDeleteStudentsApi } from '@/api/student'

export default {
  name: 'StudentManagement',
  inject: ['logOperation'],
  data() {
    return {
      // 文件上传相关
      fileList: [],
      selectedFile: null,
      isProcessing: false,
      isImporting: false,
      
      // 数据预览相关
      previewData: [],
      previewCurrentPage: 1,
      previewPageSize: 10,
      
      // 学生列表相关
      studentsList: [],
      searchKeyword: '',
      studentsCurrentPage: 1,
      studentsPageSize: 20,
      selectedStudents: [],
      selectAllPages: false, // 是否全选所有页
      
      // 统计数据
      totalStudents: 0,
      registeredStudents: 0,
      unregisteredStudents: 0,
      totalMajors: 0,
      maleStudents: 0,
      femaleStudents: 0,
      studentsWithoutInviteCode: 0, // 未生成邀请码的学生数量
      
      // 筛选相关
      currentFilter: 'all'
    }
  },
  computed: {
    // 预览数据分页
    paginatedPreviewData() {
      if (!this.previewData || !Array.isArray(this.previewData)) {
        return []
      }
      const start = (this.previewCurrentPage - 1) * this.previewPageSize
      const end = start + this.previewPageSize
      return this.previewData.slice(start, end)
    },
    
    // 过滤后的学生列表
    filteredStudentsList() {
      let filteredList = this.studentsList || []

      // 先根据筛选条件过滤
      if (this.currentFilter !== 'all') {
        switch (this.currentFilter) {
          case 'male':
            filteredList = filteredList.filter(student => student.gender === '男')
            break
          case 'female':
            filteredList = filteredList.filter(student => student.gender === '女')
            break
          case 'registered':
            filteredList = filteredList.filter(student => student.isRegistered === true)
            break
          case 'unregistered':
            filteredList = filteredList.filter(student => student.isRegistered === false)
            break
        }
      }

      // 再根据搜索关键词过滤
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase()
        filteredList = filteredList.filter(student => 
          student.studentId.toLowerCase().includes(keyword) ||
          student.realName.toLowerCase().includes(keyword) ||
          student.major.toLowerCase().includes(keyword)
        )
      }

      return filteredList
    },
    
    // 学生列表分页
    paginatedStudentsList() {
      const start = (this.studentsCurrentPage - 1) * this.studentsPageSize
      const end = start + this.studentsPageSize
      return this.filteredStudentsList.slice(start, end)
    }
  },
  watch: {
    // 监听搜索关键词变化，取消全选
    searchKeyword() {
      this.selectAllPages = false
      this.selectedStudents = []
    }
  },
  mounted() {
    // 优先从后端加载数据
    this.loadStudentsFromBackend()
  },
  methods: {
    // 从后端加载学生数据
    async loadStudentsFromBackend() {
      try {
        const response = await getStudentList({
          currentPage: 1,
          pageSize: 10000, // 加载所有数据
          keyword: '',
          major: '',
          gender: '',
          grade: '',
          registrationStatus: ''
        })
        
        if (response.code === 200 && response.data) {
          this.studentsList = response.data.students || []
          
          // 更新统计信息
          if (response.data.stats) {
            this.totalStudents = response.data.stats.total || 0
            this.registeredStudents = response.data.stats.registered || 0
            this.unregisteredStudents = response.data.stats.unregistered || 0
          }
          
          this.updateStats()
          
          console.log('✅ 从后端加载学生数据成功:', this.studentsList.length)
        } else {
          console.warn('⚠️ 后端返回异常:', response)
          this.$message.error('加载学生数据失败：' + (response.message || '未知错误'))
        }
      } catch (error) {
        console.error('❌ 从后端加载学生数据失败:', error)
        this.$message.error('加载学生数据失败：' + (error.message || '网络错误'))
      }
    },
    
    // 更新统计数据
    updateStats() {
      const students = this.studentsList || []
      this.totalStudents = students.length
      this.registeredStudents = students.filter(s => s.isRegistered).length
      this.unregisteredStudents = this.totalStudents - this.registeredStudents
      
      // 性别统计（使用中文：男/女）
      this.maleStudents = students.filter(s => s.gender === '男').length
      this.femaleStudents = students.filter(s => s.gender === '女').length
      
      // 邀请码统计
      this.studentsWithoutInviteCode = students.filter(s => !s.inviteCode).length
      
      const majors = new Set(students.map(s => s.major))
      this.totalMajors = majors.size
    },
    
    // 文件选择处理
    handleFileChange(file) {
      this.selectedFile = file.raw
      this.previewData = []
    },
    
    // 文件移除处理
    handleFileRemove() {
      this.selectedFile = null
      this.previewData = []
    },
    
    // 删除文件
    removeFile() {
      this.selectedFile = null
      this.fileList = []
      this.previewData = []
      this.$message.info('已删除文件')
    },
    
    // 获取文件图标类名
    getFileIconClass(filename) {
      if (!filename) return ''
      const ext = filename.toLowerCase().split('.').pop()
      if (ext === 'csv') return 'csv-file'
      if (ext === 'xlsx' || ext === 'xls') return 'excel-file'
      return ''
    },
    
    // 格式化文件大小
    formatFileSize(bytes) {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
    },
    
    // 预览文件数据
    async previewFile() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择文件')
        return
      }
      
      this.isProcessing = true
      
      try {
        const data = await this.parseFile(this.selectedFile)
        this.previewData = data
        this.previewCurrentPage = 1
        this.$message.success(`解析成功，共 ${data.length} 条记录`)
        this.logOperation('预览文件', '学生信息管理')
      } catch (error) {
        console.error('文件解析失败:', error)
        this.$message.error('文件解析失败：' + error.message)
      } finally {
        this.isProcessing = false
      }
    },
    
    // 解析文件（支持Excel和CSV）
    parseFile(file) {
      const fileName = file.name.toLowerCase()
      
      if (fileName.endsWith('.csv')) {
        return this.parseCSVFile(file)
      } else if (fileName.endsWith('.xlsx') || fileName.endsWith('.xls')) {
        return this.parseExcelFile(file)
      } else {
        return Promise.reject(new Error('不支持的文件格式，请上传 .xlsx 或 .csv 文件'))
      }
    },
    
    // 解析CSV文件
    parseCSVFile(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        
        reader.onload = (e) => {
          try {
            const text = e.target.result
            const lines = text.split(/\r?\n/).filter(line => line.trim())
            
            if (lines.length < 2) {
              reject(new Error('CSV文件内容为空或格式不正确'))
              return
            }
            
            // 解析表头
            const headers = lines[0].split(',').map(h => h.trim())
            
            // 解析数据行
            const jsonData = []
            for (let i = 1; i < lines.length; i++) {
              const values = lines[i].split(',')
              if (values.length === headers.length) {
                const row = {}
                headers.forEach((header, index) => {
                  row[header] = values[index] ? values[index].trim() : ''
                })
                jsonData.push(row)
              }
            }
            
            // 验证和转换数据
            const processedData = this.validateAndProcessData(jsonData)
            resolve(processedData)
          } catch (error) {
            reject(new Error('CSV文件格式错误：' + error.message))
          }
        }
        
        reader.onerror = () => {
          reject(new Error('文件读取失败'))
        }
        
        reader.readAsText(file, 'UTF-8')
      })
    },
    
    // 解析Excel文件
    parseExcelFile(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader()
        
        reader.onload = (e) => {
          try {
            const data = e.target.result
            const workbook = XLSX.read(data, { type: 'binary' })
            const sheetName = workbook.SheetNames[0]
            const worksheet = workbook.Sheets[sheetName]
            const jsonData = XLSX.utils.sheet_to_json(worksheet)
            
            // 验证和转换数据
            const processedData = this.validateAndProcessData(jsonData)
            resolve(processedData)
          } catch (error) {
            reject(new Error('Excel文件格式错误：' + error.message))
          }
        }
        
        reader.onerror = () => {
          reject(new Error('文件读取失败'))
        }
        
        reader.readAsBinaryString(file)
      })
    },
    
    // 验证和处理数据
    validateAndProcessData(jsonData) {
      const requiredFields = ['学号', '姓名', '性别', '录取专业', '班级', '年级']
      const processedData = []
      
      jsonData.forEach((row, index) => {
        const student = {
          isValid: true,
          errors: []
        }
        
        // 验证必需字段
        requiredFields.forEach(field => {
          if (!row[field] || String(row[field]).trim() === '') {
            student.isValid = false
            student.errors.push(`第${index + 2}行：${field}不能为空`)
          }
        })
        
        if (student.isValid) {
          student.studentId = String(row['学号']).trim()
          student.realName = String(row['姓名']).trim()
          
          const genderRaw = String(row['性别']).trim()
          student.gender = genderRaw
          
          student.major = String(row['录取专业']).trim()
          student.grade = String(row['年级']).trim()
          student.className = String(row['班级']).trim()
          
          // 邀请码处理：如果Excel中提供了邀请码则使用，否则暂不生成
          if (row['邀请码'] && String(row['邀请码']).trim() !== '' && String(row['邀请码']).trim() !== '（留空自动生成）') {
            student.inviteCode = String(row['邀请码']).trim()
            // 邀请码状态和有效期
            student.inviteCodeStatus = 'unused'
            const expiryDate = new Date()
            expiryDate.setDate(expiryDate.getDate() + 60)
            student.inviteCodeExpiry = expiryDate.toISOString()
          } else {
            // 导入时不生成邀请码，等待管理员手动批量生成
            student.inviteCode = null
            student.inviteCodeStatus = null
            student.inviteCodeExpiry = null
          }
          
          student.isRegistered = false
          // 注册时间将在用户实际注册时设置
          
          // 验证学号格式
          if (!/^\d{8,12}$/.test(student.studentId)) {
            student.isValid = false
            student.errors.push(`第${index + 2}行：学号格式不正确`)
          }
          
          // 验证性别（使用中文：男/女）
          if (student.gender !== '男' && student.gender !== '女') {
            student.isValid = false
            student.errors.push(`第${index + 2}行：性别只能是"男"或"女"`)
          }
        }
        
        processedData.push(student)
      })
      
      return processedData
    },
    
    // 获取邀请码状态
    getInviteCodeStatus(student) {
      // 如果没有邀请码，返回未生成状态
      if (!student.inviteCode) {
        return 'not_generated'
      }
      
      // 如果已注册，邀请码状态为已使用
      if (student.isRegistered) {
        return 'used'
      }
      
      // 检查是否过期
      if (student.inviteCodeExpiry) {
        const expiryDate = new Date(student.inviteCodeExpiry)
        const now = new Date()
        if (now > expiryDate) {
          return 'expired'
        }
      }
      
      // 返回数据库中的状态，默认为未使用
      return student.inviteCodeStatus || 'unused'
    },
    
    // 获取邀请码状态文本
    getInviteCodeStatusText(student) {
      const status = this.getInviteCodeStatus(student)
      const statusMap = {
        'not_generated': '未生成',
        'unused': '未使用',
        'used': '已使用',
        'expired': '已过期'
      }
      return statusMap[status] || '未知'
    },
    
    // 获取邀请码状态标签类型
    getInviteCodeStatusType(student) {
      const status = this.getInviteCodeStatus(student)
      const typeMap = {
        'not_generated': 'warning',  // 橙色
        'unused': 'success',         // 绿色
        'used': 'info',              // 灰色
        'expired': 'danger'          // 红色
      }
      return typeMap[status] || 'info'
    },
    
    // 格式化注册时间
    formatRegistrationDate(dateString) {
      if (!dateString) return '未注册'
      
      try {
        const date = new Date(dateString)
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      } catch (error) {
        console.error('时间格式化失败:', error)
        return '格式错误'
      }
    },
    
    // 格式化邀请码有效期
    formatInviteCodeExpiry(expiryString) {
      if (!expiryString) return '无期限'
      
      try {
        const date = new Date(expiryString)
        return date.toLocaleDateString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        })
      } catch (error) {
        return '格式错误'
      }
    },
    
    // 批量生成邀请码
    async batchGenerateInviteCodes() {
      const studentsWithoutCode = this.studentsList.filter(s => !s.inviteCode)
      
      if (studentsWithoutCode.length === 0) {
        this.$message.warning('没有需要生成邀请码的学生')
        return
      }
      
      try {
        const result = await this.$confirm(
          `确定要为 <strong>${studentsWithoutCode.length}</strong> 名学生批量生成邀请码吗？<br/><span style="color: #67c23a;">邀请码有效期为30天</span>`,
          '批量生成邀请码',
          {
            confirmButtonText: '确定生成',
            cancelButtonText: '取消',
            type: 'success',
            dangerouslyUseHTMLString: true
          }
        )
        
        if (!result) return
        
        // 显示加载提示
        const loading = this.$loading({
          lock: true,
          text: `正在生成邀请码...`,
          background: 'rgba(0, 0, 0, 0.7)'
        })
        
        try {
          // 🔥 调用后端API生成邀请码
          // 获取学生ID列表
          const studentIds = studentsWithoutCode.map(s => s.id)
          
          const response = await generateInviteCodes(studentIds)
          
          console.log('📥 邀请码生成结果:', response)
          
          // 关闭加载
          loading.close()
          
          if (response.code === 200) {
            const result = response.data
            
            // 显示结果
            if (result.failureCount > 0) {
              this.$message.warning(
                `成功生成 ${result.successCount} 个邀请码，跳过 ${result.skippedCount} 个（已有有效邀请码），失败 ${result.failureCount} 个`
              )
            } else {
              this.$message.success(
                `成功生成 ${result.successCount} 个邀请码！${result.skippedCount > 0 ? `（跳过 ${result.skippedCount} 个已有邀请码的学生）` : ''}`
              )
            }
            
            // 刷新学生列表
            await this.loadStudentsFromBackend()
            
            this.logOperation(`批量生成邀请码 ${result.successCount} 个`, '学生信息管理')
          } else {
            this.$message.error(response.message || '生成邀请码失败')
          }
          
        } catch (error) {
          loading.close()
          console.error('❌ 生成邀请码失败:', error)
          this.$message.error('生成邀请码失败：' + (error.message || '网络错误'))
        }
        
      } catch (error) {
        if (error === 'cancel') {
          // 用户取消
          return
        }
        console.error('批量生成邀请码失败:', error)
        this.$message.error('批量生成邀请码失败')
      }
    },
    
    // 全选所有页数据
    selectAllPagesData() {
      this.selectAllPages = true
      this.selectedStudents = [] // 清空当前页选择
      this.$message.success(`已选择全部 ${this.filteredStudentsList.length} 条数据`)
    },
    
    // 取消全选
    cancelSelectAll() {
      this.selectAllPages = false
      this.selectedStudents = []
      this.$message.info('已取消全选')
    },
    
    // 筛选学生
    filterStudents(filterType) {
      // 如果点击的是当前已激活的筛选，则取消筛选
      if (this.currentFilter === filterType) {
        this.currentFilter = 'all'
        this.studentsCurrentPage = 1
        // 筛选变化时取消全选
        this.selectAllPages = false
        this.selectedStudents = []
        return
      }
      
      // 否则应用新的筛选
      this.currentFilter = filterType
      this.studentsCurrentPage = 1 // 重置到第一页
      // 筛选变化时取消全选
      this.selectAllPages = false
      this.selectedStudents = []
    },
    
    // 清除筛选
    clearFilter() {
      this.currentFilter = 'all'
      this.studentsCurrentPage = 1
      // 清除筛选时取消全选
      this.selectAllPages = false
      this.selectedStudents = []
    },
    
    // 获取筛选标签文本
    getFilterLabel(filterType) {
      const labels = {
        'male': '男生',
        'female': '女生', 
        'registered': '已注册',
        'unregistered': '未注册'
      }
      return labels[filterType] || filterType
    },
    
    // 导入数据
    async importData() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择Excel文件')
        return
      }
      
      if (!this.previewData || !Array.isArray(this.previewData)) {
        this.$message.warning('请先预览数据')
        return
      }
      
      const validData = this.previewData.filter(item => item.isValid)
      
      if (validData.length === 0) {
        this.$message.warning('没有有效的数据可以导入')
        return
      }
      
      this.isImporting = true
      
      try {
        // 🔥 调用真正的后端API导入
        const response = await importStudents(this.selectedFile)
        
        console.log('📥 后端返回结果:', response)
        
        if (response.code === 200) {
          const result = response.data
          
          // 显示导入结果
          if (result.failureCount > 0) {
            // 有失败记录
            let errorMsg = `成功导入 ${result.successCount} 条，失败 ${result.failureCount} 条\n`
            if (result.errors && result.errors.length > 0) {
              errorMsg += '\n失败原因：\n'
              result.errors.slice(0, 5).forEach(err => {
                errorMsg += `- ${err.studentId || '第' + err.rowIndex + '行'}: ${err.errorMessage}\n`
              })
              if (result.errors.length > 5) {
                errorMsg += `... 还有 ${result.errors.length - 5} 条错误`
              }
            }
            this.$alert(errorMsg, '导入结果', {
              type: 'warning',
              confirmButtonText: '我知道了'
            })
          } else {
            // 全部成功
            this.$message.success(`成功导入 ${result.successCount} 条学生信息！`)
          }
          
          // 清空预览数据和文件
          this.previewData = []
          this.fileList = []
          this.selectedFile = null
          
          // 刷新学生列表
          await this.loadStudentsFromBackend()
          
          this.logOperation(`导入学生信息 ${result.successCount} 条`, '学生信息管理')
        } else {
          this.$message.error(response.message || '导入失败')
        }
        
      } catch (error) {
        console.error('❌ 导入失败:', error)
        this.$message.error('导入失败：' + (error.message || '网络错误'))
      } finally {
        this.isImporting = false
      }
    },
    
    // 下载模板
    downloadTemplate() {
      const templateData = [
        {
          '学号': '2023001001',
          '姓名': '张三',
          '性别': '男',
          '录取专业': '计算机科学与技术',
          '年级': '2023',
          '班级': '计科2301',
          '邀请码': '（留空自动生成）'
        },
        {
          '学号': '2023001002',
          '姓名': '李四',
          '性别': '女',
          '录取专业': '软件工程',
          '年级': '2023',
          '班级': '软工2301',
          '邀请码': '（留空自动生成）'
        },
        {
          '学号': '2023001003',
          '姓名': '王五',
          '性别': '男',
          '录取专业': '数据科学与大数据技术',
          '年级': '2023',
          '班级': '数据2301',
          '邀请码': '（留空自动生成）'
        }
      ]
      
      const ws = XLSX.utils.json_to_sheet(templateData)
      
      // 设置列宽
      ws['!cols'] = [
        { wch: 12 },  // 学号
        { wch: 10 },  // 姓名
        { wch: 6 },   // 性别
        { wch: 25 },  // 录取专业
        { wch: 8 },   // 年级
        { wch: 15 },  // 班级
        { wch: 18 }   // 邀请码
      ]
      
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, '学生信息模板')
      
      XLSX.writeFile(wb, '学生信息导入模板.xlsx')
      this.$message.success('模板下载成功')
      this.logOperation('下载Excel模板', '学生信息管理')
    },
    
    // 导出学生列表
    exportStudentsList() {
      if (!this.studentsList || this.studentsList.length === 0) {
        this.$message.warning('暂无数据可导出')
        return
      }
      
      const exportData = this.studentsList.map(student => ({
        '学号': student.studentId,
        '姓名': student.realName,
        '性别': student.gender,
        '录取专业': student.major,
        '年级': student.grade,
        '班级': student.className,
        '邀请码': student.inviteCode || '未生成',
        '邀请码状态': this.getInviteCodeStatusText(student),
        '邀请码有效期': student.inviteCodeExpiry ? this.formatInviteCodeExpiry(student.inviteCodeExpiry) : (student.inviteCode ? '无期限' : '-'),
        '注册状态': student.isRegistered ? '已注册' : '未注册',
        '注册时间': student.registrationDate ? this.formatRegistrationDate(student.registrationDate) : '未注册'
      }))
      
      const ws = XLSX.utils.json_to_sheet(exportData)
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, '学生列表')
      
      const fileName = `学生列表_${new Date().toISOString().slice(0, 10)}.xlsx`
      XLSX.writeFile(wb, fileName)
      
      this.$message.success('学生列表导出成功')
      this.logOperation('导出学生列表', '学生信息管理')
    },
    
    // 处理表格选择变化
    handleSelectionChange(selection) {
      this.selectedStudents = selection || []
    },
    
    // 查看学生详情
    viewStudentDetail(student) {
      const inviteCodeStatus = this.getInviteCodeStatus(student)
      const inviteCodeStatusText = this.getInviteCodeStatusText(student)
      
      let inviteCodeStatusColor = '#67c23a' // 默认绿色
      if (inviteCodeStatus === 'not_generated') {
        inviteCodeStatusColor = '#e6a23c' // 橙色
      } else if (inviteCodeStatus === 'used') {
        inviteCodeStatusColor = '#909399' // 灰色
      } else if (inviteCodeStatus === 'expired') {
        inviteCodeStatusColor = '#f56c6c' // 红色
      }
      
      const inviteCodeDisplay = student.inviteCode 
        ? `<code style="background: #f4f4f5; padding: 2px 8px; border-radius: 3px; font-size: 14px;">${student.inviteCode}</code>`
        : '<span style="color: #e6a23c; font-weight: 600;">未生成</span>'
      
      // 获取宿舍位置信息
      let dormInfo = '<span style="color: #909399;">未分配</span>'
      if (student.dormLocation) {
        dormInfo = `<span style="color: #409eff; font-weight: 600;">${student.dormLocation}</span>`
      }
      
      // 获取联系方式
      const email = student.email || '<span style="color: #909399;">未填写</span>'
      const phone = student.phone || '<span style="color: #909399;">未填写</span>'
      
      // 性别直接使用中文（后端已返回中文）
      const genderText = student.gender || '-'
      
      const detailHtml = `
        <div style="text-align: left; padding: 10px; line-height: 2;">
          <p><strong>学号：</strong>${student.studentId}</p>
          <p><strong>姓名：</strong>${student.realName}</p>
          <p><strong>性别：</strong>${genderText}</p>
          <p><strong>录取专业：</strong>${student.major}</p>
          <p><strong>年级：</strong>${student.grade}</p>
          <p><strong>班级：</strong>${student.className}</p>
          <hr style="border: none; border-top: 1px dashed #dcdfe6; margin: 12px 0;">
          <p><strong>宿舍位置：</strong>${dormInfo}</p>
          <p><strong>邮箱：</strong>${email}</p>
          <p><strong>联系电话：</strong>${phone}</p>
          <hr style="border: none; border-top: 1px dashed #dcdfe6; margin: 12px 0;">
          <p><strong>邀请码：</strong>${inviteCodeDisplay}</p>
          <p><strong>邀请码状态：</strong><span style="color: ${inviteCodeStatusColor}; font-weight: 600;">${inviteCodeStatusText}</span></p>
          ${student.inviteCodeExpiry ? `<p><strong>邀请码有效期：</strong>${this.formatInviteCodeExpiry(student.inviteCodeExpiry)}</p>` : ''}
          <hr style="border: none; border-top: 1px dashed #dcdfe6; margin: 12px 0;">
          <p><strong>注册状态：</strong>${student.isRegistered ? '<span style="color: #67c23a;">已注册</span>' : '<span style="color: #909399;">未注册</span>'}</p>
          ${student.registrationDate ? `<p><strong>注册时间：</strong>${this.formatRegistrationDate(student.registrationDate)}</p>` : ''}
        </div>
      `
      
      this.$alert(detailHtml, '学生详细信息', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭',
        customClass: 'student-detail-dialog'
      })
    },
    
    // 删除单个学生
    async deleteStudent(student) {
      try {
        // 🔥 调用后端API删除
        const response = await deleteStudentApi(student.id)
        
        if (response.code === 200) {
          this.$message.success('删除成功')
          
          // 刷新学生列表
          await this.loadStudentsFromBackend()
          
          this.selectedStudents = []
          this.logOperation(`删除学生信息：${student.realName}(${student.studentId})`, '学生信息管理')
        } else {
          this.$message.error(response.message || '删除失败')
        }
      } catch (error) {
        console.error('❌ 删除学生失败:', error)
        this.$message.error('删除失败：' + (error.message || '网络错误'))
      }
    },
    
    // 批量删除学生
    async batchDeleteStudents() {
      let studentsToDelete = []
      let deleteCount = 0
      
      if (this.selectAllPages) {
        // 删除所有筛选后的数据
        studentsToDelete = this.filteredStudentsList
        deleteCount = studentsToDelete.length
      } else {
        // 删除当前页选中的数据
        if (!this.selectedStudents || this.selectedStudents.length === 0) {
          this.$message.warning('请先选择要删除的学生')
          return
        }
        studentsToDelete = this.selectedStudents
        deleteCount = studentsToDelete.length
      }
      
      const studentNames = studentsToDelete.slice(0, 3).map(s => s.realName).join('、')
      const displayText = deleteCount > 3 
        ? `${studentNames} 等 ${deleteCount} 名学生` 
        : studentNames
      
      const confirmMessage = this.selectAllPages
        ? `确定要删除所有筛选结果中的 <strong>${deleteCount}</strong> 名学生的信息吗？<br/><span style="color: #f56c6c;">此操作不可恢复！</span>`
        : `确定要删除 ${displayText} 的信息吗？<br/><span style="color: #f56c6c;">此操作不可恢复！</span>`
      
      try {
        await this.$confirm(
          confirmMessage, 
          '批量删除确认', 
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
            dangerouslyUseHTMLString: true
          }
        )
        
        // 🔥 调用后端API批量删除
        const studentIds = studentsToDelete.map(s => s.id)
        
        const loading = this.$loading({
          lock: true,
          text: '正在删除...',
          background: 'rgba(0, 0, 0, 0.7)'
        })
        
        try {
          const response = await batchDeleteStudentsApi(studentIds)
          
          loading.close()
          
          if (response.code === 200) {
            this.$message.success(`成功删除 ${deleteCount} 名学生的信息`)
            
            // 刷新学生列表
            await this.loadStudentsFromBackend()
            
            this.selectedStudents = []
            this.selectAllPages = false
            
            this.logOperation(`批量删除学生信息 ${deleteCount} 条`, '学生信息管理')
          } else {
            this.$message.error(response.message || '删除失败')
          }
        } catch (error) {
          loading.close()
          console.error('❌ 批量删除失败:', error)
          this.$message.error('删除失败：' + (error.message || '网络错误'))
        }
        
      } catch (error) {
        // 用户取消
        if (error !== 'cancel') {
          console.error('删除操作失败:', error)
        }
      }
    },
    
    // 分页处理方法
    handlePreviewPageChange(page) {
      this.previewCurrentPage = page
    },
    
    handleStudentsPageChange(page) {
      this.studentsCurrentPage = page
      // 翻页时清空当前页选择（但保留全选所有页的状态）
      if (!this.selectAllPages) {
        this.selectedStudents = []
      }
    },
    
    handleStudentsPageSizeChange(size) {
      this.studentsPageSize = size
      this.studentsCurrentPage = 1
      // 改变每页数量时清空当前页选择（但保留全选所有页的状态）
      if (!this.selectAllPages) {
        this.selectedStudents = []
      }
    }
  }
}
</script>

<style scoped>
.student-management {
  padding: 0;
}

.page-header {
  margin-bottom: 32px;
  padding-top: 8px;
  background: #fafbfc;
  border-radius: 8px;
  padding: 20px 24px;
  border-left: 3px solid #409eff;
}

.page-header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-icon {
  font-size: 40px;
  color: #409eff;
  flex-shrink: 0;
  opacity: 0.85;
}

.page-title-group {
  flex: 1;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
}

.page-description {
  color: #909399;
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
  color: #409eff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-count {
  color: #909399;
  font-size: 14px;
  font-weight: normal;
}

/* 导入卡片样式 */
.import-card {
  height: fit-content;
}

.import-description {
  margin-bottom: 24px;
}

:deep(.import-description .el-alert__content) {
  width: 100%;
}

.alert-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.import-info-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 12px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 10px;
  border: 2px solid #e1e8f0;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.info-item:hover {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #409eff;
  transform: translateX(3px) translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.info-icon {
  font-size: 26px;
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.info-content {
  flex: 1;
}

.info-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
  font-weight: 600;
}

.info-value {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  font-weight: 500;
}

.upload-demo {
  margin-bottom: 20px;
}

/* 已上传文件卡片样式 */
.uploaded-file-card {
  margin: 20px 0;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.file-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: white;
}

.file-icon {
  font-size: 48px;
  flex-shrink: 0;
  animation: bounce 0.6s ease-in-out;
}

@keyframes bounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.file-icon.csv-file {
  color: #4ade80;
  filter: drop-shadow(0 2px 4px rgba(74, 222, 128, 0.4));
}

.file-icon.excel-file {
  color: #10b981;
  filter: drop-shadow(0 2px 4px rgba(16, 185, 129, 0.4));
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin-bottom: 4px;
  word-break: break-all;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.file-size {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* 删除按钮样式 */
.delete-file-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  border: 3px solid #ff4d4f;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
}

.delete-file-btn:hover {
  background: #ff4d4f;
  transform: scale(1.15) rotate(90deg);
  box-shadow: 0 4px 16px rgba(255, 77, 79, 0.5);
}

.delete-file-btn:active {
  transform: scale(1.05) rotate(90deg);
}

.delete-icon {
  font-size: 28px;
  font-weight: bold;
  color: #ff4d4f;
  transition: color 0.3s ease;
}

.delete-file-btn:hover .delete-icon {
  color: white;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 统计卡片样式 */
.stats-card {
  height: fit-content;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.stat-number.text-primary {
  color: #409eff;
}

.stat-number.text-danger {
  color: #f56c6c;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

/* 预览和列表卡片样式 */
.preview-card,
.students-list-card {
  margin-bottom: 24px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 表格提示信息 */
.table-tips {
  margin-top: 16px;
  margin-bottom: 0;
}

.table-tips :deep(.el-alert) {
  border-radius: 4px;
  padding: 10px 16px;
}

.table-tips :deep(.el-alert__title) {
  font-size: 14px;
  color: #606266;
}

.table-tips strong {
  color: #409eff;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
  
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .action-buttons {
    flex-direction: column;
  }
}

/* Element Plus 组件样式调整 */
:deep(.el-upload-dragger) {
  border: 3px dashed #409eff;
  border-radius: 12px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  transition: all 0.3s;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

:deep(.el-upload-dragger::before) {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.1) 0%, transparent 70%);
  animation: pulse 3s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
}

:deep(.el-upload-dragger:hover) {
  border-color: #0066cc;
  background: linear-gradient(135deg, #e0f2fe 0%, #bfdbfe 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.2);
}

:deep(.el-icon--upload) {
  font-size: 80px !important;
  color: #409eff !important;
  margin-bottom: 16px;
}

:deep(.el-upload__text) {
  font-size: 16px !important;
  color: #303133 !important;
  font-weight: 500 !important;
}

:deep(.el-upload__text em) {
  color: #409eff !important;
  font-weight: 600 !important;
}

:deep(.el-upload__tip) {
  margin-top: 12px !important;
  font-size: 14px !important;
  color: #606266 !important;
  font-weight: 500 !important;
}

/* 文本样式 */
.text-muted {
  color: #909399;
  font-style: italic;
}

/* 可点击的统计卡片样式 */
.stat-item.clickable {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  position: relative;
}

.stat-item.clickable:hover {
  background: rgba(64, 158, 255, 0.08);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  border: 1px solid rgba(64, 158, 255, 0.3);
}

.stat-item.clickable:hover .stat-number {
  color: #409eff !important;
  font-weight: 600;
}

.stat-item.clickable:hover .stat-label {
  color: #303133;
  font-weight: 500;
}

.stat-item.clickable.active {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.stat-item.clickable.active .stat-number {
  color: white !important;
}

.stat-item.clickable.active .stat-label {
  color: white;
}

.stat-item.clickable::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 4px solid currentColor;
  opacity: 0;
  transition: all 0.3s ease;
}

.stat-item.clickable:hover::after {
  opacity: 0.6;
}

.stat-item.clickable.active::after {
  opacity: 0.8;
  border-top: none;
  border-bottom: 4px solid white;
  transform: translateY(-50%) rotate(0deg);
}

/* 搜索和筛选组合 */
.search-filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-tag {
  font-size: 12px;
  white-space: nowrap;
}

.action-button-group {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

/* 全选按钮强调样式 */
.action-button-group .el-button--warning.is-plain {
  border-color: #e6a23c;
  color: #e6a23c;
  background-color: #fdf6ec;
}

.action-button-group .el-button--warning.is-plain:hover {
  background-color: #e6a23c;
  border-color: #e6a23c;
  color: #fff;
}

:deep(.el-card__body) {
  padding: 24px;
}

/* 表格容器样式 - 支持横向滚动 */
.table-container {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
}

/* 表格样式优化 */
:deep(.el-table) {
  min-width: 100%;
}

/* 表格操作按钮间距 */
:deep(.el-table .el-button + .el-popconfirm) {
  margin-left: 8px;
}

/* 滚动条美化 */
.table-container::-webkit-scrollbar {
  height: 8px;
}

.table-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.table-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 学生详情弹窗样式 */
:deep(.student-detail-dialog) {
  width: 500px;
}

:deep(.student-detail-dialog .el-message-box__message) {
  padding: 0;
}

:deep(.student-detail-dialog code) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}
</style>
