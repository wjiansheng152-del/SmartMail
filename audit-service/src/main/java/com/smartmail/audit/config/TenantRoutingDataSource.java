package com.smartmail.audit.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.smartmail.common.tenant.TenantContext;

/**
 * 按当前租户解析数据源 lookup key。未配置的 tenantId 以 defaultTargetDataSource 回退（共享 tenant_default）。
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
    }
}
