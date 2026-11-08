<template>
  <div class="email-verify-container">
    <div class="email-verify-card">
      <div class="email-verify-header">
        <h1 class="email-verify-title">邮箱验证</h1>
        <p class="email-verify-subtitle">请输入发送到您邮箱的验证码</p>
      </div>
      
      <el-form 
        ref="emailVerifyForm" 
        :model="emailVerifyForm" 
        :rules="emailVerifyRules" 
        class="email-verify-form"
      >
        <el-form-item prop="verifyCode">
          <el-input
            v-model="emailVerifyForm.verifyCode"
            placeholder="请输入6位验证码"
            prefix-icon="Key"
            size="large"
            maxlength="6"
            clearable
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            class="email-verify-button"
            :loading="loading"
            @click="handleVerify"
          >
            验证
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="email-verify-footer">
        <span>没有收到验证码？</span>
        <el-link type="primary" @click="resendCode" :disabled="countdown > 0">
          {{ countdown > 0 ? `${countdown}秒后重发` : '重新发送' }}
        </el-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EmailVerify',
  data() {
    return {
      loading: false,
      countdown: 0,
      emailVerifyForm: {
        verifyCode: ''
      },
      emailVerifyRules: {
        verifyCode: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    async handleVerify() {
      try {
        await this.$refs.emailVerifyForm.validate()
        this.loading = true
        
        // 模拟验证过程
        setTimeout(() => {
          this.loading = false
          this.$message.success('邮箱验证成功')
          this.$router.push('/student')
        }, 1500)
      } catch (error) {
        console.error('验证失败:', error)
      }
    },
    resendCode() {
      this.countdown = 60
      const timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          clearInterval(timer)
        }
      }, 1000)
      
      this.$message.success('验证码已重新发送')
    }
  }
}
</script>

<style scoped>
.email-verify-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
}

.email-verify-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 400px;
}

.email-verify-header {
  text-align: center;
  margin-bottom: 32px;
}

.email-verify-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.email-verify-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.email-verify-form {
  margin-bottom: 24px;
}

.email-verify-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.email-verify-footer {
  text-align: center;
  font-size: 14px;
  color: #606266;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-button--primary) {
  border-radius: 8px;
}
</style>
