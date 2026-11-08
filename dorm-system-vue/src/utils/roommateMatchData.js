// 室友匹配数据管理
const STORAGE_KEY = 'roommate_match_data'

// 获取存储的数据
function getStoredData() {
  const data = localStorage.getItem(STORAGE_KEY)
  return data ? JSON.parse(data) : {
    users: [],
    dorms: [],
    matchSettings: {},
    invitations: []
  }
}

// 保存数据到本地存储
function saveData(data) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

// 用户相关操作
export function getUserById(userId) {
  const data = getStoredData()
  return data.users.find(user => user.id === userId)
}

export function saveUser(user) {
  const data = getStoredData()
  const existingIndex = data.users.findIndex(u => u.id === user.id)
  
  if (existingIndex >= 0) {
    data.users[existingIndex] = { ...data.users[existingIndex], ...user }
  } else {
    data.users.push(user)
  }
  
  saveData(data)
}

export function getAllUsers() {
  const data = getStoredData()
  return data.users
}

// 宿舍相关操作
export function getDormById(dormId) {
  const data = getStoredData()
  return data.dorms.find(dorm => dorm.id === dormId)
}

export function saveDorm(dorm) {
  const data = getStoredData()
  const existingIndex = data.dorms.findIndex(d => d.id === dorm.id)
  
  if (existingIndex >= 0) {
    data.dorms[existingIndex] = { ...data.dorms[existingIndex], ...dorm }
  } else {
    data.dorms.push(dorm)
  }
  
  saveData(data)
}

export function getAllDorms() {
  const data = getStoredData()
  return data.dorms
}

// 匹配设置相关操作
export function getMatchSettings(userId) {
  const data = getStoredData()
  return data.matchSettings[userId] || {
    preciseMatchTags: { like: [], dislike: [] },
    fuzzyMatchTags: { like: [], dislike: [] },
    matchPreference: 'precise'
  }
}

export function saveMatchSettings(userId, settings) {
  const data = getStoredData()
  data.matchSettings[userId] = settings
  saveData(data)
}

// 邀请相关操作
export function getInvitations(userId) {
  const data = getStoredData()
  return data.invitations.filter(invite => invite.toUserId === userId)
}

export function saveInvitation(invitation) {
  const data = getStoredData()
  data.invitations.push(invitation)
  saveData(data)
}

export function updateInvitation(invitationId, updates) {
  const data = getStoredData()
  const index = data.invitations.findIndex(invite => invite.id === invitationId)
  
  if (index >= 0) {
    data.invitations[index] = { ...data.invitations[index], ...updates }
    saveData(data)
  }
}

// 智能匹配算法
export function performSmartMatch(userId, matchTags, matchFilters = {}) {
  const data = getStoredData()
  const currentUser = getUserById(userId)
  const allUsers = getAllUsers().filter(user => user.id !== userId)
  
  // 过滤掉不符合匹配条件的用户
  const filteredUsers = allUsers.filter(user => {
    // 🚨 关键：性别过滤 - 必须同性别才能匹配
    if (!currentUser.gender || !user.gender || currentUser.gender !== user.gender) {
      return false
    }
    
    // 应用匹配筛选条件
    if (matchFilters.major) {
      if (matchFilters.major === 'same') {
        if (user.major !== currentUser.major) return false
      } else {
        if (user.major !== matchFilters.major) return false
      }
    }
    
    if (matchFilters.className) {
      if (matchFilters.className === 'same') {
        if (user.className !== currentUser.className) return false
      } else {
        if (user.className !== matchFilters.className) return false
      }
    }
    
    const userTags = user.tags || []
    
    // 不希望匹配的标签 - 精准匹配
    if (matchTags.dislike.some(tag => userTags.includes(tag))) {
      return false
    }
    
    return true
  })
  
  // 计算匹配度 - 先精准后模糊
  const scoredUsers = filteredUsers.map(user => {
    const userTags = user.tags || []
    let score = 0
    let preciseMatches = []
    let fuzzyMatches = []
    
    // 先尝试精准匹配
    for (const likeTag of matchTags.like) {
      if (userTags.includes(likeTag)) {
        score += 25 // 精准匹配高分
        preciseMatches.push(likeTag)
      } else {
        // 如果没有精准匹配，尝试模糊匹配
        const fuzzyMatch = userTags.find(userTag => 
          userTag.includes(likeTag) || likeTag.includes(userTag)
        )
        if (fuzzyMatch) {
          score += 15 // 模糊匹配较低分
          fuzzyMatches.push({ original: likeTag, matched: fuzzyMatch })
        }
      }
    }
    
    // 专业相同加分
    if (currentUser.major === user.major) {
      score += 15
    }
    
    // 班级相同加分
    if (currentUser.className === user.className) {
      score += 10
    }
    
    return {
      ...user,
      matchScore: Math.min(score, 100),
      preciseMatches,
      fuzzyMatches
    }
  })
  
  // 按匹配度排序
  scoredUsers.sort((a, b) => b.matchScore - a.matchScore)
  
  return scoredUsers.slice(0, 15) // 返回前15个最佳匹配，用于测试分页功能
}

