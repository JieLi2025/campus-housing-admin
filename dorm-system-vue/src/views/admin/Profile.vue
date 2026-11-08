<template>
  <div class="profile-page">
    <el-card>
      <template #header>
        <div class="header-actions">
          <h2>个人设置</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="userInfo" label-width="120px" :rules="basicRules" ref="basicFormRef">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="userInfo.username" placeholder="请输入用户名" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="userInfo.realName" placeholder="请输入真实姓名" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="userInfo.email" placeholder="请输入邮箱地址" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="userInfo.phone" placeholder="请输入手机号码" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="部门" prop="department">
                  <el-select v-model="userInfo.department" placeholder="请选择部门" style="width: 100%">
                    <el-option label="宿舍管理处" value="宿舍管理处" />
                    <el-option label="学生处" value="学生处" />
                    <el-option label="后勤处" value="后勤处" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职位" prop="position">
                  <el-input v-model="userInfo.position" placeholder="请输入职位" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="办公地点" prop="officeLocation">
                  <el-input v-model="userInfo.officeLocation" placeholder="请输入办公地点，如：1号楼101室" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="工作时间">
                  <div class="work-time-picker">
                    <el-time-picker
                      v-model="userInfo.workStartTime"
                      format="HH:mm"
                      value-format="HH:mm"
                      placeholder="上班时间"
                      style="width: 45%"
                    />
                    <span class="time-separator">至</span>
                    <el-time-picker
                      v-model="userInfo.workEndTime"
                      format="HH:mm"
                      value-format="HH:mm"
                      placeholder="下班时间"
                      style="width: 45%"
                    />
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="个人简介" prop="bio">
              <el-input v-model="userInfo.bio" type="textarea" :rows="3" placeholder="请输入个人简介" />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="saveBasicInfo">保存信息</el-button>
              <el-button @click="resetBasicInfo">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 密码修改 -->
        <el-tab-pane label="密码修改" name="password">
          <el-form :model="passwordForm" label-width="120px" :rules="passwordRules" ref="passwordFormRef">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" show-password />
            </el-form-item>
            
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="changePassword">修改密码</el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { getCurrentUser, setCurrentUser } from '@/utils/auth.js'
import { updateAdmin, getAdminCredentials } from '@/utils/adminManagement.js'
import { getProfile, updateProfile, updatePassword } from '@/api/profile.js'

export default {
  name: 'AdminProfile',
  inject: {
    logOperation: {
      from: 'logOperation',
      default: () => () => {} // 提供一个空函数作为默认值
    }
  },
  data() {
    // 密码确认验证
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    };
    
    return {
      activeTab: 'basic',
      userInfo: {
        username: '',
        realName: '',
        email: '',
        phone: '',
        department: '',
        position: '',
        officeLocation: '',
        workStartTime: '',
        workEndTime: '',
        bio: ''
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      basicRules: {
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
        ]
      },
      passwordRules: {
        currentPassword: [
          { required: true, message: '请输入当前密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  mounted() {
    this.loadUserInfo();
  },
  methods: {
    async loadUserInfo() {
      try {
        // 从后端API获取用户信息
        const res = await getProfile()
        if (res.code === 200 && res.data) {
          const data = res.data
          this.userInfo = {
            username: data.username || '',
            realName: data.realName || '',
            email: data.email || '',
            phone: data.phone || '',
            department: data.department || '',
            position: data.position || '',
            officeLocation: data.officeLocation || '',
            workStartTime: data.workStartTime || '',
            workEndTime: data.workEndTime || '',
            bio: data.bio || ''
          }
          // 兼容旧的workHours格式
          if (!data.workStartTime && data.workHours) {
            const match = data.workHours.match(/(\d{1,2}:\d{2})-(\d{1,2}:\d{2})/)
            if (match) {
              this.userInfo.workStartTime = match[1]
              this.userInfo.workEndTime = match[2]
            }
          }
        } else {
          // 如果后端没有数据，使用默认数据
          this.setDefaultUserInfo()
        }
      } catch (error) {
        console.error('加载用户信息失败:', error)
        // 加载失败时使用默认数据，不进行路由跳转
        this.setDefaultUserInfo()
      }
    },
    
    setDefaultUserInfo() {
      // 从当前登录信息获取基本数据
      const currentUser = getCurrentUser()
      this.userInfo = {
        username: currentUser?.username || '',
        realName: currentUser?.realName || '',
        email: currentUser?.email || '',
        phone: currentUser?.phone || '',
        department: '',
        position: '',
        officeLocation: '',
        workStartTime: '',
        workEndTime: '',
        bio: ''
      }
      console.log('使用默认数据')
    },
    async saveBasicInfo() {
      this.$refs.basicFormRef.validate(async (valid) => {
        if (valid) {
          try {
            // 组合工作时间
            const saveData = { ...this.userInfo }
            if (this.userInfo.workStartTime && this.userInfo.workEndTime) {
              saveData.workHours = `${this.userInfo.workStartTime}-${this.userInfo.workEndTime}`
            }
            
            const res = await updateProfile(saveData)
            if (res.code === 200) {
              this.$message.success('基本信息保存成功！')
              this.logOperation('保存基本信息', '个人设置')
              // 更新本地用户信息
              const currentUser = getCurrentUser()
              if (currentUser) {
                setCurrentUser({ ...currentUser, ...this.userInfo })
              }
            } else {
              this.$message.error(res.message || '保存失败')
            }
          } catch (error) {
            console.error('保存基本信息失败:', error)
            this.$message.error('保存失败：' + (error.message || '网络错误'))
          }
        }
      })
    },
    resetBasicInfo() {
      // 重新加载用户信息，而不是简单地重置表单（避免重置用户名等不可修改字段）
      this.loadUserInfo();
      this.$message.info('已重置基本信息');
    },
    async changePassword() {
      this.$refs.passwordFormRef.validate(async (valid) => {
        if (valid) {
          try {
            // 调用后端API修改密码
            const res = await updatePassword({
              currentPassword: this.passwordForm.currentPassword,
              newPassword: this.passwordForm.newPassword,
              confirmPassword: this.passwordForm.confirmPassword
            })

            if (res.code === 200) {
              this.$message.success('密码修改成功！')
              this.logOperation('修改密码', '个人设置')
              this.resetPasswordForm()
              
              // 提示用户重新登录
              this.$confirm('密码已修改成功，需要重新登录。是否现在重新登录？', '提示', {
                confirmButtonText: '重新登录',
                cancelButtonText: '稍后登录',
                type: 'success'
              }).then(() => {
                // 清除当前登录状态并跳转到登录页
                localStorage.clear()
                this.$router.push('/login')
              }).catch(() => {
                // 用户选择稍后登录
              })
            } else {
              this.$message.error(res.message || '密码修改失败')
            }
          } catch (error) {
            console.error('密码修改失败:', error)
            this.$message.error('密码修改失败：' + (error.message || '网络错误'))
          }
        }
      })
    },
    resetPasswordForm() {
      this.$refs.passwordFormRef.resetFields();
    },

  }
}
</script>

<style scoped>
.profile-page {
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



/* 表单样式优化 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input__inner) {
  border-radius: 6px;
}

:deep(.el-textarea__inner) {
  border-radius: 6px;
}

:deep(.el-select) {
  width: 100%;
}

/* 工作时间选择器样式 */
.work-time-picker {
  display: flex;
  align-items: center;
  width: 100%;
}

.time-separator {
  margin: 0 8px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-page {
    padding: 10px;
  }
}
</style>
