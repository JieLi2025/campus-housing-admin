/**
 * 学生分配数据管理器
 * 提供统一的数据持久化和状态同步机制
 */

import { 
  getDormGroups, 
  saveDormGroups, 
  getRemainingStudents, 
  saveRemainingStudents,
  getGroupingStatistics,
  validateGroupingData,
  clearAllGroupingData,
  GroupStatus
} from './studentGrouping.js'
import { 
  getRoomSelectionRecords, 
  saveRoomSelectionRecords,
  getRoomSelectionStatus,
  setRoomSelectionStatus,
  getRoomSelectionStatistics,
  clearRoomSelectionData
} from './roomSelection.js'
import { validateAllocationResults } from './adminProcessing.js'

// 分配阶段枚举
export const AllocationPhase = {
  PREPARATION: 'preparation',           // 准备阶段
  SELF_GROUPING: 'self_grouping',      // 学生自组阶段
  ROOM_SELECTION: 'room_selection',     // 房间选择阶段
  ADMIN_PROCESSING: 'admin_processing', // 管理员处理阶段
  COMPLETED: 'completed'                // 完成阶段
}

// 存储键名常量
const STORAGE_KEYS = {
  ALLOCATION_PHASE: 'allocation_phase',
  GROUPING_TIME_SETTINGS: 'grouping_time_settings',
  PHASE_HISTORY: 'phase_history',
  ALLOCATION_SETTINGS: 'allocation_settings',
  DATA_VERSION: 'allocation_data_version'
}

// 数据版本（用于兼容性检查）
const CURRENT_DATA_VERSION = '1.0.0'

/**
 * 获取当前分配阶段
 */
export function getCurrentPhase() {
  try {
    const phase = localStorage.getItem(STORAGE_KEYS.ALLOCATION_PHASE)
    return phase && Object.values(AllocationPhase).includes(phase) ? 
      phase : AllocationPhase.PREPARATION
  } catch (error) {
    console.error('获取当前阶段失败:', error)
    return AllocationPhase.PREPARATION
  }
}

/**
 * 设置当前分配阶段
 */
export function setCurrentPhase(phase) {
  try {
    if (!Object.values(AllocationPhase).includes(phase)) {
      throw new Error(`无效的分配阶段: ${phase}`)
    }
    
    // 记录阶段历史
    const history = getPhaseHistory()
    history.push({
      phase,
      timestamp: new Date().toISOString(),
      previousPhase: getCurrentPhase()
    })
    savePhaseHistory(history)
    
    // 保存当前阶段
    localStorage.setItem(STORAGE_KEYS.ALLOCATION_PHASE, phase)
    
    // 触发阶段变更事件
    window.dispatchEvent(new CustomEvent('allocation-phase-changed', {
      detail: { phase, timestamp: new Date().toISOString() }
    }))
    
    return true
  } catch (error) {
    console.error('设置当前阶段失败:', error)
    return false
  }
}

/**
 * 获取阶段历史记录
 */
export function getPhaseHistory() {
  try {
    const history = localStorage.getItem(STORAGE_KEYS.PHASE_HISTORY)
    return history ? JSON.parse(history) : []
  } catch (error) {
    console.error('获取阶段历史失败:', error)
    return []
  }
}

/**
 * 保存阶段历史记录
 */
function savePhaseHistory(history) {
  try {
    localStorage.setItem(STORAGE_KEYS.PHASE_HISTORY, JSON.stringify(history))
    return true
  } catch (error) {
    console.error('保存阶段历史失败:', error)
    return false
  }
}

/**
 * 获取自组时间设置
 */
export function getGroupingTimeSettings() {
  try {
    const settings = localStorage.getItem(STORAGE_KEYS.GROUPING_TIME_SETTINGS)
    return settings ? JSON.parse(settings) : { startTime: null, endTime: null }
  } catch (error) {
    console.error('获取自组时间设置失败:', error)
    return { startTime: null, endTime: null }
  }
}

/**
 * 保存自组时间设置
 */
