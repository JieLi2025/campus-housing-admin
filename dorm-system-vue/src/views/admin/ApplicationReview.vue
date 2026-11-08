<template>
  <div class="application-review-page">
    <el-card>
      <template #header>
        <h2>申请审核</h2>
      </template>
      
      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学号、姓名"
              clearable
              @input="loadApplications"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="5">
            <el-select v-model="filterType" placeholder="申请类型" clearable style="width: 100%" @change="loadApplications">
              <el-option label="调宿申请" value="调宿申请" />
              <el-option label="退宿申请" value="退宿申请" />
              <el-option label="物品申报" value="物品申报" />
            </el-select>
          </el-col>
          <el-col :span="5">
            <el-select v-model="filterStatus" placeholder="审核状态" clearable style="width: 100%" @change="loadApplications">
              <el-option label="待审核" value="待审核" />
              <el-option label="已通过" value="已通过" />
              <el-option label="已拒绝" value="已拒绝" />
              <el-option label="待赔偿处理" value="待赔偿处理" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-button @click="reset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-col>
        </el-row>
      </div>
      
      <!-- 申请列表 -->
      <el-table :data="applications" style="width: 100%" :header-cell-style="{ textAlign: 'center' }" :cell-style="{ textAlign: 'center' }">
        <el-table-column prop="studentName" label="申请人" width="80" align="center" />
        <el-table-column prop="studentNumber" label="学号" width="110" align="center" />
        <el-table-column prop="type" label="申请类型" width="90" align="center">
          <template #default="scope">
            {{ translateApplicationType(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="150" align="center">
          <template #default="scope">
            {{ scope.row.applyTime || formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="200" show-overflow-tooltip align="center">
          <template #default="scope">
            <span class="reason-text">{{ scope.row.reason }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(translateStatus(scope.row.status))">
              {{ translateStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button size="small" @click="viewDetails(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 申请详情对话框 -->
      <el-dialog
        v-model="showDetailDialog"
        title="申请详情"
        width="800px"
        top="5vh"
        :before-close="closeDetailDialog"
      >
        <div v-if="currentApplication" class="application-detail">
          <!-- 基本信息 -->
          <el-card class="detail-section">
            <template #header>
              <h4>基本信息</h4>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="申请人">{{ currentApplication.studentName }}</el-descriptions-item>
              <el-descriptions-item label="学号">{{ currentApplication.studentNumber }}</el-descriptions-item>
              <el-descriptions-item label="申请类型">{{ translateApplicationType(currentApplication.type) }}</el-descriptions-item>
              <el-descriptions-item label="申请时间">{{ currentApplication.applyTime || formatTime(currentApplication.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <el-tag :type="getStatusType(translateStatus(currentApplication.status))">
                  {{ translateStatus(currentApplication.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="审核时间">{{ formatTime(currentApplication.approvedAt) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
          
          <!-- 宿舍信息 -->
          <el-card class="detail-section">
            <template #header>
              <h4>宿舍信息</h4>
            </template>
            
            <!-- 分配阶段申请特殊提示 -->
            <el-alert 
              v-if="currentApplication.allocationPhase === 'self-selection'"
              type="warning" 
              :closable="false"
              style="margin-bottom: 16px;"
            >
              <template #title>
                <strong>⚠️ 自主分配期申请</strong>
              </template>
              <div>
                该学生在自主分配期提交申请，当前宿舍：<strong>{{ currentApplication.currentDormInfo || currentApplication.dormInfo }}</strong>
                <br>
                <span v-if="currentApplication.isLeader === false">
                  • 该学生为普通成员，原宿舍队伍保持不变
                </span>
                <span v-else>
                  • 该学生原为舍长，已转让舍长身份
                </span>
                <br>
                • 审批通过后，原宿舍将空出1个床位，需要后续分配
              </div>
            </el-alert>
            
            <el-descriptions :column="2" border>
              <el-descriptions-item label="当前宿舍">{{ currentApplication.currentDormInfo || currentApplication.dormInfo || '暂无' }}</el-descriptions-item>
              <el-descriptions-item :label="getTargetLabel(currentApplication)">
                {{ getTargetValue(currentApplication) }}
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApplication.allocationPhase" label="申请阶段">
                <el-tag :type="currentApplication.allocationPhase === 'self-selection' ? 'warning' : 'info'">
                  {{ currentApplication.allocationPhase === 'self-selection' ? '自主分配期' : '正式入住后' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApplication.isLeader !== undefined" label="舍长身份">
                {{ currentApplication.isLeader === false ? '普通成员' : '已转让舍长' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="currentApplication.assignedDormInfo || currentApplication.assignedDorm" label="管理员分配" :span="2">
                <el-tag type="success" size="large">
                  {{ currentApplication.assignedDormInfo || currentApplication.assignedDorm }}
                </el-tag>
                <span v-if="currentApplication.assignmentType === 'exchange' || currentApplication.assignmentType === 'EXCHANGE'" style="margin-left: 8px;">
                  （与 {{ currentApplication.exchangeTargetStudent?.name || currentApplication.exchangeStudentName }} 调换）
                </span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
          
          <!-- 申请原因 -->
          <el-card class="detail-section">
            <template #header>
              <h4>申请原因</h4>
            </template>
            <div class="reason-content">
              {{ currentApplication.reason }}
            </div>
          </el-card>
          
          <!-- 审批操作（仅待审核状态显示） -->
          <el-card v-if="currentApplication.status === 'PENDING' || currentApplication.status === 'pending' || currentApplication.status === '待审核'" class="detail-section">
            <template #header>
              <h4>审批操作</h4>
            </template>
            <el-form :model="approvalForm" label-width="100px">
              <el-form-item label="审批意见">
                <el-input
                  v-model="approvalForm.comment"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入审批意见（可选）"
                />
              </el-form-item>
              <el-form-item>
                <!-- 调宿申请显示开始分配按钮 -->
                <el-button 
                  v-if="isTransferApplication" 
                  type="primary" 
                  @click="startDormAssignment"
                >
                  开始分配宿舍
                </el-button>
                <!-- 调宿申请：必须先分配后才能通过 -->
                <el-button 
                  v-if="isTransferApplication" 
                  type="success" 
                  @click="approveTransferApplication"
                  :disabled="!currentApplication.assignedDormInfo && !currentApplication.assignedDorm"
                >
                  通过申请
                  <el-tooltip v-if="!currentApplication.assignedDormInfo && !currentApplication.assignedDorm" content="请先点击【开始分配宿舍】进行分配" placement="top">
                    <el-icon style="margin-left: 4px;"><QuestionFilled /></el-icon>
                  </el-tooltip>
                </el-button>
                <!-- 退宿申请显示物资清点按钮，不能直接通过 -->
                <el-button 
                  v-if="isCheckoutApplication" 
                  type="warning" 
                  @click="startMaterialInventory"
                >
                  开始物资清点
                </el-button>
                <!-- 物品申报可以直接通过或拒绝 -->
                <el-button 
                  v-if="isMaterialReportApplication" 
                  type="success" 
                  @click="approveMaterialReport"
                >
                  已处理
                </el-button>
                <el-button type="danger" @click="showRejectDialog">拒绝申请</el-button>
                <el-button @click="closeDetailDialog">取消</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          
          <!-- 待赔偿状态操作 -->
          <el-card v-if="currentApplication.status === 'COMPENSATION_PENDING' || currentApplication.status === '待赔偿处理'" class="detail-section">
            <template #header>
              <h4>赔偿处理</h4>
            </template>
            <el-alert
              title="物资清点结果"
              type="warning"
              :closable="false"
              style="margin-bottom: 16px;"
            >
              <template #default>
                <pre style="white-space: pre-wrap; margin: 8px 0;">{{ currentApplication.adminComment }}</pre>
              </template>
            </el-alert>
            <div style="text-align: center;">
              <el-button type="success" @click="confirmCompensation">
                确认赔偿完成，通过退宿
              </el-button>
              <el-button @click="closeDetailDialog">关闭</el-button>
            </div>
          </el-card>
                 </div>
       </el-dialog>

    <!-- 拒绝申请对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝申请"
      width="500px"
      top="5vh"
    >
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明拒绝原因（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 宿舍分配对话框 -->
    <el-dialog 
      v-model="dormAssignmentDialogVisible" 
      title="宿舍分配" 
      width="700px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="dorm-assignment-content">
        <!-- 学生信息 -->
        <div class="student-info-section">
          <h4>学生信息</h4>
          <div class="info-card">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="姓名">{{ currentApplication?.studentName }}</el-descriptions-item>
              <el-descriptions-item label="学号">{{ currentApplication?.studentNumber }}</el-descriptions-item>
              <el-descriptions-item label="当前宿舍">{{ currentApplication?.currentDormInfo || currentApplication?.dormInfo }}</el-descriptions-item>
              <el-descriptions-item label="目标宿舍">{{ currentApplication?.targetDormInfo || currentApplication?.targetDorm || '待分配' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <!-- 申请原因 -->
        <div class="reason-section">
          <h4>申请原因</h4>
          <div class="reason-display">
            {{ currentApplication?.reason }}
          </div>
        </div>

        <!-- 分配方式选择 -->
        <div class="assignment-type-section">
          <h4>分配方式</h4>
          <el-radio-group v-model="assignmentForm.type" size="large">
            <el-radio value="direct">
              <el-icon><House /></el-icon>
              <span>直接分配到宿舍</span>
              <el-tag type="success" size="small" style="margin-left: 8px;">推荐</el-tag>
            </el-radio>
            <el-radio value="exchange">
              <el-icon><RefreshRight /></el-icon>
              <span>寻找调换对象</span>
              <el-tag type="warning" size="small" style="margin-left: 8px;">需协商</el-tag>
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 直接分配模式 -->
        <div v-if="assignmentForm.type === 'direct'" class="direct-assignment-section">
          <h4>选择宿舍</h4>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-select v-model="assignmentForm.buildingId" placeholder="选择楼栋" @change="handleBuildingChange">
                <el-option 
                  v-for="building in managedBuildings" 
                  :key="building.id" 
                  :label="building.name" 
                  :value="building.id" 
                />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select v-model="assignmentForm.floor" placeholder="选择楼层" @change="loadAvailableRooms">
                <el-option 
                  v-for="floor in (managedBuildings.find(b => b.id === assignmentForm.buildingId)?.totalFloors || 6)" 
                  :key="floor" 
                  :label="`${floor}楼`" 
                  :value="floor" 
                />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select v-model="assignmentForm.room" placeholder="选择宿舍">
                <el-option 
                  v-for="room in availableRooms" 
                  :key="room.id" 
                  :label="room.label" 
                  :value="room.value" 
                />
              </el-select>
            </el-col>
          </el-row>
          
          <!-- 宿舍信息预览 -->
          <div v-if="assignmentForm.room" class="dorm-preview">
            <el-alert 
              :title="`${assignmentForm.building}-${assignmentForm.room}室`"
              type="success"
              :closable="false"
            >
              <template #default>
                <p>当前状态: {{ selectedRoomInfo.occupiedBeds }}/{{ selectedRoomInfo.bedCount }}人 ({{ selectedRoomInfo.availableBeds > 0 ? '有空位' : '已满' }})</p>
                <p v-if="selectedRoomMajorInfo">分配专业: {{ selectedRoomMajorInfo }}</p>
                <p v-else style="color: #909399;">该宿舍暂未分配专业</p>
              </template>
            </el-alert>
          </div>
        </div>

        <!-- 调换模式 -->
        <div v-if="assignmentForm.type === 'exchange'" class="exchange-assignment-section">
          <h4>学生调换确认</h4>
          
          <!-- 调换说明 -->
          <div class="exchange-info">
            <el-alert 
              title="调换说明"
              type="warning"
              :closable="false"
              style="margin-bottom: 16px;"
            >
              <template #default>
                <p>学生已自行协商好调换对象，管理员需要找到目标学生并确认调换。</p>
              </template>
            </el-alert>
          </div>
          
          <!-- 搜索目标学生 -->
          <div class="search-target-student">
            <h5>搜索目标学生</h5>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-input 
                  v-model="exchangeForm.studentName" 
                  placeholder="输入学生姓名"
                  @input="searchTargetStudent"
                />
              </el-col>
              <el-col :span="12">
                <el-input 
                  v-model="exchangeForm.studentId" 
                  placeholder="输入学号"
                  @input="searchTargetStudent"
                />
              </el-col>
            </el-row>
          </div>

          <!-- 目标学生信息 -->
          <div v-if="targetStudent" class="target-student-info">
            <h5>目标学生信息</h5>
            <div class="student-card">
              <div class="student-details">
                <div class="student-name">{{ targetStudent.name }}</div>
                <div class="student-id">学号: {{ targetStudent.studentId }}</div>
                <div class="student-dorm">当前宿舍: {{ targetStudent.dormInfo }}</div>
              </div>
              <div class="student-actions">
                <el-button type="success" size="small" @click="confirmExchange">
                  <el-icon><Check /></el-icon>
                  确认调换
                </el-button>
              </div>
            </div>
          </div>

          <!-- 调换预览 -->
          <div v-if="targetStudent" class="exchange-preview">
            <h5>调换预览</h5>
            <div class="exchange-flow">
              <div class="exchange-item">
                <div class="student-name">{{ currentApplication?.studentName }}</div>
                <div class="student-dorm">{{ currentApplication?.currentDormInfo || currentApplication?.dormInfo }}</div>
              </div>
              <div class="exchange-arrow">
                <el-icon><Right /></el-icon>
                <span>调换</span>
                <el-icon><Right /></el-icon>
              </div>
              <div class="exchange-item">
                <div class="student-name">{{ targetStudent.name }}</div>
                <div class="student-dorm">{{ targetStudent.dormInfo }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeDormAssignmentDialog">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmAssignment"
          :disabled="!canConfirmAssignment"
        >
          {{ assignmentForm.type === 'direct' ? '确认分配' : '确认调换' }}
        </el-button>
      </template>
    </el-dialog>
     </el-card>
  </div>
</template>

<script>
import { 
  getApplicationList,
  approveApplication,
  updateApplication,
  getApplicationDetail
} from '@/api/application.js'
import { getStudentList } from '@/api/student.js'
import { getMyBuildings } from '@/api/adminDormManagement.js'
import { getRoomsByBuilding } from '@/api/room.js'
import { House, RefreshRight, Check, Right, QuestionFilled, Search, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'ApplicationReview',
  components: {
    House,
    RefreshRight,
    Check,
    Right,
    QuestionFilled,
    Search,
    Refresh
  },
  data() {
    return {
      searchKeyword: '',
      filterType: '',
      filterStatus: '',
      applications: [],
      loading: false,
      showDetailDialog: false,
      currentApplication: null,
      approvalForm: {
        comment: ''
      },
      // 拒绝申请相关
      rejectDialogVisible: false,
      rejectForm: {
        reason: ''
      },
      // 宿舍分配相关
      dormAssignmentDialogVisible: false,
      assignmentForm: {
        type: 'direct', // direct: 直接分配, exchange: 调换
        buildingId: null, // 楼栋ID
        building: '', // 楼栋名称（显示用）
        floor: null,
        roomId: null, // 房间ID
        room: '' // 房间号（显示用）
      },
      availableRooms: [],
      managedBuildings: [], // 管理员管理的楼栋列表
      exchangeForm: {
        studentName: '',
        studentId: ''
      },
      targetStudent: null,
      currentPage: 1,
      pageSize: 20,
      total: 0
    }
  },
  computed: {
    canConfirmAssignment() {
      if (this.assignmentForm.type === 'direct') {
        return this.assignmentForm.building && this.assignmentForm.room
      } else {
        return this.targetStudent !== null
      }
    },
    // 判断是否为调宿申请
    isTransferApplication() {
      if (!this.currentApplication) return false
      const type = this.currentApplication.type
      return type === 'TRANSFER' || type === 'transfer' || type === '调宿申请'
    },
    // 判断是否为退宿申请
    isCheckoutApplication() {
      if (!this.currentApplication) return false
      const type = this.currentApplication.type
      return type === 'CHECKOUT' || type === 'checkout' || type === '退宿申请'
    },
    // 判断是否为物品申报
    isMaterialReportApplication() {
      if (!this.currentApplication) return false
      const type = this.currentApplication.type
      return type === 'MATERIAL_REPORT' || type === 'material_report' || type === '物品申报'
    },
    // 获取选中房间的详细信息
    selectedRoomInfo() {
      if (!this.assignmentForm.room) {
        return { occupiedBeds: 0, bedCount: 0, availableBeds: 0 }
      }
      const room = this.availableRooms.find(r => r.value === this.assignmentForm.room)
      return room || { occupiedBeds: 0, bedCount: 0, availableBeds: 0 }
    },
    // 获取选中房间对应的专业班级信息
    selectedRoomMajorInfo() {
      if (!this.assignmentForm.room) {
        return null
      }
      const room = this.availableRooms.find(r => r.value === this.assignmentForm.room)
      if (!room) {
        return null
      }
      // 直接从房间数据中获取专业班级信息
      if (room.majorName || room.className) {
        // 如果majorName已经包含班级信息（格式如"专业 - 班级"），直接返回
        if (room.majorName && room.majorName.includes(' - ')) {
          return room.majorName
        }
        // 否则，如果有className，拼接显示
        if (room.className) {
          return `${room.majorName} - ${room.className}`
        }
        return room.majorName
      }
      return null
    }
  },
  mounted() {
    this.loadApplications();
    this.loadManagedBuildings(); // 加载管理员管理的楼栋
    // 设置定时器，每5秒检查一次申请更新
    this.applicationTimer = setInterval(() => {
      this.loadApplications();
    }, 5000);
    
    // 处理从其他页面跳转时的查询参数
    this.handleQueryParams();
  },
  beforeUnmount() {
    // 清除定时器
    if (this.applicationTimer) {
      clearInterval(this.applicationTimer);
    }
  },
  methods: {
    // 处理查询参数
    handleQueryParams() {
      const { applicationId } = this.$route.query
      
      if (applicationId) {
        // 使用 nextTick 确保数据已加载
        this.$nextTick(() => {
          const application = this.applications.find(app => app.id == applicationId)
          if (application) {
            this.viewDetails(application)
          }
          // 清除查询参数
          this.$router.replace({ query: {} })
        })
      }
    },

    async loadApplications() {
      try {
        this.loading = true
        const params = {
          currentPage: this.currentPage,
          pageSize: this.pageSize
        }
        
        // 添加筛选条件
        if (this.searchKeyword && this.searchKeyword.trim()) {
          params.keyword = this.searchKeyword.trim()
        }
        
        if (this.filterType) {
          params.type = this.filterType
        }
        
        if (this.filterStatus) {
          params.status = this.filterStatus
        }
        
        const res = await getApplicationList(params)
        
        if (res.code === 200) {
          this.applications = res.data.records || res.data || []
          this.total = res.data.total || this.applications.length
        }
      } catch (error) {
        console.error('加载申请列表失败:', error)
        this.$message.error('加载申请列表失败')
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.searchKeyword = ''
      this.filterType = ''
      this.filterStatus = ''
      this.loadApplications();
      this.$message.success('筛选条件已重置');
    },
    async viewDetails(application) {
      // 如果申请对象没有完整信息，从后端加载详情
      if ((!application.currentDormInfo && !application.dormInfo) || !application.reason) {
        try {
          const res = await getApplicationDetail(application.id)
          if (res.code === 200 && res.data) {
            this.currentApplication = res.data
          } else {
            this.currentApplication = application
          }
        } catch (error) {
          console.error('加载申请详情失败:', error)
          this.currentApplication = application
        }
      } else {
        this.currentApplication = application
      }
      this.showDetailDialog = true;
    },
    closeDetailDialog() {
      this.showDetailDialog = false;
      this.currentApplication = null;
      this.approvalForm.comment = '';
    },
    async approveWithComment() {
      // 这个方法已不再使用，保留以防其他地方调用
      await this.approveTransferApplication()
    },
    // 显示拒绝对话框
    showRejectDialog() {
      this.rejectForm.reason = '';
      this.rejectDialogVisible = true;
    },
    // 确认拒绝
    async confirmReject() {
      if (!this.rejectForm.reason.trim()) {
        this.$message.warning('请填写拒绝原因');
        return;
      }
      
      try {
        // 使用 approveApplication，传递 approved: false 来拒绝申请
        const res = await approveApplication(this.currentApplication.id, {
          approved: false,
          comment: this.rejectForm.reason,
          rejectReason: this.rejectForm.reason
        })
        
        if (res.code === 200) {
          this.$message.success('申请已拒绝');
          this.rejectDialogVisible = false;
          this.closeDetailDialog();
          this.loadApplications();
        } else {
          this.$message.error(res.message || '拒绝失败');
        }
      } catch (error) {
        console.error('拒绝申请失败:', error)
        this.$message.error('拒绝失败：' + (error.message || '网络错误'));
      }
    },
    // 翻译申请类型
    translateApplicationType(type) {
      const typeMap = {
        'TRANSFER': '调宿申请',
        'CHECKOUT': '退宿申请',
        'MATERIAL_REPORT': '物品申报',
        'transfer': '调宿申请',
        'checkout': '退宿申请',
        'material_report': '物品申报',
        '调宿申请': '调宿申请',
        '退宿申请': '退宿申请',
        '物品申报': '物品申报'
      }
      return typeMap[type] || type
    },
    // 获取目标字段的标签（根据申请类型动态显示）
    getTargetLabel(application) {
      if (!application) return '学生期望'
      
      // 物品申报显示申报物品
      const type = application.type
      if (type === 'MATERIAL_REPORT' || type === 'material_report' || type === '物品申报') {
        return '申报物品'
      }
      
      // 退宿申请显示联系电话
      if (type === 'CHECKOUT' || type === 'checkout' || type === '退宿申请') {
        return '联系电话'
      }
      
      const targetInfo = application.targetDormInfo || application.targetDorm || ''
      // 如果包含"目标室友"，说明是换室友类型
      if (targetInfo.includes('目标室友')) {
        return '目标室友'
      }
      return '期望宿舍'
    },
    // 获取目标字段的值（根据申请类型动态显示）
    getTargetValue(application) {
      if (!application) return '未填写'
      
      // 物品申报从reason中提取物品名称
      const type = application.type
      if (type === 'MATERIAL_REPORT' || type === 'material_report' || type === '物品申报') {
        return this.extractMaterialInfo(application.reason)
      }
      
      // 退宿申请显示联系电话
      if (type === 'CHECKOUT' || type === 'checkout' || type === '退宿申请') {
        return application.contactPhone || '未填写'
      }
      
      const targetInfo = application.targetDormInfo || application.targetDorm || ''
      if (!targetInfo) return '未填写'
      // 如果是目标室友格式，提取学号
      if (targetInfo.includes('目标室友学号:')) {
        return targetInfo.replace('目标室友学号:', '').trim()
      }
      return targetInfo
    },
    // 从reason中提取物品申报信息
    extractMaterialInfo(reason) {
      if (!reason) return '-'
      // reason格式：【问题类型】物品名称 - 宿舍：问题描述
      const match = reason.match(/【([^】]+)】([^-]+)/)
      if (match) {
        return `${match[2].trim()}（${match[1]}）`
      }
      return reason
    },
    // 翻译状态
    translateStatus(status) {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'INVENTORY_PASSED': '物资清点通过',
        'COMPENSATION_PENDING': '待赔偿处理',
        'pending': '待审核',
        'approved': '已通过',
        'rejected': '已拒绝',
        '待审核': '待审核',
        '已通过': '已通过',
        '已拒绝': '已拒绝',
        '物资清点通过': '物资清点通过',
        '待赔偿处理': '待赔偿处理'
      }
      return statusMap[status] || status
    },
    getStatusType(status) {
      const types = {
        '待审核': 'warning',
        '已通过': 'success',
        '已拒绝': 'danger',
        '物资清点通过': 'success',
        '待赔偿处理': 'warning'
      }
      return types[status] || 'info'
    },
    formatTime(time) {
      if (!time) return '--'
      try {
        const date = new Date(time)
        if (isNaN(date.getTime())) return '--'
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (e) {
        return '--'
      }
    },
    startMaterialInventory() {
      // 从宿舍信息中提取宿舍号
      const dormInfo = this.currentApplication.currentDormInfo || this.currentApplication.dormInfo || ''
      const dormNumber = dormInfo.match(/\d+/)?.[0] || '101'
      
      // 跳转到物资清点页面，只传递必要的ID信息
      this.$router.push({
        name: 'MaterialInventory',
        params: {
          dormNumber: dormNumber,
          applicationId: this.currentApplication.id
        }
      })
    },
    // 确认赔偿完成
    async confirmCompensation() {
      try {
        await this.$confirm(
          '确认学生已完成赔偿？确认后将通过退宿申请。',
          '确认赔偿',
          {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const { confirmCompensation } = await import('@/api/adminMaterial.js')
        const res = await confirmCompensation(this.currentApplication.id)
        
        if (res.code === 200) {
          this.$message.success('赔偿已确认，退宿申请已通过')
          this.closeDetailDialog()
          this.loadApplications()
        } else {
          this.$message.error(res.message || '操作失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('确认赔偿失败:', error)
          this.$message.error('操作失败')
        }
      }
    },
    // 开始分配宿舍
    async startDormAssignment() {
      this.dormAssignmentDialogVisible = true
      // 重置表单
      this.assignmentForm = {
        type: 'direct',
        buildingId: null,
        building: '',
        floor: null,
        roomId: null,
        room: ''
      }
      this.exchangeForm = {
        studentName: '',
        studentId: ''
      }
      this.targetStudent = null
      this.availableRooms = []
      
      // 如果还没有加载楼栋列表，先加载
      if (this.managedBuildings.length === 0) {
        await this.loadManagedBuildings()
      }
    },
    // 关闭分配对话框
    closeDormAssignmentDialog() {
      this.dormAssignmentDialogVisible = false
    },
    
    // 处理楼栋选择变化
    handleBuildingChange() {
      const building = this.managedBuildings.find(b => b.id === this.assignmentForm.buildingId)
      if (building) {
        this.assignmentForm.building = building.name
      }
      this.assignmentForm.floor = null
      this.assignmentForm.room = ''
      this.availableRooms = []
    },
    // 加载管理员管理的楼栋
    async loadManagedBuildings() {
      try {
        const res = await getMyBuildings()
        if (res.code === 200 && res.data) {
          this.managedBuildings = res.data.map(building => ({
            id: building.id,
            name: building.buildingName || building.name || `${building.id}号楼`,
            buildingCode: building.buildingCode,
            totalFloors: building.totalFloors || 6
          }))
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
      }
    },
    
    // 加载可用房间（从后端API获取）
    async loadAvailableRooms() {
      if (!this.assignmentForm.buildingId || !this.assignmentForm.floor) {
        this.availableRooms = []
        return
      }
      
      try {
        const res = await getRoomsByBuilding(this.assignmentForm.buildingId)
        if (res.code === 200 && res.data) {
          // 过滤出指定楼层且有可用床位的房间
          const rooms = res.data
            .filter(room => room.floor === this.assignmentForm.floor && room.availableBeds > 0)
            .map(room => ({
              label: `${room.roomNumber}室 (${room.occupiedBeds || 0}/${room.bedCount}人)`,
              value: room.roomNumber,
              id: room.id,
              availableBeds: room.availableBeds,
              occupiedBeds: room.occupiedBeds || 0,
              bedCount: room.bedCount,
              majorName: room.majorName || null,
              className: room.className || null
            }))
          this.availableRooms = rooms
        } else {
          this.availableRooms = []
        }
      } catch (error) {
        console.error('加载可用房间失败:', error)
        this.$message.error('加载可用房间失败')
        this.availableRooms = []
      }
    },
    // 搜索目标学生（从后端API搜索）
    async searchTargetStudent() {
      if (!this.exchangeForm.studentName && !this.exchangeForm.studentId) {
        this.targetStudent = null
        return
      }
      
      try {
        const keyword = this.exchangeForm.studentName || this.exchangeForm.studentId
        const res = await getStudentList({
          keyword: keyword.trim(),
          currentPage: 1,
          pageSize: 10
        })
        
        if (res.code === 200 && res.data && res.data.students && res.data.students.length > 0) {
          // 优先匹配学号，其次匹配姓名
          let student = null
          if (this.exchangeForm.studentId) {
            student = res.data.students.find(s => s.studentId === this.exchangeForm.studentId.trim())
          }
          if (!student && this.exchangeForm.studentName) {
            student = res.data.students.find(s => s.realName === this.exchangeForm.studentName.trim())
          }
          if (!student && res.data.students.length === 1) {
            student = res.data.students[0]
          }
          
          if (student) {
            // 构建宿舍信息字符串
            const dormInfo = student.dormNumber 
              ? `${student.dormNumber}` 
              : '暂未分配'
            
            this.targetStudent = {
              id: student.id,
              name: student.realName,
              studentId: student.studentId,
              dormInfo: dormInfo
            }
          } else {
            this.$message.warning('未找到匹配的学生，请检查输入信息')
            this.targetStudent = null
          }
        } else {
          this.$message.warning('未找到匹配的学生')
          this.targetStudent = null
        }
      } catch (error) {
        console.error('搜索学生失败:', error)
        this.$message.error('搜索学生失败')
        this.targetStudent = null
      }
    },
    // 确认调换
    confirmExchange() {
      this.$message.success('调换信息已确认，等待执行')
    },
    // 确认分配（保存分配信息到后端）
    async confirmAssignment() {
      if (this.assignmentForm.type === 'direct') {
        // 直接分配模式
        const selectedRoom = this.availableRooms.find(r => r.value === this.assignmentForm.room)
        if (!selectedRoom) {
          this.$message.error('请选择有效的房间')
          return
        }
        
        const building = this.managedBuildings.find(b => b.id === this.assignmentForm.buildingId)
        const newDorm = `${building?.name || ''}-${this.assignmentForm.room}`
        
        this.$confirm(
          `确定将学生 ${this.currentApplication.studentName} 从 ${this.currentApplication.currentDormInfo || this.currentApplication.dormInfo} 分配到 ${newDorm} 吗？`,
          '确认分配',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(async () => {
          try {
            // 调用后端API保存分配信息
            const res = await updateApplication(this.currentApplication.id, {
              assignedBuildingId: this.assignmentForm.buildingId,
              assignedRoomId: selectedRoom.id,
              assignedDormInfo: newDorm,
              assignmentType: 'DIRECT'
            })
            
            if (res.code === 200) {
              // 更新本地申请对象
              this.currentApplication.assignedDormInfo = newDorm
              this.currentApplication.assignedDorm = newDorm
              this.currentApplication.assignmentType = 'direct'
              this.currentApplication.assignedBuildingId = this.assignmentForm.buildingId
              this.currentApplication.assignedRoomId = selectedRoom.id
              
              this.$message.success('分配方案已保存！请点击"通过申请"完成审批流程。')
              this.dormAssignmentDialogVisible = false
            } else {
              this.$message.error(res.message || '保存分配信息失败')
            }
          } catch (error) {
            console.error('保存分配信息失败:', error)
            this.$message.error('保存分配信息失败')
          }
        }).catch(() => {
          this.$message.info('已取消分配')
        })
      } else {
        // 调换模式
        if (!this.targetStudent || !this.targetStudent.id) {
          this.$message.warning('请先搜索并选择调换对象')
          return
        }
        
        const targetDorm = this.targetStudent.dormInfo
        this.$confirm(
          `确定将 ${this.currentApplication.studentName}（${this.currentApplication.currentDormInfo || this.currentApplication.dormInfo}）与 ${this.targetStudent.name}（${targetDorm}）进行宿舍调换吗？`,
          '确认调换',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(async () => {
          try {
            // 调用后端API保存调换信息
            const res = await updateApplication(this.currentApplication.id, {
              assignedDormInfo: targetDorm,
              assignmentType: 'EXCHANGE',
              exchangeStudentId: this.targetStudent.id
            })
            
            if (res.code === 200) {
              // 更新本地申请对象
              this.currentApplication.assignedDormInfo = targetDorm
              this.currentApplication.assignedDorm = targetDorm
              this.currentApplication.assignmentType = 'exchange'
              this.currentApplication.exchangeStudentId = this.targetStudent.id
              this.currentApplication.exchangeTargetStudent = {
                name: this.targetStudent.name,
                studentId: this.targetStudent.studentId,
                dormInfo: this.targetStudent.dormInfo
              }
              
              this.$message.success('调换方案已保存！请点击"通过申请"完成审批流程。')
              this.dormAssignmentDialogVisible = false
            } else {
              this.$message.error(res.message || '保存调换信息失败')
            }
          } catch (error) {
            console.error('保存调换信息失败:', error)
            this.$message.error('保存调换信息失败')
          }
        }).catch(() => {
          this.$message.info('已取消调换')
        })
      }
    },
    // 通过物品申报（调用后端API）
    async approveMaterialReport() {
      this.$confirm(
        `确认已处理该物品申报吗？`,
        '确认处理',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          const res = await approveApplication(this.currentApplication.id, {
            approved: true,
            comment: this.approvalForm.comment || '物品问题已处理'
          })
          
          if (res.code === 200) {
            this.$message.success('物品申报已处理')
            this.closeDetailDialog()
            this.loadApplications()
          } else {
            this.$message.error(res.message || '处理失败')
          }
        } catch (error) {
          console.error('处理物品申报失败:', error)
          this.$message.error('处理失败：' + (error.message || '网络错误'))
        }
      }).catch(() => {
        this.$message.info('已取消操作')
      })
    },
    // 通过调宿申请（调用后端API）
    async approveTransferApplication() {
      if (!this.currentApplication.assignedDormInfo && !this.currentApplication.assignedDorm) {
        this.$message.warning('请先点击"开始分配宿舍"进行分配')
        return
      }
      
      const assignedDorm = this.currentApplication.assignedDormInfo || this.currentApplication.assignedDorm
      const currentDorm = this.currentApplication.currentDormInfo || this.currentApplication.dormInfo
      
      this.$confirm(
        `确认通过该调宿申请吗？学生 ${this.currentApplication.studentName} 的宿舍将从 ${currentDorm} 更新为 ${assignedDorm}`,
        '确认通过申请',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          // 构建审批数据
          const approveData = {
            approved: true,
            comment: this.approvalForm.comment || ''
          }
          
          // 如果是调宿申请，添加分配信息
          if (this.isTransferApplication) {
            if (this.currentApplication.assignedBuildingId) {
              approveData.assignedBuildingId = this.currentApplication.assignedBuildingId
            }
            if (this.currentApplication.assignedRoomId) {
              approveData.assignedRoomId = this.currentApplication.assignedRoomId
            }
            if (assignedDorm) {
              approveData.assignedDormInfo = assignedDorm
            }
            if (this.currentApplication.assignmentType) {
              approveData.assignmentType = this.currentApplication.assignmentType.toUpperCase()
            }
            if (this.currentApplication.exchangeStudentId) {
              approveData.exchangeStudentId = this.currentApplication.exchangeStudentId
            }
          }
          
          // 调用后端API审批申请
          const res = await approveApplication(this.currentApplication.id, approveData)
          
          if (res.code === 200) {
            this.currentApplication.status = 'APPROVED'
            this.$message.success('申请已通过！学生宿舍信息已更新。')
            this.closeDetailDialog()
            this.loadApplications() // 重新加载申请列表
            // 触发通知更新事件
            window.dispatchEvent(new Event('notificationUpdated'))
          } else {
            this.$message.error(res.message || '审批失败')
          }
        } catch (error) {
          console.error('审批申请失败:', error)
          this.$message.error('审批失败：' + (error.message || '网络错误'))
        }
      }).catch(() => {
        this.$message.info('已取消操作')
      })
    }
  }
}
</script>

<style scoped>
.application-review-page {
  padding: 20px;
}

.filter-section {
  margin-bottom: 20px;
}

.application-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.reason-content {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  line-height: 1.6;
  color: #606266;
  font-size: 14px;
}

@media (max-width: 768px) {
  .application-detail {
    max-height: 65vh;
  }
  
  .detail-section {
    margin-bottom: 16px;
  }
}

/* 宿舍分配对话框样式 */
.dorm-assignment-content {
  max-height: 600px;
  overflow-y: auto;
}

.student-info-section,
.reason-section,
.assignment-type-section,
.direct-assignment-section,
.exchange-assignment-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.student-info-section h4,
.reason-section h4,
.assignment-type-section h4,
.direct-assignment-section h4,
.exchange-assignment-section h4 {
  margin: 0 0 12px 0;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.exchange-assignment-section h5 {
  margin: 16px 0 12px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 600;
}

.info-card {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
}

.reason-display {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
  line-height: 1.6;
  color: #606266;
}

.dorm-preview {
  margin-top: 16px;
}

.target-student-info {
  margin-top: 16px;
}

.student-card {
  background: #fff;
  padding: 16px;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.student-details .student-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.student-details .student-id,
.student-details .student-dorm {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.exchange-preview {
  margin-top: 16px;
}

.exchange-flow {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #fff;
  padding: 16px;
  border-radius: 6px;
}

.exchange-item {
  flex: 1;
  text-align: center;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 6px;
}

.exchange-item .student-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.exchange-item .student-dorm {
  font-size: 13px;
  color: #606266;
}

.exchange-arrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-size: 14px;
  padding: 0 16px;
}

.exchange-arrow .el-icon {
  font-size: 20px;
}

/* 表格样式优化 */
.reason-text {
  display: inline-block;
  text-align: center;
  line-height: 1.5;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f5f7fa;
  font-weight: 600;
}

:deep(.el-table .cell) {
  padding: 8px 12px;
}
</style>
