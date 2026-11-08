package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.BuildingMajorAssignment;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.BuildingMajorAssignmentMapper;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 楼栋专业分配服务
 * 
 * @author Campus Dorm System
 * @date 2025-10-27
 */
@Service
@Slf4j
public class BuildingMajorAssignmentService {
    
    @Autowired
    private BuildingMajorAssignmentMapper assignmentMapper;
    
    @Autowired
    private BuildingMapper buildingMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    /**
     * 批量设置楼栋的专业分配
     * 
     * @param buildingId 楼栋ID
     * @param assignments 专业分配列表
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> batchSaveAssignments(Long buildingId, List<Map<String, Object>> assignments) {
        log.info("批量保存楼栋{}的专业分配，数量：{}", buildingId, assignments.size());
        
        // 1. 验证楼栋存在
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return Result.error("楼栋不存在");
        }
        
        // 获取楼栋性别（用于验证学生数）
        String buildingGender = building.getBuildingType(); // "男" 或 "女"
        log.info("楼栋{}的性别类型：{}", buildingId, buildingGender);
        
        // 2. 🔥 关键修复：清除该楼栋中不再属于新分配范围的学生的宿舍信息
        clearOrphanedStudentAllocations(buildingId, assignments);
        
        // 3. 删除该楼栋的旧分配
        LambdaQueryWrapper<BuildingMajorAssignment> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(BuildingMajorAssignment::getBuildingId, buildingId);
        int deletedCount = assignmentMapper.delete(deleteWrapper);
        log.info("删除楼栋{}的旧专业分配：{}条", buildingId, deletedCount);
        
        // 4. 插入新分配并记录分片信息
        int successCount = 0;
        List<StudentSegmentUpdate> segmentUpdates = new ArrayList<>();
        
        for (Map<String, Object> assignmentData : assignments) {
            BuildingMajorAssignment assignment = new BuildingMajorAssignment();
            assignment.setBuildingId(buildingId);
            assignment.setMajorName((String) assignmentData.get("majorName"));
            assignment.setClassName((String) assignmentData.getOrDefault("className", ""));
            assignment.setClassSegment((String) assignmentData.get("classSegment")); // 新增：班级分片
            assignment.setGrade((String) assignmentData.getOrDefault("grade", ""));
            assignment.setGender((String) assignmentData.getOrDefault("gender", ""));
            assignment.setAssignmentMode((String) assignmentData.getOrDefault("assignmentMode", "class-only"));
            assignment.setStudentIdRange((String) assignmentData.get("studentIdRange")); // 新增：学号范围
            assignment.setRoomType((String) assignmentData.get("roomType")); // 房间类型（中文格式，如"二人间"、"四人间"）
            assignment.setStatus(1);
            
            // 🔥 关键修复：验证并修正 allocatedStudents 字段
            // 从数据库查询实际符合条件的学生数（按性别、专业、班级、年级过滤）
            int frontendAllocatedStudents = getIntValue(assignmentData, "allocatedStudents", 0);
            int actualStudentCount = countActualStudents(
                assignment.getMajorName(),
                assignment.getClassName(),
                assignment.getGrade(),
                buildingGender,
                assignment.getClassSegment(),
                assignmentData
            );
            
            // 如果前端传递的学生数与实际学生数不一致，使用实际学生数
            if (frontendAllocatedStudents != actualStudentCount) {
                log.warn("⚠️ 专业分配学生数不一致：{}-{}-{}，前端传递={}，实际学生数={}，使用实际学生数",
                    assignment.getMajorName(), assignment.getClassName(), assignment.getGrade(),
                    frontendAllocatedStudents, actualStudentCount);
            }
            assignment.setAllocatedStudents(actualStudentCount);
            assignment.setAllocatedRooms(getIntValue(assignmentData, "allocatedRooms", 0));
            
            int result = assignmentMapper.insert(assignment);
            if (result > 0) {
                successCount++;
                
                // 收集需要更新分片的学生信息
                if (assignmentData.get("studentIds") != null) {
                    @SuppressWarnings("unchecked")
                    List<?> rawIds = (List<?>) assignmentData.get("studentIds");
                    // 将Integer或Long统一转换为Long类型
                    List<Long> studentIds = rawIds.stream()
                        .map(id -> id instanceof Integer ? ((Integer) id).longValue() : (Long) id)
                        .collect(java.util.stream.Collectors.toList());
                    String segment = (String) assignmentData.get("classSegment");
                    
                    segmentUpdates.add(new StudentSegmentUpdate(studentIds, segment));
                }
            }
        }
        
        // 4. 批量更新学生的 class_segment 字段
        if (!segmentUpdates.isEmpty()) {
            log.info("开始批量更新学生分片信息，共{}组", segmentUpdates.size());
            int updatedStudents = updateStudentSegments(segmentUpdates);
            log.info("学生分片信息更新完成：{}名学生", updatedStudents);
        }
        
        log.info("专业分配保存完成：成功{}条", successCount);
        return Result.success("专业分配保存成功", successCount);
    }
    
    /**
     * 获取楼栋的专业分配列表
     */
    public Result<List<BuildingMajorAssignment>> getAssignmentsByBuilding(Long buildingId) {
        log.info("获取楼栋{}的专业分配", buildingId);
        
        List<BuildingMajorAssignment> assignments = assignmentMapper.selectByBuildingId(buildingId);
        
        log.info("查询到{}条专业分配", assignments.size());
        return Result.success(assignments);
    }
    
