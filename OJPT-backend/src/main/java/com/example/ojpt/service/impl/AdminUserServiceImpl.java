package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ojpt.dto.AdminUserCreateDTO;
import com.example.ojpt.vo.AdminUserVO;
import com.example.ojpt.converter.AdminUserConverter;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import com.example.ojpt.security.RefreshTokenStore;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final AdminUserConverter adminUserConverter;
    private final TokenBlacklistService tokenBlacklistService;
    @SuppressWarnings("unused")
    private final RefreshTokenStore refreshTokenStore; // 预留：用于将来删除用户的所有 refresh token
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    
    // 用户黑名单有效期：30 天（秒）
    private static final long USER_BLACKLIST_TTL_SECONDS = 30L * 24 * 60 * 60;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createUser(AdminUserCreateDTO dto) {
        // 1. 参数整理与去重
        List<String> roleCodes = normalizeRoleCodes(dto.getRoleCodes());

        // 2. 基本校验：用户名/邮箱唯一
        ensureUsernameAvailable(dto.getUsername());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            ensureEmailAvailable(dto.getEmail());
        }

        // 3. 校验角色合法性
        List<Role> roles = loadRoles(roleCodes);
        String primaryRoleCode = determinePrimaryRole(roles, roleCodes);

        // 4. 构造并落库用户
        User user = buildUser(dto, primaryRoleCode);
        userMapper.insert(user);

        // 5. 绑定角色关系并落库用户权限
        for (Role role : roles) {
            UserRole userRole = new UserRole()
                    .setUserId(user.getId())
                    .setRoleId(role.getId())
                    .setBindSource("ADMIN_CREATE");
            userRoleMapper.insert(userRole);
        }

        // 6. 返回脱敏信息（MapStruct 编译期映射）
        return adminUserConverter.toVo(user, roleCodes);
    }

    /**
     * 根据角色定义的 level 挑选优先级最高的角色。
     * 若 level 并列，则按前端传入顺序（normalize 后的顺序）取最前。
     */
    private String determinePrimaryRole(List<Role> roles, List<String> orderedRoleCodes) {
        if (roles.isEmpty()) {
            return "USER";
        }
        // level 越大优先级越高
        int maxLevel = roles.stream()
                .map(Role::getLevel)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        Set<String> topLevelCodes = roles.stream()
                .filter(r -> Objects.equals(r.getLevel(), maxLevel))
                .map(Role::getCode)
                .collect(Collectors.toSet());

        return orderedRoleCodes.stream()
                .filter(topLevelCodes::contains)
                .findFirst()
                .orElse(roles.get(0).getCode());
    }

    private List<String> normalizeRoleCodes(List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return List.of("USER");
        }
        Set<String> distinct = new HashSet<>();
        List<String> normalized = roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .filter(distinct::add)
                .toList();
        if (normalized.isEmpty()) {
            return List.of("USER");
        }
        return normalized;
    }

    private void ensureUsernameAvailable(String username) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
    }

    private void ensureEmailAvailable(String email) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已存在");
        }
    }

    private List<Role> loadRoles(List<String> roleCodes) {
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .in(Role::getCode, roleCodes));
        Set<String> foundCodes = roles.stream().map(Role::getCode).collect(Collectors.toSet());
        List<String> missing = roleCodes.stream().filter(code -> !foundCodes.contains(code)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不存在：" + String.join(",", missing));
        }
        return roles;
    }

    private User buildUser(AdminUserCreateDTO dto, String primaryRoleCode) {
        return new User()
                .setUsername(dto.getUsername())
                .setPassword(PASSWORD_ENCODER.encode(dto.getPassword()))
                .setEmail(dto.getEmail())
                .setPhone(dto.getPhone())
                .setStatus(1)
                .setRoleType(primaryRoleCode);
    }

    @Override
    public void blacklistUser(Long userId, Long ttlSeconds) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 2. 将用户加入黑名单，使用指定时长；未指定或非法则使用默认 30 天
        long ttl = (ttlSeconds == null || ttlSeconds <= 0) ? USER_BLACKLIST_TTL_SECONDS : ttlSeconds;
        tokenBlacklistService.addUserToBlacklist(userId, ttl);

        // 3. 可选：删除该用户的所有 refresh token（强制所有设备下线）
        // 注意：这里需要知道所有 refresh token 的 jti，但当前设计中没有维护这个映射
        // 如果需要精确删除，需要额外维护 userId -> refresh token jti 的映射关系
        // 当前实现：用户黑名单生效后，所有请求都会被拦截，refresh token 虽然还在但无法使用
    }

    @Override
    public void unblacklistUser(Long userId) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }

        // 2. 从黑名单中移除用户
        tokenBlacklistService.removeUserFromBlacklist(userId);
    }
}

