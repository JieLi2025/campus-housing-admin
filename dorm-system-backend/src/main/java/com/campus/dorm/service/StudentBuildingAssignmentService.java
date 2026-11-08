package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.Result;
import com.campus.dorm.entity.Building;
import com.campus.dorm.entity.BuildingMajorAssignment;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.BuildingMapper;
import com.campus.dorm.mapper.BuildingMajorAssignmentMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.vo.StudentBuildingAssignmentVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生班级楼栋分配查询服务
 * 
 * @author Campus Dorm System
 * @date 2025-11-03
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StudentBuildingAssignmentService {
    
    private final StudentMapper studentMapper;
    private final BuildingMajorAssignmentMapper assignmentMapper;
    private final BuildingMapper buildingMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 根据学生ID查询班级楼栋分配信息
     * @param studentId 学生ID（非学号，是student表的主键）
     * @return 班级分配信息
     */
    public Result<StudentBuildingAssignmentVO> getMyBuildingAssignment(Long studentId) {
        log.info("查询学生{}的班级楼栋分配", studentId);
        
        // 获取学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        
        StudentBuildingAssignmentVO vo = new StudentBuildingAssignmentVO();
        vo.setMajor(student.getMajor());
        vo.setClassName(student.getClassName());
        
        // 查询该专业和班级的分配记录（支持班级分片精确匹配）
        LambdaQueryWrapper<BuildingMajorAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BuildingMajorAssignment::getMajorName, student.getMajor())
                .eq(BuildingMajorAssignment::getStatus, 1);
        
        // 如果有班级，需要匹配班级
        if (student.getClassName() != null && !student.getClassName().isEmpty()) {
            wrapper.and(w -> w.eq(BuildingMajorAssignment::getClassName, student.getClassName())
                    .or()
                    .isNull(BuildingMajorAssignment::getClassName)
                    .or()
                    .eq(BuildingMajorAssignment::getClassName, ""));
        }
        
        // 🔑 关键：匹配班级分片（解决拆分班级的学生归属问题）
        if (student.getClassSegment() != null && !student.getClassSegment().isEmpty()) {
            // 学生有分片标识，精确匹配该分片
            wrapper.eq(BuildingMajorAssignment::getClassSegment, student.getClassSegment());
            log.info("学生{}属于{}班-{}组，精确匹配分片", studentId, student.getClassName(), student.getClassSegment());
        } else {
            // 学生无分片标识，只匹配未拆分的班级（class_segment为NULL或空）
            wrapper.and(w -> w.isNull(BuildingMajorAssignment::getClassSegment)
                    .or()
                    .eq(BuildingMajorAssignment::getClassSegment, ""));
        }
        
        List<BuildingMajorAssignment> assignments = assignmentMapper.selectList(wrapper);
        
        if (assignments.isEmpty()) {
            vo.setHasAssignment(false);
            vo.setAssignments(new ArrayList<>());
            return Result.success(vo);
        }
        
        vo.setHasAssignment(true);
        
        // 转换为VO
        List<StudentBuildingAssignmentVO.AssignmentInfo> assignmentInfos = new ArrayList<>();
        for (BuildingMajorAssignment assignment : assignments) {
            Building building = buildingMapper.selectById(assignment.getBuildingId());
            if (building == null) {
                continue;
            }
            
            StudentBuildingAssignmentVO.AssignmentInfo info = new StudentBuildingAssignmentVO.AssignmentInfo();
            info.setBuildingId(building.getId());
            info.setBuildingName(building.getBuildingName());
            info.setBuildingType(building.getBuildingType());
            info.setAssignmentMode(assignment.getAssignmentMode());
            info.setAllocatedRooms(assignment.getAllocatedRooms());
            info.setAllocatedStudents(assignment.getAllocatedStudents());
            info.setAreaName(assignment.getAreaName());
            
            // 新增：设置班级分片信息（用于前端显示）
            info.setClassSegment(assignment.getClassSegment());
            info.setStudentIdRange(assignment.getStudentIdRange());
            
            // 解析楼层列表
            if (assignment.getFloors() != null && !assignment.getFloors().isEmpty()) {
                try {
                    List<Integer> floors = objectMapper.readValue(assignment.getFloors(), new TypeReference<List<Integer>>() {});
                    info.setFloors(floors);
                } catch (Exception e) {
                    log.error("解析楼层JSON失败：{}", assignment.getFloors(), e);
                    info.setFloors(new ArrayList<>());
                }
            }
            
            assignmentInfos.add(info);
        }
        
        vo.setAssignments(assignmentInfos);
        
        return Result.success(vo);
    }
}

