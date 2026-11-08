/**
 * 管理员处理剩余学生分配工具
 * 提供智能分配、手动分配等核心功能
 */

import { 
  getDormGroups, 
  saveDormGroups, 
  updateDormGroup, 
  getRemainingStudents, 
  saveRemainingStudents,
  removeRemainingStudent,
  addMemberToGroup,
  createDormGroup,
  GroupStatus,
  StudentStatus
} from './studentGrouping.js'
import { 
  getAvailableRooms, 
  selectRoom, 
  autoAssignRooms 
} from './roomSelection.js'

// 分配策略枚举
export const AllocationStrategy = {
  BY_CLASS: 'by_class',           // 按班级分配
  BY_MAJOR: 'by_major',           // 按专业分配
  BY_GENDER: 'by_gender',         // 按性别分配
  RANDOM: 'random',               // 随机分配
  INTELLIGENT: 'intelligent'      // 智能分配（综合考虑多个因素）
}

// 处理结果类型
export const ProcessingResultType = {
  SUCCESS: 'success',
  WARNING: 'warning',
  ERROR: 'error',
  INFO: 'info'
}

/**
 * 智能补充未满宿舍组
 * 为人数不足的宿舍组自动添加合适的学生
 */
export function autoFillIncompleteGroups(strategy = AllocationStrategy.INTELLIGENT) {
  try {
    const groups = getDormGroups()
    const remainingStudents = getRemainingStudents()
    
    // 找出未满员的宿舍组
    const incompleteGroups = groups.filter(g => 
      g.status === GroupStatus.INCOMPLETE && g.memberCount < g.maxMembers
    )
    
    if (incompleteGroups.length === 0) {
      return {
        success: true,
        message: '没有需要补充的未满宿舍组',
        results: []
      }
    }
    
    const results = []
    
    // 为每个未满宿舍组寻找合适的学生
    incompleteGroups.forEach(group => {
      const neededCount = group.maxMembers - group.memberCount
      const suitableStudents = findSuitableStudents(group, remainingStudents, strategy, neededCount)
      
      if (suitableStudents.length === 0) {
        results.push({
          type: ProcessingResultType.WARNING,
          groupName: group.groupName,
          message: `未找到合适的学生补充到宿舍组"${group.groupName}"`,
          studentsAdded: []
        })
        return
      }
      
      // 添加学生到宿舍组
      const addedStudents = []
      const studentsToAdd = suitableStudents.slice(0, neededCount)
      
      studentsToAdd.forEach(student => {
        try {
          addMemberToGroup(group.id, student)
          removeRemainingStudent(student.studentId)
          addedStudents.push(student)
        } catch (error) {
          console.error(`添加学生${student.name}到宿舍组失败:`, error)
        }
      })
      
      if (addedStudents.length > 0) {
        results.push({
          type: ProcessingResultType.SUCCESS,
          groupName: group.groupName,
          message: `成功为宿舍组"${group.groupName}"添加了${addedStudents.length}名学生`,
          studentsAdded: addedStudents.map(s => s.name)
        })
      }
    })
    
    return {
      success: true,
      message: `处理完成，成功补充了${results.filter(r => r.type === ProcessingResultType.SUCCESS).length}个宿舍组`,
      results
    }
    
  } catch (error) {
    console.error('智能补充未满宿舍组失败:', error)
    return {
      success: false,
      message: error.message,
      results: []
    }
  }
}

/**
 * 按班级自动分配剩余学生
 */
