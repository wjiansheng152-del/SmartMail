package com.smartmail.iam.web;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.iam.config.JwtProperties;
import com.smartmail.iam.config.JwtUtil;
import com.smartmail.iam.security.SmartMailUserDetails;
import com.smartmail.iam.web.dto.LoginRequest;
import com.smartmail.iam.web.dto.RefreshRequest;
import com.smartmail.iam.web.dto.TokenResponse;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

/**
 * 认证控制器：提供登录与 Token 刷新接口，URI 符合 /api/iam/auth 规范。
 * <p>
 * 登录成功后返回 accessToken、refreshToken 及过期时间；后续请求需在 Header 中携带
 * Authorization: Bearer &lt;accessToken&gt;。refresh 用于在 accessToken 过期后换取新双 Token。
 * </p>
 */
@RestController
@RequestMapping("/api/iam/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /**
     * 用户登录：校验用户名密码，签发 accessToken 与 refreshToken。
     *
     * @param request 含 username、password，需通过校验
     * @return 200 + Result&lt;TokenResponse&gt;，含 accessToken、refreshToken、过期秒数
     */
    @PostMapping("/login")
    public ResponseEntity<Result<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SmartMailUserDetails userDetails = (SmartMailUserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        String userId = String.valueOf(userDetails.getUserId());
        String tenantId = userDetails.getTenantId() != null ? userDetails.getTenantId() : "";

        String accessToken = jwtUtil.generateAccessToken(userId, username, tenantId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);

        TokenResponse response = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresIn(jwtProperties.getAccessExpireSeconds())
                .refreshExpiresIn(jwtProperties.getRefreshExpireSeconds())
                .build();
        return ResponseEntity.ok(Result.ok(response));
    }

    /**
     * 刷新 Token：使用有效的 refreshToken 换取新的 accessToken 与 refreshToken。
     *
     * @param request 含 refreshToken
     * @return 200 + Result&lt;TokenResponse&gt;；若 refreshToken 无效或已过期则 401
     */
    @PostMapping("/refresh")
    public ResponseEntity<Result<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parseAndValidate(request.getRefreshToken());
        } catch (Exception e) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "请使用 refreshToken");
        }
        String userId = claims.get(JwtUtil.CLAIM_USER_ID, String.class);
        String username = claims.getSubject();
        String tenantId = claims.get(JwtUtil.CLAIM_TENANT_ID, String.class);
        if (tenantId == null) {
            tenantId = "";
        }

        String accessToken = jwtUtil.generateAccessToken(userId, username, tenantId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);

        TokenResponse response = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresIn(jwtProperties.getAccessExpireSeconds())
                .refreshExpiresIn(jwtProperties.getRefreshExpireSeconds())
                .build();
        return ResponseEntity.ok(Result.ok(response));
    }
}
