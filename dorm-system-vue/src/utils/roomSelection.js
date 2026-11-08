/**
 * 房间选择管理工具
 * 提供房间选择的核心逻辑和状态管理功能
 */

import { getDormGroups, saveDormGroups, updateDormGroup, GroupStatus } from './studentGrouping.js'
import { getAdminBuildings } from './buildingManagement.js'
import { getBuildingAllocationScope } from './allocationScope.js'

// 存储键名常量
const STORAGE_KEYS = {
  ROOM_SELECTION_RECORDS: 'room_selection_records',
  ROOM_SELECTION_STATUS: 'room_selection_status',
  ROOM_SELECTION_DEADLINE: 'room_selection_deadline',
  AVAILABLE_ROOMS: 'available_rooms_cache'
}

// 房间选择状态枚举
export const SelectionStatus = {
  PENDING: 'pending',      // 待确认
  CONFIRMED: 'confirmed',  // 已确认
  CANCELLED: 'cancelled'   // 已取消
}

// 房间状态枚举
export const RoomStatus = {
  AVAILABLE: 'available',  // 可选择
  SELECTED: 'selected',    // 已被选择
  OCCUPIED: 'occupied',    // 已入住
  MAINTENANCE: 'maintenance' // 维修中
}

/**
 * 获取房间选择记录
 */
export function getRoomSelectionRecords() {
  try {
    const records = localStorage.getItem(STORAGE_KEYS.ROOM_SELECTION_RECORDS)
    return records ? JSON.parse(records) : []
  } catch (error) {
    console.error('获取房间选择记录失败:', error)
    return []
  }
}

/**
 * 保存房间选择记录
 */
export function saveRoomSelectionRecords(records) {
  try {
    localStorage.setItem(STORAGE_KEYS.ROOM_SELECTION_RECORDS, JSON.stringify(records))
    return true
  } catch (error) {
    console.error('保存房间选择记录失败:', error)
    return false
  }
}

/**
 * 获取房间选择开放状态
 */
export function getRoomSelectionStatus() {
  try {
    const status = localStorage.getItem(STORAGE_KEYS.ROOM_SELECTION_STATUS)
    return status ? JSON.parse(status) : { open: false, deadline: null }
  } catch (error) {
    console.error('获取房间选择状态失败:', error)
    return { open: false, deadline: null }
  }
}

/**
 * 设置房间选择状态
 */
export function setRoomSelectionStatus(open, deadline = null) {
  try {
    const status = { open, deadline, updateTime: new Date().toISOString() }
    localStorage.setItem(STORAGE_KEYS.ROOM_SELECTION_STATUS, JSON.stringify(status))
    
    if (deadline) {
      localStorage.setItem(STORAGE_KEYS.ROOM_SELECTION_DEADLINE, deadline)
    } else {
      localStorage.removeItem(STORAGE_KEYS.ROOM_SELECTION_DEADLINE)
    }
    
    return true
  } catch (error) {
    console.error('设置房间选择状态失败:', error)
    return false
  }
}

/**
 * 获取可用房间列表
 * 基于管理员权限和专业分配范围
 */
export function getAvailableRooms(adminUsername) {
  try {
    // 获取管理员管理的楼栋
    const managedBuildings = getAdminBuildings(adminUsername)
    const availableRooms = []
    
    managedBuildings.forEach(building => {
      // 获取楼栋的专业分配范围
      const scope = getBuildingAllocationScope(building.id)
      
      if (scope && scope.professionalChips) {
        let roomCounter = 1
        
        scope.professionalChips.forEach(chip => {
          // 获取该专业的所有班级
          const groups = getDormGroups()
          const majorGroups = groups.filter(g => g.major === chip.majorName)
          const classes = [...new Set(majorGroups.map(g => g.class))].sort()
          
          // 为每个班级分配专门的房间区域
          classes.forEach(className => {
            const classGroups = majorGroups.filter(g => g.class === className)
            const roomsForClass = Math.ceil(classGroups.length / 1) // 每个宿舍组需要1间房
            
            for (let i = 0; i < roomsForClass; i++) {
              const roomNumber = `${building.name.charAt(0)}${String(roomCounter).padStart(3, '0')}`
              
              availableRooms.push({
                id: `${building.id}-${roomNumber}`,
                roomNumber,
                buildingId: building.id,
                buildingName: building.name,
                floor: Math.ceil(roomCounter / 10), // 假设每层10个房间
                capacity: 4,
                currentOccupancy: 0,
                status: RoomStatus.AVAILABLE,
                allowedMajor: chip.majorName,
                allowedClass: className, // 新增：指定允许的班级
                allowedGender: chip.gender || 'mixed',
                facilities: ['床位', '书桌', '衣柜', '空调'],
                createdTime: new Date().toISOString()
              })
              
              roomCounter++
            }
          })
        })
      }
    })
    
    return availableRooms
  } catch (error) {
    console.error('获取可用房间失败:', error)
    return []
  }
}

