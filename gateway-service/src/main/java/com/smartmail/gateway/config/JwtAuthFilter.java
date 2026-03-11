package com.smartmail.gateway.config;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

/**
 * 网关 JWT 认证过滤器：对除登录、刷新外的请求校验 Authorization: Bearer &lt;token&gt;，
 * 校验通过后将 userId、tenantId、username 写入请求头转发给下游，校验失败返回 401 JSON。
 * <p>
 * 放行路径：/api/iam/auth/login、/api/iam/auth/refresh；与 IAM 使用相同密钥（app.jwt.secret）。
 * </p>
 */
@Component
public class JwtAuthFilter implements WebFilter {

    /** 下游请求头：用户 ID，从 JWT claim userId 解析 */
    public static final String HEADER_USER_ID = "X-User-Id";
    /** 下游请求头：租户 ID，从 JWT claim tenantId 解析，供租户数据源路由使用 */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    /** 下游请求头：用户名，从 JWT subject 解析 */
    public static final String HEADER_USERNAME = "X-Username";

    private final GatewayJwtProperties jwtProperties;

    public JwtAuthFilter(GatewayJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (isAuthPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return write401(exchange, "缺少或无效的 Authorization 头");
        }
        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.get("userId", String.class);
            String tenantIdVal = claims.get("tenantId", String.class);
            String username = claims.getSubject();
            final String tenantId = tenantIdVal != null ? tenantIdVal : "";
            final String usernameVal = username != null ? username : "";

            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.headers(h -> {
                        h.set(HEADER_USER_ID, userId != null ? userId : "");
                        h.set(HEADER_TENANT_ID, tenantId);
                        h.set(HEADER_USERNAME, usernameVal);
                    }))
                    .build();
            return chain.filter(mutated);
        } catch (Exception e) {
            return write401(exchange, "Token 无效或已过期");
        }
    }

    private boolean isAuthPath(String path) {
        return "/api/iam/auth/login".equals(path) || "/api/iam/auth/refresh".equals(path);
    }

    private Mono<Void> write401(ServerWebExchange exchange, String errorInfo) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"errorCode\":\"401\",\"errorInfo\":\"" + escapeJson(errorInfo) + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
