// 申请数据管理工具

// 申请数据存储键
const APPLICATION_DATA_KEY = 'applicationData';

// 默认申请数据
const defaultApplications = [
  {
    id: 1,
    studentName: '张三',
    studentId: '2021001001',
    type: '调宿申请',
    applyTime: '2024-01-15',
    reason: '希望调换到同专业同学宿舍',
    status: '待审核',
    dormInfo: '1号楼-101',
    targetDorm: '2号楼-205',
    createTime: new Date('2024-01-15 09:15:00')
  },
  {
    id: 2,
    studentName: '李四',
    studentId: '2021001002',
    type: '退宿申请',
    applyTime: '2024-01-14',
    reason: '个人原因申请退宿',
    status: '已通过',
    dormInfo: '3号楼-302',
    targetDorm: null,
    createTime: new Date('2024-01-14 14:30:00')
  },
  {
    id: 3,
    studentName: '王五',
    studentId: '2021001003',
    type: '调宿申请',
    applyTime: '2024-01-13',
    reason: '宿舍环境不适合',
    status: '已拒绝',
    rejectReason: '目前宿舍资源紧张，暂时无法满足调宿需求',
    dormInfo: '4号楼-401',
    targetDorm: '1号楼-105',
    createTime: new Date('2024-01-13 16:45:00')
  },
  {
    id: 4,
    studentName: '赵六',
    studentId: '2021001004',
    type: '退宿申请',
    applyTime: '2024-01-16',
    reason: '毕业申请退宿，需要办理离校手续',
    status: '待审核',
    dormInfo: '1号楼-101',
    targetDorm: null,
    createTime: new Date('2024-01-16 10:20:00')
  },
  {
    id: 5,
    studentName: '测试学生A',
    studentId: '2021001005',
    type: '退宿申请',
    applyTime: '2024-01-17',
    reason: '测试物资清点功能，请点击开始物资清点按钮',
    status: '待审核',
    dormInfo: '1号楼-102',
    targetDorm: null,
    createTime: new Date('2024-01-17 15:30:00')
  },
  {
    id: 6,
    studentName: '测试学生B',
    studentId: '2021001006',
    type: '退宿申请',
    applyTime: '2024-01-18',
    reason: '因个人原因需要退宿',
    status: '待赔偿处理',
    dormInfo: '1号楼-101',
    targetDorm: null,
    createTime: new Date('2024-01-18 10:00:00'),
    compensationList: [
      { itemName: '椅子', quantity: 1, amount: 150, reason: '椅子腿损坏' },
      { itemName: '床板', quantity: 1, amount: 200, reason: '床板开裂' },
      { itemName: '书架', quantity: 1, amount: 100, reason: '缺失未归还' }
    ],
    contactPhone: '0571-88888888'
  },
  // 王小明的申请记录
  {
    id: 7,
    studentName: '王小明',
    studentId: '2024001',
    type: '调宿申请',
    applyTime: '2024-01-20',
    reason: '现在的室友作息时间不太合适，经常晚上打游戏影响休息，希望能调换到作息规律的宿舍',
    status: '待审核',
    dormInfo: '2号楼-203',
    targetDorm: '1号楼-106',
    createTime: new Date('2024-01-20 09:30:00')
  },
  {
    id: 8,
    studentName: '王小明',
    studentId: '2024001',
    type: '调宿申请',
    applyTime: '2024-01-10',
    reason: '希望调换到离图书馆更近的宿舍，方便学习',
    status: '已通过',
    dormInfo: '3号楼-301',
    targetDorm: '2号楼-203',
    createTime: new Date('2024-01-10 14:20:00'),
    assignedDorm: '2号楼-203',
    assignmentType: 'direct'
  },
  {
    id: 9,
    studentName: '王小明',
    studentId: '2024001',
    type: '调宿申请',
    applyTime: '2023-12-15',
    reason: '想和高中同学住一起',
    status: '已拒绝',
    rejectReason: '该宿舍已满员，且不符合专业优先分配原则',
    dormInfo: '3号楼-301',
    targetDorm: '4号楼-405',
    createTime: new Date('2023-12-15 11:00:00')
  },
  {
    id: 10,
    studentName: '王小明',
    studentId: '2024001',
    type: '退宿申请',
    applyTime: '2023-11-20',
    reason: '原计划转学，后来取消了',
    status: '已拒绝',
    rejectReason: '学生已撤回申请',
    dormInfo: '3号楼-301',
    targetDorm: null,
    createTime: new Date('2023-11-20 16:45:00')
  },
  {
    id: 11,
    studentName: '王小明',
    studentId: '2024001',
    type: '调宿申请',
    applyTime: '2024-01-18',
    reason: '宿舍空调经常故障，影响居住体验',
    status: '待审核',
    dormInfo: '2号楼-203',
    targetDorm: '1号楼-105',
    createTime: new Date('2024-01-18 15:10:00')
  }
];

