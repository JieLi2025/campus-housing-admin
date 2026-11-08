<template>
  <div class="profile-page">
    <el-card>
      <template #header>
        <h2>个人中心</h2>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="profile-info">
            <div class="avatar-section" @click="showAvatarDialog">
              <el-avatar :size="120" :src="editableInfo.avatar" class="profile-avatar" />
              <div class="avatar-overlay">
                <el-icon class="camera-icon"><Camera /></el-icon>
              </div>
            </div>
            <div class="user-name-section">
              <h2 class="user-name">{{ basicInfo.realName }}</h2>
              <p class="user-title">{{ basicInfo.major }} · {{ basicInfo.className }}</p>
            </div>
            <div class="basic-info">
              <div class="info-row">
                <el-icon class="info-icon"><User /></el-icon>
                <span class="info-text">{{ getGenderText(basicInfo.gender) }}</span>
              </div>
              <div class="info-row">
                <el-icon class="info-icon"><School /></el-icon>
                <span class="info-text">{{ basicInfo.studentId }}</span>
              </div>
              <div class="info-row">
                <el-icon class="info-icon"><Message /></el-icon>
                <span class="info-text">{{ editableInfo.email || '未绑定邮箱' }}</span>
              </div>
              <div class="info-row">
                <el-icon class="info-icon"><OfficeBuilding /></el-icon>
                <span class="info-text">{{ editableInfo.dormLocation || '暂未分配宿舍' }}</span>
              </div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="16">
          <div class="profile-tags">
            <h4><el-icon><CollectionTag /></el-icon> 个人标签</h4>
            <div class="tags-container">
              <el-tag 
                v-for="tag in tags" 
                :key="tag" 
                closable
                @close="removeTag(tag)"
                style="margin: 5px;"
              >
                {{ tag }}
              </el-tag>
              <el-button size="small" @click="showAddTagDialog">添加标签</el-button>
            </div>
          </div>
          
          <div class="profile-bio">
            <h4><el-icon><EditPen /></el-icon> 个人简介</h4>
            <div class="bio-content">
              <p>{{ editableInfo.bio || '这个人很懒什么都没写' }}</p>
              <el-button size="small" @click="showEditBioDialog">编辑简介</el-button>
            </div>
          </div>
          
          <div class="schedule-settings">
            <h4><el-icon><Clock /></el-icon> 作息时间</h4>
            <el-form label-width="80px">
              <el-form-item label="就寝时间">
                <el-time-picker
                  v-model="editableInfo.bedTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择就寝时间"
                  @change="handleTimeChange"
                />
              </el-form-item>
              <el-form-item label="起床时间">
                <el-time-picker
                  v-model="editableInfo.wakeTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择起床时间"
                  @change="handleTimeChange"
                />
              </el-form-item>
            </el-form>
          </div>

        </el-col>
      </el-row>
    </el-card>


    <!-- 添加标签对话框 -->
    <el-dialog v-model="addTagDialogVisible" title="添加标签" width="400px">
      <el-form>
        <el-form-item label="标签名称">
          <el-input v-model="newTag" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addTag">添加</el-button>
      </template>
    </el-dialog>

    <!-- 编辑简介对话框 -->
    <el-dialog v-model="editBioDialogVisible" title="编辑个人简介" width="500px">
      <el-form>
        <el-form-item>
          <el-input 
            v-model="tempBio" 
            type="textarea" 
            :rows="4"
            placeholder="请输入个人简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editBioDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBio">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改头像对话框 -->
    <el-dialog v-model="avatarDialogVisible" title="修改头像" width="500px">
      <div class="avatar-change-content">
        <div class="avatar-preview">
          <el-avatar :size="120" :src="tempAvatar || editableInfo.avatar" />
        </div>
        
        <el-tabs v-model="avatarUploadType" class="avatar-upload-tabs">
          <el-tab-pane label="文件上传" name="upload">
            <div class="upload-section">
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
                action="#"
                :auto-upload="false"
                :on-change="handleAvatarChange"
              >
                <el-button type="primary" plain>
                  <el-icon><Plus /></el-icon>
                  选择图片
                </el-button>
              </el-upload>
              <div class="upload-tips">
                <p>支持 JPG、PNG 格式，文件大小不超过 2MB</p>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="网络链接" name="url">
            <el-form label-width="80px">
              <el-form-item label="头像URL">
                <el-input 
                  v-model="tempAvatar" 
                  placeholder="请输入头像URL"
                  @input="handleUrlChange"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="avatarDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAvatar">保存</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script>
import { getProfile, updateProfile } from '@/api/profile.js'
import defaultAvatar from '@/assets/images/default-avatar.svg?url'

