package com.smartmail.iam.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-at-least-32-bytes-long-for-hs256");
        props.setAccessExpireSeconds(3600);
        props.setRefreshExpireSeconds(604800);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    void generateAndParseAccessToken() {
        String token = jwtUtil.generateAccessToken("user1", "admin", "default");
        assertNotNull(token);
        Claims claims = jwtUtil.parseAndValidate(token);
        assertEquals("admin", claims.getSubject());
        assertEquals("user1", claims.get(JwtUtil.CLAIM_USER_ID, String.class));
        assertEquals("default", claims.get(JwtUtil.CLAIM_TENANT_ID, String.class));
        assertFalse(jwtUtil.isRefreshToken(claims));
    }

    @Test
    void generateAndParseRefreshToken() {
        String token = jwtUtil.generateRefreshToken("user1", "admin");
        assertNotNull(token);
        Claims claims = jwtUtil.parseAndValidate(token);
        assertTrue(jwtUtil.isRefreshToken(claims));
    }
}
