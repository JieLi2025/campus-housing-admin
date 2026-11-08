<template>
  <div class="visitor-info-page">
    <div class="visitor-container">
      <!-- 访客登记表单卡片 -->
      <el-card class="visitor-card" v-if="!submitted">
        <template #header>
          <div class="card-header">
            <h2>访客登记</h2>
          </div>
        </template>
        
        <el-form 
          ref="visitorFormRef" 
          :model="visitorForm" 
          :rules="visitorRules" 
          label-width="0"
          class="visitor-form"
        >
          <!-- 访客姓名 -->
          <el-form-item prop="visitorName" class="form-item-custom">
            <el-input 
              v-model="visitorForm.visitorName" 
              placeholder="请输入访客姓名"
              clearable
              size="large"
              class="form-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <!-- 身份证号 -->
          <el-form-item prop="visitorIdCard" class="form-item-custom">
            <el-input 
              v-model="visitorForm.visitorIdCard" 
              placeholder="请输入身份证号"
              clearable
              size="large"
              class="form-input"
            >
              <template #prefix>
                <el-icon><CreditCard /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <!-- 被访学生姓名 -->
          <el-form-item prop="studentName" class="form-item-custom">
            <el-input 
              v-model="visitorForm.studentName" 
              placeholder="请输入被访学生姓名"
              clearable
              size="large"
              class="form-input"
              @blur="searchStudent"
              @input="handleStudentNameChange"
            >
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <!-- 楼栋 -->
          <el-form-item prop="buildingId" class="form-item-custom">
            <el-select 
              v-model.number="visitorForm.buildingId" 
              placeholder="请选择楼栋"
              size="large"
              class="form-input form-select"
              clearable
              @change="handleBuildingChange"
            >
              <el-option
                v-for="building in buildings"
                :key="building.id"
                :label="building.buildingName"
                :value="Number(building.id)"
              />
            </el-select>
          </el-form-item>
          
          <!-- 宿舍号 -->
          <el-form-item prop="studentRoom" class="form-item-custom">
            <el-input 
              v-model="visitorForm.studentRoom" 
              placeholder="请输入宿舍号"
              clearable
              size="large"
              class="form-input"
              @blur="handleRoomBlur"
            >
              <template #prefix>
                <el-icon><House /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <!-- 验证提示信息 -->
          <el-form-item v-if="validationMessage" class="form-item-custom">
            <el-alert
              :type="validationMessageType"
              :title="validationMessage"
              :closable="false"
              show-icon
            />
          </el-form-item>
          
          <!-- 访客电话 -->
          <el-form-item prop="visitorPhone" class="form-item-custom">
            <el-input 
              v-model="visitorForm.visitorPhone" 
              placeholder="请输入访客电话"
              clearable
              size="large"
              class="form-input"
            >
              <template #prefix>
                <el-icon><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <!-- 预计到访时间 -->
          <el-form-item prop="visitDateTime" class="form-item-custom">
            <el-date-picker
              v-model="visitorForm.visitDateTime"
              type="datetime"
              placeholder="请选择预计到访时间"
              size="large"
              class="form-input"
              style="width: 100%"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            >
              <template #prefix>
                <el-icon><Clock /></el-icon>
              </template>
            </el-date-picker>
          </el-form-item>
          
          <!-- 预计离开时间 -->
          <el-form-item prop="leaveDateTime" class="form-item-custom">
            <el-date-picker
              v-model="visitorForm.leaveDateTime"
              type="datetime"
              placeholder="请选择预计离开时间"
              size="large"
              class="form-input"
              style="width: 100%"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DD HH:mm"
            >
              <template #prefix>
                <el-icon><Clock /></el-icon>
              </template>
            </el-date-picker>
          </el-form-item>
          
          <!-- 来访事由 -->
          <el-form-item prop="visitReason" class="form-item-custom">
            <el-select 
              v-model="visitorForm.visitReason" 
              placeholder="请选择来访事由"
              size="large"
              class="form-input form-select"
              clearable
              allow-create
              filterable
            >
              <el-option label="探访同学" value="探访同学" />
              <el-option label="送物品" value="送物品" />
              <el-option label="办理事务" value="办理事务" />
              <el-option label="家长探望" value="家长探望" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          
          <div class="visitor-rules">
            <h3>访客须知</h3>
            <ul>
              <li>请如实填写访客信息，确保信息准确</li>
              <li>请在指定时间内完成访问，超时将自动失效</li>
              <li>请遵守宿舍管理规定，保持安静</li>
              <li>请勿携带违禁物品进入宿舍</li>
              <li>离开时请向管理员登记</li>
              <li>请携带有效身份证件，配合管理员查验</li>
            </ul>
          </div>
          
          <!-- 提交按钮 -->
          <el-form-item class="submit-button-item">
            <el-button 
              type="primary" 
              @click="submitForm" 
              :loading="submitting"
              size="large"
              class="submit-button"
            >
              访客登记
            </el-button>
          </el-form-item>
          
          <!-- 提示信息 -->
          <div class="form-hint">
            <span class="hint-text">访客无需注册，直接填写信息即可</span>
          </div>
        </el-form>
      </el-card>
      
      <!-- 提交成功后的信息展示 -->
      <el-card class="visitor-card" v-else>
        <template #header>
          <div class="card-header">
            <h2>访客登记成功</h2>
            <el-tag :type="getStatusTagType(visitorInfo.status)">{{ getStatusText(visitorInfo.status) }}</el-tag>
          </div>
        </template>
        
        <div class="content-wrapper">
          <div class="info-section">
            <div class="visitor-info">
              <div class="info-item">
                <span class="label">访客姓名：</span>
                <span class="value">{{ visitorInfo.visitorName }}</span>
              </div>
              <div class="info-item">
                <span class="label">访客电话：</span>
                <span class="value">{{ visitorInfo.visitorPhone }}</span>
              </div>
              <div class="info-item" v-if="visitorInfo.visitorIdCard">
                <span class="label">身份证号：</span>
                <span class="value">{{ visitorInfo.visitorIdCard }}</span>
              </div>
              <div class="info-item">
                <span class="label">被访学生：</span>
                <span class="value">{{ visitorInfo.studentName }}</span>
              </div>
              <div class="info-item">
                <span class="label">访问楼栋：</span>
                <span class="value">{{ visitorInfo.buildingName }}</span>
              </div>
              <div class="info-item">
                <span class="label">访问宿舍：</span>
                <span class="value">{{ visitorInfo.studentRoom }}</span>
              </div>
              <div class="info-item">
                <span class="label">来访日期：</span>
                <span class="value">{{ visitorInfo.visitDate }}</span>
              </div>
              <div class="info-item" v-if="visitorInfo.visitTime">
                <span class="label">预计到访时间：</span>
                <span class="value">{{ formatDateTime(visitorInfo.visitTime) }}</span>
              </div>
              <div class="info-item" v-if="visitorInfo.leaveTime">
                <span class="label">预计离开时间：</span>
                <span class="value">{{ formatDateTime(visitorInfo.leaveTime) }}</span>
              </div>
              <div class="info-item">
                <span class="label">来访事由：</span>
                <span class="value">{{ visitorInfo.visitReason }}</span>
              </div>
              <div class="info-item" v-if="targetAdmin">
                <span class="label">负责管理员：</span>
                <span class="value">{{ targetAdmin }}</span>
              </div>
            </div>
            
            <div class="success-message">
              <el-alert
                title="登记成功"
                type="success"
                :closable="false"
                show-icon
              >
                <template #default>
                  <p>您的访客登记已提交成功，管理员将尽快审核。</p>
                  <p v-if="targetAdmin">负责管理员：{{ targetAdmin }}</p>
                </template>
              </el-alert>
             </div>
          </div>
          
          <!-- 二维码区域 -->
          <div class="qr-section">
            <div class="qr-code-section">
              <h3>访客凭证</h3>
              <div class="qr-code" v-if="qrCodeUrl">
                <img :src="qrCodeUrl" alt="访客二维码" class="qr-image" />
                 </div>
              <div class="qr-code qr-placeholder" v-else>
                <el-icon><CreditCard /></el-icon>
                <p>生成中...</p>
               </div>
              <p class="qr-tip">请保存此二维码，进入宿舍时出示给管理员</p>
            </div>
          </div>
        </div>
        
        <div class="action-buttons">
          <el-button type="primary" @click="goBack">返回首页</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { createVisitor, validateStudentInfo as validateStudentInfoAPI, getVisitorStatus } from '@/api/visitor'
