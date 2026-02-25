package com.example.ojpt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ojpt.jwt")
public class JwtProperties {

    /**
     * HS256 秘钥，至少 32 bytes。
     */
    private String secret;

    /**
     * 访问令牌过期秒数。
     */
    private long accessExpSeconds;

    /**
     * 刷新令牌过期秒数。
     */
    private long refreshExpSeconds;

    /**
     * 签发者。
     */
    private String issuer;

    /**
     * Redis 前缀，用于存储刷新令牌。
     */
    private String redisPrefix;
}

