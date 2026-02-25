package com.example.ojpt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类：配置 RedisTemplate
 * Spring Boot 4.0 中推荐使用 StringRedisSerializer，JSON 序列化在业务层手动处理
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate，显式指定序列化方式，避免默认 JdkSerialization 带来的可读性差问题。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 StringRedisSerializer 序列化 key 和 value
        // 对于复杂对象，建议在业务层使用 ObjectMapper 进行 JSON 转换
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key 采用 String 的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash 的 key 也采用 String 的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value 序列化方式采用 String（业务层手动 JSON 转换）
        template.setValueSerializer(stringRedisSerializer);
        // hash 的 value 序列化方式采用 String
        template.setHashValueSerializer(stringRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 提供 ObjectMapper Bean，用于业务层的 JSON 序列化/反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

