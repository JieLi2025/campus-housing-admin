/**
 * 学生数据验证工具
 * 基于Excel导入的学生数据进行验证
 */

/**
 * 从localStorage获取学生数据库
 * @returns {Object} 学生数据库对象
 */
function getStudentsDatabase() {
  try {
    const data = localStorage.getItem('studentsDatabase')
    return data ? JSON.parse(data) : {}
  } catch (error) {
    console.error('获取学生数据库失败:', error)
    return {}
  }
}

/**
 * 保存学生数据库到localStorage
 * @param {Object} database - 学生数据库对象
 */
function saveStudentsDatabase(database) {
  try {
    localStorage.setItem('studentsDatabase', JSON.stringify(database))
  } catch (error) {
    console.error('保存学生数据库失败:', error)
  }
}

/**
 * 从localStorage获取已注册用户列表
 * @returns {Array} 已注册用户列表
 */
function getRegisteredUsers() {
  try {
    const data = localStorage.getItem('registeredUsers')
    return data ? JSON.parse(data) : []
  } catch (error) {
    console.error('获取已注册用户失败:', error)
    return []
  }
}

/**
 * 验证学号是否存在于导入的学生数据中
 * @param {string} studentId - 学号
 * @returns {Object} 验证结果
 */
export function validateStudentId(studentId) {
  const studentsDatabase = getStudentsDatabase()
  
  if (!studentId) {
    return {
      success: false,
      message: '学号不能为空'
    }
  }
  
  // 检查学号格式
  if (!/^\d{8,12}$/.test(studentId)) {
    return {
      success: false,
      message: '学号格式不正确，请输入8-12位数字'
    }
  }
  
  // 检查学号是否在导入的数据中
  if (!studentsDatabase[studentId]) {
    return {
      success: false,
      message: '学号不存在，请联系管理员确认您的学号是否已导入系统'
    }
  }
  
  return {
    success: true,
    message: '学号验证通过',
    studentInfo: studentsDatabase[studentId]
  }
}

/**
 * 验证邀请码是否与学号匹配
 * @param {string} studentId - 学号
 * @param {string} inviteCode - 邀请码
 * @returns {Object} 验证结果
 */
export function validateInviteCode(studentId, inviteCode) {
  const studentsDatabase = getStudentsDatabase()
  
  if (!studentId || !inviteCode) {
    return {
      success: false,
      message: '学号和邀请码不能为空'
    }
  }
  
  // 先验证学号是否存在
  const studentValidation = validateStudentId(studentId)
  if (!studentValidation.success) {
    return studentValidation
  }
  
  const studentInfo = studentsDatabase[studentId]
  
  // 检查邀请码是否匹配
  if (studentInfo.inviteCode !== inviteCode) {
    return {
      success: false,
      message: '邀请码与学号不匹配，请检查输入是否正确'
    }
  }
  
  return {
    success: true,
    message: '邀请码验证通过',
    studentInfo: studentInfo
  }
}

/**
 * 检查学号是否已注册
 * @param {string} studentId - 学号
 * @returns {Object} 检查结果
 */
export function checkStudentRegistration(studentId) {
  const studentsDatabase = getStudentsDatabase()
  const registeredUsers = getRegisteredUsers()
  
  // 检查在数据库中的注册状态
  if (studentsDatabase[studentId] && studentsDatabase[studentId].isRegistered) {
    return {
      isRegistered: true,
      message: '该学号已注册，无法重复注册'
    }
  }
  
  // 检查在注册用户列表中的状态
  const isInRegisteredList = registeredUsers.some(user => user.studentId === studentId)
  if (isInRegisteredList) {
    return {
      isRegistered: true,
      message: '该学号已注册，无法重复注册'
    }
  }
  
  return {
    isRegistered: false,
    message: '学号可以注册'
  }
}

/**
 * 标记学生为已注册状态
 * @param {string} studentId - 学号
 * @param {Object} registrationData - 注册数据
 */
