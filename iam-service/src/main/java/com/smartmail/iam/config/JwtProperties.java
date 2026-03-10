package com.smartmail.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * JWT 配置：密钥与过期时间。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HMAC 密钥，至少 256 位（32 字节）推荐 Base64 或长字符串。
     */
    private String secret = "smartmail-jwt-secret-key-at-least-32-bytes-long-for-hs256";

    /**
     * Access Token 有效期（秒）。
     */
    private long accessExpireSeconds = 3600L;

    /**
     * Refresh Token 有效期（秒）。
     */
    private long refreshExpireSeconds = 604800L;

    /**
     * 签发者。
     */
    private String issuer = "smartmail-iam";
}
