// 楼栋管理工具

// 楼栋数据存储
const BUILDING_STORAGE_KEY = 'buildingAssignments';
const BUILDING_AREAS_STORAGE_KEY = 'buildingAreas'; // 区域数据存储key

// 所有楼栋数据
const allBuildings = [
  {
    id: 1,
    name: '1号楼',
    totalRooms: 50,
    occupancyRate: 85,
    status: 'active',
    description: '男生宿舍楼',
    gender: '男',
    maxFloors: 4,
    areaAssignments: {
      // 区域分配：按专业/班级划分
      areas: [
        {
          id: 'area_1_1',
          name: '计算机学院区域',
          floors: [1, 2],
          majors: ['计算机科学与技术', '软件工程', '网络工程'],
          classes: ['计科2021-1班', '计科2021-2班', '软工2021-1班', '网工2021-1班'],
          rooms: ['101', '102', '103', '201', '202', '203']
        },
        {
          id: 'area_1_2', 
          name: '电子信息学院区域',
          floors: [3, 4],
          majors: ['电子信息工程', '通信工程', '自动化'],
          classes: ['电信2021-1班', '通信2021-1班', '自动2021-1班'],
          rooms: ['301', '302', '303', '401', '402', '403']
        }
      ]
    }
  },
  {
    id: 2,
    name: '2号楼',
    totalRooms: 45,
    occupancyRate: 92,
    status: 'active',
    description: '女生宿舍楼',
    gender: '女',
    maxFloors: 4,
    areaAssignments: {
      areas: [
        {
          id: 'area_2_1',
          name: '文学院区域',
          floors: [1, 2],
          majors: ['汉语言文学', '英语', '新闻学'],
          classes: ['中文2021-1班', '英语2021-1班', '新闻2021-1班'],
          rooms: ['101', '102', '103', '201', '202', '203']
        },
        {
          id: 'area_2_2',
          name: '管理学院区域', 
          floors: [3, 4],
          majors: ['工商管理', '会计学', '市场营销'],
          classes: ['工商2021-1班', '会计2021-1班', '营销2021-1班'],
          rooms: ['301', '302', '303', '401', '402', '403']
        }
      ]
    }
  },
  {
    id: 3,
    name: '3号楼',
    totalRooms: 60,
    occupancyRate: 78,
    status: 'active',
    description: '理工科男生楼',
    gender: '男',
    maxFloors: 3,
    areaAssignments: {
      areas: [
        {
          id: 'area_3_1',
          name: '机械工程学院区域',
          floors: [1, 2, 3],
          majors: ['机械工程', '材料科学与工程', '工业设计'],
          classes: ['机械2021-1班', '材料2021-1班', '工设2021-1班'],
          rooms: ['101', '102', '103', '201', '202', '203', '301', '302', '303']
        }
      ]
    }
  },
  {
    id: 4,
    name: '4号楼',
    totalRooms: 40,
    occupancyRate: 95,
    status: 'active',
    description: '研究生男生宿舍楼',
    gender: '男',
    maxFloors: 4,
    areaAssignments: {
      areas: [
        {
          id: 'area_4_1',
          name: '研究生男生区域',
          floors: [1, 2, 3, 4],
          majors: ['所有专业'],
          classes: ['研究生'],
          rooms: ['101', '102', '103', '201', '202', '203', '301', '302', '303', '401', '402', '403']
        }
      ]
    }
  },
  {
    id: 5,
    name: '5号楼',
    totalRooms: 55,
    occupancyRate: 88,
    status: 'active',
    description: '留学生女生宿舍楼',
    gender: '女',
    maxFloors: 5,
    areaAssignments: {
      areas: [
        {
          id: 'area_5_1',
          name: '留学生女生区域',
          floors: [1, 2, 3, 4, 5],
          majors: ['所有专业'],
          classes: ['留学生'],
          rooms: ['101', '102', '103', '201', '202', '203', '301', '302', '303', '401', '402', '403', '501', '502', '503']
        }
      ]
    }
  }
];

// 初始化楼栋区域数据（从 localStorage 加载或使用默认值）
function initializeBuildingAreas() {
  const storedAreas = localStorage.getItem(BUILDING_AREAS_STORAGE_KEY);
  if (storedAreas) {
    try {
      const areasData = JSON.parse(storedAreas);
      // 将存储的区域数据恢复到 allBuildings 中
      allBuildings.forEach(building => {
        if (areasData[building.id]) {
          building.areaAssignments = { areas: areasData[building.id] };
        }
      });
      console.log('✅ 从 localStorage 加载区域数据成功');
    } catch (error) {
      console.error('加载区域数据失败:', error);
    }
  } else {
    // 第一次使用，保存默认数据
    saveBuildingAreas();
    console.log('✅ 初始化默认区域数据');
  }
}

// 保存楼栋区域数据到 localStorage
function saveBuildingAreas() {
  const areasData = {};
  allBuildings.forEach(building => {
    if (building.areaAssignments && building.areaAssignments.areas) {
      areasData[building.id] = building.areaAssignments.areas;
    }
  });
  localStorage.setItem(BUILDING_AREAS_STORAGE_KEY, JSON.stringify(areasData));
  console.log('💾 区域数据已保存到 localStorage');
}

// 初始化区域数据
initializeBuildingAreas();

// 获取楼栋分配数据
export function getBuildingAssignments() {
  const assignmentsStr = localStorage.getItem(BUILDING_STORAGE_KEY);
  if (assignmentsStr) {
    return JSON.parse(assignmentsStr);
  }
  // 默认分配
  const defaultAssignments = {
    'admin': [1, 2],      // 张管理员管理1号楼和2号楼
    'admin2': [3],        // 李管理员管理3号楼
    'admin3': [4],        // 王管理员管理4号楼
    'admin4': [5]         // 赵管理员管理5号楼
  };
  setBuildingAssignments(defaultAssignments);
  return defaultAssignments;
}