import { getAllBuildings } from '@/api/building'
import { getStudentByStudentId, getStudentList, getStudentById } from '@/api/student'
import { getStudentDorm } from '@/api/allocation'
import { createNotification } from '@/api/notification'
import { User, CreditCard, UserFilled, OfficeBuilding, House, Clock, Phone } from '@element-plus/icons-vue'
import QRCode from 'qrcode'

export default {
  name: 'VisitorInfo',
  components: {
    User,
    CreditCard,
    UserFilled,
    OfficeBuilding,
    House,
    Clock,
    Phone
  },
  data() {
    return {
      // 表单数据
      visitorForm: {
        visitorName: '',
        visitorPhone: '',
        visitorIdCard: '',
        relationship: '',
        studentName: '',
        buildingId: null,
        studentRoom: '',
        visitDateTime: '', // 预计到访时间（日期+时间）
        leaveDateTime: '', // 预计离开时间（日期+时间）
        visitDate: '', // 从visitDateTime提取
        visitTime: '', // 从visitDateTime提取
        duration: null,
        visitReason: '探访同学', // 默认值
        remarks: ''
      },
      
      // 表单验证规则
      visitorRules: {
        visitorName: [
          { required: true, message: '请输入访客姓名', trigger: 'blur' }
        ],
        visitorIdCard: [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
        ],
        studentName: [
          { required: true, message: '请输入被访学生姓名', trigger: 'blur' }
        ],
        buildingId: [
          { required: true, message: '请选择楼栋', trigger: 'change' }
        ],
        studentRoom: [
          { required: true, message: '请输入宿舍号', trigger: 'blur' }
        ],
        visitorPhone: [
          { required: true, message: '请输入访客电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        visitDateTime: [
          { required: true, message: '请选择预计到访时间', trigger: 'change' }
        ],
        leaveDateTime: [
          { required: true, message: '请选择预计离开时间', trigger: 'change' }
        ],
        visitReason: [
          { required: true, message: '请选择来访事由', trigger: 'change' }
        ]
      },
      
      // 楼栋列表
      buildings: [],
      
      // 提交状态
      submitted: false,
      submitting: false,
      
      // 提交成功后的信息
      visitorInfo: {},
      targetAdmin: '',
      buildingName: '',
      
      // 二维码
      qrCodeUrl: '',
      
      // 验证提示信息
      validationMessage: '',
      validationMessageType: 'error', // 'success' | 'warning' | 'error'
      validating: false, // 是否正在验证
      
      // 状态轮询定时器
      statusPollingTimer: null,
      
      // 刷新状态loading
      refreshingStatus: false
    }
  },
  mounted() {
    // 检查是否有路由查询参数传递的访客信息
    if (this.$route.query.visitorName) {
      // 如果有访客信息，显示成功页面
      this.loadVisitorInfoFromQuery()
    } else {
      // 否则显示登记表单
      this.loadBuildings()
    }
  },
  beforeUnmount() {
    // 清除状态轮询定时器
    this.stopStatusPolling()
  },
  watch: {
    // 监听路由变化，确保刷新后能正确显示
    '$route.query': {
      handler(newQuery) {
        if (newQuery.visitorName) {
          // 如果有访客信息，重新加载
          this.loadVisitorInfoFromQuery()
    } else {
          // 如果没有查询参数，显示表单
          this.submitted = false
          this.loadBuildings()
        }
      }
    }
  },
  methods: {
    // 从路由参数加载访客信息（用于显示成功页面）
    loadVisitorInfoFromQuery() {
      const query = this.$route.query
      if (query.visitorName) {
      this.visitorInfo = {
          visitorName: query.visitorName || '',
          visitorPhone: query.visitorPhone || '',
          visitorIdCard: query.visitorIdCard || '',
          studentName: query.studentName || '',
          buildingName: query.buildingName || '',
          studentRoom: query.studentRoom || '',
          visitDate: query.visitDate || '',
          visitTime: query.visitTime || '',
          leaveTime: query.leaveTime || '',
          visitReason: query.visitReason || '探访同学',
          status: query.status || 'PENDING' // 默认待审核
        }
        this.targetAdmin = query.targetAdmin || ''
        this.buildingName = query.buildingName || ''
        this.submitted = true
        // 生成二维码
        this.generateQRCode()
        // 启动状态轮询（每30秒查询一次）
        this.startStatusPolling()
      } else {
        // 没有查询参数，显示登记表单
        this.submitted = false
        this.loadBuildings()
      }
    },
    
    // 启动状态轮询
    startStatusPolling() {
      // 如果没有身份证号或来访日期，无法查询状态
      if (!this.visitorInfo.visitorIdCard || !this.visitorInfo.visitDate) {
        return
      }
      
      // 清除之前的定时器
      this.stopStatusPolling()
      
      // 立即查询一次
      this.fetchVisitorStatus()
      
      // 每10秒查询一次状态
      this.statusPollingTimer = setInterval(() => {
        this.fetchVisitorStatus()
      }, 10000)
    },
    
    // 停止状态轮询
    stopStatusPolling() {
      if (this.statusPollingTimer) {
        clearInterval(this.statusPollingTimer)
        this.statusPollingTimer = null
      }
    },
    
    // 查询访客状态
    async fetchVisitorStatus() {
      try {
        // 确保日期格式为 yyyy-MM-dd
        let visitDate = this.visitorInfo.visitDate
        if (visitDate && visitDate.length > 10) {
          visitDate = visitDate.substring(0, 10)
        }
        
        // 如果没有身份证号或日期，跳过查询
        if (!this.visitorInfo.visitorIdCard || !visitDate) {
          console.log('缺少身份证号或日期，跳过状态查询')
          return
        }
        
        const res = await getVisitorStatus(this.visitorInfo.visitorIdCard, visitDate)
        if (res.code === 200 && res.data) {
          const oldStatus = this.visitorInfo.status
          const newStatus = res.data.status
          
          // 更新状态
          this.visitorInfo.status = newStatus
          
          // 如果状态发生变化，重新生成二维码并提示用户
          if (oldStatus !== newStatus) {
            await this.generateQRCode()
            const statusLabel = this.getStatusText(newStatus)
            this.$message.success(`状态已更新：${statusLabel}`)
          }
          
          // 如果状态是已完成或已取消，停止轮询
          if (newStatus === 'COMPLETED' || newStatus === 'CANCELLED') {
            this.stopStatusPolling()
          }
        }
      } catch (error) {
        console.error('查询访客状态失败:', error)
        // 查询失败不影响页面显示，静默处理
      }
    },
    
    // 生成二维码
    async generateQRCode() {
      try {
        // 获取状态显示文本
        const statusText = this.getStatusText(this.visitorInfo.status)
        
        // 构建二维码内容（美观的文本格式，方便扫码查看）
        const qrText = `【访客登记凭证】
━━━━━━━━━━━━━━
访客姓名：${this.visitorInfo.visitorName}
联系电话：${this.visitorInfo.visitorPhone || '-'}
身份证号：${this.visitorInfo.visitorIdCard || '-'}
━━━━━━━━━━━━━━
被访学生：${this.visitorInfo.studentName}
访问楼栋：${this.visitorInfo.buildingName}
访问宿舍：${this.visitorInfo.studentRoom}
━━━━━━━━━━━━━━
来访日期：${this.visitorInfo.visitDate}
到访时间：${this.formatDateTime(this.visitorInfo.visitTime) || '-'}
离开时间：${this.formatDateTime(this.visitorInfo.leaveTime) || '-'}
来访事由：${this.visitorInfo.visitReason || '探访同学'}
审核状态：${statusText}
━━━━━━━━━━━━━━
请出示此凭证给管理员核验`
        
        // 生成二维码图片
        this.qrCodeUrl = await QRCode.toDataURL(qrText, {
          width: 300,
          margin: 2,
          color: {
            dark: '#000000',
            light: '#FFFFFF'
          }
        })
      } catch (error) {
        console.error('生成二维码失败:', error)
        this.qrCodeUrl = ''
      }
    },
    
    // 格式化日期时间
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return ''
      try {
        const date = new Date(dateTimeStr)
        if (isNaN(date.getTime())) {
          // 如果不是有效的日期字符串，直接返回
          return dateTimeStr
        }
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        return `${year}-${month}-${day} ${hours}:${minutes}`
      } catch (error) {
        return dateTimeStr
      }
    },
    
    // 获取状态显示文本
    getStatusText(status) {
      const statusMap = {
        'PENDING': '待审核',
        'APPROVED': '已通过',
        'REJECTED': '已拒绝',
        'CHECKED_IN': '已登记',
        'COMPLETED': '已离开',
        'CANCELLED': '已取消'
      }
      return statusMap[status] || status || '待审核'
    },
    
    // 获取状态标签类型（颜色）
    getStatusTagType(status) {
      const typeMap = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'CHECKED_IN': 'primary',
        'COMPLETED': 'info',
        'CANCELLED': 'info'
      }
      return typeMap[status] || 'warning'
    },
    
    // 加载楼栋列表
    async loadBuildings() {
      try {
        const res = await getAllBuildings()
        if (res.code === 200) {
          this.buildings = res.data || []
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
        this.$message.error('加载楼栋列表失败')
      }
    },
    
    // 楼栋选择变化
    handleBuildingChange(buildingId) {
      // 确保 buildingId 是数字类型
      const id = Number(buildingId)
      console.log('楼栋选择变化:', id, '类型:', typeof id, '原始值:', buildingId)
      
      if (id && !isNaN(id) && id > 0) {
        const building = this.buildings.find(b => Number(b.id) === id)
        if (building) {
          this.buildingName = building.buildingName
          // 确保 buildingId 是数字
          this.visitorForm.buildingId = Number(building.id)
          // 获取该楼栋的管理员
          this.getBuildingManager(building)
        }
      } else {
        // 如果选择被清空，重置相关字段
        this.visitorForm.buildingId = null
        this.buildingName = ''
        this.targetAdmin = ''
      }
      // 楼栋变化后，如果已填写学生姓名和宿舍号，自动验证
      this.autoValidate()
    },
    
    // 学生姓名变化
    handleStudentNameChange() {
      // 清除之前的验证信息
      this.validationMessage = ''
      // 如果已填写楼栋和宿舍号，自动验证
      this.autoValidate()
    },
    
    // 搜索学生信息（失焦时触发）
    async searchStudent() {
      // 如果输入的是学号格式，尝试自动填充学生信息
      const input = this.visitorForm.studentName?.trim()
      if (!input) {
        return
      }
      
      // 如果看起来像学号（纯数字或数字开头），尝试查询
      if (/^\d+/.test(input)) {
        try {
          // 尝试通过学号查询
          const res = await getStudentByStudentId(input)
          if (res.code === 200 && res.data) {
            const student = res.data
            this.visitorForm.studentName = student.realName || input
            
            // 查询学生的宿舍分配信息
            const dormRes = await getStudentDorm(student.id)
            if (dormRes.code === 200 && dormRes.data) {
              const dorm = dormRes.data
              if (dorm.buildingName && dorm.roomNumber) {
                // 找到对应的楼栋
                const building = this.buildings.find(b => b.buildingName === dorm.buildingName)
                if (building) {
                  const buildingId = Number(building.id)
                  this.visitorForm.buildingId = buildingId
                  this.handleBuildingChange(buildingId)
                }
                this.visitorForm.studentRoom = dorm.roomNumber
                
                // 自动验证
                await this.autoValidate()
              }
            }
          }
        } catch (error) {
          // 查询失败，忽略错误，让用户手动填写
          console.log('自动填充学生信息失败，需要手动填写')
        }
      } else {
        // 不是学号格式，触发自动验证
        await this.autoValidate()
      }
    },
    
    // 宿舍号失焦时验证
    async handleRoomBlur() {
      await this.autoValidate()
    },
    
    // 自动验证（当三个字段都填写时）
    async autoValidate() {
      // 如果三个字段都填写了，自动验证
      if (this.visitorForm.studentName && this.visitorForm.buildingId && this.visitorForm.studentRoom) {
        // 防抖：避免频繁调用
        if (this.validating) {
          return
        }
        
        this.validating = true
        this.validationMessage = '正在验证...'
        this.validationMessageType = 'info'
        
        try {
          const result = await this.validateStudentInfo()
          
          if (result.valid) {
            this.validationMessage = '✓ 验证通过'
            this.validationMessageType = 'success'
        } else {
            this.validationMessage = result.message || '验证失败'
            this.validationMessageType = 'error'
          }
        } catch (error) {
          this.validationMessage = '验证失败，请稍后重试'
          this.validationMessageType = 'error'
        } finally {
          this.validating = false
        }
      } else {
        // 如果字段不完整，清除验证信息
        this.validationMessage = ''
      }
    },
    
    // 获取楼栋管理员
    getBuildingManager(building) {
      if (building && building.managerName) {
        // managerName 存储的是 username，需要查询对应的真实姓名
        // 这里先使用 managerName，如果需要显示真实姓名，可以调用管理员API查询
        this.targetAdmin = building.managerName
      } else {
        this.targetAdmin = ''
      }
    },
    
    // 验证学生信息是否匹配楼栋和宿舍号（使用公开API）
    async validateStudentInfo() {
      console.log('验证学生信息 - buildingId:', this.visitorForm.buildingId, '类型:', typeof this.visitorForm.buildingId)
      
      if (!this.visitorForm.studentName || !this.visitorForm.buildingId || !this.visitorForm.studentRoom) {
        return { valid: false, message: '请填写学生姓名、楼栋和宿舍号' }
      }
      
      // 确保 buildingId 是数字类型
      const buildingId = Number(this.visitorForm.buildingId)
      if (isNaN(buildingId) || buildingId <= 0) {
        console.error('验证时 buildingId 无效:', this.visitorForm.buildingId)
        return { valid: false, message: '楼栋ID无效，请重新选择' }
      }
      
      try {
        // 调用公开的验证API
        const res = await validateStudentInfoAPI(
          this.visitorForm.studentName,
          buildingId, // 使用转换后的数字
          this.visitorForm.studentRoom
        )
        
        if (res.code === 200 && res.data) {
          const validationResult = res.data
          
          if (!validationResult.valid) {
            return { 
              valid: false, 
              message: validationResult.message || '验证失败'
            }
          }
          
          // 验证通过，可以自动修正楼栋和宿舍号
          if (validationResult.actualBuildingName && validationResult.actualRoomNumber) {
            // 如果实际楼栋与选择的不同，自动更新
            const actualBuilding = this.buildings.find(b => b.buildingName === validationResult.actualBuildingName)
            if (actualBuilding) {
              const actualBuildingId = Number(actualBuilding.id)
              const currentBuildingId = Number(this.visitorForm.buildingId)
              if (actualBuildingId !== currentBuildingId) {
                this.visitorForm.buildingId = actualBuildingId
                this.handleBuildingChange(actualBuildingId)
              }
            }
            
            // 如果实际宿舍号与输入的不同，自动更新
            if (validationResult.actualRoomNumber !== this.visitorForm.studentRoom) {
              this.visitorForm.studentRoom = validationResult.actualRoomNumber
            }
          }
          
          return { valid: true }
        } else {
          return { valid: false, message: res.message || '验证失败' }
        }
      } catch (error) {
        console.error('验证学生信息失败:', error)
        // 如果API调用失败，返回错误
        const errorMessage = error.response?.data?.message || error.message || '验证学生信息失败，请确认信息正确或联系管理员'
        return { valid: false, message: errorMessage }
      }
    },
    
    // 提交表单
    async submitForm() {
      // 先进行表单验证
      this.$refs.visitorFormRef.validate(async (valid) => {
        if (!valid) {
          this.$message.warning('请填写完整的必填信息')
          return
        }
        
        // 验证必填字段
        if (!this.visitorForm.studentName || !this.visitorForm.buildingId || !this.visitorForm.studentRoom) {
          this.$message.warning('请填写学生姓名、楼栋和宿舍号')
          return
        }
        
        // 🔥 强制验证：必须验证通过才能提交
        this.submitting = true
        try {
          const validationResult = await this.validateStudentInfo()
          
          if (!validationResult.valid) {
            this.submitting = false
            this.$message.error(validationResult.message || '验证失败，无法提交')
            // 更新验证提示
            this.validationMessage = validationResult.message || '验证失败'
            this.validationMessageType = 'error'
            return
          }
          
          // 验证通过，继续提交
          await this.doSubmit()
        } catch (error) {
          this.submitting = false
          console.error('验证失败:', error)
          const errorMessage = error.response?.data?.message || error.message || '验证失败，请稍后重试'
          this.$message.error(errorMessage)
          this.validationMessage = errorMessage
          this.validationMessageType = 'error'
        }
      })
    },
    
    // 执行提交（验证通过后）
    async doSubmit() {
      try {
        // 验证时间
        if (!this.visitorForm.visitDateTime || !this.visitorForm.leaveDateTime) {
          this.$message.warning('请选择预计到访时间和离开时间')
          this.submitting = false
          return
        }
        
        // 验证离开时间不能早于到访时间
        const visitTime = new Date(this.visitorForm.visitDateTime)
        const leaveTime = new Date(this.visitorForm.leaveDateTime)
        if (leaveTime <= visitTime) {
          this.$message.warning('离开时间必须晚于到访时间')
          this.submitting = false
          return
        }
        
        // 从日期时间中提取日期和时间
        const visitDateTime = new Date(this.visitorForm.visitDateTime)
        const visitDate = visitDateTime.toISOString().split('T')[0] // YYYY-MM-DD
        const visitTimeStr = `${String(visitDateTime.getHours()).padStart(2, '0')}:${String(visitDateTime.getMinutes()).padStart(2, '0')}` // HH:mm
        
        // 计算预计时长（小时）
        const duration = Math.ceil((leaveTime - visitTime) / (1000 * 60 * 60))
        
        // 如果没有填写电话，使用身份证号后4位作为默认值（仅用于后端必填字段）
        const visitorPhone = this.visitorForm.visitorPhone || (this.visitorForm.visitorIdCard ? this.visitorForm.visitorIdCard.slice(-4) : '0000')
        
        // 验证 buildingId 是否存在且有效
        console.log('提交前检查 buildingId:', this.visitorForm.buildingId, '类型:', typeof this.visitorForm.buildingId)
        
        if (!this.visitorForm.buildingId) {
          this.submitting = false
          this.$message.error('请选择楼栋')
          return
        }
        
        // 确保 buildingId 是数字类型
        const buildingId = Number(this.visitorForm.buildingId)
        if (isNaN(buildingId) || buildingId <= 0) {
          this.submitting = false
          console.error('buildingId 无效:', this.visitorForm.buildingId, '转换后:', buildingId)
          this.$message.error('楼栋ID无效，请重新选择')
          return
        }
        
        // 构建提交数据
        const submitData = {
          visitorName: this.visitorForm.visitorName,
          visitorPhone: visitorPhone,
          visitorIdCard: this.visitorForm.visitorIdCard || null,
          relationship: this.visitorForm.relationship || null,
          studentName: this.visitorForm.studentName,
          studentRoom: this.visitorForm.studentRoom,
          buildingId: buildingId, // 确保是数字类型
          visitDate: visitDate,
          visitTime: visitTimeStr,
          duration: duration > 0 ? duration : 1,
          visitReason: this.visitorForm.visitReason || '探访同学',
          remarks: this.visitorForm.remarks || null
        }
        
        // 提交访客登记（使用管理员端API，支持公开访问）
        // 注意：如果后端返回错误（code !== 200），响应拦截器会 reject，进入 catch 分支
        const res = await createVisitor(submitData)
        
        // 如果执行到这里，说明 res.code === 200（响应拦截器已经处理了错误情况）
        // 保存提交成功的信息
        this.visitorInfo = {
          visitorName: this.visitorForm.visitorName,
          visitorPhone: visitorPhone,
          visitorIdCard: this.visitorForm.visitorIdCard,
          studentName: this.visitorForm.studentName,
          buildingName: this.buildingName,
          studentRoom: this.visitorForm.studentRoom,
          visitDate: visitDate,
          visitTime: this.visitorForm.visitDateTime,
          leaveTime: this.visitorForm.leaveDateTime,
          visitReason: this.visitorForm.visitReason || '探访同学',
          status: 'PENDING' // 新提交的访客登记默认为待审核状态
        }
        
        // 发送通知给楼栋管理员
        await this.sendNotificationToAdmin(res.data?.id)
        
        this.submitted = true
        this.$message.success('访客登记提交成功')
        
        // 生成二维码
        await this.generateQRCode()
        
        // 更新URL，添加查询参数，这样刷新页面也能看到成功页面
        this.$router.replace({
          path: '/visitor-info',
          query: {
            visitorName: this.visitorInfo.visitorName,
            visitorPhone: this.visitorInfo.visitorPhone,
            visitorIdCard: this.visitorInfo.visitorIdCard,
            studentName: this.visitorInfo.studentName,
            buildingName: this.visitorInfo.buildingName,
            studentRoom: this.visitorInfo.studentRoom,
            visitDate: this.visitorInfo.visitDate,
            visitTime: this.visitorInfo.visitTime,
            leaveTime: this.visitorInfo.leaveTime,
            visitReason: this.visitorInfo.visitReason,
            status: this.visitorInfo.status,
            targetAdmin: this.targetAdmin
          }
        })
      } catch (error) {
        console.error('提交访客登记失败:', error)
        // 响应拦截器已经显示了错误消息，这里只需要更新验证状态
        // 如果是验证错误，更新验证提示
        const errorMessage = error.response?.data?.message || error.message || ''
        if (errorMessage.includes('学生') || errorMessage.includes('不在') || errorMessage.includes('验证') || errorMessage.includes('未找到')) {
          this.validationMessage = errorMessage
          this.validationMessageType = 'error'
        }
        // 不显示额外的错误消息，因为响应拦截器已经显示了
      } finally {
        this.submitting = false
      }
    },
    
    // 发送通知给楼栋管理员
    async sendNotificationToAdmin(visitorId) {
      if (!this.visitorForm.buildingId || !this.targetAdmin) {
        console.warn('无法发送通知：缺少楼栋或管理员信息')
        return
      }
      
      try {
        // 获取楼栋信息
        const building = this.buildings.find(b => b.id === this.visitorForm.buildingId)
        if (!building) {
          console.warn('未找到楼栋信息')
          return
        }
        
        // 构建通知内容
        const notificationTitle = '新的访客登记申请'
        const visitTimeStr = this.visitorForm.visitDateTime || this.visitorForm.visitDate
        const leaveTimeStr = this.visitorForm.leaveDateTime || ''
        const notificationMessage = `访客${this.visitorForm.visitorName}申请访问${this.buildingName}${this.visitorForm.studentRoom}宿舍，被访学生：${this.visitorForm.studentName}，预计到访时间：${visitTimeStr}${leaveTimeStr ? '，预计离开时间：' + leaveTimeStr : ''}，请及时审核。`
        
        // 发送通知给管理员
        // 注意：这里需要根据实际的后端API调整
        // 如果后端支持根据楼栋ID或管理员username发送通知，应该调用相应的API
        // 目前使用通用的创建通知API，targetUserId需要是管理员的用户ID
        // 由于我们只有managerName（username），可能需要先查询管理员的ID
        
        // 简化处理：创建通知，目标角色为管理员
        const notificationData = {
          title: notificationTitle,
          message: notificationMessage,
          type: 'info',
          targetRole: 'ADMIN',
          icon: 'visitor'
        }
        
        // 如果后端支持根据楼栋发送通知，可以添加buildingId字段
        if (building.id) {
          notificationData.buildingId = building.id
        }
        
        await createNotification(notificationData)
        console.log('通知已发送给管理员')
      } catch (error) {
        console.error('发送通知失败:', error)
        // 通知发送失败不影响主流程，只记录错误
      }
    },
    
    // 重置表单
    resetForm() {
      this.$refs.visitorFormRef.resetFields()
      this.visitorForm = {
        visitorName: '',
        visitorPhone: '',
        visitorIdCard: '',
        relationship: '',
        studentName: '',
        buildingId: null,
        studentRoom: '',
        visitDateTime: '',
        leaveDateTime: '',
        visitDate: '',
        visitTime: '',
        duration: null,
        visitReason: '探访同学',
        remarks: ''
      }
      this.targetAdmin = ''
      this.buildingName = ''
    },
    
    // 返回首页
    goBack() {
      this.$router.push('/')
    },
    
    // 手动刷新状态
    async refreshStatus() {
      this.refreshingStatus = true
      try {
        await this.fetchVisitorStatus()
        this.$message.info('状态已刷新')
      } catch (error) {
        console.error('刷新状态失败:', error)
      } finally {
        this.refreshingStatus = false
      }
    }
  }
}
</script>