/**
 * 宿舍组选择房间
 */
export function selectRoom(groupId, roomId, adminUsername) {
  try {
    // 验证宿舍组状态
    const groups = getDormGroups()
    const group = groups.find(g => g.id === groupId)
    
    if (!group) {
      throw new Error('宿舍组不存在')
    }
    
    if (group.status !== GroupStatus.COMPLETE) {
      throw new Error('只有满员的宿舍组才能选择房间')
    }
    
    if (group.roomId) {
      throw new Error('该宿舍组已经选择了房间')
    }
    
    // 验证房间可用性
    const availableRooms = getAvailableRooms(adminUsername)
    const room = availableRooms.find(r => r.id === roomId)
    
    if (!room) {
      throw new Error('房间不存在或不在可选范围内')
    }
    
    if (room.status !== RoomStatus.AVAILABLE) {
      throw new Error('房间不可用')
    }
    
    // 验证专业匹配
    if (room.allowedMajor && group.major !== room.allowedMajor) {
      throw new Error(`该房间仅限${room.allowedMajor}专业学生选择`)
    }
    
    // 验证班级匹配
    if (room.allowedClass && group.class !== room.allowedClass) {
      throw new Error(`该房间仅限${room.allowedClass}班级学生选择`)
    }
    
    // 验证性别匹配
    if (room.allowedGender !== 'mixed' && group.gender !== room.allowedGender) {
      throw new Error(`该房间仅限${room.allowedGender}生选择`)
    }
    
    // 检查房间是否已被其他组选择
    const existingRecords = getRoomSelectionRecords()
    const roomAlreadySelected = existingRecords.find(r => 
      r.roomId === roomId && r.status !== SelectionStatus.CANCELLED
    )
    
    if (roomAlreadySelected) {
      throw new Error('该房间已被其他宿舍组选择')
    }
    
    // 创建选择记录
    const selectionRecord = {
      id: Date.now().toString(),
      groupId: group.id,
      groupName: group.groupName,
      groupLeader: group.leaderName,
      leaderId: group.leaderId,
      roomId: room.id,
      roomNumber: room.roomNumber,
      buildingId: room.buildingId,
      buildingName: room.buildingName,
      floor: room.floor,
      selectionTime: new Date().toISOString(),
      status: SelectionStatus.PENDING,
      adminUsername,
      memberCount: group.memberCount
    }
    
    // 保存选择记录
    existingRecords.push(selectionRecord)
    saveRoomSelectionRecords(existingRecords)
    
    // 更新宿舍组状态
    updateDormGroup(groupId, {
      roomId: room.id,
      buildingId: room.buildingId,
      status: GroupStatus.ROOM_SELECTED
    })
    
    return selectionRecord
    
  } catch (error) {
    console.error('房间选择失败:', error)
    throw error
  }
}

/**
 * 确认房间选择
 */
export function confirmRoomSelection(recordId) {
  try {
    const records = getRoomSelectionRecords()
    const record = records.find(r => r.id === recordId)
    
    if (!record) {
      throw new Error('选择记录不存在')
    }
    
    if (record.status !== SelectionStatus.PENDING) {
      throw new Error('该选择记录已处理')
    }
    
    // 更新记录状态
    record.status = SelectionStatus.CONFIRMED
    record.confirmTime = new Date().toISOString()
    
    saveRoomSelectionRecords(records)
    
    return record
    
  } catch (error) {
    console.error('确认房间选择失败:', error)
    throw error
  }
}

/**
 * 取消房间选择
 */
export function cancelRoomSelection(recordId, reason = '') {
  try {
    const records = getRoomSelectionRecords()
    const record = records.find(r => r.id === recordId)
    
    if (!record) {
      throw new Error('选择记录不存在')
    }
    
    // 更新记录状态
    record.status = SelectionStatus.CANCELLED
    record.cancelTime = new Date().toISOString()
    record.cancelReason = reason
    
    saveRoomSelectionRecords(records)
    
    // 清除宿舍组的房间分配
    updateDormGroup(record.groupId, {
      roomId: null,
      buildingId: null,
      status: GroupStatus.COMPLETE
    })
    
    return record
    
  } catch (error) {
    console.error('取消房间选择失败:', error)
    throw error
  }
}