// 设置楼栋分配数据
export function setBuildingAssignments(assignments) {
  localStorage.setItem(BUILDING_STORAGE_KEY, JSON.stringify(assignments));
}

// 获取指定管理员管理的楼栋
export function getAdminBuildings(adminUsername) {
  const assignments = getBuildingAssignments();
  const buildingIds = assignments[adminUsername] || [];
  return allBuildings.filter(building => buildingIds.includes(building.id));
}

// 获取所有楼栋
export function getAllBuildings() {
  return allBuildings;
}

// 更新楼栋分配
export function updateBuildingAssignment(adminUsername, buildingIds) {
  const assignments = getBuildingAssignments();
  assignments[adminUsername] = buildingIds;
  setBuildingAssignments(assignments);
}

// 获取未分配的楼栋
export function getUnassignedBuildings() {
  const assignments = getBuildingAssignments();
  const assignedBuildingIds = Object.values(assignments).flat();
  return allBuildings.filter(building => !assignedBuildingIds.includes(building.id));
}

// 获取楼栋详细信息
export function getBuildingById(buildingId) {
  return allBuildings.find(building => building.id === buildingId);
}

// 区域管理相关函数

// 获取楼栋的区域分配
export function getBuildingAreas(buildingId) {
  // 确保先初始化区域数据
  initializeBuildingAreas();
  const building = getBuildingById(buildingId);
  return building?.areaAssignments?.areas || [];
}

// 添加区域到楼栋
export function addAreaToBuilding(buildingId, areaData) {
  const buildingIndex = allBuildings.findIndex(building => building.id === buildingId);
  if (buildingIndex === -1) return false;
  
  if (!allBuildings[buildingIndex].areaAssignments) {
    allBuildings[buildingIndex].areaAssignments = { areas: [] };
  }
  
  const newArea = {
    id: `area_${buildingId}_${Date.now()}`,
    name: areaData.name,
    floors: areaData.floors,
    majors: areaData.majors || [],
    classes: areaData.classes || [],
    rooms: areaData.rooms || [],
    gender: areaData.gender || null
  };
  
  allBuildings[buildingIndex].areaAssignments.areas.push(newArea);
  
  // 保存到 localStorage
  saveBuildingAreas();
  
  return newArea;
}

// 更新区域信息
export function updateBuildingArea(buildingId, areaId, areaData) {
  const building = allBuildings.find(building => building.id === buildingId);
  if (!building || !building.areaAssignments) return false;
  
  const areaIndex = building.areaAssignments.areas.findIndex(area => area.id === areaId);
  if (areaIndex === -1) return false;
  
  building.areaAssignments.areas[areaIndex] = {
    ...building.areaAssignments.areas[areaIndex],
    ...areaData
  };
  
  // 保存到 localStorage
  saveBuildingAreas();
  
  return true;
}

// 删除区域
export function removeAreaFromBuilding(buildingId, areaId) {
  const building = allBuildings.find(building => building.id === buildingId);
  if (!building || !building.areaAssignments) return false;
  
  const areaIndex = building.areaAssignments.areas.findIndex(area => area.id === areaId);
  if (areaIndex === -1) return false;
  
  building.areaAssignments.areas.splice(areaIndex, 1);
  
  // 保存到 localStorage  ✅ 关键修复：删除后立即保存
  saveBuildingAreas();
  
  return true;
}

// 根据学生信息推荐宿舍区域
export function recommendAreaForStudent(studentInfo) {
  const { gender, major, className } = studentInfo;
  const recommendations = [];
  
  allBuildings.forEach(building => {
    // 首先按性别过滤楼栋
    if (building.gender !== '混合' && building.gender !== gender) {
      return;
    }
    
    building.areaAssignments?.areas?.forEach(area => {
      let score = 0;
      
      // 性别匹配（对于混合楼栋中的区域）
      if (area.gender && area.gender === gender) {
        score += 50;
      } else if (!area.gender && building.gender === gender) {
        score += 50;
      }
      
      // 专业匹配
      if (area.majors.includes(major) || area.majors.includes('所有专业')) {
        score += 30;
      }
      
      // 班级匹配
      if (area.classes.includes(className) || area.classes.includes('所有班级')) {
        score += 20;
      }
      
      if (score > 0) {
        recommendations.push({
          buildingId: building.id,
          buildingName: building.name,
          areaId: area.id,
          areaName: area.name,
          score: score,
          availableRooms: area.rooms
        });
      }
    });
  });
  
  // 按分数排序，分数高的优先
  return recommendations.sort((a, b) => b.score - a.score);
}

// 获取所有专业列表（用于下拉选择）
export function getAllMajors() {
  const majors = new Set();
  allBuildings.forEach(building => {
    building.areaAssignments?.areas?.forEach(area => {
      area.majors.forEach(major => {
        if (major !== '所有专业') {
          majors.add(major);
        }
      });
    });
  });
  return Array.from(majors).sort();
}

// 获取所有班级列表（用于下拉选择）
export function getAllClasses() {
  const classes = new Set();
  allBuildings.forEach(building => {
    building.areaAssignments?.areas?.forEach(area => {
      area.classes.forEach(className => {
        if (className !== '所有班级' && className !== '研究生' && className !== '留学生') {
          classes.add(className);
        }
      });
    });
  });
  return Array.from(classes).sort();
}

// 更新楼栋名称（超级管理员功能）
export function updateBuildingName(buildingId, newName) {
  const buildingIndex = allBuildings.findIndex(building => building.id === buildingId);
  if (buildingIndex === -1) return false;
  
  allBuildings[buildingIndex].name = newName;
  return true;
}
