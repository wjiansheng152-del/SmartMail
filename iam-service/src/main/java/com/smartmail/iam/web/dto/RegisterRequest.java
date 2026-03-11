package com.smartmail.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO。
 * <p>
 * 用于 POST /api/iam/auth/register，与 LoginRequest 风格一致；
 * 用户名长度与表 sys_user.username 字段一致（1~64），密码最小长度 6 位。
 * </p>
 */
@Data
public class RegisterRequest {

    /** 用户名，必填，长度 1~64，与数据库字段一致 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 64, message = "用户名长度须在 1~64 之间")
    private String username;

    /** 密码，必填，最小 6 位 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于 6 位")
    private String password;
}
