package com.smartmail.scheduler.config;

import com.smartmail.common.tenant.TenantContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 按租户上下文路由到对应 Schema 数据源。
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
    }
}
