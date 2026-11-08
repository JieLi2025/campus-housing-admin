/**
 * 智能宿舍分配引擎
 * 实现专业优先级计算、自动分配和分配质量评估
 * 
 * 【状态】: 预留算法，暂未集成到主流程
 * 【用途】: 提供智能分配算法，可用于后续功能扩展
 * 【价值】: 包含成熟的分配算法逻辑，建议保留备用
 * 【最后更新】: 2025-10-20
 */

/**
 * 专业优先级计算策略
 */
export const PRIORITY_STRATEGIES = {
  STUDENT_COUNT: 'student_count',   // 按学生人数
  CUSTOM: 'custom'                  // 自定义排序
}

/**
 * 智能宿舍分配器
 */
export class SmartDormAllocator {
  constructor(floor, students, config = {}) {
    this.floor = floor
    this.students = students || []
    this.config = {
      priorityStrategy: PRIORITY_STRATEGIES.STUDENT_COUNT,
      bedsPerRoom: 4, // 默认每间宿舍4个床位
      allowManualOverride: true,
      genderSeparation: true,
      ...config
    }
    
    // 分析上下文数据
    this.context = this.analyzeContext()
  }

  /**
   * 分析分配上下文
   */
  analyzeContext() {
    // 按专业分组学生
    const studentsByMajor = this.groupStudentsByMajor()
    
    // 计算每个专业需要的房间数
    const roomRequirements = this.calculateRoomRequirements(studentsByMajor)
    
    // 分析可用房间
    const availableRooms = this.analyzeAvailableRooms()
    
    // 找到连续房间段
    const continuousSegments = this.findContinuousRoomSegments(availableRooms)
    
    return {
      studentsByMajor,
      roomRequirements,
      availableRooms,
      continuousSegments,
      totalStudents: this.students.length,
      totalAvailableBeds: availableRooms.reduce((sum, room) => sum + room.availableBeds, 0)
    }
  }

  /**
   * 按专业分组学生
   */
  groupStudentsByMajor() {
    const groups = {}
    
    this.students.forEach(student => {
      const major = student.major || '未分配专业'
      if (!groups[major]) {
        groups[major] = []
      }
      groups[major].push(student)
    })
    
    return groups
  }

  /**
   * 计算每个专业需要的房间数
   */
  calculateRoomRequirements(studentsByMajor) {
    const requirements = {}
    
    Object.keys(studentsByMajor).forEach(major => {
      const students = studentsByMajor[major]
      const maleStudents = students.filter(s => s.gender === '男').length
      const femaleStudents = students.filter(s => s.gender === '女').length
      
      requirements[major] = {
        total: students.length,
        male: maleStudents,
        female: femaleStudents,
        maleRooms: Math.ceil(maleStudents / this.config.bedsPerRoom),
        femaleRooms: Math.ceil(femaleStudents / this.config.bedsPerRoom),
        totalRooms: Math.ceil(maleStudents / this.config.bedsPerRoom) + 
                   Math.ceil(femaleStudents / this.config.bedsPerRoom)
      }
    })
    
    return requirements
  }

  /**
   * 分析可用房间
   */
  analyzeAvailableRooms() {
    // 这里应该从实际的房间数据获取，暂时用模拟数据
    const rooms = []
    
    // 模拟3层楼，每层20个房间
    for (let roomNum = 1; roomNum <= 20; roomNum++) {
      const roomId = `${this.floor}0${roomNum.toString().padStart(2, '0')}`
      rooms.push({
        id: roomId,
        roomNumber: roomId,
        totalBeds: this.config.bedsPerRoom,
        occupiedBeds: 0,
        availableBeds: this.config.bedsPerRoom,
        gender: null, // 可分配给任何性别
        status: 'available',
        position: { x: roomNum, y: 1 } // 假设都在同一行
      })
    }
    
    return rooms.filter(room => room.status === 'available' && room.availableBeds > 0)
  }