export function saveGroupingTimeSettings(startTime, endTime) {
  try {
    const settings = {
      startTime,
      endTime,
      updateTime: new Date().toISOString()
    }
    localStorage.setItem(STORAGE_KEYS.GROUPING_TIME_SETTINGS, JSON.stringify(settings))
    
    // 触发时间设置变更事件
    window.dispatchEvent(new CustomEvent('grouping-time-changed', {
      detail: settings
    }))
    
    return true
  } catch (error) {
    console.error('保存自组时间设置失败:', error)
    return false
  }
}

/**
 * 获取自组时间状态
 */
export function getGroupingTimeStatus() {
  const settings = getGroupingTimeSettings()
  
  if (!settings.startTime || !settings.endTime) {
    return { status: 'not_set', text: '未设置' }
  }
  
  const now = new Date()
  const start = new Date(settings.startTime)
  const end = new Date(settings.endTime)
  
  if (now < start) {
    return { status: 'not_started', text: '未开始', type: 'info' }
  } else if (now >= start && now <= end) {
    return { status: 'active', text: '进行中', type: 'success' }
  } else {
    return { status: 'expired', text: '已结束', type: 'danger' }
  }
}

/**
 * 获取完整的分配状态
 */
export function getAllocationStatus() {
  try {
    const currentPhase = getCurrentPhase()
    const groupingStats = getGroupingStatistics()
    const roomSelectionStats = getRoomSelectionStatistics()
    const groupingTimeStatus = getGroupingTimeStatus()
    const roomSelectionStatus = getRoomSelectionStatus()
    const validation = validateAllocationResults()
    
    return {
      currentPhase,
      phaseIndex: Object.values(AllocationPhase).indexOf(currentPhase),
      groupingStats,
      roomSelectionStats,
      groupingTimeStatus,
      roomSelectionStatus,
      validation,
      lastUpdate: new Date().toISOString()
    }
  } catch (error) {
    console.error('获取分配状态失败:', error)
    return null
  }
}

/**
 * 初始化分配数据
 * 创建默认的学生和宿舍组数据用于演示
 */
