package com.example.ojpt.config;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一配置 MyBatis-Plus ID 生成策略，确保所有节点的 workerId/dataCenterId 一致可控。
 */
@Configuration
public class MybatisPlusIdConfig {

    /**
     * 使用默认雪花算法生成器，并显式指定 dataCenterId 与 workerId。
     * 当前阶段只有单机部署，因此临时统一写成 (dataCenterId=1, workerId=1)。
     * 如果后续扩容多机房/多节点，务必为不同实例分配不重复的 (dataCenterId, workerId) 组合，
     * 以避免雪花 ID 冲突。
     */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        long dataCenterId = 1L; // 机房 ID（0~31），目前单机部署固定为 1
        long workerId = 1L;     // 机器 ID（0~31），目前单机部署固定为 1
        return new DefaultIdentifierGenerator(workerId, dataCenterId);
    }
}

