<template>
  <div class="student-drag-panel">
    <div class="panel-header">
      <h3 class="panel-title">
        <el-icon><UserFilled /></el-icon>
        待分配学生
      </h3>
      
      <div class="panel-controls">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索学生"
          size="small"
          clearable
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-select
          v-model="filterMajor"
          placeholder="筛选专业"
          size="small"
          clearable
          class="filter-select"
        >
          <el-option label="全部专业" value="" />
          <el-option
            v-for="major in availableMajors"
            :key="major"
            :label="major"
            :value="major"
          />
        </el-select>
        
        <el-select
          v-model="filterGender"
          placeholder="筛选性别"
          size="small"
          clearable
          class="filter-select"
        >
          <el-option label="全部" value="" />
          <el-option label="男生" value="男" />
          <el-option label="女生" value="女" />
        </el-select>
      </div>
    </div>

    <div class="panel-stats">
      <div class="stat-item">
        <span class="stat-value">{{ filteredStudents.length }}</span>
        <span class="stat-label">待分配</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ allocatedCount }}</span>
        <span class="stat-label">已分配</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ totalStudents }}</span>
        <span class="stat-label">总计</span>
      </div>
    </div>

    <div class="students-container">
      <div class="major-groups" v-if="groupByMajor">
        <div
          v-for="major in displayMajors"
          :key="major"
          class="major-group"
        >
          <div class="group-header">
            <div class="group-title">
              <div 
                class="major-color"
                :style="{ backgroundColor: getMajorColor(major) }"
              ></div>
              <span class="major-name">{{ major }}</span>
              <span class="student-count">({{ getStudentsByMajor(major).length }}人)</span>
            </div>
            <el-button
              size="small"
              text
              @click="toggleMajorCollapse(major)"
            >
              <el-icon v-if="collapsedMajors.includes(major)"><ArrowRight /></el-icon>
              <el-icon v-else><ArrowDown /></el-icon>
            </el-button>
          </div>
          
          <div v-if="!collapsedMajors.includes(major)" class="students-list">
            <div
              v-for="student in getStudentsByMajor(major)"
              :key="student.id"
              :class="[
                'student-card',
                { 'dragging': draggedStudent?.id === student.id }
              ]"
              draggable="true"
              @dragstart="onDragStart($event, student)"
              @dragend="onDragEnd"
            >
              <div class="student-avatar">
                <el-icon v-if="student.gender === '男'"><Male /></el-icon>
                <el-icon v-else><Female /></el-icon>
              </div>
              
              <div class="student-info">
                <div class="student-name">{{ student.name }}</div>
                <div class="student-details">
                  <span class="student-id">{{ student.studentId }}</span>
                  <span class="student-class">{{ student.class }}</span>
                </div>
              </div>
              
              <div class="student-indicators">
                <el-tag
                  v-if="student.hasSpecialNeeds"
                  size="small"
                  type="warning"
                  effect="plain"
                >
                  特需
                </el-tag>
                <el-tag
                  v-if="student.preferredRoommates && student.preferredRoommates.length > 0"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  室友
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-else class="students-list">
        <div
          v-for="student in filteredStudents"
          :key="student.id"
          :class="[
            'student-card',
            { 'dragging': draggedStudent?.id === student.id }
          ]"
          draggable="true"
          @dragstart="onDragStart($event, student)"
          @dragend="onDragEnd"
        >
          <div class="student-avatar">
            <el-icon v-if="student.gender === '男'"><Male /></el-icon>
            <el-icon v-else><Female /></el-icon>
          </div>
          
          <div class="student-info">
            <div class="student-name">{{ student.name }}</div>
            <div class="student-details">
              <span class="student-id">{{ student.studentId }}</span>
              <span class="student-major">{{ student.major }}</span>
              <span class="student-class">{{ student.class }}</span>
            </div>
          </div>
          
          <div class="student-indicators">
            <el-tag
              v-if="student.hasSpecialNeeds"
              size="small"
              type="warning"
              effect="plain"
            >
              特需
            </el-tag>
            <el-tag
              v-if="student.preferredRoommates && student.preferredRoommates.length > 0"
              size="small"
              type="info"
              effect="plain"
            >
              室友
            </el-tag>
          </div>
        </div>
      </div>
      
      <div v-if="filteredStudents.length === 0" class="empty-state">
        <el-empty description="暂无符合条件的学生" />
      </div>
    </div>

    <!-- 拖拽预览提示 -->
    <div v-if="isDragging" class="drag-preview-overlay">
      <div class="drag-preview-content">
        <div class="preview-avatar">
          <el-icon v-if="draggedStudent?.gender === '男'"><Male /></el-icon>
          <el-icon v-else><Female /></el-icon>
        </div>
        <div class="preview-info">
          <div class="preview-name">{{ draggedStudent?.name }}</div>
          <div class="preview-major">{{ draggedStudent?.major }}</div>
        </div>
        <div class="preview-hint">
          拖拽到房间进行分配
        </div>
      </div>
    </div>

    <!-- 分配历史/撤销区域 -->
    <div class="allocation-history" v-if="recentAllocations.length > 0">
      <div class="history-header">
        <h4>最近分配</h4>
        <el-button size="small" text @click="clearHistory">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
      </div>
      
      <div class="history-list">
        <div
          v-for="(allocation, index) in recentAllocations"
          :key="index"
          class="history-item"
        >
          <div class="history-info">
            <span class="history-student">{{ allocation.student.name }}</span>
            <span class="history-arrow">→</span>
            <span class="history-room">{{ allocation.room.roomNumber }}</span>
          </div>
          <el-button
            size="small"
            text
            type="danger"
            @click="undoAllocation(allocation)"
          >
            <el-icon><RefreshLeft /></el-icon>
            撤销
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import {
  UserFilled,
  Search,
  Male,
  Female,
  ArrowRight,
  ArrowDown,
  Delete,
  RefreshLeft
} from '@element-plus/icons-vue'

