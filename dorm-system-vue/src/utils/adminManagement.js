// 管理员数据管理工具
// 用于统一管理超级管理员和普通管理员的数据同步

const ADMIN_STORAGE_KEY = 'dorm_system_admins';
const ADMIN_CREDENTIALS_KEY = 'dorm_system_admin_credentials';

// 默认管理员数据
const defaultAdmins = [
  {
    id: 1,
    name: '张管理员',
    username: 'admin',
    email: 'admin@dormitory.edu.cn',
    phone: '13800138000',
    department: '宿舍管理处',
    position: '宿舍管理员',
    bio: '负责学生宿舍的日常管理和维护工作',
    buildings: '未分配',
    status: 'active',
    type: 'admin',
    createdAt: '2024-01-01',
    lastLogin: null
  },
  {
    id: 2,
    name: '李管理员',
    username: 'admin2',
    email: 'admin2@dormitory.edu.cn',
    phone: '13800138001',
    department: '学生处',
    position: '学生管理员',
    bio: '负责学生事务管理和服务',
    buildings: '未分配',
    status: 'active',
    type: 'admin',
    createdAt: '2024-01-01',
    lastLogin: null
  },
  {
    id: 3,
    name: '王管理员',
    username: 'admin3',
    email: 'admin3@dormitory.edu.cn',
    phone: '13800138002',
    department: '后勤处',
    position: '后勤管理员',
    bio: '负责后勤保障和设施维护',
    buildings: '未分配',
    status: 'active',
    type: 'admin',
    createdAt: '2024-01-01',
    lastLogin: null
  },
  {
    id: 4,
    name: '赵管理员',
    username: 'admin4',
    email: 'admin4@dormitory.edu.cn',
    phone: '13800138003',
    department: '宿舍管理处',
    position: '宿舍管理员',
    bio: '负责宿舍安全和卫生管理',
    buildings: '未分配',
    status: 'active',
    type: 'admin',
    createdAt: '2024-01-01',
    lastLogin: null
  }
];

// 默认管理员凭据（用户名和密码）
const defaultCredentials = {
  'admin': { password: '12345678', active: true },
  'admin2': { password: '12345678', active: true },
  'admin3': { password: '12345678', active: true },
  'admin4': { password: '12345678', active: true },
  'superadmin': { password: '12345678', active: true }
};

// 获取所有管理员数据
export function getAllAdmins() {
  const adminsStr = localStorage.getItem(ADMIN_STORAGE_KEY);
  if (adminsStr) {
    try {
      return JSON.parse(adminsStr);
    } catch (error) {
      console.error('解析管理员数据失败:', error);
    }
  }
  
  // 如果没有数据，初始化默认数据
  setAllAdmins(defaultAdmins);
  return defaultAdmins;
}

// 设置所有管理员数据
export function setAllAdmins(admins) {
  try {
    localStorage.setItem(ADMIN_STORAGE_KEY, JSON.stringify(admins));
  } catch (error) {
    console.error('保存管理员数据失败:', error);
  }
}

// 获取管理员凭据
export function getAdminCredentials() {
  const credentialsStr = localStorage.getItem(ADMIN_CREDENTIALS_KEY);
  if (credentialsStr) {
    try {
      return JSON.parse(credentialsStr);
    } catch (error) {
      console.error('解析管理员凭据失败:', error);
    }
  }
  
  // 如果没有凭据，初始化默认凭据
  setAdminCredentials(defaultCredentials);
  return defaultCredentials;
}

// 设置管理员凭据
export function setAdminCredentials(credentials) {
  try {
    localStorage.setItem(ADMIN_CREDENTIALS_KEY, JSON.stringify(credentials));
  } catch (error) {
    console.error('保存管理员凭据失败:', error);
  }
}

// 添加新管理员
export function addAdmin(adminData) {
  const admins = getAllAdmins();
  const credentials = getAdminCredentials();
  
  // 检查用户名是否已存在
  if (admins.some(admin => admin.username === adminData.username)) {
    return { success: false, message: '用户名已存在' };
  }
  
  // 检查邮箱是否已存在
  if (admins.some(admin => admin.email === adminData.email)) {
    return { success: false, message: '邮箱已存在' };
  }
  
  // 生成新的ID
  const newId = Math.max(...admins.map(admin => admin.id), 0) + 1;
  
  // 创建新管理员
  const newAdmin = {
    id: newId,
    name: adminData.name,
    username: adminData.username,
    email: adminData.email,
    phone: adminData.phone || '',
    department: adminData.department || '宿舍管理处',
    position: adminData.position || '宿舍管理员',
    bio: adminData.bio || '负责宿舍管理工作',
    buildings: '未分配',
    status: 'active',
    type: 'admin',
    createdAt: new Date().toISOString().split('T')[0],
    lastLogin: null
  };
  
  // 添加到管理员列表
  admins.push(newAdmin);
  setAllAdmins(admins);
  
  // 添加登录凭据
  credentials[adminData.username] = {
    password: adminData.password,
    active: true
  };
  setAdminCredentials(credentials);
  
  return { success: true, admin: newAdmin };
}

