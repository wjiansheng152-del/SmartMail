package com.smartmail.delivery.web;

import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.delivery.service.SmtpConfigService;
import com.smartmail.delivery.web.dto.SmtpConfigDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 用户 SMTP 配置接口：GET 查询当前用户配置（密码脱敏），PUT 保存/更新。
 * 依赖网关转发的 X-User-Id 请求头识别当前用户。
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class SmtpConfigController {

    private static final Logger log = LoggerFactory.getLogger(SmtpConfigController.class);

    private final SmtpConfigService smtpConfigService;

    /** 网关转发的用户 ID 请求头，与 JwtAuthFilter.HEADER_USER_ID 一致 */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 查询当前用户的 SMTP 配置，密码以 **** 占位返回；无配置返回 data 为 null。
     */
    @GetMapping("/smtp-config")
    public Result<SmtpConfigDto> get(@RequestHeader(value = HEADER_USER_ID, required = false) String userIdStr) {
        Long userId = parseUserId(userIdStr);
        try {
            SmtpConfigDto dto = smtpConfigService.getByUserId(userId);
            return Result.ok(dto);
        } catch (Exception e) {
            log.error("GET smtp-config 异常", e);
            throw mapToBizException(e);
        }
    }

    /**
     * 保存或更新当前用户的 SMTP 配置；password 可选，留空表示不修改密码。
     */
    @PutMapping("/smtp-config")
    public Result<SmtpConfigDto> save(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userIdStr,
            @RequestBody SmtpConfigDto body) {
        Long userId = parseUserId(userIdStr);
        try {
            SmtpConfigDto saved = smtpConfigService.save(userId, body);
            return Result.ok(saved);
        } catch (Exception e) {
            log.error("PUT smtp-config 异常", e);
            throw mapToBizException(e);
        }
    }

    /** 将数据库等异常转为业务异常，便于前端展示（如表不存在时提示执行 DDL） */
    private static BizException mapToBizException(Exception e) {
        String msg = e.getMessage();
        Throwable cause = e.getCause();
        if (cause != null) {
            msg = cause.getMessage();
        }
        if (msg != null && (msg.contains("doesn't exist") || msg.contains("smtp_config"))) {
            return new BizException(ErrorCode.INTERNAL_ERROR,
                    "smtp_config 表不存在，请在租户库执行 docs/sql/tenant_default-smtp_config.sql");
        }
        return new BizException(ErrorCode.INTERNAL_ERROR, "服务器内部错误");
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少用户标识");
        }
        try {
            return Long.parseLong(userIdStr.trim());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户标识格式错误");
        }
    }
}
