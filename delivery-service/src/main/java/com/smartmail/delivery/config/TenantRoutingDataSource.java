package com.smartmail.delivery.config;

import com.smartmail.common.tenant.TenantContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 按租户上下文路由到对应 Schema 数据源。
 * <p>
 * 未在 app.tenant.schemas 中配置的 tenantId 会以 defaultTargetDataSource 回退（共享 tenant_default）。
 * </p>
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
    }
}
