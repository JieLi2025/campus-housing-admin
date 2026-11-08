import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 登录注册模块
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/Register.vue')
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/auth/ForgotPassword.vue')
  },
  {
    path: '/email-verify',
    name: 'EmailVerify',
    component: () => import('../views/auth/EmailVerify.vue')
  },

  // 访客页面
  {
    path: '/visitor-info',
    name: 'VisitorInfo',
    component: () => import('../views/visitor/VisitorInfo.vue')
  },
  {
    path: '/visitor-verify',
    name: 'VisitorVerify',
    component: () => import('../views/visitor/VisitorVerify.vue')
  },

  // 学生端页面
  {
    path: '/student',
    name: 'StudentLayout',
    component: () => import('../views/student/Layout.vue'),
    children: [
      {
        path: '',
        name: 'StudentHome',
        redirect: '/student/roommate-match'
      },
      {
        path: 'profile',
        name: 'StudentProfile',
        component: () => import('../views/student/Profile.vue')
      },
      {
        path: 'roommate-match',
        name: 'RoommateMatch',
        component: () => import('../views/student/RoommateMatch.vue')
      },
      {
        path: 'allocation-flow',
        name: 'StudentAllocationFlow',
        component: () => import('../views/student/AllocationFlow.vue')
      },

      {
        path: 'dorm-management',
        name: 'MyDormInfo',
        component: () => import('../views/student/MyDormInfo.vue')
      },
      {
        path: 'material-management',
        name: 'MaterialManagement',
        component: () => import('../views/student/MaterialManagement.vue')
      },
      {
        path: 'dorm-chat',
        name: 'DormChat',
        component: () => import('../views/student/DormChat.vue')
      },
      {
        path: 'building-info',
        name: 'StudentBuildingInfo',
        component: () => import('../views/student/BuildingInfo.vue')
      },
      {
        path: 'notifications',
        name: 'StudentNotifications',
        component: () => import('../views/student/Notifications.vue')
      },
      {
        path: 'chat-messages',
        name: 'StudentChatMessages',
        component: () => import('../views/student/ChatMessages.vue')
      },

    ]
  },

  // 管理员端页面
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('../views/admin/Layout.vue'),
    children: [
      {
        path: '',
        name: 'AdminHome',
        redirect: '/admin/dorm-management'
      },
      {
        path: 'dorm-management',
        name: 'AdminDormManagement',
        component: () => import('../views/admin/DormManagement.vue')
      },
      {
        path: 'student-allocation',
        name: 'StudentAllocation',
        component: () => import('../views/admin/StudentAllocation.vue')
      },
      {
        path: 'application-review',
        name: 'ApplicationReview',
        component: () => import('../views/admin/ApplicationReview.vue')
      },
      {
        path: 'announcement',
        name: 'Announcement',
        component: () => import('../views/admin/Announcement.vue')
      },

      {
        path: 'visitor-management',
        name: 'VisitorManagement',
        component: () => import('../views/admin/VisitorManagement.vue')
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('../views/admin/Profile.vue')
      },
      {
        path: 'notifications',
        name: 'AdminNotifications',
        component: () => import('../views/admin/Notifications.vue')
      },
        {
          path: 'chat-messages',
          name: 'AdminChatMessages',
          component: () => import('../views/admin/ChatMessages.vue')
        },
        {
          path: 'student-info',
          name: 'AdminStudentInfo',
          component: () => import('../views/admin/StudentInfo.vue')
        }
        ,
        {
          path: 'building-group-chat',
          name: 'BuildingGroupChat',
          component: () => import('../views/admin/BuildingGroupChat.vue')
        },
        {
          path: 'material-inventory/:dormNumber/:applicationId',
          name: 'MaterialInventory',
          component: () => import('../views/admin/MaterialInventory.vue'),
          meta: { title: '退宿物资清点' }
        }
    ]
  },

  // 超级管理员端页面
  {
    path: '/super-admin',
    name: 'SuperAdminLayout',
    component: () => import('../views/super-admin/Layout.vue'),
    children: [
      {
        path: '',
        name: 'SuperAdminHome',
        redirect: '/super-admin/student-management'
      },
      {
        path: 'student-management',
        name: 'StudentManagement',
        component: () => import('../views/super-admin/StudentManagement.vue')
      },
      {
        path: 'admin-management',
        name: 'AdminManagement',
        component: () => import('../views/super-admin/AdminManagement.vue')
      },
      {
        path: 'building-management',
        name: 'BuildingManagement',
        component: () => import('../views/super-admin/BuildingManagement.vue')
      },
      {
        path: 'config-management',
        name: 'ConfigManagement',
        component: () => import('../views/super-admin/ConfigManagement.vue')
      },
      {
        path: 'announcement',
        name: 'SuperAdminAnnouncement',
        component: () => import('../views/super-admin/Announcement.vue')
      },
      // 兼容旧路径，重定向到配置管理页面
      {
        path: 'system-config',
        redirect: '/super-admin/config-management'
      },
      {
        path: 'material-config',
        redirect: '/super-admin/config-management'
      },
      {
        path: 'room-config',
        redirect: '/super-admin/config-management'
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 权限控制
router.beforeEach((to, from, next) => {
  // 🔒 会话隔离：动态导入 auth 工具函数，避免循环依赖
  // 根据目标路由获取对应角色的用户信息
  let token = null
  let userType = null
  
  // 推断目标路由的角色
  const targetPath = to.path
  let targetRole = null
  if (targetPath.startsWith('/student')) {
    targetRole = 'student'
  } else if (targetPath.startsWith('/super-admin')) {
    targetRole = 'superadmin'
  } else if (targetPath.startsWith('/admin')) {
    targetRole = 'admin'
  }
  
  // 使用会话隔离的存储键
  if (targetRole) {
    const STORAGE_KEYS = {
      student: { user: 'student_user', token: 'student_token' },
      admin: { user: 'admin_user', token: 'admin_token' },
      superadmin: { user: 'superadmin_user', token: 'superadmin_token' }
    }
    
    const keys = STORAGE_KEYS[targetRole]
    token = localStorage.getItem(keys.token)
    
    const currentUserStr = localStorage.getItem(keys.user)
    if (currentUserStr) {
      try {
        const currentUser = JSON.parse(currentUserStr)
        userType = currentUser.userType
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }
  }
  
  // 公开页面，无需登录
  const publicPages = ['/login', '/register', '/forgot-password', '/email-verify', '/visitor-info', '/visitor-verify']
  const isPublicPage = publicPages.some(page => to.path.startsWith(page))
  
  // 如果是公开页面，直接放行
  if (isPublicPage) {
    next()
    return
  }
  
  // 检查是否已登录
  if (!token || !userType) {
    // 未登录，跳转到登录页
    next('/login')
    return
  }
  
  // 根据路由路径和用户类型进行权限校验
  const path = to.path
  
  // 超级管理员页面：只允许 superadmin 访问
  if (path.startsWith('/super-admin')) {
    if (userType !== 'superadmin') {
      console.warn(`权限不足：用户类型 ${userType} 尝试访问超级管理员页面 ${path}`)
      // 跳转到对应角色的首页
      if (userType === 'admin') {
        next('/admin')
      } else if (userType === 'student') {
        next('/student')
      } else {
        next('/login')
      }
      return
    }
  }
  
  // 管理员页面：允许 admin 和 superadmin 访问
  if (path.startsWith('/admin')) {
    if (userType !== 'admin' && userType !== 'superadmin') {
      console.warn(`权限不足：用户类型 ${userType} 尝试访问管理员页面 ${path}`)
      if (userType === 'student') {
        next('/student')
      } else {
        next('/login')
      }
      return
    }
  }
  
  // 学生页面：只允许 student 访问
  if (path.startsWith('/student')) {
    if (userType !== 'student') {
      console.warn(`权限不足：用户类型 ${userType} 尝试访问学生页面 ${path}`)
      if (userType === 'admin') {
        next('/admin')
      } else if (userType === 'superadmin') {
        next('/super-admin')
      } else {
        next('/login')
      }
      return
    }
  }
  
  // 权限验证通过，允许访问
  next()
})

export default router
