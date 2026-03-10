package com.smartmail.contact.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.smartmail.common.tenant.TenantContext;

/**
 * 按当前租户解析数据源 lookup key。
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
    }
}