// 更新管理员信息
export function updateAdmin(adminId, updateData) {
  const admins = getAllAdmins();
  const adminIndex = admins.findIndex(admin => admin.id === adminId);
  
  if (adminIndex === -1) {
    return { success: false, message: '管理员不存在' };
  }
  
  // 更新管理员信息
  const updatedAdmin = { ...admins[adminIndex], ...updateData };
  admins[adminIndex] = updatedAdmin;
  setAllAdmins(admins);
  
  // 如果更新了密码，同时更新凭据
  if (updateData.password) {
    const credentials = getAdminCredentials();
    if (credentials[updatedAdmin.username]) {
      credentials[updatedAdmin.username].password = updateData.password;
      setAdminCredentials(credentials);
    }
  }
  
  return { success: true, admin: updatedAdmin };
}

// 删除管理员
export function deleteAdmin(adminId) {
  const admins = getAllAdmins();
  const adminIndex = admins.findIndex(admin => admin.id === adminId);
  
  if (adminIndex === -1) {
    return { success: false, message: '管理员不存在' };
  }
  
  const deletedAdmin = admins[adminIndex];
  
  // 从管理员列表中移除
  admins.splice(adminIndex, 1);
  setAllAdmins(admins);
  
  // 从凭据中移除
  const credentials = getAdminCredentials();
  if (credentials[deletedAdmin.username]) {
    delete credentials[deletedAdmin.username];
    setAdminCredentials(credentials);
  }
  
  return { success: true, admin: deletedAdmin };
}

// 切换管理员状态
export function toggleAdminStatus(adminId) {
  const admins = getAllAdmins();
  const adminIndex = admins.findIndex(admin => admin.id === adminId);
  
  if (adminIndex === -1) {
    return { success: false, message: '管理员不存在' };
  }
  
  // 切换状态
  const admin = admins[adminIndex];
  admin.status = admin.status === 'active' ? 'inactive' : 'active';
  setAllAdmins(admins);
  
  // 同时更新凭据的激活状态
  const credentials = getAdminCredentials();
  if (credentials[admin.username]) {
    credentials[admin.username].active = admin.status === 'active';
    setAdminCredentials(credentials);
  }
  
  return { success: true, admin: admin };
}

// 验证管理员登录
export function validateAdminLogin(username, password) {
  const credentials = getAdminCredentials();
  const admins = getAllAdmins();
  
  // 检查凭据是否存在
  if (!credentials[username]) {
    return { success: false, message: '用户名不存在' };
  }
  
  const credential = credentials[username];
  
  // 检查密码
  if (credential.password !== password) {
    return { success: false, message: '密码错误' };
  }
  
  // 检查账号是否激活
  if (!credential.active) {
    return { success: false, message: '账号已被禁用' };
  }
  
  // 查找管理员信息
  const admin = admins.find(a => a.username === username && a.status === 'active');
  
  if (!admin) {
    return { success: false, message: '管理员账号不存在或已被禁用' };
  }
  
  // 更新最后登录时间
  admin.lastLogin = new Date().toISOString();
  setAllAdmins(admins);
  
  return { success: true, admin: admin };
}

// 根据用户名获取管理员信息
export function getAdminByUsername(username) {
  const admins = getAllAdmins();
  return admins.find(admin => admin.username === username);
}

// 检查用户名是否存在
export function checkUsernameExists(username, excludeId = null) {
  const admins = getAllAdmins();
  return admins.some(admin => admin.username === username && admin.id !== excludeId);
}

// 检查邮箱是否存在
export function checkEmailExists(email, excludeId = null) {
  const admins = getAllAdmins();
  return admins.some(admin => admin.email === email && admin.id !== excludeId);
}

// 同步管理员数据（确保数据一致性）
export function syncAdminData() {
  const admins = getAllAdmins();
  const credentials = getAdminCredentials();
  
  // 清理无效的凭据（管理员已删除）
  const validUsernames = admins.map(admin => admin.username);
  Object.keys(credentials).forEach(username => {
    if (username !== 'superadmin' && !validUsernames.includes(username)) {
      delete credentials[username];
    }
  });
  
  // 为新管理员添加默认凭据（如果缺失）
  admins.forEach(admin => {
    if (!credentials[admin.username]) {
      credentials[admin.username] = {
        password: '12345678', // 默认密码
        active: admin.status === 'active'
      };
    } else {
      // 同步激活状态
      credentials[admin.username].active = admin.status === 'active';
    }
  });
  
  setAdminCredentials(credentials);
  
  return { success: true, message: '数据同步完成' };
}

// 初始化管理员数据
export function initializeAdminData() {
  // 确保有基础数据
  getAllAdmins();
  getAdminCredentials();
  
  // 执行数据同步
  syncAdminData();
  
  console.log('管理员数据初始化完成');
}
