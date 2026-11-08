/**
 * axios 请求封装
 * 统一处理请求拦截、响应拦截、错误处理
 * 🔒 会话隔离：根据当前路由获取对应角色的 token
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken } from '@/utils/auth'

// 创建 axios 实例
const request = axios.create({
  baseURL: 'http://localhost:8080/api', // 后端地址 (加上 /api 上下文路径)
  timeout: 15000, // 请求超时时间 15秒
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// ========== 错误消息去重机制 ==========
const errorMessageCache = new Map()
const ERROR_MESSAGE_DURATION = 3000 // 3秒内相同错误只显示一次

/**
 * 显示错误消息（带去重）
 * @param {string} message - 错误消息
 */
function showErrorMessage(message) {
  const now = Date.now()
  const lastTime = errorMessageCache.get(message)
  
  // 如果3秒内已经显示过相同的错误，则不再显示
  if (lastTime && now - lastTime < ERROR_MESSAGE_DURATION) {
    return
  }
  
  // 记录消息显示时间
  errorMessageCache.set(message, now)
  
  // 显示错误消息
  ElMessage.error(message)
  
  // 清理过期的缓存（避免内存泄漏）
  setTimeout(() => {
    errorMessageCache.delete(message)
  }, ERROR_MESSAGE_DURATION)
}

// ========== 请求拦截器 ==========
request.interceptors.request.use(
  config => {
    // 🔒 会话隔离：从当前路由对应的存储空间获取 token
    const token = getToken()
    
    // 如果 token 存在，添加到请求头
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    return config
  },
  error => {
    console.error('❌ 请求错误:', error)
    return Promise.reject(error)
  }
)

// ========== 响应拦截器 ==========
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 后端返回的统一格式：{ code, message, data, timestamp }
    // code === 200 表示成功
    if (res.code === 200) {
      return res
    } else {
      // 业务错误 - 在这里显示错误提示（统一处理）
      // 注意：这里 reject 后不会进入 error 分支，因为这是成功的 HTTP 响应
      showErrorMessage(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
  },
  error => {
    // 处理 HTTP 错误状态码（4xx, 5xx）
    if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      const data = error.response.data
      
      // 如果后端返回的是统一的 Result 格式，优先使用其中的 message
      if (data && typeof data === 'object' && data !== null && 'message' in data && 'code' in data) {
        // 这是后端的统一 Result 格式，显示其中的 message
        showErrorMessage(data.message || '操作失败')
      } else if (data && typeof data === 'object' && data !== null && 'message' in data) {
        // 有 message 字段但不是标准 Result 格式
        showErrorMessage(data.message || '操作失败')
      } else {
        // 非统一格式的错误响应
        switch (status) {
          case 400:
            showErrorMessage(data?.message || '请求参数错误')
            break
          case 401:
            showErrorMessage('未登录或登录已过期，请重新登录')
            // 🔒 会话隔离：清除当前角色的登录信息
            import('@/utils/auth').then(({ clearCurrentUser }) => {
              clearCurrentUser()
            })
            router.push('/login')
            break
          case 403:
            showErrorMessage('没有权限访问')
            break
          case 404:
            showErrorMessage('请求的资源不存在')
            break
          case 500:
            showErrorMessage(data?.message || '服务器内部错误')
            break
          default:
            showErrorMessage(data?.message || `服务器错误 (${status})`)
        }
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应（网络错误）
      showErrorMessage('网络连接失败，请检查网络')
    } else {
      // 请求配置出错（这种情况很少见，通常不会发生）
      // 不显示错误，因为这种情况通常是代码问题，用户无法解决
      console.error('请求配置错误:', error.message)
    }
    
    return Promise.reject(error)
  }
)

export default request

