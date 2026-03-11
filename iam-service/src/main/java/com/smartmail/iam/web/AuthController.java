package com.smartmail.iam.web;

import java.time.LocalDateTime;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.iam.config.JwtProperties;
import com.smartmail.iam.config.JwtUtil;
import com.smartmail.iam.entity.TenantMetadata;
import com.smartmail.iam.entity.User;
import com.smartmail.iam.repository.TenantMetadataRepository;
import com.smartmail.iam.repository.UserRepository;
import com.smartmail.iam.security.SmartMailUserDetails;
import com.smartmail.iam.web.dto.LoginRequest;
import com.smartmail.iam.web.dto.RefreshRequest;
import com.smartmail.iam.web.dto.RegisterRequest;
import com.smartmail.iam.web.dto.TokenResponse;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

/**
 * 认证控制器：提供登录、注册与 Token 刷新接口，URI 符合 /api/iam/auth 规范。
 * <p>
 * 登录成功后返回 accessToken、refreshToken 及过期时间；后续请求需在 Header 中携带
 * Authorization: Bearer &lt;accessToken&gt;。refresh 用于在 accessToken 过期后换取新双 Token。
 * 注册即创建新租户 + 该租户下唯一用户（一租户一账号），成功后返回 201。
 * </p>
 */
@RestController
@RequestMapping("/api/iam/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final TenantMetadataRepository tenantMetadataRepository;
    private final PasswordEncoder passwordEncoder;

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
        Long userIdLong = userDetails.getUserId();
        String userId = String.valueOf(userIdLong);
        String tenantId = userDetails.getTenantId() != null ? userDetails.getTenantId() : "";

        String accessToken = jwtUtil.generateAccessToken(userId, username, tenantId);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);

        TokenResponse response = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresIn(jwtProperties.getAccessExpireSeconds())
                .refreshExpiresIn(jwtProperties.getRefreshExpireSeconds())
                .userId(userIdLong)
                .username(username)
                .tenantId(tenantId)
                .build();
        return ResponseEntity.ok(Result.ok(response));
    }

    /**
     * 注册即租户：先创建租户元数据（tenant_metadata），再创建该租户下唯一用户（sys_user），一租户一账号。
     * 租户共享 tenant_default 库；tenant_id 由用户名转 slug，冲突时加短后缀直至唯一。
     *
     * @param request 含 username、password，需通过校验（用户名 1~64 字符，密码至少 6 位）
     * @return 201 + Result（data 可为 null）；用户名已存在时 409
     */
    @PostMapping("/register")
    public ResponseEntity<Result<Void>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        String tenantId = generateUniqueTenantId(request.getUsername());
        LocalDateTime now = LocalDateTime.now();

        TenantMetadata tenant = new TenantMetadata();
        tenant.setTenantId(tenantId);
        tenant.setSchemaName("tenant_default");
        tenant.setCreateTime(now);
        tenant.setUpdateTime(now);
        tenantMetadataRepository.save(tenant);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTenantId(tenantId);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(null));
    }

    /**
     * 由用户名生成唯一 tenant_id：小写、非字母数字替换为下划线、长度上限 64；冲突则追加短后缀。
     */
    private String generateUniqueTenantId(String username) {
        String base = username.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (base.isEmpty()) {
            base = "tenant";
        }
        if (base.length() > 56) {
            base = base.substring(0, 56);
        }
        String candidate = base;
        for (int i = 0; i < 10; i++) {
            if (!tenantMetadataRepository.existsByTenantId(candidate)) {
                return candidate;
            }
            candidate = base + "_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "生成租户 ID 冲突，请稍后重试");
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

        Long userIdLong = null;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
        }

        TokenResponse response = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresIn(jwtProperties.getAccessExpireSeconds())
                .refreshExpiresIn(jwtProperties.getRefreshExpireSeconds())
                .userId(userIdLong)
                .username(username)
                .tenantId(tenantId)
                .build();
        return ResponseEntity.ok(Result.ok(response));
    }
}
