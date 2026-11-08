/**
 * 认证相关 API
 * 包括登录、注册、登出、密码找回等
 */
import request from '@/utils/request'

/**
 * 学生登录
 * @param {string} studentId - 学号
 * @param {string} password - 密码
 * @returns {Promise} 返回包含 token 和用户信息的响应
 */
export function studentLogin(studentId, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username: studentId,  // 后端使用 username 字段
      password: password,
      userType: 'student'
    }
  })
}

/**
 * 管理员登录（支持超级管理员和普通管理员）
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise} 返回包含 token 和用户信息的响应
 */
export async function adminLogin(username, password) {
  // 先尝试超级管理员登录
  try {
    const response = await request({
      url: '/auth/login',
      method: 'post',
      data: {
        username: username,
        password: password,
        userType: 'superadmin'
      }
    })
    // 如果成功，直接返回
    if (response.code === 200) {
      return response
    }
  } catch (error) {
    // 超级管理员登录失败，静默处理，继续尝试普通管理员
    if (import.meta.env.DEV) {
      console.log('超级管理员登录失败，尝试普通管理员登录')
    }
  }
  
  // 尝试普通管理员登录
  try {
    const response = await request({
      url: '/auth/login',
      method: 'post',
      data: {
        username: username,
        password: password,
        userType: 'admin'
      }
    })
    return response
  } catch (error) {
    // 两次登录都失败，抛出错误
    throw error
  }
}

/**
 * 学生注册
 * @param {Object} registerData - 注册数据
 * @param {string} registerData.studentId - 学号
 * @param {string} registerData.realName - 真实姓名
 * @param {string} registerData.email - 邮箱
 * @param {string} registerData.phone - 手机号
 * @param {string} registerData.password - 密码
 * @param {string} registerData.inviteCode - 邀请码（可选）
 * @returns {Promise}
 */
export function studentRegister(registerData) {
  return request({
    url: '/auth/register',
    method: 'post',
    data: {
      studentId: registerData.studentId,
      realName: registerData.realName,
      email: registerData.email,
      phone: registerData.phone,
      password: registerData.password,
      confirmPassword: registerData.confirmPassword,
      inviteCode: registerData.inviteCode
    }
  })
}

/**
 * 发送验证码到邮箱
 * @param {string} email - 邮箱地址
 * @param {string} type - 验证码类型（register/forgot）
 * @returns {Promise}
 */
export function sendVerificationCode(email, type = 'register') {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data: {
      email: email,
      type: type
    }
  })
}

/**
 * 验证邮箱验证码
 * @param {string} email - 邮箱地址
 * @param {string} code - 验证码
 * @returns {Promise}
 */
export function verifyCode(email, code) {
  return request({
    url: '/auth/verify-code',
    method: 'post',
    data: {
      email: email,
      code: code
    }
  })
}

/**
 * 重置密码
 * @param {string} email - 邮箱地址
 * @param {string} newPassword - 新密码
 * @param {string} code - 验证码
 * @returns {Promise}
 */
export function resetPassword(email, newPassword, code) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data: {
      email: email,
      newPassword: newPassword,
      code: code
    }
  })
}

/**
 * 退出登录
 * @returns {Promise}
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取当前用户信息
 * @returns {Promise}
 */
export function getCurrentUserInfo() {
  return request({
    url: '/auth/user-info',
    method: 'get'
  })
}

/**
 * 刷新 token
 * @returns {Promise}
 */
export function refreshToken() {
  return request({
    url: '/auth/refresh-token',
    method: 'post'
  })
}


/**
 * 验证邀请码用于重置密码
 * @param {string} studentId - 学号
 * @param {string} inviteCode - 邀请码
 * @returns {Promise}
 */
export function verifyInviteCodeForReset(studentId, inviteCode) {
  return request({
    url: '/auth/verify-invite-code',
    method: 'post',
    data: {
      studentId: studentId,
      inviteCode: inviteCode
    }
  })
}

/**
 * 通过邀请码重置密码
 * @param {string} studentId - 学号
 * @param {string} inviteCode - 邀请码
 * @param {string} newPassword - 新密码
 * @returns {Promise}
 */
export function resetPasswordByInviteCode(studentId, inviteCode, newPassword) {
  return request({
    url: '/auth/reset-password-by-invite-code',
    method: 'post',
    data: {
      studentId: studentId,
      inviteCode: inviteCode,
      newPassword: newPassword
    }
  })
}

/**
 * 获取系统管理员联系方式（公开接口）
 * @returns {Promise} 返回管理员联系方式 { name, email, phone }
 */
export function getAdminContact() {
  return request({
    url: '/auth/admin-contact',
    method: 'get'
  })
}
