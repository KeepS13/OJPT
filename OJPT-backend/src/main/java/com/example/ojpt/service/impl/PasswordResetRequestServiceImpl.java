package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ojpt.entity.PasswordResetRequest;
import com.example.ojpt.entity.User;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.PasswordResetRequestMapper;
import com.example.ojpt.mapper.UserMapper;
import com.example.ojpt.security.RefreshTokenStore;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.service.PasswordResetRequestService;
import com.example.ojpt.vo.PasswordResetRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PasswordResetRequestServiceImpl implements PasswordResetRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String DEFAULT_RESET_PASSWORD = "123456";

    private final PasswordResetRequestMapper requestMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistService tokenBlacklistService;
    @Value("${ojpt.jwt.access-exp-seconds:1800}")
    private long accessTokenTtlSeconds;

    @Override
    @Transactional
    public void submitRequest(String account) {
        String normalizedAccount = normalizeAccount(account);
        if (normalizedAccount.isBlank()) {
            return;
        }

        User user = findUser(normalizedAccount);
        if (user == null) {
            return;
        }

        // Lock the target user row so concurrent reset requests for the same user
        // serialize before checking/inserting pending requests.
        userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, user.getId())
                .last("FOR UPDATE"));

        Long pendingCount = requestMapper.selectCount(new LambdaQueryWrapper<PasswordResetRequest>()
                .eq(PasswordResetRequest::getUserId, user.getId())
                .eq(PasswordResetRequest::getStatus, STATUS_PENDING));
        if (pendingCount != null && pendingCount > 0) {
            return;
        }

        requestMapper.insert(new PasswordResetRequest()
                .setUserId(user.getId())
                .setAccountIdentifier(normalizedAccount)
                .setStatus(STATUS_PENDING));
    }

    @Override
    public List<PasswordResetRequestVO> listRequests(String status) {
        LambdaQueryWrapper<PasswordResetRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(PasswordResetRequest::getStatus, status.trim().toUpperCase());
        }
        wrapper.orderByDesc(PasswordResetRequest::getCreatedAt);

        List<PasswordResetRequest> requests = requestMapper.selectList(wrapper);
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = requests.stream()
                .map(PasswordResetRequest::getUserId)
                .distinct()
                .toList();
        Map<Long, User> usersById = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return requests.stream()
                .map(request -> toVO(request, usersById.get(request.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void approveRequest(Long requestId, Long reviewerId) {
        PasswordResetRequest request = getRequestOrThrow(requestId);
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw BusinessException.userNotFound();
        }

        markReviewed(requestId, STATUS_APPROVED, reviewerId);

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(passwordEncoder.encode(DEFAULT_RESET_PASSWORD));
        userMapper.updateById(updateUser);
        refreshTokenStore.deleteAllByUserId(user.getId());
        tokenBlacklistService.addPermissionChangeBlacklist(user.getId(), accessTokenTtlSeconds);
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, Long reviewerId) {
        getRequestOrThrow(requestId);
        markReviewed(requestId, STATUS_REJECTED, reviewerId);
    }

    private PasswordResetRequest getRequestOrThrow(Long requestId) {
        PasswordResetRequest request = requestMapper.selectById(requestId);
        if (request == null) {
            throw BusinessException.notFound("password reset request");
        }
        return request;
    }

    private void markReviewed(Long requestId, String status, Long reviewerId) {
        PasswordResetRequest update = new PasswordResetRequest()
                .setStatus(status)
                .setReviewedBy(reviewerId)
                .setReviewedAt(LocalDateTime.now());
        int updated = requestMapper.update(update, new LambdaQueryWrapper<PasswordResetRequest>()
                .eq(PasswordResetRequest::getId, requestId)
                .eq(PasswordResetRequest::getStatus, STATUS_PENDING));
        if (updated == 0) {
            throw BusinessException.badRequest("password reset request has been reviewed");
        }
    }

    private PasswordResetRequestVO toVO(PasswordResetRequest request, User user) {
        PasswordResetRequestVO vo = new PasswordResetRequestVO()
                .setId(request.getId())
                .setUserId(request.getUserId())
                .setAccountIdentifier(request.getAccountIdentifier())
                .setStatus(request.getStatus())
                .setReviewedBy(request.getReviewedBy())
                .setReviewedAt(request.getReviewedAt())
                .setCreatedAt(request.getCreatedAt());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setEmail(user.getEmail());
        }
        return vo;
    }

    private User findUser(String account) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .last("LIMIT 1");
        if (account.contains("@")) {
            wrapper.eq(User::getEmail, account.toLowerCase());
        } else if (account.matches("^1[3-9]\\d{9}$")) {
            wrapper.eq(User::getPhone, account);
        } else {
            wrapper.eq(User::getUsername, account);
        }
        return userMapper.selectOne(wrapper);
    }

    private String normalizeAccount(String account) {
        if (account == null) {
            return "";
        }
        String trimmed = account.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase() : trimmed;
    }
}