<style scoped>
/* 基础页面样式 */
.visitor-info-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 1.25rem;
  overflow-y: auto;
  box-sizing: border-box;
  width: 100vw !important;
  max-width: 100vw !important;
  position: relative !important;
}

/* 覆盖全局CSS的所有影响 */
.visitor-info-page {
  /* 覆盖body的flex布局影响 */
  position: relative !important;
  width: 100vw !important;
  height: 100vh !important;
  max-width: 100vw !important;
  overflow-x: visible !important;
}

/* 覆盖#app的所有限制 */
.visitor-info-page #app {
  display: block !important;
  grid-template-columns: none !important;
  max-width: none !important;
  margin: 0 !important;
  padding: 0 !important;
  width: 100% !important;
  height: 100% !important;
}

.visitor-container {
  width: 100% !important;
  max-width: 75rem; /* 1200px */
  margin: 1.25rem 0;
  box-sizing: border-box;
  overflow: visible;
  position: relative !important;
}

.visitor-card {
  border-radius: 1rem;
  box-shadow: 0 0.75rem 2.5rem rgba(0, 0, 0, 0.15);
  width: 100% !important;
  max-width: 100% !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 0;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 1.75rem;
  font-weight: 700;
}

/* 响应式内容包装器 */
.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.875rem;
  width: 100%;
}