export function autoAssignByClass() {
  try {
    const remainingStudents = getRemainingStudents()
    
    if (remainingStudents.length === 0) {
      return {
        success: true,
        message: '没有需要分配的剩余学生',
        results: []
      }
    }
    
    // 按班级和性别分组
    const studentsByClassAndGender = {}
    
    remainingStudents.forEach(student => {
      const key = `${student.class}-${student.gender}`
      if (!studentsByClassAndGender[key]) {
        studentsByClassAndGender[key] = {
          class: student.class,
          gender: student.gender,
          major: student.major,
          students: []
        }
      }
      studentsByClassAndGender[key].students.push(student)
    })
    
    const results = []
    
    // 为每个班级性别组合创建宿舍组
    Object.values(studentsByClassAndGender).forEach(group => {
      const students = group.students
      const groupsNeeded = Math.ceil(students.length / 4)
      
      for (let i = 0; i < groupsNeeded; i++) {
        const groupStudents = students.slice(i * 4, (i + 1) * 4)
        
        if (groupStudents.length === 0) continue
        
        try {
          // 创建新宿舍组
          const newGroup = createDormGroup({
            groupName: `${group.class}-${group.gender}生宿舍${i + 1}`,
            leaderName: groupStudents[0].name,
            leaderId: groupStudents[0].studentId,
            major: group.major,
            class: group.class,
            gender: group.gender,
            members: groupStudents.map(student => ({
              ...student,
              role: student === groupStudents[0] ? 'leader' : 'member',
              joinTime: new Date().toISOString()
            }))
          })
          
          // 从剩余学生列表中移除
          groupStudents.forEach(student => {
            removeRemainingStudent(student.studentId)
          })
          
          results.push({
            type: ProcessingResultType.SUCCESS,
            groupName: newGroup.groupName,
            message: `成功创建宿舍组"${newGroup.groupName}"，包含${groupStudents.length}名学生`,
            studentsAssigned: groupStudents.map(s => s.name),
            groupId: newGroup.id
          })
          
        } catch (error) {
          results.push({
            type: ProcessingResultType.ERROR,
            groupName: `${group.class}-${group.gender}生宿舍${i + 1}`,
            message: `创建宿舍组失败: ${error.message}`,
            studentsAssigned: []
          })
        }
      }
    })
    
    return {
      success: true,
      message: `按班级分配完成，创建了${results.filter(r => r.type === ProcessingResultType.SUCCESS).length}个新宿舍组`,
      results
    }
    
  } catch (error) {
    console.error('按班级自动分配失败:', error)
    return {
      success: false,
      message: error.message,
      results: []
    }
  }
}

/**
 * 手动分配学生到指定宿舍组
 */
export function manualAssignStudent(studentId, targetGroupId) {
  try {
    const remainingStudents = getRemainingStudents()
    const student = remainingStudents.find(s => s.studentId === studentId)
    
    if (!student) {
      throw new Error('学生不在剩余学生列表中')
    }
    
    const groups = getDormGroups()
    const targetGroup = groups.find(g => g.id === targetGroupId)
    
    if (!targetGroup) {
      throw new Error('目标宿舍组不存在')
    }
    
    if (targetGroup.memberCount >= targetGroup.maxMembers) {
      throw new Error('目标宿舍组已满员')
    }
    
    // 验证性别匹配
    if (targetGroup.gender && student.gender !== targetGroup.gender) {
      throw new Error(`性别不匹配，该宿舍组只接受${targetGroup.gender}生`)
    }
    
    // 添加学生到宿舍组
    addMemberToGroup(targetGroupId, student)
    removeRemainingStudent(studentId)
    
    return {
      success: true,
      message: `成功将学生${student.name}分配到宿舍组"${targetGroup.groupName}"`,
      student: student.name,
      group: targetGroup.groupName
    }
    
  } catch (error) {
    console.error('手动分配学生失败:', error)
    return {
      success: false,
      message: error.message
    }
  }
}

/**
 * 为单独学生创建新宿舍组
 */
export function createNewGroupForStudent(studentId, groupName) {
  try {
    const remainingStudents = getRemainingStudents()
    const student = remainingStudents.find(s => s.studentId === studentId)
    
    if (!student) {
      throw new Error('学生不在剩余学生列表中')
    }
    
    // 创建新宿舍组
    const newGroup = createDormGroup({
      groupName: groupName || `${student.name}的宿舍`,
      leaderName: student.name,
      leaderId: student.studentId,
      major: student.major,
      class: student.class,
      gender: student.gender,
      members: [{
        ...student,
        role: 'leader',
        joinTime: new Date().toISOString()
      }]
    })
    
    // 从剩余学生列表中移除
    removeRemainingStudent(studentId)
    
    return {
      success: true,
      message: `成功为学生${student.name}创建新宿舍组"${newGroup.groupName}"`,
      student: student.name,
      group: newGroup.groupName,
      groupId: newGroup.id
    }
    
  } catch (error) {
    console.error('创建新宿舍组失败:', error)
    return {
      success: false,
      message: error.message
    }
  }
}

/**
 * 寻找适合加入宿舍组的学生
 */