export default {
  name: 'StudentProfile',
  data() {
    return {
      // 不可编辑的基础信息（来自Excel导入）
      basicInfo: {
        realName: '',
        studentId: '',
        gender: '',
        major: '',
        className: ''
      },
      // 可编辑的个人信息
      editableInfo: {
        avatar: defaultAvatar,
        bio: '',
        email: '',
        bedTime: '23:00',
        wakeTime: '07:00'
      },
      tags: [],
      addTagDialogVisible: false,
      editBioDialogVisible: false,
      avatarDialogVisible: false,
      tempAvatar: '',
      avatarUploadType: 'upload',
      newTag: '',
      tempBio: '',
      loading: false
    }
  },
  mounted() {
    this.loadProfileData()
    
    // 监听邮箱更新事件
    this.emailUpdateHandler = (event) => {
      if (event.detail && event.detail.newEmail) {
        // 更新本地显示的邮箱
        this.editableInfo.email = event.detail.newEmail
      }
    }
    window.addEventListener('emailUpdated', this.emailUpdateHandler)
  },
  beforeUnmount() {
    // 移除事件监听器
    if (this.emailUpdateHandler) {
      window.removeEventListener('emailUpdated', this.emailUpdateHandler)
    }
  },
  methods: {
    // 从后端API加载个人信息
    async loadProfileData() {
      try {
        this.loading = true
        const response = await getProfile()
        
        if (response.code === 200 && response.data) {
          const profile = response.data
          
          // 填充基础信息
          this.basicInfo = {
            realName: profile.realName || '未设置',
            studentId: profile.studentId || profile.username || '未知',
            gender: profile.gender || '男',
            major: profile.major || '未设置专业',
            className: profile.className || '未设置班级'
          }
          
          // 填充可编辑信息
          this.editableInfo = {
            avatar: profile.avatar || defaultAvatar,
            bio: profile.bio || '',
            email: profile.email || '',
            bedTime: profile.bedTime || '23:00',
            wakeTime: profile.wakeTime || '07:00',
            dormLocation: profile.dormLocation || '暂未分配宿舍'
          }
          
          // 解析标签
          if (profile.tags) {
            try {
              this.tags = JSON.parse(profile.tags)
            } catch (e) {
              this.tags = []
            }
          } else {
            this.tags = []
          }
          
          console.log('个人信息加载成功:', profile)
        } else {
          this.$message.error(response.message || '加载个人信息失败')
        }
      } catch (error) {
        console.error('加载个人信息失败:', error)
        this.$message.error('加载个人信息失败，请重新登录')
        // 如果是认证失败，跳转到登录页
        if (error.response && error.response.status === 401) {
          this.$router.push('/login')
        }
      } finally {
        this.loading = false
      }
    },
    
    // 保存个人信息到后端
    async saveProfileData() {
      try {
        this.loading = true
        
        // 准备更新数据
        const updateData = {
          avatar: this.editableInfo.avatar,
          bio: this.editableInfo.bio,
          email: this.editableInfo.email,
          bedTime: this.editableInfo.bedTime,
          wakeTime: this.editableInfo.wakeTime,
          tags: JSON.stringify(this.tags) // 转为JSON字符串
        }
        
        const response = await updateProfile(updateData)
        
        if (response.code === 200) {
          this.$message.success('保存成功')
          return true
        } else {
          this.$message.error(response.message || '保存失败')
          return false
        }
      } catch (error) {
        console.error('保存个人信息失败:', error)
        this.$message.error('保存失败，请稍后重试')
        return false
      } finally {
        this.loading = false
      }
    },
    
    async removeTag(tag) {
      const index = this.tags.indexOf(tag)
      if (index > -1) {
        this.tags.splice(index, 1)
        // 保存数据，saveProfileData中已有成功提示，无需重复显示
        await this.saveProfileData()
      }
    },
    showAddTagDialog() {
      this.newTag = ''
      this.addTagDialogVisible = true
    },
    async addTag() {
      if (!this.newTag.trim()) {
        this.$message.warning('请输入标签名称')
        return
      }
      if (this.tags.includes(this.newTag.trim())) {
        this.$message.warning('标签已存在')
        return
      }
      this.tags.push(this.newTag.trim())
      this.addTagDialogVisible = false
      
      // 保存数据，saveProfileData中已有成功提示，无需重复显示
      await this.saveProfileData()
    },
    showEditBioDialog() {
      this.tempBio = this.editableInfo.bio
      this.editBioDialogVisible = true
    },
    async saveBio() {
      this.editableInfo.bio = this.tempBio
      this.editBioDialogVisible = false
      
      // 保存数据，saveProfileData中已有成功提示，无需重复显示
      await this.saveProfileData()
    },
    
    // 显示头像修改对话框
    showAvatarDialog() {
      this.tempAvatar = this.editableInfo.avatar
      this.avatarUploadType = 'upload'
      this.avatarDialogVisible = true
    },
    
    // 文件上传之前的验证
    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
      const isLt2M = file.size / 1024 / 1024 < 2

      if (!isJPG) {
        this.$message.error('头像图片只能是 JPG/PNG 格式!')
        return false
      }
      if (!isLt2M) {
        this.$message.error('头像图片大小不能超过 2MB!')
        return false
      }
      return true
    },
    
    // 文件选择变化时
    handleAvatarChange(file) {
      if (this.beforeAvatarUpload(file.raw)) {
        // 使用 FileReader 读取文件并转换为 base64
        const reader = new FileReader()
        reader.onload = (e) => {
          this.tempAvatar = e.target.result
        }
        reader.readAsDataURL(file.raw)
      }
    },
    
    // 文件上传成功
    handleAvatarSuccess(response, file) {
      this.tempAvatar = URL.createObjectURL(file.raw)
    },
    
    // URL输入变化时
    handleUrlChange() {
      // URL变化时的处理逻辑
    },
    
    // 保存头像
    async saveAvatar() {
      if (!this.tempAvatar) {
        this.$message.error('请选择头像或输入头像URL')
        return
      }
      
      this.editableInfo.avatar = this.tempAvatar
      this.avatarDialogVisible = false
      
      const success = await this.saveProfileData()
      if (success) {
        this.$message.success('头像更新成功')
        // 触发自定义事件，通知 Layout.vue 更新右上角头像
        window.dispatchEvent(new CustomEvent('avatarUpdated', { 
          detail: { newAvatar: this.tempAvatar } 
        }))
      }
    },
    
    // 性别显示文本
    getGenderText(gender) {
      // 后端已返回中文，直接使用
      return gender === '男' || gender === '女' ? gender : gender || '保密'
    },
    
    
    // 作息时间变更时保存
    async handleTimeChange() {
      await this.saveProfileData()
    },
    
    // 保存设置
    saveSettings() {
      // TODO: 如果需要，可以将设置保存到后端
      this.$message.success('设置已保存')
    },
    
    // 加载设置
    loadSettings() {
      // TODO: 如果需要，可以从后端加载设置
    }
  }
}
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.profile-info {
  text-align: center;
  padding: 30px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  color: white;
  position: relative;
  overflow: hidden;
}