// 随机匹配算法
export function performRandomMatch(userId, matchFilters = {}) {
  const data = getStoredData()
  const currentUser = getUserById(userId)
  const allUsers = getAllUsers().filter(user => user.id !== userId)
  
  // 🚨 关键：性别过滤 - 只匹配同性别用户
  const filteredUsers = allUsers.filter(user => {
    if (!currentUser.gender || !user.gender || currentUser.gender !== user.gender) {
      return false
    }
    
    // 应用匹配筛选条件
    if (matchFilters.major) {
      if (matchFilters.major === 'same') {
        if (user.major !== currentUser.major) return false
      } else {
        if (user.major !== matchFilters.major) return false
      }
    }
    
    if (matchFilters.className) {
      if (matchFilters.className === 'same') {
        if (user.className !== currentUser.className) return false
      } else {
        if (user.className !== matchFilters.className) return false
      }
    }
    
    
    return true
  })
  
  // 随机打乱筛选后的用户列表
  const shuffledUsers = [...filteredUsers].sort(() => Math.random() - 0.5)
  
  return shuffledUsers.slice(0, 15).map(user => ({
    ...user,
    matchScore: null // 随机匹配不需要计算匹配分数
  }))
}

// 初始化测试数据
export function initTestData() {
  const testData = {
    users: [
      {
        id: 1,
        name: '张三',
        studentId: '2021001001',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男', // 性别：男/女
        avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
        bio: '热爱编程，喜欢安静的环境',
        tags: ['爱干净', '夜猫子', '爱学习']
      },
      {
        id: 2,
        name: '李四',
        studentId: '2021001002',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
        bio: '喜欢学习，生活规律，爱干净。平时喜欢在图书馆看书，希望找到一个同样爱学习的室友一起进步。',
        tags: ['爱学习', '爱干净', '安静', '早起', '爱读书']
      },
      {
        id: 3,
        name: '王五',
        studentId: '2021001003',
        major: '软件工程',
        className: '软工2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/9/bb/e27858cc9733e7b337d3211f5c70e8png.png',
        bio: '热爱编程和运动，周末经常去健身房。性格开朗，喜欢和室友一起学习讨论技术问题。',
        tags: ['爱学习', '爱运动', '外向', '爱做饭', '幽默']
      },
      {
        id: 4,
        name: '小美',
        studentId: '2021001004',
        major: '信息管理',
        className: '信管2101班',
        gender: '女',
        avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
        bio: '外向开朗，喜欢音乐和旅行。会弹吉他，偶尔会在宿舍练习，希望室友不要介意。',
        tags: ['爱音乐', '外向', '爱旅行', '夜猫子', '爱游戏']
      },
      {
        id: 5,
        name: '小雅',
        studentId: '2021001005',
        major: '数据科学',
        className: '数科2101班',
        gender: '女',
        avatar: 'https://cube.elemecdn.com/6/94/4d3ea53c6becc11b6b5b6d71de44cpng.png',
        bio: '喜欢安静的学习环境，作息规律。平时爱看电影，喜欢独立思考，但也愿意与室友分享有趣的想法。',
        tags: ['安静', '独立', '爱学习', '早起', '内向']
      },
      {
        id: 6,
        name: '刘强',
        studentId: '2021001006',
        major: '计算机科学与技术',
        className: '计科2102班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/8/6c/0b3c9e8a7e3c8d8f8e8e8e8e8e8e8epng.png',
        bio: '热爱编程，经常参加编程竞赛。性格内向但专业能力强，希望找到志同道合的室友。',
        tags: ['爱学习', '内向', '安静', '严谨', '爱游戏']
      },
      {
        id: 7,
        name: '孙伟',
        studentId: '2021001007',
        major: '软件工程',
        className: '软工2102班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/4/8e/0c2e3a8d4f5a6b7c8d9e0f1a2b3c4epng.png',
        bio: '喜欢运动和音乐，会弹钢琴。性格开朗，希望能在宿舍营造轻松愉快的氛围。',
        tags: ['爱音乐', '爱运动', '外向', '随和', '幽默']
      },
      {
        id: 8,
        name: '小兰',
        studentId: '2021001008',
        major: '计算机科学与技术',
        className: '计科2103班',
        gender: '女',
        avatar: 'https://cube.elemecdn.com/a/2b/3c4d5e6f7a8b9c0d1e2f3a4b5c6d7epng.png',
        bio: '热爱学习，目标是考研究生。喜欢安静的环境，作息规律，希望室友也能互相督促学习。',
        tags: ['爱学习', '安静', '早起', '严谨', '爱读书']
      },
      {
        id: 9,
        name: '小玲',
        studentId: '2021001009',
        major: '信息管理',
        className: '信管2102班',
        gender: '女',
        avatar: 'https://cube.elemecdn.com/b/3c/4d5e6f7a8b9c0d1e2f3a4b5c6d7e8fpng.png',
        bio: '性格活泼开朗，喜欢音乐和舞蹈。经常参加社团活动，希望室友能理解偶尔晚归的情况。',
        tags: ['外向', '爱音乐', '爱旅行', '夜猫子', '合群']
      },
      {
        id: 10,
        name: '小慧',
        studentId: '2021001010',
        major: '数据科学',
        className: '数科2102班',
        gender: '女',
        avatar: 'https://cube.elemecdn.com/c/4d/5e6f7a8b9c0d1e2f3a4b5c6d7e8f9apng.png',
        bio: '喜欢做饭和整理，希望宿舍能保持干净整洁。性格温和，善于沟通，重视室友关系。',
        tags: ['爱干净', '爱做饭', '随和', '早起', '合群']
      },
      // 新增更多计科2101班男生，用于测试分页功能
      {
        id: 11,
        name: '赵六',
        studentId: '2021001011',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/1/2c/3d4e5f6a7b8c9d0e1f2a3b4c5d6e7fpng.png',
        bio: '热爱编程，经常熬夜写代码。喜欢安静的环境，希望室友也能理解程序员的生活节奏。',
        tags: ['爱学习', '夜猫子', '安静', '严谨', '爱游戏']
      },
      {
        id: 12,
        name: '钱七',
        studentId: '2021001012',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/2/3d/4e5f6a7b8c9d0e1f2a3b4c5d6e7f8apng.png',
        bio: '喜欢运动和音乐，会弹吉他。性格开朗，希望能在宿舍营造轻松愉快的氛围。',
        tags: ['爱音乐', '爱运动', '外向', '随和', '幽默']
      },
      {
        id: 13,
        name: '孙八',
        studentId: '2021001013',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/3/4e/5f6a7b8c9d0e1f2a3b4c5d6e7f8a9bpng.png',
        bio: '热爱学习，目标是考研究生。喜欢安静的环境，作息规律，希望室友也能互相督促学习。',
        tags: ['爱学习', '安静', '早起', '严谨', '爱读书']
      },
      {
        id: 14,
        name: '李九',
        studentId: '2021001014',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/4/5f/6a7b8c9d0e1f2a3b4c5d6e7f8a9b0cpng.png',
        bio: '性格活泼开朗，喜欢音乐和舞蹈。经常参加社团活动，希望室友能理解偶尔晚归的情况。',
        tags: ['外向', '爱音乐', '爱旅行', '夜猫子', '合群']
      },
      {
        id: 15,
        name: '周十',
        studentId: '2021001015',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/5/6a/7b8c9d0e1f2a3b4c5d6e7f8a9b0c1dpng.png',
        bio: '喜欢做饭和整理，希望宿舍能保持干净整洁。性格温和，善于沟通，重视室友关系。',
        tags: ['爱干净', '爱做饭', '随和', '早起', '合群']
      },
      {
        id: 16,
        name: '吴十一',
        studentId: '2021001016',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/6/7b/8c9d0e1f2a3b4c5d6e7f8a9b0c1d2epng.png',
        bio: '热爱编程和运动，周末经常去健身房。性格开朗，喜欢和室友一起学习讨论技术问题。',
        tags: ['爱学习', '爱运动', '外向', '爱做饭', '幽默']
      },
      {
        id: 17,
        name: '郑十二',
        studentId: '2021001017',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/7/8c/9d0e1f2a3b4c5d6e7f8a9b0c1d2e3fpng.png',
        bio: '内向但专业能力强，喜欢独立思考。平时爱看电影，希望室友能理解安静的学习环境。',
        tags: ['安静', '独立', '爱学习', '内向', '严谨']
      },
      {
        id: 18,
        name: '王十三',
        studentId: '2021001018',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/8/9d/0e1f2a3b4c5d6e7f8a9b0c1d2e3f4apng.png',
        bio: '喜欢音乐和旅行，会弹钢琴。性格开朗，希望能在宿舍营造轻松愉快的氛围。',
        tags: ['爱音乐', '爱旅行', '外向', '随和', '幽默']
      },
      {
        id: 19,
        name: '冯十四',
        studentId: '2021001019',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/9/0e/1f2a3b4c5d6e7f8a9b0c1d2e3f4a5bpng.png',
        bio: '热爱学习，作息规律。平时爱看书，喜欢独立思考，但也愿意与室友分享有趣的想法。',
        tags: ['爱学习', '安静', '早起', '爱读书', '严谨']
      },
      {
        id: 20,
        name: '陈十五',
        studentId: '2021001020',
        major: '计算机科学与技术',
        className: '计科2101班',
        gender: '男',
        avatar: 'https://cube.elemecdn.com/0/1f/2a3b4c5d6e7f8a9b0c1d2e3f4a5b6cpng.png',
        bio: '喜欢运动和游戏，性格开朗。希望找到志同道合的室友一起学习和娱乐。',
        tags: ['爱运动', '爱游戏', '外向', '随和', '合群']
      }
    ],
    dorms: [
      {
        id: 1,
        building: '3号楼',
        room: '502室',
        capacity: 4,
        currentOccupants: 2,
        occupants: [1, 2]
      },
      {
        id: 2,
        building: '2号楼',
        room: '301室',
        capacity: 4,
        currentOccupants: 1,
        occupants: [3]
      }
    ],
    matchSettings: {},
    invitations: []
  }
  
  saveData(testData)
}

// 清除所有数据
export function clearAllData() {
  localStorage.removeItem(STORAGE_KEY)
}

// 重置为测试数据
export function resetToTestData() {
  clearAllData()
  initTestData()
}