function findSuitableStudents(group, availableStudents, strategy, maxCount) {
  let candidates = availableStudents.filter(student => {
    // 基本条件：性别匹配
    if (group.gender && student.gender !== group.gender) {
      return false
    }
    
    // 排除已分配的学生
    if (student.status === StudentStatus.IN_GROUP || student.status === StudentStatus.ASSIGNED) {
      return false
    }
    
    // 严格限制：只能在同班级内进行匹配
    if (group.class && student.class !== group.class) {
      return false
    }
    
    return true
  })
  
  // 根据策略排序
  switch (strategy) {
    case AllocationStrategy.BY_CLASS:
      candidates.sort((a, b) => {
        if (a.class === group.class && b.class !== group.class) return -1
        if (a.class !== group.class && b.class === group.class) return 1
        return 0
      })
      break
      
    case AllocationStrategy.BY_MAJOR:
      candidates.sort((a, b) => {
        if (a.major === group.major && b.major !== group.major) return -1
        if (a.major !== group.major && b.major === group.major) return 1
        return 0
      })
      break
      
    case AllocationStrategy.INTELLIGENT:
      candidates.sort((a, b) => {
        let scoreA = 0, scoreB = 0
        
        // 由于已经在筛选阶段限制了同班级，这里只需要按其他条件排序
        // 同专业优先
        if (a.major === group.major) scoreA += 5
        if (b.major === group.major) scoreB += 5
        
        // 同性别必须（已在筛选阶段保证）
        if (a.gender === group.gender) scoreA += 3
        if (b.gender === group.gender) scoreB += 3
        
        // 按学号排序作为最后的排序依据，保证结果稳定
        if (scoreA === scoreB) {
          return a.studentId.localeCompare(b.studentId)
        }
        
        return scoreB - scoreA
      })
      break
      
    case AllocationStrategy.RANDOM:
      // 随机打乱
      for (let i = candidates.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [candidates[i], candidates[j]] = [candidates[j], candidates[i]]
      }
      break
  }
  
  return candidates.slice(0, maxCount)
}

/**
 * 批量处理所有剩余分配问题
 */
export function batchProcessAllRemaining(adminUsername) {
  try {
    const results = {
      incompleteGroupsProcessed: 0,
      newGroupsCreated: 0,
      studentsAssigned: 0,
      roomsAssigned: 0,
      errors: [],
      details: []
    }
    
    // 1. 智能补充未满宿舍组
    const fillResult = autoFillIncompleteGroups(AllocationStrategy.INTELLIGENT)
    if (fillResult.success) {
      results.incompleteGroupsProcessed = fillResult.results.filter(r => r.type === ProcessingResultType.SUCCESS).length
      results.details.push(...fillResult.results)
    } else {
      results.errors.push(`补充未满宿舍组失败: ${fillResult.message}`)
    }
    
    // 2. 按班级分配剩余学生
    const assignResult = autoAssignByClass()
    if (assignResult.success) {
      results.newGroupsCreated = assignResult.results.filter(r => r.type === ProcessingResultType.SUCCESS).length
      results.studentsAssigned = assignResult.results.reduce((sum, r) => 
        sum + (r.studentsAssigned ? r.studentsAssigned.length : 0), 0
      )
      results.details.push(...assignResult.results)
    } else {
      results.errors.push(`按班级分配失败: ${assignResult.message}`)
    }
    
    // 3. 自动分配房间给新创建的满员宿舍组
    const roomAssignResult = autoAssignRooms(adminUsername, 'major_match')
    if (roomAssignResult.success) {
      results.roomsAssigned = roomAssignResult.assignments.filter(a => a.success).length
      results.details.push({
        type: ProcessingResultType.INFO,
        message: roomAssignResult.message,
        assignments: roomAssignResult.assignments
      })
    } else {
      results.errors.push(`自动分配房间失败: ${roomAssignResult.message}`)
    }
    
    return {
      success: results.errors.length === 0,
      message: `批量处理完成：补充${results.incompleteGroupsProcessed}个宿舍组，创建${results.newGroupsCreated}个新宿舍组，分配${results.studentsAssigned}名学生，分配${results.roomsAssigned}个房间`,
      results
    }
    
  } catch (error) {
    console.error('批量处理失败:', error)
    return {
      success: false,
      message: error.message,
      results: { errors: [error.message] }
    }
  }
}

/**
 * 验证分配结果
 */