/**
 * 获取房间选择统计信息
 */
export function getRoomSelectionStatistics() {
  const records = getRoomSelectionRecords()
  const groups = getDormGroups()
  
  const completedGroups = groups.filter(g => g.status === GroupStatus.COMPLETE).length
  const roomSelectedGroups = groups.filter(g => g.roomId).length
  
  const confirmedSelections = records.filter(r => r.status === SelectionStatus.CONFIRMED).length
  const pendingSelections = records.filter(r => r.status === SelectionStatus.PENDING).length
  const cancelledSelections = records.filter(r => r.status === SelectionStatus.CANCELLED).length
  
  return {
    totalCompletedGroups: completedGroups,
    selectedRoomsCount: roomSelectedGroups,
    confirmedSelections,
    pendingSelections,
    cancelledSelections,
    selectionProgress: completedGroups > 0 ? Math.round(roomSelectedGroups / completedGroups * 100) : 0,
    hasUnselectedGroups: roomSelectedGroups < completedGroups
  }
}

/**
 * 获取房间占用情况
 */
export function getRoomOccupancyStatus(adminUsername) {
  const availableRooms = getAvailableRooms(adminUsername)
  const records = getRoomSelectionRecords()
  
  // 更新房间状态
  const roomsWithStatus = availableRooms.map(room => {
    const selection = records.find(r => 
      r.roomId === room.id && r.status !== SelectionStatus.CANCELLED
    )
    
    if (selection) {
      return {
        ...room,
        status: selection.status === SelectionStatus.CONFIRMED ? RoomStatus.OCCUPIED : RoomStatus.SELECTED,
        occupiedBy: selection.groupName,
        occupiedTime: selection.selectionTime,
        currentOccupancy: selection.memberCount
      }
    }
    
    return room
  })
  
  // 按楼栋和楼层分组
  const occupancyByBuilding = {}
  roomsWithStatus.forEach(room => {
    if (!occupancyByBuilding[room.buildingName]) {
      occupancyByBuilding[room.buildingName] = {}
    }
    
    if (!occupancyByBuilding[room.buildingName][room.floor]) {
      occupancyByBuilding[room.buildingName][room.floor] = {
        total: 0,
        available: 0,
        selected: 0,
        occupied: 0,
        rooms: []
      }
    }
    
    const floorData = occupancyByBuilding[room.buildingName][room.floor]
    floorData.total++
    floorData.rooms.push(room)
    
    switch (room.status) {
      case RoomStatus.AVAILABLE:
        floorData.available++
        break
      case RoomStatus.SELECTED:
        floorData.selected++
        break
      case RoomStatus.OCCUPIED:
        floorData.occupied++
        break
    }
  })
  
  return {
    rooms: roomsWithStatus,
    occupancyByBuilding,
    summary: {
      totalRooms: roomsWithStatus.length,
      availableRooms: roomsWithStatus.filter(r => r.status === RoomStatus.AVAILABLE).length,
      selectedRooms: roomsWithStatus.filter(r => r.status === RoomStatus.SELECTED).length,
      occupiedRooms: roomsWithStatus.filter(r => r.status === RoomStatus.OCCUPIED).length
    }
  }
}

/**
 * 批量确认房间选择
 */
export function batchConfirmSelections(recordIds) {
  const results = []
  
  recordIds.forEach(recordId => {
    try {
      const result = confirmRoomSelection(recordId)
      results.push({ recordId, success: true, data: result })
    } catch (error) {
      results.push({ recordId, success: false, error: error.message })
    }
  })
  
  return results
}

/**
 * 自动分配房间给未选择的宿舍组
 * @param {string} adminUsername - 管理员用户名
 * @param {string} strategy - 分配策略: 'class_priority'(班级优先), 'major_match'(专业匹配), 'random'(随机)
 * @param {Array} specificGroupIds - 可选，指定要分配的宿舍组ID列表
 */
