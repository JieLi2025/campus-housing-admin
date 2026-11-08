<template>
  <div class="floor-plan-view">
    <div class="floor-plan-header">
      <div class="header-left">
        <h3 class="floor-title">
          <el-icon><OfficeBuilding /></el-icon>
          {{ buildingName }} {{ floor }}层平面图
        </h3>
        <div class="floor-stats">
          <span class="stat-item">
            <el-icon><Grid /></el-icon>
            {{ totalRooms }}间宿舍
          </span>
          <span class="stat-item">
            <el-icon><User /></el-icon>
            {{ totalStudents }}名学生
          </span>
          <span class="stat-item">
            <el-icon><Reading /></el-icon>
            {{ majorCount }}个专业
          </span>
        </div>
      </div>
      
      <div class="header-actions">
        <el-button-group>
          <el-button 
            :type="viewMode === 'major' ? 'primary' : ''" 
            @click="viewMode = 'major'"
            size="small"
          >
            <el-icon><Collection /></el-icon>
            按专业查看
          </el-button>
          <el-button 
            :type="viewMode === 'class' ? 'primary' : ''" 
            @click="viewMode = 'class'"
            size="small"
          >
            <el-icon><Files /></el-icon>
            按班级查看
          </el-button>
          <el-button 
            :type="viewMode === 'gender' ? 'primary' : ''" 
            @click="viewMode = 'gender'"
            size="small"
          >
            <el-icon><Male /></el-icon>
            按性别查看
          </el-button>
        </el-button-group>
        
        <el-button @click="showLegend = !showLegend" size="small">
          <el-icon><View /></el-icon>
          {{ showLegend ? '隐藏' : '显示' }}图例
        </el-button>
      </div>
    </div>

    <!-- 图例 -->
    <div v-if="showLegend" class="legend-panel">
      <div class="legend-title">
        <el-icon><InfoFilled /></el-icon>
        图例说明
      </div>
      <div class="legend-content">
        <div v-if="viewMode === 'major'" class="legend-items">
          <div 
            v-for="(major, index) in majorList" 
            :key="major"
            class="legend-item"
          >
            <div 
              class="legend-color" 
              :style="{ backgroundColor: getMajorColor(major) }"
            ></div>
            <span class="legend-label">{{ major }}</span>
            <span class="legend-count">({{ getMajorStudentCount(major) }}人)</span>
          </div>
        </div>
        
        <div v-else-if="viewMode === 'gender'" class="legend-items">
          <div class="legend-item">
            <div class="legend-color" style="background-color: #409eff;"></div>
            <span class="legend-label">男生宿舍</span>
          </div>
          <div class="legend-item">
            <div class="legend-color" style="background-color: #f56c6c;"></div>
            <span class="legend-label">女生宿舍</span>
          </div>
          <div class="legend-item">
            <div class="legend-color" style="background-color: #e4e7ed;"></div>
            <span class="legend-label">空置房间</span>
          </div>
        </div>
        
        <div v-else class="legend-items">
          <div class="legend-item">
            <div class="legend-color" style="background-color: #67c23a;"></div>
            <span class="legend-label">已分配</span>
          </div>
          <div class="legend-item">
            <div class="legend-color" style="background-color: #e4e7ed;"></div>
            <span class="legend-label">空置</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 楼层平面图 -->
    <div class="floor-plan-container">
      <div class="floor-grid" :style="gridStyle">
        <div
          v-for="room in roomLayout"
          :key="room.id"
          :class="[
            'room-cell',
            getRoomClass(room),
            { 'selected': selectedRooms.includes(room.id) },
            { 'dragging-over': isDraggingOver(room.id) }
          ]"
          :style="getRoomStyle(room)"
          @click="selectRoom(room)"
          @dragover.prevent="onDragOver($event, room)"
          @dragleave="onDragLeave($event, room)"
          @drop="onDrop($event, room)"
        >
          <div class="room-number">{{ room.roomNumber }}</div>
          <div class="room-info">
            <div class="occupancy-info">
              {{ room.occupiedBeds }}/{{ room.totalBeds }}
            </div>
            <div v-if="room.assignedMajor" class="room-major">
              {{ room.assignedMajor }}
            </div>
            <div v-if="room.assignedClass && viewMode === 'class'" class="room-class">
              {{ room.assignedClass }}
            </div>
          </div>
          
          <!-- 房间状态指示器 -->
          <div class="room-indicators">
            <div 
              v-if="room.gender" 
              :class="['gender-indicator', room.gender === '男' ? 'male' : 'female']"
            >
              {{ room.gender === '男' ? '♂' : '♀' }}
            </div>
            <div 
              v-if="room.hasSpecialNeeds" 
              class="special-indicator"
              title="有特殊需求学生"
            >
              <el-icon><Warning /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 选择工具栏 -->
    <div v-if="selectedRooms.length > 0" class="selection-toolbar">
      <div class="selection-info">
        已选择 {{ selectedRooms.length }} 个房间
      </div>
      <div class="selection-actions">
        <el-button size="small" @click="mergeSelectedRooms">
          <el-icon><Connection /></el-icon>
          合并为区域
        </el-button>
        <el-button size="small" @click="assignMajorToSelected">
          <el-icon><UserFilled /></el-icon>
          指定专业
        </el-button>
        <el-button size="small" type="danger" @click="clearSelection">
          <el-icon><Close /></el-icon>
          取消选择
        </el-button>
      </div>
    </div>

    <!-- 房间详情弹窗 -->
    <el-dialog
      v-model="roomDetailVisible"
      :title="`房间 ${selectedRoom?.roomNumber} 详情`"
      width="600px"
    >
      <div v-if="selectedRoom" class="room-detail-content">
        <div class="detail-section">
          <h4>基本信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="房间号">{{ selectedRoom.roomNumber }}</el-descriptions-item>
            <el-descriptions-item label="总床位">{{ selectedRoom.totalBeds }}</el-descriptions-item>
            <el-descriptions-item label="已住人数">{{ selectedRoom.occupiedBeds }}</el-descriptions-item>
            <el-descriptions-item label="剩余床位">{{ selectedRoom.totalBeds - selectedRoom.occupiedBeds }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ selectedRoom.gender || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ selectedRoom.assignedMajor || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="班级" v-if="selectedRoom.assignedClass">
              {{ selectedRoom.assignedClass }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div v-if="selectedRoom.residents && selectedRoom.residents.length > 0" class="detail-section">
          <h4>住宿学生</h4>
          <el-table :data="selectedRoom.residents" size="small">
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="studentId" label="学号" />
            <el-table-column prop="major" label="专业" />
            <el-table-column prop="class" label="班级" />
            <el-table-column prop="bedNumber" label="床位号" />
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 专业分配对话框 -->
    <el-dialog
      v-model="assignMajorVisible"
      title="为选中房间分配专业"
      width="500px"
    >
      <el-form :model="assignForm" label-width="80px">
        <el-form-item label="专业">
          <el-select v-model="assignForm.major" placeholder="选择专业" style="width: 100%">
            <el-option
              v-for="major in availableMajors"
              :key="major"
              :label="major"
              :value="major"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="assignForm.gender">
            <el-radio label="男">男生</el-radio>
            <el-radio label="女">女生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="区域名称">
          <el-input v-model="assignForm.areaName" placeholder="可选，为这组房间命名" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="assignMajorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssignMajor">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, watch } from 'vue'
import {
  OfficeBuilding,
  Grid,
  User,
  Reading,
  Collection,
  Files,
  Male,
  View,
  InfoFilled,
  Warning,
  Connection,
  UserFilled,
  Close
} from '@element-plus/icons-vue'

export default {
  name: 'FloorPlanView',
  components: {
    OfficeBuilding,
    Grid,
    User,
    Reading,
    Collection,
    Files,
    Male,
    View,
    InfoFilled,
    Warning,
    Connection,
    UserFilled,
    Close
  },
  props: {
    buildingName: {
      type: String,
      default: '宿舍楼'
    },
    floor: {
      type: Number,
      required: true
    },
    rooms: {
      type: Array,
      default: () => []
    },
    allocation: {
      type: Object,
      default: () => ({})
    }
  },
  emits: ['room-select', 'room-assign', 'area-create'],
  setup(props, { emit }) {
    // 响应式数据
    const viewMode = ref('major') // major, class, gender
    const showLegend = ref(true)
    const selectedRooms = ref([])
    const selectedRoom = ref(null)
    const roomDetailVisible = ref(false)
    const assignMajorVisible = ref(false)
    const dragOverRoom = ref(null)
    
    const assignForm = reactive({
      major: '',
      gender: '',
      areaName: ''
    })
    
    // 专业颜色映射
    const majorColors = {
      '计算机科学与技术': '#409eff',
      '软件工程': '#67c23a',
      '数据科学与大数据技术': '#e6a23c',
      '人工智能': '#f56c6c',
      '网络工程': '#909399',
      '信息安全': '#b37feb'
    }
    
    // 计算属性
    const roomLayout = computed(() => {
      return props.rooms.map(room => ({
        ...room,
        x: parseInt(room.roomNumber.slice(-2)) % 10 || 10,
        y: Math.floor(parseInt(room.roomNumber.slice(-2)) / 10) + 1
      }))
    })
    
    const gridStyle = computed(() => {
      const maxX = Math.max(...roomLayout.value.map(r => r.x), 10)
      const maxY = Math.max(...roomLayout.value.map(r => r.y), 3)
      
      return {
        gridTemplateColumns: `repeat(${maxX}, 1fr)`,
        gridTemplateRows: `repeat(${maxY}, 1fr)`
      }
    })
    
    const totalRooms = computed(() => props.rooms.length)
    
    const totalStudents = computed(() => {
      return props.rooms.reduce((sum, room) => sum + (room.occupiedBeds || 0), 0)
    })
    
    const majorList = computed(() => {
      const majors = new Set()
      props.rooms.forEach(room => {
        if (room.assignedMajor) {
          majors.add(room.assignedMajor)
        }
      })
      return Array.from(majors)
    })
    
    const majorCount = computed(() => majorList.value.length)
    
    const availableMajors = computed(() => {
      // 从props.allocation或其他数据源获取可用专业列表
      return Object.keys(majorColors)
    })
    
    // 方法
    const getMajorColor = (major) => {
      return majorColors[major] || '#909399'
    }
    
    const getMajorStudentCount = (major) => {
      return props.rooms.reduce((count, room) => {
        return room.assignedMajor === major ? count + (room.occupiedBeds || 0) : count
      }, 0)
    }
    
    const getRoomClass = (room) => {
      const classes = []
      
      if (room.status === 'maintenance') {
        classes.push('maintenance')
      } else if (room.status === 'disabled') {
        classes.push('disabled')
      } else if (room.occupiedBeds >= room.totalBeds) {
        classes.push('full')
      } else if (room.occupiedBeds > 0) {
        classes.push('partial')
      } else {
        classes.push('empty')
      }
      
      if (room.gender) {
        classes.push(`gender-${room.gender === '男' ? 'male' : 'female'}`)
      }
      
      return classes
    }
    
    const getRoomStyle = (room) => {
      const style = {
        gridColumn: room.x,
        gridRow: room.y
      }
      
      if (viewMode.value === 'major' && room.assignedMajor) {
        style.backgroundColor = getMajorColor(room.assignedMajor)
      } else if (viewMode.value === 'gender') {
        if (room.gender === '男') {
          style.backgroundColor = '#409eff'
        } else if (room.gender === '女') {
          style.backgroundColor = '#f56c6c'
        }
      }
      
      return style
    }
    
    const selectRoom = (room) => {
      const index = selectedRooms.value.indexOf(room.id)
      
      if (index > -1) {
        selectedRooms.value.splice(index, 1)
      } else {
        selectedRooms.value.push(room.id)
      }
      
      emit('room-select', {
        room,
        selected: index === -1,
        selectedRooms: selectedRooms.value
      })
    }
    
    const showRoomDetail = (room) => {
      selectedRoom.value = room
      roomDetailVisible.value = true
    }
    
    const clearSelection = () => {
      selectedRooms.value = []
    }
    
    const mergeSelectedRooms = () => {
      if (selectedRooms.value.length < 2) {
        return
      }
      
      // 触发区域创建事件
      emit('area-create', {
        roomIds: selectedRooms.value,
        rooms: props.rooms.filter(room => selectedRooms.value.includes(room.id))
      })
    }
    
    const assignMajorToSelected = () => {
      if (selectedRooms.value.length === 0) {
        return
      }
      
      assignForm.major = ''
      assignForm.gender = ''
      assignForm.areaName = ''
      assignMajorVisible.value = true
    }
    
    const confirmAssignMajor = () => {
      if (!assignForm.major || !assignForm.gender) {
        return
      }
      
      emit('room-assign', {
        roomIds: selectedRooms.value,
        major: assignForm.major,
        gender: assignForm.gender,
        areaName: assignForm.areaName
      })
      
      assignMajorVisible.value = false
      clearSelection()
    }
    
    // 拖拽相关方法
    const isDraggingOver = (roomId) => {
      return dragOverRoom.value === roomId
    }
    
    const onDragOver = (event, room) => {
      event.preventDefault()
      dragOverRoom.value = room.id
    }
    
    const onDragLeave = (event, room) => {
      if (!event.currentTarget.contains(event.relatedTarget)) {
        dragOverRoom.value = null
      }
    }
    
    const onDrop = (event, room) => {
      event.preventDefault()
      dragOverRoom.value = null
      
      // 处理学生拖拽到房间的逻辑
      const studentData = event.dataTransfer.getData('application/json')
      if (studentData) {
        try {
          const student = JSON.parse(studentData)
          emit('student-drop', { student, room })
        } catch (error) {
          console.error('拖拽数据解析失败:', error)
        }
      }
    }
    
    return {
      viewMode,
      showLegend,
      selectedRooms,
      selectedRoom,
      roomDetailVisible,
      assignMajorVisible,
      assignForm,
      roomLayout,
      gridStyle,
      totalRooms,
      totalStudents,
      majorList,
      majorCount,
      availableMajors,
      getMajorColor,
      getMajorStudentCount,
      getRoomClass,
      getRoomStyle,
      selectRoom,
      showRoomDetail,
      clearSelection,
      mergeSelectedRooms,
      assignMajorToSelected,
      confirmAssignMajor,
      isDraggingOver,
      onDragOver,
      onDragLeave,
      onDrop
    }
  }
}
</script>

<style scoped>
.floor-plan-view {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
}

/* 头部区域 */
.floor-plan-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e4e7ed;
}

