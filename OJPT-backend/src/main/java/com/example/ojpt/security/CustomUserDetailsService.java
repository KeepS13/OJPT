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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        LinkedHashSet<String> roleCodes = new LinkedHashSet<>();
        if (user.getRoleType() != null) {
            roleCodes.add(user.getRoleType());
        }

        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, user.getId()));
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            roleCodes.addAll(roles.stream().map(Role::getCode).collect(Collectors.toSet()));
        }

        List<GrantedAuthority> authorities = SystemRoleScope.normalizeRoleCodes(roleCodes).stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toList());

        Integer status = user.getStatus();
        boolean locked = status != null && status == 0;
        boolean enabled = status == null || status != 0;
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