export function autoAssignRooms(adminUsername, strategy = 'class_priority', specificGroupIds = null) {
  try {
    const groups = getDormGroups()
    const availableRooms = getAvailableRooms(adminUsername)
    const records = getRoomSelectionRecords()
    
    // 找出未选择房间的满员宿舍组
    let unselectedGroups = groups.filter(g => 
      g.status === GroupStatus.COMPLETE && !g.roomId
    )
    
    // 如果指定了特定的宿舍组，只处理这些组
    if (specificGroupIds && specificGroupIds.length > 0) {
      unselectedGroups = unselectedGroups.filter(g => specificGroupIds.includes(g.id))
    }
    
    // 找出可用房间
    const freeRooms = availableRooms.filter(room => {
      const isSelected = records.some(r => 
        r.roomId === room.id && r.status !== SelectionStatus.CANCELLED
      )
      return !isSelected && room.status === RoomStatus.AVAILABLE
    })
    
    if (unselectedGroups.length === 0) {
      return { 
        success: true, 
        message: specificGroupIds ? '指定的宿舍组都已分配房间' : '没有需要分配房间的宿舍组', 
        assignments: [] 
      }
    }
    
    if (freeRooms.length < unselectedGroups.length) {
      return { 
        success: false, 
        message: `可用房间不足，需要${unselectedGroups.length}间，只有${freeRooms.length}间可用`,
        assignments: [],
        unassignedGroups: unselectedGroups.map(g => ({
          id: g.id,
          name: g.groupName,
          major: g.major,
          class: g.class,
          memberCount: g.memberCount
        }))
      }
    }
    
    const assignments = []
    const usedRooms = new Set()
    
    // 根据策略分配房间
    if (strategy === 'class_priority') {
      // 按班级优先分配 - 同班级的组优先分配到同楼层
      const groupsByClass = {}
      unselectedGroups.forEach(group => {
        const classKey = `${group.major}-${group.class || 'unknown'}`
        if (!groupsByClass[classKey]) {
          groupsByClass[classKey] = []
        }
        groupsByClass[classKey].push(group)
      })
      
      // 为每个班级分配房间
      Object.entries(groupsByClass).forEach(([classKey, classGroups]) => {
        // 找到适合该班级的房间
        const suitableRooms = freeRooms.filter(room => 
          !usedRooms.has(room.id) &&
          (room.allowedMajor === classGroups[0].major || !room.allowedMajor) &&
          (room.allowedClass === classGroups[0].class || !room.allowedClass) &&
          (room.allowedGender === 'mixed' || room.allowedGender === classGroups[0].gender)
        )
        
        // 按楼层分组，尽量让同班级住在同楼层
        const roomsByFloor = {}
        suitableRooms.forEach(room => {
          if (!roomsByFloor[room.floor]) {
            roomsByFloor[room.floor] = []
          }
          roomsByFloor[room.floor].push(room)
        })
        
        let assignedCount = 0
        // 优先使用能容纳整个班级的楼层
        const sortedFloors = Object.entries(roomsByFloor)
          .sort(([, roomsA], [, roomsB]) => roomsB.length - roomsA.length)
        
        for (const [floor, floorRooms] of sortedFloors) {
          for (const group of classGroups) {
            if (assignedCount >= classGroups.length) break
            
            const availableRoom = floorRooms.find(room => !usedRooms.has(room.id))
            if (availableRoom) {
              try {
                const assignment = selectRoom(group.id, availableRoom.id, adminUsername)
                assignments.push({ 
                  groupId: group.id,
                  group: group.groupName, 
                  room: availableRoom.roomNumber,
                  floor: availableRoom.floor,
                  building: availableRoom.buildingName,
                  major: group.major,
                  class: group.class,
                  success: true 
                })
                usedRooms.add(availableRoom.id)
                assignedCount++
              } catch (error) {
                assignments.push({ 
                  groupId: group.id,
                  group: group.groupName, 
                  success: false, 
                  error: error.message 
                })
              }
            }
          }
        }
      })
      
    } else if (strategy === 'major_match') {
      // 优先匹配专业
      unselectedGroups.forEach(group => {
        const matchingRoom = freeRooms.find(room => 
          !usedRooms.has(room.id) &&
          room.allowedMajor === group.major && 
          (room.allowedGender === 'mixed' || room.allowedGender === group.gender)
        )
        
        if (matchingRoom) {
          try {
            const assignment = selectRoom(group.id, matchingRoom.id, adminUsername)
            assignments.push({ 
              groupId: group.id,
              group: group.groupName, 
              room: matchingRoom.roomNumber,
              building: matchingRoom.buildingName,
              major: group.major,
              success: true 
            })
            usedRooms.add(matchingRoom.id)
          } catch (error) {
            assignments.push({ 
              groupId: group.id,
              group: group.groupName, 
              success: false, 
              error: error.message 
            })
          }
        }
      })
      
    } else {
      // 随机分配 - 在符合条件的房间中随机选择
      // 注意：由于管理员只能管理特定楼栋，且楼栋已按性别和专业预分配，
      // 所以这里的"随机"是在当前楼栋符合条件的房间中随机选择
      unselectedGroups.forEach(group => {
        // 找到符合该宿舍组条件的所有可用房间
        const suitableRooms = freeRooms.filter(room => 
          !usedRooms.has(room.id) &&
          // 专业匹配检查
          (room.allowedMajor === group.major || !room.allowedMajor) &&
          // 班级匹配检查 - 关键：确保只能选择分配给该班级的房间
          (room.allowedClass === group.class || !room.allowedClass) &&
          // 性别匹配检查（实际上在同一楼栋内应该都是同性别）
          (room.allowedGender === 'mixed' || room.allowedGender === group.gender)
        )
        
        if (suitableRooms.length > 0) {
          // 在符合条件的房间中随机选择一个
          const randomIndex = Math.floor(Math.random() * suitableRooms.length)
          const selectedRoom = suitableRooms[randomIndex]
          
          try {
            const assignment = selectRoom(group.id, selectedRoom.id, adminUsername)
            assignments.push({ 
              groupId: group.id,
              group: group.groupName, 
              room: selectedRoom.roomNumber,
              building: selectedRoom.buildingName,
              major: group.major,
              class: group.class,
              success: true 
            })
            usedRooms.add(selectedRoom.id)
          } catch (error) {
            assignments.push({ 
              groupId: group.id,
              group: group.groupName, 
              success: false, 
              error: error.message 
            })
          }
        } else {
          assignments.push({ 
            groupId: group.id,
            group: group.groupName, 
            success: false, 
            error: '没有符合条件的可用房间' 
          })
        }
      })
    }
    
    const successCount = assignments.filter(a => a.success).length
    const failCount = assignments.filter(a => !a.success).length
    
    return {
      success: successCount > 0,
      message: `成功为${successCount}个宿舍组分配了房间${failCount > 0 ? `，${failCount}个分配失败` : ''}`,
      assignments,
      statistics: {
        total: unselectedGroups.length,
        success: successCount,
        failed: failCount,
        strategy: strategy
      }
    }
    
  } catch (error) {
    console.error('自动分配房间失败:', error)
    return { success: false, message: error.message, assignments: [] }
  }
}

