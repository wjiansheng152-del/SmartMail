package com.smartmail.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * 网关 Security 配置：放行所有请求，由 JwtAuthFilter 自行校验 JWT。
 * <p>
 * 若未配置，Spring Security 自动配置会要求所有请求认证，导致未带 Token 的登录/注册等返回 401；
 * 此处显式 permitAll 后，认证逻辑仅由 JwtAuthFilter 负责。
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }
}