  /**
   * 找到连续房间段
   */
  findContinuousRoomSegments(availableRooms) {
    const sortedRooms = availableRooms.sort((a, b) => 
      parseInt(a.roomNumber.slice(-2)) - parseInt(b.roomNumber.slice(-2))
    )
    
    const segments = []
    let currentSegment = []
    
    sortedRooms.forEach((room, index) => {
      if (currentSegment.length === 0) {
        currentSegment = [room]
      } else {
        const lastRoom = currentSegment[currentSegment.length - 1]
        const lastRoomNum = parseInt(lastRoom.roomNumber.slice(-2))
        const currentRoomNum = parseInt(room.roomNumber.slice(-2))
        
        if (currentRoomNum === lastRoomNum + 1) {
          // 连续房间
          currentSegment.push(room)
        } else {
          // 不连续，保存当前段，开始新段
          if (currentSegment.length > 0) {
            segments.push([...currentSegment])
          }
          currentSegment = [room]
        }
      }
      
      // 最后一个房间
      if (index === sortedRooms.length - 1 && currentSegment.length > 0) {
        segments.push([...currentSegment])
      }
    })
    
    return segments
  }

  /**
   * 确定专业优先级
   */
  determineMajorPriorities() {
    const majors = Object.keys(this.context.studentsByMajor)
    
    switch (this.config.priorityStrategy) {
      case PRIORITY_STRATEGIES.STUDENT_COUNT:
        return this.priorityByStudentCount(majors)
      case PRIORITY_STRATEGIES.CUSTOM:
        return this.config.customPriorities || this.priorityByStudentCount(majors)
      default:
        return this.priorityByStudentCount(majors)
    }
  }


  /**
   * 按学生人数排序
   */
  priorityByStudentCount(majors) {
    return majors.map(major => ({
      major,
      studentCount: this.context.studentsByMajor[major].length,
      requiredRooms: this.context.roomRequirements[major].totalRooms
    })).sort((a, b) => b.studentCount - a.studentCount)
  }


  /**
   * 生成分配预览
   */
  generateAllocationPreview(majorPriorities) {
    const preview = {
      majorAllocations: [],
      totalRoomsNeeded: 0,
      totalRoomsAvailable: this.context.availableRooms.length,
      feasible: true,
      warnings: []
    }
    
    let usedRooms = 0
    const allocationResults = []
    
    majorPriorities.forEach((priority, index) => {
      const major = priority.major
      const requirement = this.context.roomRequirements[major]
      const roomsNeeded = requirement.totalRooms
      
      const allocation = {
        major,
        priority: index + 1,
        studentCount: priority.studentCount,
        roomsNeeded,
        estimatedRooms: this.estimateRoomAllocation(usedRooms, roomsNeeded)
      }
      
      allocationResults.push(allocation)
      usedRooms += roomsNeeded
    })
    
    preview.majorAllocations = allocationResults
    preview.totalRoomsNeeded = usedRooms
    preview.feasible = usedRooms <= preview.totalRoomsAvailable
    
    // 生成警告
    if (!preview.feasible) {
      preview.warnings.push(`房间不足：需要${usedRooms}间，可用${preview.totalRoomsAvailable}间`)
    }
    
    return preview
  }

  /**
   * 估算房间分配
   */
  estimateRoomAllocation(startIndex, roomCount) {
    const availableRooms = this.context.availableRooms
    const endIndex = Math.min(startIndex + roomCount, availableRooms.length)
    
    return availableRooms.slice(startIndex, endIndex).map(room => room.roomNumber)
  }



  /**
   * 执行分配
   */
  executeAllocation(majorPriorities) {
    const allocationResult = {
      success: false,
      allocations: [],
      conflicts: [],
      summary: {}
    }
    
    try {
      let currentRoomIndex = 0
      const finalAllocations = []
      
      for (const priority of majorPriorities) {
        const major = priority.major
        const students = this.context.studentsByMajor[major]
        const requirement = this.context.roomRequirements[major]
        
        const majorAllocation = this.allocateMajorToRooms(
          major, 
          students, 
          requirement, 
          currentRoomIndex
        )
        
        finalAllocations.push(majorAllocation)
        currentRoomIndex += requirement.totalRooms
      }
      
      allocationResult.success = true
      allocationResult.allocations = finalAllocations
      allocationResult.summary = this.generateAllocationSummary(finalAllocations)
      
    } catch (error) {
      allocationResult.success = false
      allocationResult.conflicts.push({
        type: 'execution_error',
        message: error.message
      })
    }
    
    return allocationResult
  }

