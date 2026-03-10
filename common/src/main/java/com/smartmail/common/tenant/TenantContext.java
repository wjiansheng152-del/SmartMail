package com.smartmail.common.tenant;

import lombok.experimental.UtilityClass;

/**
 * 租户上下文，使用 ThreadLocal 线程隔离存储当前请求的租户标识。
 * <p>
 * 网关在鉴权后从 JWT 解析租户并写入请求头 X-Tenant-Id，下游服务通过 TenantContextFilter
 * 读取该头并调用 setTenantId；数据源路由、审计等逻辑通过 getTenantId() 获取当前租户。
 * 请求结束时必须调用 clear() 避免线程复用时串租户。
 * </p>
 */
@UtilityClass
public class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    /** 设置当前请求的租户 ID，通常由 TenantContextFilter 在入口处调用 */
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    /** 获取当前请求的租户 ID，供数据源路由、审计等使用 */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /** 请求结束时清理，防止线程池复用时携带旧租户 */
    public static void clear() {
        TENANT_ID.remove();
    }
}
