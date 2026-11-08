<template>
  <div class="match-page">
    <!-- 主要内容区域 -->
    <div class="main-content">

      <!-- 匹配设置区域 -->
      <div class="match-settings">
        <!-- 匹配模式选择 -->
        <div class="mode-selection">
          <div class="section-header">
        <h3>选择匹配模式</h3>
          </div>
          <div class="mode-options">
            <div 
              :class="['mode-option', { 'active': matchMode === 'system' }]"
              @click="switchToSystemMode"
            >
              <div class="mode-icon">
                <el-icon><MagicStick /></el-icon>
              </div>
              <div class="mode-content">
                <h4>标签匹配</h4>
                <p>根据标签偏好推荐</p>
              </div>
            </div>
            <div 
              :class="['mode-option', { 'active': matchMode === 'random' }]"
              @click="switchToRandomMode"
            >
              <div class="mode-icon">
                <el-icon><Refresh /></el-icon>
              </div>
              <div class="mode-content">
                <h4>随机匹配</h4>
                <p>随机选择同班同学</p>
              </div>
            </div>
        </div>
      </div>

      <!-- 标签匹配设置 -->
        <div v-if="matchMode === 'system'" class="tag-settings">
        <h3>标签匹配设置</h3>
              
                <div class="tags-container">
            <!-- 希望匹配的标签 -->
            <div class="tag-group">
              <div class="group-header">
                <h4>希望匹配的标签</h4>
                <el-tag size="small" type="success">偏好标签</el-tag>
              </div>
              
              <div class="tag-section">
                <div class="section-content">
                  <div class="content-header">
                    <el-icon class="like-icon"><CircleCheck /></el-icon>
                    <span class="header-text">添加您希望匹配的标签</span>
                  </div>
                  <div class="tags-list">
                    <el-tag 
                      v-for="tag in matchTags.like" 
                      :key="'like-' + tag" 
                      closable
                      @close="removeTag(tag, 'like')"
                      type="success"
                      class="tag-item"
                    >
                      {{ tag }}
                    </el-tag>
                    <el-button 
                      size="small" 
                      @click="showAddTagDialog('like')"
                      class="add-btn"
                      type="primary"
                      plain
                    >
                      <el-icon><Plus /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
              </div>

              <!-- 不希望匹配的标签 -->
            <div class="tag-group">
              <div class="group-header">
                <h4>不希望匹配的标签</h4>
                <el-tag size="small" type="danger">排除标签</el-tag>
              </div>
              
              <div class="tag-section">
                <div class="section-content">
                  <div class="content-header">
                    <el-icon class="dislike-icon"><Close /></el-icon>
                    <span class="header-text">添加您不希望匹配的标签</span>
                  </div>
                  <div class="tags-list">
                    <el-tag 
                      v-for="tag in matchTags.dislike" 
                      :key="'dislike-' + tag" 
                      closable
                      @close="removeTag(tag, 'dislike')"
                      type="danger"
                      class="tag-item"
                    >
                      {{ tag }}
                    </el-tag>
                    <el-button 
                      size="small" 
                      @click="showAddTagDialog('dislike')"
                      class="add-btn"
                      type="danger"
                      plain
                    >
                      <el-icon><Plus /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
                </div>
              </div>

        <!-- 匹配操作 -->
        <div class="match-action">
          <div class="action-info">
            <h3>开始匹配</h3>
            <p v-if="matchMode === 'system'">系统将根据您的标签偏好进行标签匹配</p>
            <p v-else>系统将随机为您匹配同班同学</p>
          </div>
          <el-button 
            v-if="!loading"
            type="primary" 
            size="large"
            @click="startMatch" 
            :disabled="hasTagConflicts"
            class="match-btn"
          >
            <el-icon><Search /></el-icon>
            开始匹配
          </el-button>
          <el-button 
            v-else
            type="danger" 
            size="large"
            @click="cancelMatch" 
            class="match-btn"
          >
            <el-icon><Close /></el-icon>
            取消匹配
          </el-button>
      </div>
      
      <!-- 标签冲突提示 -->
      <div v-if="hasTagConflicts" class="conflict-warning">
        <el-alert
          title="标签冲突警告"
          type="warning"
          :description="`以下标签同时在希望匹配和不希望匹配中：${conflictTags.join('、')}。请先解决冲突再进行匹配。`"
          show-icon
          :closable="false"
        />
      </div>
      </div>
      
      <!-- 匹配结果 - 完全复制室友招募样式 -->
      <div v-if="results.length > 0" class="match-settings">
        <div class="student-list">
          <div class="list-header">
            <div class="list-title">
              <h3>匹配结果 ({{ results.length }} 人)</h3>
            </div>
            <div class="list-actions">
              <el-button type="text" size="small" @click="refreshMatch" :loading="loading">
                <el-icon><Refresh /></el-icon>
                重新匹配
              </el-button>
            </div>
          </div>
          
          <!-- 学生列表卡片 -->
          <div class="student-list-container">
            <div v-for="student in displayResults" :key="student.id" class="student-item">
              <div class="student-card">
                <div class="student-header">
                  <el-avatar :size="50" :src="student.avatar" class="student-avatar" />
                  <div class="student-basic-info">
                    <h4 class="student-name">{{ student.name }}</h4>
                    <p class="student-major">{{ student.major }}</p>
                    <p class="student-class">{{ student.className }}</p>
                  </div>
                </div>
                
                <div class="student-tags">
                  <div class="tags-list">
                    <el-tag 
                      v-for="tag in (student.tags || []).slice(0, 3)" 
                      :key="tag" 
                      size="small"
                      class="tag-item"
                    >
                      {{ tag }}
                    </el-tag>
                    <span v-if="student.tags && student.tags.length > 3" class="more-tags">+{{ student.tags.length - 3 }}</span>
                    <span v-if="!student.tags || student.tags.length === 0" class="no-tags">暂无标签</span>
                  </div>
                </div>
                
                <div class="student-actions">
                  <el-button size="small" type="primary" @click="showUserDetail(student)" class="action-btn">
                    <el-icon><User /></el-icon>
                    详情
                  </el-button>
                  <!-- 已邀请状态 -->
                  <el-dropdown 
                    v-if="isStudentInvited(student.id)"
                    @command="(command) => handleStudentAction(command, student)" 
                    trigger="click"
                  >
                    <el-button size="small" type="success" plain disabled class="action-btn">
                      <el-icon><Check /></el-icon>
                      已邀请
                      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="chat">
                          <el-icon><ChatDotRound /></el-icon>
                          直接聊天
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <!-- 邀请中状态 -->
                  <el-button 
                    v-else-if="isStudentInviting(student.id)"
                    size="small" 
                    type="success"
                    loading
                    class="action-btn"
                  >
                    发送中...
                  </el-button>
                  <!-- 正常状态 -->
                  <el-dropdown 
                    v-else
                    @command="(command) => handleStudentAction(command, student)" 
                    trigger="click"
                  >
                    <el-button size="small" type="success" class="action-btn">
                      <el-icon><ChatDotRound /></el-icon>
                      联系
                      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="invite">
                          <el-icon><House /></el-icon>
                          发送宿舍邀请
                        </el-dropdown-item>
                        <el-dropdown-item command="chat">
                          <el-icon><ChatDotRound /></el-icon>
                          直接聊天
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 分页组件 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[6, 12]"
              :total="results.length"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 添加标签对话框 -->
    <el-dialog
      v-model="addTagDialogVisible"
      :title="`添加${addTagDialogType === 'like' ? '希望匹配' : '不希望匹配'}标签`"
      width="500px"
    >
      <el-form :model="newTagForm" label-width="80px">
        <el-form-item label="标签类型">
          <el-radio-group v-model="newTagForm.type">
            <el-radio label="like">希望匹配</el-radio>
            <el-radio label="dislike">不希望匹配</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签内容">
          <el-input
            v-model="newTagForm.content"
            placeholder="请输入标签内容"
            @keyup.enter="addTag"
          />
        </el-form-item>
      </el-form>
      
      <!-- 热门标签参考 -->
      <div class="popular-tags-section">
        <h4>热门标签参考（可多选）</h4>
        <div class="popular-tags">
          <el-tag 
            v-for="tag in popularTags" 
            :key="'popular-' + tag"
            @click="togglePopularTag(tag)"
            class="popular-tag"
            :class="{ 'selected': selectedPopularTags.includes(tag) }"
            :type="selectedPopularTags.includes(tag) ? 'success' : 'info'"
            :effect="selectedPopularTags.includes(tag) ? 'dark' : 'plain'"
          >
            {{ tag }}
          </el-tag>
        </div>
        <div v-if="selectedPopularTags.length > 0" class="selected-tags-info">
          <p>已选择 {{ selectedPopularTags.length }} 个标签：{{ selectedPopularTags.join('、') }}</p>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="cancelAddTag">取消</el-button>
        <el-button type="primary" @click="addTag">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情对话框 -->
    <el-dialog 
      v-model="userDetailVisible" 
      title="用户详情" 
      width="600px"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
      <div v-if="selectedUser" class="user-detail-content">
        <div class="user-header">
          <el-avatar :size="80" :src="selectedUser.avatar" />
          <div class="user-basic-info">
            <h3>{{ selectedUser.name }}</h3>
            <p>{{ selectedUser.major }} - {{ selectedUser.className }}</p>
            <p>学号：{{ selectedUser.studentNumber || '未知' }}</p>
          </div>
        </div>

        <el-divider />

        <div class="user-sections">
          <div class="section">
            <h4><el-icon><User /></el-icon> 个人简介</h4>
            <p class="bio-text">{{ selectedUser.bio || '该用户暂未填写个人简介' }}</p>
          </div>

          <div class="section">
            <h4><el-icon><CollectionTag /></el-icon> 个人标签</h4>
            <div class="tags-display">
              <el-tag 
                v-for="tag in selectedUser.tags" 
                :key="tag" 
                style="margin: 3px;"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!selectedUser.tags || selectedUser.tags.length === 0" class="no-tags-text">
                该用户暂未设置个人标签
              </span>
            </div>
          </div>

          <div class="section" v-if="selectedUser.dormInfo">
            <h4><el-icon><House /></el-icon> 宿舍信息</h4>
            <p>{{ selectedUser.dormInfo }}</p>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="userDetailVisible = false">关闭</el-button>
          <el-dropdown @command="(command) => handleDetailAction(command)" trigger="click">
            <el-button type="success">
              <el-icon><ChatDotRound /></el-icon>
              联系TA
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="invite">
                  <el-icon><House /></el-icon>
                  发送宿舍邀请
                </el-dropdown-item>
                <el-dropdown-item command="chat">
                  <el-icon><ChatDotRound /></el-icon>
                  直接聊天
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { 
  searchRoommates,
  getPopularTags,
  sendRoommateInvite
} from '@/api/roommate.js'
import { sendInvite as sendInviteAPI } from '@/api/studentAllocation'
import { MagicStick, Refresh, Star, Close, Plus, User, Phone, Message, CircleCheck, ChatDotRound, House, ArrowDown, CollectionTag, Search, Check } from '@element-plus/icons-vue'

