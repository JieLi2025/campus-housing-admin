<template>
  <div class="material-management-page">
    <el-card>
      <template #header>
        <h2>宿舍用品管理</h2>
      </template>
      
      <!-- 宿舍用品清单 -->
      <div class="materials-section">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <h3 style="margin: 0;">
            宿舍用品清单
            <span v-if="dormInfo.building && dormInfo.roomNumber" style="font-size: 14px; color: #909399; font-weight: normal; margin-left: 10px;">
              （{{ dormInfo.building }} {{ dormInfo.roomNumber }}）
            </span>
          </h3>
          <el-button type="primary" @click="reportOtherIssue">申报其他物品</el-button>
        </div>
        <el-table :data="dormMaterials" border style="width: 100%" v-loading="loading">
          <el-table-column prop="name" label="用品名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="category" label="类别" width="100">
            <template #default="{ row }">
              <el-tag :type="getMaterialCategoryType(row.category)" size="small">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="配置数量" width="100" align="center">
            <template #default="{ row }">
              {{ row.allocatedQuantity }}{{ row.unit }}
            </template>
          </el-table-column>
          <el-table-column label="实际数量" width="100" align="center">
            <template #default="{ row }">
              {{ row.actualQuantity }}{{ row.unit }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getMaterialStatusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button 
                type="warning" 
                size="small" 
                @click="reportMaterialIssue(row)"
                v-if="row.status !== '损坏' && row.status !== '缺失'"
              >
                申报问题
              </el-button>
              <el-tag v-else type="info" size="small">已申报</el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="!loading && dormMaterials.length === 0" :description="emptyMessage || '暂无用品信息'">
          <template #image>
            <el-icon :size="60" :color="emptyType === 'not-allocated' ? '#E6A23C' : '#909399'">
              <component :is="emptyType === 'not-allocated' ? 'Clock' : 'Box'" />
            </el-icon>
          </template>
          <template #default v-if="emptyType === 'not-allocated'">
            <div style="color: #909399; font-size: 14px; margin-top: 10px;">
              <p style="margin: 0 0 8px 0;">{{ emptyMessage }}</p>
              <p style="margin: 0; font-size: 13px;">完成选房后即可查看宿舍配置的用品清单</p>
              <el-button type="primary" size="small" style="margin-top: 12px;" @click="$router.push('/student/dorm-management')">
                前往宿舍申请
              </el-button>
            </div>
          </template>
        </el-empty>
      </div>

      <!-- 我的申报记录 -->
      <div class="reports-section">
        <h3>我的申报记录</h3>
        <el-table :data="myReports" border>
          <el-table-column prop="materialName" label="用品名称" width="100" />
          <el-table-column prop="issueType" label="问题类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getIssueTypeTagType(row.issueType)" size="small">{{ row.issueType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="issue" label="问题描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="reportTime" label="申报时间" width="170">
            <template #default="{ row }">
              {{ formatTime(row.reportTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="处理状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="getReportStatusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handleRemark" label="处理备注" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.handleRemark || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === '待处理'">
                <el-button type="primary" size="small" @click="editReport(row)">修改</el-button>
                <el-button type="danger" size="small" @click="withdrawReport(row)">撤回</el-button>
              </template>
              <span v-else style="color: #909399;">-</span>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="myReports.length === 0" description="暂无申报记录" />
      </div>
    </el-card>

    <!-- 申报问题对话框 -->
    <el-dialog
      v-model="showReportDialog"
      :title="reportForm.isCustomItem ? '申报其他物品' : '申报用品问题'"
      width="500px"
    >
      <el-form :model="reportForm" :rules="reportRules" ref="reportFormRef" label-width="100px">
        <el-form-item label="用品名称" :prop="reportForm.isCustomItem ? 'customMaterialName' : ''">
          <el-input 
            v-if="reportForm.isCustomItem"
            v-model="reportForm.customMaterialName" 
            placeholder="请输入物品名称"
          />
          <el-input v-else v-model="reportForm.materialName" disabled />
        </el-form-item>
        <el-form-item label="问题类型" prop="issueType">
          <el-select v-model="reportForm.issueType" placeholder="请选择问题类型" style="width: 100%">
            <el-option label="损坏" value="损坏" />
            <el-option label="缺失" value="缺失" />
            <el-option label="故障" value="故障" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="issue">
          <el-input
            v-model="reportForm.issue"
            type="textarea"
            :rows="3"
            placeholder="请详细描述问题"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交申报</el-button>
      </template>
    </el-dialog>

    <!-- 修改申报对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="修改申报"
      width="500px"
    >
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="用品名称">
          <el-input v-model="editForm.materialName" disabled />
        </el-form-item>
        <el-form-item label="问题类型" prop="issueType">
          <el-select v-model="editForm.issueType" placeholder="请选择问题类型" style="width: 100%">
            <el-option label="损坏" value="损坏" />
            <el-option label="缺失" value="缺失" />
            <el-option label="故障" value="故障" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="issue">
          <el-input
            v-model="editForm.issue"
            type="textarea"
            :rows="3"
            placeholder="请详细描述问题"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditReport">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getMyRoomMaterialTemplate, submitMaterialReport, getMyMaterialReports, withdrawMaterialReport, updateMaterialReport } from '@/api/material.js'
import { getAllocationResult } from '@/api/studentAllocation.js'
import { Clock, Box } from '@element-plus/icons-vue'

export default {
  name: 'MaterialManagement',
  components: {
    Clock,
    Box
  },
  data() {
    return {
      dormMaterials: [],
      myReports: [],
      loading: false,
      dormInfo: {
        building: '',
        roomNumber: ''
      },
      // 空状态提示信息
      emptyMessage: '',
      emptyType: '', // 'not-allocated' | 'not-configured' | ''
      // 修改申报对话框
      showEditDialog: false,
      editForm: {
        id: null,
        materialName: '',
        issueType: '',
        issue: ''
      },
      showReportDialog: false,
      reportForm: {
        materialName: '',
        materialId: null,
        customMaterialName: '',
        isCustomItem: false,
        issueType: '',
        issue: ''
      },
      reportRules: {
        customMaterialName: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
        issueType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
        issue: [{ required: true, message: '请描述问题', trigger: 'blur' }]
      },
      editRules: {
        issueType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
        issue: [{ required: true, message: '请描述问题', trigger: 'blur' }]
      }
    }
  },
  mounted() {
    this.loadDormInfo()
    this.loadDormMaterials()
    this.loadMyReports()
  },
  methods: {
    async loadDormInfo() {
      try {
        const res = await getAllocationResult()
        if (res.code === 200 && res.data && res.data.success) {
          this.dormInfo = {
            building: res.data.building || '',
            roomNumber: res.data.roomNumber || ''
          }
        }
      } catch (error) {
        console.error('加载宿舍信息失败:', error)
      }
    },
    
    async loadDormMaterials() {
      this.loading = true
      this.emptyMessage = ''
      this.emptyType = ''
      try {
        // 从后端API获取物资配置模板
        const res = await getMyRoomMaterialTemplate()
        if (res.code === 200 && res.data) {
          // 检查是否有提示信息（未分配宿舍或未配置物资）
          if (res.data.message) {
            if (res.data.message.includes('尚未分配') || res.data.message.includes('未分配')) {
              this.emptyMessage = '您尚未完成宿舍选房'
              this.emptyType = 'not-allocated'
            } else if (res.data.message.includes('暂未配置')) {
              this.emptyMessage = '该楼栋房型暂未配置物资，请联系管理员'
              this.emptyType = 'not-configured'
            } else {
              this.emptyMessage = res.data.message
              this.emptyType = ''
            }
            this.dormMaterials = []
            return
          }
          
          // 解析物资配置JSON
          let materials = []
          try {
            materials = JSON.parse(res.data.materials || '[]')
          } catch (e) {
            console.error('解析物资配置失败:', e)
            materials = []
          }
          
          if (materials.length === 0) {
            this.emptyMessage = '该楼栋房型暂未配置物资，请联系管理员'
            this.emptyType = 'not-configured'
          }
          
          // 转换为前端显示格式
          this.dormMaterials = materials.map((item, index) => ({
            materialId: item.id || index + 1,
            name: item.name,
            category: item.category || this.getCategoryFromName(item.name),
            allocatedQuantity: item.quantity || 1,
            actualQuantity: item.quantity || 1,
            unit: item.unit || '个',
            status: '正常',
            remark: ''
          }))
          
          // 根据申报记录更新物品状态
          this.updateMaterialStatusFromReports()
        }
      } catch (error) {
        console.error('加载宿舍物资失败:', error)
        this.emptyMessage = '加载失败，请刷新重试'
        this.emptyType = ''
        // 如果API调用失败，显示空列表
        this.dormMaterials = []
      } finally {
        this.loading = false
      }
    },
    
    // 根据申报记录更新物品状态
    updateMaterialStatusFromReports() {
      if (!this.myReports || this.myReports.length === 0) return
      
      this.dormMaterials.forEach(material => {
        // 查找该物品的申报记录（未处理的优先）
        const pendingReport = this.myReports.find(
          r => r.materialName === material.name && r.status === '待处理'
        )
        
        if (pendingReport) {
          // 有待处理的申报，显示问题类型
          material.status = pendingReport.issueType
        } else {
          // 没有待处理的申报，显示正常
          material.status = '正常'
        }
      })
    },
    
    // 根据物品名称推断类别
    getCategoryFromName(name) {
      if (!name) return '其他'
      const furnitureKeywords = ['床', '桌', '椅', '柜', '架']
      const electricKeywords = ['空调', '热水器', '电扇', '风扇', '灯', '电器']
      const dailyKeywords = ['被', '枕', '垫', '帘', '拖把', '扫帚', '垃圾桶']
      
      if (furnitureKeywords.some(k => name.includes(k))) return '家具'
      if (electricKeywords.some(k => name.includes(k))) return '电器'
      if (dailyKeywords.some(k => name.includes(k))) return '日用品'
      return '其他'
    },
    
    async loadMyReports() {
      try {
        const res = await getMyMaterialReports()
        if (res.code === 200 && res.data) {
          // 转换后端数据格式为前端显示格式
          this.myReports = res.data.map(item => ({
            id: item.id,
            materialName: this.extractMaterialName(item.reason),
            issueType: this.extractIssueType(item.reason),
            issue: this.extractIssueDesc(item.reason),
            reportTime: item.createdAt,
            status: this.translateStatus(item.status),
            handleRemark: item.adminComment || ''
          }))
          
          // 根据申报记录更新物品状态
          this.updateMaterialStatusFromReports()
        }
      } catch (error) {
        console.error('加载申报记录失败:', error)
        this.myReports = []
      }
    },
    
    // 从reason中提取物品名称
    extractMaterialName(reason) {
      if (!reason) return '-'
      const match = reason.match(/【[^】]+】([^-]+)/)
      return match ? match[1].trim() : '-'
    },
    
    // 从reason中提取问题类型
    extractIssueType(reason) {
      if (!reason) return '-'
      const match = reason.match(/【([^】]+)】/)
      return match ? match[1] : '-'
    },
    
    // 从reason中提取问题描述
    extractIssueDesc(reason) {
      if (!reason) return '-'
      const match = reason.match(/：(.+)$/)
      return match ? match[1] : '-'
    },
    
    // 翻译状态
    translateStatus(status) {
      if (!status) return '-'
      const upperStatus = status.toUpperCase()
      const statusMap = {
        'PENDING': '待处理',
        'APPROVED': '已处理',
        'REJECTED': '已拒绝'
      }
      return statusMap[upperStatus] || status
    },
    
    // 获取问题类型标签颜色
    getIssueTypeTagType(issueType) {
      const types = {
        '损坏': 'danger',
        '缺失': 'warning',
        '故障': 'warning',
        '其他': 'info'
      }
      return types[issueType] || 'info'
    },
    
    getMaterialCategoryType(category) {
      const types = {
        '家具': 'primary',
        '电器': 'success',
        '日用品': 'warning',
        '其他': 'info'
      }
      return types[category] || 'info'
    },
    
    getMaterialStatusType(status) {
      const types = {
        '正常': 'success',
        '损坏': 'danger',
        '缺失': 'warning',
        '故障': 'warning',
        '其他': 'info'
      }
      return types[status] || 'warning'
    },
    
    getReportStatusType(status) {
      const types = {
        '待处理': 'warning',
        '处理中': 'primary',
        '已处理': 'success',
        '已拒绝': 'danger'
      }
      return types[status] || 'info'
    },
    
    reportMaterialIssue(material) {
      this.reportForm = {
        materialName: material.name,
        materialId: material.materialId,
        customMaterialName: '',
        isCustomItem: false,
        issueType: '',
        issue: ''
      }
      this.showReportDialog = true
      // 清除之前的验证状态
      this.$nextTick(() => {
        if (this.$refs.reportFormRef) {
          this.$refs.reportFormRef.clearValidate()
        }
      })
    },
    
    reportOtherIssue() {
      this.reportForm = {
        materialName: '',
        materialId: null,
        customMaterialName: '',
        isCustomItem: true,
        issueType: '',
        issue: ''
      }
      this.showReportDialog = true
      // 清除之前的验证状态
      this.$nextTick(() => {
        if (this.$refs.reportFormRef) {
          this.$refs.reportFormRef.clearValidate()
        }
      })
    },
    
    async submitReport() {
      this.$refs.reportFormRef.validate(async (valid) => {
        if (valid) {
          // 确定物品名称：如果是自定义物品，使用customMaterialName，否则使用materialName
          const finalMaterialName = this.reportForm.isCustomItem 
            ? this.reportForm.customMaterialName 
            : this.reportForm.materialName
          
          try {
            // 调用后端API提交申报
            const res = await submitMaterialReport({
              materialName: finalMaterialName,
              issueType: this.reportForm.issueType,
              issue: this.reportForm.issue
            })
            
            if (res.code === 200) {
              this.$message.success('申报成功！管理员将尽快处理')
              this.showReportDialog = false
              this.loadMyReports()
              
              // 触发通知更新事件
              window.dispatchEvent(new Event('notificationUpdated'))
              
              // 清空表单
              this.reportForm = {
                materialName: '',
                materialId: null,
                customMaterialName: '',
                isCustomItem: false,
                issueType: '',
                issue: ''
              }
              if (this.$refs.reportFormRef) {
                this.$refs.reportFormRef.resetFields()
              }
            } else {
              this.$message.error(res.message || '申报失败')
            }
          } catch (error) {
            console.error('提交申报失败:', error)
            this.$message.error('申报失败：' + (error.message || '网络错误'))
          }
        }
      })
    },
    
    formatTime(timeStr) {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN')
    },
    
    // 修改申报
    editReport(row) {
      this.editForm = {
        id: row.id,
        materialName: row.materialName,
        issueType: row.issueType,
        issue: row.issue
      }
      this.showEditDialog = true
      this.$nextTick(() => {
        if (this.$refs.editFormRef) {
          this.$refs.editFormRef.clearValidate()
        }
      })
    },
    
    // 提交修改
    async submitEditReport() {
      this.$refs.editFormRef.validate(async (valid) => {
        if (valid) {
          try {
            const res = await updateMaterialReport(this.editForm.id, {
              materialName: this.editForm.materialName,
              issueType: this.editForm.issueType,
              issue: this.editForm.issue
            })
            
            if (res.code === 200) {
              this.$message.success('修改成功')
              this.showEditDialog = false
              this.loadMyReports()
            } else {
              this.$message.error(res.message || '修改失败')
            }
          } catch (error) {
            console.error('修改申报失败:', error)
            this.$message.error('修改失败：' + (error.message || '网络错误'))
          }
        }
      })
    },
    
    // 撤回申报
    withdrawReport(row) {
      this.$confirm('确定要撤回这条申报吗？', '确认撤回', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await withdrawMaterialReport(row.id)
          if (res.code === 200) {
            this.$message.success('申报已撤回')
            this.loadMyReports()
          } else {
            this.$message.error(res.message || '撤回失败')
          }
        } catch (error) {
          console.error('撤回申报失败:', error)
          this.$message.error('撤回失败：' + (error.message || '网络错误'))
        }
      }).catch(() => {
        // 取消操作
      })
    }
  }
}
</script>

<style scoped>
.material-management-page {
  padding: 20px;
}

.materials-section,
.reports-section {
  margin-bottom: 40px;
}

h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: normal;
  color: #303133;
}
</style>

