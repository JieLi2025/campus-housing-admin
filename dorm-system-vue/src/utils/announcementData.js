// 公告数据管理工具

// 公告数据存储键
const ANNOUNCEMENT_DATA_KEY = 'announcementData';

// 默认公告数据
const defaultAnnouncements = [
  {
    id: 1,
    title: '宿舍卫生检查通知',
    content: '本周五将进行宿舍卫生检查，请各位同学做好清洁工作。检查内容包括：床铺整理、地面清洁、物品摆放等。请同学们提前做好准备，确保宿舍环境整洁。',
    publishTime: '2024-01-15',
    status: 'active',
    expireDate: '2024-01-20'
  },
  {
    id: 2,
    title: '寒假放假安排',
    content: '寒假将于1月20日开始，请同学们提前做好离校准备。离校前请检查宿舍水电是否关闭，贵重物品请妥善保管。返校时间另行通知。',
    publishTime: '2024-01-10',
    status: 'active',
    expireDate: '2024-01-25'
  },
  {
    id: 3,
    title: '宿舍维修通知',
    content: '1号楼将进行水管维修，请同学们注意用水安全。维修期间可能会有短暂停水，请提前储备用水。维修完成后会及时通知。',
    publishTime: '2024-01-05',
    status: 'expired',
    expireDate: '2024-01-15'
  },
  {
    id: 4,
    title: '网络维护通知',
    content: '宿舍网络将于明天上午9:00-11:00进行升级维护，期间网络可能不稳定或暂时中断。请同学们合理安排上网时间。',
    publishTime: '2024-01-12',
    status: 'active',
    expireDate: '2024-01-18'
  },
  {
    id: 5,
    title: '安全提醒',
    content: '近期发现多起违规用电情况，请同学们注意用电安全。禁止使用大功率电器，如电热毯、电暖器等。如有违规将按相关规定处理。',
    publishTime: '2024-01-08',
    status: 'active',
    expireDate: '2024-01-30'
  }
];

// 获取公告数据
export function getAnnouncementData() {
  const dataStr = localStorage.getItem(ANNOUNCEMENT_DATA_KEY);
  if (dataStr) {
    try {
      const data = JSON.parse(dataStr);
      return data;
    } catch (error) {
      console.error('解析公告数据失败:', error);
      // 如果解析失败，重置为默认数据
      setAnnouncementData(defaultAnnouncements);
      return defaultAnnouncements;
    }
  }
  
  // 如果没有数据，保存默认数据
  setAnnouncementData(defaultAnnouncements);
  return defaultAnnouncements;
}

// 设置公告数据
export function setAnnouncementData(announcements) {
  localStorage.setItem(ANNOUNCEMENT_DATA_KEY, JSON.stringify(announcements));
}

// 添加新公告
export function addAnnouncement(announcement) {
  const announcements = getAnnouncementData();
  
  // 格式化过期日期
  let formattedExpireDate = null;
  if (announcement.expireDate) {
    if (announcement.expireDate instanceof Date) {
      formattedExpireDate = announcement.expireDate.toISOString().split('T')[0];
    } else if (typeof announcement.expireDate === 'string') {
      // 如果是ISO格式，提取日期部分
      if (announcement.expireDate.includes('T')) {
        formattedExpireDate = announcement.expireDate.split('T')[0];
      } else {
        formattedExpireDate = announcement.expireDate;
      }
    }
  }
  
  const newAnnouncement = {
    ...announcement,
    id: Date.now(), // 使用时间戳作为ID
    publishTime: new Date().toLocaleDateString(),
    expireDate: formattedExpireDate
  };
  announcements.unshift(newAnnouncement); // 添加到开头
  setAnnouncementData(announcements);
  return newAnnouncement;
}

// 更新公告
export function updateAnnouncement(announcementId, updatedData) {
  const announcements = getAnnouncementData();
  const index = announcements.findIndex(announcement => announcement.id === announcementId);
  if (index !== -1) {
    // 格式化过期日期
    let formattedExpireDate = null;
    if (updatedData.expireDate) {
      if (updatedData.expireDate instanceof Date) {
        formattedExpireDate = updatedData.expireDate.toISOString().split('T')[0];
      } else if (typeof updatedData.expireDate === 'string') {
        // 如果是ISO格式，提取日期部分
        if (updatedData.expireDate.includes('T')) {
          formattedExpireDate = updatedData.expireDate.split('T')[0];
        } else {
          formattedExpireDate = updatedData.expireDate;
        }
      }
    }
    
    const updatedAnnouncement = {
      ...announcements[index],
      ...updatedData,
      expireDate: formattedExpireDate !== null ? formattedExpireDate : updatedData.expireDate
    };
    
    announcements[index] = updatedAnnouncement;
    setAnnouncementData(announcements);
    return true;
  }
  return false;
}

// 删除公告
export function deleteAnnouncement(announcementId) {
  const announcements = getAnnouncementData();
  const filteredAnnouncements = announcements.filter(announcement => announcement.id !== announcementId);
  setAnnouncementData(filteredAnnouncements);
}

// 根据状态筛选公告
export function getAnnouncementsByStatus(status) {
  const announcements = getAnnouncementData();
  if (status) {
    return announcements.filter(announcement => announcement.status === status);
  }
  return announcements;
}

// 搜索公告
export function searchAnnouncements(keyword) {
  const announcements = getAnnouncementData();
  if (!keyword) {
    return announcements;
  }
  
  return announcements.filter(announcement => 
    announcement.title.includes(keyword) || 
    announcement.content.includes(keyword)
  );
}

// 获取活跃公告（已发布且未过期）
export function getActiveAnnouncements() {
  const announcements = getAnnouncementData();
  const now = new Date();
  
  return announcements.filter(announcement => {
    if (announcement.status !== 'active') {
      return false;
    }
    
    if (!announcement.expireDate) {
      return true; // 无过期日期的公告
    }
    
    const expireDate = new Date(announcement.expireDate);
    return expireDate > now;
  });
}

// 重置为默认公告数据
export function resetToDefaultAnnouncements() {
  setAnnouncementData(defaultAnnouncements);
}

// 清空所有公告数据
export function clearAllAnnouncements() {
  setAnnouncementData([]);
}
