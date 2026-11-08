<template>
  <div class="building-management-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>楼栋管理</h2>
        </div>
      </template>

      <!-- 标签页区域 -->
      <el-tabs v-model="activeTab" class="main-tabs">
        <!-- 楼栋信息标签页 -->
        <el-tab-pane label="楼栋信息" name="building-info">
          <div class="building-info-content">
            <!-- 楼栋信息操作区 -->
            <div class="building-info-header">
              <div class="header-buttons">
                <el-button type="primary" @click="showAddBuildingDialog">
                  <template #icon>
                    <Plus />
                  </template>
                  添加楼栋
                </el-button>
              </div>
            </div>

            <!-- 搜索区域 -->
            <div class="building-search-area">
              <el-row :gutter="20" align="middle">
                <el-col :span="12">
                  <el-input
                    v-model="buildingSearchKeyword"
                    placeholder="搜索楼栋名称、类型或分配状态..."
                    clearable
                    @input="handleBuildingSearch"
                    @clear="clearBuildingSearch"
                    class="search-input"
                  >
                    <template #prefix>
                      <el-icon><Search /></el-icon>
                    </template>
                  </el-input>
                </el-col>
                <el-col :span="6">
                  <el-select
                    v-model="buildingSearchType"
                    placeholder="筛选类型"
                    clearable
                    @change="handleBuildingSearch"
                    style="width: 100%"
                  >
                    <el-option label="全部类型" value="" />
                    <el-option label="男生楼" value="男" />
                    <el-option label="女生楼" value="女" />
                  </el-select>
                </el-col>
                <el-col :span="6">
                  <el-select
                    v-model="buildingSearchStatus"
                    placeholder="筛选状态"
                    clearable
                    @change="handleBuildingSearch"
                    style="width: 100%"
                  >
                    <el-option label="全部状态" value="" />
                    <el-option label="已分配" value="assigned" />
                    <el-option label="未分配" value="unassigned" />
                  </el-select>
                </el-col>
              </el-row>
              
              <!-- 搜索结果提示 -->
              <div v-if="buildingSearchKeyword || buildingSearchType || buildingSearchStatus" class="search-result-tip">
                <el-tag type="info" size="small">
                  <el-icon><Search /></el-icon>
                  搜索结果：{{ filteredBuildings.length }} 个楼栋
                </el-tag>
                <el-button type="text" size="small" @click="clearAllBuildingSearch" style="margin-left: 10px;">
                  <el-icon><RefreshLeft /></el-icon>
                  清空筛选
                </el-button>
              </div>
            </div>

            <!-- 楼栋统计 -->
            <div class="building-stats">
              <el-row :gutter="20">
                <el-col :span="6">
                  <el-statistic title="总楼栋数" :value="availableBuildings.length" suffix="栋">
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em"><OfficeBuilding /></el-icon>
                    </template>
                  </el-statistic>
                </el-col>
                <el-col :span="6">
                  <el-statistic title="已分配" :value="getAssignedBuildingsCount()" suffix="栋">
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em"><Check /></el-icon>
                    </template>
                  </el-statistic>
                </el-col>
                <el-col :span="6">
                  <el-statistic title="未分配" :value="availableBuildings.length - getAssignedBuildingsCount()" suffix="栋">
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em"><Warning /></el-icon>
                    </template>
                  </el-statistic>
                </el-col>
                <el-col :span="6">
                  <el-statistic title="管理员数" :value="admins.filter(a => a.status === 'active').length" suffix="人">
                    <template #prefix>
                      <el-icon style="vertical-align: -0.125em"><User /></el-icon>
                    </template>
                  </el-statistic>
                </el-col>
              </el-row>
            </div>

            <!-- 楼栋信息表格 -->
            <div class="buildings-table">
               <el-table 
                 :data="filteredBuildings" 
                 style="width: 100%" 
                 :header-cell-style="{ background: '#f5f7fa' }"
                 row-key="id"
                 empty-text="暂无楼栋数据，请先添加楼栋"
               >
                <el-table-column prop="name" label="楼栋名称" min-width="140" align="center">
                  <template #default="scope">
                    <el-tooltip effect="dark" placement="top" :disabled="!scope.row.address && !scope.row.managerName && !scope.row.description">
                      <template #content>
                        <div style="max-width: 300px; line-height: 1.6;">
                          <div v-if="scope.row.address"><strong>详细地址：</strong>{{ scope.row.address }}</div>
                          <div v-if="scope.row.managerName"><strong>管理员信息：</strong>{{ scope.row.managerName }}<span v-if="scope.row.managerPhone"> ({{ scope.row.managerPhone }})</span></div>
                          <div v-if="scope.row.description" :style="(scope.row.address || scope.row.managerName) ? 'margin-top: 4px; padding-top: 4px; border-top: 1px solid rgba(255,255,255,0.2);' : ''"><strong>楼栋描述：</strong>{{ scope.row.description }}</div>
                        </div>
                      </template>
                      <div class="building-name">
                        <el-icon><OfficeBuilding /></el-icon>
                        <span>{{ scope.row.name }}</span>
                      </div>
                    </el-tooltip>
                  </template>
                </el-table-column>
                 <el-table-column prop="gender" label="类型" min-width="120" align="center">
                   <template #default="scope">
                     <el-tag 
                       :type="scope.row.gender === '男' ? 'primary' : 'danger'" 
                       size="small"
                     >
                       {{ scope.row.gender + '生楼' }}
                     </el-tag>
                   </template>
                 </el-table-column>
                <el-table-column label="设计容量" min-width="120" align="center">
                  <template #default="scope">
                    <div class="capacity-display">
                      <el-icon><User /></el-icon>
                      <span>{{ getBuildingCapacity(scope.row) }}人</span>
                    </div>
                  </template>
                </el-table-column>
                 <el-table-column label="房间类型" min-width="120" align="center">
                   <template #default="scope">
                     <el-tag type="info" size="small">
                       {{ getBuildingRoomType(scope.row) }}
                     </el-tag>
                   </template>
                 </el-table-column>
                <el-table-column label="分配管理员" min-width="150" align="center">
                  <template #default="scope">
                    <div class="assignment-status" :key="`assignment-${scope.row.id}-${assignmentUpdateCounter}`">
                      <template v-if="getBuildingAssignee(scope.row.id)">
                        <el-tag type="success" size="small">
                          <el-icon><User /></el-icon>
                          {{ getBuildingAssignee(scope.row.id) }}
                        </el-tag>
                      </template>
                      <template v-else>
                        <el-tag type="info" size="small">未分配</el-tag>
                      </template>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="专业分配状态" min-width="150" align="center">
                  <template #default="scope">
                    <div class="major-assignment-status">
                      <template v-if="getBuildingMajorAssignment(scope.row.id)">
                        <el-tag type="success" size="small">
                          <el-icon><School /></el-icon>
                          已分配专业
                        </el-tag>
                      </template>
                      <template v-else>
                        <el-tag type="warning" size="small">未分配专业</el-tag>
                      </template>
                    </div>
                  </template>
                </el-table-column>

                 <el-table-column label="操作" min-width="240" align="center">
                   <template #default="scope">
                     <div class="building-actions">
                      <!-- 编辑楼栋 -->
                      <el-tooltip content="编辑楼栋" placement="top">
                        <span class="op-btn-wrap">
                          <el-button size="small" type="info" @click="editBuilding(scope.row)" circle>
                            <template #icon>
                              <Edit />
                            </template>
                          </el-button>
                        </span>
                      </el-tooltip>

                      <!-- 分配管理员 -->
                      <el-tooltip content="分配管理员" placement="top">
                        <span class="op-btn-wrap">
                          <el-button 
                            size="small" 
                            type="warning" 
                            @click="quickAssignBuilding(scope.row)"
                            circle
                          >
                            <template #icon>
                              <User />
                            </template>
                          </el-button>
                        </span>
                      </el-tooltip>

                      <!-- 专业分配：统一结构 + 引导式交互（无宿舍提示并直达添加） -->
                      <el-tooltip
                        :content="getBuildingRoomCount(scope.row.id) === 0 
                          ? '请先添加宿舍后再进行专业分配' 
                          : getBuildingMajorAssignment(scope.row.id) 
                            ? '重新分配专业' 
                            : '分配专业'"
                        placement="top"
                      >
                        <span class="op-btn-wrap">
                          <el-button 
                            size="small" 
                            :type="getBuildingMajorAssignment(scope.row.id) ? 'warning' : 'success'" 
                            @click="onMajorAssignClick(scope.row)"
                            circle
                          >
                            <template #icon>
                              <School />
                            </template>
                          </el-button>
                        </span>
                      </el-tooltip>

                      <!-- 删除楼栋 -->
                      <el-tooltip content="删除楼栋" placement="top">
                        <span class="op-btn-wrap">
                          <el-button 
                            size="small" 
                            type="danger" 
                            @click="deleteBuilding(scope.row)"
                            circle
                          >
                            <template #icon>
                              <Delete />
                            </template>
                          </el-button>
                        </span>
                      </el-tooltip>
                    </div>
                   </template>
                 </el-table-column>
               </el-table>
             </div>
           </div>
         </el-tab-pane>

        <!-- 宿舍管理标签页 -->
        <el-tab-pane label="宿舍管理" name="dorm-management">
          <div class="dorm-management-content">
            <!-- 宿舍管理整合头部 -->
            <div class="dorm-management-unified-header">
              <!-- 头部信息和操作区 -->
              <div class="header-section">
                <div class="header-content">
                  <div class="header-icon">
                    <el-icon><Grid /></el-icon>
                  </div>
                  <div class="header-text">
                    <h3>宿舍信息管理</h3>
                    <p>管理各楼栋的宿舍房间信息，包括批量操作和详情查看</p>
                  </div>
                </div>
                <div class="header-actions">
                  <el-button type="primary" @click="showBatchAddDialog">
                    <template #icon>
                      <Plus />
                    </template>
                    批量添加宿舍
                  </el-button>
                  <el-button type="success" @click="showAddDormDialog">
                    <template #icon>
                      <Plus />
                    </template>
                    添加宿舍
                  </el-button>
                </div>
              </div>

              <!-- 统计信息区 -->
              <div class="statistics-section">
                <el-row :gutter="20">
                  <el-col :span="6">
                    <el-statistic title="总宿舍数" :value="totalDormsCount" suffix="间">
                      <template #prefix>
                        <el-icon style="vertical-align: -0.125em"><Grid /></el-icon>
                      </template>
                    </el-statistic>
                  </el-col>
                  <el-col :span="6">
                    <el-statistic title="总床位数" :value="totalBedsCount" suffix="床">
                      <template #prefix>
                        <el-icon style="vertical-align: -0.125em"><User /></el-icon>
                      </template>
                    </el-statistic>
                  </el-col>
                  <el-col :span="6">
                    <el-statistic title="已入住" :value="occupiedBedsCount" suffix="床">
                      <template #prefix>
                        <el-icon style="vertical-align: -0.125em"><Check /></el-icon>
                      </template>
                    </el-statistic>
                  </el-col>
                  <el-col :span="6">
                    <el-statistic title="空余床位" :value="availableBedsCount" suffix="床">
                      <template #prefix>
                        <el-icon style="vertical-align: -0.125em"><Plus /></el-icon>
                      </template>
                    </el-statistic>
                  </el-col>
                </el-row>
              </div>
            </div>


            <!-- 各楼栋宿舍概况表格 -->
            <div class="dorms-table">
              <div class="table-header">
                <div class="table-title">
                  <h4>各楼栋宿舍概况</h4>
                </div>
              </div>
              <el-table 
                :data="buildingDormSummaryData" 
                style="width: 100%" 
                :header-cell-style="{ background: '#f5f7fa' }"
                row-key="buildingId"
                empty-text="暂无宿舍数据，请先添加宿舍"
              >
                <el-table-column prop="buildingName" label="楼栋名称" min-width="150" align="center">
                  <template #default="scope">
                    <div class="building-name">
                      <el-icon><OfficeBuilding /></el-icon>
                      <span>{{ scope.row.buildingName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="gender" label="类型" width="120" align="center">
                  <template #default="scope">
                    <el-tag 
                      :type="scope.row.gender === '男' ? 'primary' : 'danger'" 
                      size="small"
                    >
                      {{ scope.row.gender }}生楼
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="totalRooms" label="宿舍数量" min-width="130" align="center">
                  <template #default="scope">
                    <div class="count-display">
                      <el-icon><Grid /></el-icon>
                      <span>{{ scope.row.totalRooms }}间</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="totalBeds" label="床位总数" min-width="130" align="center">
                  <template #default="scope">
                    <div class="count-display">
                      <el-icon><User /></el-icon>
                      <span>{{ scope.row.totalBeds }}床</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="occupiedBeds" label="已入住" min-width="120" align="center">
                  <template #default="scope">
                    <div class="count-display occupied">
                      <el-icon><Check /></el-icon>
                      <span>{{ scope.row.occupiedBeds }}床</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="入住率" min-width="200" align="center">
                  <template #default="scope">
                    <div class="occupancy-rate">
                      <el-progress 
                        :percentage="scope.row.occupancyRate" 
                        :color="scope.row.occupancyRate > 90 ? '#f56c6c' : scope.row.occupancyRate > 70 ? '#e6a23c' : '#67c23a'"
                        :stroke-width="18"
                        text-inside
                        style="width: 150px"
                      />
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center">
                  <template #default="scope">
                    <el-button 
                      size="small" 
                      type="primary" 
                      @click="viewBuildingDorms(scope.row)"
                    >
                      查看详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 添加楼栋对话框 -->
    <el-dialog v-model="addBuildingDialogVisible" title="添加楼栋" width="600px">
      <el-form :model="buildingForm" label-width="100px" :rules="buildingRules" ref="buildingFormRef">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="buildingForm.name" placeholder="请输入楼栋名称" />
        </el-form-item>
        <el-form-item label="楼栋类型" prop="gender">
          <el-select v-model="buildingForm.gender" placeholder="请选择楼栋类型" style="width: 100%">
            <el-option label="男生楼" value="男" />
            <el-option label="女生楼" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋描述">
          <el-input 
            v-model="buildingForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入楼栋描述（可选）" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addBuildingDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addBuilding">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑楼栋对话框 -->
    <el-dialog v-model="editBuildingDialogVisible" title="编辑楼栋" width="600px">
      <el-form :model="editBuildingForm" label-width="100px" :rules="buildingRules" ref="editBuildingFormRef">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="editBuildingForm.name" placeholder="请输入楼栋名称" />
        </el-form-item>
        <el-form-item label="楼栋类型" prop="gender">
          <el-select v-model="editBuildingForm.gender" placeholder="请选择楼栋类型" style="width: 100%">
            <el-option label="男生楼" value="男" />
            <el-option label="女生楼" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input 
            v-model="editBuildingForm.address" 
            placeholder="请输入楼栋详细地址，如：校园西区兰园路1号" 
          />
        </el-form-item>
        <el-form-item label="楼栋描述">
          <el-input 
            v-model="editBuildingForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入楼栋描述（可选）" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editBuildingDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateBuilding">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 快速分配对话框 -->
    <el-dialog v-model="quickAssignVisible" title="分配管理员" width="500px">
      <div v-if="currentQuickAssignBuilding" class="quick-assign-info">
        <div class="building-info">
          <el-icon class="building-icon"><OfficeBuilding /></el-icon>
          <div>
            <h3>{{ currentQuickAssignBuilding.name }}</h3>
            <el-tag :type="currentQuickAssignBuilding.gender === '男' ? 'primary' : 'danger'" size="small">
              {{ currentQuickAssignBuilding.gender }}生楼
            </el-tag>
          </div>
        </div>
        
        <div class="current-assignment">
          <span class="label">当前分配：</span>
          <template v-if="getBuildingAssignee(currentQuickAssignBuilding.id)">
            <el-tag type="success" size="small">
              {{ getBuildingAssignee(currentQuickAssignBuilding.id) }}
            </el-tag>
          </template>
          <template v-else>
            <el-tag type="info" size="small">未分配</el-tag>
          </template>
        </div>
      </div>

      <el-form :model="quickAssignForm" label-width="100px">
        <el-form-item label="选择管理员">
          <el-select 
            v-model="quickAssignForm.adminUsername" 
            placeholder="请选择管理员" 
            clearable
            style="width: 100%"
          >
            <el-option 
              v-for="admin in admins.filter(a => a.status === 'active')" 
              :key="admin.username"
              :label="admin.name" 
              :value="admin.username"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="quickAssignVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="saveQuickAssign"
            :loading="quickAssignSaving"
          >
            确定分配
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 专业分配对话框（全新UI） -->
    <el-dialog v-model="majorAssignmentDialogVisible" title="楼栋专业分配" width="1000px" top="5vh">
      <div v-if="currentAssignmentBuilding" class="major-assign-new">
        <!-- 楼栋信息卡 -->
        <div class="building-info-card">
          <div class="info-left">
            <el-icon class="title-icon"><OfficeBuilding /></el-icon>
            <div class="title-wrap">
              <h3>{{ currentAssignmentBuilding.buildingName }}</h3>
              <div class="meta-tags">
                <el-tag :type="currentAssignmentBuilding.gender === '男' ? 'primary' : 'danger'" size="small">{{ currentAssignmentBuilding.gender }}生楼</el-tag>
                <el-tag type="info" size="small">{{ currentAssignmentBuilding.bedsPerRoom }}人间</el-tag>
                <el-tag type="warning" size="small">总共{{ currentAssignmentBuilding.totalRooms }}间</el-tag>
                <el-tag 
                  :type="getCurrentBuildingRemainingRoomsType" 
                  size="small"
                  effect="dark"
                >
                  剩余{{ getCurrentBuildingRemainingRooms }}间
                </el-tag>
                <el-tag v-if="getBuildingMajorAssignment(currentAssignmentBuilding.buildingId)" type="success" size="small">已分配专业</el-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 当前分配状态显示 -->
        <div v-if="getBuildingMajorAssignment(currentAssignmentBuilding.buildingId)" class="current-assignment-info">
          <div class="section-head">
            <div class="head-left">
              <h4>当前专业分配</h4>
              <span class="sub">以下是该楼栋当前的专业分配情况</span>
            </div>
            <div class="head-actions">
              <el-button size="small" type="danger" @click="clearCurrentAssignment">清空当前分配</el-button>
            </div>
          </div>
          <div class="current-allocations-new">
            <div v-for="major in aggregatedCurrentAllocations" 
                 :key="major.majorName" 
                 class="major-allocation-card">
              <!-- 专业头部（始终显示） -->
              <div class="major-card-header" @click="toggleMajorExpand(major.majorName)">
                <div class="major-header-left">
                  <div class="expand-btn-wrapper">
                    <el-icon class="expand-btn" :class="{ 'is-expanded': major.isExpanded }">
                      <ArrowRight />
                    </el-icon>
                  </div>
                  <div class="major-title-section">
                    <h4 class="major-title">{{ major.majorName }}</h4>
                    <div class="major-meta">
                      <el-tag type="info" size="small" effect="plain">{{ major.classCount }}个班级</el-tag>
                    </div>
                  </div>
                </div>
                <div class="major-header-right">
                  <div class="stat-item">
                    <span class="stat-label">学生数</span>
                    <span class="stat-value">{{ major.totalStudents }}人</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">宿舍数</span>
                    <span class="stat-value">{{ major.totalRooms }}间</span>
                  </div>
                </div>
              </div>
              
              <!-- 展开的班级详情 -->
              <el-collapse-transition>
                <div v-if="major.isExpanded" class="major-card-body">
                  <div class="class-list">
                    <div v-for="(classItem, idx) in major.classes" 
                         :key="idx" 
                         class="class-item">
                      <div class="class-item-left">
                        <el-icon class="class-icon"><Document /></el-icon>
                        <span class="class-label">{{ classItem.className }}</span>
                        <span v-if="classItem.classSegment" class="segment-badge">{{ classItem.classSegment }}组</span>
                      </div>
                      <div class="class-item-right">
                        <el-tag size="small" type="success">{{ classItem.students }}人</el-tag>
                        <el-tag size="small" type="warning">{{ classItem.rooms }}间</el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </el-collapse-transition>
            </div>
          </div>
        </div>

        <!-- 分配模式选择 -->
        <div class="section">
          <div class="section-head">
            <div class="head-left">
              <h4>分配模式</h4>
              <span class="sub">选择分配策略，影响资源利用率和班级完整性</span>
            </div>
          </div>
          <el-radio-group v-model="allocationMode" class="mode-selector">
            <el-radio label="strict">
              <div class="mode-option">
                <div class="mode-title">完整分配</div>
                <div class="mode-desc">保持班级完整，不拆分班级，可能有空置房间</div>
              </div>
            </el-radio>
            <el-radio label="flexible">
              <div class="mode-option">
                <div class="mode-title">拆分分配</div>
                <div class="mode-desc">拆分大班级以填满楼栋</div>
              </div>
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 专业选择区 -->
        <div class="section">
          <div class="section-head">
            <div class="head-left">
              <h4>专业选择</h4>
              <span class="sub">仅选择要入住的专业，是否住得下由系统评估</span>
            </div>
            <div class="head-actions">
              <el-button size="small" type="primary" @click="onSmartMatch">专业分配</el-button>
              <el-button size="small" @click="onResetSelection">清空选择</el-button>
            </div>
          </div>

          <el-checkbox-group v-model="selectedMajorsForAssignment" class="major-list">
            <div 
              v-for="m in availableMajorsForAssignment"
              :key="m.id"
              class="major-card"
            >
              <div class="major-card-head">
                <el-checkbox :label="m.name">
                  <span class="major-name">{{ m.name }}</span>
                </el-checkbox>
              </div>
              <div class="major-card-body">
                <div class="stat"><span class="label">总人数：</span><span class="value">{{ m.totalStudents }}</span></div>
                <div class="stat"><span class="label">需宿舍：</span><span class="value">{{ m.roomsNeeded }} 间</span></div>
                <div class="stat"><span class="label">班级数：</span><span class="value">{{ m.classes ? m.classes.length : 1 }} 个</span></div>
              </div>
            </div>
          </el-checkbox-group>
        </div>

        <!-- 匹配结果区 -->
        <div class="section">
          <div class="section-head">
            <div class="head-left">
              <h4>专业分配结果</h4>
              <span class="sub" v-if="!smartAllocation">点击"专业分配"查看能否住得下，可手动选择</span>
            </div>
          </div>

          <div v-if="smartAllocation" class="result-wrap">
            <!-- 模式提示 -->
            <el-alert 
              v-if="smartAllocation.mode === 'flexible' && smartAllocation.results.some(r => r.details && r.details.some(d => d.isSplit))"
              type="warning" 
              :closable="false"
              style="margin-bottom: 16px;"
            >
              <template #title>
                <span style="font-weight: 600;">检测到班级拆分</span>
              </template>
              拆分分配模式下系统拆分了部分班级以填满楼栋。拆分的班级将按学号排序分组，学生可通过分组标识（A/B组）查询自己的楼栋归属。
            </el-alert>
            
            <div class="result-grid">
              <div class="result-card" v-for="r in filteredAllocationResults" :key="r.majorId">
                <div class="result-title">{{ r.name }}</div>
                <div class="result-stats">
                  <div class="rs"><span class="label">占用宿舍：</span><span class="value">{{ r.rooms }} 间</span></div>
                  <div class="rs"><span class="label">安置人数：</span><span class="value">{{ r.students }} 人</span></div>
                  <div class="rs"><span class="label">模式：</span><span class="value">{{ r.mode === 'full-major' ? '整专业' : r.mode === 'class-only' ? '整班组合' : '整班组合' }}</span></div>
                </div>
                <div v-if="r.details && r.details.length" class="result-details" style="margin-top:12px;">
                  <div v-for="(d, idx) in r.details" :key="idx" style="margin-bottom: 8px; padding: 8px; background: #f5f7fa; border-radius: 4px;">
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="color:#303133;font-weight:500;">{{ d.className || (d.classIndex + 1 + '班') }}</span>
                      <el-tag v-if="d.isSplit" type="danger" size="small" effect="dark">已拆分-{{ d.segment }}组</el-tag>
                      <el-tag v-else type="success" size="small">完整</el-tag>
                    </div>
                    <div style="color:#606266;font-size:12px;margin-top:4px;">
                      安置人数：{{ d.students }} 人，占用宿舍：{{ d.rooms }} 间
                      <span v-if="d.isSplit && d.segment" style="color:#f56c6c;">
                        （分片 {{ d.segment }}）
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="result-summary">
              <el-alert :closable="false" type="info"
                :title="`总占用宿舍：${smartAllocation.usedRooms} / ${currentAssignmentBuilding.totalRooms} | 利用率：${Math.round(smartAllocation.usedRooms / currentAssignmentBuilding.totalRooms * 100)}%`" />
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="majorAssignmentDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="saveMajorAssignment"
            :disabled="!smartAllocation"
            :loading="assignmentSaving"
          >
            {{ smartAllocation ? '保存分配方案' : '请先进行专业分配' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量添加宿舍对话框 -->
    <el-dialog v-model="batchAddDialogVisible" title="批量添加宿舍" width="600px">
      <el-form :model="batchAddForm" label-width="120px">
        <el-form-item label="选择楼栋" required>
          <el-select v-model="batchAddForm.buildingId" placeholder="选择楼栋" style="width: 100%">
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="`${building.name} (${building.gender}生楼)`" 
              :value="building.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="起始楼层" required>
          <el-input-number v-model="batchAddForm.startFloor" :min="1" :max="currentBuildingMaxFloors" />
        </el-form-item>
        <el-form-item label="结束楼层" required>
          <el-input-number v-model="batchAddForm.endFloor" :min="batchAddForm.startFloor" :max="currentBuildingMaxFloors" />
        </el-form-item>
        <el-form-item label="每层房间数" required>
          <el-input-number v-model="batchAddForm.roomsPerFloor" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="房间类型" required>
          <el-select v-model="batchAddForm.roomType" placeholder="选择房间类型" style="width: 100%">
            <el-option label="双人间" value="双人间" />
            <el-option label="四人间" value="四人间" />
            <el-option label="六人间" value="六人间" />
            <el-option label="八人间" value="八人间" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchAddDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveBatchAdd">批量添加</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 单个添加宿舍对话框 -->
    <el-dialog v-model="addDormDialogVisible" title="添加宿舍" width="600px">
      <el-form :model="addDormForm" label-width="120px">
        <el-form-item label="选择楼栋" required>
          <el-select v-model="addDormForm.buildingId" placeholder="选择楼栋" style="width: 100%">
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="`${building.name} (${building.gender}生楼)`" 
              :value="building.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号" required>
          <el-input v-model="addDormForm.room" placeholder="请输入房间号，如：101" />
        </el-form-item>
        <el-form-item label="房间类型" required>
          <el-select v-model="addDormForm.type" placeholder="选择房间类型" style="width: 100%">
            <el-option label="双人间" value="双人间" />
            <el-option label="四人间" value="四人间" />
            <el-option label="六人间" value="六人间" />
            <el-option label="八人间" value="八人间" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDormDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAddDorm">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 楼栋宿舍详情对话框 -->
    <el-dialog v-model="buildingDormsDialogVisible" :title="`${currentBuildingDorms?.buildingName} - 宿舍详情`" width="90%" top="5vh">
      <div v-if="currentBuildingDorms" class="building-dorms-content">
        <div class="dorms-header">
          <div class="building-summary">
            <el-statistic title="宿舍数量" :value="buildingDormsList.length" suffix="间" />
            <el-statistic title="床位总数" :value="currentBuildingDorms.totalBeds" suffix="床" />
            <el-statistic title="已入住" :value="currentBuildingDorms.occupiedBeds" suffix="床" />
            <el-statistic title="入住率" :value="currentBuildingDorms.occupancyRate" suffix="%" />
          </div>
          <div class="batch-operations-top" style="display:flex; gap:12px; align-items: center;">
            <el-button 
              :type="isAllSelected ? 'default' : 'primary'" 
              @click="toggleSelectAllDorms" 
              size="default"
            >
              {{ isAllSelected ? '取消全选' : '全选宿舍' }} ({{ buildingDormsList.length }})
            </el-button>
            <el-tooltip
              :disabled="!isBuildingAssignedMajor(currentBuildingDorms.buildingId)"
              content="该楼栋已分配专业，不能批量编辑宿舍"
              placement="top"
            >
              <span>
                <el-button 
                  type="warning" 
                  @click="showBatchEditDialog" 
                  :disabled="selectedDormIds.length === 0 || isBuildingAssignedMajor(currentBuildingDorms.buildingId)" 
                  size="default"
                >
                  批量编辑 ({{ selectedDormIds.length }})
                </el-button>
              </span>
            </el-tooltip>
            <div v-if="selectedDormIds.length > 0" class="selection-info">
              <el-text type="info" size="small">
                已选择 {{ selectedDormIds.length }} / {{ buildingDormsList.length }} 间宿舍
              </el-text>
            </div>
          </div>
        </div>
        
        <el-table 
          ref="dormsTable"
          :data="buildingDormsPagedList" 
          style="width: 100%" 
          :header-cell-style="{ background: '#f5f7fa' }"
          @selection-change="handleDormSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column prop="room" label="房间号" width="120" align="center" />
          <el-table-column prop="type" label="房间类型" width="120" align="center" />
          <el-table-column prop="totalBeds" label="床位数" width="100" align="center">
            <template #default="scope">
              <span>{{ scope.row.totalBeds }}床</span>
            </template>
          </el-table-column>
          <el-table-column prop="occupiedBeds" label="已入住" width="100" align="center">
            <template #default="scope">
              <span>{{ scope.row.occupiedBeds }}床</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120" align="center">
            <template #default="scope">
              <el-tag 
                :type="scope.row.status === 'full' ? 'danger' : 'success'"
                size="small"
              >
                {{ scope.row.status === 'full' ? '已满' : '可用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="居住学生" min-width="200" align="center">
            <template #default="scope">
              <div v-if="scope.row.students && scope.row.students.length > 0" class="residents-list">
                <el-tag 
                  v-for="student in scope.row.students" 
                  :key="student.studentId"
                  size="small"
                  class="resident-tag"
                  type="primary"
                >
                  {{ student.realName }}
                </el-tag>
              </div>
              <el-tag v-else type="success" size="small">暂无</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-tooltip
                :disabled="!isBuildingAssignedMajor(scope.row.buildingId)"
                content="该楼栋已分配专业，不能删除宿舍"
                placement="top"
              >
                <span>
                  <el-button 
                    size="small" 
                    type="danger" 
                    @click="deleteDorm(scope.row)"
                    :disabled="isBuildingAssignedMajor(scope.row.buildingId)"
                  >
                    删除
                  </el-button>
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="dorms-pagination" style="margin-top: 16px; display: flex; justify-content: flex-end;">
          <el-pagination
            layout="prev, pager, next, jumper"
            :page-size="dormsPageSize"
            :total="buildingDormsList.length"
            :current-page="dormsPage"
            @current-change="handleDormsPageChange"
          />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="buildingDormsDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量编辑对话框 -->
    <el-dialog v-model="batchEditDialogVisible" :title="`批量编辑宿舍 (${selectedDormIds.length} 间)`" width="600px">
      <div class="batch-edit-info" style="margin-bottom: 16px; padding: 12px; background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 6px;">
        <el-text type="info" size="small">
          <el-icon><InfoFilled /></el-icon>
          将对 <strong>{{ selectedDormIds.length }}</strong> 间宿舍执行批量操作，包括跨页选择的所有宿舍
        </el-text>
      </div>
      <el-form :model="batchEditForm" label-width="120px">
        <el-form-item label="操作类型">
          <el-radio-group v-model="batchEditForm.action">
            <el-radio label="edit">修改房间类型</el-radio>
            <el-radio label="delete">批量删除</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 修改房间类型 -->
        <el-form-item v-if="batchEditForm.action === 'edit'" label="新房间类型">
          <el-select v-model="batchEditForm.roomType" placeholder="选择房间类型" style="width: 100%">
            <el-option label="双人间" value="双人间" />
            <el-option label="四人间" value="四人间" />
            <el-option label="六人间" value="六人间" />
            <el-option label="八人间" value="八人间" />
          </el-select>
          <el-text size="small" type="info" style="margin-top: 8px; display: block;">
            同时会应用该房型的模板设施配置
          </el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchEditDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveBatchEdit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getAllBuildings as getAllBuildingsFromAPI,
  createBuilding as createBuildingAPI,
  updateBuilding as updateBuildingAPI,
  deleteBuilding as deleteBuildingAPI,
  assignBuildingManager
} from '@/api/building'
import { getAllAdmins } from '@/api/admin'
import { getRoomsByBuilding, createRoom, batchCreateRooms, batchUpdateRoomType, batchDeleteRooms } from '@/api/room.js'
import { 
  saveMajorAssignments as saveBuildingMajorAssignment, 
  getMajorAssignments as getBuildingMajorAssignments, 
  deleteMajorAssignment as deleteBuildingMajorAssignment,
  getRemainingStudentsByMajor
} from '@/api/majorAssignment.js'
import { getStudentList } from '@/api/student.js'
import { 
  OfficeBuilding, 
  Plus, 
  Search, 
  RefreshLeft, 
  Check, 
  Warning, 
  User, 
  Edit, 
  Delete, 
  School,
  Grid,
  InfoFilled,
  ArrowRight,
  Document
} from '@element-plus/icons-vue'

export default {
  name: 'BuildingManagement',
  inject: ['logOperation'],
  components: {
    OfficeBuilding,
    Plus,
    Search,
    RefreshLeft,
    Check,
    Warning,
    User,
    Edit,
    Delete,
    School,
    Grid,
    InfoFilled,
    ArrowRight,
    Document
  },
  data() {
    return {
      availableBuildings: [],
      admins: [],
      assignmentUpdateCounter: 0,
      buildingAssignmentListener: null, // 楼栋分配更新事件监听器
      
      // 搜索相关
      buildingSearchKeyword: '',
      buildingSearchType: '',
      buildingSearchStatus: '',
      
      // 对话框状态
      addBuildingDialogVisible: false,
      editBuildingDialogVisible: false,
      quickAssignVisible: false,
      majorAssignmentDialogVisible: false,
      
      // 表单数据
      buildingForm: {
        name: '',
        gender: ''
      },
      editBuildingForm: {
        id: null,
        name: '',
        gender: '',
        address: '',
        description: ''
      },
      quickAssignForm: {
        adminUsername: ''
      },
      
      // 当前操作的楼栋
      currentQuickAssignBuilding: null,
      currentAssignmentBuilding: null,
      currentEditBuilding: null,
      
      // 加载状态
      quickAssignSaving: false,
      assignmentSaving: false,
      
      // 专业分配相关
      selectedMajorsForAssignment: [],
      availableMajorsForAssignment: [],
      smartAllocation: null,
      allocationMode: 'strict', // 分配模式：strict-完整分配，flexible-拆分分配
      currentBuildingStudents: [], // 当前楼栋可分配的学生列表（用于计算分片）
      expandedMajors: {}, // 追踪哪些专业处于展开状态 { '专业名': true/false }
      
      // 标签页相关（从localStorage读取上次的标签，如果没有则默认为building-info）
      activeTab: localStorage.getItem('buildingManagement_activeTab') || 'building-info',
      
      // 宿舍管理相关
      batchAddDialogVisible: false,
      addDormDialogVisible: false,
      buildingDormsDialogVisible: false,
      batchEditDialogVisible: false,
      
      // 表单数据
      batchAddForm: {
        buildingId: '',
        startFloor: 1,
        endFloor: 4,
        roomsPerFloor: 10,
        roomType: '四人间'
      },
      addDormForm: {
        buildingId: '',
        room: '',
        type: '四人间'
      },
      batchEditForm: {
        roomType: '',
        action: 'edit'
      },
      
      // 宿舍数据
      currentBuildingDorms: null,
      buildingDormsList: [],
      buildingDormSummaryData: [],  // 各楼栋宿舍统计数据
      selectedDorms: [],
      // 跨页选择：存储已选择宿舍的 id
      selectedDormIds: [],
      // 分页
      dormsPage: 1,
      dormsPageSize: 10,
      
      // 专业分配数据（缓存）
      buildingMajorAssignmentsCache: {},
      
      // 表单验证规则
      buildingRules: {
        name: [
          { required: true, message: '请输入楼栋名称', trigger: 'blur' }
        ],
        gender: [
          { required: true, message: '请选择楼栋类型', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    // 🔥 新增：过滤掉0人的专业分配结果
    filteredAllocationResults() {
      if (!this.smartAllocation || !this.smartAllocation.results) return [];
      // 只返回安置人数大于0的专业
      return this.smartAllocation.results.filter(r => r.students > 0);
    },
    
    // 🔥 新增：当前楼栋剩余宿舍数量
    getCurrentBuildingRemainingRooms() {
      if (!this.currentAssignmentBuilding) return 0;
      const totalRooms = this.currentAssignmentBuilding.totalRooms || 0;
      const assignedRooms = this.getAssignedRoomsCount(this.currentAssignmentBuilding.buildingId);
      return totalRooms - assignedRooms;
    },
    
    // 🔥 新增：剩余宿舍标签颜色类型
    getCurrentBuildingRemainingRoomsType() {
      if (!this.currentAssignmentBuilding) return 'info';
      const totalRooms = this.currentAssignmentBuilding.totalRooms || 0;
      const remainingRooms = this.getCurrentBuildingRemainingRooms;
      const remainingPercentage = totalRooms > 0 ? (remainingRooms / totalRooms) * 100 : 0;
      
      if (remainingPercentage <= 0) {
        return 'danger';  // 没有剩余，红色
      } else if (remainingPercentage < 30) {
        return 'warning'; // 剩余少于30%，橙色
      } else {
        return 'success'; // 剩余充足，绿色
      }
    },
    
    // 🔥 新增：聚合当前分配数据（按专业分组）
    aggregatedCurrentAllocations() {
      const assignment = this.getBuildingMajorAssignment(this.currentAssignmentBuilding?.buildingId);
      if (!assignment || !assignment.allocations) return [];
      
      // 按专业分组
      const majorMap = {};
      assignment.allocations.forEach(allocation => {
        const majorName = allocation.majorName;
        
        if (!majorMap[majorName]) {
          majorMap[majorName] = {
            majorName: majorName,
            totalStudents: 0,
            totalRooms: 0,
            classCount: 0,
            classes: [],
            isExpanded: this.expandedMajors[majorName] || false
          };
        }
        
        majorMap[majorName].totalStudents += allocation.students || 0;
        majorMap[majorName].totalRooms += allocation.rooms || 0;
        majorMap[majorName].classCount += 1;
        majorMap[majorName].classes.push({
          className: allocation.className || '未分班',
          classSegment: allocation.classSegment,
          students: allocation.students,
          rooms: allocation.rooms
        });
      });
      
      return Object.values(majorMap);
    },
    
    // 判断是否全选状态
    isAllSelected() {
      return this.buildingDormsList.length > 0 && 
             this.selectedDormIds.length === this.buildingDormsList.length;
    },
    
    // 当前选中楼栋的最大楼层数
    currentBuildingMaxFloors() {
      // 统一限制为15层，无论是否选择楼栋
      return 15;
    },
    
    buildingDormsPagedList() {
      const start = (this.dormsPage - 1) * this.dormsPageSize;
      const end = start + this.dormsPageSize;
      return this.buildingDormsList.slice(start, end);
    },
    // 过滤后的楼栋列表
    filteredBuildings() {
      let filtered = [...this.availableBuildings];
      
      // 按关键词搜索
      if (this.buildingSearchKeyword) {
        const keyword = this.buildingSearchKeyword.toLowerCase();
        filtered = filtered.filter(building => {
          const nameMatch = building.name.toLowerCase().includes(keyword);
          const assigneeMatch = this.getBuildingAssignee(building.id) && 
            this.getBuildingAssignee(building.id).toLowerCase().includes(keyword);
          return nameMatch || assigneeMatch;
        });
      }
      
      // 按类型筛选
      if (this.buildingSearchType) {
        filtered = filtered.filter(building => building.gender === this.buildingSearchType);
      }
      
      // 按分配状态筛选
      if (this.buildingSearchStatus) {
        filtered = filtered.filter(building => {
          const isAssigned = !!this.getBuildingAssignee(building.id);
          if (this.buildingSearchStatus === 'assigned') {
            return isAssigned;
          } else if (this.buildingSearchStatus === 'unassigned') {
            return !isAssigned;
          }
          return true;
        });
      }
      
      return filtered;
    },
    
    // 专业分配对话框相关计算属性
    totalAssignedCapacity() {
      return this.availableMajorsForAssignment
        .filter(major => this.selectedMajorsForAssignment.includes(major.name))
        .reduce((total, major) => total + (major.assignedCapacity || 0), 0)
    },

    remainingCapacity() {
      if (!this.currentAssignmentBuilding) return 0
      return this.currentAssignmentBuilding.totalCapacity - this.totalAssignedCapacity
    },
    
    // 宿舍统计相关计算属性（从后端数据计算）
    totalDormsCount() {
      return this.buildingDormSummaryData.reduce((sum, item) => sum + item.totalRooms, 0);
    },
    // 总床位数  
    totalBedsCount() {
      return this.buildingDormSummaryData.reduce((sum, item) => sum + item.totalBeds, 0);
    },
    // 已入住床位数
    occupiedBedsCount() {
      return this.buildingDormSummaryData.reduce((sum, item) => sum + item.occupiedBeds, 0);
    },
    // 可用床位数
    availableBedsCount() {
      return this.totalBedsCount - this.occupiedBedsCount;
    }
  },
  async mounted() {
    await this.loadData();
    await this.loadAllBuildingMajorAssignments();
    
    // 监听楼栋分配更新事件
    this.buildingAssignmentListener = () => {
      console.log('检测到楼栋分配变更，刷新数据');
      this.loadBuildingAssignments();
    };
    window.addEventListener('buildingAssignmentUpdated', this.buildingAssignmentListener);
  },
  
  watch: {
    // 监听标签页切换，保存状态到localStorage
    activeTab(newTab) {
      localStorage.setItem('buildingManagement_activeTab', newTab);
    }
  },
  
  beforeUnmount() {
    // 清理监听器
    if (this.buildingAssignmentListener) {
      window.removeEventListener('buildingAssignmentUpdated', this.buildingAssignmentListener);
    }
  },
  methods: {
    // 加载数据
    async loadData() {
      await this.loadAvailableBuildings();
      await this.loadBuildingDormSummary();  // 加载宿舍统计
      this.loadAdmins();
      this.loadBuildingAssignments();
      this.loadBuildingMajorAssignments();
    },
    
    // 加载各楼栋宿舍统计数据
    async loadBuildingDormSummary() {
      try {
        const summary = [];
        
        // 为每个楼栋加载房间统计
        for (const building of this.availableBuildings) {
          const res = await getRoomsByBuilding(building.id);
          
          if (res.code === 200 && res.data) {
            const rooms = res.data;
            const totalRooms = rooms.length;
            const totalBeds = rooms.reduce((sum, room) => sum + (room.bedCount || 0), 0);
            const occupiedBeds = rooms.reduce((sum, room) => sum + (room.occupiedBeds || 0), 0);
            const occupancyRate = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0;
            
            // 统计房间类型（取最常见的房间类型）
            const roomTypeCount = {};
            rooms.forEach(room => {
              const type = room.roomType;
              if (type) {
                roomTypeCount[type] = (roomTypeCount[type] || 0) + 1;
              }
            });
            
            // 找出最常见的房间类型
            let mainRoomType = null;
            let maxCount = 0;
            for (const [type, count] of Object.entries(roomTypeCount)) {
              if (count > maxCount) {
                maxCount = count;
                mainRoomType = type;
              }
            }
            
            summary.push({
              buildingId: building.id,
              buildingName: building.buildingName || building.name,
              gender: building.buildingType || building.gender || '男', // buildingType已经是中文了
              totalRooms,
              totalBeds,
              occupiedBeds,
              occupancyRate,
              roomType: mainRoomType  // 添加主要房间类型
            });
          } else {
            // 如果获取失败，添加空数据
            summary.push({
              buildingId: building.id,
              buildingName: building.buildingName || building.name,
              gender: building.buildingType || building.gender || '男', // buildingType已经是中文了
              totalRooms: 0,
              totalBeds: 0,
              occupiedBeds: 0,
              occupancyRate: 0,
              roomType: null
            });
          }
        }
        
        this.buildingDormSummaryData = summary;
      } catch (error) {
        console.error('[loadBuildingDormSummary] 加载失败:', error);
        this.buildingDormSummaryData = [];
      }
    },
    
    async loadAvailableBuildings() {
      try {
        const response = await getAllBuildingsFromAPI();
        if (response.code === 200 && response.data) {
          // 将后端数据转换为前端格式
          let buildings = response.data.map(building => ({
            id: building.id,
            name: building.buildingName,
            code: building.buildingCode,
            gender: building.buildingType || '男', // buildingType已经是中文了
            buildingType: building.buildingType,
            floors: building.floorCount,
            totalRooms: building.totalRooms,
            address: building.address || '',
            managerName: building.managerName || '',
            managerPhone: building.managerPhone || '',
            description: building.description || '',
            status: building.status === 1 ? '启用' : '停用',
            allocatedRooms: building.allocatedRooms || 0,
            totalBeds: building.totalBeds || 0,
            occupiedBeds: building.occupiedBeds || 0,
            occupancyRate: building.occupancyRate || 0,
            createdAt: building.createdAt,
            updatedAt: building.updatedAt
          }));
          
          // 智能排序：按楼栋名称排序，确保同园区的楼栋排在一起
          // 如：兰园1号楼、兰园2号楼在一起；梅园1号楼、梅园2号楼在一起；A栋、B栋、C栋在一起
          buildings.sort((a, b) => {
            const nameA = a.name || '';
            const nameB = b.name || '';
            
            // 匹配中文园区名称（如"兰园"、"梅园"）+ 数字 + 号楼
            const chineseMatch = /^([\u4e00-\u9fa5]+园)(\d+)号?楼?$/;
            const matchA = nameA.match(chineseMatch);
            const matchB = nameB.match(chineseMatch);
            
            if (matchA && matchB) {
              // 都是"XX园N号楼"格式
              const gardenA = matchA[1];
              const gardenB = matchB[1];
              const numA = parseInt(matchA[2]);
              const numB = parseInt(matchB[2]);
              
              // 先按园区名排序
              if (gardenA !== gardenB) {
                return gardenA.localeCompare(gardenB, 'zh-CN');
              }
              // 园区相同，按数字排序
              return numA - numB;
            }
            
            // 匹配英文字母 + 数字的格式（如"A栋"、"A1栋"、"B栋"）
            const letterMatch = /^([A-Za-z]+)(\d*)(.*)$/;
            const letterMatchA = nameA.match(letterMatch);
            const letterMatchB = nameB.match(letterMatch);
            
            if (letterMatchA && letterMatchB && !matchA && !matchB) {
              const letterA = letterMatchA[1].toUpperCase();
              const letterB = letterMatchB[1].toUpperCase();
              const numberA = letterMatchA[2] ? parseInt(letterMatchA[2]) : 0;
              const numberB = letterMatchB[2] ? parseInt(letterMatchB[2]) : 0;
              
              // 先按字母排序
              if (letterA !== letterB) {
                return letterA.localeCompare(letterB);
              }
              // 字母相同，按数字排序
              return numberA - numberB;
            }
            
            // 如果都不匹配，按名称原始顺序排序（使用中文排序规则）
            return nameA.localeCompare(nameB, 'zh-CN');
          });
          
          this.availableBuildings = buildings;
        }
      } catch (error) {
        console.error('加载楼栋数据失败:', error);
        this.$message.error('加载楼栋数据失败，请刷新重试');
        this.availableBuildings = [];
      }
    },
    
    async loadAdmins() {
      try {
        const response = await getAllAdmins();
        if (response.code === 200 && response.data) {
          // 转换后端数据格式以适配前端
          this.admins = response.data.map(admin => ({
            id: admin.id,
            username: admin.username,
            realName: admin.realName,
            name: admin.realName, // 兼容旧代码
            email: admin.email,
            phone: admin.phone || '',
            userType: admin.userType,
            status: admin.statusText || (admin.status === 1 ? 'active' : 'inactive'),
            avatar: admin.avatar,
            createdAt: admin.createdAt
          }));
        } else {
          this.$message.warning('加载管理员列表失败');
          this.admins = [];
        }
      } catch (error) {
        console.error('加载管理员列表失败:', error);
        this.$message.error('加载管理员列表失败');
        this.admins = [];
      }
    },
    
    loadBuildingAssignments() {
      // 触发重新渲染分配状态（数据直接从building表的manager_name读取）
      this.assignmentUpdateCounter++;
    },

    // 获取楼栋专业分配状态（同步方法，从缓存读取）
    getBuildingMajorAssignment(buildingId) {
      const assignment = this.buildingMajorAssignmentsCache[buildingId];
      // 返回有效的分配数据，null 也视为无分配
      return assignment && assignment.allocations && assignment.allocations.length > 0 ? assignment : null;
    },

    // 从API加载楼栋专业分配（异步方法，更新缓存）
    async loadBuildingMajorAssignment(buildingId) {
      try {
        const res = await getBuildingMajorAssignments(buildingId);
        if (res.data && res.data.length > 0) {
          // 转换API返回的数据格式为前端所需格式
          const allocations = res.data.map(item => ({
            majorId: item.id,
            majorName: item.majorName,
            className: item.className || '', // 🔥 添加班级名称
            classSegment: item.classSegment || null, // 🔥 添加班级分片
            students: item.allocatedStudents,
            rooms: item.allocatedRooms,
            mode: item.assignmentMode
          }));
          // 更新缓存（Vue 3 直接赋值即可，无需 $set）
          this.buildingMajorAssignmentsCache[buildingId] = {
            buildingId: buildingId,
            allocations: allocations
          };
        } else {
          // 如果没有数据，清除缓存（使用赋值为 null 而不是 delete，确保 Vue 3 响应式更新）
          this.buildingMajorAssignmentsCache[buildingId] = null;
        }
      } catch (error) {
        console.error('查询专业分配失败:', error);
      }
    },

    // 加载所有楼栋的专业分配
    async loadAllBuildingMajorAssignments() {
      for (const building of this.availableBuildings) {
        await this.loadBuildingMajorAssignment(building.id);
      }
    },

    // 格式化修改时间
    formatUpdateTime(updateTime) {
      if (!updateTime) return '未知';
      try {
        const date = new Date(updateTime);
        return date.toLocaleDateString('zh-CN');
      } catch (error) {
        return '未知';
      }
    },
     
    // 获取楼栋分配者姓名
    getBuildingAssignee(buildingId) {
      // 从building表的managerName字段读取（存储的是username）
      const building = this.availableBuildings.find(b => b.id === buildingId);
      const username = building?.managerName;
      
      if (!username) return null;
      
      // 根据username查找管理员，返回真实姓名用于显示
      const admin = this.admins.find(a => a.username === username);
      return admin ? (admin.realName || admin.name || username) : username;
    },
    
    // 获取已分配管理员的楼栋数量
    getAssignedBuildingsCount() {
      // 统计有 managerName 的楼栋数量
      return this.availableBuildings.filter(building => 
        building.managerName && building.managerName.trim() !== ''
      ).length;
    },
    
    // 搜索相关方法
    handleBuildingSearch() {
      // 搜索逻辑在计算属性中处理
    },
    
    clearBuildingSearch() {
      this.buildingSearchKeyword = '';
    },
    
    clearAllBuildingSearch() {
      this.buildingSearchKeyword = '';
      this.buildingSearchType = '';
      this.buildingSearchStatus = '';
    },
    
    // 添加楼栋
    showAddBuildingDialog() {
      this.buildingForm = {
        name: '',
        gender: '',
        description: ''
      };
      this.addBuildingDialogVisible = true;
    },
    
    // 智能生成楼栋编码
    generateBuildingCode(name) {
      if (!name) return '';
      
      // 匹配中文园区格式：兰园1号楼 -> LY1, 梅园2号楼 -> MY2
      const gardenMatch = name.match(/^([\u4e00-\u9fa5]+园)(\d+)号?楼?$/);
      if (gardenMatch) {
        const garden = gardenMatch[1]; // "兰园"
        const num = gardenMatch[2]; // "1"
        // 取园区名每个字的首字母拼音（简化：取前两个字符）+ 数字
        const gardenCode = garden.substring(0, garden.length - 1); // 去掉"园"字
        return `${gardenCode}${num}`;
      }
      
      // 匹配纯英文+数字格式：A栋 -> A, B1栋 -> B1, C2号楼 -> C2
      const letterMatch = name.match(/^([A-Za-z]+\d*).*$/);
      if (letterMatch) {
        return letterMatch[1].toUpperCase();
      }
      
      // 匹配纯数字：1 -> BUILD1, 2 -> BUILD2（避免单字符冲突）
      if (/^\d+$/.test(name)) {
        return `BUILD${name}`;
      }
      
      // 其他情况：使用完整名称作为编码（限制长度）
      return name.substring(0, 20).toUpperCase();
    },
    
    async addBuilding() {
      this.$refs.buildingFormRef.validate(async (valid) => {
        if (valid) {
          try {
            // 构造后端需要的数据格式
            const buildingData = {
              buildingName: this.buildingForm.name,
              buildingCode: this.generateBuildingCode(this.buildingForm.name),
              buildingType: this.buildingForm.gender, // 直接使用中文：男/女
              floorCount: 6, // 默认值
              totalRooms: 100, // 默认值
              description: this.buildingForm.description,
              status: 1 // 启用状态
            };
            
            const response = await createBuildingAPI(buildingData);
            if (response.code === 200) {
              this.$message.success('楼栋添加成功');
              this.addBuildingDialogVisible = false;
              // 重新加载楼栋列表
              await this.loadAvailableBuildings();
              this.logOperation(`添加楼栋：${this.buildingForm.name}`, '楼栋管理');
            } else {
              // 显示后端返回的具体错误信息
              this.$message.error(response.message || '添加失败');
            }
          } catch (error) {
            console.error('添加楼栋异常:', error);
            // 优先使用 error.message（request.js 已将后端错误信息放入其中）
            if (error.message) {
              this.$message.error(error.message);
            } else if (error.response && error.response.data && error.response.data.message) {
              this.$message.error(error.response.data.message);
            } else {
              // 网络错误或其他异常
              this.$message.error('网络错误，请检查连接后重试');
            }
          }
        }
      });
    },
    
    // 编辑楼栋
    editBuilding(building) {
      this.currentEditBuilding = building;
      this.editBuildingForm = {
        id: building.id,
        name: building.name,
        gender: building.gender,
        address: building.address || '',
        description: building.description || ''
      };
      this.editBuildingDialogVisible = true;
    },
    
    async updateBuilding() {
      this.$refs.editBuildingFormRef.validate(async (valid) => {
        if (valid) {
          try {
            // 构造后端需要的数据格式
            const buildingData = {
              id: this.editBuildingForm.id,
              buildingName: this.editBuildingForm.name,
              buildingCode: this.generateBuildingCode(this.editBuildingForm.name),
              buildingType: this.editBuildingForm.gender, // 直接使用中文：男/女
              floorCount: this.currentEditBuilding.floors || 6,
              totalRooms: this.currentEditBuilding.totalRooms || 100,
              address: this.editBuildingForm.address,
              managerName: this.currentEditBuilding.managerName,
              managerPhone: this.currentEditBuilding.managerPhone,
              description: this.editBuildingForm.description,
              status: this.currentEditBuilding.status === '启用' ? 1 : 0
            };
            
            const response = await updateBuildingAPI(buildingData);
            if (response.code === 200) {
              this.$message.success('楼栋信息更新成功');
              this.editBuildingDialogVisible = false;
              // 重新加载楼栋列表
              await this.loadAvailableBuildings();
              this.logOperation(`编辑楼栋：${this.editBuildingForm.name}`, '楼栋管理');
            } else {
              // 显示后端返回的具体错误信息
              this.$message.error(response.message || '更新失败');
            }
          } catch (error) {
            console.error('更新楼栋异常:', error);
            // 优先使用 error.message（request.js 已将后端错误信息放入其中）
            if (error.message) {
              this.$message.error(error.message);
            } else if (error.response && error.response.data && error.response.data.message) {
              this.$message.error(error.response.data.message);
            } else {
              // 网络错误或其他异常
              this.$message.error('网络错误，请检查连接后重试');
            }
          }
        }
      });
    },
    
    // 删除楼栋
    deleteBuilding(building) {
      this.$confirm(`确定要删除楼栋"${building.name}"吗？删除后将无法恢复。`, '确认删除', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await deleteBuildingAPI(building.id);
          if (response.code === 200) {
            this.$message.success(`楼栋 ${building.name} 删除成功`);
            // 重新加载楼栋列表
            await this.loadAvailableBuildings();
            
            // 后端通过外键约束CASCADE自动清理楼栋分配数据
            this.loadBuildingAssignments();
            this.logOperation(`删除楼栋：${building.name}`, '楼栋管理');
          } else {
            this.$message.error(response.message || '删除失败');
          }
        } catch (error) {
          console.error('删除楼栋失败:', error);
          this.$message.error('删除楼栋失败，请重试');
        }
      }).catch(() => {
        // 用户取消删除
      });
    },
    
    // 快速分配管理员
    quickAssignBuilding(building) {
      this.currentQuickAssignBuilding = building;
      
      // 获取当前分配的管理员
      const currentAssignee = this.getBuildingAssigneeUsername(building.id);
      this.quickAssignForm.adminUsername = currentAssignee || '';
      
      this.quickAssignVisible = true;
    },
    
    getBuildingAssigneeUsername(buildingId) {
      // 从building表的managerName反查username
      const building = this.availableBuildings.find(b => b.id === buildingId);
      if (!building || !building.managerName) {
        return null;
      }
      
      // 通过managerName查找对应的管理员username
      const admin = this.admins.find(a => 
        (a.realName === building.managerName || a.name === building.managerName) && 
        a.status === 'active'
      );
      
      return admin ? admin.username : null;
    },
    
    async saveQuickAssign() {
      if (!this.currentQuickAssignBuilding) return;
      
      this.quickAssignSaving = true;
      
      try {
        const buildingId = this.currentQuickAssignBuilding.id;
        const newAdminUsername = this.quickAssignForm.adminUsername;
        
        // 获取选中的管理员信息
        let managerName = null;  // 存储username到数据库
        let managerPhone = null;
        let displayName = null;  // 用于显示的真实姓名
        
        if (newAdminUsername) {
          const selectedAdmin = this.admins.find(a => a.username === newAdminUsername);
          if (selectedAdmin) {
            // 🔥 修复：存储username到数据库，而不是真实姓名
            managerName = selectedAdmin.username;
            managerPhone = selectedAdmin.phone || '';
            displayName = selectedAdmin.realName || selectedAdmin.name || selectedAdmin.username;
          }
        }
        
        // 调用新的简化API：直接更新building表的manager_name（存储username）
        const response = await assignBuildingManager(
          buildingId, 
          managerName || '', 
          managerPhone || ''
        );
        
        if (response.code === 200) {
          // 重新加载楼栋列表和管理员列表以更新显示
          await this.loadAvailableBuildings();
          await this.loadAdmins();  // 🔥 修复：刷新管理员列表，更新管理楼栋信息
          
          // 广播楼栋分配更新事件，通知其他页面刷新
          window.dispatchEvent(new CustomEvent('buildingAssignmentUpdated', { 
            detail: { 
              buildingId: buildingId,
              managerName: managerName,
              action: managerName ? 'assign' : 'remove'
            } 
          }));
          
          // 关闭对话框
          this.quickAssignVisible = false;
          
          // 使用真实姓名显示提示信息
          const actionText = displayName ? 
            `已将 ${this.currentQuickAssignBuilding.name} 分配给 ${displayName}` :
            `已取消 ${this.currentQuickAssignBuilding.name} 的管理员分配`;
          
          this.$message.success(actionText);
          this.logOperation(actionText, '楼栋管理');
        } else {
          this.$message.error(response.message || '分配失败');
        }
        
      } catch (error) {
        console.error('分配操作失败:', error);
        this.$message.error(error.message || '分配失败，请重试');
      } finally {
        this.quickAssignSaving = false;
      }
    },
    
    // 获取楼栋房间数量（用于判断是否有宿舍）
    getBuildingRoomCount(buildingId) {
      // 从 buildingDormSummaryData 读取房间数量
      if (!this.buildingDormSummaryData || this.buildingDormSummaryData.length === 0) {
        return 0;
      }
      
      const buildingSummary = this.buildingDormSummaryData.find(b => 
        b.buildingId == buildingId
      );
      
      return buildingSummary ? buildingSummary.totalRooms : 0;
    },
    
    // 判断楼栋是否已有宿舍（用于禁用专业分配入口）
    hasDorms(buildingId) {
      const roomCount = this.getBuildingRoomCount(buildingId);
      return roomCount > 0;
    },

    // 专业分配入口统一点击处理：有宿舍→进入分配；无宿舍→引导添加
    onMajorAssignClick(building) {
      if (!this.hasDorms(building.id)) {
        this.$confirm(
          '该楼栋尚未添加宿舍，需先添加宿舍后才能进行“专业分配”。',
          '请先添加宿舍',
          {
            confirmButtonText: '添加宿舍',
            cancelButtonText: '批量添加',
            distinguishCancelAndClose: true,
            type: 'warning'
          }
        ).then(() => {
          // 单个添加宿舍：预选楼栋
          this.addDormForm = { buildingId: building.id, room: '', type: '四人间' }
          this.addDormDialogVisible = true
        }).catch((action) => {
          if (action === 'cancel') {
            // 批量添加宿舍：预选楼栋
            this.batchAddForm = {
              buildingId: building.id,
              startFloor: 1,
              endFloor: 6,
              roomsPerFloor: 10,
              roomType: '四人间'
            }
            this.batchAddDialogVisible = true
          }
          // 关闭/ESC 不做处理
        })
        return
      }
      // 已有宿舍，正常进入
      this.showMajorAssignment(building)
    },

    // 🔥 新增：切换专业展开/收起状态
    toggleMajorExpand(majorName) {
      // Vue 3 响应式更新
      this.expandedMajors[majorName] = !this.expandedMajors[majorName];
      this.$forceUpdate(); // 强制更新视图
    },
    
    // 清空当前专业分配
    async clearCurrentAssignment() {
      try {
        await this.$confirm('确定要清空当前的专业分配吗？此操作不可撤销。', '确认清空', {
          confirmButtonText: '确定清空',
          cancelButtonText: '取消',
          type: 'warning'
        });
        
        try {
          const buildingId = this.currentAssignmentBuilding.buildingId;
          const buildingName = this.currentAssignmentBuilding.buildingName;
          
          // 调用后端API，保存空数组来清空分配
          await saveBuildingMajorAssignment(buildingId, []);
          
          // 立即清空缓存（确保响应式更新）
          this.buildingMajorAssignmentsCache[buildingId] = null;
          
          // 重置选择状态（清空后不自动全选）
          this.selectedMajorsForAssignment = [];
          this.smartAllocation = null;
          
          // 强制刷新界面
          this.$forceUpdate();
          
          this.$message.success('专业分配已清空');
          this.logOperation(`清空楼栋专业分配：${buildingName}`, '楼栋管理');
          
          // 等待 500ms 让数据库事务完全提交，再重新查询
          await new Promise(resolve => setTimeout(resolve, 500));
          
          // 重新加载该楼栋的专业分配数据（从后端确认）
          await this.loadBuildingMajorAssignment(buildingId);
          
          // 不关闭对话框，直接刷新可用专业列表
          await this.refreshAvailableMajors(buildingId);
        } catch (error) {
          console.error('清空专业分配失败:', error);
          this.$message.error('清空失败，请重试');
        }
      } catch {
        // 用户取消操作
      }
    },
    
    // 刷新可用专业列表（清空分配后调用）
    async refreshAvailableMajors(buildingId) {
      const building = this.availableBuildings.find(b => b.id === buildingId);
      if (!building) {
        console.error('未找到楼栋信息');
        return;
      }
      
      // 重新获取学生数据
      const genderForAPI = building.gender;
      let students = [];
      
      try {
        const response = await getStudentList({
          currentPage: 1,
          pageSize: 10000,
          gender: genderForAPI,
          registrationStatus: ''
        });
        
        if (response.code === 200 && response.data && response.data.students) {
          students = response.data.students;
          this.currentBuildingStudents = students;
        }
      } catch (error) {
        console.error('获取学生数据失败:', error);
        return;
      }
      
      // 重新计算专业统计
      const majorStats = {};
      students.forEach(student => {
        const majorName = student.major;
        const className = student.className || student.class;
        
        if (!majorStats[majorName]) {
          majorStats[majorName] = {
            name: majorName,
            students: [],
            classes: {}
          };
        }
        
        majorStats[majorName].students.push(student);
        
        if (className) {
          if (!majorStats[majorName].classes[className]) {
            majorStats[majorName].classes[className] = 0;
          }
          majorStats[majorName].classes[className]++;
        }
      });
      
      // 检查其他楼栋的已分配专业（排除当前楼栋）
      const allocatedClassSegments = new Set();
      
      Object.keys(this.buildingMajorAssignmentsCache).forEach(bid => {
        // 跳过当前楼栋（因为已经清空了）
        if (bid == buildingId) return;
        
        const assignment = this.buildingMajorAssignmentsCache[bid];
        if (assignment && assignment.allocations) {
          assignment.allocations.forEach(allocation => {
            const majorName = allocation.majorName;
            const className = allocation.className;
            const classSegment = allocation.classSegment;
            
            if (className) {
              const key = classSegment 
                ? `${majorName}-${className}-${classSegment}` 
                : `${majorName}-${className}`;
              allocatedClassSegments.add(key);
            } else {
              allocatedClassSegments.add(`${majorName}-*`);
            }
          });
        }
      });
      
      // 过滤已分配的学生
      Object.keys(majorStats).forEach(majorName => {
        const major = majorStats[majorName];
        
        if (allocatedClassSegments.has(`${majorName}-*`)) {
          delete majorStats[majorName];
          return;
        }
        
        const remainingStudents = major.students.filter(student => {
          const className = student.className || student.class;
          const classSegment = student.classSegment || null;
          
          if (!className) return true;
          
          const classKey = `${majorName}-${className}`;
          if (allocatedClassSegments.has(classKey)) {
            return false;
          }
          
          if (classSegment) {
            const segmentKey = `${majorName}-${className}-${classSegment}`;
            return !allocatedClassSegments.has(segmentKey);
          }
          
          return true;
        });
        
        major.students = remainingStudents;
        
        major.classes = {};
        remainingStudents.forEach(student => {
          const className = student.className || student.class;
          if (className) {
            if (!major.classes[className]) {
              major.classes[className] = 0;
            }
            major.classes[className]++;
          }
        });
        
        if (remainingStudents.length === 0) {
          delete majorStats[majorName];
        }
      });
      
      // 转换为前端格式
      const bedsPerRoom = this.currentAssignmentBuilding.bedsPerRoom;
      const realMajors = Object.values(majorStats).map(major => {
        const classNames = Object.keys(major.classes);
        const classSizes = classNames.length > 0 
          ? Object.values(major.classes)
          : [major.students.length];
        
        return {
          id: major.name.toLowerCase().replace(/\s+/g, ''),
          name: major.name,
          classNames: classNames.length > 0 ? classNames : ['未分班'],
          classSizes: classSizes
        };
      });
      
      this.availableMajorsForAssignment = realMajors
        .map(m => {
          const classes = m.classSizes.map((size, idx) => {
            const roomsNeeded = Math.ceil(size / bedsPerRoom);
            const fullRooms = Math.floor(size / bedsPerRoom);
            const remainder = size % bedsPerRoom;
            const bundles = [];
            for (let i = 0; i < fullRooms; i += 1) bundles.push({ size: bedsPerRoom, type: 'full' });
            if (remainder > 0) bundles.push({ size: remainder, type: 'partial' });
            return { classIndex: idx, className: m.classNames[idx], size, roomsNeeded, fullRooms, remainder, bundles };
          });
          const totalStudents = m.classSizes.reduce((a, b) => a + b, 0);
          const roomsNeeded = classes.reduce((s, c) => s + c.roomsNeeded, 0);
          return { id: m.id, name: m.name, totalStudents, roomsNeeded, classes };
        })
        .filter(m => m.totalStudents > 0);
      
      // 清空选择状态
      this.selectedMajorsForAssignment = [];
      this.smartAllocation = null;
      
      console.log('✅ 刷新可用专业列表完成，共', this.availableMajorsForAssignment.length, '个专业');
    },

    // 检查楼栋是否已分配专业
    isBuildingAssignedMajor(buildingId) {
      const assignment = this.getBuildingMajorAssignment(buildingId);
      return assignment && assignment.allocations && assignment.allocations.length > 0;
    },

    // 专业分配（新版：构建班级与宿舍包）
    async showMajorAssignment(building) {
      // 从后端数据检查楼栋是否有宿舍
      const roomCount = this.getBuildingRoomCount(building.id);
      
      if (roomCount === 0) {
        this.$message.warning('该楼栋尚未添加宿舍，请先在"宿舍管理"中添加后再进行专业分配');
        return;
      }
      
      // 从后端统计数据获取总房间数
      const buildingSummary = this.buildingDormSummaryData.find(b => b.buildingId == building.id);
      const totalRooms = buildingSummary ? buildingSummary.totalRooms : (building.totalRooms || 0);
      const totalBeds = buildingSummary ? buildingSummary.totalBeds : 0;
      
      // 计算平均床位数（作为当前房型）
      let bedsPerRoom = building.bedsPerRoom || 4;
      if (totalRooms > 0 && totalBeds > 0) {
        bedsPerRoom = Math.round(totalBeds / totalRooms);
      }
      
      // 将床位数转换为中文房间类型
      const roomType = bedsPerRoom === 2 ? '2人间' : 
                       bedsPerRoom === 4 ? '4人间' : 
                       bedsPerRoom === 6 ? '6人间' : 
                       bedsPerRoom === 8 ? '8人间' : '4人间';
      
      this.currentAssignmentBuilding = {
        buildingId: building.id,
        buildingName: building.name,
        gender: building.gender,
        bedsPerRoom,
        roomType,  // 新增：中文房间类型
        totalRooms,
        totalCapacity: bedsPerRoom * totalRooms
      };

      // 🔥 修复：从后端API获取学生数据（必须按性别过滤）
      let students = [];
      try {
        // 直接使用中文性别值
        const genderForAPI = building.gender; // 直接使用"男"或"女"
        
        if (!genderForAPI || (genderForAPI !== '男' && genderForAPI !== '女')) {
          this.$message.error('楼栋性别信息不完整，无法进行专业分配');
          return;
        }
        
        console.log(`🏢 楼栋 ${building.name} (${building.gender}生楼) - 查询 ${genderForAPI} 学生`);
        
        const response = await getStudentList({
          currentPage: 1,
          pageSize: 10000, // 获取所有学生
          gender: genderForAPI, // 直接使用中文性别值
          registrationStatus: '' // 不过滤注册状态，查询所有学生
        });
        
        if (response.code === 200 && response.data && response.data.students) {
          students = response.data.students;
          // 保存学生数据供后续分片计算使用
          this.currentBuildingStudents = students;
          
          console.log(`✅ 成功获取 ${students.length} 名${building.gender}生数据`);
          
          if (students.length === 0) {
            this.$message.warning(`未找到符合条件的${building.gender}生，无法进行专业分配`);
            return;
          }
        } else {
          this.$message.warning('获取学生数据失败，无法进行专业分配');
          return;
        }
      } catch (error) {
        console.error('获取学生数据失败:', error);
        this.$message.error('获取学生数据失败');
        return;
      }
      
      // 按专业分组统计学生和班级
      const majorStats = {}
      students.forEach(student => {
        const majorName = student.major
        const className = student.className || student.class
        
        if (!majorStats[majorName]) {
          majorStats[majorName] = {
            name: majorName,
            students: [],
            classes: {}
          }
        }
        
        majorStats[majorName].students.push(student)
        
        if (className) {
          if (!majorStats[majorName].classes[className]) {
            majorStats[majorName].classes[className] = 0
          }
          majorStats[majorName].classes[className]++
        }
      })
      
      // 🔥 修复：检查所有楼栋的专业分配，排除已分配的班级/分片
      const allocatedClassSegments = new Set(); // 格式："{专业名}-{班级名}-{分片}" 或 "{专业名}-{班级名}"
      
      Object.keys(this.buildingMajorAssignmentsCache).forEach(buildingId => {
        const assignment = this.buildingMajorAssignmentsCache[buildingId];
        if (assignment && assignment.allocations) {
          // 🔥 关键修复：编辑当前楼栋时，跳过当前楼栋的已分配记录
          // 这样才能重新编辑当前楼栋的专业分配
          if (String(buildingId) === String(building.buildingId)) {
            console.log(`⏭️ 跳过当前楼栋 ${buildingId} 的已分配记录，允许重新编辑`);
            return; // 跳过当前楼栋
          }
          
          assignment.allocations.forEach(allocation => {
            const majorName = allocation.majorName;
            const className = allocation.className;
            const classSegment = allocation.classSegment;
            
            if (className) {
              // 构建唯一标识
              const key = classSegment 
                ? `${majorName}-${className}-${classSegment}` 
                : `${majorName}-${className}`;
              allocatedClassSegments.add(key);
            } else {
              // 如果没有班级名称，标记整个专业
              allocatedClassSegments.add(`${majorName}-*`);
            }
          });
        }
      });
      
      console.log(`🚫 排除已分配的班级/分片：`, Array.from(allocatedClassSegments));
      
      // 从统计中移除已分配的班级和学生
      Object.keys(majorStats).forEach(majorName => {
        const major = majorStats[majorName];
        
        // 检查整个专业是否已被分配
        if (allocatedClassSegments.has(`${majorName}-*`)) {
          delete majorStats[majorName];
          return;
        }
        
        // 过滤掉已分配的学生（基于班级和分片）
        const remainingStudents = major.students.filter(student => {
          const className = student.className || student.class;
          const classSegment = student.classSegment || null;
          
          if (!className) return true; // 没有班级信息的学生保留
          
          // 检查完整班级是否已分配
          const classKey = `${majorName}-${className}`;
          if (allocatedClassSegments.has(classKey)) {
            return false; // 整个班级已分配，排除
          }
          
          // 检查特定分片是否已分配
          if (classSegment) {
            const segmentKey = `${majorName}-${className}-${classSegment}`;
            return !allocatedClassSegments.has(segmentKey);
          }
          
          return true;
        });
        
        major.students = remainingStudents;
        
        // 重新计算班级人数
        major.classes = {};
        remainingStudents.forEach(student => {
          const className = student.className || student.class;
          if (className) {
            if (!major.classes[className]) {
              major.classes[className] = 0;
            }
            major.classes[className]++;
          }
        });
        
        // 如果该专业已全部分配完，删除该专业（不再出现在可选列表）
        if (remainingStudents.length === 0) {
          delete majorStats[majorName];
        }
      });
      
      // 转换为适合分配的格式
      const realMajors = Object.values(majorStats).map((major, index) => {
        const classNames = Object.keys(major.classes)
        const classSizes = classNames.length > 0 
          ? Object.values(major.classes)
          : [major.students.length] // 如果没有班级信息，整个专业作为一个班级
        
        return {
          id: major.name.toLowerCase().replace(/\s+/g, ''),
          name: major.name,
          classNames: classNames.length > 0 ? classNames : ['未分班'], // 🔥 保留实际的班级名称
          classSizes: classSizes
          }
        })
      
      this.availableMajorsForAssignment = realMajors
        .map(m => {
          const classes = m.classSizes.map((size, idx) => {
            const roomsNeeded = Math.ceil(size / bedsPerRoom);
            const fullRooms = Math.floor(size / bedsPerRoom);
            const remainder = size % bedsPerRoom;
            const bundles = [];
            for (let i = 0; i < fullRooms; i += 1) bundles.push({ size: bedsPerRoom, type: 'full' });
            if (remainder > 0) bundles.push({ size: remainder, type: 'partial' });
            return { classIndex: idx, className: m.classNames[idx], size, roomsNeeded, fullRooms, remainder, bundles }; // 🔥 使用实际的className
          });
          const totalStudents = m.classSizes.reduce((a, b) => a + b, 0);
          const roomsNeeded = classes.reduce((s, c) => s + c.roomsNeeded, 0);
          return { id: m.id, name: m.name, totalStudents, roomsNeeded, classes };
        })
        .filter(m => m.totalStudents > 0); // 🔥 关键修复：过滤无学生的专业

      this.selectedMajorsForAssignment = this.availableMajorsForAssignment.map(m => m.name);
      this.smartAllocation = null;
      this.majorAssignmentDialogVisible = true;
    },
    
    async saveMajorAssignment() {
      if (!this.smartAllocation) {
        this.$message.warning('请先进行专业分配');
        return;
      }

      // 🔥 关键修复：过滤掉0人的专业（自动移除，不报错）
      const result = (this.smartAllocation.results || []).filter(r => r.students > 0);
      
      // 验证：检查是否有有效的分配结果
      if (result.length === 0) {
        this.$message.error('没有可保存的分配结果，请重新进行专业分配');
        return;
      }

      // 验证：检查总分配学生数
      const totalStudents = result.reduce((s, r) => s + (r.students || 0), 0);
      if (totalStudents <= 0) {
        this.$message.error('总分配学生数为0，不能保存空的分配方案');
        return;
      }

      // 验证：检查是否占用了房间
      const totalUsedRooms = this.smartAllocation.usedRooms || 0;
      if (totalUsedRooms <= 0) {
        this.$message.error('没有占用任何房间，不能保存空的分配方案');
        return;
      }

      this.assignmentSaving = true;

      try {
        const payload = {
          buildingId: this.currentAssignmentBuilding.buildingId,
          buildingName: this.currentAssignmentBuilding.buildingName,
          gender: this.currentAssignmentBuilding.gender,
          bedsPerRoom: this.currentAssignmentBuilding.bedsPerRoom,
          totalRooms: this.currentAssignmentBuilding.totalRooms,
          allocations: result.map(r => ({
            majorId: r.majorId,
            majorName: r.name,
            mode: r.mode,
            rooms: r.rooms,
            students: r.students,
            details: r.details
          })),
          summary: {
            usedRooms: totalUsedRooms,
            totalRooms: this.currentAssignmentBuilding.totalRooms,
            totalStudents: totalStudents
          },
          createdAt: new Date().toISOString()
        };

        // 保存到后端数据库
        try {
          // 转换为后端API所需格式（支持班级拆分）
          const assignments = [];
          
          for (const allocation of payload.allocations) {
            // 检查是否有班级详情
            if (allocation.details && allocation.details.length > 0) {
              // 遍历每个班级（可能有多个班级或拆分的班级片段）
              for (const detail of allocation.details) {
                if (detail.type === 'class') {
                  // 🔥 关键修复：获取该班级的学生列表，必须先按性别过滤！
                  const allClassStudents = this.getStudentsByMajorAndClass(
                    allocation.majorName, 
                    detail.className
                  );
                  
                  // 🔥 第一步：根据楼栋性别过滤学生
                  const buildingGender = this.currentAssignmentBuilding.gender; // 直接使用中文值
                  const classStudents = allClassStudents.filter(s => s.gender === buildingGender);
                  
                  console.log(`📊 获取学生列表：${allocation.majorName}-${detail.className}`);
                  console.log(`   全部学生：${allClassStudents.length}名，${this.currentAssignmentBuilding.gender}生：${classStudents.length}名`);
                  
                  if (classStudents.length === 0) {
                    console.warn(`⚠️ 警告：${allocation.majorName}-${detail.className} 没有${this.currentAssignmentBuilding.gender}生，跳过`);
                    continue;
                  }
                  
                  // 如果是拆分的班级，需要按学号排序并取指定数量
                  let segmentStudents = classStudents;
                  let studentIdRange = '';
                  
                  if (detail.isSplit) {
                    // 🔥 第二步：在同性别学生中按学号排序
                    segmentStudents = [...classStudents].sort((a, b) => 
                      a.studentId.localeCompare(b.studentId)
                    );
                    
                    // 计算该分片应该包含的学生
                    const segmentSize = detail.students;
                    const segment = detail.segment;
                    
                    // 🔥 第三步：根据分片标识确定起始位置（在同性别学生中切分）
                    const segmentIndex = segment.charCodeAt(0) - 65; // A=0, B=1, C=2
                    const startIdx = segmentIndex * segmentSize;
                    const endIdx = startIdx + segmentSize;
                    
                    segmentStudents = segmentStudents.slice(startIdx, endIdx);
                    
                    console.log(`   🔀 拆分${segment}组：从第${startIdx}名到第${endIdx}名（共${segmentStudents.length}名${this.currentAssignmentBuilding.gender}生）`);
                    
                    // 生成学号范围
                    if (segmentStudents.length > 0) {
                      studentIdRange = `${segmentStudents[0].studentId}~${segmentStudents[segmentStudents.length - 1].studentId}`;
                    }
                  }
                  
                  assignments.push({
                    majorName: allocation.majorName,
                    className: detail.className,
                    classSegment: detail.isSplit ? detail.segment : null,
                    grade: this.getCorrectGrade(allocation.majorName, detail.className),
                    gender: this.currentAssignmentBuilding.gender, // 🔥 直接使用中文性别（男/女）
                    allocatedStudents: detail.students || 0,
                    allocatedRooms: detail.rooms || 0,
                    assignmentMode: allocation.mode || 'class-only',
                    studentIdRange: studentIdRange,
                    roomType: this.currentAssignmentBuilding.roomType, // 🔥 新增：房间类型（中文格式）
                    studentIds: segmentStudents.map(s => s.id) // 传递学生ID列表给后端
                  });
                }
              }
            } else {
              // 整专业模式（无班级详情）
              assignments.push({
                majorName: allocation.majorName,
                className: '',
                classSegment: null,
                grade: this.getCorrectGrade(allocation.majorName, null),
                gender: this.currentAssignmentBuilding.gender, // 🔥 修复：直接使用中文性别
                allocatedStudents: allocation.students || 0,
                allocatedRooms: allocation.rooms || 0,
                assignmentMode: allocation.mode || 'class-only',
                studentIdRange: null,
                roomType: this.currentAssignmentBuilding.roomType, // 🔥 新增：房间类型（中文格式）
                studentIds: []
              });
            }
          }

          // 调用API保存专业分配
          console.log('📤 准备保存专业分配到后端，数据：', {
            buildingId: payload.buildingId,
            assignmentsCount: assignments.length,
            firstAssignment: assignments[0]
          });
          
          await saveBuildingMajorAssignment(payload.buildingId, assignments);
          
          console.log('✅ 专业分配保存到后端成功');
          
          // 🔥 关键修复：同步更新 buildingMajorAssignmentsV2 localStorage，供楼栋管理员读取
          // 需要在 allocations 中添加 grade 信息
          const allocationsWithGrade = payload.allocations.map(allocation => {
            const allocationCopy = { ...allocation };
            if (allocation.details && allocation.details.length > 0) {
              allocationCopy.details = allocation.details.map(detail => ({
                ...detail,
                grade: this.getCorrectGrade(allocation.majorName, detail.className)
              }));
            } else {
              // 整专业模式，添加年级
              allocationCopy.grade = this.getCorrectGrade(allocation.majorName, null);
            }
            return allocationCopy;
          });
          
          const store = JSON.parse(localStorage.getItem('buildingMajorAssignmentsV2') || '{}');
          store[payload.buildingId] = {
            buildingId: payload.buildingId,
            buildingName: payload.buildingName,
            allocations: allocationsWithGrade,
            updatedAt: new Date().toISOString()
          };
          localStorage.setItem('buildingMajorAssignmentsV2', JSON.stringify(store));
          console.log('✅ 已更新 buildingMajorAssignmentsV2 localStorage:', store[payload.buildingId]);
          
          this.$message.success('专业分配保存成功');
        } catch (error) {
          console.error('保存专业分配到数据库失败:', error);
          console.error('错误详情:', {
            message: error.message,
            response: error.response?.data,
            status: error.response?.status
          });
          
          const errorMsg = error.response?.data?.message || error.message || '保存失败';
          this.$message.error(`保存失败：${errorMsg}`);
          return;
        }
        this.majorAssignmentDialogVisible = false;
        this.logOperation(`专业分配：为楼栋 ${this.currentAssignmentBuilding.buildingName} 保存 ${result.length} 个专业的方案（占用${payload.summary.usedRooms}间）`, '楼栋管理');
        
        // 重新加载该楼栋的专业分配缓存
        await this.loadBuildingMajorAssignment(payload.buildingId);
      } catch (error) {
        console.error('保存专业分配失败:', error);
        this.$message.error('保存失败，请重试');
      } finally {
        this.assignmentSaving = false;
      }
    },
    
    // 宿舍管理相关方法
    showBatchAddDialog() {
      this.batchAddForm = {
        buildingId: '',
        startFloor: 1,
        endFloor: 6,
        roomsPerFloor: 10,
        roomType: '四人间'
      };
      this.batchAddDialogVisible = true;
    },
    
    showAddDormDialog() {
      this.addDormForm = {
        buildingId: '',
        room: '',
        type: '四人间'
      };
      this.addDormDialogVisible = true;
    },
    
    async saveBatchAdd() {
      if (!this.batchAddForm.buildingId) {
        this.$message.warning('请选择楼栋');
        return;
      }
      
      if (this.batchAddForm.startFloor > this.batchAddForm.endFloor) {
        this.$message.warning('起始楼层不能大于结束楼层');
        return;
      }
      
      if (!this.batchAddForm.roomsPerFloor || this.batchAddForm.roomsPerFloor <= 0) {
        this.$message.warning('每层房间数必须大于0');
        return;
      }
      
      if (!this.batchAddForm.roomType) {
        this.$message.warning('请选择房间类型');
        return;
      }
      
      const building = this.availableBuildings.find(b => b.id == this.batchAddForm.buildingId);
      if (!building) {
        this.$message.error('选择的楼栋不存在');
        return;
      }
      
      // 计算容纳人数（根据房间类型）
      const capacityMap = {
        '双人间': 2,
        '四人间': 4,
        '六人间': 6,
        '八人间': 8
      };
      const capacity = capacityMap[this.batchAddForm.roomType] || 4;
      
      // 转换房间类型为后端格式（数据库已改为中文）
      const roomTypeMap = {
        '双人间': '2人间',
        '四人间': '4人间',
        '六人间': '6人间',
        '八人间': '8人间'
      };
      const roomType = roomTypeMap[this.batchAddForm.roomType] || '4人间';
      
      try {
        // 调用后端批量创建API
        const res = await batchCreateRooms({
          buildingId: this.batchAddForm.buildingId,
          startFloor: this.batchAddForm.startFloor,
          endFloor: this.batchAddForm.endFloor,
          roomsPerFloor: this.batchAddForm.roomsPerFloor,
          bedCount: capacity,  // 修复：使用bedCount而不是capacity
          roomType: roomType,
          facilities: JSON.stringify({ 空调: true, 独立卫浴: false })  // 默认设施
        });
        
        if (res.code === 200) {
          const result = res.data;
          this.batchAddDialogVisible = false;
          
          if (result.failureCount > 0) {
            this.$message.warning(`批量创建完成：成功 ${result.successCount} 个，失败 ${result.failureCount} 个。${result.errors.slice(0, 3).join('；')}`);
          } else {
            this.$message.success(`成功批量添加 ${result.successCount} 个宿舍`);
          }
          
          this.logOperation(`批量添加宿舍：${building.name} ${result.successCount}个`, '楼栋管理');
          
          // 刷新楼栋列表统计
          await this.loadAvailableBuildings();
          await this.loadBuildingDormSummary();
        } else {
          this.$message.error(res.message || '批量创建失败');
        }
      } catch (error) {
        console.error('批量创建宿舍失败：', error);
        this.$message.error('批量创建失败：' + (error.message || '网络错误'));
      }
    },
    
    async saveAddDorm() {
      if (!this.addDormForm.buildingId || !this.addDormForm.room) {
        this.$message.warning('请填写完整信息');
        return;
      }
      
      const building = this.availableBuildings.find(b => b.id == this.addDormForm.buildingId);
      if (!building) {
        this.$message.error('选择的楼栋不存在');
        return;
      }
      
      // 转换房间类型为后端格式（数据库已改为中文）
      const roomTypeMap = {
        '双人间': '2人间',
        '四人间': '4人间',
        '六人间': '6人间',
        '八人间': '8人间'
      };
      const roomType = roomTypeMap[this.addDormForm.type] || '4人间';
      
      // 计算床位数
      const capacityMap = {
        '双人间': 2,
        '四人间': 4,
        '六人间': 6,
        '八人间': 8
      };
      const bedCount = capacityMap[this.addDormForm.type] || 4;
      
      // 计算楼层
      const floor = Math.floor(parseInt(this.addDormForm.room) / 100);
      
      try {
        // 调用后端API创建房间
        const roomData = {
          buildingId: this.addDormForm.buildingId,
          roomNumber: this.addDormForm.room,
          floor: floor,
          roomType: roomType,
          bedCount: bedCount,
          availableBeds: bedCount,
          status: 'available',
          facilities: JSON.stringify({ 空调: true, 独立卫浴: false })  // 默认设施
        };
        
        const res = await createRoom(roomData);
        
        if (res.code === 200) {
          this.addDormDialogVisible = false;
          this.$message.success('宿舍添加成功');
          this.logOperation(`添加宿舍：${building.buildingName || building.name} ${this.addDormForm.room}`, '楼栋管理');
          
          // 刷新楼栋统计和列表
          await this.loadBuildingDormSummary();
          if (this.currentBuildingDorms) {
            await this.viewBuildingDorms(this.currentBuildingDorms);
          }
        } else {
          this.$message.error(res.message || '添加失败');
        }
      } catch (error) {
        console.error('添加宿舍失败：', error);
        this.$message.error('添加失败：' + (error.message || '网络错误'));
      }
    },
    
    async viewBuildingDorms(buildingData) {
      this.currentBuildingDorms = buildingData;
      
      // 清空之前的选择状态
      this.selectedDorms = [];
      this.selectedDormIds = [];
      
      try {
        // 调用后端API加载该楼栋的宿舍详情
        const res = await getRoomsByBuilding(buildingData.buildingId || buildingData.id);
        
        if (res.code === 200 && res.data) {
          // 转换后端数据格式为前端需要的格式
          this.buildingDormsList = res.data.map(room => ({
            id: room.id,
            buildingId: room.buildingId,
            building: room.buildingName || buildingData.buildingName || buildingData.name,
            room: room.roomNumber,
            roomNumber: room.roomNumber,
            floor: room.floor,
            type: this.convertRoomTypeToDisplay(room.roomType),
            roomType: room.roomType,
            totalBeds: room.bedCount || 0,
            occupiedBeds: room.occupiedBeds || 0,
            availableBeds: room.availableBeds || 0,
            status: room.status === 'available' ? 'available' : room.status,
            facilities: room.facilities,
            students: room.students || [],  // 居住学生列表（后端返回）
            residents: room.students || []  // 保持兼容性
          }));
          
          // 清空表格选择状态
          this.$nextTick(() => {
            if (this.$refs.dormsTable) {
              this.$refs.dormsTable.clearSelection();
            }
          });
        } else {
          this.$message.warning('加载宿舍列表失败');
          this.buildingDormsList = [];
        }
      } catch (error) {
        console.error('加载宿舍列表失败：', error);
        this.$message.error('加载宿舍列表失败：' + (error.message || '网络错误'));
        this.buildingDormsList = [];
      }
      
      this.buildingDormsDialogVisible = true;
    },
    
    // 转换房间类型为显示文本（数据库已改为中文）
    convertRoomTypeToDisplay(roomType) {
      const typeMap = {
        '2人间': '双人间',
        '4人间': '四人间',
        '6人间': '六人间',
        '8人间': '八人间',
        '单人间': '单人间',
        '3人间': '三人间',
        // 兼容旧数据
        'double_bed': '双人间',
        'four_bed': '四人间',
        'six_bed': '六人间',
        'eight_bed': '八人间'
      };
      return typeMap[roomType] || roomType || '四人间';
    },
    
    handleDormSelectionChange(selection) {
      this.selectedDorms = selection;
      // 合并到跨页选择集合
      const pageIds = new Set(this.buildingDormsPagedList.map(d => d.id));
      // 先移除本页所有 id
      this.selectedDormIds = this.selectedDormIds.filter(id => !pageIds.has(id));
      // 再把当前页勾选的 id 加入
      this.selectedDormIds.push(...selection.map(d => d.id));
      // 去重
      this.selectedDormIds = Array.from(new Set(this.selectedDormIds));
    },
    handleDormsPageChange(page) {
      this.dormsPage = page;
      // 翻页后根据跨页选择集合恢复本页的勾选状态
      this.$nextTick(() => {
        const rowsToSelect = this.buildingDormsPagedList.filter(d => this.selectedDormIds.includes(d.id));
        if (this.$refs.dormsTable) {
          this.$refs.dormsTable.clearSelection();
          rowsToSelect.forEach(row => this.$refs.dormsTable.toggleRowSelection(row, true));
        }
      });
    },

    // 切换全选状态
    toggleSelectAllDorms() {
      if (this.isAllSelected) {
        // 取消全选
        this.selectedDormIds = [];
        this.selectedDorms = [];
        // 清空当前页的选择状态
        if (this.$refs.dormsTable) {
          this.$refs.dormsTable.clearSelection();
        }
        this.$message.info('已取消全选');
      } else {
        // 全选
        this.selectedDormIds = this.buildingDormsList.map(d => d.id);
        // 更新当前页的选择状态
        this.$nextTick(() => {
          if (this.$refs.dormsTable) {
            this.$refs.dormsTable.clearSelection();
            this.buildingDormsPagedList.forEach(row => {
              this.$refs.dormsTable.toggleRowSelection(row, true);
            });
          }
        });
        this.$message.success(`已全选 ${this.buildingDormsList.length} 间宿舍`);
      }
    },

    // 更新当前楼栋的统计信息
    updateCurrentBuildingStats() {
      if (!this.currentBuildingDorms) return;
      
      const totalRooms = this.buildingDormsList.length;
      const totalBeds = this.buildingDormsList.reduce((sum, dorm) => sum + (dorm.totalBeds || 0), 0);
      const occupiedBeds = this.buildingDormsList.reduce((sum, dorm) => sum + (dorm.occupiedBeds || 0), 0);
      const occupancyRate = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0;
      
      // 更新当前楼栋统计信息
      this.currentBuildingDorms.totalRooms = totalRooms;
      this.currentBuildingDorms.totalBeds = totalBeds;
      this.currentBuildingDorms.occupiedBeds = occupiedBeds;
      this.currentBuildingDorms.occupancyRate = occupancyRate;
    },
    
    showBatchEditDialog() {
      this.batchEditForm = {
        roomType: '',
        action: 'edit'
      };
      this.batchEditDialogVisible = true;
    },
    
    async saveBatchEdit() {
      if (this.batchEditForm.action === 'edit' && !this.batchEditForm.roomType) {
        this.$message.warning('请选择房间类型');
        return;
      }
      
      if (this.batchEditForm.action === 'delete') {
        this.batchDeleteDorms();
        return;
      }
      
      // 批量修改房间类型（调用后端API）
      const targetIds = this.selectedDormIds.length > 0 ? this.selectedDormIds : this.selectedDorms.map(d => d.id);
      
      if (targetIds.length === 0) {
        this.$message.warning('请先选择要编辑的宿舍');
        return;
      }
      
      // 计算床位数（根据房间类型）
      const capacityMap = {
        '双人间': 2,
        '四人间': 4,
        '六人间': 6,
        '八人间': 8
      };
      const capacity = capacityMap[this.batchEditForm.roomType] || 4;
      
      // 转换房间类型为后端格式（数据库已改为中文）
      const roomTypeMap = {
        '双人间': '2人间',
        '四人间': '4人间',
        '六人间': '6人间',
        '八人间': '8人间'
      };
      const roomType = roomTypeMap[this.batchEditForm.roomType] || '4人间';
      
      try {
        // 调用后端批量更新API（同时更新房间类型和床位数）
        const res = await batchUpdateRoomType(targetIds, roomType, capacity);
        
        if (res.code === 200) {
          const result = res.data;
          this.batchEditDialogVisible = false;
          
          if (result.failureCount > 0) {
            this.$message.warning(`批量更新完成：成功 ${result.successCount} 个，失败 ${result.failureCount} 个。${result.errors.slice(0, 3).join('；')}`);
          } else {
            this.$message.success(`成功修改 ${result.successCount} 个宿舍`);
          }
          
          this.logOperation(`批量修改宿舍类型：${this.batchEditForm.roomType}`, '楼栋管理');
          
          // 清空选择
          this.selectedDorms = [];
          this.selectedDormIds = [];
          
          // 刷新楼栋列表统计
          await this.loadAvailableBuildings();
          await this.loadBuildingDormSummary();
          // 如果在详情页，刷新详情页数据
          if (this.currentBuildingDorms) {
            await this.viewBuildingDorms(this.currentBuildingDorms);
          }
        } else {
          this.$message.error(res.message || '批量更新失败');
        }
      } catch (error) {
        console.error('批量更新宿舍失败：', error);
        this.$message.error('批量更新失败：' + (error.message || '网络错误'));
      }
    },
    
    // 单个删除宿舍
    async deleteDorm(dorm) {
      try {
        await this.$confirm(
          `确定要删除宿舍 ${dorm.roomNumber || dorm.room} 吗？`, 
          '确认删除', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        // 检查是否有学生入住
        if (dorm.occupiedBeds > 0 || dorm.currentCount > 0) {
          this.$message.warning(`宿舍 ${dorm.roomNumber || dorm.room} 有学生入住，无法删除`);
          return;
        }
        
        // 调用后端删除API
        const res = await batchDeleteRooms([dorm.id]);
        
        if (res.code === 200) {
          const result = res.data;
          
          if (result.successCount > 0) {
            // 刷新数据
            await this.loadAvailableBuildings();
            await this.loadBuildingDormSummary();
            
            // 刷新详情页
            if (this.currentBuildingDorms) {
              await this.viewBuildingDorms(this.currentBuildingDorms);
            }
            
            this.$message.success(`宿舍 ${dorm.roomNumber || dorm.room} 删除成功`);
            this.logOperation(`删除宿舍：${dorm.roomNumber || dorm.room}`, '楼栋管理');
          } else {
            this.$message.error(result.errors[0] || '删除失败');
          }
        } else {
          this.$message.error(res.message || '删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除宿舍失败：', error);
          this.$message.error('删除失败：' + (error.message || '网络错误'));
        }
      }
    },
    
    async batchDeleteDorms() {
      if (this.selectedDormIds.length === 0) {
        this.$message.warning('请先选择要删除的宿舍');
        return;
      }
      
      try {
        await this.$confirm(
          `确定要删除选中的 ${this.selectedDormIds.length} 个宿舍吗？\n\n注意：有学生入住的宿舍将无法删除。`, 
          '确认删除', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        );
        
        // 调用后端批量删除API
        const res = await batchDeleteRooms(this.selectedDormIds);
        
        if (res.code === 200) {
          const result = res.data;
          this.batchEditDialogVisible = false;
          
          // 清空选择
          this.selectedDorms = [];
          this.selectedDormIds = [];
          
          // 刷新数据
          await this.loadAvailableBuildings();
          await this.loadBuildingDormSummary();
          
          // 刷新详情页数据
          if (this.currentBuildingDorms) {
            await this.viewBuildingDorms(this.currentBuildingDorms);
          }
          
          // 处理提示信息
          if (result.failureCount > 0) {
            // 如果有失败的，显示详细信息
            if (result.occupiedRoomNumbers && result.occupiedRoomNumbers.length > 0) {
              // 有学生入住的房间
              const occupiedCount = result.occupiedRoomNumbers.length;
              const roomList = result.occupiedRoomNumbers.slice(0, 10).join('、');
              const moreText = occupiedCount > 10 ? ` 等${occupiedCount}个` : '';
              
              this.$alert(
                `批量删除完成：成功 ${result.successCount} 个，失败 ${result.failureCount} 个。\n\n以下宿舍有学生入住，无法删除：\n${roomList}${moreText}`,
                '删除结果',
                {
                  confirmButtonText: '知道了',
                  type: 'warning',
                  dangerouslyUseHTMLString: false
                }
              );
            } else {
              // 其他原因失败
              this.$message.warning(`批量删除完成：成功 ${result.successCount} 个，失败 ${result.failureCount} 个。${result.errors.slice(0, 2).join('；')}`);
            }
          } else {
            // 全部成功
            this.$message.success(`成功删除 ${result.successCount} 个宿舍`);
          }
          
          this.logOperation(`批量删除宿舍：成功${result.successCount}个`, '楼栋管理');
        } else {
          this.$message.error(res.message || '批量删除失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量删除宿舍失败：', error);
          this.$message.error('批量删除失败：' + (error.message || '网络错误'));
        }
      }
    },
    
    // 专业分配相关方法
    async showMajorAssignmentDialog(buildingData) {
      // 楼栋基础属性（性别、房型、宿舍数、总容量）
      const bedsPerRoom = buildingData.bedsPerRoom || 4;
      const totalRooms = buildingData.totalRooms || 50;
      this.currentAssignmentBuilding = {
        buildingId: buildingData.buildingId || buildingData.id,
        buildingName: buildingData.buildingName || buildingData.name,
        gender: buildingData.gender,
        bedsPerRoom,
        totalRooms,
        totalCapacity: totalRooms * bedsPerRoom
      };

      // 🔥 修复：从后端API获取学生数据
      let students = [];
      try {
        const response = await getStudentList({
          currentPage: 1,
          pageSize: 10000, // 获取所有学生
          gender: buildingData.buildingType || '', // 使用buildingType（male/female）而不是gender（男/女）
          registrationStatus: '' // 不过滤注册状态，查询所有学生
        });
        
        if (response.code === 200 && response.data && response.data.students) {
          students = response.data.students;
        } else {
          this.$message.warning('获取学生数据失败，无法进行专业分配');
          return;
        }
      } catch (error) {
        console.error('获取学生数据失败:', error);
        this.$message.error('获取学生数据失败');
        return;
      }
      
      // 按专业分组统计学生和班级
      const majorStats = {}
      students.forEach(student => {
        const majorName = student.major
        const className = student.className || student.class
        
        if (!majorStats[majorName]) {
          majorStats[majorName] = {
            name: majorName,
            students: [],
            classes: {}
          }
        }
        
        majorStats[majorName].students.push(student)
        
        if (className) {
          if (!majorStats[majorName].classes[className]) {
            majorStats[majorName].classes[className] = 0
          }
          majorStats[majorName].classes[className]++
        }
      })
      
      // 🔥 检查所有楼栋的专业分配，排除已分配的学生
      const currentBuildingId = buildingData.buildingId || buildingData.id;
      Object.keys(this.buildingMajorAssignmentsCache).forEach(buildingId => {
        const assignment = this.buildingMajorAssignmentsCache[buildingId];
        if (assignment && assignment.allocations) {
          assignment.allocations.forEach(allocation => {
            const majorName = allocation.majorName;
            const allocatedStudents = allocation.students || 0;
            
            // 如果该专业已在某楼栋分配，减去已分配的学生数
            if (majorStats[majorName]) {
              // 从学生列表中移除已分配的学生数量
              const remainingStudents = majorStats[majorName].students.slice(allocatedStudents);
              majorStats[majorName].students = remainingStudents;
              
              // 重新计算班级人数
              majorStats[majorName].classes = {};
              remainingStudents.forEach(student => {
                const className = student.className || student.class;
                if (className) {
                  if (!majorStats[majorName].classes[className]) {
                    majorStats[majorName].classes[className] = 0;
                  }
                  majorStats[majorName].classes[className]++;
                }
              });
              
              // 如果该专业已全部分配完，删除该专业（不再出现在可选列表）
              if (majorStats[majorName].students.length === 0) {
                delete majorStats[majorName];
              }
            }
          });
        }
      });
      
      // 转换为适合分配的格式
      const realMajors = Object.values(majorStats).map((major, index) => {
        const classNames = Object.keys(major.classes)
        const classSizes = classNames.length > 0 
          ? Object.values(major.classes)
          : [major.students.length] // 如果没有班级信息，整个专业作为一个班级
        
        return {
          id: major.name.toLowerCase().replace(/\s+/g, ''),
          name: major.name,
          classNames: classNames.length > 0 ? classNames : ['未分班'], // 🔥 保留实际的班级名称
          classSizes: classSizes
        }
      })

      // 预构建每个专业的班级与宿舍包
      this.availableMajorsForAssignment = realMajors.map(m => {
        const classes = m.classSizes.map((size, idx) => ({
          classIndex: idx,
          className: m.classNames[idx], // 🔥 使用实际的className
          size,
          roomsNeeded: Math.ceil(size / bedsPerRoom),
          fullRooms: Math.floor(size / bedsPerRoom),
          remainder: size % bedsPerRoom,
          bundles: [] // 每个元素代表一个宿舍包（占1间，value为入住人数）
        }));
        classes.forEach(c => {
          for (let i = 0; i < c.fullRooms; i += 1) {
            c.bundles.push({ size: bedsPerRoom, type: 'full' });
          }
          if (c.remainder > 0) {
            c.bundles.push({ size: c.remainder, type: 'partial' });
          }
        });
        const totalStudents = m.classSizes.reduce((a, b) => a + b, 0);
        const roomsNeeded = classes.reduce((sum, c) => sum + c.roomsNeeded, 0);
        return {
          id: m.id,
          name: m.name,
          totalStudents,
          roomsNeeded,
          classes
        };
      });

      // 初始化选择与分配结果
      this.selectedMajorsForAssignment = this.availableMajorsForAssignment.map(m => m.name);
      this.smartAllocation = null;
      this.majorAssignmentDialogVisible = true;
    },
    
    getUtilizationColor(percentage) {
      if (percentage >= 90) return '#f56c6c'; // 红色 - 接近满载
      if (percentage >= 70) return '#e6a23c'; // 橙色 - 较高利用率
      if (percentage >= 50) return '#409eff'; // 蓝色 - 中等利用率
      return '#67c23a'; // 绿色 - 低利用率
    },
    onSmartMatch() {
      if (!this.currentAssignmentBuilding) return;
      if (!this.selectedMajorsForAssignment || this.selectedMajorsForAssignment.length === 0) {
        this.$message.warning('请先选择需要分配的专业');
        return;
      }
      this.smartAllocation = this.calculateSmartFit(this.selectedMajorsForAssignment);
    },
    onResetSelection() {
      this.selectedMajorsForAssignment = [];
      this.smartAllocation = null;
    },
    // 一键压缩：在已选方案基础上，尽量减少占用宿舍直到不超
    onCompressToFit() {
      const limit = this.currentAssignmentBuilding.totalRooms;
      if (!this.smartAllocation || this.smartAllocation.usedRooms <= limit) return;
      // 简化策略：按“占用宿舍/收益(学生数)最小”的明细反向移除
      const clone = JSON.parse(JSON.stringify(this.smartAllocation));
      const flat = [];
      clone.results.forEach(r => {
        // 从“宿舍包明细”构造可回退项
        (r.details || []).forEach(d => flat.push({ owner: r, entry: d }));
        // 同时允许整体回退整班（当详情中含有class聚合）
        (r.details || [])
          .filter(d => d.type === 'class')
          .forEach(d => flat.push({ owner: r, entry: { type: 'class-whole', rooms: d.rooms, students: d.students } }));
      });
      // 价值/重量：students/rooms 低优先移除
      flat.sort((a, b) => (a.entry.students / a.entry.rooms) - (b.entry.students / b.entry.rooms));
      for (const item of flat) {
        if (clone.usedRooms <= limit) break;
        const target = item.owner;
        if (!target || target.rooms <= 0) continue;
        const rmRooms = Math.min(item.entry.rooms, clone.usedRooms - limit);
        const rmStudents = Math.round(item.entry.students * (rmRooms / item.entry.rooms));
        target.rooms -= rmRooms;
        target.students = Math.max(0, target.students - rmStudents);
        clone.usedRooms -= rmRooms;
        target.overflow = (target.overflow || 0) + rmStudents;
      }
      this.smartAllocation = clone;
      this.$message.success('已压缩到可容纳范围');
    },
    // 一键补齐：在未满时，尽量提升利用率（模拟从其他专业/剩余里补齐）
    onFillToFull() {
      const limit = this.currentAssignmentBuilding.totalRooms;
      if (!this.smartAllocation || this.smartAllocation.usedRooms >= limit) return;
      const clone = JSON.parse(JSON.stringify(this.smartAllocation));
      const beds = this.currentAssignmentBuilding.bedsPerRoom;
      // 不允许凭空造人：总新增人数不能超过“所选专业总人数 - 当前已安置人数”
      const totalSelectable = this.availableMajorsForAssignment
        .filter(m => this.selectedMajorsForAssignment.includes(m.name))
        .reduce((s, m) => s + (m.totalStudents || 0), 0);
      const alreadyPlaced = clone.results.reduce((s, r) => s + (r.students || 0), 0);
      let remainingStudents = Math.max(0, totalSelectable - alreadyPlaced);

      while (clone.usedRooms < limit && remainingStudents > 0 && clone.results.length > 0) {
        for (const r of clone.results) {
          if (clone.usedRooms >= limit || remainingStudents <= 0) break;
          const put = Math.min(beds, remainingStudents);
          if (put <= 0) break;
          r.details = r.details || [];
          r.details.push({ type: 'bundle', rooms: 1, students: put });
          r.rooms += 1;
          r.students += put;
          clone.usedRooms += 1;
          remainingStudents -= put;
        }
      }
      this.smartAllocation = clone;
      this.$message.success('已尝试补齐以提升利用率');
    },
    
    // 专业分配：支持完整分配和拆分分配
    calculateSmartFit(selectedMajorsNames) {
      const totalRooms = this.currentAssignmentBuilding.totalRooms;
      const bedsPerRoom = this.currentAssignmentBuilding.bedsPerRoom;
      const selectedMajors = this.availableMajorsForAssignment.filter(m => selectedMajorsNames.includes(m.name));

      // 🔥 关键修复：计算已分配的宿舍数量，从总数中扣除
      const assignedRooms = this.getAssignedRoomsCount(this.currentAssignmentBuilding.buildingId);
      const availableRooms = totalRooms - assignedRooms;
      
      console.log(`专业分配：总宿舍${totalRooms}间，已分配${assignedRooms}间，可用${availableRooms}间`);
      
      if (availableRooms <= 0) {
        this.$message.warning('该楼栋已无可用宿舍，请先清空当前分配');
        return {
          mode: this.allocationMode,
          usedRooms: 0,
          totalRooms: availableRooms,
          results: []
        };
      }

      let usedRooms = 0;
      const results = [];

      // 1) 第一阶段：优先分配完整专业或完整班级
      for (const major of selectedMajors) {
        if (usedRooms + major.roomsNeeded <= availableRooms) {
          const hasClassInfo = major.classes && major.classes.length > 0;
          
          if (hasClassInfo) {
            results.push({
              majorId: major.id,
              name: major.name,
              mode: 'class-only',
              rooms: major.roomsNeeded,
              students: major.totalStudents,
              details: major.classes.map(cls => ({
                type: 'class',
                classIndex: cls.classIndex,
                className: cls.className,
                rooms: cls.roomsNeeded,
                students: cls.size,
                isSplit: false
              }))
            });
          } else {
            results.push({
              majorId: major.id,
              name: major.name,
              mode: 'full-major',
              rooms: major.roomsNeeded,
              students: major.totalStudents,
              details: []
            });
          }
          usedRooms += major.roomsNeeded;
        } else {
          results.push({ majorId: major.id, name: major.name, mode: 'pending', rooms: 0, students: 0, details: [] });
        }
      }

      // 2) 第二阶段：处理pending专业，尝试装入整班
      for (const res of results) {
        if (res.mode !== 'pending') continue;
        const major = selectedMajors.find(m => m.id === res.majorId);
        const classesSorted = [...major.classes].sort((a, b) => a.roomsNeeded - b.roomsNeeded);
        for (const cls of classesSorted) {
          if (usedRooms + cls.roomsNeeded <= availableRooms) {
            res.mode = 'class-only';
            res.details.push({ 
              type: 'class', 
              classIndex: cls.classIndex, 
              className: cls.className, 
              rooms: cls.roomsNeeded, 
              students: cls.size,
              isSplit: false
            });
            res.rooms += cls.roomsNeeded;
            res.students += cls.size;
            usedRooms += cls.roomsNeeded;
          }
        }
      }

      // 3) 第三阶段：拆分分配 - 填充剩余空间（拆分班级）
      if (this.allocationMode === 'flexible' && usedRooms < availableRooms) {
        const remainingRooms = availableRooms - usedRooms;
        const remainingBeds = remainingRooms * bedsPerRoom;
        
        console.log(`拆分分配：剩余${remainingRooms}间宿舍（${remainingBeds}个床位），尝试填充`);
        
        // 收集所有未完全分配的班级
        const pendingClasses = [];
        for (const res of results) {
          const major = selectedMajors.find(m => m.id === res.majorId);
          if (!major || !major.classes) continue;
          
          const allocatedClasses = res.details.filter(d => d.type === 'class').map(d => d.classIndex);
          const unallocatedClasses = major.classes.filter(cls => !allocatedClasses.includes(cls.classIndex));
          
          unallocatedClasses.forEach(cls => {
            pendingClasses.push({
              major: res,
              class: cls,
              majorName: major.name
            });
          });
        }
        
        // 按班级人数倒序排序（优先拆分大班级）
        pendingClasses.sort((a, b) => b.class.size - a.class.size);
        
        // 尝试填充
        for (const pc of pendingClasses) {
          if (usedRooms >= availableRooms) break;  // 🔥 修复：使用availableRooms而非totalRooms
          
          const cls = pc.class;
          const availableBeds = (availableRooms - usedRooms) * bedsPerRoom;  // 🔥 修复：使用availableRooms
          
          if (availableBeds > 0 && cls.size > 0) {
            // 能装下部分学生，拆分班级
            const segmentStudents = Math.min(cls.size, availableBeds);
            const segmentRooms = Math.ceil(segmentStudents / bedsPerRoom);
            
            // 生成分片标识（A/B/C）
            const existingSegments = pc.major.details
              .filter(d => d.classIndex === cls.classIndex && d.segment)
              .map(d => d.segment);
            const nextSegment = String.fromCharCode(65 + existingSegments.length); // A=65
            
            pc.major.details.push({
              type: 'class',
              classIndex: cls.classIndex,
              className: cls.className,
              rooms: segmentRooms,
              students: segmentStudents,
              isSplit: true,
              segment: nextSegment,
              splitInfo: {
                originalSize: cls.size,
                segmentSize: segmentStudents,
                segment: nextSegment
              }
            });
            
            pc.major.rooms += segmentRooms;
            pc.major.students += segmentStudents;
            pc.major.mode = 'class-only';
            usedRooms += segmentRooms;
            
            console.log(`拆分 ${pc.majorName}-${cls.className}：分配${segmentStudents}人（${nextSegment}组）到当前楼栋`);
          }
        }
      }

      return { usedRooms, totalRooms: availableRooms, results, mode: this.allocationMode };
    },
    
    // 🔥 新增：获取楼栋已分配的宿舍数量
    getAssignedRoomsCount(buildingId) {
      const assignment = this.getBuildingMajorAssignment(buildingId);
      if (!assignment || !assignment.allocations) return 0;
      
      // 累加所有已分配专业占用的宿舍数量
      return assignment.allocations.reduce((total, allocation) => {
        return total + (allocation.rooms || 0);
      }, 0);
    },
    
    // 获取指定专业和班级的学生列表
    getStudentsByMajorAndClass(majorName, className) {
      return this.currentBuildingStudents.filter(s => 
        s.major === majorName && s.className === className
      );
    },
    
    // 🔥 新增：智能推断正确的年级
    getCorrectGrade(majorName, className) {
      console.log(`🔍 getCorrectGrade 调用：专业=${majorName}, 班级=${className}`);
      console.log(`🔍 currentBuildingStudents 数量：${this.currentBuildingStudents.length}`);
      
      // 1. 从学生数据中查找（最可靠）
      const student = this.currentBuildingStudents.find(s => {
        if (className) {
          return s.major === majorName && s.className === className;
        } else {
          return s.major === majorName;
        }
      });
      
      if (student && student.grade) {
        console.log(`✅ 从学生数据获取年级：${majorName}-${className} → ${student.grade}`);
        return student.grade;
      }
      
      console.warn(`⚠️ 未找到匹配的学生，专业=${majorName}, 班级=${className}`);
      
      // 2. 如果没有找到学生，尝试从班级名称推断
      // 班级名称格式：软工2301 → 23 → 2023
      if (className) {
        const match = className.match(/(\d{2})(\d{2})/);  // 匹配班级编号，如"2301"
        if (match) {
          const yearSuffix = match[1];  // "23"
          const grade = '20' + yearSuffix;  // "2023"
          console.log(`⚠️ 从班级名称推断年级：${className} → ${grade}`);
          return grade;
        }
      }
      
      // 3. 默认值：当前年份-1（假设是去年入学的学生）
      const currentYear = new Date().getFullYear();
      console.warn(`⚠️ 无法确定年级，使用默认值：${currentYear - 1}`);
      return String(currentYear - 1);
    },
    
    // 获取楼栋设计容量
    getBuildingCapacity(building) {
      // 从后端加载的统计数据中获取
      const summary = this.buildingDormSummaryData.find(s => s.buildingId === building.id);
      return summary ? summary.totalBeds : 0;
    },
    
    // 获取楼栋主要房间类型
    getBuildingRoomType(building) {
      // 从后端加载的统计数据中获取（返回房间类型统计）
      const summary = this.buildingDormSummaryData.find(s => s.buildingId === building.id);
      if (summary && summary.roomType) {
        const typeMap = {
          // 新格式（中文）
          '2人间': '双人间',
          '4人间': '四人间',
          '6人间': '六人间',
          '8人间': '八人间',
          '单人间': '单人间',
          '3人间': '三人间',
          // 兼容旧格式（英文）
          'four_bed': '四人间',
          'six_bed': '六人间',
          'double_bed': '双人间',
          'single_bed': '单人间',
          'triple_bed': '三人间'
        }
        return typeMap[summary.roomType] || summary.roomType;
      }
      
      // 如果没有统计数据，尝试从楼栋描述或其他字段推断（这里暂时返回默认）
      return '暂无数据';
    },

    // 初始化数据
    loadBuildingMajorAssignments() {
      // 模拟专业分配数据
      this.buildingMajorAssignments = this.availableBuildings.map(building => ({
        buildingId: building.id,
        buildingName: building.name,
        gender: building.gender,
        totalCapacity: 200,
        assignedMajors: [],
        utilizationRate: 0
      }));
    }
  }
}
</script>

<style scoped>
.building-management-page {
  padding: 0;
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

.header-buttons {
  display: flex;
  gap: 12px;
}

.building-search-area {
  margin-bottom: 24px;
}

.search-result-tip {
  margin-top: 16px;
  display: flex;
  align-items: center;
}

.building-stats {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 8px;
}

.buildings-table {
  margin-top: 20px;
}

.building-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.building-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.assignment-status {
  display: flex;
  justify-content: center;
}

.quick-assign-info {
  margin-bottom: 20px;
}

.building-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 16px;
}

.building-icon {
  font-size: 24px;
  color: #409eff;
}

.building-info h3 {
  margin: 0 0 8px 0;
  color: #303133;
}

.current-assignment {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label {
  font-weight: 600;
  color: #606266;
}

.major-assignment-content {
  max-height: 600px;
  overflow-y: auto;
}

/* 新版专业分配UI样式 */
.major-assign-new {
  max-height: 70vh;
  overflow-y: auto;
}

/* 分配模式选择器样式 */
.mode-selector {
  width: 100%;
  display: flex;
  gap: 16px;
}
.mode-selector .el-radio {
  flex: 1;
  margin-right: 0;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px 16px; /* 增加垂直padding */
  transition: all 0.3s;
  min-height: 90px; /* 确保有足够的高度 */
}
.mode-selector .el-radio:hover {
  border-color: #409eff;
  background: #f0f7ff;
}
.mode-selector .el-radio.is-checked {
  border-color: #409eff;
  background: #ecf5ff;
}
/* 确保 radio 内部标签有足够空间 */
.mode-selector .el-radio :deep(.el-radio__label) {
  width: 100%;
  padding-left: 8px;
  white-space: normal; /* 允许换行 */
}
.mode-option {
  margin-left: 0; /* 移除左边距，避免空间不足 */
  display: flex;
  flex-direction: column;
  justify-content: flex-start; /* 从顶部开始，不要居中 */
  width: 100%; /* 占满可用宽度 */
  padding-right: 8px; /* 右侧留一点空间 */
}
.mode-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  margin-bottom: 6px; /* 增加间距 */
}
.mode-desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.6; /* 增加行高 */
  word-break: break-word; /* 允许单词内换行 */
  overflow-wrap: break-word; /* 确保长单词换行 */
  white-space: normal; /* 允许换行 */
  max-width: 100%; /* 确保不会超出容器 */
  margin: 0; /* 移除默认边距 */
}

.building-info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 20px;
}
.title-icon { font-size: 30px; color: rgba(255,255,255,0.9); }
.info-left { display: flex; align-items: center; gap: 12px; }
.title-wrap h3 { margin: 0 0 6px 0; font-weight: 600; }
.meta-tags { 
  display: flex; 
  gap: 6px; 
}
/* 修复标签在紫色背景上的可读性 */
.meta-tags .el-tag {
  font-weight: 600;
  border: none;
}
.meta-tags .el-tag--primary {
  background-color: #409eff !important;
  color: #fff !important;
}
.meta-tags .el-tag--danger {
  background-color: #f56c6c !important;
  color: #fff !important;
}
.meta-tags .el-tag--info {
  background-color: rgba(255, 255, 255, 0.9) !important;
  color: #667eea !important;
}
.meta-tags .el-tag--warning {
  background-color: #e6a23c !important;
  color: #fff !important;
}
.meta-tags .el-tag--success {
  background-color: #67c23a !important;
  color: #fff !important;
}

/* 当前专业分配区域样式 */
.current-assignment-info {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}
/* 🔥 全新设计：当前专业分配卡片 */
.current-allocations-new {
  display: flex;
  flex-direction: column;
  gap: 10px;  /* 减小卡片间距 */
  margin-top: 12px;
}

.major-allocation-card {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.3s;
}

.major-allocation-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border-color: #c6e2ff;
}

/* 专业卡片头部 */
.major-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;  /* 减小内边距 */
  background: linear-gradient(to right, #f5f7fa, #ffffff);
  cursor: pointer;
  user-select: none;
  transition: background 0.3s;
}

.major-card-header:hover {
  background: linear-gradient(to right, #e8f4ff, #ffffff);
}

.major-card-header:active {
  background: #f0f7ff;
}

.major-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.expand-btn-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.expand-btn {
  color: #409eff;
  font-size: 18px;
  transition: transform 0.3s;
  cursor: pointer;
}

.expand-btn.is-expanded {
  transform: rotate(90deg);
}

.major-title-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.major-title {
  margin: 0;
  font-size: 15px;  /* 减小字体 */
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.major-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.major-header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  line-height: 1;
}

.stat-value {
  font-size: 15px;  /* 减小字体 */
  font-weight: 600;
  color: #409eff;
  line-height: 1;
}

/* 专业卡片主体（展开内容） */
.major-card-body {
  padding: 12px 16px;  /* 减小内边距 */
  background: #fafafa;
  border-top: 1px solid #e4e7ed;
}

.class-list {
  display: grid !important;  /* 强制Grid布局 */
  grid-template-columns: repeat(3, 1fr) !important;  /* 固定3列，均匀分布 */
  gap: 12px;  /* 增加间距让布局更舒适 */
  width: 100%;  /* 确保占满容器宽度 */
}

/* 小屏幕显示2列 */
@media (max-width: 800px) {
  .class-list {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

/* 移动端时显示1列 */
@media (max-width: 500px) {
  .class-list {
    grid-template-columns: 1fr !important;
  }
}

.class-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;  /* 减小内边距 */
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  transition: all 0.2s;
}

.class-item:hover {
  border-color: #409eff;
  background: #f0f7ff;
  /* 移除translateX，避免Grid布局抖动 */
}

.class-item-left {
  display: flex;
  align-items: center;
  gap: 8px;  /* 减小间距 */
  flex: 1;  /* 占据可用空间 */
  min-width: 0;  /* 允许flex收缩 */
}

.class-icon {
  color: #67c23a;
  font-size: 16px;
}

.class-label {
  font-size: 13px;  /* 减小字体 */
  color: #303133;
  font-weight: 500;
  white-space: nowrap;  /* 不换行 */
  overflow: hidden;  /* 超出隐藏 */
  text-overflow: ellipsis;  /* 显示省略号 */
}

.class-item-right {
  display: flex;
  align-items: center;
  gap: 6px;  /* 减小标签间距 */
  flex-shrink: 0;  /* 防止收缩 */
}
.segment-badge {
  display: inline-block;
  margin-left: 4px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.head-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.head-actions {
  display: flex;
  gap: 8px;
}

.section { background:#fff; border:1px solid #e4e7ed; border-radius:12px; padding:16px; margin-bottom:16px; }
.section-head { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:12px; }
.section-head h4 { margin:0; font-size:16px; font-weight:600; color:#303133; }
.section-head .sub { color:#909399; font-size:13px; }

.major-list { 
  display: grid; 
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); 
  gap: 16px; 
}

.major-card { 
  border: 1px solid #e4e7ed; 
  border-radius: 12px; 
  padding: 0;
  background: #fff; 
  transition: all 0.3s ease;
  overflow: hidden;
}

.major-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.major-card-head { 
  display: flex; 
  align-items: center; 
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #fafbfc 100%);
  border-bottom: 1px solid #e4e7ed;
}

.major-card-head :deep(.el-checkbox) {
  width: 100%;
}

.major-card-head :deep(.el-checkbox__label) {
  width: 100%;
  font-size: 15px;
}

.major-name { 
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

/* 专业选择区的卡片内容（统计信息） */
.major-list .major-card-body { 
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  gap: 8px;
}

.major-list .major-card-body .stat { 
  display: flex;
  align-items: baseline;
  gap: 3px;
  white-space: nowrap;
  flex-shrink: 0;
}

.major-list .major-card-body .label { 
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
}

.major-list .major-card-body .value {
  color: #409eff;
  font-weight: 600;
  font-size: 13px;
  line-height: 1.4;
}

.result-wrap { margin-top:8px; }
.result-grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap:12px; }
.result-card { border:2px solid #e4e7ed; border-radius:10px; padding:12px; background:#f9fafb; }
.result-title { font-weight:600; margin-bottom:6px; }
.result-stats .rs { font-size:13px; color:#606266; }
.result-stats .label { color:#909399; }
.result-summary { margin-top:8px; }

.building-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 20px;
}

.capacity-info {
  text-align: center;
}

.major-selection {
  margin-bottom: 20px;
}

.major-selection h4 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.major-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.major-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.major-checkbox {
  width: 100%;
}

.major-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-left: 24px;
}

.major-name {
  font-weight: 600;
  color: #303133;
}

.capacity-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.capacity-text {
  color: #909399;
  font-size: 14px;
}

.assignment-summary {
  margin-top: 20px;
}

:deep(.el-statistic__head) {
  font-size: 14px;
  color: #909399;
}

:deep(.el-statistic__content) {
  font-size: 24px;
  font-weight: 600;
}

 /* 主标签页样式（清理旧样式后重置为简洁风） */
 .main-tabs {
   margin: 0;
 }

 .main-tabs :deep(.el-tabs__header) {
   margin: 0;
   background: #fff;
   border-bottom: 1px solid #e4e7ed;
   padding: 0 24px;
   overflow: visible;
 }

 .main-tabs :deep(.el-tabs__nav-wrap) {
   padding: 0;
 }

 .main-tabs :deep(.el-tabs__item) {
   height: 48px;
   padding: 0 24px;
   display: flex;
   align-items: center;
   line-height: 1;
   font-size: 14px;
   font-weight: 500;
   color: #606266;
   border: none;
   background: transparent;
   box-shadow: none;
   transition: color 0.2s ease;
 }

 .main-tabs :deep(.el-tabs__item:hover) {
   color: #409eff;
 }

 .main-tabs :deep(.el-tabs__item.is-active) {
   color: #409eff;
   font-weight: 600;
 }

 .main-tabs :deep(.el-tabs__active-bar) {
   height: 3px;
   background: #409eff;
   border-radius: 2px;
 }

 .main-tabs :deep(.el-tabs__content) {
   padding: 0;
   background: #fff;
 }

 /* 楼栋信息标签页样式 */
 .building-info-content {
   padding: 20px;
 }

 .building-info-header {
   display: flex;
   justify-content: flex-end;
   margin-bottom: 20px;
 }

 .building-info-header .header-buttons {
   display: flex;
   gap: 12px;
 }

/* 宿舍管理样式 */
.dorm-management-content {
  padding: 20px;
}

.dorm-management-unified-header {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 8px;
}

.dorm-management-unified-header .header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dorm-management-unified-header .header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dorm-management-unified-header .header-icon {
  font-size: 24px;
  color: #409eff;
}

.dorm-management-unified-header .header-text h3 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.dorm-management-unified-header .header-text p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.dorm-management-unified-header .header-actions {
  display: flex;
  gap: 12px;
}

.dorm-management-unified-header .statistics-section {
  border-top: 1px solid #e4e7ed;
  padding-top: 20px;
}


.dorms-table {
  margin-top: 24px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-title h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.count-display {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.count-display.occupied {
  color: #67c23a;
}

.occupancy-rate {
  display: flex;
  justify-content: center;
}

/* 楼栋宿舍详情样式 */
.building-dorms-content {
  max-height: 600px;
  overflow-y: auto;
}

.dorms-header {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.building-summary {
  display: flex;
  gap: 40px;
  justify-content: center;
}

.residents-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.resident-tag {
  margin: 0;
}

.no-residents {
  color: #c0c4cc;
}

.batch-operations {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.form-tip {
  margin-top: 4px;
  padding: 4px 8px;
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 4px;
  font-size: 12px;
}

/* 批量编辑设施网格 */
.batch-facilities-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 6px;
  max-height: 300px;
  overflow-y: auto;
}

.batch-facility-item {
  padding: 8px 12px;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  transition: all 0.2s;
  cursor: pointer;
  user-select: none;
}

.batch-facility-item:hover {
  border-color: #409eff;
  box-shadow: 0 1px 3px rgba(64, 158, 255, 0.1);
}

.batch-facility-item.facility-checked {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.batch-facility-item :deep(.el-checkbox) {
  width: 100%;
}

.batch-facility-item :deep(.el-checkbox__label) {
  font-size: 13px;
}
</style>
