package com.smartmail.common.tenant;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 租户上下文过滤器：从请求头解析租户 ID 并写入 TenantContext，请求结束后清理。
 * <p>
 * 网关在转发请求时会携带 X-Tenant-Id（来自 JWT 或默认值）。各业务服务需通过
 * FilterRegistrationBean 注册本 Filter，并保证在业务逻辑之前执行（order 最高），
 * 这样 AbstractRoutingDataSource 等才能正确拿到当前租户。
 * </p>
 */
public class TenantContextFilter extends HttpFilter {

    /** 请求头名：租户 ID，与网关下发的头一致 */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String tenantId = request.getHeader(HEADER_TENANT_ID);
            TenantContext.setTenantId(tenantId != null && !tenantId.isBlank() ? tenantId : "default");
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
