package com.example.ojpt.security;

import com.example.ojpt.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties props;

    /**
     * 将刷新 token 的 jti 存入 Redis，值为 refresh token 本身，便于对比。
     */
    public void store(Long userId, String jti, String refreshToken) {
        String key = buildKey(userId, jti);
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(props.getRefreshExpSeconds()));
    }

    /**
     * 校验 jti 是否存在且一致。
     */
    public boolean validate(Long userId, String jti, String refreshToken) {
        String key = buildKey(userId, jti);
        String val = redisTemplate.opsForValue().get(key);
        return refreshToken.equals(val);
    }

    public enum ValidateResult {
        VALID,
        INVALID,
        ROTATED
    }

    public ValidateResult validateWithRotation(Long userId, String jti, String refreshToken) {
        String key = buildKey(userId, jti);
        String val = redisTemplate.opsForValue().get(key);
        if (refreshToken.equals(val)) {
            return ValidateResult.VALID;
        }
        if (isRotated(userId, jti)) {
            return ValidateResult.ROTATED;
        }
        return ValidateResult.INVALID;
    }

    public void markRotated(Long userId, String jti) {
        String key = buildRotatedKey(userId, jti);
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(props.getRefreshExpSeconds()));
    }

    public boolean isRotated(Long userId, String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildRotatedKey(userId, jti)));
    }

    /**
     * 轮换时删除旧 jti。
     */
    public void delete(Long userId, String jti) {
        redisTemplate.delete(buildKey(userId, jti));
    }

    /**
     * 删除用户的所有 refresh token（强制所有设备下线）。
     * 通过匹配 key 模式来删除所有相关的 token。
     * 
     * @param userId 用户ID
     */
    public void deleteAllByUserId(Long userId) {
        Set<String> allKeys = new HashSet<>();

        String refreshPattern = props.getRedisPrefix() + "refresh:" + userId + ":*";
        Set<String> refreshKeys = redisTemplate.keys(refreshPattern);
        if (refreshKeys != null) allKeys.addAll(refreshKeys);

        String rotatedPattern = props.getRedisPrefix() + "refresh:rotated:" + userId + ":*";
        Set<String> rotatedKeys = redisTemplate.keys(rotatedPattern);
        if (rotatedKeys != null) allKeys.addAll(rotatedKeys);

        if (!allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
        }
    }

    private String buildKey(Long userId, String jti) {
        return props.getRedisPrefix() + "refresh:" + userId + ":" + jti;
    }

    private String buildRotatedKey(Long userId, String jti) {
        return props.getRedisPrefix() + "refresh:rotated:" + userId + ":" + jti;
    }
}

