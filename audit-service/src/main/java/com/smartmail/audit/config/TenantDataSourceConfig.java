package com.smartmail.audit.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

import com.smartmail.common.tenant.TenantContextFilter;
import com.zaxxer.hikari.HikariDataSource;

import lombok.RequiredArgsConstructor;

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
        for (Map.Entry<String, String> e : tenantSchemaProperties.getSchemas().entrySet()) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(baseUrl + "/" + e.getValue());
            ds.setUsername(tenantSchemaProperties.getUsername());
            ds.setPassword(tenantSchemaProperties.getPassword() != null ? tenantSchemaProperties.getPassword() : "");
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            targetDataSources.put(e.getKey(), ds);
        }
        routing.setTargetDataSources(targetDataSources);
        routing.setDefaultTargetDataSource(targetDataSources.get("default") != null
                ? targetDataSources.get("default") : targetDataSources.values().iterator().next());
        return routing;
    }
}
