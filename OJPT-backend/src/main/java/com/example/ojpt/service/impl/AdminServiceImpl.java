package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ojpt.common.PageResult;
import com.example.ojpt.dto.PermissionCreateDTO;
import com.example.ojpt.dto.PermissionUpdateDTO;
import com.example.ojpt.dto.RoleCreateDTO;
import com.example.ojpt.dto.RolePermissionAssignDTO;
import com.example.ojpt.dto.RoleUpdateDTO;
import com.example.ojpt.dto.SchoolCreateDTO;
import com.example.ojpt.dto.SchoolUpdateDTO;
import com.example.ojpt.dto.UserRoleUpdateDTO;
import com.example.ojpt.dto.UserUpdateDTO;
import com.example.ojpt.entity.Permission;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.RolePermission;
import com.example.ojpt.entity.School;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.exception.ErrorCode;
import com.example.ojpt.mapper.PermissionMapper;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.RolePermissionMapper;
import com.example.ojpt.mapper.SchoolMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import com.example.ojpt.security.RefreshTokenStore;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.AdminService;
import com.example.ojpt.service.UserService;
import com.example.ojpt.vo.PermissionVO;
import com.example.ojpt.vo.RoleVO;
import com.example.ojpt.vo.SchoolVO;
import com.example.ojpt.vo.StatisticsVO;
import com.example.ojpt.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final SchoolMapper schoolMapper;
    private final UserService userService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistService tokenBlacklistService;
    
    // 权限变更后的临时黑名单有效期（15分钟），与 access token 过期时间一致
    private static final long PERMISSION_CHANGE_BLACKLIST_TTL_SECONDS = 15 * 60;
    
    @Override
    public PageResult<UserDetailVO> getUsers(Integer page, Integer size, Integer status, String roleType, String keyword) {
        log.debug("查询用户列表: page={}, size={}, status={}, roleType={}, keyword={}", 
                page, size, status, roleType, keyword);
        
        Page<User> userPage = new Page<>(page != null ? page : 1, size != null ? size : 10);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIsDeleted, 0);
        
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (roleType != null) {
            wrapper.eq(User::getRoleType, roleType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                .or().like(User::getEmail, keyword)
                .or().like(User::getPhone, keyword));
        }
        
        Page<User> result = userMapper.selectPage(userPage, wrapper);
        
        List<UserDetailVO> records = result.getRecords().stream()
            .map(u -> userService.getCurrentUserDetail(u.getId()))
            .collect(Collectors.toList());
        
        log.debug("查询用户列表完成: total={}, pages={}", result.getTotal(), result.getPages());
        
        return PageResult.<UserDetailVO>builder()
                .records(records)
                .total(result.getTotal())
                .current(result.getCurrent())
                .size(result.getSize())
                .pages(result.getPages())
                .build();
    }
    
    @Override
    public UserDetailVO getUser(Long userId) {
        UserDetailVO detail = userService.getCurrentUserDetail(userId);
        if (detail == null) {
            throw BusinessException.userNotFound();
        }
        return detail;
    }
    
    @Override
    @Transactional
    public void updateUser(Long userId, UserUpdateDTO dto) {
        log.info("管理员更新用户信息: userId={}", userId);
        userService.updateProfile(userId, dto);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("管理员删除用户: userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        
        user.setIsDeleted(1);
        userMapper.updateById(user);
        
        log.info("用户已标记删除: userId={}", userId);
    }
    
    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        if (userId == null) {
            throw BusinessException.badRequest("用户ID不能为空");
        }
        if (status == null) {
            throw BusinessException.badRequest("状态值不能为空");
        }
        
        log.info("更新用户状态: userId={}, status={}", userId, status);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        
        user.setStatus(status);
        userMapper.updateById(user);
        
        // 如果状态变更为禁用，也强制用户重新登录
        if (status == 0) {
            forceUserReLogin(userId);
            log.info("用户已禁用并强制重新登录: userId={}", userId);
        }
    }
    
    @Override
    @Transactional
    public void updateUserRoles(Long userId, UserRoleUpdateDTO dto) {
        log.info("更新用户角色: userId={}, roles={}", userId, dto.getRoleCodes());
        
        // 删除旧的角色绑定
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
            .eq(UserRole::getUserId, userId));
        
        // 创建新的角色绑定
        for (String roleCode : dto.getRoleCodes()) {
            Role role = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                    .eq(Role::getCode, roleCode)
            );
            
            if (role == null) {
                throw BusinessException.roleNotFound(roleCode);
            }
            
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setBindSource("ADMIN_UPDATE");
            userRoleMapper.insert(userRole);
        }
        
        // 权限变更后，强制用户重新登录以刷新 token 中的角色信息
        forceUserReLogin(userId);
        log.info("用户角色更新完成，已强制重新登录: userId={}", userId);
    }
    
    /**
     * 强制用户重新登录：删除所有 refresh token，并加入权限变更临时黑名单。
     * 这样用户的所有请求会被拒绝，refresh token 也无法使用，必须重新登录。
     * 
     * @param userId 用户ID
     */
    private void forceUserReLogin(Long userId) {
        // 1. 删除用户的所有 refresh token（使所有设备的 refresh token 失效）
        refreshTokenStore.deleteAllByUserId(userId);
        
        // 2. 将用户加入权限变更临时黑名单（15分钟），拒绝所有 access token 请求
        // 这样即使用户有有效的 access token，也会被拒绝，必须重新登录
        tokenBlacklistService.addPermissionChangeBlacklist(userId, PERMISSION_CHANGE_BLACKLIST_TTL_SECONDS);
    }
    
    @Override
    public List<RoleVO> getRoles() {
        List<Role> roles = roleMapper.selectList(null);
        
        return roles.stream().map(r -> {
            RoleVO vo = new RoleVO();
            vo.setId(r.getId());
            vo.setCode(r.getCode());
            vo.setName(r.getName());
            vo.setDescription(r.getDescription());
            vo.setLevel(r.getLevel());
            vo.setCreatedAt(r.getCreatedAt());
            vo.setUpdatedAt(r.getUpdatedAt());
            
            // 统计权限数量
            long permissionCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<RolePermission>()
                    .eq(RolePermission::getRoleId, r.getId())
            );
            vo.setPermissionCount(permissionCount);
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public RoleVO createRole(RoleCreateDTO dto) {
        log.info("创建角色: code={}, name={}", dto.getCode(), dto.getName());
        
        // 检查code是否已存在
        Role existing = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getCode, dto.getCode())
        );
        
        if (existing != null) {
            throw BusinessException.roleCodeExists();
        }
        
        Role role = new Role();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setLevel(dto.getLevel() != null ? dto.getLevel() : 0);
        
        roleMapper.insert(role);
        
        log.info("角色创建成功: roleId={}", role.getId());
        
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setLevel(role.getLevel());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());
        vo.setPermissionCount(0L);
        
        return vo;
    }
    
    @Override
    public RoleVO getRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.roleNotFound();
        }
        
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setLevel(role.getLevel());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());
        
        // 获取权限列表
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId)
        );
        
        List<Long> permissionIds = rolePermissions.stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toList());
        
        if (!permissionIds.isEmpty()) {
            List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
            List<PermissionVO> permissionVOs = permissions.stream()
                .map(p -> {
                    PermissionVO pvo = new PermissionVO();
                    pvo.setId(p.getId());
                    pvo.setResource(p.getResource());
                    pvo.setAction(p.getAction());
                    pvo.setConditionJson(p.getConditionJson());
                    pvo.setDescription(p.getDescription());
                    pvo.setCreatedAt(p.getCreatedAt());
                    pvo.setUpdatedAt(p.getUpdatedAt());
                    return pvo;
                })
                .collect(Collectors.toList());
            vo.setPermissions(permissionVOs);
        }
        
        vo.setPermissionCount((long) (vo.getPermissions() != null ? vo.getPermissions().size() : 0));
        
        return vo;
    }
    
    @Override
    @Transactional
    public void updateRole(Long roleId, RoleUpdateDTO dto) {
        log.info("更新角色: roleId={}", roleId);
        
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.roleNotFound();
        }
        
        if (dto.getName() != null) {
            role.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        if (dto.getLevel() != null) {
            role.setLevel(dto.getLevel());
        }
        
        roleMapper.updateById(role);
    }
    
    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        log.info("删除角色: roleId={}", roleId);
        
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.roleNotFound();
        }
        
        // 检查是否有用户绑定
        long userCount = userRoleMapper.selectCount(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, roleId)
        );
        
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS);
        }
        
        // 删除权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
            .eq(RolePermission::getRoleId, roleId));
        
        roleMapper.deleteById(roleId);
        log.info("角色删除成功: roleId={}", roleId);
    }
    
    @Override
    public List<PermissionVO> getPermissions(String resource, String action, String keyword) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        
        if (resource != null && !resource.isEmpty()) {
            wrapper.eq(Permission::getResource, resource);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(Permission::getAction, action);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Permission::getResource, keyword)
                .or().like(Permission::getAction, keyword)
                .or().like(Permission::getDescription, keyword));
        }
        
        List<Permission> permissions = permissionMapper.selectList(wrapper);
        
        return permissions.stream().map(p -> {
            PermissionVO vo = new PermissionVO();
            vo.setId(p.getId());
            vo.setResource(p.getResource());
            vo.setAction(p.getAction());
            vo.setConditionJson(p.getConditionJson());
            vo.setDescription(p.getDescription());
            vo.setCreatedAt(p.getCreatedAt());
            vo.setUpdatedAt(p.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public PermissionVO createPermission(PermissionCreateDTO dto) {
        log.info("创建权限: resource={}, action={}", dto.getResource(), dto.getAction());
        
        // 检查是否已存在
        Permission existing = permissionMapper.selectOne(
            new LambdaQueryWrapper<Permission>()
                .eq(Permission::getResource, dto.getResource())
                .eq(Permission::getAction, dto.getAction())
        );
        
        if (existing != null) {
            throw BusinessException.permissionExists();
        }
        
        Permission permission = new Permission();
        permission.setResource(dto.getResource());
        permission.setAction(dto.getAction());
        permission.setConditionJson(dto.getConditionJson());
        permission.setDescription(dto.getDescription());
        
        permissionMapper.insert(permission);
        
        log.info("权限创建成功: permissionId={}", permission.getId());
        
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setResource(permission.getResource());
        vo.setAction(permission.getAction());
        vo.setConditionJson(permission.getConditionJson());
        vo.setDescription(permission.getDescription());
        vo.setCreatedAt(permission.getCreatedAt());
        vo.setUpdatedAt(permission.getUpdatedAt());
        
        return vo;
    }
    
    @Override
    public PermissionVO getPermission(Long permissionId) {
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw BusinessException.permissionNotFound();
        }
        
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setResource(permission.getResource());
        vo.setAction(permission.getAction());
        vo.setConditionJson(permission.getConditionJson());
        vo.setDescription(permission.getDescription());
        vo.setCreatedAt(permission.getCreatedAt());
        vo.setUpdatedAt(permission.getUpdatedAt());
        
        return vo;
    }
    
    @Override
    @Transactional
    public void updatePermission(Long permissionId, PermissionUpdateDTO dto) {
        log.info("更新权限: permissionId={}", permissionId);
        
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw BusinessException.permissionNotFound();
        }
        
        if (dto.getConditionJson() != null) {
            permission.setConditionJson(dto.getConditionJson());
        }
        if (dto.getDescription() != null) {
            permission.setDescription(dto.getDescription());
        }
        
        permissionMapper.updateById(permission);
    }
    
    @Override
    @Transactional
    public void deletePermission(Long permissionId) {
        log.info("删除权限: permissionId={}", permissionId);
        
        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw BusinessException.permissionNotFound();
        }
        
        // 检查是否有角色关联
        long roleCount = rolePermissionMapper.selectCount(
            new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getPermissionId, permissionId)
        );
        
        if (roleCount > 0) {
            throw new BusinessException(ErrorCode.PERMISSION_IN_USE);
        }
        
        permissionMapper.deleteById(permissionId);
        log.info("权限删除成功: permissionId={}", permissionId);
    }
    
    @Override
    @Transactional
    public void assignPermissionsToRole(Long roleId, RolePermissionAssignDTO dto) {
        log.info("为角色分配权限: roleId={}, permissionIds={}", roleId, dto.getPermissionIds());
        
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.roleNotFound();
        }
        
        for (Long permissionId : dto.getPermissionIds()) {
            Permission permission = permissionMapper.selectById(permissionId);
            if (permission == null) {
                throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND, "权限不存在: " + permissionId);
            }
            
            // 检查是否已存在
            RolePermission existing = rolePermissionMapper.selectOne(
                new LambdaQueryWrapper<RolePermission>()
                    .eq(RolePermission::getRoleId, roleId)
                    .eq(RolePermission::getPermissionId, permissionId)
            );
            
            if (existing == null) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        
        log.info("角色权限分配成功: roleId={}", roleId);
    }
    
    @Override
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
            .eq(RolePermission::getRoleId, roleId)
            .eq(RolePermission::getPermissionId, permissionId));
    }
    
    @Override
    public PageResult<SchoolVO> getSchools(Integer page, Integer size, Integer status, String keyword) {
        log.debug("查询学校列表: page={}, size={}, status={}, keyword={}", page, size, status, keyword);
        
        Page<School> schoolPage = new Page<>(page != null ? page : 1, size != null ? size : 10);
        
        LambdaQueryWrapper<School> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(School::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(School::getName, keyword);
        }
        
        Page<School> result = schoolMapper.selectPage(schoolPage, wrapper);
        
        List<SchoolVO> records = result.getRecords().stream()
            .map(this::buildSchoolVO)
            .collect(Collectors.toList());
        
        log.debug("查询学校列表完成: total={}, pages={}", result.getTotal(), result.getPages());
        
        return PageResult.<SchoolVO>builder()
                .records(records)
                .total(result.getTotal())
                .current(result.getCurrent())
                .size(result.getSize())
                .pages(result.getPages())
                .build();
    }
    
    @Override
    @Transactional
    public SchoolVO createSchool(SchoolCreateDTO dto) {
        log.info("创建学校: name={}", dto.getName());
        
        School school = new School();
        school.setName(dto.getName());
        school.setContact(dto.getContact());
        school.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        
        schoolMapper.insert(school);
        
        log.info("学校创建成功: schoolId={}", school.getId());
        
        return buildSchoolVO(school);
    }
    
    @Override
    public SchoolVO getSchool(Long schoolId) {
        School school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw BusinessException.schoolNotFound();
        }
        
        return buildSchoolVO(school);
    }
    
    @Override
    @Transactional
    public void updateSchool(Long schoolId, SchoolUpdateDTO dto) {
        log.info("更新学校信息: schoolId={}", schoolId);
        
        School school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw BusinessException.schoolNotFound();
        }
        
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
    @Transactional
    public void deleteSchool(Long schoolId) {
        log.info("删除学校: schoolId={}", schoolId);
        
        School school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw BusinessException.schoolNotFound();
        }
        
        // FUTURE: 检查是否有关联数据（院系、班级等）
        schoolMapper.deleteById(schoolId);
        
        log.info("学校删除成功: schoolId={}", schoolId);
    }
    
    @Override
    @Transactional
    public void updateSchoolStatus(Long schoolId, Integer status) {
        if (schoolId == null) {
            throw BusinessException.badRequest("学校ID不能为空");
        }
        if (status == null) {
            throw BusinessException.badRequest("状态值不能为空");
        }
        
        log.info("更新学校状态: schoolId={}, status={}", schoolId, status);
        
        School school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw BusinessException.schoolNotFound();
        }
        
        school.setStatus(status);
        schoolMapper.updateById(school);
    }
    
    @Override
    @Transactional
    public void certifySchool(Long schoolId) {
        log.info("认证学校: schoolId={}", schoolId);
        
        School school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw BusinessException.schoolNotFound();
        }
        
        school.setStatus(1);
        school.setCertifiedAt(LocalDateTime.now());
        schoolMapper.updateById(school);
        
        log.info("学校认证成功: schoolId={}", schoolId);
    }
    
    @Override
    @Transactional
    public void uncertifySchool(Long schoolId) {
        log.info("取消学校认证: schoolId={}", schoolId);
        
        School school = schoolMapper.selectById(schoolId);
        if (school == null) {
            throw BusinessException.schoolNotFound();
        }
        
        school.setStatus(2);
        school.setCertifiedAt(null);
        schoolMapper.updateById(school);
        
        log.info("学校认证已取消: schoolId={}", schoolId);
    }
    
    @Override
    public StatisticsVO getOverviewStatistics() {
        StatisticsVO vo = new StatisticsVO();
        
        long userCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
        );
        
        long schoolCount = schoolMapper.selectCount(null);
        
        // TODO: 计算其他统计数据
        
        vo.setTotalCount(userCount + schoolCount);
        
        Map<String, Long> statusCount = new HashMap<>();
        statusCount.put("users", userCount);
        statusCount.put("schools", schoolCount);
        vo.setStatusCount(statusCount);
        
        return vo;
    }
    
    @Override
    public StatisticsVO getUserStatistics() {
        StatisticsVO vo = new StatisticsVO();
        
        long totalUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
        );
        
        // 按状态统计
        Map<String, Long> statusCount = new HashMap<>();
        for (int status = 0; status <= 2; status++) {
            long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .eq(User::getIsDeleted, 0)
                    .eq(User::getStatus, status)
            );
            statusCount.put(String.valueOf(status), count);
        }
        
        vo.setTotalCount(totalUsers);
        vo.setStatusCount(statusCount);
        
        return vo;
    }
    
    @Override
    public StatisticsVO getSchoolStatistics() {
        StatisticsVO vo = new StatisticsVO();
        
        long totalSchools = schoolMapper.selectCount(null);
        
        // 按状态统计
        Map<String, Long> statusCount = new HashMap<>();
        for (int status = 0; status <= 2; status++) {
            long count = schoolMapper.selectCount(
                new LambdaQueryWrapper<School>()
                    .eq(School::getStatus, status)
            );
            statusCount.put(String.valueOf(status), count);
        }
        
        vo.setTotalCount(totalSchools);
        vo.setStatusCount(statusCount);
        
        return vo;
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
        
        // TODO: 计算统计信息
        vo.setDepartmentCount(0L);
        vo.setClassCount(0L);
        vo.setTeacherCount(0L);
        vo.setStudentCount(0L);
        
        return vo;
    }
}