/* 左侧信息区域 */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 1.875rem;
}

.visitor-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 0.75rem;
  border: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.info-item:hover {
  background: #f0f2f5;
  border-color: #c0c4cc;
  transform: translateY(-0.125rem);
  box-shadow: 0 0.25rem 0.75rem rgba(0, 0, 0, 0.1);
}

.label {
  font-weight: 600;
  color: #606266;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.value {
  color: #303133;
  font-size: 1rem;
  line-height: 1.5;
  word-break: break-word;
  overflow-wrap: break-word;
}



/* 右侧二维码区域 */
.qr-section {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.qr-code-section {
  text-align: center;
  padding: 1.875rem;
  background: #f8f9fa;
  border-radius: 1rem;
  border: 1px solid #e4e7ed;
  width: 100%;
  max-width: 21.875rem; /* 350px */
  min-height: 15rem; /* 240px - 避免布局抖动 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.qr-code-section h3 {
  margin: 0 0 1.5rem 0;
  color: #303133;
  font-size: 1.25rem;
}

.qr-code {
  display: inline-block;
  width: 12.5rem; /* 200px */
  height: 12.5rem; /* 200px */
  background: white;
  border: 2px dashed #dcdfe6;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.25rem;
  transition: all 0.3s ease;
}

.qr-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 0.75rem;
}