export default {
  name: 'RoommateMatch',
  components: {
    MagicStick,
    Refresh,
    Star,
    Close,
    Plus,
    User,
    Phone,
    Message,
    CircleCheck,
    ChatDotRound,
    House,
    ArrowDown,
    CollectionTag,
    Search,
    Check
  },
  data() {
    return {
      matchMode: 'system',
      loading: false,
      matchTimer: null,  // 匹配定时器
      matchTags: {
        like: [],
        dislike: []
      },
      results: [],
      currentUserId: 1,  // 改为数字ID，对应测试数据中的张三
      currentUserClass: '计科2101班',
      currentPage: 1,
      pageSize: 6,
      addTagDialogVisible: false,
      addTagDialogType: 'like',
      newTagForm: {
        type: 'like',
        content: ''
      },
      userDetailVisible: false,
      selectedUser: null,
      selectedPopularTags: [],  // 多选的热门标签
      invitedStudents: [],  // 已邀请的学生ID列表
      invitingStudents: [],  // 正在邀请中的学生ID列表
      dynamicPopularTags: []  // 从后端获取的动态热门标签
    }
  },
  computed: {
    hasTagConflicts() {
      const like = new Set(this.matchTags.like)
      const dislike = new Set(this.matchTags.dislike)
      
      const conflicts = []
      
      // 检查冲突
      for (const tag of like) {
        if (dislike.has(tag)) {
          conflicts.push(tag)
        }
      }
      
      return conflicts.length > 0
    },
    conflictTags() {
      const like = new Set(this.matchTags.like)
      const dislike = new Set(this.matchTags.dislike)
      
      const conflicts = []
      
      for (const tag of like) {
        if (dislike.has(tag)) {
          conflicts.push(tag)
        }
      }
      
      return conflicts
    },
    displayResults() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.results.slice(start, end)
    },
    totalPages() {
      return Math.ceil(this.results.length / this.pageSize)
    },
    hasMoreResults() {
      return this.results.length > this.pageSize
    },
    // 合并预定义标签和动态热门标签
    allPopularTags() {
      // 预定义的基础标签
      const baseTags = [
        '早睡早起',
        '夜猫子',
        '爱干净',
        '爱学习',
        '喜欢安静',
        '性格开朗',
        '爱打游戏',
        '不吸烟',
        '作息规律',
        '爱社交',
        '不打呼噜'
      ]
      // 合并动态热门标签（去重）
      const dynamicTags = this.dynamicPopularTags.map(t => t.tag)
      const allTags = [...new Set([...baseTags, ...dynamicTags])]
      return allTags
    },
    popularTags() {
      return this.allPopularTags
    }
  },
  mounted() {
    this.loadPopularTags()
  },
  
  beforeUnmount() {
    // 组件卸载时清除定时器
    if (this.matchTimer) {
      clearTimeout(this.matchTimer)
      this.matchTimer = null
    }
  },
  methods: {
    async loadPopularTags() {
      try {
        const res = await getPopularTags(20)
        if (res.code === 200 && res.data) {
          this.dynamicPopularTags = res.data
        }
      } catch (error) {
        console.log('获取热门标签失败，使用预定义标签')
        this.dynamicPopularTags = []
      }
    },
    async performSmartMatch() {
      try {
        const res = await searchRoommates({
          preferredTags: this.matchTags.like,
          excludedTags: this.matchTags.dislike
        })
        if (res.code === 200) {
          this.results = res.data || []
        } else {
          this.results = []
          console.error('匹配失败:', res.message)
        }
      } catch (error) {
        this.results = []
        console.error('匹配失败:', error)
        this.$message.error('网络请求失败，请重试')
      }
    },
    async performRandomMatch() {
      try {
        const res = await searchRoommates({ random: true })
        if (res.code === 200) {
          this.results = res.data || []
        } else {
          this.results = []
          console.error('匹配失败:', res.message)
        }
      } catch (error) {
        this.results = []
        console.error('匹配失败:', error)
        this.$message.error('网络请求失败，请重试')
      }
    },
    async startMatch() {
      // 标签匹配模式需要验证是否有标签
      if (this.matchMode === 'system') {
        const hasLikeTags = this.matchTags.like && this.matchTags.like.length > 0
        const hasDislikeTags = this.matchTags.dislike && this.matchTags.dislike.length > 0
        
        if (!hasLikeTags && !hasDislikeTags) {
          this.$message.warning('请先添加匹配标签，再进行标签匹配')
          return
        }
      }
      
      this.loading = true
      this.results = []
      
      // 等待500ms后执行匹配（模拟加载效果）
      await new Promise(resolve => {
        this.matchTimer = setTimeout(resolve, 500)
      })
      
      // 执行匹配
      try {
        if (this.matchMode === 'system') {
          await this.performSmartMatch()
        } else {
          await this.performRandomMatch()
        }
      } finally {
        this.loading = false
        this.matchTimer = null
      }
      
      // 如果没有匹配到结果，给出提示
      if (this.results.length === 0) {
        if (this.matchMode === 'system') {
          // 标签匹配模式：给出更具体的建议
          const hasLikeTags = this.matchTags.like && this.matchTags.like.length > 0
          const hasDislikeTags = this.matchTags.dislike && this.matchTags.dislike.length > 0
          
          if (hasLikeTags && hasDislikeTags) {
            this.$message({
              message: '未找到完全匹配的室友。建议：减少排除标签或尝试添加更多偏好标签来扩大匹配范围',
              type: 'info',
              duration: 4000
            })
          } else if (hasLikeTags) {
            this.$message({
              message: '未找到匹配的室友。建议：添加更多偏好标签（如作息习惯、兴趣爱好等）可以获得更精准的匹配',
              type: 'info',
              duration: 4000
            })
          } else if (hasDislikeTags) {
            this.$message({
              message: '排除条件过于严格，未找到合适的室友。建议：减少排除标签或添加偏好标签',
              type: 'info',
              duration: 4000
            })
          }
        } else {
          // 随机匹配模式
          this.$message({
            message: '暂无可匹配的同班同学，请稍后再试',
            type: 'info',
            duration: 3000
          })
        }
      } else {
        // 匹配成功后，自动滚动到结果区域
        this.$nextTick(() => {
          this.scrollToResults()
        })
      }
    },
    
    // 取消匹配
    cancelMatch() {
      if (this.matchTimer) {
        clearTimeout(this.matchTimer)
        this.matchTimer = null
      }
      this.loading = false
      this.results = []
    },
    refreshMatch() {
      this.startMatch()
    },
    showAddTagDialog(type) {
      this.addTagDialogType = type
      this.newTagForm.type = type
      this.selectedPopularTags = []  // 清空之前的选择
      this.addTagDialogVisible = true
    },
    addTag() {
      // 优先处理多选的热门标签
      if (this.selectedPopularTags.length > 0) {
        this.addSelectedTags()
        this.addTagDialogVisible = false
        return
      }
      
      // 处理手动输入的标签
      if (!this.newTagForm.content.trim()) {
        this.$message.warning('请输入标签内容或选择热门标签')
        return
      }
      
      const tag = this.newTagForm.content.trim()
      
      if (this.matchTags[this.newTagForm.type].includes(tag)) {
        this.$message.warning('标签已存在')
        return
      }
      
      this.matchTags[this.newTagForm.type].push(tag)
      this.newTagForm.content = ''
      this.addTagDialogVisible = false
    },
    removeTag(tag, type) {
      const index = this.matchTags[type].indexOf(tag)
      if (index > -1) {
        this.matchTags[type].splice(index, 1)
      }
    },
    getTagType(tag) {
      const colors = ['', 'success', 'info', 'warning', 'danger']
      return colors[tag.length % colors.length]
    },
    async invite(user) {
      // 防止重复邀请
      if (this.isStudentInviting(user.id) || this.isStudentInvited(user.id)) {
        return
      }
      
      // 标记为邀请中
      this.invitingStudents.push(user.id)
      
      try {
        const res = await sendInviteAPI({
          receiverId: user.id,
          studentId: user.studentNumber
        })
        
        if (res.code === 200) {
          this.$message.success(`已向 ${user.name} 发送宿舍邀请！`)
          
          // 添加到已邀请列表
          if (!this.invitedStudents.includes(user.id)) {
            this.invitedStudents.push(user.id)
          }
        } else {
          this.$message.error(res.message || '发送邀请失败')
        }
      } catch (error) {
        console.error('发送邀请失败:', error)
        // 错误消息已由request拦截器统一处理，这里不再重复显示
      } finally {
        // 移除邀请中状态
        const index = this.invitingStudents.indexOf(user.id)
        if (index > -1) {
          this.invitingStudents.splice(index, 1)
        }
      }
    },
    showUserDetail(user) {
      this.selectedUser = user
      this.userDetailVisible = true
    },
    inviteFromDetail() {
      if (this.selectedUser) {
        this.invite(this.selectedUser)
        this.userDetailVisible = false
      }
    },
    selectPopularTag(tagName) {
      // 将热门标签填入输入框（保留原有功能）
      this.newTagForm.content = tagName
    },
    togglePopularTag(tagName) {
      // 切换热门标签的选中状态
      const index = this.selectedPopularTags.indexOf(tagName)
      if (index > -1) {
        this.selectedPopularTags.splice(index, 1)
      } else {
        this.selectedPopularTags.push(tagName)
      }
    },
    addSelectedTags() {
      // 添加所有选中的热门标签
      if (this.selectedPopularTags.length === 0) {
        this.$message.warning('请先选择要添加的标签')
        return
      }
      
      const currentType = this.newTagForm.type
      let addedCount = 0
      
      this.selectedPopularTags.forEach(tagName => {
        if (!this.matchTags[currentType].includes(tagName)) {
          this.matchTags[currentType].push(tagName)
          addedCount++
        }
      })
      
      if (addedCount > 0) {
        this.$message.success(`成功添加 ${addedCount} 个标签`)
        // 清空选中状态
        this.selectedPopularTags = []
      } else {
        this.$message.info('所选标签已存在')
      }
    },
    scrollToResults() {
      // 滚动到匹配结果区域
      const resultsElement = document.querySelector('.match-results')
      if (resultsElement) {
        resultsElement.scrollIntoView({ 
          behavior: 'smooth', 
          block: 'start' 
        })
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      // 切换页面时滚动到结果区域顶部
      this.$nextTick(() => {
        this.scrollToResults()
      })
    },
    switchToSystemMode() {
      // 切换到标签匹配模式
      this.matchMode = 'system'
      // 清空匹配结果
      this.clearMatchResults()
    },
    switchToRandomMode() {
      // 切换到随机匹配模式
      this.matchMode = 'random'
      // 清空匹配结果
      this.clearMatchResults()
    },
    clearMatchResults() {
      // 清空匹配结果和重置分页
      this.results = []
      this.currentPage = 1
      // 停止正在进行的匹配
      if (this.matchTimer) {
        clearTimeout(this.matchTimer)
        this.matchTimer = null
      }
      this.loading = false
    },
    cancelAddTag() {
      // 取消添加标签，清空选择状态
      this.selectedPopularTags = []
      this.newTagForm.content = ''
      this.addTagDialogVisible = false
    },
    // 获取匹配度对应的样式类
    getMatchRateClass(rate) {
      if (rate >= 80) return 'rate-high'
      if (rate >= 60) return 'rate-medium'
      return 'rate-low'
    },
    // 获取匹配度对应的tag类型
    getMatchRateType(rate) {
      if (rate >= 80) return 'success'
      if (rate >= 60) return 'warning'
      return 'info'
    },
    // 判断是否为偏好标签
    isPreferredTag(tag) {
      return this.matchTags.like && this.matchTags.like.includes(tag)
    },
    // 判断是否为排除标签
    isExcludedTag(tag) {
      return this.matchTags.dislike && this.matchTags.dislike.includes(tag)
    },
    // 处理学生卡片操作
    async handleStudentAction(command, student) {
      switch (command) {
        case 'invite':
          await this.invite(student)
          break
        case 'chat':
          await this.startChat(student)
          break
      }
    },
    // 处理详情页面中的操作
    async handleDetailAction(command) {
      if (!this.selectedUser) return
      
      switch (command) {
        case 'invite':
          await this.invite(this.selectedUser)
          this.userDetailVisible = false
          break
        case 'chat':
          await this.startChat(this.selectedUser)
          this.userDetailVisible = false
          break
      }
    },
    // 直接开始聊天
    async startChat(student) {
      try {
        const { navigateToChat } = await import('@/utils/inviteService.js')
        
        // 使用 userId（系统用户ID）而不是 id（学生表主键），确保聊天对象正确
        const chatUserId = student.userId || student.id
        
        // 确认是否跳转到聊天页面
        this.$confirm(`跳转到与${student.name}的聊天页面？`, '开始聊天', {
          confirmButtonText: '立即跳转',
          cancelButtonText: '取消',
          type: 'info'
        }).then(() => {
          navigateToChat(`student_${chatUserId}`, this.$router, 'roommate-match')
        }).catch(() => {
          // 用户取消
        })
        
      } catch (error) {
        console.error('跳转聊天失败:', error)
        this.$message.error('跳转失败，请重试')
      }
    },
    // 检查学生是否正在邀请中
    isStudentInviting(studentId) {
      return this.invitingStudents.includes(studentId)
    },
    // 检查学生是否已邀请
    isStudentInvited(studentId) {
      return this.invitedStudents.includes(studentId)
    },
    // 分页大小改变
    handleSizeChange(newSize) {
      this.pageSize = newSize
      this.currentPage = 1
    }
  }
}
</script>

