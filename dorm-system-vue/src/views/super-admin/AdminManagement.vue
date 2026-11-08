<template>
  <div class="admin-management-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>管理员管理</h2>
          <div class="header-buttons">
            <el-button type="primary" @click="showAddDialog">
              <template #icon>
                <Plus />
              </template>
              添加管理员
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 管理员列表 -->
      <el-table :data="admins" style="width: 100%" :header-cell-style="{ background: '#f5f7fa' }">
        <el-table-column prop="name" label="姓名" width="120" align="center" />
        <el-table-column prop="username" label="用户名" width="120" align="center" />
        <el-table-column prop="email" label="邮箱" min-width="200" align="center" show-overflow-tooltip />
        <el-table-column prop="buildings" label="管理楼栋" width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" type="info" @click="editAdmin(scope.row)">
                <template #icon>
                  <Edit />
                </template>
                编辑
              </el-button>
              <el-button 
                size="small" 
                :type="scope.row.status === 'active' ? 'danger' : 'success'"
                @click="toggleStatus(scope.row)"
              >
                <template #icon>
                  <Lock v-if="scope.row.status === 'active'" />
                  <Unlock v-else />
                </template>
                {{ scope.row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="deleteAdmin(scope.row)">
                <template #icon>
                  <Delete />
                </template>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加管理员对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加管理员" width="600px">
      <el-form :model="adminForm" label-width="100px" :rules="formRules" ref="adminFormRef">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="adminForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="adminForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="adminForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="adminForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="adminForm.password" 
            type="password" 
            placeholder="请输入密码(6-10位)" 
            show-password 
            maxlength="10"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="adminForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码(6-10位)" 
            show-password 
            maxlength="10"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="管理楼栋" prop="buildings">
          <el-select 
            v-model="adminForm.buildings" 
            multiple 
            placeholder="请选择要分配的楼栋"
            style="width: 100%"
            clearable
          >
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="building.name" 
              :value="building.id" 
              :disabled="isBuildingOccupiedByOthers(building.id)"
            >
              <span>{{ building.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                {{ building.gender + '生楼' }}
              </span>
            </el-option>
          </el-select>
          <div class="buildings-note">
            <el-icon size="14"><InfoFilled /></el-icon>
            <span>可以选择多个楼栋，已被其他管理员占用的楼栋无法选择</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addAdmin">添加</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑管理员对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑管理员" width="600px">
      <el-form :model="editForm" label-width="100px" :rules="editFormRules" ref="editFormRef">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        
        <!-- 重置密码功能 -->
        <el-divider content-position="left">
          <el-icon><Key /></el-icon>
          重置密码
        </el-divider>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="editForm.newPassword" 
            type="password" 
            placeholder="留空则不修改密码(6-10位)" 
            show-password 
            clearable
            maxlength="10"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="editForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码(6-10位)" 
            show-password 
            clearable
            maxlength="10"
            show-word-limit
          />
        </el-form-item>
        
        <div class="password-tip">
          <el-icon size="14"><InfoFilled /></el-icon>
          <span>如需重置密码，请输入新密码并确认。如不需要修改密码，请保持密码字段为空。</span>
        </div>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateAdmin">保存</el-button>
        </span>
      </template>
    </el-dialog>




    <!-- 超级管理员批量添加宿舍对话框 -->
    <el-dialog v-model="superBatchAddDialogVisible" title="批量添加宿舍" width="600px">
      <el-form :model="superBatchAddForm" label-width="120px">
        <el-form-item label="选择楼栋" required>
          <el-select v-model="superBatchAddForm.buildingId" placeholder="选择楼栋" style="width: 100%">
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="`${building.name} (${building.gender}生楼)`" 
              :value="building.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="起始楼层" required>
          <el-input-number v-model="superBatchAddForm.startFloor" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="结束楼层" required>
          <el-input-number v-model="superBatchAddForm.endFloor" :min="superBatchAddForm.startFloor" :max="20" />
        </el-form-item>
        <el-form-item label="每层房间数" required>
          <el-input-number v-model="superBatchAddForm.roomsPerFloor" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="房间类型" required>
          <el-select v-model="superBatchAddForm.roomType" placeholder="选择房间类型" style="width: 100%">
            <el-option label="双人间" value="双人间" />
            <el-option label="四人间" value="四人间" />
            <el-option label="六人间" value="六人间" />
            <el-option label="八人间" value="八人间" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="superBatchAddDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveSuperBatchAdd">批量添加</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 超级管理员单个添加宿舍对话框 -->
    <el-dialog v-model="superAddDormDialogVisible" title="添加宿舍" width="600px">
      <el-form :model="superAddDormForm" label-width="120px">
        <el-form-item label="选择楼栋" required>
          <el-select v-model="superAddDormForm.buildingId" placeholder="选择楼栋" style="width: 100%">
            <el-option 
              v-for="building in availableBuildings" 
              :key="building.id" 
              :label="`${building.name} (${building.gender}生楼)`" 
              :value="building.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号" required>
          <el-input v-model="superAddDormForm.room" placeholder="请输入房间号，如：101" />
        </el-form-item>
        <el-form-item label="房间类型" required>
          <el-select v-model="superAddDormForm.type" placeholder="选择房间类型" style="width: 100%">
            <el-option label="双人间" value="双人间" />
            <el-option label="四人间" value="四人间" />
            <el-option label="六人间" value="六人间" />
            <el-option label="八人间" value="八人间" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="superAddDormDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveSuperAddDorm">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 添加/编辑楼栋对话框 -->
    <el-dialog 
      v-model="buildingDialogVisible" 
      :title="buildingDialogTitle" 
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="buildingForm" label-width="100px" :rules="buildingFormRules" ref="buildingFormRef">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="buildingForm.name" placeholder="请输入楼栋名称（如：1号楼）" />
        </el-form-item>
        <el-form-item label="楼栋类型" prop="gender">
          <el-select v-model="buildingForm.gender" placeholder="请选择楼栋类型" style="width: 100%">
            <el-option label="男生楼" value="男" />
            <el-option label="女生楼" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋描述" prop="description">
          <el-input 
            v-model="buildingForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入楼栋描述信息（如：位置、特色等）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="buildingDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveBuildingInfo" :loading="buildingSaving">
            {{ isEditingBuilding ? '更新' : '添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 快速分配楼栋对话框 -->
    <el-dialog 
      v-model="quickAssignVisible" 
      title="快速分配楼栋" 
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="quick-assign-content">
        <div class="assign-info">
          <el-icon class="building-icon-large"><OfficeBuilding /></el-icon>
          <div class="assign-details">
            <h4>{{ currentQuickAssignBuilding?.name || '未知楼栋' }}</h4>
            <p>{{ currentQuickAssignBuilding?.description || currentQuickAssignBuilding?.name || '暂无描述' }}</p>
  </div>
        </div>
        
        <el-form :model="quickAssignForm" label-width="100px">
          <el-form-item label="分配给">
            <el-select 
              v-model="quickAssignForm.adminUsername" 
              placeholder="请选择管理员"
              style="width: 100%"
              clearable
            >
              <el-option label="取消分配" value="" />
              <el-option
                v-for="admin in admins.filter(a => a.status === 'active')"
                :key="admin.username"
                :label="`${admin.name} (${admin.username})`"
                :value="admin.username"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="quickAssignVisible = false">取消</el-button>
          <el-button type="primary" @click="saveQuickAssign" :loading="quickAssignSaving">
            保存分配
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 专业分配对话框 -->
    <el-dialog 
      v-model="majorAssignmentDialogVisible" 
      title="专业分配管理" 
      width="800px"
      @close="resetMajorAssignmentDialog"
    >
      <div class="major-assignment-dialog">
        <div class="building-info">
          <div class="building-header">
            <el-icon><OfficeBuilding /></el-icon>
            <div class="building-details">
              <h3>{{ currentAssignmentBuilding?.buildingName }}</h3>
              <p>{{ currentAssignmentBuilding?.gender }}生楼 | 总容量: {{ currentAssignmentBuilding?.totalCapacity }}人</p>
            </div>
          </div>
        </div>

        <div class="assignment-content">
          <div class="available-majors">
            <h4>可分配专业</h4>
            <div class="major-list">
              <div 
                v-for="major in availableMajorsForAssignment" 
                :key="major.name"
                class="major-item"
                :class="{ 'selected': selectedMajorsForAssignment.includes(major.name) }"
                @click="toggleMajorSelection(major.name)"
              >
                <div class="major-header">
                  <span class="major-name">{{ major.name }}</span>
                  <span class="student-count">{{ major.studentCount }}人</span>
                </div>
                <div class="major-info">
                  <span class="gender-info">{{ major.gender }}生</span>
                  <span class="capacity-info" v-if="selectedMajorsForAssignment.includes(major.name)">
                    分配容量: 
                    <el-input-number 
                      v-model="major.assignedCapacity" 
                      :min="0" 
                      :max="currentAssignmentBuilding?.totalCapacity"
                      size="small"
                      @change="validateCapacityAllocation"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="assignment-summary" v-if="selectedMajorsForAssignment.length > 0">
            <h4>分配汇总</h4>
            <div class="summary-stats">
              <div class="stat-item">
                <span class="label">已分配专业:</span>
                <span class="value">{{ selectedMajorsForAssignment.length }}个</span>
              </div>
              <div class="stat-item">
                <span class="label">总分配容量:</span>
                <span class="value">{{ totalAssignedCapacity }}人</span>
              </div>
              <div class="stat-item">
                <span class="label">剩余容量:</span>
                <span class="value" :class="{ 'warning': remainingCapacity < 0 }">
                  {{ remainingCapacity }}人
                </span>
              </div>
            </div>
            
            <el-alert 
              v-if="remainingCapacity < 0"
              title="容量超限"
              type="error"
              :description="`分配的容量超出楼栋总容量 ${Math.abs(remainingCapacity)} 人，请调整分配方案`"
              show-icon
              :closable="false"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="majorAssignmentDialogVisible = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="saveMajorAssignment" 
            :disabled="remainingCapacity < 0 || selectedMajorsForAssignment.length === 0"
            :loading="assignmentSaving"
          >
            保存分配
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 楼栋宿舍详情对话框 -->
    <el-dialog 
      v-model="buildingDormsDialogVisible" 
      :title="`${currentBuildingDorms?.buildingName} - 宿舍详情`"
      width="90%"
      :close-on-click-modal="false"
    >
      <div class="building-dorms-dialog" v-if="currentBuildingDorms">
        <!-- 楼栋信息 -->
        <div class="building-info-header">
          <div class="building-basic-info">
            <h3>
              <el-icon><OfficeBuilding /></el-icon>
              {{ currentBuildingDorms.buildingName }}
            </h3>
            <div class="info-stats">
              <el-tag :type="currentBuildingDorms.gender === '男' ? 'primary' : 'danger'" size="small">
                {{ currentBuildingDorms.gender }}生楼
              </el-tag>
              <span class="stat">总宿舍：{{ buildingDormsList.length }} 间</span>
              <span class="stat">总床位：{{ currentBuildingDorms.totalBeds }} 床</span>
            </div>
          </div>
          
          <!-- 批量操作按钮 -->
          <div class="batch-actions">
            <el-button 
              type="warning" 
              @click="showBatchEditDialog"
              :disabled="selectedDorms.length === 0"
            >
              <template #icon>
                <Edit />
              </template>
              批量编辑 ({{ selectedDorms.length }})
            </el-button>
            <el-button 
              type="danger" 
              @click="showBatchDeleteDialog"
              :disabled="selectedDorms.length === 0"
            >
              <template #icon>
                <Delete />
              </template>
              批量删除 ({{ selectedDorms.length }})
            </el-button>
          </div>
        </div>

        <!-- 宿舍列表 -->
        <div class="dorms-list">
          <el-table 
            ref="dormTable"
            :data="buildingDormsList" 
            @selection-change="handleDormSelectionChange"
            style="width: 100%" 
            max-height="500px"
            :header-cell-style="{ background: '#f5f7fa' }"
            row-key="id"
            empty-text="该楼栋暂无宿舍，请先添加宿舍"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="room" label="房间号" width="100" align="center" sortable />
            <el-table-column prop="floor" label="楼层" width="80" align="center" sortable />
            <el-table-column prop="type" label="房间类型" width="120" align="center">
              <template #default="scope">
                <el-tag type="info" size="small">{{ scope.row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalBeds" label="床位数" width="80" align="center" />
            <el-table-column prop="occupiedBeds" label="已入住" width="80" align="center" />
            <el-table-column label="入住率" width="120" align="center">
              <template #default="scope">
                <el-progress 
                  :percentage="Math.round((scope.row.occupiedBeds / scope.row.totalBeds) * 100)" 
                  :stroke-width="6"
                  :show-text="false"
                  :color="scope.row.occupiedBeds === scope.row.totalBeds ? '#f56c6c' : '#67c23a'"
                />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="scope">
                <el-tag 
                  :type="scope.row.status === 'available' ? 'success' : 'warning'" 
                  size="small"
                >
                  {{ scope.row.status === 'available' ? '空闲' : '已满' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button 
                  size="small" 
                  type="primary" 
                  @click="editSingleDorm(scope.row)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="buildingDormsDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量编辑/删除对话框 -->
    <el-dialog 
      v-model="batchEditDialogVisible" 
      :title="batchEditForm.action === 'edit' ? '批量编辑宿舍' : '批量删除宿舍'"
      width="500px"
    >
      <div>
        <div class="batch-info">
          <p><strong>已选择宿舍：</strong>{{ selectedDorms.length }} 间</p>
          <div class="selected-rooms" v-if="selectedDorms.length > 0">
            <el-tag 
              v-for="dorm in selectedDorms" 
              :key="dorm.id"
              size="small"
              style="margin: 2px;"
            >
              {{ dorm.room }}房 ({{ dorm.type }})
            </el-tag>
          </div>
        </div>
        
        <!-- 批量编辑表单 -->
        <div v-if="batchEditForm.action === 'edit'" style="margin-top: 20px;">
          <el-form label-width="120px">
            <el-form-item label="新房间类型：" required>
              <el-select v-model="batchEditForm.roomType" placeholder="选择新的房间类型" style="width: 100%">
                <el-option label="双人间" value="双人间" />
                <el-option label="四人间" value="四人间" />
                <el-option label="六人间" value="六人间" />
                <el-option label="八人间" value="八人间" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 删除确认 -->
        <div v-if="batchEditForm.action === 'delete'" style="margin-top: 20px;">
          <el-alert 
            title="确认删除" 
            type="warning" 
            description="删除后不可恢复，请确认是否继续删除选中的宿舍？"
            :closable="false"
          />
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchEditDialogVisible = false">取消</el-button>
          <el-button 
            :type="batchEditForm.action === 'edit' ? 'primary' : 'danger'" 
            @click="executeBatchOperation"
            :disabled="batchEditForm.action === 'edit' && !batchEditForm.roomType"
          >
            {{ batchEditForm.action === 'edit' ? '确认编辑' : '确认删除' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script>
import { 
  getAllBuildings as getAllBuildingsAPI,
  createBuilding as createBuildingAPI,
  updateBuilding as updateBuildingAPI,
  deleteBuilding as deleteBuildingAPI,
  assignBuildingManager,
  assignBuildingsToManager
} from '@/api/building.js'
import { 
  getAdminList,
  createAdmin,
  updateAdmin as updateAdminAPI,
  deleteAdmin as deleteAdminAPI,
  toggleAdminStatus as toggleAdminStatusAPI,
  resetAdminPassword
} from '@/api/admin.js'
import {
  getRoomsByBuilding,
  createRoom,
  batchCreateRooms,
  batchUpdateRoomType,
  batchDeleteRooms
} from '@/api/room.js'
import {
  saveMajorAssignments,
  getMajorAssignments,
  getAllMajorAssignments
} from '@/api/majorAssignment.js'
import { getStudentList } from '@/api/student.js'
import { 
  Search, 
  RefreshLeft,
  Plus,
  Edit,
  Delete,
  User,
  OfficeBuilding,
  Check,
  Warning,
  Grid,
  Lock,
  Unlock
} from '@element-plus/icons-vue'

export default {
  name: 'AdminManagement',
  inject: ['logOperation'],
  components: {
    Search,
    RefreshLeft,
    Plus,
    Edit,
    Delete,
    User,
    OfficeBuilding,
    Check,
    Warning,
    Grid,
    Lock,
    Unlock
  },
  data() {
    return {
      addDialogVisible: false,
      editDialogVisible: false,
      buildingDialogVisible: false,
      superBatchAddDialogVisible: false,
      superAddDormDialogVisible: false,
      quickAssignVisible: false,
      currentEditAdmin: null,
      currentQuickAssignBuilding: null,
      buildingSaving: false,
      quickAssignSaving: false,
      isEditingBuilding: false,
      editingBuildingId: null,
      adminForm: {
        name: '',
        username: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        buildings: []
      },
      editForm: {
        name: '',
        username: '',
        email: '',
        phone: '',
        newPassword: '',
        confirmPassword: ''
      },
      buildingForm: {
        name: '',
        gender: '',
        description: ''
      },
      quickAssignForm: {
        adminUsername: ''
      },
      // 专业分配相关数据
      majorAssignmentDialogVisible: false,
      currentAssignmentBuilding: null,
      selectedMajorsForAssignment: [],
      availableMajorsForAssignment: [],
      assignmentSaving: false,
      // 楼栋搜索相关数据
      buildingSearchKeyword: '',
      buildingSearchType: '',
      buildingSearchStatus: '',
      formRules: {
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { validator: this.validateUsername, trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
          { max: 10, message: '密码长度不能超过10位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: this.validateConfirmPassword, trigger: 'blur' }
        ]
      },
      editFormRules: {
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        newPassword: [
          { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
          { max: 10, message: '密码长度不能超过10位', trigger: 'blur' }
        ],
        confirmPassword: [
          { validator: this.validateEditConfirmPassword, trigger: 'blur' }
        ]
      },
      buildingFormRules: {
        name: [
          { required: true, message: '请输入楼栋名称', trigger: 'blur' },
          { validator: this.validateBuildingName, trigger: 'blur' }
        ],
        gender: [
          { required: true, message: '请选择楼栋类型', trigger: 'change' }
        ],
        description: [
          { required: true, message: '请输入楼栋描述', trigger: 'blur' }
        ]
      },
      availableBuildings: [],
      assignmentUpdateCounter: 0, // 用于强制更新分配状态显示
      admins: [], // 管理员数据将从统一数据源加载
      
      // 超级管理员批量添加宿舍表单
      superBatchAddForm: {
        buildingId: '',
        startFloor: 1,
        endFloor: 6,
        roomsPerFloor: 10,
        roomType: '四人间',
        totalBeds: 4
      },
      
      // 超级管理员单个添加宿舍表单
      superAddDormForm: {
        buildingId: '',
        room: '',
        type: '四人间',
        totalBeds: 4
      },

      // 楼栋宿舍详情相关
      buildingDormsDialogVisible: false,
      currentBuildingDorms: null,
      buildingDormsList: [],
      selectedDorms: [],
      
      // 批量操作相关
      batchEditDialogVisible: false,
      batchEditForm: {
        roomType: '',
        action: 'edit' // 'edit' 或 'delete'
      }
    }
  },
  
  computed: {
    buildingDialogTitle() {
      return this.isEditingBuilding ? '编辑楼栋' : '添加楼栋';
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
    
    // 过滤后的楼栋列表
    filteredBuildings() {
      let filtered = [...this.availableBuildings];
      
      // 按关键词搜索
      if (this.buildingSearchKeyword) {
        const keyword = this.buildingSearchKeyword.toLowerCase();
        filtered = filtered.filter(building => {
          const nameMatch = building.name.toLowerCase().includes(keyword);
          const descMatch = building.description && building.description.toLowerCase().includes(keyword);
          const assigneeMatch = this.getBuildingAssignee(building.id) && 
            this.getBuildingAssignee(building.id).toLowerCase().includes(keyword);
          return nameMatch || descMatch || assigneeMatch;
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
    }
  },
  
  async mounted() {
    // 🔥 注意加载顺序：先加载楼栋，再加载管理员（因为需要根据楼栋数据计算buildingIds）
    await this.loadAvailableBuildings();
    await this.loadAdmins(); // 依赖 availableBuildings 来计算 buildingIds
    this.cleanupInvalidAssignments();
    
    // 监听宿舍数据更新事件，实现跨页面数据同步
    this.setupDataSyncListener();
  },
  
  beforeUnmount() {
    // 组件销毁时清理监听器
    window.removeEventListener('dormDataUpdated', this.handleDormDataUpdate)
  },
  
  methods: {
    async loadAvailableBuildings() {
      try {
        const response = await getAllBuildingsAPI()
        if (response.code === 200) {
          // 转换后端数据格式为前端需要的格式
          this.availableBuildings = (response.data || []).map(building => ({
            id: building.id,
            name: building.buildingName,
            gender: building.buildingType || '男', // buildingType已经是中文了
            description: building.description || building.buildingName,
            totalRooms: building.totalRooms || 0,
            occupancyRate: building.occupancyRate || 0,
            status: building.status === 1 ? 'active' : 'inactive',
            managerName: building.managerName || '',
            managerPhone: building.managerPhone || ''
          }))
        } else {
          console.warn('获取楼栋列表失败:', response.message)
          this.availableBuildings = []
        }
      } catch (error) {
        console.error('加载楼栋列表失败:', error)
        this.$message.warning('加载楼栋列表失败，请刷新重试')
        this.availableBuildings = []
      }
    },
    
    // 加载管理员数据
    async loadAdmins() {
        try {
          const response = await getAdminList({ currentPage: 1, pageSize: 1000 })
          if (response.code === 200 && response.data && response.data.records) {
            this.admins = response.data.records.map(admin => {
              // 🔥 计算buildingIds用于业务逻辑检查（如删除管理员前的验证）
              const buildingIds = this.availableBuildings
                .filter(b => b.managerName === admin.username)
                .map(b => b.id);
              
              return {
                id: admin.id,
                username: admin.username,
                name: admin.realName,
                email: admin.email,
                phone: admin.phone,
                status: admin.status === 1 ? 'active' : 'inactive',
                buildings: admin.managedBuilding || '未分配',  // 显示用的楼栋名称
                buildingIds: buildingIds  // 业务逻辑用的楼栋ID数组
              };
            })
          }
        } catch (error) {
          console.error('加载管理员列表失败:', error)
          this.$message.error('加载管理员列表失败')
        }
      },
      
      // 同步数据
      syncData() {
        this.loadAdmins();
        // loadBuildingAssignments 已废弃，因为后端已经返回了完整的管理楼栋数据
      },
      
      async loadBuildingAssignments() {
        // 🔥 此方法已废弃，后端API已经在getAdminList中返回managedBuilding字段
        // 保留此方法仅用于向后兼容，但不做任何操作
        console.log('⚠️ loadBuildingAssignments已废弃，后端已返回完整数据');
      },
      // 用户名验证
    validateUsername(rule, value, callback) {
      if (value === '') {
        callback(new Error('请输入用户名'));
      } else {
        // 后端会在创建时检查用户名是否存在
        callback();
      }
    },
      
      // 密码确认验证
    validateConfirmPassword(rule, value, callback) {
      if (value === '') {
        callback(new Error('请再次输入密码'));
      } else if (value !== this.adminForm.password) {
        callback(new Error('两次输入密码不一致!'));
      } else {
        callback();
      }
    },

    // 编辑表单密码确认验证
    validateEditConfirmPassword(rule, value, callback) {
      if (this.editForm.newPassword && value === '') {
        callback(new Error('请再次输入新密码'));
      } else if (this.editForm.newPassword && value !== this.editForm.newPassword) {
        callback(new Error('两次输入密码不一致!'));
      } else {
        callback();
      }
    },

    // 楼栋名称验证
    validateBuildingName(rule, value, callback) {
      if (value === '') {
        callback(new Error('请输入楼栋名称'));
      } else {
        // 检查是否重复（编辑时排除自己）
        const isDuplicate = this.availableBuildings.some(building => 
          building.name === value && building.id !== this.editingBuildingId
        );
        if (isDuplicate) {
          callback(new Error('楼栋名称已存在，请换一个'));
        } else {
          callback();
        }
      }
    },
    async showAddDialog() {
      // 刷新最新的楼栋数据，确保楼栋列表是最新的
      await this.loadAvailableBuildings();
      
      this.adminForm = {
        name: '',
        username: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        buildings: []
      }
      this.addDialogVisible = true
    },
    async addAdmin() {
      this.$refs.adminFormRef.validate(async (valid) => {
        if (valid) {
      // 前端二次验证：检查选中的楼栋是否已被占用
      if (this.adminForm.buildings.length > 0) {
        // 刷新楼栋数据以获取最新的manager_name
        await this.loadAvailableBuildings();
        
        for (const buildingId of this.adminForm.buildings) {
          if (this.isBuildingOccupiedByOthers(buildingId, this.adminForm.username)) {  // 🔥 修复：传递username而不是name
            const building = this.availableBuildings.find(b => b.id === buildingId);
            const buildingName = building ? building.name : `楼栋${buildingId}`;
            const occupier = this.getBuildingAssignee(buildingId);
            this.$message.warning(`${buildingName} 已被 ${occupier} 管理，请重新选择`);
            return;
          }
        }
      }
      
      try {
            const response = await createAdmin({
              username: this.adminForm.username,
              password: this.adminForm.password,
              realName: this.adminForm.name,
              email: this.adminForm.email,
              phone: this.adminForm.phone
            });
            
            if (response.code === 200) {
              // 使用新的简化API分配楼栋（更新building表的manager_name）
              if (this.adminForm.buildings.length > 0) {
                try {
                  await assignBuildingsToManager(
                    this.adminForm.username, // 🔥 修复：传递username而不是name，因为manager_name字段存储的是username
                    this.adminForm.phone || '', // 管理员电话
                    this.adminForm.buildings // 楼栋ID列表
                  );
                } catch (assignError) {
                  console.error('分配楼栋失败:', assignError);
                  this.$message.warning('管理员创建成功，但楼栋分配失败，请手动分配');
                }
              }
              
              // 重新加载楼栋数据和管理员数据
              await this.loadAvailableBuildings(); // 刷新楼栋数据
              await this.loadAdmins(); // 重新加载管理员（后端会自动返回最新的管理楼栋信息）
              
              this.addDialogVisible = false;
              this.resetForm();
              
              // 广播楼栋分配数据更新事件
              window.dispatchEvent(new CustomEvent('buildingAssignmentUpdated', { 
                detail: { 
                  adminName: this.adminForm.name, 
                  buildings: this.adminForm.buildings,
                  action: 'assign'
                } 
              }));
              
              this.$message.success('管理员添加成功');
              this.logOperation(`添加管理员：${this.adminForm.name}`, '管理员管理');
            } else {
              this.$message.error(response.message || '添加管理员失败');
            }
          } catch (error) {
            console.error('添加管理员失败:', error);
            // 错误已经由request拦截器显示，这里不再重复显示
          }
        }
      })
    },
    editAdmin(admin) {
      this.currentEditAdmin = admin
      
      this.editForm = {
        name: admin.name,
        username: admin.username,
        email: admin.email,
        phone: admin.phone || '',
        newPassword: '',
        confirmPassword: ''
      }
      this.editDialogVisible = true
    },
    async updateAdmin() {
      this.$refs.editFormRef.validate(async (valid) => {
        if (valid) {
          if (this.currentEditAdmin) {
            try {
              // 更新基本信息
              const response = await updateAdminAPI({
                id: this.currentEditAdmin.id,
                username: this.editForm.username,
                realName: this.editForm.name,
                email: this.editForm.email,
                phone: this.editForm.phone
              });
              
              let successMessage = '管理员信息更新成功';
              let logMessage = `编辑管理员信息：${this.editForm.name}`;
              
              // 如果需要重置密码
              if (this.editForm.newPassword) {
                const passwordResponse = await resetAdminPassword(this.currentEditAdmin.id, {
                  newPassword: this.editForm.newPassword
                });
                
                if (passwordResponse.code === 200) {
                  successMessage = '管理员信息和密码更新成功';
                  logMessage += '，重置密码';
                }
              }
              
              if (response.code === 200) {
                // 重新加载管理员数据
                await this.loadAdmins();
                
                this.editDialogVisible = false;
                this.$message.success(successMessage);
                this.logOperation(logMessage, '管理员管理');
              } else {
                this.$message.error(response.message || '更新管理员失败');
              }
            } catch (error) {
              console.error('更新管理员失败:', error);
              // 错误已经由request拦截器显示，这里不再重复显示
            }
          }
        }
      })
    },
    toggleStatus(admin) {
      const action = admin.status === 'active' ? '禁用' : '启用'
      this.$confirm(`确定要${action}${admin.name}吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await toggleAdminStatusAPI(admin.id);
          
          if (response.code === 200) {
            // 重新加载管理员数据
            await this.loadAdmins();
            
            this.$message.success(`${action}成功`);
            this.logOperation(`${action}管理员：${admin.name}`, '管理员管理');
          } else {
            this.$message.error(response.message || `${action}失败`);
          }
        } catch (error) {
          console.error(`${action}管理员失败:`, error);
          // 错误已经由request拦截器显示，这里不再重复显示
        }
      })
    },
    

    
    // 超级管理员批量添加宿舍
    showSuperBatchAddDialog() {
      this.superBatchAddForm = {
        buildingId: '',
        startFloor: 1,
        endFloor: 6,
        roomsPerFloor: 10,
        roomType: '四人间'
      }
      this.superBatchAddDialogVisible = true
    },
    
    // 保存超级管理员批量添加
    async saveSuperBatchAdd() {
      if (!this.superBatchAddForm.buildingId) {
        this.$message.warning('请选择楼栋')
        return
      }
      
      const building = this.availableBuildings.find(b => b.id == this.superBatchAddForm.buildingId)
      if (!building) {
        this.$message.error('选择的楼栋不存在')
        return
      }
      
      // 根据房间类型自动计算床位数
      const bedCountMap = {
        '双人间': 2,
        '四人间': 4,
        '六人间': 6,
        '八人间': 8
      }
      const totalBeds = bedCountMap[this.superBatchAddForm.roomType] || 4
      
      // 转换房间类型为后端格式（新格式：中文）
      const roomTypeMap = {
        '双人间': '2人间',
        '四人间': '4人间',
        '六人间': '6人间',
        '八人间': '8人间'
      }
      const roomType = roomTypeMap[this.superBatchAddForm.roomType] || '4人间'
      
      try {
        // 调用后端API批量创建房间
        const response = await batchCreateRooms({
          buildingId: this.superBatchAddForm.buildingId,
          startFloor: this.superBatchAddForm.startFloor,
          endFloor: this.superBatchAddForm.endFloor,
          roomsPerFloor: this.superBatchAddForm.roomsPerFloor,
          roomType: roomType,
          bedCount: totalBeds
        })
        
        if (response.code === 200) {
          const result = response.data
          this.superBatchAddDialogVisible = false
          this.$message.success(`批量添加成功，共添加${result.successCount}个宿舍`)
          this.logOperation(`批量添加宿舍：${building.name} ${result.successCount}间`, '楼栋管理')
          
          // 如果当前正在查看该楼栋的宿舍，刷新列表
          if (this.currentBuildingDorms && this.currentBuildingDorms.buildingId == this.superBatchAddForm.buildingId) {
            this.loadBuildingDormsList()
          }
        } else {
          this.$message.error(response.message || '批量创建失败')
        }
      } catch (error) {
        console.error('批量创建房间失败:', error)
        this.$message.error('批量创建失败')
      }
    },

    // 查看楼栋宿舍详情
    viewBuildingDorms(building) {
      this.currentBuildingDorms = building
      this.buildingDormsDialogVisible = true
      this.selectedDorms = []
      this.loadBuildingDormsList()
    },

    // 超级管理员添加宿舍
    showSuperAddDormDialog() {
      this.superAddDormForm = {
        buildingId: '',
        room: '',
        type: '四人间'
      }
      this.superAddDormDialogVisible = true
    },
    
    // 保存超级管理员单个添加
    async saveSuperAddDorm() {
      if (!this.superAddDormForm.buildingId) {
        this.$message.warning('请选择楼栋')
        return
      }
      
      if (!this.superAddDormForm.room.trim()) {
        this.$message.warning('请输入房间号')
        return
      }
      
      const building = this.availableBuildings.find(b => b.id == this.superAddDormForm.buildingId)
      if (!building) {
        this.$message.error('选择的楼栋不存在')
        return
      }
      
      // 根据房间类型自动计算床位数
      const bedCountMap = {
        '双人间': 2,
        '四人间': 4,
        '六人间': 6,
        '八人间': 8
      }
      const totalBeds = bedCountMap[this.superAddDormForm.type] || 4
      
      // 转换房间类型为后端格式（新格式：中文）
      const roomTypeMap = {
        '双人间': '2人间',
        '四人间': '4人间',
        '六人间': '6人间',
        '八人间': '8人间'
      }
      const roomType = roomTypeMap[this.superAddDormForm.type] || '4人间'
      
      // 计算楼层
      const roomNumber = this.superAddDormForm.room.trim()
      const floor = Math.floor(parseInt(roomNumber) / 100)
      
      try {
        // 调用后端API创建房间
        const response = await createRoom({
          buildingId: this.superAddDormForm.buildingId,
          roomNumber: roomNumber,
          floor: floor,
          roomType: roomType,
          bedCount: totalBeds,
          status: 'available'
        })
        
        if (response.code === 200) {
          this.superAddDormDialogVisible = false
          this.$message.success('宿舍添加成功')
          this.logOperation(`添加宿舍：${building.name} ${roomNumber}`, '楼栋管理')
          
          // 如果当前正在查看该楼栋的宿舍，刷新列表
          if (this.currentBuildingDorms && this.currentBuildingDorms.buildingId == this.superAddDormForm.buildingId) {
            this.loadBuildingDormsList()
          }
        } else {
          this.$message.error(response.message || '添加宿舍失败')
        }
      } catch (error) {
        console.error('创建房间失败:', error)
        this.$message.error('添加宿舍失败')
      }
    },


    // 显示添加楼栋对话框
    showAddBuildingDialog() {
      this.isEditingBuilding = false;
      this.editingBuildingId = null;
      this.buildingForm = {
        name: '',
        gender: '',
        description: ''
      };
      this.buildingDialogVisible = true;
    },

    // 编辑楼栋
    editBuilding(building) {
      this.isEditingBuilding = true;
      this.editingBuildingId = building.id;
      this.buildingForm = {
        name: building.name,
        gender: building.gender,
        description: building.description
      };
      this.buildingDialogVisible = true;
    },

    // 保存楼栋信息
    async saveBuildingInfo() {
      if (!this.$refs.buildingFormRef) return;
      
      try {
        await this.$refs.buildingFormRef.validate();
        this.buildingSaving = true;

        // buildingType直接使用中文
        const buildingType = this.buildingForm.gender

        if (this.isEditingBuilding) {
          // 更新楼栋
          const response = await updateBuildingAPI({
            id: this.editingBuildingId,
            buildingName: this.buildingForm.name,
            buildingType: buildingType,
            description: this.buildingForm.description
          })
          
          if (response.code === 200) {
            this.$message.success('楼栋信息更新成功')
            this.logOperation(`更新楼栋信息：${this.buildingForm.name}`, '楼栋管理')
            // 重新加载楼栋列表
            await this.loadAvailableBuildings()
          } else {
            this.$message.error(response.message || '更新楼栋失败')
          }
        } else {
          // 添加楼栋
          const response = await createBuildingAPI({
            buildingName: this.buildingForm.name,
            buildingCode: 'B' + Date.now(), // 自动生成楼栋编码
            buildingType: buildingType, // 中文：男/女
            description: this.buildingForm.description,
            status: 1
          })
          
          if (response.code === 200) {
            this.$message.success('楼栋添加成功')
            this.logOperation(`添加楼栋：${this.buildingForm.name}`, '楼栋管理')
            // 重新加载楼栋列表
            await this.loadAvailableBuildings()
          } else {
            this.$message.error(response.message || '添加楼栋失败')
          }
        }

        this.buildingDialogVisible = false
      } catch (error) {
        console.error('保存楼栋失败:', error)
        // 错误已经由request拦截器处理
      } finally {
        this.buildingSaving = false
      }
    },

    // 删除楼栋
    deleteBuilding(building) {
      // ✅ 不需要前端阻止删除已分配的楼栋
      // 删除后，管理员的楼栋列表会自动更新（因为楼栋已不存在）
      
      this.$confirm(`确定要删除楼栋"${building.name}"吗？删除后将无法恢复。`, '确认删除', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await deleteBuildingAPI(building.id)
          
          if (response.code === 200) {
            this.$message.success(`楼栋 ${building.name} 删除成功`)
            this.logOperation(`删除楼栋：${building.name}`, '楼栋管理')
            
            // 🔥 删除后刷新数据，管理员的楼栋列表会自动更新
            await this.loadAvailableBuildings()  // 刷新楼栋列表
            await this.loadAdmins()              // 刷新管理员列表，自动计算新的楼栋分配
          } else {
            this.$message.error(response.message || '删除楼栋失败')
          }
        } catch (error) {
          console.error('删除楼栋失败:', error)
          // 错误已经由request拦截器处理
        }
      }).catch(() => {
        // 用户取消删除
      });
    },

    // 快速分配楼栋
    quickAssignBuilding(building) {
      this.currentQuickAssignBuilding = building;
      // 获取当前分配的管理员
      const assignee = this.getBuildingAssigneeUsername(building.id);
      this.quickAssignForm.adminUsername = assignee || '';
      this.quickAssignVisible = true;
    },

    // 保存快速分配
    async saveQuickAssign() {
      if (!this.currentQuickAssignBuilding) return;
      
      this.quickAssignSaving = true;
      
      try {
        const buildingId = this.currentQuickAssignBuilding.id;
        const newAdminUsername = this.quickAssignForm.adminUsername;
        
        // 查找新管理员的ID
        const newAdmin = newAdminUsername ? 
          this.admins.find(a => a.username === newAdminUsername) : null;
        
        if (newAdminUsername && !newAdmin) {
          this.$message.error('选择的管理员不存在');
          return;
        }
        
        // 使用新的简化API分配楼栋（更新building表的manager_name）
        const adminName = newAdmin ? newAdmin.username : '';  // 🔥 修复：传递username而不是realName
        const adminPhone = newAdmin ? (newAdmin.phone || '') : '';
        
        try {
          // 调用新的API：直接更新building表的manager_name
          await assignBuildingManager(buildingId, adminName, adminPhone);
        } catch (assignError) {
          console.error('分配楼栋失败:', assignError);
          this.$message.error(assignError.message || '分配失败');
          return;
        }
        
        // 更新视图数据
        await this.loadAvailableBuildings(); // 重新加载楼栋以获取最新的manager_name
        await this.loadAdmins(); // 重新加载管理员（后端会自动返回最新的管理楼栋信息）
        
        // 关闭对话框并显示成功消息
        this.quickAssignVisible = false;
        
        // 使用真实姓名显示提示信息
        const displayName = newAdmin ? (newAdmin.realName || newAdmin.name || newAdmin.username) : '';
        const actionText = displayName ? 
          `已将 ${this.currentQuickAssignBuilding.name} 分配给 ${displayName}` :
          `已取消 ${this.currentQuickAssignBuilding.name} 的分配`;
        
        // 广播楼栋分配数据更新事件（传递username用于数据更新）
        window.dispatchEvent(new CustomEvent('buildingAssignmentUpdated', { 
          detail: { 
            adminName: adminName,  // 这里传递的是username
            buildingId: buildingId,
            action: adminName ? 'assign' : 'remove'
          } 
        }));
        
        this.$message.success(actionText);
        this.logOperation(actionText, '楼栋管理');
        
      } catch (error) {
        console.error('分配操作失败:', error);
        this.$message.error('分配失败，请重试');
      } finally {
        this.quickAssignSaving = false;
      }
    },

    // 获取楼栋分配者姓名（显示用，返回真实姓名）
    getBuildingAssignee(buildingId) {
      // 从building表的managerName字段读取（存储的是username）
      const building = this.availableBuildings.find(b => b.id === buildingId);
      const username = building?.managerName;
      
      if (!username) return null;
      
      // 根据username查找管理员，返回真实姓名用于显示
      const admin = this.admins.find(a => a.username === username);
      return admin ? (admin.realName || admin.name || username) : username;
    },

    // 获取楼栋分配者用户名
    getBuildingAssigneeUsername(buildingId) {
      // managerName存储的就是username，直接返回
      const building = this.availableBuildings.find(b => b.id === buildingId);
      return building?.managerName || null;
    },

    // 获取已分配楼栋数量
    getAssignedBuildingsCount() {
      const assignedBuildings = new Set();
      
      // 从管理员数据中统计已分配的楼栋
      this.admins.forEach(admin => {
        if (admin.buildingIds && Array.isArray(admin.buildingIds)) {
          admin.buildingIds.forEach(id => {
            if (this.availableBuildings.some(building => building.id === id)) {
              assignedBuildings.add(id);
            }
          });
        }
      });
      
      return assignedBuildings.size;
    },

    // 清理无效的楼栋分配数据（已由后端数据库外键约束处理）
    cleanupInvalidAssignments() {
      // 后端使用外键约束自动清理无效数据，此方法保留用于兼容性
      console.log('后端自动处理无效分配数据，无需手动清理');
    },

    
    // 删除管理员
    deleteAdmin(admin) {
      // 检查管理员是否还管理着楼栋
      if (admin.buildingIds && admin.buildingIds.length > 0) {
        const buildingNames = this.getBuildingNames(admin.buildingIds);
        
        this.$alert(
          `管理员"${admin.name}"目前还管理着以下楼栋，无法直接删除：\n\n${buildingNames}\n\n请先将这些楼栋重新分配给其他管理员或取消分配后，再删除该管理员。`,
          '无法删除',
          {
            confirmButtonText: '我知道了',
            type: 'warning',
            dangerouslyUseHTMLString: false
          }
        );
        return;
      }
      
      this.$confirm(`确定要删除管理员"${admin.name}"吗？删除后将无法恢复。`, '确认删除', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await deleteAdminAPI(admin.id);
          
          if (response.code === 200) {
            // 后端CASCADE删除会自动清除楼栋分配，无需手动调用
            
            // 重新加载管理员数据
            await this.loadAdmins();
            
            this.$message.success(`管理员 ${admin.name} 删除成功`);
            this.logOperation(`删除管理员：${admin.name}`, '管理员管理');
          } else {
            this.$message.error(response.message || '删除管理员失败');
          }
        } catch (error) {
          console.error('删除管理员失败:', error);
          // 错误已经由request拦截器显示，这里不再重复显示
        }
      }).catch(() => {
        // 用户取消删除
      });
    },
    
    // 获取楼栋名称字符串
    getBuildingNames(buildingIds) {
      if (!buildingIds || buildingIds.length === 0) return '未分配';
      const names = buildingIds.map(id => {
        const building = this.availableBuildings.find(b => b.id === id);
        return building ? building.name : `楼栋${id}`;
      });
      return names.join(', ');
    },
    
      // 检查楼栋是否被其他管理员占用
    isBuildingOccupiedByOthers(buildingId, excludeAdminUsername = null) {
      // 🔥 修复：managerName字段存储的是username，直接比较username
      const building = this.availableBuildings.find(b => b.id === buildingId);
      if (!building || !building.managerName || building.managerName.trim() === '') {
        return false; // 未分配或managerName为空，不算占用
      }
      
      // 如果有excludeAdminUsername，检查是否为同一管理员
      if (excludeAdminUsername && building.managerName === excludeAdminUsername) {
        return false; // 是同一管理员，不算占用
      }
      
      return true; // 被其他管理员占用
    },
    
    // 楼栋搜索相关方法
    handleBuildingSearch() {
      // 搜索逻辑由计算属性 filteredBuildings 自动处理
      // 这个方法主要用于触发搜索（如果需要额外逻辑可以在这里添加）
    },
    
    clearBuildingSearch() {
      this.buildingSearchKeyword = '';
    },
    
    clearAllBuildingSearch() {
      this.buildingSearchKeyword = '';
      this.buildingSearchType = '';
      this.buildingSearchStatus = '';
    },

    // 专业分配相关方法
    showMajorAssignmentDialog(building) {
      this.currentAssignmentBuilding = building
      this.majorAssignmentDialogVisible = true
      this.loadAvailableMajorsForAssignment()
    },

    async loadAvailableMajorsForAssignment() {
      if (!this.currentAssignmentBuilding) return
      
      // 🔥 修复：从后端API获取学生数据
      let studentData = [];
      try {
        const response = await getStudentList({
          currentPage: 1,
          pageSize: 10000, // 获取所有学生
          gender: this.currentAssignmentBuilding.gender, // 只获取对应性别的学生
          registrationStatus: '' // 不过滤注册状态，查询所有学生
        });
        
        if (response.code === 200 && response.data && response.data.records) {
          studentData = response.data.records;
        } else {
          this.$message.warning('获取学生数据失败，无法进行专业分配');
          return;
        }
      } catch (error) {
        console.error('获取学生数据失败:', error);
        this.$message.error('获取学生数据失败');
        return;
      }
      
      // 按专业和性别分组统计学生
      const majorStats = {}
      studentData.forEach(student => {
        if (!majorStats[student.major]) {
          majorStats[student.major] = {
            name: student.major,
            gender: student.gender,
            studentCount: 0,
            assignedCapacity: 0
          }
        }
        majorStats[student.major].studentCount++
      })

      try {
        // 调用后端API获取当前楼栋的专业分配
        const response = await getMajorAssignments(this.currentAssignmentBuilding.buildingId)
        
        if (response.code === 200) {
          const currentAssignments = response.data || []
          
          // 更新已分配的容量
          currentAssignments.forEach(assignment => {
            if (majorStats[assignment.majorName]) {
              majorStats[assignment.majorName].assignedCapacity = assignment.allocatedStudents || 0
            }
          })
          
          this.availableMajorsForAssignment = Object.values(majorStats)
          this.selectedMajorsForAssignment = currentAssignments.map(a => a.majorName)
        } else {
          // API失败时使用空数据
          this.availableMajorsForAssignment = Object.values(majorStats)
          this.selectedMajorsForAssignment = []
        }
      } catch (error) {
        console.error('加载专业分配失败:', error)
        // 失败时使用本地数据初始化
        this.availableMajorsForAssignment = Object.values(majorStats)
        this.selectedMajorsForAssignment = []
      }
    },

    toggleMajorSelection(majorName) {
      const index = this.selectedMajorsForAssignment.indexOf(majorName)
      if (index > -1) {
        // 取消选择
        this.selectedMajorsForAssignment.splice(index, 1)
        // 重置分配容量
        const major = this.availableMajorsForAssignment.find(m => m.name === majorName)
        if (major) {
          major.assignedCapacity = 0
        }
      } else {
        // 选择专业
        this.selectedMajorsForAssignment.push(majorName)
        // 设置默认分配容量（不超过学生人数）
        const major = this.availableMajorsForAssignment.find(m => m.name === majorName)
        if (major) {
          major.assignedCapacity = Math.min(major.studentCount, this.remainingCapacity + (major.assignedCapacity || 0))
        }
      }
    },

    validateCapacityAllocation() {
      // 实时验证容量分配的合理性
      this.$nextTick(() => {
        // 触发计算属性更新
      })
    },

    async saveMajorAssignment() {
      if (this.remainingCapacity < 0 || this.selectedMajorsForAssignment.length === 0) {
        return
      }

      this.assignmentSaving = true

      try {
        // 准备保存的数据
        const assignments = this.selectedMajorsForAssignment.map(majorName => {
          const major = this.availableMajorsForAssignment.find(m => m.name === majorName)
          return {
            majorName: majorName,
            capacity: major.assignedCapacity || 0,
            allocatedStudents: major.assignedCapacity || 0,
            allocatedRooms: Math.ceil((major.assignedCapacity || 0) / 4), // 假设平均4人/间
            assignmentMode: 'class-only'
          }
        })

        // 调用后端API保存专业分配
        const response = await saveMajorAssignments(this.currentAssignmentBuilding.buildingId, assignments)
        
        if (response.code === 200) {
          this.$message.success('专业分配保存成功')
          this.majorAssignmentDialogVisible = false
          
          // 记录操作日志
          this.logOperation('专业分配', `为楼栋 ${this.currentAssignmentBuilding.buildingName} 分配了 ${assignments.length} 个专业`)
        } else {
          this.$message.error(response.message || '保存专业分配失败')
        }
      } catch (error) {
        console.error('保存专业分配失败:', error)
        this.$message.error('保存失败，请重试')
      } finally {
        this.assignmentSaving = false
      }
    },

    resetMajorAssignmentDialog() {
      this.currentAssignmentBuilding = null
      this.selectedMajorsForAssignment = []
      this.availableMajorsForAssignment = []
    },

    getUtilizationColor(percentage) {
      if (percentage >= 90) return '#f56c6c' // 红色 - 接近满载
      if (percentage >= 70) return '#e6a23c' // 橙色 - 较高利用率
      if (percentage >= 50) return '#409eff' // 蓝色 - 中等利用率
      return '#67c23a' // 绿色 - 低利用率
    },

    // 楼栋宿舍详情相关方法
    async loadBuildingDormsList() {
      if (!this.currentBuildingDorms) return
      
      try {
        // 调用后端API获取房间列表
        const response = await getRoomsByBuilding(this.currentBuildingDorms.buildingId)
        
        if (response.code === 200) {
          // 转换后端数据格式为前端格式
          this.buildingDormsList = (response.data || []).map(room => ({
            id: room.id,
            buildingId: room.buildingId,
            building: this.currentBuildingDorms.buildingName || this.currentBuildingDorms.name,
            room: room.roomNumber,
            roomNumber: room.roomNumber,
            type: this.convertRoomTypeToDisplay(room.roomType),
            roomType: room.roomType,
            totalBeds: room.bedCount,
            occupiedBeds: room.occupiedBeds || 0,
            availableBeds: room.availableBeds || room.bedCount,
            status: room.status,
            floor: room.floor
          })).sort((a, b) => {
            // 按楼层和房间号排序
            if (a.floor !== b.floor) {
              return a.floor - b.floor
            }
            return parseInt(a.room) - parseInt(b.room)
          })
        } else {
          this.$message.error('加载房间列表失败')
          this.buildingDormsList = []
        }
      } catch (error) {
        console.error('加载房间列表失败:', error)
        this.$message.error('加载房间列表失败')
        this.buildingDormsList = []
      }
    },
    
    // 辅助方法：转换房间类型为显示文本
    convertRoomTypeToDisplay(roomType) {
      const typeMap = {
        // 新格式（中文）
        '2人间': '双人间',
        '4人间': '四人间',
        '6人间': '六人间',
        '8人间': '八人间',
        '单人间': '单人间',
        '3人间': '三人间',
        // 兼容旧格式（英文）
        'double_bed': '双人间',
        'four_bed': '四人间',
        'six_bed': '六人间',
        'eight_bed': '八人间'
      }
      return typeMap[roomType] || roomType || '四人间'
    },

    handleDormSelectionChange(selection) {
      this.selectedDorms = selection
    },

    showBatchEditDialog() {
      if (this.selectedDorms.length === 0) {
        this.$message.warning('请先选择要编辑的宿舍')
        return
      }
      this.batchEditForm.action = 'edit'
      this.batchEditForm.roomType = ''
      this.batchEditDialogVisible = true
    },

    showBatchDeleteDialog() {
      if (this.selectedDorms.length === 0) {
        this.$message.warning('请先选择要删除的宿舍')
        return
      }
      this.batchEditForm.action = 'delete'
      this.batchEditDialogVisible = true
    },

    executeBatchOperation() {
      if (this.batchEditForm.action === 'edit') {
        this.executeBatchEdit()
      } else if (this.batchEditForm.action === 'delete') {
        this.executeBatchDelete()
      }
    },

    async executeBatchEdit() {
      if (!this.batchEditForm.roomType) {
        this.$message.warning('请选择新的房间类型')
        return
      }

      const bedCountMap = {
        '双人间': 2,
        '四人间': 4,
        '六人间': 6,
        '八人间': 8
      }
      const newBedCount = bedCountMap[this.batchEditForm.roomType] || 4
      
      // 转换房间类型为后端格式（新格式：中文）
      const roomTypeMap = {
        '双人间': '2人间',
        '四人间': '4人间',
        '六人间': '6人间',
        '八人间': '8人间'
      }
      const roomType = roomTypeMap[this.batchEditForm.roomType] || '4人间'

      // 获取选中房间的ID列表
      const roomIds = this.selectedDorms.map(dorm => dorm.id)
      
      if (roomIds.length === 0) {
        this.$message.warning('请选择要编辑的宿舍')
        return
      }

      try {
        // 调用后端API批量更新房间类型
        const response = await batchUpdateRoomType(roomIds, roomType, newBedCount)
        
        if (response.code === 200) {
          this.batchEditDialogVisible = false
          this.selectedDorms = []
          this.$message.success(`成功批量编辑 ${roomIds.length} 间宿舍为 ${this.batchEditForm.roomType}`)
          this.logOperation(`批量编辑宿舍：${this.currentBuildingDorms.buildingName} ${roomIds.length}间 → ${this.batchEditForm.roomType}`, '宿舍管理')
          
          // 重新加载当前楼栋宿舍列表
          await this.loadBuildingDormsList()
        } else {
          this.$message.error(response.message || '批量编辑失败')
        }
      } catch (error) {
        console.error('批量编辑房间失败:', error)
        this.$message.error('批量编辑失败')
      }
    },

    async executeBatchDelete() {
      // 检查是否有已入住的宿舍
      const occupiedDorms = this.selectedDorms.filter(dorm => dorm.occupiedBeds > 0)
      if (occupiedDorms.length > 0) {
        this.$message.error(`无法删除已有学生入住的宿舍，请先处理以下房间：${occupiedDorms.map(d => d.room).join(', ')}`)
        return
      }

      // 获取选中房间的ID列表
      const roomIds = this.selectedDorms.map(d => d.id)
      
      if (roomIds.length === 0) {
        this.$message.warning('请选择要删除的宿舍')
        return
      }

      try {
        // 调用后端API批量删除房间
        const response = await batchDeleteRooms(roomIds)
        
        if (response.code === 200) {
          this.batchEditDialogVisible = false
          const deletedCount = this.selectedDorms.length
          this.selectedDorms = []
          this.$message.success(`成功删除 ${deletedCount} 间宿舍`)
          this.logOperation(`批量删除宿舍：${this.currentBuildingDorms.buildingName} ${deletedCount}间`, '宿舍管理')
          
          // 重新加载当前楼栋宿舍列表
          await this.loadBuildingDormsList()
        } else {
          this.$message.error(response.message || '批量删除失败')
        }
      } catch (error) {
        console.error('批量删除房间失败:', error)
        this.$message.error('批量删除失败')
      }
    },

    editSingleDorm(dorm) {
      // 这里可以实现单个宿舍编辑功能
      this.$message.info('单个宿舍编辑功能即将完善')
    },
    
    // 重置表单
    resetForm() {
      this.adminForm = {
        name: '',
        username: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        buildings: []
      };
    },

    // 设置数据同步监听器
    setupDataSyncListener() {
      // 监听来自普通管理员端的宿舍数据更新
      window.addEventListener('dormDataUpdated', (event) => {
        console.log('超级管理员端接收到宿舍数据更新通知:', event.detail)
        
        // 如果楼栋宿舍详情对话框正在显示，重新加载数据
        if (this.buildingDormsDialogVisible && this.currentBuildingDorms) {
          this.loadBuildingDormsList()
        }
      })
    }
  }
}
</script>

<style scoped>
.admin-management-page {
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

.action-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
  padding: 6px 12px;
  font-size: 12px;
}

/* 表格样式优化 */
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

/* 楼栋搜索区域样式 */
.building-search-area {
  margin-bottom: 20px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.search-input {
  font-size: 14px;
}

.search-result-tip {
  margin-top: 15px;
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #606266;
}

.search-result-tip .el-tag {
  margin-right: 8px;
}

.search-result-tip .el-button {
  font-size: 12px;
  color: #409eff;
  padding: 0;
  margin-left: 5px;
}

.search-result-tip .el-button:hover {
  color: #66b1ff;
}

:deep(.el-table--border) {
  border: 1px solid #ebeef5;
}

/* 邮箱列样式 */
:deep(.el-table .cell) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 对话框样式 */
:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-dialog__header) {
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  padding: 20px 24px;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  color: #303133;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #e4e7ed;
  padding: 16px 24px;
}

/* 表单样式 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input__inner) {
  border-radius: 6px;
}

:deep(.el-select) {
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-management-page {
    padding: 10px;
  }
  
  .header-actions {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
  
  .buildings-grid {
    grid-template-columns: 1fr;
  }
  
  .assignment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .management-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

/* 楼栋分配相关样式 */
.building-assignment-content {
  padding: 0;
}

.assignment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.assignment-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.buildings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.building-item {
  border: 2px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.building-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.building-item.assigned {
  border-color: #67c23a;
  background-color: #f0f9ff;
}

.building-item.occupied {
  border-color: #f56c6c;
  background-color: #fef0f0;
  cursor: not-allowed;
}

.building-info h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.building-desc {
  color: #606266;
  font-size: 14px;
  margin: 8px 0;
}

.assignment-status {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.assigned-icon {
  color: #67c23a;
  font-size: 20px;
}

.occupied-text {
  font-size: 12px;
  color: #f56c6c;
}

/* 楼栋管理相关样式 */
.building-management-content {
  padding: 0;
}

.management-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.management-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

/* 简化后的样式 */
.buildings-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.no-buildings {
  color: #909399;
  font-style: italic;
}

.buildings-note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.password-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 10px 12px;
  background-color: #f0f9ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  margin-top: 8px;
}

.password-tip span {
  flex: 1;
}

.header-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 6px;
}


/* 页面头部优化 */
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 12px;
}

/* 楼栋管理对话框样式 */
.building-management-container {
  padding: 0;
}

.building-tabs :deep(.el-tabs__content) {
  padding-top: 0;
}

.dorm-management-content {
  padding: 20px;
}

.dorm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.dorm-actions {
  display: flex;
  gap: 12px;
}

.super-dorm-list {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.placeholder-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
}

.count-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #606266;
}

.count-display.occupied {
  color: #67c23a;
}

.count-display .el-icon {
  font-size: 14px;
}

.occupancy-rate {
  display: flex;
  justify-content: center;
}

.dorms-table {
  margin-top: 30px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.table-actions {
  display: flex;
  gap: 12px;
}

/* 宿舍管理标签页专用样式 */
.building-management-container .management-header {
  margin-bottom: 24px;
}

.building-management-container .management-header .header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.building-management-container .management-header .header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

/* 楼栋信息标签页的图标样式 */
.building-management-container .el-tab-pane[name="building-info"] .header-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

/* 宿舍管理标签页的图标样式 */
.building-management-container .el-tab-pane[name="dorm-management"] .header-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.building-management-container .management-header .header-text h2 {
  margin: 0 0 4px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

/* 楼栋信息标签页的标题颜色 */
.building-info-title {
  color: #ffffff !important;
  text-shadow: 0 2px 8px rgba(79, 172, 254, 0.3) !important;
}

/* 宿舍管理标签页的标题颜色 */
.dorm-management-title {
  color: #ffffff !important;
  text-shadow: 0 2px 8px rgba(67, 233, 123, 0.3) !important;
}

.building-management-container .management-header .header-text p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.building-management-container .statistics-overview {
  margin-bottom: 30px;
  padding: 24px;
  background: linear-gradient(135deg, #fafbfc 0%, #e8ecf0 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.building-management-container .statistics-overview .el-statistic {
  text-align: center;
}

.building-management-container .statistics-overview .el-statistic__head {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.building-management-container .statistics-overview .el-statistic__content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.building-management-container .statistics-overview .el-statistic__number {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}

.building-management-container .statistics-overview .el-statistic__suffix {
  font-size: 18px;
  font-weight: 500;
  color: #909399;
}

.management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #43cea2 0%, #185a9d 100%);
  border-radius: 12px;
  color: white;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 15px;
}


.header-icon {
  font-size: 24px;
  color: rgba(255, 255, 255, 0.9);
}

.header-text h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-text p {
  margin: 5px 0 0 0;
  opacity: 0.8;
  font-size: 14px;
}

.building-stats {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.building-stats :deep(.el-statistic__head) {
  color: #606266;
  font-size: 14px;
}

.building-stats :deep(.el-statistic__content) {
  color: #303133;
  font-weight: 600;
}

.buildings-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.building-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.building-name .el-icon {
  color: #409eff;
}

.assignment-status .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.building-actions {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
  align-items: center;
}

/* 快速分配对话框样式 */
.quick-assign-content {
  padding: 10px 0;
}

.assign-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.building-icon-large {
  font-size: 32px;
  color: #409eff;
}

.assign-details h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
}

.assign-details p {
  margin: 0;
  font-size: 13px;
  color: #606266;
}

/* 按钮优化 */
.action-buttons .el-button {
  margin: 2px;
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.action-buttons .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.action-buttons .el-button--info {
  background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
  border-color: #409eff;
  color: white;
}

.action-buttons .el-button--info:hover {
  background: linear-gradient(135deg, #337ecc 0%, #5aa3e6 100%);
  border-color: #337ecc;
}

.action-buttons .el-button--danger {
  background: linear-gradient(135deg, #f56c6c 0%, #ff8a8a 100%);
  border-color: #f56c6c;
  color: white;
}

.action-buttons .el-button--danger:hover {
  background: linear-gradient(135deg, #e85656 0%, #f07777 100%);
  border-color: #e85656;
}

.action-buttons .el-button--success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  border-color: #67c23a;
  color: white;
}

.action-buttons .el-button--success:hover {
  background: linear-gradient(135deg, #5daf34 0%, #7bc956 100%);
  border-color: #5daf34;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .header-buttons {
    justify-content: center;
  }
  
  .management-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .building-stats :deep(.el-col) {
    margin-bottom: 16px;
  }
  
  .building-actions {
    flex-direction: column;
    gap: 4px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
}

/* 专业分配相关样式 */
.major-assignment-section {
  margin: 20px 0;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.major-assignment-section .section-header {
  margin-bottom: 20px;
}

.major-assignment-section .section-header h3 {
  color: #303133;
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.major-assignment-section .section-header p {
  color: #909399;
  margin: 0;
  font-size: 14px;
}

.assigned-majors {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  align-items: center;
}

.major-tag {
  margin: 0 !important;
}

/* 专业分配对话框样式 */
.major-assignment-dialog {
  padding: 10px 0;
}

.building-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.building-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.building-header .el-icon {
  font-size: 24px;
  color: #409eff;
}

.building-details h3 {
  margin: 0 0 4px 0;
  color: #303133;
  font-size: 18px;
}

.building-details p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.assignment-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.available-majors h4 {
  color: #303133;
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 600;
}

.major-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.major-item {
  padding: 15px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.3s;
}

.major-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.major-item.selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.major-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.major-name {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.student-count {
  color: #909399;
  font-size: 13px;
}

.major-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.gender-info {
  padding: 2px 8px;
  background: #f0f9ff;
  color: #1d4ed8;
  border-radius: 4px;
  font-size: 12px;
}

.capacity-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.assignment-summary {
  padding: 15px;
  background: #f0f9ff;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.assignment-summary h4 {
  color: #303133;
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 600;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 15px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.stat-item .label {
  color: #606266;
  font-size: 13px;
}

.stat-item .value {
  color: #303133;
  font-weight: 600;
  font-size: 14px;
}

.stat-item .value.warning {
  color: #f56c6c;
}

/* 楼栋宿舍详情对话框样式 */
.building-dorms-dialog {
  padding: 20px 0;
}

.building-info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.building-basic-info h3 {
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
}

.info-stats {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.info-stats .stat {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.batch-actions {
  display: flex;
  gap: 10px;
}

.batch-actions .el-button {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: #333;
}

.batch-actions .el-button:hover {
  background: rgba(255, 255, 255, 1);
}

.dorms-list {
  margin-top: 20px;
}

.batch-info {
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 15px;
}

.batch-info p {
  margin: 0 0 10px 0;
  color: #303133;
  font-weight: 600;
}

.selected-rooms {
  max-height: 120px;
  overflow-y: auto;
}

.selected-rooms .el-tag {
  margin: 2px 4px 2px 0;
}
</style>