.qr-code:hover {
  border-color: #409eff;
  box-shadow: 0 0.25rem 0.75rem rgba(64, 158, 255, 0.2);
}

.qr-placeholder {
  text-align: center;
  color: #909399;
}

.qr-placeholder .el-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: #c0c4cc;
}

.qr-tip {
  color: #606266;
  font-size: 0.875rem;
  margin: 0;
  line-height: 1.5;
}

/* 申请状态信息样式 */
.application-info {
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 0.75rem;
  padding: 1.25rem;
  margin-bottom: 1.5rem;
}

.application-info h3 {
  margin: 0 0 1rem 0;
  color: #374151;
  font-size: 1.125rem;
  font-weight: 600;
}

.application-info .info-item {
  display: flex;
  align-items: center;
  margin-bottom: 0.75rem;
}

.application-info .info-item:last-child {
  margin-bottom: 0;
}

.application-info .label {
  font-weight: 500;
  color: #6b7280;
  min-width: 6rem;
  margin-right: 0.5rem;
}

.application-info .value {
  color: #374151;
  font-weight: 500;
}

/* 表单样式 */
.visitor-form {
  padding: 2rem 0;
}

.form-item-custom {
  margin-bottom: 1.5rem;
}

.form-item-custom .el-form-item__content {
  width: 100%;
}

