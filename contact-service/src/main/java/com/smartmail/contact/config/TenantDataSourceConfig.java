package com.smartmail.contact.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

import com.smartmail.common.tenant.TenantContextFilter;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

/**
 * 租户数据源路由：根据 TenantContext 选择 schema；并注册租户上下文 Filter。
 */
@Configuration
@ConditionalOnProperty(name = "app.tenant.enabled", havingValue = "true", matchIfMissing = true)
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
            String tenantId = e.getKey();
            String schema = e.getValue();
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(baseUrl + "/" + schema);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            ds.setConnectionTimeout(15000);
            ds.setInitializationFailTimeout(15000);
            targetDataSources.put(tenantId, ds);
        }

        routing.setTargetDataSources(targetDataSources);
        Object first = targetDataSources.get("default");
        if (first == null) {
            first = targetDataSources.values().iterator().next();
        }
        routing.setDefaultTargetDataSource(first);
        return routing;
    }
}