<style scoped>
.match-page {
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.class-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #7f8c8d;
  font-size: 0.9rem;
}

.class-info .el-icon {
  color: #3498db;
  font-size: 1rem;
}

.main-content {
  max-width: 1000px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}


.match-settings {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.mode-selection h3,
.tag-settings h3 {
  color: #2c3e50;
  margin-bottom: 20px;
  font-size: 1.2rem;
  font-weight: 600;
}

.mode-options {
  display: flex;
  gap: 15px;
}

.mode-option {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border: 2px solid #e1e8ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.mode-option:hover {
  border-color: #3498db;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.15);
}

.mode-option.active {
  border-color: #3498db;
  background: linear-gradient(135deg, #e3f2fd 0%, #f8f9fa 100%);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.2);
}

.mode-icon {
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.mode-option.active .mode-icon {
  background: #3498db;
  color: white;
}

.mode-option .el-icon {
  font-size: 1.5rem;
  color: #3498db;
}

.mode-option.active .el-icon {
  color: white;
}

.mode-content h4 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 1.1rem;
  font-weight: 600;
}

.mode-content p {
  margin: 0;
  color: #7f8c8d;
  font-size: 0.9rem;
}

.tag-settings {
  margin-top: 30px;
}

.match-desc {
  color: #7f8c8d;
  font-size: 0.9rem;
  margin: 0 0 20px 0;
  padding: 10px 15px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #3498db;
}

.tags-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.tag-group {
  border: 1px solid #e1e8ed;
  border-radius: 12px;
  padding: 20px;
  background: #fafafa;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e1e8ed;
}

.group-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.1rem;
  font-weight: 600;
}

.tag-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tag-section {
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 15px;
}

.section-content {
  padding: 0;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-weight: 500;
  color: #2c3e50;
  font-size: 0.9rem;
}

.header-text {
  flex: 1;
}

.like-icon {
  color: #27ae60;
  font-size: 16px;
  flex-shrink: 0;
}

.dislike-icon {
  color: #e74c3c;
  font-size: 16px;
  flex-shrink: 0;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag-item {
  margin: 0;
}

.add-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.popular-tags-section {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
}

.popular-tags-section h4 {
  margin: 0 0 12px 0;
  color: #2c3e50;
  font-size: 0.95rem;
  font-weight: 600;
}

.popular-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.popular-tag {
  cursor: pointer;
  transition: all 0.2s ease;
  margin: 0;
  font-size: 0.8rem;
}

.popular-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.popular-tag.selected {
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  font-weight: 600;
}

.selected-tags-info {
  margin-top: 15px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
}

.selected-tags-info p {
  margin: 0 0 10px 0;
  color: #0369a1;
  font-size: 0.9rem;
}

.match-action {
  text-align: center;
  margin-top: 30px;
  padding: 25px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 1px solid #e1e8ed;
}

.action-info h3 {
  color: #2c3e50;
  margin-bottom: 10px;
  font-size: 1.3rem;
  font-weight: 600;
}

.action-info p {
  color: #7f8c8d;
  margin-bottom: 20px;
  font-size: 0.95rem;
}

.match-btn {
  padding: 15px 40px;
  font-size: 1.1rem;
  border-radius: 25px;
  min-width: 180px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.conflict-warning {
  margin: 20px 0;
}

.student-list {
  margin-top: 20px;
}

/* 分页组件样式 */
.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.match-pagination {
  --el-pagination-font-size: 14px;
  --el-pagination-bg-color: #fff;
  --el-pagination-text-color: #606266;
  --el-pagination-border-radius: 6px;
}

.match-pagination .el-pagination__total {
  color: #606266;
  font-weight: 500;
}

.match-pagination .el-pager li {
  border-radius: 4px;
  margin: 0 2px;
}

.match-pagination .el-pager li.is-active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
}

/* 用户详情对话框样式 */
.user-detail-content {
  padding: 10px 0;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.user-basic-info h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  color: #303133;
}

.user-basic-info p {
  margin: 5px 0;
  color: #606266;
  font-size: 14px;
}

.user-sections {
  margin-top: 20px;
}

.section {
  margin-bottom: 25px;
}

.section h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #409eff;
  font-weight: 600;
}

.bio-text {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
  min-height: 50px;
  border-left: 4px solid #409eff;
}

.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 32px;
  align-items: center;
}

