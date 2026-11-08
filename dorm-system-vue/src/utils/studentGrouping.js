/**
 * 学生自组宿舍管理工具
 * 提供学生分组的核心逻辑和数据管理功能
 */

// 存储键名常量
const STORAGE_KEYS = {
  DORM_GROUPS: 'dorm_groups',
  GROUPING_PHASE: 'grouping_phase',
  GROUPING_TIME_SETTINGS: 'grouping_time_settings',
  REMAINING_STUDENTS: 'remaining_students',
  ROOM_SELECTION_RECORDS: 'room_selection_records'
}

// 宿舍组状态枚举
export const GroupStatus = {
  INCOMPLETE: 'incomplete',  // 未满员
  COMPLETE: 'complete',      // 已满员
  ROOM_SELECTED: 'room_selected'  // 已选择房间
}

// 学生状态枚举
export const StudentStatus = {
  UNGROUPED: 'ungrouped',    // 未分组
  IN_GROUP: 'in_group',      // 已加入宿舍组
  ASSIGNED: 'assigned'       // 已分配房间
}

/**
 * 获取所有宿舍组
 */
export function getDormGroups() {
  try {
    const groups = localStorage.getItem(STORAGE_KEYS.DORM_GROUPS)
    return groups ? JSON.parse(groups) : []
  } catch (error) {
    console.error('获取宿舍组数据失败:', error)
    return []
  }
}

/**
 * 保存宿舍组数据
 */
export function saveDormGroups(groups) {
  try {
    localStorage.setItem(STORAGE_KEYS.DORM_GROUPS, JSON.stringify(groups))
    return true
  } catch (error) {
    console.error('保存宿舍组数据失败:', error)
    return false
  }
}

/**
 * 创建新的宿舍组
 */
export function createDormGroup(groupData) {
  const groups = getDormGroups()
  
  // 生成唯一ID
  const newId = Date.now().toString()
  
  // 根据房间类型确定最大成员数，默认为4人间
  const maxMembers = groupData.maxMembers || groupData.roomCapacity || 4
  
  const newGroup = {
    id: newId,
    groupName: groupData.groupName,
    leaderName: groupData.leaderName,
    leaderId: groupData.leaderId,
    memberCount: groupData.members ? groupData.members.length : 1,
    major: groupData.major,
    class: groupData.class,
    gender: groupData.gender, // 宿舍性别限制
    createTime: new Date().toISOString(),
    status: groupData.members && groupData.members.length >= maxMembers ? GroupStatus.COMPLETE : GroupStatus.INCOMPLETE,
    members: groupData.members || [],
    roomId: null, // 选择的房间ID
    buildingId: null, // 所在楼栋ID
    maxMembers: maxMembers, // 最大成员数，支持2人、4人、6人、8人间
    roomType: groupData.roomType || getRoomTypeByCapacity(maxMembers) // 房间类型
  }
  
  groups.push(newGroup)
  saveDormGroups(groups)
  
  return newGroup
}

/**
 * 更新宿舍组信息
 */
export function updateDormGroup(groupId, updateData) {
  const groups = getDormGroups()
  const groupIndex = groups.findIndex(g => g.id === groupId)
  
  if (groupIndex === -1) {
    throw new Error(`未找到ID为 ${groupId} 的宿舍组`)
  }
  
  // 更新数据
  Object.assign(groups[groupIndex], updateData)
  
  // 更新状态
  if (groups[groupIndex].members) {
    groups[groupIndex].memberCount = groups[groupIndex].members.length
    groups[groupIndex].status = groups[groupIndex].memberCount >= 4 ? GroupStatus.COMPLETE : GroupStatus.INCOMPLETE
  }
  
  saveDormGroups(groups)
  return groups[groupIndex]
}

/**
 * 删除宿舍组
 */
