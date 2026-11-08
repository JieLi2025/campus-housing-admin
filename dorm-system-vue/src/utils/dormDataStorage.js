// 宿舍数据管理工具

// 数据存储键
const BUILDING_DATA_KEY = 'buildingData'
const DORM_DATA_KEY = 'dormData'

// 默认楼栋数据
const defaultBuildings = [
  {
    id: 1,
    name: '1号楼',
    totalRooms: 50,
    totalBeds: 200,
    occupiedBeds: 170,
    floors: 6,
    description: '男生宿舍楼',
    createTime: new Date('2024-01-01')
  },
  {
    id: 2,
    name: '2号楼',
    totalRooms: 45,
    totalBeds: 180,
    occupiedBeds: 165,
    floors: 6,
    description: '女生宿舍楼',
    createTime: new Date('2024-01-01')
  }
]

// 默认宿舍数据
const defaultDorms = [
  { id: 1, buildingId: 1, building: '1号楼', room: '101', type: '四人间', totalBeds: 4, occupiedBeds: 4, status: 'full', floor: 1 },
  { id: 2, buildingId: 1, building: '1号楼', room: '102', type: '四人间', totalBeds: 4, occupiedBeds: 3, status: 'available', floor: 1 },
  { id: 3, buildingId: 2, building: '2号楼', room: '201', type: '六人间', totalBeds: 6, occupiedBeds: 5, status: 'available', floor: 2 }
]

// 楼栋管理函数
export function getBuildingData() {
  const data = localStorage.getItem(BUILDING_DATA_KEY)
  if (data) {
    return JSON.parse(data)
  }
  setBuildingData(defaultBuildings)
  return defaultBuildings
}

export function setBuildingData(buildings) {
  localStorage.setItem(BUILDING_DATA_KEY, JSON.stringify(buildings))
}

export function addBuilding(building) {
  const buildings = getBuildingData()
  const newBuilding = {
    ...building,
    id: Date.now(),
    totalRooms: 0,
    totalBeds: 0,
    occupiedBeds: 0,
    createTime: new Date()
  }
  buildings.push(newBuilding)
  setBuildingData(buildings)
  return newBuilding
}

export function updateBuilding(id, updatedBuilding) {
  const buildings = getBuildingData()
  const index = buildings.findIndex(b => b.id == id)
  if (index !== -1) {
    buildings[index] = { ...buildings[index], ...updatedBuilding }
    setBuildingData(buildings)
    return buildings[index]
  }
  return null
}

export function deleteBuilding(id) {
  const buildings = getBuildingData()
  const filteredBuildings = buildings.filter(b => b.id != id)
  setBuildingData(filteredBuildings)
  
  // 同时删除该楼栋下的所有宿舍
  const dorms = getDormData()
  const filteredDorms = dorms.filter(d => d.buildingId != id)
  setDormData(filteredDorms)
  
  return true
}

// 宿舍管理函数
export function getDormData() {
  const data = localStorage.getItem(DORM_DATA_KEY)
  if (data) {
    return JSON.parse(data)
  }
  setDormData(defaultDorms)
  return defaultDorms
}

export function setDormData(dorms) {
  localStorage.setItem(DORM_DATA_KEY, JSON.stringify(dorms))
}

export function addDorm(dorm) {
  const dorms = getDormData()
  const newDorm = {
    ...dorm,
    id: Date.now(),
    occupiedBeds: 0,
    status: 'available'
  }
  dorms.push(newDorm)
  setDormData(dorms)
  updateBuildingStats(dorm.buildingId)
  return newDorm
}

export function updateDorm(id, updatedDorm) {
  const dorms = getDormData()
  const index = dorms.findIndex(d => d.id == id)
  if (index !== -1) {
    const oldBuildingId = dorms[index].buildingId
    dorms[index] = { ...dorms[index], ...updatedDorm }
    
    if (dorms[index].occupiedBeds >= dorms[index].totalBeds) {
      dorms[index].status = 'full'
    } else {
      dorms[index].status = 'available'
    }
    
    setDormData(dorms)
    updateBuildingStats(oldBuildingId)
    if (updatedDorm.buildingId && updatedDorm.buildingId != oldBuildingId) {
      updateBuildingStats(updatedDorm.buildingId)
    }
    return dorms[index]
  }
  return null
}

export function deleteDorm(id) {
  const dorms = getDormData()
  const dorm = dorms.find(d => d.id == id)
  const filteredDorms = dorms.filter(d => d.id != id)
  setDormData(filteredDorms)
  
  if (dorm) {
    updateBuildingStats(dorm.buildingId)
  }
  return true
}

// 批量添加宿舍
export function batchAddDorms(buildingId, startFloor, endFloor, roomsPerFloor, roomType, totalBeds) {
  const dorms = getDormData()
  const building = getBuildingData().find(b => b.id == buildingId)
  
  if (!building) {
    throw new Error('楼栋不存在')
  }
  
  const newDorms = []
  
  for (let floor = startFloor; floor <= endFloor; floor++) {
    for (let room = 1; room <= roomsPerFloor; room++) {
      const roomNumber = floor * 100 + room
      const existingDorm = dorms.find(d => d.buildingId == buildingId && d.room == roomNumber.toString())
      
      if (!existingDorm) {
        const newDorm = {
          id: Date.now() + newDorms.length,
          buildingId: buildingId,
          building: building.name,
          room: roomNumber.toString(),
          type: roomType,
          totalBeds: totalBeds,
          occupiedBeds: 0,
          status: 'available',
          floor: floor
        }
        newDorms.push(newDorm)
      }
    }
  }
  
  dorms.push(...newDorms)
  setDormData(dorms)
  updateBuildingStats(buildingId)
  return newDorms
}

// 更新楼栋统计信息
function updateBuildingStats(buildingId) {
  const dorms = getDormData()
  const buildingDorms = dorms.filter(d => d.buildingId == buildingId)
  
  const totalRooms = buildingDorms.length
  const totalBeds = buildingDorms.reduce((sum, dorm) => sum + dorm.totalBeds, 0)
  const occupiedBeds = buildingDorms.reduce((sum, dorm) => sum + dorm.occupiedBeds, 0)
  
  updateBuilding(buildingId, {
    totalRooms,
    totalBeds,
    occupiedBeds
  })
}

// 搜索宿舍
export function searchDorms(keyword, buildingId) {
  const dorms = getDormData()
  let filteredDorms = dorms
  
  if (keyword) {
    filteredDorms = filteredDorms.filter(dorm => 
      dorm.room.includes(keyword) || 
      dorm.building.includes(keyword) ||
      dorm.type.includes(keyword)
    )
  }
  
  if (buildingId) {
    filteredDorms = filteredDorms.filter(dorm => dorm.buildingId == buildingId)
  }
  
  return filteredDorms
}

// 获取楼栋统计信息
export function getBuildingStats(buildingId) {
  const dorms = getDormData()
  const buildingDorms = dorms.filter(d => d.buildingId == buildingId)
  
  const totalRooms = buildingDorms.length
  const totalBeds = buildingDorms.reduce((sum, dorm) => sum + dorm.totalBeds, 0)
  const occupiedBeds = buildingDorms.reduce((sum, dorm) => sum + dorm.occupiedBeds, 0)
  const occupancyRate = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0
  
  return {
    totalRooms,
    totalBeds,
    occupiedBeds,
    occupancyRate
  }
}
