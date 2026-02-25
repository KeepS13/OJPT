package com.example.ojpt.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1) 查用户基础信息
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 2) 收集主角色（role_type 字段）
        Set<String> roleCodes = new HashSet<>();
        if (user.getRoleType() != null) {
            roleCodes.add(user.getRoleType());
        }

        // 3) 查用户-角色关系，补充所有绑定角色
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            roleCodes.addAll(roles.stream().map(Role::getCode).collect(Collectors.toSet()));
        }

        // 4) 角色编码 -> Spring Security 权限（加 ROLE_ 前缀以兼容 hasRole）
        List<GrantedAuthority> authorities = roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toList());

        // 5) 账号状态：
        //    0 = 禁用（不允许登录）
        //    1 = 启用
        //    2 = 待审核（允许完成密码校验，但登录成功后再拦截提示待审核）
        Integer status = user.getStatus();
        boolean locked = status != null && status == 0;
        boolean enabled = status == null || status != 0; // 仅禁用时关闭
        return new LoginUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                enabled,
                true,
                true,
                !locked,
                status
        );
    }
}