.header-left {
  flex: 1;
}

.floor-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.floor-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 图例面板 */
.legend-panel {
  background: white;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.legend-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}

.legend-content {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid #ddd;
}

.legend-label {
  color: #303133;
}

.legend-count {
  color: #909399;
  font-size: 12px;
}

/* 楼层平面图 */
.floor-plan-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow-x: auto;
}

.floor-grid {
  display: grid;
  gap: 8px;
  min-width: 800px;
  min-height: 400px;
}

.room-cell {
  position: relative;
  min-height: 80px;
  background: #f0f0f0;
  border: 2px solid #ddd;
  border-radius: 8px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.room-cell:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.room-cell.selected {
  border-color: #409eff;
  background: #e6f4ff;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.3);
}

.room-cell.dragging-over {
  border-color: #67c23a;
  background: #f0f9ff;
  border-style: dashed;
}

.room-cell.full {
  background: #fef0f0;
  border-color: #f56c6c;
}

.room-cell.partial {
  background: #fff7e6;
  border-color: #e6a23c;
}

.room-cell.empty {
  background: #f0f9ff;
  border-color: #409eff;
}

.room-cell.maintenance {
  background: #f5f5f5;
  border-color: #909399;
  opacity: 0.7;
}

.room-cell.disabled {
  background: #fafafa;
  border-color: #c0c4cc;
  opacity: 0.5;
}

.room-number {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  text-align: center;
}

.room-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
}