// 获取申请数据
export function getApplicationData() {
  let applications = [];
  
  // 读取管理员创建的申请数据
  const dataStr = localStorage.getItem(APPLICATION_DATA_KEY);
  if (dataStr) {
    try {
      const data = JSON.parse(dataStr);
      // 将时间字符串转换回Date对象
      applications = data.map(item => ({
        ...item,
        createTime: new Date(item.createTime)
      }));
    } catch (error) {
      console.error('解析申请数据失败:', error);
      // 如果解析失败，使用默认数据
      applications = [...defaultApplications];
    }
  } else {
    // 如果没有数据，使用默认数据
    applications = [...defaultApplications];
    setApplicationData(defaultApplications);
  }
  
  // 读取学生提交的调宿申请
  const transferDataStr = localStorage.getItem('transferApplications');
  if (transferDataStr) {
    try {
      const transferData = JSON.parse(transferDataStr);
      // 转换为统一格式
      const formattedTransferData = transferData.map(item => ({
        id: item.id,
        studentName: item.studentName,
        studentId: item.studentId,
        type: '调宿申请',
        applyTime: item.submitTime,
        reason: item.reason,
        status: item.status,
        dormInfo: item.dormInfo,
        targetDorm: item.targetBuilding && item.targetRoom ? 
          `${item.targetBuilding}${item.targetRoom}` : item.targetBuilding || '待分配',
        allocationPhase: item.allocationPhase, // 'self-selection' 或 'confirmed'
        isLeader: item.isLeader, // 是否转让了舍长
        description: item.description,
        createTime: new Date(item.submitTime || Date.now())
      }));
      
      // 合并申请数据
      applications = [...applications, ...formattedTransferData];
    } catch (error) {
      console.error('解析学生调宿申请失败:', error);
    }
  }
  
  // 按时间倒序排序
  applications.sort((a, b) => b.createTime - a.createTime);
  
  return applications;
}

// 设置申请数据
export function setApplicationData(applications) {
  localStorage.setItem(APPLICATION_DATA_KEY, JSON.stringify(applications));
}

// 添加新申请
export function addApplication(application) {
  const applications = getApplicationData();
  const newApplication = {
    ...application,
    id: Date.now(), // 使用时间戳作为ID
    createTime: new Date(),
    status: '待审核'
  };
  applications.unshift(newApplication); // 添加到开头
  setApplicationData(applications);
  return newApplication;
}

// 更新申请状态
export function updateApplicationStatus(applicationId, status) {
  const applications = getApplicationData();
  const application = applications.find(app => app.id == applicationId); // 使用 == 进行类型转换比较
  if (application) {
    application.status = status;
    setApplicationData(applications);
    return true;
  }
  return false;
}

// 删除申请
export function deleteApplication(applicationId) {
  const applications = getApplicationData();
  const filteredApplications = applications.filter(app => app.id !== applicationId);
  setApplicationData(filteredApplications);
}

// 获取待审核的申请数量
export function getPendingApplicationCount() {
  const applications = getApplicationData();
  return applications.filter(app => app.status === '待审核').length;
}

// 根据状态筛选申请
export function getApplicationsByStatus(status) {
  const applications = getApplicationData();
  if (status) {
    return applications.filter(app => app.status === status);
  }
  return applications;
}

// 根据类型筛选申请
export function getApplicationsByType(type) {
  const applications = getApplicationData();
  if (type) {
    return applications.filter(app => app.type === type);
  }
  return applications;
}

// 重置为默认申请数据
export function resetToDefaultApplications() {
  setApplicationData(defaultApplications);
}

// 清空所有申请数据
export function clearAllApplications() {
  setApplicationData([]);
}