.form-input {
  width: 100%;
}

.form-input .el-input__wrapper {
  padding-left: 2.5rem;
}

.form-input .el-input__prefix {
  left: 0.75rem;
  color: #909399;
}

.form-select {
  width: 100%;
}

.form-select .el-select__wrapper {
  padding-left: 2.5rem;
}

.submit-button-item {
  margin-top: 2rem;
  margin-bottom: 1rem;
}

.submit-button {
  width: 100%;
  height: 3.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.submit-button:hover {
  background: linear-gradient(135deg, #5568d3 0%, #653a8f 100%);
}

.form-hint {
  text-align: center;
  margin-top: 1rem;
}

.hint-text {
  color: #67c23a;
  font-size: 0.875rem;
}

/* 访客须知 */
.visitor-rules {
  padding: 1.5rem;
  background: #fff7e6;
  border-radius: 1rem;
  border-left: 4px solid #e6a23c;
  box-shadow: 0 0.125rem 0.5rem rgba(230, 162, 60, 0.1);
  margin: 1.5rem 0;
}

.visitor-rules h3 {
  margin: 0 0 1.25rem 0;
  color: #e6a23c;
  font-size: 1.125rem;
}

.visitor-rules ul {
  margin: 0;
  padding-left: 1.25rem;
}

.visitor-rules li {
  margin-bottom: 0.75rem;
  color: #606266;
  line-height: 1.6;
  font-size: 0.9375rem;
}

/* 成功消息样式 */
.success-message {
  margin-top: 1.5rem;
}

.success-message .el-alert {
  border-radius: 0.75rem;
}

.success-message .el-alert p {
  margin: 0.5rem 0;
  line-height: 1.6;
}



/* 操作按钮 */
.action-buttons {
  text-align: center;
  display: flex;
  flex-direction: row;
  gap: 1rem;
  justify-content: center;
  padding: 1.25rem 0;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  min-width: 8.75rem;
  height: 3rem;
  font-size: 1rem;
}

/* ===== 响应式断点设计 ===== */

/* 大屏布局：≥1024px - Flex 并列显示 */
@media (min-width: 1024px) {
  /* 覆盖全局CSS的所有影响 */
  .visitor-info-page {
    position: relative !important;
    width: 100vw !important;
    height: 100vh !important;
    display: flex !important;
    align-items: flex-start !important;
    justify-content: center !important;
  }
  
  /* 覆盖#app的所有限制 */
  .visitor-info-page #app {
    display: block !important;
    grid-template-columns: none !important;
    max-width: none !important;
    margin: 0 !important;
    padding: 0 !important;
    width: 100% !important;
    height: 100% !important;
  }
  
  /* 强制重置所有可能的冲突样式 */
  .visitor-container {
    max-width: 75rem !important; /* 1200px */
    width: 100% !important;
  }
  
  /* 内容包装器：强制Flex并列布局 */
  .content-wrapper {
    display: flex !important;
    flex-direction: row !important;
    gap: 1.5rem !important; /* 24px 间距 */
    align-items: flex-start !important;
    width: 100% !important;
  }
  
  /* 左侧信息区域：强制70%占比 */
  .info-section {
    display: flex !important;
    flex-direction: column !important;
    flex: 0 0 70% !important;
    max-width: 70% !important;
    width: 70% !important;
    gap: 1.875rem !important;
  }
  
  /* 右侧二维码区域：强制30%占比 */
  .qr-section {
    display: flex !important;
    flex: 0 0 30% !important;
    max-width: 30% !important;
    width: 30% !important;
    justify-content: flex-start !important;
  }
  
  /* 信息项：强制水平布局 */
  .info-item {
    display: flex !important;
    flex-direction: row !important;
    align-items: center !important;
    padding: 1.25rem !important;
    width: 100% !important;
  }
  
  .label {
    min-width: 8.75rem !important; /* 140px */
    margin-bottom: 0 !important;
    margin-right: 1rem !important;
    font-size: 1rem !important;
    flex-shrink: 0 !important;
  }
  
  .value {
    flex: 1 !important;
    font-size: 1rem !important;
    word-break: break-word !important;
    overflow-wrap: break-word !important;
  }
  

  
  /* 二维码区域：强制调整 */
  .qr-code-section {
    padding: 2.5rem !important;
    max-width: 100% !important;
    min-height: 18.75rem !important; /* 300px */
    width: 100% !important;
  }
  
  .qr-code {
    width: 15.625rem !important; /* 250px */
    height: 15.625rem !important; /* 250px */
  }
  
  .qr-code-section h3 {
    font-size: 1.375rem !important;
  }
  
  /* 访客须知调整 */
  .visitor-rules {
    padding: 1.875rem !important;
  }
  
  .visitor-rules h3 {
    font-size: 1.25rem !important;
  }
  
  .visitor-rules li {
    font-size: 1rem !important;
  }
  

  
  /* 按钮：强制水平排列 */
  .action-buttons {
    display: flex !important;
    flex-direction: row !important;
    gap: 1.25rem !important;
  }
  
  .action-buttons .el-button {
    min-width: 10rem !important;
    height: 3.25rem !important;
    font-size: 1.125rem !important;
  }
  
  /* 标题调整 */
  .card-header h2 {
    font-size: 2rem !important;
  }
}

/* 超大屏布局：≥1400px - 更宽松的间距 */
@media (min-width: 1400px) {
  .visitor-container {
    max-width: 87.5rem; /* 1400px */
  }
  
  .content-wrapper {
    gap: 3.75rem; /* 60px 间距 */
  }
  
  .qr-code {
    width: 17.5rem; /* 280px */
    height: 17.5rem; /* 280px */
  }
  
  .qr-code-section {
    padding: 3rem;
    min-height: 21.875rem; /* 350px */
  }
}

/* 小屏布局：<1024px - 垂直堆叠（默认样式已实现） */
/* 平板端优化：768px - 1023px */
@media (min-width: 768px) and (max-width: 1023px) {
  .visitor-container {
    max-width: 50rem; /* 800px */
  }
  
  .info-item {
    padding: 1.125rem;
  }
  
  .label {
    font-size: 0.9375rem;
  }
  
  .value {
    font-size: 0.9375rem;
  }
  
  .qr-code {
    width: 12.5rem; /* 200px */
    height: 12.5rem; /* 200px */
  }
  
  .qr-code-section {
    padding: 1.5rem;
    min-height: 13.75rem; /* 220px */
  }
}

/* 移动端优化：<768px */
@media (max-width: 767px) {
  .visitor-info-page {
    padding: 0.625rem;
  }
  
  .visitor-container {
    max-width: 100%;
    margin: 0.625rem 0;
  }
  
  .card-header {
    padding: 1rem 0;
  }
  
  .card-header h2 {
    font-size: 1.375rem;
  }
  
  .content-wrapper {
    gap: 1.25rem;
  }
  
  .info-item {
    padding: 1rem;
  }
  
  .label {
    font-size: 0.875rem;
  }
  
  .value {
    font-size: 1rem;
  }
  

  
  .qr-code-section {
    padding: 1.25rem;
    min-height: 11.25rem; /* 180px */
  }
  
  .qr-code {
    width: 11.25rem; /* 180px */
    height: 11.25rem; /* 180px */
  }
  
  .qr-placeholder .el-icon {
    font-size: 2.5rem;
  }
  
  .visitor-rules {
    padding: 1.25rem;
  }
  
  .action-buttons {
    align-items: center;
  }
  
  .action-buttons .el-button {
    width: 100%;
    height: 2.75rem;
  }
}

/* 超小屏优化：<480px */
@media (max-width: 479px) {
  .visitor-info-page {
    padding: 0.3125rem;
  }
  
  .visitor-container {
    margin: 0.3125rem 0;
  }
  
  .card-header h2 {
    font-size: 1.25rem;
  }
  
  .info-item {
    padding: 0.75rem;
  }
  
  .label {
    font-size: 0.8125rem;
  }
  
  .value {
    font-size: 0.9375rem;
  }
  
  .qr-code {
    width: 9.375rem; /* 150px */
    height: 9.375rem; /* 150px */
  }
  
  .qr-placeholder .el-icon {
    font-size: 2rem;
  }
}
</style>
