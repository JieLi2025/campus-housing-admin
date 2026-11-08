// 学生用品申报记录管理

const REPORT_STORAGE_KEY = 'material_reports'

// 默认测试数据
const defaultReports = [
  {
    id: 1,
    studentId: 'S001',
    studentName: '张三',
    dormNumber: '101',
    buildingName: '1号楼',
    materialName: '空调',
    materialId: 2,
    issueType: '故障',
    issue: '空调不制冷，可能需要加氟或维修压缩机',
    reportTime: '2024-01-14T10:30:00',
    status: '已处理',
    handleTime: '2024-01-14T15:20:00',
    handleRemark: '已安排维修师傅处理，空调已修好'
  },
  {
    id: 2,
    studentId: 'S001',
    studentName: '张三',
    dormNumber: '101',
    buildingName: '1号楼',
    materialName: '椅子',
    materialId: 3,
    issueType: '损坏',
    issue: '椅子腿断了，无法正常使用',
    reportTime: '2024-01-15T09:15:00',
    status: '待处理'
  }
]

// 获取所有申报记录
export function getAllReports() {
  const data = localStorage.getItem(REPORT_STORAGE_KEY)
  if (data) {
    return JSON.parse(data)
  }
  // 初始化默认数据
  saveReports(defaultReports)
  return defaultReports
}

// 保存申报记录
function saveReports(reports) {
  localStorage.setItem(REPORT_STORAGE_KEY, JSON.stringify(reports))
}

// 创建新申报
export function createReport(report) {
  const reports = getAllReports()
  const maxId = reports.reduce((max, item) => Math.max(max, item.id || 0), 0)
  
  const newReport = {
    id: maxId + 1,
    ...report,
    reportTime: new Date().toISOString(),
    status: '待处理'
  }
  
  reports.push(newReport)
  saveReports(reports)
  return newReport
}

// 获取学生的申报记录
export function getStudentReports(studentId) {
  const reports = getAllReports()
  return reports.filter(r => r.studentId === studentId)
}

// 获取宿舍的申报记录
export function getDormReports(dormNumber) {
  const reports = getAllReports()
  return reports.filter(r => r.dormNumber === dormNumber)
}

// 获取楼栋的申报记录
export function getBuildingReports(buildingName) {
  const reports = getAllReports()
  return reports.filter(r => r.buildingName === buildingName)
}

// 获取待处理的申报
export function getPendingReports() {
  const reports = getAllReports()
  return reports.filter(r => r.status === '待处理')
}

// 获取单个申报详情
export function getReportById(reportId) {
  const reports = getAllReports()
  return reports.find(r => r.id === reportId)
}

// 更新申报状态
export function updateReportStatus(reportId, status, handleRemark = '') {
  const reports = getAllReports()
  const index = reports.findIndex(r => r.id === reportId)
  
  if (index !== -1) {
    reports[index].status = status
    reports[index].handleTime = new Date().toISOString()
    reports[index].handleRemark = handleRemark
    saveReports(reports)
    return true
  }
  return false
}

// 删除申报
export function deleteReport(reportId) {
  const reports = getAllReports()
  const filtered = reports.filter(r => r.id !== reportId)
  
  if (filtered.length < reports.length) {
    saveReports(filtered)
    return true
  }
  return false
}

// 批量处理申报
export function batchUpdateReports(reportIds, status, handleRemark = '') {
  const reports = getAllReports()
  let updated = 0
  
  reports.forEach(report => {
    if (reportIds.includes(report.id)) {
      report.status = status
      report.handleTime = new Date().toISOString()
      report.handleRemark = handleRemark
      updated++
    }
  })
  
  if (updated > 0) {
    saveReports(reports)
  }
  
  return updated
}

// 获取申报统计
export function getReportStatistics() {
  const reports = getAllReports()
  
  return {
    total: reports.length,
    pending: reports.filter(r => r.status === '待处理').length,
    processing: reports.filter(r => r.status === '处理中').length,
    completed: reports.filter(r => r.status === '已处理').length,
    rejected: reports.filter(r => r.status === '已拒绝').length
  }
}

// 获取申报状态选项
export function getReportStatusOptions() {
  return ['待处理', '处理中', '已处理', '已拒绝']
}

// 获取问题类型选项
export function getIssueTypeOptions() {
  return ['损坏', '缺失', '故障', '其他']
}