    /**
     * 删除专业分配
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteAssignment(Long assignmentId) {
        log.info("删除专业分配：id={}", assignmentId);
        
        int result = assignmentMapper.deleteById(assignmentId);
        if (result > 0) {
            log.info("专业分配删除成功");
            return Result.success("专业分配删除成功");
        }
        
        return Result.error("专业分配删除失败");
    }
    
    /**
     * 获取所有专业分配（Map格式）
     * 返回格式：{ buildingId: [{majorName, capacity, ...}, ...] }
     */
    public Result<Map<Long, List<Map<String, Object>>>> getAllAssignmentsMap() {
        log.info("获取所有专业分配（Map格式）");
        
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> allAssignments = assignmentMapper.selectList(wrapper);
        
        Map<Long, List<Map<String, Object>>> result = new HashMap<>();
        
        for (BuildingMajorAssignment assignment : allAssignments) {
            Long buildingId = assignment.getBuildingId();
            if (!result.containsKey(buildingId)) {
                result.put(buildingId, new ArrayList<>());
            }
            
            Map<String, Object> assignmentMap = new HashMap<>();
            assignmentMap.put("id", assignment.getId());
            assignmentMap.put("majorName", assignment.getMajorName());
            assignmentMap.put("className", assignment.getClassName());
            assignmentMap.put("grade", assignment.getGrade());
            assignmentMap.put("gender", assignment.getGender());
            assignmentMap.put("capacity", assignment.getAllocatedStudents());
            assignmentMap.put("allocatedRooms", assignment.getAllocatedRooms());
            assignmentMap.put("assignmentMode", assignment.getAssignmentMode());
            
            result.get(buildingId).add(assignmentMap);
        }
        
        log.info("查询到{}个楼栋的专业分配", result.size());
        return Result.success(result);
    }
    
    /**
     * 获取各专业剩余学生数
     * 返回格式：{ "男": { "专业名": 剩余人数, ... }, "女": { "专业名": 剩余人数, ... } }
     */
    public Result<Map<String, Map<String, Integer>>> getRemainingStudentsByMajor() {
        log.info("计算各专业剩余学生数");
        
        // 1. 查询所有学生，按性别和专业统计总数
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getDeleted, 0); // 只查未删除的学生
        List<Student> allStudents = studentMapper.selectList(studentWrapper);
        
        Map<String, Map<String, Integer>> totalByMajor = new HashMap<>();
        totalByMajor.put("男", new HashMap<>());
        totalByMajor.put("女", new HashMap<>());
        
        for (Student student : allStudents) {
            String genderDb = student.getGender(); // 现在已经是中文"男"或"女"
            String major = student.getMajor();
            
            if (genderDb == null || major == null) {
                continue;
            }
            
            // 直接使用中文性别值（数据库已更新为中文）
            String gender = genderDb;
            
            Map<String, Integer> genderMap = totalByMajor.get(gender);
            if (genderMap == null) {
                genderMap = new HashMap<>();
                totalByMajor.put(gender, genderMap);
            }
            
            genderMap.put(major, genderMap.getOrDefault(major, 0) + 1);
        }
        
        log.info("学生总数统计：男生专业数={}, 女生专业数={}", 
                totalByMajor.get("男").size(), totalByMajor.get("女").size());
        