/**
 * 清空房间选择数据
 */
export function clearRoomSelectionData() {
  localStorage.removeItem(STORAGE_KEYS.ROOM_SELECTION_RECORDS)
  localStorage.removeItem(STORAGE_KEYS.ROOM_SELECTION_STATUS)
  localStorage.removeItem(STORAGE_KEYS.ROOM_SELECTION_DEADLINE)
  localStorage.removeItem(STORAGE_KEYS.AVAILABLE_ROOMS)
  return true
}

/**
 * 获取未分配房间的宿舍组列表
 */
export function getUnassignedGroups() {
  try {
    const groups = getDormGroups()
    
    // 找出已组建完成但未分配房间的宿舍组
    const unassignedGroups = groups.filter(g => 
      g.status === GroupStatus.COMPLETE && !g.roomId
    )
    
    // 按班级分组统计
    const groupsByClass = {}
    unassignedGroups.forEach(group => {
      const classKey = `${group.major}-${group.class || '未知班级'}`
      if (!groupsByClass[classKey]) {
        groupsByClass[classKey] = {
          major: group.major,
          class: group.class || '未知班级',
          groups: [],
          count: 0
        }
      }
      groupsByClass[classKey].groups.push({
        id: group.id,
        groupName: group.groupName,
        leaderName: group.leaderName,
        memberCount: group.memberCount,
        gender: group.gender,
        createdTime: group.createdTime
      })
      groupsByClass[classKey].count++
    })
    
    return {
      total: unassignedGroups.length,
      groupsByClass,
      groups: unassignedGroups.map(g => ({
        id: g.id,
        groupName: g.groupName,
        leaderName: g.leaderName,
        major: g.major,
        class: g.class,
        memberCount: g.memberCount,
        gender: g.gender,
        createdTime: g.createdTime
      }))
    }
  } catch (error) {
    console.error('获取未分配房间的宿舍组失败:', error)
    return { total: 0, groupsByClass: {}, groups: [] }
  }
}

/**
 * 批量分配指定宿舍组到房间
 * @param {Array} groupIds - 要分配的宿舍组ID列表
 * @param {string} adminUsername - 管理员用户名
 * @param {string} strategy - 分配策略
 */
