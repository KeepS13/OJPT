package com.example.ojpt.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.plugin.Interceptor;
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
 * 为确保分页能力正常工作，本配置类统一装配 MyBatis-Plus 分页拦截器，避免分页接口返回 total/pages 为 0。
 */
@Configuration
public class MybatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 构建 MyBatis-Plus SqlSessionFactory，确保自动填充/逻辑删除等 MP 能力生效。
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(
            DataSource dataSource,
            MetaObjectHandler metaObjectHandler,
            MybatisPlusInterceptor mybatisPlusInterceptor
    ) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(metaObjectHandler); // 启用 MP 自动填充
        factoryBean.setGlobalConfig(globalConfig);
        factoryBean.setPlugins(new Interceptor[]{mybatisPlusInterceptor});
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

