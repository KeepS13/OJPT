package com.example.ojpt.security;

import com.example.ojpt.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Access Token 黑名单服务：用于主动撤销 access token。
 * 
 * 原理：
 * 1. 将需要撤销的 token 的 jti 存入 Redis，TTL 设置为 token 剩余有效期
 * 2. 在 JwtAuthenticationFilter 中检查 token 的 jti 是否在黑名单中
 * 3. 如果在黑名单中，拒绝请求；否则正常处理
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties props;

    /**
     * 将 access token 的 jti 加入黑名单。
     * 
     * @param jti token 的 jti（JWT ID）
     * @param ttlSeconds token 剩余有效期（秒），用于设置 Redis TTL
     */
    public void addToBlacklist(String jti, long ttlSeconds) {
        if (ttlSeconds > 0) {
            String key = buildKey(jti);
            // 存储一个标记值（可以是 "1" 或 jti 本身），TTL 与 token 过期时间一致
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    /**
     * 检查 access token 的 jti 是否在黑名单中。
     * 
     * @param jti token 的 jti
     * @return true 表示在黑名单中（已撤销），false 表示正常
     */
    public boolean isBlacklisted(String jti) {
        String key = buildKey(jti);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将用户加入黑名单（管理员强制下线）。
     * 
     * @param userId 用户ID
     * @param ttlSeconds 黑名单有效期（秒），建议设置较长（如 30 天）
     */
    public void addUserToBlacklist(Long userId, long ttlSeconds) {
        String key = buildUserBlacklistKey(userId);
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 检查用户是否在黑名单中。
     * 
     * @param userId 用户ID
     * @return true 表示用户被拉黑，false 表示正常
     */
    public boolean isUserBlacklisted(Long userId) {
        String key = buildUserBlacklistKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将用户从黑名单中移除（恢复用户）。
     * 
     * @param userId 用户ID
     */
    public void removeUserFromBlacklist(Long userId) {
        String key = buildUserBlacklistKey(userId);
        redisTemplate.delete(key);
    }

    /**
     * 查询用户黑名单剩余时间（秒）。
     *
     * @param userId 用户ID
     * @return 剩余秒数；若未被拉黑或已过期，可能返回 null 或负数
     */
    public Long getUserBlacklistTtlSeconds(Long userId) {
        String key = buildUserBlacklistKey(userId);
        return redisTemplate.getExpire(key);
    }

    /**
     * 将用户加入权限变更临时黑名单（用于强制重新登录）。
     * 与永久封禁不同，权限变更临时黑名单在用户重新登录时会被清除。
     * 
     * @param userId 用户ID
     * @param ttlSeconds 黑名单有效期（秒），建议设置为 access token 过期时间（如 15 分钟）
     */
    public void addPermissionChangeBlacklist(Long userId, long ttlSeconds) {
        String key = buildPermissionChangeBlacklistKey(userId);
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 检查用户是否在权限变更临时黑名单中。
     * 
     * @param userId 用户ID
     * @return true 表示在权限变更临时黑名单中，false 表示正常
     */
    public boolean isPermissionChangeBlacklisted(Long userId) {
        String key = buildPermissionChangeBlacklistKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 将用户从权限变更临时黑名单中移除（允许重新登录）。
     * 
     * @param userId 用户ID
     */
    public void removePermissionChangeBlacklist(Long userId) {
        String key = buildPermissionChangeBlacklistKey(userId);
        redisTemplate.delete(key);
    }

    /**
     * 构建 token 黑名单的 Redis key。
     */
    private String buildKey(String jti) {
        return props.getRedisPrefix() + "blacklist:" + jti;
    }

    /**
     * 构建用户黑名单的 Redis key（永久封禁）。
     */
    private String buildUserBlacklistKey(Long userId) {
        return props.getRedisPrefix() + "user:blacklist:" + userId;
    }

    /**
     * 构建权限变更临时黑名单的 Redis key。
     */
    private String buildPermissionChangeBlacklistKey(Long userId) {
        return props.getRedisPrefix() + "user:permission-change:" + userId;
    }
}

