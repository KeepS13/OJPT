package com.example.ojpt.service;

import com.example.ojpt.entity.PasswordResetRequest;
import com.example.ojpt.entity.User;
import com.example.ojpt.mapper.PasswordResetRequestMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.security.RefreshTokenStore;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.impl.PasswordResetRequestServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetRequestServiceTest {

    private PasswordResetRequestService newService(
            PasswordResetRequestMapper requestMapper,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            RefreshTokenStore refreshTokenStore,
            TokenBlacklistService tokenBlacklistService
    ) {
        PasswordResetRequestServiceImpl service = new PasswordResetRequestServiceImpl(
                requestMapper,
                userMapper,
                passwordEncoder,
                refreshTokenStore,
                tokenBlacklistService
        );
        ReflectionTestUtils.setField(service, "accessTokenTtlSeconds", 1800L);
        return service;
    }

    @Test
    void submitRequest_createsPendingRequestForExistingUsername() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetRequestMapper requestMapper = mock(PasswordResetRequestMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        PasswordResetRequestService service = newService(
                requestMapper,
                userMapper,
                passwordEncoder,
                refreshTokenStore,
                tokenBlacklistService
        );

        when(userMapper.selectOne(any())).thenReturn(new User().setId(10L).setUsername("alice"));
        when(requestMapper.selectCount(any())).thenReturn(0L);

        service.submitRequest(" alice ");

        ArgumentCaptor<PasswordResetRequest> captor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(requestMapper).insert(captor.capture());
        PasswordResetRequest saved = captor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals("alice", saved.getAccountIdentifier());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void submitRequest_returnsSilentlyWhenAccountDoesNotExist() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetRequestMapper requestMapper = mock(PasswordResetRequestMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        PasswordResetRequestService service = newService(
                requestMapper,
                userMapper,
                passwordEncoder,
                refreshTokenStore,
                tokenBlacklistService
        );

        when(userMapper.selectOne(any())).thenReturn(null);

        service.submitRequest("missing@example.com");

        verify(requestMapper, never()).insert(any(PasswordResetRequest.class));
    }

    @Test
    void approveRequest_resetsPasswordToEncodedDefaultAndMarksApproved() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetRequestMapper requestMapper = mock(PasswordResetRequestMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        PasswordResetRequestService service = newService(
                requestMapper,
                userMapper,
                passwordEncoder,
                refreshTokenStore,
                tokenBlacklistService
        );

        PasswordResetRequest request = new PasswordResetRequest()
                .setId(99L)
                .setUserId(10L)
                .setStatus("PENDING");
        when(requestMapper.selectById(99L)).thenReturn(request);
        when(userMapper.selectById(10L)).thenReturn(new User().setId(10L).setPassword("old"));
        when(passwordEncoder.encode("123456")).thenReturn("encoded-default");
        when(requestMapper.update(any(PasswordResetRequest.class), any())).thenReturn(1);

        service.approveRequest(99L, 1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(10L, userCaptor.getValue().getId());
        assertEquals("encoded-default", userCaptor.getValue().getPassword());
        verify(refreshTokenStore).deleteAllByUserId(10L);
        verify(tokenBlacklistService).addPermissionChangeBlacklist(10L, 1800L);

        ArgumentCaptor<PasswordResetRequest> requestCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(requestMapper).update(requestCaptor.capture(), any());
        PasswordResetRequest updated = requestCaptor.getValue();
        assertEquals("APPROVED", updated.getStatus());
        assertEquals(1L, updated.getReviewedBy());
        assertNotNull(updated.getReviewedAt());
    }

    @Test
    void rejectRequest_marksRejectedWithoutChangingPassword() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetRequestMapper requestMapper = mock(PasswordResetRequestMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        PasswordResetRequestService service = newService(
                requestMapper,
                userMapper,
                passwordEncoder,
                refreshTokenStore,
                tokenBlacklistService
        );

        PasswordResetRequest request = new PasswordResetRequest()
                .setId(99L)
                .setUserId(10L)
                .setStatus("PENDING");
        when(requestMapper.selectById(99L)).thenReturn(request);
        when(requestMapper.update(any(PasswordResetRequest.class), any())).thenReturn(1);

        service.rejectRequest(99L, 1L);

        verify(userMapper, never()).updateById(any(User.class));
        ArgumentCaptor<PasswordResetRequest> requestCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(requestMapper).update(requestCaptor.capture(), any());
        assertEquals("REJECTED", requestCaptor.getValue().getStatus());
        assertEquals(1L, requestCaptor.getValue().getReviewedBy());
    }

    @Test
    void listRequests_returnsUserSnapshotForAdmin() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetRequestMapper requestMapper = mock(PasswordResetRequestMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        PasswordResetRequestService service = newService(
                requestMapper,
                userMapper,
                passwordEncoder,
                refreshTokenStore,
                tokenBlacklistService
        );

        when(requestMapper.selectList(any())).thenReturn(List.of(
                new PasswordResetRequest().setId(99L).setUserId(10L).setAccountIdentifier("alice").setStatus("PENDING")
        ));
        when(userMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(
                new User().setId(10L).setUsername("alice").setEmail("alice@example.com")
        ));

        var result = service.listRequests("PENDING");

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).getId());
        assertEquals("alice", result.get(0).getUsername());
        assertEquals("alice@example.com", result.get(0).getEmail());
    }
}
