<template>
  <div class="student-allocation-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>学生分配管理</h2>
          <div class="header-buttons">
            <el-button v-if="currentPhase === 'completed'" type="success" plain @click="exportAllocationReport">
              <template #icon>
                <Document />
              </template>
              导出报表
            </el-button>
          </div>
        </div>
              </template>

      <!-- 阶段导航 -->
      <div class="phase-navigator">
        <el-steps :active="currentPhaseIndex" align-center class="custom-steps">
          <el-step title="分配范围确认" description="选择楼栋范围并确认专业分配" />
          <el-step title="学生自组宿舍" description="学生自由组建宿舍" />
          <el-step title="房间选择" description="已组满宿舍选择房间" />
          <el-step title="剩余处理" description="处理未分配学生" />
          <el-step title="分配完成" description="所有学生已分配" />
        </el-steps>
      </div>

      <!-- 阶段1: 分配范围确认 -->
      <div v-if="currentPhase === 'preparation'" class="phase-content">
        <!-- 批次信息设置 -->
        <el-card shadow="never" class="batch-info-card">
          <template #header>
              <div class="card-header-enhanced">
                <div class="header-left" style="display: flex; align-items: center; gap: 12px;">
                  <h3>📝 批次信息设置</h3>
                  <el-tag v-if="!batchDescription.trim()" type="warning" effect="plain" round class="pill">
                    请设置批次描述
                  </el-tag>
                </div>
              </div>
          </template>
          
          <div class="batch-info-form">
            <el-form :model="batchForm" label-width="120px" size="default">
              <el-form-item label="批次描述" required>
                <el-input
                  v-model="batchDescription"
                  placeholder="请输入本次分配的描述，如：2024级新生入学分配、转专业学生补充分配等"
                  maxlength="100"
                  show-word-limit
                  clearable
                />
                <div class="form-tip">
                  <el-text type="info" size="small">
                    💡 建议包含年级、分配类型等关键信息，便于后续查找和管理
                  </el-text>
                </div>
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <el-card shadow="never" class="allocation-status-card">
          <template #header>
            <div class="card-header-enhanced">
              <div class="header-left">
            <h3>📋 专业宿舍分配状态</h3>
                <!-- 🔥 新增：年级筛选 -->
                <div class="grade-filter" v-if="availableGrades.length > 0">
                  <span class="filter-label">年级筛选：</span>
                  <el-checkbox-group v-model="selectedGrades" @change="handleGradeFilterChange">
                    <el-checkbox
                      v-for="grade in availableGrades"
                      :key="grade"
                      :label="grade"
                      border
                      size="small"
                    >
                      {{ grade }}级
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
                <div class="header-stats">
                  <el-tag type="success" effect="plain" round class="pill">
                    已分配 <strong>{{ majorAllocationStatus.filter(m => m.allocated).length }}</strong>/<span class="total">{{ majorAllocationStatus.length }}</span> 专业
                  </el-tag>
                  <el-tag type="info" effect="plain" round class="pill">
                    总人数 <strong>{{ majorAllocationStatus.reduce((sum, m) => sum + m.capacity, 0) }}</strong> 人
                  </el-tag>
                  <el-tag type="warning" effect="plain" round class="pill">
                    班级数 <strong>{{ getTotalClassCount() }}</strong> 个
                  </el-tag>
                </div>
              </div>
            </div>
          </template>
          
          <div class="major-allocation-status">
            <el-table :data="majorAllocationStatus" stripe class="enhanced-table" size="default">
              <el-table-column prop="majorName" label="专业名称" min-width="180" show-overflow-tooltip>
                <template #default="scope">
                  <div class="major-name">{{ scope.row.majorName }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="buildingName" label="分配楼栋" min-width="150" align="center" />
              <el-table-column prop="roomCount" label="分配宿舍数" min-width="150" align="center">
                <template #default="scope">
                  <span class="number-value">{{ scope.row.roomCount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="classCount" label="班级数" min-width="130" align="center">
                <template #default="scope">
                  <span class="number-value class-number">{{ Math.ceil(scope.row.capacity / 30) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="capacity" label="总人数" min-width="130" align="center">
                <template #default="scope">
                  <span class="number-value capacity-number">{{ scope.row.capacity }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" min-width="130" align="center">
                <template #default="scope">
                  <el-tag 
                    :type="scope.row.allocated ? 'success' : 'danger'"
                    class="status-tag"
                    size="small"
                  >
                    {{ scope.row.allocated ? '已分配' : '未分配' }}
                  </el-tag>
                </template>
              </el-table-column>

            </el-table>
          </div>
        </el-card>

        <!-- 开始下一阶段 -->
        <div style="margin-top: 24px; text-align: center;">
          <el-button 
            type="primary" 
            size="large"
            :disabled="!canStartGroupingPhase"
            @click="startGroupingPhase"
            class="primary-action-btn"
          >
            开始学生自组阶段
          </el-button>
          <p v-if="!canStartGroupingPhase" class="hint-text" style="margin-top: 12px; color: #909399;">
            💡 请先完成所有专业的宿舍分配才能进入下一阶段
          </p>
        </div>
      </div>

      <!-- 阶段2: 学生自组宿舍 -->
      <div v-if="currentPhase === 'self_grouping'" class="phase-content">
        <div class="grouping-header">
          <el-card shadow="never">
            <template #header>
              <div class="header-with-actions">
                <h3>👥 学生自组宿舍管理</h3>
          </div>
      </template>

            <!-- 自组进度概览 -->
            <div class="grouping-overview">
        <el-row :gutter="20">
          <el-col :span="6">
                  <el-statistic title="已组满宿舍" :value="completedGroups" suffix="个">
            </el-statistic>
          </el-col>
          <el-col :span="6">
                  <el-statistic title="未满宿舍" :value="incompleteGroups" suffix="个">
            </el-statistic>
          </el-col>
          <el-col :span="6">
                  <el-statistic title="单独学生" :value="singleStudents" suffix="人">
            </el-statistic>
          </el-col>
          <el-col :span="6">
                  <el-statistic title="整体进度" :value="overallProgress" suffix="%">
            </el-statistic>
          </el-col>
        </el-row>
      </div>

            <!-- 宿舍组管理 -->
            <div class="dormitory-groups-management">
              <div class="management-header">
                <h4>宿舍组列表</h4>
                <div class="header-actions">
                  <el-button @click="refreshGroupData" size="small">
                    <el-icon><Refresh /></el-icon> 刷新数据
                  </el-button>
                </div>
              </div>
              
              <el-table :data="paginatedGroups" style="width: 100%" size="small">
                <el-table-column prop="groupName" label="宿舍组名称" min-width="120">
                  <template #default="{ row }">
                    <el-tag :type="(row.status === 'complete' || row.status === 'full' || row.status === 'room_selected') ? 'success' : 'warning'">
                      {{ row.groupName }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="leaderName" label="组长" width="100">
                  <template #default="{ row }">
                    {{ row.leaderName || '—' }}
                  </template>
                </el-table-column>
                <el-table-column prop="memberCount" label="人数" width="80">
                  <template #default="{ row }">
                    <span :style="{ color: (row.memberCount || 0) >= (row.maxMembers || 4) ? '#67C23A' : '#E6A23C' }">
                      {{ row.memberCount || 0 }}/{{ row.maxMembers || 4 }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="major" label="专业" width="120" />
                <el-table-column prop="class" label="班级" width="100">
                  <template #default="{ row }">
                    {{ row.class || row.className || '—' }}
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="(row.status === 'complete' || row.status === 'full' || row.status === 'room_selected') ? 'success' : 'warning'" size="small">
                      {{ (row.status === 'complete' || row.status === 'full' || row.status === 'room_selected') ? '已满员' : '未满员' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right" align="center">
                  <template #default="{ row }">
                    <div class="action-buttons">
                      <el-button 
                        @click="viewGroupMembers(row)" 
                        type="primary"
                        size="small"
                      >
                        查看详情
                      </el-button>
                      <el-button 
                        @click="dissolveGroup(row)" 
                        type="danger" 
                        size="small"
                      >
                        解散
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 分页 -->
              <div class="pagination-wrapper">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50]"
                  :small="true"
                  :total="dormGroups.length"
                  layout="total, sizes, prev, pager, next, jumper"
                />
              </div>
            </div>
          </el-card>
        </div>

        <div class="phase-actions">
          <el-button type="primary" @click="startRoomSelectionPhase" class="primary-action-btn">
            开始房间选择阶段
          </el-button>
        </div>
      </div>

      <!-- 阶段3: 房间选择 -->
      <div v-if="currentPhase === 'room_selection'" class="phase-content">
        <el-card shadow="never">
            <template #header>
              <div class="header-with-actions">
                <h3><el-icon><House /></el-icon> 房间选择管理</h3>
              <div class="selection-controls">
              <el-button 
                  v-if="!roomSelectionOpen" 
                  type="success" 
                  @click="openRoomSelection"
                >
                  开放房间选择
                </el-button>
                <el-button 
                  v-else 
                  type="danger" 
                  @click="closeRoomSelection"
                >
                  关闭房间选择
                </el-button>
                <el-button @click="setSelectionDeadline">
                  {{ selectionDeadline ? '修改截止时间' : '设置截止时间' }}
                </el-button>
              </div>
              
              <!-- 截止时间显示 -->
              <div v-if="selectionDeadline" class="deadline-info">
                <el-alert
                  :title="`截止时间：${selectionDeadline}`"
                  type="info"
                  :closable="false"
                  show-icon
                >
                  <template #default>
                    <span>到达截止时间后，系统将自动关闭房间选择功能</span>
                    <el-button 
                      size="small" 
                      type="text" 
                      @click="clearDeadline"
                style="margin-left: 10px;"
              >
                      清除截止时间
              </el-button>
                  </template>
                </el-alert>
            </div>
            </div>
          </template>

          <!-- 选择状态概览 -->
          <div class="selection-overview">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="selection-stat">
                  <div class="stat-icon">
                    <el-icon><Clock /></el-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-number">{{ roomSelectionOpen ? '已开放' : '已关闭' }}</div>
                    <div class="stat-label">选择状态</div>
                  </div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="selection-stat">
                  <div class="stat-icon">
                    <el-icon><Warning /></el-icon>
                  </div>
                  <div class="stat-content">
                    <div class="stat-number">{{ totalCompletedGroups - selectedRoomsCount }}</div>
                    <div class="stat-label">待选择宿舍组</div>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 选择进度提示 -->
          <div v-if="hasUnselectedGroups && roomSelectionOpen" class="selection-reminder">
            <el-alert
              :title="`还有 ${totalCompletedGroups - selectedRoomsCount} 个宿舍组尚未选择房间`"
              type="warning"
              :description="`建议：可以设置截止时间或发送提醒通知催促学生选择。如果长时间无响应，可以考虑强制结束选择。`"
              show-icon
              :closable="false"
            />
          </div>
    </el-card>

        <div class="phase-actions">
          <el-button @click="goBackToGroupingPhase" type="info" plain>
            <el-icon><ArrowLeft /></el-icon> 返回学生自组阶段
          </el-button>
          <el-button type="primary" @click="startProcessingPhase" :disabled="roomSelectionOpen" class="primary-action-btn">
            开始剩余处理阶段
          </el-button>
          <p v-if="roomSelectionOpen" class="hint-text">
            💡 请先关闭房间选择才能进入下一阶段
          </p>
        </div>
      </div>  <!-- 关闭 v-if="currentPhase === 'room_selection'" -->

      <!-- 阶段4: 剩余处理 -->
      <div v-if="currentPhase === 'admin_processing'" class="phase-content">
        <!-- 顶部概览统计 -->
        <div class="processing-header">
          <el-card shadow="never" class="stats-overview">
            <div class="stats-grid stats-grid-three">
              <div class="stat-card unassigned">
                <div class="stat-icon">
                  <el-icon><User /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ remainingStudentsList.length }}</div>
                  <div class="stat-label">待分配学生</div>
                </div>
              </div>
              
              <div class="stat-card incomplete">
                <div class="stat-icon">
                  <el-icon><Warning /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ incompleteGroupsList.length }}</div>
                  <div class="stat-label">未满宿舍组</div>
                </div>
              </div>
              
              <div class="stat-card available">
                <div class="stat-icon">
                  <el-icon><House /></el-icon>
                </div>
                <div class="stat-content">
                  <div class="stat-number">{{ availableBeds }}</div>
                  <div class="stat-label">总可用床位</div>
                  <div class="stat-detail" style="font-size: 12px; color: #909399; margin-top: 4px;">
                    未满组: {{ incompleteGroupsList.length }} | 空房间: {{ vacantRoomBeds }}
                  </div>
                </div>
              </div>
                </div>
              </el-card>
        </div>

        <!-- 主要工作区域 - 双栏布局 -->
        <div class="processing-workspace">
          <el-row :gutter="24">
            <!-- 左侧：待分配学生区域 -->
            <el-col :span="10">
              <el-card shadow="never" class="students-panel">
                <template #header>
                  <div class="panel-header">
                    <div class="header-title">
                      <el-icon><User /></el-icon>
                      <span>待分配学生</span>
                      <el-tag type="info" size="small" style="margin-left: 8px;">
                        {{ filteredStudents.length }} 人
                      </el-tag>
                    </div>
                    <div class="header-controls">
                      <el-input
                        v-model="studentSearchKeyword"
                        placeholder="搜索学生姓名或学号"
                        size="small"
                        style="width: 200px;"
                        clearable
                      >
                        <template #prefix>
                          <el-icon><Search /></el-icon>
                </template>
                      </el-input>
                      <el-select
                        v-model="studentFilterMajor"
                        placeholder="筛选专业"
                        size="small"
                        style="width: 120px;"
                        clearable
                      >
                        <el-option label="全部专业" value="" />
                        <el-option 
                          v-for="major in availableMajors" 
                          :key="major" 
                          :label="major" 
                          :value="major" 
                        />
                      </el-select>
                    </div>
                  </div>
                </template>
                
                <div class="students-container">
                  <div class="students-list">
                    <div 
                      v-for="student in filteredStudents" 
                      :key="student.id"
                      class="student-card"
                      :class="{ 'selected': selectedStudents.includes(student.id) }"
                      @click="toggleStudentSelection(student.id)"
                    >
                      <div class="student-avatar">
                        <el-avatar :size="32" :style="{ backgroundColor: getAvatarColor(student.gender) }">
                          {{ student.name.charAt(0) }}
                        </el-avatar>
                      </div>
                      
                      <div class="student-info">
                        <div class="student-main-info">
                          <span class="student-name">{{ student.name }}</span>
                          <el-tag :type="student.gender === '男' ? 'primary' : 'danger'" size="small" class="gender-tag">
                            {{ student.gender }}
                          </el-tag>
                          <span class="student-id">{{ student.studentId }}</span>
                          <el-tag v-if="student.isRegistered === false" type="warning" size="small" style="margin-left: 4px;">
                            未注册
                          </el-tag>
                        </div>
                        <div class="student-class">{{ student.major }} - {{ student.className }}</div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 批量操作栏 -->
                  <div class="batch-operations" v-if="selectedStudents.length > 0">
                    <div class="batch-info">
                      已选择 <strong>{{ selectedStudents.length }}</strong> 名学生
                    </div>
                    <div class="batch-actions">
                      <el-button size="small" type="primary" @click="batchAutoAssign">批量分配</el-button>
                      <el-button size="small" type="info" @click="clearSelection">清除选择</el-button>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
            
            <!-- 右侧：宿舍分配面板 -->
            <el-col :span="14">
              <el-card shadow="never" class="dormitory-panel">
                <template #header>
                  <div class="panel-header">
                    <div class="header-title">
                      <el-icon><House /></el-icon>
                      <span>宿舍分配面板</span>
                    </div>
                    <div class="header-controls">
                      <el-radio-group v-model="dormitoryViewMode" size="small">
                        <el-radio-button label="incomplete">未满宿舍组</el-radio-button>
                        <el-radio-button label="available">可用房间</el-radio-button>
                      </el-radio-group>
                      
                      <!-- 小范围筛选下拉菜单 -->
                      <div class="scope-filter">
                        <!-- 未满宿舍组时显示专业筛选 -->
                        <el-select 
                          v-if="dormitoryViewMode === 'incomplete'"
                          v-model="dormitoryFilterMajor"
                          placeholder="筛选专业"
                          size="small"
                          clearable
                          style="width: 140px;"
                          @change="filterDormitories"
                        >
                          <el-option label="全部专业" value="" />
                          <el-option 
                            v-for="major in availableMajors" 
                            :key="major" 
                            :label="major" 
                            :value="major" 
                          />
                        </el-select>
                        
                        <!-- 可用房间时显示楼层筛选 -->
                        <el-select 
                          v-if="dormitoryViewMode === 'available'"
                          v-model="roomFilterFloor"
                          placeholder="筛选楼层"
                          size="small"
                          clearable
                          style="width: 140px;"
                          @change="filterRooms"
                        >
                          <el-option label="全部楼层" value="" />
                          <el-option label="1楼" value="1" />
                          <el-option label="2楼" value="2" />
                          <el-option label="3楼" value="3" />
                          <el-option label="4楼" value="4" />
                          <el-option label="5楼" value="5" />
                          <el-option label="6楼" value="6" />
                        </el-select>
                    </div>
                  </div>
                </div>
                </template>
                
                <!-- 未满宿舍组视图 -->
                <div v-if="dormitoryViewMode === 'incomplete'" class="incomplete-groups-view">
                  <div class="groups-grid">
                    <div 
                      v-for="group in filteredIncompleteGroups" 
                      :key="group.id"
                      class="group-card"
                    >
                      <div class="group-header">
                        <div class="group-title-section">
                        <div class="group-name">{{ group.groupName }}</div>
                          <div class="group-class">{{ group.className }}</div>
                          <div class="group-leader">组长：{{ group.leaderName || '未设置' }}</div>
                          <div class="room-type">{{ getRoomTypeByCapacity(group.maxMembers) }}</div>
                        </div>
                        <div class="group-capacity">
                          <el-tag type="warning" size="small">
                            {{ group.memberCount || 0 }}/{{ group.maxMembers || 4 }}
                          </el-tag>
                        </div>
                      </div>
                      
                      <div class="group-content">
                        <div class="group-left">
                      <div class="group-members">
                            <div class="members-list" :class="`capacity-${group.maxMembers}`">
                          <div 
                            v-for="member in group.members" 
                            :key="member.studentId"
                            class="member-item"
                          >
                                <el-avatar :size="20" :style="{ backgroundColor: getAvatarColor(member.gender) }">
                              {{ member.name.charAt(0) }}
                            </el-avatar>
                            <span class="member-name">{{ member.name }}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                        
                        <div class="group-right">
                          <!-- 空床位占位符 -->
                          <div class="empty-beds" :class="`capacity-${group.maxMembers}`">
                            <div 
                              v-for="i in Math.max(0, (group.maxMembers || 4) - (group.memberCount || 0))" 
                              :key="'empty-' + i"
                              class="empty-bed"
                            >
                              <el-icon><Plus /></el-icon>
                              <span>空床位</span>
                            </div>
                          </div>
                          
                          <div class="group-actions">
                            <el-button size="small" type="success" @click="viewGroupDetails(group)">
                              查看详情
                            </el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 可用房间视图 -->
                <div v-if="dormitoryViewMode === 'available'" class="available-rooms-view">
                  <!-- 房间筛选 -->
                  <div class="room-filters">
                    <el-select 
                      v-model="roomFilterClass" 
                      placeholder="选择班级" 
                      clearable
                      @change="filterRooms"
                      class="class-filter"
                    >
                      <el-option 
                        v-for="classInfo in availableClasses" 
                        :key="classInfo.value" 
                        :label="classInfo.label" 
                        :value="classInfo.value"
                      />
                    </el-select>
                  </div>
                  
                  <div class="rooms-compact-grid">
                    <div 
                      v-for="room in filteredAvailableRooms" 
                      :key="room.id"
                      class="room-compact-card"
                      :class="getRoomStatusClass(room)"
                      @click="assignToRoom(room)"
                    >
                      <div class="room-compact-header">
                        <span class="room-number">{{ room.roomNumber }}</span>
                        <span class="room-building">{{ room.buildingName }} {{ room.floor }}楼</span>
                      </div>
                      
                      <div class="room-compact-info">
                        <span class="room-type">{{ room.roomType }}</span>
                      </div>
                      
                      <div class="room-compact-status">
                        <el-tag 
                          :type="getCapacityTagType(room)" 
                          size="small"
                        >
                            {{ room.occupiedBeds }}/{{ room.capacity }}
                          </el-tag>
                      </div>
                      
                      <div class="room-hover-info">
                        <span>点击分配</span>
                      </div>
                    </div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 底部操作区域 -->
        <div class="processing-footer">
          <el-card shadow="never" class="actions-panel">
            <div class="footer-content">
              <div class="quick-actions">
                <el-button type="primary" @click="intelligentAutoAssign">
                  <el-icon><Star /></el-icon>
                  一键分配
                </el-button>
              </div>

              <div class="validation-actions">
                <el-button type="primary" @click="completeAllocation" class="primary-action-btn">
                  <el-icon><Check /></el-icon>
                  完成分配
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>  <!-- 关闭 v-if="currentPhase === 'admin_processing'" -->
              
      <!-- 阶段5: 分配完成 -->
      <div v-if="currentPhase === 'completed'" class="phase-content">
        <el-result
          icon="success"
          title="分配完成"
          sub-title="所有学生已完成宿舍分配"
        >
          <template #extra>
            <el-button type="warning" @click="startNewAllocationRound">开始新一轮分配</el-button>
          </template>
        </el-result>

      </div>
    </el-card>



    <!-- 手动分配对话框 -->
    <el-dialog v-model="manualAssignDialogVisible" title="手动分配工具" width="1400px" :close-on-click-modal="false">
      <div class="manual-assign-content">
        <el-row :gutter="20">
          <!-- 左侧：未分组学生 -->
          <el-col :span="8">
            <el-card shadow="never">
              <template #header>
                <div class="card-header">
                  <span><el-icon><User /></el-icon> 未分组学生 ({{ remainingStudentsList.length }}人)</span>
                  <el-button size="small" @click="refreshRemainingStudents">刷新</el-button>
                </div>
              </template>
              <div class="student-list">
                <div 
                  v-for="student in remainingStudentsList" 
                  :key="student.id"
                  class="student-item"
                >
                  <div class="student-info">
                    <div class="student-name">{{ student.name }}</div>
                    <div class="student-details">
                      <el-tag size="small" :type="student.gender === '男' ? 'primary' : 'success'">
                        {{ student.gender }}
                      </el-tag>
                      <span class="student-id">{{ student.studentId }}</span>
                    </div>
                    <div class="student-major">{{ student.major }} - {{ student.className }}</div>
                  </div>
                  <div class="student-actions">
                    <el-button size="small" type="primary" @click="createNewGroupWithStudent(student)">
                      新建宿舍组
                    </el-button>
                  </div>
                </div>
                <div v-if="remainingStudentsList.length === 0" class="empty-state">
                  <el-empty description="暂无未分组学生" :image-size="80" />
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 中间：未满员宿舍组 -->
          <el-col :span="8">
            <el-card shadow="never">
              <template #header>
                <div class="card-header">
                  <span><el-icon><Warning /></el-icon> 未满员宿舍组 ({{ incompleteGroupsList.length }}个)</span>
                  <el-button size="small" @click="loadRemainingData">刷新</el-button>
                </div>
              </template>
              <div class="group-list">
                <div 
                  v-for="group in incompleteGroupsList" 
                  :key="group.id"
                  class="group-item incomplete-group"
                >
                  <div class="group-header">
                    <div class="group-name">{{ group.groupName }}</div>
                    <div class="group-stats">
                      <el-tag type="warning" size="small">{{ group.memberCount }}/{{ group.maxMembers || 4 }}</el-tag>
                      <span class="needed">还需{{ group.needed }}人</span>
                      <span class="room-type-small">{{ getRoomTypeByCapacity(group.maxMembers || 4) }}</span>
                    </div>
                  </div>
                  <div class="group-info">
                    <div class="group-details">{{ group.className }}</div>
                    <div class="group-members">
                      <el-tag 
                        v-for="member in getGroupMembers(group.id)" 
                        :key="member.studentId"
                        size="small"
                        :type="member.role === 'leader' ? 'warning' : 'info'"
                        class="member-tag"
                      >
                        {{ member.name }}
                      </el-tag>
                    </div>
                  </div>
                </div>
                <div v-if="incompleteGroupsList.length === 0" class="empty-state">
                  <el-empty description="暂无未满员宿舍组" :image-size="80" />
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- 右侧：操作面板 -->
          <el-col :span="8">
            <el-card shadow="never">
              <template #header>
                <span><el-icon><Tools /></el-icon> 分配工具</span>
              </template>
              <div class="tools-panel">
                <!-- 快速操作 -->
                <div class="tool-section">
                  <h4>快速操作</h4>
                  <el-button-group class="tool-buttons">

                    <el-button @click="createNewGroup" type="success">
                      <el-icon><Plus /></el-icon>
                      新建宿舍组
                    </el-button>
                  </el-button-group>
                </div>

                <!-- 分配统计 -->
                <div class="tool-section">
                  <h4>分配统计</h4>
                  <div class="stats-grid">
                    <div class="stat-item">
                      <div class="stat-number">{{ remainingStudentsList.length }}</div>
                      <div class="stat-label">未分组学生</div>
                    </div>
                    <div class="stat-item">
                      <div class="stat-number">{{ incompleteGroupsList.length }}</div>
                      <div class="stat-label">未满员宿舍组</div>
                    </div>
                    <div class="stat-item">
                      <div class="stat-number">{{ completedGroups }}</div>
                      <div class="stat-label">已满员宿舍组</div>
                    </div>
                  </div>
                </div>

                <!-- 操作历史 -->
                <div class="tool-section">
                  <h4>操作历史</h4>
                  <div class="operation-history">
                    <div 
                      v-for="(operation, index) in operationHistory" 
                      :key="index"
                      class="history-item"
                    >
                      <div class="history-time">{{ formatTime(operation.time) }}</div>
                      <div class="history-action">{{ operation.action }}</div>
                    </div>
                    <div v-if="operationHistory.length === 0" class="empty-history">
                      暂无操作记录
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="manualAssignDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="saveManualAssignments">保存分配结果</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 宿舍组成员对话框 -->
    <el-dialog v-model="groupMembersDialogVisible" :title="`${currentGroup?.groupName} - 成员列表`" width="600px">
      <el-table :data="currentGroup?.members" style="width: 100%" size="small">
        <el-table-column prop="studentId" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.gender === '男' ? 'primary' : 'success'" size="small">
              {{ row.gender }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" width="120" />
        <el-table-column prop="class" label="班级" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 'leader' ? 'warning' : 'info'" size="small">
              {{ row.role === 'leader' ? '组长' : '成员' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="groupMembersDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportGroupMembers">导出成员信息</el-button>
      </template>
    </el-dialog>

    <!-- 宿舍组详情对话框 -->
    <el-dialog v-model="groupDetailsDialogVisible" title="宿舍组详情" width="800px" class="group-details-dialog">
      <div class="group-details-content" v-if="selectedGroupDetails">
        <!-- 基本信息卡片 -->
        <el-card class="details-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">基本信息</span>
              <el-tag :type="(selectedGroupDetails.memberCount || 0) >= (selectedGroupDetails.maxMembers || 4) ? 'success' : 'warning'">
                {{ (selectedGroupDetails.memberCount || 0) >= (selectedGroupDetails.maxMembers || 4) ? '已满员' : '未满员' }}
              </el-tag>
            </div>
          </template>
          <div class="basic-info">
            <div class="info-row">
              <div class="info-item">
                <label>宿舍组名称：</label>
                <span>{{ selectedGroupDetails.groupName }}</span>
              </div>
              <div class="info-item">
                <label>房间类型：</label>
                <span>{{ getRoomTypeByCapacity(selectedGroupDetails.maxMembers || 4) }}</span>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item">
                <label>组长：</label>
                <span>{{ selectedGroupDetails.leaderName || '未设置' }}</span>
              </div>
              <div class="info-item">
                <label>当前人数：</label>
                <span>{{ selectedGroupDetails.memberCount || 0 }}/{{ selectedGroupDetails.maxMembers || 4 }}</span>
              </div>
            </div>
            <div class="info-row">
              <div class="info-item">
                <label>专业班级：</label>
                <span>{{ selectedGroupDetails.major }}-{{ selectedGroupDetails.class }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 成员信息卡片 -->
        <el-card class="details-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">成员信息</span>
              <el-button size="small" type="primary" @click="addMemberToGroup" v-if="selectedGroupDetails.memberCount < (selectedGroupDetails.maxMembers || 4)">
                <el-icon><Plus /></el-icon> 添加成员
              </el-button>
            </div>
          </template>
          <div class="members-info">
            <div class="members-grid">
              <div 
                v-for="member in selectedGroupDetails.members" 
                :key="member.studentId"
                class="member-card"
              >
                <div class="member-avatar">
                  <el-avatar :size="50" :src="member.avatar">
                    {{ member.name.charAt(0) }}
                  </el-avatar>
                  <el-tag v-if="isGroupLeader(member, selectedGroupDetails)" size="small" type="success" class="leader-tag">
                    组长
                  </el-tag>
                </div>
                <div class="member-info">
                  <div class="member-name">{{ member.name }}</div>
                  <div class="member-details">
                    <div class="detail-item">学号：{{ member.studentId }}</div>
                    <div class="detail-item" v-if="member.email">邮箱：{{ member.email }}</div>
                    <div class="detail-item">专业：{{ member.major }}</div>
                  </div>
                </div>
                <div class="member-actions">
                  <el-button 
                    v-if="!isGroupLeader(member, selectedGroupDetails)" 
                    size="small" 
                    @click="setAsLeader(member)"
                  >
                    设为组长
                  </el-button>
                  <el-button 
                    v-else 
                    size="small" 
                    disabled
                    style="height: 28px;"
                  >
                    已是组长
                  </el-button>
                  <el-button 
                    size="small" 
                    type="danger" 
                    @click="removeMemberFromGroup(member)"
                    :disabled="isGroupLeader(member, selectedGroupDetails) && selectedGroupDetails.members.length > 1"
                  >
                    移除
                  </el-button>
                </div>
              </div>
              
              <!-- 空床位占位符 -->
              <div 
                v-for="i in Math.max(0, (selectedGroupDetails.maxMembers || 4) - (selectedGroupDetails.memberCount || 0))" 
                :key="'empty-' + i"
                class="empty-member-slot"
              >
                <div class="empty-avatar">
                  <el-icon size="30"><Plus /></el-icon>
                </div>
                <div class="empty-text">空床位</div>
              </div>
            </div>
          </div>
        </el-card>


      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="groupDetailsDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="editGroup(selectedGroupDetails)">
            <el-icon><Edit /></el-icon> 编辑宿舍组
          </el-button>
          <el-button type="danger" @click="confirmDeleteGroup(selectedGroupDetails)" v-if="selectedGroupDetails.memberCount === 0">
            <el-icon><Delete /></el-icon> 删除宿舍组
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 宿舍组编辑对话框 -->
    <el-dialog v-model="editGroupDialogVisible" title="编辑宿舍组" width="500px">
      <el-form :model="editGroupForm" label-width="100px">
        <el-form-item label="宿舍组名称">
          <el-input v-model="editGroupForm.groupName" placeholder="请输入宿舍组名称" />
        </el-form-item>
        <el-form-item label="组长">
          <el-select v-model="editGroupForm.leaderId" placeholder="选择组长">
            <el-option
              v-for="member in editGroupForm.members"
              :key="member.studentId"
              :label="member.name"
              :value="member.studentId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroupEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学生选择对话框 -->
    <el-dialog 
      v-model="studentSelectionDialogVisible" 
      :title="studentSelectionDialogTitle" 
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="student-selection-content">
        <!-- 搜索和筛选 -->
        <div class="selection-header">
          <div class="search-and-filters">
            <el-input
              v-model="studentSearchKeyword"
              placeholder="搜索学生姓名或学号"
              clearable
              class="search-input"
              @input="filterAvailableStudents"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            
            <div class="filter-options">
              <el-checkbox v-model="studentFilterOptions.showUnassigned" @change="filterAvailableStudents">
                未组建宿舍
              </el-checkbox>
              <el-checkbox v-model="studentFilterOptions.showIncomplete" @change="filterAvailableStudents">
                宿舍组未满
              </el-checkbox>
            </div>
          </div>
        </div>

        <!-- 学生列表 -->
        <div class="students-list">
          <div class="list-header">
            <span>可添加学生 ({{ filteredStudents.length }}人)</span>
            <el-button 
              size="small" 
              type="primary" 
              :disabled="selectedStudents.length === 0"
              @click="confirmAddStudents"
            >
              添加选中学生 ({{ selectedStudents.length }})
            </el-button>
          </div>
          
          <div class="students-grid" v-if="filteredStudents.length > 0">
            <div 
              v-for="student in filteredStudents" 
              :key="student.id"
              class="student-item"
              :class="{ 'selected': selectedStudents.includes(student.id) }"
              @click="toggleStudentSelection(student.id)"
            >
              <el-checkbox 
                :model-value="selectedStudents.includes(student.id)"
                @change="toggleStudentSelection(student.id)"
                @click.stop
              />
              <div class="student-avatar">
                <el-avatar :size="40" :src="student.avatar">
                  {{ student.name.charAt(0) }}
                </el-avatar>
              </div>
              <div class="student-info">
                <div class="student-name">
                  {{ student.name }}
                  <el-tag v-if="student.isRegistered === false" type="warning" size="small" style="margin-left: 4px;">
                    未注册
                  </el-tag>
                </div>
                <div class="student-id">{{ student.studentId }}</div>
                <div class="student-class">{{ student.major }} - {{ student.className }}</div>
                <div class="student-status">
                  <el-tag 
                    v-if="student.groupStatus === 'unassigned'" 
                    type="warning" 
                    size="small"
                    effect="plain"
                  >
                    未组建宿舍
                  </el-tag>
                  <el-tag 
                    v-else-if="student.groupStatus === 'incomplete'" 
                    type="info" 
                    size="small"
                    effect="plain"
                  >
                    宿舍未满 ({{ student.currentGroupSize }}/{{ student.maxGroupSize }})
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
          
          <el-empty v-else description="没有找到符合条件的学生" />
        </div>
      </div>
    </el-dialog>

    <!-- 阶段切换面板 - 快速切换阶段 -->
    <div class="dev-debug-panel" v-if="isDevelopment">
      <div class="debug-panel-header">
        <span>🔄 阶段切换</span>
        <el-button 
          size="small" 
          text 
          @click="toggleDebugPanel"
          :icon="debugPanelExpanded ? 'ArrowDown' : 'ArrowUp'"
        />
      </div>
      <div v-show="debugPanelExpanded" class="debug-panel-content">
        <div class="phase-buttons">
          <el-button 
            size="small" 
            :type="currentPhase === 'preparation' ? 'primary' : 'default'"
            @click="quickSwitchPhase('preparation')"
          >
            阶段1
          </el-button>
          <el-button 
            size="small" 
            :type="currentPhase === 'self_grouping' ? 'primary' : 'default'"
            @click="quickSwitchPhase('self_grouping')"
          >
            阶段2
          </el-button>
          <el-button 
            size="small" 
            :type="currentPhase === 'room_selection' ? 'primary' : 'default'"
            @click="quickSwitchPhase('room_selection')"
          >
            阶段3
          </el-button>
          <el-button 
            size="small" 
            :type="currentPhase === 'admin_processing' ? 'primary' : 'default'"
            @click="quickSwitchPhase('admin_processing')"
          >
            阶段4
          </el-button>
          <el-button 
            size="small" 
            :type="currentPhase === 'completed' ? 'primary' : 'default'"
            @click="quickSwitchPhase('completed')"
          >
            阶段5
          </el-button>
        </div>
        <div class="current-phase-info">
          当前：{{ currentPhaseText }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getCurrentUser } from '@/utils/auth.js'
import { getAdminBuildings } from '@/utils/buildingManagement.js'
import { getBuildingAllocationScope } from '@/utils/allocationScope.js'
import { GroupStatus } from '@/utils/studentGrouping.js'
import { 
  getDormGroupList,
  getDormGroupById,
  createDormGroup as createDormGroupAPI,
  updateDormGroup as updateDormGroupAPI,
  deleteDormGroup as deleteDormGroupAPI,
  selectRoom as selectRoomAPI,
  getDormGroupStatistics,
  cancelRoomSelection as cancelRoomSelectionAPI
} from '@/api/dormGroup.js'
import { getStudentList } from '@/api/student.js'
import { getAllocationStatistics } from '@/api/allocationStats.js'
import { getAllBuildings } from '@/api/building.js'
import { getMyBuildings } from '@/api/adminDormManagement.js'
import {
  getRoomSelectionConfig,
  openRoomSelection as openRoomSelectionAPI,
  closeRoomSelection as closeRoomSelectionAPI,
  updateDeadline,
  getRoomSelectionStatistics,
  switchAllocationPhase,
  switchBuildingPhase
} from '@/api/roomSelection.js'
import {
  getRoomsByBuilding
} from '@/api/room.js'
import {
  autoAssignRooms
} from '@/utils/roomSelection.js'
import {
  manualAssignStudent,
  createNewGroupForStudent,
  validateAllocationResults
} from '@/utils/adminProcessing.js'
import {
  intelligentAssign,
  assignToGroup,
  assignToRoom
} from '@/api/allocation.js'
import {
  AllocationPhase,
  getCurrentPhase,
  setCurrentPhase,
  getGroupingTimeSettings,
  saveGroupingTimeSettings,

  initializeAllocationData,
  exportAllAllocationData
} from '@/utils/allocationDataManager.js'
import { 
  Document, 
  Clock, 
  SuccessFilled, 
  WarningFilled, 
  User, 
  TrendCharts,
  ArrowDown,
  House, 
  DataBoard,
  Warning,
  ArrowLeft,
  Refresh,
  Search,
  Plus,
  Tools,
  Star,
  Download,
  Check,
  MagicStick  // 一键智能分配图标
} from '@element-plus/icons-vue'

export default {
  name: 'StudentAllocation',
  components: {
    Document,
    Clock,
    SuccessFilled,
    WarningFilled,
    User,
    TrendCharts,
    ArrowDown,
    House,
    DataBoard,
    Warning,
    ArrowLeft,
    Refresh,
    Search,
    Plus,
    Tools,
    Star,
    Download,
    Check,
    MagicStick
  },
  data() {
    return {
      // 开发调试（截图时设为false隐藏调试面板）
      isDevelopment: true,
      debugPanelExpanded: true,
      
      // 当前阶段
      currentPhase: AllocationPhase.PREPARATION,
      
      // 管理员数据
      managedBuildings: [],
      currentUser: null,
      
      // 楼栋选择数据（自动设置为管理员管理的所有楼栋）
      selectedBuildings: [], // 自动包含管理员管理的所有楼栋ID
      
      // 批次信息
      batchDescription: '', // 批次描述
      batchForm: {}, // 批次表单数据
      
      // 分配统计（用于检测分配模式）
      allocationStats: {
        autoAssignedCount: 0, // 系统自动分配数量
        manualAssignedCount: 0, // 管理员手动分配数量
        selfGroupedCount: 0 // 学生自组数量
      },
      
      // 后端统计数据
      backendStats: {
        waitingStudentsCount: 0,    // 待分配学生
        incompleteGroupsCount: 0,   // 未满宿舍组
        incompleteGroupBeds: 0,     // 未满组空床位
        completeGroupsCount: 0,     // 已满员宿舍组
        vacantRoomsCount: 0,        // 空置房间数
        vacantRoomBeds: 0,          // 空置房间床位
        totalAvailableBeds: 0       // 总可用床位
      },
      
      // 专业分配数据
      majorAllocationStatus: [],
      
      // 🔥 新增：年级筛选
      availableGrades: [],  // 可用的年级列表（动态提取）
      selectedGrades: [],   // 选中的年级
      
      // 学生自组数据
      dormGroups: [], // 自组宿舍列表
      groupingTimeSettings: {
        startTime: '',
        endTime: ''
      },
      groupingDeadlineTimer: null, // 自组截止时间定时器
      autoRefreshTimer: null, // 自动刷新定时器
      autoRefreshInterval: 30000, // 自动刷新间隔（30秒）
      
      // 宿舍组管理相关
      currentPage: 1,
      pageSize: 10,
      groupMembersDialogVisible: false,
      groupDetailsDialogVisible: false,
      selectedGroupDetails: null,
      
      // 学生选择对话框
      studentSelectionDialogVisible: false,
      studentSelectionContext: null, // 'group' or 'room'
      availableStudents: [],
      selectedStudents: [],
      studentSearchKeyword: '',
      studentFilterOptions: {
        showUnassigned: true,
        showIncomplete: true
      },
      editGroupDialogVisible: false,
      currentGroup: null,
      editGroupForm: {
        groupName: '',
        leaderId: '',
        members: []
      },
      
      // 房间选择数据
      roomSelectionOpen: false,
      selectedRoomForAssignment: null,
      selectionDeadline: null,
      deadlineTimer: null,
      
      // 剩余处理数据
      incompleteGroupsList: [],
      remainingStudentsList: [],
      remainingStudentsPage: 1,
      remainingStudentsPageSize: 5,
      
      // 新UI数据
      studentSearchKeyword: '',
      studentFilterMajor: '',
      selectedStudents: [],
      dormitoryViewMode: 'incomplete', // 'incomplete' | 'available'
      dormitoryFilterBuilding: '',
      dormitoryFilterMajor: '', // 宿舍面板专业筛选
      filteredIncompleteGroups: [],
      filteredAvailableRooms: [],
      availableRoomsList: [], // 可用房间原始列表
      
      // 动态专业和班级列表
      availableMajors: [], // 从学生数据中提取的专业列表
      availableClasses: [], // 从学生数据中提取的班级列表
      
      // 房间筛选数据
      roomFilterClass: '',
      roomFilterFloor: '',
      
      // 对话框状态
      timeSettingDialogVisible: false,
      manualAssignDialogVisible: false,
      
      // 手动分配工具数据
      draggedStudent: null,
      operationHistory: [],
      
      // 表单数据
      timeForm: {
        startTime: '',
        endTime: ''
      },
      
      // 最终统计
      finalStats: {
        totalStudents: 0,
        totalRooms: 0,
        avgOccupancy: 0,
        selfGroupingRate: 0
      }
    }
  },
  computed: {
    // 当前阶段索引（用于步骤条）
    currentPhaseIndex() {
      const phases = Object.values(AllocationPhase)
      return phases.indexOf(this.currentPhase)
    },

    // 筛选后的学生列表
    filteredStudents() {
      if (!this.remainingStudentsList) return []
      
      let filtered = [...this.remainingStudentsList]
      
      // 搜索关键词筛选
      if (this.studentSearchKeyword.trim()) {
        const keyword = this.studentSearchKeyword.trim().toLowerCase()
        filtered = filtered.filter(student => 
          student.name.toLowerCase().includes(keyword) ||
          student.studentId.toLowerCase().includes(keyword)
        )
      }
      
      // 专业筛选
      if (this.studentFilterMajor && this.studentFilterMajor.trim()) {
        filtered = filtered.filter(student => student.major === this.studentFilterMajor)
      }
      
      // 根据宿舍组状态筛选
      const { showUnassigned, showIncomplete } = this.studentFilterOptions
      
      // 如果两个都没选中，不显示任何学生
      if (!showUnassigned && !showIncomplete) {
        return []
      }
      
      // 如果两个都选中，显示所有学生
      if (showUnassigned && showIncomplete) {
        return filtered
      }
      
      // 只选中了其中一个
      filtered = filtered.filter(student => {
        if (showUnassigned && student.groupStatus === 'unassigned') {
          return true
        }
        if (showIncomplete && student.groupStatus === 'incomplete') {
          return true
        }
        return false
      })
      
      return filtered
    },

    // 学生选择对话框标题
    studentSelectionDialogTitle() {
      if (this.studentSelectionContext === 'room' && this.selectedRoomForAssignment) {
        return `分配学生到房间 ${this.selectedRoomForAssignment.roomNumber}`
      } else if (this.studentSelectionContext === 'group') {
        return '添加成员'
      }
      return '选择学生'
    },
    
    // 当前阶段文本
    currentPhaseText() {
      const texts = {
        [AllocationPhase.PREPARATION]: '专业分配确认',
        [AllocationPhase.SELF_GROUPING]: '学生自组阶段',
        [AllocationPhase.ROOM_SELECTION]: '房间选择阶段',
        [AllocationPhase.ADMIN_PROCESSING]: '剩余处理阶段',
        [AllocationPhase.COMPLETED]: '分配完成'
      }
      return texts[this.currentPhase]
    },
    
    // 阶段标签类型
    phaseTagType() {
      const types = {
        [AllocationPhase.PREPARATION]: 'info',
        [AllocationPhase.SELF_GROUPING]: 'warning',
        [AllocationPhase.ROOM_SELECTION]: 'primary',
        [AllocationPhase.ADMIN_PROCESSING]: 'danger',
        [AllocationPhase.COMPLETED]: 'success'
      }
      return types[this.currentPhase]
    },
    
    // 分配模式计算
    computedAllocationMode() {
      const { autoAssignedCount, manualAssignedCount, selfGroupedCount } = this.allocationStats
      const hasAuto = autoAssignedCount > 0
      const hasManual = manualAssignedCount > 0
      const hasSelf = selfGroupedCount > 0

      const modes = [hasAuto, hasManual, hasSelf].filter(Boolean).length

      if (modes > 1) {
        return '混合模式'
      } else if (hasAuto) {
        return '系统分配'
      } else if (hasManual) {
        return '手动分配'
      } else if (hasSelf) {
        return '学生自组'
      } else {
        return '未开始'
      }
    },

    // 是否所有专业都已分配
    allMajorsAllocated() {
      return this.majorAllocationStatus.every(major => major.allocated)
    },
    
    // 是否可以开始学生自组阶段
    canStartGroupingPhase() {
      // 移除对selectedBuildings的检查，直接使用管理的楼栋
      return this.managedBuildings.length > 0 && 
             this.allMajorsAllocated && 
             this.batchDescription.trim().length > 0
    },
    
    // 自组时间显示
    groupingTimeRange() {
      if (this.groupingTimeSettings.startTime && this.groupingTimeSettings.endTime) {
        return `${this.groupingTimeSettings.startTime} ~ ${this.groupingTimeSettings.endTime}`
      }
      return '未设置'
    },
    
    // 自组时间状态
    groupingTimeStatus() {
      if (!this.groupingTimeSettings.startTime || !this.groupingTimeSettings.endTime) {
        return null
      }
      
      const now = new Date()
      const start = new Date(this.groupingTimeSettings.startTime)
      const end = new Date(this.groupingTimeSettings.endTime)
      
      if (now < start) {
        return { type: 'info', text: '未开始' }
      } else if (now >= start && now <= end) {
        return { type: 'success', text: '进行中' }
      } else {
        return { type: 'danger', text: '已结束' }
      }
    },
    
    // 统计数据
    // 数据库状态值：forming-组建中，full-已满员，room_selected-已选房，allocated-已分配，disbanded-已解散
    completedGroups() {
      return this.dormGroups.filter(group => 
        group.status === 'complete' || group.status === 'full' || group.status === 'room_selected'
      ).length
    },
    
    incompleteGroups() {
      return this.dormGroups.filter(group => group.status === 'incomplete' || group.status === 'forming' || group.status === GroupStatus.INCOMPLETE).length
    },
    
    singleStudents() {
      // 计算未分组的学生数量（从剩余学生列表）
      return this.remainingStudentsList?.length || 0
    },
    
    overallProgress() {
      // 计算已分配学生数（所有宿舍组中的学生）
      const assignedStudents = this.dormGroups.reduce((total, group) => {
        return total + (group.memberCount || group.currentMembers || 0)
      }, 0)
      // 总学生数 = 已分配学生 + 待分配学生
      const totalStudents = assignedStudents + (this.remainingStudentsList?.length || 0)
      return totalStudents > 0 ? Math.round(assignedStudents / totalStudents * 100) : 0
    },
    
    // 房间选择统计
    selectedRoomsCount() {
      return this.dormGroups.filter(group => group.roomId).length
    },
    
    totalCompletedGroups() {
      // 只统计已满员但还未选房的宿舍组（不包括已选房的）
      return this.dormGroups.filter(group => 
        (group.status === 'complete' || group.status === 'full') && !group.roomId
      ).length + this.selectedRoomsCount
    },
    
    // 是否有未选择房间的宿舍组
    // 数据库状态值：full-已满员待选房，complete是前端兼容值
    hasUnselectedGroups() {
      return this.dormGroups.some(g => 
        (g.status === 'complete' || g.status === 'full') && !g.roomId
      )
    },

    // 分页后的宿舍组数据
    paginatedGroups() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.dormGroups.slice(start, start + this.pageSize)
    },


    // 分页后的剩余学生数据
    paginatedRemainingStudents() {
      const start = (this.remainingStudentsPage - 1) * this.remainingStudentsPageSize
      return this.remainingStudentsList.slice(start, start + this.remainingStudentsPageSize)
    },

    // 未满宿舍组的空床位数
    incompleteGroupBeds() {
      return this.incompleteGroupsList.reduce((total, group) => {
        return total + Math.max(0, (group.maxMembers || 4) - (group.memberCount || 0));
      }, 0);
    },
    
    // 空房间的床位数
    vacantRoomBeds() {
      return this.availableRoomsList.reduce((total, room) => {
        // 只统计完全空置的房间
        if (room.occupiedBeds === 0) {
          return total + (room.capacity || 4);
        }
        return total;
      }, 0);
    },
    
    // 总可用床位数
    availableBeds() {
      return this.incompleteGroupBeds + this.vacantRoomBeds;
    }
  },
  mounted() {
    this.loadData()
    // initializeAvailableClasses 会在 loadAvailableRooms 完成后自动调用
    // 启动自动刷新机制
    this.startAutoRefresh()
  },
  beforeUnmount() {
    // 清理定时器
    this.clearDeadlineTimer()
    this.stopAutoRefresh()
  },
  methods: {
    // 根据容量获取房间类型
    // 统一使用中文数字格式：二人间、四人间、六人间、八人间
    getRoomTypeByCapacity(capacity) {
      switch (capacity) {
        case 2:
          return '二人间'
        case 4:
          return '四人间'
        case 6:
          return '六人间'
        case 8:
          return '八人间'
        default:
          return '四人间'
      }
    },

    // 加载数据
    async loadData() {
      this.currentUser = getCurrentUser()
      await this.loadManagedBuildings()  // 等待楼栋数据加载完成
      await this.loadMajorAllocationStatus()  // 🔧 修复：等待专业分配状态加载完成
      this.loadCurrentPhase()
      // 页面加载完成
      this.loadGroupingTimeSettings()
      await this.loadSelectionDeadline()  // 等待房间选择配置加载完成
      await this.loadStudentGroups()
      
      // 🔧 修复：在学生自组阶段、房间选择阶段和剩余处理阶段都加载剩余学生数据
      if (this.currentPhase === AllocationPhase.SELF_GROUPING || this.currentPhase === AllocationPhase.ROOM_SELECTION) {
        // 加载剩余学生数据以显示统计
        await this.loadRemainingStudentsFromAPI()
      } else if (this.currentPhase === AllocationPhase.ADMIN_PROCESSING) {
        // 剩余处理阶段：加载完整的统计数据
        await Promise.all([
          this.loadBackendStatistics(),  // 加载后端统计
          this.loadRemainingData()        // 加载剩余学生数据
        ])
      }
      
      this.initializeDataIfNeeded()
    },
    
    // 初始化数据（如果需要）
    async initializeDataIfNeeded() {
      // 从后端加载数据，不再需要初始化示例数据
      await this.loadStudentGroups()
      if (this.dormGroups.length === 0) {
        // 暂无宿舍组数据
      }
    },
    
    // 加载管理的楼栋（从后端API加载真实数据）
    async loadManagedBuildings() {
      if (!this.currentUser || !this.currentUser.id) {
        console.warn('无法加载楼栋：当前用户信息不完整')
        return
      }
      
      try {
        // 加载管理员负责的楼栋
        
        // 1. 从后端加载管理员管理的楼栋（带统计信息）
        const buildingsResponse = await getMyBuildings()
        
        if (buildingsResponse.code === 200 && buildingsResponse.data) {
          // 管理员楼栋数据加载成功
          console.log('🏢 [loadManagedBuildings] 后端返回的楼栋数据:', buildingsResponse.data)
          
          // 2. 映射楼栋数据
          this.managedBuildings = buildingsResponse.data.map(building => {
            console.log('🏢 处理楼栋:', building)
            return {
              id: building.id,
              name: building.buildingName || building.name || `楼栋${building.id}`,  // 兼容多种字段名
              gender: building.buildingType || '男',
              buildingType: building.buildingType,
              totalRooms: building.totalRooms || 0,
              totalBeds: building.totalBeds || (building.totalRooms * 4)
            }
          })
          
          // 管理的楼栋加载成功
          // 自动将所有管理的楼栋设置为选中状态
          this.selectedBuildings = this.managedBuildings.map(b => b.id)
        } else {
          console.warn('未找到楼栋分配数据')
          this.$message.warning('未找到楼栋分配数据，请联系系统管理员配置')
          this.managedBuildings = []
          this.selectedBuildings = []
        }
      } catch (error) {
        console.error('加载管理的楼栋失败:', error)
        this.$message.error('加载楼栋信息失败：' + (error.message || '网络错误，请刷新页面重试'))
        this.managedBuildings = []
        this.selectedBuildings = []
      }
    },
    
    // 获取分配范围文本（用于显示）
    getAllocationScopeText() {
      if (this.managedBuildings.length === 0) {
        return '未设置'
      }
      const buildingNames = this.managedBuildings.map(b => b.name)
      return buildingNames.join('、')
    },
    
    // 加载专业分配状态
    async loadMajorAllocationStatus() {
      this.majorAllocationStatus = []
      
      console.log('📊 [loadMajorAllocationStatus] 开始加载专业分配状态')
      console.log('📊 managedBuildings:', this.managedBuildings)
      
      // 🔧 修复：从后端获取学生列表，用于计算每个班级的真实学生人数
      let studentCountByClass = {}
      try {
        const res = await getStudentList({ currentPage: 1, pageSize: 10000 })
        if (res.code === 200 && res.data?.students) {
          // 按班级统计学生人数
          res.data.students.forEach(student => {
            const className = student.className
            if (className) {
              studentCountByClass[className] = (studentCountByClass[className] || 0) + 1
            }
          })
          console.log('📊 班级学生人数统计:', studentCountByClass)
        }
      } catch (error) {
        console.error('获取学生列表失败:', error)
      }
      
      this.managedBuildings.forEach(building => {
        console.log(`📊 处理楼栋: ${building.name} (ID: ${building.id})`)
        const scope = getBuildingAllocationScope(building.id)
        console.log(`📊 楼栋 ${building.name} 的分配范围:`, scope)
        
        if (!scope.professionalChips || scope.professionalChips.length === 0) {
          console.warn(`⚠️ 楼栋 ${building.name} 没有专业分配数据`)
        }
        
        scope.professionalChips.forEach(chip => {
          // 🔧 修复：使用后端数据库中的真实学生人数
          const className = chip.className
          const realStudentCount = studentCountByClass[className] || 0
          const capacity = realStudentCount > 0 ? realStudentCount : chip.totalSeats
          
          console.log(`📊   专业卡片: ${chip.majorName}, 班级: ${className}, 真实学生数: ${realStudentCount}, totalSeats: ${chip.totalSeats}`)
          this.majorAllocationStatus.push({
            majorName: chip.majorName,
            buildingName: building.name,
            buildingId: building.id,
            roomCount: Math.ceil(capacity / 4), // 假设每间4人
            capacity: capacity,
            allocated: capacity > 0,
            grade: chip.grade || this.extractGradeFromClassName(chip.className)
          })
        })
      })
      
      console.log('📊 最终 majorAllocationStatus:', this.majorAllocationStatus)
      
      // 🔥 加载年级筛选
      this.loadGradeFilter()
    },
    
    // 🔥 新增：从班级名称中提取年级
    extractGradeFromClassName(className) {
      if (!className) return null
      const match = className.match(/(\d{4})/)
      return match ? match[1] : null
    },
    
    // 🔥 新增：加载年级筛选
    loadGradeFilter() {
      // 1. 从 majorAllocationStatus 中提取所有年级
      const gradesSet = new Set()
      this.majorAllocationStatus.forEach(major => {
        if (major.grade) {
          gradesSet.add(major.grade)
        }
      })
      
      // 2. 转换为数组并排序（降序，最新的年级在前）
      this.availableGrades = Array.from(gradesSet).sort((a, b) => b.localeCompare(a))
      
      // 3. 从 localStorage 加载已保存的年级筛选
      const savedGrades = localStorage.getItem('selectedGrades')
      if (savedGrades) {
        try {
          const parsed = JSON.parse(savedGrades)
          // 只保留仍然存在的年级
          this.selectedGrades = parsed.filter(g => this.availableGrades.includes(g))
        } catch (e) {
          console.error('解析保存的年级筛选失败:', e)
          this.selectedGrades = []
        }
      }
      
      // 4. 如果没有保存的筛选，或者保存的筛选为空，自动勾选所有年级
      if (this.selectedGrades.length === 0) {
        this.selectedGrades = [...this.availableGrades]
        // 保存到 localStorage
        localStorage.setItem('selectedGrades', JSON.stringify(this.selectedGrades))
      }
      
      console.log('✅ 年级筛选加载完成:', {
        availableGrades: this.availableGrades,
        selectedGrades: this.selectedGrades
      })
    },
    
    // 🔥 新增：处理年级筛选变化
    handleGradeFilterChange(selectedGrades) {
      console.log('年级筛选变化:', selectedGrades)
      
      // 保存到 localStorage
      localStorage.setItem('selectedGrades', JSON.stringify(selectedGrades))
      
      // 重新加载专业分配状态（应用筛选）
      this.applyGradeFilter()
    },
    
    // 🔥 新增：应用年级筛选
    applyGradeFilter() {
      // 重新加载原始数据
      const originalData = []
      
      this.managedBuildings.forEach(building => {
        const scope = getBuildingAllocationScope(building.id)
        
        scope.professionalChips.forEach(chip => {
          const grade = chip.grade || this.extractGradeFromClassName(chip.className)
          
          // 只添加选中年级的专业
          if (!grade || this.selectedGrades.includes(grade)) {
            originalData.push({
              majorName: chip.majorName,
              buildingName: building.name,
              buildingId: building.id,
              roomCount: Math.ceil(chip.totalSeats / 4),
              capacity: chip.totalSeats,
              allocated: chip.totalSeats > 0,
              grade: grade
            })
          }
        })
      })
      
      this.majorAllocationStatus = originalData
      
      console.log('✅ 年级筛选应用完成，显示专业数:', this.majorAllocationStatus.length)
    },
    
    // 加载当前阶段
    loadCurrentPhase() {
      this.currentPhase = getCurrentPhase()
    },
    
    // 加载截止时间设置
    // 加载房间选择配置（从后端API）
    async loadSelectionDeadline() {
      try {
        const res = await getRoomSelectionConfig()
        if (res.code === 200 && res.data) {
          const config = res.data
          console.log('📋 房间选择配置:', config, '-> isOpen:', config.isOpen, '-> roomSelectionOpen:', config.isOpen === 1)
          this.roomSelectionOpen = config.isOpen === 1
          this.selectionDeadline = config.deadline
          
          // 🔑 关键修复：同步后端的开放状态到前端的currentPhase
          // 如果后端显示已开放，但前端currentPhase不是room_selection，需要同步
          if (this.roomSelectionOpen && this.currentPhase !== AllocationPhase.ROOM_SELECTION) {
            console.log('📋 检测到后端已开放房间选择，同步前端currentPhase')
            this.currentPhase = AllocationPhase.ROOM_SELECTION
            this.saveCurrentPhase()
          }
          // 如果后端显示未开放，但前端currentPhase是room_selection，也需要同步
          // 这种情况可能是管理员在其他地方关闭了选择
          if (!this.roomSelectionOpen && this.currentPhase === AllocationPhase.ROOM_SELECTION) {
            console.log('📋 检测到后端已关闭房间选择，但前端仍在room_selection阶段')
            // 不自动切换阶段，让用户决定是否进入下一阶段
          }
          
          // 如果有截止时间且选择已开放，检查是否过期
          if (this.selectionDeadline && this.roomSelectionOpen) {
            const deadlineTime = new Date(this.selectionDeadline)
            const now = new Date()
            if (deadlineTime > now) {
              // 截止时间未过期，启动定时器
              this.startDeadlineTimer()
            }
            // 如果已过期，不自动触发关闭，保持当前状态让用户决定
          } else {
            // 如果选择未开放，清除定时器
            this.clearDeadlineTimer()
          }
        }
      } catch (error) {
        console.error('加载房间选择配置失败:', error)
        // 使用默认值
        this.roomSelectionOpen = false
        this.selectionDeadline = null
      }
    },
    
    // 旧的loadSelectionDeadline函数（已废弃，保留代码以备参考）
    loadSelectionDeadline_OLD() {
      const savedDeadline = localStorage.getItem('roomSelectionDeadline')
      if (savedDeadline) {
        const deadlineTime = new Date(savedDeadline)
        const now = new Date()
        
        // 如果截止时间还未到，恢复设置
        if (deadlineTime > now) {
          this.selectionDeadline = savedDeadline
          // 如果房间选择已开放，启动定时器
          if (this.roomSelectionOpen) {
            this.startDeadlineTimer()
          }
        } else {
          // 截止时间已过，清除设置并自动关闭选择
          localStorage.removeItem('roomSelectionDeadline')
          if (this.roomSelectionOpen) {
            const unselectedGroupsCount = this.totalCompletedGroups - this.selectedRoomsCount
            const incompleteGroupsCount = this.incompleteGroupsList.length
            const remainingStudentsCount = this.remainingStudentsList.length
            const hasRemainingIssues = unselectedGroupsCount > 0 || incompleteGroupsCount > 0 || remainingStudentsCount > 0
            this.doCloseSelection(hasRemainingIssues)
          }
        }
      }
    },
    
    // 加载自组时间设置（已禁用自动跳转功能）
    loadGroupingTimeSettings() {
      // 不再使用自组时间设置，直接返回
    },
    
    // 保存当前阶段
    saveCurrentPhase() {
      try {
        setCurrentPhase(this.currentPhase)
        // 已保存当前阶段
        // 阶段切换后重新启动自动刷新
        this.startAutoRefresh()
      } catch (error) {
        console.error('保存阶段失败:', error)
      }
    },
    
    // 获取总班级数量
    getTotalClassCount() {
      // 模拟计算班级数量，实际应该从数据中计算
      return this.majorAllocationStatus.reduce((total, major) => {
        // 假设每个专业有若干个班级，这里简单估算
        return total + Math.ceil(major.capacity / 30) // 假设每班30人
      }, 0)
    },
    
    // 开始学生自组阶段
    startGroupingPhase() {
      this.currentPhase = AllocationPhase.SELF_GROUPING
      this.saveCurrentPhase()
      // saveCurrentPhase会自动启动自动刷新
      this.$message.success('已进入学生自组阶段')
    },
    


    // 加载学生宿舍组数据（从后端API）
    async loadStudentGroups() {
      try {
        const res = await getDormGroupList({
          currentPage: 1,
          pageSize: 1000 // 加载所有数据
        })
        if (res.code === 200) {
          this.dormGroups = res.data.records || []
          // 解析members字段（JSON字符串 → 对象）并映射字段名
          this.dormGroups = this.dormGroups.map(group => {
            // 解析members字段
            let members = []
            try {
              members = typeof group.members === 'string' 
                ? JSON.parse(group.members || '[]') 
                : (group.members || [])
            } catch (e) {
              // 解析members字段失败
              members = []
            }
            
            // 从members中提取组长信息（如果没有leaderName）
            let leaderName = group.leaderName
            let leaderId = group.leaderId
            
            // 如果leaderId存在，查找对应的成员并设置leaderName
            if (leaderId && members.length > 0) {
              // 查找组长（isLeader为true或者id匹配leaderId）
              const leader = members.find(m => 
                (m.isLeader === true || m.role === 'leader' || 
                 (m.id && String(m.id) === String(leaderId)) || 
                 (m.studentId && String(m.studentId) === String(leaderId)))
              )
              if (leader) {
                leaderName = leader.name || leader.realName
                leaderId = leader.id || leader.studentId || leaderId
              }
            } else if (!leaderId && members.length > 0) {
              // 如果leaderId不存在，查找isLeader为true的成员
              const leader = members.find(m => m.isLeader === true || m.role === 'leader')
              if (leader) {
                leaderId = leader.id || leader.studentId
                leaderName = leader.name || leader.realName
              }
            }
            
            // 映射字段名，确保前端能正确显示
            return {
              ...group,
              // 字段名映射
              memberCount: group.currentMembers || group.memberCount || members.length || 0,
              currentMembers: group.currentMembers || members.length || 0,
              class: group.className || group.class || '',
              className: group.className || group.class || '',
              createTime: group.createdAt || group.createTime || group.created_at,
              createdAt: group.createdAt || group.createTime || group.created_at,
              // 组长信息
              leaderId: leaderId || group.leaderId || null,
              leaderName: leaderName || group.leaderName || '未知',
              // 成员列表
              members: members,
              // 确保maxMembers有默认值
              maxMembers: group.maxMembers || 4,
              // 🔧 修复：映射selectedRoomId到roomId，确保关闭房间选择时能正确检测
              roomId: group.selectedRoomId || group.roomId || null
            }
          })
          
          // 🔧 修复：如果当前在学生自组阶段，加载剩余学生数据以更新统计
          if (this.currentPhase === AllocationPhase.SELF_GROUPING) {
            await this.loadRemainingStudentsFromAPI()
          }
        }
      } catch (error) {
        console.error('加载宿舍组失败:', error)
        this.dormGroups = []
      }
    },

    // 刷新宿舍组数据
    async refreshGroupData() {
      await this.loadStudentGroups()
      // 如果当前在学生自组阶段，也刷新剩余学生数据
      if (this.currentPhase === AllocationPhase.SELF_GROUPING) {
        await this.loadRemainingStudentsFromAPI()
      }
      this.$message.success('数据已刷新')
    },
    
    // 启动自动刷新机制
    startAutoRefresh() {
      // 清除之前的定时器
      this.stopAutoRefresh()
      
      // 只在学生自组阶段和房间选择阶段启用自动刷新
      if (this.currentPhase === AllocationPhase.SELF_GROUPING || 
          this.currentPhase === AllocationPhase.ROOM_SELECTION ||
          this.currentPhase === AllocationPhase.ADMIN_PROCESSING) {
        this.autoRefreshTimer = setInterval(() => {
          this.autoRefreshData()
        }, this.autoRefreshInterval)
        // 自动刷新已启动
      }
    },
    
    // 停止自动刷新
    stopAutoRefresh() {
      if (this.autoRefreshTimer) {
        clearInterval(this.autoRefreshTimer)
        this.autoRefreshTimer = null
        // 自动刷新已停止
      }
    },
    
    // 自动刷新数据（静默刷新，不显示消息）
    async autoRefreshData() {
      try {
        // 根据当前阶段刷新相应的数据
        if (this.currentPhase === AllocationPhase.SELF_GROUPING) {
          // 学生自组阶段：刷新宿舍组列表和剩余学生数据
          await Promise.all([
            this.loadStudentGroups(),
            this.loadRemainingStudentsFromAPI()
          ])
        } else if (this.currentPhase === AllocationPhase.ROOM_SELECTION) {
          // 房间选择阶段：刷新宿舍组
          await this.loadStudentGroups()
        } else if (this.currentPhase === AllocationPhase.ADMIN_PROCESSING) {
          // 剩余处理阶段：刷新所有数据
          await Promise.all([
            this.loadStudentGroups(),
            this.loadBackendStatistics(),
            this.loadRemainingData()
          ])
        }
        
        // 更新统计信息
        this.updateAllocationStats()
      } catch (error) {
        // 自动刷新数据失败（静默）
        // 静默失败，不打扰用户
      }
    },

    // 查看宿舍组详情
    viewGroupDetails(group) {
      // 构建详细的宿舍组信息
      this.selectedGroupDetails = {
        ...group,
        status: (group.memberCount >= (group.maxMembers || 4)) ? 'complete' : 'incomplete'
      }
      
      // 确保leaderId正确设置（如果从members中找不到，尝试从leaderId字段获取）
      if (!this.selectedGroupDetails.leaderId && this.selectedGroupDetails.members && this.selectedGroupDetails.members.length > 0) {
        // 查找isLeader为true的成员
        const leader = this.selectedGroupDetails.members.find(m => m.isLeader === true || m.role === 'leader')
        if (leader) {
          this.selectedGroupDetails.leaderId = leader.id || leader.studentId
          this.selectedGroupDetails.leaderName = leader.name || leader.realName
        }
      }
      
      this.groupDetailsDialogVisible = true
    },



    // 判断成员是否是组长
    isGroupLeader(member, groupDetails) {
      if (!member || !groupDetails) return false
      // 检查多种可能的匹配方式
      const memberId = member.id || member.studentId
      const leaderId = groupDetails.leaderId
      
      // 1. ID匹配（数字ID）
      if (memberId && leaderId && String(memberId) === String(leaderId)) {
        return true
      }
      // 2. studentId匹配（学号字符串）
      if (member.studentId && leaderId && String(member.studentId) === String(leaderId)) {
        return true
      }
      // 3. isLeader标记
      if (member.isLeader === true) {
        return true
      }
      // 4. role字段
      if (member.role === 'leader') {
        return true
      }
      return false
    },

    // 设置为组长
    setAsLeader(member) {
      this.$confirm(`确定要将 ${member.name} 设置为组长吗？`, '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 更新组长信息 - 使用id字段（学生ID），如果没有则使用studentId
        const newLeaderId = member.id || member.studentId
        this.selectedGroupDetails.leaderId = newLeaderId
        this.selectedGroupDetails.leaderName = member.name
        
        // 更新成员的isLeader标记
        this.selectedGroupDetails.members.forEach(m => {
          m.isLeader = (m.id === newLeaderId || m.studentId === newLeaderId)
          m.role = m.isLeader ? 'leader' : 'member'
        })
        
        this.$message.success('组长设置成功')
      }).catch(() => {
        this.$message.info('已取消操作')
      })
    },

    // 从宿舍组移除成员
    removeMemberFromGroup(member) {
      // 如果是组长且还有其他成员，不允许移除
      if (this.isGroupLeader(member, this.selectedGroupDetails) && this.selectedGroupDetails.members.length > 1) {
        this.$message.warning('组长不能被移除，请先设置其他成员为组长')
        return
      }
      
      this.$confirm(`确定要将 ${member.name} 从宿舍组中移除吗？`, '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 移除成员 - 使用id或studentId匹配
        const memberId = member.id || member.studentId
        const memberIndex = this.selectedGroupDetails.members.findIndex(m => 
          (m.id && m.id === memberId) || (m.studentId && m.studentId === memberId)
        )
        if (memberIndex > -1) {
          this.selectedGroupDetails.members.splice(memberIndex, 1)
          this.selectedGroupDetails.memberCount = this.selectedGroupDetails.members.length
          
          // 如果移除的是组长，清空组长信息或设置第一个成员为组长
          if (this.isGroupLeader(member, this.selectedGroupDetails)) {
            if (this.selectedGroupDetails.members.length > 0) {
              // 自动设置第一个成员为组长
              const newLeader = this.selectedGroupDetails.members[0]
              this.selectedGroupDetails.leaderId = newLeader.id || newLeader.studentId
              this.selectedGroupDetails.leaderName = newLeader.name
              newLeader.isLeader = true
              newLeader.role = 'leader'
              this.$message.success('已自动设置第一个成员为组长')
            } else {
              // 没有成员了，清空组长信息
              this.selectedGroupDetails.leaderId = null
              this.selectedGroupDetails.leaderName = null
            }
          }
          
          this.$message.success('成员移除成功')
        }
      }).catch(() => {
        this.$message.info('已取消操作')
      })
    },

    // 添加成员到宿舍组
    addMemberToGroup() {
      if (!this.selectedGroupDetails) {
        this.$message.error('未选择宿舍组');
        return;
      }
      
      // 检查是否已满员
      if (this.selectedGroupDetails.memberCount >= (this.selectedGroupDetails.maxMembers || 4)) {
        this.$message.warning('该宿舍组已满员，无法添加成员');
        return;
      }
      
      this.studentSelectionContext = 'group';
      this.studentSelectionDialogVisible = true;
      this.selectedStudents = [];
    },

    // 过滤可用学生
    filterAvailableStudents() {
      // 这个方法用于根据搜索和筛选条件过滤学生列表
      // 目前filteredStudents是通过computed属性计算的，所以这里不需要额外操作
      // 触发学生筛选
    },

    // 切换学生选择状态
    toggleStudentSelection(studentId) {
      const index = this.selectedStudents.indexOf(studentId);
      if (index > -1) {
        this.selectedStudents.splice(index, 1);
      } else {
        // 只有在"添加成员到宿舍组"场景下才检查人数限制
        // 批量智能分配时不应该有人数限制
        if (this.studentSelectionContext === 'group' && this.selectedGroupDetails) {
          const maxCanAdd = (this.selectedGroupDetails?.maxMembers || 4) - (this.selectedGroupDetails?.memberCount || 0);
          if (this.selectedStudents.length >= maxCanAdd) {
            this.$message.warning(`最多只能添加 ${maxCanAdd} 名学生`);
            return;
          }
        }
        this.selectedStudents.push(studentId);
      }
      // 更新选中的学生
    },

    // 确认添加学生
    async confirmAddStudents() {
      if (this.selectedStudents.length === 0) {
        this.$message.warning('请选择要添加的学生');
        return;
      }
      
      // 从未分配学生列表中获取学生信息
      const remainingStudents = this.remainingStudentsList || [];
      const studentsToAdd = this.selectedStudents.map(studentId => {
        return remainingStudents.find(s => s.id === studentId);
      }).filter(s => s);
      
      if (studentsToAdd.length === 0) {
        this.$message.error('未找到选中的学生信息');
        return;
      }
      
      // 记录分配前的房间状态（提升到外层作用域）
      let roomBefore = null;
      if (this.studentSelectionContext === 'room') {
        roomBefore = this.availableRoomsList.find(r => r.id === this.selectedRoomForAssignment.id);
      }
      
      try {
        // 判断是添加到宿舍组还是分配到房间
        if (this.studentSelectionContext === 'room') {
          // 分配学生到房间
          // 智能判断：如果房间已有人，加入现有宿舍组；否则创建新宿舍组
          const shouldCreateNewGroup = (roomBefore?.occupiedBeds || 0) === 0;
          
          const res = await assignToRoom({
            studentIds: studentsToAdd.map(s => s.id),
            roomId: this.selectedRoomForAssignment.id,
            createNewGroup: shouldCreateNewGroup
          });
          
          if (res.code !== 200) {
            this.$message.error(`分配失败: ${res.message}`);
            return;
          }
          
          this.$message.success(`成功将 ${studentsToAdd.length} 名学生分配到房间 ${this.selectedRoomForAssignment.roomNumber}`);
        } else {
          // 添加学生到宿舍组
          
          if (!this.selectedGroupDetails || !this.selectedGroupDetails.id) {
            this.$message.error('未选择宿舍组');
            return;
          }
          
          // 逐个添加学生到宿舍组
          for (const student of studentsToAdd) {
            const res = await assignToGroup({
              studentId: student.id,
              groupId: this.selectedGroupDetails.id,
              force: false
            });
            
            if (res.code !== 200) {
              this.$message.error(`添加 ${student.name} 失败: ${res.message}`);
              return;
            }
          }
          
          this.$message.success(`成功添加 ${studentsToAdd.length} 名学生到宿舍组`);
        }
        
        // 关闭对话框并清空选择
        this.studentSelectionDialogVisible = false;
        this.selectedStudents = [];
        
        // 显示加载提示
        const loading = this.$loading({
          lock: true,
          text: '正在刷新数据...',
          background: 'rgba(0, 0, 0, 0.7)'
        });
        
        try {
          // 刷新数据
          await this.loadStudentGroups();
          await this.loadRemainingData();
          await this.loadBackendStatistics();
          
          // 数据刷新完成;
        } finally {
          loading.close();
        }
        
        // 如果宿舍组详情对话框打开，更新显示
        if (this.groupDetailsDialogVisible && this.selectedGroupDetails) {
          const updatedGroup = this.dormGroups.find(g => g.id === this.selectedGroupDetails.id);
          if (updatedGroup) {
            this.selectedGroupDetails = {
              ...updatedGroup,
              status: (updatedGroup.memberCount >= (updatedGroup.maxMembers || 4)) ? 'complete' : 'incomplete'
            };
          }
        }
      } catch (error) {
        console.error('操作失败:', error);
        this.$message.error('操作失败：' + error.message);
      }
    },



    // 确认删除宿舍组
    confirmDeleteGroup(group) {
      this.$confirm(`确定要删除宿舍组"${group.groupName}"吗？此操作不可恢复。`, '确认删除', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
      }).then(() => {
        // 执行删除操作
        this.deleteGroup(group)
        this.groupDetailsDialogVisible = false
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    // 删除宿舍组（调用后端API）
    async deleteGroup(group) {
      try {
        const res = await deleteDormGroupAPI(group.id)
        if (res.code === 200) {
          // 重新加载所有数据
          await this.loadStudentGroups()
          await this.loadRemainingData()
          
          // 更新统计数据
          this.updateAllocationStats()
          
          this.$message.success('宿舍组删除成功')
        } else {
          this.$message.error(res.message || '删除失败')
        }
      } catch (error) {
        console.error('删除宿舍组失败:', error)
        this.$message.error('删除失败：' + (error.message || '网络错误'))
      }
    },

    // 查看宿舍组成员
    viewGroupMembers(group) {
      this.currentGroup = group
      this.selectedGroupDetails = {
        ...group,
        status: (group.memberCount >= (group.maxMembers || 4)) ? 'complete' : 'incomplete'
      }
      
      // 确保leaderId正确设置（如果从members中找不到，尝试从leaderId字段获取）
      if (!this.selectedGroupDetails.leaderId && this.selectedGroupDetails.members && this.selectedGroupDetails.members.length > 0) {
        // 查找isLeader为true的成员
        const leader = this.selectedGroupDetails.members.find(m => m.isLeader === true || m.role === 'leader')
        if (leader) {
          this.selectedGroupDetails.leaderId = leader.id || leader.studentId
          this.selectedGroupDetails.leaderName = leader.name || leader.realName
        }
      }
      
      this.groupDetailsDialogVisible = true
    },

    // 编辑宿舍组
    editGroup(group) {
      this.currentGroup = group
      this.editGroupForm = {
        groupName: group.groupName,
        leaderId: group.leaderId,
        members: [...group.members]
      }
      this.editGroupDialogVisible = true
    },

    // 保存宿舍组编辑（调用后端API）
    async saveGroupEdit() {
      try {
        // 准备更新数据
        const updateData = {
          groupName: this.editGroupForm.groupName,
          leaderId: this.editGroupForm.leaderId
        }
        
        // 更新组长信息
        const newLeader = this.editGroupForm.members.find(m => m.id === this.editGroupForm.leaderId)
        if (newLeader) {
          updateData.leaderName = newLeader.name || newLeader.studentName
        }
        
        const res = await updateDormGroupAPI(this.currentGroup.id, updateData)
        if (res.code === 200) {
          // 重新加载数据
          await this.loadStudentGroups()
          
          // 立即更新当前详情页面的数据
          if (this.selectedGroupDetails && this.selectedGroupDetails.id === this.currentGroup.id) {
            this.selectedGroupDetails.groupName = this.editGroupForm.groupName
            this.selectedGroupDetails.leaderId = this.editGroupForm.leaderId
            this.selectedGroupDetails.leaderName = updateData.leaderName
          }
          if (this.currentGroup) {
            this.currentGroup.groupName = this.editGroupForm.groupName
            this.currentGroup.leaderId = this.editGroupForm.leaderId
            this.currentGroup.leaderName = updateData.leaderName
          }
          
          this.editGroupDialogVisible = false
          this.$message.success('宿舍组信息已更新')
        } else {
          this.$message.error(res.message || '更新失败')
        }
      } catch (error) {
        console.error('更新宿舍组失败:', error)
        this.$message.error('更新失败：' + (error.message || '网络错误'))
      }
    },

    // 解散宿舍组（调用后端API）
    dissolveGroup(group) {
      this.$confirm(
        `确定要解散宿舍组"${group.groupName}"吗？解散后所有成员将变为单独学生。`,
        '确认解散',
        {
          confirmButtonText: '确定解散',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          const res = await deleteDormGroupAPI(group.id)
          if (res.code === 200) {
            // 重新加载所有数据
            await this.loadStudentGroups()
            await this.loadRemainingData()
            
            // 更新统计数据（重要！）
            this.updateAllocationStats()
            
            // 关闭详情对话框
            this.groupDetailsDialogVisible = false
            
            this.$message.success(`宿舍组"${group.groupName}"已解散`)
          } else {
            this.$message.error(res.message || '解散失败')
          }
        } catch (error) {
          console.error('解散宿舍组失败:', error)
          this.$message.error('解散失败：' + (error.message || '网络错误'))
        }
      }).catch(() => {
        // 用户点击了"取消"按钮，不做任何操作
      })
    },

    // 导出宿舍组成员信息
    exportGroupMembers() {
      if (!this.currentGroup) return
      
      let csvContent = '\uFEFF'
      csvContent += `宿舍组：${this.currentGroup.groupName}\n`
      csvContent += `组长：${this.currentGroup.leaderName}\n`
      csvContent += `专业：${this.currentGroup.major}\n`
      csvContent += `班级：${this.currentGroup.class}\n`
      csvContent += `创建时间：${this.currentGroup.createTime}\n`
      csvContent += `\n成员信息\n`
      csvContent += `学号,姓名,性别,专业,班级,联系电话,角色\n`
      
      this.currentGroup.members.forEach(member => {
        csvContent += `${member.studentId},${member.name},${member.gender},${member.major},${member.class},${member.phone},${member.role === 'leader' ? '组长' : '成员'}\n`
      })
      
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      link.setAttribute('href', url)
      link.setAttribute('download', `${this.currentGroup.groupName}_成员信息_${new Date().toISOString().split('T')[0]}.csv`)
      link.style.visibility = 'hidden'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      this.$message.success('成员信息导出成功')
    },

    // 格式化时间
    formatTime(timeString) {
      if (!timeString) return '-'
      return new Date(timeString).toLocaleString('zh-CN')
    },

    // 更新分配统计数据
    updateAllocationStats() {
      // 重置统计
      this.allocationStats = {
        autoAssignedCount: 0,
        manualAssignedCount: 0,
        selfGroupedCount: 0
      }

      // 统计自组宿舍数量
      const selfGroupedGroups = this.dormGroups.filter(group => 
        group.creationType === 'self' || group.status === 'complete' || group.status === 'full'
      )
      this.allocationStats.selfGroupedCount = selfGroupedGroups.reduce((total, group) => 
        total + (group.memberCount || 0), 0
      )

      // 统计管理员手动分配数量（从操作历史或标记中获取）
      this.allocationStats.manualAssignedCount = this.operationHistory?.length || 0

      // 统计系统自动分配数量
      const totalAssigned = this.allocationStats.selfGroupedCount + this.allocationStats.manualAssignedCount
      const totalStudents = this.finalStats.totalStudents || 150
      this.allocationStats.autoAssignedCount = Math.max(0, totalStudents - totalAssigned)
    },

    // 加载剩余处理数据
    async loadRemainingData() {
      // 加载未满员宿舍组
      this.incompleteGroupsList = this.dormGroups.filter(g => 
        g.status === GroupStatus.INCOMPLETE || g.status === 'incomplete' || g.status === 'forming'
      ).map(group => ({
        id: group.id,
        groupName: group.groupName,
        className: `${group.major}-${group.className}`,
        class: group.className, // Ensure class property is included
        memberCount: group.memberCount,
        maxMembers: group.maxMembers || 4,
        needed: (group.maxMembers || 4) - (group.memberCount || 0),
        major: group.major,
        members: group.members || [],
        leaderName: group.leaderName
      }))

      // 加载剩余学生数据（从后端API）
      await this.loadRemainingStudentsFromAPI()
      
      // 加载可用房间数据
      await this.loadAvailableRooms()
      
      // 提取专业和班级列表
      this.extractMajorsAndClasses()
      
      // 🔧 修复：保留筛选条件，重新应用筛选而不是重置
      if (this.dormitoryViewMode === 'incomplete') {
        this.filterDormitories()
      } else {
        this.filterRooms()
      }
      
      // 更新分配统计
      this.updateAllocationStats()
    },
    
    // 加载后端统计数据
    async loadBackendStatistics() {
      try {
        // 🔧 传递管理员负责的楼栋ID列表
        const buildingIds = this.managedBuildings.map(b => b.id)
        const res = await getAllocationStatistics(buildingIds)
        
        if (res.code === 200) {
          this.backendStats = res.data
        } else {
          console.error('获取统计数据失败:', res.message)
          this.$message.error('获取统计数据失败')
        }
      } catch (error) {
        console.error('❌ 加载统计数据失败:', error)
        // 出错时使用默认值
        this.backendStats = {
          waitingStudentsCount: 0,
          incompleteGroupsCount: 0,
          incompleteGroupBeds: 0,
          completeGroupsCount: 0,
          vacantRoomsCount: 0,
          vacantRoomBeds: 0,
          totalAvailableBeds: 0
        }
      }
    },
    
    // 从后端API加载剩余学生
    async loadRemainingStudentsFromAPI() {
      try {
        const res = await getStudentList({
          currentPage: 1,
          pageSize: 1000
        })
        // 学生API响应
        
        if (res.code === 200) {
          const allStudents = res.data.students || []
          
          // 筛选出未分配宿舍的学生
          const groupedStudentIds = new Set()
          this.dormGroups.forEach(group => {
            if (group.members && Array.isArray(group.members)) {
              group.members.forEach(member => {
                if (member.id) {
                  groupedStudentIds.add(member.id)
                }
              })
            }
          })
          
          // 🔧 修复：筛选出未分组的学生（包括未注册学生，让管理员可以提前分配）
          const ungroupedStudents = allStudents.filter(student => {
            const isGrouped = groupedStudentIds.has(student.id)
            return !isGrouped
          })
          
          // 🔧 修复：只显示真正未分配的学生（不在任何宿舍组中的学生）
          // 已经在宿舍组中的学生（即使宿舍组未满）不应该显示在待分配列表中
          // 管理员可以通过"未满宿舍组"面板查看和管理这些学生
          this.remainingStudentsList = ungroupedStudents.map(student => ({
            id: student.id,
            studentId: student.studentNumber || student.studentId || student.id,
            name: student.realName || student.name || `学生${student.id}`,
            gender: student.gender || '未知',
            major: student.major || '未分配',
            className: student.className || student.class || '未分配',
            phone: student.phone || '',
            email: student.email || '',
            groupStatus: 'unassigned',  // 未组建宿舍
            isRegistered: student.isRegistered === 1 || student.isRegistered === true  // 标记注册状态
          }))
        }
      } catch (error) {
        console.error('加载剩余学生失败:', error)
        this.remainingStudentsList = []
      }
    },
    
    // 加载可用房间数据
    async loadAvailableRooms() {
      try {
        const allRooms = []
        
        // 遍历每个楼栋，查询其房间列表
        for (const building of this.managedBuildings) {
          try {
            const buildingName = building.name || building.buildingName || `楼栋${building.id}`
            const res = await getRoomsByBuilding(building.id)
            
            if (res.code === 200 && res.data && Array.isArray(res.data)) {
              // 楼栋房间数据
              
              // 筛选有空床位的房间
              // 显示所有房间，包括满员的
              const availableRooms = res.data
                .map(room => ({
                  id: room.id,
                  roomNumber: room.roomNumber,
                  buildingName: buildingName,
                  buildingId: building.id,
                  floor: room.floor,
                  capacity: room.bedCount || 4,
                  occupiedBeds: (room.bedCount || 4) - (room.availableBeds || 0),
                  availableBeds: room.availableBeds || 0,
                  majorName: room.majorName || '',
                  className: room.className || '',
                  roomType: room.roomType || '四人间',
                  isFull: (room.availableBeds || 0) === 0  // 标记是否满员
                }))
              
              const fullCount = availableRooms.filter(r => r.isFull).length
              const emptyCount = availableRooms.filter(r => !r.isFull).length
              console.log(`  ✅ 楼栋 ${buildingName} 共 ${availableRooms.length} 个房间 (空闲: ${emptyCount}, 满员: ${fullCount})`)
              allRooms.push(...availableRooms)
            } else {
              console.warn(`⚠️ 楼栋 ${buildingName} 返回数据格式异常:`, res)
            }
          } catch (error) {
            console.error(`❌ 查询楼栋 ${buildingName} 的房间失败:`, error)
          }
        }
        
        this.availableRoomsList = allRooms
        
        // 从房间数据中提取班级列表
        this.initializeAvailableClasses()
        
        // 初始化筛选结果（显示所有房间）
        this.filterRooms()
      } catch (error) {
        console.error('❌ 加载可用房间失败:', error)
        this.availableRoomsList = []
        this.filteredAvailableRooms = []
      }
    },
    
    // 提取专业和班级列表
    extractMajorsAndClasses() {
      // 从剩余学生中提取专业
      const majorsSet = new Set()
      const classesSet = new Set()
      
      this.remainingStudentsList.forEach(student => {
        if (student.major && student.major !== '未分配' && student.major.trim()) {
          majorsSet.add(student.major)
        }
        if (student.className && student.className !== '未分配' && student.className.trim()) {
          classesSet.add(student.className)
        }
      })
      
      // 从未满宿舍组中提取专业
      this.incompleteGroupsList.forEach(group => {
        if (group.major && group.major !== '未分配' && group.major.trim()) {
          majorsSet.add(group.major)
        }
      })
      
      // 过滤掉空值和undefined，然后排序
      this.availableMajors = Array.from(majorsSet).filter(m => m).sort()
      
      // 将班级列表转换为对象数组，用于el-select
      const classesArray = Array.from(classesSet).filter(c => c).sort()
      this.availableClasses = classesArray.map(className => ({
        label: className,
        value: className
      }))
    },
    
    // 筛选宿舍（未满宿舍组或可用房间）
    filterDormitories() {
      if (this.dormitoryViewMode === 'incomplete') {
        // 筛选未满宿舍组
        let filtered = [...this.incompleteGroupsList]
        
        if (this.dormitoryFilterMajor) {
          filtered = filtered.filter(group => group.major === this.dormitoryFilterMajor)
        }
        
        this.filteredIncompleteGroups = filtered
      } else {
        // 切换到可用房间视图时，应用当前的筛选条件
        this.filterRooms()
      }
    },

    // 获取房间状态样式类
    getRoomStatusClass(room) {
      // 判断是否满员：可用床位为0
      if (room.availableBeds === 0 || room.occupiedBeds === room.capacity) {
        return 'room-full' // 已满员 - 红色
      } else if (room.occupiedBeds === 0) {
        return 'room-empty' // 未入住 - 绿色
      } else {
        return 'room-partial' // 部分入住 - 蓝色
      }
    },
    
    // 获取容量标签类型
    getCapacityTagType(room) {
      if (room.occupiedBeds === 4) {
        return 'warning' // 已满员
      } else if (room.occupiedBeds === 0) {
        return 'success' // 未入住
      } else {
        return 'primary' // 部分入住
      }
    },
    
    // 按班级筛选房间
    // 筛选可用房间（楼层和班级）
    filterRooms() {
      let filtered = [...this.availableRoomsList]
      
      // 楼层筛选
      if (this.roomFilterFloor) {
        filtered = filtered.filter(room => {
          // 从房间号提取楼层（假设房间号格式如 "101", "201" 等）
          const floor = Math.floor(parseInt(room.roomNumber) / 100)
          return floor.toString() === this.roomFilterFloor
        })
      }
      
      // 班级筛选（根据房间的className字段）
      if (this.roomFilterClass) {
        filtered = filtered.filter(room => {
          // 房间的className字段直接存储了班级信息
          // 如果房间有班级信息，匹配该班级
          if (room.className) {
            return room.className === this.roomFilterClass
          }
          // 如果房间没有班级信息，说明是空房间，也显示出来（可以分配给该班级）
          return !room.className || room.className === ''
        })
      }
      
      this.filteredAvailableRooms = filtered
    },
    
    // 初始化可用班级列表
    initializeAvailableClasses() {
      // 从房间数据中提取班级列表
      const classesSet = new Set()
      
      this.availableRoomsList.forEach(room => {
        if (room.className && room.className.trim()) {
          classesSet.add(room.className)
        }
      })
      
      // 转换为下拉框需要的格式
      this.availableClasses = Array.from(classesSet).map(className => ({
        label: className,
        value: className
      }))
      
      // 班级列表提取完成
    },

    // 学生选择事件
    onStudentSelect(student) {
      // 学生选择处理
    },
    

    
    // 开始房间选择阶段
    startRoomSelectionPhase() {
      // 检查是否有未满员的宿舍组
      const incompleteGroups = this.dormGroups.filter(g => 
        g.memberCount < (g.maxMembers || 4)
      );
      
      if (incompleteGroups.length > 0) {
        this.$confirm(
          `当前还有 ${incompleteGroups.length} 个未满员的宿舍组，确定要进入房间选择阶段吗？未满员的宿舍组将无法选择房间。`,
          '确认切换阶段',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          this.doStartRoomSelection();
        }).catch(() => {
          this.$message.info('已取消操作');
        });
      } else {
        this.doStartRoomSelection();
      }
    },
    
    // 执行开始房间选择
    async doStartRoomSelection() {
      try {
        // 按楼栋切换阶段（只影响当前管理员负责的楼栋）
        let successCount = 0
        let failedCount = 0
        
        for (const building of this.managedBuildings) {
          try {
            const res = await switchBuildingPhase(building.id, 'room_selection')
            if (res.code === 200) {
              successCount++
              console.log(`✅ 楼栋 ${building.name} 阶段切换成功`)
            } else {
              failedCount++
              console.error(`❌ 楼栋 ${building.name} 阶段切换失败:`, res.message)
            }
          } catch (e) {
            failedCount++
            console.error(`❌ 楼栋 ${building.name} 阶段切换异常:`, e)
          }
        }
        
        if (successCount === 0) {
          this.$message.error('所有楼栋阶段切换失败')
          return
        }
        
        // 切换阶段
        this.currentPhase = AllocationPhase.ROOM_SELECTION;
        this.saveCurrentPhase();
        
        // 保存阶段切换到localStorage
        localStorage.setItem('system_allocation_phase', 'room_selection');
        
        // 🔧 加载房间选择配置（包括截止时间）
        await this.loadSelectionDeadline();
        
        // 🔧 如果没有截止时间，基于阶段切换时间自动设置为60天后
        if (!this.selectionDeadline) {
          const phaseStartTime = new Date() // 阶段切换时间（当前时间）
          const defaultDeadline = new Date(phaseStartTime)
          defaultDeadline.setDate(defaultDeadline.getDate() + 60) // 60天后
          
          // 格式化为 YYYY-MM-DD HH:mm:ss
          const pad = (n) => n < 10 ? '0' + n : n
          const formattedDeadline = `${defaultDeadline.getFullYear()}-${pad(defaultDeadline.getMonth() + 1)}-${pad(defaultDeadline.getDate())} ${pad(defaultDeadline.getHours())}:${pad(defaultDeadline.getMinutes())}:${pad(defaultDeadline.getSeconds())}`
          
          // 保存到后端
          try {
            // 获取当前管理员管理的第一个楼栋ID
            const buildingId = this.managedBuildings.length > 0 ? this.managedBuildings[0].id : null
            const updateRes = await updateDeadline({
              deadline: formattedDeadline,
              buildingId: buildingId,
              adminUsername: this.currentUser.username
            })
            
            if (updateRes.code === 200) {
              this.selectionDeadline = formattedDeadline
              this.$message.success(`已进入房间选择阶段，截止时间自动设置为60天后（${formattedDeadline}）`)
              
              // 启动定时器
              this.startDeadlineTimer()
            }
          } catch (error) {
            console.error('设置默认截止时间失败:', error)
            this.$message.warning('已进入房间选择阶段，但截止时间设置失败，请手动设置')
          }
        } else {
          if (failedCount > 0) {
            this.$message.warning(`已进入房间选择阶段，${successCount} 个楼栋成功，${failedCount} 个楼栋失败`)
          } else {
            this.$message.success('已进入房间选择阶段，学生可以开始选择房间');
          }
        }
        
        // 触发事件通知
        window.dispatchEvent(new Event('allocationPhaseChanged'));
      } catch (error) {
        console.error('切换阶段失败:', error)
        this.$message.error('操作失败，请稍后重试')
      }
    },
    
    // 返回学生自组阶段
    goBackToGroupingPhase() {
      this.$confirm(
        '返回学生自组阶段将保留已选择的房间状态，确定要返回吗？', 
        '确认返回', 
        {
          confirmButtonText: '确定返回',
          cancelButtonText: '取消',
          type: 'info'
        }
      ).then(() => {
        this.currentPhase = AllocationPhase.SELF_GROUPING;
        this.saveCurrentPhase();
        
        // 保存阶段切换到localStorage
        localStorage.setItem('system_allocation_phase', 'grouping');
        
        this.$message.success('已返回学生自组阶段');
        
        // 触发事件通知
        window.dispatchEvent(new Event('allocationPhaseChanged'));
      }).catch(() => {
        // 用户点击了"取消"按钮，不做任何操作
      });
    },
    
    // 开放房间选择
    openRoomSelection() {
      // 如果已经开放，提示用户
      if (this.roomSelectionOpen) {
        this.$message.info('房间选择已经是开放状态')
        return
      }
      
      // 检查截止时间是否已过期
      if (this.selectionDeadline) {
        const deadlineTime = new Date(this.selectionDeadline)
        const now = new Date()
        
        if (deadlineTime <= now) {
          // 截止时间已过，提示重新设置
          this.$confirm(
            '之前设置的截止时间已过期，是否要重新设置截止时间？', 
            '截止时间已过期', 
            {
              confirmButtonText: '重新设置',
              cancelButtonText: '暂不设置',
              type: 'warning'
            }
          ).then(() => {
            this.setSelectionDeadline()
          }).catch(() => {
            // 用户选择暂不设置，不做任何操作
          }).finally(() => {
            this.doOpenRoomSelection()
          })
          return
        }
      }
      
      this.doOpenRoomSelection()
    },
    
    // 执行开放房间选择（调用后端API）
    // silent: 是否静默模式（不显示提示）
    async doOpenRoomSelection(silent = false) {
      // 🔑 先清除旧的定时器
      this.clearDeadlineTimer()
      
      try {
        console.log('🚀 开放房间选择，截止时间:', this.selectionDeadline)
        const res = await openRoomSelectionAPI({
          deadline: this.selectionDeadline,
          batchName: this.batchDescription || '房间选择批次',
          description: '管理员开放房间选择',
          adminUsername: this.currentUser.username
        })
        console.log('🚀 开放房间选择结果:', res)
        
        if (res.code === 200) {
          this.roomSelectionOpen = true
          
          // 🔑 同步更新前端的currentPhase到room_selection
          if (this.currentPhase !== AllocationPhase.ROOM_SELECTION) {
            this.currentPhase = AllocationPhase.ROOM_SELECTION
            this.saveCurrentPhase()
            console.log('📋 开放房间选择：已同步currentPhase到room_selection')
          }
          
          if (!silent) {
            this.$message.success('房间选择已开放')
          }
          
          // 如果有设置截止时间且未过期，启动新的定时器
          if (this.selectionDeadline) {
            const deadlineTime = new Date(this.selectionDeadline)
            const now = new Date()
            if (deadlineTime > now) {
              this.startDeadlineTimer()
            }
          }
        } else {
          this.$message.error(res.message || '开放失败')
        }
      } catch (error) {
        console.error('开放房间选择失败:', error)
        this.$message.error('开放失败：' + (error.message || '网络错误'))
      }
    },
    
    // 关闭房间选择
    closeRoomSelection() {
      // 准确计算：已满员（或状态为room_selected）但未关联roomId的宿舍组数量
      const unselectedGroupsCount = this.dormGroups.filter(g => 
        (g.status === 'complete' || g.status === 'full' || g.status === 'room_selected') && !g.roomId
      ).length
      
      // 使用计算属性获取实时的未满宿舍组数量
      const incompleteGroupsCount = this.incompleteGroups
      // 剩余学生数量可能无法准确获取（如果没有加载），暂且认为0，或者使用computedAllocationMode判断
      const remainingStudentsCount = this.remainingStudentsList?.length || 0
      
      let needAdminProcessing = false
      let warningMessage = '确定要关闭房间选择吗？'
      
      if (unselectedGroupsCount > 0 || incompleteGroupsCount > 0 || remainingStudentsCount > 0) {
        needAdminProcessing = true
        let details = []
        
        if (unselectedGroupsCount > 0) {
          details.push(`${unselectedGroupsCount} 个宿舍组尚未选择房间`)
        }
        if (incompleteGroupsCount > 0) {
          details.push(`${incompleteGroupsCount} 个宿舍组人数不满`)
        }
        if (remainingStudentsCount > 0) {
          details.push(`${remainingStudentsCount} 名学生未组建宿舍`)
        }
        
        warningMessage = `${details.join('，')}，关闭后这些情况将由管理员处理。确定要关闭吗？`
      }
      
      if (needAdminProcessing) {
        this.$confirm(warningMessage, '确认关闭房间选择', {
          confirmButtonText: '确定关闭',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.doCloseSelection(true)
        }).catch(() => {
          // 用户点击了"取消"按钮，不做任何操作
        })
      } else {
        this.doCloseSelection(false)
      }
    },
    
    // 执行关闭选择（调用后端API）
    async doCloseSelection(hasRemainingIssues) {
      try {
        const res = await closeRoomSelectionAPI(this.currentUser.username)
        
        if (res.code === 200) {
          this.roomSelectionOpen = false
          this.clearDeadlineTimer()
          // 清除截止时间
          this.selectionDeadline = null
          localStorage.removeItem('roomSelectionDeadline')
          
          if (hasRemainingIssues) {
            this.$message.warning('房间选择已关闭')
          } else {
            this.$message.success('房间选择已关闭，学生无法继续选择房间')
          }
        } else {
          this.$message.error(res.message || '关闭失败')
        }
      } catch (error) {
        console.error('关闭房间选择失败:', error)
        this.$message.error('关闭失败：' + (error.message || '网络错误'))
      }
    },
    
    // 静默关闭选择（不显示提示，用于截止时间到期自动关闭）
    async doCloseSelectionSilent() {
      try {
        const res = await closeRoomSelectionAPI(this.currentUser.username)
        if (res.code === 200) {
          this.roomSelectionOpen = false
          this.clearDeadlineTimer()
          // 清除截止时间
          this.selectionDeadline = null
          localStorage.removeItem('roomSelectionDeadline')
        }
      } catch (error) {
        console.error('静默关闭房间选择失败:', error)
      }
    },
    
    // 设置截止时间（调用后端API）
    setSelectionDeadline() {
      // 将当前截止时间转换为datetime-local格式 (YYYY-MM-DDTHH:mm)
      let currentDeadlineForInput = ''
      if (this.selectionDeadline) {
        // 处理 "YYYY-MM-DD HH:mm:ss" 格式
        currentDeadlineForInput = this.selectionDeadline.replace(' ', 'T').substring(0, 16)
      }
      
      this.$prompt('请设置房间选择的截止时间', '设置截止时间', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'datetime-local',
        inputValue: currentDeadlineForInput
      }).then(async ({ value }) => {
        if (value) {
          const deadlineTime = new Date(value)
          const now = new Date()
          
          if (deadlineTime <= now) {
            this.$message.error('截止时间不能早于或等于当前时间')
            return
          }
          
          // 🔑 关键：先清除旧的定时器，防止旧定时器触发关闭逻辑
          this.clearDeadlineTimer()
          
          // 将datetime-local格式转换为后端期望的格式 (YYYY-MM-DD HH:mm:ss)
          const pad = (n) => n < 10 ? '0' + n : n
          const formattedDeadline = `${deadlineTime.getFullYear()}-${pad(deadlineTime.getMonth() + 1)}-${pad(deadlineTime.getDate())} ${pad(deadlineTime.getHours())}:${pad(deadlineTime.getMinutes())}:${pad(deadlineTime.getSeconds())}`
          
          try {
            // 先更新截止时间
            const buildingId = this.managedBuildings.length > 0 ? this.managedBuildings[0].id : null
            const res = await updateDeadline({
              deadline: formattedDeadline,
              buildingId: buildingId,
              adminUsername: this.currentUser.username
            })
            
            if (res.code === 200) {
              // 更新本地状态
              this.selectionDeadline = formattedDeadline
              
              // 自动开放房间选择（无论当前状态如何）
              try {
                const openRes = await openRoomSelectionAPI({
                  deadline: formattedDeadline,
                  batchName: this.batchDescription || '房间选择批次',
                  description: '设置截止时间自动开放',
                  adminUsername: this.currentUser.username
                })
                
                if (openRes.code === 200) {
                  // 🔑 关键：确保按钮状态更新
                  this.roomSelectionOpen = true
                  
                  // 🔑 同步更新前端的currentPhase到room_selection
                  // 这样刷新页面后，前端状态和后端状态保持一致
                  if (this.currentPhase !== AllocationPhase.ROOM_SELECTION) {
                    this.currentPhase = AllocationPhase.ROOM_SELECTION
                    this.saveCurrentPhase()
                    console.log('📋 已同步currentPhase到room_selection')
                  }
                  
                  this.$message.success(`已设置截止时间并开放房间选择`)
                  // 启动定时器
                  this.startDeadlineTimer()
                } else {
                  // 即使开放失败，也更新本地状态让用户可以手动操作
                  this.$message.warning(`截止时间已设置，但开放房间选择失败：${openRes.message}`)
                }
              } catch (openError) {
                console.error('自动开放房间选择失败:', openError)
                this.$message.warning(`截止时间已设置，但开放房间选择失败`)
              }
            }
          } catch (error) {
            console.error('设置截止时间失败:', error)
            this.$message.error('设置失败：' + (error.message || '网络错误'))
          }
        }
      }).catch(() => {
        this.$message.info('已取消设置')
      })
    },
    
    // 启动截止时间定时器
    startDeadlineTimer() {
      if (!this.selectionDeadline) {
        return
      }
      
      // 清除之前的定时器
      this.clearDeadlineTimer()
      
      const deadlineTime = new Date(this.selectionDeadline)
      const now = new Date()
      const timeLeft = deadlineTime.getTime() - now.getTime()
      
      // setTimeout 最大延迟约 24.8 天 (2147483647ms)
      const MAX_TIMEOUT = 2147483647
      
      if (timeLeft > 0) {
        if (timeLeft > MAX_TIMEOUT) {
          // 如果超过最大延迟，先设置一个中间定时器，之后再重新检查
          console.log('⏰ 截止时间超过24天，设置中间定时器')
          this.deadlineTimer = setTimeout(() => {
            // 重新启动定时器
            this.startDeadlineTimer()
          }, MAX_TIMEOUT)
        } else {
          // 正常设置定时器
          console.log('⏰ 定时器将在', Math.round(timeLeft / 1000), '秒后触发')
          this.deadlineTimer = setTimeout(() => {
            const unselectedGroupsCount = this.totalCompletedGroups - this.selectedRoomsCount
            const incompleteGroupsCount = this.incompleteGroupsList.length
            const remainingStudentsCount = this.remainingStudentsList.length
            const hasRemainingIssues = unselectedGroupsCount > 0 || incompleteGroupsCount > 0 || remainingStudentsCount > 0
            
            // 清除过期的截止时间设置
            this.selectionDeadline = null
            localStorage.removeItem('roomSelectionDeadline')
            
            // 只显示一条综合提示
            if (hasRemainingIssues) {
              this.$message.warning('房间选择时间已到，系统已自动关闭选择功能，存在需要管理员处理的情况')
            } else {
              this.$message.warning('房间选择时间已到，系统已自动关闭选择功能')
            }
            
            // 静默关闭选择，不显示额外提示
            this.doCloseSelectionSilent()
          }, timeLeft)
        }
      }
    },
    
    // 清除截止时间定时器
    clearDeadlineTimer() {
      if (this.deadlineTimer) {
        console.log('⏰ 清除定时器')
        clearTimeout(this.deadlineTimer)
        this.deadlineTimer = null
      }
    },
    
    // 清除截止时间设置
    clearDeadline() {
      this.$confirm(
        '清除截止时间后，房间选择将不会自动关闭，确定要清除吗？', 
        '确认清除截止时间', 
        {
          confirmButtonText: '确定清除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          // 调用后端API清除截止时间
          const buildingId = this.managedBuildings.length > 0 ? this.managedBuildings[0].id : null
          const res = await updateDeadline({
            deadline: null,
            buildingId: buildingId,
            adminUsername: this.currentUser.username
          })
          
          if (res.code === 200) {
            this.selectionDeadline = null
            localStorage.removeItem('roomSelectionDeadline')
            this.clearDeadlineTimer()
            this.$message.success('已清除截止时间设置')
          } else {
            this.$message.error(res.message || '清除失败')
          }
        } catch (error) {
          console.error('清除截止时间失败:', error)
          this.$message.error('清除失败：' + (error.message || '网络错误'))
        }
      }).catch((err) => {
        // 用户点击了"取消"按钮，不做任何操作
        if (err !== 'cancel') {
          console.error(err)
        }
      })
    },
    
    // 提醒未选择的宿舍组（调用后端API发送通知）
    remindUnselectedGroups() {
      const unselectedCount = this.totalCompletedGroups - this.selectedRoomsCount
      this.$confirm(
        `当前有 ${unselectedCount} 个宿舍组尚未选择房间，是否发送提醒通知？`, 
        '发送提醒', 
        {
          confirmButtonText: '发送提醒',
          cancelButtonText: '取消',
          type: 'info'
        }
      ).then(async () => {
        try {
          const res = await remindUnselectedGroupsAPI()
          if (res.code === 200) {
            const data = res.data
            this.$message.success(data.message || `已向 ${unselectedCount} 个宿舍组发送选择房间的提醒`)
          } else {
            this.$message.error(res.message || '发送提醒失败')
          }
        } catch (error) {
          console.error('发送提醒失败:', error)
          this.$message.error('发送提醒失败：' + (error.message || '网络错误'))
        }
      }).catch(() => {
        // 用户点击了"取消"按钮，不做任何操作
      })
    },
    
    
    // 开始剩余处理阶段
    startProcessingPhase() {
      const buildingNames = this.managedBuildings.map(b => b.name).join('、')
      this.$confirm(
        `进入剩余处理阶段将关闭 ${buildingNames} 的学生自组和房间选择功能，确定要进入吗？`, 
        '确认进入剩余处理阶段', 
        {
          confirmButtonText: '确定进入',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          // 按楼栋切换阶段（只影响当前管理员负责的楼栋）
          let successCount = 0
          let failedCount = 0
          
          for (const building of this.managedBuildings) {
            try {
              const res = await switchBuildingPhase(building.id, 'admin_allocation')
              if (res.code === 200) {
                successCount++
                console.log(`✅ 楼栋 ${building.name} 阶段切换成功`)
              } else {
                failedCount++
                console.error(`❌ 楼栋 ${building.name} 阶段切换失败:`, res.message)
              }
            } catch (e) {
              failedCount++
              console.error(`❌ 楼栋 ${building.name} 阶段切换异常:`, e)
            }
          }
          
          if (successCount === 0) {
            this.$message.error('所有楼栋阶段切换失败')
            return
          }
          
          // 关闭所有学生操作功能
          this.roomSelectionOpen = false
          this.clearDeadlineTimer()
          
          // 进入剩余处理阶段
          this.currentPhase = AllocationPhase.ADMIN_PROCESSING
          this.saveCurrentPhase()
          
          await Promise.all([
            this.loadBackendStatistics(),  // 加载后端统计
            this.loadRemainingData()        // 加载剩余学生数据
          ])
          
          if (failedCount > 0) {
            this.$message.warning(`已进入剩余处理阶段，${successCount} 个楼栋成功，${failedCount} 个楼栋失败`)
          } else {
            this.$message.success('已进入剩余处理阶段，学生自组和房间选择功能已关闭')
          }
        } catch (error) {
          console.error('进入剩余处理阶段失败:', error)
          this.$message.error('进入剩余处理阶段失败: ' + (error.message || '未知错误'))
        }
      }).catch(() => {
        // 用户点击了"取消"按钮，不做任何操作
      })
    },
    


    // 手动补充宿舍组
    manualFillGroup(group) {
      this.currentGroup = group
      this.manualAssignDialogVisible = true
    },
    
    // 处理学生分配操作

    

    
    openManualAssignDialog() {
      this.manualAssignDialogVisible = true
      this.loadRemainingData()
      this.refreshRemainingStudents()
      this.operationHistory = []
    },

    // 手动分配工具相关方法
    handleDragStart(event, student) {
      this.draggedStudent = student
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', JSON.stringify(student))
    },

    // 拖拽分配功能已移除，请使用点击分配按钮

    async createNewGroupWithStudent(student) {
      try {
        const result = await createNewGroupForStudent(student.studentId)
        if (result.success) {
          this.$message.success(`成功为 ${student.name} 创建新宿舍组`)
          
          // 记录操作历史
          this.operationHistory.unshift({
            time: new Date(),
            action: `为 ${student.name} 创建新宿舍组`
          })

          // 刷新数据
          this.loadStudentGroups()
          this.loadRemainingData()
          this.refreshRemainingStudents()
        } else {
          this.$message.error(result.message)
        }
      } catch (error) {
        this.$message.error(`创建宿舍组失败: ${error.message}`)
      }
    },

    async createNewGroup() {
      if (this.remainingStudentsList.length === 0) {
        this.$message.warning('没有未分组的学生')
        return
      }

      // 选择一个学生作为组长
      const firstStudent = this.remainingStudentsList[0]
      await this.createNewGroupWithStudent(firstStudent)
    },

    // 刷新剩余学生（从后端API）
    async refreshRemainingStudents() {
      await this.loadRemainingStudentsFromAPI()
    },

    getGroupMembers(groupId) {
      const group = this.dormGroups.find(g => g.id === groupId)
      return group ? group.members : []
    },

    getGroupGender(groupId) {
      const group = this.dormGroups.find(g => g.id === groupId)
      return group ? group.gender : null
    },

    getGroupMajor(groupId) {
      const group = this.dormGroups.find(g => g.id === groupId)
      return group ? group.major : null
    },

    async saveManualAssignments() {
      try {
        // 验证当前分配状态
        const validation = validateAllocationResults()
        
        if (validation.isValid) {
          this.$message.success('手动分配结果已保存')
          this.manualAssignDialogVisible = false
          
          // 刷新主界面数据
          this.loadStudentGroups()
          this.loadRemainingData()
        } else {
          const result = await this.$confirm(
            `发现 ${validation.issues.length} 个问题，是否仍要保存？\n${validation.issues.slice(0, 3).join('\n')}`,
            '验证警告',
            {
              confirmButtonText: '仍要保存',
              cancelButtonText: '继续编辑',
              type: 'warning'
            }
          )
          
          if (result) {
            this.$message.success('手动分配结果已保存（存在警告）')
            this.manualAssignDialogVisible = false
            this.loadStudentGroups()
            this.loadRemainingData()
          }
        }
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(`保存失败: ${error.message}`)
        }
      }
    },

    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    // 完成分配
    async completeAllocation() {
      try {
        const buildingNames = this.managedBuildings.map(b => b.name).join('、')
        // 确认操作
        await this.$confirm(`完成分配后，将把 ${buildingNames} 的宿舍信息同步到学生表，学生信息页面将显示宿舍号。确定要完成分配吗？`, '确认完成分配', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const loading = this.$loading({
          lock: true,
          text: '正在同步宿舍信息到学生表...',
          spinner: 'el-icon-loading',
          background: 'rgba(0, 0, 0, 0.7)'
        })
        
        try {
          // 调用后端API，传入管理员负责的楼栋ID列表
          const buildingIds = this.managedBuildings.map(b => b.id)
          const { completeAllocation: completeAllocationAPI } = await import('@/api/allocation.js')
          const res = await completeAllocationAPI(buildingIds)
          
          loading.close()
          
          if (res.code === 200) {
            // 更新前端状态
            this.currentPhase = AllocationPhase.COMPLETED
            this.saveCurrentPhase()
            
            // 显示详细结果
            const data = res.data || {}
            this.$message.success({
              message: `分配完成！已同步 ${data.successCount || 0} 个学生的宿舍信息`,
              duration: 3000
            })
            
            console.log('完成分配结果：', data)
          } else {
            this.$message.error(res.message || '完成分配失败')
          }
        } catch (error) {
          loading.close()
          console.error('完成分配失败：', error)
          this.$message.error('完成分配失败：' + (error.message || '未知错误'))
        }
      } catch {
        // 用户取消操作
      }
    },

    // ========== 开发调试功能 ==========
    
    // 快速切换阶段（仅开发模式）
    async quickSwitchPhase(phase) {
      const phaseNames = {
        'preparation': '分配范围确认',
        'self_grouping': '学生自组宿舍',
        'room_selection': '房间选择',
        'admin_processing': '剩余处理',
        'completed': '分配完成'
      }
      
      console.log('🔄 切换阶段:', this.currentPhase, '->', phase)
      this.currentPhase = phase
      this.saveCurrentPhase()
      
      // 🔧 如果切换到房间选择阶段，加载截止时间配置
      if (phase === 'room_selection') {
        await this.loadSelectionDeadline()
      } else {
        // 切换到其他阶段时，清除截止时间显示
        this.selectionDeadline = null
      }
      
      console.log('✅ 当前阶段已更新为:', this.currentPhase)
      this.$message.success(`已切换到：${phaseNames[phase]}`)
    },
    
    // 切换调试面板展开状态
    toggleDebugPanel() {
      this.debugPanelExpanded = !this.debugPanelExpanded
    },
    
    // 导出报表
    exportAllocationReport() {
      try {
        const data = exportAllAllocationData()
        if (!data) {
          this.$message.error('导出数据失败')
          return
        }
        
        // 计算统计数据
        const totalGroups = this.dormGroups.length
        const completedGroupsCount = this.completedGroups
        const incompleteGroupsCount = this.incompleteGroups
        const remainingStudentsCount = this.remainingStudentsList.length
        const totalStudentsInGroups = this.dormGroups.reduce((sum, g) => sum + (g.memberCount || g.members?.length || 0), 0)
        const totalStudents = totalStudentsInGroups + remainingStudentsCount
        const completionRate = totalStudents > 0 ? Math.round((totalStudentsInGroups / totalStudents) * 100) : 0
        
        // 获取楼栋信息
        const buildingNames = this.managedBuildings.map(b => b.name || b.buildingName || `楼栋${b.id}`).filter(n => n)
        
        // 生成CSV内容
        let csvContent = '\uFEFF' // BOM for UTF-8
        csvContent += `学生宿舍分配报表\n`
        csvContent += `\n`
        csvContent += `【基本信息】\n`
        csvContent += `导出时间,${new Date().toLocaleString('zh-CN')}\n`
        csvContent += `批次描述,${this.batchDescription || '未设置'}\n`
        csvContent += `当前阶段,${this.currentPhaseText}\n`
        csvContent += `分配楼栋,${buildingNames.length > 0 ? buildingNames.join('、') : '未指定'}\n`
        csvContent += `\n`
        
        csvContent += `【统计汇总】\n`
        csvContent += `项目,数量\n`
        csvContent += `总学生数,${totalStudents}\n`
        csvContent += `已分配学生,${totalStudentsInGroups}\n`
        csvContent += `待分配学生,${remainingStudentsCount}\n`
        csvContent += `总宿舍组数,${totalGroups}\n`
        csvContent += `已满员宿舍组,${completedGroupsCount}\n`
        csvContent += `未满员宿舍组,${incompleteGroupsCount}\n`
        csvContent += `分配完成率,${completionRate}%\n`
        csvContent += `\n`
        
        // 宿舍组详情
        csvContent += `【宿舍组详情】\n`
        csvContent += `宿舍组名称,组长,当前人数,最大容量,专业,班级,房间号,状态\n`
        this.dormGroups.forEach(group => {
          const statusText = (group.status === 'complete' || group.status === 'full' || group.status === 'room_selected' || group.status === 'allocated') ? '已满员' : '未满员'
          const roomNumber = group.roomNumber || group.room?.roomNumber || '未分配'
          const memberCount = group.memberCount || group.members?.length || 0
          const maxMembers = group.maxMembers || 4
          csvContent += `${group.groupName || ''},${group.leaderName || ''},${memberCount},${maxMembers},${group.major || ''},${group.class || group.className || ''},${roomNumber},${statusText}\n`
        })
        csvContent += `\n`
        
        // 宿舍组成员详情
        csvContent += `【宿舍组成员详情】\n`
        csvContent += `宿舍组名称,学号,姓名,性别,专业,班级,角色\n`
        this.dormGroups.forEach(group => {
          if (group.members && group.members.length > 0) {
            group.members.forEach(member => {
              const role = member.role === 'leader' ? '组长' : '成员'
              csvContent += `${group.groupName || ''},${member.studentId || ''},${member.name || member.realName || ''},${member.gender || ''},${member.major || ''},${member.className || member.class || ''},${role}\n`
            })
          }
        })
        csvContent += `\n`
        
        // 待分配学生列表
        if (this.remainingStudentsList.length > 0) {
          csvContent += `【待分配学生】\n`
          csvContent += `学号,姓名,性别,专业,班级\n`
          this.remainingStudentsList.forEach(student => {
            csvContent += `${student.studentId || ''},${student.name || student.realName || ''},${student.gender || ''},${student.major || ''},${student.className || student.class || ''}\n`
          })
          csvContent += `\n`
        }
        
        // 专业分配状态
        if (this.majorAllocationStatus.length > 0) {
          csvContent += `【专业分配状态】\n`
          csvContent += `专业名称,分配楼栋,分配宿舍数,总人数,状态\n`
          this.majorAllocationStatus.forEach(major => {
            const statusText = major.allocated ? '已分配' : '未分配'
            csvContent += `${major.majorName || ''},${major.buildingName || ''},${major.roomCount || 0},${major.capacity || 0},${statusText}\n`
          })
        }
        
        // 下载文件
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
        const link = document.createElement('a')
        const url = URL.createObjectURL(blob)
        link.setAttribute('href', url)
        link.setAttribute('download', `学生宿舍分配报表_${new Date().toISOString().split('T')[0]}.csv`)
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        
        this.$message.success('分配报表导出成功')
      } catch (error) {
        console.error('导出报表失败:', error)
        this.$message.error(`导出失败: ${error.message}`)
      }
    },
    
    
    // 开始新一轮分配
    startNewAllocationRound() {
      this.$confirm('确定要开始新一轮分配吗？当前分配结果将保存到历史记录中。', '确认新一轮分配', {
        confirmButtonText: '开始新一轮',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 保存当前分配到历史记录
        this.saveAllocationToHistory()
        
        // 重新加载数据，但保持楼栋选择
        this.loadData()
        
        // 重置到分配范围确认阶段
        this.currentPhase = AllocationPhase.PREPARATION
        this.saveCurrentPhase()
        
        this.$message.success('已开始新一轮分配，上一轮结果已保存到历史记录')
      }).catch(() => {
        // 用户点击了"取消"按钮，不做任何操作
      })
    },
    
    // 保存分配到历史记录
    saveAllocationToHistory() {
      const timestamp = new Date();
      
      // 🔧 修复：从实际数据计算统计信息
      // 计算已分配学生总数（所有宿舍组中的学生）
      const assignedStudentsCount = this.dormGroups.reduce((total, group) => {
        return total + (group.memberCount || group.members?.length || 0)
      }, 0)
      
      // 计算未分配学生数
      const unassignedStudentsCount = this.remainingStudentsList?.length || 0
      
      // 总学生数 = 已分配 + 未分配
      const totalStudents = assignedStudentsCount + unassignedStudentsCount
      
      // 计算宿舍组数量
      const totalGroups = this.dormGroups.length
      const completeGroups = this.dormGroups.filter(g => 
        g.status === 'complete' || g.status === 'full' || g.status === 'room_selected'
      ).length
      const incompleteGroups = this.incompleteGroupsList?.length || 0
      
      // 计算空余房间和床位
      const emptyRooms = this.availableRoomsList?.filter(r => r.occupiedBeds === 0).length || 0
      
      // 🔧 修复：直接计算空余床位，而不是依赖计算属性
      // 1. 未满宿舍组的空床位
      const incompleteGroupBeds = (this.incompleteGroupsList || []).reduce((total, group) => {
        return total + Math.max(0, (group.maxMembers || 4) - (group.memberCount || 0))
      }, 0)
      
      // 2. 空房间的床位（完全空置的房间）
      const vacantRoomBeds = (this.availableRoomsList || []).reduce((total, room) => {
        if (room.occupiedBeds === 0) {
          return total + (room.capacity || 4)
        }
        return total
      }, 0)
      
      // 3. 总空余床位
      const emptyBeds = incompleteGroupBeds + vacantRoomBeds
      
      console.log('📊 [保存历史] 空余床位计算:', {
        incompleteGroupBeds,
        vacantRoomBeds,
        emptyBeds,
        incompleteGroupsCount: this.incompleteGroupsList?.length || 0,
        availableRoomsCount: this.availableRoomsList?.length || 0
      })
      
      // 获取专业列表
      const majors = [...new Set(this.majorAllocationStatus.map(m => m.majorName).filter(m => m))]
      
      // 🔧 修复：获取楼栋名称，使用正确的字段
      const buildingNames = this.managedBuildings.map(b => b.name || b.buildingName || `楼栋${b.id}`).filter(n => n)
      
      const allocationRecord = {
        // 基础信息
        id: `BATCH_${timestamp.getFullYear()}${(timestamp.getMonth() + 1).toString().padStart(2, '0')}${timestamp.getDate().toString().padStart(2, '0')}_${timestamp.getHours().toString().padStart(2, '0')}${timestamp.getMinutes().toString().padStart(2, '0')}`,
        batchName: `${timestamp.getFullYear()}年${timestamp.getMonth() + 1}月分配批次`,
        description: this.batchDescription || '常规新生宿舍分配',
        
        // 🔧 修复：添加timestamp字段，解决Invalid Date问题
        timestamp: timestamp.toISOString(),
        startTime: timestamp.toISOString(),
        endTime: timestamp.toISOString(),
        
        // 执行信息
        operatorId: this.currentUser?.username || 'admin',
        operatorName: this.currentUser?.realName || this.currentUser?.name || '管理员',
        allocationMode: this.computedAllocationMode,
        
        // 🔧 修复：范围信息，确保有数据
        targetBuildings: buildingNames.length > 0 ? buildingNames : ['未指定楼栋'],
        targetMajors: majors.length > 0 ? majors : ['未指定专业'],
        targetGrades: this.selectedGrades?.length > 0 ? this.selectedGrades.map(g => `${g}级`) : ['未指定年级'],
        
        // 🔧 修复：统计信息，使用实际数据
        statistics: {
          totalStudents: totalStudents,
          totalMajors: majors.length,
          selfGroupedDorms: totalGroups,
          adminAssignedCount: assignedStudentsCount,
          emptyRooms: emptyRooms,
          emptyDorms: emptyBeds
        },
        
        // 元数据
        metadata: {
          dataVersion: '1.2.0',
          systemVersion: '2.1.0',
          completeGroups: completeGroups,
          incompleteGroups: incompleteGroups,
          unassignedStudents: unassignedStudentsCount
        }
      };
      
      // 保存到本地存储
      const history = JSON.parse(localStorage.getItem('allocationHistory') || '[]');
      history.push(allocationRecord);
      localStorage.setItem('allocationHistory', JSON.stringify(history));
    },

    // 新UI方法（占位符，仅用于UI展示）
    getAvailableBeds() {
      // 计算未满宿舍组的可用床位数
      const incompleteBeds = this.incompleteGroupsList.reduce((total, group) => {
        return total + Math.max(0, (group.maxMembers || 4) - (group.memberCount || 0))
      }, 0)
      
      // TODO: 需要从后端API获取真实的空置房间数据
      // 目前只统计未满宿舍组的空床位
      return incompleteBeds
    },

    getProcessingProgress() {
      // 计算处理进度
      const totalIssues = this.remainingStudentsList.length + this.incompleteGroupsList.length
      if (totalIssues === 0) return 100
      const resolved = Math.max(0, 10 - totalIssues) // 模拟已解决的问题
      return Math.round((resolved / 10) * 100)
    },

    getAvatarColor(gender) {
      return gender === '男' ? '#409EFF' : '#F56C6C'
    },

    // 已合并到上面的 toggleStudentSelection 方法

    clearSelection() {
      this.selectedStudents = []
    },

    // 拖拽功能和快速操作已移除

    // 批量智能分配
    async batchAutoAssign() {
      if (this.selectedStudents.length === 0) {
        this.$message.warning('请先选择要分配的学生')
        return
      }
      
      // 清空可能残留的宿舍组上下文，避免影响学生选择
      this.selectedGroupDetails = null
      this.studentSelectionContext = null
      
      console.log('🎯 [批量智能分配] 开始')
      console.log('🏢 managedBuildings:', this.managedBuildings)
      console.log('🏢 managedBuildings.length:', this.managedBuildings.length)
      console.log('🏢 buildingIds:', this.managedBuildings.map(b => b.id))
      console.log('👥 selectedStudents:', this.selectedStudents)
      console.log('👥 selectedCount:', this.selectedStudents.length)
      
      // 检查楼栋ID是否有效
      const buildingIds = this.managedBuildings.map(b => b.id)
      if (!buildingIds || buildingIds.length === 0) {
        this.$message.error('未找到管理的楼栋，请刷新页面重试')
        console.error('❌ 楼栋ID为空!')
        return
      }
      
      const loading = this.$loading({
        lock: true,
        text: '正在批量分配中...',
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      try {
        const requestData = {
          studentIds: this.selectedStudents,
          buildingIds: this.managedBuildings.map(b => b.id),
          strategy: 'same_class',
          allowCrossMajor: false,
          allowCrossClass: false
        }
        
        console.log('📤 [批量智能分配] 发送请求', requestData)
        console.log('📤 [批量智能分配] API URL: /admin/allocation/intelligent-assign')
        console.log('📤 [批量智能分配] Method: POST')
        
        const res = await intelligentAssign(requestData)
        
        console.log('📥 [批量智能分配] 收到响应', res)
        
        loading.close()
        
        if (res.code === 200) {
          const result = res.data
          console.log('✅ [批量智能分配] 成功', {
            successCount: result.successCount,
            failedCount: result.failedCount,
            details: result
          })
          
          // 显示警告信息
          if (result.warnings && result.warnings.length > 0) {
            console.warn('⚠️ [批量智能分配] 警告信息', result.warnings)
            result.warnings.forEach(warning => {
              console.warn('  - ' + warning)
            })
          }
          
          if (result.failedCount > 0 && result.failureDetails) {
            console.warn('⚠️ [批量智能分配] 失败详情', result.failureDetails)
          }
          
          this.$message.success(`批量分配完成！成功：${result.successCount} 人，失败：${result.failedCount} 人`)
          
          // 清除选择
          this.selectedStudents = []
          
          // 刷新数据
          await this.loadStudentGroups()
          await this.loadRemainingData()
          await this.loadBackendStatistics()
        } else {
          console.error('❌ [批量智能分配] 失败', res)
          this.$message.error(res.message || '批量分配失败')
        }
      } catch (error) {
        loading.close()
        console.error('❌ [批量智能分配] 异常', error)
        this.$message.error('批量分配失败：' + (error.message || '网络错误'))
      }
    },

    // 分配到房间（点击房间）
    assignToRoom(room) {
      // 检查房间是否满员
      if (room.isFull || room.availableBeds === 0) {
        this.$message.warning(`房间 ${room.roomNumber} 已满员，无法继续分配学生`)
        return
      }
      
      this.selectedRoomForAssignment = room
      this.studentSelectionContext = 'room'
      this.studentSelectionDialogVisible = true
      this.selectedStudents = []
      console.log(`准备为房间 ${room.roomNumber} 分配学生`)
    },

    // 一键智能分配（通过全选学生后调用批量智能分配）
    async intelligentAutoAssign() {
      // 🔧 修复：使用正确的变量名 remainingStudentsList
      // 检查是否有待分配学生
      if (!this.remainingStudentsList || this.remainingStudentsList.length === 0) {
        this.$message.warning('没有待分配的学生')
        return
      }
      
      this.$confirm(
        `确定要执行一键智能分配吗？系统将自动为所有 ${this.remainingStudentsList.length} 名待分配学生匹配合适的宿舍。`, 
        '确认操作', 
        {
          confirmButtonText: '开始分配',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        // 🔧 修复：全选所有待分配学生的ID（不是学生对象）
        this.selectedStudents = this.remainingStudentsList.map(s => s.id)
        console.log('🎯 [一键智能分配] 已全选学生', this.selectedStudents.length, '人')
        
        // 🔧 调用批量智能分配功能
        await this.batchAutoAssign()
      }).catch(() => {
        // 用户取消
      })
    },


    // 单个学生智能分配
    async singleStudentAutoAssign(student) {
      const loading = this.$loading({
        lock: true,
        text: `正在为 ${student.name} 智能分配...`,
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      try {
        const res = await intelligentAssign({
          studentIds: [student.id],
          buildingIds: this.managedBuildings.map(b => b.id),
          strategy: 'same_class',
          allowCrossMajor: false,
          allowCrossClass: false
        })
        
        loading.close()
        
        if (res.code === 200) {
          const result = res.data
          if (result.successCount > 0) {
            this.$message.success(`成功为 ${student.name} 分配宿舍`)
            // 刷新数据
            await this.loadStudentGroups()
            await this.loadRemainingData()
            await this.loadBackendStatistics()
          } else {
            this.$message.warning(`未能为 ${student.name} 找到合适的宿舍`)
          }
        } else {
          this.$message.error(res.message || '智能分配失败')
        }
      } catch (error) {
        loading.close()
        console.error('智能分配失败:', error)
        this.$message.error('智能分配失败：' + (error.message || '网络错误'))
      }
    },


  }
}
</script>

<style scoped>
.student-allocation-page {
  padding: 12px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.header-actions h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-buttons {
  display: flex;
  gap: 12px;
  align-items: center;
}

.header-buttons .el-tag {
  font-weight: 500;
  padding: 8px 16px;
}

.header-buttons .el-button {
  font-weight: 500;
}

.phase-navigator {
  margin: 12px 0 16px 0;
  padding: 16px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

/* 步骤条样式增强 */
.custom-steps :deep(.el-step__title) {
  font-weight: 600;
  font-size: 15px;
  line-height: 1.4;
}

.custom-steps :deep(.el-step__description) {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.custom-steps :deep(.el-step.is-process .el-step__title) {
  color: #409eff;
  font-weight: 700;
}

.custom-steps :deep(.el-step.is-process .el-step__description) {
  color: #409eff;
  font-weight: 500;
}

.custom-steps :deep(.el-step__icon) {
  border-width: 2px;
}

.custom-steps :deep(.el-step.is-process .el-step__icon) {
  border-color: #409eff;
  background: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
}

.phase-content {
  margin-top: 8px;
}

.phase-actions {
  margin-top: 24px;
  text-align: center;
  padding: 20px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.phase-actions .hint-text {
  margin-top: 12px;
  color: #909399;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.primary-action-btn {
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.primary-action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.primary-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

/* 阶段1样式 */
.major-allocation-status {
  margin-bottom: 20px;
}

.allocation-status-card {
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-header-enhanced {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-left h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

/* 🔥 新增：年级筛选样式 */
.grade-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.grade-filter .filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
}

.grade-filter :deep(.el-checkbox-group) {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.grade-filter :deep(.el-checkbox.is-bordered) {
  padding: 6px 12px;
  border-radius: 4px;
  transition: all 0.3s;
}

.grade-filter :deep(.el-checkbox.is-bordered.is-checked) {
  background: #ecf5ff;
  border-color: #409eff;
}

.grade-filter :deep(.el-checkbox__label) {
  font-size: 13px;
  font-weight: 500;
}

.header-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.stat-item {
  font-size: 14px;
  color: #606266;
}

.stat-item strong {
  color: #409eff;
  font-weight: 600;
}

.stat-divider {
  color: #dcdfe6;
}

/* 表格样式增强 */
.enhanced-table {
  border-radius: 6px;
  overflow: hidden;
  width: 100%;
}

.enhanced-table :deep(.el-table__header-wrapper) {
  background: #f8f9fa;
}

.enhanced-table :deep(.el-table__header th) {
  background: #f8f9fa;
  color: #303133;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #e4e7ed;
  padding: 12px 0;
}

.enhanced-table :deep(.el-table__body tr) {
  transition: background-color 0.2s ease;
}

.enhanced-table :deep(.el-table__body tr:hover) {
  background-color: #f5f7fa !important;
}

.enhanced-table :deep(.el-table__body td) {
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
}

.major-name {
  font-weight: 500;
  color: #303133;
}

.number-value {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
  font-weight: 600;
  color: #409EFF;
}

.capacity-number {
  color: #67c23a;
}

.class-number {
  color: #e6a23c;
}

.status-tag {
  font-weight: 500;
  border: none;
  padding: 4px 12px;
}

.status-tag.el-tag--success {
  background: #f0f9ff;
  color: #067f23;
  border: 1px solid #a7f3d0;
}

.status-tag.el-tag--danger {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

/* 阶段2样式 */
.header-with-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.time-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-label {
  color: #606266;
  font-size: 14px;
}

.time-range {
  font-weight: 500;
}

.grouping-overview {
  margin-top: 20px;
}

/* 统计卡片居中对齐 */
.grouping-overview .el-col {
  text-align: center;
}

.grouping-overview .el-statistic {
  text-align: center;
}

.grouping-overview .el-statistic__head {
  text-align: center;
  margin-bottom: 8px;
}

.grouping-overview .el-statistic__content {
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: baseline;
  flex-wrap: wrap;
}

.grouping-overview .el-statistic__number {
  text-align: center;
}

/* 宿舍组管理样式 */
.dormitory-groups-management {
  margin-top: 20px;
}

.management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.management-header h4 {
  margin: 0;
  color: #606266;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 阶段3样式 */
.selection-overview {
  margin-bottom: 20px;
}

.selection-reminder {
  margin-top: 20px;
}

.selection-stat {
  display: flex;
  align-items: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-icon {
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 24px;
  color: #409EFF;
}

.stat-content .stat-number {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}

.stat-content .stat-label {
  color: #909399;
  font-size: 14px;
}

.hint-text {
  margin-top: 10px;
  color: #909399;
  font-size: 14px;
  text-align: center;
}

.deadline-info {
  margin-top: 15px;
}


/* 阶段4样式 */
.processing-overview {
  margin-bottom: 20px;
}

.empty-state {
  padding: 20px;
  text-align: center;
}

.incomplete-groups, .remaining-students {
  min-height: 200px;
}

.batch-processing {
  margin-top: 20px;
}

.batch-actions .el-row {
  margin-bottom: 10px;
}

/* 阶段5样式 */
.final-statistics {
  margin-top: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

/* 操作按钮样式 */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
}

/* 对话框样式 */
.manual-assign-content {
  min-height: 600px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 手动分配工具样式 */
.student-list {
  max-height: 500px;
  overflow-y: auto;
}

.student-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.student-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.student-info {
  margin-bottom: 8px;
}

.student-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.student-details {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.student-id {
  color: #909399;
  font-size: 12px;
}

.student-major {
  color: #606266;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  flex: 1;
}

.group-list {
  max-height: 500px;
  overflow-y: auto;
}

.group-item {
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fff;
  transition: all 0.3s ease;
}

.incomplete-group {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.incomplete-group:hover {
  border-color: #e6a23c;
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.2);
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
}

.group-name {
  font-weight: 600;
  color: #303133;
  font-size: 16px;
  margin-bottom: 2px;
}

.group-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.needed {
  color: #e6a23c;
  font-size: 12px;
}

.group-info {
  margin-bottom: 12px;
  flex: 1;
}

.group-content {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.group-left {
  flex: 1;
  min-width: 0;
}

.group-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.group-details {
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
  line-height: 1.4;
}

.group-members {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.member-tag {
  margin: 0;
}

.drop-zone {
  border: 2px dashed #c0c4cc;
  border-radius: 4px;
  padding: 8px;
  text-align: center;
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 32px;
  transition: all 0.3s ease;
}

.incomplete-group:hover .drop-zone {
  border-color: #e6a23c;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
}

.tools-panel {
  padding: 0;
}

.tool-section {
  margin-bottom: 24px;
}

.tool-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.tool-buttons {
  width: 100%;
}

.tool-buttons .el-button {
  flex: 1;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-number {
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.stat-label {
  color: #909399;
  font-size: 12px;
}

.operation-history {
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.history-item:last-child {
  border-bottom: none;
}

.history-time {
  color: #909399;
  font-size: 11px;
  margin-bottom: 2px;
}

.history-action {
  color: #606266;
  font-size: 12px;
}

.empty-history {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
  padding: 20px 0;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
}

.dialog-footer {
  text-align: right;
}

/* ===== 新设计的剩余处理阶段样式 ===== */

/* 顶部统计概览 */
.processing-header {
  margin-bottom: 24px;
}

.stats-overview {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
}

.stats-grid-three {
  grid-template-columns: repeat(3, 1fr);
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 24px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-right: 1px solid #e4e7ed;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
  position: relative;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.stat-card:last-child {
  border-right: none;
}

.stat-card:hover {
  background: #f8f9fa;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card.unassigned {
  border-left: 4px solid #e6a23c;
  border-bottom-color: #e6a23c;
  background: linear-gradient(135deg, #fef7e6 0%, #ffffff 100%);
}

.stat-card.incomplete {
  border-left: 4px solid #f56c6c;
  border-bottom-color: #f56c6c;
  background: linear-gradient(135deg, #fef0f0 0%, #ffffff 100%);
}

.stat-card.available {
  border-left: 4px solid #67c23a;
  border-bottom-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
}

.stat-card.progress {
  border-left: 4px solid #409eff;
  border-bottom-color: #409eff;
  background: linear-gradient(135deg, #e6f7ff 0%, #ffffff 100%);
}

.stat-icon {
  margin-right: 16px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(64, 158, 255, 0.1);
}

.stat-card.unassigned .stat-icon {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}

.stat-card.incomplete .stat-icon {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.stat-card.available .stat-icon {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}

.stat-card.progress .stat-icon {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}

.stat-icon .el-icon {
  font-size: 24px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

/* 主工作区域 */
.processing-workspace {
  margin-bottom: 24px;
}

/* 面板通用样式 - 这些class就是el-card本身 */
.students-panel,
.dormitory-panel {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden !important; /* 强制隐藏溢出 */
  height: 600px !important; /* 强制固定高度 */
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex !important;
  flex-direction: column !important;
}

/* 确保El-Card内容区域也能正确处理滚动 */
.students-panel :deep(.el-card__body) {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
  padding: 0 !important;
  min-height: 0 !important; /* 关键：允许flex子元素缩小 */
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  flex-wrap: nowrap;
  gap: 12px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  min-width: 0;
  flex-shrink: 0;
}

.header-title .el-icon {
  font-size: 18px;
  color: #409eff;
}

.student-count-badge {
  margin-left: 8px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 学生面板样式 */
.students-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0; /* 确保flex容器可以收缩 */
}

.students-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;  /* 🔥 修复：增加左右padding从8px到16px */
  padding-bottom: 0;
  max-height: calc(600px - 120px); /* 减去header和padding的高度 */
  min-height: 0; /* 确保flex子元素可以收缩 */
}

.student-card {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 6px;
  background: #fff;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  min-height: 50px;
}

.student-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.student-card.selected {
  border-color: #409eff;
  background: #f0f9ff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.student-avatar {
  margin-right: 10px;
}

.student-info {
  flex: 1;
  min-width: 0;
}

.student-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.student-main-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
}

.gender-tag {
  flex-shrink: 0;
}

.student-details {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
}

.student-id {
  font-size: 12px;
  color: #909399;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
}

.student-class {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 批量操作栏 */
.batch-operations {
  padding: 16px;
  background: #f8f9fa;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.batch-info {
  font-size: 14px;
  color: #606266;
}

.batch-info strong {
  color: #409eff;
  font-weight: 600;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

/* 宿舍分配面板样式 */
.dormitory-panel :deep(.el-card__body) {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
  padding: 0 !important; /* 强制覆盖element-plus默认padding */
  min-height: 0 !important; /* 关键：允许flex子元素缩小 */
}

.incomplete-groups-view,
.available-rooms-view {
  flex: 1 !important;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  padding: 16px;
  min-height: 0 !important; /* 关键：允许flex子元素缩小并触发滚动 */
  max-height: 100% !important; /* 限制最大高度 */
}

/* 未满宿舍组网格 */
.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.groups-grid .group-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
  position: relative;
  min-height: 120px;
  box-sizing: border-box;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}

.groups-grid .group-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  border-color: #409eff;
}

.group-card:hover {
  border-color: #e6a23c;
  box-shadow: 0 4px 12px rgba(230, 162, 60, 0.15);
  transform: translateY(-2px);
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.group-capacity {
  flex-shrink: 0;
}

.group-capacity .el-tag {
  font-weight: 500;
}

.group-info {
  margin-bottom: 12px;
}

.group-class {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.group-leader {
  font-size: 12px;
  color: #909399;
}

.room-type {
  font-size: 11px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 4px;
  display: inline-block;
}

.room-type-small {
  font-size: 10px;
  color: #909399;
  margin-left: 8px;
}

.group-members {
  margin-bottom: 12px;
}

.members-list {
  display: grid;
  gap: 6px;
  margin-bottom: 8px;
}

/* 根据房间容量调整网格布局 */
.members-list.capacity-2 {
  grid-template-columns: repeat(2, 1fr);
}

.members-list.capacity-4 {
  grid-template-columns: repeat(2, 1fr);
}

.members-list.capacity-6 {
  grid-template-columns: repeat(3, 1fr);
}

.members-list.capacity-8 {
  grid-template-columns: repeat(4, 1fr);
}

.member-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: #f0f9ff;
  border-radius: 6px;
  font-size: 12px;
}

.member-name {
  color: #409eff;
  font-weight: 500;
}

.empty-beds {
  display: grid;
  gap: 8px;
}

/* 空床位网格布局 */
.empty-beds.capacity-2 {
  grid-template-columns: repeat(2, 1fr);
}

.empty-beds.capacity-4 {
  grid-template-columns: repeat(2, 1fr);
}

.empty-beds.capacity-6 {
  grid-template-columns: repeat(3, 1fr);
}

.empty-beds.capacity-8 {
  grid-template-columns: repeat(4, 1fr);
}

.empty-bed {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #fef0e6;
  border: 1px dashed #e6a23c;
  border-radius: 6px;
  font-size: 11px;
  color: #e6a23c;
  cursor: pointer;
  transition: all 0.3s ease;
  opacity: 0.7;
}

.empty-bed:hover {
  opacity: 1;
  background: #fff7e6;
  border-color: #d48806;
  color: #d48806;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(212, 136, 6, 0.2);
}

.empty-bed .el-icon {
  font-size: 12px;
}

.group-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  justify-content: center;
}

.group-actions .el-button {
  font-size: 12px;
  padding: 6px 12px;
}

.drop-zone {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: rgba(230, 162, 60, 0.05);
  border-top: 1px dashed #e6a23c;
  border-radius: 0 0 12px 12px;
  text-align: center;
  font-size: 11px;
  color: #e6a23c;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.group-card:hover .drop-zone {
  opacity: 1;
}

.drop-zone .el-icon {
  font-size: 12px;
  margin-right: 4px;
}

/* 房间筛选样式 */
.room-filters {
  margin-bottom: 16px;
  
  .class-filter {
    width: 200px;
  }
}

/* 紧凑房间网格 */
.rooms-compact-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  padding: 16px 0;
  max-height: 500px;
  overflow-y: auto;
  
  /* 滚动条样式 */
  &::-webkit-scrollbar {
    width: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #888;
    border-radius: 4px;
    
    &:hover {
      background: #555;
    }
  }
}

.room-compact-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 12px;
  background: white;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  
  &:hover {
  transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    
    .room-hover-info {
      opacity: 1;
    }
  }
  
  &.drag-over {
    border-color: #67c23a;
    background-color: #f0f9ff;
  }
  
  /* 房间状态颜色 */
  &.room-empty {
    border-color: #67c23a;
    background: linear-gradient(135deg, #f0f9ff 0%, #e8f5e8 100%);
  }
  
  &.room-partial {
    border-color: #409eff;
    background: linear-gradient(135deg, #ecf5ff 0%, #e1f0ff 100%);
  }
  
  &.room-full {
    border-color: #f56c6c;
    background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
    cursor: not-allowed;  /* 满员房间不可点击 */
    opacity: 0.8;  /* 降低透明度 */
    
    &:hover {
      transform: none;  /* 禁用悬停效果 */
      box-shadow: none;
    }
  }
}

.room-compact-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .room-number {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
  
  .room-building {
    font-size: 12px;
    color: #909399;
  }
}

.room-compact-info {
  margin: 4px 0;
  
  .room-type {
    font-size: 12px;
    color: #606266;
    background: #f4f4f5;
    padding: 2px 8px;
    border-radius: 4px;
    display: inline-block;
  }
}

.room-compact-status {
  margin-top: 8px;
  
  .el-tag {
    font-size: 11px;
  }
}

.room-hover-info {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(64, 158, 255, 0.9);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.room-actions {
  margin-bottom: 12px;
}

.room-card .drop-zone {
  background: rgba(103, 194, 58, 0.05);
  border-top-color: #67c23a;
  color: #67c23a;
}

/* 底部操作区域 */
.processing-footer {
  margin-top: 24px;
}

.actions-panel {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
}

.quick-actions {
  display: flex;
  gap: 16px;
}

.quick-actions .el-button {
  padding: 12px 24px;
  font-weight: 600;
  border-radius: 8px;
}

.validation-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 宿舍组详情对话框样式 */
.group-details-dialog {
  .el-dialog__body {
    padding: 20px;
    max-height: 70vh;
    overflow-y: auto;
  }
}

.group-details-content {
  .details-card {
    margin-bottom: 20px;
    border: 1px solid #e4e7ed;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .el-card__header {
      background-color: #f8f9fa;
      border-bottom: 1px solid #e4e7ed;
    }
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .card-title {
        font-weight: 600;
        font-size: 16px;
        color: #303133;
      }
    }
  }
}

.basic-info {
  .info-row {
    display: flex;
    margin-bottom: 15px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .info-item {
      flex: 1;
      display: flex;
      align-items: center;
      
      label {
        font-weight: 500;
        color: #606266;
        margin-right: 8px;
        min-width: 80px;
      }
      
      span {
        color: #303133;
      }
    }
  }
}

.members-info {
  .members-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 15px;
  }
  
  .member-card {
    display: flex;
    padding: 15px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    background-color: #fafafa;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
    }
    
    .member-avatar {
      position: relative;
      margin-right: 15px;
      
      .leader-tag {
        position: absolute;
        top: -5px;
        right: -5px;
        z-index: 1;
      }
    }
    
    .member-info {
      flex: 1;
      
      .member-name {
        font-weight: 600;
        font-size: 16px;
        color: #303133;
        margin-bottom: 8px;
      }
      
      .member-details {
        .detail-item {
          font-size: 12px;
          color: #909399;
          margin-bottom: 3px;
          
          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }
    
    .member-actions {
      display: flex;
      flex-direction: column;
      gap: 5px;
      align-items: flex-start;
      
      .el-button {
        width: 80px;
        margin: 0 !important;
        padding: 5px 15px;
      }
    }
  }
  
  .empty-member-slot {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    background-color: #f5f7fa;
    min-height: 120px;
    
    .empty-avatar {
      color: #c0c4cc;
      margin-bottom: 8px;
    }
    
    .empty-text {
      color: #909399;
      font-size: 14px;
    }
  }
}



.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stat-card {
    border-bottom: 1px solid #f0f2f5;
  }
  
  .stat-card:nth-child(2n) {
    border-right: none;
  }
  
  .stat-card:nth-child(n+3) {
    border-bottom: none;
  }
}

@media (max-width: 768px) {
  .processing-workspace .el-row {
    flex-direction: column;
  }
  
  .processing-workspace .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }
  
  .students-panel,
  .dormitory-panel {
    height: 400px;
  }
  
  .footer-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .quick-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
}
/* 楼栋选择样式 */
.building-scope-card {
  margin-bottom: 24px;
}

.building-selection {
  padding: 0;
}

.selection-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.selection-label {
  font-weight: 500;
  color: #303133;
}

.buildings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.building-option {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #ffffff;
}

.building-option:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.building-option.selected {
  border-color: #409eff;
  background: linear-gradient(135deg, #e6f7ff 0%, #ffffff 100%);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.building-icon {
  font-size: 24px;
  margin-right: 12px;
  flex-shrink: 0;
}

.building-info {
  flex: 1;
}

.building-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.building-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.selection-indicator {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #409eff;
  color: white;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.building-option.selected .selection-indicator {
  opacity: 1;
}

.selection-summary {
  margin-top: 16px;
}

.allocation-scope-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 学生选择对话框样式 */
.student-selection-content {
  .selection-header {
    margin-bottom: 20px;
    
    .search-and-filters {
      display: flex;
      align-items: center;
      gap: 20px;
      
      .search-input {
        flex: 1;
        min-width: 200px;
      }
      
      .filter-options {
        display: flex;
        gap: 20px;
        flex-wrap: wrap;
        
        .el-checkbox {
          margin-right: 0;
        }
      }
    }
  }
  
  .students-list {
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;
      padding-bottom: 10px;
      border-bottom: 1px solid #ebeef5;
      
      span {
        font-weight: 600;
        color: #303133;
      }
    }
    
    .students-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 15px;
      max-height: 400px;
      overflow-y: auto;
      
      .student-item {
        display: flex;
        align-items: center;
        padding: 15px;
        border: 2px solid #ebeef5;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.3s ease;
        background-color: #fff;
        
        &:hover {
          border-color: #c6e2ff;
          background-color: #f5f9ff;
        }
        
        &.selected {
          border-color: #409eff;
          background-color: #ecf5ff;
        }
        
        .el-checkbox {
          margin-right: 12px;
        }
        
        .student-avatar {
          margin-right: 12px;
        }
        
        .student-info {
          flex: 1;
          
          .student-name {
            font-weight: 600;
            color: #303133;
            margin-bottom: 4px;
          }
          
          .student-id {
            font-size: 12px;
            color: #909399;
            margin-bottom: 4px;
          }
          
          .student-class {
            font-size: 12px;
            color: #606266;
            margin-bottom: 6px;
          }
          
          .student-status {
            .el-tag {
              font-size: 11px;
            }
          }
        }
      }
    }
  }
}

/* ========== 开发调试面板样式 ========== */
.dev-debug-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 280px;
  
  .debug-panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 8px 8px 0 0;
    cursor: pointer;
    user-select: none;
    
    span {
      font-weight: 600;
      font-size: 14px;
    }
    
    .el-button {
      color: white;
      
      &:hover {
        background: rgba(255, 255, 255, 0.1);
      }
    }
  }
  
  .debug-panel-content {
    padding: 16px;
    
    .phase-buttons {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 12px;
      
      .el-button {
        width: 100%;
        justify-content: center;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateX(2px);
        }
      }
    }
    
    .current-phase-info {
      padding: 8px;
      background: #f5f7fa;
      border-radius: 4px;
      text-align: center;
      font-size: 12px;
      color: #606266;
      font-weight: 500;
    }
  }
}

</style>

