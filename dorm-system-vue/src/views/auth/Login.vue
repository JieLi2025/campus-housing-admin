<template>
  <div class="login-container">
    <!-- 左侧登录区域 -->
    <div class="login-section">
      <div class="login-card">
        <div class="login-header">
          <h1 class="login-title">高校学生住宿管理平台</h1>
          <p class="login-subtitle">欢迎回来，请登录您的账号</p>
        </div>
        
        <!-- 用户类型选择 -->
        <div class="user-type-selector">
          <el-radio-group v-model="userType" @change="handleUserTypeChange" class="user-type-group">
            <el-radio-button label="student">学生</el-radio-button>
            <el-radio-button label="admin">管理员</el-radio-button>
            <el-radio-button label="visitor">访客</el-radio-button>
          </el-radio-group>
        </div>
        
        <el-form 
          ref="loginForm" 
          :model="loginForm" 
          :rules="loginRules" 
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <!-- 学生登录字段 -->
          <template v-if="userType === 'student'">
            <el-form-item prop="studentId">
              <el-input
                v-model.trim="loginForm.studentId"
                placeholder="请输入学号"
                prefix-icon="User"
                size="large"
                clearable
                maxlength="12"
                @keyup.enter.stop="handleLogin"
              />
            </el-form-item>
          </template>
          
          <!-- 管理员登录字段 -->
          <template v-else-if="userType === 'admin'">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入管理员用户名"
                prefix-icon="User"
                size="large"
                clearable
                autocomplete="off"
                @keyup.enter.stop="handleLogin"
              />
            </el-form-item>
          </template>
          
          <!-- 访客登录字段 -->
          <template v-else-if="userType === 'visitor'">
            <!-- 访客操作切换 -->
            <div class="visitor-mode-switch">
              <el-radio-group v-model="visitorMode" size="small">
                <el-radio-button label="register">新访客登记</el-radio-button>
                <el-radio-button label="query">查询登记状态</el-radio-button>
              </el-radio-group>
            </div>
            
            <!-- 查询登记状态模式 -->
            <template v-if="visitorMode === 'query'">
              <el-form-item prop="queryIdCard">
                <el-input
                  v-model="loginForm.queryIdCard"
                  placeholder="请输入身份证号"
                  prefix-icon="Document"
                  size="large"
                  clearable
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
              
              <el-form-item prop="queryDate">
                <el-date-picker
                  v-model="loginForm.queryDate"
                  type="date"
                  placeholder="请选择来访日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  size="large"
                  style="width: 100%"
                />
              </el-form-item>
            </template>
            
            <!-- 新访客登记模式 -->
            <template v-else>
              <el-form-item prop="visitorName">
                <el-input
                  v-model="loginForm.visitorName"
                  placeholder="请输入访客姓名"
                  prefix-icon="User"
                  size="large"
                  clearable
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
              
              <el-form-item prop="idCard">
                <el-input
                  v-model="loginForm.idCard"
                  placeholder="请输入身份证号"
                  prefix-icon="Document"
                  size="large"
                  clearable
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
              
              <el-form-item prop="visitorPhone">
                <el-input
                  v-model="loginForm.visitorPhone"
                  placeholder="请输入联系电话"
                  prefix-icon="Phone"
                  size="large"
                  clearable
                  maxlength="11"
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
              <el-form-item prop="visitedStudent">
                <el-input
                  v-model="loginForm.visitedStudent"
                  placeholder="请输入被访学生姓名"
                  prefix-icon="User"
                  size="large"
                  clearable
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
              
              <el-form-item prop="buildingId">
                <el-select
                  v-model.number="loginForm.buildingId"
                  placeholder="请选择楼栋"
                  size="large"
                  style="width: 100%"
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
              
              <el-form-item prop="visitDorm">
                <el-input
                  v-model="loginForm.visitDorm"
                  placeholder="请输入访问宿舍"
                  prefix-icon="House"
                  size="large"
                  clearable
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
              
              <el-form-item prop="visitTime">
                <el-date-picker
                  v-model="loginForm.visitTime"
                  type="datetime"
                  placeholder="请选择预计到访时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DD HH:mm"
                  size="large"
                  style="width: 100%"
                />
              </el-form-item>
              
              <el-form-item prop="visitReason">
                <el-input
                  v-model="loginForm.visitReason"
                  placeholder="请输入来访事由"
                  prefix-icon="Document"
                  size="large"
                  clearable
                  maxlength="200"
                  @keyup.enter.stop="handleLogin"
                />
              </el-form-item>
            </template>
          </template>
          
          <el-form-item prop="password" v-if="userType !== 'visitor'">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
              autocomplete="new-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="login-button"
              :loading="loading"
              :disabled="loading"
              @click="handleLogin"
            >
              {{ getLoginButtonText() }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-footer">
          <el-link type="primary" @click="$router.push('/forgot-password')" v-if="userType === 'student'">
            忘记密码？
          </el-link>
          <span class="divider" v-if="userType === 'student'">|</span>
          <el-link type="primary" @click="$router.push('/register')" v-if="userType === 'student'">
            注册新账号
          </el-link>
          <span v-else-if="userType === 'admin'" class="no-register">管理员账号请联系超级管理员创建</span>
          <span v-else-if="userType === 'visitor'" class="visitor-info">访客无需注册，直接填写信息即可</span>
        </div>
      </div>
    </div>

    <!-- 右侧介绍区域 -->
    <div class="intro-section">
      <div class="intro-content">
        <div class="intro-header">
          <h2 class="intro-title">高校学生住宿管理系统</h2>
          <p class="intro-subtitle">为高校师生提供全方位的住宿服务解决方案</p>
        </div>
        
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><House /></el-icon>
            </div>
            <div class="feature-content">
              <h3>自由组建宿舍</h3>
              <p>学生自主组队选择室友，打造理想的宿舍生活环境</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="feature-content">
              <h3>社交互动平台</h3>
              <p>宿舍内部交流、活动组织，促进同学间的友谊</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Tools /></el-icon>
            </div>
            <div class="feature-content">
              <h3>便捷服务管理</h3>
              <p>维修申请、访客预约、公告通知，一站式服务</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="feature-content">
              <h3>实时解答疑惑</h3>
              <p>管理员在线解答学生问题，快速响应各类需求</p>
            </div>
          </div>
        </div>
        
        <div class="contact-section">
          <div class="contact-title">系统管理员联系方式</div>
          <div class="contact-info">
            <div class="contact-item">
              <el-icon><User /></el-icon>
              <span>{{ adminContact.name }}</span>
            </div>
            <div class="contact-item">
              <el-icon><Message /></el-icon>
              <span>{{ adminContact.email }}</span>
            </div>
            <div class="contact-item">
              <el-icon><Phone /></el-icon>
              <span>{{ adminContact.phone }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { studentLogin, adminLogin, getAdminContact } from '@/api/auth.js'
import { setCurrentUser, setToken } from '@/utils/auth.js'
import { registerVisitor } from '@/api/visitor'
import { getAllBuildings } from '@/api/building'

export default {
  name: 'Login',
  data() {
    return {
      loading: false,
      userType: 'student', // 默认选择学生登录
      visitorMode: 'register', // 访客模式：register-新登记，query-查询状态
      buildings: [], // 楼栋列表
      // 管理员联系方式（从后端获取）
      adminContact: {
        name: '系统管理员',
        email: 'admin@campus.edu.cn',
        phone: '请联系学校管理部门'
      },
      loginForm: {
        email: '',
        studentId: '',
        username: '',
        password: '',
        visitorName: '',
        visitorPhone: '',
        idCard: '',
        visitedStudent: '',
        buildingId: null,
        visitDorm: '',
        visitTime: '',
        visitReason: '',
        // 查询状态用的字段
        queryIdCard: '',
        queryDate: ''
      },
      loginRules: {
        studentId: [
          { required: true, message: '请输入学号', trigger: 'blur' },
          { pattern: /^\d{8,12}$/, message: '学号格式不正确，请输入8-12位数字', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '请输入管理员用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 8, message: '密码至少8位', trigger: 'blur' }
        ],
        visitorName: [
          { required: true, message: '请输入访客姓名', trigger: 'blur' },
          { min: 2, max: 10, message: '姓名长度在2-10个字符', trigger: 'blur' }
        ],
        idCard: [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '身份证号格式不正确', trigger: 'blur' }
        ],
        visitorPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        visitedStudent: [
          { required: true, message: '请输入被访学生姓名', trigger: 'blur' },
          { min: 2, max: 10, message: '姓名长度在2-10个字符', trigger: 'blur' }
        ],
        buildingId: [
          { required: true, message: '请选择楼栋', trigger: 'change' }
        ],
        visitDorm: [
          { required: true, message: '请输入访问宿舍', trigger: 'blur' }
        ],
        visitTime: [
          { required: true, message: '请选择预计到访时间', trigger: 'change' }
        ],
        visitReason: [
          { required: true, message: '请输入来访事由', trigger: 'blur' },
          { min: 2, max: 200, message: '来访事由长度在2-200个字符', trigger: 'blur' }
        ],
        queryIdCard: [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, message: '身份证号格式不正确', trigger: 'blur' }
        ],
        queryDate: [
          { required: true, message: '请选择来访日期', trigger: 'change' }
        ]
      }
    }
  },
  mounted() {
    // 加载楼栋列表
    this.loadBuildings()
    // 加载管理员联系方式
    this.loadAdminContact()
  },
  methods: {
    // 加载楼栋列表
    async loadBuildings() {
      try {
        const res = await getAllBuildings()
        if (res.code === 200) {
          this.buildings = res.data || []
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
      }
    },
    
    // 加载管理员联系方式
    async loadAdminContact() {
      try {
        const res = await getAdminContact()
        if (res.code === 200 && res.data) {
          this.adminContact = res.data
        }
      } catch (error) {
        console.error('加载管理员联系方式失败:', error)
        // 失败时保持默认值
      }
    },
    
    // 楼栋选择变化
    handleBuildingChange(buildingId) {
      // 确保 buildingId 是数字类型
      if (buildingId) {
        const id = Number(buildingId)
        if (!isNaN(id) && id > 0) {
          this.loginForm.buildingId = id
        }
      }
    },
    
    handleUserTypeChange() {
      // 切换用户类型时清空表单
      this.visitorMode = 'register' // 重置访客模式
      this.loginForm = {
        email: '',
        studentId: '',
        username: '',
        password: '',
        visitorName: '',
        visitorPhone: '',
        idCard: '',
        visitedStudent: '',
        buildingId: null,
        visitDorm: '',
        visitTime: '',
        leaveTime: '',
        queryIdCard: '',
        queryDate: ''
      }
      // 清除表单验证
      this.$nextTick(() => {
        if (this.$refs.loginForm) {
          this.$refs.loginForm.clearValidate()
        }
      })
    },
    
    getLoginButtonText() {
      if (this.userType === 'visitor') {
        return this.visitorMode === 'query' ? '查询状态' : '访客登记'
      }
      const buttonTexts = {
        'student': '学生登录',
        'admin': '管理员登录'
      }
      return buttonTexts[this.userType] || '登录'
    },
    
    async handleLogin() {
      // 防止重复提交
      if (this.loading) {
        return
      }
      
      // 关闭所有现有的消息提示，防止堆叠
      this.$message.closeAll()
      
      try {
        // 手动检查必填字段
        if (this.userType === 'student') {
          if (!this.loginForm.studentId) {
            this.$message.error('请输入学号')
            return
          }
          if (!this.loginForm.password) {
            this.$message.error('请输入密码')
            return
          }
        } else if (this.userType === 'admin') {
          if (!this.loginForm.username) {
            this.$message.error('请输入管理员用户名')
            return
          }
          if (!this.loginForm.password) {
            this.$message.error('请输入密码')
            return
          }
        }
        
        this.loading = true
        
        // 根据用户类型处理不同的逻辑
        if (this.userType === 'visitor') {
          this.loading = true
          
          // 查询状态模式
          if (this.visitorMode === 'query') {
            try {
              if (!this.loginForm.queryIdCard || !this.loginForm.queryDate) {
                this.$message.warning('请填写身份证号和来访日期')
                this.loading = false
                return
              }
              
              // 调用查询状态API
              const { getVisitorStatus } = await import('@/api/visitor')
              const res = await getVisitorStatus(this.loginForm.queryIdCard, this.loginForm.queryDate)
              
              this.loading = false
              
              if (res.code === 200 && res.data) {
                // 跳转到访客信息页面显示状态
                this.$router.push({
                  path: '/visitor-info',
                  query: {
                    visitorName: res.data.visitorName,
                    visitorPhone: res.data.visitorPhone || '',
                    visitorIdCard: res.data.visitorIdCard || this.loginForm.queryIdCard,
                    studentName: res.data.studentName,
                    buildingName: res.data.buildingName || '',
                    studentRoom: res.data.studentRoom,
                    visitDate: res.data.visitDate,
                    visitTime: res.data.visitDate + ' ' + (res.data.visitTime || '00:00'),
                    visitReason: res.data.visitReason || '探访同学',
                    status: res.data.status
                  }
                })
              }
            } catch (error) {
              this.loading = false
              console.error('查询状态失败:', error)
            }
            return
          }
          
          // 访客登记逻辑 - 调用后端API
          try {
            // 验证必填字段
            if (!this.loginForm.visitorName || !this.loginForm.visitorPhone || !this.loginForm.idCard || !this.loginForm.visitedStudent || 
                !this.loginForm.buildingId || !this.loginForm.visitDorm || !this.loginForm.visitTime) {
              this.$message.warning('请填写完整的访客信息')
              this.loading = false
              return
            }
            
            // 确保 buildingId 是数字类型
            const buildingId = Number(this.loginForm.buildingId)
            if (isNaN(buildingId) || buildingId <= 0) {
              this.$message.error('楼栋ID无效，请重新选择')
              this.loading = false
              return
            }
            
            // 验证离开时间不能早于到访时间
            // 从日期时间中提取日期和时间
            const visitDateTime = new Date(this.loginForm.visitTime)
            const visitDate = visitDateTime.toISOString().split('T')[0] // YYYY-MM-DD
            const visitTimeStr = `${String(visitDateTime.getHours()).padStart(2, '0')}:${String(visitDateTime.getMinutes()).padStart(2, '0')}` // HH:mm
            
            // 构建提交数据
            const submitData = {
              visitorName: this.loginForm.visitorName,
              visitorPhone: this.loginForm.visitorPhone,
              visitorIdCard: this.loginForm.idCard,
              relationship: null,
              studentName: this.loginForm.visitedStudent,
              studentRoom: this.loginForm.visitDorm,
              buildingId: buildingId,
              visitDate: visitDate,
              visitTime: visitTimeStr,
              visitReason: this.loginForm.visitReason || '探访同学',
              remarks: null
            }
            
            // 提交访客登记（使用公开接口）
            const res = await registerVisitor(submitData)
            
            this.loading = false
            
            if (res.code === 200) {
              // 跳转到访客信息页面，传递访客信息
              this.$router.push({
                path: '/visitor-info',
                query: {
                  visitorName: this.loginForm.visitorName,
                  visitorPhone: submitData.visitorPhone,
                  visitorIdCard: this.loginForm.idCard,
                  studentName: this.loginForm.visitedStudent,
                  buildingName: this.buildings.find(b => b.id === this.loginForm.buildingId)?.buildingName || '',
                  studentRoom: this.loginForm.visitDorm,
                  visitDate: visitDate,
                  visitTime: this.loginForm.visitTime,
                  visitReason: this.loginForm.visitReason || '探访同学',
                  status: 'PENDING', // 新提交的访客登记默认为待审核状态
                  targetAdmin: this.buildings.find(b => b.id === this.loginForm.buildingId)?.managerName || ''
                }
              })
              this.$message.success('访客登记提交成功')
            }
          } catch (error) {
            this.loading = false
            console.error('访客登记失败:', error)
            // 错误消息由响应拦截器统一处理
          }
        } else {
          // 🎯 学生和管理员登录 - 调用后端 API
          let response
          
          if (this.userType === 'admin') {
            // 管理员登录
            response = await adminLogin(this.loginForm.username, this.loginForm.password)
          } else {
            // 学生登录
            response = await studentLogin(this.loginForm.studentId, this.loginForm.password)
          }
          
          this.loading = false
          
          // 后端返回格式：{ code: 200, message: "登录成功", data: LoginVO }
          // LoginVO 包含 token 和所有用户信息
          if (response.code === 200 && response.data) {
            const loginData = response.data  // 整个 data 就是 LoginVO
            
            // 🔒 使用会话隔离的方法保存用户信息（自动处理字段统一）
            // 必须先保存用户信息，因为 setToken 需要用户类型
            setCurrentUser(loginData)
            setToken(loginData.token, loginData.userType)
            
            // 根据用户类型跳转到不同页面
            if (loginData.userType === 'student') {
              this.$router.push('/student')
              this.$message.success('学生登录成功')
            } else if (loginData.userType === 'superadmin') {
              this.$router.push('/super-admin')
              this.$message.success('超级管理员登录成功')
            } else if (loginData.userType === 'admin') {
              this.$router.push('/admin')
              this.$message.success('管理员登录成功')
            }
          } else {
            // response.code !== 200，错误已在拦截器中显示，这里不需要再显示
            // 拦截器会自动调用 showErrorMessage 显示错误
          }
        }
      } catch (error) {
        this.loading = false
        console.error('登录失败:', error)
        
        // 所有错误都已在拦截器中统一处理和显示，这里不需要再显示
        // 拦截器会根据不同错误类型显示相应提示：
        // - 业务错误 (code !== 200): 显示后端返回的message
        // - HTTP错误 (400/401/403等): 显示对应的错误提示
        // - 网络错误: 显示"网络连接失败"
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow-x: hidden;
  width: 100%;
}

.login-section {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px;
  min-height: 100vh;
  box-sizing: border-box;
  overflow-y: auto;
  /* 隐藏滚动条但保留滚动功能 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.login-section::-webkit-scrollbar {
  display: none; /* Chrome/Safari/Opera */
}

.login-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  padding: 48px;
  width: 100%;
  max-width: 420px;
  margin: auto 0;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 用户类型选择器样式 */
.user-type-selector {
  margin-bottom: 32px;
  text-align: center;
}

.user-type-group {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  background: #f8f9fa;
  padding: 8px;
  border-radius: 12px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.06);
}

:deep(.el-radio-button) {
  flex: 1;
  min-width: 0;
}

:deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px !important;
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  padding: 14px 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border: 2px solid transparent !important;
  background: white;
  color: #606266;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

:deep(.el-radio-button__inner::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

:deep(.el-radio-button__inner:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.25);
  border-color: #667eea !important;
  color: #667eea;
}

:deep(.el-radio-button__inner:hover::before) {
  opacity: 0.05;
}

:deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white !important;
  border-color: #667eea !important;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transform: translateY(-1px);
}

:deep(.el-radio-button.is-active .el-radio-button__inner::before) {
  opacity: 0;
}

:deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 10px !important;
}

:deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 10px !important;
}

/* 添加选中状态的动画效果 */
:deep(.el-radio-button.is-active .el-radio-button__inner) {
  animation: buttonPulse 0.3s ease-out;
}

@keyframes buttonPulse {
  0% {
    transform: translateY(-1px) scale(1);
  }
  50% {
    transform: translateY(-1px) scale(1.02);
  }
  100% {
    transform: translateY(-1px) scale(1);
  }
}

.login-form {
  margin-bottom: 24px;
}

/* 访客模式切换样式 */
.visitor-mode-switch {
  margin-bottom: 20px;
  text-align: center;
}

.visitor-mode-switch :deep(.el-radio-group) {
  width: 100%;
}

.visitor-mode-switch :deep(.el-radio-button) {
  width: 50%;
}

.visitor-mode-switch :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 8px !important;
  padding: 10px 16px;
  font-size: 14px;
}