.profile-info::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent, rgba(255,255,255,0.1), transparent);
  transform: rotate(45deg);
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
  100% { transform: translateX(100%) translateY(100%) rotate(45deg); }
}

.avatar-section {
  margin-bottom: 20px;
  position: relative;
  cursor: pointer;
}

.profile-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.avatar-section:hover .profile-avatar {
  transform: scale(1.05);
  border-color: rgba(255, 255, 255, 0.6);
}

.avatar-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  opacity: 0;
  visibility: hidden;
}

.avatar-section:hover .avatar-overlay {
  opacity: 1;
  visibility: visible;
  background: rgba(0, 0, 0, 0.7);
  transform: translate(-50%, -50%) scale(1.1);
}

.camera-icon {
  color: white;
  font-size: 20px;
}

.user-name-section {
  margin-bottom: 25px;
}

.user-name {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.user-title {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
  font-weight: 300;
}

.basic-info {
  text-align: left;
  margin: 0;
  padding: 0;
  background: transparent;
}

.info-row {
  display: flex;
  align-items: center;
  margin: 12px 0;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.info-row:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateX(5px);
}

.info-icon {
  margin-right: 12px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  width: 20px;
  display: flex;
  justify-content: center;
}

.info-text {
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
  font-weight: 400;
}



.profile-tags, .profile-bio, .schedule-settings, .preference-settings {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.profile-tags h4, .profile-bio h4, .schedule-settings h4, .preference-settings h4 {
  margin: 0 0 15px 0;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.setting-desc {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.preference-settings .el-form-item {
  margin-bottom: 20px;
}

.preference-settings .el-form-item__content {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.preference-settings .el-slider {
  margin-right: 15px;
}

/* 头像上传样式 */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-avatar-preview {
  flex-shrink: 0;
}

.avatar-input-group {
  flex: 1;
}

/* 个人简介样式 */
.bio-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bio-content p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
  min-height: 24px;
}

/* 头像修改对话框样式 */
.avatar-change-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.avatar-preview {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.avatar-upload-tabs {
  width: 100%;
}

.upload-section {
  text-align: center;
  padding: 20px;
}

.avatar-uploader {
  display: inline-block;
}

.upload-tips {
  margin-top: 15px;
}

.upload-tips p {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

</style>
