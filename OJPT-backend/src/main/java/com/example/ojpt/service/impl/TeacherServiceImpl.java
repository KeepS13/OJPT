package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.PaginationUtils;
import com.example.ojpt.dto.ApplicationReviewDTO;
import com.example.ojpt.dto.ClassCreateDTO;
import com.example.ojpt.dto.ClassUpdateDTO;
import com.example.ojpt.entity.ClassTeacher;
import com.example.ojpt.entity.ClassUser;
import com.example.ojpt.entity.Clazz;
import com.example.ojpt.entity.Department;
import com.example.ojpt.entity.School;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserProfile;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.mapper.ClassTeacherMapper;
import com.example.ojpt.mapper.ClassUserMapper;
import com.example.ojpt.mapper.ClazzMapper;
import com.example.ojpt.mapper.DepartmentMapper;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.SchoolMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserProfileMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import com.example.ojpt.service.TeacherService;
import com.example.ojpt.vo.ClassApplicationVO;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;
import com.example.ojpt.vo.TeacherVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {
    
    private final ClazzMapper clazzMapper;
    private final ClassUserMapper classUserMapper;
    private final ClassTeacherMapper classTeacherMapper;
    private final DepartmentMapper departmentMapper;
    private final SchoolMapper schoolMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    
    @Override
    public PageResult<ClassVO> getMyClasses(Long teacherId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);

        List<Clazz> classesAsHead = clazzMapper.selectList(
            new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getTeacherId, teacherId)
        );
        List<ClassTeacher> classTeachers = classTeacherMapper.selectList(
            new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getTeacherId, teacherId)
        );
        List<Long> classIds = classTeachers.stream()
            .map(ClassTeacher::getClassId)
            .collect(Collectors.toList());
        List<Clazz> classesAsTeacher = classIds.isEmpty() ? List.of() :
            clazzMapper.selectBatchIds(classIds);
        List<Clazz> allClasses = classesAsHead.stream()
            .filter(c -> !classIds.contains(c.getId()))
            .collect(Collectors.toList());
        allClasses.addAll(classesAsTeacher);
        List<ClassVO> all = allClasses.stream()
            .map(c -> buildClassVO(c, null, null, null))
            .collect(Collectors.toList());
        int total = all.size();
        int from = (p - 1) * s;
        if (from >= total) {
            return PageResult.of(List.of(), total, p, s);
        }
        int to = Math.min(from + s, total);
        return PageResult.of(new ArrayList<>(all.subList(from, to)), total, p, s);
    }
    
    @Override
    @Transactional
    public ClassVO createClass(Long teacherId, ClassCreateDTO dto) {
        // 检查院系是否存在
        Department department = departmentMapper.selectById(dto.getDepartmentId());
        if (department == null) {
            throw new RuntimeException("院系不存在");
        }
        
        Clazz clazz = new Clazz();
        clazz.setDepartmentId(dto.getDepartmentId());
        clazz.setName(dto.getName());
        clazz.setYear(dto.getYear());
        clazz.setTeacherId(dto.getTeacherId() != null ? dto.getTeacherId() : teacherId);
        clazz.setMerk(dto.getMerk());
        
        clazzMapper.insert(clazz);
        
        return buildClassVO(clazz, null, null, null);
    }
    
    @Override
    public ClassVO getClassDetail(Long teacherId, Long classId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        // 检查权限
        checkTeacherPermission(teacherId, clazz);
        
        return buildClassVO(clazz, null, null, null);
    }
    
    @Override
    @Transactional
    public void updateClass(Long teacherId, Long classId, ClassUpdateDTO dto) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        // 检查权限
        checkTeacherPermission(teacherId, clazz);
        
        if (dto.getName() != null) {
            clazz.setName(dto.getName());
        }
        if (dto.getYear() != null) {
            clazz.setYear(dto.getYear());
        }
        if (dto.getTeacherId() != null) {
            clazz.setTeacherId(dto.getTeacherId());
        }
        if (dto.getMerk() != null) {
            clazz.setMerk(dto.getMerk());
        }
        
        clazzMapper.updateById(clazz);
    }
    
    @Override
    @Transactional
    public void deleteClass(Long teacherId, Long classId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        // 检查权限：只有班主任可以删除
        if (!teacherId.equals(clazz.getTeacherId())) {
            throw new RuntimeException("只有班主任可以删除班级");
        }
        
        // 删除关联数据
        classUserMapper.delete(new LambdaQueryWrapper<ClassUser>()
            .eq(ClassUser::getClassId, classId));
        classTeacherMapper.delete(new LambdaQueryWrapper<ClassTeacher>()
            .eq(ClassTeacher::getClassId, classId));
        
        clazzMapper.deleteById(classId);
    }
    
    @Override
    public PageResult<ClassMemberVO> getClassStudents(Long teacherId, Long classId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        checkTeacherPermission(teacherId, clazz);

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
    
    @Override
    public PageResult<ClassApplicationVO> getApplications(Long teacherId, Long classId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        checkTeacherPermission(teacherId, clazz);

        Page<ClassUser> pageParam = new Page<>(p, s);
        Page<ClassUser> result = classUserMapper.selectPage(pageParam,
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getJoinStatus, "PENDING")
        );
        List<ClassApplicationVO> records = result.getRecords().stream().map(cu -> {
            User user = userMapper.selectById(cu.getUserId());
            if (user == null) {
                return null;
            }
            ClassApplicationVO vo = new ClassApplicationVO();
            vo.setId(cu.getId());
            vo.setClassId(classId);
            vo.setClassName(clazz.getName());
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
            vo.setAvatar(user.getAvatar());
            vo.setJoinType(cu.getJoinType());
            vo.setJoinStatus(cu.getJoinStatus());
            vo.setJoinAt(cu.getJoinAt());
            vo.setReviewerId(cu.getReviewerId());
            vo.setReviewAt(cu.getReviewAt());
            vo.setReviewComment(cu.getReviewComment());
            if (cu.getReviewerId() != null) {
                User reviewer = userMapper.selectById(cu.getReviewerId());
                if (reviewer != null) {
                    vo.setReviewerName(reviewer.getUsername());
                }
            }
            UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, user.getId())
            );
            if (profile != null) {
                vo.setStudentNo(profile.getStudentNo());
            }
            return vo;
        }).filter(vo -> vo != null).collect(Collectors.toList());
        return PageResult.<ClassApplicationVO>builder()
            .records(records)
            .total(result.getTotal())
            .current(result.getCurrent())
            .size(result.getSize())
            .pages(result.getPages())
            .build();
    }
    
    @Override
    @Transactional
    public void approveApplication(Long teacherId, Long classId, Long applicationId, ApplicationReviewDTO dto) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        checkTeacherPermission(teacherId, clazz);
        
        ClassUser classUser = classUserMapper.selectById(applicationId);
        if (classUser == null || !classUser.getClassId().equals(classId)) {
            throw new RuntimeException("申请记录不存在");
        }
        
        if (!"PENDING".equals(classUser.getJoinStatus())) {
            throw new RuntimeException("该申请已处理");
        }
        
        classUser.setJoinStatus("APPROVED");
        classUser.setReviewerId(teacherId);
        classUser.setReviewAt(LocalDateTime.now());
        classUser.setJoinAt(LocalDateTime.now());
        if (dto != null && dto.getReviewComment() != null) {
            classUser.setReviewComment(dto.getReviewComment());
        }
        
        classUserMapper.updateById(classUser);
        
        // 自动授予 STUDENT 角色
        grantStudentRoleIfNeeded(classUser.getUserId());
    }
    
    @Override
    @Transactional
    public void rejectApplication(Long teacherId, Long classId, Long applicationId, ApplicationReviewDTO dto) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        checkTeacherPermission(teacherId, clazz);
        
        ClassUser classUser = classUserMapper.selectById(applicationId);
        if (classUser == null || !classUser.getClassId().equals(classId)) {
            throw new RuntimeException("申请记录不存在");
        }
        
        if (!"PENDING".equals(classUser.getJoinStatus())) {
            throw new RuntimeException("该申请已处理");
        }
        
        classUser.setJoinStatus("REJECTED");
        classUser.setReviewerId(teacherId);
        classUser.setReviewAt(LocalDateTime.now());
        if (dto != null && dto.getReviewComment() != null) {
            classUser.setReviewComment(dto.getReviewComment());
        }
        
        classUserMapper.updateById(classUser);
    }
    
    @Override
    @Transactional
    public void inviteStudent(Long teacherId, Long classId, Long studentId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        checkTeacherPermission(teacherId, clazz);
        
        // 检查是否已经存在
        ClassUser existing = classUserMapper.selectOne(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, studentId)
        );
        
        if (existing != null) {
            throw new RuntimeException("该学员已申请或加入该班级");
        }
        
        ClassUser classUser = new ClassUser();
        classUser.setClassId(classId);
        classUser.setUserId(studentId);
        classUser.setJoinType("INVITE");
        classUser.setJoinStatus("PENDING");
        
        classUserMapper.insert(classUser);
    }
    
    @Override
    @Transactional
    public void removeStudent(Long teacherId, Long classId, Long studentId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        checkTeacherPermission(teacherId, clazz);
        
        ClassUser classUser = classUserMapper.selectOne(
            new LambdaQueryWrapper<ClassUser>()
                .eq(ClassUser::getClassId, classId)
                .eq(ClassUser::getUserId, studentId)
        );
        
        if (classUser == null) {
            throw new RuntimeException("该学员未加入该班级");
        }
        
        Long userId = classUser.getUserId();
        classUserMapper.deleteById(classUser.getId());
        
        // 检查是否需要移除 STUDENT 角色
        revokeStudentRoleIfNeeded(userId);
    }
    
    @Override
    public PageResult<TeacherVO> getClassTeachers(Long teacherId, Long classId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        checkTeacherPermission(teacherId, clazz);

        List<TeacherVO> teachers = new ArrayList<>();
        if (clazz.getTeacherId() != null) {
            User headTeacher = userMapper.selectById(clazz.getTeacherId());
            if (headTeacher != null) {
                TeacherVO vo = new TeacherVO();
                vo.setTeacherId(headTeacher.getId());
                vo.setUsername(headTeacher.getUsername());
                vo.setEmail(headTeacher.getEmail());
                vo.setAvatar(headTeacher.getAvatar());
                vo.setRole("班主任");
                teachers.add(vo);
            }
        }
        List<ClassTeacher> classTeachers = classTeacherMapper.selectList(
            new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getClassId, classId)
        );
        for (ClassTeacher ct : classTeachers) {
            User teacher = userMapper.selectById(ct.getTeacherId());
            if (teacher != null) {
                TeacherVO vo = new TeacherVO();
                vo.setTeacherId(teacher.getId());
                vo.setUsername(teacher.getUsername());
                vo.setEmail(teacher.getEmail());
                vo.setAvatar(teacher.getAvatar());
                vo.setRole(ct.getRole() != null ? ct.getRole() : "任课教师");
                vo.setCreatedAt(ct.getCreatedAt());
                teachers.add(vo);
            }
        }
        int total = teachers.size();
        int from = (p - 1) * s;
        if (from >= total) {
            return PageResult.of(List.of(), total, p, s);
        }
        int to = Math.min(from + s, total);
        return PageResult.of(new ArrayList<>(teachers.subList(from, to)), total, p, s);
    }
    
    @Override
    @Transactional
    public void addTeacherToClass(Long teacherId, Long classId, Long newTeacherId, String role) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        checkTeacherPermission(teacherId, clazz);
        
        // 检查是否已经存在
        ClassTeacher existing = classTeacherMapper.selectOne(
            new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getClassId, classId)
                .eq(ClassTeacher::getTeacherId, newTeacherId)
        );
        
        if (existing != null) {
            throw new RuntimeException("该教师已在该班级中");
        }
        
        // 不能添加班主任（班主任在class表中）
        if (clazz.getTeacherId() != null && clazz.getTeacherId().equals(newTeacherId)) {
            throw new RuntimeException("该教师已是班主任");
        }
        
        ClassTeacher classTeacher = new ClassTeacher();
        classTeacher.setClassId(classId);
        classTeacher.setTeacherId(newTeacherId);
        classTeacher.setRole(role);
        
        classTeacherMapper.insert(classTeacher);
    }
    
    @Override
    @Transactional
    public void removeTeacherFromClass(Long teacherId, Long classId, Long removeTeacherId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        checkTeacherPermission(teacherId, clazz);
        
        // 不能删除班主任
        if (clazz.getTeacherId() != null && clazz.getTeacherId().equals(removeTeacherId)) {
            throw new RuntimeException("不能删除班主任，请先更换班主任");
        }
        
        ClassTeacher classTeacher = classTeacherMapper.selectOne(
            new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getClassId, classId)
                .eq(ClassTeacher::getTeacherId, removeTeacherId)
        );
        
        if (classTeacher == null) {
            throw new RuntimeException("该教师不在该班级中");
        }
        
        classTeacherMapper.deleteById(classTeacher.getId());
    }
    
    private void checkTeacherPermission(Long teacherId, Clazz clazz) {
        // 检查是否是班主任
        if (teacherId.equals(clazz.getTeacherId())) {
            return;
        }
        
        // 检查是否是助教/任课教师
        ClassTeacher classTeacher = classTeacherMapper.selectOne(
            new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getClassId, clazz.getId())
                .eq(ClassTeacher::getTeacherId, teacherId)
        );
        
        if (classTeacher == null) {
            throw new RuntimeException("您无权管理该班级");
        }
    }
    
    private ClassVO buildClassVO(Clazz clazz, String joinStatus, String joinType, LocalDateTime joinAt) {
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
        
        Department department = departmentMapper.selectById(clazz.getDepartmentId());
        if (department != null) {
            vo.setDepartmentName(department.getName());
            vo.setSchoolId(department.getSchoolId());
            
            School school = schoolMapper.selectById(department.getSchoolId());
            if (school != null) {
                vo.setSchoolName(school.getName());
            }
        }
        
        if (clazz.getTeacherId() != null) {
            User teacher = userMapper.selectById(clazz.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getUsername());
            }
        }
        
        return vo;
    }
    
    /**
     * 如果用户还没有 STUDENT 角色，则授予该角色
     */
    private void grantStudentRoleIfNeeded(Long userId) {
        // 查找 STUDENT 角色
        Role studentRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "STUDENT"));
        if (studentRole == null) {
            return; // STUDENT 角色不存在，跳过
        }
        
        // 检查用户是否已有 STUDENT 角色
        UserRole existing = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, studentRole.getId()));
        
        if (existing == null) {
            // 授予 STUDENT 角色
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(studentRole.getId());
            userRole.setBindSource("CLASS_APPROVED");
            userRoleMapper.insert(userRole);
        }
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




