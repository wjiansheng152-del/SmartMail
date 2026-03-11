package com.smartmail.campaign.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.smartmail.common.tenant.TenantContext;

/**
 * 按当前租户解析数据源 lookup key。
 * <p>
 * 未在 app.tenant.schemas 中配置的 tenantId 会由 AbstractRoutingDataSource 以 defaultTargetDataSource 回退（共享 tenant_default）。
 * </p>
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
    }
}
