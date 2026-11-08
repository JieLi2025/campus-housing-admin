<template>
  <div class="register-container">
    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="decoration-circle circle-1"></div>
      <div class="decoration-circle circle-2"></div>
      <div class="decoration-circle circle-3"></div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧介绍区域 -->
      <div class="intro-section">
        <div class="intro-content">
          <div class="intro-header">
            <div class="logo-section">
              <div class="logo-icon">
                <el-icon><School /></el-icon>
              </div>
              <h1 class="intro-title">学生住宿管理平台</h1>
            </div>
            <p class="intro-subtitle">智能化的学生住宿管理系统，让校园生活更美好</p>
          </div>
          
          <div class="feature-list">
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><House /></el-icon>
              </div>
              <div class="feature-content">
                <h3>宿舍分配管理</h3>
                <p>便捷的分配流程，支持室友匹配和自主选房</p>
              </div>
            </div>
            
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="feature-content">
                <h3>社交互动平台</h3>
                <p>宿舍群聊、室友招募、楼栋动态，促进交流</p>
              </div>
            </div>
            
            <div class="feature-item">
              <div class="feature-icon">
                <el-icon><Calendar /></el-icon>
              </div>
              <div class="feature-content">
                <h3>便捷服务管理</h3>
                <p>访客预约、物资管理、申请审核，一站式服务</p>
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

      <!-- 右侧注册表单区域 -->
      <div class="register-section">
        <div class="register-card">
          <div class="register-header">
            <h2 class="register-title">学生注册</h2>
            <p class="register-subtitle">请填写学号、邮箱、邀请码和密码完成注册</p>
          </div>
          
          <el-form 
            ref="registerForm" 
            :model="registerForm" 
            :rules="registerRules" 
            class="register-form"
            label-position="top"
            @submit.prevent="handleRegister"
          >
            <el-form-item label="真实姓名" prop="realName" class="form-item">
              <el-input
                v-model="registerForm.realName"
                placeholder="请输入真实姓名"
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="学号" prop="studentId" class="form-item">
              <el-input
                v-model="registerForm.studentId"
                placeholder="请输入学号"
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="邮箱" prop="email" class="form-item">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="手机号" prop="phone" class="form-item">
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><Phone /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="邀请码" prop="inviteCode" class="form-item">
              <el-input
                v-model="registerForm.inviteCode"
                placeholder="请输入邀请码"
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            
            <el-form-item label="密码" prop="password" class="form-item">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码（至少8位）"
                show-password
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item label="确认密码" prop="confirmPassword" class="form-item">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                clearable
                size="large"
                @keyup.enter.stop="checkAndSubmitRegister"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <div class="form-buttons">
              <el-form-item class="form-item">
                <el-button 
                  type="primary" 
                  size="large" 
                  class="register-button"
                  :loading="loading"
                  @click="handleRegister"
                >
                  <el-icon v-if="!loading"><UserFilled /></el-icon>
                  {{ loading ? '注册中...' : '立即注册' }}
                </el-button>
              </el-form-item>
              
              <el-form-item class="form-item">
                <el-button 
                  size="large" 
                  class="reset-button"
                  @click="resetForm"
                >
                  <el-icon><RefreshRight /></el-icon>
                  重置表单
                </el-button>
              </el-form-item>
            </div>
          </el-form>
          
          <div class="register-footer">
            <div class="help-info">
              <p class="info-text">注册说明：</p>
              <ul class="info-list">
                <li>真实姓名必须与系统记录匹配</li>
                <li>邀请码随录取通知书发放</li>
                <li>密码至少8位，建议包含大小写字母和数字</li>
                <li>每个学号只能注册一次</li>
              </ul>
            </div>
            <div class="admin-contact-info">
              <div class="contact-header">
                <el-icon class="contact-icon"><Message /></el-icon>
                <span class="contact-title">遇到问题？</span>
              </div>
              <p class="contact-description">
                如果您的学号不在系统中或遇到其他注册问题，请联系系统管理员：
              </p>
              <div class="contact-email">
                <el-icon class="email-icon"><Message /></el-icon>
                <a :href="'mailto:' + superAdminEmail" class="email-link">
                  {{ superAdminEmail || '加载中...' }}
                </a>
              </div>
            </div>
            <div class="footer-links">
              <span class="footer-text">已有账号？</span>
              <router-link to="/login" class="login-link">立即登录</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Register',
  data() {
    return {
      loading: false,
      submitTimeout: null,
      superAdminEmail: '', // 超级管理员邮箱
      // 管理员联系方式
      adminContact: {
        name: '系统管理员',
        email: 'admin@campus.edu.cn',
        phone: '请联系学校管理部门'
      },
      registerForm: {
        realName: '',
        studentId: '',
        email: '',
        phone: '',
        inviteCode: '',
        password: '',
        confirmPassword: ''
      },
      registerRules: {
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' },
          { min: 2, max: 10, message: '姓名长度应为2-10个字符', trigger: 'blur' },
          { pattern: /^[\u4e00-\u9fa5a-zA-Z\s]+$/, message: '姓名只能包含中文、英文字母和空格', trigger: 'blur' }
        ],
        studentId: [
          { required: true, message: '请输入学号', trigger: 'blur' },
          { pattern: /^\d{8,12}$/, message: '学号格式不正确，请输入8-12位数字', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
        ],
        inviteCode: [
          { required: true, message: '请输入邀请码', trigger: 'blur' },
          { pattern: /^[A-Z0-9]{10}$/, message: '邀请码格式为10位字母数字组合', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 8, message: '密码至少8位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { 
            validator: (rule, value, callback) => {
              if (value !== this.registerForm.password) {
                callback(new Error('两次输入的密码不一致'))
              } else {
                callback()
              }
            }, 
            trigger: 'blur' 
          }
        ]
      }
    }
  },
  mounted() {
    // 获取超级管理员邮箱
    this.loadSuperAdminEmail()
    // 加载管理员联系方式
    this.loadAdminContact()
  },
  methods: {
    // 加载管理员联系方式
    async loadAdminContact() {
      try {
        const { getAdminContact } = await import('@/api/auth.js')
        const res = await getAdminContact()
        if (res.code === 200 && res.data) {
          this.adminContact = res.data
        }
      } catch (error) {
        console.error('加载管理员联系方式失败:', error)
      }
    },
    
    // 加载超级管理员邮箱
    loadSuperAdminEmail() {
      try {
        // 尝试从系统配置中获取
        const systemConfig = localStorage.getItem('systemConfig')
        if (systemConfig) {
          const config = JSON.parse(systemConfig)
          if (config.adminEmail) {
            this.superAdminEmail = config.adminEmail
            return
          }
        }
      } catch (e) {
        console.error('获取系统配置失败:', e)
      }

      // 尝试从超级管理员账户信息中获取
      try {
        const registeredUsers = JSON.parse(localStorage.getItem('registeredUsers') || '[]')
        const superAdmin = registeredUsers.find(user => user.userType === 'superadmin')
        if (superAdmin && superAdmin.email) {
          this.superAdminEmail = superAdmin.email
          return
        }
      } catch (e) {
        console.error('获取超级管理员信息失败:', e)
      }

      // 使用默认邮箱
      this.superAdminEmail = 'superadmin@example.com'
    },

    async handleRegister() {
      try {
        // 表单验证
        const valid = await this.$refs.registerForm.validate().catch(err => {
          console.log('表单验证失败:', err)
          
          // 解析验证错误并显示具体信息
          if (err && typeof err === 'object') {
            const firstError = Object.values(err)[0]
            if (Array.isArray(firstError) && firstError.length > 0) {
              this.$message.error(firstError[0].message || '表单验证失败')
            } else {
              this.$message.error('请检查表单输入是否正确')
            }
          } else {
            this.$message.error('请检查表单输入是否正确')
          }
          return false
        })
        
        if (!valid) {
          return
        }
        
        this.loading = true
        
        // 🎯 调用后端注册 API
        const { studentRegister } = await import('@/api/auth.js')
        
        // 调用后端注册接口
        const response = await studentRegister({
          studentId: this.registerForm.studentId,
          realName: this.registerForm.realName,
          email: this.registerForm.email,
          phone: this.registerForm.phone,
          password: this.registerForm.password,
          confirmPassword: this.registerForm.confirmPassword,
          inviteCode: this.registerForm.inviteCode || '' // 邀请码可选
        })
        
        // 注册成功（request.js已经确保code===200才会到这里）
        this.$message.success(response.message || '注册成功！请使用学号和密码登录')
        
        // 延迟跳转到登录页面，在loading结束前保持状态
        setTimeout(() => {
          this.loading = false
          this.$router.push('/login')
        }, 1500)
        
      } catch (error) {
        // request.js 已经显示了错误消息，这里不再重复显示
        // 只记录错误日志用于调试
        console.error('注册失败:', error)
        this.loading = false
      }
    },
    // 清空表单
    resetForm() {
      this.$refs.registerForm.resetFields()
      this.registerForm = {
        realName: '',
        studentId: '',
        email: '',
        phone: '',
        inviteCode: '',
        password: '',
        confirmPassword: ''
      }
    },
    
    // 检查并提交注册（用于回车键触发）
    checkAndSubmitRegister() {
      // 防抖处理，避免快速重复触发
      if (this.submitTimeout) {
        clearTimeout(this.submitTimeout)
      }
      
      this.submitTimeout = setTimeout(() => {
        // 检查必填字段是否都已填写
        const requiredFields = ['studentId', 'email', 'phone', 'inviteCode', 'password', 'confirmPassword']
        const isComplete = requiredFields.every(field => this.registerForm[field] && this.registerForm[field].trim())
        
        if (isComplete) {
          this.handleRegister()
        } else {
          // 如果表单不完整，不执行任何操作（让用户继续填写）
          console.log('表单未完成，继续填写...')
        }
      }, 100) // 100ms防抖
    },
    
  }
}
</script>

