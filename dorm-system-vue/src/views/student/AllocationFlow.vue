<template>
  <div class="allocation-flow">
    <!-- 步骤指示器 -->
    <div class="steps-container">
      <el-steps :active="currentStep" align-center>
        <el-step title="组建团队" description="寻找室友组建团队"></el-step>
        <el-step title="选择房间" description="舍长选择宿舍房间"></el-step>
        <el-step title="查看结果" description="确认分配结果"></el-step>
      </el-steps>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 步骤1: 组建室友团队 -->
      <el-card v-if="currentStep === 0" class="step-card">
        <template #header>
          <div class="card-header">
            <h3>组建室友团队</h3>
            <div class="header-right">
              <span class="status-tag" :class="teamStatus">{{ getTeamStatusText() }}</span>
            </div>
          </div>
        </template>
        
        <!-- 当前团队状态 -->
        <div class="team-info">
          <div class="assigned-area">
            <h4>分配区域信息</h4>
            <!-- 区域未分配提示 -->
            <el-alert 
              v-if="assignedArea.areaAssigned === false" 
              type="warning" 
              :closable="false"
              show-icon
              style="margin-bottom: 12px;"
            >
              <template #title>
                楼栋管理员正在划分宿舍区域，请耐心等待
              </template>
              <template #default>
                您的班级已分配到 {{ assignedArea.building || '待定' }}，但具体楼层和房间尚未划分完成。
              </template>
            </el-alert>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="专业班级">
                {{ studentInfo.major }} - {{ studentInfo.class }}
                <el-tag v-if="assignedArea.classSegment" type="warning" size="small" style="margin-left: 8px;">
                  {{ assignedArea.classSegment }}组
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="分配楼栋">{{ assignedArea.building || '等待分配' }}</el-descriptions-item>
              <el-descriptions-item label="分配楼层">
                <span v-if="assignedArea.areaAssigned === false" style="color: #E6A23C;">等待分配</span>
                <span v-else>{{ assignedArea.floors }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="可选房间">
                <span v-if="assignedArea.areaAssigned === false" style="color: #E6A23C;">等待分配</span>
                <span v-else>{{ assignedArea.availableRooms }}间</span>
              </el-descriptions-item>
              <el-descriptions-item v-if="assignedArea.studentIdRange" label="学号范围" :span="2">
                <el-tag type="info" size="small">{{ assignedArea.studentIdRange }}</el-tag>
                <span style="color: #909399; font-size: 12px; margin-left: 8px;">
                  （班级分组：您所在的组包含这些学号的同学）
                </span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div class="current-team">
            <!-- 数据加载中时显示加载状态 -->
            <div v-if="loading && !assignedArea.roomType" style="padding: 20px; text-align: center; color: #909399;">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span style="margin-left: 8px;">加载团队信息中...</span>
            </div>
            <!-- 数据加载完成后显示团队卡片 -->
            <template v-else-if="assignedArea.roomType">
              <h4>当前团队 ({{ currentTeam.members.length }}/{{ getRequiredTeamSize() }}人)</h4>
              <div class="team-members" :data-team-size="getRequiredTeamSize()">
              <div v-for="member in currentTeam.members" :key="member.id" class="member-card">
                <!-- 操作菜单（右上角） -->
                <div class="member-actions-dropdown" v-if="canOperateMember(member)" :style="{ display: 'block', visibility: 'visible', zIndex: 1000 }">
                  <el-dropdown @command="(command) => handleMemberOperation(command, member)" trigger="click" :popper-options="{ placement: 'bottom-end', popperClass: 'member-action-dropdown-menu' }">
                    <el-button type="text" class="more-btn" circle title="操作菜单" style="pointer-events: auto; z-index: 1001;">
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <!-- 舍长操作其他成员 -->
                        <template v-if="!isMemberLeader(member) && isCurrentUserLeader()">
                          <el-dropdown-item command="remove">
                            <el-icon><Close /></el-icon>
                            移除成员
                          </el-dropdown-item>
                        </template>
                        
                        <!-- 舍长操作自己 -->
                        <template v-if="isMemberLeader(member) && isCurrentUser(member) && currentTeam.members.length > 1">
                          <el-dropdown-item command="transfer">
                            <el-icon><Switch /></el-icon>
                            转让舍长
                          </el-dropdown-item>
                        </template>
                        
                        <!-- 普通成员操作自己 -->
                        <template v-if="!isMemberLeader(member) && isCurrentUser(member)">
                          <el-dropdown-item command="leave">
                            <el-icon><Back /></el-icon>
                            退出队伍
                          </el-dropdown-item>
                        </template>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                
                <!-- 成员信息 -->
                <div class="member-content">
                  <el-avatar :src="member.avatar" :size="40">{{ member.name.charAt(0) }}</el-avatar>
                  <div class="member-info">
                    <div class="member-name-row">
                      <span class="member-name">{{ member.name }}</span>
                      <el-tag v-if="member.isLeader" type="success" size="small" class="leader-badge">舍长</el-tag>
                    </div>
                    <div class="member-detail">{{ member.studentId }}</div>
                    <div class="member-detail">{{ member.major }}</div>
                  </div>
                </div>
              </div>
              
              <!-- 空位显示 -->
              <div v-for="i in (getRequiredTeamSize() - currentTeam.members.length)" :key="'empty-' + i" class="member-card empty">
                <div class="member-content">
                  <el-avatar :size="40">
                    <el-icon><Plus /></el-icon>
                  </el-avatar>
                  <div class="member-info">
                    <div class="member-name">待加入</div>
                  </div>
                </div>
              </div>
            </div>
            </template>
          </div>
        </div>
        
        <!-- 操作区域 -->
        <div class="team-actions">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="邀请室友" name="invite">
              <div class="invite-section">
                <el-form :model="inviteForm" inline @submit.prevent>
                  <el-form-item label="学号">
                    <el-input 
                      v-model="inviteForm.studentId" 
                      placeholder="输入室友学号" 
                      style="width: 200px"
                      @blur="searchStudentByStudentId"
                      @input="handleStudentIdInput"
                      @paste="handlePaste"
                      @keyup.enter="handleEnterKey"
                      clearable
                    ></el-input>
                  </el-form-item>
                  <el-form-item>
                    <el-button 
                      type="primary" 
                      @click="sendInvite" 
                      :loading="inviting" 
                      :disabled="!searchedStudent || isTeamFull()"
                    >
                      发送邀请
                    </el-button>
                    <span v-if="isTeamFull()" style="margin-left: 8px; color: #f56c6c; font-size: 12px;">
                      团队已满，无法继续邀请
                    </span>
                  </el-form-item>
                </el-form>
                
                <!-- 显示查询到的学生信息 -->
                <div v-if="searchedStudent" class="student-info-card" style="margin-top: 16px; padding: 12px; background: #f5f7fa; border-radius: 4px;">
                  <div style="display: flex; align-items: center; gap: 12px;">
                    <el-avatar :src="searchedStudent.avatar" :size="40">{{ searchedStudent.realName?.charAt(0) }}</el-avatar>
                    <div style="flex: 1;">
                      <div style="font-weight: 500; margin-bottom: 4px;">{{ searchedStudent.realName }}</div>
                      <div style="font-size: 12px; color: #909399;">
                        <span>学号：{{ searchedStudent.studentId }}</span>
                        <span style="margin-left: 12px;">专业：{{ searchedStudent.major || '-' }}</span>
                        <span style="margin-left: 12px;">班级：{{ searchedStudent.className || '-' }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 查询中提示 -->
                <div v-if="searchingStudent" style="margin-top: 8px; color: #909399; font-size: 12px;">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span style="margin-left: 4px;">查询中...</span>
                </div>
                
                <!-- 未找到学生提示（仅在查询完成后且确实未找到时显示，避免闪烁） -->
                <div v-if="inviteForm.studentId && !searchingStudent && !searchedStudent && inviteForm.studentId.trim().length >= 10 && inviteForm.studentId.trim() !== studentInfo.studentId" 
                     style="margin-top: 8px; color: #f56c6c; font-size: 12px;">
                  <el-icon><WarningFilled /></el-icon>
                  <span style="margin-left: 4px;">未找到该学号对应的学生</span>
                </div>
                
                <!-- 不能邀请自己的提示（单独处理，避免与查询状态混淆） -->
                <div v-if="inviteForm.studentId && inviteForm.studentId.trim() === studentInfo.studentId && inviteForm.studentId.trim().length >= 10" 
                     style="margin-top: 8px; color: #f56c6c; font-size: 12px;">
                  <el-icon><WarningFilled /></el-icon>
                  <span style="margin-left: 4px;">不能邀请自己</span>
                </div>
                
                <div class="invite-history" v-if="sentInvites.length > 0">
                  <h5>已发送邀请</h5>
                  <div class="sent-invite-table-container">
                    <el-table :data="sentInvites" size="small" max-height="400" style="width: 100%">
                      <el-table-column prop="studentName" label="姓名" width="100" align="center" header-align="center"></el-table-column>
                      <el-table-column prop="studentId" label="学号" width="120" align="center" header-align="center"></el-table-column>
                      <el-table-column prop="major" label="专业" min-width="140" align="center" header-align="center">
                        <template #default="scope">
                          {{ scope.row.major || '-' }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="class" label="班级" width="120" align="center" header-align="center">
                        <template #default="scope">
                          {{ scope.row.class && scope.row.class !== '-' ? scope.row.class : '-' }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="status" label="状态" width="100" align="center" header-align="center">
                        <template #default="scope">
                          <el-tag :type="getInviteStatusType(scope.row.status)" size="small">
                            {{ scope.row.status }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column prop="sentTime" label="发送时间" width="150" align="center" header-align="center"></el-table-column>
                      <el-table-column label="操作" width="120" align="center" header-align="center">
                        <template #default="scope">
                          <el-button 
                            v-if="scope.row.status === '待回复' || scope.row.originalStatus === 'PENDING'" 
                            type="danger" 
                            size="small" 
                            @click="cancelInvite(scope.row.id)"
                          >
                            撤回
                          </el-button>
                          <el-button 
                            v-else-if="scope.row.status === '已撤回' || scope.row.originalStatus === 'CANCELLED' || scope.row.status === '已拒绝' || scope.row.originalStatus === 'REJECTED' || scope.row.status === '已接受' || scope.row.originalStatus === 'ACCEPTED'" 
                            type="danger" 
                            size="small" 
                            plain
                            @click="deleteInvite(scope.row.id)"
                          >
                            删除
                          </el-button>
                          <span v-else style="color: #909399; font-size: 12px;">-</span>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="收到邀请" name="received">
              <div class="received-section">
                <div v-if="pendingReceivedInvites.length === 0" class="empty-state">
                  <el-empty description="暂无收到的邀请"></el-empty>
                </div>
                <div v-else class="invite-list-container">
                  <div v-for="invite in pendingReceivedInvites" :key="invite.id" class="invite-card">
                    <div class="invite-info">
                      <el-avatar :src="invite.senderAvatar" :size="40">{{ invite.senderName.charAt(0) }}</el-avatar>
                      <div class="invite-details">
                        <div class="sender-name">{{ invite.senderName }}</div>
                        <div class="sender-info">{{ invite.senderStudentId }} | {{ invite.senderMajor }}</div>
                        <div class="invite-time">{{ invite.sentTime }}</div>
                      </div>
                    </div>
                    <div class="invite-actions">
                      <el-button type="success" size="small" @click="acceptInvite(invite.id)">接受</el-button>
                      <el-button type="danger" size="small" @click="rejectInvite(invite.id)">拒绝</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="寻找室友" name="search">
              <div class="search-section">
                <!-- 搜索框 -->
                <el-form :model="searchForm" inline style="margin-bottom: 16px;">
                  <el-form-item label="搜索">
                    <el-input 
                      v-model="searchForm.keyword" 
                      placeholder="输入姓名或学号搜索同班同学" 
                      style="width: 280px"
                      clearable
                      @input="searchRoommates"
                    >
                      <template #prefix>
                        <el-icon><Search /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="searchRoommates">
                      <el-icon><Search /></el-icon>
                      搜索
                    </el-button>
                  </el-form-item>
                </el-form>
                
                <!-- 同班同学列表 -->
                <div class="search-results" v-if="searchResults.length > 0">
                  <div v-for="student in paginatedSearchResults" :key="student.id" class="student-card">
                    <el-avatar :src="student.avatar" :size="40">{{ student.name.charAt(0) }}</el-avatar>
                    <div class="student-info">
                      <div class="student-name">{{ student.name }}</div>
                      <div class="student-detail">{{ student.studentId }} | {{ student.major }} {{ student.class }}班</div>
                      <div class="student-tags">
                        <el-tag v-for="tag in student.tags" :key="tag" size="small">{{ tag }}</el-tag>
                      </div>
                    </div>
                    <div class="student-actions">
                      <!-- 已邀请状态 -->
                      <el-button 
                        v-if="isStudentInvited(student.id)" 
                        size="small" 
                        disabled
                        type="success"
                        plain
                      >
                        <el-icon><Check /></el-icon>
                        已邀请
                      </el-button>
                      <!-- 邀请中状态 -->
                      <el-button 
                        v-else-if="isStudentInviting(student.id)" 
                        size="small" 
                        loading
                        type="primary"
                      >
                        发送中...
                      </el-button>
                      <!-- 正常邀请按钮 -->
                      <el-button 
                        v-else
                        type="primary" 
                        size="small" 
                        @click="inviteStudent(student.id)"
                        :disabled="isTeamFull()"
                      >
                        邀请
                      </el-button>
                    </div>
                  </div>
                  
                  <!-- 分页组件 -->
                  <div class="pagination-container" style="margin-top: 20px; text-align: center;">
                    <el-pagination
                      v-model:current-page="searchCurrentPage"
                      v-model:page-size="searchPageSize"
                      :page-sizes="[7, 12]"
                      :total="searchResults.length"
                      layout="total, sizes, prev, pager, next, jumper"
                      @size-change="handleSearchSizeChange"
                      @current-change="handleSearchPageChange"
                    />
                  </div>
                </div>
                <el-empty 
                  v-else 
                  description="暂无符合条件的同学" 
                  :image-size="100"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
        
        <!-- 团队操作 -->
        <div class="team-operations" v-if="currentTeam.members.length >= 1">
          <!-- 显示按钮（已满 + 是舍长 + 阶段已开启） -->
          <div v-if="canProceedToRoomSelection()" style="text-align: center;">
            <el-button 
              type="primary" 
              size="large" 
              @click="proceedToRoomSelection"
            >
              进入选房阶段
            </el-button>
          </div>
          
          <!-- 仅在团队已满时显示等待提示 -->
          <div v-else-if="isTeamFull()" style="text-align: center; color: #909399; padding: 16px;">
            <span v-if="!isCurrentUserLeader()">
              等待舍长 {{ getLeaderName() }} 进入选房阶段
            </span>
            <span v-else-if="systemPhase !== 'room_selection'">
              团队已满，等待管理员开启房间选择阶段
            </span>
          </div>
        </div>
      </el-card>

      <!-- 步骤2: 选择宿舍房间 -->
      <el-card v-if="currentStep === 1" class="step-card">
        <template #header>
          <div class="card-header">
            <h3>选择宿舍房间</h3>
            <span class="status-tag" :class="roomSelectionStatus">{{ getRoomSelectionStatusText() }}</span>
          </div>
        </template>
        
        <!-- 团队信息展示 -->
        <div class="team-summary">
          <h4>团队信息</h4>
          <div class="team-members-summary">
            <el-tag v-for="member in currentTeam.members" :key="member.id" :type="member.isLeader ? 'success' : 'info'">
              {{ member.name }}{{ member.isLeader ? '(舍长)' : '' }}
            </el-tag>
          </div>
        </div>
        
        <!-- 可选房间列表 -->
        <div class="room-selection">
          <h4>可选房间 ({{ assignedArea.building }} {{ assignedArea.floors }})</h4>
          <div class="room-grid" v-if="availableRooms.length > 0">
            <div 
              v-for="room in availableRooms" 
              :key="room.id" 
              class="room-card"
              :class="{ 
                'selected': selectedRoom?.id === room.id,
                'occupied': room.status === 'occupied',
                'reserved': room.status === 'reserved'
              }"
              @click="selectRoom(room)"
            >
              <div class="room-number">{{ room.number }}</div>
              <div class="room-info">
                <div class="room-floor">{{ room.floor }}楼</div>
                <div class="room-type">{{ formatRoomType(room.type) }}</div>
                <div class="room-status">
                  <el-tag 
                    :type="getRoomStatusType(room.status)" 
                    size="small"
                  >
                    {{ getRoomStatusText(room.status) }}
                  </el-tag>
                </div>
              </div>
              <div class="room-features">
                <el-icon v-if="room.阳台" title="阳台"><House /></el-icon>
                <el-icon v-if="room.空调" title="空调"><MostlyCloudy /></el-icon>
                <el-icon v-if="room.独立卫浴" title="独立卫浴"><Setting /></el-icon>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无符合条件的可选房间" />
        </div>
        
        <!-- 房间详情 -->
        <div v-if="selectedRoom" class="room-details">
          <h4>房间详情 - {{ selectedRoom.number }}</h4>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="房间号">{{ selectedRoom.number }}</el-descriptions-item>
                <el-descriptions-item label="楼层">{{ selectedRoom.floor }}楼</el-descriptions-item>
                <el-descriptions-item label="房间类型">{{ selectedRoom.type }}</el-descriptions-item>
              </el-descriptions>
            </el-col>
            <el-col :span="12">
              <div class="room-features-detail">
                <h5>房间设施</h5>
                <div class="features-list">
                  <!-- 优先显示物资配置列表 -->
                  <template v-if="selectedRoom.materialsList && selectedRoom.materialsList.length > 0">
                    <el-tag 
                      v-for="(item, index) in selectedRoom.materialsList" 
                      :key="index" 
                      type="success"
                      style="margin-right: 8px; margin-bottom: 8px;"
                    >
                      {{ item.name }}{{ item.quantity > 1 ? ` x${item.quantity}` : '' }}
                    </el-tag>
                  </template>
                  <!-- 向后兼容：如果没有物资配置，显示旧的设施信息 -->
                  <template v-else>
                    <el-tag v-if="selectedRoom.阳台" type="success">阳台</el-tag>
                    <el-tag v-if="selectedRoom.空调" type="success">空调</el-tag>
                    <el-tag v-if="selectedRoom.独立卫浴" type="success">独立卫浴</el-tag>
                    <el-tag v-if="selectedRoom.WiFi" type="success">WiFi</el-tag>
                    <el-tag v-if="selectedRoom.暖气" type="success">暖气</el-tag>
                    <el-tag v-if="selectedRoom.热水器" type="success">热水器</el-tag>
                  </template>
                  <!-- 无配置时显示提示 -->
                  <span v-if="!selectedRoom.materialsList?.length && !selectedRoom.阳台 && !selectedRoom.空调 && !selectedRoom.独立卫浴" style="color: #909399;">
                    暂无设施配置
                  </span>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
        
        <!-- 选房操作 -->
        <div class="room-actions">
          <div v-if="isCurrentUserLeader()">
            <el-button 
              type="primary" 
              size="large" 
              :disabled="!selectedRoom || selectedRoom.status !== 'available'"
              @click="confirmRoomSelection"
              :loading="confirmingRoom"
            >
              确认选择此房间
            </el-button>
            <p class="action-note">作为舍长，您可以为团队选择房间</p>
          </div>
          <div v-else class="waiting-tip">
            <div class="waiting-tip-content">
              <el-icon class="waiting-icon"><InfoFilled /></el-icon>
              <div class="waiting-text">
                <p class="waiting-title">等待舍长选择</p>
                <p class="waiting-desc">舍长 {{ getLeaderName() }} 正在为团队选择房间，请耐心等待</p>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 步骤3: 分配结果 -->
      <el-card v-if="currentStep === 2" class="step-card">
        <template #header>
          <h3>分配结果</h3>
        </template>
        
        <div class="result-content" v-if="allocationResult">
          <!-- 顶部状态提示卡片 -->
          <div 
            v-if="allocationPhase === 'adjustable'"
            class="status-info-card adjustable"
          >
            <div class="status-icon">
              <el-icon :size="28"><Timer /></el-icon>
            </div>
            <div class="status-text">
              <p class="status-title">当前处于自主分配调整期</p>
              <p class="status-desc">如需调整请在 <strong>{{ formatDeadlineTime(adjustmentDeadline) }}</strong> 前操作，舍长可重新选房</p>
            </div>
          </div>

          <div 
            v-else-if="allocationPhase === 'confirmed' && (systemPhase === 'admin_allocation' || systemPhase === 'result_viewing')"
            class="status-info-card confirmed"
          >
            <div class="status-icon">
              <el-icon :size="28"><SuccessFilled /></el-icon>
            </div>
            <div class="status-text">
              <p class="status-title">{{ systemPhase === 'admin_allocation' ? '管理员正在处理剩余分配' : '分配结果已确认' }}</p>
              <p class="status-desc">自主选房阶段已结束，如需调宿请通过"调宿申请"功能提交申请</p>
            </div>
          </div>

          <div 
            v-else-if="allocationPhase === 'confirmed'"
            class="status-info-card confirmed"
          >
            <div class="status-icon">
              <el-icon :size="28"><SuccessFilled /></el-icon>
            </div>
            <div class="status-text">
              <p class="status-title">分配结果已确认</p>
              <p class="status-desc">如需调宿，请通过"调宿申请"功能提交申请，由管理员审核处理</p>
            </div>
          </div>

          <div class="result-success-icon">
            <el-icon :size="64" :color="allocationResult.success ? '#67C23A' : '#E6A23C'">
              <component :is="allocationResult.success ? 'SuccessFilled' : 'WarningFilled'" />
            </el-icon>
          </div>
          
          <div class="result-title">
            <h2>{{ allocationResult.success ? '分配成功' : '分配待调整' }}</h2>
            <p class="result-subtitle">{{ allocationResult.message }}</p>
          </div>

          <!-- 简化的分配信息 -->
          <div class="result-summary">
            <div class="summary-item">
              <el-icon class="summary-icon"><OfficeBuilding /></el-icon>
              <div class="summary-info">
                <span class="summary-label">楼栋房间</span>
                <span class="summary-value">{{ allocationResult.building }} {{ allocationResult.roomNumber }}</span>
              </div>
            </div>
            <div class="summary-item">
              <el-icon class="summary-icon"><MostlyCloudy /></el-icon>
              <div class="summary-info">
                <span class="summary-label">楼层床位</span>
                <span class="summary-value">{{ allocationResult.floor }}层 / {{ allocationResult.bedNumber }}号</span>
              </div>
            </div>
            <div class="summary-item">
              <el-icon class="summary-icon"><User /></el-icon>
              <div class="summary-info">
                <span class="summary-label">室友数量</span>
                <span class="summary-value">{{ allocationResult.roommates?.length || 0 }}人</span>
              </div>
            </div>
          </div>

          <div class="result-actions">
            <el-button type="primary" size="large" @click="showDormDetails">
              <el-icon><View /></el-icon> 查看宿舍详情
            </el-button>
            
            <!-- 只在调整期显示舍长重新选房 -->
            <el-button v-if="allocationPhase === 'adjustable' && isLeader" size="large" @click="reSelectRoom">
              <el-icon><RefreshRight /></el-icon> 重新选房
            </el-button>
          </div>

          <div class="result-tip" v-if="allocationPhase === 'adjustable'">
            <el-icon><InfoFilled /></el-icon>
            <span>如对分配结果满意，无需操作。如需调整，舍长可重新选房</span>
          </div>
          <div class="result-tip" v-else>
            <el-icon><InfoFilled /></el-icon>
            <span>如对分配结果满意，无需操作。如需调宿，请前往"宿舍申请"页面提交申请</span>
          </div>
        </div>
        
        <!-- 数据加载中或未分配状态 -->
        <div v-else class="result-content">
          <el-empty 
            v-if="loading" 
            description="加载中..." 
            :image-size="100"
          />
          <!-- 等待管理员分配状态 -->
          <div v-else-if="allocationPhase === 'waiting'" class="waiting-admin-allocation">
            <div class="waiting-icon-simple">
              <el-icon :size="80" color="#409EFF"><Clock /></el-icon>
            </div>
            <div class="waiting-title">
              <h2>等待管理员分配</h2>
              <p class="waiting-subtitle">自主选房阶段已结束，管理员正在处理宿舍分配，请耐心等待</p>
            </div>
            <div class="waiting-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>请稍后刷新页面查看分配结果，或等待系统通知</span>
            </div>
            <div class="waiting-action">
              <el-button type="primary" @click="refreshAllocationResult" :loading="refreshing">
                <el-icon><Refresh /></el-icon> 刷新查看结果
              </el-button>
            </div>
          </div>
          <el-empty 
            v-else 
            description="暂未分配宿舍，请先完成前面的步骤" 
            :image-size="100"
          >
            <el-button type="primary" @click="currentStep = 0">返回组建团队</el-button>
          </el-empty>
        </div>
      </el-card>

      <!-- 宿舍详情对话框 -->
      <el-dialog
        v-model="dormDetailsVisible"
        title="宿舍详情"
        width="700px"
        :close-on-click-modal="false"
      >
        <div class="dorm-details" v-if="allocationResult">
          <!-- 基本信息 -->
          <div class="details-section">
            <h4 class="section-title">
              <el-icon><OfficeBuilding /></el-icon>
              <span>基本信息</span>
            </h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">楼栋</span>
                <span class="info-value">{{ allocationResult.building || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">房间号</span>
                <span class="info-value">{{ allocationResult.roomNumber || '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">楼层</span>
                <span class="info-value">{{ allocationResult.floor ? allocationResult.floor + '层' : '-' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">床位号</span>
                <span class="info-value">{{ allocationResult.bedNumber ? allocationResult.bedNumber + '号' : '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 房间设施 -->
          <div class="details-section">
            <h4 class="section-title">
              <el-icon><Setting /></el-icon>
              <span>房间设施</span>
            </h4>
            <div class="facilities-tags">
              <!-- 优先显示物资配置列表 -->
              <template v-if="dormMaterialsList && dormMaterialsList.length > 0">
                <el-tag 
                  v-for="(item, index) in dormMaterialsList" 
                  :key="index" 
                  type="success"
                  style="margin-right: 8px; margin-bottom: 8px;"
                >
                  {{ item.name }}{{ item.quantity > 1 ? ` x${item.quantity}` : '' }}
                </el-tag>
              </template>
              <!-- 向后兼容：显示旧的设施标签 -->
              <template v-else>
                <el-tag v-if="dormFacilities.阳台" type="success">阳台</el-tag>
                <el-tag v-if="dormFacilities.空调" type="success">空调</el-tag>
                <el-tag v-if="dormFacilities.独立卫浴" type="success">独立卫浴</el-tag>
                <el-tag v-if="dormFacilities.WiFi" type="success">WiFi</el-tag>
                <el-tag v-if="dormFacilities.暖气" type="success">暖气</el-tag>
                <el-tag v-if="dormFacilities.热水器" type="success">热水器</el-tag>
                <el-tag v-if="!hasAnyFacility" type="info">暂无特殊设施</el-tag>
              </template>
            </div>
          </div>

          <!-- 室友信息 -->
          <div class="details-section">
            <h4 class="section-title">
              <el-icon><User /></el-icon>
              <span>室友信息 ({{ allocationResult.roommates?.length || 0 }}人)</span>
            </h4>
            <div v-if="allocationResult.roommates && allocationResult.roommates.length > 0" class="roommates-grid">
              <div v-for="roommate in allocationResult.roommates" :key="roommate.studentId || roommate.name || roommate" class="roommate-item">
                <!-- 支持新格式（对象）和旧格式（字符串）的室友数据 -->
                <el-avatar :size="50" :src="roommate.avatar">
                  {{ typeof roommate === 'string' ? roommate.charAt(0) : (roommate.name?.charAt(0) || '?') }}
                </el-avatar>
                <span class="roommate-name">{{ typeof roommate === 'string' ? roommate : roommate.name }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无室友信息" :image-size="100" />
          </div>
        </div>
        <div v-else class="dorm-details">
          <el-empty description="暂无宿舍信息" :image-size="100" />
        </div>

        <template #footer>
          <el-button @click="dormDetailsVisible = false">关闭</el-button>
        </template>
      </el-dialog>

    </div>

    <!-- 转让舍长对话框 -->
    <el-dialog
      v-model="showTransferLeaderDialog"
      title="转让舍长"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item label="选择新舍长">
          <el-select v-model="selectedNewLeader" placeholder="请选择新舍长" style="width: 100%;">
            <el-option 
              v-for="member in currentTeam.members.filter(m => !m.isLeader)" 
              :key="member.id"
              :label="member.name + ' (' + member.studentId + ')'"
              :value="member.id"
            />
          </el-select>
        </el-form-item>
        <el-alert 
          type="warning" 
          :closable="false" 
          show-icon
          style="margin-top: 12px;"
        >
          转让后您将成为普通成员，新舍长将拥有队伍管理权限
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="showTransferLeaderDialog = false">取消</el-button>
        <el-button type="primary" @click="transferLeadership">确认转让</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script>
import { InfoFilled, SuccessFilled, WarningFilled, User, Check, Position, OfficeBuilding, MostlyCloudy, View, Setting, RefreshRight, Close, House, MoreFilled, Timer, Loading, Switch, Back, Clock, Refresh } from '@element-plus/icons-vue'

// 分配流程API
import {
  getAssignedArea,
  getCurrentTeam,
  sendInvite as sendInviteAPI,
  cancelInvite as cancelInviteAPI,
  getReceivedInvites,
  getSentInvites,
  acceptInvite as acceptInviteAPI,
  rejectInvite as rejectInviteAPI,
  searchClassmates,
  removeMember as removeMemberAPI,
  transferLeadership as transferLeadershipAPI,
  transferLeaderAndLeave as transferLeaderAndLeaveAPI,
  disbandGroupAndLeave as disbandGroupAndLeaveAPI,
  leaveTeam as leaveTeamAPI,
  getAvailableRooms as getAvailableRoomsAPI,
  confirmRoomSelection as confirmRoomSelectionAPI,
  cancelRoomSelection as cancelRoomSelectionAPI,
  getAllocationResult as getAllocationResultAPI,
  getSystemPhase
} from '@/api/studentAllocation'
import { getStudentByStudentId } from '@/api/student'

// 用户信息API
import { getProfile } from '@/api/profile'

export default {
  name: 'AllocationFlow',
  components: {
    InfoFilled, SuccessFilled, WarningFilled, User, Check, Position, OfficeBuilding, MostlyCloudy, View, Setting, RefreshRight, Close, House, MoreFilled, Timer, Loading, Switch, Back, Clock, Refresh
  },
  data() {
    // 从 localStorage 恢复步骤状态，防止刷新后丢失
    // 限制步骤范围为0-2（组建团队、选择房间、查看结果）
    let savedStep = parseInt(localStorage.getItem('allocation_current_step') || '0')
    if (savedStep < 0 || savedStep > 2) {
      savedStep = 0
      localStorage.setItem('allocation_current_step', '0')
    }
    return {
      currentStep: savedStep,
      transferSubmitting: false,
      showTransferHistory: false,
      inviting: false,
      confirmingRoom: false,
      activeTab: 'invite',
      loading: false,
      refreshing: false,
      
      // 转让舍长对话框
      showTransferLeaderDialog: false,
      selectedNewLeader: '',
      
      // 学生信息（从API获取）
      studentInfo: {
        id: null,
        name: '',
        studentId: '',
        major: '',
        class: ''
      },
      
      // 分配区域信息（从API获取）
      assignedArea: {
        building: '',
        floors: '',
        availableRooms: 0,
        roomType: '',
        buildingId: null,
        classSegment: null, // 班级分片标识（A/B/C）
        studentIdRange: null, // 学号范围
        areaAssigned: null // 区域是否已分配（楼栋管理员是否已完成划分）
      },
      
      // 系统阶段（从后端获取）
      systemPhase: 'self_grouping',
      
      // 当前团队（从API获取）
      currentTeam: {
        id: null,
        leaderId: null,
        leaderName: '',
        members: [],
        memberCount: 0,
        maxMembers: 4,
        status: 'incomplete'
      },
      
      teamStatus: 'forming',
      
      // 邀请表单
      inviteForm: {
        studentId: ''
      },
      
      // 学号查询结果
      searchedStudent: null,
      searchingStudent: false,
      searchTimer: null, // 防抖定时器
      
      // 已发送邀请（从API获取）
      sentInvites: [],
      
      // 收到的邀请（从API获取）
      receivedInvites: [],
      
      // 搜索表单
      searchForm: {
        keyword: ''
      },
      
      // 搜索结果（从API获取）
      searchResults: [],
      
      // 搜索结果分页
      searchCurrentPage: 1,
      searchPageSize: 7,
      
      // 邀请状态跟踪
      invitingStudents: [], // 正在邀请的学生ID列表
      invitedStudents: [],  // 已邀请的学生ID列表
      
      // 房间选择
      roomSelectionStatus: 'pending', // pending, selecting, completed
      selectedRoom: null,
      
      // 可选房间（从API获取）
      availableRooms: [],
      
      // 分配结果（从API获取）
      allocationResult: null,
      
      // 宿舍详情对话框
      dormDetailsVisible: false,
      dormFacilities: {},
      dormMaterialsList: [], // 物资配置列表
      isLeader: false,
      
      // 分配阶段管理
      allocationPhase: 'adjustable', // 'adjustable' | 'confirmed'
      adjustmentDeadline: null, // 调整截止时间
      adjustmentTimeLeft: '加载中...', // 剩余调整时间（显示用）
      timeUpdateTimer: null // 时间更新定时器
    }
  },
  
  async mounted() {
    // 1. 从API获取当前用户信息
    await this.loadUserInfo()
    
    // 2. 初始化页面数据
    await this.initPageData()
    
    // 3. 加载调整截止时间
    this.loadAdjustmentDeadline()
    
    // 4. 启动定时器，每分钟更新一次剩余时间
    this.timeUpdateTimer = setInterval(() => {
      this.updateAdjustmentTimeLeft()
    }, 60000)

    // 5. 启动数据自动刷新定时器（每5秒刷新一次）
    // 在所有步骤都检查系统阶段，以便及时响应管理员阶段切换
    this.dataRefreshTimer = setInterval(() => {
      // 始终检查系统阶段
      this.fetchSystemPhase()
      
      // 仅在组建团队阶段(Step 0)刷新团队和邀请信息
      if (this.currentStep === 0) {
        // 静默刷新，不显示loading状态
        this.loadTeamInfoSilent()
        this.loadInvitesSilent()
      }
    }, 5000)
    
    // 6. 监听WebSocket团队更新通知
    this.setupTeamUpdateListener()
  },
  
  beforeUnmount() {
    // 清除定时器
    if (this.timeUpdateTimer) {
      clearInterval(this.timeUpdateTimer)
    }
    if (this.dataRefreshTimer) {
      clearInterval(this.dataRefreshTimer)
    }

  },
  
  computed: {
    // 检查是否有任何设施
    hasAnyFacility() {
      return Object.values(this.dormFacilities).some(value => value === true)
    },
    
    // 可选作为新舍长的成员（排除当前用户，使用学号匹配）
    availableMembersForLeader() {
      const currentStudentId = this.studentInfo.studentId
      return this.currentTeam.members.filter(m => m.studentId !== currentStudentId)
    },
    
    // 过滤待处理的邀请（排除已接受和已拒绝的）
    pendingReceivedInvites() {
      return this.receivedInvites.filter(invite => 
        invite.status === 'pending' || !invite.status
      )
    },
    
    // 分页显示的搜索结果
    paginatedSearchResults() {
      const start = (this.searchCurrentPage - 1) * this.searchPageSize
      const end = start + this.searchPageSize
      return this.searchResults.slice(start, end)
    },
    
    // 判断截止时间是否紧急（小于24小时）
    isDeadlineUrgent() {
      if (!this.formingTeamsDeadline) return false
      const deadline = new Date(this.formingTeamsDeadline)
      const now = new Date()
      const diff = deadline - now
      return diff > 0 && diff < 24 * 60 * 60 * 1000 // 小于24小时
    }
  },
  
  watch: {
    // 监听步骤变化，自动加载对应数据
    async currentStep(newStep) {
      await this.loadStepData(newStep)
    }
  },
  
  methods: {
    /**
     * 处理步骤切换（内部使用，不再暴露给用户手动切换）
     */
    async handleStepChange(step) {
      // 限制步骤范围为0-2
      if (step < 0 || step > 2) {
        step = 0
      }
      this.currentStep = step
      localStorage.setItem('allocation_current_step', step.toString())
      await this.loadStepData(step)
    },
    
    /**
     * 根据步骤加载对应数据
     */
    async loadStepData(step) {
      try {
        if (step === 0) {
          await this.loadInvites()
          await this.searchRoommates()
        } else if (step === 1) {
          await this.loadAvailableRooms()
        } else if (step === 2) {
          // 如果是等待管理员分配状态，不需要重新加载
          if (this.allocationPhase !== 'waiting') {
            await this.loadAllocationResult()
          }
        }
      } catch (error) {
        console.error(`加载步骤 ${step} 数据失败`, error)
      }
    },
    /**
     * ==================== 生命周期方法 ====================
     */
    beforeDestroy() {
      // 清理定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
    },
    
    /**
     * ==================== API数据加载方法 ====================
     */
    
    /**
     * 获取用户信息
     */
    async loadUserInfo() {
      try {
        const res = await getProfile()
        if (res.code === 200 && res.data) {
          this.studentInfo = {
            id: res.data.id,
            name: res.data.realName || res.data.name,
            studentId: res.data.studentId,
            major: res.data.major,
            class: res.data.className
          }
        }
      } catch (error) {
        console.error('获取用户信息失败', error)
        this.$message.error('获取用户信息失败，请重新登录')
      }
    },
    
    /**
     * 初始化页面数据
     */
    async initPageData() {
      this.loading = true
      let hasShownError = false // 标记是否已显示错误提示
      try {
        // 并行加载基础信息（不抛出错误，允许部分失败）
        await Promise.all([
          this.loadAssignedArea().catch(error => {
            const errorMsg = error.response?.data?.message || error.message || ''
            if (!hasShownError && errorMsg !== '学生不存在') {
              hasShownError = true
              console.error('获取分配区域信息失败', errorMsg)
            }
          }),
          this.loadTeamInfo().catch(error => {
            const errorMsg = error.response?.data?.message || error.message || ''
            if (!hasShownError && errorMsg !== '学生不存在') {
              hasShownError = true
              console.error('获取团队信息失败', errorMsg)
            }
          }),
          this.fetchSystemPhase()
        ])
        
        // 检查是否已有分配结果（用于自动跳转到结果页面）
        // 这对于非舍长成员特别重要，当舍长选完房后，成员刷新页面应该自动跳转到结果页
        try {
          const resultRes = await getAllocationResultAPI()
          if (resultRes.code === 200 && resultRes.data) {
            if (resultRes.data.success) {
              // 已有有效分配结果，自动跳转到结果页面
              this.currentStep = 2
              localStorage.setItem('allocation_current_step', '2')
              this.allocationResult = resultRes.data
              this.allocationPhase = resultRes.data.allocationPhase || 'adjustable'
              this.adjustmentDeadline = resultRes.data.adjustmentDeadline
              this.isLeader = resultRes.data.isLeader || false
              this.dormFacilities = resultRes.data.facilities || {}
              
              // 解析物资配置列表
              if (resultRes.data.materials) {
                try {
                  this.dormMaterialsList = JSON.parse(resultRes.data.materials)
                } catch (e) {
                  this.dormMaterialsList = []
                }
              } else {
                this.dormMaterialsList = []
              }
              return // 已加载结果，直接返回
            } else {
              // 没有有效分配结果
              // 如果系统阶段是 admin_allocation 或 completed，跳转到步骤2显示等待页面
              if (this.systemPhase === 'admin_allocation' || this.systemPhase === 'completed') {
                this.currentStep = 2
                localStorage.setItem('allocation_current_step', '2')
                this.allocationResult = null
                this.allocationPhase = 'waiting' // 标记为等待管理员分配
                return
              }
              // 否则重置到步骤0
              if (this.currentStep === 2) {
                this.currentStep = 0
                localStorage.setItem('allocation_current_step', '0')
                this.allocationResult = null
              }
            }
          }
        } catch (e) {
          // 查询失败，继续正常流程
          console.warn('检查分配结果失败', e)
          // 如果系统阶段是 admin_allocation 或 completed，即使查询失败也跳转到步骤2
          if (this.systemPhase === 'admin_allocation' || this.systemPhase === 'completed') {
            this.currentStep = 2
            localStorage.setItem('allocation_current_step', '2')
            this.allocationResult = null
            this.allocationPhase = 'waiting'
            return
          }
        }
        
        // 如果当前步骤是选房阶段，恢复选房状态
        if (this.currentStep === 1) {
          this.roomSelectionStatus = 'selecting'
        }
        
        // 根据当前步骤加载对应数据
        if (this.currentStep === 0) {
          await this.loadInvites()
          await this.searchRoommates() // 加载同学列表
        } else if (this.currentStep === 1) {
          await this.loadAvailableRooms()
        } else if (this.currentStep === 2) {
          await this.loadAllocationResult()
        }
      } catch (error) {
        console.error('初始化页面数据失败', error)
        // 如果还没有显示错误提示，则显示一个通用错误
        if (!hasShownError) {
          const errorMsg = error.response?.data?.message || error.message || '加载数据失败，请刷新页面重试'
          this.$message.error(errorMsg)
        }
      } finally {
        this.loading = false
      }
    },
    
    /**
     * 获取系统选房阶段
     */
    async fetchSystemPhase() {
      try {
        const res = await getSystemPhase()
        if (res.code === 200 && res.data) {
          const newPhase = res.data.currentPhase || 'forming_teams'
          const oldPhase = this.systemPhase
          this.systemPhase = newPhase
          
          // 如果阶段变为 admin_allocation 或 completed，且当前不在步骤2，需要检查是否跳转
          if ((newPhase === 'admin_allocation' || newPhase === 'completed') && 
              oldPhase !== newPhase && 
              this.currentStep !== 2) {
            console.log('🔄 系统阶段变更为', newPhase, '，检查是否需要跳转到等待页面')
            // 检查是否有分配结果
            try {
              const resultRes = await getAllocationResultAPI()
              if (resultRes.code === 200 && resultRes.data && resultRes.data.success) {
                // 有分配结果，跳转到结果页
                this.currentStep = 2
                localStorage.setItem('allocation_current_step', '2')
                this.allocationResult = resultRes.data
                this.allocationPhase = resultRes.data.allocationPhase || 'confirmed'
              } else {
                // 没有分配结果，跳转到等待页面
                this.currentStep = 2
                localStorage.setItem('allocation_current_step', '2')
                this.allocationResult = null
                this.allocationPhase = 'waiting'
              }
            } catch (e) {
              // 查询失败，也跳转到等待页面
              this.currentStep = 2
              localStorage.setItem('allocation_current_step', '2')
              this.allocationResult = null
              this.allocationPhase = 'waiting'
            }
          }
        }
      } catch (error) {
        console.error('获取系统阶段失败', error)
        // 默认保持当前值或设为 'forming_teams'
      }
    },
    
    /**
     * 格式化截止时间（用于选房截止时间显示）
     */
    formatDeadlineTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${month}-${day} ${hours}:${minutes}`
    },

    /**
     * 静默加载团队信息（不显示loading，用于自动刷新）
     */
    async loadTeamInfoSilent() {
      try {
        const res = await getCurrentTeam()
        if (res.code === 200 && res.data) {
          // 只有当数据发生变化时才更新，避免界面闪烁（简单比较成员数量或ID）
          // 这里直接更新，Vue的Diff算法会处理DOM更新
          this.currentTeam = res.data
          
          // 确保 isLeader 字段正确设置
          if (this.currentTeam.members && this.currentTeam.leaderId) {
            const leaderId = Number(this.currentTeam.leaderId)
            this.currentTeam.members.forEach(member => {
              const memberId = member.id ? Number(member.id) : null
              if (memberId !== null && memberId === leaderId) {
                member.isLeader = true
              }
            })
          }
          
          // 更新团队状态
          if (this.currentTeam.memberCount >= this.currentTeam.maxMembers) {
            this.teamStatus = 'ready'
          } else {
            this.teamStatus = 'forming'
          }
        }
      } catch (error) {
        // 静默失败，不提示错误
        console.debug('自动刷新团队信息失败', error)
      }
    },

    /**
     * 静默加载邀请信息（不显示loading，用于自动刷新）
     */
    async loadInvitesSilent() {
      try {
        // 并行加载发送和接收的邀请
        const [sentRes, receivedRes] = await Promise.all([
          getSentInvites().catch(() => null),
          getReceivedInvites().catch(() => null)
        ])

        // 更新已发送邀请
        if (sentRes && sentRes.code === 200 && sentRes.data) {
          this.sentInvites = sentRes.data.map(invite => ({
            id: invite.id,
            studentName: invite.receiverName,
            studentId: invite.receiverNumber,
            major: invite.receiverMajor || '-',
            class: invite.receiverClass || '-',
            status: this.mapInviteStatus(invite.status),
            originalStatus: invite.status,
            sentTime: this.formatDateTime(invite.createdAt)
          }))
        }

        // 更新收到的邀请
        if (receivedRes && receivedRes.code === 200 && receivedRes.data) {
          this.receivedInvites = receivedRes.data.map(invite => ({
            id: invite.id,
            senderName: invite.senderName,
            senderStudentId: invite.senderNumber,
            senderMajor: invite.senderMajor || '',
            senderAvatar: '',
            sentTime: this.formatDateTime(invite.createdAt),
            status: 'pending'
          }))
        }
      } catch (error) {
        console.debug('自动刷新邀请信息失败', error)
      }
    },

    /**
     * 获取分配区域信息
     */
    async loadAssignedArea() {
      try {
        const res = await getAssignedArea()
        if (res.code === 200 && res.data) {
          this.assignedArea = res.data
        } else {
          // 不在这里显示错误，由调用方统一处理
          throw new Error(res.message || '获取分配区域信息失败')
        }
      } catch (error) {
        console.error('获取分配区域失败', error)
        throw error
      }
    },
    
    /**
     * 获取团队信息
     */
    async loadTeamInfo() {
      try {
        const res = await getCurrentTeam()
        if (res.code === 200 && res.data) {
          this.currentTeam = res.data
          
          // 确保 isLeader 字段正确设置（如果后端没有设置，前端补充）
          if (this.currentTeam.members && this.currentTeam.leaderId) {
            const leaderId = Number(this.currentTeam.leaderId)
            this.currentTeam.members.forEach(member => {
              const memberId = member.id ? Number(member.id) : null
              // 如果成员ID匹配舍长ID，确保 isLeader 为 true
              if (memberId !== null && memberId === leaderId) {
                member.isLeader = true
              }
            })
          }
          
          // 更新团队状态
          if (this.currentTeam.memberCount >= this.currentTeam.maxMembers) {
            this.teamStatus = 'ready'
          } else {
            this.teamStatus = 'forming'
          }
        } else {
          // 不在这里显示错误，由调用方统一处理
          throw new Error(res.message || '获取团队信息失败')
        }
      } catch (error) {
        console.error('获取团队信息失败', error)
        throw error
      }
    },
    
    /**
     * 加载所有邀请
     */
    async loadInvites() {
      await Promise.all([
        this.loadSentInvites(),
        this.loadReceivedInvites()
      ])
    },
    
    /**
     * 加载已发送的邀请列表
     */
    async loadSentInvites() {
      try {
        const res = await getSentInvites()
        if (res.code === 200 && res.data) {
          this.sentInvites = res.data.map(invite => ({
            id: invite.id,
            studentName: invite.receiverName,
            studentId: invite.receiverNumber,
            major: invite.receiverMajor || '-',
            class: invite.receiverClass || '-',
            status: this.mapInviteStatus(invite.status),
            originalStatus: invite.status, // 保存原始状态用于判断
            sentTime: this.formatDateTime(invite.createdAt)
          }))
        }
      } catch (error) {
        console.error('加载已发送邀请失败', error)
      }
    },
    
    /**
     * 加载收到的邀请列表
     */
    async loadReceivedInvites() {
      try {
        const res = await getReceivedInvites()
        if (res.code === 200 && res.data) {
          this.receivedInvites = res.data.map(invite => ({
            id: invite.id,
            senderName: invite.senderName,
            senderStudentId: invite.senderNumber,
            senderMajor: invite.senderMajor || '',
            senderAvatar: '',
            sentTime: this.formatDateTime(invite.createdAt),
            status: 'pending'
          }))
        }
      } catch (error) {
        console.error('加载收到的邀请失败', error)
      }
    },
    
    /**
     * 加载可选房间列表
     */
    async loadAvailableRooms() {
      try {
        const res = await getAvailableRoomsAPI()
        if (res.code === 200 && res.data) {
          this.availableRooms = res.data.map(room => {
            // 解析物资配置（优先使用materials，向后兼容facilities）
            const materialsData = this.parseMaterials(room.materials) || this.parseMaterials(room.facilities)
            
            return {
              id: room.id,
              number: `${room.buildingName}${room.roomNumber}`,
              floor: room.floor,
              type: room.roomType,
              bedCount: room.bedCount,
              availableBeds: room.availableBeds,
              // 根据可用床位数判断状态：有可用床位则为available，否则为occupied
              status: room.availableBeds > 0 ? 'available' : 'occupied',
              // 保存原始物资配置数据
              materialsRaw: room.materials,
              materialsList: materialsData,
              // 统一使用中文 key（从物资配置中提取设施信息）
              阳台: this.hasMaterial(materialsData, '阳台') || this.hasFacility(room.facilities, '阳台'),
              空调: this.hasMaterial(materialsData, '空调') || this.hasFacility(room.facilities, '空调'),
              独立卫浴: this.hasMaterial(materialsData, '独立卫浴') || this.hasFacility(room.facilities, '独立卫浴'),
              WiFi: this.hasMaterial(materialsData, 'WiFi') || this.hasFacility(room.facilities, 'WiFi'),
              暖气: this.hasMaterial(materialsData, '暖气') || this.hasFacility(room.facilities, '暖气'),
              热水器: this.hasMaterial(materialsData, '热水器') || this.hasFacility(room.facilities, '热水器')
            }
          })
          
          console.log(`已加载 ${this.availableRooms.length} 个可选房间`)
        }
      } catch (error) {
        console.error('加载可用房间失败', error)
        this.$message.error('加载房间列表失败')
      }
    },
    
    /**
     * 解析物资配置JSON
     */
    parseMaterials(materialsJson) {
      if (!materialsJson) return null
      try {
        const data = JSON.parse(materialsJson)
        // 如果是数组格式（物资列表），直接返回
        if (Array.isArray(data)) {
          return data
        }
        // 如果是对象格式（旧的设施配置），转换为数组
        return Object.entries(data)
          .filter(([key, value]) => value === true)
          .map(([key]) => ({ name: key, quantity: 1, unit: '个' }))
      } catch (e) {
        return null
      }
    },
    
    /**
     * 检查物资列表中是否包含某项
     */
    hasMaterial(materialsList, name) {
      if (!materialsList || !Array.isArray(materialsList)) return false
      return materialsList.some(item => item.name === name)
    },
    
    /**
     * 获取分配结果
     */
    async loadAllocationResult() {
      try {
        const res = await getAllocationResultAPI()
        if (res.code === 200 && res.data) {
          if (res.data.success) {
            // 有有效的分配结果
            this.allocationResult = res.data
            this.allocationPhase = res.data.allocationPhase || 'adjustable'
            this.adjustmentDeadline = res.data.adjustmentDeadline
            this.isLeader = res.data.isLeader || false
            this.dormFacilities = res.data.facilities || {}
            
            // 解析物资配置列表
            if (res.data.materials) {
              try {
                this.dormMaterialsList = JSON.parse(res.data.materials)
              } catch (e) {
                this.dormMaterialsList = []
              }
            } else {
              this.dormMaterialsList = []
            }
          } else {
            // 没有有效的分配结果
            this.allocationResult = null
            
            // 如果系统阶段是 admin_allocation 或 completed，保持在步骤2显示等待页面
            if (this.systemPhase === 'admin_allocation' || this.systemPhase === 'completed') {
              this.allocationPhase = 'waiting'
              // 不显示提示，不跳转
            } else {
              // 其他阶段，重置到步骤0
              this.currentStep = 0
              localStorage.setItem('allocation_current_step', '0')
              this.$message.info(res.data.message || '暂未分配宿舍，请重新组建团队')
              // 重新加载团队信息
              await this.loadTeamInfo()
              await this.loadInvites()
            }
          }
        } else {
          this.$message.warning(res.message || '暂未分配宿舍')
        }
      } catch (error) {
        console.error('获取分配结果失败', error)
      }
    },
    
    /**
     * 刷新分配结果（用于等待管理员分配时手动刷新）
     */
    async refreshAllocationResult() {
      this.refreshing = true
      try {
        // 先刷新系统阶段
        await this.fetchSystemPhase()
        
        const res = await getAllocationResultAPI()
        if (res.code === 200 && res.data && res.data.success) {
          // 有有效的分配结果了
          this.allocationResult = res.data
          this.allocationPhase = res.data.allocationPhase || 'adjustable'
          this.adjustmentDeadline = res.data.adjustmentDeadline
          this.isLeader = res.data.isLeader || false
          this.dormFacilities = res.data.facilities || {}
          
          // 解析物资配置列表
          if (res.data.materials) {
            try {
              this.dormMaterialsList = JSON.parse(res.data.materials)
            } catch (e) {
              this.dormMaterialsList = []
            }
          } else {
            this.dormMaterialsList = []
          }
          
          this.$message.success('宿舍已分配成功！')
        } else {
          // 仍然没有分配结果
          this.$message.info('管理员尚未完成分配，请稍后再试')
        }
      } catch (error) {
        console.error('刷新分配结果失败', error)
        this.$message.error('刷新失败，请稍后重试')
      } finally {
        this.refreshing = false
      }
    },

    /**
     * 加载申请历史
     */
    async loadApplicationHistory() {
      try {
        const res = await getApplicationHistory()
        if (res.code === 200 && res.data) {
          this.transferHistory = res.data.map(app => ({
            submitTime: this.formatDateTime(app.createdAt),
            type: this.formatApplicationType(app.type),
            reason: app.reason,
            status: this.mapApplicationStatus(app.status),
            remark: app.adminComment || app.rejectReason || '-'
          }))
        }
      } catch (error) {
        console.error('加载申请历史失败', error)
      }
    },
    
    
    // 处理学号输入
    handleStudentIdInput() {
      const studentId = this.inviteForm.studentId?.trim()
      // 如果输入为空或太短，清空查询结果
      if (!studentId || studentId.length < 10) {
        this.searchedStudent = null
        this.searchingStudent = false
        return
      }
      
      // 如果输入的是当前学生自己，清空查询结果（不显示提示，由下方提示显示）
      if (studentId === this.studentInfo.studentId) {
        this.searchedStudent = null
        this.searchingStudent = false
        return
      }
      
      // 如果学号变化了，且旧结果与新学号不匹配，清空旧结果
      if (this.searchedStudent && this.searchedStudent.studentId !== studentId) {
        this.searchedStudent = null
      }
      
      // 在防抖期间也标记为查询中，避免显示错误提示
      this.searchingStudent = true
      
      // 防抖：延迟查询，避免频繁请求（缩短到300ms，提升响应速度）
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
      }
      this.searchTimer = setTimeout(() => {
        this.searchStudentByStudentId()
      }, 300)
    },
    
    // 处理粘贴事件
    handlePaste() {
      // 清除之前的防抖定时器
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      
      // 粘贴后立即触发查询（延迟一点确保值已更新）
      setTimeout(() => {
        const studentId = this.inviteForm.studentId?.trim()
        if (studentId && studentId.length >= 10) {
          this.searchStudentByStudentId()
        } else {
          this.searchedStudent = null
        }
      }, 100)
    },
    
    // 处理回车键
    handleEnterKey() {
      // 阻止默认提交行为，只触发查询
      if (this.searchedStudent) {
        this.sendInvite()
      } else {
        this.searchStudentByStudentId()
      }
    },
    
    // 根据学号查询学生信息
    async searchStudentByStudentId() {
      const studentId = this.inviteForm.studentId?.trim()
      // 学号长度至少10位才查询
      if (!studentId || studentId.length < 10) {
        this.searchedStudent = null
        this.searchingStudent = false
        return
      }
      
      // 如果查询的是当前学生自己，不查询（由下方提示显示）
      if (studentId === this.studentInfo.studentId) {
        this.searchedStudent = null
        this.searchingStudent = false
        return
      }
      
      // 保存当前查询的学号，用于验证结果是否匹配
      const currentSearchId = studentId
      this.searchingStudent = true
      // 不立即清空结果，保留旧结果直到新结果返回，避免闪烁
      
      try {
        const res = await getStudentByStudentId(studentId)
        // 验证学号是否仍然匹配（防止用户快速输入导致结果错乱）
        if (this.inviteForm.studentId?.trim() !== currentSearchId) {
          return
        }
        
        if (res.code === 200 && res.data) {
          this.searchedStudent = {
            id: res.data.id,
            realName: res.data.realName,
            studentId: res.data.studentId,
            major: res.data.major,
            className: res.data.className,
            avatar: res.data.avatar
          }
        } else {
          this.searchedStudent = null
        }
      } catch (error) {
        // 验证学号是否仍然匹配
        if (this.inviteForm.studentId?.trim() !== currentSearchId) {
          return
        }
        console.error('查询学生信息失败', error)
        this.searchedStudent = null
        // 不显示页面上方的错误提示，只保留下方的提示
      } finally {
        // 再次验证学号是否仍然匹配
        if (this.inviteForm.studentId?.trim() === currentSearchId) {
          this.searchingStudent = false
        }
      }
    },
    
    // 发送邀请
    async sendInvite() {
      if (!this.inviteForm.studentId) {
        this.$message.warning('请输入学号')
        return
      }
      
      // 检查是否已查询到学生信息
      if (!this.searchedStudent) {
        this.$message.warning('请先确认学生信息')
        return
      }
      
      const studentId = this.inviteForm.studentId.trim()
      
      // 检查是否邀请自己
      if (studentId === this.studentInfo.studentId) {
        this.$message.warning('不能邀请自己')
        return
      }
      
      // 检查团队是否已满
      if (this.isTeamFull()) {
        this.$message.warning('您的团队已满员，无法继续邀请')
        return
      }
      
      // 检查是否已经邀请过（前端防重）
      const alreadyInvited = this.sentInvites.some(inv => 
        inv.studentId === studentId || inv.receiverNumber === studentId
      )
      if (alreadyInvited) {
        this.$message.warning('该学号已发送过邀请')
        return
      }
      
      this.inviting = true
      
      try {
        const res = await sendInviteAPI({
          studentId: studentId
        })
        
        if (res.code === 200) {
          this.$message.success('邀请已发送！')
          this.inviteForm.studentId = ''
          this.searchedStudent = null
          
          // 重新加载已发送邀请列表
          await this.loadSentInvites()
          
          // ========== 集成聊天系统 ==========
          // 从搜索结果中查找学生信息
          const student = this.searchResults.find(s => s.studentId === studentId)
          if (student) {
            this.addToChatContacts(student, res.data?.id || 'inv' + Date.now())
          } else {
            this.addToChatContactsBasic(studentId, res.data?.id || 'inv' + Date.now())
          }
        } else {
          this.$message.error(res.message || '发送邀请失败')
        }
      } catch (error) {
        console.error('发送邀请失败', error)
        this.$message.error('发送邀请失败，请稍后重试')
      } finally {
        this.inviting = false
      }
    },
    
    // 添加基本联系人信息（当学生详细信息不可用时）
    async addToChatContactsBasic(studentId, inviteId) {
      try {
        const { getPrivateChats, savePrivateChats, addPrivateChatMessage } = await import('@/utils/studentMessageData.js')
        
        const privateChats = getPrivateChats()
        const contactId = `student_${studentId}_${Date.now()}`
        
        // 创建基本联系人
        if (!privateChats[contactId]) {
          privateChats[contactId] = {
            messages: [],
            unreadCount: 0,
            contactInfo: {
              id: contactId,
              name: '学生 ' + studentId,
              avatar: '',
              type: 'student',
              major: this.studentInfo.major,
              studentId: studentId,
              className: this.studentInfo.class
            }
          }
          savePrivateChats(privateChats)
          console.log('已创建基本联系人:', contactId)
        }
        
        // 创建邀请消息
        const inviteMessage = {
          id: 'msg_' + Date.now(),
          type: 'dorm_invite',
          content: '邀请你组成宿舍队伍，一起申请宿舍分配',
          time: new Date().toISOString(),
          timestamp: new Date().toISOString(),
          isOwn: true,
          inviteStatus: 'pending', // pending, accepted, rejected, cancelled
          inviteData: {
            inviteId: inviteId,
            inviterName: this.studentInfo.name,
            inviterStudentId: this.studentInfo.studentId,
            inviterMajor: this.studentInfo.major,
            status: 'pending',
            sentTime: new Date().toLocaleString()
          }
        }
        
        addPrivateChatMessage(contactId, inviteMessage)
        
        console.log('已添加基本邀请到聊天系统:', contactId)
        
        // 手动触发 storage 事件
        window.dispatchEvent(new Event('storage'))
      } catch (error) {
        console.error('添加基本聊天联系人失败:', error)
      }
    },
    
    // 撤回邀请
    async cancelInvite(inviteId) {
      try {
        const res = await cancelInviteAPI(inviteId)
        if (res.code === 200) {
          this.$message.success('邀请已撤回')
          
          // 同步更新聊天系统中的邀请状态
          this.updateInviteStatusInChat(inviteId, 'cancelled')
          
          // 重新加载邀请列表
          await this.loadSentInvites()
        } else {
          this.$message.error(res.message || '撤回失败')
        }
      } catch (error) {
        console.error('撤回邀请失败', error)
        this.$message.error('撤回失败，请稍后重试')
      }
    },
    
    // 删除邀请（已撤回或已拒绝的邀请）
    async deleteInvite(inviteId) {
      try {
        await this.$confirm('确定要删除这条邀请记录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await cancelInviteAPI(inviteId)
        if (res.code === 200) {
          this.$message.success('邀请记录已删除')
          
          // 重新加载邀请列表
          await this.loadSentInvites()
        } else {
          this.$message.error(res.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除邀请失败', error)
          this.$message.error('删除失败，请稍后重试')
        }
      }
    },
    
    // 更新聊天系统中的邀请状态
    async updateInviteStatusInChat(inviteId, newStatus) {
      try {
        const { getPrivateChats, savePrivateChats } = await import('@/utils/studentMessageData.js')
        
        const privateChats = getPrivateChats()
        
        // 遍历所有私聊，找到包含该邀请ID的消息
        for (const contactId in privateChats) {
          const chat = privateChats[contactId]
          if (chat.messages) {
            const message = chat.messages.find(
              msg => msg.type === 'dorm_invite' && 
                     msg.inviteData && 
                     msg.inviteData.inviteId === inviteId
            )
            
            if (message && message.inviteData) {
              // 更新邀请状态（同时更新两个字段以保持兼容性）
              message.inviteData.status = newStatus
              message.inviteStatus = newStatus
              
              // 如果是撤回，标记消息为已撤回
              if (newStatus === 'cancelled') {
                message.recalled = true
              }
              
              savePrivateChats(privateChats)
              console.log('已更新聊天中的邀请状态:', inviteId, newStatus)
              
              // 手动触发 storage 事件
              window.dispatchEvent(new Event('storage'))
              break
            }
          }
        }
      } catch (error) {
        console.error('更新聊天邀请状态失败:', error)
      }
    },
    
    // 接受邀请
    async acceptInvite(inviteId) {
      try {
        await this.$confirm(
          '确认接受此邀请？接受后您将加入对方的队伍', 
          '确认', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const result = await acceptInviteAPI(inviteId)
        if (result.code === 200) {
          this.$message.success('已加入团队！')
          
          // 同步更新聊天系统中的邀请状态
          this.updateInviteStatusInChat(inviteId, 'accepted')
          
          // 重新加载团队和邀请信息
          await Promise.all([
            this.loadTeamInfo(),
            this.loadReceivedInvites()
          ])
        } else {
          this.$message.error(result.message || '接受邀请失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('接受邀请失败', error)
          // request.js 的响应拦截器已经显示了错误提示，这里不再重复显示
          // 只记录日志即可
        }
      }
    },
    
    // 拒绝邀请
    async rejectInvite(inviteId) {
      try {
        const res = await rejectInviteAPI(inviteId)
        if (res.code === 200) {
          this.$message.success('已拒绝邀请')
          
          // 同步更新聊天系统中的邀请状态
          this.updateInviteStatusInChat(inviteId, 'rejected')
          
          // 重新加载邀请列表
          await this.loadReceivedInvites()
        } else {
          this.$message.error(res.message || '操作失败')
        }
      } catch (error) {
        console.error('拒绝邀请失败', error)
        this.$message.error('操作失败，请稍后重试')
      }
    },
    
    // 搜索室友（从API获取）
    async searchRoommates() {
      try {
        const keyword = this.searchForm.keyword.trim()
        const res = await searchClassmates(keyword)
        
        if (res.code === 200 && res.data) {
          this.searchResults = res.data.map(student => ({
            id: student.id,
            name: student.realName,
            studentId: student.studentId,
            major: student.major,
            class: student.className,
            avatar: student.avatar || '',
            tags: this.parseTags(student.tags)
          }))
          
          if (this.searchResults.length === 0 && keyword) {
            this.$message.info('未找到符合条件的同学')
          }
          
          // 重置到第一页
          this.searchCurrentPage = 1
        }
      } catch (error) {
        console.error('搜索同学失败', error)
        this.$message.error('搜索失败，请稍后重试')
      }
    },
    
    // 分页大小改变
    handleSearchSizeChange(newSize) {
      this.searchPageSize = newSize
      this.searchCurrentPage = 1
    },
    
    // 分页页码改变
    handleSearchPageChange(newPage) {
      this.searchCurrentPage = newPage
    },
    
    // 判断学生是否正在邀请中
    isStudentInviting(studentId) {
      return this.invitingStudents.includes(studentId)
    },
    
    // 判断学生是否已邀请
    isStudentInvited(studentId) {
      return this.invitedStudents.includes(studentId)
    },
    
    // 邀请学生（从搜索列表）
    async inviteStudent(studentId) {
      // 防止重复邀请
      if (this.isStudentInviting(studentId) || this.isStudentInvited(studentId)) {
        return
      }

      // 检查团队是否已满
      if (this.isTeamFull()) {
        this.$message.warning('您的团队已满员，无法继续邀请')
        return
      }
      
      const student = this.searchResults.find(s => s.id === studentId)
      if (!student) {
        this.$message.error('未找到该学生信息')
        return
      }
      
      // 标记为邀请中
      this.invitingStudents.push(studentId)
      
      try {
        const res = await sendInviteAPI({
          receiverId: studentId,
          studentId: student.studentId,
          message: '邀请你加入我的宿舍团队'
        })
        
        if (res.code === 200) {
          this.$message.success(`已向 ${student.name} 发送邀请！`)
          
          // 添加到已邀请列表
          if (!this.invitedStudents.includes(studentId)) {
            this.invitedStudents.push(studentId)
          }
          
          // 重新加载已发送邀请列表
          await this.loadSentInvites()
          
          // 集成聊天系统
          this.addToChatContacts(student, res.data?.id || 'inv' + Date.now())
        } else {
          this.$message.error(res.message || '发送邀请失败')
        }
      } catch (error) {
        console.error('邀请学生失败', error)
        this.$message.error('发送邀请失败，请稍后重试')
      } finally {
        // 移除邀请中状态
        const index = this.invitingStudents.indexOf(studentId)
        if (index > -1) {
          this.invitingStudents.splice(index, 1)
        }
      }
    },
    
    // 添加联系人到聊天系统并发送邀请消息
    async addToChatContacts(student, inviteId) {
      try {
        // 使用 import 而不是 require，确保事件触发
        const { getPrivateChats, savePrivateChats, addPrivateChatMessage } = await import('@/utils/studentMessageData.js')
        
        const privateChats = getPrivateChats()
        const contactId = `student_${student.id}`
        
        // 如果联系人不存在，创建新联系人
        if (!privateChats[contactId]) {
          privateChats[contactId] = {
            messages: [],
            unreadCount: 0,
            contactInfo: {
              id: student.id,
              name: student.name,
              avatar: student.avatar || '',
              type: 'student',
              major: student.major,
              studentId: student.studentId,
              className: student.class
            }
          }
          savePrivateChats(privateChats)
          console.log('已创建新联系人:', contactId, student.name)
        }
        
        // 创建邀请消息
        const inviteMessage = {
          id: 'msg_' + Date.now(),
          type: 'dorm_invite',
          content: '邀请你组成宿舍队伍，一起申请宿舍分配',
          time: new Date().toISOString(),
          timestamp: new Date().toISOString(), // 添加 timestamp 字段
          isOwn: true,
          inviteStatus: 'pending', // pending, accepted, rejected, cancelled
          inviteData: {
            inviteId: inviteId,
            inviterName: this.studentInfo.name,
            inviterStudentId: this.studentInfo.studentId,
            inviterMajor: this.studentInfo.major,
            status: 'pending',
            sentTime: new Date().toLocaleString()
          }
        }
        
        // 添加消息到聊天记录（这个函数会触发事件）
        addPrivateChatMessage(contactId, inviteMessage)
        
        console.log('已添加邀请到聊天系统:', contactId, '消息ID:', inviteMessage.id)
        
        // 手动触发一次 storage 事件，确保其他标签页也能收到更新
        window.dispatchEvent(new Event('storage'))
      } catch (error) {
        console.error('添加聊天联系人失败:', error)
      }
    },
    
    // 辅助方法：判断成员是否是舍长
    isMemberLeader(member) {
      if (!member || !this.currentTeam) return false
      
      // 优先检查 isLeader 标志
      if (member.isLeader === true) {
        return true
      }
      
      // 通过ID匹配（leaderId是student表ID，member.id也是student表ID）
      const memberId = member.id ? Number(member.id) : null
      const leaderId = this.currentTeam.leaderId ? Number(this.currentTeam.leaderId) : null
      
      if (memberId !== null && leaderId !== null && memberId === leaderId) {
        return true
      }
      
      return false
    },
    
    // 辅助方法：判断是否是当前用户
    isCurrentUser(member) {
      if (!member || !this.studentInfo) return false
      
      // 优先通过学号匹配（因为member.id是student表ID，studentInfo.id是sys_user表ID）
      if (member.studentId && this.studentInfo.studentId) {
        const isMatch = member.studentId === this.studentInfo.studentId
        if (isMatch) {
          return true
        }
      }
      
      // 备用方案：尝试通过ID匹配（兼容旧逻辑）
      const memberId = member.id ? String(member.id) : null
      const currentUserId = this.studentInfo.id ? String(this.studentInfo.id) : null
      const memberIdNum = member.id ? Number(member.id) : NaN
      const currentUserIdNum = this.studentInfo.id ? Number(this.studentInfo.id) : NaN
      
      const isMatch = (memberId && currentUserId && memberId === currentUserId) || 
                     (!isNaN(memberIdNum) && !isNaN(currentUserIdNum) && memberIdNum === currentUserIdNum)
      
      return isMatch
    },
    
    // 辅助方法：判断当前用户是否是舍长
    isCurrentUserLeader() {
      if (!this.currentTeam || !this.studentInfo) return false
      
      // 优先通过学号匹配：找到舍长成员，比较学号
      if (this.currentTeam.leaderId && this.studentInfo.studentId) {
        const leaderMember = this.currentTeam.members.find(m => 
          m.id === this.currentTeam.leaderId || 
          (m.isLeader === true)
        )
        if (leaderMember && leaderMember.studentId === this.studentInfo.studentId) {
          return true
        }
      }
      
      // 备用方案：通过ID匹配（leaderId是student表ID）
      const leaderId = this.currentTeam.leaderId ? String(this.currentTeam.leaderId) : null
      // 注意：这里不能直接用studentInfo.id，因为它是sys_user表ID
      // 需要通过member.id来匹配
      const currentUserMember = this.currentTeam.members.find(m => 
        m.studentId === this.studentInfo.studentId
      )
      if (currentUserMember) {
        const currentMemberId = currentUserMember.id ? String(currentUserMember.id) : null
        const currentMemberIdNum = currentUserMember.id ? Number(currentUserMember.id) : NaN
        const leaderIdNum = this.currentTeam.leaderId ? Number(this.currentTeam.leaderId) : NaN
        
        const isMatch = (leaderId && currentMemberId && leaderId === currentMemberId) || 
                       (!isNaN(leaderIdNum) && !isNaN(currentMemberIdNum) && leaderIdNum === currentMemberIdNum)
        return isMatch
      }
      
      return false
    },
    
    // 判断是否可以操作该成员
    canOperateMember(member) {
      // 确保数据存在
      if (!member || !this.currentTeam || !this.studentInfo) {
        console.log('canOperateMember: 数据不完整', { 
          member, 
          currentTeam: this.currentTeam, 
          studentInfo: this.studentInfo 
        })
        return false
      }
      
      // 确保member.id存在
      if (!member.id) {
        return false
      }
      
      // 如果是当前用户自己，总是显示操作按钮
      if (this.isCurrentUser(member)) {
        return true
      }
      
      // 舍长可以操作其他成员（移除成员）
      if (this.isCurrentUserLeader() && !this.isMemberLeader(member)) {
        return true
      }
      
      return false
    },
    
    // 统一处理成员操作
    handleMemberOperation(command, member) {
      switch (command) {
        case 'remove':
          this.removeMember(member.id)
          break
        case 'transfer':
          this.showTransferLeaderDialog = true
          break
        case 'leave':
          this.leaveTeam()
          break
      }
    },
    
    // 移除团队成员
    async removeMember(memberId) {
      try {
        await this.$confirm('确认移除此成员？', '确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await removeMemberAPI(memberId)
        if (res.code === 200) {
          this.$message.success('成员已移除')
          
          // 重新加载团队信息
          await this.loadTeamInfo()
        } else {
          this.$message.error(res.message || '移除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('移除成员失败', error)
          this.$message.error('操作失败，请稍后重试')
        }
      }
    },
    
    // 退出队伍（普通成员）
    async leaveTeam() {
      try {
        await this.$confirm(
          '确认退出当前队伍？退出后您将创建新的队伍', 
          '确认退出',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const res = await leaveTeamAPI()
        if (res.code === 200) {
          this.$message.success('已退出队伍，您现在是新队伍的舍长')
          
          // 重新加载团队信息
          await this.loadTeamInfo()
        } else {
          this.$message.error(res.message || '退出失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('退出团队失败', error)
          this.$message.error('操作失败，请稍后重试')
        }
      }
    },
    
    // 转让舍长
    async transferLeadership() {
      if (!this.selectedNewLeader) {
        this.$message.warning('请选择新舍长')
        return
      }
      
      try {
        await this.$confirm('确认转让舍长给选中的成员？', '确认转让', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const res = await transferLeadershipAPI(this.selectedNewLeader)
        if (res.code === 200) {
          this.$message.success('舍长已转让')
          this.showTransferLeaderDialog = false
          this.selectedNewLeader = ''
          
          // 重新加载团队信息
          await this.loadTeamInfo()
        } else {
          this.$message.error(res.message || '转让失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('转让舍长失败', error)
          this.$message.error('操作失败，请稍后重试')
        }
      }
    },
    
    // 进入选房阶段
    proceedToRoomSelection() {
      this.currentStep = 1
      this.roomSelectionStatus = 'selecting'
      localStorage.setItem('allocation_current_step', '1')
      this.$message.success('进入选房阶段！')
    },
    
    // 选择房间
    selectRoom(room) {
      if (room.status !== 'available') {
        this.$message.warning('该房间不可选择')
        return
      }
      this.selectedRoom = room
    },
    
    // 确认房间选择
    async confirmRoomSelection() {
      if (!this.selectedRoom) {
        this.$message.warning('请先选择房间')
        return
      }
      
      try {
        await this.$confirm(
          `确认选择房间 ${this.selectedRoom.number}？`, 
          '确认选择',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        this.confirmingRoom = true
        
        const res = await confirmRoomSelectionAPI({
          roomId: this.selectedRoom.id,
          buildingId: this.assignedArea.buildingId,
          groupId: this.currentTeam.id
        })
        
        if (res.code === 200) {
          this.$message.success('房间选择成功！')
          this.roomSelectionStatus = 'completed'
          this.currentStep = 2
          
          // 加载分配结果
          await this.loadAllocationResult()
        } else {
          this.$message.error(res.message || '选房失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('确认房间失败', error)
          this.$message.error('选房失败，请稍后重试')
        }
      } finally {
        this.confirmingRoom = false
      }
    },
    
    // 获取舍长姓名
    getLeaderName() {
      const leader = this.currentTeam.members.find(m => m.isLeader)
      return leader ? leader.name : '未知'
    },
    
    // 确认分配
    confirmAllocation() {
      this.$confirm('确认接受此宿舍分配？', '确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$message.success('已确认接受分配结果')
      })
    },
    
    // 显示宿舍详情
    showDormDetails() {
      this.dormDetailsVisible = true
    },
    
    // 舍长重新选房
    async reSelectRoom() {
      try {
        await this.$confirm('确定要重新选择房间吗？当前选择的房间将被释放', '确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 调用后端API释放当前房间
        const res = await cancelRoomSelectionAPI()
        if (res.code === 200) {
          // 释放成功，回到选房阶段
          this.selectedRoom = null
          this.roomSelectionStatus = 'selecting'
          this.currentStep = 1
          this.allocationResult = null
          localStorage.setItem('allocation_current_step', '1')
          
          // 重新加载可选房间列表
          await this.loadAvailableRooms()
          this.$message.success('已返回选房阶段，请重新选择房间')
        } else {
          this.$message.error(res.message || '释放房间失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('重新选房失败', error)
          this.$message.error('操作失败，请稍后重试')
        }
      }
    },
    
    // 跳转到调宿申请
    goToTransferApplication() {
      // 如果是舍长且已分配房间，需要先转让舍长身份
      if (this.isLeader && this.currentStep === 2) {
        // 检查队伍人数
        if (this.currentTeam.members.length === 1) {
          // 舍长是唯一成员，需要解散宿舍组
          this.$confirm(
            '你是宿舍唯一成员，确认后将解散宿舍组并释放房间，你将失去当前宿舍分配',
            '提交调宿申请',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
          ).then(async () => {
            try {
              // 调用后端API解散宿舍组
              const res = await disbandGroupAndLeaveAPI()
              if (res.code !== 200) {
                this.$message.error(res.message || '解散宿舍组失败')
                return
              }
              
              // 更新本地状态
              this.isLeader = false
              this.allocationResult = null
              
              this.$message.success('已解散宿舍组，请继续提交调宿申请')
              this.currentStep = 3
              localStorage.setItem('allocation_current_step', '3')
            } catch (error) {
              console.error('解散宿舍组失败', error)
              this.$message.error('操作失败，请稍后重试')
            }
          })
        } else {
          // 需要先转让舍长
          this.$message.warning('作为舍长，你需要先转让舍长身份才能提交调宿申请')
          this.selectedNewLeaderForTransfer = ''
          this.showTransferLeaderForTransferDialog = true
        }
      } else {
        // 普通成员或未分配房间的舍长，弹出提示
        this.$confirm(
          '你将失去当前宿舍分配',
          '提交调宿申请',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        ).then(() => {
          this.currentStep = 3
          localStorage.setItem('allocation_current_step', '3')
        })
      }
    },
    
    // 转让舍长并继续申请（调用后端API完成转让+离开）
    async confirmTransferAndApply() {
      if (!this.selectedNewLeaderForTransfer) {
        this.$message.warning('请选择新舍长')
        return
      }
      
      try {
        // 调用后端API：转让舍长并离开团队
        const res = await transferLeaderAndLeaveAPI(this.selectedNewLeaderForTransfer)
        if (res.code !== 200) {
          this.$message.error(res.message || '转让舍长失败')
          return
        }
        
        // 更新本地状态
        this.isLeader = false
        this.allocationResult = null // 清空分配结果，因为已离开原宿舍
        
        this.showTransferLeaderForTransferDialog = false
        this.$message.success('已转让舍长并离开原宿舍，请继续提交调宿申请')
        
        // 继续到调宿申请页面
        this.currentStep = 3
        localStorage.setItem('allocation_current_step', '3')
      } catch (error) {
        console.error('转让舍长并离开失败', error)
        this.$message.error('操作失败，请稍后重试')
      }
    },
    
    // 提交调宿申请
    async submitTransferApplication() {
      try {
        await this.$refs.transferFormRef.validate()
        this.transferSubmitting = true
        
        const res = await submitTransferApplicationAPI({
          transferType: this.transferForm.transferType,
          targetBuilding: this.transferForm.targetBuilding,
          targetRoom: this.transferForm.targetRoom,
          reason: this.transferForm.reason,
          description: this.transferForm.description,
          allocationPhase: this.allocationPhase === 'adjustable' ? 
            'self-selection' : 'confirmed',
          isLeader: this.isLeader,
          contactPhone: this.transferForm.contactPhone || ''
        })
        
        if (res.code === 200) {
          this.$message.success('调宿申请提交成功，请等待审核！')
          this.transferStatus = 'pending'
          this.resetTransferForm()
          
          // 重新加载申请历史
          await this.loadApplicationHistory()
        } else {
          this.$message.error(res.message || '提交失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('提交调宿申请失败', error)
          this.$message.error('提交失败，请稍后重试')
        }
      } finally {
        this.transferSubmitting = false
      }
    },
    
    // 重置调宿表单
    resetTransferForm() {
      this.$refs.transferFormRef.resetFields()
      this.transferForm.attachments = []
    },
    
    // 格式化截止时间显示
    formatDeadlineTime(deadline) {
      if (!deadline) return '未设置'
      const date = new Date(deadline)
      const month = date.getMonth() + 1
      const day = date.getDate()
      const hours = date.getHours()
      const minutes = date.getMinutes()
      return `${month}月${day}日 ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
    },
    
    // 更新剩余时间显示
    updateAdjustmentTimeLeft() {
      if (!this.adjustmentDeadline) {
        this.adjustmentTimeLeft = '未设置'
        return
      }
      
      const now = new Date()
      const deadline = new Date(this.adjustmentDeadline)
      const diff = deadline - now
      
      if (diff <= 0) {
        this.adjustmentTimeLeft = '已截止'
        // 自动切换为已确认状态
        if (this.allocationPhase === 'adjustable') {
          this.allocationPhase = 'confirmed'
          localStorage.setItem('allocation_phase', 'confirmed')
        }
        return
      }
      
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
      
      if (days > 0) {
        this.adjustmentTimeLeft = `${days}天${hours}小时`
      } else if (hours > 0) {
        this.adjustmentTimeLeft = `${hours}小时${minutes}分钟`
      } else {
        this.adjustmentTimeLeft = `${minutes}分钟`
      }
    },
    
    // 从系统配置读取截止时间
    loadAdjustmentDeadline() {
      // 如果已经从后端获取了截止时间，直接使用，不要覆盖
      if (this.adjustmentDeadline) {
        this.updateAdjustmentTimeLeft()
        return
      }
      
      // 从 localStorage 读取管理员设置的截止时间（作为备用）
      const systemConfig = JSON.parse(localStorage.getItem('system_config') || '{}')
      if (systemConfig.adjustmentDeadline) {
        this.adjustmentDeadline = systemConfig.adjustmentDeadline
      }
      // 注意：不再设置默认的3天后时间，应该由后端统一管理
      // 如果后端没有返回截止时间，说明系统配置有问题，应该显示"未设置"
      
      this.updateAdjustmentTimeLeft()
    },
    
    // 查看申请历史
    viewTransferHistory() {
      this.showTransferHistory = !this.showTransferHistory
    },
    
    // 文件预览
    handlePreview(file) {
      console.log('预览文件:', file)
    },
    
    // 移除文件
    handleRemove(file, fileList) {
      console.log('移除文件:', file, fileList)
    },
    
    // 获取团队状态文本
    getTeamStatusText() {
      const map = {
        forming: '组建中',
        ready: '准备就绪',
        completed: '已完成'
      }
      return map[this.teamStatus] || '未知'
    },
    
    // 获取房间选择状态文本
    getRoomSelectionStatusText() {
      const map = {
        pending: '待选择',
        selecting: '选择中',
        completed: '已完成'
      }
      return map[this.roomSelectionStatus] || '未知'
    },
    
    // 获取邀请状态类型
    getInviteStatusType(status) {
      const map = {
        '已接受': 'success',
        '已拒绝': 'danger',
        '待回复': 'warning'
      }
      return map[status] || 'info'
    },
    
    // 获取房间状态类型
    getRoomStatusType(status) {
      const map = {
        'available': 'success',
        'occupied': 'danger',
        'reserved': 'warning'
      }
      return map[status] || 'info'
    },
    
    // 格式化房间类型
    // 统一使用中文数字格式：二人间、四人间、六人间、八人间
    formatRoomType(type) {
      const typeMap = {
        // 新格式（中文数字）
        '二人间': '二人间',
        '四人间': '四人间',
        '六人间': '六人间',
        '八人间': '八人间',
        // 兼容旧格式（阿拉伯数字）
        '2人间': '二人间',
        '4人间': '四人间',
        '6人间': '六人间',
        '8人间': '八人间',
        '单人间': '单人间',
        '3人间': '三人间',
        // 兼容旧格式（英文）
        'four_bed': '四人间',
        'six_bed': '六人间',
        'double_bed': '二人间',
        'two_bed': '二人间',
        'single_bed': '单人间',
        'triple_bed': '三人间',
        'eight_bed': '八人间'
      }
      return typeMap[type] || type || '未知房型'
    },

    // 获取房间状态文本
    getRoomStatusText(status) {
      const map = {
        'available': '可选择',
        'occupied': '已入住',
        'reserved': '已预订'
      }
      return map[status] || '未知'
    },
    
    // 获取调宿状态文本
    getTransferStatusText() {
      const map = {
        none: '未申请',
        pending: '审核中',
        approved: '已批准',
        rejected: '已拒绝'
      }
      return map[this.transferStatus] || '未知'
    },
    
    // 获取状态标签类型
    getStatusType(status) {
      const map = {
        '已批准': 'success',
        '已拒绝': 'danger',
        '审核中': 'warning',
        '待处理': 'info'
      }
      return map[status] || 'info'
    },
    
    // 格式化申请时间
    formatApplicationTime(time) {
      if (!time) return '-'
      const date = new Date(time)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hour = String(date.getHours()).padStart(2, '0')
      const minute = String(date.getMinutes()).padStart(2, '0')
      return `${year}-${month}-${day} ${hour}:${minute}`
    },
    
    // 格式化申请类型
    formatApplicationType(type) {
      const typeMap = {
        'TRANSFER': '调宿申请',
        'transfer': '调宿申请',
        'room': '换房间',
        'roommate': '换室友',
        '调宿申请': '调宿申请',
        '退宿申请': '退宿申请'
      }
      return typeMap[type] || type || '未知'
    },
    
    // 格式化申请状态
    formatApplicationStatus(status) {
      const statusMap = {
        'PENDING': '待审核',
        'pending': '待审核',
        'APPROVED': '已通过',
        'approved': '已通过',
        'REJECTED': '已拒绝',
        'rejected': '已拒绝',
        '待审核': '待审核',
        '已通过': '已通过',
        '已拒绝': '已拒绝'
      }
      return statusMap[status] || status || '未知'
    },
    
    // 获取申请状态标签类型
    getApplicationStatusType(status) {
      const upperStatus = status?.toUpperCase?.() || status
      const map = {
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        '待审核': 'warning',
        '已通过': 'success',
        '已拒绝': 'danger'
      }
      return map[upperStatus] || map[status] || 'info'
    },
    
    // 获取需要的团队人数（优先从团队数据获取，其次从分配区域获取）
    getRequiredTeamSize() {
      // 优先使用团队的maxMembers（这是实际创建团队时确定的值）
      if (this.currentTeam && this.currentTeam.maxMembers) {
        return this.currentTeam.maxMembers
      }
      // 如果数据未加载完成，返回0，避免显示错误的默认值
      if (!this.assignedArea.roomType) {
        return 0
      }
      // 从分配区域的房间类型推断
      const roomType = this.assignedArea.roomType
      const match = roomType.match(/(\d+)/)
      return match ? parseInt(match[1]) : 4
    },
    
    // 判断团队是否已满
    isTeamFull() {
      return this.currentTeam.members.length >= this.getRequiredTeamSize()
    },
    
    // 判断是否可以进入选房阶段
    canProceedToRoomSelection() {
      const isLeader = this.isCurrentUserLeader()
      const isFull = this.isTeamFull()
      const phaseOpen = this.systemPhase === 'room_selection'
      return isLeader && isFull && phaseOpen
    },
    
    /**
     * ==================== 辅助方法 ====================
     */
    
    /**
     * 映射邀请状态
     */
    mapInviteStatus(status) {
      const map = {
        'PENDING': '待回复',
        'ACCEPTED': '已接受',
        'REJECTED': '已拒绝',
        'CANCELLED': '已撤回'
      }
      return map[status] || status
    },
    
    /**
     * 映射申请状态
     */
    mapApplicationStatus(status) {
      const map = {
        'PENDING': '审核中',
        'APPROVED': '已批准',
        'REJECTED': '已拒绝',
        '待审核': '审核中',
        '已通过': '已批准',
        '已拒绝': '已拒绝'
      }
      return map[status] || status
    },
    
    /**
     * 格式化日期时间
     */
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    /**
     * 解析标签JSON
     */
    parseTags(tagsJson) {
      if (!tagsJson) return []
      try {
        return JSON.parse(tagsJson)
      } catch (e) {
        return []
      }
    },
    
    /**
     * 检查设施是否存在
     * 统一使用中文 key，同时兼容旧的英文 key 格式
     */
    hasFacility(facilitiesJson, facilityKey) {
      if (!facilitiesJson) return false
      try {
        const facilities = JSON.parse(facilitiesJson)
        // 中文 key 映射（兼容所有可能的格式，优先使用中文）
        const keyMap = {
          '阳台': ['阳台', 'hasBalcony', 'has阳台', 'balcony'],
          '空调': ['空调', 'hasAirConditioner', 'has空调', 'airConditioner', 'air_conditioner'],
          '独立卫浴': ['独立卫浴', 'hasPrivateBathroom', 'has独立卫浴', 'privateBathroom', 'private_bathroom'],
          'WiFi': ['WiFi', 'wifi', 'hasWifi', 'hasWiFi', 'has WiFi'],
          '暖气': ['暖气', 'hasHeating', 'has暖气', 'heating'],
          '热水器': ['热水器', 'hasWaterHeater', 'has热水器', 'waterHeater', 'water_heater']
        }
        // 检查所有可能的 key
        const possibleKeys = keyMap[facilityKey] || [facilityKey]
        return possibleKeys.some(key => facilities[key] === true)
      } catch (e) {
        return false
      }
    },
    
    /**
     * 设置团队更新监听器（WebSocket）
     */
    async setupTeamUpdateListener() {
      try {
        const { default: webSocketManager } = await import('@/utils/websocket.js')
        const { getCurrentUserId } = await import('@/utils/auth.js')
        
        const userId = getCurrentUserId()
        if (!userId) {
          console.warn('未获取到用户ID，无法监听团队更新')
          return
        }
        
        // 连接WebSocket
        webSocketManager.connect(
          userId,
          () => {
            console.log('WebSocket连接成功，开始监听团队更新')
            
            // 注册团队更新消息处理器
            webSocketManager.registerMessageHandler('team-updates', (message) => {
              console.log('收到团队更新通知:', message)
              
              if (message.type === 'TEAM_UPDATE') {
                const { action, message: notificationMessage } = message
                
                if (action === 'REMOVED') {
                  // 被移除出队伍，显示通知并刷新团队信息
                  this.$notify({
                    title: '团队变更通知',
                    message: notificationMessage || '您已被移出队伍',
                    type: 'warning',
                    duration: 5000
                  })
                  
                  // 立即刷新团队信息
                  this.loadTeamInfo()
                } else if (action === 'LEADER_CHANGED') {
                  // 舍长变更
                  this.$notify({
                    title: '团队变更通知',
                    message: notificationMessage || '舍长已变更',
                    type: 'info',
                    duration: 3000
                  })
                  
                  // 刷新团队信息
                  this.loadTeamInfo()
                }
              }
            })
          },
          (error) => {
            console.error('WebSocket连接失败:', error)
          }
        )
      } catch (error) {
        console.error('设置团队更新监听失败:', error)
      }
    }
  }
}
</script>

<style scoped>
.allocation-flow {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.steps-container {
  margin-bottom: 30px;
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.main-content {
  margin-bottom: 20px;
}

.step-card {
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.card-header .header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.deadline-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.deadline-info .el-icon {
  color: #409eff;
}

.deadline-label {
  color: #909399;
}

.deadline-time {
  color: #409eff;
  font-weight: 500;
}

.deadline-time.deadline-urgent {
  color: #f56c6c;
  animation: pulse 1s infinite;
}

.countdown {
  color: #67c23a;
  font-size: 12px;
}

.deadline-time.deadline-urgent + .countdown {
  color: #f56c6c;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.status-tag {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #f5dab1;
}

.status-tag.submitted {
  background: #f0f9ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.status-tag.processing {
  background: #f0f9ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.status-tag.none {
  background: #f5f7fa;
  color: #909399;
  border: 1px solid #dcdfe6;
}

.waiting-content {
  text-align: center;
}

.waiting-info {
  text-align: left;
  max-width: 400px;
  margin: 0 auto;
}

.waiting-info p {
  margin: 8px 0;
}

.waiting-info ul {
  margin: 12px 0;
  padding-left: 20px;
}

.result-content {
  text-align: center;
}

.result-details {
  text-align: left;
  max-width: 600px;
  margin: 0 auto;
}

.result-actions {
  margin-top: 20px;
  text-align: center;
}

.result-actions .el-button {
  margin: 0 8px;
}

.text-muted {
  color: #909399;
}

.transfer-history {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.transfer-history h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.quick-nav {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 12px;
}

/* 上传组件对齐 */
.upload-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-demo {
  display: inline-block;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .allocation-flow {
    padding: 10px;
  }
  
  .steps-container {
    padding: 20px 10px;
  }
  
  .quick-nav {
    position: static;
    margin-top: 20px;
    text-align: center;
  }
  
  .result-details {
    max-width: 100%;
  }
  
  .waiting-info {
    max-width: 100%;
  }
}

/* 步骤样式优化 */
:deep(.el-steps) {
  margin: 0;
}

:deep(.el-step__title) {
  font-weight: 600;
}

:deep(.el-step__description) {
  color: #909399;
}

/* 表单样式优化 */
:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

:deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

/* 结果页面样式 */
:deep(.el-descriptions__title) {
  font-weight: 600;
  margin-bottom: 16px;
}

:deep(.el-descriptions-item__label) {
  font-weight: 500;
}

/* 表格样式 */
:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: #fafafa;
  font-weight: 600;
}

/* 团队组建样式 */
.team-info {
  margin-bottom: 24px;
}

.assigned-area {
  margin-bottom: 20px;
}

.assigned-area h4,
.current-team h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.team-members {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

/* 4人间：2x2 布局，限制最大宽度 */
@media (min-width: 768px) {
  .team-members[data-team-size="4"] {
    grid-template-columns: repeat(2, minmax(180px, 280px));
    justify-content: start;
  }
}

/* 6人间：3x2 布局，限制最大宽度 */
@media (min-width: 768px) {
  .team-members[data-team-size="6"] {
    grid-template-columns: repeat(3, minmax(180px, 240px));
    justify-content: start;
  }
}

/* 8人间：4x2 布局，限制最大宽度 */
@media (min-width: 1024px) {
  .team-members[data-team-size="8"] {
    grid-template-columns: repeat(4, minmax(180px, 220px));
    justify-content: start;
  }
}

/* 平板设备：8人间改为2x4 */
@media (min-width: 768px) and (max-width: 1023px) {
  .team-members[data-team-size="8"] {
    grid-template-columns: repeat(2, minmax(180px, 280px));
    justify-content: start;
  }
}

/* 小屏幕：所有布局都是单列 */
@media (max-width: 767px) {
  .team-members {
    grid-template-columns: 1fr;
  }
}

.member-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 12px !important;
  border: 2px solid #ebeef5;
  border-radius: 8px !important;
  background: #fff;
  transition: all 0.3s;
  min-height: 110px !important;
  max-width: 100%;
  overflow: visible;
  z-index: 1;
}

.member-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.member-card.empty {
  border-style: dashed;
  background: #fafafa;
  color: #909399;
  justify-content: center;
  align-items: center;
}

/* 操作菜单按钮 */
.member-actions-dropdown {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1000;
  transition: all 0.2s ease;
  pointer-events: auto;
  display: block !important;
  visibility: visible !important;
}

/* 默认状态：显示按钮（完全不透明，确保可见） */
.member-card .member-actions-dropdown {
  opacity: 1 !important;
  display: block !important;
  visibility: visible !important;
}

/* 悬停卡片时：显示完整按钮（完全不透明） */
.member-card:hover .member-actions-dropdown {
  opacity: 1 !important;
}

/* 悬停按钮时：保持显示 */
.member-card .member-actions-dropdown:hover {
  opacity: 1 !important;
}

.more-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #606266;
  transition: all 0.2s ease;
  display: flex !important;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 1) !important;
  border: 1px solid #e4e7ed;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: auto;
  z-index: 1001;
  position: relative;
}

.more-btn .el-icon {
  font-size: 16px;
  width: 16px;
  height: 16px;
}

/* 悬停卡片时：按钮样式变化（显示背景和阴影） */
.member-card:hover .more-btn {
  color: #409eff;
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.25);
  border-color: #409eff;
}

/* 悬停按钮时：高亮显示 */
.more-btn:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  transform: scale(1.1);
  border-color: #409eff;
}

.more-btn:active {
  transform: scale(0.95);
}

/* 下拉菜单样式 */
:deep(.member-action-dropdown-menu) {
  z-index: 10000 !important;
}

/* 成员内容区域 */
.member-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.member-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.leader-badge {
  flex-shrink: 0;
}

.member-detail {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-actions {
  margin-top: 20px;
}

.invite-section,
.received-section,
.search-section {
  padding: 16px 0;
}

/* 移除空状态组件的背景色 */
.search-section :deep(.el-empty) {
  background: transparent;
  padding: 40px 0;
}

.invite-history {
  margin-top: 12px;
}

.invite-history h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
}

/* 已发送邀请表格容器 - 支持滚动 */
.sent-invite-table-container {
  border-radius: 4px;
  overflow: hidden;
}

.sent-invite-table-container .el-table {
  border-radius: 4px;
}

/* 自定义表格滚动条 */
.sent-invite-table-container :deep(.el-table__body-wrapper)::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.sent-invite-table-container :deep(.el-table__body-wrapper)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.sent-invite-table-container :deep(.el-table__body-wrapper)::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.sent-invite-table-container :deep(.el-table__body-wrapper)::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* 邀请列表容器 - 支持滚动 */
.invite-list-container {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

/* 自定义滚动条样式 */
.invite-list-container::-webkit-scrollbar {
  width: 6px;
}

.invite-list-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.invite-list-container::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.invite-list-container::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

.invite-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.invite-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.invite-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.invite-details {
  flex: 1;
}

.sender-name {
  font-weight: 500;
  color: #303133;
}

.sender-info,
.invite-time {
  font-size: 12px;
  color: #909399;
}

.invite-actions {
  display: flex;
  gap: 8px;
}

.student-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 8px;
}

.student-info {
  flex: 1;
}

.student-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.student-detail {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.student-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.team-operations {
  margin-top: 24px;
  padding: 16px;
  background: #f0f9ff;
  border-radius: 8px;
}

.leader-note {
  color: #909399;
  font-size: 14px;
}

/* 房间选择样式 */
.team-summary {
  margin-bottom: 20px;
}

.team-members-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.room-selection h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.room-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.room-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.room-card.selected {
  border-color: #409eff;
  background: #f0f9ff;
  border-width: 2px;
}

.room-card.occupied {
  border-color: #f56c6c;
  background: #fef0f0;
  cursor: not-allowed;
  opacity: 0.7;
}

.room-card.reserved {
  border-color: #e6a23c;
  background: #fdf6ec;
  cursor: not-allowed;
  opacity: 0.7;
}

.room-number {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.room-info {
  margin-bottom: 8px;
}

.room-floor,
.room-type {
  font-size: 12px;
  color: #606266;
  margin-bottom: 2px;
}

.room-status {
  margin-top: 4px;
}

.room-features {
  display: flex;
  gap: 6px;
  color: #409eff;
  font-size: 14px;
}

.room-details {
  margin: 24px 0;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.room-details h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.room-features-detail h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
}

.features-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.room-actions {
  text-align: center;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.action-note {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

/* 等待提示样式 */
.waiting-tip {
  padding: 30px;
  text-align: center;
}

.waiting-tip-content {
  display: inline-flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 32px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-left: 4px solid #409eff;
  border-radius: 8px;
  text-align: left;
}

.waiting-icon {
  font-size: 24px;
  color: #409eff;
  flex-shrink: 0;
  margin-top: 2px;
}

.waiting-text {
  flex: 1;
}

.waiting-title {
  margin: 0 0 6px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.waiting-desc {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

/* 分配结果优化样式 */
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.result-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.phase-badge {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 16px !important;
  height: auto !important;
  line-height: normal !important;
}

.phase-badge .el-icon {
  font-size: 16px;
  margin: 0 !important;
  vertical-align: middle;
}

.phase-badge span {
  line-height: normal;
  vertical-align: middle;
}

/* 状态信息卡片 */
.status-info-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 28px;
  border-left: 4px solid;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.status-info-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.status-info-card.adjustable {
  background: linear-gradient(135deg, #fff7e6 0%, #fff3d9 100%);
  border-left-color: #E6A23C;
}

.status-info-card.confirmed {
  background: linear-gradient(135deg, #f0f9ff 0%, #e7f5f0 100%);
  border-left-color: #67C23A;
}

.status-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.status-info-card.adjustable .status-icon {
  color: #E6A23C;
}

.status-info-card.confirmed .status-icon {
  color: #67C23A;
}

.status-info-card.waiting {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eef5 100%);
  border-left-color: #909399;
}

.status-info-card.waiting .status-icon {
  color: #909399;
}

/* 等待管理员分配样式 */
.waiting-admin-allocation {
  text-align: center;
  padding: 60px 20px;
}

.waiting-icon-simple {
  margin-bottom: 24px;
}

.waiting-title h2 {
  margin: 0 0 12px 0;
  font-size: 22px;
  color: #303133;
  font-weight: 500;
}

.waiting-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.waiting-tip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 24px;
  padding: 12px 20px;
  background: #f5f7fa;
  border-radius: 8px;
  color: #909399;
  font-size: 14px;
}

.waiting-action {
  margin-top: 20px;
}

.status-text {
  flex: 1;
}

.status-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.status-desc {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.status-desc strong {
  color: #E6A23C;
  font-weight: 600;
}

.result-content {
  padding: 20px;
}

.result-success-icon {
  text-align: center;
  margin-bottom: 20px;
}

.result-title {
  text-align: center;
  margin-bottom: 32px;
}

.result-title h2 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.result-subtitle {
  margin: 0;
  font-size: 16px;
  color: #909399;
}

/* 简化的分配摘要 */
.result-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 12px;
  border: 1px solid #b3d8ff;
  transition: all 0.3s;
}

.summary-item:hover {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.summary-icon {
  font-size: 32px;
  color: #409eff;
  flex-shrink: 0;
}

.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 13px;
  color: #606266;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.result-actions {
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.result-actions .el-button {
  min-width: 160px;
}

.result-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: #fef0f0;
  border-left: 4px solid #f56c6c;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
}

.result-tip .el-icon {
  color: #f56c6c;
}

/* 宿舍详情对话框样式 */
.dorm-details {
  padding: 10px 0;
}

.details-section {
  margin-bottom: 24px;
}

.details-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.section-title .el-icon {
  color: #409eff;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.info-label {
  font-size: 14px;
  color: #909399;
}

.info-value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.facilities-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.facilities-tags .el-tag {
  font-size: 14px;
  padding: 8px 16px;
}

.roommates-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.roommate-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.roommate-item:hover {
  background: #e8f4ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.roommate-item .roommate-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

/* 申请历史表格样式 */
.transfer-history {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.transfer-history h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.transfer-history h4::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: #409eff;
  border-radius: 2px;
}

.transfer-history .el-table {
  border-radius: 8px;
  overflow: hidden;
}

.transfer-history .el-table th {
  background: #f5f7fa !important;
  font-weight: 600;
  color: #303133;
}

.transfer-history .reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
  max-height: 3em;
}

</style>
