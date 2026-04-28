package com.example.ojpt.service;

import com.example.ojpt.config.JwtProperties;
import com.example.ojpt.dto.RegisterRequestDTO;
import com.example.ojpt.entity.Role;
import com.example.ojpt.entity.User;
import com.example.ojpt.entity.UserProfile;
import com.example.ojpt.entity.UserRole;
import com.example.ojpt.mapper.RoleMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.mapper.UserProfileMapper;
import com.example.ojpt.mapper.UserRoleMapper;
import com.example.ojpt.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRegistrationServiceTest {

    @Test
    void registerWithEmail_createsEnabledUserProfileAndUserRole() {
        UserMapper userMapper = mock(UserMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        JwtProperties jwtProperties = new JwtProperties();
        UserService service = new UserServiceImpl(
                userMapper,
                userRoleMapper,
                roleMapper,
                userProfileMapper,
                passwordEncoder,
                redisTemplate,
                jwtProperties
        );

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("pass1234")).thenReturn("encoded-password");
        when(roleMapper.selectOne(any())).thenReturn(new Role().setId(100L).setCode("USER"));
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(200L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setAccount("new-user@example.com");
        dto.setPassword("pass1234");
        dto.setNickname("小明");
        dto.setGender(1);

        User registered = service.register(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertSame(savedUser, registered);
        assertEquals(200L, savedUser.getId());
        assertEquals("小明", savedUser.getUsername());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals("new-user@example.com", savedUser.getEmail());
        assertNull(savedUser.getPhone());
        assertEquals(1, savedUser.getStatus());
        assertEquals("USER", savedUser.getRoleType());
        assertEquals(0, savedUser.getIsDeleted());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).insert(profileCaptor.capture());
        assertEquals(200L, profileCaptor.getValue().getUserId());
        assertEquals(1, profileCaptor.getValue().getGender());
        assertNull(profileCaptor.getValue().getBirthday());

        ArgumentCaptor<UserRole> userRoleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleMapper).insert(userRoleCaptor.capture());
        assertEquals(200L, userRoleCaptor.getValue().getUserId());
        assertEquals(100L, userRoleCaptor.getValue().getRoleId());
        assertEquals("REGISTER", userRoleCaptor.getValue().getBindSource());
    }

    @Test
    void registerWithPhone_rejectsDuplicatePhone() {
        UserMapper userMapper = mock(UserMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        JwtProperties jwtProperties = new JwtProperties();
        UserService service = new UserServiceImpl(
                userMapper,
                userRoleMapper,
                roleMapper,
                userProfileMapper,
                passwordEncoder,
                redisTemplate,
                jwtProperties
        );

        when(userMapper.selectOne(any())).thenReturn(new User().setId(10L).setPhone("13800138000"));

        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setAccount("13800138000");
        dto.setPassword("pass1234");
        dto.setNickname("小红");
        dto.setGender(2);
        dto.setBirthday(LocalDate.of(2001, 3, 4));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.register(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userMapper, never()).insert(any(User.class));
        verify(userProfileMapper, never()).insert(any(UserProfile.class));
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }
}
