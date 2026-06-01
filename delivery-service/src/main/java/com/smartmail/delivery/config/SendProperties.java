package com.smartmail.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 发送队列与重试相关配置。
 * 用于限制 SMTP 发信失败后的重试次数，避免对外部邮件服务器（如 QQ 邮箱）造成频繁连接触发限流。
 */
@Data
@ConfigurationProperties(prefix = "app.send")
public class SendProperties {

    /** 发送交换机名称 */
    private String exchange = "smartmail.send";

    /** 发送路由键 */
    private String routingKey = "send.task";

    /**
     * 单条投递任务最大尝试次数（含首次发送）。
     * 例如 3 表示：首次失败后最多再重试 2 次，共 3 次尝试。
     */
    private int maxAttempts = 3;

    /** 首次重试前的等待毫秒数 */
    private long retryIntervalMs = 30_000L;

    /** 重试间隔指数退避倍数，第 n 次重试等待 retryIntervalMs * multiplier^n */
    private double retryBackoffMultiplier = 2.0;

    /** 单次重试等待上限（毫秒），防止间隔过长 */
    private long maxRetryIntervalMs = 300_000L;
}