export function deleteDormGroup(groupId) {
  const groups = getDormGroups()
  const groupIndex = groups.findIndex(g => g.id === groupId)
  
  if (groupIndex === -1) {
    throw new Error(`未找到ID为 ${groupId} 的宿舍组`)
  }
  
  const deletedGroup = groups.splice(groupIndex, 1)[0]
  saveDormGroups(groups)
  
  // 将组员变为单独学生
  if (deletedGroup.members && deletedGroup.members.length > 0) {
    addRemainingStudents(deletedGroup.members.map(member => ({
      ...member,
      status: StudentStatus.UNGROUPED,
      previousGroup: deletedGroup.groupName
    })))
  }
  
  return deletedGroup
}

/**
 * 向宿舍组添加成员
 */
export function addMemberToGroup(groupId, studentData) {
  const groups = getDormGroups()
  const group = groups.find(g => g.id === groupId)
  
  if (!group) {
    throw new Error(`未找到ID为 ${groupId} 的宿舍组`)
  }
  
  if (group.memberCount >= group.maxMembers) {
    throw new Error('宿舍组已满员，无法添加更多成员')
  }
  
  // 检查性别是否匹配
  if (group.gender && studentData.gender !== group.gender) {
    throw new Error(`性别不匹配，该宿舍组只接受${group.gender}生`)
  }
  
  // 检查专业是否匹配（可选限制）
  if (group.major && studentData.major !== group.major) {
    console.warn('专业不匹配，建议同专业学生组建宿舍')
  }
  
  // 添加成员
  if (!group.members) {
    group.members = []
  }
  
  const newMember = {
    studentId: studentData.studentId,
    name: studentData.name,
    gender: studentData.gender,
    major: studentData.major,
    class: studentData.class,
    phone: studentData.phone,
    role: 'member',
    joinTime: new Date().toISOString()
  }
  
  group.members.push(newMember)
  
  // 更新宿舍组状态
  return updateDormGroup(groupId, { members: group.members })
}

/**
 * 从宿舍组移除成员
 */
export function removeMemberFromGroup(groupId, studentId) {
  const groups = getDormGroups()
  const group = groups.find(g => g.id === groupId)
  
  if (!group) {
    throw new Error(`未找到ID为 ${groupId} 的宿舍组`)
  }
  
  if (!group.members) {
    throw new Error('宿舍组没有成员')
  }
  
  const memberIndex = group.members.findIndex(m => m.studentId === studentId)
  if (memberIndex === -1) {
    throw new Error(`未找到学号为 ${studentId} 的成员`)
  }
  
  const removedMember = group.members.splice(memberIndex, 1)[0]
  
  // 如果移除的是组长，需要重新指定组长
  if (group.leaderId === studentId && group.members.length > 0) {
    const newLeader = group.members[0]
    newLeader.role = 'leader'
    group.leaderId = newLeader.studentId
    group.leaderName = newLeader.name
  }
  
  // 如果组员全部离开，删除宿舍组
  if (group.members.length === 0) {
    deleteDormGroup(groupId)
    return null
  }
  
  // 更新宿舍组
  updateDormGroup(groupId, { 
    members: group.members,
    leaderId: group.leaderId,
    leaderName: group.leaderName
  })
  
  // 将移除的成员添加到剩余学生列表
  addRemainingStudents([{
    ...removedMember,
    status: StudentStatus.UNGROUPED,
    leftGroup: group.groupName,
    leftTime: new Date().toISOString()
  }])
  
  return removedMember
}

/**
 * 更换宿舍组组长
 */
export function changeGroupLeader(groupId, newLeaderId) {
  const groups = getDormGroups()
  const group = groups.find(g => g.id === groupId)
  
  if (!group || !group.members) {
    throw new Error('宿舍组不存在或没有成员')
  }
  
  const newLeader = group.members.find(m => m.studentId === newLeaderId)
  if (!newLeader) {
    throw new Error('新组长不在宿舍组成员中')
  }
  
  // 更新角色
  group.members.forEach(member => {
    member.role = member.studentId === newLeaderId ? 'leader' : 'member'
  })
  
  // 更新宿舍组信息
  return updateDormGroup(groupId, {
    leaderId: newLeaderId,
    leaderName: newLeader.name,
    members: group.members
  })
}

/**
 * 获取剩余学生（未分组学生）
 */
