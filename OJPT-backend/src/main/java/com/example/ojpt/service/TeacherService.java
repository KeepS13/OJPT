package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.ApplicationReviewDTO;
import com.example.ojpt.dto.ClassCreateDTO;
import com.example.ojpt.dto.ClassUpdateDTO;
import com.example.ojpt.vo.ClassApplicationVO;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;

public interface TeacherService {
    
    /**
     * 获取我管理的班级列表
     */
    PageResult<ClassVO> getMyClasses(Long teacherId, Integer page, Integer size);
    
    /**
     * 创建班级
     */
    ClassVO createClass(Long teacherId, ClassCreateDTO dto);
    
    /**
     * 获取班级详情
     */
    ClassVO getClassDetail(Long teacherId, Long classId);
    
    /**
     * 更新班级信息
     */
    void updateClass(Long teacherId, Long classId, ClassUpdateDTO dto);
    
    /**
     * 删除班级
     */
    void deleteClass(Long teacherId, Long classId);
    
    /**
     * 获取班级学员列表（已通过审核的）
     */
    PageResult<ClassMemberVO> getClassStudents(Long teacherId, Long classId, Integer page, Integer size);
    
    /**
     * 获取加入申请列表（待审核）
     */
    PageResult<ClassApplicationVO> getApplications(Long teacherId, Long classId, Integer page, Integer size);
    
    /**
     * 批准加入申请
     */
    void approveApplication(Long teacherId, Long classId, Long applicationId, ApplicationReviewDTO dto);
    
    /**
     * 拒绝加入申请
     */
    void rejectApplication(Long teacherId, Long classId, Long applicationId, ApplicationReviewDTO dto);
    
    /**
     * 邀请学员加入
     */
    void inviteStudent(Long teacherId, Long classId, Long studentId);
    
    /**
     * 移除学员
     */
    void removeStudent(Long teacherId, Long classId, Long studentId);
    
    /**
     * 获取班级的教师列表
     */
    PageResult<com.example.ojpt.vo.TeacherVO> getClassTeachers(Long teacherId, Long classId, Integer page, Integer size);
    
    /**
     * 添加教师到班级
     */
    void addTeacherToClass(Long teacherId, Long classId, Long newTeacherId, String role);
    
    /**
     * 移除班级教师
     */
    void removeTeacherFromClass(Long teacherId, Long classId, Long removeTeacherId);
}




