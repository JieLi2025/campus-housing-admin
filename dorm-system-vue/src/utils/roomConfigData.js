// 房间配置数据管理工具

const STORAGE_KEY = 'roomConfigTemplates'

// 设施选项数据（可自定义）
const FACILITIES_STORAGE_KEY = 'roomFacilitiesConfig'

// 默认设施选项（纯文本，无图标）- 仅固定设施，统一使用中文 key
const defaultFacilityOptions = [
  { key: '阳台', label: '阳台' },
  { key: '空调', label: '空调' },
  { key: '独立卫浴', label: '独立卫浴' },
  { key: 'WiFi', label: 'WiFi' },
  { key: '暖气', label: '暖气' },
  { key: '热水器', label: '热水器' }
]

// 获取设施选项配置
export function getFacilityOptions() {
  try {
    const data = localStorage.getItem(FACILITIES_STORAGE_KEY)
    if (data) {
      return JSON.parse(data)
    }
    // 初始化默认设施
    saveFacilityOptions(defaultFacilityOptions)
    return defaultFacilityOptions
  } catch (error) {
    console.error('获取设施选项失败:', error)
    return defaultFacilityOptions
  }
}

// 保存设施选项配置
export function saveFacilityOptions(options) {
  try {
    localStorage.setItem(FACILITIES_STORAGE_KEY, JSON.stringify(options))
    return true
  } catch (error) {
    console.error('保存设施选项失败:', error)
    return false
  }
}

// 添加自定义设施
export function addCustomFacility(facility) {
  const options = getFacilityOptions()
  
  // 检查key是否已存在
  if (options.some(opt => opt.key === facility.key)) {
    return { success: false, message: '设施标识已存在' }
  }
  
  options.push(facility)
  saveFacilityOptions(options)
  return { success: true, message: '添加成功' }
}

// 删除设施
export function deleteFacility(key) {
  const options = getFacilityOptions()
  const filteredOptions = options.filter(opt => opt.key !== key)
  
  if (filteredOptions.length === options.length) {
    return { success: false, message: '设施不存在' }
  }
  
  saveFacilityOptions(filteredOptions)
  return { success: true, message: '删除成功' }
}

// 更新设施
export function updateFacility(key, updates) {
  const options = getFacilityOptions()
  const index = options.findIndex(opt => opt.key === key)
  
  if (index === -1) {
    return { success: false, message: '设施不存在' }
  }
  
  options[index] = { ...options[index], ...updates }
  saveFacilityOptions(options)
  return { success: true, message: '更新成功' }
}

// 重置为默认设施
export function resetFacilityOptions() {
  saveFacilityOptions(defaultFacilityOptions)
  return { success: true, message: '已重置为默认设施' }
}

// 动态导出（为了保持兼容性）
export const facilityOptions = getFacilityOptions()

// 房间类型选项
export const roomTypeOptions = [
  '双人间',
  '四人间',
  '六人间',
  '八人间'
]

// 默认房间配置模板（仅固定设施）- 统一使用中文 key
const defaultTemplates = {
  '双人间': {
    roomType: '双人间',
    bedCount: 2,
    facilities: {
      '阳台': true,
      '空调': true,
      '独立卫浴': true,
      'WiFi': true,
      '暖气': true,
      '热水器': true
    },
    description: '标准双人间配置'
  },
  '四人间': {
    roomType: '四人间',
    bedCount: 4,
    facilities: {
      '阳台': true,
      '空调': true,
      '独立卫浴': true,
      'WiFi': true,
      '暖气': false,
      '热水器': true
    },
    description: '标准四人间配置'
  },
  '六人间': {
    roomType: '六人间',
    bedCount: 6,
    facilities: {
      '阳台': false,
      '空调': true,
      '独立卫浴': false,
      'WiFi': true,
      '暖气': false,
      '热水器': false
    },
    description: '标准六人间配置，公共卫浴'
  },
  '八人间': {
    roomType: '八人间',
    bedCount: 8,
    facilities: {
      '阳台': false,
      '空调': true,
      '独立卫浴': false,
      'WiFi': true,
      '暖气': false,
      '热水器': false
    },
    description: '标准八人间配置，公共卫浴'
  }
}

