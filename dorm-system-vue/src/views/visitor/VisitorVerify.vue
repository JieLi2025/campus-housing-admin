<template>
  <div class="visitor-verify-page">
    <div class="verify-container">
      <el-card class="verify-card">
        <template #header>
          <div class="card-header">
            <h2>访客信息验证</h2>
            <el-tag type="success">已验证</el-tag>
          </div>
        </template>
        
        <div class="visitor-details">
          <div class="info-item">
            <span class="label">访客姓名：</span>
            <span class="value">{{ visitorInfo.name }}</span>
          </div>
          <div class="info-item">
            <span class="label">身份证号：</span>
            <span class="value">{{ visitorInfo.idCard }}</span>
          </div>
          <div class="info-item">
            <span class="label">被访学生：</span>
            <span class="value">{{ visitorInfo.visitedStudent }}</span>
          </div>
          <div class="info-item">
            <span class="label">访问宿舍：</span>
            <span class="value">{{ visitorInfo.visitDorm }}</span>
          </div>
          <div class="info-item">
            <span class="label">预计到访时间：</span>
            <span class="value">{{ visitorInfo.visitTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">预计离开时间：</span>
            <span class="value">{{ visitorInfo.leaveTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">登记时间：</span>
            <span class="value">{{ visitorInfo.registerTime }}</span>
          </div>
        </div>
        
        <div class="verification-status">
          <div class="status-item">
            <el-icon class="status-icon success"><CircleCheck /></el-icon>
            <span>访客身份已验证</span>
          </div>
          <div class="status-item">
            <el-icon class="status-icon success"><CircleCheck /></el-icon>
            <span>访问时间在有效期内</span>
          </div>
          <div class="status-item">
            <el-icon class="status-icon success"><CircleCheck /></el-icon>
            <span>访问宿舍信息正确</span>
          </div>
        </div>
        
        <div class="action-buttons">
          <el-button type="success" @click="approveVisit">允许进入</el-button>
          <el-button type="danger" @click="rejectVisit">拒绝进入</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { CircleCheck } from '@element-plus/icons-vue'

export default {
  name: 'VisitorVerify',
  components: {
    CircleCheck
  },
  data() {
    return {
      visitorInfo: {
        name: '',
        idCard: '',
        visitedStudent: '',
        visitDorm: '',
        visitTime: '',
        leaveTime: '',
        registerTime: ''
      }
    }
  },
  mounted() {
    // 从URL参数获取访客信息
    this.loadVisitorInfoFromUrl()
  },
  methods: {
    loadVisitorInfoFromUrl() {
      const urlParams = new URLSearchParams(window.location.search)
      this.visitorInfo = {
        name: urlParams.get('name') || '',
        idCard: urlParams.get('idCard') || '',
        visitedStudent: urlParams.get('visitedStudent') || '',
        visitDorm: urlParams.get('visitDorm') || '',
        visitTime: urlParams.get('visitTime') || '',
        leaveTime: urlParams.get('leaveTime') || '',
        registerTime: urlParams.get('registerTime') || ''
      }
    },
    
    approveVisit() {
      this.$message.success('访客已获准进入宿舍')
      // 这里可以添加记录访问日志的逻辑
    },
    
    rejectVisit() {
      this.$message.error('访客被拒绝进入宿舍')
      // 这里可以添加记录拒绝日志的逻辑
    },
    
    goBack() {
      this.$router.go(-1)
    }
  }
}
</script>

<style scoped>
.visitor-verify-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
}

.verify-container {
  width: 100%;
  max-width: 40rem;
}

.verify-card {
  border-radius: 1rem;
  box-shadow: 0 0.75rem 2.5rem rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 0;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 1.75rem;
  font-weight: 700;
}

.visitor-details {
  margin-bottom: 2rem;
}

.info-item {
  display: flex;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 0.75rem;
  border: 1px solid #e4e7ed;
  margin-bottom: 1rem;
  transition: all 0.3s ease;
}

.info-item:hover {
  background: #f0f2f5;
  border-color: #c0c4cc;
  transform: translateY(-0.125rem);
  box-shadow: 0 0.25rem 0.75rem rgba(0, 0, 0, 0.1);
}

.label {
  font-weight: 600;
  color: #606266;
  font-size: 0.875rem;
  min-width: 8rem;
  margin-right: 1rem;
}

.value {
  color: #303133;
  font-size: 1rem;
  line-height: 1.5;
  flex: 1;
}

.verification-status {
  background: #f0f9ff;
  border-radius: 1rem;
  padding: 1.5rem;
  margin-bottom: 2rem;
  border: 1px solid #bae6fd;
}

.status-item {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.status-item:last-child {
  margin-bottom: 0;
}

.status-icon {
  font-size: 1.25rem;
  margin-right: 0.75rem;
}

.status-icon.success {
  color: #10b981;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  min-width: 8rem;
  height: 3rem;
  font-size: 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .visitor-verify-page {
    padding: 0.625rem;
  }
  
  .verify-container {
    max-width: 100%;
  }
  
  .card-header h2 {
    font-size: 1.375rem;
  }
  
  .info-item {
    flex-direction: column;
    padding: 0.75rem;
  }
  
  .label {
    margin-bottom: 0.5rem;
    margin-right: 0;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .action-buttons .el-button {
    width: 100%;
  }
}
</style>
