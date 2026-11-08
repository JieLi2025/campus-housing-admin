// 邮件服务工具
class EmailService {
  constructor() {
    // 邮件服务配置 - 仅开发环境模拟发送
    this.config = {
      // 始终使用模拟发送（开发环境）
      isDevelopment: true
    }
    
    // 存储验证码（实际项目中应该使用 Redis 或数据库）
    this.verificationCodes = new Map()
    
    // 邮件模板
    this.emailTemplates = {
      verificationCode: (code) => `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;">
            <h1 style="color: white; margin: 0;">宿舍管理系统</h1>
            <p style="color: white; margin: 10px 0 0 0;">邮箱验证码</p>
          </div>
          <div style="padding: 30px; background: #f8f9fa;">
            <h2 style="color: #333; margin-bottom: 20px;">您的验证码</h2>
            <div style="background: white; padding: 20px; border-radius: 10px; text-align: center; margin: 20px 0;">
              <span style="font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px;">${code}</span>
            </div>
            <p style="color: #666; margin: 20px 0;">验证码有效期为5分钟，请尽快使用。</p>
            <p style="color: #999; font-size: 12px; margin-top: 30px;">
              如果您没有请求此验证码，请忽略此邮件。
            </p>
          </div>
        </div>
      `
    }
  }
  
  // 生成6位数字验证码
  generateVerificationCode() {
    return Math.floor(100000 + Math.random() * 900000).toString()
  }
  
  // 发送验证码邮件
  async sendVerificationCode(email, code) {
    try {
      // 模拟发送
      await this.simulateEmailSending(email, code)
      
      // 存储验证码
      this.verificationCodes.set(email, {
        code,
        timestamp: Date.now(),
        attempts: 0
      })
      
      return { success: true, message: '验证码发送成功' }
    } catch (error) {
      return { success: false, message: '验证码发送失败：' + error.message }
    }
  }
  
  // 模拟邮件发送
  async simulateEmailSending(email, code) {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  
  // 真实邮件发送功能已移除
  // 如需真实邮件发送，请参考 EMAIL_SETUP.md 文档进行配置
  
  // 验证验证码
  verifyCode(email, inputCode) {
    const storedData = this.verificationCodes.get(email)
    
    if (!storedData) {
      return { success: false, message: '验证码已过期或不存在' }
    }
    
    // 检查验证码是否过期
    if (this.isCodeExpired(email)) {
      this.verificationCodes.delete(email)
      return { success: false, message: '验证码已过期，请重新获取' }
    }
    
    // 检查验证码是否正确
    if (storedData.code === inputCode) {
      // 验证成功后删除验证码
      this.verificationCodes.delete(email)
      return { success: true, message: '验证码验证成功' }
    } else {
      // 增加尝试次数
      storedData.attempts++
      this.verificationCodes.set(email, storedData)
      
      if (storedData.attempts >= 3) {
        // 超过3次尝试，删除验证码
        this.verificationCodes.delete(email)
        return { success: false, message: '验证码错误次数过多，请重新获取' }
      }
      
      return { success: false, message: `验证码错误，还剩${3 - storedData.attempts}次尝试机会` }
    }
  }
  
  // 检查验证码是否过期（5分钟过期）
  isCodeExpired(email) {
    const storedData = this.verificationCodes.get(email)
    if (!storedData) return true
    
    const now = Date.now()
    const fiveMinutes = 5 * 60 * 1000 // 5分钟
    
    return (now - storedData.timestamp) > fiveMinutes
  }
  
  // 清理过期的验证码
  cleanupExpiredCodes() {
    const now = Date.now()
    const fiveMinutes = 5 * 60 * 1000
    
    for (const [email, data] of this.verificationCodes.entries()) {
      if ((now - data.timestamp) > fiveMinutes) {
        this.verificationCodes.delete(email)
      }
    }
  }
}

// 创建单例实例
const emailService = new EmailService()

// 定期清理过期验证码
setInterval(() => {
  emailService.cleanupExpiredCodes()
}, 60000) // 每分钟清理一次

export default emailService
