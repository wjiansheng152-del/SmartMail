package com.smartmail.campaign.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.smartmail.common.tenant.TenantContext;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
    }
}
