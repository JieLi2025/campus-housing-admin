/**
 * 用户认证和状态管理工具
 * 注意：登录逻辑已迁移到后端 API（见 @/api/auth.js）
 * 本文件仅保留前端状态管理相关的工具函数
 * 
 * 🔒 会话隔离：不同角色使用独立的存储空间，避免互相干扰
 */

// ========== 会话隔离配置 ==========
// 不同角色使用不同的 localStorage 键，实现会话隔离
const STORAGE_KEYS = {
  student: {
    user: 'student_user',
    token: 'student_token'
  },
  admin: {
    user: 'admin_user',
    token: 'admin_token'
  },
  superadmin: {
    user: 'superadmin_user',
    token: 'superadmin_token'
  }
}

/**
 * 根据当前路由获取用户类型
 * @returns {string} 'student' | 'admin' | 'superadmin'
 */
function getCurrentRoleFromRoute() {
  const path = window.location.pathname
  if (path.startsWith('/student')) {
    return 'student'
  } else if (path.startsWith('/super-admin')) {
    return 'superadmin'
  } else if (path.startsWith('/admin')) {
    return 'admin'
  }
  
  // 默认返回 null，由调用方处理
  return null
}

/**
 * 获取当前角色的存储键
 * @param {string} role - 角色类型（可选，不传则从路由推断）
 * @returns {Object} { user: string, token: string }
 */
function getStorageKeys(role = null) {
  // 如果没有指定角色，从路由推断
  const currentRole = role || getCurrentRoleFromRoute()
  
  // 如果还是无法确定角色，尝试从所有存储中找到有效的用户
  if (!currentRole) {
    for (const [roleName, keys] of Object.entries(STORAGE_KEYS)) {
      const userStr = localStorage.getItem(keys.user)
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          if (user && user.userType) {
            return STORAGE_KEYS[user.userType] || keys
          }
        } catch (e) {
          // 忽略解析错误
        }
      }
    }
    
    // 如果都找不到，返回默认的学生键
    return STORAGE_KEYS.student
  }
  
  return STORAGE_KEYS[currentRole] || STORAGE_KEYS.student
}

// ========== 用户信息管理 ==========

/**
 * 获取当前登录用户信息
 * 🔧 统一字段：后端返回 userId，前端统一为 id
 * 🔒 会话隔离：根据当前路由获取对应角色的用户信息
 * @param {string} role - 角色类型（可选）
 * @returns {Object|null} 用户信息对象或 null
 */
export function getCurrentUser(role = null) {
  const keys = getStorageKeys(role)
  const userStr = localStorage.getItem(keys.user)
  
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      
      // 🔧 字段统一处理：确保 id 字段存在
      // 后端返回 userId，前端统一使用 id
      if (user && !user.id && user.userId) {
        user.id = user.userId
      }
      
      return user
    } catch (e) {
      console.error('解析用户信息失败:', e)
      return null
    }
  }
  return null
}

/**
 * 保存用户登录信息到本地存储
 * 🔧 统一字段：确保同时保存 userId 和 id
 * 🔒 会话隔离：根据用户类型保存到对应的存储空间
 * @param {Object} user - 用户信息对象
 */
export function setCurrentUser(user) {
  if (user) {
    // 🔧 字段统一处理：确保同时有 id 和 userId
    if (user.userId && !user.id) {
      user.id = user.userId
    } else if (user.id && !user.userId) {
      user.userId = user.id
    }
    
    // 🔒 根据用户类型选择存储键
    const keys = getStorageKeys(user.userType)
    localStorage.setItem(keys.user, JSON.stringify(user))
  }
}

/**
 * 清除用户登录信息
 * 🔒 会话隔离：根据当前路由清除对应角色的信息
 * @param {string} role - 角色类型（可选）
 */
export function clearCurrentUser(role = null) {
  const keys = getStorageKeys(role)
  localStorage.removeItem(keys.user)
  localStorage.removeItem(keys.token)
}

/**
 * 清除所有角色的登录信息（完全退出登录）
 */
export function clearAllUsers() {
  Object.values(STORAGE_KEYS).forEach(keys => {
    localStorage.removeItem(keys.user)
    localStorage.removeItem(keys.token)
  })
}

/**
 * 检查用户是否已登录
 * 🔒 会话隔离：检查当前路由对应角色的登录状态
 * @param {string} role - 角色类型（可选）
 * @returns {boolean}
 */
export function isLoggedIn(role = null) {
  const user = getCurrentUser(role)
  const token = getToken(role)
  return user !== null && token !== null
}

/**
 * 获取用户类型
 * 🔒 会话隔离：获取当前路由对应角色的用户类型
 * @param {string} role - 角色类型（可选）
 * @returns {string|null} 'student' | 'admin' | 'superadmin' | null
 */
export function getUserType(role = null) {
  const user = getCurrentUser(role)
  return user ? user.userType : null
}

// ========== Token 管理 ==========

/**
 * 获取 JWT Token
 * 🔒 会话隔离：根据当前路由获取对应角色的 token
 * @param {string} role - 角色类型（可选）
 * @returns {string|null}
 */
export function getToken(role = null) {
  const keys = getStorageKeys(role)
  return localStorage.getItem(keys.token)
}

/**
 * 保存 JWT Token
 * 🔒 会话隔离：根据角色保存到对应的存储空间
 * @param {string} token
 * @param {string} role - 角色类型（必需）
 */
export function setToken(token, role) {
  if (!role) {
    console.warn('⚠️ setToken 需要指定角色类型')
    return
  }
  const keys = getStorageKeys(role)
  localStorage.setItem(keys.token, token)
}

/**
 * 清除 JWT Token
 * 🔒 会话隔离：根据当前路由清除对应角色的 token
 * @param {string} role - 角色类型（可选）
 */
export function clearToken(role = null) {
  const keys = getStorageKeys(role)
  localStorage.removeItem(keys.token)
}

/**
 * 获取当前用户ID
 * @param {string} role - 角色类型（可选）
 * @returns {number|null}
 */
export function getCurrentUserId(role = null) {
  const user = getCurrentUser(role)
  return user ? (user.id || user.userId) : null
}
