package com.smartmail.gateway.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存限流：按 X-User-Id 或 IP 限制每分钟请求数（V1 内存实现，可后续改为 Redis）。
 */
@Component
public class RateLimitFilter implements WebFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 300;
    private final Map<String, CountWindow> windows = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String key = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (key == null || key.isBlank()) {
            key = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
        }
        long now = System.currentTimeMillis();
        CountWindow w = windows.compute(key, (k, v) -> {
            if (v == null || now - v.minuteStart > 60_000) {
                return new CountWindow(now, 1);
            }
            v.count++;
            return v;
        });
        if (w.count > MAX_REQUESTS_PER_MINUTE) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"errorCode\":\"429\",\"errorInfo\":\"请求过于频繁\"}";
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
        return chain.filter(exchange);
    }

    private static class CountWindow {
        final long minuteStart;
        int count;

        CountWindow(long minuteStart, int count) {
            this.minuteStart = minuteStart;
            this.count = count;
        }
    }
}