export function validateAllocationResults() {
  try {
    const groups = getDormGroups()
    const remainingStudents = getRemainingStudents()
    
    const issues = []
    const warnings = []
    
    // 检查宿舍组
    groups.forEach(group => {
      // 检查成员数量
      if (group.memberCount < 1) {
        issues.push(`宿舍组"${group.groupName}"没有成员`)
      } else if (group.memberCount < 4 && group.status === GroupStatus.COMPLETE) {
        warnings.push(`宿舍组"${group.groupName}"标记为满员但只有${group.memberCount}人`)
      }
      
      // 检查房间分配
      if (group.status === GroupStatus.COMPLETE && !group.roomId) {
        warnings.push(`满员宿舍组"${group.groupName}"尚未分配房间`)
      }
      
      // 检查性别一致性
      if (group.members && group.members.length > 1) {
        const genders = [...new Set(group.members.map(m => m.gender))]
        if (genders.length > 1) {
          issues.push(`宿舍组"${group.groupName}"存在性别混合`)
        }
      }
    })
    
    // 统计信息
    const stats = {
      totalGroups: groups.length,
      completeGroups: groups.filter(g => g.status === GroupStatus.COMPLETE).length,
      incompleteGroups: groups.filter(g => g.status === GroupStatus.INCOMPLETE).length,
      roomAssignedGroups: groups.filter(g => g.roomId).length,
      remainingStudentsCount: remainingStudents.length,
      totalStudents: groups.reduce((sum, g) => sum + g.memberCount, 0) + remainingStudents.length
    }
    
    return {
      isValid: issues.length === 0,
      issues,
      warnings,
      stats,
      completionRate: stats.totalStudents > 0 ? 
        Math.round((stats.totalStudents - remainingStudents.length) / stats.totalStudents * 100) : 0
    }
    
  } catch (error) {
    console.error('验证分配结果失败:', error)
    return {
      isValid: false,
      issues: [error.message],
      warnings: [],
      stats: {},
      completionRate: 0
    }
  }
}

/**
 * 获取分配处理建议
 */
export function getProcessingSuggestions() {
  try {
    const groups = getDormGroups()
    const remainingStudents = getRemainingStudents()
    const suggestions = []
    
    // 分析未满宿舍组
    const incompleteGroups = groups.filter(g => g.status === GroupStatus.INCOMPLETE)
    if (incompleteGroups.length > 0) {
      suggestions.push({
        type: 'action',
        priority: 'high',
        title: '补充未满宿舍组',
        description: `有${incompleteGroups.length}个宿舍组人数不足，建议使用智能补充功能`,
        action: 'autoFillIncompleteGroups'
      })
    }
    
    // 分析剩余学生
    if (remainingStudents.length > 0) {
      const studentsByClass = {}
      remainingStudents.forEach(student => {
        const key = student.class
        if (!studentsByClass[key]) {
          studentsByClass[key] = { '男': 0, '女': 0 }
        }
        studentsByClass[key][student.gender === '男' ? '男' : '女']++
      })
      
      suggestions.push({
        type: 'action',
        priority: 'high',
        title: '分配剩余学生',
        description: `有${remainingStudents.length}名学生未分配，涉及${Object.keys(studentsByClass).length}个班级`,
        action: 'autoAssignByClass',
        details: studentsByClass
      })
    }
    
    // 分析房间分配
    const unassignedRooms = groups.filter(g => g.status === GroupStatus.COMPLETE && !g.roomId)
    if (unassignedRooms.length > 0) {
      suggestions.push({
        type: 'action',
        priority: 'medium',
        title: '分配房间',
        description: `有${unassignedRooms.length}个满员宿舍组尚未分配房间`,
        action: 'autoAssignRooms'
      })
    }
    
    return suggestions
    
  } catch (error) {
    console.error('获取处理建议失败:', error)
    return []
  }
}

/**
 * 导出处理结果报告
 */
export function exportProcessingReport() {
  try {
    const validation = validateAllocationResults()
    const suggestions = getProcessingSuggestions()
    const groups = getDormGroups()
    const remainingStudents = getRemainingStudents()
    
    return {
      exportTime: new Date().toISOString(),
      validation,
      suggestions,
      summary: {
        totalGroups: groups.length,
        totalStudents: validation.stats.totalStudents,
        completionRate: validation.completionRate,
        remainingIssues: remainingStudents.length + validation.issues.length
      },
      groups,
      remainingStudents
    }
    
  } catch (error) {
    console.error('导出处理报告失败:', error)
    return null
  }
}
