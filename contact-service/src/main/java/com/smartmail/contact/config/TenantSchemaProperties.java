package com.smartmail.contact.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 租户与 schema 映射及租户数据源基础 URL。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.tenant")
public class TenantSchemaProperties {

    /**
     * 租户 ID -> schema 名（MySQL 即 database 名），如 default -> tenant_default
     */
    private Map<String, String> schemas = new HashMap<>();

    /**
     * 租户库基础 URL（不含 schema），如 jdbc:mysql://localhost:3306
     */
    private String baseUrl = "jdbc:mysql://localhost:3306";

    private String username = "root";
    private String password = "";

    public TenantSchemaProperties() {
        this.schemas.put("default", "tenant_default");
    }
}
