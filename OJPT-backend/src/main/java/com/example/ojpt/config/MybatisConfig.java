package com.example.ojpt.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis/MyBatis-Plus 基础配置：
 * 1. 通过 {@link MybatisSqlSessionFactoryBean} 复用 Spring 管理的数据源；
 * 2. 暴露 {@link SqlSessionTemplate} 以便 Mapper 层按模板执行 SQL。
 * <p>
 * 当前为了保持行为可控，不额外启用 XML 配置或插件，后续若引入分页、审计等插件，
 * 可在本配置类里统一装配，避免散落各处。
 */
@Configuration
public class MybatisConfig {

    /**
     * 构建 MyBatis-Plus SqlSessionFactory，确保自动填充/逻辑删除等 MP 能力生效。
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, MetaObjectHandler metaObjectHandler) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler); // 启用 MP 自动填充
        factoryBean.setGlobalConfig(globalConfig);
        return factoryBean.getObject();
    }
    
    /**
     * 暴露 SqlSessionTemplate，供 MyBatis-Spring 框架管理 Mapper 调用。
     */
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}

