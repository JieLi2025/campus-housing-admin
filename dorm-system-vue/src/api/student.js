import request from '@/utils/request'

/**
 * 获取学生列表（分页、搜索、筛选）
 */
export function getStudentList(queryParams) {
  return request({
    url: '/students/list',
    method: 'post',
    data: queryParams
  })
}

/**
 * 根据ID获取学生详情
 */
export function getStudentById(id) {
  return request({
    url: `/students/${id}`,
    method: 'get'
  })
}

/**
 * 根据学号获取学生详情
 */
export function getStudentByStudentId(studentId) {
  return request({
    url: `/students/by-student-id/${studentId}`,
    method: 'get'
  })
}

/**
 * 根据用户ID获取学生详情
 */
export function getStudentByUserId(userId) {
  return request({
    url: `/students/by-user-id/${userId}`,
    method: 'get'
  })
}

/**
 * Excel导入学生信息
 */
export function importStudents(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/students/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000 // 导入可能需要较长时间，设置60秒超时
  })
}

/**
 * 批量生成邀请码
 */
export function generateInviteCodes(studentIds) {
  return request({
    url: '/students/generate-invite-codes',
    method: 'post',
    data: studentIds
  })
}

/**
 * 更新学生信息
 */
export function updateStudent(id, data) {
  return request({
    url: `/students/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除单个学生（硬删除）
 * 警告：此操作会永久删除学生及所有关联数据（室友邀请、队伍、床位、申请、通知等），不可恢复！
 */
export function deleteStudent(id) {
  return request({
    url: `/students/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除学生（硬删除）
 * 警告：此操作会永久删除学生及所有关联数据（室友邀请、队伍、床位、申请、通知等），不可恢复！
 */
export function batchDeleteStudents(studentIds) {
  return request({
    url: '/students/batch',
    method: 'delete',
    data: studentIds
  })
}

/**
 * 获取学生的所有关联数据（删除前预览）
 */
export function getStudentRelatedData(id) {
  return request({
    url: `/students/${id}/related-data`,
    method: 'get'
  })
}

/**
 * 调试接口：对比楼栋专业分配与实际学生数据
 */
export function debugCompareAssignment() {
  return request({
    url: '/students/debug/compare-assignment',
    method: 'get'
  })
}

/**
 * 分析接口：检测专业分配数据不匹配问题
 */
export function analyzeAssignmentMismatch() {
  return request({
    url: '/students/debug/analyze-mismatch',
    method: 'get'
  })
}

