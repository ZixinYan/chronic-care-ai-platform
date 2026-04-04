package com.zixin.utils.MyBatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class CommonMybatisPlusConfig {

    /**
     * MyBatis-Plus插件配置（分页等）
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setOverflow(true);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * SQL监控原生拦截器
     */
    @Bean
    @ConditionalOnProperty(prefix = "mybatis-plus", name = "sql-monitor-enabled", havingValue = "true", matchIfMissing = true)
    public SqlMonitorInterceptor sqlMonitorInterceptor() {
        SqlMonitorInterceptor interceptor = new SqlMonitorInterceptor();
        Properties properties = new Properties();
        properties.setProperty("slowSqlThreshold", String.valueOf(3000));
        interceptor.setProperties(properties);

        return interceptor;
    }
}