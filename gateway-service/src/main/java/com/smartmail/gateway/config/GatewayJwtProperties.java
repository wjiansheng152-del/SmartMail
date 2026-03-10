package com.smartmail.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class GatewayJwtProperties {

    private String secret = "smartmail-jwt-secret-key-at-least-32-bytes-long-for-hs256";
}
