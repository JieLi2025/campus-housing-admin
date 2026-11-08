<template>
  <div class="config-management-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>配置管理</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 物资配置标签页 -->
        <el-tab-pane label="物资配置" name="materials">
          <div class="materials-config">
            <!-- 配置说明 -->
            <div class="config-tip">
              <el-icon><InfoFilled /></el-icon>
              配置各楼栋不同房型的物资清单。首次配置时可使用默认模板快速填充，保存后可独立编辑。
            </div>

            <!-- 筛选区域 -->
            <el-card class="filter-card" shadow="never">
              <template #header>
                <div style="font-weight: 500; font-size: 15px;">
                  <el-icon style="vertical-align: middle;"><OfficeBuilding /></el-icon>
                  <span style="margin-left: 8px;">选择楼栋和房型</span>
                </div>
              </template>
              <el-form :inline="true" class="filter-form">
                <el-form-item label="选择楼栋">
                  <el-select 
                    v-model="selectedBuilding" 
                    placeholder="请选择楼栋" 
                    @change="onBuildingChange"
                    style="width: 200px;"
                    clearable
                  >
                    <el-option
                      v-for="building in buildingList"
                      :key="building.name"
                      :label="building.name"
                      :value="building.name"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="选择房型">
                  <el-select 
                    v-model="selectedRoomType" 
                    placeholder="请选择房型" 
                    @change="onRoomTypeChange"
                    style="width: 200px;"
                    clearable
                    :disabled="!selectedBuilding"
                  >
                    <el-option
                      v-for="roomType in roomTypeOptions"
                      :key="roomType"
                      :label="roomType"
                      :value="roomType"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="selectedBuilding && selectedRoomType">
                  <el-button @click="copyFromOther">
                    <el-icon><CopyDocument /></el-icon> 复制其他楼栋
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 当前配置显示 -->
            <el-card v-if="selectedBuilding && selectedRoomType" class="template-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <span>当前配置：{{ selectedBuilding }} - {{ selectedRoomType }}</span>
                  <el-button type="primary" size="small" @click="addMaterialDialog = true">
                    <el-icon><Plus /></el-icon> 添加用品
                  </el-button>
                </div>
              </template>

              <el-table :data="currentTemplate" border stripe>
                <el-table-column prop="name" label="用品名称" min-width="150" />
                <el-table-column prop="category" label="类别" width="130" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getCategoryType(row.category)">{{ row.category }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="quantity" label="数量" width="120" align="center" />
                <el-table-column prop="unit" label="单位" width="120" align="center" />
                <el-table-column label="操作" width="200" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" size="small" @click="editMaterial(row)">编辑</el-button>
                    <el-button type="danger" size="small" @click="deleteMaterial(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="save-actions">
                <el-button type="primary" @click="saveCurrentTemplate">保存配置</el-button>
                <el-button @click="resetTemplate">重置</el-button>
              </div>
            </el-card>

            <!-- 未选择提示 -->
            <el-card v-else shadow="never" class="empty-tip-card">
              <el-empty description="">
                <template #image>
                  <el-icon :size="80" color="#909399"><Box /></el-icon>
                </template>
                <template #description>
                  <div class="empty-tip-text">
                    <p style="font-size: 16px; color: #606266; margin: 10px 0;">
                      {{ !selectedBuilding ? '请先选择楼栋' : '请选择房型' }}
                    </p>
                    <p style="font-size: 14px; color: #909399; margin: 0;">
                      {{ !selectedBuilding ? '在上方选择一个楼栋，然后选择房型查看用品配置' : '在上方选择一个房型，即可查看和编辑该楼栋房型的用品配置' }}
                    </p>
                  </div>
                </template>
              </el-empty>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 设施管理标签页 -->
        <el-tab-pane label="设施管理" name="room-config">
          <div class="room-config">
            <!-- 配置说明 -->
            <div class="config-tip">
              <el-icon><InfoFilled /></el-icon>
              管理设施列表和房型默认模板。房型默认模板用于物资配置首次加载时的初始设施。
            </div>

            <!-- 子标签页 -->
            <div class="sub-tabs">
              <div 
                class="sub-tab-item" 
                :class="{ 'active': facilitySubTab === 'template' }"
                @click="facilitySubTab = 'template'"
              >
                <el-icon><Tickets /></el-icon>
                <span>房型默认模板</span>
              </div>
              <div 
                class="sub-tab-item" 
                :class="{ 'active': facilitySubTab === 'list' }"
                @click="facilitySubTab = 'list'"
              >
                <el-icon><Setting /></el-icon>
                <span>设施列表</span>
              </div>
            </div>

            <!-- 房型默认模板 -->
            <div v-show="facilitySubTab === 'template'">
              <el-card shadow="never" style="margin-bottom: 20px;">
                <template #header>
                  <div style="font-weight: 500; font-size: 15px;">
                    <el-icon style="vertical-align: middle;"><OfficeBuilding /></el-icon>
                    <span style="margin-left: 8px;">选择房型</span>
                  </div>
                </template>
                <el-radio-group v-model="selectedTemplateRoomType" size="large" @change="onTemplateRoomTypeChange">
                  <el-radio-button 
                    v-for="roomType in roomTypeOptions" 
                    :key="roomType"
                    :label="roomType"
                  >
                    {{ roomType }}
                  </el-radio-button>
                </el-radio-group>
              </el-card>

              <el-card v-if="selectedTemplateRoomType" shadow="never">
                <template #header>
                  <div class="card-header">
                    <span>{{ selectedTemplateRoomType }} 默认设施</span>
                  </div>
                </template>
                
                <div class="facilities-grid-compact">
                  <div 
                    v-for="facility in currentFacilityOptions" 
                    :key="facility.id"
                    class="facility-item"
                    :class="{ 'facility-checked': templateSelectedFacilities.includes(facility.id) }"
                    @click="toggleTemplateFacility(facility.id)"
                  >
                    <el-checkbox
                      :model-value="templateSelectedFacilities.includes(facility.id)"
                      @click.stop
                      @change="toggleTemplateFacility(facility.id)"
                    >
                      {{ facility.label }}
                    </el-checkbox>
                  </div>
                </div>
                
                <el-text size="small" type="info" style="margin-top: 12px; display: block;">
                  提示：勾选的设施将作为该房型的默认配置，在物资配置首次加载时自动填充。
                </el-text>
                
                <div style="margin-top: 20px;">
                  <el-button type="primary" @click="saveRoomTypeTemplate">保存配置</el-button>
                </div>
              </el-card>

              <el-card v-else shadow="never" class="empty-tip-card">
                <el-empty description="">
                  <template #image>
                    <el-icon :size="80" color="#909399"><OfficeBuilding /></el-icon>
                  </template>
                  <template #description>
                    <div class="empty-tip-text">
                      <p style="font-size: 16px; color: #606266; margin: 10px 0;">请先选择房型</p>
                      <p style="font-size: 14px; color: #909399; margin: 0;">选择一个房型，即可配置该房型的默认设施</p>
                    </div>
                  </template>
                </el-empty>
              </el-card>
            </div>

            <!-- 设施列表 -->
            <div v-show="facilitySubTab === 'list'">
              <el-card shadow="never">
                <template #header>
                  <div class="card-header">
                    <span>设施列表 ({{ currentFacilityOptions.length }}个)</span>
                    <el-button type="primary" size="small" @click="showAddFacilityDialog">
                      <el-icon><Plus /></el-icon> 添加设施
                    </el-button>
                  </div>
                </template>

                <el-table :data="currentFacilityOptions" border stripe>
                  <el-table-column type="index" label="#" width="60" align="center" />
                  <el-table-column prop="label" label="设施名称" min-width="200" />
                  <el-table-column label="操作" width="180" align="center">
                    <template #default="{ row }">
                      <el-button size="small" @click="editFacilityItem(row)">编辑</el-button>
                      <el-popconfirm
                        title="确定删除此设施吗？已使用的房间不受影响。"
                        @confirm="deleteFacilityItem(row.key)"
                      >
                        <template #reference>
                          <el-button size="small" type="danger">删除</el-button>
                        </template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 添加/编辑用品对话框 -->
    <el-dialog
      v-model="addMaterialDialog"
      :title="editingMaterial ? '编辑用品' : '添加用品'"
      width="500px"
    >
      <el-form :model="materialForm" :rules="materialRules" ref="materialFormRef" label-width="80px">
        <el-form-item label="用品名称" prop="name">
          <el-input v-model="materialForm.name" placeholder="请输入用品名称" />
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="materialForm.quantity" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="materialForm.unit" placeholder="如：张、个、台" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMaterialDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMaterial">确定</el-button>
      </template>
    </el-dialog>

    <!-- 复制配置对话框 -->
    <el-dialog v-model="copyDialog" title="复制其他楼栋配置" width="500px">
      <el-form label-width="100px">
        <el-form-item label="源楼栋">
          <el-select v-model="copySource.building" placeholder="请选择源楼栋" style="width: 100%">
            <el-option
              v-for="building in buildingList"
              :key="building.name"
              :label="building.name"
              :value="building.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="源房型">
          <el-select v-model="copySource.roomType" placeholder="请选择源房型" style="width: 100%">
            <el-option v-for="type in roomTypeOptions" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCopy">确定复制</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑设施对话框 -->
    <el-dialog 
      v-model="facilityDialogVisible" 
      :title="facilityDialogTitle"
      width="500px"
    >
      <el-form :model="facilityForm" label-width="100px">
        <el-form-item label="设施名称" required>
          <el-input 
            v-model="facilityForm.label" 
            placeholder="例如：饮水机、空调、书桌"
            maxlength="10"
            show-word-limit
            clearable
          />
          <el-text size="small" type="info" style="margin-top: 8px; display: block;">
            添加后可在房间模板和批量编辑中使用
          </el-text>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="facilityDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFacilityItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAllMaterialTemplatesAPI,
  getMaterialTemplateByTypeAPI,
  createMaterialTemplateAPI,
  updateMaterialTemplateAPI,
  getAllBuildingsAPI
} from '@/api/config'
import {
  getAllFacilitiesAPI,
  addFacilityAPI,
  updateFacilityAPI,
  deleteFacilityAPI,
  getFacilitiesByRoomTypeAPI,
  saveRoomTypeTemplateAPI
} from '@/api/facility'
// 已移除字典表依赖
import { 
  Plus, OfficeBuilding, Box, CopyDocument, InfoFilled, Setting, Tickets
} from '@element-plus/icons-vue'