  /**
   * 为专业分配房间
   */
  allocateMajorToRooms(major, students, requirement, startRoomIndex) {
    const availableRooms = this.context.availableRooms
    const assignedRooms = []
    
    // 按性别分组
    const maleStudents = students.filter(s => s.gender === '男')
    const femaleStudents = students.filter(s => s.gender === '女')
    
    let currentIndex = startRoomIndex
    
    // 先分配男生
    if (maleStudents.length > 0) {
      const maleRooms = this.assignStudentsToRooms(maleStudents, availableRooms, currentIndex, '男')
      assignedRooms.push(...maleRooms)
      currentIndex += maleRooms.length
    }
    
    // 再分配女生
    if (femaleStudents.length > 0) {
      const femaleRooms = this.assignStudentsToRooms(femaleStudents, availableRooms, currentIndex, '女')
      assignedRooms.push(...femaleRooms)
    }
    
    return {
      major,
      totalStudents: students.length,
      assignedRooms,
      roomCount: assignedRooms.length,
      continuityScore: this.calculateContinuityScore(assignedRooms.map(r => r.roomNumber))
    }
  }

  /**
   * 分配学生到房间
   */
  assignStudentsToRooms(students, availableRooms, startIndex, gender) {
    const roomsNeeded = Math.ceil(students.length / this.config.bedsPerRoom)
    const assignedRooms = []
    
    for (let i = 0; i < roomsNeeded && (startIndex + i) < availableRooms.length; i++) {
      const room = availableRooms[startIndex + i]
      const studentsForRoom = students.slice(i * this.config.bedsPerRoom, (i + 1) * this.config.bedsPerRoom)
      
      assignedRooms.push({
        ...room,
        gender,
        assignedStudents: studentsForRoom,
        occupiedBeds: studentsForRoom.length
      })
    }
    
    return assignedRooms
  }

  /**
   * 生成分配总结
   */
  generateAllocationSummary(allocations) {
    const totalStudents = allocations.reduce((sum, alloc) => sum + alloc.totalStudents, 0)
    const totalRooms = allocations.reduce((sum, alloc) => sum + alloc.roomCount, 0)
    const avgContinuity = allocations.reduce((sum, alloc) => sum + alloc.continuityScore, 0) / allocations.length
    
    return {
      totalMajors: allocations.length,
      totalStudents,
      totalRooms,
      avgContinuityScore: Math.round(avgContinuity),
      roomUtilizationRate: Math.round((totalRooms / this.context.availableRooms.length) * 100)
    }
  }
}

/**
 * 分配质量评估器
 */
export class AllocationQualityEvaluator {
  static evaluate(allocation) {
    const metrics = {
      majorContinuity: this.calculateMajorContinuity(allocation),
      classCohesion: this.calculateClassCohesion(allocation),
      roomUtilization: this.calculateRoomUtilization(allocation),
      genderSeparation: this.calculateGenderSeparation(allocation),
      overallScore: 0
    }
    
    // 计算综合评分
    metrics.overallScore = (
      metrics.majorContinuity * 0.4 +
      metrics.classCohesion * 0.3 +
      metrics.roomUtilization * 0.2 +
      metrics.genderSeparation * 0.1
    )
    
    return metrics
  }
  
  static calculateMajorContinuity(allocation) {
    // 计算专业连续性评分
    return 85 // 暂时返回固定值
  }
  
  static calculateClassCohesion(allocation) {
    // 计算班级凝聚度评分
    return 78 // 暂时返回固定值
  }
  
  static calculateRoomUtilization(allocation) {
    // 计算房间利用率评分
    return 92 // 暂时返回固定值
  }
  
  static calculateGenderSeparation(allocation) {
    // 计算性别分离评分
    return 100 // 暂时返回固定值
  }
}

/**
 * 分配冲突检测器
 */
export class AllocationConflictDetector {
  static detectConflicts(allocationDraft) {
    const conflicts = []
    
    // 检测容量超限
    conflicts.push(...this.checkCapacityOverflow(allocationDraft))
    
    // 检测性别混合
    conflicts.push(...this.checkGenderMixing(allocationDraft))
    
    // 检测专业分散
    conflicts.push(...this.checkMajorFragmentation(allocationDraft))
    
    return conflicts.map(conflict => ({
      ...conflict,
      suggestions: this.generateSuggestions(conflict)
    }))
  }
  
  static checkCapacityOverflow(allocationDraft) {
    // 检测容量超限的逻辑
    return []
  }
  
  static checkGenderMixing(allocationDraft) {
    // 检测性别混合的逻辑
    return []
  }
  
  static checkMajorFragmentation(allocationDraft) {
    // 检测专业分散的逻辑
    return []
  }
  
  static generateSuggestions(conflict) {
    // 生成解决建议
    return []
  }
}
