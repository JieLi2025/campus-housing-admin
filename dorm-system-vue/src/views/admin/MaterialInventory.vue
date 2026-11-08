<template>
  <div class="material-inventory-page">
    <el-card>
      <template #header>
        <div class="header-content">
          <h2>退宿物资清点</h2>
          <el-button @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回申请审核
          </el-button>
        </div>
      </template>

      <!-- 申请信息 -->
      <div class="application-info" v-if="applicationInfo">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="申请人">{{ applicationInfo.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ applicationInfo.studentNumber }}</el-descriptions-item>
          <el-descriptions-item label="宿舍">{{ applicationInfo.currentDormInfo || applicationInfo.dormInfo }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(applicationInfo.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="退宿原因" :span="4">{{ applicationInfo.reason }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 物资清点表 -->
      <div class="inventory-section">
        <div class="section-header">
          <h3>物资清点清单</h3>
          <el-tag type="info">请逐项核对物资状态</el-tag>
        </div>
        
        <el-table :data="materialList" border style="width: 100%" v-loading="loading">
          <el-table-column prop="name" label="物品名称" width="150" />
          <el-table-column prop="category" label="类别" width="100">
            <template #default="{ row }">
              <el-tag :type="getCategoryType(row.category)" size="small">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="配置数量" width="100" align="center" />
          <el-table-column label="实际状态" width="150" align="center">
            <template #default="{ row }">
              <el-select v-model="row.status" placeholder="选择状态" size="small" style="width: 120px">
                <el-option label="完好" value="good" />
                <el-option label="损坏" value="damaged" />
                <el-option label="缺失" value="missing" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="赔偿金额" width="120" align="center">
            <template #default="{ row }">
              <el-input-number 
                v-if="row.status === 'damaged' || row.status === 'missing'"
                v-model="row.compensation" 
                :min="0" 
                :precision="2"
                size="small"
                style="width: 100px"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="150">
            <template #default="{ row }">
              <el-input 
                v-model="row.remark" 
                placeholder="备注说明"
                size="small"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 清点结果汇总 -->
      <div class="summary-section">
        <el-card shadow="never">
          <div class="summary-content">
            <div class="summary-item">
              <span class="label">物品总数：</span>
              <span class="value">{{ materialList.length }} 项</span>
            </div>
            <div class="summary-item">
              <span class="label">完好：</span>
              <span class="value good">{{ goodCount }} 项</span>
            </div>
            <div class="summary-item">
              <span class="label">损坏：</span>
              <span class="value damaged">{{ damagedCount }} 项</span>
            </div>
            <div class="summary-item">
              <span class="label">缺失：</span>
              <span class="value missing">{{ missingCount }} 项</span>
            </div>
            <div class="summary-item total">
              <span class="label">赔偿总额：</span>
              <span class="value">¥ {{ totalCompensation.toFixed(2) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 操作按钮 -->
      <div class="action-section">
        <el-button @click="goBack">取消</el-button>
        <el-button 
          v-if="totalCompensation > 0"
          type="primary" 
          @click="submitWithCompensation"
          :loading="submitting"
        >
          提交并等待赔偿
        </el-button>
        <el-button 
          v-else
          type="success" 
          @click="approveCheckout"
          :loading="submitting"
        >
          物资完好，通过退宿
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ArrowLeft } from '@element-plus/icons-vue'
import { getApplicationDetail, approveApplication } from '@/api/application.js'
import { getRoomMaterialTemplate, submitInventoryResult } from '@/api/adminMaterial.js'

export default {
  name: 'MaterialInventory',
  components: {
    ArrowLeft
  },
  data() {
    return {
      applicationId: null,
      dormNumber: '',
      applicationInfo: null,
      materialList: [],
      loading: false,
      saving: false,
      submitting: false
    }
  },
  computed: {
    goodCount() {
      return this.materialList.filter(m => m.status === 'good').length
    },
    damagedCount() {
      return this.materialList.filter(m => m.status === 'damaged').length
    },
    missingCount() {
      return this.materialList.filter(m => m.status === 'missing').length
    },
    totalCompensation() {
      return this.materialList.reduce((sum, m) => {
        if (m.status === 'damaged' || m.status === 'missing') {
          return sum + (m.compensation || 0)
        }
        return sum
      }, 0)
    }
  },
  mounted() {
    this.applicationId = this.$route.params.applicationId
    this.dormNumber = this.$route.params.dormNumber
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        // 加载申请详情
        if (this.applicationId) {
          const res = await getApplicationDetail(this.applicationId)
          if (res.code === 200 && res.data) {
            this.applicationInfo = res.data
          }
        }
        
        // 加载物资模板
        await this.loadMaterialTemplate()
      } catch (error) {
        console.error('加载数据失败:', error)
        this.$message.error('加载数据失败')
      } finally {
        this.loading = false
      }
    },
    
    async loadMaterialTemplate() {
      try {
        // 尝试从后端获取物资模板
        const res = await getRoomMaterialTemplate(this.dormNumber)
        if (res.code === 200 && res.data) {
          let materials = []
          try {
            materials = JSON.parse(res.data.materials || '[]')
          } catch (e) {
            materials = []
          }
          
          this.materialList = materials.map((item, index) => ({
            id: item.id || index + 1,
            name: item.name,
            category: item.category || this.getCategoryFromName(item.name),
            quantity: item.quantity || 1,
            unit: item.unit || '个',
            status: 'good',
            compensation: 0,
            remark: ''
          }))
        }
      } catch (error) {
        console.error('加载物资模板失败:', error)
        // 使用默认物资列表
        this.materialList = this.getDefaultMaterials()
      }
      
      // 如果没有物资数据，使用默认列表
      if (this.materialList.length === 0) {
        this.materialList = this.getDefaultMaterials()
      }
    },
    
    getDefaultMaterials() {
      return [
        { id: 1, name: '床', category: '家具', quantity: 1, unit: '张', status: 'good', compensation: 0, remark: '' },
        { id: 2, name: '书桌', category: '家具', quantity: 1, unit: '张', status: 'good', compensation: 0, remark: '' },
        { id: 3, name: '椅子', category: '家具', quantity: 1, unit: '把', status: 'good', compensation: 0, remark: '' },
        { id: 4, name: '衣柜', category: '家具', quantity: 1, unit: '个', status: 'good', compensation: 0, remark: '' },
        { id: 5, name: '空调', category: '电器', quantity: 1, unit: '台', status: 'good', compensation: 0, remark: '' },
        { id: 6, name: '电扇', category: '电器', quantity: 1, unit: '台', status: 'good', compensation: 0, remark: '' },
        { id: 7, name: '热水器', category: '电器', quantity: 1, unit: '台', status: 'good', compensation: 0, remark: '' },
        { id: 8, name: '窗帘', category: '日用品', quantity: 1, unit: '套', status: 'good', compensation: 0, remark: '' }
      ]
    },
    
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
    
    getCategoryType(category) {
      const types = {
        '家具': 'primary',
        '电器': 'success',
        '日用品': 'warning',
        '其他': 'info'
      }
      return types[category] || 'info'
    },
    
    formatTime(time) {
      if (!time) return '--'
      try {
        const date = new Date(time)
        return date.toLocaleString('zh-CN')
      } catch (e) {
        return '--'
      }
    },
    
    goBack() {
      this.$router.push({ name: 'ApplicationReview' })
    },
    
    async saveInventory() {
      this.saving = true
      try {
        const inventoryData = {
          applicationId: this.applicationId,
          materials: this.materialList.map(m => ({
            name: m.name,
            status: m.status,
            compensation: m.compensation || 0,
            remark: m.remark
          })),
          totalCompensation: this.totalCompensation
        }
        
        const res = await submitInventoryResult(inventoryData)
        if (res.code === 200) {
          this.$message.success('清点结果已保存')
        }
      } catch (error) {
        console.error('保存失败:', error)
        this.$message.error('保存失败')
      } finally {
        this.saving = false
      }
    },
    
    async submitWithCompensation() {
      // 检查是否所有物品都已清点
      const unchecked = this.materialList.filter(m => !m.status)
      if (unchecked.length > 0) {
        this.$message.warning('请完成所有物品的状态检查')
        return
      }
      
      this.submitting = true
      try {
        // 先保存清点结果
        await this.saveInventory()
        
        // 更新申请状态为待赔偿
        const res = await approveApplication(this.applicationId, {
          approved: true,
          status: 'COMPENSATION_PENDING',
          comment: `物资清点完成，需赔偿 ¥${this.totalCompensation.toFixed(2)}`,
          inventoryResult: {
            totalCompensation: this.totalCompensation,
            damagedItems: this.materialList.filter(m => m.status === 'damaged'),
            missingItems: this.materialList.filter(m => m.status === 'missing')
          }
        })
        
        if (res.code === 200) {
          this.$message.success('已提交，等待学生赔偿')
          this.goBack()
        }
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error('提交失败')
      } finally {
        this.submitting = false
      }
    },
    
    async approveCheckout() {
      this.submitting = true
      try {
        // 先保存清点结果
        await this.saveInventory()
        
        // 通过退宿申请
        const res = await approveApplication(this.applicationId, {
          approved: true,
          comment: '物资清点完好，退宿申请通过'
        })
        
        if (res.code === 200) {
          this.$message.success('退宿申请已通过')
          this.goBack()
        }
      } catch (error) {
        console.error('审批失败:', error)
        this.$message.error('审批失败')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>


<style scoped>
.material-inventory-page {
  padding: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
}

.application-info {
  margin-bottom: 24px;
}

.inventory-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.summary-section {
  margin-bottom: 24px;
}

.summary-content {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: center;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-item .label {
  color: #606266;
}

.summary-item .value {
  font-weight: 600;
  font-size: 16px;
}

.summary-item .value.good {
  color: #67c23a;
}

.summary-item .value.damaged {
  color: #e6a23c;
}

.summary-item .value.missing {
  color: #f56c6c;
}

.summary-item.total {
  margin-left: auto;
  padding: 8px 16px;
  background: #fef0f0;
  border-radius: 4px;
}

.summary-item.total .value {
  color: #f56c6c;
  font-size: 20px;
}

.action-section {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