.occupancy-info {
  font-size: 12px;
  color: #606266;
  text-align: center;
  font-weight: 500;
}

.room-major,
.room-class {
  font-size: 11px;
  color: #909399;
  text-align: center;
  line-height: 1.2;
  word-break: break-all;
}

.room-indicators {
  position: absolute;
  top: 5px;
  right: 5px;
  display: flex;
  gap: 4px;
}

.gender-indicator {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 10px;
  font-weight: 600;
}

.gender-indicator.male {
  background: #409eff;
}

.gender-indicator.female {
  background: #f56c6c;
}

.special-indicator {
  width: 16px;
  height: 16px;
  background: #e6a23c;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 10px;
}

/* 选择工具栏 */
.selection-toolbar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 20px;
  z-index: 1000;
}

.selection-info {
  font-weight: 500;
  color: #303133;
}

.selection-actions {
  display: flex;
  gap: 10px;
}

/* 房间详情 */
.room-detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .floor-grid {
    min-width: 600px;
  }
  
  .room-cell {
    min-height: 60px;
  }
}

@media (max-width: 768px) {
  .floor-plan-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .header-actions {
    align-self: stretch;
  }
  
  .floor-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .legend-content {
    justify-content: center;
  }
  
  .floor-grid {
    min-width: 400px;
    gap: 4px;
  }
  
  .room-cell {
    min-height: 50px;
    padding: 4px;
  }
  
  .selection-toolbar {
    position: relative;
    bottom: auto;
    left: auto;
    transform: none;
    margin-top: 20px;
    flex-direction: column;
    gap: 10px;
  }
}
</style>