export function getRemainingStudents() {
  try {
    const students = localStorage.getItem(STORAGE_KEYS.REMAINING_STUDENTS)
    let result = students ? JSON.parse(students) : []
    
    // 如果没有数据或数据太少，添加测试数据来验证滚动功能
    if (result.length < 15) {
      const testStudents = []
      for (let i = result.length; i < 25; i++) {
        testStudents.push({
          studentId: `TEST${String(i + 1).padStart(3, '0')}`,
          name: `测试学生${i + 1}`,
          gender: i % 2 === 0 ? '男' : '女',
          major: ['计算机科学', '软件工程', '信息安全'][i % 3],
          class: `${Math.floor(i / 5) + 1}班`,
          phone: `138${String(i).padStart(8, '0')}`,
          email: `test${i + 1}@example.com`,
          status: StudentStatus.UNGROUPED,
          addTime: new Date().toISOString()
        })
      }
      result = [...result, ...testStudents]
    }
    
    return result
  } catch (error) {
    console.error('获取剩余学生数据失败:', error)
    return []
  }
}

/**
 * 保存剩余学生数据
 */
export function saveRemainingStudents(students) {
  try {
    localStorage.setItem(STORAGE_KEYS.REMAINING_STUDENTS, JSON.stringify(students))
    return true
  } catch (error) {
    console.error('保存剩余学生数据失败:', error)
    return false
  }
}

/**
 * 添加剩余学生
 */
export function addRemainingStudents(students) {
  const existingStudents = getRemainingStudents()
  const newStudents = Array.isArray(students) ? students : [students]
  
  // 去重处理
  newStudents.forEach(student => {
    const existingIndex = existingStudents.findIndex(s => s.studentId === student.studentId)
    if (existingIndex === -1) {
      existingStudents.push({
        ...student,
        status: StudentStatus.UNGROUPED,
        addTime: new Date().toISOString()
      })
    } else {
      // 更新现有学生信息
      Object.assign(existingStudents[existingIndex], student)
    }
  })
  
  saveRemainingStudents(existingStudents)
  return existingStudents
}

/**
 * 移除剩余学生（已分配到宿舍组）
 */
export function removeRemainingStudent(studentId) {
  const students = getRemainingStudents()
  const studentIndex = students.findIndex(s => s.studentId === studentId)
  
  if (studentIndex !== -1) {
    const removedStudent = students.splice(studentIndex, 1)[0]
    saveRemainingStudents(students)
    return removedStudent
  }
  
  return null
}

/**
 * 获取宿舍组统计信息
 */
export function getGroupingStatistics() {
  const groups = getDormGroups()
  const remainingStudents = getRemainingStudents()
  
  const completedGroups = groups.filter(g => g.status === GroupStatus.COMPLETE).length
  const incompleteGroups = groups.filter(g => g.status === GroupStatus.INCOMPLETE).length
  const roomSelectedGroups = groups.filter(g => g.roomId).length
  
  const totalStudentsInGroups = groups.reduce((sum, group) => sum + group.memberCount, 0)
  const singleStudents = remainingStudents.length
  const totalStudents = totalStudentsInGroups + singleStudents
  
  return {
    completedGroups,
    incompleteGroups,
    roomSelectedGroups,
    singleStudents,
    totalStudents,
    totalGroups: groups.length,
    overallProgress: totalStudents > 0 ? Math.round((completedGroups * 4) / totalStudents * 100) : 0,
    roomSelectionProgress: completedGroups > 0 ? Math.round(roomSelectedGroups / completedGroups * 100) : 0
  }
}

/**
 * 按专业和班级分组获取学生
 */
export function getStudentsByMajorAndClass() {
  const groups = getDormGroups()
  const remainingStudents = getRemainingStudents()
  
  const result = {}
  
  // 处理已分组学生
  groups.forEach(group => {
    if (group.members) {
      group.members.forEach(member => {
        const key = `${member.major}-${member.class}`
        if (!result[key]) {
          result[key] = {
            major: member.major,
            class: member.class,
            groupedStudents: [],
            ungroupedStudents: []
          }
        }
        result[key].groupedStudents.push({
          ...member,
          groupName: group.groupName,
          groupId: group.id
        })
      })
    }
  })
  
  // 处理未分组学生
  remainingStudents.forEach(student => {
    const key = `${student.major}-${student.class}`
    if (!result[key]) {
      result[key] = {
        major: student.major,
        class: student.class,
        groupedStudents: [],
        ungroupedStudents: []
      }
    }
    result[key].ungroupedStudents.push(student)
  })
  
  return result
}

