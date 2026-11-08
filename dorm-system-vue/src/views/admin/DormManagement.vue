<template>
  <div class="admin-dorm-page">
    <el-card>
      <template #header>
        <h2>宿舍管理</h2>
      </template>
      
      <!-- 楼栋管理 -->
      <div class="building-section">
        <div class="section-header">
          <h3>我管理的楼栋</h3>
          <div class="header-buttons">
            <el-button type="primary" @click="showAreaManagementDialog" :disabled="managedBuildings.length === 0">
              <el-icon><Grid /></el-icon>
              楼层区域管理
            </el-button>
          </div>
        </div>
        <div class="buildings-compact-grid">
          <div 
            v-for="building in buildingStats" 
            :key="building.id"
            class="building-card-compact"
            @click="selectBuilding(building)"
            :class="{ 'selected': selectedBuildingId === building.id }"
          >
            <!-- 紧凑式玻璃卡片 -->
            <div class="compact-glass-container">
              <!-- 左侧图标和基本信息 -->
              <div class="left-section">
                <div class="building-icon-compact">
                  <OfficeBuilding />
                </div>
                <div class="building-basic-info">
                  <h4 class="building-name-compact">{{ building.name || '未命名楼栋' }}</h4>
                  <span class="building-type-compact" :class="building.gender === '男' ? 'male' : 'female'">
                    {{ building.gender }}生楼
                  </span>
                </div>
              </div>

              <!-- 中间统计数据 -->
              <div class="middle-section">
                <div class="stat-compact">
                  <div class="stat-icon-mini">
                    <HomeFilled />
                  </div>
                  <div class="stat-value">{{ building.totalRooms || 0 }}</div>
                  <div class="stat-unit">间</div>
                </div>
                <div class="stat-compact">
                  <div class="stat-icon-mini">
                    <User />
                  </div>
                  <div class="stat-value">{{ building.occupiedBeds || 0 }}</div>
                  <div class="stat-unit">人</div>
                </div>
                <div class="stat-compact">
                  <div class="stat-icon-mini">
                    <Reading />
                  </div>
                  <div class="stat-value">{{ building.occupancyRate || 0 }}%</div>
                  <div class="stat-unit">入住率</div>
                </div>
              </div>

              <!-- 右侧进度条和状态 -->
              <div class="right-section">
                <div class="progress-section">
                  <div class="progress-bar-mini">
                    <div 
                      class="progress-fill-mini" 
                      :style="{ width: (building.occupancyRate || 0) + '%' }"
                      :class="{
                        'low': (building.occupancyRate || 0) < 60,
                        'medium': (building.occupancyRate || 0) >= 60 && (building.occupancyRate || 0) < 85,
                        'high': (building.occupancyRate || 0) >= 85
                      }"
                    ></div>
                  </div>
                  <div class="status-text-mini">
                    {{ (building.occupancyRate || 0) < 60 ? '空房多' : (building.occupancyRate || 0) < 85 ? '良好' : (building.occupancyRate || 0) < 100 ? '较满' : '满员' }}
                  </div>
                </div>
                <div class="selection-indicator-compact">
                  <div class="indicator-dot-mini"></div>
                </div>
              </div>

              <!-- 描述信息（可选，紧凑显示） -->
              <div class="description-compact" v-if="building.description">
                {{ building.description }}
              </div>

              <!-- 镜面反射效果 -->
              <div class="mirror-effect-compact"></div>
            </div>
          </div>
        </div>
        
        <!-- 无楼栋提示 -->
        <div v-if="managedBuildings.length === 0" class="no-buildings">
          <el-empty description="您还没有被分配管理楼栋">
            <p class="hint-text">请联系超级管理员为您分配楼栋管理权限</p>
          </el-empty>
        </div>
      </div>
      
      <!-- 宿舍列表 -->
      <div class="dorm-list">
        <div class="section-header">
          <h3>宿舍列表</h3>
          <div class="action-buttons">
          </div>
        </div>
        
        <!-- 未选择楼栋提示 -->
        <div v-if="!selectedBuildingId && managedBuildings.length > 0" class="no-building-selected">
          <el-empty description="">
            <template #image>
              <el-icon :size="80" color="#409eff">
                <OfficeBuilding />
              </el-icon>
            </template>
            <template #description>
              <h3 style="color: #606266; margin-bottom: 12px;">请先选择要管理的楼栋</h3>
              <p style="color: #909399; font-size: 14px;">👆 点击上方楼栋卡片选择要管理的楼栋，然后即可查看该楼栋的宿舍列表</p>
            </template>
          </el-empty>
        </div>
        
        <div v-if="selectedBuildingId" class="search-bar">
          <el-input v-model="searchKeyword" placeholder="搜索宿舍号" style="width: 200px;" />
        </div>
        
        <el-table v-if="selectedBuildingId" :data="paginatedDorms" style="width: 100%">
          <el-table-column prop="building" label="楼栋" align="center" />
          <el-table-column prop="room" label="房间号" align="center" />
          <el-table-column prop="type" label="类型" align="center" />
          <el-table-column prop="totalBeds" label="总床位数" align="center" />
          <el-table-column prop="occupiedBeds" label="已住人数" align="center" />
          <el-table-column label="归属对象" align="center">
            <template #default="scope">
              <el-tag 
                v-if="isRoomAssigned(scope.row)" 
                type="info"
                :title="getRoomBelongToText(scope.row.belongTo)"
              >
                {{ scope.row.belongTo.className || scope.row.belongTo.majorName }}
              </el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
                     <el-table-column label="操作" width="120" align="center">
             <template #default="scope">
               <div class="action-buttons-cell">
                 <el-button size="small" type="primary" @click="viewDetails(scope.row)">查看详情</el-button>
               </div>
             </template>
           </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div v-if="selectedBuildingId" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[8, 16, 32]"
            :total="filteredDorms.length"
            :pager-count="5"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 楼栋管理对话框 -->
    <el-dialog v-model="buildingDialogVisible" :title="buildingDialogTitle" width="600px">
      <el-form :model="buildingForm" label-width="100px">
        <el-form-item label="楼栋名称" required>
          <el-input v-model="buildingForm.name" placeholder="请输入楼栋名称" />
        </el-form-item>
        <el-form-item label="楼层数" required>
          <el-input-number v-model="buildingForm.floors" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="buildingForm.description" type="textarea" placeholder="请输入楼栋描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="buildingDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveBuilding">保存</el-button>
        </span>
      </template>
    </el-dialog>

         <!-- 宿舍人员名单对话框 -->
     <el-dialog v-model="detailDialogVisible" :title="`${currentDorm?.building}${currentDorm?.room} - 人员名单`" width="800px">
       <div v-if="currentDorm" class="dorm-residents-dialog">
         <!-- 宿舍基本信息 -->
         <el-card class="dorm-info-card" shadow="never">
           <template #header>
             <div class="dorm-info-header">
               <span>宿舍信息</span>
               <el-button type="primary" size="small" @click="exportResidentsList">导出名单</el-button>
             </div>
           </template>
           <el-row :gutter="24" justify="center">
             <el-col :span="8">
               <div class="info-item">
                 <label>楼栋：</label>
                 <span>{{ currentDorm.building }}</span>
               </div>
             </el-col>
             <el-col :span="8">
               <div class="info-item">
                 <label>房间号：</label>
                 <span>{{ currentDorm.room }}</span>
               </div>
             </el-col>
             <el-col :span="8">
               <div class="info-item">
                 <label>类型：</label>
                 <span>{{ currentDorm.type }}</span>
               </div>
             </el-col>
           </el-row>
         </el-card>
         
         <!-- 人员名单表格 -->
        <el-card class="residents-table-card" shadow="never">
          <template #header>
            <span>人员名单 ({{ currentDorm.residents?.length || 0 }}/{{ currentDorm.totalBeds }})</span>
          </template>
          <el-table :data="currentDorm.residents || []" style="width: 100%" size="small">
              <el-table-column prop="name" label="姓名" width="100" />
              <el-table-column prop="studentId" label="学号" width="120" />
              <el-table-column prop="major" label="专业" />
              <el-table-column prop="className" label="班级" width="120" />
              <el-table-column prop="email" label="邮箱" width="200" show-overflow-tooltip />
              <el-table-column prop="bedNumber" label="床位号" width="80" align="center" />
            </el-table>
         </el-card>
       </div>
     </el-dialog>


    <!-- 楼层区域管理对话框 -->
    <el-dialog 
      v-model="areaManagementDialogVisible" 
      title="楼层区域管理" 
      width="95%"
      top="3vh"
      :close-on-click-modal="false"
      :modal-append-to-body="false"
      custom-class="area-management-dialog"
    >
      <div v-if="selectedBuildingId" class="area-management-content">
        <!-- 楼栋信息 -->
        <el-card class="building-info-card" shadow="never">
          <template #header>
            <div class="building-info-header">
              <span>{{ selectedBuildingName }} - 楼层区域划分</span>
              <el-button type="primary" size="small" @click="showAddAreaDialog">
                <el-icon><Plus /></el-icon>
                添加区域
              </el-button>
            </div>
          </template>
          <div class="building-details">
            <el-tag :type="selectedBuilding?.gender === '男' ? 'primary' : 'danger'">
              {{ selectedBuilding?.gender }}生楼栋
            </el-tag>
            <span class="building-desc">{{ selectedBuilding?.description }}</span>
          </div>
        </el-card>

        <!-- 区域列表 -->
        <div class="areas-container" :key="`areas-${areaUpdateCounter}`">
          <div v-if="buildingAreas.length > 0" class="areas-grid">
            <el-card 
              v-for="area in buildingAreas" 
              :key="`area-${area.id}-${areaUpdateCounter}`" 
              class="area-card"
              shadow="hover"
            >
              <template #header>
                <div class="area-card-header">
                  <span class="area-name">{{ area.name }}</span>
                  <div class="area-actions">
                    <el-button type="primary" size="small" @click="editArea(area)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button type="danger" size="small" @click="deleteArea(area)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>

              <div class="area-info-premium">
                <!-- 楼层信息 -->
                <div class="info-block floors-block">
                  <div class="block-header">
                    <div class="header-icon floors-icon">
                      <el-icon><OfficeBuilding /></el-icon>
                  </div>
                    <span class="block-title">楼层</span>
                  </div>
                  <div class="floors-container">
                    <div 
                      v-for="floor in area.floors.sort((a, b) => a - b)" 
                      :key="floor"
                      class="floor-chip"
                    >
                      {{ floor }}
                    </div>
                  </div>
                </div>
                
                <!-- 专业信息（基于实际分配数据） -->
                <div class="info-block majors-block" v-if="getAreaAllocationData(area).majors.length > 0">
                  <div class="block-header">
                    <div class="header-icon majors-icon">
                      <el-icon><School /></el-icon>
                  </div>
                    <span class="block-title">专业</span>
                    <div class="count-badge">{{ getAreaAllocationData(area).majors.length }}</div>
                  </div>
                  <div class="majors-container" :title="getAreaAllocationData(area).majors.length > 1 ? '鼠标悬停查看详情' : ''">
                    <div 
                      v-for="major in getAreaAllocationData(area).majors.slice(0, 1)" 
                      :key="major.majorName"
                      class="major-chip"
                      :class="{ 'class-mode': major.mode === 'class-only' }"
                      :title="major.majorName"
                    >
                      <span class="major-name">{{ major.majorName }}</span>
                      <div class="mode-indicator" :class="major.mode === 'class-only' ? 'class-only' : 'full-major'" style="display: none;">
                        {{ major.mode === 'class-only' ? '班' : '专' }}
                      </div>
                    </div>
                    <div 
                      v-if="getAreaAllocationData(area).majors.length > 1"
                      class="more-chip view-more"
                      @click="showAreaAllocationDetails(area)"
                      title="点击查看全部专业详情"
                    >
                      <span>+{{ getAreaAllocationData(area).majors.length - 1 }}</span>
                      <el-icon class="more-icon"><More /></el-icon>
                    </div>
                  </div>
                </div>
                
                <!-- 班级信息（基于实际分配数据，限制显示数量） -->
                <div class="info-block classes-block" v-if="getAreaAllocationData(area).classes.length > 0">
                  <div class="block-header">
                    <div class="header-icon classes-icon">
                      <el-icon><UserFilled /></el-icon>
                  </div>
                    <span class="block-title">班级</span>
                    <div class="count-badge">{{ getAreaAllocationData(area).classes.length }}</div>
                  </div>
                  <div class="classes-container" :title="getAreaAllocationData(area).classes.length > 3 ? '鼠标悬停查看详情' : ''">
                    <div 
                      v-for="classInfo in getAreaAllocationData(area).classes.slice(0, 3)" 
                      :key="classInfo.classKey"
                      class="class-chip"
                      :title="`${classInfo.className} (${classInfo.majorName})`"
                    >
                      {{ classInfo.className }}
                    </div>
                    <div 
                      v-if="getAreaAllocationData(area).classes.length > 3"
                      class="more-chip view-more"
                      @click="showAreaAllocationDetails(area)"
                      title="点击查看全部班级详情"
                    >
                      <span>+{{ getAreaAllocationData(area).classes.length - 3 }}</span>
                      <el-icon class="more-icon"><More /></el-icon>
                    </div>
                  </div>
                </div>
                
                <!-- 空状态提示 -->
                <div v-if="getAreaAllocationData(area).majors.length === 0" class="empty-state-premium">
                  <div class="empty-icon">
                    <el-icon><InfoFilled /></el-icon>
                  </div>
                  <span class="empty-text">暂未分配专业班级</span>
                  </div>
                
                <!-- 底部统计信息 -->
                <div class="stats-premium">
                  <div class="stats-row">
                    <div class="stat-card">
                      <div class="stat-content">
                        <div class="stat-value">{{ getAreaRoomCount(area) }}</div>
                        <div class="stat-label">房间</div>
                      </div>
                    </div>
                    <div class="stat-card">
                      <div class="stat-content">
                        <div class="stat-value">{{ area.floors.length }}</div>
                        <div class="stat-label">楼层</div>
                      </div>
                    </div>
                    <div class="stat-card">
                      <div class="stat-content">
                        <div class="stat-value">{{ getAreaStudentCount(area) }}</div>
                        <div class="stat-label">学生</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
          
          <!-- 无区域状态 -->
          <div v-else class="no-areas">
            <el-empty description="该楼栋暂未设置区域划分">
              <el-button type="primary" @click="showAddAreaDialog">
                立即添加区域
              </el-button>
            </el-empty>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="areaManagementDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加/编辑区域对话框 -->
    <el-dialog 
      v-model="areaDialogVisible" 
      :title="areaDialogTitle" 
      width="1400px"
      top="3vh"
      :close-on-click-modal="false"
      custom-class="add-area-dialog"
      @close="resetAreaForm"
    >
      <el-form :model="areaForm" :rules="areaFormRules" ref="areaFormRef" label-width="100px">
        <el-form-item label="区域名称" prop="name">
          <el-input 
            ref="areaNameInput"
            v-model="areaForm.name" 
            placeholder="如：计算机学院区域" 
          />
        </el-form-item>
        
        <el-form-item label="楼层范围" prop="floors">
          <div class="floors-selector">
            <!-- 楼栋实际楼层提示 -->
            <div v-if="hasActualRoomData" class="floor-info-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>该楼栋共有 <strong>{{ currentBuildingActualFloors }}</strong> 层，灰色楼层为超出范围</span>
            </div>
            <div v-else-if="currentBuildingActualFloors > 0" class="floor-warning-tip">
              <el-icon><Warning /></el-icon>
              <span>该楼栋暂无房间数据，默认可选 <strong>{{ currentBuildingActualFloors }}</strong> 层，灰色楼层为预留扩展</span>
            </div>
            
            <div class="floors-grid" v-if="currentBuildingActualFloors > 0">
              <div 
              v-for="floor in currentBuildingMaxFloors" 
              :key="floor" 
                class="floor-option"
                :class="{ 
                  'selected': areaForm.floors.includes(floor),
                  'occupied': isFloorOccupied(floor),
                  'disabled': isFloorOccupied(floor) || floor > currentBuildingActualFloors,
                  'unavailable': floor > currentBuildingActualFloors
                }"
                @click="toggleFloor(floor)"
              >
                <div class="floor-number">{{ floor }}</div>
                <div class="floor-label">{{ floor }}层</div>
                <div v-if="isFloorOccupied(floor)" class="occupied-indicator">已分配</div>
              </div>
            </div>
            <div class="floors-summary" v-if="areaForm.floors && areaForm.floors.length > 0">
              <span class="summary-text">
                已选择 {{ areaForm.floors.length }} 个楼层：
                <span class="selected-floors">{{ areaForm.floors.sort((a, b) => a - b).join('、') }}层</span>
              </span>
              <el-button 
                    size="small"
                type="text" 
                @click="clearAllFloors"
                class="clear-btn"
                  >
                清空选择
                      </el-button>
                </div>
            
          </div>
        </el-form-item>
        
        <el-form-item label="分配管理">
          <div class="allocation-management-container">
            
            <!-- 顶部工具栏 -->
            <div class="management-header">
              <div class="header-left">
                <h4 class="section-title">专业班级分配</h4>
                <el-tag v-if="canAssignToCurrentTarget" type="info" size="small" class="current-target">
                  当前目标: {{ currentMajor?.majorName }}
                </el-tag>
              </div>
              <div class="header-actions">
                  <el-button 
                    size="small" 
                    type="primary" 
                    @click="onSyncAllocationScope" 
                    :disabled="!selectedBuildingId"
                  >
                    <el-icon><Refresh /></el-icon>
                    同步信息
                  </el-button>
          </div>
            </div>

            <div class="management-content">
              <!-- 左侧：分配范围 -->
              <div class="allocation-scope">
                <div v-if="professionalChips && professionalChips.length" class="scope-content">
                  <!-- 专业选择 -->
                  <div class="chips-section">
                    <div class="chips-label">
                      <div class="label-left">
                        <el-icon><School /></el-icon>
                        <span>选择专业/班级</span>
                      </div>
                      <div class="batch-mode-controls">
                        <el-switch
                          v-model="batchAssignmentMode"
                          size="small"
                          active-text="批量模式"
                          inline-prompt
                        />
                        <el-button
                          v-if="batchAssignmentMode"
                          type="primary"
                          size="small"
                          @click="toggleSelectAllClasses"
                        >
                          {{ isAllClassesSelected ? '取消全选' : '全选' }}
                        </el-button>
                      </div>
                    </div>
                    <div class="chips-grid">
                      <div 
                        v-for="m in professionalChips"
                        :key="m.classKey ? `${m.majorId || m.majorName}_${m.classKey}` : (m.majorId || m.majorName)"
                        class="allocation-chip"
                        :class="{ 
                          'active': currentMajorKey === (m.classKey ? `${m.majorId || m.majorName}_${m.classKey}` : (m.majorId || m.majorName)),
                          'over-allocated': m.remainingSeats < 0,
                          'selected-for-batch': batchAssignmentMode && isClassSelectedForBatch(m)
                        }"
                        @click="batchAssignmentMode ? toggleClassForBatch(m) : onSelectMajor(m)"
                      >
                        <!-- 批量模式下显示复选框 -->
                        <el-checkbox
                          v-if="batchAssignmentMode"
                          :model-value="isClassSelectedForBatch(m)"
                          @click.stop
                          @change="toggleClassForBatch(m)"
                          class="batch-checkbox"
                        />
                        <div class="chip-header">
                          <span class="chip-name">{{ m.majorName }}</span>
                        </div>
                        <div class="chip-stats">
                          <span class="stat-item">已用 <strong>{{ m.usedSeats }}</strong></span>
                          <span class="stat-divider">·</span>
                          <span class="stat-item">总计 <strong>{{ m.totalSeats }}</strong></span>
                          <span class="stat-divider">·</span>
                          <span class="stat-item remaining" :class="{ 'negative': m.remainingSeats < 0 }">
                            剩余 <strong>{{ m.remainingSeats }}</strong>
                          </span>
                        </div>
                        <div class="chip-progress">
                          <el-progress 
                            :percentage="Math.min(100, Math.round((m.usedSeats / m.totalSeats) * 100))"
                            :stroke-width="4"
                            :show-text="false"
                            :status="m.remainingSeats < 0 ? 'exception' : (m.remainingSeats === 0 ? 'success' : '')"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                </div>
                
                <el-empty 
                  v-else 
                  description="未获取到本楼的专业班级配额"
                  :image-size="80"
                  class="scope-empty"
                >
                  <template #description>
                    <p>未获取到本楼的专业班级配额</p>
                    <p class="empty-hint">请点击右上角"同步信息"按钮刷新数据</p>
              </template>
                </el-empty>
              </div>

              <!-- 右侧：房间分配 -->
              <div class="room-assignment">
                <div class="assignment-header">
                  <span class="assignment-title">房间分配管理</span>
                  <div class="assignment-actions">
                    <!-- 批量自动分配按钮 -->
                    <el-button
                      v-if="batchAssignmentMode && selectedClassesForBatch.length > 0"
                      type="success"
                      size="small"
                      :loading="batchAssigning"
                      @click="startBatchAutoAssignment"
                    >
                      <el-icon><MagicStick /></el-icon>
                      批量自动分配 ({{ selectedClassesForBatch.length }})
                    </el-button>
                    <el-tooltip 
                      content="勾选房间后，将选中的房间分配给当前专业/班级"
                      placement="top"
                      :disabled="selectedRoomIdsForAssign.length > 0"
                    >
                      <el-button 
                        v-if="!batchAssignmentMode"
                        type="primary" 
                        size="small"
                        :disabled="!canAssignToCurrentTarget || selectedRoomIdsForAssign.length === 0"
                        @click="assignSelectedRoomsToCurrentTarget"
                      >
                        <el-icon><Check /></el-icon>
                        分配选中房间 ({{ selectedRoomIdsForAssign.length }})
                      </el-button>
                    </el-tooltip>
                <el-button 
                  size="small" 
                      :disabled="selectedRoomIdsForAssign.length === 0"
                      @click="unassignSelectedRooms"
                    >
                      取消分配
                </el-button>
                    <el-dropdown @command="handleRoomCommand">
                      <el-button size="small" type="info">
                        更多操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="auto-fill" :disabled="!canAssignToCurrentTarget">
                              <el-icon><MagicStick /></el-icon>
                              一键自动分配
                            </el-dropdown-item>
                            <el-dropdown-item command="batch-unassign">
                              <el-icon><RefreshLeft /></el-icon>
                              撤回专业分配
                            </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
                
                <div class="room-table-container">
                  <!-- 楼层已选择时显示房间表格 -->
                  <el-table 
                    v-if="areaForm.floors && areaForm.floors.length > 0"
                    :data="roomsForAssignment" 
                    height="445"
                    @selection-change="onAssignRoomsSelectionChange"
                    stripe
                    size="small"
                    style="width: 100%"
                  >
                    <el-table-column type="selection" width="55" />
                    <el-table-column prop="room" label="房间号" min-width="120" />
                    <el-table-column prop="floor" label="楼层" width="100" align="center" />
                    <el-table-column prop="type" label="类型" min-width="120" />
                    <el-table-column prop="totalBeds" label="总床位" width="120" align="center" />
                    <el-table-column prop="occupiedBeds" label="已住" width="100" align="center" />
                    <el-table-column label="归属对象" min-width="180">
                      <template #default="scope">
                        <el-tag 
                          v-if="isRoomAssigned(scope.row)" 
                          type="primary" 
                          size="small" 
                          effect="light"
                          :title="getRoomBelongToText(scope.row.belongTo)"
                        >
                          {{ scope.row.belongTo.className || scope.row.belongTo.majorName }}
                        </el-tag>
                        <span v-else class="text-placeholder">未分配</span>
                      </template>
                    </el-table-column>
                  </el-table>
                  
                  <!-- 楼层未选择时显示空状态提示 -->
                  <el-empty 
                    v-else 
                    description="" 
                    :image-size="100"
                    class="floor-empty-state"
                  >
                    <template #image>
                      <el-icon :size="80" color="#909399">
                        <OfficeBuilding />
                      </el-icon>
                    </template>
                    <template #description>
                      <div style="color: #606266; font-size: 15px; margin-bottom: 8px;">请先选择楼层范围</div>
                      <div style="color: #909399; font-size: 13px;">👆 点击上方楼层选择器，选择要管理的楼层</div>
                    </template>
                  </el-empty>
                </div>
                
                <!-- 提示信息 -->
                <div v-if="!canAssignToCurrentTarget" class="assignment-hint">
                  <el-icon><InfoFilled /></el-icon>
                  <span>请先在左侧选择要分配的专业或班级</span>
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="areaDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveArea" :loading="areaSaving">
            {{ isAreaEditMode ? '更新' : '添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 未入住学生对话框 -->
    <el-dialog 
      v-model="unallocatedStudentsDialogVisible"
      :title="`未入住学生 - ${selectedBuildingName}`"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="unallocated-students-content">
        <div class="toolbar">
          <div>
            <el-input v-model="studentSearchKeyword" placeholder="搜索姓名/学号" clearable style="width: 220px; margin-right: 8px;" />
            <el-select v-model="selectedRoomForCheckin" placeholder="选择目标房间" clearable style="width: 220px;">
              <el-option 
                v-for="room in inScopeRoomsWithSpace" 
                :key="room.id" 
                :label="`${room.floor}楼-${room.room} (余${room.totalBeds - room.occupiedBeds})`" 
                :value="room.id"
              />
            </el-select>
          </div>
          <div class="quota-indicator">
            剩余名额：{{ currentTargetRemainingSeats }}
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div class="card-header">未入住（{{ filteredUnallocatedStudents.length }}）</div>
              </template>
              <el-table 
                :data="paginatedUnallocatedStudents"
                height="360"
                @selection-change="onSelectStudentsForCheckin"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column prop="studentId" label="学号" width="120" />
                <el-table-column prop="name" label="姓名" width="100" />
                <el-table-column prop="major" label="专业" />
                <el-table-column prop="className" label="班级" width="120" />
              </el-table>
              <div style="margin-top: 8px; display:flex; justify-content: space-between; align-items: center;">
                <el-pagination
                  v-model:current-page="currentUnallocPage"
                  v-model:page-size="unallocPageSize"
                  :total="filteredUnallocatedStudents.length"
                  :page-sizes="[10,20,50]"
                  layout="total, sizes, prev, pager, next"
                />
                <el-button type="primary" :disabled="selectedStudentsForCheckin.length===0 || !selectedRoomForCheckin" @click="batchCheckinToRoom">安排到房间</el-button>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="never">
              <template #header>
                <div class="card-header">已入住（范围内）</div>
              </template>
              <el-table 
                :data="paginatedAllocatedStudents"
                height="360"
                @selection-change="onSelectStudentsForCheckout"
              >
                <el-table-column type="selection" width="48" />
                <el-table-column prop="studentId" label="学号" width="120" />
                <el-table-column prop="name" label="姓名" width="100" />
                <el-table-column prop="major" label="专业" />
                <el-table-column prop="className" label="班级" width="120" />
                <el-table-column prop="roomLabel" label="房间" width="120" />
              </el-table>
              <div style="margin-top: 8px; display:flex; justify-content: space-between; align-items: center;">
                <el-pagination
                  v-model:current-page="currentAllocPage"
                  v-model:page-size="allocPageSize"
                  :total="inScopeAllocatedStudents.length"
                  :page-sizes="[10,20,50]"
                  layout="total, sizes, prev, pager, next"
                />
                <el-button type="warning" :disabled="selectedStudentsForCheckout.length===0" @click="batchCheckout">撤销入住</el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="unallocatedStudentsDialogVisible=false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 用品管理对话框 -->
    <el-dialog
      v-model="materialsDialogVisible"
      :title="`用品管理 - ${currentMaterialsRoom ? currentMaterialsRoom.building + '-' + currentMaterialsRoom.room : ''}`"
      width="900px"
    >
      <el-tabs v-model="materialTabActive">
        <!-- 用品清单标签页 -->
        <el-tab-pane label="用品清单" name="materials">
          <div class="materials-management">
            <el-alert 
              type="info" 
              :closable="false" 
              style="margin-bottom: 16px;"
              title="用品配置来自超级管理员端的物资配置，如需修改请联系超级管理员"
            />

            <el-table :data="roomMaterials" border style="width: 100%" v-loading="roomMaterialsLoading">
              <el-table-column prop="name" label="用品名称" min-width="140" show-overflow-tooltip />
              <el-table-column prop="category" label="类别" width="100">
                <template #default="{ row }">
                  <el-tag :type="getMaterialCategoryType(row.category)" size="small">{{ row.category }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="配置数量" width="120" align="center">
                <template #default="{ row }">
                  {{ row.quantity }}{{ row.unit }}
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="!roomMaterialsLoading && roomMaterials.length === 0" description="该楼栋房型暂未配置用品，请在超级管理员端配置" />
          </div>
        </el-tab-pane>

        <!-- 申报记录标签页 -->
        <el-tab-pane label="申报记录" name="reports">
          <div class="reports-management">
            <el-table :data="roomReports" border>
              <el-table-column prop="studentName" label="申报学生" width="100" />
              <el-table-column prop="materialName" label="用品名称" width="120" />
              <el-table-column prop="issueType" label="问题类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.issueType === '损坏' ? 'danger' : 'warning'">{{ row.issueType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="issue" label="问题描述" min-width="150" />
              <el-table-column prop="reportTime" label="申报时间" width="150">
                <template #default="{ row }">
                  {{ formatReportTime(row.reportTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="处理状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getReportStatusType(row.status)">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="handleReport(row)"
                    v-if="row.status !== '已处理'"
                  >
                    处理
                  </el-button>
                  <el-button 
                    type="info" 
                    size="small" 
                    @click="viewReportDetail(row)"
                    v-else
                  >
                    查看
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-if="roomReports.length === 0" description="暂无申报记录" />
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="materialsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑宿舍用品对话框 -->
    <el-dialog
      v-model="addRoomMaterialDialog"
      :title="editingRoomMaterial ? '编辑用品' : '添加用品'"
      width="500px"
    >
      <el-form :model="roomMaterialForm" :rules="roomMaterialRules" ref="roomMaterialFormRef" label-width="100px">
        <el-form-item label="用品名称" prop="name">
          <el-input v-model="roomMaterialForm.name" placeholder="请输入用品名称" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="roomMaterialForm.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="家具" value="家具" />
            <el-option label="电器" value="电器" />
            <el-option label="日用品" value="日用品" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-select v-model="roomMaterialForm.unit" placeholder="请选择单位" style="width: 100%">
            <el-option label="张" value="张" />
            <el-option label="把" value="把" />
            <el-option label="个" value="个" />
            <el-option label="台" value="台" />
            <el-option label="套" value="套" />
            <el-option label="件" value="件" />
            <el-option label="条" value="条" />
            <el-option label="块" value="块" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置数量" prop="allocatedQuantity">
          <el-input-number v-model="roomMaterialForm.allocatedQuantity" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实际数量" prop="actualQuantity">
          <el-input-number v-model="roomMaterialForm.actualQuantity" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="roomMaterialForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="正常" value="正常" />
            <el-option label="损坏" value="损坏" />
            <el-option label="缺失" value="缺失" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="roomMaterialForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRoomMaterialDialog = false">取消</el-button>
        <el-button type="primary" @click="saveRoomMaterial">确定</el-button>
      </template>
    </el-dialog>

    <!-- 处理申报对话框 -->
    <el-dialog
      v-model="handleReportDialog"
      :title="currentReport ? '处理申报' : '申报详情'"
      width="600px"
    >
      <el-form v-if="currentReport" label-width="100px">
        <el-form-item label="申报学生">
          <el-input v-model="currentReport.studentName" disabled />
        </el-form-item>
        <el-form-item label="用品名称">
          <el-input v-model="currentReport.materialName" disabled />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-tag :type="currentReport.issueType === '损坏' ? 'danger' : 'warning'">
            {{ currentReport.issueType }}
          </el-tag>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="currentReport.issue" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="申报时间">
          <span>{{ formatReportTime(currentReport.reportTime) }}</span>
        </el-form-item>
        <el-divider />
        <el-form-item label="处理状态" v-if="currentReport.status !== '已处理'">
          <el-select v-model="reportHandleForm.status" placeholder="请选择处理状态" style="width: 100%">
            <el-option label="处理中" value="处理中" />
            <el-option label="已处理" value="已处理" />
            <el-option label="已拒绝" value="已拒绝" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态" v-else>
          <el-tag type="success">已处理</el-tag>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input 
            v-model="reportHandleForm.handleRemark" 
            type="textarea" 
            :rows="3" 
            placeholder="请填写处理备注"
            :disabled="currentReport.status === '已处理'"
          />
        </el-form-item>
        <el-form-item label="已有备注" v-if="currentReport.handleRemark && currentReport.status === '已处理'">
          <div style="padding: 10px; background: #f5f7fa; border-radius: 4px;">
            {{ currentReport.handleRemark }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleReportDialog = false">
          {{ currentReport && currentReport.status === '已处理' ? '关闭' : '取消' }}
        </el-button>
        <el-button 
          type="primary" 
          @click="submitReportHandle" 
          v-if="currentReport && currentReport.status !== '已处理'"
        >
          提交处理
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script>
import { getCurrentUser } from '@/utils/auth.js'
import { 
  getAdminBuildings, 
  getBuildingAreas, 
  addAreaToBuilding, 
  updateBuildingArea, 
  removeAreaFromBuilding,
  getAllMajors,
  getAllClasses,
  getBuildingById
} from '@/utils/buildingManagement.js'
import { getBuildingAllocationScope } from '@/utils/allocationScope.js'
import { syncAdminData } from '@/utils/adminManagement.js'
import { 
  Grid, 
  Plus, 
  Edit, 
  Delete, 
  OfficeBuilding, 
  User, 
  Reading, 
  Notebook, 
  HomeFilled,
  Tools,
  Document,
  More,
  QuestionFilled,
  School,
  MagicStick,
  InfoFilled,
  RefreshLeft,
  Warning,
  Check
} from '@element-plus/icons-vue'

// 导入后端API
import { getAllBuildings, getBuildingDetail } from '@/api/building.js'
import { getRoomsByBuilding, getRoomDetail } from '@/api/room.js'
import { getMajorAssignments } from '@/api/majorAssignment.js'
import { getStudentList } from '@/api/student.js'
// 导入管理员宿舍管理API
import { 
  getMyBuildings, 
  getBuildingRooms, 
  getBuildingStatistics,
  getRoomResidents as getApiRoomResidents,
  batchAssignRooms,
  getBuildingAreas as getApiBuildingAreas,
  createArea,
  updateArea,
  deleteArea as deleteApiArea
} from '@/api/adminDormManagement.js'

// 导入组件

export default {
  name: 'AdminDormManagement',
  components: {
    Grid,
    Plus,
    Edit,
    Delete,
    OfficeBuilding,
    User,
    Reading,
    Notebook,
    HomeFilled,
    Tools,
    Document
  },
  data() {
    return {
      searchKeyword: '',
      filterBuilding: '',
      dormUpdateCounter: 0,
      managedBuildings: [], // 管理员负责的楼栋
      selectedBuildingId: null, // 当前选中的楼栋ID
      
      // 区域管理相关
      areaManagementDialogVisible: false,
      buildingAreas: [],
      areaDialogVisible: false,
      isAreaEditMode: false,
      editingAreaId: null,
      areaSaving: false,
      areaUpdateCounter: 0, // 用于强制更新区域显示
      allMajors: [],
      allClasses: [],

      // 分配范围（chips）相关
      professionalChips: [],
      classChipsByMajor: {},
      currentMajorKey: '',
      currentClassKey: '',
      allocationScopeTimestamp: 0,
      
      // 批量分配相关
      selectedClassesForBatch: [], // 选中的班级/专业用于批量分配
      batchAssignmentMode: false, // 是否处于批量分配模式
      batchAssigning: false, // 批量分配进行中
      allBuildingRoomsForBatch: [], // 批量分配时使用的整个楼栋的所有房间

      // 房间分配（在区域对话框中）
      selectedRoomIdsForAssign: [],
      
      // 未入住学生对话框
      unallocatedStudentsDialogVisible: false,
      studentSearchKeyword: '',
      selectedRoomForCheckin: null,
      selectedStudentsForCheckin: [],
      selectedStudentsForCheckout: [],
      // 分页（未入住/已入住）
      currentUnallocPage: 1,
      unallocPageSize: 10,
      currentAllocPage: 1,
      allocPageSize: 10,
      
      areaForm: {
        name: '',
        floors: [],
        majors: [],
        classes: []
      },
      
      areaFormRules: {
        name: [
          { required: true, message: '请输入区域名称', trigger: 'blur' }
        ],
        floors: [
          { required: true, message: '请选择楼层范围', trigger: 'change' }
        ]
      },
      
      buildings: [
        {
          id: 1,
          name: '1号楼',
          floors: 6,
          description: '男生宿舍楼'
        },
        {
          id: 2,
          name: '2号楼',
          floors: 6,
          description: '女生宿舍楼'
        }
      ],
             dorms: [],
      // 对话框状态
      buildingDialogVisible: false,
      detailDialogVisible: false,
      
      // 表单数据
      buildingForm: {
        name: '',
        floors: 6,
        description: ''
      },
      dormForm: {
        buildingId: '',
        room: '',
        type: '',
        totalBeds: 4,
        occupiedBeds: 0,
        floor: 1
      },
      
      // 当前编辑的项目
      currentBuilding: null,
      currentDorm: null,
      isEditMode: false,
      
      // 分页相关
      currentPage: 1,
      pageSize: 16,
      
      // 用品管理相关
      materialsDialogVisible: false,
      materialTabActive: 'materials',
      currentMaterialsRoom: null,
      roomMaterials: [],
      roomMaterialsLoading: false,
      roomReports: [],
      // 申报处理相关
      handleReportDialog: false,
      currentReport: null,
      reportHandleForm: {
        status: '处理中',
        handleRemark: ''
      },
      // 房间用品添加/编辑对话框
      addRoomMaterialDialog: false,
      editingRoomMaterial: null
    }
  },
  computed: {
    // 当前楼栋的最大楼层数（用于显示楼层选择器）
    currentBuildingMaxFloors() {
      // 固定显示15层选择器
      return 15;
    },
    
    // 当前楼栋的实际楼层数（用于判断楼层是否可选）
    currentBuildingActualFloors() {
      if (!this.selectedBuildingId) {
        return 0 // 未选择楼栋时不显示楼层
      }
      
      // 优先从批量分配加载的房间数据中获取（如果有的话）
      if (this.allBuildingRoomsForBatch && this.allBuildingRoomsForBatch.length > 0) {
        const floors = this.allBuildingRoomsForBatch
          .map(room => room.floor || 0)
          .filter(floor => floor > 0)
        
        if (floors.length > 0) {
          return Math.max(...floors)
        }
      }
      
      // 否则从本地宿舍数据计算
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
      const buildingDorms = allDorms.filter(dorm => dorm.buildingId == this.selectedBuildingId)
      
      if (buildingDorms.length === 0) {
        // 如果没有宿舍数据，返回默认10层（允许管理员先创建区域）
        return 10;
      }
      
      // 从实际宿舍数据中计算最大楼层数
      const floors = buildingDorms.map(dorm => dorm.floor || 0).filter(floor => floor > 0)
      if (floors.length === 0) {
        return 10; // 如果没有有效楼层数据，返回默认10层
      }
      
      const maxFloor = Math.max(...floors)
      // 返回实际楼层数，但至少6层（考虑到可能需要扩展）
      return Math.max(maxFloor, 6)
    },
    
    // 判断是否有实际的房间数据
    hasActualRoomData() {
      if (!this.selectedBuildingId) {
        return false
      }
      
      // 检查批量分配数据
      if (this.allBuildingRoomsForBatch && this.allBuildingRoomsForBatch.length > 0) {
        return true
      }
      
      // 检查本地宿舍数据
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
      const buildingDorms = allDorms.filter(dorm => dorm.buildingId == this.selectedBuildingId)
      
      return buildingDorms.length > 0
    },
    
    // 当前选中的分配目标（专业/班级）辅助
    currentMajor() {
      const key = this.currentMajorKey
      return this.professionalChips.find(m => {
        // 对于班级级别的卡片，使用组合 key 匹配
        if (m.classKey) {
          const compositeKey = `${m.majorId || m.originalMajorName || m.majorName}_${m.classKey}`
          return compositeKey === key
        }
        // 对于专业级别的卡片，使用原有逻辑
        return (m.majorId || m.majorName) === key
      }) || null
    },
    
    // 批量分配模式：是否全选
    isAllClassesSelected() {
      if (!this.professionalChips || this.professionalChips.length === 0) {
        return false;
      }
      return this.selectedClassesForBatch.length === this.professionalChips.length;
    },
    currentMajorClasses() {
      const key = this.currentMajorKey
      return this.classChipsByMajor[key] || []
    },
    // 计算每个楼栋的实际统计数据
    buildingStats() {
      return this.managedBuildings.map(building => {
        // 获取该楼栋下的所有宿舍
        const buildingDorms = this.dorms.filter(dorm => dorm.buildingId === building.id)
        
        // 计算总房间数
        const totalRooms = buildingDorms.length
        
        // 计算总床位数和已住人数
        const totalBeds = buildingDorms.reduce((sum, dorm) => sum + dorm.totalBeds, 0)
        const occupiedBeds = buildingDorms.reduce((sum, dorm) => sum + dorm.occupiedBeds, 0)
        
        // 计算入住率
        const occupancyRate = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0
        
        return {
          ...building,
          totalRooms,
          totalBeds,
          occupiedBeds,
          occupancyRate
        }
      })
    },
    filterOptions() {
      return [
        { label: '全部楼栋', value: '' },
        ...this.managedBuildings.map(building => ({
          label: building.name,
          value: building.id
        }))
      ]
    },
    filteredDorms() {
      let result = this.dorms
      
      if (this.searchKeyword) {
        result = result.filter(dorm => 
          dorm.room.includes(this.searchKeyword) || 
          dorm.building.includes(this.searchKeyword) ||
          dorm.type.includes(this.searchKeyword)
        )
      }
      
      if (this.filterBuilding) {
        result = result.filter(dorm => dorm.buildingId == this.filterBuilding)
      }
      return result
    },
    buildingDialogTitle() {
      return this.isEditMode ? '编辑楼栋' : '添加楼栋'
    },
    
    selectedBuilding() {
      return this.managedBuildings.find(b => b.id === this.selectedBuildingId)
    },
    
    selectedBuildingName() {
      return this.selectedBuilding?.name || ''
    },
    
    // 分页后的宿舍数据
    paginatedDorms() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredDorms.slice(start, end)
    },
    hasAllocationScope() {
      return Array.isArray(this.professionalChips) && this.professionalChips.length > 0
    },
    // 区域对话框中用于分配的房间（当前楼栋 + 选中楼层过滤）
    roomsForAssignment() {
      if (!this.selectedBuildingId) return []
      // 如果未选择楼层，返回空数组（显示提示信息）
      if (!this.areaForm.floors || this.areaForm.floors.length === 0) return []
      const floors = new Set(this.areaForm.floors)
      return this.dorms
        .filter(d => d.buildingId === this.selectedBuildingId)
        .filter(d => floors.has(d.floor))
        .sort((a, b) => {
          if (a.floor !== b.floor) return a.floor - b.floor
          return parseInt(a.room) - parseInt(b.room)
        })
    },

    // 是否具备当前分配目标（chips）
    canAssignToCurrentTarget() {
      if (!this.currentMajor) return false
      // 注释：不再强制要求class-only模式必须选择班级
      // 专业级别的操作也是有意义的（如智能班级分配）
      // if (this.currentMajor.mode === 'class-only' && !this.currentClassKey) return false
      return true
    },
    // chips 剩余名额统计（基于房间归属与入住数）
    chipsUsageStats() {
      const res = { major: {}, clazz: {} }
      const toMajorKey = (d) => d?.belongTo?.majorKey
      const toClazzKey = (d) => (d?.belongTo?.majorKey && d?.belongTo?.classKey) ? `${d.belongTo.majorKey}::${d.belongTo.classKey}` : null
      
      // 统计房间归属占用的床位（按 totalBeds 计）
      this.dorms
        .filter(d => d.buildingId === this.selectedBuildingId)
        .forEach(d => {
          const mKey = toMajorKey(d)
          const cKey = toClazzKey(d)
          if (mKey) {
            res.major[mKey] = res.major[mKey] || { usedByRooms: 0, occupied: 0 }
            res.major[mKey].usedByRooms += (d.totalBeds || 0)
          }
          if (cKey) {
            res.clazz[cKey] = res.clazz[cKey] || { usedByRooms: 0, occupied: 0 }
            res.clazz[cKey].usedByRooms += (d.totalBeds || 0)
          }
        })
      // 统计入住人数（用 occupiedBeds 或 residents 数）
      this.dorms
        .filter(d => d.buildingId === this.selectedBuildingId)
        .forEach(d => {
          const occupied = d.occupiedBeds || (Array.isArray(d.residents) ? d.residents.length : 0) || 0
          const mKey = toMajorKey(d)
          const cKey = toClazzKey(d)
          if (mKey) {
            res.major[mKey] = res.major[mKey] || { usedByRooms: 0, occupied: 0 }
            res.major[mKey].occupied += occupied
          }
          if (cKey) {
            res.clazz[cKey] = res.clazz[cKey] || { usedByRooms: 0, occupied: 0 }
            res.clazz[cKey].occupied += occupied
          }
        })
      return res
    },
    // 当前对象剩余名额（未入住弹窗显示）
    currentTargetRemainingSeats() {
      if (!this.currentMajor) return 0
      const majorKey = this.currentMajorKey
      const classKey = this.currentMajor.mode === 'class-only' ? (this.currentClassKey || null) : null
      return this.getRemainingSeatsForTarget(majorKey, classKey)
    },
    // 范围内且有空床位的房间
    inScopeRoomsWithSpace() {
      const floors = this.areaForm.floors && this.areaForm.floors.length > 0 ? new Set(this.areaForm.floors) : null
      const majorKey = this.currentMajorKey
      const classKey = this.currentMajor?.mode === 'class-only' ? (this.currentClassKey || null) : null
      return this.roomsForAssignment.filter(r => {
        if ((r.totalBeds - (r.occupiedBeds || 0)) <= 0) return false
        if (floors && !floors.has(r.floor)) return false
        // 房间必须已归属到当前对象
        const bt = r.belongTo
        if (!bt) return false
        const okMajor = bt.majorKey === majorKey
        const okClass = (!classKey) || (bt.classKey === classKey)
        return okMajor && okClass
      })
    },
    // 数据源与筛选
    unallocatedStudentsAll() {
      const db = JSON.parse(localStorage.getItem('studentsDatabase') || '{}')
      return Object.values(db).filter(s => s.registrationDate && !s.dormitoryInfo)
    },
    unallocatedStudentsInScope() {
      if (!this.currentMajor || !this.selectedBuilding) return []
      const majorName = this.currentMajor.majorName
      const gender = this.selectedBuilding.gender
      const classKey = this.currentMajor.mode === 'class-only' ? (this.currentClassKey || null) : null
      return this.unallocatedStudentsAll.filter(s => {
        if (s.gender !== gender) return false
        if (s.major !== majorName) return false
        if (classKey) {
          const key = s.classId || s.className
          return String(key) === String(classKey)
        }
        return true
      }).map(s => ({ ...s, className: s.className || s.class }))
    },
    filteredUnallocatedStudents() {
      const kw = (this.studentSearchKeyword || '').trim().toLowerCase()
      if (!kw) return this.unallocatedStudentsInScope
      return this.unallocatedStudentsInScope.filter(s => (s.name || '').toLowerCase().includes(kw) || (s.studentId || '').toLowerCase().includes(kw))
    },
    // 分页数据
    paginatedUnallocatedStudents() {
      const start = (this.currentUnallocPage - 1) * this.unallocPageSize
      return this.filteredUnallocatedStudents.slice(start, start + this.unallocPageSize)
    },
    paginatedAllocatedStudents() {
      const start = (this.currentAllocPage - 1) * this.allocPageSize
      return this.inScopeAllocatedStudents.slice(start, start + this.allocPageSize)
    },
    // 已入住（范围内）用于撤销
    inScopeAllocatedStudents() {
      const list = []
      const majorKey = this.currentMajorKey
      const classKey = this.currentMajor?.mode === 'class-only' ? (this.currentClassKey || null) : null
      this.dorms
        .filter(d => d.buildingId === this.selectedBuildingId)
        .forEach(d => {
          const bt = d.belongTo
          if (!bt) return
          const okMajor = (bt.majorKey === majorKey)
          const okClass = (!classKey) || (bt.classKey === classKey)
          if (okMajor && okClass && Array.isArray(d.residents)) {
            d.residents.forEach(r => list.push({ ...r, className: r.className || r.class, roomLabel: `${d.floor}-${d.room}`, roomId: d.id }))
          }
        })
      return list
    },
    
    areaDialogTitle() {
      return this.isAreaEditMode ? '编辑区域' : '添加区域'
    },
    
  },
  mounted() {
    // 同步管理员数据
    syncAdminData();
    this.loadData()
    this.loadManagedBuildings()
    this.loadMajorsAndClasses()
    this.loadStudentsData() // 加载学生数据
    // 初次计算 chips 用量
    this.$nextTick(() => {
      this.refreshChipsUsage()
    })
    
    // 检查是否从通知跳转过来
    this.handleQueryParams()
  },
  methods: {
    // ==================== 辅助方法 ====================
    
    // 判断房间是否已分配（统一判断逻辑）
    isRoomAssigned(room) {
      if (!room || !room.belongTo) return false
      if (typeof room.belongTo !== 'object') return false
      // 检查是否有有效的专业或班级名称
      const hasMajor = room.belongTo.majorName && room.belongTo.majorName.trim() !== ''
      const hasClass = room.belongTo.className && room.belongTo.className.trim() !== ''
      return hasMajor || hasClass
    },
    
    // 获取房间归属显示文本
    getRoomBelongToText(belongTo) {
      if (!belongTo) return ''
      const majorName = (belongTo.majorName || '').trim()
      const className = (belongTo.className || '').trim()
      if (!majorName && !className) return ''
      if (className) return `${majorName} · ${className}`
      return majorName
    },
    
    // 处理来自通知的query参数
    handleQueryParams() {
      const { action, dormNumber, reportId } = this.$route.query
      
      if (action === 'openMaterialDialog' && dormNumber) {
        // 延迟执行，确保数据已加载
        this.$nextTick(() => {
          // 找到对应的宿舍
          const building = this.buildings.find(b => 
            b.dorms && b.dorms.some(d => d.dormNumber === dormNumber)
          )
          
          if (building) {
            const dorm = building.dorms.find(d => d.dormNumber === dormNumber)
            if (dorm) {
              // 打开用品管理对话框
              this.manageMaterials(dorm)
              
              // 如果指定了reportId，切换到申报记录标签页
              if (reportId) {
                this.$nextTick(() => {
                  this.materialTabActive = 'reports'
                })
              }
            }
          }
          
          // 清除query参数，避免刷新时重复触发
          this.$router.replace({ query: {} })
        })
      }
    },
    
    // 载入本楼的分配范围（优先从后端API获取，localStorage作为备用）
    async loadAllocationScope() {
      if (!this.selectedBuildingId) return
      
      // 🔥 修复：优先从后端API获取数据
      try {
        const response = await getMajorAssignments(this.selectedBuildingId)
        if (response.code === 200 && response.data && response.data.length > 0) {
          // 转换API数据为前端所需格式
          const allocations = response.data.map(item => ({
            majorId: item.id,
            majorName: item.majorName,
            className: item.className || '',
            classSegment: item.classSegment || null,
            grade: item.grade || '',  // 添加年级字段
            students: item.allocatedStudents,
            rooms: item.allocatedRooms,
            mode: item.assignmentMode || 'full-major',
            details: item.className ? [{
              className: item.className,
              classSegment: item.classSegment,
              grade: item.grade || '',  // 添加年级字段
              students: item.allocatedStudents,
              rooms: item.allocatedRooms,
              isSplit: !!item.classSegment
            }] : []
          }))
          
          // 更新localStorage缓存（供离线使用）
          const store = JSON.parse(localStorage.getItem('buildingMajorAssignmentsV2') || '{}')
          store[this.selectedBuildingId] = {
            buildingId: this.selectedBuildingId,
            allocations: allocations,
            updatedAt: new Date().toISOString()
          }
          localStorage.setItem('buildingMajorAssignmentsV2', JSON.stringify(store))
          console.log('✅ 从后端API加载专业分配数据成功:', allocations)
        }
      } catch (error) {
        console.warn('从后端API加载失败，尝试使用localStorage缓存:', error)
      }
      
      // 从localStorage读取（可能是刚从API更新的，也可能是缓存）
      const scope = getBuildingAllocationScope(this.selectedBuildingId)
      this.professionalChips = scope.professionalChips || []
      this.classChipsByMajor = scope.classChipsByMajor || {}
      this.allocationScopeTimestamp = scope.generatedAt
      
      // 若当前选中项不在范围内，重置
      if (!this.currentMajor || !(this.currentMajorKey in this.classChipsByMajor) && (this.currentClassKey)) {
        this.currentClassKey = ''
      }
      if (!this.currentMajor) {
        this.currentMajorKey = ''
      }
    },
    async onSyncAllocationScope() {
      await this.loadAllocationScope()
      this.refreshChipsUsage()
      
      if (this.professionalChips && this.professionalChips.length > 0) {
        this.$message.success('已同步专业分配信息')
      } else {
        this.$message.warning('未找到本楼栋的专业分配数据，请联系超级管理员先进行专业分配')
      }
    },
    onSelectMajor(major) {
      // 如果是班级级别的卡片，使用组合 key
      const key = major.classKey 
        ? `${major.majorId || major.originalMajorName || major.majorName}_${major.classKey}` 
        : (major.majorId || major.majorName)
      this.currentMajorKey = key
      // 如果选择的是班级级别的卡片，同时设置 classKey
      if (major.classKey) {
        this.currentClassKey = major.classKey
      } else {
        // 切换专业时，重置班级选择
        this.currentClassKey = ''
      }
    },
    onSelectClass(cls) {
      this.currentClassKey = cls.classKey
    },
    
    // ==================== 批量分配相关方法 ====================
    
    // 加载整个楼栋的所有房间（用于批量分配）
    async loadAllBuildingRoomsForBatch() {
      if (!this.selectedBuildingId) {
        return;
      }
      
      try {
        console.log(`🔄 开始加载楼栋${this.selectedBuildingId}的房间数据...`);
        // 调用后端API获取该楼栋的所有房间
        const response = await getBuildingRooms(this.selectedBuildingId);
        
        console.log('📦 后端返回的原始数据:', response);
        
        if (response && response.code === 200 && response.data && response.data.length > 0) {
          console.log('📦 后端返回的第一条数据示例:', response.data[0]);
          
          // 转换数据格式以匹配现有的房间对象结构
          this.allBuildingRoomsForBatch = (response.data || [])
            .filter(room => {
              const hasRoom = room && room.room;
              if (!hasRoom) {
                console.warn('⚠️ 发现没有房间号的数据:', room);
              }
              return hasRoom;
            })
            .map(room => {
              // 🔥 修复：处理后端返回的 belongTo，确保格式正确
              let belongTo = null;
              
              if (room.belongTo) {
                const hasMajor = room.belongTo.majorName && room.belongTo.majorName.trim() !== ''
                const hasClass = room.belongTo.className && room.belongTo.className.trim() !== ''
                
                if (hasMajor || hasClass) {
                  // 尝试从 professionalChips 匹配，获取正确的 key
                  let majorKey = room.belongTo.majorName || ''
                  let classKey = room.belongTo.className || ''
                  
                  if (hasClass) {
                    const matchedChip = this.professionalChips.find(chip => {
                      const chipMajorName = chip.originalMajorName || chip.majorName
                      const majorMatches = chipMajorName === room.belongTo.majorName || 
                                          room.belongTo.majorName === `${chipMajorName} - ${chip.className}` ||
                                          room.belongTo.majorName.startsWith(chipMajorName + ' - ');
                      return majorMatches && chip.className === room.belongTo.className
                    })
                    
                    if (matchedChip) {
                      majorKey = matchedChip.majorId || matchedChip.originalMajorName || matchedChip.majorName
                      classKey = matchedChip.classKey || room.belongTo.className
                    } else {
                      // 提取专业名："人工智能 - 人工智能2402" -> "人工智能"
                      majorKey = room.belongTo.majorName.includes(' - ') 
                        ? room.belongTo.majorName.split(' - ')[0]
                        : room.belongTo.majorName
                    }
                  }
                  
                  belongTo = {
                    majorName: room.belongTo.majorName,
                    className: room.belongTo.className || '',
                    majorKey: majorKey,
                    classKey: classKey
                  }
                }
              }
              
              return {
                id: room.id,
                room: room.room,
                floor: room.floor || 0,
                type: room.type || '标准间',
                totalBeds: room.totalBeds || 4,
                occupiedBeds: room.occupiedBeds || 0,
                belongTo: belongTo  // ✅ 使用处理后的 belongTo
              }
            });
          
          console.log(`✅ 批量分配：成功加载 ${this.allBuildingRoomsForBatch.length} 个房间`);
        } else {
          console.warn('⚠️ 后端API返回数据为空，尝试从localStorage获取');
          // 如果后端API调用失败，从localStorage获取
          this.loadAllBuildingRoomsFromLocalStorage();
        }
      } catch (error) {
        console.error('❌ 加载楼栋房间失败，使用本地数据:', error);
        this.loadAllBuildingRoomsFromLocalStorage();
      }
    },
    
    // 从localStorage加载房间数据（备用方案）
    loadAllBuildingRoomsFromLocalStorage() {
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]');
      this.allBuildingRoomsForBatch = allDorms
        .filter(dorm => dorm.buildingId == this.selectedBuildingId && dorm.room) // 确保有房间号
        .map(dorm => ({
          id: dorm.id,
          room: dorm.room,
          floor: dorm.floor || 0,
          type: dorm.type || '标准间',
          totalBeds: dorm.totalBeds || 4,
          occupiedBeds: dorm.occupiedBeds || 0,
          belongTo: dorm.belongTo || null
        }));
      
      console.log(`批量分配：从本地加载 ${this.allBuildingRoomsForBatch.length} 个房间`);
    },
    
    // 切换全选/取消全选
    toggleSelectAllClasses() {
      if (this.isAllClassesSelected) {
        this.selectedClassesForBatch = [];
      } else {
        this.selectedClassesForBatch = [...this.professionalChips];
      }
    },
    
    // 判断某个班级/专业是否被选中
    isClassSelectedForBatch(classItem) {
      const key = classItem.classKey 
        ? `${classItem.majorId || classItem.majorName}_${classItem.classKey}` 
        : (classItem.majorId || classItem.majorName);
      return this.selectedClassesForBatch.some(item => {
        const itemKey = item.classKey 
          ? `${item.majorId || item.majorName}_${item.classKey}` 
          : (item.majorId || item.majorName);
        return itemKey === key;
      });
    },
    
    // 切换某个班级/专业的选中状态
    toggleClassForBatch(classItem) {
      const key = classItem.classKey 
        ? `${classItem.majorId || classItem.majorName}_${classItem.classKey}` 
        : (classItem.majorId || classItem.majorName);
      
      const index = this.selectedClassesForBatch.findIndex(item => {
        const itemKey = item.classKey 
          ? `${item.majorId || item.majorName}_${item.classKey}` 
          : (item.majorId || item.majorName);
        return itemKey === key;
      });
      
      if (index > -1) {
        this.selectedClassesForBatch.splice(index, 1);
      } else {
        this.selectedClassesForBatch.push(classItem);
      }
    },
    
    // 开始批量自动分配
    async startBatchAutoAssignment() {
      if (!this.selectedBuildingId) {
        this.$message.warning('请先选择楼栋');
        return;
      }
      
      if (this.selectedClassesForBatch.length === 0) {
        this.$message.warning('请先选择要分配的班级/专业');
        return;
      }
      
      // 加载整个楼栋的所有房间（用于批量分配）
      this.$message.info('正在加载楼栋所有房间信息...');
      await this.loadAllBuildingRoomsForBatch();
      
      // 先计算分配预览
      const preview = this.calculateAssignmentPreview();
      
      if (!preview || preview.length === 0) {
        this.$message.warning('没有可分配的房间，该楼栋所有房间都已被分配');
        return;
      }
      
      // 生成预览HTML - 简约风格
      const totalRooms = this.allBuildingRoomsForBatch.length;
      const availableRooms = this.allBuildingRoomsForBatch.filter(r => !r.belongTo).length;
      const occupiedRooms = totalRooms - availableRooms;
      const occupancyRate = totalRooms > 0 ? Math.round((occupiedRooms / totalRooms) * 100) : 0;
      
      let previewHTML = '<div style="max-height: 600px; overflow-y: auto; text-align: left;">';
      
      // 顶部统计 - 简约风格
      previewHTML += `
        <div style="
          background: #f8f9fa;
          padding: 16px;
          border: 1px solid #dee2e6;
          border-radius: 4px;
          margin-bottom: 20px;
        ">
          <div style="font-size: 14px; font-weight: 600; color: #212529; margin-bottom: 12px;">
            楼栋房间统计
          </div>
          <div style="display: flex; gap: 24px; flex-wrap: wrap;">
            <div>
              <div style="font-size: 13px; color: #6c757d; margin-bottom: 4px;">总房间数</div>
              <div style="font-size: 22px; font-weight: 700; color: #212529;">${totalRooms}</div>
            </div>
            <div>
              <div style="font-size: 13px; color: #6c757d; margin-bottom: 4px;">可用房间</div>
              <div style="font-size: 22px; font-weight: 700; color: #28a745;">${availableRooms}</div>
            </div>
            <div>
              <div style="font-size: 13px; color: #6c757d; margin-bottom: 4px;">已占用</div>
              <div style="font-size: 22px; font-weight: 700; color: #6c757d;">${occupiedRooms}</div>
            </div>
            <div>
              <div style="font-size: 13px; color: #6c757d; margin-bottom: 4px;">入住率</div>
              <div style="font-size: 22px; font-weight: 700; color: #212529;">${occupancyRate}%</div>
            </div>
          </div>
        </div>
      `;
      
      // 分配预览标题
      previewHTML += `
        <div style="
          font-size: 14px;
          font-weight: 600;
          color: #212529;
          margin-bottom: 16px;
        ">
          分配方案预览
        </div>
      `;
      
      // 班级分配列表 - 简约风格
      preview.forEach((item, index) => {
        const status = item.allocatedBeds >= item.requiredBeds ? '充足' : '不足';
        const statusColor = item.allocatedBeds >= item.requiredBeds ? '#28a745' : '#ffc107';
        // 限制百分比最大100%显示，但保留实际数字
        const percentage = item.requiredBeds > 0 ? Math.round((item.allocatedBeds / item.requiredBeds) * 100) : 100;
        const displayPercentage = Math.min(percentage, 100);
        const isOverflow = item.allocatedBeds > item.requiredBeds;
        
        // 按楼层分组房间
        const roomsByFloor = {};
        item.rooms.forEach(r => {
          if (!roomsByFloor[r.floor]) {
            roomsByFloor[r.floor] = [];
          }
          roomsByFloor[r.floor].push(r.room);
        });
        
        previewHTML += `
          <div style="
            background: white;
            border: 1px solid #dee2e6;
            border-radius: 4px;
            padding: 16px;
            margin-bottom: 12px;
          ">
            <!-- 班级名称和状态 -->
            <div style="
              display: flex;
              align-items: center;
              justify-content: space-between;
              margin-bottom: 12px;
              padding-bottom: 12px;
              border-bottom: 1px solid #e9ecef;
            ">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="
                  display: inline-block;
                  min-width: 24px;
                  height: 24px;
                  line-height: 24px;
                  text-align: center;
                  background: #6c757d;
                  color: white;
                  border-radius: 3px;
                  font-size: 13px;
                  font-weight: 600;
                ">${index + 1}</span>
                <span style="font-size: 15px; font-weight: 600; color: #212529;">${item.className}</span>
              </div>
              <span style="
                font-size: 13px;
                color: ${statusColor};
                font-weight: 600;
              ">${status}</span>
            </div>
            
            <!-- 房间列表（按楼层分组） -->
            <div>
              <div style="
                font-size: 13px;
                color: #6c757d;
                margin-bottom: 8px;
              ">分配房间：</div>
              <div style="display: flex; flex-direction: column; gap: 6px;">
                ${Object.entries(roomsByFloor).map(([floor, rooms]) => `
                  <div style="
                    background: #f8f9fa;
                    padding: 8px 12px;
                    border-left: 3px solid ${statusColor};
                    font-size: 13px;
                    color: #495057;
                  ">
                    <span style="font-weight: 600;">${floor}楼：</span>
                    ${rooms.join('、')}
                  </div>
                `).join('')}
              </div>
            </div>
          </div>
        `;
      });
      
      previewHTML += '</div>';
      
      // 底部提示
      previewHTML += `
        <div style="
          margin-top: 16px;
          padding: 12px;
          background: #fff3cd;
          border: 1px solid #ffc107;
          border-radius: 4px;
          font-size: 13px;
          color: #856404;
          line-height: 1.6;
        ">
          提示：已占用的房间会被自动跳过。部分班级分配的床位可能超过实际需求，这是因为房间不可拆分。
        </div>
      `;
      
      // 确认对话框
      const confirmResult = await this.$confirm(
        previewHTML,
        `批量自动分配预览（共 ${preview.length} 个班级）`,
        {
          confirmButtonText: '确认分配',
          cancelButtonText: '取消',
          dangerouslyUseHTMLString: true,
          customClass: 'batch-assignment-preview-dialog'
        }
      ).catch(() => false);
      
      if (!confirmResult) {
        return;
      }
      
      this.batchAssigning = true;
      
      try {
        await this.performBatchAutoAssignment();
      } catch (error) {
        console.error('批量分配失败:', error);
        this.$message.error('批量分配失败: ' + error.message);
      } finally {
        this.batchAssigning = false;
      }
    },
    
    // 计算分配预览（不实际执行分配）
    calculateAssignmentPreview() {
      // 1. 获取所有可用房间（按楼层、房间号排序）
      // 批量分配时使用整个楼栋的房间数据
      const allRooms = this.allBuildingRoomsForBatch || [];
      
      const availableRooms = allRooms
        .filter(room => !room.belongTo && room.room) // 只选择未分配且有房间号的房间
        .sort((a, b) => {
          // 先按楼层排序，再按房间号排序
          if (a.floor !== b.floor) {
            return a.floor - b.floor;
          }
          // 提取房间号中的数字进行排序（确保room字段存在）
          const numA = a.room ? (parseInt(a.room.replace(/\D/g, '')) || 0) : 0;
          const numB = b.room ? (parseInt(b.room.replace(/\D/g, '')) || 0) : 0;
          return numA - numB;
        });
      
      if (availableRooms.length === 0) {
        return [];
      }
      
      // 计算每个班级的分配结果
      let roomIndex = 0;
      const preview = [];
      
      for (const classItem of this.selectedClassesForBatch) {
        const className = classItem.className || classItem.majorName;
        const requiredBeds = classItem.totalSeats || 0;
        let allocatedBeds = 0;
        const assignedRooms = [];
        
        // 为当前班级分配足够的房间
        while (allocatedBeds < requiredBeds && roomIndex < availableRooms.length) {
          const room = availableRooms[roomIndex];
          assignedRooms.push({
            id: room.id,
            floor: room.floor,
            room: room.room,
            beds: room.totalBeds
          });
          allocatedBeds += room.totalBeds;
          roomIndex++;
        }
        
        if (assignedRooms.length > 0) {
          preview.push({
            className,
            rooms: assignedRooms,
            allocatedBeds,
            requiredBeds,
          });
        }
      }
      
      return preview;
    },
    
    // 执行批量自动分配
    async performBatchAutoAssignment() {
      // 1. 获取所有可用房间（按楼层、房间号排序）
      // 批量分配时使用整个楼栋的房间数据
      const allRooms = this.allBuildingRoomsForBatch || [];
      const availableRooms = allRooms
        .filter(room => !room.belongTo && room.room) // 只选择未分配且有房间号的房间
        .sort((a, b) => {
          // 先按楼层排序，再按房间号排序
          if (a.floor !== b.floor) {
            return a.floor - b.floor;
          }
          // 提取房间号中的数字进行排序（确保room字段存在）
          const numA = a.room ? (parseInt(a.room.replace(/\D/g, '')) || 0) : 0;
          const numB = b.room ? (parseInt(b.room.replace(/\D/g, '')) || 0) : 0;
          return numA - numB;
        });
      
      if (availableRooms.length === 0) {
        this.$message.warning('没有可用的未分配房间');
        return;
      }
      
      // 2. 保持用户选择的顺序（不重新排序）
      // 3. 为每个班级分配房间
      let roomIndex = 0;
      const assignmentResults = [];
      
      for (const classItem of this.selectedClassesForBatch) {
        const className = classItem.className || classItem.majorName;
        const requiredBeds = classItem.totalSeats || 0;
        let allocatedBeds = 0;
        const assignedRooms = [];
        
        // 为当前班级分配足够的房间
        while (allocatedBeds < requiredBeds && roomIndex < availableRooms.length) {
          const room = availableRooms[roomIndex];
          assignedRooms.push(room.id);
          allocatedBeds += room.totalBeds;
          roomIndex++;
        }
        
        if (assignedRooms.length > 0) {
          assignmentResults.push({
            classItem,
            className,
            roomIds: assignedRooms,
            allocatedBeds,
            requiredBeds,
          });
        }
      }
      
      if (assignmentResults.length === 0) {
        this.$message.warning('没有可分配的房间');
        return;
      }
      
      // 4. 执行分配（调用后端API）
      let successCount = 0;
      let failCount = 0;
      
      for (const result of assignmentResults) {
        try {
          // 调用分配API
          await this.assignRoomsToTarget(result.classItem, result.roomIds);
          successCount++;
        } catch (error) {
          console.error(`分配 ${result.className} 失败:`, error);
          failCount++;
        }
      }
      
        // 5. 显示结果
        if (successCount > 0) {
          this.$message.success(`分配完成！成功：${successCount} 个班级/专业`);
          
          // 🔥 本地操作模式：不重新加载数据，保持本地分配的状态
          // 刷新卡片统计（基于本地 this.dorms 数据）
          this.refreshChipsUsage();
          
          // 清空选择并退出批量模式
          this.selectedClassesForBatch = [];
          this.batchAssignmentMode = false;
        }
      
      if (failCount > 0) {
        this.$message.warning(`有 ${failCount} 个班级/专业分配失败`);
      }
    },
    
    // 为指定目标分配房间（辅助方法）- 仅本地操作，不保存数据库
    async assignRoomsToTarget(targetClassItem, roomIds) {
      // 🔥 修改为本地操作：只更新 this.dorms，不调用后端API
      // 只有点击"添加"按钮后才会保存到数据库
      
      const realMajorName = targetClassItem.originalMajorName || targetClassItem.majorName;
      const majorKey = targetClassItem.majorId || realMajorName;
      const classKey = targetClassItem.classKey || targetClassItem.className;
      
      console.log('🔄 本地分配房间:', {
        卡片显示名: targetClassItem.majorName,
        原始专业名: realMajorName,
        班级名: targetClassItem.className,
        房间数: roomIds.length,
        majorKey: majorKey,
        classKey: classKey
      });
      
      // 更新本地 this.dorms 数据
      roomIds.forEach(roomId => {
        const room = this.dorms.find(r => r.id === roomId);
        if (room) {
          room.belongTo = {
            majorName: realMajorName,
            className: targetClassItem.className || '',
            majorKey: majorKey,
            classKey: classKey
          };
        }
      });
      
      console.log(`✅ 本地分配完成：${roomIds.length} 个房间`);
      
      return { code: 200, message: '本地分配成功' };
    },
    
    // ==================== 批量分配相关方法结束 ====================
    
    loadData() {
      // 根据管理员分配的楼栋加载对应的宿舍数据
      this.loadDormsByManagedBuildings()
    },
    
    // 根据管理员管理的楼栋加载宿舍数据（简化为按需加载）
    async loadDormsByManagedBuildings() {
      // 现在改为按需加载：选择楼栋后才加载该楼栋的房间数据
      // 这里只需要初始化空数组
      this.dorms = []
      console.log('宿舍数据将在选择楼栋后按需加载')
    },
    
    // 为指定楼栋生成宿舍数据
    generateDormsForBuildings(buildings) {
      const dorms = []
      let dormId = 1
      
      buildings.forEach(building => {
        // 根据楼栋类型和房间数生成宿舍
        const roomsPerFloor = Math.ceil(building.totalRooms / 6) // 假设6层楼
        
        for (let floor = 1; floor <= 6; floor++) {
          for (let roomNum = 1; roomNum <= roomsPerFloor; roomNum++) {
            if (dorms.length >= building.totalRooms) break
            
            const roomNumber = `${floor}${String(roomNum).padStart(2, '0')}`
            const totalBeds = building.id === 2 ? 6 : 4 // 2号楼6人间，其他4人间
            const occupiedBeds = Math.floor(Math.random() * (totalBeds + 1)) // 随机入住人数
            
            dorms.push({
              id: dormId++,
              buildingId: building.id,
              building: building.name,
              room: roomNumber,
              type: totalBeds === 6 ? '六人间' : '四人间',
              totalBeds: totalBeds,
              occupiedBeds: occupiedBeds,
              floor: floor,
              residents: this.generateResidents(occupiedBeds, dormId)
            })
          }
        }
      })
      
      console.log('生成的宿舍数据:', dorms)
      return dorms
    },
    
    // 生成住宿人员数据
    generateResidents(count, dormId) {
      const residents = []
      const names = ['张三', '李四', '王五', '赵六', '孙七', '周八', '吴九', '郑十', '钱十一', '孙十二']
      
      for (let i = 0; i < count; i++) {
        residents.push({
          id: dormId * 100 + i,
          name: names[i] || `学生${i + 1}`,
          studentId: `2021${String(dormId * 100 + i).padStart(3, '0')}`,
          phone: `138000${String(dormId * 100 + i).padStart(5, '0')}`,
          checkInDate: '2023-09-01',
          status: '在住'
        })
      }
      
      return residents
    },
    
    // 转换房间类型显示
    // 统一使用中文数字格式：二人间、四人间、六人间、八人间
    convertRoomTypeToDisplay(roomType) {
      const typeMap = {
        'double_bed': '二人间',
        'two_bed': '二人间',
        'four_bed': '四人间',
        'six_bed': '六人间',
        'eight_bed': '八人间'
      }
      return typeMap[roomType] || roomType || '四人间'
    },
    
    // 加载管理员负责的楼栋（改用后端API）
    async loadManagedBuildings() {
      try {
        const response = await getMyBuildings()
        if (response.code === 200 && response.data) {
          console.log('🔍 后端返回的原始楼栋数据:', response.data)
          
          // 转换后端数据格式为前端需要的格式
          this.managedBuildings = response.data.map(building => {
            console.log('🏢 处理楼栋 - ID:', building.id)
            console.log('   - buildingName:', building.buildingName)
            console.log('   - name:', building.name)
            console.log('   - gender:', building.gender)
            console.log('   - buildingType:', building.buildingType)
            console.log('   - 完整对象:', building)
            
            const processedName = building.buildingName || building.name || `${building.id}号楼`
            console.log('   ✅ 最终使用的name:', processedName)
            
            return {
              id: building.id,
              name: processedName,
              buildingCode: building.buildingCode,
              totalRooms: building.totalRooms || 0,
              totalFloors: building.totalFloors || 0,
              gender: building.gender || building.buildingType || '男', // buildingType已经是中文了
              description: building.description
            }
          })
          
          console.log('✅ 加载管理员楼栋成功:', this.managedBuildings)
          
          // 如果只有一个楼栋，自动选中
          if (this.managedBuildings.length === 1) {
            this.selectedBuildingId = this.managedBuildings[0].id
            console.log('🎯 自动选择唯一楼栋:', this.managedBuildings[0].name)
            // 自动加载该楼栋的数据（不显示提示消息）
            this.$nextTick(() => {
              this.selectBuilding(this.managedBuildings[0], false)
            })
          }
        } else {
          console.warn('获取管理员楼栋失败:', response.message)
        }
      } catch (error) {
        console.error('加载管理员楼栋失败:', error)
        // 降级到本地数据
        const currentUser = getCurrentUser()
        if (currentUser && currentUser.username) {
          this.managedBuildings = getAdminBuildings(currentUser.username)
          // 如果只有一个楼栋，自动选中
          if (this.managedBuildings.length === 1) {
            this.selectedBuildingId = this.managedBuildings[0].id
            this.$nextTick(() => {
              this.selectBuilding(this.managedBuildings[0], false)
            })
          }
        }
      }
    },
    
    // 加载专业和班级选项
    loadMajorsAndClasses() {
      // 获取系统默认专业和班级
      const systemMajors = getAllMajors()
      const systemClasses = getAllClasses()
      
      // 获取用户自定义专业和班级
      try {
        const customMajors = JSON.parse(localStorage.getItem('customMajors') || '[]')
        const customClasses = JSON.parse(localStorage.getItem('customClasses') || '[]')
        
        // 合并系统和自定义选项，去重
        this.allMajors = [...new Set([...systemMajors, ...customMajors])]
        this.allClasses = [...new Set([...systemClasses, ...customClasses])]
      } catch (error) {
        console.error('加载自定义专业班级失败:', error)
        this.allMajors = systemMajors
        this.allClasses = systemClasses
      }
    },
    
    // 加载学生数据（从后端API）
    async loadStudentsData() {
      try {
        console.log('🔄 开始从后端加载学生数据...')
        
        const response = await getStudentList({
          page: 1,
          pageSize: 10000 // 获取所有学生
        })
        
        console.log('📦 后端返回数据:', response)
        
        if (response.code === 200 && response.data) {
          // 尝试多种可能的数据结构
          const students = response.data.students || response.data.records || response.data.list || response.data
          
          if (!Array.isArray(students)) {
            console.error('❌ 学生数据格式错误，不是数组:', students)
            console.error('   data结构:', Object.keys(response.data))
            return
          }
          
          console.log(`✅ 成功加载 ${students.length} 条学生数据`)
          console.log('👤 学生数据样例（前2条）:', students.slice(0, 2))
          
          // 转换为studentsDatabase格式：{ studentId: studentInfo }
          const studentsDB = {}
          students.forEach(student => {
            const studentId = student.studentId || student.id
            studentsDB[studentId] = {
              studentId: studentId,
              id: student.id,
              name: student.name || student.studentName,
              gender: student.gender,
              major: student.major || student.majorName,
              className: student.className || student.class,
              classId: student.classId,
              phone: student.phone,
              email: student.email,
              dormitoryInfo: student.dormitoryInfo,
              registrationDate: student.registrationDate || student.createdAt
            }
          })
          
          console.log('🔄 转换后的学生数据库样例:', Object.values(studentsDB).slice(0, 2))
          console.log(`📊 学生数据库大小: ${Object.keys(studentsDB).length} 条记录`)
          
          // 保存到localStorage
          try {
            localStorage.setItem('studentsDatabase', JSON.stringify(studentsDB))
            console.log('💾 学生数据已保存到localStorage')
            
            // 验证保存结果
            const savedData = JSON.parse(localStorage.getItem('studentsDatabase') || '{}')
            console.log(`✅ 验证保存成功: ${Object.keys(savedData).length} 条记录`)
          } catch (saveError) {
            console.error('❌ 保存到localStorage失败:', saveError)
          }
          
          // 输出统计信息
          const majors = [...new Set(students.map(s => s.major || s.majorName).filter(Boolean))]
          console.log(`📊 包含专业 (${majors.length}个): ${majors.join('、')}`)
          
          // 按专业统计人数
          const majorStats = {}
          students.forEach(s => {
            const major = s.major || s.majorName
            if (major) {
              majorStats[major] = (majorStats[major] || 0) + 1
            }
          })
          console.table(majorStats)
          
        } else {
          console.warn('⚠️ 获取学生数据失败:', response.message || response)
        }
      } catch (error) {
        console.error('❌ 加载学生数据失败:', error)
        console.error('错误堆栈:', error.stack)
        // 不阻塞其他功能，仅在控制台警告
      }
    },
    
    // 选择楼栋（改用后端API）
    // showMessage: 是否显示成功提示，默认为true（用户主动选择），自动加载时传false
    async selectBuilding(building, showMessage = true) {
      const buildingId = typeof building === 'object' ? building.id : building
      this.selectedBuildingId = buildingId
      
      // 查找楼栋信息
      const buildingInfo = this.managedBuildings.find(b => b.id === buildingId)
      const buildingName = buildingInfo?.name
      
      // 🔥 修复：只在用户主动选择时才显示提示
      if (buildingName && showMessage) {
        this.$message.success(`已选择${buildingName}`)
      }
      
      // 🔥 关键修复：先加载分配范围（professionalChips），再加载房间数据
      // 这样 loadBuildingRooms 中才能匹配到正确的 majorId
      await this.loadAllocationScope()
      
      // 加载该楼栋的房间数据
      await this.loadBuildingRooms(buildingId)
      
      // 🔥 核心修复：加载房间数据后立即刷新统计！
      this.$nextTick(() => {
        this.refreshChipsUsage()
        console.log('✅ 选择楼栋后自动刷新统计数据')
      })
    },
    
    // 加载楼栋房间数据
    async loadBuildingRooms(buildingId) {
      try {
        this.loading = true
        const response = await getBuildingRooms(buildingId)
        
        if (response.code === 200 && response.data) {
            // 转换后端数据格式
            this.dorms = response.data.map(room => {
              // 检查是否有有效的专业或班级名称（排除空字符串、null、undefined）
              const hasMajor = room.majorName && room.majorName.trim() !== ''
              const hasClass = room.className && room.className.trim() !== ''
              
              // 🔥 关键修复：查找对应的专业 ID 和班级 Key，确保 key 匹配
              let majorKey = room.majorName || ''
              let classKey = room.className || ''
              
              if (hasMajor) {
                // 如果有班级，优先匹配班级级别的卡片
                if (hasClass) {
                  const matchedClassChip = this.professionalChips.find(chip => {
                    const chipMajorName = chip.originalMajorName || chip.majorName
                    
                    // 🔥 修复：兼容两种情况
                    // 1. room.majorName = "人工智能" (正确格式)
                    // 2. room.majorName = "人工智能 - 人工智能2402" (旧数据格式)
                    const majorMatches = chipMajorName === room.majorName || 
                                        room.majorName === `${chipMajorName} - ${chip.className}` ||
                                        room.majorName.startsWith(chipMajorName + ' - ');
                    
                    return majorMatches && chip.className === room.className
                  })
                  
                  if (matchedClassChip) {
                    majorKey = matchedClassChip.majorId || matchedClassChip.originalMajorName || matchedClassChip.majorName
                    classKey = matchedClassChip.classKey || room.className
                    
                  } else {
                    // 如果没找到班级卡片，尝试从 majorName 中提取原始专业名
                    // "人工智能 - 人工智能2402" -> "人工智能"
                    const extractedMajorName = room.majorName.includes(' - ') 
                      ? room.majorName.split(' - ')[0]
                      : room.majorName;
                    majorKey = extractedMajorName
                  }
                } else {
                  // 没有班级，匹配专业级别的卡片
                  const matchedMajorChip = this.professionalChips.find(chip => {
                    const chipMajorName = chip.originalMajorName || chip.majorName
                    return chipMajorName === room.majorName && !chip.classKey
                  })
                  
                  if (matchedMajorChip) {
                    majorKey = matchedMajorChip.majorId || matchedMajorChip.originalMajorName || matchedMajorChip.majorName
                  } else {
                    // 如果没找到专业卡片，使用专业名作为 key
                    majorKey = room.majorName
                  }
                }
              }
              
              return {
                id: room.id,
                buildingId: buildingId,
                building: room.building || '',  // 直接使用API返回的building字段
                room: room.room || '',          // 直接使用API返回的room字段
                type: this.convertRoomTypeToDisplay(room.type),  // API返回的是type，不是roomType
                totalBeds: room.totalBeds || 4,     // API返回的是totalBeds，不是bedCount
                occupiedBeds: room.occupiedBeds || 0,  // API返回的是occupiedBeds，不是occupiedCount
                floor: room.floor || 1,
                residents: room.residents || [],
                belongTo: (hasMajor || hasClass) ? {
                  majorName: room.majorName || '',
                  className: room.className || '',
                  majorKey: majorKey,  // ✅ 使用匹配的 majorKey（可能是 majorId）
                  classKey: classKey   // ✅ 使用匹配的 classKey
                } : null,
                notes: room.notes || ''
              }
            })
          
          console.log(`✅ 加载楼栋${buildingId}房间数据成功:`, this.dorms.length, '间')
          
          // 加载房间数据后立即刷新统计数据
          this.$nextTick(() => {
            this.refreshChipsUsage()
          })
        } else {
          console.warn('获取房间数据失败:', response.message)
          this.dorms = []
        }
      } catch (error) {
        console.error('加载房间数据失败:', error)
        this.$message.error('加载房间数据失败')
        this.dorms = []
      } finally {
        this.loading = false
      }
    },
    
    // 显示区域管理对话框
    async showAreaManagementDialog() {
      if (!this.selectedBuildingId) {
        this.$message.warning('请先选择要管理的楼栋')
        return
      }
      
      // 加载楼栋区域
      this.loadBuildingAreas()
      // 加载分配范围
      this.loadAllocationScope()
      // 加载楼栋所有房间数据（用于计算实际楼层数）
      await this.loadAllBuildingRoomsForBatch()
      
      this.areaManagementDialogVisible = true
    },
    
    // 加载楼栋区域（改用后端API）
    async loadBuildingAreas() {
      if (!this.selectedBuildingId) return
      
      try {
        const response = await getApiBuildingAreas(this.selectedBuildingId)
        if (response.code === 200 && response.data) {
          this.buildingAreas = response.data
          console.log('✅ 从后端加载区域数据成功:', this.buildingAreas)
        } else {
          console.warn('获取区域数据失败:', response.message)
          this.buildingAreas = []
        }
      } catch (error) {
        console.error('加载区域数据失败:', error)
        // 降级到本地数据
        this.buildingAreas = getBuildingAreas(this.selectedBuildingId)
      }
    },
    
    // 显示添加区域对话框
    async showAddAreaDialog() {
      this.isAreaEditMode = false
      this.editingAreaId = null
      this.resetAreaForm()
      
      // 🔥 核心修复：添加区域时也重新加载房间数据并刷新统计
      console.log('➕ 添加区域，重新加载房间数据...')
      await this.loadBuildingRooms(this.selectedBuildingId)
      
      // 刷新统计数据
      this.$nextTick(() => {
        this.refreshChipsUsage()
        console.log('✅ 添加区域时已刷新统计数据')
      })
      
      this.areaDialogVisible = true
    },
    
    // 编辑区域
    async editArea(area) {
      this.isAreaEditMode = true
      this.editingAreaId = area.id
      this.areaForm = {
        name: area.name,
        floors: [...area.floors],
        majors: [...area.majors],
        classes: [...area.classes]
      }
      
      // 🔥 核心修复：编辑区域时重新加载房间数据并刷新统计
      console.log('🔧 编辑区域，重新加载房间数据...')
      await this.loadBuildingRooms(this.selectedBuildingId)
      
      // 刷新统计数据
      this.$nextTick(() => {
        this.refreshChipsUsage()
        console.log('✅ 编辑区域时已刷新统计数据')
      })
      
      this.areaDialogVisible = true
    },
    
    // 删除区域（改用后端API）
    deleteArea(area) {
      this.$confirm(`确定要删除区域"${area.name}"吗？`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await deleteApiArea(area.id)
          if (response.code === 200) {
            this.$message.success('区域删除成功')
            
            // 重新加载数据确保数据一致性
            await this.loadBuildingAreas()
            
            // 强制更新视图
            this.areaUpdateCounter++
            this.$nextTick(() => {
              this.$forceUpdate()
            })
          } else {
            this.$message.error(response.message || '区域删除失败')
          }
        } catch (error) {
          console.error('删除区域失败:', error)
          this.$message.error('删除区域失败，请重试')
        }
      }).catch(() => {
        // 用户取消删除
      })
    },
    
    // 保存区域（改用后端API）
    saveArea() {
      this.$refs.areaFormRef.validate(async (valid) => {
        if (!valid) {
          // 验证失败时，滚动到表单顶部、聚焦输入框并提示
          this.$nextTick(() => {
            const dialogContent = document.querySelector('.add-area-dialog .el-dialog__body')
            if (dialogContent) {
              dialogContent.scrollTop = 0
            }
            // 聚焦到区域名称输入框
            if (this.$refs.areaNameInput) {
              this.$refs.areaNameInput.focus()
            }
          })
          this.$message.warning('请填写完整的区域信息')
          return
        }
        
        // 验证分配范围是否有效
        if (!this.professionalChips || this.professionalChips.length === 0) {
          this.$message.error('请先同步信息范围，确保有可分配的专业/班级')
          return
        }

        // 验证是否有实际的房间分配
        const hasRoomAssignments = this.roomsForAssignment.some(room => room.belongTo)
        if (!hasRoomAssignments) {
          this.$message.warning('建议先分配一些房间给专业/班级再保存区域')
        }

        this.areaSaving = true
        
        try {
          // 🔥 第一步：保存房间分配信息到数据库
          // 只保存当前选择的楼层的房间（避免覆盖其他楼层的撤回操作）
          const assignmentsByClass = new Map();
          const selectedFloors = new Set(this.areaForm.floors || []);
          
          this.dorms
            .filter(room => {
              // 只保存当前楼栋、选中楼层、有归属的房间
              return room.buildingId === this.selectedBuildingId 
                && room.belongTo 
                && selectedFloors.has(room.floor)
            })
            .forEach(room => {
              const key = `${room.belongTo.majorName}::${room.belongTo.className || ''}`;
              if (!assignmentsByClass.has(key)) {
                assignmentsByClass.set(key, {
                  majorName: room.belongTo.majorName,
                  className: room.belongTo.className || null,
                  roomIds: []
                });
              }
              assignmentsByClass.get(key).roomIds.push(room.id);
            });
          
          console.log('💾 准备保存房间分配:', Array.from(assignmentsByClass.values()));
          
          // 批量保存每个专业/班级的房间分配
          let savedCount = 0;
          for (const assignment of assignmentsByClass.values()) {
            try {
              const response = await batchAssignRooms({
                buildingId: this.selectedBuildingId,
                majorName: assignment.majorName,
                className: assignment.className,
                roomIds: assignment.roomIds,
                action: 'assign'
              });
              
              if (response.code === 200) {
                savedCount++;
                console.log(`✅ 保存成功: ${assignment.className || assignment.majorName}, ${assignment.roomIds.length} 个房间`);
              }
            } catch (error) {
              console.error(`❌ 保存失败: ${assignment.className || assignment.majorName}`, error);
            }
          }
          
          // 不显示房间分配保存提示，统一在区域保存成功后显示
          
          // 🔥 第二步：保存区域信息
          // 构建后端需要的数据格式，包含所有分配的专业/班级
          const assignments = Array.from(assignmentsByClass.values()).map(assignment => {
            // 从 professionalChips 中查找对应的 chip，获取 grade 信息
            const chip = this.professionalChips.find(c => {
              const chipMajorName = c.originalMajorName || c.majorName
              if (assignment.className) {
                // 班级模式：匹配专业和班级
                return chipMajorName === assignment.majorName && c.className === assignment.className
              } else {
                // 专业模式：只匹配专业
                return chipMajorName === assignment.majorName && !c.classKey
              }
            })
            
            return {
              majorName: assignment.majorName,
              className: assignment.className,
              grade: chip?.grade || '',  // 从 chip 中获取年级
              mode: assignment.className ? 'class-only' : 'full-major',
              allocatedStudents: 0,  // 可以后续统计
              allocatedRooms: assignment.roomIds.length
            }
          })
          
          const areaData = {
            buildingId: this.selectedBuildingId,  // ✅ 后端DTO验证需要这个字段
            areaName: this.areaForm.name,  // ✅ 后端字段名是 areaName
            floors: this.areaForm.floors,
            assignments: assignments  // ✅ 传入所有分配的专业/班级
          }
          
          let response
          if (this.isAreaEditMode) {
            // 更新区域
            response = await updateArea(this.editingAreaId, areaData)
            if (response.code === 200) {
              this.$message.success('区域更新成功')
              this.areaDialogVisible = false
              
              // 🔥 重新加载所有相关数据，确保统计数据同步
              await this.loadBuildingAreas()
              await this.loadAllocationScope()  // ✅ 刷新分配范围
              await this.loadBuildingRooms(this.selectedBuildingId)  // ✅ 刷新房间数据
              
              // ✅ 刷新卡片统计
              this.$nextTick(() => {
                this.refreshChipsUsage()
              })
              
              // 强制更新视图
              this.areaUpdateCounter++
              this.$nextTick(() => {
                this.$forceUpdate()
              })
            } else {
              this.$message.error(response.message || '区域更新失败')
            }
          } else {
            // 添加区域
            response = await createArea(this.selectedBuildingId, areaData)
            if (response.code === 200) {
              // 显示包含房间分配信息的完整提示
              const message = savedCount > 0 
                ? `区域添加成功，已分配 ${savedCount} 个专业/班级的房间` 
                : '区域添加成功'
              this.$message.success(message)
              this.areaDialogVisible = false
              
              // 🔥 重新加载所有相关数据，确保统计数据同步
              await this.loadBuildingAreas()
              await this.loadAllocationScope()  // ✅ 刷新分配范围
              await this.loadBuildingRooms(this.selectedBuildingId)  // ✅ 刷新房间数据
              
              // ✅ 刷新卡片统计
              this.$nextTick(() => {
                this.refreshChipsUsage()
              })
              
              // 强制更新视图
              this.areaUpdateCounter++
              this.$nextTick(() => {
                this.$forceUpdate()
              })
            } else {
              this.$message.error(response.message || '区域添加失败')
            }
          }
        } catch (error) {
          console.error('保存区域失败:', error)
          this.$message.error('操作失败，请重试')
        } finally {
          this.areaSaving = false
        }
      })
    },
    
    // 重置区域表单
    resetAreaForm() {
      this.areaForm = {
        name: '',
        floors: [],
        majors: [],
        classes: []
      }
      this.$nextTick(() => {
        this.$refs.areaFormRef?.clearValidate()
      })
    },
    
    
    
    // 检查楼层是否已被占用
    isFloorOccupied(floor) {
      if (this.isAreaEditMode) {
        // 编辑模式下，排除当前编辑的区域
        return this.buildingAreas
          .filter(area => area.id !== this.editingAreaId)
          .some(area => area.floors.includes(floor))
      }
      return this.buildingAreas.some(area => area.floors.includes(floor))
    },

    // 切换楼层选择状态
    toggleFloor(floor) {
      if (this.isFloorOccupied(floor)) {
        return // 已被占用的楼层不能选择
      }
      
      if (floor > this.currentBuildingActualFloors) {
        return // 超出楼栋实际楼层的不能选择
      }
      
      const floors = [...this.areaForm.floors]
      const index = floors.indexOf(floor)
      
      if (index > -1) {
        floors.splice(index, 1)
      } else {
        floors.push(floor)
      }
      
      this.areaForm.floors = floors
    },

    // 清空所有楼层选择
    clearAllFloors() {
      this.areaForm.floors = []
    },

    // 获取楼栋的宿舍楼层信息（用于调试显示）
    getDormFloorsForBuilding(buildingId) {
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
      const buildingDorms = allDorms.filter(dorm => dorm.buildingId == buildingId)
      const floors = [...new Set(buildingDorms.map(dorm => dorm.floor).filter(floor => floor > 0))]
      return floors.sort((a, b) => a - b)
    },
    
    // 楼栋管理
    showAddBuildingDialog() {
      this.isEditMode = false
      this.buildingForm = {
        name: '',
        floors: 6,
        description: ''
      }
      this.buildingDialogVisible = true
    },
    
    editBuilding(building) {
      this.isEditMode = true
      this.currentBuilding = building
      this.buildingForm = { ...building }
      this.buildingDialogVisible = true
    },
    
    saveBuilding() {
      if (!this.buildingForm.name) {
        this.$message.warning('请输入楼栋名称')
        return
      }
      
      if (this.isEditMode) {
        // 更新楼栋
        Object.assign(this.currentBuilding, this.buildingForm)
        this.$message.success('楼栋更新成功')
      } else {
                 // 添加楼栋
         const newBuilding = {
           id: Date.now(),
           ...this.buildingForm
         }
        this.buildings.push(newBuilding)
        this.$message.success('楼栋添加成功')
      }
      
      this.buildingDialogVisible = false
    },
    
    deleteBuilding(building) {
      this.$confirm(`确定要删除楼栋"${building.name}"吗？删除后将同时删除该楼栋下的所有宿舍。`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.buildings = this.buildings.filter(b => b.id !== building.id)
        this.dorms = this.dorms.filter(d => d.buildingId !== building.id)
        this.$message.success('楼栋删除成功')
      })
    },
    
    manageBuilding(building) {
      this.filterBuilding = building.id
      this.search()
    },
    
    
    
    async viewDetails(dorm) {
      // 先显示基本信息
      this.currentDorm = { ...dorm, residents: [] }
      this.detailDialogVisible = true
      
      // 从后端获取房间住户数据
      if (dorm.id) {
        try {
          const response = await getApiRoomResidents(dorm.id)
          if (response.code === 200 && response.data) {
            // 转换后端数据格式
            this.currentDorm.residents = response.data.map(resident => ({
              name: resident.realName || resident.name,
              studentId: resident.studentId,
              major: resident.major,
              className: resident.className,
              email: resident.email || '未绑定',
              bedNumber: resident.bedNumber || '-'
            }))
            console.log('✅ 获取房间住户成功:', this.currentDorm.residents)
          }
        } catch (error) {
          console.error('获取房间住户失败:', error)
        }
      }
      
      // 刷新 chips 用量，确保汇总条即时
      this.refreshChipsUsage()
    },
    
    // 触发全局数据同步
    triggerGlobalDataSync() {
      // 发送自定义事件通知其他页面数据已更新
      window.dispatchEvent(new CustomEvent('dormDataUpdated', {
        detail: {
          timestamp: Date.now(),
          source: 'DormManagement',
          action: 'statusUpdate'
        }
      }))
      
      // 更新数据版本号，用于强制刷新
      const currentVersion = parseInt(localStorage.getItem('dormDataVersion') || '0')
      localStorage.setItem('dormDataVersion', (currentVersion + 1).toString())
    },

    // ========== 区域对话框：房间分配到当前对象 ==========
    onAssignRoomsSelectionChange(selection) {
      this.selectedRoomIdsForAssign = selection.map(r => r.id)
    },
    assignSelectedRoomsToCurrentTarget() {
      if (!this.canAssignToCurrentTarget) {
        this.$message.warning('请先选择分配对象（专业/班级）')
        return
      }

      if (this.selectedRoomIdsForAssign.length === 0) {
        this.$message.warning('请先选择要分配的房间')
        return
      }

      const major = this.currentMajor
      // 🔥 对于班级级别的卡片，使用 originalMajorName 获取真实的专业名称
      const majorKey = major.majorId || major.originalMajorName || major.majorName
      const classKey = this.currentClassKey || major.classKey || null
      const classList = this.currentMajorClasses
      const className = classKey ? (classList.find(c => c.classKey === classKey)?.className || major.className || '') : ''

      // 计算所选房间的空床位
      const seatsNeeded = this.roomsForAssignment
        .filter(d => this.selectedRoomIdsForAssign.includes(d.id))
        .reduce((s, d) => s + Math.max(0, (d.totalBeds || 0) - (d.occupiedBeds || 0)), 0)

      if (seatsNeeded === 0) {
        this.$message.warning('所选房间都已住满')
        return
      }

      // 配额校验：验证选择的房间数不能超过配额限制
      if (!classKey) {
        // 专业级分配：检查整个专业的配额
        const roomQuota = this.getRemainingRoomsForTarget(majorKey, null)
        const selectedRoomCount = this.selectedRoomIdsForAssign.length
        
        if (roomQuota.remainingRooms <= 0) {
          this.$message.error('当前专业房间配额已用完')
          return
        }
        
        if (selectedRoomCount > roomQuota.remainingRooms) {
          this.$message.error(`超出配额限制，最多还能分配${roomQuota.remainingRooms}间房`)
          return
        }
      } else {
        // 班级级分配：严格按照配额分配
        const remaining = this.getRemainingSeatsForTarget(majorKey, classKey)
        if (remaining <= 0) {
          this.$message.error('当前班级配额已用完，所有学生都已分配房间')
          return
        }
      }

      const selectedSet = new Set(this.selectedRoomIdsForAssign)
      
      // 提前获取和排序选中的房间
      const selectedRooms = this.roomsForAssignment.filter(d => selectedSet.has(d.id))
      selectedRooms.sort((a, b) => {
        if (a.floor !== b.floor) return a.floor - b.floor
        return parseInt(a.room) - parseInt(b.room)
      })
      
      // 过滤出未分配的房间（排除已经分配给任何班级的房间）
      const unassignedRooms = selectedRooms.filter(room => !room.belongTo)
      
      if (unassignedRooms.length === 0) {
        this.$message.warning('所选房间都已被分配，请选择其他未分配的房间')
        return
      }
      
      // ✅ 手动分配：选中几间就分配几间（不做智能筛选）
      // 只检查是否有已分配的房间，如果有就过滤掉
      if (unassignedRooms.length < selectedRooms.length) {
        const skipped = selectedRooms.length - unassignedRooms.length
        this.$message.info(`已自动跳过 ${skipped} 间已分配的房间，将分配 ${unassignedRooms.length} 间未分配的房间`)
      }
      
      // 更新 selectedSet 为实际要分配的房间
      selectedSet.clear()
      unassignedRooms.forEach(room => selectedSet.add(room.id))
      
      const actualAssignedCount = unassignedRooms.length
      console.log(`手动分配：用户选择了 ${selectedRooms.length} 间房，实际分配 ${actualAssignedCount} 间未分配的房间`)
      
      // 更新当前组件内存与本地存储
      const updateAndPersist = () => {
        const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
        const allById = new Map(allDorms.map(d => [d.id, d]))
        
        // 获取最终要分配的房间
        const finalSelectedRooms = this.dorms.filter(d => selectedSet.has(d.id))
        finalSelectedRooms.sort((a, b) => {
          if (a.floor !== b.floor) return a.floor - b.floor
          return parseInt(a.room) - parseInt(b.room)
        })

        // 执行分配
        if (classKey) {
          finalSelectedRooms.forEach(d => {
            const belongTo = {
              buildingId: this.selectedBuildingId,
              majorKey,
              majorName: major.majorName,
              classKey: classKey,
              className: className
            }
            d.belongTo = belongTo
            if (allById.has(d.id)) {
              allById.get(d.id).belongTo = belongTo
            }
          })
        } else {
          // 自动班级分配：强制使用学生数据中的真实班级信息
          const extractedClasses = this.extractClassesFromStudentData(major.majorName)
          console.log(`🔍 提取到的班级信息 (${major.majorName}):`, extractedClasses)
          if (extractedClasses && extractedClasses.length > 0) {
            // 使用从学生数据提取的真实班级信息进行分配
            console.log(`🏠 开始为 ${extractedClasses.length} 个班级分配 ${finalSelectedRooms.length} 间房间`)
            this.autoAssignClassesToRooms(finalSelectedRooms, extractedClasses, majorKey, major.majorName, allById)
          } else {
            // 没有学生数据时，分配给整个专业（不使用通用班级名称）
            finalSelectedRooms.forEach(d => {
              const belongTo = {
                buildingId: this.selectedBuildingId,
                majorKey,
                majorName: major.majorName
              }
              d.belongTo = belongTo
              if (allById.has(d.id)) {
                allById.get(d.id).belongTo = belongTo
              }
            })
          }
        }
        
        localStorage.setItem('allDormsData', JSON.stringify(Array.from(allById.values())))
      }
      updateAndPersist()
      
      // 生成成功消息
      const realMajorName = major.originalMajorName || major.majorName
      let successMsg = `分配完成`
      if (className) {
        successMsg = `已将 ${actualAssignedCount} 间房分配到 ${realMajorName}·${className}`
      } else if (major.classKey) {
        // 如果是班级级别的卡片，直接显示卡片名称（已包含专业和班级）
        successMsg = `已将 ${actualAssignedCount} 间房分配到 ${major.majorName}`
      } else {
        const extractedClasses = this.extractClassesFromStudentData(realMajorName)
        if (extractedClasses?.length > 0) {
          successMsg = `已将 ${actualAssignedCount} 间房分配到 ${realMajorName} (${extractedClasses.length}个班级)`
        } else {
          successMsg = `已将 ${actualAssignedCount} 间房分配到 ${realMajorName}`
        }
      }
      
      this.$message.success(successMsg)
      this.selectedRoomIdsForAssign = []
      this.refreshChipsUsage()
    },
    unassignSelectedRooms() {
      if (this.selectedRoomIdsForAssign.length === 0) return
      const selectedSet = new Set(this.selectedRoomIdsForAssign)
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
      const allById = new Map(allDorms.map(d => [d.id, d]))
      this.dorms.forEach(d => {
        if (selectedSet.has(d.id)) {
          d.belongTo = null
          if (allById.has(d.id)) {
            allById.get(d.id).belongTo = null
          }
        }
      })
      localStorage.setItem('allDormsData', JSON.stringify(Array.from(allById.values())))
      this.$message.success('已取消分配')
      this.selectedRoomIdsForAssign = []
      this.refreshChipsUsage()
    },
    // 系统分配到当前对象：智能分配可用房间（支持自动班级分配）
    autoFillRoomsForCurrentTarget() {
      if (!this.canAssignToCurrentTarget) return
      
      // 检查是否选择了楼层
      if (!this.areaForm.floors || this.areaForm.floors.length === 0) {
        this.$message.warning('请先选择要分配的楼层')
        return
      }
      
      // 检查是否有房间数据（仅控制台提示）
      if (!this.dorms || this.dorms.length === 0) {
        console.warn('⚠️ 当前楼栋没有房间数据')
        return
      }
      
      const major = this.currentMajor
      // 🔥 对于班级级别的卡片，使用 originalMajorName 获取真实的专业名称
      const majorKey = major.majorId || major.originalMajorName || major.majorName
      const realMajorName = major.originalMajorName || major.majorName
      
      // 获取可用房间（使用统一的判断方法）
      const candidates = this.roomsForAssignment
        .filter(d => !this.isRoomAssigned(d))
        .sort((a, b) => {
          if (a.floor !== b.floor) return a.floor - b.floor
          return parseInt(a.room) - parseInt(b.room)
        })
      
      if (candidates.length === 0) {
        const totalInFloors = this.roomsForAssignment.length
        if (totalInFloors === 0) {
          // 仅在控制台提示，不显示前端消息
          console.warn(`⚠️ 所选楼层（${this.areaForm.floors.join('、')}层）内没有房间数据`)
          return
        } else {
          // 正常情况：房间都已分配，显示前端提示
          this.$message.info(`所选楼层内的 ${totalInFloors} 间房间都已被分配，无可用房间`)
          return
        }
      }

      // 检查是否有班级信息
      const extractedClasses = this.extractClassesFromStudentData(realMajorName)
      const configuredClasses = this.currentMajorClasses
      
      console.log('🔍 当前选择的对象:', {
        majorName: major.majorName,
        className: major.className,
        mode: major.mode,
        classKey: this.currentClassKey,
        realMajorName: realMajorName
      })
      console.log('📚 提取到的班级列表:', extractedClasses)
      
      // 优先使用超级管理员的配额分配
      const superAdminQuota = this.getRemainingSeatsForTarget(majorKey, null) + (this.chipsUsageStats.major[majorKey]?.usedByRooms || 0)
      console.log(`📊 专业 ${realMajorName}: 超级管理员配额 ${superAdminQuota}`)
      
      // ✅ 正确逻辑：使用与超级管理员相同的房间计算逻辑
      if (extractedClasses?.length > 0) {
        // 如果当前选择的是单个班级（class-only模式），只为该班级计算
        let classesToProcess = extractedClasses
        if (major.mode === 'class-only' && major.className) {
          // 只处理当前选中的班级
          classesToProcess = extractedClasses.filter(cls => cls.className === major.className)
          console.log(`🎯 单班级模式: 只为 ${major.className} 计算房间需求`)
        } else {
          console.log(`🎯 专业模式: 为所有 ${extractedClasses.length} 个班级计算房间需求`)
        }
        
        // 有班级信息：按超级管理员的逻辑计算房间需求
        let totalRoomsNeeded = 0
        const classRoomDetails = []
        
        // 获取当前楼栋的房间类型（床位数）- 从实际房间数据中获取
        let bedsPerRoom = 4 // 默认4人间
        
        if (this.dorms && this.dorms.length > 0) {
          // 从当前实际房间数据中统计床位数
          const countMap = {}
          this.dorms.forEach(d => {
            const beds = Number(d.totalBeds || d.bedsPerRoom || 0)
            if (!beds) return
            countMap[beds] = (countMap[beds] || 0) + 1
          })
          
          if (Object.keys(countMap).length > 0) {
            // 选择占比最多的床位数作为当前房型
            const best = Object.entries(countMap).sort((a, b) => b[1] - a[1])[0]
            bedsPerRoom = Number(best[0])
            console.log(`🏠 楼栋房间类型: ${bedsPerRoom}人间 (统计: ${JSON.stringify(countMap)})`)
          } else {
            console.warn(`⚠️ 未能从房间数据中提取床位数，使用默认值: ${bedsPerRoom}人间`)
          }
        } else {
          console.warn(`⚠️ 没有房间数据，使用默认值: ${bedsPerRoom}人间`)
        }
        
        if (classesToProcess.length === 0) {
          console.warn(`⚠️ 未找到匹配的班级，当前选择: ${major.className || '无'}`)
          this.$message.warning('未找到匹配的班级数据')
          return
        }
        
        classesToProcess.forEach(cls => {
          const classSize = cls.totalSeats || cls.studentCount
          
          // ✅ 使用与超级管理员完全相同的计算逻辑
          const roomsNeeded = Math.ceil(classSize / bedsPerRoom)
          const fullRooms = Math.floor(classSize / bedsPerRoom)
          const remainder = classSize % bedsPerRoom
          
          // 构建宿舍包信息（与超级管理员一致）
          const bundles = []
          for (let i = 0; i < fullRooms; i += 1) {
            bundles.push({ size: bedsPerRoom, type: 'full' })
          }
          if (remainder > 0) {
            bundles.push({ size: remainder, type: 'partial' })
          }
          
          totalRoomsNeeded += roomsNeeded
          classRoomDetails.push({
            className: cls.className,
            students: classSize,
            roomsNeeded: roomsNeeded,
            fullRooms: fullRooms,
            remainder: remainder,
            bundles: bundles
          })
          
          console.log(`班级 ${cls.className}: ${classSize}人, 需要 ${roomsNeeded} 间房 (满房${fullRooms}间 + 剩余${remainder}人)`)
          console.log(`  宿舍包:`, bundles)
        })
        
        console.log(`🏠 专业总房间需求: ${totalRoomsNeeded} 间 (基于${bedsPerRoom}人间计算)`, classRoomDetails)
        
        // 检查配额限制
        // 🔥 修复：如果是班级模式，应该检查班级的配额，而不是整个专业的配额
        const classKeyForQuota = (major.mode === 'class-only' && major.className) ? major.className : null
        const roomQuota = this.getRemainingRoomsForTarget(majorKey, classKeyForQuota)
        console.log(`📊 ${major.mode === 'class-only' ? '班级' : '专业'} ${major.majorName}${classKeyForQuota ? (' - ' + classKeyForQuota) : ''}: 超级管理员房间配额`, roomQuota)
        
        if (roomQuota.remainingRooms <= 0) {
          const target = classKeyForQuota ? `班级 ${major.className}` : `专业 ${major.majorName}`
          this.$message.warning(`当前${target}的房间配额已用完`)
          return
        }
        
        // ✅ 关键修复：按实际需求分配，但不能超过配额
        if (totalRoomsNeeded > roomQuota.remainingRooms) {
          const target = classKeyForQuota ? `班级 ${major.className}` : `专业 ${major.majorName}`
          this.$message.error(`${target}需要${totalRoomsNeeded}间房，但配额只剩${roomQuota.remainingRooms}间`)
          return
        }
        
        // 选择实际需要的房间数量
        const selectedRooms = candidates.slice(0, totalRoomsNeeded)
        
        if (selectedRooms.length < totalRoomsNeeded) {
          this.$message.error(`可用房间不足！需要 ${totalRoomsNeeded} 间，只有 ${selectedRooms.length} 间可用`)
          return
        }
        
        this.selectedRoomIdsForAssign = selectedRooms.map(r => r.id)
        this.$message.success(`已选择 ${totalRoomsNeeded} 间房间`)
        
      } else {
        // 没有班级信息：使用配额限制但按实际需求分配
        const roomQuota = this.getRemainingRoomsForTarget(majorKey, null)
        console.log(`📊 专业 ${realMajorName}: 超级管理员房间配额`, roomQuota)
        
        if (roomQuota.remainingRooms <= 0) {
          this.$message.warning('当前专业房间配额已用完')
          return
        }
        
        // 对于没有班级信息的专业，按学生人数估算需求
        const actualStudents = this.getActualStudentCount(realMajorName)
        const estimatedRoomsNeeded = Math.ceil(actualStudents / 4)
        
        console.log(`📊 学生数统计: ${actualStudents}人, 估算需要: ${estimatedRoomsNeeded}间房`)
        
        // ❌ 没有学生数据无法分配
        if (estimatedRoomsNeeded === 0) {
          this.$message.error({
            message: '没有找到该专业的学生数据！请先在【超级管理员 - 学生管理】中导入学生信息',
            duration: 5000
          })
          console.error(`❌ 无法分配：专业 ${realMajorName} 没有学生数据`)
          return
        }
        
        const roomsToSelect = Math.min(estimatedRoomsNeeded, roomQuota.remainingRooms, candidates.length)
        const selectedRooms = candidates.slice(0, roomsToSelect)
        
        if (selectedRooms.length === 0) {
          this.$message.warning('没有可用房间或配额已用完')
          return
        }
        
        this.selectedRoomIdsForAssign = selectedRooms.map(r => r.id)
        this.$message.success(`已选择 ${roomsToSelect} 间房间`)
      }
      
      // 执行分配
      this.assignSelectedRoomsToCurrentTarget()
    },
    // 撤销分配：回收当前对象在所选楼层内的所有归属房间（数据库操作）
    async batchUnassignCurrentTargetInFloors() {
      if (!this.canAssignToCurrentTarget) {
        this.$message.warning('请先选择要撤回的专业')
        return
      }
      
      // 🔥 修复：从 currentMajor 直接获取 majorId，而不是使用组合 key
      const major = this.currentMajor
      const majorKey = major.majorId || major.originalMajorName || major.majorName
      const classKey = major.classKey || this.currentClassKey || null
      const floors = this.areaForm.floors && this.areaForm.floors.length > 0 ? new Set(this.areaForm.floors) : null
      
      const targets = this.roomsForAssignment.filter(d => {
        if (floors && !floors.has(d.floor)) return false
        const bt = d.belongTo
        if (!bt) return false
        const okMajor = bt.majorKey === majorKey || bt.majorKey == majorKey  // 兼容数字和字符串
        const okClass = (!classKey) || (bt.classKey === classKey)
        return okMajor && okClass
      })
      
      if (targets.length === 0) {
        this.$message.info('当前楼层范围内没有需要回收的房间')
        return
      }
      
      // 确认操作
      const targetName = classKey 
        ? `${major.originalMajorName || major.majorName} · ${major.className}`
        : (major.originalMajorName || major.majorName)
      
      try {
        await this.$confirm(`确定要撤回 ${targetName} 在所选楼层的房间分配吗？这将清空 ${targets.length} 间房间的归属信息。`, '撤回专业分配', {
          confirmButtonText: '确定撤回',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 调用后端API执行撤回操作
        const roomIds = targets.map(t => t.id)
        const requestPayload = {
          buildingId: this.selectedBuildingId,
          majorName: major.originalMajorName || major.majorName,
          className: major.className || null,
          roomIds: roomIds,
          action: 'unassign'  // ✅ 取消分配操作
        }
        
        const response = await batchAssignRooms(requestPayload)
        
        if (response.code === 200) {
          this.$message.success(`已撤回 ${targets.length} 间房间的分配`)
          
          // 重新加载数据
          await this.loadAllocationScope()  // 刷新分配范围
          await this.loadBuildingRooms(this.selectedBuildingId)  // 刷新房间数据
          console.log('🔍 数据变化对比:', {
            刷新前: beforeRefresh,
            刷新后: afterRefresh,
            是否有变化: JSON.stringify(beforeRefresh) !== JSON.stringify(afterRefresh)
          })
          
          this.refreshChipsUsage()
          
          // 强制更新视图
          this.$nextTick(() => {
            this.$forceUpdate()
          })
          
          console.log('✅ 数据刷新完成')
        } else {
          console.error('❌ 后端返回错误:', response)
          this.$message.error(response.message || '撤回失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('撤回专业分配失败:', error)
          this.$message.error('撤回失败，请重试')
        }
      }
    },
    // ===== 未入住学生对话框 =====
    openUnallocatedStudentsDialog() {
      if (!this.canAssignToCurrentTarget) return
      this.unallocatedStudentsDialogVisible = true
    },
    onSelectStudentsForCheckin(sel) {
      this.selectedStudentsForCheckin = sel
    },
    onSelectStudentsForCheckout(sel) {
      this.selectedStudentsForCheckout = sel
    },
    // 批量安排入住到选定房间（仅演示：更新 room.residents/occupiedBeds）
    batchCheckinToRoom() {
      if (!this.selectedRoomForCheckin || this.selectedStudentsForCheckin.length === 0) return
      const room = this.dorms.find(d => d.id === this.selectedRoomForCheckin)
      if (!room) return
      // 校验：房间必须归属到当前对象
      const bt = room.belongTo
      const majorKey = this.currentMajorKey
      const classKey = this.currentMajor?.mode === 'class-only' ? (this.currentClassKey || null) : null
      if (!bt || bt.majorKey !== majorKey || (!!classKey && bt.classKey !== classKey)) {
        this.$message.error('目标房间未归属到当前对象，无法安排入住')
        return
      }
      const empty = Math.max(0, (room.totalBeds || 0) - (room.occupiedBeds || 0))
      if (this.selectedStudentsForCheckin.length > empty) {
        this.$message.error(`房间余床 ${empty}，选择人数 ${this.selectedStudentsForCheckin.length}`)
        return
      }
      // 校验：性别
      const gender = this.selectedBuilding?.gender
      if (gender && this.selectedStudentsForCheckin.some(s => s.gender !== gender)) {
        this.$message.error('存在性别不匹配的学生，无法安排到该楼栋')
        return
      }
      // 校验：重复入住
      const db = this.getStudentsDB()
      const dup = this.selectedStudentsForCheckin.find(s => db[s.studentId]?.dormitoryInfo)
      if (dup) {
        this.$message.error(`学生 ${dup.name} 已有入住记录，不能重复安排`)
        return
      }
      if (!Array.isArray(room.residents)) room.residents = []
      this.selectedStudentsForCheckin.forEach(s => {
        const resident = { ...s, bedNumber: (room.residents.length + 1), checkInDate: new Date().toISOString().split('T')[0] }
        room.residents.push(resident)
        // 写入学生数据库 dormitoryInfo
        this.setStudentDormitoryInfo(s.studentId, {
          buildingId: room.buildingId,
          buildingName: room.building,
          roomId: room.id,
          roomNumber: room.room,
          floor: room.floor,
          majorName: this.currentMajor?.majorName,
          classKey: this.currentClassKey || null,
          className: s.className || s.class || null,
          bedNumber: resident.bedNumber,
          checkInDate: resident.checkInDate
        })
      })
      room.occupiedBeds = (room.occupiedBeds || 0) + this.selectedStudentsForCheckin.length
      // TODO: 若有学生数据库，需同步写入 dormitoryInfo
      this.persistDorms()
      this.$message.success('已安排入住')
      this.selectedStudentsForCheckin = []
      this.selectedRoomForCheckin = null
      this.refreshChipsUsage()
    },
    batchCheckout() {
      if (this.selectedStudentsForCheckout.length === 0) return
      const byRoomId = new Map()
      this.selectedStudentsForCheckout.forEach(s => {
        const arr = byRoomId.get(s.roomId) || []
        arr.push(s)
        byRoomId.set(s.roomId, arr)
      })
      byRoomId.forEach((list, roomId) => {
        const room = this.dorms.find(d => d.id === roomId)
        if (!room || !Array.isArray(room.residents)) return
        list.forEach(s => {
          const idx = room.residents.findIndex(r => r.studentId === s.studentId)
          if (idx > -1) room.residents.splice(idx, 1)
          // 清除学生数据库 dormitoryInfo
          this.clearStudentDormitoryInfo(s.studentId)
        })
        room.occupiedBeds = Math.max(0, (room.occupiedBeds || 0) - list.length)
      })
      this.persistDorms()
      this.$message.success('已撤销入住')
      this.selectedStudentsForCheckout = []
      this.refreshChipsUsage()
    },
    // ===== 学生数据库持久化 =====
    getStudentsDB() {
      try {
        return JSON.parse(localStorage.getItem('studentsDatabase') || '{}')
      } catch (e) {
        console.error('读取学生数据库失败', e)
        return {}
      }
    },
    saveStudentsDB(db) {
      try {
        localStorage.setItem('studentsDatabase', JSON.stringify(db))
      } catch (e) {
        console.error('保存学生数据库失败', e)
      }
    },
    setStudentDormitoryInfo(studentId, info) {
      const db = this.getStudentsDB()
      const s = db[studentId] || { studentId }
      s.dormitoryInfo = { ...info }
      db[studentId] = s
      this.saveStudentsDB(db)
    },
    clearStudentDormitoryInfo(studentId) {
      const db = this.getStudentsDB()
      if (db[studentId] && db[studentId].dormitoryInfo) {
        delete db[studentId].dormitoryInfo
        this.saveStudentsDB(db)
      }
    },
    persistDorms() {
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
      const byId = new Map(allDorms.map(d => [d.id, d]))
      this.dorms.forEach(d => {
        if (byId.has(d.id)) byId.set(d.id, { ...byId.get(d.id), ...d })
      })
      localStorage.setItem('allDormsData', JSON.stringify(Array.from(byId.values())))
    },
    // chips 汇总条点击：设置当前对象并打开未入住学生视图
    onSummaryChipClickMajor(m) {
      // 如果是班级级别的卡片，使用组合 key
      if (m.classKey) {
        this.currentMajorKey = `${m.majorId || m.originalMajorName || m.majorName}_${m.classKey}`
        this.currentClassKey = m.classKey
      } else {
        this.currentMajorKey = m.majorId || m.majorName
        this.currentClassKey = ''
      }
      this.openUnallocatedStudentsDialog()
    },
    onSummaryChipClickClass(m, c) {
      this.currentMajorKey = m.majorId || m.majorName
      this.currentClassKey = c.classKey
      this.openUnallocatedStudentsDialog()
    },
    // 计算目标对象剩余名额
    getRemainingSeatsForTarget(majorKey, classKey) {
      // seatQuota
      let seatQuota = 0
      const majorChip = this.professionalChips.find(m => (m.majorId || m.majorName) === majorKey)
      if (!majorChip) return 0
      if (!classKey || majorChip.mode === 'full-major') {
        seatQuota = majorChip.totalSeats
      } else {
        const list = this.classChipsByMajor[majorKey] || []
        const c = list.find(x => x.classKey === classKey)
        seatQuota = c ? c.totalSeats : 0
      }
      // 已占用（房间归属 + 入住）
      const stats = this.chipsUsageStats
      const usedByRooms = (!classKey || majorChip.mode === 'full-major')
        ? (stats.major[majorKey]?.usedByRooms || 0)
        : (stats.clazz[`${majorKey}::${classKey}`]?.usedByRooms || 0)
      const occupied = (!classKey || majorChip.mode === 'full-major')
        ? (stats.major[majorKey]?.occupied || 0)
        : (stats.clazz[`${majorKey}::${classKey}`]?.occupied || 0)
      const remaining = Math.max(0, seatQuota - usedByRooms - occupied)
      return remaining
    },

    // 新增：获取房间配额和剩余房间数
    getRemainingRoomsForTarget(majorKey, classKey) {
      const store = JSON.parse(localStorage.getItem('buildingMajorAssignmentsV2') || '{}')
      const buildingData = store[this.selectedBuildingId]
      
      if (!buildingData) return { totalRooms: 0, usedRooms: 0, remainingRooms: 0 }
      
      // 找到对应的专业分配
      const allocation = buildingData.allocations.find(a => a.majorId === majorKey || a.majorName === majorKey)
      if (!allocation) return { totalRooms: 0, usedRooms: 0, remainingRooms: 0 }
      
      let totalRooms = 0
      if (!classKey || allocation.mode === 'full-major') {
        // 整个专业的房间配额
        totalRooms = allocation.rooms || 0
      } else {
        // 特定班级的房间配额：在class-only模式下，从details中查找对应班级
        if (allocation.mode === 'class-only' && allocation.details) {
          const classDetail = allocation.details.find(d => d.classKey === classKey || d.className === classKey)
          if (classDetail) {
            totalRooms = classDetail.rooms || 0
          }
        }
      }
      
      // 计算已用房间数
      const allDorms = JSON.parse(localStorage.getItem('allDormsData') || '[]')
      const usedRooms = allDorms.filter(room => {
        const bt = room.belongTo
        if (!bt || bt.buildingId != this.selectedBuildingId) return false
        const matchMajor = bt.majorKey === majorKey
        const matchClass = !classKey || bt.classKey === classKey
        return matchMajor && matchClass
      }).length
      
      const remainingRooms = Math.max(0, totalRooms - usedRooms)
      
      console.log(`🏠 房间配额查询 - 专业: ${majorKey}, 班级: ${classKey || '全专业'}`)
      console.log(`📊 总房间配额: ${totalRooms}, 已用: ${usedRooms}, 剩余: ${remainingRooms}`)
      
      return { totalRooms, usedRooms, remainingRooms }
    },
      // 刷新 chips 的 used/remaining 显示
      refreshChipsUsage() {
        const stats = this.chipsUsageStats
        
        this.professionalChips = this.professionalChips.map(m => {
        let usedRooms = 0
        let occupied = 0
        
        // 🔥 如果卡片有 classKey，说明是班级级别的卡片
        if (m.classKey) {
          const majorKey = m.majorId || m.originalMajorName || m.majorName
          const classStatsKey = `${majorKey}::${m.classKey}`
          usedRooms = stats.clazz[classStatsKey]?.usedByRooms || 0
          occupied = stats.clazz[classStatsKey]?.occupied || 0
        } else {
          // 专业级别的卡片
          const key = m.majorId || m.majorName
          
          if (m && m.mode === 'class-only') {
            // 按班级模式：统计该专业下所有班级的使用量
            const classes = this.classChipsByMajor[key] || []
            classes.forEach(c => {
              const classKey = `${key}::${c.classKey}`
              usedRooms += stats.clazz[classKey]?.usedByRooms || 0
              occupied += stats.clazz[classKey]?.occupied || 0
            })
          } else {
            // 整专业模式：使用专业级别统计
            usedRooms = stats.major[key]?.usedByRooms || 0
            occupied = stats.major[key]?.occupied || 0
          }
        }
        
        const used = Math.min(m.totalSeats, usedRooms + occupied)
        return { ...m, usedSeats: used, remainingSeats: Math.max(0, m.totalSeats - used) }
      })
      const next = {}
      Object.keys(this.classChipsByMajor || {}).forEach(majorKey => {
        next[majorKey] = (this.classChipsByMajor[majorKey] || []).map(c => {
          const key = `${majorKey}::${c.classKey}`
          const usedRooms = stats.clazz[key]?.usedByRooms || 0
          const occupied = stats.clazz[key]?.occupied || 0
          const used = Math.min(c.totalSeats, usedRooms + occupied)
          return { ...c, usedSeats: used, remainingSeats: Math.max(0, c.totalSeats - used) }
        })
      })
      this.classChipsByMajor = next
    },
    
    // 分页方法
    handleSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },
    
    handleCurrentChange(val) {
      this.currentPage = val
    },
    
    
    // 搜索功能
    search() {
      // 搜索逻辑已在computed中实现
    },
    
         resetSearch() {
       this.searchKeyword = ''
       this.filterBuilding = ''
       this.filterStatus = ''
     },
     
     // 导出人员名单
     exportResidentsList() {
       if (!this.currentDorm || !this.currentDorm.residents) {
         this.$message.warning('没有人员数据可导出')
         return
       }
       
       // 准备导出数据
       const dormInfo = this.currentDorm
       const residents = this.currentDorm.residents
       
       // 创建CSV内容
       let csvContent = '\uFEFF' // 添加BOM，确保中文正确显示
       
       // 添加宿舍信息
       csvContent += `宿舍信息\n`
       csvContent += `楼栋,${dormInfo.building}\n`
       csvContent += `房间号,${dormInfo.room}\n`
       csvContent += `类型,${dormInfo.type}\n`
       csvContent += `入住率,${dormInfo.totalBeds > 0 ? Math.round((dormInfo.occupiedBeds / dormInfo.totalBeds) * 100) : 0}%\n`
       csvContent += `\n`
       
               // 添加人员名单表头
        csvContent += `人员名单\n`
        csvContent += `姓名,学号,联系电话,入住日期\n`
        
        // 添加人员数据
        residents.forEach(resident => {
          csvContent += `${resident.name},${resident.studentId},${resident.phone},${resident.checkInDate}\n`
        })
       
       // 创建下载链接
       const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
       const link = document.createElement('a')
       const url = URL.createObjectURL(blob)
       link.setAttribute('href', url)
       link.setAttribute('download', `${dormInfo.building}${dormInfo.room}_人员名单_${new Date().toISOString().split('T')[0]}.csv`)
       link.style.visibility = 'hidden'
       document.body.appendChild(link)
       link.click()
       document.body.removeChild(link)
       
      this.$message.success('人员名单导出成功')
    },
    
    
    // 处理房间操作下拉菜单命令
    handleRoomCommand(command) {
      switch (command) {
        case 'auto-fill':
          this.autoFillRoomsForCurrentTarget()
          break
        case 'batch-unassign':
          this.batchUnassignCurrentTargetInFloors()
          break
        case 'unallocated-students':
          this.openUnallocatedStudentsDialog()
          break
      }
    },

    // 显示选择专业提示
    showSelectMajorTip() {
      this.$message.warning('请先选择专业')
    },

    // 获取专业的实际学生数量（从学生数据库，按当前楼栋性别筛选）
    getActualStudentCount(majorName) {
      const studentsDB = JSON.parse(localStorage.getItem('studentsDatabase') || '{}')
      const buildingGender = this.selectedBuilding?.gender
      let students = Object.values(studentsDB).filter(s => s.major === majorName)
      
      // 如果有楼栋性别信息，只统计对应性别的学生
      if (buildingGender) {
        // 使用中文性别：'男'/'女'
        students = students.filter(s => s.gender === buildingGender)
      }
      
      return students.length
    },

    // 自动为房间分配班级（根据班级学生人数需求连续分配）
    autoAssignClassesToRooms(selectedRooms, classes, majorKey, majorName, allById) {
      if (!selectedRooms.length || !classes.length) {
        console.log(`❌ 分配失败: selectedRooms=${selectedRooms.length}, classes=${classes.length}`)
        return
      }

      let roomIndex = 0
      console.log(`🏠 开始自动分配: ${classes.length}个班级, ${selectedRooms.length}间房间`)
      
      // 按班级顺序连续分配房间，根据每班学生人数确定需要的房间数
      classes.forEach((classInfo, index) => {
        const studentCount = classInfo.studentCount || classInfo.totalSeats || 0
        let remainingStudents = studentCount
        const roomsForThisClass = []
        
        console.log(`📝 班级${index + 1}: ${classInfo.className}, 学生数: ${studentCount}`)
        
        // 为当前班级分配连续的房间，直到满足学生人数需求
        while (remainingStudents > 0 && roomIndex < selectedRooms.length) {
          const room = selectedRooms[roomIndex]
          const roomCapacity = room.totalBeds || 4 // 使用实际床位数，默认4个
          
          const belongTo = {
            buildingId: this.selectedBuildingId,
            majorKey,
            majorName,
            classKey: classInfo.classKey,
            className: classInfo.className
          }
          room.belongTo = belongTo
          if (allById.has(room.id)) {
            allById.get(room.id).belongTo = belongTo
          }
          
          roomsForThisClass.push(room.room)
          remainingStudents -= roomCapacity
          roomIndex++
        }
        
        console.log(`✅ ${classInfo.className}分配完成: ${roomsForThisClass.join(', ')}`)
      })

      // 如果还有剩余房间，按顺序分配给前面的班级作为备用
      if (roomIndex < selectedRooms.length) {
        let classIndex = 0
        while (roomIndex < selectedRooms.length && classIndex < classes.length) {
          const room = selectedRooms[roomIndex]
          const classInfo = classes[classIndex]
          const belongTo = {
            buildingId: this.selectedBuildingId,
            majorKey,
            majorName,
            classKey: classInfo.classKey,
            className: classInfo.className
          }
          room.belongTo = belongTo
          if (allById.has(room.id)) {
            allById.get(room.id).belongTo = belongTo
          }
          roomIndex++
          classIndex = (classIndex + 1) % classes.length // 循环分配剩余房间
        }
      }
    },


    // 从学生数据中提取班级信息
    extractClassesFromStudentData(majorName) {
      const studentsDB = JSON.parse(localStorage.getItem('studentsDatabase') || '{}')
      const buildingGender = this.selectedBuilding?.gender
      let students = Object.values(studentsDB).filter(s => s.major === majorName)
      
      // 如果有楼栋性别信息，只统计对应性别的学生
      if (buildingGender) {
        students = students.filter(s => s.gender === buildingGender)
      }
      
      console.log(`📊 提取班级信息 - 专业: ${majorName}, 楼栋性别: ${buildingGender}, 学生数: ${students.length}`)
      console.log(`   学生性别分布:`, students.slice(0, 3).map(s => s.gender))
      
      if (students.length === 0) {
        return []
      }

      // 统计各班级的学生数量
      const classStats = {}
      students.forEach(student => {
        const className = student.className || student.class
        if (className) {
          const classKey = student.classId || className
          if (!classStats[classKey]) {
            classStats[classKey] = {
              classKey: classKey,
              className: className,
              studentCount: 0
            }
          }
          classStats[classKey].studentCount++
        }
      })

      // 转换为班级配额格式（每个学生需要1个床位）
      return Object.values(classStats).map(cls => ({
        classKey: cls.classKey,
        className: cls.className,
        totalSeats: cls.studentCount,
        usedSeats: 0,
        remainingSeats: cls.studentCount
      }))
    },


    // ==================== 区域卡片数据获取方法 ====================
    
    // 获取区域的分配数据（基于房间的belongTo属性）
    getAreaAllocationData(area) {
      if (!area || !this.selectedBuildingId) {
        return { majors: [], classes: [] }
      }
      
      const floorSet = new Set(area.floors)
      const roomsInArea = this.dorms.filter(d => 
        d.buildingId === this.selectedBuildingId && floorSet.has(d.floor)
      )
      
      const majorMap = new Map()
      const classMap = new Map()
      
      roomsInArea.forEach(room => {
        if (room.belongTo) {
          const { majorKey, majorName, classKey, className } = room.belongTo
          
          // 收集专业信息
          if (majorKey && majorName) {
            if (!majorMap.has(majorKey)) {
              majorMap.set(majorKey, {
                majorKey,
                majorName,
                mode: classKey ? 'class-only' : 'full-major',
                classCount: 0
              })
            }
            if (classKey) {
              majorMap.get(majorKey).classCount++
            }
          }
          
          // 收集班级信息
          if (classKey && className) {
            const key = `${majorKey}_${classKey}`
            if (!classMap.has(key)) {
              // 从 professionalChips 中查找该班级的学生数
              let studentCount = 0
              const chip = this.professionalChips.find(c => 
                c.majorKey === majorKey && c.classKey === classKey
              )
              if (chip) {
                studentCount = chip.totalSeats || chip.studentCount || 0
              }
              
              classMap.set(key, {
                classKey,
                className,
                majorName,
                majorKey,
                studentCount
              })
            }
          }
        }
      })
      
      return {
        majors: Array.from(majorMap.values()),
        classes: Array.from(classMap.values())
      }
    },
    
    // 获取区域的房间数量
    getAreaRoomCount(area) {
      if (!area || !this.selectedBuildingId) return 0
      const floorSet = new Set(area.floors)
      return this.dorms.filter(d => 
        d.buildingId === this.selectedBuildingId && floorSet.has(d.floor)
      ).length
    },
    
    // 获取区域的学生数量（统计分配的专业/班级的学生总数）
    getAreaStudentCount(area) {
      if (!area || !this.selectedBuildingId) return 0
      
      // 获取该区域的所有分配数据
      const allocationData = this.getAreaAllocationData(area)
      
      // 统计所有班级的学生数
      let totalStudents = 0
      
      // 统计班级学生数
      if (allocationData.classes && allocationData.classes.length > 0) {
        totalStudents += allocationData.classes.reduce((sum, cls) => {
          return sum + (cls.studentCount || 0)
        }, 0)
      }
      
      // 如果没有班级数据，尝试从专业估算
      if (totalStudents === 0 && allocationData.majors && allocationData.majors.length > 0) {
        // 从房间分配估算：房间数 × 平均床位数
        const floorSet = new Set(area.floors)
        const roomsInArea = this.dorms.filter(d => 
          d.buildingId === this.selectedBuildingId && 
          floorSet.has(d.floor) && 
          d.belongTo
        )
        
        totalStudents = roomsInArea.reduce((sum, room) => {
          return sum + (room.totalBeds || room.bedCount || 4)
        }, 0)
      }
      
      return totalStudents
    },
    
    // 显示区域分配详情（查看全部专业班级）
    showAreaAllocationDetails(area) {
      const data = this.getAreaAllocationData(area)
      
      let content = `<div style="max-height: 450px; overflow-y: auto; padding: 8px;">`
      
      if (data.majors.length > 0) {
        content += `<div style="margin-bottom: 24px;">`
        content += `<h4 style="margin: 0 0 12px 0; color: #374151; font-size: 14px; font-weight: 600; display: flex; align-items: center;">
          <span style="display: inline-block; width: 3px; height: 14px; background: #3b82f6; margin-right: 8px; border-radius: 2px;"></span>
          专业列表 (${data.majors.length}个)
        </h4>`
        content += `<div style="display: flex; flex-wrap: wrap; gap: 6px;">`
        data.majors.forEach(major => {
          const bgColor = '#dbeafe'
          const borderColor = '#3b82f6'
          const textColor = '#1e40af'
          content += `<span style="display: inline-flex; align-items: center; gap: 4px; padding: 6px 10px; background: ${bgColor}; border: 1px solid ${borderColor}; border-radius: 6px; font-size: 12px; color: ${textColor}; font-weight: 500;">
            ${major.majorName}
          </span>`
        })
        content += `</div></div>`
      }
      
      if (data.classes.length > 0) {
        content += `<div>`
        content += `<h4 style="margin: 0 0 12px 0; color: #374151; font-size: 14px; font-weight: 600; display: flex; align-items: center;">
          <span style="display: inline-block; width: 3px; height: 14px; background: #10b981; margin-right: 8px; border-radius: 2px;"></span>
          班级列表 (${data.classes.length}个)
        </h4>`
        content += `<div style="display: flex; flex-wrap: wrap; gap: 6px;">`
        data.classes.forEach(cls => {
          content += `<span style="display: inline-block; padding: 6px 10px; background: #ecfdf5; border: 1px solid #10b981; border-radius: 6px; font-size: 12px; color: #047857; font-weight: 500;" title="所属专业: ${cls.majorName}">${cls.className}</span>`
        })
        content += `</div></div>`
      }
      
      if (data.majors.length === 0 && data.classes.length === 0) {
        content += `<div style="text-align: center; padding: 40px 20px;">
          <div style="font-size: 48px; color: #e5e7eb; margin-bottom: 12px;">📋</div>
          <p style="color: #6b7280; font-size: 14px; margin: 0;">该区域暂未分配专业班级</p>
        </div>`
      }
      
      content += `</div>`
      
      this.$alert(content, `📍 ${area.name} - 分配详情`, {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定'
      })
    },
    
    // 用品管理相关方法
    async manageMaterials(room) {
      this.currentMaterialsRoom = room
      this.materialTabActive = 'materials'
      this.materialsDialogVisible = true
      await this.loadRoomMaterials()
      await this.loadRoomReports()
    },
    
    async loadRoomMaterials() {
      if (!this.currentMaterialsRoom) return
      
      this.roomMaterialsLoading = true
      try {
        // 从后端获取物资模板配置
        const { getMaterialTemplateByTypeAPI } = await import('@/api/config.js')
        const building = this.buildings.find(b => b.buildingName === this.currentMaterialsRoom.building)
        if (!building) {
          this.roomMaterials = []
          return
        }
        
        const roomType = this.currentMaterialsRoom.type || '四人间'
        const res = await getMaterialTemplateByTypeAPI(building.id, building.buildingType, roomType)
        
        if (res.code === 200 && res.data && res.data.materials) {
          const materials = JSON.parse(res.data.materials || '[]')
          this.roomMaterials = materials
        } else {
          this.roomMaterials = []
        }
      } catch (error) {
        console.error('加载物资配置失败:', error)
        this.roomMaterials = []
      } finally {
        this.roomMaterialsLoading = false
      }
    },
    
    async loadRoomReports() {
      if (!this.currentMaterialsRoom) return
      
      const { getDormReports } = await import('@/utils/materialReportData.js')
      const dormNumber = this.currentMaterialsRoom.room
      this.roomReports = getDormReports(dormNumber)
    },
    
    getMaterialCategoryType(category) {
      const types = {
        '家具': '',
        '电器': 'success',
        '日用品': 'warning',
        '其他': 'info'
      }
      return types[category] || ''
    },
    
    // 申报处理相关方法
    handleReport(report) {
      this.currentReport = report
      this.reportHandleForm = {
        status: report.status === '待处理' ? '处理中' : report.status,
        handleRemark: report.handleRemark || ''
      }
      this.handleReportDialog = true
    },
    
    viewReportDetail(report) {
      this.currentReport = report
      this.reportHandleForm = {
        status: report.status,
        handleRemark: report.handleRemark || ''
      }
      this.handleReportDialog = true
    },
    
    async submitReportHandle() {
      if (!this.reportHandleForm.handleRemark.trim()) {
        this.$message.warning('请填写处理备注')
        return
      }
      
      const { updateReportStatus } = await import('@/utils/materialReportData.js')
      
      const success = updateReportStatus(
        this.currentReport.id,
        this.reportHandleForm.status,
        this.reportHandleForm.handleRemark
      )
      
      if (success) {
        this.$message.success('处理成功！学生可在申报记录中查看处理结果')
        this.handleReportDialog = false
        await this.loadRoomReports()
      } else {
        this.$message.error('处理失败！')
      }
    },
    
    formatReportTime(timeStr) {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN')
    },
    
    getReportStatusType(status) {
      const types = {
        '待处理': 'warning',
        '处理中': 'primary',
        '已处理': 'success',
        '已拒绝': 'danger'
      }
      return types[status] || 'info'
    }
  }
}
</script>

<style scoped>
.admin-dorm-page {
  padding: 20px;
}

.building-section,
.dorm-list {
  margin-bottom: 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

/* 🏢 紧凑高效楼栋卡片 - 最大化信息密度 */
.buildings-compact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 12px;
  margin-top: 20px;
}

/* 🎨 紧凑卡片主容器 */
.building-card-compact {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.building-card-compact:hover {
  transform: translateY(-3px);
}

/* 🪞 紧凑玻璃容器 */
.compact-glass-container {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: 12px;
  background: linear-gradient(145deg, 
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.85) 50%,
    rgba(255, 255, 255, 0.95) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 1px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  overflow: hidden;
  transition: all 0.3s ease;
  min-height: 64px;
}

.building-card-compact:hover .compact-glass-container {
  box-shadow: 
    0 6px 24px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

/* 📍 左侧区域 - 图标和基本信息 */
.left-section {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 140px;
}

.building-icon-compact {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(100, 116, 139, 0.3);
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.building-card-compact:hover .building-icon-compact {
  transform: scale(1.05);
}

.building-basic-info {
  display: flex !important;
  flex-direction: column !important;
  gap: 4px !important;
  flex: 1 !important;
  min-width: 80px !important; /* 确保有足够空间显示文字 */
  max-width: none !important;
}

.building-name-compact {
  font-size: 18px !important;
  font-weight: 800 !important;
  color: #0f172a !important;
  margin: 0 0 4px 0 !important;
  line-height: 1.2 !important;
  white-space: nowrap !important;
  overflow: visible !important;
  letter-spacing: 0.3px !important;
  display: block !important;
  opacity: 1 !important;
  visibility: visible !important;
}

.building-type-compact {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 12px;
  letter-spacing: 0.2px;
  background: rgba(100, 116, 139, 0.08);
  color: #64748b;
  border: 1px solid rgba(100, 116, 139, 0.15);
  display: inline-block;
  width: fit-content;
}

.building-type-compact.male {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.2);
}

.building-type-compact.female {
  background: rgba(236, 72, 153, 0.1);
  color: #ec4899;
  border-color: rgba(236, 72, 153, 0.2);
}

.building-type-compact.mixed {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
  border-color: rgba(168, 85, 247, 0.2);
}

/* 📊 中间区域 - 统计数据 */
.middle-section {
  display: flex;
  gap: 12px;
  flex: 1;
  justify-content: center;
}

.stat-compact {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  border-radius: 6px;
  background: rgba(248, 250, 252, 0.6);
  border: 1px solid rgba(226, 232, 240, 0.4);
  transition: all 0.3s ease;
  min-width: 0;
  flex: 1;
}

.stat-compact:hover {
  background: rgba(248, 250, 252, 0.8);
  transform: translateY(-1px);
}

.stat-icon-mini {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: linear-gradient(135deg, #64748b, #475569);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 10px;
  flex-shrink: 0;
}

.stat-value {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
}

.stat-unit {
  font-size: 9px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
}

/* 📈 右侧区域 - 进度条和状态 */
.right-section {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.progress-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-bar-mini {
  height: 4px;
  background: rgba(226, 232, 240, 0.4);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill-mini {
  height: 100%;
  border-radius: 2px;
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.progress-fill-mini::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: miniShimmer 2s infinite;
}

@keyframes miniShimmer {
  0% { left: -100%; }
  100% { left: 100%; }
}

.progress-fill-mini.low {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.progress-fill-mini.medium {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.progress-fill-mini.high {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.status-text-mini {
  font-size: 9px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-align: center;
}

.selection-indicator-compact {
  width: 16px;
  height: 16px;
  position: relative;
  flex-shrink: 0;
}

.indicator-dot-mini {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e8f0;
  transition: all 0.3s ease;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 📝 描述信息（紧凑版） */
.description-compact {
  position: absolute;
  bottom: -24px;
  left: 20px;
  right: 20px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(226, 232, 240, 0.5);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 11px;
  color: #64748b;
  line-height: 1.3;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.3s ease;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.building-card-compact:hover .description-compact {
  opacity: 1;
  transform: translateY(0);
}

/* 🪞 镜面反射效果（紧凑版） */
.mirror-effect-compact {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.1) 0%,
    transparent 30%,
    transparent 70%,
    rgba(255, 255, 255, 0.05) 100%
  );
  pointer-events: none;
}

.building-card-compact:hover .mirror-effect-compact {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.15) 0%,
    transparent 25%,
    transparent 75%,
    rgba(255, 255, 255, 0.08) 100%
  );
}

/* ✨ 选中状态效果 - 增强视觉反馈 */
.building-card-compact.selected .compact-glass-container {
  background: linear-gradient(
    135deg,
    rgba(16, 185, 129, 0.05) 0%,
    rgba(16, 185, 129, 0.02) 100%
  );
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.08),
    0 1px 4px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.building-card-compact.selected .indicator-dot-mini {
  background: linear-gradient(135deg, #10b981, #34d399);
  animation: compactPulse 1.5s ease infinite;
}

@keyframes compactPulse {
  0%, 100% { 
    transform: translate(-50%, -50%) scale(1);
  }
  50% { 
    transform: translate(-50%, -50%) scale(1.2);
  }
}

/* 📱 响应式设计 - 紧凑版 */
@media (max-width: 768px) {
  .buildings-compact-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .compact-glass-container {
    padding: 12px 16px;
    gap: 12px;
    min-height: 56px;
  }
  
  .left-section {
    min-width: 120px;
    gap: 10px;
  }
  
  .building-icon-compact {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }
  
  .building-name-compact {
    font-size: 16px;
    font-weight: 700;
  }
  
  .building-type-compact {
    font-size: 10px;
    padding: 2px 8px;
  }
  
  .middle-section {
    gap: 16px;
  }
  
  .stat-compact {
    padding: 6px 10px;
    gap: 4px;
  }
  
  .stat-icon-mini {
    width: 18px;
    height: 18px;
    font-size: 9px;
  }
  
  .stat-value {
    font-size: 14px;
  }
  
  .stat-unit {
    font-size: 9px;
  }
  
  .right-section {
    min-width: 100px;
    gap: 10px;
  }
  
  .status-text-mini {
    font-size: 8px;
  }
  
  .description-compact {
    bottom: -20px;
    left: 16px;
    right: 16px;
    padding: 4px 8px;
    font-size: 10px;
  }
}

@media (max-width: 560px) {
  .buildings-compact-grid {
    grid-template-columns: 1fr;
  }
  
  .compact-glass-container {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 12px;
  }
  
  .left-section, .middle-section, .right-section {
    width: 100%;
    min-width: auto;
  }
  
  .left-section {
    justify-content: flex-start;
  }
  
  .middle-section {
    justify-content: space-around;
  }
  
  .right-section {
    justify-content: space-between;
  }
  
  .description-compact {
    position: relative;
    bottom: auto;
    left: auto;
    right: auto;
    margin-top: 8px;
    opacity: 1;
    transform: none;
  }
}

 .building-actions {
   margin-top: 15px;
   display: flex;
   gap: 8px;
   justify-content: center;
 }

.action-buttons-cell {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
}

 .action-buttons-cell .el-button {
   margin: 0;
   padding: 6px 12px;
   font-size: 12px;
   min-width: 60px;
 }

.search-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* chips 空态提示下移，避免遮挡按钮区 */
.empty-tip {
  margin: 16px 0 8px 0;
}

/* 房间分配工具条布局优化 */
.rooms-assign-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

/* 🎨 统一分配管理容器 */
.allocation-management-container {
  width: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 顶部工具栏 */
.management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.current-target {
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 8px;
}

/* 主要内容区域 - 左右分栏 */
.management-content {
  display: flex;
  flex-direction: row;
  gap: 0;
}

/* 左侧：分配范围 */
.allocation-scope {
  flex: 0 0 350px;
  width: 350px;
  padding: 20px;
  border-right: 1px solid #e2e8f0;
  background: #fafbfc;
  max-height: 505px;
  overflow-y: auto;
}

.scope-content {
  height: 100%;
}

.chips-section {
  margin-bottom: 16px;
}

.chips-section:last-child {
  margin-bottom: 0;
}

.chips-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.label-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chips-label .el-icon {
  width: 18px;
  height: 18px;
  color: #3b82f6;
}

.batch-mode-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.batch-mode-controls .el-switch {
  margin-right: 0;
  --el-switch-on-color: #409eff;
}

.batch-mode-controls .el-switch__label {
  color: #606266;
  font-size: 13px;
}

.batch-mode-controls .el-switch__label.is-active {
  color: #409eff;
}

.batch-mode-controls .el-button {
  font-weight: 500;
  min-width: 70px;
  background-color: #409eff !important;
  color: #ffffff !important;
  border-color: #409eff !important;
}

.batch-mode-controls .el-button:hover {
  background-color: #66b1ff !important;
  border-color: #66b1ff !important;
}

.chips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 10px;
}

.class-chips {
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
}

/* 🏷️ 分配芯片样式 */
.allocation-chip {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

/* 批量模式下选中的卡片 */
.allocation-chip.selected-for-batch {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  border-color: #0ea5e9;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.2);
}

/* 批量模式下的复选框 */
.batch-checkbox {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
}

.allocation-chip::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(59, 130, 246, 0.03) 50%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.allocation-chip:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.allocation-chip:hover::before {
  opacity: 1;
}

.allocation-chip.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #dbeafe 0%, #f0f9ff 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
}

.allocation-chip.over-allocated {
  border-color: #ef4444;
  background: linear-gradient(135deg, #fecaca 0%, #fef2f2 100%);
}

.chip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.chip-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.chip-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.stat-item {
  font-weight: 500;
}

.stat-item strong {
  font-weight: 700;
  color: #334155;
}

.stat-item.remaining strong {
  color: #059669;
}

.stat-item.remaining.negative strong {
  color: #dc2626;
}

.stat-divider {
  color: #94a3b8;
  font-weight: 400;
}

.chip-progress {
  margin-top: 2px;
}

/* 班级芯片简化版 */
.class-chip .chip-stats {
  justify-content: center;
}

.stat-compact {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

/* 空状态 */
.scope-empty {
  padding: 40px 20px;
  text-align: center;
}

.scope-empty p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.empty-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

/* 右侧：房间分配 */
.room-assignment {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 12px;
}

.assignment-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  flex-shrink: 0;
}

.assignment-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.room-table-container {
  flex: 1;
  padding: 0;
  background: white;
  overflow: hidden;
}

.floor-empty-state {
  padding: 40px 20px;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.assignment-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  border-top: 1px solid #e2e8f0;
  margin-top: auto;
  flex-shrink: 0;
}

.text-placeholder {
  color: #cbd5e1;
  font-style: italic;
  font-size: 12px;
}

/* 批量分配预览对话框样式 */
.batch-assignment-preview-dialog {
  width: 800px;
  max-width: 90vw;
}

.batch-assignment-preview-dialog .el-message-box__message {
  max-height: 600px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .chips-grid {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }
}

@media (max-width: 1200px) {
  /* 注释掉column布局，保持左右布局 */
  /* .management-content {
    flex-direction: column;
  } */
  
  /* .allocation-scope {
    border-right: none;
    border-bottom: 1px solid #e2e8f0;
  } */
  
  .chips-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}

@media (max-width: 768px) {
  .management-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .assignment-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .assignment-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  
  .chips-grid {
    grid-template-columns: 1fr;
  }
}

 @media (max-width: 768px) {
   .section-header {
     flex-direction: column;
     align-items: flex-start;
     gap: 10px;
   }
   
   .search-bar {
     flex-direction: column;
     align-items: stretch;
   }
   
   .building-actions {
     flex-direction: column;
   }
   
   .action-buttons-cell {
     flex-direction: column;
     gap: 4px;
   }
   
    .action-buttons-cell .el-button {
   width: 100%;
   justify-content: center;
 }
}

/* 宿舍人员名单对话框样式 */
.dorm-residents-dialog {
  .dorm-info-card {
    margin-bottom: 20px;
    
    .dorm-info-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .info-item {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      
      label {
        font-weight: bold;
        color: #606266;
        margin-right: 8px;
        min-width: 60px;
      }
      
      span {
        color: #303133;
      }
    }
  }
  
  .residents-table-card {
    .el-table {
      .el-table__header {
        background-color: #f5f7fa;
      }
    }
  }
}
/* 旧的楼栋卡片样式已移除，使用新的紧凑高效设计 */

.no-buildings {
  text-align: center;
  padding: 40px 0;
}

.no-building-selected {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.03), rgba(64, 158, 255, 0.01));
  border-radius: 12px;
  border: 2px dashed rgba(64, 158, 255, 0.2);
  margin: 20px 0;
}

.hint-text {
  color: #909399;
  font-size: 14px;
  margin-top: 8px;
}

/* 🎨 区域管理对话框样式美化 */
.area-management-dialog {
  --el-dialog-margin-top: 2vh;
  
  /* 🚀 关键修复：隔离对话框，避免全局CSS影响 */
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 2000 !important;
  
  /* 重置全局flex布局的影响 */
  display: block !important;
  place-items: unset !important;
  
  /* 确保对话框容器不受全局样式影响 */
  .el-overlay {
    position: fixed !important;
    top: 0 !important;
    left: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
  }
}

.area-management-dialog .el-dialog {
  margin: 2vh auto !important;
  max-height: 96vh !important;
  height: 96vh !important;
  width: 95% !important;
  max-width: 1400px !important;
  display: flex !important;
  flex-direction: column !important;
  
  /* 确保对话框本身不受全局布局影响 */
  position: relative !important;
  transform: none !important;
}

.area-management-dialog .el-dialog__body {
  flex: 1;
  overflow: hidden;
  padding: 0;
}

.area-management-content {
  height: calc(96vh - 140px);
  overflow-y: auto;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  
  /* 优化滚动条样式 */
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 #f1f1f1;
  
  /* 确保内容区域不会超出对话框 */
  max-height: calc(96vh - 140px);
  min-height: 400px;
}

.area-management-content::-webkit-scrollbar {
  width: 8px;
}

.area-management-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.area-management-content::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 4px;
}

.area-management-content::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #5a6fd8, #6a4190);
}

/* 🏢 楼栋信息卡片美化 */
.building-info-card {
  margin-bottom: 24px;
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
  overflow: hidden;
  position: relative;
}

.building-info-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='20' height='20' viewBox='0 0 20 20' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Ccircle cx='3' cy='3' r='3'/%3E%3Ccircle cx='13' cy='13' r='3'/%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
}

.building-info-card :deep(.el-card__header) {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  padding: 20px 24px;
  backdrop-filter: blur(10px);
}

.building-info-card :deep(.el-card__body) {
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.05);
}

.building-info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 18px;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.building-info-header .el-button {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 600;
  backdrop-filter: blur(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.building-info-header .el-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.building-details {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.building-details .el-tag {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.building-desc {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  font-style: italic;
}

/* 🎯 区域容器美化 */
.areas-container {
  margin-top: 24px;
  position: relative;
}

.areas-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 8px;
  
  /* 响应式布局优化 */
  @media (min-width: 1200px) {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  }
  
  @media (min-width: 1600px) {
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  }
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

/* 🎯 现代简洁区域卡片 */
.area-card {
  border: none;
  border-radius: 16px;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.area-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, 
    rgba(102, 126, 234, 0.03) 0%, 
    rgba(118, 75, 162, 0.03) 50%, 
    rgba(240, 147, 251, 0.03) 100%);
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}

.area-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, 
    transparent 0%,
    #667eea 20%,
    #764ba2 50%,
    #f093fb 80%,
    transparent 100%);
  transform: scaleX(0);
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: center;
}

.area-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  border-color: #667eea;
}

.area-card:hover::before {
  opacity: 1;
}

.area-card:hover::after {
  transform: scaleX(1);
}

.area-card :deep(.el-card__header) {
  background: #fafbfc;
  border: none;
  padding: 24px 24px 16px;
  position: relative;
  border-bottom: 1px solid #f0f0f0;
}

.area-card :deep(.el-card__body) {
  padding: 0 24px 24px;
  background: transparent;
}

.area-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.area-name {
  font-size: 18px;
  font-weight: 800;
  color: #1a202c;
  display: flex;
  align-items: center;
  gap: 12px;
  letter-spacing: -0.025em;
  position: relative;
}

.area-name::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { 
    transform: scale(1); 
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
  }
  50% { 
    transform: scale(1.1); 
    box-shadow: 0 0 0 6px rgba(102, 126, 234, 0.1);
  }
}

.area-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.area-card:hover .area-actions {
  opacity: 1;
  transform: translateX(0);
}

.area-actions .el-button {
  border-radius: 10px;
  font-weight: 600;
  border: none;
  backdrop-filter: blur(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 36px;
  height: 36px;
  padding: 0;
}

.area-actions .el-button:hover {
  transform: translateY(-3px) scale(1.1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.area-actions .el-button[type="primary"] {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.area-actions .el-button[type="danger"] {
  background: linear-gradient(135deg, #ff6b6b, #ee5a52);
  color: white;
}

/* 🎨 简约精致区域信息布局 */
.area-info-premium {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 信息块 */
.info-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 块头部 */
.block-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.header-icon {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  background: #f8fafc;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.block-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  flex: 1;
}

.count-badge {
  background: #f1f5f9;
  color: #64748b;
  font-size: 10px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  min-width: 16px;
  text-align: center;
}

/* 楼层容器 */
.floors-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.floor-chip {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.floor-chip:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

/* 专业容器 */
.majors-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.major-chip {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s ease;
  cursor: default;
  max-width: 140px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.major-chip:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.major-name {
  font-size: 11px;
  font-weight: 500;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.mode-indicator {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 8px;
  font-weight: 600;
  flex-shrink: 0;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.mode-indicator.full-major {
  background: #334155;
  color: white;
  border-color: #334155;
}

.mode-indicator.class-only {
  background: #64748b;
  color: white;
  border-color: #64748b;
}

/* 班级容器 */
.classes-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.class-chip {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.15s ease;
  cursor: default;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.class-chip:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

/* 仅信息显示的班级芯片 */
.class-chip.info-only {
  cursor: default;
  opacity: 0.8;
}

.class-chip.info-only:hover {
  background: #f8fafc;
  border-color: #e2e8f0;
}

/* 更多按钮 */
.more-chip {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 10px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
}

.more-chip:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #475569;
}

/* 增强的查看更多按钮 */
.more-chip.view-more {
  gap: 2px;
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #6b7280;
}

.more-chip.view-more:hover {
  background: #e5e7eb;
  border-color: #9ca3af;
  color: #374151;
  transform: translateY(-1px);
}

.more-chip .more-icon {
  font-size: 8px;
  margin-left: 1px;
}

/* 模式标签样式 */
.mode-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

/* 帮助图标 */
.help-icon {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.95);
  cursor: help;
  transition: color 0.2s ease;
  margin-left: 2px;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.3);
}

.help-icon:hover {
  color: white;
  text-shadow: 0 0 3px rgba(0, 0, 0, 0.5);
}

/* 调试信息 */
.chip-debug {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px solid #f0f0f0;
}

.debug-info {
  font-size: 10px;
  color: #666;
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #e9ecef;
}

/* 简约空状态 */
.empty-state-premium {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: #fafbfc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
}

.empty-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 14px;
  border: 1px solid #e2e8f0;
}

.empty-text {
  font-size: 12px;
  font-weight: 500;
}

/* 简约统计信息 */
.stats-premium {
  margin-top: 12px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.stats-row {
  display: flex;
  gap: 8px;
}

.stat-card {
  flex: 1;
  background: #fafbfc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
  cursor: default;
}

.stat-card:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  line-height: 1;
}

.stat-label {
  font-size: 10px;
  color: #64748b;
  font-weight: 500;
  margin-top: 2px;
}

/* 🎨 增强版区域信息布局（保留兼容） */
.area-info-enhanced {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 信息节 */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.section-header .el-icon {
  font-size: 14px;
  color: #6b7280;
}

.section-title {
  flex: 1;
}

/* 楼层显示 */
.floors-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.floor-tag {
  font-size: 11px;
  font-weight: 500;
}

/* 专业显示 */
.majors-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.major-tag {
  font-size: 11px;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 班级显示 */
.classes-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.class-tag {
  font-size: 11px;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 查看更多按钮 */
.view-more-btn {
  font-size: 11px;
  padding: 2px 8px;
  height: 22px;
  color: #6b7280;
  text-decoration: none;
}

.view-more-btn:hover {
  color: #374151;
  background-color: #f3f4f6;
}

/* 空状态 */
.empty-allocation {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  color: #6b7280;
  font-size: 12px;
}

.empty-allocation .el-icon {
  font-size: 14px;
  color: #9ca3af;
}

/* 增强版统计信息 */
.area-stats-enhanced {
  padding: 12px 0 0;
  border-top: 1px solid #f1f5f9;
  margin-top: 8px;
}

.stat-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-number {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.stat-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 500;
}

.stat-item.rooms .stat-number {
  color: #059669;
}

.stat-item.floors .stat-number {
  color: #0369a1;
}

.stat-item.students .stat-number {
  color: #dc2626;
}

.stat-divider {
  color: #d1d5db;
  font-weight: 300;
}

/* 🎨 简约信息布局设计 - 区域信息（保留兼容） */
.area-info-simple {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 📋 信息组 */
.info-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.info-title {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.info-count {
  background: #f1f5f9;
  color: #64748b;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
}

/* 🏢 楼层显示 */
.floors-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.floor-badge {
  background: #3b82f6;
  color: white;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.floor-badge:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

/* 🎓 专业显示 */
.majors-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.major-badge {
  background: #8b5cf6;
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  transition: all 0.2s ease;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.major-badge:hover {
  background: #7c3aed;
  transform: translateY(-1px);
}

/* 📚 班级显示 */
.classes-display {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.class-badge {
  background: #10b981;
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  transition: all 0.2s ease;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.class-badge:hover {
  background: #047857;
  transform: translateY(-1px);
}

/* 📊 底部统计 */
.area-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #f1f5f9;
  margin-top: 8px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
}

.stat-divider {
  color: #cbd5e1;
  font-size: 14px;
}

.gender-value.male {
  color: #3b82f6;
}

.gender-value.female {
  color: #ec4899;
}

/* 旧的详细信息样式已移除 */

/* 旧样式已移除，使用新的详细信息布局 */

/* 标签容器样式已整合到上面的专业和班级样式中 */

/* 🚫 无区域状态美化 */
.no-areas {
  text-align: center;
  padding: 60px 20px;
  color: #6c757d;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 16px;
  border: 2px dashed #dee2e6;
  position: relative;
  overflow: hidden;
  margin: 20px 0;
}

.no-areas::before {
  content: '🏢';
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
  opacity: 0.6;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.no-areas .el-empty {
  --el-empty-description-color: #6c757d;
}

.no-areas .el-button {
  margin-top: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 25px;
  padding: 12px 24px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.no-areas .el-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.no-areas .el-button:hover::before {
  left: 100%;
}

.no-areas .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

/* 楼层选择器样式 */
.floors-selector {
  width: 100%;
}

/* 楼层提示信息 */
.floor-info-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #e1f3ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #409eff;
}

.floor-info-tip strong {
  color: #0066cc;
  font-weight: 600;
}

.floor-warning-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff7e6;
  border: 1px solid #ffd666;
  border-radius: 4px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #d48806;
}

.floors-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(50px, 1fr));
  gap: 6px;
  margin-bottom: 16px;
  max-width: 400px;
}

.floor-option {
  position: relative;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 6px 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
  min-height: 40px;
  min-width: 50px;
  max-width: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
}

.floor-option:hover:not(.disabled) {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.floor-option.selected {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border-color: #1d4ed8;
  color: white;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

.floor-option.disabled {
  background: #f8fafc;
  border-color: #cbd5e1;
  cursor: not-allowed;
  opacity: 0.6;
}

.floor-option.occupied {
  border-color: #ef4444;
  background: #fef2f2;
}

.floor-number {
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}

.floor-option.selected .floor-number {
  color: white;
}

.floor-option.disabled .floor-number {
  color: #9ca3af;
}

.floor-label {
  font-size: 9px;
  font-weight: 500;
  color: #64748b;
  line-height: 1;
}

.floor-option.selected .floor-label {
  color: rgba(255, 255, 255, 0.9);
}

.floor-option.disabled .floor-label {
  color: #9ca3af;
}

/* 超出楼栋实际楼层的楼层样式 */
.floor-option.unavailable {
  background: #f1f5f9;
  border-color: #cbd5e1;
  cursor: not-allowed;
  opacity: 0.4;
}

.floor-option.unavailable .floor-number {
  color: #94a3b8;
}

.floor-option.unavailable .floor-label {
  color: #94a3b8;
}

.occupied-indicator {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ef4444;
  color: white;
  font-size: 8px;
  font-weight: 600;
  padding: 1px 4px;
  border-radius: 6px;
  white-space: nowrap;
  box-shadow: 0 1px 3px rgba(239, 68, 68, 0.3);
}

.floors-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
}

.summary-text {
  color: #475569;
  font-weight: 500;
}

.selected-floors {
  color: #3b82f6;
  font-weight: 600;
}

.clear-btn {
  font-size: 12px;
  color: #64748b;
  padding: 4px 8px;
}

.clear-btn:hover {
  color: #ef4444;
  background-color: #fef2f2;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .floors-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
  }
  
  .floor-option {
    padding: 8px 6px;
    min-height: 48px;
  }
  
  .floor-number {
    font-size: 14px;
  }
  
  .floor-label {
    font-size: 9px;
  }
  
  .occupied-indicator {
    font-size: 7px;
    padding: 1px 3px;
  }
  
  .floors-summary {
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .floors-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .floor-option {
    padding: 6px 4px;
    min-height: 42px;
  }
  
  .floor-number {
    font-size: 12px;
  }
}

.floor-occupied {
  color: #f56c6c;
  font-size: 12px;
  margin-left: 4px;
}

/* 响应式设计 */
/* 📱 响应式设计优化 */
@media (max-width: 768px) {
  .area-management-dialog .el-dialog {
    width: 98% !important;
    margin: 1vh auto !important;
    max-height: 98vh !important;
  }
  
  .area-management-content {
    height: calc(96vh - 120px);
    max-height: calc(96vh - 120px);
    padding: 12px;
  }
  
  .areas-grid {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 4px;
  }
  
  .building-info-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .area-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .building-info-card {
    margin-bottom: 16px;
  }
  
  /* Premium 区域卡片移动端优化 */
  .area-info-premium {
    gap: 16px;
  }
  
  .info-block {
    gap: 10px;
  }
  
  .block-header {
    gap: 6px;
  }
  
  .header-icon {
    width: 18px;
    height: 18px;
    font-size: 11px;
  }
  
  .block-title {
    font-size: 12px;
  }
  
  .count-badge {
    font-size: 9px;
    padding: 1px 5px;
  }
  
  .floors-container,
  .majors-container,
  .classes-container {
    gap: 4px;
  }
  
  .floor-chip {
    font-size: 10px;
    padding: 3px 6px;
  }
  
  .major-chip {
    padding: 4px 8px;
    max-width: 120px;
    gap: 4px;
  }
  
  .major-name {
    font-size: 10px;
  }
  
  .mode-indicator {
    width: 14px;
    height: 14px;
    font-size: 7px;
  }
  
  .class-chip {
    font-size: 10px;
    padding: 3px 8px;
    max-width: 90px;
  }
  
  .more-chip {
    font-size: 9px;
    padding: 3px 6px;
    min-width: 28px;
  }
  
  .empty-state-premium {
    padding: 16px;
    gap: 6px;
  }
  
  .empty-icon {
    width: 28px;
    height: 28px;
    font-size: 14px;
  }
  
  .empty-text {
    font-size: 11px;
  }
  
  .stats-premium {
    padding-top: 12px;
  }
  
  .stats-row {
    gap: 8px;
  }
  
  .stat-card {
    padding: 10px 6px;
  }
  
  .stat-value {
    font-size: 14px;
  }
  
  .stat-label {
    font-size: 9px;
  }
  
  /* 简约布局移动端优化（保留兼容） */
  .area-info-simple {
    gap: 16px;
  }
  
  .info-group {
    gap: 6px;
  }
  
  .floors-display,
  .majors-display,
  .classes-display {
    gap: 4px;
  }
  
  .floor-badge {
    font-size: 12px;
    padding: 3px 8px;
  }
  
  .major-badge,
  .class-badge {
    font-size: 10px;
    padding: 2px 6px;
    max-width: 100px;
  }
  
  .area-stats {
    padding: 12px 0;
    gap: 8px;
  }
  
  .stat-value {
    font-size: 16px;
  }
}

/* 🖥️ 大屏幕优化 */
@media (min-width: 1920px) {
  .areas-grid {
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 20px;
  }
}

/* Element Plus 样式调整 */
:deep(.el-checkbox-group) {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 8px;
}

:deep(.el-checkbox) {
  margin-right: 0;
  white-space: nowrap;
}

/* 🚀 修复添加区域对话框定位问题 */
.add-area-dialog {
  /* 隔离对话框，避免全局CSS影响 */
  .el-overlay {
    position: fixed !important;
    top: 0 !important;
    left: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    z-index: 2001 !important;
  }
}

.add-area-dialog .el-dialog {
  margin: 5vh auto !important;
  max-height: 90vh !important;
  width: 1400px !important;
  max-width: 95vw !important;
  position: relative !important;
  transform: none !important;
  
  /* 确保对话框内容可以滚动 */
  display: flex !important;
  flex-direction: column !important;
  
  .el-dialog__body {
    flex: 1;
    overflow-y: auto;
    max-height: calc(90vh - 120px);
    padding: 20px;
  }
  
  .el-dialog__footer {
    flex-shrink: 0;
    padding: 15px 20px 20px;
    border-top: 1px solid #ebeef5;
  }
}

/* 优化表单布局 */
.add-area-dialog .el-form {
  .el-form-item {
    margin-bottom: 22px;
  }
  
  .el-checkbox-group {
    max-height: 200px;
    overflow-y: auto;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 12px;
    background-color: #fafafa;
  }
  
  .el-select {
    width: 100%;
  }
}

/* 选择器管理样式 */
.select-with-management {
  .select-header {
    padding: 8px 12px;
    border-bottom: 1px solid #e4e7ed;
    background-color: #f5f7fa;
    margin: -12px -12px 8px -12px;
  }
  
  .option-with-delete {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px !important;
    
    .option-label {
      flex: 1;
      margin-right: 8px;
    }
    
    .delete-btn {
      opacity: 0;
      transition: opacity 0.2s ease;
      padding: 2px 4px;
      min-height: auto;
      height: 20px;
      width: 20px;
      
      .el-icon {
        font-size: 12px;
      }
    }
    
    &:hover .delete-btn {
      opacity: 1;
    }
  }
}

/* Element Plus 选择器下拉面板样式调整 */
:deep(.el-select-dropdown) {
  .el-select-dropdown__header {
    padding: 0;
  }
  
  .el-select-dropdown__item {
    padding: 0;
    height: auto;
    line-height: normal;
    
    &.option-with-delete {
      padding: 8px 12px;
      
      &:hover {
        background-color: #f5f7fa;
      }
    }
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .add-area-dialog .el-dialog {
    width: 95vw !important;
    margin: 2vh auto !important;
    max-height: 96vh !important;
    
    .el-dialog__body {
      max-height: calc(96vh - 100px);
      padding: 15px;
    }
  }
  
  .select-with-management {
    .option-with-delete .delete-btn {
      opacity: 1; /* 移动端始终显示删除按钮 */
    }
  }
}

</style>