export function initializeAllocationData() {
  try {
    // 检查是否已有数据
    const existingGroups = getDormGroups()
    if (existingGroups.length > 0) {
      return { success: true, message: '数据已存在，无需初始化' }
    }
    
    // 创建示例学生数据
    const sampleStudents = [
      { studentId: '2023001', name: '张三', gender: '男', major: '计算机科学与技术', class: '2023-1班', phone: '13800138001' },
      { studentId: '2023002', name: '李四', gender: '男', major: '计算机科学与技术', class: '2023-1班', phone: '13800138002' },
      { studentId: '2023003', name: '王五', gender: '男', major: '计算机科学与技术', class: '2023-1班', phone: '13800138003' },
      { studentId: '2023004', name: '赵六', gender: '男', major: '计算机科学与技术', class: '2023-1班', phone: '13800138004' },
      { studentId: '2023011', name: '小明', gender: '男', major: '软件工程', class: '2023-2班', phone: '13800138011' },
      { studentId: '2023012', name: '小红', gender: '女', major: '软件工程', class: '2023-2班', phone: '13800138012' },
      { studentId: '2023013', name: '小华', gender: '男', major: '软件工程', class: '2023-2班', phone: '13800138013' },
      { studentId: '2023021', name: '陈七', gender: '男', major: '计算机科学与技术', class: '2023-3班', phone: '13800138021' },
      { studentId: '2023022', name: '周八', gender: '男', major: '计算机科学与技术', class: '2023-3班', phone: '13800138022' },
      { studentId: '2023031', name: '李明', gender: '男', major: '计算机科学与技术', class: '2023-1班', phone: '13800138031' },
      { studentId: '2023032', name: '王芳', gender: '女', major: '软件工程', class: '2023-2班', phone: '13800138032' },
      { studentId: '2023033', name: '张伟', gender: '男', major: '计算机科学与技术', class: '2023-1班', phone: '13800138033' },
      { studentId: '2023034', name: '刘敏', gender: '女', major: '软件工程', class: '2023-2班', phone: '13800138034' }
    ]
    
    // 创建示例宿舍组
    const sampleGroups = [
      {
        id: '1',
        groupName: '快乐小分队',
        leaderName: '张三',
        leaderId: '2023001',
        memberCount: 4,
        major: '计算机科学与技术',
        class: '2023-1班',
        gender: '男',
        createTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
        status: GroupStatus.COMPLETE,
        members: [
          { ...sampleStudents[0], role: 'leader', joinTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString() },
          { ...sampleStudents[1], role: 'member', joinTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString() },
          { ...sampleStudents[2], role: 'member', joinTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString() },
          { ...sampleStudents[3], role: 'member', joinTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString() }
        ],
        roomId: null,
        buildingId: null,
        maxMembers: 4
      },
      {
        id: '2',
        groupName: '学霸联盟',
        leaderName: '小明',
        leaderId: '2023011',
        memberCount: 3,
        major: '软件工程',
        class: '2023-2班',
        gender: '男',
        createTime: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
        status: GroupStatus.INCOMPLETE,
        members: [
          { ...sampleStudents[4], role: 'leader', joinTime: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString() },
          { ...sampleStudents[6], role: 'member', joinTime: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString() },
          { ...sampleStudents[7], role: 'member', joinTime: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString() }
        ],
        roomId: null,
        buildingId: null,
        maxMembers: 4
      }
    ]
    
    // 剩余学生（未分组）
    const remainingStudents = [
      { ...sampleStudents[5], status: 'ungrouped', addTime: new Date().toISOString() },
      { ...sampleStudents[9], status: 'ungrouped', addTime: new Date().toISOString() },
      { ...sampleStudents[10], status: 'ungrouped', addTime: new Date().toISOString() },
      { ...sampleStudents[11], status: 'ungrouped', addTime: new Date().toISOString() },
      { ...sampleStudents[12], status: 'ungrouped', addTime: new Date().toISOString() }
    ]
    
    // 保存数据
    saveDormGroups(sampleGroups)
    saveRemainingStudents(remainingStudents)
    
    // 为满员宿舍组分配示例房间（避免验证警告）
    const completeGroups = sampleGroups.filter(g => g.status === GroupStatus.COMPLETE)
    completeGroups.forEach((group, index) => {
      group.roomId = `demo-room-${index + 1}`
      group.buildingId = `demo-building-${index + 1}`
      group.status = GroupStatus.ROOM_SELECTED
    })
    
    // 重新保存更新后的数据
    saveDormGroups(sampleGroups)
    
    // 设置数据版本
    localStorage.setItem(STORAGE_KEYS.DATA_VERSION, CURRENT_DATA_VERSION)
    
    return {
      success: true,
      message: '成功初始化分配数据',
      stats: {
        groups: sampleGroups.length,
        students: sampleStudents.length,
        remainingStudents: remainingStudents.length
      }
    }
    
  } catch (error) {
    console.error('初始化分配数据失败:', error)
    return {
      success: false,
      message: error.message
    }
  }
}

/**
 * 重置所有分配数据
 */
export function resetAllAllocationData() {
  try {
    // 清除所有相关数据
    clearAllGroupingData()
    clearRoomSelectionData()
    localStorage.removeItem(STORAGE_KEYS.ALLOCATION_PHASE)
    localStorage.removeItem(STORAGE_KEYS.GROUPING_TIME_SETTINGS)
    localStorage.removeItem(STORAGE_KEYS.PHASE_HISTORY)
    localStorage.removeItem(STORAGE_KEYS.ALLOCATION_SETTINGS)
    localStorage.removeItem(STORAGE_KEYS.DATA_VERSION)
    
    // 重置到准备阶段
    setCurrentPhase(AllocationPhase.PREPARATION)
    
    // 触发重置事件
    window.dispatchEvent(new CustomEvent('allocation-data-reset', {
      detail: { timestamp: new Date().toISOString() }
    }))
    
    return true
  } catch (error) {
    console.error('重置分配数据失败:', error)
    return false
  }
}

/**
 * 导出完整的分配数据
 */
