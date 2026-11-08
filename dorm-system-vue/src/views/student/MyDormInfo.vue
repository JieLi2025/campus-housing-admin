<template>
  <div class="dorm-management-page">
    <el-card>
      <template #header>
        <h2>宿舍申请</h2>
      </template>
      
      <!-- 宿舍信息 -->
      <div class="dorm-info">
        <h3>我的宿舍信息</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="宿舍楼">{{ dormInfo.building || '-' }}</el-descriptions-item>
          <el-descriptions-item label="宿舍号">{{ dormInfo.roomNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="床位号">{{ dormInfo.bedNumber || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <!-- 申请操作 -->
      <div class="application-section">
        <h3>申请操作</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="hover" class="action-card">
              <h4>调宿申请</h4>
              <p>申请调换到其他宿舍</p>
              <el-button type="primary" @click="handleTransferClick">申请调宿</el-button>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" class="action-card">
              <h4>退宿申请</h4>
              <p>申请退出当前宿舍</p>
              <el-button type="warning" @click="handleCheckoutClick">申请退宿</el-button>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 调宿申请表单（展开式） -->
      <div v-if="showTransferForm" class="transfer-form-section">
        <div class="section-header">
          <h3>调宿申请</h3>
          <el-button type="text" @click="showTransferForm = false">
            <el-icon><Close /></el-icon> 收起
          </el-button>
        </div>
        
        <el-form :model="transferForm" :rules="transferRules" ref="transferFormRef" label-width="100px">
          <el-form-item label="申请类型" prop="transferType">
            <el-radio-group v-model="transferForm.transferType">
              <el-radio label="room">换房间</el-radio>
              <el-radio label="roommate">换室友</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="目标房间" v-if="transferForm.transferType === 'room'" prop="targetRoom">
            <el-input v-model="transferForm.targetRoom" placeholder="请输入目标房间号" style="width: 200px"></el-input>
          </el-form-item>
          
          <el-form-item label="目标室友" v-if="transferForm.transferType === 'roommate'">
            <el-input v-model="transferForm.targetRoommateId" placeholder="请输入目标室友的学号" style="width: 200px"></el-input>
          </el-form-item>
          
          <el-form-item label="申请原因" prop="reason">
            <el-select v-model="transferForm.reason" placeholder="请选择申请原因" style="width: 300px">
              <el-option label="身体健康原因" value="health"></el-option>
              <el-option label="学习环境需要" value="study"></el-option>
              <el-option label="室友关系问题" value="roommate_conflict"></el-option>
              <el-option label="家庭特殊情况" value="family"></el-option>
              <el-option label="其他原因" value="other"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="详细说明" prop="description">
            <el-input 
              v-model="transferForm.description" 
              type="textarea" 
              :rows="4" 
              placeholder="请详细说明申请调宿的原因"
              maxlength="500"
              show-word-limit
            ></el-input>
          </el-form-item>
          
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input 
              v-model="transferForm.contactPhone" 
              placeholder="请输入11位手机号" 
              style="width: 200px"
              maxlength="11"
              @input="transferForm.contactPhone = transferForm.contactPhone.replace(/[^\d]/g, '')"
            ></el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="submitTransfer" :loading="transferSubmitting">提交申请</el-button>
            <el-button @click="resetTransferForm">重置</el-button>
          </el-form-item>
        </el-form>
        
      </div>
      
      <!-- 我的申请记录 -->
      <div class="application-history">
        <div class="section-header">
          <h3>我的申请记录</h3>
          <el-button type="text" @click="loadApplications">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
        <el-table :data="applications" style="width: 100%" stripe v-loading="loading">
          <el-table-column prop="createdAt" label="申请时间" width="170" align="center">
            <template #default="scope">
              <span>{{ formatApplicationTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="申请类型" width="110" align="center">
            <template #default="scope">
              <el-tag size="small" :type="getApplicationTypeTagType(scope.row.type)">
                {{ formatApplicationType(scope.row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="申请原因" min-width="200" align="center">
            <template #default="scope">
              <el-tooltip :content="scope.row.reason" placement="top" :disabled="!scope.row.reason || scope.row.reason.length < 30">
                <span class="reason-text">{{ scope.row.reason || '-' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="getApplicationStatusType(scope.row.status)" effect="dark">
                {{ formatApplicationStatus(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="scope">
              <el-button size="small" type="primary" @click="viewApplicationDetail(scope.row)">详情</el-button>
              <el-button 
                v-if="isPending(scope.row.status)" 
                size="small" 
                type="warning"
                @click="editApplication(scope.row)"
              >修改</el-button>
              <el-button 
                v-if="isPending(scope.row.status)" 
                size="small" 
                type="danger"
                @click="handleWithdraw(scope.row)"
              >撤回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
    
    <!-- 退宿申请对话框 -->
    <el-dialog
      v-model="showCheckoutDialog"
      title="退宿申请"
      width="550px"
    >
      <!-- 退宿流程说明 -->
      <el-alert
        title="退宿流程说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #default>
          <ol style="margin: 8px 0 0 0; padding-left: 20px; line-height: 1.8;">
            <li>提交退宿申请</li>
            <li>管理员进行物资清点</li>
            <li>如有损坏/缺失物品，需完成赔偿</li>
            <li>审核通过后，办理退宿手续</li>
          </ol>
        </template>
      </el-alert>
      
      <el-form :model="checkoutForm" label-width="100px">
        <el-form-item label="退宿原因" required>
          <el-select v-model="checkoutForm.reasonType" placeholder="请选择退宿原因" style="width: 100%; margin-bottom: 10px;">
            <el-option label="毕业离校" value="graduation" />
            <el-option label="休学/退学" value="leave_school" />
            <el-option label="校外住宿" value="off_campus" />
            <el-option label="其他原因" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细说明" required>
          <el-input
            v-model="checkoutForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请详细说明退宿原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input
            v-model="checkoutForm.contactPhone"
            placeholder="请输入11位手机号"
            style="width: 200px"
            maxlength="11"
            @input="checkoutForm.contactPhone = checkoutForm.contactPhone.replace(/[^\d]/g, '')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCheckoutDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCheckout" :loading="checkoutSubmitting">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 申请详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="申请详情"
      width="600px"
    >
      <div v-if="currentApplication" class="application-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请类型">
            <el-tag :type="getApplicationTypeTagType(currentApplication.type)">
              {{ formatApplicationType(currentApplication.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatApplicationTime(currentApplication.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getApplicationStatusType(currentApplication.status)" effect="dark">
              {{ formatApplicationStatus(currentApplication.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请原因">{{ currentApplication.reason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话" v-if="currentApplication.contactPhone">{{ currentApplication.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="当前宿舍" v-if="currentApplication.currentDormInfo">
            {{ currentApplication.currentDormInfo }}
          </el-descriptions-item>
          <el-descriptions-item label="目标宿舍" v-if="currentApplication.targetDormInfo">
            {{ currentApplication.targetDormInfo }}
          </el-descriptions-item>
          <el-descriptions-item label="审核备注" v-if="currentApplication.adminComment || currentApplication.rejectReason">
            {{ currentApplication.adminComment || currentApplication.rejectReason }}
          </el-descriptions-item>
          <el-descriptions-item label="分配宿舍" v-if="currentApplication.assignedDormInfo">
            <el-tag type="success">{{ currentApplication.assignedDormInfo }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 状态提示 -->
        <el-alert
          v-if="isPending(currentApplication.status)"
          title="申请审核中"
          type="info"
          :closable="false"
          style="margin-top: 20px;"
        >
          您的申请正在审核中，请耐心等待管理员处理。
        </el-alert>

        <el-alert
          v-if="currentApplication.status === 'REJECTED' || currentApplication.status === '已拒绝'"
          title="申请已被拒绝"
          type="error"
          :closable="false"
          style="margin-top: 20px;"
        >
          {{ currentApplication.rejectReason || currentApplication.adminComment || '如有疑问，请联系宿舍管理员。' }}
        </el-alert>

        <el-alert
          v-if="currentApplication.status === 'APPROVED' || currentApplication.status === '已通过'"
          title="申请已通过"
          type="success"
          :closable="false"
          style="margin-top: 20px;"
        >
          您的申请已通过，请关注后续安排通知。
        </el-alert>
      </div>

      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button 
          v-if="currentApplication && isPending(currentApplication.status)" 
          type="primary" 
          @click="editApplicationFromDetail"
        >修改申请</el-button>
      </template>
    </el-dialog>

    <!-- 修改申请对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="修改申请"
      width="600px"
    >
      <el-form v-if="editingApplication" :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="申请类型">
          <el-tag :type="getApplicationTypeTagType(editingApplication.type)">
            {{ formatApplicationType(editingApplication.type) }}
          </el-tag>
        </el-form-item>
        
        <!-- 调宿申请编辑 -->
        <template v-if="isTransferType(editingApplication.type)">
          <el-form-item label="申请类型" prop="transferType">
            <el-radio-group v-model="editForm.transferType">
              <el-radio label="room">换房间</el-radio>
              <el-radio label="roommate">换室友</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="目标房间" v-if="editForm.transferType === 'room'">
            <el-input v-model="editForm.targetRoom" placeholder="请输入目标房间号" style="width: 200px"></el-input>
          </el-form-item>
          
          <el-form-item label="目标室友" v-if="editForm.transferType === 'roommate'">
            <el-input v-model="editForm.targetRoommateId" placeholder="请输入目标室友的学号" style="width: 200px"></el-input>
          </el-form-item>
          
          <el-form-item label="申请原因" prop="reason">
            <el-select v-model="editForm.reason" placeholder="请选择申请原因" style="width: 300px">
              <el-option label="身体健康原因" value="health"></el-option>
              <el-option label="学习环境需要" value="study"></el-option>
              <el-option label="室友关系问题" value="roommate_conflict"></el-option>
              <el-option label="家庭特殊情况" value="family"></el-option>
              <el-option label="其他原因" value="other"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="详细说明" prop="description">
            <el-input 
              v-model="editForm.description" 
              type="textarea" 
              :rows="4" 
              placeholder="请详细说明申请调宿的原因"
              maxlength="500"
              show-word-limit
            ></el-input>
          </el-form-item>
          
          <el-form-item label="联系电话">
            <el-input 
              v-model="editForm.contactPhone" 
              placeholder="请输入11位手机号" 
              style="width: 200px"
              maxlength="11"
              @input="editForm.contactPhone = editForm.contactPhone.replace(/[^\d]/g, '')"
            ></el-input>
          </el-form-item>
        </template>
        
        <!-- 退宿申请编辑 -->
        <template v-else-if="isCheckoutType(editingApplication.type)">
          <el-form-item label="退宿原因" prop="checkoutReasonType">
            <el-select v-model="editForm.checkoutReasonType" placeholder="请选择退宿原因" style="width: 100%;">
              <el-option label="毕业离校" value="graduation" />
              <el-option label="休学/退学" value="leave_school" />
              <el-option label="校外住宿" value="off_campus" />
              <el-option label="其他原因" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细说明" prop="checkoutReason">
            <el-input
              v-model="editForm.checkoutReason"
              type="textarea"
              :rows="4"
              placeholder="请详细说明退宿原因"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input 
              v-model="editForm.checkoutContactPhone" 
              placeholder="请输入11位手机号" 
              style="width: 200px"
              maxlength="11"
              @input="editForm.checkoutContactPhone = editForm.checkoutContactPhone.replace(/[^\d]/g, '')"
            ></el-input>
          </el-form-item>
        </template>
        
        <!-- 物品申报编辑 -->
        <template v-else-if="isMaterialReportType(editingApplication.type)">
          <el-form-item label="物品名称">
            <el-input v-model="editForm.materialName" placeholder="请输入物品名称" style="width: 200px"></el-input>
          </el-form-item>
          <el-form-item label="问题类型" prop="issueType">
            <el-select v-model="editForm.issueType" placeholder="请选择问题类型" style="width: 200px">
              <el-option label="损坏" value="损坏" />
              <el-option label="缺失" value="缺失" />
              <el-option label="故障" value="故障" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="问题描述" prop="materialIssue">
            <el-input
              v-model="editForm.materialIssue"
              type="textarea"
              :rows="4"
              placeholder="请详细描述问题"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </template>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditApplication" :loading="editSubmitting">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Close, Refresh } from '@element-plus/icons-vue'
import { getMyApplications, submitApplication, withdrawApplication, updateStudentApplication } from '@/api/application.js'
import { submitTransferApplication, getApplicationHistory, getAllocationResult } from '@/api/studentAllocation.js'
import { updateMaterialReport } from '@/api/material.js'

export default {
  name: 'DormManagement',
  components: {
    Close,
    Refresh
  },
  data() {
    return {
      // 宿舍信息
      dormInfo: {
        building: '',
        roomNumber: '',
        bedNumber: ''
      },
      // 申请记录
      applications: [],
      loading: false,
      // 调宿申请
      showTransferForm: false,
      transferSubmitting: false,
      transferForm: {
        transferType: 'room',
        targetRoom: '',
        targetRoommateId: '',
        reason: '',
        description: '',
        contactPhone: ''
      },
      transferRules: {
        transferType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
        targetRoom: [
          { 
            validator: (rule, value, callback) => {
              // 只有选择换房间时才验证
              if (this.transferForm.transferType === 'room') {
                if (!value || !value.trim()) {
                  callback(new Error('请输入目标房间号'))
                } else {
                  callback()
                }
              } else {
                callback()
              }
            }, 
            trigger: 'blur' 
          }
        ],
        reason: [{ required: true, message: '请选择申请原因', trigger: 'change' }],
        description: [{ required: true, message: '请填写详细说明', trigger: 'blur' }],
        contactPhone: [
          {
            validator: (rule, value, callback) => {
              if (value && value.trim()) {
                // 验证手机号格式：11位数字，以1开头
                const phoneRegex = /^1[3-9]\d{9}$/
                if (!phoneRegex.test(value.trim())) {
                  callback(new Error('请输入正确的手机号'))
                } else {
                  callback()
                }
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      },
      // 退宿申请
      showCheckoutDialog: false,
      checkoutSubmitting: false,
      checkoutForm: {
        reasonType: '',
        reason: '',
        contactPhone: ''
      },
      // 申请详情
      showDetailDialog: false,
      currentApplication: null,
      // 编辑申请
      showEditDialog: false,
      editSubmitting: false,
      editingApplication: null,
      editForm: {
        // 调宿申请字段
        transferType: 'room',
        targetRoom: '',
        targetRoommateId: '',
        reason: '',
        description: '',
        contactPhone: '',
        // 退宿申请字段
        checkoutReasonType: '',
        checkoutReason: '',
        checkoutContactPhone: '',
        // 物品申报字段
        materialName: '',
        issueType: '',
        materialIssue: ''
      },
      editRules: {
        reason: [{ required: true, message: '请选择申请原因', trigger: 'change' }],
        description: [{ required: true, message: '请填写详细说明', trigger: 'blur' }],
        checkoutReasonType: [{ required: true, message: '请选择退宿原因', trigger: 'change' }],
        checkoutReason: [{ required: true, message: '请填写详细说明', trigger: 'blur' }],
        issueType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
        materialIssue: [{ required: true, message: '请描述问题', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.loadDormInfo()
    this.loadApplications()
  },
  methods: {
    // 加载宿舍信息
    async loadDormInfo() {
      try {
        const res = await getAllocationResult()
        if (res.code === 200 && res.data && res.data.success) {
          this.dormInfo = {
            building: res.data.building,
            roomNumber: res.data.roomNumber,
            bedNumber: res.data.bedNumber ? res.data.bedNumber + '号床' : '-'
          }
        }
      } catch (error) {
        console.error('加载宿舍信息失败:', error)
      }
    },
    
    // 加载申请记录
    async loadApplications() {
      try {
        this.loading = true
        const res = await getApplicationHistory()
        if (res.code === 200) {
          this.applications = res.data || []
        }
      } catch (error) {
        console.error('加载申请记录失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 提交调宿申请
    async submitTransfer() {
      try {
        await this.$refs.transferFormRef.validate()
        this.transferSubmitting = true
        
        const res = await submitTransferApplication({
          transferType: this.transferForm.transferType,
          targetRoom: this.transferForm.targetRoom,
          targetRoommateId: this.transferForm.targetRoommateId,
          reason: this.transferForm.reason,
          description: this.transferForm.description,
          contactPhone: this.transferForm.contactPhone
        })
        
        if (res.code === 200) {
          this.$message.success('调宿申请提交成功，请等待管理员审核')
          this.showTransferForm = false
          this.resetTransferForm()
          this.loadApplications()
        }
        // 错误已在 request.js 拦截器中统一显示
      } catch (error) {
        // 表单验证失败返回 false，不需要处理
        // 其他错误已在 request.js 拦截器中统一显示
        if (error !== false) {
          console.error('提交调宿申请失败:', error)
        }
      } finally {
        this.transferSubmitting = false
      }
    },
    
    // 重置调宿表单
    resetTransferForm() {
      this.transferForm = {
        transferType: 'room',
        targetRoom: '',
        targetRoommateId: '',
        reason: '',
        description: '',
        contactPhone: ''
      }
      this.$refs.transferFormRef?.resetFields()
    },
    
    // 点击调宿申请按钮
    handleTransferClick() {
      // 检查是否已分配宿舍
      if (!this.dormInfo.building || !this.dormInfo.roomNumber) {
        this.$message.warning('您尚未分配宿舍，无法申请调宿')
        return
      }
      this.showTransferForm = true
    },
    
    // 点击退宿申请按钮
    handleCheckoutClick() {
      // 检查是否已分配宿舍
      if (!this.dormInfo.building || !this.dormInfo.roomNumber) {
        this.$message.warning('您尚未分配宿舍，无法申请退宿')
        return
      }
      this.showCheckoutDialog = true
    },
    
    // 提交退宿申请
    async submitCheckout() {
      if (!this.checkoutForm.reasonType) {
        this.$message.warning('请选择退宿原因')
        return
      }
      if (!this.checkoutForm.reason.trim()) {
        this.$message.warning('请填写详细说明')
        return
      }
      // 验证联系电话格式
      if (this.checkoutForm.contactPhone && this.checkoutForm.contactPhone.trim()) {
        const phoneRegex = /^1[3-9]\d{9}$/
        if (!phoneRegex.test(this.checkoutForm.contactPhone.trim())) {
          this.$message.warning('请输入正确的手机号')
          return
        }
      }
      
      try {
        this.checkoutSubmitting = true
        
        // 构建完整的退宿原因
        const reasonTypeLabels = {
          'graduation': '毕业离校',
          'leave_school': '休学/退学',
          'off_campus': '校外住宿',
          'other': '其他原因'
        }
        const fullReason = `【${reasonTypeLabels[this.checkoutForm.reasonType]}】${this.checkoutForm.reason}`
        
        const res = await submitApplication({
          type: '退宿申请',
          reason: fullReason,
          contactPhone: this.checkoutForm.contactPhone
        })
        
        if (res.code === 200) {
          this.$message.success('退宿申请提交成功，请等待管理员进行物资清点')
          this.showCheckoutDialog = false
          this.checkoutForm = { reasonType: '', reason: '', contactPhone: '' }
          this.loadApplications()
        }
        // 错误已在 request.js 拦截器中统一显示
      } catch (error) {
        console.error('提交退宿申请失败:', error)
        // 错误已在 request.js 拦截器中统一显示，这里不再重复
      } finally {
        this.checkoutSubmitting = false
      }
    },
    
    // 查看申请详情
    viewApplicationDetail(application) {
      this.currentApplication = application
      this.showDetailDialog = true
    },
    
    // 格式化申请时间
    formatApplicationTime(time) {
      if (!time) return '-'
      const date = new Date(time)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}`
    },
    
    // 格式化申请类型
    formatApplicationType(type) {
      if (!type) return '未知'
      const typeMap = {
        'TRANSFER': '调宿申请',
        'CHECKOUT': '退宿申请',
        'MATERIAL_REPORT': '物品申报',
        '调宿申请': '调宿申请',
        '退宿申请': '退宿申请',
        '物品申报': '物品申报'
      }
      return typeMap[type] || typeMap[type.toUpperCase()] || type
    },
    
    // 获取申请类型标签颜色
    getApplicationTypeTagType(type) {
      if (!type) return 'info'
      const typeColorMap = {
        'TRANSFER': 'primary',
        'CHECKOUT': 'warning',
        'MATERIAL_REPORT': 'success',
        '调宿申请': 'primary',
        '退宿申请': 'warning',
        '物品申报': 'success'
      }
      return typeColorMap[type] || typeColorMap[type.toUpperCase()] || 'info'
    },
    
    // 格式化申请状态
    formatApplicationStatus(status) {
      if (!status) return '未知'
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'INVENTORY_PASSED': '物资清点通过',
        'COMPENSATION_PENDING': '待赔偿',
        '待审核': '待审核',
        '已通过': '已通过',
        '已拒绝': '已拒绝',
        '物资清点通过': '物资清点通过',
        '待赔偿处理': '待赔偿'
      }
      return statusMap[status] || statusMap[status.toUpperCase()] || status
    },
    
    // 获取申请状态标签类型
    getApplicationStatusType(status) {
      if (!status) return 'info'
      const map = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'INVENTORY_PASSED': 'success',
        'COMPENSATION_PENDING': 'warning',
        '待审核': 'warning',
        '已通过': 'success',
        '已拒绝': 'danger',
        '物资清点通过': 'success',
        '待赔偿处理': 'warning'
      }
      return map[status] || map[status.toUpperCase()] || 'info'
    },
    
    // 判断是否为待审核状态
    isPending(status) {
      if (!status) return false
      return status === 'PENDING' || status === '待审核'
    },
    
    // 判断是否为调宿申请
    isTransferType(type) {
      if (!type) return false
      return type === 'TRANSFER' || type === '调宿申请'
    },
    
    // 判断是否为退宿申请
    isCheckoutType(type) {
      if (!type) return false
      return type === 'CHECKOUT' || type === '退宿申请'
    },
    
    // 判断是否为物品申报
    isMaterialReportType(type) {
      if (!type) return false
      return type === 'MATERIAL_REPORT' || type === '物品申报'
    },
    
    // 编辑申请
    editApplication(application) {
      this.editingApplication = application
      this.initEditForm(application)
      this.showEditDialog = true
      this.$nextTick(() => {
        if (this.$refs.editFormRef) {
          this.$refs.editFormRef.clearValidate()
        }
      })
    },
    
    // 从详情页编辑
    editApplicationFromDetail() {
      this.showDetailDialog = false
      this.editApplication(this.currentApplication)
    },
    
    // 初始化编辑表单
    initEditForm(application) {
      const type = application.type
      
      if (this.isTransferType(type)) {
        // 解析调宿申请的reason字段
        const reasonMatch = application.reason?.match(/^([^:：]+)[:：]?\s*(\d+)?[:：]?\s*(.*)$/)
        
        // 解析目标宿舍信息
        const targetInfo = application.targetDormInfo || ''
        const isRoommateType = targetInfo.includes('目标室友')
        
        this.editForm = {
          transferType: isRoommateType ? 'roommate' : 'room',
          targetRoom: isRoommateType ? '' : targetInfo,
          targetRoommateId: isRoommateType ? targetInfo.replace('目标室友学号:', '').trim() : '',
          reason: this.parseTransferReason(application.reason),
          description: this.parseTransferDescription(application.reason),
          contactPhone: application.contactPhone || '',
          checkoutReason: '',
          materialName: '',
          issueType: '',
          materialIssue: ''
        }
      } else if (this.isCheckoutType(type)) {
        // 解析退宿原因：格式为【原因类型】详细说明
        const checkoutReasonParsed = this.parseCheckoutReason(application.reason)
        this.editForm = {
          transferType: 'room',
          targetRoom: '',
          targetRoommateId: '',
          reason: '',
          description: '',
          contactPhone: '',
          checkoutReasonType: checkoutReasonParsed.reasonType,
          checkoutReason: checkoutReasonParsed.detail,
          checkoutContactPhone: application.contactPhone || '',
          materialName: '',
          issueType: '',
          materialIssue: ''
        }
      } else if (this.isMaterialReportType(type)) {
        // 解析物品申报的reason字段：【问题类型】物品名称 - 房间号：问题描述
        const parsed = this.parseMaterialReportReason(application.reason)
        this.editForm = {
          transferType: 'room',
          targetRoom: '',
          targetRoommateId: '',
          reason: '',
          description: '',
          contactPhone: '',
          checkoutReason: '',
          materialName: parsed.materialName,
          issueType: parsed.issueType,
          materialIssue: parsed.issue
        }
      }
    },
    
    // 解析调宿申请原因
    parseTransferReason(reason) {
      if (!reason) return ''
      const reasonMap = {
        '身体健康原因': 'health',
        '学习环境需要': 'study',
        '室友关系问题': 'roommate_conflict',
        '家庭特殊情况': 'family',
        '其他原因': 'other'
      }
      for (const [label, value] of Object.entries(reasonMap)) {
        if (reason.includes(label)) {
          return value
        }
      }
      return 'other'
    },
    
    // 解析调宿申请详细说明
    parseTransferDescription(reason) {
      if (!reason) return ''
      // 尝试提取冒号后的内容
      const match = reason.match(/[:：]\s*(.+)$/)
      return match ? match[1] : reason
    },
    
    // 解析物品申报原因
    parseMaterialReportReason(reason) {
      if (!reason) return { issueType: '', materialName: '', issue: '' }
      // 格式：【问题类型】物品名称 - 房间号：问题描述
      const issueTypeMatch = reason.match(/【([^】]+)】/)
      const materialNameMatch = reason.match(/】([^-]+)/)
      const issueMatch = reason.match(/：(.+)$/)
      
      return {
        issueType: issueTypeMatch ? issueTypeMatch[1] : '',
        materialName: materialNameMatch ? materialNameMatch[1].trim() : '',
        issue: issueMatch ? issueMatch[1] : ''
      }
    },
    
    // 解析退宿原因
    parseCheckoutReason(reason) {
      if (!reason) return { reasonType: '', detail: '' }
      // 格式：【原因类型】详细说明
      const reasonTypeMatch = reason.match(/【([^】]+)】/)
      const detailMatch = reason.match(/】(.+)$/)
      
      // 原因类型映射
      const reasonTypeMap = {
        '毕业离校': 'graduation',
        '休学/退学': 'leave_school',
        '校外住宿': 'off_campus',
        '其他原因': 'other'
      }
      
      const reasonTypeLabel = reasonTypeMatch ? reasonTypeMatch[1] : ''
      return {
        reasonType: reasonTypeMap[reasonTypeLabel] || 'other',
        detail: detailMatch ? detailMatch[1].trim() : reason
      }
    },
    
    // 提交编辑
    async submitEditApplication() {
      try {
        await this.$refs.editFormRef?.validate()
      } catch (e) {
        return
      }
      
      this.editSubmitting = true
      const type = this.editingApplication.type
      
      try {
        if (this.isMaterialReportType(type)) {
          // 物品申报使用专门的API
          const res = await updateMaterialReport(this.editingApplication.id, {
            materialName: this.editForm.materialName,
            issueType: this.editForm.issueType,
            issue: this.editForm.materialIssue
          })
          
          if (res.code === 200) {
            this.$message.success('修改成功')
            this.showEditDialog = false
            this.loadApplications()
          }
        } else {
          // 调宿/退宿申请
          let reason = ''
          if (this.isTransferType(type)) {
            const reasonLabels = {
              'health': '身体健康原因',
              'study': '学习环境需要',
              'roommate_conflict': '室友关系问题',
              'family': '家庭特殊情况',
              'other': '其他原因'
            }
            reason = `${reasonLabels[this.editForm.reason] || this.editForm.reason}: ${this.editForm.description}`
          } else if (this.isCheckoutType(type)) {
            // 构建完整的退宿原因
            const reasonTypeLabels = {
              'graduation': '毕业离校',
              'leave_school': '休学/退学',
              'off_campus': '校外住宿',
              'other': '其他原因'
            }
            reason = `【${reasonTypeLabels[this.editForm.checkoutReasonType] || '其他原因'}】${this.editForm.checkoutReason}`
          }
          
          // 构建目标宿舍信息
          let targetDormInfo = ''
          if (this.isTransferType(type)) {
            if (this.editForm.transferType === 'room' && this.editForm.targetRoom) {
              targetDormInfo = this.editForm.targetRoom
            } else if (this.editForm.transferType === 'roommate' && this.editForm.targetRoommateId) {
              targetDormInfo = '目标室友学号: ' + this.editForm.targetRoommateId
            }
          }
          
          // 根据申请类型获取联系电话
          const contactPhone = this.isCheckoutType(type) 
            ? this.editForm.checkoutContactPhone 
            : this.editForm.contactPhone
          
          const res = await updateStudentApplication(this.editingApplication.id, {
            reason: reason,
            targetDormInfo: targetDormInfo,
            contactPhone: contactPhone
          })
          
          if (res.code === 200) {
            this.$message.success('修改成功')
            this.showEditDialog = false
            this.loadApplications()
          }
        }
      } catch (error) {
        console.error('修改申请失败:', error)
      } finally {
        this.editSubmitting = false
      }
    },
    
    // 撤回申请
    async handleWithdraw(application) {
      try {
        await this.$confirm(
          '确定要撤回这条申请吗？撤回后将无法恢复。',
          '撤回申请',
          {
            confirmButtonText: '确定撤回',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const res = await withdrawApplication(application.id)
        if (res.code === 200) {
          this.$message.success('申请已撤回')
          this.loadApplications()
        } else {
          // 错误已在 request.js 拦截器中显示，这里不再重复显示
        }
      } catch (error) {
        // 用户取消操作，不做任何处理
        // 其他错误已在 request.js 拦截器中统一显示，这里不再重复
        if (error !== 'cancel') {
          console.error('撤回申请失败:', error)
        }
      }
    }
  }
}
</script>

<style scoped>
.dorm-management-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.dorm-info,
.application-section,
.application-history,
.transfer-form-section {
  margin-bottom: 30px;
}

.dorm-info h3,
.application-section h3,
.application-history h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 申请操作卡片 */
.action-card {
  text-align: center;
  transition: all 0.3s;
}

.action-card:hover {
  transform: translateY(-4px);
}

.action-card h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
}

.action-card p {
  margin: 0 0 15px 0;
  color: #909399;
  font-size: 14px;
}

/* 调宿申请表单区域 */
.transfer-form-section {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-header h3::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #409eff;
  border-radius: 2px;
}

/* 申请历史表格 */
.application-history .el-table {
  border-radius: 8px;
  overflow: hidden;
}

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
  max-height: 3em;
}

/* 申请详情 */
.application-detail {
  padding: 10px 0;
}

</style>
