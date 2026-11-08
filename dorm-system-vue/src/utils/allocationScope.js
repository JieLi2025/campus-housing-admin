/**
 * allocationScope.js
 * 从 buildingMajorAssignmentsV2 生成普通管理员可用的“分配范围 chips 数据”。
 * 说明：
 * - 只读取超级管理员已保存的分配结果，不涉及已入住/已分配房间的动态扣减（后续步骤再叠加）。
 * - 班级唯一键优先使用 classId；如果没有，则以 className 作为键。
 */

/* eslint-disable */

/**
 * 读取 localStorage JSON
 */
function readLS(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw);
  } catch (e) {
    console.warn('[allocationScope] readLS error', key, e);
    return fallback;
  }
}

/**
 * 从 buildingMajorAssignmentsV2 中提取某楼栋的分配范围
 * @param {string|number} buildingId
 * @returns {{
 *   professionalChips: Array<{majorId?: string, majorName: string, totalSeats: number, usedSeats: number, remainingSeats: number, mode: 'full-major'|'class-only'}>,
 *   classChipsByMajor: Record<string, Array<{classKey: string, className: string, totalSeats: number, usedSeats: number, remainingSeats: number}>>,
 *   raw?: any,
 *   generatedAt: number
 * }}
 */
export function getBuildingAllocationScope(buildingId) {
  const store = readLS('buildingMajorAssignmentsV2', {});
  const payload = store?.[buildingId];
  const result = {
    professionalChips: [],
    classChipsByMajor: {},
    raw: payload,
    generatedAt: Date.now()
  };

  if (!payload || !Array.isArray(payload.allocations)) return result;

  console.log('🔍 [allocationScope] 开始处理楼栋', buildingId, '的分配数据');
  console.log('📦 allocations 数量:', payload.allocations.length);

  payload.allocations.forEach((al, index) => {
    // 兼容：没有 mode 时以 details 判定
    const mode = al.mode || (Array.isArray(al.details) && al.details.length > 0 ? 'class-only' : 'full-major');
    console.log(`\n[${index + 1}] 专业: ${al.majorName}, mode: ${mode}, details:`, al.details?.length || 0);
    
    if (mode === 'full-major') {
      // 整专业模式：创建专业级别的卡片
      const totalSeats = Number(al.students || 0);
      result.professionalChips.push({
        majorId: al.majorId,
        majorName: al.majorName,
        totalSeats,
        usedSeats: 0,
        remainingSeats: totalSeats,
        mode: 'full-major'
      });
    } else {
      // 按班级模式：为每个班级创建独立的卡片
      (al.details || []).forEach((d, idx) => {
        const classKey = d.classId ?? d.className ?? String(idx);
        const className = d.className ?? `班级${idx + 1}`;
        // 🔧 修复：如果students为0，使用rooms*4估算学生人数
        const rooms = Number(d.rooms || 0);
        const seats = Number(d.students || 0) || (rooms * 4);
        const grade = d.grade || '';  // 获取年级信息
        
        // 创建班级级别的卡片，显示"专业 - 班级"
        const chipName = `${al.majorName} - ${className}`;
        console.log(`   ➕ 创建班级卡片: ${chipName} (${seats}人, rooms=${rooms}, 年级: ${grade})`);
        result.professionalChips.push({
          majorId: al.majorId,
          majorName: chipName, // 🔥 关键：显示专业+班级
          originalMajorName: al.majorName, // 保留原始专业名
          className: className, // 保留班级名
          classKey: classKey,
          grade: grade,  // 添加年级字段
          totalSeats: seats,
          usedSeats: 0,
          remainingSeats: seats,
          mode: 'class-only'
        });
      });
      
      // 同时保留班级数据到 classChipsByMajor 供其他地方使用
      const list = (al.details || []).map((d, idx) => {
        const classKey = d.classId ?? d.className ?? String(idx);
        const className = d.className ?? `班级${idx + 1}`;
        const seats = Number(d.students || 0);
        return {
          classKey,
          className,
          totalSeats: seats,
          usedSeats: 0,
          remainingSeats: seats
        };
      });
      result.classChipsByMajor[al.majorId || al.majorName] = list;
    }
  });

  console.log('\n✅ 最终生成的卡片:');
  result.professionalChips.forEach((chip, idx) => {
    console.log(`   ${idx + 1}. ${chip.majorName} (${chip.totalSeats}人, mode: ${chip.mode})`);
  });

  return result;
}

export default {
  getBuildingAllocationScope
};