export default {
  name: 'ConfigManagement',
  inject: ['logOperation'],
  components: {
    Plus, OfficeBuilding, Box, CopyDocument, InfoFilled, Setting, Tickets
  },
  data() {
    return {
      activeTab: 'materials',
      
      // 物资配置相关
      buildingList: [],
      selectedBuilding: '',
      selectedRoomType: '',
      roomTypeOptions: [],
      currentTemplate: [],
      addMaterialDialog: false,
      editingMaterial: null,
      materialForm: {
        name: '',
        category: '非固定设施',
        quantity: 1,
        unit: '个'
      },
      materialRules: {
        name: [{ required: true, message: '请输入用品名称', trigger: 'blur' }],
        category: [{ required: true, message: '请选择类别', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
        unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
      },
      copyDialog: false,
      copySource: {
        building: '',
        roomType: ''
      },
      
      // 设施管理相关
      currentFacilityOptions: [],
      facilitySubTab: 'template',
      selectedTemplateRoomType: '',
      templateSelectedFacilities: [],
      
      // 设施管理
      facilityDialogVisible: false,
      facilityDialogTitle: '添加设施',
      facilityForm: {
        label: ''
      },
      editingFacilityId: null
    }
  },
  async mounted() {
    // 恢复上次的标签页状态
    const savedTab = localStorage.getItem('configManagementActiveTab')
    if (savedTab) {
      this.activeTab = savedTab
    }
    
    await this.loadBuildingList()
    await this.loadRoomTypes()
    await this.initFacilityOptions()
  },
  watch: {
    activeTab(newVal) {
      localStorage.setItem('configManagementActiveTab', newVal)
    }
  },
  methods: {
    // ========== 通用方法 ==========
    
    // 加载楼栋列表
    async loadBuildingList() {
      try {
        const response = await getAllBuildingsAPI()
        if (response.code === 200) {
          this.buildingList = response.data.map(b => ({
            id: b.id,
            name: b.buildingName,
            type: b.buildingType
          }))
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
      }
    },
    
    // 加载房间类型（硬编码）
    // 统一使用中文数字格式：二人间、四人间、六人间、八人间
    loadRoomTypes() {
      this.roomTypeOptions = ['二人间', '四人间', '六人间', '八人间']
    },
    
    // 初始化设施选项
    async initFacilityOptions() {
      try {
        const response = await getAllFacilitiesAPI()
        if (response.code === 200) {
          this.currentFacilityOptions = response.data.map(item => ({
            id: item.id,
            key: item.facilityName,
            label: item.facilityName
          }))
        }
      } catch (error) {
        console.error('加载设施选项失败，使用默认值', error)
        // 统一使用中文 key
        this.currentFacilityOptions = [
          { key: '空调', label: '空调' },
          { key: '洗衣机', label: '洗衣机' },
          { key: '饮水机', label: '饮水机' },
          { key: '风扇', label: '风扇' },
          { key: '书桌', label: '书桌' },
          { key: '椅子', label: '椅子' },
          { key: '衣柜', label: '衣柜' },
          { key: '独立卫浴', label: '独立卫浴' },
          { key: '阳台', label: '阳台' }
        ]
      }
    },
    
    // ========== 物资配置相关方法 ==========
    
    onBuildingChange() {
      this.selectedRoomType = ''
      this.currentTemplate = []
    },
    
    async onRoomTypeChange() {
      await this.loadTemplate()
    },
    
    async loadTemplate() {
      if (this.selectedBuilding && this.selectedRoomType) {
        const building = this.buildingList.find(b => b.name === this.selectedBuilding)
        if (!building) {
          this.$message.warning('未找到楼栋信息')
          return
        }
        
        // 1. 尝试从material_template加载已保存的配置（按buildingId查询）
        let savedMaterials = []
        let hasSavedConfig = false
        try {
          const response = await getMaterialTemplateByTypeAPI(building.id, building.type, this.selectedRoomType)
          
          if (response.code === 200 && response.data) {
            try {
              savedMaterials = JSON.parse(response.data.materials || '[]')
              hasSavedConfig = true // 标记已有保存的配置
            } catch (e) {
              console.error('解析物资列表失败:', e)
            }
          }
        } catch (error) {
          console.log('该楼栋房型暂无配置')
        }
        
        // 2. 如果已有保存的配置，直接使用
        if (hasSavedConfig && savedMaterials.length > 0) {
          this.currentTemplate = savedMaterials
          return
        }
        
        // 3. 首次配置：从房型默认模板获取设施
        try {
          const templateResponse = await getFacilitiesByRoomTypeAPI(this.selectedRoomType)
          if (templateResponse.code === 200 && templateResponse.data.length > 0) {
            this.currentTemplate = templateResponse.data.map(f => ({
              id: `固定设施_${f.facilityName}`,
              name: f.facilityName,
              category: '固定设施',
              quantity: f.defaultQuantity || 1,
              unit: f.unit || '个'
            }))
            return
          }
        } catch (error) {
          console.log('获取房型默认模板失败，使用空配置')
        }
        
        // 4. 如果没有配置默认模板，显示空列表
        this.currentTemplate = []
        this.$message.info('该房型暂无默认模板，请在"设施管理-房型默认模板"中配置，或手动添加物资')
      }
    },
    
    getCategoryType(category) {
      const types = {
        '固定设施': 'danger',
        '非固定设施': 'success'
      }
      return types[category] || ''
    },
    
    editMaterial(row) {
      this.editingMaterial = row
      this.materialForm = { ...row }
      this.addMaterialDialog = true
    },
    
    deleteMaterial(row) {
      this.$confirm(`确定要删除"${row.name}"吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const index = this.currentTemplate.findIndex(item => item.id === row.id)
        if (index > -1) {
          this.currentTemplate.splice(index, 1)
          this.$message.success(`已删除"${row.name}"`)
        }
      }).catch(() => {})
    },
    
    saveMaterial() {
      this.$refs.materialFormRef.validate((valid) => {
        if (valid) {
          const existingItem = this.currentTemplate.find(item => 
            item.name === this.materialForm.name && 
            (!this.editingMaterial || item.id !== this.editingMaterial.id)
          )
          
          if (existingItem) {
            this.$message.error(`物品"${this.materialForm.name}"已存在（${existingItem.category}），不能重复添加`)
            return
          }
          
          if (this.editingMaterial) {
            const index = this.currentTemplate.findIndex(item => item.id === this.editingMaterial.id)
            if (index > -1) {
              this.currentTemplate[index] = { 
                ...this.currentTemplate[index],
                ...this.materialForm 
              }
              this.$message.success(`已修改"${this.materialForm.name}"`)
            }
          } else {
            // 使用"类别_名称"作为ID，确保唯一性
            const newId = `${this.materialForm.category}_${this.materialForm.name}`
            this.currentTemplate.push({
              id: newId,
              ...this.materialForm
            })
            this.$message.success(`已添加"${this.materialForm.name}"`)
          }
          
          this.addMaterialDialog = false
          this.resetMaterialForm()
          // 不再自动保存，等用户点击"保存配置"按钮
        }
      })
    },
    
    resetMaterialForm() {
      this.editingMaterial = null
      this.materialForm = {
        name: '',
        category: '非固定设施',
        quantity: 1,
        unit: '个'
      }
      if (this.$refs.materialFormRef) {
        this.$refs.materialFormRef.resetFields()
      }
    },
    
    async saveCurrentTemplate() {
      if (!this.selectedBuilding || !this.selectedRoomType) {
        this.$message.warning('请先选择楼栋和房型！')
        return
      }
      
      try {
        const building = this.buildingList.find(b => b.name === this.selectedBuilding)
        if (!building) {
          this.$message.error('未找到楼栋信息')
          return
        }
        
        const allItems = this.currentTemplate.map(item => ({
          id: item.id,
          name: item.name,
          category: item.category,
          quantity: item.quantity,
          unit: item.unit
        }))
        
        const templateData = {
          templateName: `${this.selectedBuilding}-${this.selectedRoomType}`,
          buildingId: building.id,  // 使用楼栋ID实现隔离
          buildingType: building.type,
          roomType: this.selectedRoomType,
          materials: JSON.stringify(allItems)
        }
        
        try {
          // 按buildingId查询是否已存在配置
          const existingResponse = await getMaterialTemplateByTypeAPI(building.id, building.type, this.selectedRoomType)
          
          if (existingResponse.code === 200 && existingResponse.data) {
            templateData.id = existingResponse.data.id
            const response = await updateMaterialTemplateAPI(templateData)
            if (response.code === 200) {
              this.$message.success('配置更新成功！')
              this.logOperation(
                `更新用品模板: ${this.selectedBuilding} - ${this.selectedRoomType}`,
                '用品模板配置'
              )
            }
          } else {
            // 不存在则创建新配置
            const response = await createMaterialTemplateAPI(templateData)
            if (response.code === 200) {
              this.$message.success('配置保存成功！')
              this.logOperation(
                `创建用品模板: ${this.selectedBuilding} - ${this.selectedRoomType}`,
                '用品模板配置'
              )
            }
          }
        } catch (error) {
          // 异常时尝试创建
          const response = await createMaterialTemplateAPI(templateData)
          if (response.code === 200) {
            this.$message.success('配置保存成功！')
            this.logOperation(
              `创建用品模板: ${this.selectedBuilding} - ${this.selectedRoomType}`,
              '用品模板配置'
            )
          }
        }
        
      } catch (error) {
        console.error('保存模板失败:', error)
        this.$message.error('保存失败：' + (error.message || '未知错误'))
      }
    },
    
    async resetTemplate() {
      this.$confirm('确定要重置配置吗？将放弃未保存的修改', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await this.loadTemplate()
          this.$message.success('已重置！已从数据库重新加载配置')
        } catch (error) {
          console.error('重置失败:', error)
          this.$message.error('重置失败：' + (error.message || '未知错误'))
        }
      })
    },
    
    copyFromOther() {
      if (!this.selectedBuilding || !this.selectedRoomType) {
        this.$message.warning('请先选择目标楼栋和房型！')
        return
      }
      this.copyDialog = true
    },
    
    async confirmCopy() {
      if (!this.copySource.building || !this.copySource.roomType) {
        this.$message.warning('请选择源楼栋和房型！')
        return
      }
      
      const fromBuilding = this.buildingList.find(b => b.name === this.copySource.building)
      const toBuilding = this.buildingList.find(b => b.name === this.selectedBuilding)
      
      if (!fromBuilding || !toBuilding) {
        this.$message.error('楼栋信息错误！')
        return
      }
      
      try {
        // 使用 buildingId 查询源配置（实现楼栋隔离）
        const response = await getMaterialTemplateByTypeAPI(fromBuilding.id, fromBuilding.type, this.copySource.roomType)
        
        if (response.code === 200 && response.data) {
          const materials = JSON.parse(response.data.materials || '[]')
          this.currentTemplate = JSON.parse(JSON.stringify(materials))
          await this.saveCurrentTemplate()
          
          this.$message.success('复制成功！')
          this.copyDialog = false
          this.copySource = { building: '', roomType: '' }
          this.logOperation(
            `复制配置: ${this.copySource.building}-${this.copySource.roomType} → ${this.selectedBuilding}-${this.selectedRoomType}`,
            '用品模板配置'
          )
        } else {
          this.$message.error('源配置不存在！')
        }
      } catch (error) {
        console.error('复制配置失败:', error)
        this.$message.error('复制失败：' + (error.message || '未知错误'))
      }
    },
    
    // ========== 房型默认模板 ==========
    
    async onTemplateRoomTypeChange() {
      if (!this.selectedTemplateRoomType) {
        this.templateSelectedFacilities = []
        return
      }
      
      try {
        const response = await getFacilitiesByRoomTypeAPI(this.selectedTemplateRoomType)
        if (response.code === 200) {
          this.templateSelectedFacilities = response.data.map(f => f.id)
        }
      } catch (error) {
        console.error('加载房型默认设施失败:', error)
        this.templateSelectedFacilities = []
      }
    },
    
    toggleTemplateFacility(facilityId) {
      const index = this.templateSelectedFacilities.indexOf(facilityId)
      if (index > -1) {
        this.templateSelectedFacilities.splice(index, 1)
      } else {
        this.templateSelectedFacilities.push(facilityId)
      }
    },
    
    async saveRoomTypeTemplate() {
      if (!this.selectedTemplateRoomType) {
        this.$message.warning('请先选择房型')
        return
      }
      
      try {
        const response = await saveRoomTypeTemplateAPI(
          this.selectedTemplateRoomType, 
          this.templateSelectedFacilities
        )
        if (response.code === 200) {
          this.$message.success(`${this.selectedTemplateRoomType}默认模板保存成功！`)
          this.logOperation(`保存房型默认模板: ${this.selectedTemplateRoomType}`, '设施管理')
        }
      } catch (error) {
        console.error('保存房型默认模板失败:', error)
        this.$message.error('保存失败：' + (error.message || '未知错误'))
      }
    },
    
    // ========== 设施管理 ==========
    
    showAddFacilityDialog() {
      this.facilityDialogTitle = '添加设施'
      this.editingFacilityId = null
      this.facilityForm = {
        label: ''
      }
      this.facilityDialogVisible = true
    },
    
    editFacilityItem(facility) {
      this.facilityDialogTitle = '编辑设施'
      this.editingFacilityId = facility.id
      this.facilityForm = {
        label: facility.label
      }
      this.facilityDialogVisible = true
    },
    
    async saveFacilityItem() {
      if (!this.facilityForm.label || !this.facilityForm.label.trim()) {
        this.$message.warning('请输入设施名称')
        return
      }
      
      try {
        if (this.editingFacilityId) {
          const data = {
            id: this.editingFacilityId,
            facilityName: this.facilityForm.label.trim(),
            status: 1
          }
          const response = await updateFacilityAPI(data)
          if (response.code === 200) {
            this.$message.success('设施更新成功')
            this.logOperation(`编辑设施: ${this.facilityForm.label}`, '房间配置')
            await this.initFacilityOptions()
          }
        } else {
          const data = {
            facilityName: this.facilityForm.label.trim(),
            defaultQuantity: 1,
            unit: '个',
            status: 1
          }
          const response = await addFacilityAPI(data)
          if (response.code === 200) {
            this.$message.success('设施添加成功')
            this.logOperation(`添加设施: ${this.facilityForm.label}`, '房间配置')
            await this.initFacilityOptions()
          } else {
            this.$message.error(response.message || '添加失败')
            return
          }
        }
        
        this.facilityDialogVisible = false
      } catch (error) {
        console.error('保存设施失败:', error)
        this.$message.error('保存失败：' + (error.message || '网络错误'))
      }
    },
    
    async deleteFacilityItem(key) {
      try {
        const facility = this.currentFacilityOptions.find(f => f.key === key)
        if (!facility) {
          this.$message.error('设施不存在')
          return
        }
        
        const response = await deleteFacilityAPI(facility.id)
        if (response.code === 200) {
          this.$message.success('设施已删除')
          this.logOperation(`删除设施: ${facility.label}`, '房间配置')
          await this.initFacilityOptions()
        } else {
          this.$message.error(response.message || '删除失败')
        }
      } catch (error) {
        console.error('删除设施失败:', error)
        this.$message.error('删除失败：' + (error.message || '网络错误'))
      }
    }
  }
}
</script>

<style scoped>
.config-management-page {
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions h2 {
  margin: 0;
  color: #303133;
  font-size: 20px;
  font-weight: 600;
}

.materials-config,
.room-config {
  padding: 10px 0;
}

.config-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background-color: #ecf5ff;
  border-left: 4px solid #409eff;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}

.config-tip .el-icon {
  color: #409eff;
  font-size: 16px;
}

.filter-card {
  margin-bottom: 20px;
  background-color: #f8f9fa;
}

.filter-form {
  margin: 0;
}

.template-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.save-actions {
  margin-top: 20px;
  text-align: right;
}

.sub-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  border-bottom: 2px solid #e4e7ed;
}

.sub-tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  color: #606266;
  background-color: transparent;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s;
  user-select: none;
}

.sub-tab-item:hover {
  color: #409eff;
  background-color: #ecf5ff;
}

.sub-tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  background-color: #f0f9ff;
}

.sub-tab-item .el-icon {
  font-size: 18px;
}

.facilities-grid-compact {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.facility-item {
  padding: 8px 10px;
  background-color: white;
  border-radius: 6px;
  border: 1.5px solid #e4e7ed;
  transition: all 0.2s;
  cursor: pointer;
  user-select: none;
}

.facility-item:hover {
  border-color: #409eff;
  box-shadow: 0 1px 4px rgba(64, 158, 255, 0.15);
  transform: translateY(-1px);
}

.facility-item.facility-checked {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.facility-item :deep(.el-checkbox) {
  margin-right: 0 !important;
  width: 100%;
}

.facility-item :deep(.el-checkbox__label) {
  font-size: 13px;
  padding-left: 4px;
}

:deep(.el-radio-button) {
  margin-right: 8px;
  margin-bottom: 8px;
}

.empty-tip-card {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #fafbfc 100%);
  border: 2px dashed #dcdfe6;
}

.empty-tip-text {
  margin-top: 20px;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-table th) {
  background-color: #f5f7fa !important;
  color: #606266;
  font-weight: 600;
  font-size: 14px;
  padding: 12px 0;
}

:deep(.el-table td) {
  padding: 12px 0;
  font-size: 14px;
}

@media (max-width: 768px) {
  .config-management-page {
    padding: 10px;
  }
  
  .filter-form .el-form-item {
    display: block;
    margin-bottom: 10px;
  }
}
</style>