// 获取所有房间配置模板
export function getRoomConfigTemplates() {
  try {
    const data = localStorage.getItem(STORAGE_KEY)
    if (data) {
      return JSON.parse(data)
    }
    // 如果没有数据，初始化默认模板
    saveRoomConfigTemplates(defaultTemplates)
    return defaultTemplates
  } catch (error) {
    console.error('获取房间配置模板失败:', error)
    return defaultTemplates
  }
}

// 保存所有房间配置模板
export function saveRoomConfigTemplates(templates) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(templates))
    return true
  } catch (error) {
    console.error('保存房间配置模板失败:', error)
    return false
  }
}

// 获取指定类型的房间配置模板
export function getRoomConfigTemplate(roomType) {
  const templates = getRoomConfigTemplates()
  return templates[roomType] || null
}

// 更新指定类型的房间配置模板
export function updateRoomConfigTemplate(roomType, config) {
  try {
    const templates = getRoomConfigTemplates()
    templates[roomType] = {
      ...templates[roomType],
      ...config,
      roomType // 确保roomType不变
    }
    return saveRoomConfigTemplates(templates)
  } catch (error) {
    console.error('更新房间配置模板失败:', error)
    return false
  }
}

// 重置为默认配置
export function resetRoomConfigTemplates() {
  return saveRoomConfigTemplates(defaultTemplates)
}

// 重置指定类型的配置
export function resetRoomConfigTemplate(roomType) {
  if (!defaultTemplates[roomType]) {
    return false
  }
  return updateRoomConfigTemplate(roomType, defaultTemplates[roomType])
}

// 应用模板到房间（创建房间时使用）
export function applyTemplateToRoom(roomType, roomData = {}) {
  const template = getRoomConfigTemplate(roomType)
  if (!template) {
    return roomData
  }
  
  return {
    ...roomData,
    type: roomType,
    totalBeds: template.bedCount,
    ...template.facilities // 展开所有设施
  }
}

// 获取房间设施显示列表（用于UI展示）
export function getRoomFacilityList(room) {
  const facilities = []
  
  facilityOptions.forEach(option => {
    if (room[option.key]) {
      facilities.push({
        key: option.key,
        label: option.label,
        icon: option.icon
      })
    }
  })
  
  return facilities
}

// 验证房间配置
export function validateRoomConfig(config) {
  const errors = []
  
  if (!config.roomType) {
    errors.push('房间类型不能为空')
  }
  
  if (!config.bedCount || config.bedCount < 1) {
    errors.push('床位数必须大于0')
  }
  
  if (!config.facilities || typeof config.facilities !== 'object') {
    errors.push('设施配置格式错误')
  }
  
  return {
    valid: errors.length === 0,
    errors
  }
}

// 导出配置（用于备份）
export function exportRoomConfigs() {
  const templates = getRoomConfigTemplates()
  const dataStr = JSON.stringify(templates, null, 2)
  const blob = new Blob([dataStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `room-config-${Date.now()}.json`
  link.click()
  URL.revokeObjectURL(url)
}

// 导入配置（用于恢复）
export function importRoomConfigs(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    
    reader.onload = (e) => {
      try {
        const templates = JSON.parse(e.target.result)
        
        // 验证数据格式
        if (typeof templates !== 'object') {
          throw new Error('配置文件格式错误')
        }
        
        // 保存配置
        if (saveRoomConfigTemplates(templates)) {
          resolve(templates)
        } else {
          reject(new Error('保存配置失败'))
        }
      } catch (error) {
        reject(error)
      }
    }
    
    reader.onerror = () => {
      reject(new Error('读取文件失败'))
    }
    
    reader.readAsText(file)
  })
}

