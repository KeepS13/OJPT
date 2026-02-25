package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.PaginationUtils;
import com.example.ojpt.entity.ClassUser;
import com.example.ojpt.entity.Clazz;
import com.example.ojpt.entity.Department;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.School;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserProfile;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.mapper.ClassUserMapper;
import com.example.ojpt.mapper.ClazzMapper;
import com.example.ojpt.mapper.DepartmentMapper;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.SchoolMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserProfileMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import com.example.ojpt.service.StudentService;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    
    private final ClassUserMapper classUserMapper;
    private final ClazzMapper clazzMapper;
    private final DepartmentMapper departmentMapper;
    private final SchoolMapper schoolMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    
    @Override
    public PageResult<ClassVO> getMyClasses(Long userId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);

        Page<ClassUser> pageParam = new Page<>(p, s);
        Page<ClassUser> result = classUserMapper.selectPage(pageParam,
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getUserId, userId)
        );
        List<ClassVO> records = result.getRecords().stream().map(cu -> {
            Clazz clazz = clazzMapper.selectById(cu.getClassId());
            if (clazz == null) {
                return null;
            }
            return buildClassVO(clazz, cu.getJoinStatus(), cu.getJoinType(), cu.getJoinAt());
        }).filter(vo -> vo != null).collect(Collectors.toList());
        return PageResult.<ClassVO>builder()
            .records(records)
            .total(result.getTotal())
            .current(result.getCurrent())
            .size(result.getSize())
            .pages(result.getPages())
            .build();
    }
    
    @Override
    public ClassVO getClassDetail(Long userId, Long classId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        ClassUser classUser = classUserMapper.selectOne(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, userId)
        );
        
        String joinStatus = classUser != null ? classUser.getJoinStatus() : null;
        String joinType = classUser != null ? classUser.getJoinType() : null;
        
        return buildClassVO(clazz, joinStatus, joinType, classUser != null ? classUser.getJoinAt() : null);
    }
    
    @Override
    @Transactional
    public void applyToClass(Long userId, Long classId) {
        // 检查班级是否存在
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        // 检查是否已经申请或加入
        ClassUser existing = classUserMapper.selectOne(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, userId)
        );
        
        if (existing != null) {
            throw new RuntimeException("您已经申请或加入该班级");
        }
        
        // 创建申请记录
        ClassUser classUser = new ClassUser();
        classUser.setClassId(classId);
        classUser.setUserId(userId);
        classUser.setJoinType("APPLY");
        classUser.setJoinStatus("PENDING");
        classUserMapper.insert(classUser);
    }
    
    @Override
    @Transactional
    public void quitClass(Long userId, Long classId) {
        ClassUser classUser = classUserMapper.selectOne(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, userId)
        );
        
        if (classUser == null) {
            throw new RuntimeException("您未加入该班级");
        }
        
        classUserMapper.deleteById(classUser.getId());
        
        // 检查是否需要移除 STUDENT 角色
        revokeStudentRoleIfNeeded(userId);
    }
    
    @Override
    public PageResult<ClassMemberVO> getClassMembers(Long userId, Long classId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);

        ClassUser classUser = classUserMapper.selectOne(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, userId)
        );
        if (classUser == null) {
            throw new RuntimeException("您无权查看该班级成员");
        }

        Page<ClassUser> pageParam = new Page<>(p, s);
        Page<ClassUser> result = classUserMapper.selectPage(pageParam,
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getJoinStatus, "APPROVED")
        );
        List<ClassMemberVO> records = result.getRecords().stream().map(cu -> {
            User user = userMapper.selectById(cu.getUserId());
            if (user == null) {
                return null;
            }
            ClassMemberVO vo = new ClassMemberVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setAvatar(user.getAvatar());
            vo.setJoinAt(cu.getJoinAt());
            vo.setJoinType(cu.getJoinType());
            UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, user.getId())
            );
            if (profile != null) {
                vo.setStudentNo(profile.getStudentNo());
            }
            return vo;
        }).filter(vo -> vo != null).collect(Collectors.toList());
        return PageResult.<ClassMemberVO>builder()
            .records(records)
            .total(result.getTotal())
            .current(result.getCurrent())
            .size(result.getSize())
            .pages(result.getPages())
            .build();
    }
    
    private ClassVO buildClassVO(Clazz clazz, String joinStatus, String joinType, java.time.LocalDateTime joinAt) {
        ClassVO vo = new ClassVO();
        vo.setId(clazz.getId());
        vo.setDepartmentId(clazz.getDepartmentId());
        vo.setName(clazz.getName());
        vo.setYear(clazz.getYear());
        vo.setTeacherId(clazz.getTeacherId());
        vo.setMerk(clazz.getMerk());
        vo.setCreatedAt(clazz.getCreatedAt());
        vo.setUpdatedAt(clazz.getUpdatedAt());
        vo.setJoinStatus(joinStatus);
        vo.setJoinType(joinType);
        vo.setJoinAt(joinAt);
        
        // 获取院系信息
        Department department = departmentMapper.selectById(clazz.getDepartmentId());
        if (department != null) {
            vo.setDepartmentName(department.getName());
            vo.setSchoolId(department.getSchoolId());
            
            // 获取学校信息
            School school = schoolMapper.selectById(department.getSchoolId());
            if (school != null) {
                vo.setSchoolName(school.getName());
            }
        }
        
        // 获取班主任信息
        if (clazz.getTeacherId() != null) {
            User teacher = userMapper.selectById(clazz.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getUsername());
            }
        }
        
        return vo;
    }
    
    /**
     * 如果用户没有已批准的班级，则移除 STUDENT 角色
     */
    private void revokeStudentRoleIfNeeded(Long userId) {
        // 检查用户是否还有其他已批准的班级
        long approvedClassCount = classUserMapper.selectCount(new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getUserId, userId)
                .eq(ClassUser::getJoinStatus, "APPROVED"));
        
        if (approvedClassCount == 0) {
            // 没有已批准的班级，移除 STUDENT 角色
            Role studentRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getCode, "STUDENT"));
            if (studentRole != null) {
                userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, studentRole.getId())
                        .eq(UserRole::getBindSource, "CLASS_APPROVED"));
            }
        }
    }
}

