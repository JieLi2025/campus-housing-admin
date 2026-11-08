package com.campus.dorm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.dorm.entity.Student;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生信息 Mapper
 * 
 * @author Campus Dorm System
 * @date 2024-10-21
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    
    /**
     * 物理删除学生（真正删除，绕过软删除）
     * 警告：此操作不可恢复！
     */
    @Delete("DELETE FROM student WHERE id = #{id}")
    int hardDelete(@Param("id") Long id);
    
    /**
     * 统计未分配宿舍的学生数量
     * 未分配 = 没有出现在任何宿舍组的members字段中
     * 🔧 修复：添加 deleted = 0 条件，排除已删除的学生
     * 🔧 修复：dorm_group 表没有 deleted 字段，移除该条件
     */
    @Select("SELECT COUNT(*) FROM student s " +
            "WHERE s.deleted = 0 " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM dorm_group dg " +
            "    WHERE dg.members LIKE CONCAT('%\"id\":', s.id, ',%') " +
            "        OR dg.members LIKE CONCAT('%\"id\":', s.id, '}%') " +
            ")")
    Long countUnassignedStudents();
    
    /**
     * 统计未分配宿舍的学生数量（按性别过滤）
     * @param genders 性别列表（男/女）
     * 🔧 修复：添加 deleted = 0 条件，排除已删除的学生
     * 🔧 修复：dorm_group 表没有 deleted 字段，移除该条件
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM student s " +
            "WHERE s.deleted = 0 " +
            "AND s.gender IN " +
            "<foreach item='gender' collection='genders' open='(' separator=',' close=')'>" +
            "#{gender}" +
            "</foreach> " +
            "AND NOT EXISTS ( " +
            "    SELECT 1 FROM dorm_group dg " +
            "    WHERE dg.members LIKE CONCAT('%\"id\":', s.id, ',%') " +
            "        OR dg.members LIKE CONCAT('%\"id\":', s.id, '}%') " +
            ")" +
            "</script>")
    Long countUnassignedStudentsByGenders(@Param("genders") List<String> genders);
}

