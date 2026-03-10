package com.smartmail.scheduler.config;

import com.smartmail.common.tenant.TenantContextFilter;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 租户数据源配置：按 app.tenant 构建多 Schema 数据源并注册路由数据源与租户过滤器。
 */
@Configuration
@EnableConfigurationProperties(TenantSchemaProperties.class)
@RequiredArgsConstructor
public class TenantDataSourceConfig {

    private final TenantSchemaProperties tenantSchemaProperties;

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>(new TenantContextFilter());
        reg.addUrlPatterns("/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource tenantRoutingDataSource() {
        TenantRoutingDataSource routing = new TenantRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        String baseUrl = tenantSchemaProperties.getBaseUrl().replaceFirst("/$", "");
        String username = tenantSchemaProperties.getUsername();
        String password = tenantSchemaProperties.getPassword() != null ? tenantSchemaProperties.getPassword() : "";
        for (Map.Entry<String, String> e : tenantSchemaProperties.getSchemas().entrySet()) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(baseUrl + "/" + e.getValue());
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            targetDataSources.put(e.getKey(), ds);
        }
        routing.setTargetDataSources(targetDataSources);
        routing.setDefaultTargetDataSource(targetDataSources.get("default") != null
                ? targetDataSources.get("default")
                : targetDataSources.values().iterator().next());
        return routing;
    }
}