export default {
  name: 'StudentDragPanel',
  components: {
    UserFilled,
    Search,
    Male,
    Female,
    ArrowRight,
    ArrowDown,
    Delete,
    RefreshLeft
  },
  props: {
    students: {
      type: Array,
      default: () => []
    },
    allocatedStudents: {
      type: Array,
      default: () => []
    },
    groupByMajor: {
      type: Boolean,
      default: true
    }
  },
  emits: ['student-allocate', 'allocation-undo'],
  setup(props, { emit }) {
    // 响应式数据
    const searchKeyword = ref('')
    const filterMajor = ref('')
    const filterGender = ref('')
    const collapsedMajors = ref([])
    const draggedStudent = ref(null)
    const isDragging = ref(false)
    const recentAllocations = ref([])
    
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
    const totalStudents = computed(() => props.students.length)
    
    const allocatedCount = computed(() => props.allocatedStudents.length)
    
    const unallocatedStudents = computed(() => {
      const allocatedIds = new Set(props.allocatedStudents.map(s => s.id))
      return props.students.filter(student => !allocatedIds.has(student.id))
    })
    
    const filteredStudents = computed(() => {
      let filtered = unallocatedStudents.value
      
      // 搜索关键词过滤
      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        filtered = filtered.filter(student =>
          student.name.toLowerCase().includes(keyword) ||
          student.studentId.toLowerCase().includes(keyword) ||
          student.major.toLowerCase().includes(keyword) ||
          student.class.toLowerCase().includes(keyword)
        )
      }
      
      // 专业过滤
      if (filterMajor.value) {
        filtered = filtered.filter(student => student.major === filterMajor.value)
      }
      
      // 性别过滤
      if (filterGender.value) {
        filtered = filtered.filter(student => student.gender === filterGender.value)
      }
      
      return filtered
    })
    
    const availableMajors = computed(() => {
      const majors = new Set(unallocatedStudents.value.map(s => s.major))
      return Array.from(majors).sort()
    })
    
    const displayMajors = computed(() => {
      if (filterMajor.value) {
        return [filterMajor.value]
      }
      return availableMajors.value
    })
    
    // 方法
    const getMajorColor = (major) => {
      return majorColors[major] || '#909399'
    }
    
    const getStudentsByMajor = (major) => {
      return filteredStudents.value.filter(student => student.major === major)
    }
    
    const toggleMajorCollapse = (major) => {
      const index = collapsedMajors.value.indexOf(major)
      if (index > -1) {
        collapsedMajors.value.splice(index, 1)
      } else {
        collapsedMajors.value.push(major)
      }
    }
    
    const onDragStart = (event, student) => {
      draggedStudent.value = student
      isDragging.value = true
      
      // 设置拖拽数据
      event.dataTransfer.setData('application/json', JSON.stringify(student))
      event.dataTransfer.effectAllowed = 'move'
      
      // 创建自定义拖拽图像
      const dragImage = createDragImage(student)
      event.dataTransfer.setDragImage(dragImage, 50, 25)
    }
    
    const onDragEnd = () => {
      draggedStudent.value = null
      isDragging.value = false
    }
    
    const createDragImage = (student) => {
      const dragImage = document.createElement('div')
      dragImage.className = 'drag-image'
      dragImage.innerHTML = `
        <div class="drag-image-content">
          <div class="drag-avatar">${student.gender === '男' ? '♂' : '♀'}</div>
          <div class="drag-info">
            <div class="drag-name">${student.name}</div>
            <div class="drag-major">${student.major}</div>
          </div>
        </div>
      `
      
      // 样式
      Object.assign(dragImage.style, {
        position: 'absolute',
        top: '-1000px',
        left: '-1000px',
        background: 'white',
        border: '2px solid #409eff',
        borderRadius: '8px',
        padding: '8px 12px',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
        fontSize: '12px',
        zIndex: '9999'
      })
      
      document.body.appendChild(dragImage)
      
      // 延迟删除
      setTimeout(() => {
        if (document.body.contains(dragImage)) {
          document.body.removeChild(dragImage)
        }
      }, 100)
      
      return dragImage
    }
    
    const addToHistory = (student, room) => {
      const allocation = {
        student: { ...student },
        room: { ...room },
        timestamp: Date.now()
      }
      
      recentAllocations.value.unshift(allocation)
      
      // 限制历史记录数量
      if (recentAllocations.value.length > 10) {
        recentAllocations.value = recentAllocations.value.slice(0, 10)
      }
    }
    
    const undoAllocation = (allocation) => {
      emit('allocation-undo', allocation)
      
      // 从历史记录中移除
      const index = recentAllocations.value.findIndex(item => 
        item.student.id === allocation.student.id && 
        item.room.id === allocation.room.id &&
        item.timestamp === allocation.timestamp
      )
      
      if (index > -1) {
        recentAllocations.value.splice(index, 1)
      }
    }
    
    const clearHistory = () => {
      recentAllocations.value = []
    }
    
    // 监听外部分配事件，添加到历史记录
    const handleExternalAllocation = (event) => {
      if (event.student && event.room) {
        addToHistory(event.student, event.room)
      }
    }
    
    // 暴露方法给父组件调用
    const addAllocationToHistory = (student, room) => {
      addToHistory(student, room)
    }
    
    return {
      searchKeyword,
      filterMajor,
      filterGender,
      collapsedMajors,
      draggedStudent,
      isDragging,
      recentAllocations,
      totalStudents,
      allocatedCount,
      filteredStudents,
      availableMajors,
      displayMajors,
      getMajorColor,
      getStudentsByMajor,
      toggleMajorCollapse,
      onDragStart,
      onDragEnd,
      undoAllocation,
      clearHistory,
      addAllocationToHistory
    }
  }
}
</script>

