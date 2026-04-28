package com.example.ojpt.security;

import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    @Test
    void loadUserByUsername_collapsesLegacyNonAdminRolesToUser() {
        UserMapper userMapper = mock(UserMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userMapper, userRoleMapper, roleMapper);

        User user = new User()
                .setId(1L)
                .setUsername("legacy-user")
                .setPassword("secret")
                .setStatus(1)
                .setRoleType("TEACHER");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(
                new UserRole().setUserId(1L).setRoleId(10L),
                new UserRole().setUserId(1L).setRoleId(11L)
        ));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(
                new Role().setId(10L).setCode("SCHOOL"),
                new Role().setId(11L).setCode("STUDENT")
        ));

        LoginUserDetails details = (LoginUserDetails) service.loadUserByUsername("legacy-user");

        Set<String> authorities = details.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_USER"), authorities);
    }

    @Test
    void loadUserByUsername_keepsOnlyAdminWhenAnyAdminRoleIsPresent() {
        UserMapper userMapper = mock(UserMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userMapper, userRoleMapper, roleMapper);

        User user = new User()
                .setId(2L)
                .setUsername("admin-user")
                .setPassword("secret")
                .setStatus(1)
                .setRoleType("USER");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(
                new UserRole().setUserId(2L).setRoleId(20L),
                new UserRole().setUserId(2L).setRoleId(21L)
        ));
        when(roleMapper.selectBatchIds(any())).thenReturn(List.of(
                new Role().setId(20L).setCode("ADMIN"),
                new Role().setId(21L).setCode("TEACHER")
        ));

        LoginUserDetails details = (LoginUserDetails) service.loadUserByUsername("admin-user");

        Set<String> authorities = details.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_ADMIN"), authorities);
    }
}
