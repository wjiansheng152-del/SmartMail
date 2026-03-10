package com.smartmail.iam.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * JWT 生成与校验（HS256）。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties props;

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "sub";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private SecretKey secretKey() {
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String userId, String username, String tenantId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + props.getAccessExpireSeconds() * 1000);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TENANT_ID, tenantId != null ? tenantId : "")
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuer(props.getIssuer())
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey())
                .compact();
    }

    public String generateRefreshToken(String userId, String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + props.getRefreshExpireSeconds() * 1000);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuer(props.getIssuer())
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey())
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }
}