<style scoped>
.student-drag-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}

/* 面板头部 */
.panel-header {
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8f9fa;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.panel-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 120px;
}

.filter-select {
  min-width: 100px;
}

/* 统计信息 */
.panel-stats {
  display: flex;
  padding: 10px 15px;
  background: #f0f9ff;
  border-bottom: 1px solid #e4e7ed;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #606266;
}

/* 学生容器 */
.students-container {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

/* 专业分组 */
.major-group {
  margin-bottom: 20px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 10px;
  cursor: pointer;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.major-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.major-name {
  font-weight: 600;
  color: #303133;
}

.student-count {
  font-size: 12px;
  color: #909399;
}

/* 学生列表 */
.students-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.student-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.3s ease;
  user-select: none;
}

.student-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.student-card:active {
  cursor: grabbing;
}

.student-card.dragging {
  opacity: 0.5;
  transform: scale(0.95);
}

.student-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.student-info {
  flex: 1;
  min-width: 0;
}

.student-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.student-details {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.student-details > span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.student-indicators {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #909399;
}

/* 拖拽预览覆盖层 */
.drag-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  z-index: 1000;
  pointer-events: none;
  display: flex;
  justify-content: center;
  align-items: center;
}

.drag-preview-content {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 15px;
  border: 2px solid #409eff;
}

.preview-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.preview-info {
  flex: 1;
}

.preview-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.preview-major {
  font-size: 14px;
  color: #606266;
}

.preview-hint {
  font-size: 12px;
  color: #909399;
  font-style: italic;
  margin-top: 8px;
}

/* 分配历史 */
.allocation-history {
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
  max-height: 200px;
  overflow-y: auto;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #e4e7ed;
}

.history-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.history-list {
  padding: 10px 15px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}

.history-item:last-child {
  border-bottom: none;
}

.history-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.history-student {
  font-weight: 500;
  color: #303133;
}

.history-arrow {
  color: #909399;
}

.history-room {
  color: #409eff;
  font-weight: 500;
}

/* 拖拽图像样式 */
:global(.drag-image) {
  background: white;
  border: 2px solid #409eff;
  border-radius: 8px;
  padding: 8px 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-size: 12px;
}

:global(.drag-image-content) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:global(.drag-avatar) {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

:global(.drag-name) {
  font-weight: 600;
  color: #303133;
}

:global(.drag-major) {
  font-size: 10px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .panel-controls {
    flex-direction: column;
  }
  
  .search-input,
  .filter-select {
    width: 100%;
  }
  
  .panel-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .stat-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .stat-value,
  .stat-label {
    display: inline;
  }
  
  .student-card {
    padding: 8px;
  }
  
  .student-details {
    flex-direction: column;
    gap: 2px;
  }
}
</style>
