// 访客预约数据管理工具
const STORAGE_KEY = 'visitorAppointments'

// 获取所有访客预约数据
export function getVisitorAppointments() {
  const data = localStorage.getItem(STORAGE_KEY)
  return data ? JSON.parse(data) : []
}

// 添加访客预约
export function addVisitorAppointment(appointment) {
  const appointments = getVisitorAppointments()
  const newAppointment = {
    id: Date.now(),
    ...appointment,
    status: '待审核',
    createTime: new Date().toLocaleString(),
    checkInTime: null,
    checkOutTime: null
  }
  appointments.push(newAppointment)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(appointments))
  return newAppointment
}

// 更新访客预约状态
export function updateVisitorAppointmentStatus(id, status) {
  const appointments = getVisitorAppointments()
  const appointment = appointments.find(app => app.id === id)
  if (appointment) {
    appointment.status = status
    if (status === '已通过') {
      appointment.approveTime = new Date().toLocaleString()
    } else if (status === '已拒绝') {
      appointment.rejectTime = new Date().toLocaleString()
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appointments))
    console.log(`预约状态已更新: ID=${id}, 新状态=${status}`)
  } else {
    console.warn(`未找到ID为${id}的预约`)
  }
  return appointment
}

// 访客登记
export function checkInVisitor(id, remark = '') {
  const appointments = getVisitorAppointments()
  const appointment = appointments.find(app => app.id === id)
  if (appointment) {
    appointment.status = '已登记'
    appointment.checkInTime = new Date().toLocaleString()
    appointment.remark = remark
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appointments))
    console.log(`访客已登记: ID=${id}`)
  }
  return appointment
}

// 访客离校
export function checkOutVisitor(id) {
  const appointments = getVisitorAppointments()
  const appointment = appointments.find(app => app.id === id)
  if (appointment) {
    appointment.status = '已离开'
    appointment.checkOutTime = new Date().toLocaleString()
    localStorage.setItem(STORAGE_KEY, JSON.stringify(appointments))
    console.log(`访客已离校: ID=${id}`)
  }
  return appointment
}

// 删除访客预约
export function deleteVisitorAppointment(id) {
  const appointments = getVisitorAppointments()
  const filteredAppointments = appointments.filter(app => app.id !== id)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(filteredAppointments))
  console.log(`预约已删除: ID=${id}`)
}

// 获取学生的预约记录
export function getStudentAppointments(studentId) {
  const appointments = getVisitorAppointments()
  return appointments.filter(app => app.studentId === studentId)
}

// 搜索访客预约
export function searchVisitorAppointments(keyword, status, date, appointments = null) {
  let data = appointments || getVisitorAppointments()
  
  if (keyword) {
    data = data.filter(app => 
      app.name.includes(keyword) || 
      app.idCard.includes(keyword) ||
      app.visitedStudent.includes(keyword)
    )
  }
  
  if (status) {
    data = data.filter(app => app.status === status)
  }
  
  if (date) {
    const filterDateStr = date.toISOString().split('T')[0]
    data = data.filter(app => 
      app.visitTime && app.visitTime.startsWith(filterDateStr)
    )
  }
  
  return data
}

// 获取预约统计信息
export function getAppointmentStats() {
  const appointments = getVisitorAppointments()
  const stats = {
    total: appointments.length,
    pending: appointments.filter(app => app.status === '待审核').length,
    approved: appointments.filter(app => app.status === '已通过').length,
    rejected: appointments.filter(app => app.status === '已拒绝').length,
    checkedIn: appointments.filter(app => app.status === '已登记').length,
    checkedOut: appointments.filter(app => app.status === '已离开').length
  }
  return stats
}

