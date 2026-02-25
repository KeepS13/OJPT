package com.example.ojpt.service;

import com.example.ojpt.common.PageResult;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;

public interface StudentService {
    
    /**
     * 获取学员加入的班级列表
     */
    PageResult<ClassVO> getMyClasses(Long userId, Integer page, Integer size);
    
    /**
     * 获取班级详情
     */
    ClassVO getClassDetail(Long userId, Long classId);
    
    /**
     * 申请加入班级
     */
    void applyToClass(Long userId, Long classId);
    
    /**
     * 退出班级
     */
    void quitClass(Long userId, Long classId);
    
    /**
     * 查看班级成员列表
     */
    PageResult<ClassMemberVO> getClassMembers(Long userId, Long classId, Integer page, Integer size);
}