export function batchAssignRoomsToGroups(groupIds, adminUsername, strategy = 'class_priority') {
  if (!groupIds || groupIds.length === 0) {
    return { success: false, message: '请选择要分配的宿舍组', assignments: [] }
  }
  
  return autoAssignRooms(adminUsername, strategy, groupIds)
}

/**
 * 预览房间分配方案（不实际分配）
 * @param {string} adminUsername - 管理员用户名
 * @param {string} strategy - 分配策略
 * @param {Array} specificGroupIds - 可选，指定要分配的宿舍组ID列表
 */
export function previewRoomAssignment(adminUsername, strategy = 'class_priority', specificGroupIds = null) {
  try {
    const groups = getDormGroups()
    const availableRooms = getAvailableRooms(adminUsername)
    const records = getRoomSelectionRecords()
    
    // 找出未选择房间的满员宿舍组
    let unselectedGroups = groups.filter(g => 
      g.status === GroupStatus.COMPLETE && !g.roomId
    )
    
    if (specificGroupIds && specificGroupIds.length > 0) {
      unselectedGroups = unselectedGroups.filter(g => specificGroupIds.includes(g.id))
    }
    
    // 找出可用房间
    const freeRooms = availableRooms.filter(room => {
      const isSelected = records.some(r => 
        r.roomId === room.id && r.status !== SelectionStatus.CANCELLED
      )
      return !isSelected && room.status === RoomStatus.AVAILABLE
    })
    
    const preview = {
      canAssign: freeRooms.length >= unselectedGroups.length,
      totalGroups: unselectedGroups.length,
      availableRooms: freeRooms.length,
      strategy: strategy,
      assignments: []
    }
    
    if (!preview.canAssign) {
      preview.message = `房间不足：需要${unselectedGroups.length}间，可用${freeRooms.length}间`
      return preview
    }
    
    // 模拟分配过程
    const usedRooms = new Set()
    
    if (strategy === 'class_priority') {
      const groupsByClass = {}
      unselectedGroups.forEach(group => {
        const classKey = `${group.major}-${group.class || 'unknown'}`
        if (!groupsByClass[classKey]) {
          groupsByClass[classKey] = []
        }
        groupsByClass[classKey].push(group)
      })
      
      Object.entries(groupsByClass).forEach(([classKey, classGroups]) => {
        const suitableRooms = freeRooms.filter(room => 
          !usedRooms.has(room.id) &&
          (room.allowedMajor === classGroups[0].major || !room.allowedMajor) &&
          (room.allowedClass === classGroups[0].class || !room.allowedClass) &&
          (room.allowedGender === 'mixed' || room.allowedGender === classGroups[0].gender)
        )
        
        const roomsByFloor = {}
        suitableRooms.forEach(room => {
          if (!roomsByFloor[room.floor]) {
            roomsByFloor[room.floor] = []
          }
          roomsByFloor[room.floor].push(room)
        })
        
        const sortedFloors = Object.entries(roomsByFloor)
          .sort(([, roomsA], [, roomsB]) => roomsB.length - roomsA.length)
        
        let assignedCount = 0
        for (const [floor, floorRooms] of sortedFloors) {
          for (const group of classGroups) {
            if (assignedCount >= classGroups.length) break
            
            const availableRoom = floorRooms.find(room => !usedRooms.has(room.id))
            if (availableRoom) {
              preview.assignments.push({
                groupId: group.id,
                groupName: group.groupName,
                roomNumber: availableRoom.roomNumber,
                floor: availableRoom.floor,
                building: availableRoom.buildingName,
                major: group.major,
                class: group.class
              })
              usedRooms.add(availableRoom.id)
              assignedCount++
            }
          }
        }
      })
    }
    
    preview.message = `预计可为${preview.assignments.length}个宿舍组分配房间`
    return preview
    
  } catch (error) {
    console.error('预览房间分配失败:', error)
    return { canAssign: false, message: error.message, assignments: [] }
  }
}

/**
 * 导出房间选择数据
 */
export function exportRoomSelectionData(adminUsername) {
  const records = getRoomSelectionRecords()
  const statistics = getRoomSelectionStatistics()
  const occupancyStatus = getRoomOccupancyStatus(adminUsername)
  const unassignedGroups = getUnassignedGroups()
  
  return {
    exportTime: new Date().toISOString(),
    adminUsername,
    statistics,
    records,
    occupancyStatus,
    unassignedGroups
  }
}