export function markStudentAsRegistered(studentId, registrationData = {}) {
  const studentsDatabase = getStudentsDatabase()
  
  if (studentsDatabase[studentId]) {
    // 更新学生数据库中的注册状态
    studentsDatabase[studentId].isRegistered = true
    studentsDatabase[studentId].registrationDate = new Date().toISOString()
    
    // 如果有额外的注册数据，也保存
    if (registrationData.email) {
      studentsDatabase[studentId].email = registrationData.email
    }
    if (registrationData.phone) {
      studentsDatabase[studentId].phone = registrationData.phone
    }
    
    saveStudentsDatabase(studentsDatabase)
  }
}

/**
 * 获取学生基础信息
 * @param {string} studentId - 学号
 * @returns {Object|null} 学生信息或null
 */
export function getStudentInfo(studentId) {
  const studentsDatabase = getStudentsDatabase()
  return studentsDatabase[studentId] || null
}

/**
 * 获取所有学生信息（用于管理员查看）
 * @returns {Array} 学生信息列表
 */
export function getAllStudents() {
  const studentsDatabase = getStudentsDatabase()
  return Object.values(studentsDatabase)
}

/**
 * 获取可用于注册的学号列表（用于测试）
 * @returns {Array} 可注册的学号列表
 */
export function getAvailableStudentIds() {
  const studentsDatabase = getStudentsDatabase()
  return Object.keys(studentsDatabase).filter(studentId => {
    const student = studentsDatabase[studentId]
    return !student.isRegistered
  })
}

/**
 * 根据学号获取邀请码（用于测试）
 * @param {string} studentId - 学号
 * @returns {string|null} 邀请码
 */
export function getInviteCodeByStudentId(studentId) {
  const studentInfo = getStudentInfo(studentId)
  return studentInfo ? studentInfo.inviteCode : null
}

/**
 * 验证完整的注册信息
 * @param {Object} registrationData - 注册数据
 * @returns {Object} 验证结果
 */
export function validateRegistrationData(registrationData) {
  const { studentId, email, inviteCode, realName } = registrationData
  
  // 验证学号
  const studentValidation = validateStudentId(studentId)
  if (!studentValidation.success) {
    return studentValidation
  }
  
  // 验证是否已注册
  const registrationCheck = checkStudentRegistration(studentId)
  if (registrationCheck.isRegistered) {
    return {
      success: false,
      message: registrationCheck.message
    }
  }
  
  // 验证邀请码
  const inviteValidation = validateInviteCode(studentId, inviteCode)
  if (!inviteValidation.success) {
    return inviteValidation
  }
  
  // 验证真实姓名是否与导入的数据匹配
  const studentInfo = inviteValidation.studentInfo
  if (!realName || !realName.trim()) {
    return {
      success: false,
      message: '真实姓名不能为空'
    }
  }
  
  if (!studentInfo.realName) {
    return {
      success: false,
      message: '系统中没有该学号的姓名记录，请联系管理员'
    }
  }
  
  if (realName.trim() !== studentInfo.realName.trim()) {
    return {
      success: false,
      message: '学号和姓名不匹配，请确认输入的姓名是否正确'
    }
  }
  
  return {
    success: true,
    message: '所有验证通过',
    studentInfo: studentInfo
  }
}

/**
 * 获取统计信息
 * @returns {Object} 统计数据
 */
export function getStatistics() {
  const studentsDatabase = getStudentsDatabase()
  const students = Object.values(studentsDatabase)
  
  const totalStudents = students.length
  const registeredStudents = students.filter(s => s.isRegistered).length
  const unregisteredStudents = totalStudents - registeredStudents
  
  const majors = new Set(students.map(s => s.major))
  const totalMajors = majors.size
  
  const classes = new Set(students.map(s => s.className))
  const totalClasses = classes.size
  
  return {
    totalStudents,
    registeredStudents,
    unregisteredStudents,
    totalMajors,
    totalClasses
  }
}

export default {
  validateStudentId,
  validateInviteCode,
  checkStudentRegistration,
  markStudentAsRegistered,
  getStudentInfo,
  getAllStudents,
  getAvailableStudentIds,
  getInviteCodeByStudentId,
  validateRegistrationData,
  getStatistics
}
