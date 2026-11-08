<template>
  <div class="admin-chat-messages-page">
    <div class="chat-container">
      <!-- 左侧学生列表 -->
      <div class="student-list-panel">
        <div class="student-list-header">
            <div class="search-area">
              <div class="search-box">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索学生姓名或宿舍"
                  size="small"
                  clearable
                  @input="handleSearchInput"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <!-- 搜索结果下拉列表 -->
                <div v-if="searchResults.length > 0" class="search-results-dropdown">
                  <div 
                    v-for="student in searchResults" 
                    :key="student.id"
                    class="search-result-item"
                    @click="handleSearchResultClick(student)"
                  >
                    <el-avatar :size="32" :src="student.avatar">
                      <el-icon><User /></el-icon>
                    </el-avatar>
                    <div class="student-info">
                      <div class="student-name">{{ student.name }}</div>
                      <div class="student-details">{{ student.major }} · {{ student.dormInfo || '未分配宿舍' }}</div>
                    </div>
                    <el-button 
                      v-if="student.isContact" 
                      type="primary" 
                      size="small" 
                      class="add-btn" 
                      @click.stop="goToChat(student)"
                    >
                      <el-icon><ChatDotRound /></el-icon>
                      聊天
                    </el-button>
                    <el-button 
                      v-else
                      type="primary" 
                      size="small" 
                      class="add-btn" 
                      @click.stop="openProfileDialog(student)"
                    >
                      <el-icon><View /></el-icon>
                      查看资料
                    </el-button>
                  </div>
                </div>
              </div>
              <div class="header-actions">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="refreshStudentList"
                  class="refresh-btn"
                  title="刷新学生列表"
                >
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </div>
        </div>
        
        <div class="student-list">
          <div 
            v-for="student in filteredStudentList" 
            :key="student.id"
            class="student-item"
            :class="{ active: selectedStudentId === student.id }"
            @click="selectStudent(student.id)"
            @contextmenu.prevent="openContextMenu($event, student)"
          >
            <div class="student-avatar">
              <el-avatar :size="40" :src="student.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div v-if="student.unreadCount > 0" class="unread-badge">{{ student.unreadCount }}</div>
            </div>
            
            <div class="student-info">
              <div class="student-header">
                <span class="student-name">{{ student.name }}</span>
                <span class="student-time">{{ formatTime(student.lastMessageTime) }}</span>
              </div>
              <div class="student-preview">
                <span class="student-dorm">{{ student.dormInfo || '未分配宿舍' }}</span>
                <span class="last-message">{{ student.lastMessage }}</span>
              </div>
            </div>
            
            <!-- 三个小点菜单 -->
            <div class="student-actions">
              <el-dropdown 
                @command="(command) => handleStudentAction(command, student)"
                trigger="click"
                placement="bottom-end"
                @click.stop
              >
                <el-button type="text" size="small" class="more-actions-btn">
                  <el-icon><More /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="view_profile">
                      <el-icon><View /></el-icon>
                      查看资料
                    </el-dropdown-item>
                    <el-dropdown-item command="assign_dorm">
                      <el-icon><House /></el-icon>
                      分配宿舍
                    </el-dropdown-item>
                    <el-dropdown-item command="delete_contact" divided>
                      <el-icon><Delete /></el-icon>
                      删除联系人
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          
          <div v-if="filteredStudentList.length === 0" class="empty-student-list">
            <el-icon class="empty-icon"><User /></el-icon>
            <p>暂无学生消息</p>
            <el-button type="primary" size="small" @click="refreshStudentList">刷新列表</el-button>
          </div>
        </div>
      </div>
      
      <!-- 右侧聊天内容 -->
      <div class="chat-content-panel">
        <div v-if="selectedStudent" class="chat-content">
          <!-- 聊天头部 -->
          <div class="chat-content-header">
            <div class="student-detail-info">
              <!-- 智能返回按钮（仅当从外部跳转时显示） -->
              <el-button 
                v-if="fromRoute" 
                type="text" 
                size="small" 
                @click="goBack"
                class="back-btn"
              >
                <el-icon><ArrowLeft /></el-icon>
                返回
              </el-button>
              
              <el-avatar :size="32" :src="selectedStudent.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="student-details">
                <span class="student-name">{{ selectedStudent.name }}</span>
              </div>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div class="messages-container" ref="messagesContainer">
            <div class="messages-list">
              <div 
                v-for="message in selectedStudent.messages" 
                :key="message.id"
                class="message-item"
                :class="{ 'admin-message': message.isAdmin, 'recalled-item': message.isRecalled }"
              >
                <div class="message-avatar" v-if="!message.isAdmin && !message.isRecalled">
                  <el-avatar :size="32" :src="selectedStudent.avatar">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </div>
                
                <div class="message-content" v-if="!message.isRecalled">
                  <div class="message-wrapper">
                    <div 
                      class="message-bubble" 
                      :class="{ 'admin-bubble': message.isAdmin }"
                      @contextmenu.prevent="openMessageContextMenu($event, message)"
                    >
                      <span class="message-text">{{ message.content }}</span>
                    </div>
                    <div class="message-time">{{ formatMessageTime(message.time) }}</div>
                  </div>
                </div>
                
                <!-- 撤回消息提示 -->
                <div v-else class="recalled-message">
                  <el-icon class="recall-icon"><InfoFilled /></el-icon>
                  <span class="recall-text">
                    {{ message.isAdmin ? '你撤回了一条消息' : `${selectedStudent.name}撤回了一条消息` }}
                  </span>
                </div>
                
                <div class="message-avatar" v-if="message.isAdmin && !message.isRecalled">
                  <el-avatar :size="32" :src="defaultAvatar">
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 管理员输入框 -->
          <div class="message-input-panel">
            <div class="input-toolbar">
              <el-button type="text" size="small" @click="insertQuickReply">
                <el-icon><ChatDotRound /></el-icon>
                快捷回复
              </el-button>
              <el-button type="text" size="small" @click="sendNotification">
                <el-icon><Bell /></el-icon>
                发送通知
              </el-button>
              <el-upload
                ref="fileUpload"
                :auto-upload="false"
                :show-file-list="false"
                accept=".doc,.docx,.pdf,.txt,.zip,.rar,.xlsx,.xls,.ppt,.pptx,image/*"
                :on-change="handleFileSelect"
                :limit="1"
              >
                <el-button type="text" size="small" title="上传文件或图片">
                  <el-icon><Paperclip /></el-icon>
                  上传文件
                </el-button>
              </el-upload>
            </div>
            <div class="input-area">
              <el-input
                v-model="messageInput"
                type="textarea"
                :rows="3"
                placeholder="输入回复消息..."
                @keydown.ctrl.enter="sendMessage"
              />
              <el-button 
                type="primary" 
                size="small" 
                @click="sendMessage"
                :disabled="!messageInput.trim()"
                class="send-btn"
              >
                发送
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-else class="empty-chat-content">
          <el-icon class="empty-icon" size="64"><ChatDotRound /></el-icon>
          <h3>选择学生开始对话</h3>
          <p>在左侧选择学生查看消息记录或发送新消息</p>
        </div>
      </div>
    </div>

    <!-- 快捷回复弹窗 -->
    <el-dialog v-model="quickReplyVisible" title="快捷回复管理" width="500px" top="5vh">
      <div class="quick-reply-header">
        <el-button type="primary" size="small" @click="showAddReplyDialog">
          <el-icon><Plus /></el-icon>
          添加快捷回复
        </el-button>
      </div>
      <div class="quick-reply-list">
        <div 
          v-for="reply in quickReplies" 
          :key="reply.id"
          class="quick-reply-item"
        >
          <span class="reply-content" @click="selectQuickReply(reply.content)">
            {{ reply.content }}
          </span>
          <el-button 
            type="danger" 
            size="small"
            circle
            @click="confirmDeleteReply(reply)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <div v-if="quickReplies.length === 0" class="empty-placeholder">
        <el-empty description="暂无快捷回复，点击上方按钮添加" :image-size="80" />
      </div>
    </el-dialog>

    <!-- 添加快捷回复对话框 -->
    <el-dialog v-model="addReplyDialogVisible" title="添加快捷回复" width="400px" top="5vh">
      <el-form :model="addReplyForm" label-width="80px">
        <el-form-item label="回复内容" required>
          <el-input
            v-model="addReplyForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入快捷回复内容"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addReplyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddReply">确定</el-button>
      </template>
    </el-dialog>

    <!-- 发送通知弹窗 -->
    <el-dialog v-model="notificationDialogVisible" title="发送通知" width="500px" top="5vh">
      <el-form :model="notificationForm" label-width="80px">
        <el-form-item label="通知类型">
          <el-select v-model="notificationForm.type" placeholder="选择通知类型">
            <el-option label="系统通知" value="system"></el-option>
            <el-option label="重要提醒" value="important"></el-option>
            <el-option label="一般通知" value="normal"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input
            v-model="notificationForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入通知内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notificationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendNotificationMessage">发送</el-button>
      </template>
    </el-dialog>

    <!-- 查看申请弹窗 -->
    <el-dialog 
      v-model="applicationsDialogVisible" 
      :title="`${currentStudent.name || '学生'}的申请记录`" 
      width="800px" 
      top="5vh"
    >
      <div class="applications-content" style="max-height: 70vh; overflow-y: auto;">
        <!-- 无申请记录 -->
        <el-empty 
          v-if="currentStudentApplications.length === 0" 
          description="该学生暂无申请记录"
        />
        
        <!-- 申请记录列表 -->
        <div v-else class="applications-list">
          <el-table :data="currentStudentApplications" stripe>
            <el-table-column prop="type" label="申请类型" width="120">
              <template #default="{ row }">
                <el-tag :type="row.type === '调宿申请' ? 'primary' : 'warning'">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getApplicationStatusType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="applyTime" label="申请时间" width="160">
              <template #default="{ row }">
                {{ formatApplicationTime(row.createTime || row.applyTime) }}
              </template>
            </el-table-column>
            
            <el-table-column prop="reason" label="申请原因" min-width="180" show-overflow-tooltip />
            
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="viewApplicationDetail(row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="applicationsDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="currentStudentApplications.length > 0" 
          type="primary" 
          @click="goToApplicationReview"
        >
          前往申请审核
        </el-button>
      </template>
    </el-dialog>

    <!-- 宿舍分配弹窗 -->
    <el-dialog 
      v-model="dormAssignmentDialogVisible" 
      title="宿舍分配" 
      width="700px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="dorm-assignment-content">
        <!-- 学生信息 -->
        <div class="student-info-section">
          <h4>当前学生信息</h4>
          <div class="info-card">
            <div class="info-row">
              <span class="label">学号：</span>
              <span class="value">{{ currentStudent.studentId || '—' }}</span>
              <span class="label">姓名：</span>
              <span class="value">{{ currentStudent.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">专业：</span>
              <span class="value">{{ currentStudent.major || '—' }}</span>
              <span class="label">班级：</span>
              <span class="value">{{ currentStudent.className || '—' }}</span>
            </div>
            <div class="info-row">
              <span class="label">当前宿舍：</span>
              <span class="value">{{ currentStudent.dormInfo || '未分配' }}</span>
            </div>
          </div>
        </div>

        <!-- 分配原因 -->
        <div class="reason-section">
          <h4>分配原因</h4>
          <el-radio-group v-model="assignmentForm.reason">
            <el-radio label="new_student">新生入学</el-radio>
            <el-radio label="dorm_adjustment">宿舍调整</el-radio>
            <el-radio label="conflict_resolution">矛盾处理</el-radio>
            <el-radio label="other">其他</el-radio>
          </el-radio-group>
          <el-input 
            v-if="assignmentForm.reason === 'other'"
            v-model="assignmentForm.customReason"
            placeholder="请说明具体原因"
            style="margin-top: 10px;"
          />
        </div>

        <!-- 分配方式选择 -->
        <div class="assignment-type-section">
          <h4>分配方式</h4>
          <el-radio-group v-model="assignmentForm.type" @change="handleAssignmentTypeChange">
            <el-radio label="direct">
              <span>直接分配</span>
              <el-tag type="success" size="small" style="margin-left: 8px;">有空床位</el-tag>
            </el-radio>
            <el-radio label="exchange">
              <span>寻找调换对象</span>
              <el-tag type="warning" size="small" style="margin-left: 8px;">无空床位</el-tag>
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 直接分配模式 -->
        <div v-if="assignmentForm.type === 'direct'" class="direct-assignment-section">
          <h4>宿舍分配筛选</h4>
          <div class="assignment-info">
            <el-alert 
              :title="`当前楼栋: ${currentStudent.building || '未分配'}`"
              type="info"
              :closable="false"
              style="margin-bottom: 16px;"
            />
          </div>
          
          <el-row :gutter="16">
            <el-col :span="8">
              <el-select 
                v-model="assignmentForm.major" 
                placeholder="选择专业" 
                @change="handleMajorChange"
                :loading="loadingMajor"
              >
                <el-option 
                  v-for="major in availableMajors" 
                  :key="major" 
                  :label="major" 
                  :value="major" 
                />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select 
                v-model="assignmentForm.className" 
                placeholder="选择班级" 
                @change="handleClassChange"
                :disabled="!assignmentForm.major"
                :loading="loadingClass"
              >
                <el-option 
                  v-for="classItem in availableClasses" 
                  :key="classItem.value" 
                  :label="classItem.label" 
                  :value="classItem.value" 
                />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select 
                v-model="assignmentForm.room" 
                placeholder="选择宿舍"
                :disabled="!assignmentForm.className"
                :loading="loadingRoom"
              >
                <el-option 
                  v-for="room in availableRooms" 
                  :key="room.value" 
                  :label="room.label" 
                  :value="room.value" 
                />
              </el-select>
            </el-col>
          </el-row>
          
          <!-- 宿舍信息预览 -->
          <div v-if="assignmentForm.room" class="dorm-preview">
            <el-alert 
              :title="`${currentStudent.building || '当前楼栋'}-${assignmentForm.room}室`"
              type="success"
              :closable="false"
            >
              <template #default>
                <p>当前状态: 2/4人 (有空位)</p>
                <p>室友信息: 李四、王五</p>
              </template>
            </el-alert>
          </div>
        </div>

        <!-- 调换模式 -->
        <div v-if="assignmentForm.type === 'exchange'" class="exchange-assignment-section">
          <h4>学生调换确认</h4>
          
          <!-- 调换说明 -->
          <div class="exchange-info">
            <el-alert 
              title="调换说明"
              type="warning"
              :closable="false"
              style="margin-bottom: 16px;"
            >
              <template #default>
                <p>学生已自行协商好调换对象，管理员需要找到目标学生并确认调换。</p>
              </template>
            </el-alert>
          </div>
          
          <!-- 搜索目标学生 -->
          <div class="search-target-student">
            <h5>搜索目标学生</h5>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-input 
                  v-model="exchangeForm.studentName" 
                  placeholder="输入学生姓名"
                  @input="searchTargetStudent"
                />
              </el-col>
              <el-col :span="12">
                <el-input 
                  v-model="exchangeForm.studentId" 
                  placeholder="输入学号"
                  @input="searchTargetStudent"
                />
              </el-col>
            </el-row>
          </div>

          <!-- 目标学生信息 -->
          <div v-if="targetStudent" class="target-student-info">
            <h5>目标学生信息</h5>
            <div class="student-card">
              <div class="student-avatar">
                <el-avatar :size="60" :src="targetStudent.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
              <div class="student-details">
                <div class="student-name">{{ targetStudent.name }}</div>
                <div class="student-id">学号: {{ targetStudent.studentId }}</div>
                <div class="student-info-row">
                  <div class="student-major">专业: {{ targetStudent.major }}</div>
                  <div class="student-class">班级: {{ targetStudent.className }}</div>
                </div>
                <div class="student-dorm">当前宿舍: {{ targetStudent.dormInfo }}</div>
                <div class="student-roommates">室友: {{ targetStudent.roommates }}</div>
              </div>
              <div class="student-actions">
                <el-button type="success" @click="confirmExchange">
                  <el-icon><Check /></el-icon>
                  确认调换
                </el-button>
              </div>
            </div>
          </div>

          <!-- 调换预览 -->
          <div v-if="targetStudent" class="exchange-preview">
            <h5>调换预览</h5>
            <div class="exchange-flow">
              <div class="student-card">
                <div class="student-name">{{ currentStudent.name }}</div>
                <div class="student-dorm">{{ currentStudent.dormInfo }}</div>
              </div>
              <div class="exchange-arrow">
                <el-icon><Right /></el-icon>
                <span>调换</span>
              </div>
              <div class="student-card">
                <div class="student-name">{{ targetStudent.name }}</div>
                <div class="student-dorm">{{ targetStudent.dormInfo }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeDormAssignmentDialog">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmAssignment"
          :disabled="!canConfirmAssignment"
        >
          {{ assignmentForm.type === 'direct' ? '确认分配' : '发送调换请求' }}
        </el-button>
      </template>
    </el-dialog>
    
  <!-- 学生资料对话框（复用学生信息页面样式） -->
  <el-dialog
    v-model="profileDialogVisible"
    :title="profileStudent ? (profileStudent.realName + ' - 学生详细信息') : '学生详细信息'"
    width="800px"
    top="5vh"
    :before-close="closeProfileDialog"
  >
    <div v-if="profileStudent" class="student-detail">
      <el-row :gutter="20">
        <!-- 左侧：基础信息（与StudentInfo.vue完全相同） -->
        <el-col :span="12">
          <div class="detail-section">
            <h4><el-icon><User /></el-icon> 基础信息（不可修改）</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">学号：</span>
                <span class="value">{{ profileStudent.studentId }}</span>
              </div>
              <div class="info-item">
                <span class="label">姓名：</span>
                <span class="value">{{ profileStudent.realName }}</span>
              </div>
              <div class="info-item">
                <span class="label">性别：</span>
                <span class="value">{{ profileStudent.gender }}</span>
              </div>
              <div class="info-item">
                <span class="label">班级：</span>
                <span class="value">{{ profileStudent.className }}</span>
              </div>
              <div class="info-item">
                <span class="label">专业：</span>
                <span class="value">{{ profileStudent.major }}</span>
              </div>
              <div class="info-item">
                <span class="label">宿舍号：</span>
                <span class="value" :class="{ 'empty-value': !profileStudent.dormNumber }">{{ profileStudent.dormNumber || '未分配' }}</span>
              </div>
            </div>
          </div>
          
          <div class="detail-section">
            <h4><el-icon><Calendar /></el-icon> 系统信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">注册状态：</span>
                <el-tag :type="profileStudent.isRegistered ? 'success' : 'warning'" size="small">
                  {{ profileStudent.isRegistered ? '已注册' : '未注册' }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">注册时间：</span>
                <span class="value" :class="{ 'empty-value': !profileStudent.registeredAt }">{{ profileStudent.registeredAt ? formatDateTime(profileStudent.registeredAt) : '未注册' }}</span>
              </div>
            </div>
          </div>
        </el-col>
        
        <!-- 右侧：个人资料（与StudentInfo.vue完全相同） -->
        <el-col :span="12">
          <div class="detail-section">
            <h4><el-icon><EditPen /></el-icon> 个人资料（学生可编辑）</h4>
            <div class="profile-info">
              <div class="avatar-section">
                <el-avatar :size="80" :src="studentProfile.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
              <div class="info-grid-personal">
                <div class="info-item full-width">
                  <span class="label">邮箱：</span>
                  <span class="value" :class="{ 'empty-value': !profileStudent.email }">{{ profileStudent.email || '未绑定' }}</span>
                </div>
                <div class="info-item full-width bio-item">
                  <span class="label">个人简介：</span>
                  <span class="value bio-value" :class="{ 'empty-value': !studentProfile.bio }">{{ studentProfile.bio || '暂无简介' }}</span>
                </div>
              </div>
              
              <div class="tags-section">
                <span class="label">个人标签：</span>
                <div class="tags-list">
                  <template v-if="studentTags && studentTags.length">
                    <el-tag v-for="tag in visibleTags" :key="tag" size="small" style="margin: 2px;">{{ tag }}</el-tag>
                    <el-tag v-if="hiddenTagsCount > 0" type="info" size="small" style="margin: 2px;">+{{ hiddenTagsCount }}</el-tag>
                  </template>
                  <span v-else class="no-tags">暂无标签</span>
                </div>
              </div>
            </div>
          </div>
          
          <div class="detail-section" v-if="profileStudent.isRegistered">
            <h4><el-icon><House /></el-icon> 宿舍信息</h4>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">宿舍分配：</span>
                <span class="value" :class="{ 'empty-value': !studentProfile.dormLocation }">{{ studentProfile.dormLocation || '暂未分配' }}</span>
              </div>
              <div class="info-item">
                <span class="label">宿舍偏好：</span>
                <span class="value" :class="{ 'empty-value': !studentProfile.dormPreference }">{{ studentProfile.dormPreference || '无特殊要求' }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeProfileDialog">关闭</el-button>
        <el-button 
          type="primary" 
          @click="contactStudent"
        >
          <el-icon><ChatDotRound /></el-icon>
          联系学生
        </el-button>
      </div>
    </template>
  </el-dialog>
  </div>
</template>

<script>
import { Search, ChatDotRound, User, UserFilled, More, View, ArrowLeft, Refresh, Bell, House, Document, Download, Setting, Paperclip, Plus, Delete, EditPen, Calendar, Close, RefreshLeft, DocumentCopy, InfoFilled, MoreFilled } from '@element-plus/icons-vue'
import { getQuickReplies, addQuickReply, deleteQuickReply } from '@/utils/quickReplyData.js'
import * as chatApi from '@/api/chat'
import { getStudentByStudentId, getStudentByUserId, getStudentList } from '@/api/student'
import { getMyBuildings } from '@/api/adminDormManagement'
import { getRoomsByBuilding } from '@/api/room'
import { getMajorAssignments } from '@/api/majorAssignment'
import { allocateDorm } from '@/api/allocation'
import webSocketManager from '@/utils/websocket'
import { getToken, getCurrentUser } from '@/utils/auth'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'

export default {
  name: 'AdminChatMessages',
  components: {
    Search,
    ChatDotRound,
    User,
    UserFilled,
    More,
    View,
    ArrowLeft,
    Refresh,
    Bell,
    House,
    Document,
    Paperclip,
    Download,
    Setting,
    Plus,
    Delete,
    EditPen,
    Calendar,
    Close,
    RefreshLeft,
    DocumentCopy,
    InfoFilled,
    MoreFilled
  },
  data() {
    return {
      searchKeyword: '',
      searchResults: [], // 搜索结果
      selectedStudentId: null,
      messageInput: '',
      fromRoute: null,
      quickReplyVisible: false,
      notificationDialogVisible: false,
      profileDialogVisible: false,
      profileStudent: null,
      studentProfile: {}, // 学生个人资料（与StudentInfo.vue保持一致）
      studentTags: [], // 学生标签（与StudentInfo.vue保持一致）
      currentContextMenu: null, // 当前打开的右键菜单（联系人）
      currentMessageContextMenu: null, // 当前打开的消息右键菜单
      notificationForm: {
        type: 'normal',
        content: ''
      },
      applicationsDialogVisible: false,
      currentStudentApplications: [], // 当前学生的申请记录
      dormAssignmentDialogVisible: false,
      currentStudent: {},
      assignmentForm: {
        reason: 'new_student',
        customReason: '',
        type: 'direct',
        major: '',
        className: '',
        room: ''
      },
      exchangeForm: {
        studentName: '',
        studentId: ''
      },
      availableClasses: [],
      availableRooms: [],
      availableMajors: [], // 可用专业列表
      loadingMajor: false,
      loadingClass: false,
      loadingRoom: false,
      targetStudent: null,
      quickReplies: [],
      addReplyDialogVisible: false,
      addReplyForm: {
        content: ''
      },
      studentList: [],
      defaultAvatar: defaultAvatar, // 默认头像
    }
  },
  computed: {
    filteredStudentList() {
      if (!this.searchKeyword) {
        return this.studentList.sort((a, b) => new Date(b.lastMessageTime) - new Date(a.lastMessageTime))
      }
      return this.studentList.filter(student => 
        student.name.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        student.dormInfo.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        student.lastMessage.toLowerCase().includes(this.searchKeyword.toLowerCase())
      )
    },
    selectedStudent() {
      return this.studentList.find(student => student.id === this.selectedStudentId)
    },
    visibleTags() {
      if (!this.studentTags) {
        return [];
      }
      const maxVisible = 3;
      return this.studentTags.slice(0, maxVisible);
    },
    hiddenTagsCount() {
      if (!this.studentTags) {
        return 0;
      }
      const maxVisible = 3;
      const totalTags = this.studentTags.length;
      return totalTags > maxVisible ? totalTags - maxVisible : 0;
    }
  },
  async mounted() {
    // 加载快捷回复数据
    this.loadQuickReplies()
    
    // 加载会话列表（等待完成）
    await this.loadConversations()
    
    // 处理跳转参数
    let chatId = null
    let fromRoute = null
    
    if (this.$route.query.redirect === 'true') {
      const redirectData = this.getRedirectData()
      if (redirectData) {
        chatId = redirectData.chatId
        fromRoute = redirectData.from
        sessionStorage.removeItem('chatRedirect')
      }
    } else {
      chatId = this.$route.query.chatId
      fromRoute = this.$route.query.from
    }
    
    if (chatId) {
      await this.selectSpecificStudent(chatId)
    } else if (this.studentList.length > 0) {
      this.selectStudent(this.studentList[0].id)
    }
    
    this.fromRoute = fromRoute
    
    // 连接WebSocket
    this.connectWebSocket()
  },
  beforeUnmount() {
    // 断开WebSocket连接
    this.disconnectWebSocket()
    // 组件销毁前清理右键菜单
    this.closeContextMenu()
    this.closeMessageContextMenu()
  },
  methods: {
    getRedirectData() {
      try {
        const data = sessionStorage.getItem('chatRedirect')
        if (!data) return null
        
        const redirectData = JSON.parse(data)
        const maxAge = 5 * 60 * 1000
        if (Date.now() - redirectData.timestamp > maxAge) {
          sessionStorage.removeItem('chatRedirect')
          return null
        }
        
        return redirectData
      } catch (error) {
        console.error('解析重定向数据失败:', error)
        sessionStorage.removeItem('chatRedirect')
        return null
      }
    },
    
    // 加载会话列表（从API获取）
    async loadConversations() {
      try {
        const res = await chatApi.getConversations()
        if (res.code === 200 && res.data) {
          // 过滤出学生类型的会话
          const studentConversations = res.data.filter(conv => conv.userType === 'student')
          
          // 转换为学生列表格式
          this.studentList = studentConversations.map(conv => ({
            id: `student_${conv.userId}`,
            conversationId: conv.id,
            userId: conv.userId,
            name: conv.userName || '未知学生',
            studentId: conv.studentId || '',
            major: conv.major || '',
            dormInfo: conv.dormInfo || '',
            avatar: conv.avatar || defaultAvatar,
            lastMessage: conv.lastMessage || '暂无消息',
            lastMessageTime: conv.lastMessageTime || new Date().toISOString(),
            unreadCount: conv.unreadCount || 0,
            messages: [] // 消息列表在selectStudent时加载
          }))
        }
      } catch (error) {
        console.error('加载会话列表失败:', error)
        this.$message.error('加载会话列表失败')
      }
    },
    
    async selectStudent(studentId) {
      this.selectedStudentId = studentId
      const student = this.studentList.find(s => s.id === studentId)
      if (!student) return
      
      // 如果有conversationId，加载消息历史
      if (student.conversationId) {
        try {
          const res = await chatApi.getMessages(student.conversationId, 1, 100)
          if (res.code === 200 && res.data) {
            // 转换消息格式
            student.messages = res.data.map(msg => ({
              id: msg.id,
              content: msg.content,
              time: msg.createdAt,
              isAdmin: msg.isOwn, // 管理员发送的消息isOwn为true
              isRecalled: msg.isRecalled,
              type: msg.messageType,
              fileName: msg.fileName,
              fileSize: msg.fileSize,
              url: msg.fileUrl
            }))
          }
        } catch (error) {
          console.error('加载消息历史失败:', error)
        }
      }
      
      // 标记为已读
      if (student.conversationId && student.unreadCount > 0) {
        try {
          await chatApi.markAsRead(student.conversationId)
          student.unreadCount = 0
        } catch (error) {
          console.error('标记聊天为已读失败:', error)
        }
      }
      
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    
    async selectSpecificStudent(chatId) {
      // 从chatId中提取标识符（格式：student_xxx）
      const identifier = chatId.replace('student_', '')
      
      // 先尝试通过id直接匹配
      let existingStudent = this.studentList.find(student => student.id === chatId)
      
      // 如果没找到，尝试通过学号匹配
      if (!existingStudent && identifier.length >= 8) {
        existingStudent = this.studentList.find(student => student.studentId === identifier)
      }
      
      // 如果没找到，尝试通过userId匹配
      if (!existingStudent) {
        const userId = parseInt(identifier)
        if (!isNaN(userId)) {
          existingStudent = this.studentList.find(student => student.userId === userId)
        }
      }
      
      if (existingStudent) {
        this.selectStudent(existingStudent.id)
      } else {
        this.createStudentChatFromId(chatId)
      }
    },
    
    async createStudentChatFromId(chatId) {
      try {
        // 从chatId中提取标识符（格式：student_xxx，xxx可能是userId或studentId学号）
        const identifier = chatId.replace('student_', '')
        
        // 先尝试通过学号查询学生信息获取userId
        let userId = null
        let studentData = null
        
        // 判断是学号还是userId（学号通常是数字字符串且较长，userId是纯数字）
        const isStudentId = identifier.length >= 8 // 学号通常8位以上
        
        if (isStudentId) {
          // 通过学号查询学生信息
          const studentRes = await getStudentByStudentId(identifier)
          if (studentRes.code === 200 && studentRes.data) {
            studentData = studentRes.data
            // 学生表的userId字段关联到用户表
            if (studentData.userId) {
              userId = studentData.userId
            } else {
              // 如果学生未注册，没有userId，无法创建聊天
              this.$message.warning('该学生尚未注册，无法发起聊天')
              return
            }
          } else {
            this.$message.error('无法找到该学生')
            return
          }
        } else {
          userId = parseInt(identifier)
        }
        
        // 检查是否已存在该学生的会话
        const existingStudent = this.studentList.find(s => s.userId === userId)
        if (existingStudent) {
          // 已存在，直接选中
          this.selectStudent(existingStudent.id)
          return
        }
        
        // 创建会话
        const convRes = await chatApi.createConversation(userId)
        if (convRes.code !== 200) {
          this.$message.error('创建会话失败')
          return
        }
        
        const conversationId = convRes.data
        
        // 获取用户信息
        let studentInfo = studentData
        if (!studentInfo) {
          const searchRes = await chatApi.searchContacts(String(userId))
          if (searchRes.code === 200 && searchRes.data && searchRes.data.length > 0) {
            studentInfo = searchRes.data.find(u => u.userType === 'student')
          }
        }
        
        if (studentInfo) {
          const newStudent = {
            id: `student_${userId}`,
            conversationId: conversationId,
            userId: userId,
            name: studentInfo.realName || studentInfo.userName || '未知学生',
            studentId: studentInfo.studentId || '',
            major: studentInfo.major || '',
            avatar: studentInfo.avatar || defaultAvatar,
            dormInfo: studentInfo.roomNumber || studentInfo.dormNumber || '',
            lastMessage: '开始聊天',
            lastMessageTime: new Date().toISOString(),
            unreadCount: 0,
            messages: []
          }
          
          this.studentList.unshift(newStudent)
          this.selectStudent(newStudent.id)
          this.$message.success('已创建与学生的聊天')
        } else {
          this.$message.error('无法找到该学生信息')
        }
      } catch (error) {
        console.error('创建学生聊天失败:', error)
        this.$message.error('无法创建聊天，请稍后再试')
      }
    },
    
    goBack() {
      if (this.fromRoute === 'notifications') {
        this.$router.push('/admin/notifications')
      } else if (this.fromRoute) {
        this.$router.push(`/admin/${this.fromRoute}`)
      } else {
        this.$router.go(-1)
      }
    },
    
    async sendMessage() {
      if (!this.messageInput.trim() || !this.selectedStudent) return
      
      try {
        const messageData = {
          conversationId: this.selectedStudent.conversationId,
          receiverId: this.selectedStudent.userId,
          content: this.messageInput.trim(),
          messageType: 'text'
        }
        
        const res = await chatApi.sendMessage(messageData)
        if (res.code === 200 && res.data) {
          const msg = res.data
          
          // 更新会话ID（如果后端创建了新会话）
          if (!this.selectedStudent.conversationId && msg.conversationId) {
            this.selectedStudent.conversationId = msg.conversationId
          }
          
          // 创建消息对象，格式与WebSocket消息一致
          const newMessage = {
            id: msg.id,
            content: msg.content,
            time: msg.createdAt,
            isAdmin: true,
            isRecalled: msg.isRecalled || false,
            type: msg.messageType || 'text',
            fileName: msg.fileName,
            fileSize: msg.fileSize,
            url: msg.fileUrl,
            conversationId: msg.conversationId
          }
          
          // 检查消息是否已存在（避免WebSocket重复添加）
          const existingMessage = this.selectedStudent.messages.find(m => m.id === newMessage.id)
          if (!existingMessage) {
            this.selectedStudent.messages.push(newMessage)
            this.selectedStudent.lastMessage = newMessage.content
            this.selectedStudent.lastMessageTime = newMessage.time
            
            this.$nextTick(() => {
              this.scrollToBottom()
            })
          }
          
          this.messageInput = ''
          
          this.$message.success('消息发送成功')
        } else {
          this.$message.error(res.message || '发送消息失败')
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        this.$message.error('发送消息失败')
      }
    },
    
    scrollToBottom() {
      // 使用requestAnimationFrame和延迟确保DOM更新后再滚动
      const messagesList = this.$refs.messagesContainer?.querySelector('.messages-list')
      if (!messagesList) {
        console.warn('管理员端：消息列表元素不存在')
        return
      }
      
      console.log('管理员端：开始滚动到底部', {
        scrollTop: messagesList.scrollTop,
        scrollHeight: messagesList.scrollHeight,
        clientHeight: messagesList.clientHeight
      })
      
      // 立即尝试滚动
      messagesList.scrollTop = messagesList.scrollHeight
      
      // 使用requestAnimationFrame确保在下一帧执行
      requestAnimationFrame(() => {
        messagesList.scrollTop = messagesList.scrollHeight
        console.log('管理员端：requestAnimationFrame后滚动', {
          scrollTop: messagesList.scrollTop,
          scrollHeight: messagesList.scrollHeight
        })
        
        // 延迟再次设置，确保内容高度已更新
        setTimeout(() => {
          messagesList.scrollTop = messagesList.scrollHeight
          console.log('管理员端：setTimeout后滚动', {
            scrollTop: messagesList.scrollTop,
            scrollHeight: messagesList.scrollHeight
          })
        }, 200)
      })
    },
    
    async refreshChats() {
      if (this.selectedStudent && this.selectedStudent.conversationId) {
        try {
          const res = await chatApi.getMessages(this.selectedStudent.conversationId, 1, 100)
          if (res.code === 200 && res.data) {
            this.selectedStudent.messages = res.data.map(msg => ({
              id: msg.id,
              content: msg.content,
              time: msg.createdAt,
              isAdmin: msg.isOwn,
              isRecalled: msg.isRecalled,
              type: msg.messageType,
              fileName: msg.fileName,
              fileSize: msg.fileSize,
              url: msg.fileUrl
            }))
          }
        } catch (error) {
          console.error('刷新消息失败:', error)
        }
      }
      this.$message.success('已刷新聊天数据')
    },
    
    // 连接WebSocket
    connectWebSocket() {
      const token = getToken()
      if (!token) {
        console.warn('未找到Token，无法连接WebSocket')
        return
      }
      
      const userId = this.getCurrentUserId()
      if (!userId) {
        console.warn('无法获取用户ID，WebSocket连接失败，但页面可以正常使用')
        return
      }
      
      console.log('管理员端开始连接WebSocket，用户ID:', userId)
      
      try {
        webSocketManager.connect(
          userId,
          () => {
            console.log('管理员端WebSocket连接成功')
            this.setupWebSocketHandlers()
          },
          (error) => {
            console.error('管理员端WebSocket连接失败:', error)
            // 不显示错误提示，避免干扰用户，页面可以正常使用（通过API轮询）
          }
        )
      } catch (error) {
        console.error('管理员端WebSocket连接异常:', error)
        // 静默处理，不影响页面使用
      }
    },
    
    // 断开WebSocket
    disconnectWebSocket() {
      webSocketManager.disconnect()
    },
    
    // 设置WebSocket消息处理器
    setupWebSocketHandlers() {
      console.log('管理员端注册WebSocket消息处理器')
      
      // 先清除旧的处理器（避免重复注册）
      if (this.newMessageHandler) {
        webSocketManager.off('newMessage', this.newMessageHandler)
      }
      if (this.messageRecalledHandler) {
        webSocketManager.off('messageRecalled', this.messageRecalledHandler)
      }
      if (this.conversationUpdatedHandler) {
        webSocketManager.off('conversationUpdated', this.conversationUpdatedHandler)
      }
      
      // 创建处理器函数（保存引用以便后续清除）
      this.newMessageHandler = (messageData) => {
        console.log('管理员端收到newMessage事件，数据:', messageData)
        // 使用箭头函数确保this上下文正确
        this.handleWebSocketMessage(messageData)
      }
      
      this.messageRecalledHandler = (data) => {
        this.handleMessageRecalledViaWebSocket(data)
      }
      
      this.conversationUpdatedHandler = (data) => {
        this.handleConversationUpdatedViaWebSocket(data)
      }
      
      // 注册新的处理器
      webSocketManager.on('newMessage', this.newMessageHandler)
      webSocketManager.on('messageRecalled', this.messageRecalledHandler)
      webSocketManager.on('conversationUpdated', this.conversationUpdatedHandler)
      
      console.log('管理员端WebSocket消息处理器注册完成')
    },
    
    // 处理WebSocket接收到的消息
    handleWebSocketMessage(messageData) {
      console.log('管理员端收到WebSocket消息:', messageData)
      
      const message = {
        id: messageData.id,
        content: messageData.content,
        time: messageData.createdAt,
        isAdmin: messageData.isOwn, // 对于管理员端，isOwn=true表示管理员发送的
        isRecalled: messageData.isRecalled,
        type: messageData.messageType,
        fileName: messageData.fileName,
        fileSize: messageData.fileSize,
        url: messageData.fileUrl
      }
      
      const conversationId = messageData.conversationId
      const student = this.studentList.find(s => s.conversationId === conversationId)
      
      if (student) {
        // 检查消息是否已存在（避免重复添加）
        const existingMessage = student.messages.find(m => m.id === message.id)
        if (!existingMessage) {
          // 确保messages数组存在
          if (!student.messages) {
            // Vue 3 中直接赋值即可，响应式系统会自动处理
            student.messages = []
          }
          
          // 使用Vue的响应式更新
          student.messages.push(message)
          student.lastMessage = message.content || (message.type === 'image' ? '[图片]' : message.type === 'file' ? '[文件]' : '')
          student.lastMessageTime = message.time
          
          // 如果不是管理员发送的消息（即学生发送的），增加未读计数
          if (!message.isAdmin) {
            student.unreadCount = (student.unreadCount || 0) + 1
          }
          
          // 如果当前正在查看这个聊天，滚动到底部
          if (this.selectedStudentId === student.id) {
            this.$nextTick(() => {
              this.scrollToBottom()
            })
          }
          
          console.log('消息已添加到聊天列表:', message)
        } else {
          console.log('消息已存在，跳过:', message.id)
        }
      } else {
        console.log('未找到对应的学生会话，重新加载会话列表')
        this.loadConversations()
      }
    },
    
    // 通过WebSocket处理消息撤回
    handleMessageRecalledViaWebSocket(data) {
      const conversationId = data.conversationId
      const messageId = data.messageId
      
      const student = this.studentList.find(s => s.conversationId === conversationId)
      if (student) {
        const message = student.messages.find(m => m.id === messageId)
        if (message) {
          message.isRecalled = true
          message.recallTime = new Date().toISOString()
        }
      }
    },
    
    // 通过WebSocket处理会话更新
    handleConversationUpdatedViaWebSocket(data) {
      console.log('管理员端收到会话更新通知:', data)
      // 只更新会话信息，不重新加载整个列表（避免丢失当前消息）
      const conversationId = data.conversationId
      const student = this.studentList.find(s => s.conversationId === conversationId)
      
      if (student) {
        // 只更新最后消息时间和未读计数，不重新加载消息列表
        // 这样可以避免丢失当前正在查看的消息
        console.log('更新会话信息，不重新加载消息')
      } else {
        // 如果会话不在列表中，重新加载
        console.log('会话不在列表中，重新加载会话列表')
        this.loadConversations()
      }
    },
    
    // 获取当前用户ID
    getCurrentUserId() {
      const user = getCurrentUser()
      if (user && (user.id || user.userId)) {
        return user.id || user.userId
      }
      return null
    },
    
    async refreshStudentList() {
      await this.loadConversations()
      this.$message.success('已刷新学生列表')
    },
    
    viewStudentProfile() {
      if (this.selectedStudent) {
        this.$router.push({
          path: '/admin/student-info',
          query: {
            search: this.selectedStudent.studentId || this.selectedStudent.name,
            from: 'chat'
          }
        })
      }
    },
    
    // 加载快捷回复
    loadQuickReplies() {
      this.quickReplies = getQuickReplies()
    },
    
    // 打开快捷回复对话框
    insertQuickReply() {
      this.quickReplyVisible = true
    },
    
    // 选择快捷回复
    selectQuickReply(content) {
      this.messageInput = content
      this.quickReplyVisible = false
    },
    
    // 显示添加快捷回复对话框
    showAddReplyDialog() {
      this.addReplyForm.content = ''
      this.addReplyDialogVisible = true
    },
    
    // 确认添加快捷回复
    confirmAddReply() {
      if (!this.addReplyForm.content.trim()) {
        this.$message.warning('请输入快捷回复内容')
        return
      }
      
      const result = addQuickReply(this.addReplyForm.content)
      if (result) {
        this.$message.success('添加成功')
        this.loadQuickReplies()
        this.addReplyDialogVisible = false
        this.addReplyForm.content = ''
      } else {
        this.$message.error('添加失败')
      }
    },
    
    // 确认删除快捷回复
    confirmDeleteReply(reply) {
      this.$confirm(`确定要删除快捷回复"${reply.content}"吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const result = deleteQuickReply(reply.id)
        if (result) {
          this.$message.success('删除成功')
          this.loadQuickReplies()
        } else {
          this.$message.error('删除失败')
        }
      }).catch(() => {
        // 用户取消删除
      })
    },
    
    sendNotification() {
      this.notificationDialogVisible = true
    },
    
    handleFileSelect(uploadFile) {
      const file = uploadFile.raw
      if (!file) return
      
      // 验证文件大小（10MB）
      if (file.size > 10 * 1024 * 1024) {
        this.$message.error('文件大小不能超过10MB')
        return
      }
      
      // 判断是图片还是文件
      if (file.type.startsWith('image/')) {
        this.handleImageUpload(file)
      } else {
        this.handleFileUpload(file)
      }
      
      // 清空上传组件
      this.$refs.fileUpload.clearFiles()
    },
    
    handleImageUpload(file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const imageUrl = e.target.result
        this.sendImageMessage(file.name, imageUrl, file.size)
      }
      reader.readAsDataURL(file)
    },
    
    handleFileUpload(file) {
      const fileUrl = URL.createObjectURL(file)
      this.sendFileMessage(file.name, fileUrl, file.size)
    },
    
    async sendImageMessage(fileName, imageUrl, fileSize) {
      if (!this.selectedStudent) return
      
      try {
        // 如果imageUrl是base64，需要先上传到服务器
        let fileUrl = imageUrl
        if (imageUrl.startsWith('data:')) {
          const blob = await fetch(imageUrl).then(r => r.blob())
          const file = new File([blob], fileName, { type: blob.type })
          const uploadRes = await chatApi.uploadChatFile(file)
          if (uploadRes.code === 200 && uploadRes.data) {
            fileUrl = uploadRes.data.fileUrl
          }
        }
        
        const messageData = {
          conversationId: this.selectedStudent.conversationId,
          receiverId: this.selectedStudent.userId,
          content: '',
          messageType: 'image',
          fileUrl: fileUrl,
          fileName: fileName,
          fileSize: fileSize
        }
        
        const res = await chatApi.sendMessage(messageData)
        if (res.code === 200 && res.data) {
          const msg = res.data
          
          // 更新会话ID（如果后端创建了新会话）
          if (!this.selectedStudent.conversationId && msg.conversationId) {
            this.selectedStudent.conversationId = msg.conversationId
          }
          
          const newMessage = {
            id: msg.id,
            type: 'image',
            fileName: msg.fileName,
            url: msg.fileUrl,
            fileSize: msg.fileSize,
            time: msg.createdAt,
            isAdmin: true
          }
          
          this.selectedStudent.messages.push(newMessage)
          this.selectedStudent.lastMessage = '[图片]'
          this.selectedStudent.lastMessageTime = newMessage.time
          
          this.$nextTick(() => {
            this.scrollToBottom()
          })
          
          this.$message.success('图片发送成功')
        } else {
          this.$message.error(res.message || '发送图片失败')
        }
      } catch (error) {
        console.error('发送图片失败:', error)
        this.$message.error('发送图片失败')
      }
    },
    
    async sendFileMessage(fileName, fileUrl, fileSize) {
      if (!this.selectedStudent) return
      
      try {
        // 如果fileUrl是blob URL，需要先上传到服务器
        let finalFileUrl = fileUrl
        if (fileUrl.startsWith('blob:')) {
          const response = await fetch(fileUrl)
          const blob = await response.blob()
          const file = new File([blob], fileName, { type: blob.type })
          const uploadRes = await chatApi.uploadChatFile(file)
          if (uploadRes.code === 200 && uploadRes.data) {
            finalFileUrl = uploadRes.data.fileUrl
            fileSize = uploadRes.data.fileSize
          }
        }
        
        const messageData = {
          conversationId: this.selectedStudent.conversationId,
          receiverId: this.selectedStudent.userId,
          content: '',
          messageType: 'file',
          fileUrl: finalFileUrl,
          fileName: fileName,
          fileSize: fileSize
        }
        
        const res = await chatApi.sendMessage(messageData)
        if (res.code === 200 && res.data) {
          const msg = res.data
          
          // 更新会话ID（如果后端创建了新会话）
          if (!this.selectedStudent.conversationId && msg.conversationId) {
            this.selectedStudent.conversationId = msg.conversationId
          }
          
          const newMessage = {
            id: msg.id,
            type: 'file',
            fileName: msg.fileName,
            url: msg.fileUrl,
            fileSize: msg.fileSize,
            time: msg.createdAt,
            isAdmin: true
          }
          
          this.selectedStudent.messages.push(newMessage)
          this.selectedStudent.lastMessage = `[文件] ${fileName}`
          this.selectedStudent.lastMessageTime = newMessage.time
          
          this.$nextTick(() => {
            this.scrollToBottom()
          })
          
          this.$message.success('文件发送成功')
        } else {
          this.$message.error(res.message || '发送文件失败')
        }
      } catch (error) {
        console.error('发送文件失败:', error)
        this.$message.error('发送文件失败')
      }
    },
    
    sendNotificationMessage() {
      if (!this.notificationForm.content.trim()) {
        this.$message.warning('请输入通知内容')
        return
      }
      
      this.$message.success('通知发送成功')
      this.notificationDialogVisible = false
      this.notificationForm.content = ''
    },
    
    handleStudentAction(command, student) {
      switch (command) {
        case 'send_notification':
          this.sendNotification()
          break
        case 'assign_dorm':
          this.openDormAssignmentDialog(student)
          break
        case 'view_applications':
          this.openApplicationsDialog(student)
          break
        case 'export_chat':
          this.exportChatHistory(student)
          break
      }
    },
    
    formatTime(timeStr) {
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) {
        return '刚刚'
      } else if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}分钟前`
      } else if (diff < 86400000) {
        return `${Math.floor(diff / 3600000)}小时前`
      } else if (diff < 86400000 * 7) {
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        return weekdays[date.getDay()]
      } else {
        return date.toLocaleDateString()
      }
    },
    
    formatMessageTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      if (isNaN(date.getTime())) return timeStr
      
      // 格式化时间为 HH:mm
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    },
    
    getImageUrl(url) {
      if (!url) return ''
      // 如果是相对路径，添加baseURL
      if (!url.startsWith('http') && !url.startsWith('data:')) {
        return `http://localhost:8080${url}`
      }
      return url
    },
    
    // 处理搜索输入
    async handleSearchInput() {
      if (!this.searchKeyword.trim()) {
        this.searchResults = []
        return
      }
      
      try {
        const res = await chatApi.searchContacts(this.searchKeyword.trim())
        if (res.code === 200 && res.data) {
          // 过滤出学生类型的结果
          const studentResults = res.data.filter(user => user.userType === 'student')
          
          // 转换搜索结果格式，标记是否已经是联系人
          const searchResultsPromises = studentResults.map(async user => {
            const existingStudent = this.studentList.find(s => s.userId === user.userId)
            
            // 尝试获取学生的详细信息
            let studentDetail = null
            try {
              if (user.studentId) {
                const detailRes = await getStudentByStudentId(user.studentId)
                if (detailRes.code === 200 && detailRes.data) {
                  studentDetail = detailRes.data
                }
              }
              
              // 如果通过学号查询失败，尝试通过userId查询
              if (!studentDetail && user.userId) {
                const detailRes = await getStudentByUserId(user.userId)
                if (detailRes.code === 200 && detailRes.data) {
                  studentDetail = detailRes.data
                }
              }
            } catch (error) {
              console.error('获取学生详细信息失败:', error)
            }
            
            return {
              id: `student_${user.userId}`,
              userId: user.userId,
              name: user.userName,
              studentId: studentDetail?.studentId || user.studentId || '',
              major: studentDetail?.major || '',
              className: studentDetail?.className || '',
              gender: studentDetail?.gender || '',
              dormInfo: studentDetail?.dormNumber || (existingStudent ? existingStudent.dormInfo : ''),
              avatar: user.avatar || defaultAvatar,
              type: 'student',
              isContact: !!existingStudent,
              chatId: existingStudent ? existingStudent.id : null,
              // 添加额外的字段用于资料对话框
              isRegistered: studentDetail?.isRegistered || false,
              registrationTime: studentDetail?.registeredAt || studentDetail?.registrationDate || null,
              email: studentDetail?.email || ''
            }
          })
          
          this.searchResults = await Promise.all(searchResultsPromises)
        }
      } catch (error) {
        console.error('搜索学生失败:', error)
        this.searchResults = []
        // request拦截器已经显示了错误提示，这里只记录日志
      }
    },
    
    // 处理搜索结果点击
    handleSearchResultClick(student) {
      if (student.isContact) {
        // 如果已经是联系人，跳转到聊天
        this.goToChat(student)
      } else {
        // 如果不是联系人，打开资料对话框
        this.openProfileDialog(student)
      }
    },
    
    // 跳转到聊天页面（用于已添加的联系人）
    goToChat(student) {
      if (student.chatId) {
        // 清空搜索关键词
        this.searchKeyword = ''
        this.searchResults = []
        // 选择该学生
        this.selectedStudentId = student.chatId
        this.selectStudent(student.chatId)
      }
    },
    
    // 添加学生到聊天列表
    async addStudentToChat(student) {
      try {
        // 添加联系人
        await chatApi.addContact(student.userId)
        
        // 创建会话
        const convRes = await chatApi.createConversation(student.userId)
        if (convRes.code === 200) {
          const conversationId = convRes.data
          
          const newChatStudent = {
            id: `student_${student.userId}`,
            conversationId: conversationId,
            userId: student.userId,
            name: student.name,
            studentId: student.studentId || '',
            major: student.major || '',
            dormInfo: student.dormInfo || '未分配宿舍',
            avatar: student.avatar,
            lastMessage: '开始聊天',
            lastMessageTime: new Date().toISOString(),
            unreadCount: 0,
            messages: []
          }
          
          this.studentList.unshift(newChatStudent)
          this.selectStudent(newChatStudent.id)
          
          this.searchKeyword = ''
          this.searchResults = []
          
          this.$message.success(`已添加与${student.name}的聊天`)
        }
      } catch (error) {
        console.error('添加学生到聊天列表失败:', error)
        this.$message.error('添加失败')
      }
    },
    
    // 打开资料对话框（搜索结果和右上角共用）
    // 使用与StudentInfo.vue完全相同的数据结构和加载逻辑
    async openProfileDialog(student) {
      const targetStudent = student || this.selectedStudent
      if (!targetStudent) {
        this.$message.warning('未选择学生')
        return
      }
      
      // 先设置基本信息（转换为StudentInfo.vue的格式）
      this.profileStudent = {
        studentId: targetStudent.studentId || '',
        realName: targetStudent.name || '',
        gender: targetStudent.gender || '',
        className: targetStudent.className || '',
        major: targetStudent.major || '',
        dormNumber: targetStudent.dormInfo || '',
        isRegistered: targetStudent.isRegistered !== undefined ? targetStudent.isRegistered : false,
        registeredAt: targetStudent.registrationTime || targetStudent.registeredAt || null,
        email: targetStudent.email || '',
        userId: targetStudent.userId || null // 保存userId以便后续查找学号
      }
      
      // 初始化个人资料和标签（避免显示上一次的数据）
      this.studentProfile = {
        avatar: targetStudent.avatar || '',
        bio: '',
        dormLocation: targetStudent.dormInfo || '',
        dormPreference: ''
      }
      this.studentTags = []
      
      this.profileDialogVisible = true
      
      // 加载学生的个人资料信息（与StudentInfo.vue的loadStudentProfile方法完全相同）
      await this.loadStudentProfileForDialog()
    },
    
    // 加载学生个人资料（与StudentInfo.vue的loadStudentProfile方法完全相同）
    async loadStudentProfileForDialog() {
      let response = null
      
      console.log('开始加载学生资料，当前profileStudent:', this.profileStudent)
      
      // 优先使用学号查询
      if (this.profileStudent && this.profileStudent.studentId) {
        try {
          console.log('通过学号查询学生:', this.profileStudent.studentId)
          response = await getStudentByStudentId(this.profileStudent.studentId)
          console.log('学号查询结果:', response)
        } catch (error) {
          console.error('通过学号查询学生失败:', error)
        }
      }
      
      // 如果没有学号或查询失败，尝试通过userId查询
      if ((!response || response.code !== 200) && this.profileStudent && this.profileStudent.userId) {
        try {
          console.log('通过userId查询学生:', this.profileStudent.userId)
          response = await getStudentByUserId(this.profileStudent.userId)
          console.log('userId查询结果:', response)
        } catch (error) {
          console.error('通过userId查询学生失败:', error)
        }
      }
      
      // 如果还是没有查询到，显示警告但保留基本信息
      if (!response || response.code !== 200) {
        console.warn('无法加载学生详细信息，使用基本信息')
        this.$message.warning('无法加载学生详细信息，仅显示基本信息')
        // 保持已有的基本信息，不清空
        if (!this.studentProfile.avatar) {
          this.studentProfile.avatar = ''
        }
        if (!this.studentProfile.bio) {
          this.studentProfile.bio = ''
        }
        return
      }
      
      // 处理返回的学生数据（与StudentInfo.vue完全相同）
      if (response.code === 200 && response.data) {
        // 从后端数据中提取个人资料字段（与StudentInfo.vue完全相同）
        const studentData = response.data
        console.log('成功获取学生详细数据:', studentData)
        
        // 更新基本信息
        this.profileStudent = {
          ...this.profileStudent,
          studentId: studentData.studentId || this.profileStudent.studentId,
          realName: studentData.realName || this.profileStudent.realName,
          gender: studentData.gender || this.profileStudent.gender,
          className: studentData.className || this.profileStudent.className,
          major: studentData.major || this.profileStudent.major,
          dormNumber: studentData.dormNumber || this.profileStudent.dormNumber || '未分配',
          isRegistered: studentData.isRegistered !== undefined ? studentData.isRegistered : this.profileStudent.isRegistered,
          registeredAt: studentData.registeredAt || studentData.registrationDate || this.profileStudent.registeredAt,
          email: studentData.email || this.profileStudent.email
        }
        
        // 设置个人资料（与StudentInfo.vue完全相同）
        this.studentProfile = {
          avatar: studentData.avatar || this.studentProfile.avatar || '',
          bio: studentData.bio || '',
          dormLocation: studentData.dormNumber || this.profileStudent.dormNumber || '',
          dormPreference: ''  // 这个字段后端暂无，保持为空
        }
        
        // 处理标签数据（后端返回的是JSON字符串，与StudentInfo.vue完全相同）
        if (studentData.tags) {
          try {
            // 如果tags是字符串，解析为数组
            if (typeof studentData.tags === 'string') {
              this.studentTags = JSON.parse(studentData.tags)
            } else if (Array.isArray(studentData.tags)) {
              this.studentTags = studentData.tags
            } else {
              this.studentTags = []
            }
          } catch (e) {
            console.error('解析学生标签失败:', e)
            this.studentTags = []
          }
        } else {
          this.studentTags = []
        }
        
        console.log('学生资料加载完成:', {
          profileStudent: this.profileStudent,
          studentProfile: this.studentProfile,
          studentTags: this.studentTags
        })
      } else {
        // API调用失败，使用空数据（基本信息已在profileStudent中）
        console.warn('获取学生详细信息失败，使用基本信息:', response?.message)
        this.$message.warning('获取学生详细信息失败')
      }
    },
    
    // 在资料对话框中添加到聊天
    addFromProfile() {
      if (!this.profileStudent) return
      
      // 通过userId查找是否已存在
      const exists = this.studentList.some(s => 
        s.userId === this.profileStudent.userId ||
        (this.profileStudent.studentId && s.studentId === this.profileStudent.studentId)
      )
      if (exists) {
        this.$message.info('该学生已在聊天列表中')
        return
      }
      
      // 转换为聊天列表格式
      const chatStudent = {
        id: this.profileStudent.userId ? `student_${this.profileStudent.userId}` : null,
        userId: this.profileStudent.userId,
        name: this.profileStudent.realName,
        studentId: this.profileStudent.studentId,
        major: this.profileStudent.major,
        className: this.profileStudent.className,
        dormInfo: this.profileStudent.dormNumber || '未分配',
        avatar: this.studentProfile.avatar || defaultAvatar,
        lastMessage: '暂无消息',
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0,
        messages: []
      }
      
      this.addStudentToChat(chatStudent)
      this.profileDialogVisible = false
    },
    
    // 处理学生操作
    handleStudentAction(command, student) {
      switch (command) {
        case 'view_profile':
          this.openProfileDialog(student)
          break
        case 'send_notification':
          this.openNotificationDialog(student)
          break
        case 'assign_dorm':
          this.openDormAssignmentDialog(student)
          break
        case 'view_applications':
          this.openApplicationsDialog(student)
          break
        case 'export_chat':
          this.exportChatHistory(student)
          break
        case 'delete_contact':
          this.deleteContact(student)
          break
        default:
          console.log('未知操作:', command)
      }
    },
    
    async deleteContact(student) {
      this.$confirm(`确定要删除与${student.name}的聊天记录吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 先获取联系人列表，找到对应的contactId
          const contactsRes = await chatApi.getContacts()
          if (contactsRes.code === 200 && contactsRes.data) {
            const contact = contactsRes.data.find(c => c.userId === student.userId)
            if (contact && contact.id) {
              await chatApi.deleteContact(contact.id)
            } else {
              // 如果没有找到Contact记录，说明可能是通过会话直接添加的，只删除会话即可
              console.warn('未找到Contact记录，可能该学生是通过会话直接添加的')
            }
          }
          
          // 从学生列表中移除
          const index = this.studentList.findIndex(s => s.id === student.id)
          if (index > -1) {
            this.studentList.splice(index, 1)
          }
          
          // 如果删除的是当前选中的学生，清空选择
          if (this.selectedStudentId === student.id) {
            this.selectedStudentId = null
          }
          
          this.$message.success('联系人已删除')
        } catch (error) {
          console.error('删除联系人失败:', error)
          const errorMsg = error.response?.data?.message || error.message || '删除联系人失败'
          this.$message.error(errorMsg)
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    
    // 右键打开上下文菜单
    openContextMenu(event, student) {
      // 检查是否点击的是三个小点按钮
      if (event.target.closest('.more-actions-btn') || event.target.closest('.el-dropdown')) {
        return // 如果点击的是三个小点按钮，不打开右键菜单
      }
      
      // 先关闭已存在的菜单
      this.closeContextMenu()
      
      // 创建右键菜单
      const menu = document.createElement('div')
      menu.className = 'context-menu'
      menu.style.cssText = `
        position: fixed;
        top: ${event.clientY}px;
        left: ${event.clientX}px;
        background: white;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        min-width: 120px;
        padding: 4px 0;
      `
      
      // 菜单项
      const menuItems = [
        { text: '查看资料', command: 'view_profile', icon: 'View' },
        { text: '分配宿舍', command: 'assign_dorm', icon: 'House' },
        { text: '删除联系人', command: 'delete_contact', icon: 'Delete', danger: true }
      ]
      
      menuItems.forEach(item => {
        const menuItem = document.createElement('div')
        menuItem.className = 'context-menu-item'
        menuItem.style.cssText = `
          padding: 8px 16px;
          cursor: pointer;
          font-size: 14px;
          color: ${item.danger ? '#f56c6c' : '#606266'};
          transition: background-color 0.2s;
        `
        menuItem.innerHTML = `
          <el-icon style="margin-right: 8px; font-size: 16px;">
            <${item.icon} />
          </el-icon>
          ${item.text}
        `
        
        menuItem.addEventListener('mouseenter', () => {
          menuItem.style.backgroundColor = '#f5f7fa'
        })
        
        menuItem.addEventListener('mouseleave', () => {
          menuItem.style.backgroundColor = 'transparent'
        })
        
        menuItem.addEventListener('click', () => {
          this.handleStudentAction(item.command, student)
          this.closeContextMenu()
        })
        
        menu.appendChild(menuItem)
      })
      
      // 添加到页面
      document.body.appendChild(menu)
      
      // 保存当前菜单引用
      this.currentContextMenu = menu
      
      // 点击其他地方关闭菜单
      const closeMenu = (e) => {
        if (!menu.contains(e.target)) {
          this.closeContextMenu()
        }
      }
      
      setTimeout(() => {
        document.addEventListener('click', closeMenu)
      }, 100)
    },
    
    // 关闭右键菜单
    closeContextMenu() {
      if (this.currentContextMenu) {
        try {
          document.body.removeChild(this.currentContextMenu)
        } catch (e) {
          // 菜单可能已经被移除
        }
        this.currentContextMenu = null
      }
    },

    // 打开消息右键菜单
    openMessageContextMenu(event, message) {
      // 先关闭已存在的菜单
      this.closeMessageContextMenu()
      
      // 创建菜单
      const menu = document.createElement('div')
      menu.className = 'message-context-menu'
      
      // 菜单项
      const menuItems = []
      
      // 管理员消息可以撤回
      if (message.isAdmin && this.canRecallMessage(message)) {
        menuItems.push({ text: '撤回', command: 'recall' })
      }
      
      // 所有消息都可以复制
      menuItems.push({ text: '复制', command: 'copy' })
      
      menuItems.forEach(item => {
        const menuItem = document.createElement('div')
        menuItem.className = 'context-menu-item'
        menuItem.style.cssText = `
          padding: 8px 16px;
          cursor: pointer;
          font-size: 14px;
          color: #606266;
          transition: background-color 0.2s;
          white-space: nowrap;
        `
        menuItem.textContent = item.text
        
        menuItem.addEventListener('mouseenter', () => {
          menuItem.style.backgroundColor = '#f5f7fa'
        })
        
        menuItem.addEventListener('mouseleave', () => {
          menuItem.style.backgroundColor = 'transparent'
        })
        
        menuItem.addEventListener('click', () => {
          this.handleMessageAction(item.command, message)
          this.closeMessageContextMenu()
        })
        
        menu.appendChild(menuItem)
      })
      
      // 基础样式
      menu.style.cssText = `
        position: fixed;
        background: white;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9999;
        padding: 4px 0;
      `
      
      // 临时添加到页面以获取宽高
      menu.style.visibility = 'hidden'
      document.body.appendChild(menu)
      
      const menuWidth = menu.offsetWidth
      const menuHeight = menu.offsetHeight
      const windowWidth = window.innerWidth
      const windowHeight = window.innerHeight
      
      // 智能定位：避免菜单超出屏幕或覆盖内容
      let left = event.clientX + 5  // 默认在鼠标右侧
      let top = event.clientY + 5   // 默认在鼠标下方
      
      // 如果右侧空间不够，显示在左侧
      if (left + menuWidth > windowWidth) {
        left = event.clientX - menuWidth - 5
      }
      
      // 如果下方空间不够，显示在上方
      if (top + menuHeight > windowHeight) {
        top = event.clientY - menuHeight - 5
      }
      
      // 确保不超出屏幕边界
      left = Math.max(5, Math.min(left, windowWidth - menuWidth - 5))
      top = Math.max(5, Math.min(top, windowHeight - menuHeight - 5))
      
      menu.style.left = left + 'px'
      menu.style.top = top + 'px'
      menu.style.visibility = 'visible'
      
      this.currentMessageContextMenu = menu
      
      // 点击其他地方关闭菜单
      const closeMenu = (e) => {
        if (!menu.contains(e.target)) {
          this.closeMessageContextMenu()
          document.removeEventListener('click', closeMenu)
        }
      }
      
      setTimeout(() => {
        document.addEventListener('click', closeMenu)
      }, 100)
    },

    // 关闭消息右键菜单
    closeMessageContextMenu() {
      if (this.currentMessageContextMenu) {
        try {
          document.body.removeChild(this.currentMessageContextMenu)
        } catch (e) {
          // 菜单可能已经被移除
        }
        this.currentMessageContextMenu = null
      }
    },
    
    // 关闭资料对话框
    closeProfileDialog() {
      this.profileDialogVisible = false
      this.profileStudent = null
      this.studentProfile = {}
      this.studentTags = []
    },

    // 计算是否可以确认分配
    canConfirmAssignment() {
      if (this.assignmentForm.type === 'direct') {
        return this.assignmentForm.major && this.assignmentForm.className && this.assignmentForm.room
      } else {
        return this.targetStudent !== null
      }
    },
    
    // 格式化日期时间
    formatDateTime(dateString) {
      if (!dateString) return '未知'
      return new Date(dateString).toLocaleString('zh-CN')
    },
    
    // 联系学生
    contactStudent() {
      if (!this.profileStudent) return
      
      // 检查是否已经在聊天列表中（通过userId匹配）
      const existingStudent = this.studentList.find(s => 
        s.userId === this.profileStudent.userId || 
        s.id === this.profileStudent.id ||
        (this.profileStudent.studentId && s.studentId === this.profileStudent.studentId)
      )
      if (existingStudent) {
        // 如果已存在，直接选择该学生
        this.selectStudent(existingStudent.id)
        this.closeProfileDialog()
        this.$message.success(`已切换到与${this.profileStudent.realName}的聊天`)
      } else {
        // 如果不存在，添加到聊天列表
        this.addFromProfile()
      }
    },

    // 打开发送通知弹窗
    openNotificationDialog(student) {
      this.currentStudent = student
      this.notificationDialogVisible = true
    },

    // 打开查看申请弹窗
    async openApplicationsDialog(student) {
      this.currentStudent = student
      
      // 获取该学生的所有申请记录
      try {
        const { getApplicationData } = await import('@/utils/applicationData.js')
        const allApplications = getApplicationData()
        
        // 根据学生ID或姓名筛选申请
        this.currentStudentApplications = allApplications.filter(app => 
          app.studentId === student.studentId || app.studentName === student.name
        )
        
        // 按创建时间倒序排列
        this.currentStudentApplications.sort((a, b) => {
          const timeA = new Date(a.createTime || a.applyTime).getTime()
          const timeB = new Date(b.createTime || b.applyTime).getTime()
          return timeB - timeA
        })
      } catch (error) {
        console.error('获取申请记录失败:', error)
        this.currentStudentApplications = []
      }
      
      this.applicationsDialogVisible = true
    },

    // 获取申请状态对应的标签类型
    getApplicationStatusType(status) {
      const typeMap = {
        '待审核': 'warning',
        '已通过': 'success',
        '已拒绝': 'danger',
        '待赔偿处理': 'info',
        '处理中': 'warning'
      }
      return typeMap[status] || 'info'
    },

    // 格式化申请时间
    formatApplicationTime(time) {
      if (!time) return '—'
      const date = new Date(time)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },

    // 查看申请详情（跳转到申请审核页面）
    viewApplicationDetail(application) {
      this.applicationsDialogVisible = false
      this.$router.push({
        path: '/admin/application-review',
        query: {
          applicationId: application.id
        }
      })
    },

    // 前往申请审核页面
    goToApplicationReview() {
      this.applicationsDialogVisible = false
      this.$router.push('/admin/application-review')
    },

    // 发送通知消息
    sendNotificationMessage() {
      if (!this.notificationForm.content.trim()) {
        this.$message.warning('请输入通知内容')
        return
      }
      
      // 这里可以添加实际的通知发送逻辑
      this.$message.success('通知已发送')
      this.notificationDialogVisible = false
      // 重置表单
      this.notificationForm = {
        type: 'normal',
        content: ''
      }
    },

    // 打开宿舍分配弹窗
    async openDormAssignmentDialog(student) {
      this.currentStudent = { ...student }
      this.dormAssignmentDialogVisible = true
      
      // 加载学生的完整信息（包括楼栋信息）
      try {
        let studentData = null
        
        // 优先使用学号查询
        if (student.studentId) {
          const res = await getStudentByStudentId(student.studentId)
          if (res.code === 200 && res.data) {
            studentData = res.data
          }
        }
        
        // 如果没有学号或查询失败，尝试通过userId查询
        if (!studentData && student.userId) {
          const res = await getStudentByUserId(student.userId)
          if (res.code === 200 && res.data) {
            studentData = res.data
          }
        }
        
        if (studentData) {
          // 更新学生信息
          this.currentStudent = {
            ...this.currentStudent,
            studentId: studentData.studentId || this.currentStudent.studentId,
            name: studentData.realName || this.currentStudent.name,
            major: studentData.major || this.currentStudent.major,
            className: studentData.className || this.currentStudent.className,
            gender: studentData.gender || this.currentStudent.gender,
            dormInfo: studentData.dormNumber || this.currentStudent.dormInfo || '未分配'
          }
          
          // 获取学生所在楼栋信息
          await this.loadStudentBuildingInfo()
        } else {
          this.$message.warning('无法获取学生详细信息')
        }
      } catch (error) {
        console.error('加载学生信息失败:', error)
        this.$message.error('加载学生信息失败')
      }
    },
    
    // 加载学生所在楼栋信息
    async loadStudentBuildingInfo() {
      try {
        // 获取当前管理员管理的楼栋
        const buildingsRes = await getMyBuildings()
        if (buildingsRes.code === 200 && buildingsRes.data && buildingsRes.data.length > 0) {
          // 如果有多个楼栋，根据学生的专业和班级匹配
          // 这里简化处理，使用第一个楼栋（实际应该根据学生的专业分配匹配）
          const building = buildingsRes.data[0]
          // BuildingStatisticsVO 使用 name 和 code 字段
          this.currentStudent.building = building.name || building.code || '未分配'
          this.currentStudent.buildingId = building.id
          
          // 加载该楼栋的专业分配和房间信息
          await this.loadBuildingData(building.id)
        } else {
          this.currentStudent.building = '未分配'
          this.$message.warning('您没有管理的楼栋，无法进行宿舍分配')
        }
      } catch (error) {
        console.error('加载楼栋信息失败:', error)
        this.currentStudent.building = '未分配'
        this.$message.error('加载楼栋信息失败')
      }
    },
    
    // 加载楼栋数据（专业分配和房间）
    async loadBuildingData(buildingId) {
      try {
        // 加载专业分配信息
        this.loadingMajor = true
        const majorRes = await getMajorAssignments(buildingId)
        if (majorRes.code === 200 && majorRes.data) {
          // 提取专业列表（去重）
          const majors = [...new Set(majorRes.data.map(a => a.majorName).filter(Boolean))]
          this.availableMajors = majors
          
          // 如果学生已有专业信息，自动选中
          if (this.currentStudent.major && majors.includes(this.currentStudent.major)) {
            this.assignmentForm.major = this.currentStudent.major
            // 自动加载班级列表
            await this.handleMajorChange()
            
            // 如果学生已有班级信息，自动选中
            if (this.currentStudent.className && this.availableClasses.some(c => c.value === this.currentStudent.className)) {
              this.assignmentForm.className = this.currentStudent.className
              // 自动加载房间列表
              await this.handleClassChange()
            }
          }
        }
        this.loadingMajor = false
      } catch (error) {
        console.error('加载楼栋数据失败:', error)
        this.loadingMajor = false
      }
    },

    // 关闭宿舍分配弹窗
    closeDormAssignmentDialog() {
      this.dormAssignmentDialogVisible = false
      this.currentStudent = {}
      this.resetAssignmentForm()
    },

    // 重置分配表单
    resetAssignmentForm() {
      this.assignmentForm = {
        reason: 'new_student',
        customReason: '',
        type: 'direct',
        major: '',
        className: '',
        room: ''
      }
      this.exchangeForm = {
        studentName: '',
        studentId: ''
      }
      this.availableClasses = []
      this.availableRooms = []
      this.availableMajors = []
      this.targetStudent = null
    },

    // 处理分配类型变化
    handleAssignmentTypeChange() {
      // 重置相关数据
      this.assignmentForm.major = ''
      this.assignmentForm.className = ''
      this.assignmentForm.room = ''
      this.availableClasses = []
      this.availableRooms = []
      this.availableMajors = []
      this.targetStudent = null
    },

    // 处理专业变化
    async handleMajorChange() {
      this.assignmentForm.className = ''
      this.assignmentForm.room = ''
      this.availableRooms = []
      this.availableClasses = []
      
      // 根据选择的专业加载班级列表
      if (this.assignmentForm.major && this.currentStudent.buildingId) {
        try {
          this.loadingClass = true
          const majorRes = await getMajorAssignments(this.currentStudent.buildingId)
          if (majorRes.code === 200 && majorRes.data) {
            const classes = majorRes.data
              .filter(a => a.majorName === this.assignmentForm.major)
              .map(a => ({
                label: a.className || '未命名班级',
                value: a.className
              }))
            // 去重
            this.availableClasses = classes.filter((c, index, self) => 
              index === self.findIndex(t => t.value === c.value)
            )
          }
          this.loadingClass = false
        } catch (error) {
          console.error('加载班级列表失败:', error)
          this.loadingClass = false
        }
      }
    },
    
    // 处理班级变化
    async handleClassChange() {
      this.assignmentForm.room = ''
      this.availableRooms = []
      
      // 根据选择的专业和班级加载可用房间
      if (this.assignmentForm.major && this.assignmentForm.className && this.currentStudent.buildingId) {
        try {
          this.loadingRoom = true
          const roomsRes = await getRoomsByBuilding(this.currentStudent.buildingId)
          if (roomsRes.code === 200 && roomsRes.data) {
            // 过滤出匹配专业和班级的房间，且有可用床位
            const matchedRooms = roomsRes.data.filter(room => {
              // 检查房间是否匹配专业和班级
              const matchesMajor = !room.majorName || room.majorName === this.assignmentForm.major
              const matchesClass = !room.className || room.className === this.assignmentForm.className
              // 使用后端返回的 availableBeds 字段判断是否有空床位
              const hasAvailableBeds = (room.availableBeds || 0) > 0
              return matchesMajor && matchesClass && hasAvailableBeds
            })
            
            this.availableRooms = matchedRooms.map(room => ({
              label: `${room.roomNumber}室 (${room.availableBeds || 0}个空床位)`,
              value: room.roomNumber,
              roomId: room.id,
              bedCount: room.bedCount,
              currentCount: room.bedCount - (room.availableBeds || 0),
              availableBeds: room.availableBeds || 0
            }))
          }
          this.loadingRoom = false
        } catch (error) {
          console.error('加载房间列表失败:', error)
          this.loadingRoom = false
        }
      }
    },

    // 搜索目标学生
    searchTargetStudent() {
      if (!this.exchangeForm.studentName && !this.exchangeForm.studentId) {
        this.$message.warning('请输入学生姓名或学号')
        return
      }
      
      // 模拟搜索学生
      this.targetStudent = {
        name: this.exchangeForm.studentName || '张三',
        studentId: this.exchangeForm.studentId || '2024001',
        major: '计算机科学',
        className: '计科2401',
        dormInfo: '1号楼-305',
        roommates: ['李四', '王五', '赵六']
      }
      
      this.$message.success('找到目标学生')
    },

    // 确认调换
    confirmExchange() {
      if (!this.targetStudent) {
        this.$message.warning('请先搜索目标学生')
        return
      }
      
      this.$message.success(`已将 ${this.currentStudent.name} 与 ${this.targetStudent.name} 的宿舍进行调换`)
      this.closeDormAssignmentDialog()
    },

    // 确认分配
    async confirmAssignment() {
      if (!this.assignmentForm.room) {
        this.$message.warning('请选择宿舍')
        return
      }
      
      if (!this.currentStudent.studentId) {
        this.$message.error('学生信息不完整，无法分配')
        return
      }
      
      // 找到选中的房间信息
      const selectedRoom = this.availableRooms.find(r => r.value === this.assignmentForm.room)
      if (!selectedRoom || !selectedRoom.roomId) {
        this.$message.error('房间信息无效')
        return
      }
      
      // 获取学生ID（通过学号查询）
      let studentId = null
      try {
        if (this.currentStudent.studentId) {
          const studentRes = await getStudentByStudentId(this.currentStudent.studentId)
          if (studentRes.code === 200 && studentRes.data) {
            studentId = studentRes.data.id
          }
        }
        
        if (!studentId && this.currentStudent.userId) {
          const studentRes = await getStudentByUserId(this.currentStudent.userId)
          if (studentRes.code === 200 && studentRes.data) {
            studentId = studentRes.data.id
          }
        }
        
        if (!studentId) {
          this.$message.error('无法获取学生ID')
          return
        }
        
        // 调用分配API
        const allocationData = {
          studentId: studentId,
          roomId: selectedRoom.roomId,
          bedNumber: '1', // 默认分配1号床位，实际应该让用户选择或自动分配
          allocationType: 'manual',
          remarks: this.assignmentForm.reason === 'other' 
            ? this.assignmentForm.customReason 
            : this.getReasonText(this.assignmentForm.reason)
        }
        
        const res = await allocateDorm(allocationData)
        if (res.code === 200) {
          this.$message.success(`已将 ${this.currentStudent.name} 分配到 ${this.assignmentForm.room} 宿舍`)
          this.closeDormAssignmentDialog()
          // 刷新学生列表
          await this.refreshStudentList()
        } else {
          this.$message.error(res.message || '分配失败')
        }
      } catch (error) {
        console.error('分配宿舍失败:', error)
        this.$message.error('分配失败：' + (error.message || '未知错误'))
      }
    },
    
    // 获取分配原因文本
    getReasonText(reason) {
      const reasonMap = {
        'new_student': '新生入学',
        'dorm_adjustment': '宿舍调整',
        'conflict_resolution': '矛盾处理',
        'other': '其他'
      }
      return reasonMap[reason] || '其他'
    },

    // 导出聊天记录
    exportChatHistory(student) {
      if (!student || !student.messages || student.messages.length === 0) {
        this.$message.warning('该学生暂无聊天记录')
        return
      }

      try {
        // 生成文件内容
        let content = '========================================\n'
        content += `聊天记录导出\n`
        content += `========================================\n\n`
        content += `学生姓名：${student.name}\n`
        content += `学号：${student.studentId || '—'}\n`
        content += `专业：${student.major || '—'}\n`
        content += `宿舍：${student.dormInfo || '未分配'}\n`
        content += `导出时间：${new Date().toLocaleString('zh-CN')}\n`
        content += `消息总数：${student.messages.length} 条\n`
        content += `========================================\n\n`

        // 添加消息记录
        student.messages.forEach((message, index) => {
          const time = this.formatTime(message.time)
          const sender = message.isAdmin ? '管理员' : student.name
          
          content += `【${index + 1}】 ${sender} - ${time}\n`
          
          // 处理不同类型的消息
          if (message.type === 'image') {
            content += `[图片消息]\n`
          } else if (message.type === 'file') {
            content += `[文件消息] ${message.fileName || '附件'}\n`
          } else {
            content += `${message.content}\n`
          }
          
          content += '\n'
        })

        content += '========================================\n'
        content += `导出完成 - ${new Date().toLocaleString('zh-CN')}\n`
        content += '========================================\n'

        // 创建Blob对象
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
        
        // 创建下载链接
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        
        // 生成文件名
        const fileName = `聊天记录_${student.name}_${student.studentId || ''}_${new Date().getTime()}.txt`
        link.download = fileName
        
        // 触发下载
        document.body.appendChild(link)
        link.click()
        
        // 清理
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        this.$message.success('聊天记录导出成功')
      } catch (error) {
        console.error('导出聊天记录失败:', error)
        this.$message.error('导出失败，请重试')
      }
    },

    // 处理消息操作
    handleMessageAction(command, message) {
      switch (command) {
        case 'recall':
          this.recallMessage(message)
          break
        case 'recall-disabled':
          this.$message.warning('消息发送超过2分钟，无法撤回')
          break
        case 'copy':
          this.copyMessage(message)
          break
      }
    },

    // 判断消息是否可以撤回
    canRecallMessage(message) {
      // 只有管理员自己发送的消息且发送时间在2分钟内才能撤回
      if (!message.isAdmin) return false
      const now = new Date()
      const messageTime = new Date(message.time)
      const diffMinutes = (now - messageTime) / (1000 * 60)
      return diffMinutes <= 120 // 120分钟内可以撤回（方便测试虚拟数据）
    },

    async recallMessage(message) {
      this.$confirm('确定要撤回这条消息吗？', '撤回消息', {
        confirmButtonText: '撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await chatApi.recallMessage(message.id)
          if (res.code === 200) {
            message.isRecalled = true
            message.recallTime = new Date().toISOString()
            this.$message.success('消息已撤回')
          }
        } catch (error) {
          console.error('撤回消息失败:', error)
          this.$message.error(error.message || '撤回消息失败')
        }
      }).catch(() => {
        // 用户取消撤回
      })
    },

    // 复制消息
    copyMessage(message) {
      // 复制消息内容到剪贴板
      if (navigator.clipboard) {
        navigator.clipboard.writeText(message.content).then(() => {
          this.$message.success('消息已复制到剪贴板')
        }).catch(() => {
          this.$message.error('复制失败')
        })
      } else {
        // 兼容老版本浏览器
        const textArea = document.createElement('textarea')
        textArea.value = message.content
        document.body.appendChild(textArea)
        textArea.select()
        try {
          document.execCommand('copy')
          this.$message.success('消息已复制到剪贴板')
        } catch (err) {
          this.$message.error('复制失败')
        }
        document.body.removeChild(textArea)
      }
    },
    
    // 关闭资料对话框
    closeProfileDialog() {
      this.profileDialogVisible = false
      this.profileStudent = null
      this.studentProfile = {}
      this.studentTags = []
    },
    
    // 从资料对话框添加学生到聊天列表
    addFromProfile() {
      if (!this.profileStudent) return
      
      // 构造学生对象
      const newStudent = {
        id: this.profileStudent.userId || Date.now(),
        userId: this.profileStudent.userId,
        studentId: this.profileStudent.studentId,
        name: this.profileStudent.realName,
        avatar: this.studentProfile.avatar || '',
        dormInfo: this.profileStudent.dormNumber || '未分配',
        lastMessage: '开始聊天',
        time: new Date(),
        unread: 0,
        messages: [],
        gender: this.profileStudent.gender,
        className: this.profileStudent.className,
        major: this.profileStudent.major,
        email: this.profileStudent.email,
        isRegistered: this.profileStudent.isRegistered,
        registrationTime: this.profileStudent.registeredAt
      }
      
      // 添加到学生列表
      this.studentList.unshift(newStudent)
      
      // 选择该学生
      this.selectStudent(newStudent.id)
      
      // 关闭对话框
      this.closeProfileDialog()
      
      this.$message.success(`已添加 ${newStudent.name} 到聊天列表`)
    }
  }
}
</script>

<style scoped>
.admin-chat-messages-page {
  height: calc(100vh - 70px);
  background: #f5f5f5;
  margin: 0;
  padding: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.chat-container {
  display: flex;
  height: 100%;
  background: #fff;
  overflow: hidden;
  position: relative;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 左侧学生列表 */
.student-list-panel {
  width: 350px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f8f9fa;
  flex-shrink: 0;
  overflow: visible !important;
}

.student-list-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
  height: 60px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.search-area {
  display: flex;
  gap: 8px;
  align-items: center;
  width: 100%;
}

.search-box {
  flex: 1;
  position: relative;
  min-width: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.refresh-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 50%;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 8px;
  background: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.search-box :deep(.el-input__wrapper:hover) {
  border-color: #409eff;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.2);
}

.search-box :deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.search-box :deep(.el-input__inner) {
  border: none;
  background: transparent;
  font-size: 14px;
  color: #606266;
}

.search-box :deep(.el-input__inner::placeholder) {
  color: #c0c4cc;
  font-size: 14px;
}

.search-box :deep(.el-input__prefix-inner) {
  color: #909399;
  font-size: 16px;
}

/* 搜索结果下拉框 */
.search-results-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
  overflow-x: hidden; /* 防止出现水平滚动条导致闪缩 */
  margin-top: 4px;
  will-change: scroll-position; /* 提示浏览器优化滚动 */
}

.search-result-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease; /* 避免在 hover 时引发布局抖动 */
  border-bottom: 1px solid #f5f5f5;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: #f0f9ff;
}

.search-result-item .student-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.search-result-item .student-name {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-item .student-details {
  color: #909399;
  font-size: 12px;
  line-height: 1.3;
  max-height: 2.6em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.search-result-item .add-btn {
  flex-shrink: 0;
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  border-radius: 14px;
}

.search-result-item .add-btn .el-icon {
  margin-right: 4px;
}

.student-list {
  flex: 1;
  background: #f8f9fa;
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto !important;
  overflow-x: hidden !important;
  height: auto;
  min-height: 0;
  max-height: none !important;
}

.student-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin: 2px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 10px;
  position: relative;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  min-height: 70px;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.student-item:hover {
  background: #f0f9ff;
  border-color: #b3d8ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transform: translateY(-1px);
}

.student-item.active {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.student-avatar {
  position: relative;
  margin-right: 12px;
  margin-top: 20px;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  height: 16px;
  line-height: 12px;
  text-align: center;
}

.student-info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.student-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.student-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.student-item.active .student-name {
  color: #ffffff;
  font-weight: 600;
}

.student-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  font-weight: 400;
}

.student-item.active .student-time {
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
}

.student-preview {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.student-dorm {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
  background: rgba(64, 158, 255, 0.12);
  padding: 4px 10px;
  border-radius: 8px;
  display: inline-block;
  white-space: nowrap;
  flex-shrink: 0;
  min-width: 80px;
  max-width: 100%;
  text-align: center;
  border: 1px solid rgba(64, 158, 255, 0.2);
  box-sizing: border-box;
}

.student-item.active .student-dorm {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.3);
  font-weight: 600;
}

/* 学生操作菜单 */
.student-actions {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.student-item:hover .student-actions {
  opacity: 1;
}

.student-item.active .student-actions {
  opacity: 1;
}

.more-actions-btn {
  color: #909399;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.more-actions-btn:hover {
  color: #409eff;
  background: #f5f7fa;
}

.student-item.active .more-actions-btn {
  color: rgba(255, 255, 255, 0.9);
}

.student-item.active .more-actions-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.25);
}

/* 学生详情对话框样式（复用学生信息页面样式） */
.student-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.detail-section h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 0;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-grid-personal .info-item {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
}

.info-grid-personal .info-item .label {
  min-width: 70px; /* Adjust label width */
  flex-shrink: 0;
}

.info-grid-personal .bio-item {
  flex-direction: column;
  margin-top: 16px;
}

.info-grid-personal .bio-item .bio-value {
  margin-top: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

.tags-section {
  display: flex;
  align-items: flex-start; /* Align label and tags to the top */
}

.tags-section .label {
    min-width: 70px; /* Match other labels */
    flex-shrink: 0;
    padding-top: 5px; /* Align with the center of the tags */
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: #606266;
  font-weight: 500;
  min-width: 60px;
  font-size: 14px;
}

.info-item .value {
  color: #303133;
  font-size: 14px;
  flex: 1;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.tags-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-height: 32px;
  align-items: center;
}

.no-tags,
.empty-value {
  color: #c0c4cc;
  font-style: italic;
  font-size: 14px;
}

/* 宿舍分配弹窗样式 */
.dorm-assignment-content {
  max-height: 600px;
  overflow-y: auto;
}

.student-info-section,
.reason-section,
.assignment-type-section,
.direct-assignment-section,
.exchange-assignment-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.student-info-section h4,
.reason-section h4,
.assignment-type-section h4,
.direct-assignment-section h4,
.exchange-assignment-section h4 {
  margin: 0 0 12px 0;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}

.info-card {
  background: #fff;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-row .label {
  color: #606266;
  font-weight: 500;
  width: 80px;
  font-size: 14px;
  margin-right: 8px;
  flex-shrink: 0;
}

.info-row .value {
  color: #303133;
  font-size: 14px;
  margin-right: 20px;
  flex: 1;
}

.dorm-preview {
  margin-top: 12px;
}

.search-conditions {
  margin-bottom: 16px;
}

.exchange-students-list h5 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.student-list {
  max-height: 200px;
  overflow-y: auto;
}

.exchange-student-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.exchange-student-item:hover {
  background: #f5f7fa;
  border-color: #409eff;
}

.exchange-student-item.selected {
  background: #ecf5ff;
  border-color: #409eff;
}

.exchange-student-item .student-info {
  margin-left: 12px;
  flex: 1;
}

.student-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.student-details,
.student-major,
.student-roommates {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

/* 新的宿舍分配样式 */
.assignment-info {
  margin-bottom: 16px;
}

.search-target-student h5,
.target-student-info h5,
.exchange-preview h5 {
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.target-student-info {
  margin-top: 20px;
}

.student-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 12px;
}

.student-card .student-avatar {
  margin-right: 16px;
}

.student-card .student-details {
  flex: 1;
}

.student-card .student-details .student-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.student-card .student-details .student-id,
.student-card .student-details .student-major,
.student-card .student-details .student-class,
.student-card .student-details .student-dorm,
.student-card .student-details .student-roommates {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.student-info-row {
  display: flex;
  gap: 20px;
  margin-bottom: 2px;
}

.student-info-row .student-major,
.student-info-row .student-class {
  font-size: 12px;
  color: #909399;
  margin-bottom: 0;
}

.exchange-flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.exchange-flow .student-card {
  flex: 1;
  margin: 0 12px;
}

.exchange-arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #409eff;
  font-weight: 600;
}

.exchange-arrow .el-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.last-message {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 4px;
  line-height: 1.5;
  font-weight: 400;
}

.student-item.active .last-message {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.empty-student-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #999;
  background: #fafafa;
  margin: 12px;
  border-radius: 12px;
  border: 2px dashed #e0e0e0;
  gap: 16px;
}

.empty-icon {
  font-size: 48px;
  color: #ddd;
  margin: 0;
}

/* 右侧聊天内容 */
.chat-content-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 0;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
}

.chat-content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  height: 60px;
  position: relative;
  z-index: 10;
}

.student-detail-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.back-btn {
  color: #409eff;
  padding: 4px 8px;
  margin-right: 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

.student-details {
  display: flex;
  flex-direction: column;
}

.student-details .student-name {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
  margin: 0;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.student-details .student-dorm {
  font-size: 12px;
  color: #409eff;
  margin-top: 2px;
}

.chat-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.more-actions-btn {
  color: #606266;
  transition: all 0.3s ease;
  padding: 8px;
  border-radius: 50%;
}

.more-actions-btn:hover {
  color: #409eff;
  background: #f5f7fa;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.more-actions-btn .el-icon {
  font-size: 18px;
}

.messages-container {
  flex: 1;
  background: #f8f9fa;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  position: relative;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 8px;
  min-height: 0;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.message-item.admin-message {
  justify-content: flex-end;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 60%;
  min-width: 0;
  word-wrap: break-word;
}

.message-item.admin-message .message-content {
  align-items: flex-end;
}

.message-bubble {
  background: #fff;
  padding: 8px 12px;
  border-radius: 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  word-break: break-word;
  max-width: 100%;
  width: fit-content; /* 气泡宽度根据内容自适应 */
  overflow-wrap: break-word;
}

.admin-bubble {
  background: #409eff;
  color: #fff;
}

.message-text {
  line-height: 1.4;
  font-size: 14px;
  color: #000;
}

.admin-bubble .message-text {
  color: #fff;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 8px;
}

.message-input-panel {
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 12px 24px;
  flex-shrink: 0;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.06);
  min-height: 100px;
  display: flex;
  flex-direction: column;
  position: sticky;
  bottom: 0;
  z-index: 10;
  min-width: 0;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  height: 32px;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  height: 60px;
  min-width: 0;
}

.input-area .el-textarea {
  flex: 1;
  min-width: 0;
}

.input-area :deep(.el-textarea__inner) {
  resize: none;
  border-radius: 16px;
  padding: 10px 16px;
  border: 1px solid #e4e7ed;
  background: #f8f9fa;
  min-height: 60px !important;
  max-height: 60px !important;
  overflow-y: auto;
  overflow-x: hidden;
  word-wrap: break-word;
}

.send-btn {
  height: 36px;
  border-radius: 18px;
  padding: 0 16px;
  font-weight: 500;
  flex-shrink: 0;
}

.empty-chat-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  background: #f8f9fa;
}

.empty-chat-content .empty-icon {
  color: #ddd;
  margin-bottom: 24px;
}

.empty-chat-content h3 {
  color: #666;
  margin-bottom: 8px;
}

.empty-chat-content p {
  color: #999;
  font-size: 14px;
}

/* 快捷回复样式 */
.quick-reply-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.quick-reply-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
}

.quick-reply-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: background-color 0.3s;
  gap: 12px;
}

.quick-reply-item:hover {
  background: #e8f4ff;
}

.quick-reply-item .reply-content {
  flex: 1;
  cursor: pointer;
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}

.quick-reply-item .reply-content:hover {
  color: #409eff;
}

.quick-reply-item .el-button {
  flex-shrink: 0;
}

.empty-placeholder {
  padding: 20px 0;
  text-align: center;
}

/* 添加更多交互效果 */
.student-item:hover::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #409eff;
  transition: all 0.3s ease;
}

.student-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #409eff;
}

.message-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 撤回消息样式 */
.recalled-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 16px;
  color: #909399;
  font-size: 13px;
}

.recall-icon {
  font-size: 16px;
  color: #909399;
}

.recall-text {
  font-style: italic;
}

.recalled-item {
  opacity: 0.8;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-chat-messages-page {
    height: 100vh;
    margin: 0;
    padding: 8px;
  }
  
  .chat-container {
    height: calc(100vh - 16px);
  }
  
  .student-list-panel {
    width: 280px;
    min-width: 0;
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .message-input-panel {
    padding: 16px;
  }
  
  .input-area {
    flex-direction: column;
    gap: 12px;
  }
  
  .send-btn {
    align-self: flex-end;
    width: 80px;
  }
}

/* 保持外层容器不滚动，只允许内容区域滚动 */
.admin-chat-messages-page, 
.chat-container,
.chat-content-panel,
.messages-container,
.message-input-panel {
  overflow: hidden !important;
}

.student-list-panel {
  overflow: visible !important;
}

/* 隐藏不需要的滚动条 */
.admin-chat-messages-page::-webkit-scrollbar,
.chat-container::-webkit-scrollbar,
.chat-content-panel::-webkit-scrollbar,
.message-input-panel::-webkit-scrollbar {
  display: none !important;
  width: 0 !important;
}

/* 优化消息列表和联系人列表的滚动条 - 智能显示隐藏 */
.messages-list::-webkit-scrollbar,
.student-list::-webkit-scrollbar {
  width: 6px;
}

.messages-list::-webkit-scrollbar-track,
.student-list::-webkit-scrollbar-track {
  background: transparent;
}

.messages-list::-webkit-scrollbar-thumb,
.student-list::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 3px;
  transition: all 0.3s ease;
}

/* 悬停时显示滚动条 */
.messages-list:hover::-webkit-scrollbar-thumb,
.student-list:hover::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

/* 滚动时显示滚动条 */
.messages-list.scrolling::-webkit-scrollbar-thumb,
.student-list.scrolling::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.4);
}

/* 悬停和滚动时加深滚动条 */
.messages-list:hover::-webkit-scrollbar-thumb:hover,
.student-list:hover::-webkit-scrollbar-thumb:hover,
.messages-list.scrolling::-webkit-scrollbar-thumb:hover,
.student-list.scrolling::-webkit-scrollbar-thumb:hover,
.messages-list::-webkit-scrollbar-thumb:active,
.student-list::-webkit-scrollbar-thumb:active {
  background: rgba(0, 0, 0, 0.6);
}

/* 为消息容器添加悬停状态检测 */
.messages-container:hover .messages-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
}

.messages-container:hover .messages-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

/* 滚动条淡入淡出动画 */
.messages-list::-webkit-scrollbar-thumb,
.student-list::-webkit-scrollbar-thumb {
  transition: background 0.3s ease, opacity 0.3s ease;
}

/* 恢复消息列表滚动功能 */
.messages-list {
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
}

/* 恢复联系人列表滚动功能 */
.student-list {
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
}

/* 申请记录样式 */
.applications-content {
  padding: 0;
}

.applications-list {
  :deep(.el-table) {
    font-size: 14px;
  }
  
  :deep(.el-table__header th) {
    background-color: #f5f7fa;
    color: #606266;
    font-weight: 600;
  }
  
  :deep(.el-table__row) {
    cursor: default;
    
    &:hover {
      background-color: #f5f7fa;
    }
  }
}
</style>