/**
 * 智能推荐宿舍组成员
 * 基于专业、班级、性别等条件推荐合适的学生
 */
export function recommendGroupMembers(groupId, limit = 5) {
  const groups = getDormGroups()
  const group = groups.find(g => g.id === groupId)
  const remainingStudents = getRemainingStudents()
  
  if (!group) {
    return []
  }
  
  // 筛选条件
  const recommendations = remainingStudents.filter(student => {
    // 性别匹配
    if (group.gender && student.gender !== group.gender) {
      return false
    }
    
    // 已在其他组的学生不推荐
    if (student.status === StudentStatus.IN_GROUP) {
      return false
    }
    
    return true
  })
  
  // 按匹配度排序
  recommendations.sort((a, b) => {
    let scoreA = 0, scoreB = 0
    
    // 同专业加分
    if (a.major === group.major) scoreA += 3
    if (b.major === group.major) scoreB += 3
    
    // 同班级加分
    if (a.class === group.class) scoreA += 2
    if (b.class === group.class) scoreB += 2
    
    // 同性别加分
    if (a.gender === group.gender) scoreA += 1
    if (b.gender === group.gender) scoreB += 1
    
    return scoreB - scoreA
  })
  
  return recommendations.slice(0, limit)
}

/**
 * 验证宿舍组数据完整性
 */
export function validateGroupingData() {
  const groups = getDormGroups()
  const issues = []
  
  groups.forEach(group => {
    // 检查基本信息
    if (!group.groupName || !group.leaderId) {
      issues.push(`宿舍组 ${group.id} 缺少基本信息`)
    }
    
    // 检查成员数量一致性
    if (group.members && group.memberCount !== group.members.length) {
      issues.push(`宿舍组 ${group.groupName} 成员数量不一致`)
    }
    
    // 检查组长是否在成员中
    if (group.members && group.leaderId) {
      const leaderExists = group.members.some(m => m.studentId === group.leaderId)
      if (!leaderExists) {
        issues.push(`宿舍组 ${group.groupName} 组长不在成员列表中`)
      }
    }
    
    // 检查性别一致性
    if (group.members && group.members.length > 1) {
      const genders = [...new Set(group.members.map(m => m.gender))]
      if (genders.length > 1) {
        issues.push(`宿舍组 ${group.groupName} 存在性别混合`)
      }
    }
  })
  
  return {
    isValid: issues.length === 0,
    issues
  }
}

/**
 * 清空所有分组数据（重置功能）
 */
export function clearAllGroupingData() {
  localStorage.removeItem(STORAGE_KEYS.DORM_GROUPS)
  localStorage.removeItem(STORAGE_KEYS.REMAINING_STUDENTS)
  localStorage.removeItem(STORAGE_KEYS.ROOM_SELECTION_RECORDS)
  return true
}

/**
 * 导出分组数据
 */
export function exportGroupingData() {
  const groups = getDormGroups()
  const remainingStudents = getRemainingStudents()
  const statistics = getGroupingStatistics()
  
  return {
    exportTime: new Date().toISOString(),
    statistics,
    groups,
    remainingStudents,
    validation: validateGroupingData()
  }
}

/**
 * 根据容量获取房间类型
 */
export function getRoomTypeByCapacity(capacity) {
  switch (capacity) {
    case 2:
      return '2人间'
    case 4:
      return '4人间'
    case 6:
      return '6人间'
    case 8:
      return '8人间'
    default:
      return '4人间'
  }
}

/**
 * 获取支持的房间容量选项
 */
export function getRoomCapacityOptions() {
  return [
    { value: 2, label: '2人间', description: '双人宿舍' },
    { value: 4, label: '4人间', description: '标准宿舍' },
    { value: 6, label: '6人间', description: '大型宿舍' },
    { value: 8, label: '8人间', description: '集体宿舍' }
  ]
}
