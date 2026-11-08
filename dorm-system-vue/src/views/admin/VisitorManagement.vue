<template>
  <div class="visitor-management-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>访客管理</h2>
          <el-button type="primary" @click="showAddVisitorDialog">添加访客</el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="搜索访客姓名或身份证号" style="width: 250px;" />
        <el-select v-model="filterStatus" placeholder="选择状态" style="width: 150px;">
          <el-option label="全部状态" value="" />
          <el-option label="待审核" value="待审核" />
          <el-option label="已通过" value="已通过" />
          <el-option label="已拒绝" value="已拒绝" />
          <el-option label="已登记" value="已登记" />
          <el-option label="已离开" value="已离开" />
        </el-select>
        <el-date-picker
          v-model="filterDate"
          type="date"
          placeholder="选择日期"
          style="width: 150px;"
        />
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      
      <!-- 访客记录 -->
      <el-table :data="filteredVisitors" style="width: 100%;">
        <el-table-column prop="name" label="访客姓名" min-width="100" align="center" />
        <el-table-column prop="idCard" label="身份证号" min-width="160" align="center" />
        <el-table-column prop="phone" label="联系电话" min-width="120" align="center" />
        <el-table-column prop="visitedStudent" label="被访学生" min-width="100" align="center" />
        <el-table-column prop="visitDorm" label="访问宿舍" min-width="90" align="center" />
        <el-table-column prop="visitTime" label="来访时间" min-width="140" align="center" />
        <el-table-column prop="purpose" label="来访目的" min-width="100" align="center" />
        <el-table-column prop="status" label="状态" min-width="90" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
                 <el-table-column label="操作" min-width="200" align="center">
           <template #default="scope">
             <div class="action-buttons-grid">
               <div class="button-row">
                 <el-button size="small" type="primary" @click="viewDetails(scope.row)">查看详情</el-button>
                 <el-button 
                   v-if="scope.row.status === '待审核'" 
                   size="small" 
                   type="success" 
                   @click="approveAppointment(scope.row)"
                 >
                   通过
                 </el-button>
                 <el-button 
                   v-if="scope.row.status === '已通过'" 
                   size="small" 
                   type="success" 
                   @click="checkIn(scope.row)"
                 >
                   登记
                 </el-button>
                 <el-button 
                   v-if="scope.row.status === '已登记'" 
                   size="small" 
                   type="warning" 
                   @click="checkOut(scope.row)"
                 >
                   离校
                 </el-button>
               </div>
               <div class="button-row">
                 <el-button 
                   size="small" 
                   type="danger" 
                   @click="deleteVisitor(scope.row)"
                 >
                   删除
                 </el-button>
                 <el-button 
                   v-if="scope.row.status === '待审核'" 
                   size="small" 
                   type="warning" 
                   @click="rejectAppointment(scope.row)"
                 >
                   拒绝
                 </el-button>
                 <div v-else class="empty-space"></div>
               </div>
             </div>
           </template>
         </el-table-column>
      </el-table>
    </el-card>

    <!-- 访客详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="访客详情" width="700px">
      <el-descriptions :column="2" border v-if="currentVisitor">
        <el-descriptions-item label="访客姓名">{{ currentVisitor.name }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentVisitor.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentVisitor.phone }}</el-descriptions-item>
        <el-descriptions-item label="被访学生">{{ currentVisitor.visitedStudent }}</el-descriptions-item>
        <el-descriptions-item label="访问宿舍">{{ currentVisitor.visitDorm }}</el-descriptions-item>
        <el-descriptions-item label="来访时间">{{ currentVisitor.visitTime }}</el-descriptions-item>
        <el-descriptions-item label="来访目的">{{ currentVisitor.purpose }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentVisitor.status)">
            {{ currentVisitor.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ currentVisitor.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ currentVisitor.approvedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="登记时间">{{ currentVisitor.checkedInAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="离开时间">{{ currentVisitor.checkedOutAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentVisitor.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 添加访客对话框 -->
    <el-dialog v-model="addVisitorDialogVisible" title="添加访客" width="600px">
      <el-form :model="visitorForm" label-width="100px" :rules="visitorRules" ref="visitorFormRef">
        <el-form-item label="访客姓名" prop="name">
          <el-input v-model="visitorForm.name" placeholder="请输入访客姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="visitorForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="visitorForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="来访时间" prop="visitTime">
          <el-date-picker
            v-model="visitorForm.visitTime"
            type="datetime"
            placeholder="选择来访时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="来访目的" prop="purpose">
          <el-select v-model="visitorForm.purpose" placeholder="选择来访目的" style="width: 100%">
            <el-option label="探访同学" value="探访同学" />
            <el-option label="送物品" value="送物品" />
            <el-option label="办理事务" value="办理事务" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="被访学生" prop="studentName">
          <el-input 
            v-model="visitorForm.studentName" 
            placeholder="请输入被访学生姓名或学号"
            @blur="searchStudent"
          />
        </el-form-item>
        <el-form-item label="访问楼栋" prop="buildingId">
          <el-select v-model="visitorForm.buildingId" placeholder="选择访问楼栋" style="width: 100%" @change="onBuildingChange" filterable>
            <el-option 
              v-for="building in buildings" 
              :key="building.id" 
              :label="building.name || building.buildingName" 
              :value="building.id" 
            />
          </el-select>
          <div v-if="visitorForm.buildingId" class="building-hint">
            已选择: {{ getBuildingName(visitorForm.buildingId) }}
          </div>
        </el-form-item>
        <el-form-item label="访问宿舍" prop="visitDorm">
          <el-input v-model="visitorForm.visitDorm" placeholder="请输入访问宿舍号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="visitorForm.remark" type="textarea" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addVisitorDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveVisitor">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 登记对话框 -->
    <el-dialog v-model="checkInDialogVisible" title="访客登记" width="500px">
      <el-form :model="checkInForm" label-width="100px">
        <el-form-item label="访客姓名">
          <el-input v-model="checkInForm.name" disabled />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="checkInForm.idCard" disabled />
        </el-form-item>
        <el-form-item label="登记备注">
          <el-input v-model="checkInForm.remark" type="textarea" placeholder="请输入登记备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="checkInDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCheckIn">确认登记</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getVisitorList,
  createVisitor,
  approveVisitor,
  checkInVisitor as checkInVisitorAPI,
  checkOutVisitor as checkOutVisitorAPI,
  deleteVisitor as deleteVisitorAPI
} from '@/api/visitor.js'

export default {
  name: 'VisitorManagement',
  data() {
    return {
      // 搜索和筛选
      searchKeyword: '',
      filterStatus: '',
      filterDate: null,
      
      // 访客数据
      visitors: [],
      loading: false,
      currentPage: 1,
      pageSize: 20,
      total: 0,
      
      // 对话框状态
      detailDialogVisible: false,
      addVisitorDialogVisible: false,
      checkInDialogVisible: false,
      
      // 当前访客
      currentVisitor: null,
      
      // 楼栋列表
      buildings: [],
      
      // 表单数据
      visitorForm: {
        name: '',
        idCard: '',
        phone: '',
        visitTime: null,
        purpose: '',
        studentName: '',
        studentNumber: '',
        studentId: null,
        buildingId: null,
        visitDorm: '',
        remark: ''
      },
      
             checkInForm: {
         name: '',
         idCard: '',
         remark: ''
       },
      
      // 表单验证规则
      visitorRules: {
        name: [
          { required: true, message: '请输入访客姓名', trigger: 'blur' }
        ],
        idCard: [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        visitTime: [
          { required: true, message: '请选择来访时间', trigger: 'change' }
        ],
        purpose: [
          { required: true, message: '请选择来访目的', trigger: 'change' }
        ],
        studentName: [
          { required: true, message: '请输入被访学生姓名', trigger: 'blur' }
        ],
        visitDorm: [
          { required: true, message: '请输入访问宿舍', trigger: 'blur' }
        ],
        buildingId: [
          { required: true, message: '请选择访问楼栋', trigger: 'change' }
        ]
      }
    }
  },
  mounted() {
    this.loadVisitors()
    this.loadBuildings()
  },
  computed: {
    filteredVisitors() {
      // 根据搜索条件过滤
      let result = this.visitors
      
      if (this.searchKeyword) {
        const keyword = this.searchKeyword.toLowerCase()
        result = result.filter(v => 
          v.name?.toLowerCase().includes(keyword) ||
          v.idCard?.includes(keyword)
        )
      }
      
      if (this.filterStatus) {
        result = result.filter(v => v.status === this.filterStatus)
      }
      
      if (this.filterDate) {
        const filterDateStr = this.filterDate.toLocaleDateString()
        result = result.filter(v => 
          new Date(v.visitTime).toLocaleDateString() === filterDateStr
        )
      }
      
      return result
    }
  },
  methods: {
    // 加载楼栋列表
    async loadBuildings() {
      try {
        const { getMyBuildings } = await import('@/api/adminDormManagement.js')
        const res = await getMyBuildings()
        if (res.code === 200 && res.data) {
          this.buildings = res.data
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
      }
    },
    
    // 楼栋变化时的处理
    onBuildingChange(buildingId) {
      // 可以在这里根据楼栋加载宿舍列表等
      console.log('选择楼栋:', buildingId, '楼栋名称:', this.getBuildingName(buildingId))
    },
    
    // 根据楼栋ID获取楼栋名称
    getBuildingName(buildingId) {
      const building = this.buildings.find(b => b.id === buildingId)
      return building ? (building.name || building.buildingName) : ''
    },
    
    async loadVisitors() {
      try {
        this.loading = true
        const params = {
          currentPage: this.currentPage,
          pageSize: this.pageSize
        }
        
        if (this.searchKeyword) params.keyword = this.searchKeyword
        if (this.filterStatus) params.status = this.filterStatus
        
        const res = await getVisitorList(params)
        
        if (res.code === 200) {
          // 转换后端数据字段名为前端使用的字段名
          const rawData = res.data.records || res.data || []
          this.visitors = rawData.map(item => ({
            id: item.id,
            name: item.visitorName || item.visitor_name || '',
            idCard: item.visitorIdCard || item.visitor_id_card || '',
            phone: item.visitorPhone || item.visitor_phone || '',
            visitedStudent: item.studentName || item.student_name || '',
            visitDorm: item.studentRoom || item.student_room || '',
            visitTime: this.formatVisitTime(item),
            purpose: item.visitReason || item.visit_reason || '',
            status: this.mapStatus(item.status),
            // 时间字段
            createdAt: item.createdAt || item.created_at || '',
            approvedAt: item.approvedAt || item.approved_at || '',
            checkedInAt: item.checkedInAt || item.checked_in_at || '',
            checkedOutAt: item.checkedOutAt || item.checked_out_at || '',
            remark: item.remarks || item.remark || '',
            // 保留原始数据
            rawData: item
          }))
          this.total = res.data.total || this.visitors.length
          
          console.log('访客数据加载成功:', this.visitors)
        }
      } catch (error) {
        console.error('加载访客列表失败:', error)
        this.$message.error('加载访客列表失败')
      } finally {
        this.loading = false
      }
    },
    
    // 映射后端状态到前端状态
    mapStatus(status) {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'CHECKED_IN': '已登记',
        'COMPLETED': '已离开',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status
    },
    
    // 格式化来访时间
    formatVisitTime(item) {
      const date = item.visitDate || item.visit_date || ''
      const time = item.visitTime || item.visit_time || ''
      
      if (date && time) {
        return `${date} ${time}`
      } else if (time) {
        return time
      } else if (date) {
        return date
      }
      return ''
    },
    
    // 搜索功能
    search() {
      // 搜索逻辑已在computed中实现
    },
    
    resetSearch() {
      this.searchKeyword = ''
      this.filterStatus = ''
      this.filterDate = null
    },
    
    // 查看详情
    viewDetails(visitor) {
      this.currentVisitor = visitor
      this.detailDialogVisible = true
    },
    
    // 添加访客
    showAddVisitorDialog() {
      this.visitorForm = {
        name: '',
        idCard: '',
        phone: '',
        visitTime: null,
        purpose: '',
        studentName: '',
        studentNumber: '',
        studentId: null,
        buildingId: null,
        visitDorm: '',
        remark: ''
      }
      this.addVisitorDialogVisible = true
    },
    
    // 搜索学生
    async searchStudent() {
      if (!this.visitorForm.studentName || this.visitorForm.studentName.trim() === '') {
        return
      }
      
      try {
        // 尝试通过学号查找
        const studentId = this.visitorForm.studentName.trim()
        if (/^\d+$/.test(studentId)) {
          // 如果是纯数字，尝试作为学号查询
          const { getStudentByStudentId } = await import('@/api/student.js')
          const res = await getStudentByStudentId(studentId)
          if (res.code === 200 && res.data) {
            this.visitorForm.studentId = res.data.id
            this.visitorForm.studentName = res.data.realName
            this.visitorForm.studentNumber = res.data.studentId
            // 如果有宿舍信息，自动填充
            if (res.data.room) {
              this.visitorForm.visitDorm = res.data.room
            }
          }
        }
      } catch (error) {
        // 如果查询失败，保持用户输入的学生姓名
        console.log('学生查询失败，将使用手动输入的学生姓名')
      }
    },
    
    async saveVisitor() {
      this.$refs.visitorFormRef.validate(async (valid) => {
        if (valid) {
          try {
            // 验证必填字段
            if (!this.visitorForm.studentName || this.visitorForm.studentName.trim() === '') {
              this.$message.warning('请填写被访学生姓名')
              return
            }
            
            // 转换表单数据为后端期望的格式
            const formData = {
              visitorName: this.visitorForm.name,
              visitorPhone: this.visitorForm.phone,
              visitorIdCard: this.visitorForm.idCard || null,
              visitReason: this.visitorForm.purpose,
              visitDate: null, // 需要从 visitTime 中提取日期
              visitTime: null, // 需要从 visitTime 中提取时间
              buildingId: this.visitorForm.buildingId,
              studentRoom: this.visitorForm.visitDorm || null,
              studentName: this.visitorForm.studentName.trim(),
              remarks: this.visitorForm.remark || null
            }
            
            // 添加学生信息（如果有）
            if (this.visitorForm.studentId) {
              formData.studentId = this.visitorForm.studentId
            }
            if (this.visitorForm.studentNumber) {
              formData.studentNumber = this.visitorForm.studentNumber
            }
            
            // 处理日期时间：将 datetime 拆分为 date 和 time
            if (this.visitorForm.visitTime) {
              const visitDateTime = new Date(this.visitorForm.visitTime)
              // 格式化为 yyyy-MM-dd
              const year = visitDateTime.getFullYear()
              const month = String(visitDateTime.getMonth() + 1).padStart(2, '0')
              const day = String(visitDateTime.getDate()).padStart(2, '0')
              formData.visitDate = `${year}-${month}-${day}`
              
              // 格式化为 HH:mm
              const hours = String(visitDateTime.getHours()).padStart(2, '0')
              const minutes = String(visitDateTime.getMinutes()).padStart(2, '0')
              formData.visitTime = `${hours}:${minutes}`
            }
            
            console.log('提交访客数据:', formData)
            console.log('选择的楼栋:', this.getBuildingName(this.visitorForm.buildingId))
            
            const res = await createVisitor(formData)
            if (res.code === 200) {
              this.$message.success('访客添加成功')
              this.addVisitorDialogVisible = false
              this.loadVisitors()
            }
            // 注意：错误情况由响应拦截器统一处理，这里不需要再显示
          } catch (error) {
            console.error('添加访客失败:', error)
            // 响应拦截器已经显示了错误消息，这里不需要再显示
          }
        }
      })
    },
    
    // 审核通过
    approveAppointment(appointment) {
      this.$confirm(`确定要通过${appointment.name}的访客预约吗？`, '确认通过', {
        type: 'success',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          const visitorId = appointment.rawData?.id || appointment.id
          const res = await approveVisitor(visitorId, {
            approved: true,
            adminComment: '审核通过'
          })
          if (res.code === 200) {
            this.$message.success('预约已通过')
            await this.loadVisitors()
          }
        } catch (error) {
          console.error('审核通过失败:', error)
          // 即使失败也刷新列表
          await this.loadVisitors()
          // 响应拦截器已经显示了错误消息
        }
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 审核拒绝
    rejectAppointment(appointment) {
      this.$confirm(`确定要拒绝${appointment.name}的访客预约吗？`, '确认拒绝', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          const visitorId = appointment.rawData?.id || appointment.id
          // 使用 approveVisitor 并传递 approved: false 来拒绝
          const res = await approveVisitor(visitorId, {
            approved: false,
            adminComment: '拒绝预约',
            rejectReason: '管理员拒绝'
          })
          if (res.code === 200) {
            this.$message.success('预约已拒绝')
            await this.loadVisitors()
          }
        } catch (error) {
          console.error('拒绝访客失败:', error)
          // 即使失败也刷新列表
          await this.loadVisitors()
          // 响应拦截器已经显示了错误消息
        }
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 登记
    checkIn(visitor) {
      this.currentVisitor = visitor
      this.checkInForm = {
        name: visitor.name,
        idCard: visitor.idCard,
        remark: ''
      }
      this.checkInDialogVisible = true
    },
    
    async confirmCheckIn() {
      try {
        // 使用 rawData 中的原始 ID，确保正确
        const visitorId = this.currentVisitor.rawData?.id || this.currentVisitor.id
        const res = await checkInVisitorAPI(visitorId)
        if (res.code === 200) {
          this.$message.success('登记成功')
          this.checkInDialogVisible = false
          // 刷新列表以更新状态
          await this.loadVisitors()
        }
      } catch (error) {
        console.error('登记失败:', error)
        // 即使失败也刷新列表，因为可能状态已经改变
        this.checkInDialogVisible = false
        await this.loadVisitors()
        // 响应拦截器已经显示了错误消息
      }
    },
    
    // 离校
    checkOut(visitor) {
      this.$confirm(`确定要为${visitor.name}办理离校手续吗？`, '确认离校', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          const visitorId = visitor.rawData?.id || visitor.id
          const res = await checkOutVisitorAPI(visitorId)
          if (res.code === 200) {
            this.$message.success('离校成功')
            await this.loadVisitors()
          }
        } catch (error) {
          console.error('离校失败:', error)
          // 即使失败也刷新列表
          await this.loadVisitors()
          // 响应拦截器已经显示了错误消息
        }
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    // 删除
    deleteVisitor(visitor) {
      this.$confirm(`确定要删除访客"${visitor.name}"吗？`, '确认删除', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(async () => {
        try {
          const visitorId = visitor.rawData?.id || visitor.id
          console.log('删除访客，ID:', visitorId, '原始数据:', visitor)
          
          const res = await deleteVisitorAPI(visitorId)
          console.log('删除响应:', res)
          
          if (res.code === 200) {
            this.$message.success('删除成功')
            // 重置到第一页，确保能看到更新后的数据
            this.currentPage = 1
            // 强制刷新列表
            await this.loadVisitors()
          } else {
            console.error('删除失败，响应码:', res.code, '消息:', res.message)
            this.$message.error(res.message || '删除失败')
            // 即使失败也刷新列表
            await this.loadVisitors()
          }
        } catch (error) {
          console.error('删除访客失败:', error)
          console.error('错误详情:', error.response?.data || error.message)
          // 即使失败也刷新列表，确保数据同步
          await this.loadVisitors()
          // 响应拦截器已经显示了错误消息，这里不需要再显示
        }
      }).catch(() => {
        // 用户取消操作
      })
    },
    
    getStatusType(status) {
      const types = {
        '待审核': 'warning',
        '已通过': 'success',
        '已拒绝': 'danger',
        '已登记': 'info',
        '已离开': 'info'
      }
      return types[status] || 'info'
    }
  }
}
</script>

<style scoped>
.visitor-management-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.building-hint {
  font-size: 12px;
  color: #67c23a;
  margin-top: 4px;
}

.action-buttons-grid {
  display: inline-flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
  align-items: flex-start;
}

.button-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.button-row .el-button {
  margin: 0;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 4px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  min-width: 60px;
  height: 28px;
  line-height: 1;
}

.button-row .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.button-row .el-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 查看详情按钮固定宽度 */
.button-row .el-button:first-child {
  width: 75px;
  flex: none;
}

/* 删除按钮固定宽度 */
.button-row .el-button[type="danger"] {
  width: 65px;
  flex: none;
}

/* 其他按钮自适应宽度 */
.button-row .el-button:not(:first-child):not([type="danger"]) {
  flex: 1;
  min-width: 60px;
  max-width: 75px;
}

/* 按钮颜色优化 */
.button-row .el-button[type="primary"] {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  border: none;
}

.button-row .el-button[type="success"] {
  background: linear-gradient(135deg, #67c23a 0%, #529b2e 100%);
  border: none;
}

.button-row .el-button[type="warning"] {
  background: linear-gradient(135deg, #e6a23c 0%, #cf9236 100%);
  border: none;
}

.button-row .el-button[type="danger"] {
  background: linear-gradient(135deg, #f56c6c 0%, #e74c3c 100%);
  border: none;
}

.empty-space {
  flex: 1;
  min-width: 70px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .action-buttons-grid {
    gap: 6px;
    padding: 4px 0;
  }
  
  .button-row {
    flex-direction: row;
    gap: 4px;
    flex-wrap: wrap;
  }
  
  .button-row .el-button {
    width: auto;
    height: 24px;
    font-size: 10px;
    padding: 4px 6px;
    min-width: 50px;
  }
  
  .button-row .el-button:first-child,
  .button-row .el-button[type="danger"] {
    width: auto;
  }
  
  .empty-space {
    display: none;
  }
}
</style>
