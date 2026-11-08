<template>
  <div class="forgot-password-container">
    <div class="forgot-password-card">
      <!-- 步骤指示器 -->
      <div class="step-indicator">
        <div class="step-item" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
          <div class="step-number">1</div>
          <span class="step-text">验证身份</span>
        </div>
        <div class="step-line" :class="{ active: currentStep > 1 }"></div>
        <div class="step-item" :class="{ active: currentStep >= 2 }">
          <div class="step-number">2</div>
          <span class="step-text">重置密码</span>
        </div>
      </div>

      <!-- 第一步：输入学号和邀请码 -->
      <div v-if="currentStep === 1" class="step-content">
        <div class="step-header">
          <h1 class="step-title">验证身份</h1>
          <p class="step-subtitle">请输入您的学号和邀请码进行身份验证</p>
        </div>
        
        <el-form 
          ref="verifyForm" 
          :model="verifyForm" 
          :rules="verifyRules" 
          class="step-form"
          @submit.prevent="handleVerify"
        >
          <el-form-item prop="studentId">
            <el-input
              v-model="verifyForm.studentId"
              placeholder="请输入学号"
              prefix-icon="User"
              size="large"
              clearable
              maxlength="12"
              @keyup.enter.stop="handleVerify"
            />
          </el-form-item>
          
          <el-form-item prop="inviteCode">
            <el-input
              v-model="verifyForm.inviteCode"
              placeholder="请输入邀请码"
              prefix-icon="Key"
              size="large"
              clearable
              @keyup.enter.stop="handleVerify"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="step-button"
              :loading="verifying"
              @click="handleVerify"
            >
              {{ verifying ? '验证中...' : '验证身份' }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="help-text">
          <el-icon><InfoFilled /></el-icon>
          <span>邀请码由管理员提供，如忘记请联系管理员获取</span>
        </div>
      </div>

      <!-- 第二步：输入新密码 -->
      <div v-if="currentStep === 2" class="step-content">
        <div class="step-header">
          <h1 class="step-title">重置密码</h1>
          <p class="step-subtitle">身份验证成功，请设置新密码</p>
        </div>
        
        <el-form 
          ref="resetForm" 
          :model="resetForm" 
          :rules="resetRules" 
          class="step-form"
          @submit.prevent="handleResetPassword"
        >
          <el-form-item prop="password">
            <el-input
              v-model="resetForm.password"
              type="password"
              placeholder="请输入新密码（至少8位）"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter.stop="checkAndSubmitReset"
            />
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              placeholder="请确认新密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter.stop="checkAndSubmitReset"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="step-button"
              :loading="resetting"
              @click="handleResetPassword"
            >
              {{ resetting ? '重置中...' : '重置密码' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 底部链接 -->
      <div class="forgot-password-footer">
        <el-link type="primary" @click="$router.push('/login')">
          返回登录
        </el-link>
      </div>
    </div>
  </div>
</template>

<script>
import { resetPasswordByInviteCode, verifyInviteCodeForReset } from '@/api/auth.js'

export default {
  name: 'ForgotPassword',
  data() {
    // 密码确认验证规则
    const validateConfirmPassword = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== this.resetForm.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }

    return {
      currentStep: 1,
      verifying: false,
      resetting: false,
      submitTimeout: null,
      verifyForm: {
        studentId: '',
        inviteCode: ''
      },
      resetForm: {
        password: '',
        confirmPassword: ''
      },
      verifyRules: {
        studentId: [
          { required: true, message: '请输入学号', trigger: 'blur' },
          { pattern: /^\d{8,12}$/, message: '学号格式不正确，请输入8-12位数字', trigger: 'blur' }
        ],
        inviteCode: [
          { required: true, message: '请输入邀请码', trigger: 'blur' }
        ]
      },
      resetRules: {
        password: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    async handleVerify() {
      try {
        await this.$refs.verifyForm.validate()
        
        this.verifying = true
        
        // 调用后端API验证学号和邀请码
        const response = await verifyInviteCodeForReset(
          this.verifyForm.studentId,
          this.verifyForm.inviteCode
        )
        
        if (response.code === 200) {
          this.$message.success('身份验证成功')
          this.currentStep = 2
        } else {
          this.$message.error(response.message || '验证失败，请检查学号和邀请码')
        }
      } catch (error) {
        console.error('验证失败:', error)
        // 错误消息由响应拦截器统一处理
      } finally {
        this.verifying = false
      }
    },
    
    // 检查并提交重置密码（用于回车键触发）
    checkAndSubmitReset() {
      // 防抖处理，避免快速重复触发
      if (this.submitTimeout) {
        clearTimeout(this.submitTimeout)
      }
      
      this.submitTimeout = setTimeout(() => {
        // 检查必填字段是否都已填写
        const requiredFields = ['password', 'confirmPassword']
        const isComplete = requiredFields.every(field => this.resetForm[field] && this.resetForm[field].trim())
        
        if (isComplete) {
          this.handleResetPassword()
        }
      }, 100)
    },
    
    async handleResetPassword() {
      try {
        await this.$refs.resetForm.validate()
        
        this.resetting = true
        
        // 调用后端API重置密码
        const response = await resetPasswordByInviteCode(
          this.verifyForm.studentId,
          this.verifyForm.inviteCode,
          this.resetForm.password
        )
        
        if (response.code === 200) {
          this.$message.success('密码重置成功，请使用新密码登录')
          
          // 延时跳转到登录页
          setTimeout(() => {
            this.$router.push('/login')
          }, 1500)
        } else {
          this.$message.error(response.message || '密码重置失败')
        }
      } catch (error) {
        console.error('重置密码失败:', error)
        // 错误消息由响应拦截器统一处理
      } finally {
        this.resetting = false
      }
    }
  }
}
</script>


<style scoped>
.forgot-password-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  width: 100vw !important;
  height: 100vh !important;
}

.forgot-password-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: float 20s ease-in-out infinite;
  z-index: 0;
}

.forgot-password-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.2);
  padding: 40px;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  transition: all 0.3s ease;
}

.forgot-password-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 30px 100px rgba(0, 0, 0, 0.25);
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 40px;
  gap: 16px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  transition: all 0.3s ease;
}

.step-item.active .step-number {
  background: #409eff;
  color: white;
}

.step-item.completed .step-number {
  background: #67c23a;
  color: white;
}

.step-text {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.step-item.active .step-text {
  color: #409eff;
}

.step-line {
  width: 60px;
  height: 2px;
  background: #f0f0f0;
  transition: all 0.3s ease;
}

.step-line.active {
  background: #409eff;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

.step-header {
  text-align: center;
  margin-bottom: 32px;
}

.step-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px 0;
}

.step-subtitle {
  font-size: 16px;
  color: #606266;
  margin: 0;
  line-height: 1.5;
}

.step-form {
  margin-bottom: 24px;
}

.step-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.step-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.help-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  margin-top: 16px;
}

.help-text .el-icon {
  color: #409eff;
}

.forgot-password-footer {
  text-align: center;
  font-size: 14px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  border: 2px solid #f0f0f0;
  transition: all 0.3s ease;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: #409eff;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  50% {
    transform: translate(10px, -5px) rotate(-1deg);
  }
}

@media (max-width: 768px) {
  .forgot-password-container {
    padding: 16px;
  }
  
  .forgot-password-card {
    padding: 24px;
    max-width: 100%;
  }
  
  .step-title {
    font-size: 24px;
  }
}
</style>