export function exportAllAllocationData() {
  try {
    const allocationStatus = getAllocationStatus()
    const groups = getDormGroups()
    const remainingStudents = getRemainingStudents()
    const roomSelectionRecords = getRoomSelectionRecords()
    const phaseHistory = getPhaseHistory()
    const groupingTimeSettings = getGroupingTimeSettings()
    
    return {
      exportTime: new Date().toISOString(),
      dataVersion: CURRENT_DATA_VERSION,
      allocationStatus,
      groups,
      remainingStudents,
      roomSelectionRecords,
      phaseHistory,
      groupingTimeSettings
    }
  } catch (error) {
    console.error('导出分配数据失败:', error)
    return null
  }
}

/**
 * 导入分配数据
 */
export function importAllocationData(data) {
  try {
    if (!data || !data.dataVersion) {
      throw new Error('无效的数据格式')
    }
    
    // 检查数据版本兼容性
    if (data.dataVersion !== CURRENT_DATA_VERSION) {
      console.warn(`数据版本不匹配: ${data.dataVersion} vs ${CURRENT_DATA_VERSION}`)
    }
    
    // 导入数据
    if (data.groups) {
      saveDormGroups(data.groups)
    }
    
    if (data.remainingStudents) {
      saveRemainingStudents(data.remainingStudents)
    }
    
    if (data.roomSelectionRecords) {
      saveRoomSelectionRecords(data.roomSelectionRecords)
    }
    
    if (data.groupingTimeSettings) {
      saveGroupingTimeSettings(data.groupingTimeSettings.startTime, data.groupingTimeSettings.endTime)
    }
    
    if (data.allocationStatus && data.allocationStatus.currentPhase) {
      setCurrentPhase(data.allocationStatus.currentPhase)
    }
    
    // 更新数据版本
    localStorage.setItem(STORAGE_KEYS.DATA_VERSION, CURRENT_DATA_VERSION)
    
    // 触发导入事件
    window.dispatchEvent(new CustomEvent('allocation-data-imported', {
      detail: { timestamp: new Date().toISOString() }
    }))
    
    return {
      success: true,
      message: '数据导入成功'
    }
    
  } catch (error) {
    console.error('导入分配数据失败:', error)
    return {
      success: false,
      message: error.message
    }
  }
}

/**
 * 检查数据完整性
 */
export function checkDataIntegrity() {
  try {
    const issues = []
    
    // 检查数据版本
    const dataVersion = localStorage.getItem(STORAGE_KEYS.DATA_VERSION)
    if (!dataVersion) {
      issues.push('缺少数据版本信息')
    } else if (dataVersion !== CURRENT_DATA_VERSION) {
      issues.push(`数据版本过期: ${dataVersion}`)
    }
    
    // 检查分组数据
    const groupingValidation = validateGroupingData()
    if (!groupingValidation.isValid) {
      issues.push(...groupingValidation.issues)
    }
    
    // 检查分配结果
    const allocationValidation = validateAllocationResults()
    if (!allocationValidation.isValid) {
      issues.push(...allocationValidation.issues)
    }
    
    return {
      isValid: issues.length === 0,
      issues,
      lastCheck: new Date().toISOString()
    }
    
  } catch (error) {
    console.error('检查数据完整性失败:', error)
    return {
      isValid: false,
      issues: [error.message],
      lastCheck: new Date().toISOString()
    }
  }
}

/**
 * 自动数据同步（定期保存和验证）
 */
export function startAutoSync(interval = 30000) { // 默认30秒
  const syncInterval = setInterval(() => {
    try {
      // 检查数据完整性
      const integrity = checkDataIntegrity()
      if (!integrity.isValid) {
        console.warn('数据完整性检查失败:', integrity.issues)
      }
      
      // 触发同步事件
      window.dispatchEvent(new CustomEvent('allocation-data-sync', {
        detail: { 
          timestamp: new Date().toISOString(),
          integrity
        }
      }))
      
    } catch (error) {
      console.error('自动同步失败:', error)
    }
  }, interval)
  
  // 返回清理函数
  return () => clearInterval(syncInterval)
}
