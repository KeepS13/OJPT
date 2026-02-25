package com.example.ojpt.security;

import com.example.ojpt.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtService {

    private final JwtProperties props;
    private final SecretKey secretKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.secretKey = Keys.hmacShaKeyFor(props.getSecret().getBytes());
    }

    /**
     * 生成一对 JWT 令牌（accessToken 和 refreshToken）。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param roles    角色列表
     * @return 含 accessToken、refreshToken、refresh jti 的封装对象
     */
    public TokenPair generateTokens(Long userId, String username, List<String> roles) {
        String refreshJti = UUID.randomUUID().toString();
        String accessJti = UUID.randomUUID().toString();
        Instant now = Instant.now();

        String accessToken = Jwts.builder()
                .setId(accessJti)
                .setSubject(String.valueOf(userId))
                .setIssuer(props.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getAccessExpSeconds())))
                .addClaims(Map.of(
                        "username", username,
                        "roles", roles,
                        "type", "access",
                        "jti_refresh", refreshJti
                ))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setId(refreshJti)
                .setSubject(String.valueOf(userId))
                .setIssuer(props.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(props.getRefreshExpSeconds())))
                .addClaims(Map.of(
                        "username", username,
                        "roles", roles,
                        "type", "refresh"
                ))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return new TokenPair(accessToken, refreshToken, refreshJti, accessJti);
    }

    /**
     * 解析并验证一个 JWT 字符串，返回其 Claims。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 存储 JWT 令牌对的数据结构，包含 accessToken、refreshToken 以及两者的 jti。
     *
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     * @param refreshJti   refreshToken 的 jti
     * @param accessJti    accessToken 的 jti（用于黑名单机制）
     */
    public record TokenPair(String accessToken, String refreshToken, String refreshJti, String accessJti) {
    }
}

