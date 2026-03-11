package com.smartmail.delivery.web.dto;

import lombok.Data;

/**
 * SMTP 配置 API 请求/响应 DTO。
 * GET 返回时 password 为占位 "****" 或省略；PUT 时 password 可选，留空表示不修改。
 */
@Data
public class SmtpConfigDto {

    private String host;
    private Integer port;
    private String username;
    /** 明文密码，仅 PUT 时使用；GET 返回占位 "****" */
    private String password;
    private String fromEmail;
    private String fromName;
    /** 是否使用 SSL */
    private Boolean useSsl;
}