        // 2. 查询所有专业分配，统计已分配的学生数
        LambdaQueryWrapper<BuildingMajorAssignment> assignmentWrapper = new LambdaQueryWrapper<>();
        assignmentWrapper.eq(BuildingMajorAssignment::getStatus, 1);
        List<BuildingMajorAssignment> allAssignments = assignmentMapper.selectList(assignmentWrapper);
        
        Map<String, Map<String, Integer>> allocatedByMajor = new HashMap<>();
        allocatedByMajor.put("男", new HashMap<>());
        allocatedByMajor.put("女", new HashMap<>());
        
        for (BuildingMajorAssignment assignment : allAssignments) {
            // 获取该分配对应的楼栋性别
            Building building = buildingMapper.selectById(assignment.getBuildingId());
            if (building == null) {
                continue;
            }
            
            String buildingType = building.getBuildingType(); // "男" 或 "女" 或 "混合"
            String gender = buildingType; // 现在buildingType已经是中文了，直接使用
            String major = assignment.getMajorName();
            int allocatedStudents = assignment.getAllocatedStudents() != null ? assignment.getAllocatedStudents() : 0;
            
            Map<String, Integer> genderMap = allocatedByMajor.get(gender);
            if (genderMap == null) {
                genderMap = new HashMap<>();
                allocatedByMajor.put(gender, genderMap);
            }
            
            genderMap.put(major, genderMap.getOrDefault(major, 0) + allocatedStudents);
        }
        
        log.info("已分配学生统计：男生专业数={}, 女生专业数={}", 
                allocatedByMajor.get("男").size(), allocatedByMajor.get("女").size());
        
        // 3. 计算剩余学生数 = 总数 - 已分配数
        Map<String, Map<String, Integer>> remainingByMajor = new HashMap<>();
        
        for (String gender : totalByMajor.keySet()) {
            Map<String, Integer> totalMap = totalByMajor.get(gender);
            Map<String, Integer> allocatedMap = allocatedByMajor.getOrDefault(gender, new HashMap<>());
            Map<String, Integer> remainingMap = new HashMap<>();
            
            for (String major : totalMap.keySet()) {
                int total = totalMap.get(major);
                int allocated = allocatedMap.getOrDefault(major, 0);
                int remaining = Math.max(0, total - allocated);
                remainingMap.put(major, remaining);
            }
            
            remainingByMajor.put(gender, remainingMap);
        }
        
