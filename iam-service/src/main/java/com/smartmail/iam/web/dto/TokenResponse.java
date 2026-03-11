package com.smartmail.iam.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/刷新返回：Token 及当前用户信息，供前端写入 store 以区分不同用户与租户。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long accessExpiresIn;
    private Long refreshExpiresIn;
    /** 当前用户 ID，与 JWT claim userId 一致 */
    private Long userId;
    /** 当前用户名 */
    private String username;
    /** 当前租户 ID */
    private String tenantId;
}