.no-tags-text {
  color: #c0c4cc;
  font-style: italic;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 列表头部样式 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e4e7ed;
}

.list-title h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.list-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.list-actions .el-button {
  color: #606266;
  transition: all 0.3s ease;
}

.list-actions .el-button:hover {
  color: #409eff;
  background: #f0f9ff;
}

/* 学生列表容器 - 网格布局 */
.student-list-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.student-item {
  width: 100%;
}

.student-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.student-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.student-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.student-avatar {
  flex-shrink: 0;
}

.student-basic-info {
  flex: 1;
  min-width: 0;
}

.student-name {
  margin: 0 0 5px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.student-major {
  margin: 2px 0;
  color: #409eff;
  font-size: 14px;
  font-weight: 500;
}

.student-class {
  margin: 2px 0;
  color: #909399;
  font-size: 14px;
}

.student-match-rate {
  margin: 4px 0 0 0;
}

.student-tags {
  margin-bottom: 12px;
  flex: 1;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-item {
  border-radius: 4px;
  font-size: 11px;
}

.more-tags {
  color: #909399;
  font-size: 11px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

.no-tags {
  color: #c0c4cc;
  font-size: 11px;
  font-style: italic;
}

.student-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-top: auto;
}

.student-actions .el-button {
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 500;
  font-size: 12px;
  flex: 1;
}

.student-actions .el-dropdown {
  flex: 1;
}

.student-actions .el-dropdown .el-button {
  width: 100%;
}

.student-actions .el-dropdown .el-icon--right {
  margin-left: 4px;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .match-page {
    padding: 15px;
  }
  
  .section-header {
  flex-direction: column;
    align-items: flex-start;
  gap: 10px;
}

  .match-settings {
    padding: 20px;
  }
  
  .mode-options {
    flex-direction: column;
  }
  
  .mode-option {
    padding: 15px;
  }
  
  .tags-container {
    grid-template-columns: 1fr;
  }
  
  .result-header {
    flex-direction: column;
    gap: 15px;
  }
  
  .result-actions {
    flex-direction: row;
  justify-content: center;
  }
  
  .match-btn {
    padding: 12px 30px;
    min-width: 150px;
  }
}
</style>