<style scoped>
/* 自适应但固定最大尺寸的设计 */
.register-container {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  padding: 0;
  box-sizing: border-box;
  margin: 0;
}

/* 背景装饰 */
.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 8s ease-in-out infinite;
}

.circle-1 {
  width: 120px;
  height: 120px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.circle-2 {
  width: 80px;
  height: 80px;
  top: 60%;
  right: 15%;
  animation-delay: 3s;
}

.circle-3 {
  width: 60px;
  height: 60px;
  bottom: 20%;
  left: 20%;
  animation-delay: 6s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
  }
}

/* 主要内容区域 */
.main-content {
  display: flex;
  width: 100%;
  height: 100vh;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 0;
  box-shadow: none;
  overflow: hidden;
  position: relative;
  z-index: 2;
}

/* 左侧介绍区域 */
.intro-section {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  min-width: 0;
  min-width: 500px; /* 确保最小宽度 */
}

.intro-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/><circle cx="10" cy="60" r="0.5" fill="rgba(255,255,255,0.1)"/><circle cx="90" cy="40" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  opacity: 0.2;
}

.intro-content {
  max-width: 500px;
  width: 100%;
  color: white;
  position: relative;
  z-index: 2;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

.intro-header {
  margin-bottom: 48px;
  text-align: center;
}

.logo-section {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.logo-icon .el-icon {
  font-size: 28px;
  color: white;
}

.intro-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.intro-subtitle {
  font-size: 16px;
  margin: 12px 0 0 0;
  opacity: 0.9;
  line-height: 1.6;
}

.feature-list {
  margin-bottom: 48px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.feature-icon .el-icon {
  font-size: 22px;
  color: white;
}

.feature-content h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: white;
}

.feature-content p {
  font-size: 14px;
  margin: 0;
  opacity: 0.9;
  line-height: 1.5;
  word-wrap: break-word;
  overflow-wrap: break-word;
  white-space: normal;
}

.contact-section {
  margin-top: 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.contact-section .contact-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #fff;
  text-align: center;
}

.contact-section .contact-info {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 15px;
}

.contact-section .contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.contact-section .contact-item .el-icon {
  font-size: 16px;
  color: #ffd700;
}

/* 右侧注册表单区域 */
.register-section {
  flex: 1;
  padding: 48px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: white;
  min-width: 0;
  overflow-y: auto;
  max-height: 100vh;
  scrollbar-width: thin;
  scrollbar-color: #667eea #f0f0f0;
}

/* 自定义滚动条样式 */
.register-section::-webkit-scrollbar {
  width: 8px;
}

.register-section::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 4px;
}

.register-section::-webkit-scrollbar-thumb {
  background: #667eea;
  border-radius: 4px;
}

.register-section::-webkit-scrollbar-thumb:hover {
  background: #5a6fd8;
}

.register-card {
  width: 100%;
  max-width: 500px;
  padding-top: 20px;
  padding-bottom: 20px;
}

.register-header {
  text-align: center;
  margin-bottom: 36px;
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 10px 0;
}

.register-subtitle {
  font-size: 15px;
  color: #7f8c8d;
  margin: 0;
}

.register-form {
  margin-bottom: 28px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 0;
}

.form-item {
  margin-bottom: 20px;
}

.form-item:last-child {
  margin-bottom: 0;
}

/* 验证码输入组 - 优化布局 */
.code-input-group {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  width: 100%;
}

.code-input-group .el-input {
  flex: 1;
  min-width: 250px; /* 增加最小宽度确保文字完整显示 */
}

.send-code-btn {
  width: 120px;
  height: 40px;
  font-size: 14px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.send-code-btn:disabled {
  background: #e1e8ed;
  color: #7f8c8d;
}

.register-button {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.register-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.4);
}

.register-button:active {
  transform: translateY(0);
}


/* 表单按钮组 */
.form-buttons {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
  margin-bottom: 0;
}

.reset-button {
  height: 52px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 14px;
  background: #f8fafc;
  border: 2px solid #e1e8ed;
  color: #7f8c8d;
  transition: all 0.3s ease;
}

.reset-button:hover {
  background: white;
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
}

.register-footer {
  padding-top: 24px;
  border-top: 1px solid #e1e8ed;
}

/* 帮助信息 */
.help-info {
  margin-bottom: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e1e8ed;
}

.info-text {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 10px 0;
}

.info-list {
  margin: 0;
  padding-left: 16px;
  list-style: none;
}

.info-list li {
  font-size: 13px;
  color: #7f8c8d;
  margin-bottom: 6px;
  position: relative;
  line-height: 1.4;
}

.info-list li::before {
  content: '•';
  color: #667eea;
  position: absolute;
  left: -12px;
  font-weight: bold;
}

.info-list li:last-child {
  margin-bottom: 0;
}

/* 管理员联系信息 */
.admin-contact-info {
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  border-radius: 10px;
  border: 1px solid #91caff;
}

.contact-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.contact-icon {
  font-size: 18px;
  color: #1890ff;
}

.contact-title {
  font-size: 14px;
  font-weight: 600;
  color: #0050b3;
}

.contact-description {
  font-size: 13px;
  color: #0958d9;
  margin: 0 0 10px 0;
  line-height: 1.4;
}

.contact-email {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #bae0ff;
}

.email-icon {
  font-size: 16px;
  color: #1890ff;
  flex-shrink: 0;
}

.email-link {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
  text-decoration: none;
  transition: all 0.3s;
  word-break: break-all;
}

.email-link:hover {
  color: #0050b3;
  text-decoration: underline;
}

/* 底部链接 */
.footer-links {
  text-align: center;
}

.footer-text {
  font-size: 15px;
  color: #7f8c8d;
  margin-right: 8px;
}

.login-link {
  font-size: 15px;
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
}

.login-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-input__wrapper) {
  border-radius: 10px;
  border: 2px solid #e1e8ed;
  background: #f8fafc;
  transition: all 0.3s ease;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: #667eea;
  background: white;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

:deep(.el-input__inner) {
  font-size: 15px;
  color: #2c3e50;
}

:deep(.el-input__prefix) {
  color: #667eea;
  font-size: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c3e50;
  font-size: 13px;
  margin-bottom: 6px;
}

:deep(.el-radio) {
  margin-right: 24px;
  font-weight: 500;
}

:deep(.el-radio__label) {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #2c3e50;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: #667eea;
  background: #667eea;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #667eea;
}

:deep(.el-form-item__error) {
  font-size: 12px;
  color: #e74c3c;
  margin-top: 4px;
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 10;
  background: white;
  padding: 2px 6px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .main-content {
    max-width: 100%;
    margin: 0;
  }
  
  .form-buttons {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .code-input-group {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .code-input-group .el-input {
    min-width: 100%;
  }
  
  .send-code-btn {
    width: 100%;
  }
  
  .test-data-section {
    margin-top: 12px;
  }
  
  .help-info {
    padding: 12px;
  }
  
  .info-list {
    padding-left: 12px;
  }
}

@media (max-width: 768px) {
  .register-container {
    padding: 0;
  }
  
  .main-content {
    flex-direction: column;
    max-height: none;
  }
  
  .intro-section,
  .register-section {
    padding: 24px;
  }
  
  .register-section {
    max-height: none;
    overflow-y: visible;
  }
  
  .intro-content {
    max-width: none;
  }
  
  .stats-section {
    flex-direction: column;
    gap: 12px;
  }
  
  .form-buttons {
    grid-template-columns: 1fr;
  }
  
  .help-info {
    padding: 12px;
    font-size: 12px;
  }
  
  .info-text {
    font-size: 13px;
  }
  
  .info-list li {
    font-size: 12px;
  }
}
</style>

