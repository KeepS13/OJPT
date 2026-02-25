package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.DepartmentCreateDTO;
import com.example.ojpt.dto.DepartmentUpdateDTO;
import com.example.ojpt.dto.SchoolUpdateDTO;
import com.example.ojpt.vo.ClassVO;
import com.example.ojpt.vo.DepartmentVO;
import com.example.ojpt.vo.SchoolVO;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.TeacherVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.UserDetailVO;

import java.util.List;
import java.util.Map;

public interface SchoolService {
    
    // 学校信息管理
    SchoolVO getSchoolInfo(Long schoolUserId);
    void updateSchoolInfo(Long schoolUserId, SchoolUpdateDTO dto);
    SchoolVO getCertification(Long schoolUserId);
    
    // 院系管理
    PageResult<DepartmentVO> getDepartments(Long schoolUserId, Integer page, Integer size);
    DepartmentVO createDepartment(Long schoolUserId, DepartmentCreateDTO dto);
    DepartmentVO getDepartment(Long schoolUserId, Long departmentId);
    void updateDepartment(Long schoolUserId, Long departmentId, DepartmentUpdateDTO dto);
    void deleteDepartment(Long schoolUserId, Long departmentId);
    
    // 班级管理
    PageResult<ClassVO> getClasses(Long schoolUserId, Integer page, Integer size);
    PageResult<ClassVO> getDepartmentClasses(Long schoolUserId, Long departmentId, Integer page, Integer size);
    ClassVO getClassDetail(Long schoolUserId, Long classId);
    void updateClass(Long schoolUserId, Long classId, com.example.ojpt.dto.ClassUpdateDTO dto);
    void deleteClass(Long schoolUserId, Long classId);
    
    // 教师管理
    PageResult<TeacherVO> getTeachers(Long schoolUserId, Integer page, Integer size);
    TeacherVO addTeacher(Long schoolUserId, Long userId);
    TeacherVO getTeacher(Long schoolUserId, Long teacherId);
    void updateTeacher(Long schoolUserId, Long teacherId, com.example.ojpt.dto.UserUpdateDTO dto);
    void removeTeacher(Long schoolUserId, Long teacherId);
    PageResult<ClassVO> getTeacherClasses(Long schoolUserId, Long teacherId, Integer page, Integer size);
    
    // 学员管理
    PageResult<UserDetailVO> getStudents(Long schoolUserId, Integer page, Integer size);
    PageResult<UserDetailVO> getDepartmentStudents(Long schoolUserId, Long departmentId, Integer page, Integer size);
    PageResult<ClassMemberVO> getClassStudents(Long schoolUserId, Long classId, Integer page, Integer size);
    UserDetailVO getStudent(Long schoolUserId, Long studentId);
    void updateStudent(Long schoolUserId, Long studentId, com.example.ojpt.dto.UserUpdateDTO dto);
    
    // 数据统计
    StatisticsVO getOverviewStatistics(Long schoolUserId);
    List<Map<String, Object>> getDepartmentStatistics(Long schoolUserId);
    List<Map<String, Object>> getClassStatistics(Long schoolUserId);
}