.login-button {
  width: 100%;
  height: 52px;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.login-button:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.login-footer {
  text-align: center;
  font-size: 14px;
}

.divider {
  margin: 0 16px;
  color: #dcdfe6;
}

.no-register {
  color: #909399;
  font-style: italic;
}

.visitor-info {
  color: #67c23a;
  font-style: italic;
}

.intro-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  min-height: 100vh;
  overflow-y: auto;
  box-sizing: border-box;
}

.intro-content {
  max-width: 600px;
  color: white;
}

.intro-header {
  text-align: center;
  margin-bottom: 60px;
}

.intro-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 16px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.intro-subtitle {
  font-size: 18px;
  margin: 0;
  opacity: 0.9;
  line-height: 1.6;
}

.feature-list {
  margin-bottom: 60px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 32px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(5px);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateX(8px);
}

.feature-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
  flex-shrink: 0;
}

.feature-icon .el-icon {
  font-size: 24px;
  color: white;
}

.feature-content h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: white;
}

.feature-content p {
  font-size: 16px;
  margin: 0;
  opacity: 0.9;
  line-height: 1.5;
}

.contact-section {
  margin-top: 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.contact-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #fff;
  text-align: center;
}

.contact-info {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 15px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.contact-item .el-icon {
  font-size: 16px;
  color: #ffd700;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 2px solid #e4e7ed;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

:deep(.el-button--primary) {
  border-radius: 12px;
}

/* 响应式设计 */
/* 桌面端：>= 1024px - 左右分栏布局 */
@media (min-width: 1024px) {
  .login-container {
    flex-direction: row;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
  }
  
  .login-section {
    flex: 1;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: 40px;
    height: 100vh;
    width: 50%;
    overflow-y: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }
  
  .login-section::-webkit-scrollbar {
    display: none;
  }
  
  .login-card {
    max-width: 420px;
    padding: 48px 40px;
    width: 100%;
    margin: auto 0;
  }
  
  .login-title {
    font-size: 28px;
  }
  
  .login-subtitle {
    font-size: 14px;
  }
  
  .intro-section {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px;
    height: 100vh;
    width: 50%;
    overflow: hidden;
  }
  
  .intro-content {
    max-width: 550px;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    overflow: visible;
    padding: 20px 0;
  }
  
  .intro-header {
    margin-bottom: 30px;
    margin-top: 20px;
  }
  
  .intro-title {
    font-size: 32px;
    margin-bottom: 12px;
  }
  
  .intro-subtitle {
    font-size: 16px;
  }
  
  .feature-list {
    margin-bottom: 30px;
  }
  
  .feature-item {
    margin-bottom: 20px;
    padding: 14px;
  }
  
  .feature-content h3 {
    font-size: 18px;
    margin-bottom: 6px;
  }
  
  .feature-content p {
    font-size: 14px;
    line-height: 1.4;
  }
  
  .contact-section {
    margin-top: 15px;
    padding: 15px;
  }
  
  .contact-title {
    font-size: 14px;
    margin-bottom: 10px;
  }
  
  .contact-item {
    font-size: 12px;
  }
}

/* 平板端：768px - 1023px - 优化平板显示 */
@media (min-width: 768px) and (max-width: 1023px) {
  .login-container {
    flex-direction: row;
    min-height: 100vh;
  }
  
  .login-section {
    flex: 0.8;
    padding: 30px 20px;
    height: 100vh;
    overflow-y: auto;
    align-items: flex-start;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }
  
  .login-section::-webkit-scrollbar {
    display: none;
  }
  
  .login-card {
    padding: 36px 32px;
    max-width: 380px;
    margin: auto 0;
  }
  
  .login-title {
    font-size: 24px;
  }
  
  .login-subtitle {
    font-size: 13px;
  }
  
  .intro-section {
    flex: 1.2;
    padding: 20px 15px;
    height: 100vh;
    overflow-y: auto;
    display: flex;
    align-items: flex-start;
    justify-content: center;
  }
  
  .intro-content {
    max-width: 500px;
    padding: 10px 0;
    width: 100%;
  }
  
  .intro-header {
    margin-bottom: 25px;
    text-align: center;
  }
  
  .intro-title {
    font-size: 24px;
    margin-bottom: 10px;
  }
  
  .intro-subtitle {
    font-size: 14px;
    line-height: 1.4;
  }
  
  .feature-list {
    margin-bottom: 25px;
  }
  
  .feature-item {
    margin-bottom: 16px;
    padding: 12px;
  }
  
  .feature-icon {
    width: 40px;
    height: 40px;
  }
  
  .feature-icon .el-icon {
    font-size: 20px;
  }
  
  .feature-content h3 {
    font-size: 16px;
    margin-bottom: 5px;
  }
  
  .feature-content p {
    font-size: 12px;
    line-height: 1.3;
  }
  
  .contact-section {
    padding: 15px;
  }
  
  .contact-info {
    gap: 10px;
  }
  
  /* 平板端按钮优化 */
  .user-type-group {
    gap: 6px;
    padding: 6px;
  }
  
  :deep(.el-radio-button__inner) {
    padding: 12px 10px;
    font-size: 14px;
  }
  
  .contact-item {
    font-size: 12px;
  }
}

/* 移动端：< 768px - 垂直堆叠布局，登录功能在上，功能列表在下 */
@media (max-width: 767px) {
  .login-container {
    flex-direction: column;
    min-height: 100vh;
  }
  
  .login-section {
    flex: none;
    padding: 20px;
    order: 1;
  }
  
  .login-card {
    padding: 32px 24px;
    max-width: 100%;
  }
  
  .login-title {
    font-size: 22px;
  }
  
  .login-subtitle {
    font-size: 13px;
  }
  
  /* 移动端按钮优化 */
  .user-type-group {
    gap: 4px;
    padding: 6px;
  }
  
  :deep(.el-radio-button__inner) {
    padding: 12px 8px;
    font-size: 14px;
    font-weight: 600;
  }
  
  .intro-section {
    flex: none;
    padding: 30px 20px;
    order: 2;
    background: rgba(255, 255, 255, 0.15);
  }
  
  .intro-content {
    max-width: 100%;
  }
  
  .intro-header {
    margin-bottom: 40px;
  }
  
  .intro-title {
    font-size: 24px;
  }
  
  .intro-subtitle {
    font-size: 14px;
  }
  
  .feature-list {
    margin-bottom: 40px;
  }
  
  .feature-item {
    margin-bottom: 20px;
    padding: 16px;
  }
  
  .feature-icon {
    width: 40px;
    height: 40px;
    margin-right: 16px;
  }
  
  .feature-icon .el-icon {
    font-size: 20px;
  }
  
  .feature-content h3 {
    font-size: 16px;
  }
  
  .feature-content p {
    font-size: 13px;
  }
  
  .contact-section {
    padding: 15px;
  }
  
  .contact-info {
    flex-direction: column;
    gap: 10px;
  }
  
  .contact-item {
    justify-content: center;
    font-size: 13px;
  }
}
</style>