// 初始化示例数据
export function initSampleData() {
  // 强制重新初始化数据，清除旧数据
  const sampleAppointments = [
    {
      id: 1,
      name: '张小明',
      idCard: '110101199001011234',
      phone: '13800138000',
      visitedStudent: '李四',
      visitDorm: '1号楼101室',
      visitTime: '2024-01-15 14:00',
      leaveTime: '2024-01-15 16:00',
      purpose: '探访同学',
      remark: '探访室友',
      studentId: '2021001',
      status: '已通过',
      createTime: '2024-01-14 10:00',
      approveTime: '2024-01-14 11:00',
      checkInTime: null,
      checkOutTime: null
    },
    {
      id: 2,
      name: '李小红',
      idCard: '110101199002021234',
      phone: '13800138001',
      visitedStudent: '王五',
      visitDorm: '2号楼201室',
      visitTime: '2024-01-16 16:00',
      leaveTime: '2024-01-16 18:00',
      purpose: '送物品',
      remark: '送快递',
      studentId: '2021002',
      status: '待审核',
      createTime: '2024-01-15 15:00',
      checkInTime: null,
      checkOutTime: null
    },
    {
      id: 3,
      name: '王小明',
      idCard: '110101199003031234',
      phone: '13800138002',
      visitedStudent: '赵六',
      visitDorm: '1号楼102室',
      visitTime: '2024-01-14 10:00',
      leaveTime: '2024-01-14 12:00',
      purpose: '探访同学',
      remark: '探访同学',
      studentId: '2021003',
      status: '已离开',
      createTime: '2024-01-13 09:00',
      approveTime: '2024-01-13 10:00',
      checkInTime: '2024-01-14 10:05',
      checkOutTime: '2024-01-14 12:00'
    },
    {
      id: 4,
      name: '陈小华',
      idCard: '110101199004041234',
      phone: '13800138003',
      visitedStudent: '孙七',
      visitDorm: '3号楼301室',
      visitTime: '2024-01-17 09:00',
      leaveTime: '2024-01-17 11:00',
      purpose: '办理事务',
      remark: '办理学生证',
      studentId: '2021004',
      status: '已拒绝',
      createTime: '2024-01-16 14:00',
      rejectTime: '2024-01-16 16:00',
      checkInTime: null,
      checkOutTime: null
    },
    {
      id: 5,
      name: '刘小芳',
      idCard: '110101199005051234',
      phone: '13800138004',
      visitedStudent: '周八',
      visitDorm: '1号楼103室',
      visitTime: '2024-01-18 15:00',
      leaveTime: '2024-01-18 17:00',
      purpose: '探访同学',
      remark: '探访高中同学',
      studentId: '2021005',
      status: '已登记',
      createTime: '2024-01-17 10:00',
      approveTime: '2024-01-17 11:30',
      checkInTime: '2024-01-18 15:10',
      checkOutTime: null
    },
    {
      id: 6,
      name: '赵小强',
      idCard: '110101199006061234',
      phone: '13800138005',
      visitedStudent: '吴九',
      visitDorm: '2号楼202室',
      visitTime: '2024-01-19 13:00',
      leaveTime: '2024-01-19 15:00',
      purpose: '送物品',
      remark: '送学习资料',
      studentId: '2021006',
      status: '待审核',
      createTime: '2024-01-18 16:00',
      checkInTime: null,
      checkOutTime: null
    },
    {
      id: 7,
      name: '杨小丽',
      idCard: '110101199007071234',
      phone: '13800138006',
      visitedStudent: '郑十',
      visitDorm: '3号楼302室',
      visitTime: '2024-01-20 10:00',
      leaveTime: '2024-01-20 12:00',
      purpose: '其他',
      remark: '参加社团活动',
      studentId: '2021007',
      status: '待审核',
      createTime: '2024-01-19 09:00',
      checkInTime: null,
      checkOutTime: null
    },
    {
      id: 8,
      name: '黄小伟',
      idCard: '110101199008081234',
      phone: '13800138007',
      visitedStudent: '冯十一',
      visitDorm: '1号楼104室',
      visitTime: '2024-01-21 16:00',
      leaveTime: '2024-01-21 18:00',
      purpose: '探访同学',
      remark: '探访大学同学',
      studentId: '2021008',
      status: '已通过',
      createTime: '2024-01-20 11:00',
      approveTime: '2024-01-20 12:00',
      checkInTime: null,
      checkOutTime: null
    }
  ]
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sampleAppointments))
}