        log.info("剩余学生数计算完成");
        return Result.success(remainingByMajor);
    }
    
    /**
     * 批量更新学生的班级分片标识
     * 用于班级拆分后，将学生标记为属于哪个分片（A/B/C）
     * 
     * @param updates 更新列表
     * @return 更新的学生数
     */
    private int updateStudentSegments(List<StudentSegmentUpdate> updates) {
        int totalUpdated = 0;
        
        for (StudentSegmentUpdate update : updates) {
            if (update.getStudentIds() == null || update.getStudentIds().isEmpty()) {
                continue;
            }
            
            // 批量更新这组学生的 class_segment
            for (Long studentId : update.getStudentIds()) {
                Student student = studentMapper.selectById(studentId);
                if (student != null) {
                    student.setClassSegment(update.getSegment());
                    studentMapper.updateById(student);
                    totalUpdated++;
                }
            }
        }
        
        return totalUpdated;
    }
    
    /**
     * 清除该楼栋中不再属于新分配范围的学生的宿舍信息
     * 
     * @param buildingId 楼栋ID
     * @param newAssignments 新的专业分配列表
     */
    private void clearOrphanedStudentAllocations(Long buildingId, List<Map<String, Object>> newAssignments) {
        log.info("🔍 检查楼栋{}中需要清除宿舍分配的学生", buildingId);
        
        // 1. 查询该楼栋中所有已分配宿舍的学生
        // 通过dormNumber字段判断（格式：楼栋名-房间号-床位号）
        Building building = buildingMapper.selectById(buildingId);
        if (building == null) {
            return;
        }
        
        String buildingName = building.getBuildingName();
        LambdaQueryWrapper<Student> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(Student::getDeleted, 0)
                     .like(Student::getRoomNumber, buildingName + "-"); // roomNumber以"楼栋名-"开头
        List<Student> studentsInBuilding = studentMapper.selectList(studentWrapper);
        
        log.info("楼栋{}中共有{}名已分配宿舍的学生", buildingName, studentsInBuilding.size());
        
        if (studentsInBuilding.isEmpty()) {
            return;
        }
        
        // 2. 构建新分配范围的专业/班级/分片集合
        java.util.Set<String> newAllocationKeys = new java.util.HashSet<>();
        for (Map<String, Object> assignment : newAssignments) {
            String majorName = (String) assignment.get("majorName");
            String className = (String) assignment.getOrDefault("className", "");
            String classSegment = (String) assignment.get("classSegment");
            
            // 构建唯一键：专业-班级-分片
            String key = classSegment != null 
                ? majorName + "-" + className + "-" + classSegment
                : majorName + "-" + className;
            newAllocationKeys.add(key);
        }
        
        log.info("新分配范围包含{}个专业/班级/分片组合", newAllocationKeys.size());
        
        // 3. 检查每个学生是否还在新分配范围内
        int clearedCount = 0;
        for (Student student : studentsInBuilding) {
            String studentKey = student.getClassSegment() != null
                ? student.getMajor() + "-" + student.getClassName() + "-" + student.getClassSegment()
                : student.getMajor() + "-" + student.getClassName();
            
            // 如果学生不在新分配范围内，清除其宿舍信息
            if (!newAllocationKeys.contains(studentKey)) {
                log.warn("⚠️ 学生{}（{}）的{}不在新分配范围内，清除其宿舍分配：{}", 
                    student.getRealName(), 
                    student.getStudentId(),
                    studentKey,
                    student.getRoomNumber());
                
                // 清除宿舍信息
                student.setRoomNumber(null);
                student.setRoomId(null);
                student.setDormGroupId(null);
                student.setClassSegment(null); // 也清除分片标识
                studentMapper.updateById(student);
                clearedCount++;
            }
        }
        
        log.info("✅ 清除了{}名学生的宿舍分配信息", clearedCount);
    }
    
    /**
     * 🔥 关键方法：计算实际符合条件的学生数（按性别、专业、班级、年级过滤）
     * 用于验证前端传递的学生数是否正确
     * 
     * @param majorName 专业名称
     * @param className 班级名称
     * @param grade 年级
     * @param gender 性别（楼栋性别）
     * @param classSegment 班级分片标识
     * @param assignmentData 分配数据（包含studentIds）
     * @return 实际学生数
     */
    private int countActualStudents(String majorName, String className, String grade, 
                                    String gender, String classSegment, Map<String, Object> assignmentData) {
        // 如果前端传递了studentIds，直接使用其数量（已经是按性别过滤后的）
        if (assignmentData.get("studentIds") != null) {
            @SuppressWarnings("unchecked")
            List<?> studentIds = (List<?>) assignmentData.get("studentIds");
            if (studentIds != null && !studentIds.isEmpty()) {
                log.info("使用前端传递的studentIds数量：{}", studentIds.size());
                return studentIds.size();
            }
        }
        
        // 否则，从数据库查询实际符合条件的学生数
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getDeleted, 0);
        
        // 按专业过滤
        if (majorName != null && !majorName.isEmpty()) {
            wrapper.eq(Student::getMajor, majorName);
        }
        
        // 按班级过滤
        if (className != null && !className.isEmpty()) {
            wrapper.eq(Student::getClassName, className);
        }
        
        // 按年级过滤
        if (grade != null && !grade.isEmpty()) {
            wrapper.eq(Student::getGrade, grade);
        }
        
        // 🔥 关键：按性别过滤（使用楼栋性别）
        if (gender != null && !gender.isEmpty()) {
            wrapper.eq(Student::getGender, gender);
        }
        
        // 按分片过滤（如果有）
        if (classSegment != null && !classSegment.isEmpty()) {
            wrapper.eq(Student::getClassSegment, classSegment);
        }
        
        Long count = studentMapper.selectCount(wrapper);
        log.info("查询实际学生数：专业={}, 班级={}, 年级={}, 性别={}, 分片={}, 结果={}",
            majorName, className, grade, gender, classSegment, count);
        
        return count != null ? count.intValue() : 0;
    }
    
    /**
     * 辅助方法：安全获取Integer值
     */
    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 内部类：学生分片更新信息
     */
    private static class StudentSegmentUpdate {
        private List<Long> studentIds;
        private String segment;
        
        public StudentSegmentUpdate(List<Long> studentIds, String segment) {
            this.studentIds = studentIds;
            this.segment = segment;
        }
        
        public List<Long> getStudentIds() {
            return studentIds;
        }
        
        public String getSegment() {
            return segment;
        }
    }
}
