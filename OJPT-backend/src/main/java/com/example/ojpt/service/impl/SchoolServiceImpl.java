package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.common.PaginationUtils;
import com.example.ojpt.dto.DepartmentCreateDTO;
import com.example.ojpt.dto.DepartmentUpdateDTO;
import com.example.ojpt.dto.SchoolUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.entity.ClassTeacher;
import com.example.ojpt.entity.ClassUser;
import com.example.ojpt.entity.Clazz;
import com.example.ojpt.entity.Department;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.School;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserProfile;
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
import com.example.ojpt.service.SchoolService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.ClassMemberVO;
import com.example.ojpt.vo.ClassVO;
import com.example.ojpt.vo.DepartmentVO;
import com.example.ojpt.vo.SchoolVO;
import com.example.ojpt.vo.TeacherVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {
    
    private final SchoolMapper schoolMapper;
    private final DepartmentMapper departmentMapper;
    private final ClazzMapper clazzMapper;
    private final ClassUserMapper classUserMapper;
    private final ClassTeacherMapper classTeacherMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;
    
    @Override
    public SchoolVO getSchoolInfo(Long schoolUserId) {
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, schoolUserId)
        );
        
        if (profile == null || profile.getSchoolId() == null) {
            throw new RuntimeException("您未关联学校");
        }
        
        School school = schoolMapper.selectById(profile.getSchoolId());
        if (school == null) {
            throw new RuntimeException("学校不存在");
        }
        
        return buildSchoolVO(school);
    }
    
    @Override
    @Transactional
    public void updateSchoolInfo(Long schoolUserId, SchoolUpdateDTO dto) {
        School school = getSchoolByUserId(schoolUserId);
        
        if (dto.getName() != null) {
            school.setName(dto.getName());
        }
        if (dto.getContact() != null) {
            school.setContact(dto.getContact());
        }
        if (dto.getStatus() != null) {
            school.setStatus(dto.getStatus());
        }
        
        schoolMapper.updateById(school);
    }
    
    @Override
    public SchoolVO getCertification(Long schoolUserId) {
        School school = getSchoolByUserId(schoolUserId);
        return buildSchoolVO(school);
    }
    
    @Override
    public PageResult<DepartmentVO> getDepartments(Long schoolUserId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        Page<Department> pageParam = new Page<>(p, s);
        Page<Department> result = departmentMapper.selectPage(pageParam,
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        return PageResult.from(result, d -> {
            DepartmentVO vo = new DepartmentVO();
            vo.setId(d.getId());
            vo.setSchoolId(d.getSchoolId());
            vo.setSchoolName(school.getName());
            vo.setName(d.getName());
            vo.setCreatedAt(d.getCreatedAt());
            vo.setUpdatedAt(d.getUpdatedAt());
            return vo;
        });
    }
    
    @Override
    @Transactional
    public DepartmentVO createDepartment(Long schoolUserId, DepartmentCreateDTO dto) {
        School school = getSchoolByUserId(schoolUserId);
        
        Department department = new Department();
        department.setSchoolId(school.getId());
        department.setName(dto.getName());
        
        departmentMapper.insert(department);
        
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setSchoolId(department.getSchoolId());
        vo.setSchoolName(school.getName());
        vo.setName(department.getName());
        vo.setCreatedAt(department.getCreatedAt());
        vo.setUpdatedAt(department.getUpdatedAt());
        return vo;
    }
    
    @Override
    public DepartmentVO getDepartment(Long schoolUserId, Long departmentId) {
        School school = getSchoolByUserId(schoolUserId);
        
        Department department = departmentMapper.selectById(departmentId);
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("院系不存在或不属于您的学校");
        }
        
        DepartmentVO vo = new DepartmentVO();
        vo.setId(department.getId());
        vo.setSchoolId(department.getSchoolId());
        vo.setSchoolName(school.getName());
        vo.setName(department.getName());
        vo.setCreatedAt(department.getCreatedAt());
        vo.setUpdatedAt(department.getUpdatedAt());
        return vo;
    }
    
    @Override
    @Transactional
    public void updateDepartment(Long schoolUserId, Long departmentId, DepartmentUpdateDTO dto) {
        School school = getSchoolByUserId(schoolUserId);
        
        Department department = departmentMapper.selectById(departmentId);
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("院系不存在或不属于您的学校");
        }
        
        department.setName(dto.getName());
        departmentMapper.updateById(department);
    }
    
    @Override
    @Transactional
    public void deleteDepartment(Long schoolUserId, Long departmentId) {
        School school = getSchoolByUserId(schoolUserId);
        
        Department department = departmentMapper.selectById(departmentId);
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("院系不存在或不属于您的学校");
        }
        
        // 检查是否有班级关联
        long classCount = clazzMapper.selectCount(
            new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getDepartmentId, departmentId)
        );
        
        if (classCount > 0) {
            throw new RuntimeException("该院系下还有班级，无法删除");
        }
        
        departmentMapper.deleteById(departmentId);
    }
    
    @Override
    public PageResult<ClassVO> getClasses(Long schoolUserId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        List<Department> departments = departmentMapper.selectList(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        List<Long> departmentIds = departments.stream()
            .map(Department::getId)
            .collect(Collectors.toList());
        if (departmentIds.isEmpty()) {
            return PageResult.empty(p, s);
        }

        Page<Clazz> pageParam = new Page<>(p, s);
        Page<Clazz> result = clazzMapper.selectPage(pageParam,
            new LambdaQueryWrapper<Clazz>()
                .in(Clazz::getDepartmentId, departmentIds)
        );
        return PageResult.from(result, c -> buildClassVO(c));
    }
    
    @Override
    public PageResult<ClassVO> getDepartmentClasses(Long schoolUserId, Long departmentId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        Department department = departmentMapper.selectById(departmentId);
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("院系不存在或不属于您的学校");
        }

        Page<Clazz> pageParam = new Page<>(p, s);
        Page<Clazz> result = clazzMapper.selectPage(pageParam,
            new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getDepartmentId, departmentId)
        );
        return PageResult.from(result, c -> buildClassVO(c));
    }
    
    @Override
    public ClassVO getClassDetail(Long schoolUserId, Long classId) {
        School school = getSchoolByUserId(schoolUserId);
        
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        Department department = departmentMapper.selectById(clazz.getDepartmentId());
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("班级不属于您的学校");
        }
        
        return buildClassVO(clazz);
    }
    
    @Override
    @Transactional
    public void updateClass(Long schoolUserId, Long classId, com.example.ojpt.dto.ClassUpdateDTO dto) {
        School school = getSchoolByUserId(schoolUserId);
        
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        Department department = departmentMapper.selectById(clazz.getDepartmentId());
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("班级不属于您的学校");
        }
        
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
    public void deleteClass(Long schoolUserId, Long classId) {
        School school = getSchoolByUserId(schoolUserId);
        
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        
        Department department = departmentMapper.selectById(clazz.getDepartmentId());
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("班级不属于您的学校");
        }
        
        // 在删除前获取所有受影响的用户ID（仅已批准的用户）
        List<ClassUser> approvedClassUsers = classUserMapper.selectList(new LambdaQueryWrapper<ClassUser>()
            .eq(ClassUser::getClassId, classId)
            .eq(ClassUser::getJoinStatus, "APPROVED"));
        List<Long> affectedUserIds = approvedClassUsers.stream()
            .map(ClassUser::getUserId)
            .distinct()
            .collect(Collectors.toList());
        
        // 删除关联数据
        classUserMapper.delete(new LambdaQueryWrapper<ClassUser>()
            .eq(ClassUser::getClassId, classId));
        classTeacherMapper.delete(new LambdaQueryWrapper<ClassTeacher>()
            .eq(ClassTeacher::getClassId, classId));
        
        clazzMapper.deleteById(classId);
        
        // 为每个受影响的用户检查是否需要移除 STUDENT 角色
        for (Long userId : affectedUserIds) {
            revokeStudentRoleIfNeeded(userId);
        }
    }
    
    @Override
    public PageResult<TeacherVO> getTeachers(Long schoolUserId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        Role teacherRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "TEACHER")
        );
        if (teacherRole == null) {
            return PageResult.empty(p, s);
        }
        List<UserProfile> profiles = userProfileMapper.selectList(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getSchoolId, school.getId())
        );
        List<Long> userIds = profiles.stream()
            .map(UserProfile::getUserId)
            .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return PageResult.empty(p, s);
        }
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getUserId, userIds)
                .eq(UserRole::getRoleId, teacherRole.getId())
        );
        List<Long> teacherIds = userRoles.stream()
            .map(UserRole::getUserId)
            .distinct()
            .collect(Collectors.toList());
        if (teacherIds.isEmpty()) {
            return PageResult.empty(p, s);
        }
        List<User> teachers = userMapper.selectBatchIds(teacherIds);
        List<TeacherVO> all = teachers.stream().map(t -> {
            TeacherVO vo = new TeacherVO();
            vo.setTeacherId(t.getId());
            vo.setUsername(t.getUsername());
            vo.setEmail(t.getEmail());
            vo.setAvatar(t.getAvatar());
            return vo;
        }).collect(Collectors.toList());
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
    public TeacherVO addTeacher(Long schoolUserId, Long userId) {
        School school = getSchoolByUserId(schoolUserId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取TEACHER角色
        Role teacherRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "TEACHER")
        );
        
        if (teacherRole == null) {
            throw new RuntimeException("TEACHER角色不存在");
        }
        
        // 绑定角色
        UserRole existing = userRoleMapper.selectOne(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, teacherRole.getId())
        );
        
        if (existing == null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(teacherRole.getId());
            userRole.setBindSource("SCHOOL_ADD");
            userRoleMapper.insert(userRole);
        }
        
        // 更新user_profile
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
        );
        
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setSchoolId(school.getId());
            userProfileMapper.insert(profile);
        } else {
            profile.setSchoolId(school.getId());
            userProfileMapper.updateById(profile);
        }
        
        TeacherVO vo = new TeacherVO();
        vo.setTeacherId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
    
    @Override
    public TeacherVO getTeacher(Long schoolUserId, Long teacherId) {
        School school = getSchoolByUserId(schoolUserId);
        
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, teacherId)
        );
        
        if (profile == null || !school.getId().equals(profile.getSchoolId())) {
            throw new RuntimeException("教师不属于您的学校");
        }
        
        User teacher = userMapper.selectById(teacherId);
        if (teacher == null) {
            throw new RuntimeException("教师不存在");
        }
        
        TeacherVO vo = new TeacherVO();
        vo.setTeacherId(teacher.getId());
        vo.setUsername(teacher.getUsername());
        vo.setEmail(teacher.getEmail());
        vo.setAvatar(teacher.getAvatar());
        return vo;
    }
    
    @Override
    @Transactional
    public void updateTeacher(Long schoolUserId, Long teacherId, UserUpdateDTO dto) {
        School school = getSchoolByUserId(schoolUserId);
        
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, teacherId)
        );
        
        if (profile == null || !school.getId().equals(profile.getSchoolId())) {
            throw new RuntimeException("教师不属于您的学校");
        }
        
        userService.updateProfile(teacherId, dto);
    }
    
    @Override
    @Transactional
    public void removeTeacher(Long schoolUserId, Long teacherId) {
        School school = getSchoolByUserId(schoolUserId);
        
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, teacherId)
        );
        
        if (profile == null || !school.getId().equals(profile.getSchoolId())) {
            throw new RuntimeException("教师不属于您的学校");
        }
        
        // 获取TEACHER角色
        Role teacherRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "TEACHER")
        );
        
        if (teacherRole != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, teacherId)
                .eq(UserRole::getRoleId, teacherRole.getId()));
        }
    }
    
    @Override
    public PageResult<ClassVO> getTeacherClasses(Long schoolUserId, Long teacherId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, teacherId)
        );
        if (profile == null || !school.getId().equals(profile.getSchoolId())) {
            throw new RuntimeException("教师不属于您的学校");
        }
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
        List<Department> departments = departmentMapper.selectList(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        List<Long> departmentIds = departments.stream()
            .map(Department::getId)
            .collect(Collectors.toList());
        List<ClassVO> all = allClasses.stream()
            .filter(c -> departmentIds.contains(c.getDepartmentId()))
            .map(c -> buildClassVO(c))
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
    public PageResult<UserDetailVO> getStudents(Long schoolUserId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        Role userRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "USER")
        );
        if (userRole == null) {
            return PageResult.empty(p, s);
        }
        List<UserProfile> profiles = userProfileMapper.selectList(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getSchoolId, school.getId())
        );
        List<Long> userIds = profiles.stream()
            .map(UserProfile::getUserId)
            .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return PageResult.empty(p, s);
        }
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getUserId, userIds)
                .eq(UserRole::getRoleId, userRole.getId())
        );
        List<Long> studentIds = userRoles.stream()
            .map(UserRole::getUserId)
            .distinct()
            .collect(Collectors.toList());
        List<UserDetailVO> all = studentIds.stream()
            .map(id -> userService.getCurrentUserDetail(id))
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
    public PageResult<UserDetailVO> getDepartmentStudents(Long schoolUserId, Long departmentId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        Department department = departmentMapper.selectById(departmentId);
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("院系不存在或不属于您的学校");
        }
        List<Clazz> classes = clazzMapper.selectList(
            new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getDepartmentId, departmentId)
        );
        List<Long> classIds = classes.stream()
            .map(Clazz::getId)
            .collect(Collectors.toList());
        if (classIds.isEmpty()) {
            return PageResult.empty(p, s);
        }
        List<ClassUser> classUsers = classUserMapper.selectList(
            new LambdaQueryWrapper<ClassUser>()
                .in(ClassUser::getClassId, classIds)
                .eq(ClassUser::getJoinStatus, "APPROVED")
        );
        List<Long> studentIds = classUsers.stream()
            .map(ClassUser::getUserId)
            .distinct()
            .collect(Collectors.toList());
        List<UserDetailVO> all = studentIds.stream()
            .map(id -> userService.getCurrentUserDetail(id))
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
    public PageResult<ClassMemberVO> getClassStudents(Long schoolUserId, Long classId, Integer page, Integer size) {
        int p = PaginationUtils.normalizePage(page);
        int s = PaginationUtils.normalizeSize(size);
        School school = getSchoolByUserId(schoolUserId);

        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }
        Department department = departmentMapper.selectById(clazz.getDepartmentId());
        if (department == null || !department.getSchoolId().equals(school.getId())) {
            throw new RuntimeException("班级不属于您的学校");
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
    
    @Override
    public UserDetailVO getStudent(Long schoolUserId, Long studentId) {
        School school = getSchoolByUserId(schoolUserId);
        
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, studentId)
        );
        
        if (profile == null || !school.getId().equals(profile.getSchoolId())) {
            throw new RuntimeException("学员不属于您的学校");
        }
        
        return userService.getCurrentUserDetail(studentId);
    }
    
    @Override
    @Transactional
    public void updateStudent(Long schoolUserId, Long studentId, UserUpdateDTO dto) {
        School school = getSchoolByUserId(schoolUserId);
        
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, studentId)
        );
        
        if (profile == null || !school.getId().equals(profile.getSchoolId())) {
            throw new RuntimeException("学员不属于您的学校");
        }
        
        userService.updateProfile(studentId, dto);
    }
    
    @Override
    public StatisticsVO getOverviewStatistics(Long schoolUserId) {
        School school = getSchoolByUserId(schoolUserId);
        
        StatisticsVO vo = new StatisticsVO();
        
        // 院系数
        long departmentCount = departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        
        // 班级数
        List<Department> departments = departmentMapper.selectList(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        List<Long> departmentIds = departments.stream()
            .map(Department::getId)
            .collect(Collectors.toList());
        
        long classCount = departmentIds.isEmpty() ? 0 :
            clazzMapper.selectCount(
                new LambdaQueryWrapper<Clazz>()
                    .in(Clazz::getDepartmentId, departmentIds)
            );
        
        // 教师数
        Role teacherRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "TEACHER")
        );
        long teacherCount = 0;
        if (teacherRole != null) {
            List<UserProfile> profiles = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getSchoolId, school.getId())
            );
            List<Long> userIds = profiles.stream()
                .map(UserProfile::getUserId)
                .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                teacherCount = userRoleMapper.selectCount(
                    new LambdaQueryWrapper<UserRole>()
                        .in(UserRole::getUserId, userIds)
                        .eq(UserRole::getRoleId, teacherRole.getId())
                );
            }
        }
        
        // 学员数
        Role userRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "USER")
        );
        long studentCount = 0;
        if (userRole != null) {
            List<UserProfile> profiles = userProfileMapper.selectList(
                new LambdaQueryWrapper<UserProfile>()
                    .eq(UserProfile::getSchoolId, school.getId())
            );
            List<Long> userIds = profiles.stream()
                .map(UserProfile::getUserId)
                .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                studentCount = userRoleMapper.selectCount(
                    new LambdaQueryWrapper<UserRole>()
                        .in(UserRole::getUserId, userIds)
                        .eq(UserRole::getRoleId, userRole.getId())
                );
            }
        }
        
        vo.setTotalCount(departmentCount + classCount + teacherCount + studentCount);
        
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("departments", departmentCount);
        statusCount.put("classes", classCount);
        statusCount.put("teachers", teacherCount);
        statusCount.put("students", studentCount);
        vo.setStatusCount(statusCount);
        
        return vo;
    }
    
    @Override
    public List<Map<String, Object>> getDepartmentStatistics(Long schoolUserId) {
        School school = getSchoolByUserId(schoolUserId);
        
        List<Department> departments = departmentMapper.selectList(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        
        return departments.stream().map(d -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("departmentId", d.getId());
            stat.put("departmentName", d.getName());
            
            // 班级数
            long classCount = clazzMapper.selectCount(
                new LambdaQueryWrapper<Clazz>()
                    .eq(Clazz::getDepartmentId, d.getId())
            );
            stat.put("classCount", classCount);
            
            // 学员数
            List<Clazz> classes = clazzMapper.selectList(
                new LambdaQueryWrapper<Clazz>()
                    .eq(Clazz::getDepartmentId, d.getId())
            );
            List<Long> classIds = classes.stream()
                .map(Clazz::getId)
                .collect(Collectors.toList());
            
            long studentCount = classIds.isEmpty() ? 0 :
                classUserMapper.selectCount(
                    new LambdaQueryWrapper<ClassUser>()
                        .in(ClassUser::getClassId, classIds)
                        .eq(ClassUser::getJoinStatus, "APPROVED")
                );
            stat.put("studentCount", studentCount);
            
            return stat;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getClassStatistics(Long schoolUserId) {
        School school = getSchoolByUserId(schoolUserId);
        
        List<Department> departments = departmentMapper.selectList(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        
        List<Long> departmentIds = departments.stream()
            .map(Department::getId)
            .collect(Collectors.toList());
        
        if (departmentIds.isEmpty()) {
            return List.of();
        }
        
        List<Clazz> classes = clazzMapper.selectList(
            new LambdaQueryWrapper<Clazz>()
                .in(Clazz::getDepartmentId, departmentIds)
        );
        
        return classes.stream().map(c -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("classId", c.getId());
            stat.put("className", c.getName());
            
            // 学员数
            long studentCount = classUserMapper.selectCount(
                new LambdaQueryWrapper<ClassUser>()
                    .eq(ClassUser::getClassId, c.getId())
                    .eq(ClassUser::getJoinStatus, "APPROVED")
            );
            stat.put("studentCount", studentCount);
            
            // 教师数
            long teacherCount = 1; // 班主任
            long additionalTeachers = classTeacherMapper.selectCount(
                new LambdaQueryWrapper<ClassTeacher>()
                    .eq(ClassTeacher::getClassId, c.getId())
            );
            teacherCount += additionalTeachers;
            stat.put("teacherCount", teacherCount);
            
            return stat;
        }).collect(Collectors.toList());
    }
    
    private School getSchoolByUserId(Long schoolUserId) {
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, schoolUserId)
        );
        
        if (profile == null || profile.getSchoolId() == null) {
            throw new RuntimeException("您未关联学校");
        }
        
        School school = schoolMapper.selectById(profile.getSchoolId());
        if (school == null) {
            throw new RuntimeException("学校不存在");
        }
        
        return school;
    }
    
    private SchoolVO buildSchoolVO(School school) {
        SchoolVO vo = new SchoolVO();
        vo.setId(school.getId());
        vo.setName(school.getName());
        vo.setContact(school.getContact());
        vo.setStatus(school.getStatus());
        vo.setCertifiedAt(school.getCertifiedAt());
        vo.setCreatedAt(school.getCreatedAt());
        vo.setUpdatedAt(school.getUpdatedAt());
        
        // 统计信息
        long departmentCount = departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        vo.setDepartmentCount(departmentCount);
        
        List<Department> departments = departmentMapper.selectList(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getSchoolId, school.getId())
        );
        List<Long> departmentIds = departments.stream()
            .map(Department::getId)
            .collect(Collectors.toList());
        
        long classCount = departmentIds.isEmpty() ? 0 :
            clazzMapper.selectCount(
                new LambdaQueryWrapper<Clazz>()
                    .in(Clazz::getDepartmentId, departmentIds)
            );
        vo.setClassCount(classCount);
        
        // TODO: 计算教师数和学员数
        vo.setTeacherCount(0L);
        vo.setStudentCount(0L);
        
        return vo;
    }
    
    private ClassVO buildClassVO(Clazz clazz) {
        ClassVO vo = new ClassVO();
        vo.setId(clazz.getId());
        vo.setDepartmentId(clazz.getDepartmentId());
        vo.setName(clazz.getName());
        vo.setYear(clazz.getYear());
        vo.setTeacherId(clazz.getTeacherId());
        vo.setMerk(clazz.getMerk());
        vo.setCreatedAt(clazz.getCreatedAt());
        vo.setUpdatedAt(clazz.getUpdatedAt());
        
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




