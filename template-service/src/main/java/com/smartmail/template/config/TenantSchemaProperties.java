package com.smartmail.template.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.tenant")
public class TenantSchemaProperties {

    private Map<String, String> schemas = new HashMap<>();
    private String baseUrl = "jdbc:mysql://localhost:3306";
    private String username = "root";
    private String password = "";

    public TenantSchemaProperties() {
        this.schemas.put("default", "tenant_default");
    }
}
