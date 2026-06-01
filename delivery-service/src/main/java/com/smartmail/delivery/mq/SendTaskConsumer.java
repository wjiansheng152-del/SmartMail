package com.smartmail.delivery.mq;

import com.smartmail.delivery.channel.EmailSender;
import com.smartmail.delivery.channel.SendRequest;
import com.smartmail.delivery.channel.SendResult;
import com.smartmail.delivery.channel.SendStrategy;
import com.smartmail.delivery.channel.SmtpEmailSender;
import com.smartmail.delivery.channel.UserSmtpMailSenderFactory;
import com.smartmail.delivery.config.SendProperties;
import com.smartmail.delivery.entity.CampaignBatch;
import com.smartmail.delivery.entity.DeliveryTask;
import com.smartmail.delivery.entity.SmtpConfig;
import com.smartmail.delivery.mapper.CampaignBatchMapper;
import com.smartmail.common.tenant.TenantContext;
import com.smartmail.delivery.mapper.DeliveryTaskMapper;
import com.smartmail.delivery.service.SmtpConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 消费发送队列：若 payload 带 smtpConfigUserId 则按该用户 SMTP 配置发信，否则走默认通道；
 * 发信失败时按配置重试若干次，超过上限后标记 failed 并停止重试，避免对外部 SMTP 频繁连接触发限流。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendTaskConsumer {

    public static final String QUEUE_SEND = "smartmail.send.task";
    public static final String QUEUE_SEND_DLQ = "smartmail.send.task.dlq";

    private final SendStrategy sendStrategy;
    private final SmtpConfigService smtpConfigService;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final CampaignBatchMapper campaignBatchMapper;
    private final RabbitTemplate rabbitTemplate;
    private final SendProperties sendProperties;

    @RabbitListener(queues = QUEUE_SEND)
    @Transactional(rollbackFor = Exception.class)
    public void handleSendTask(SendTaskPayload payload) {
        TenantContext.setTenantId(payload.getTenantId() != null ? payload.getTenantId() : "default");
        try {
            doHandleSendTask(payload);
        } catch (Exception e) {
            // 兜底：任何未预期异常也走统一失败处理，避免消息被 RabbitMQ 无限 requeue
            log.error("Unexpected error handling send task deliveryId={}", payload.getDeliveryId(), e);
            handleSendFailure(payload, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 执行一次发信尝试；失败时由 {@link #handleSendFailure} 决定重试或终止。
     */
    private void doHandleSendTask(SendTaskPayload payload) {
        if (payload.getDeliveryId() == null) {
            log.warn("SendTaskPayload missing deliveryId, skip status update");
        }
        EmailSender sender = resolveSender(payload);
        if (sender == null) {
            handleSendFailure(payload, "No sender available for tenant " + payload.getTenantId());
            return;
        }
        SendRequest req = SendRequest.builder()
                .to(payload.getTo())
                .subject(payload.getSubject())
                .htmlBody(payload.getHtmlBody())
                .from(payload.getFrom())
                .fromName(resolveFromName(payload))
                .build();
        SendResult result = sender.send(req);
        if (result.isSuccess()) {
            markTaskSuccess(payload);
        } else {
            handleSendFailure(payload, result.getErrorMessage());
        }
    }

    /**
     * 解析发信通道：优先用户 SMTP，否则默认通道（如 MailHog）。
     */
    private EmailSender resolveSender(SendTaskPayload payload) {
        if (payload.getSmtpConfigUserId() != null) {
            SmtpConfig config = smtpConfigService.getEntityByUserId(payload.getSmtpConfigUserId());
            if (config != null) {
                String plainPassword = smtpConfigService.decryptPassword(config.getPasswordEncrypted());
                payload.setFrom(config.getFromEmail() != null && !config.getFromEmail().isBlank()
                        ? config.getFromEmail() : payload.getFrom());
                return new SmtpEmailSender(UserSmtpMailSenderFactory.build(config, plainPassword));
            }
        }
        return sendStrategy.select(payload.getTenantId(), payload.getChannel());
    }

    private String resolveFromName(SendTaskPayload payload) {
        if (payload.getSmtpConfigUserId() == null) {
            return null;
        }
        SmtpConfig config = smtpConfigService.getEntityByUserId(payload.getSmtpConfigUserId());
        return config != null ? config.getFromName() : null;
    }

    /**
     * 发信失败处理：未达最大尝试次数则延迟后重新入队；否则标记 failed 并停止。
     */
    private void handleSendFailure(SendTaskPayload payload, String errorMessage) {
        int retryCount = payload.getRetryCount() == null ? 0 : payload.getRetryCount();
        int currentAttempt = retryCount + 1;
        int maxAttempts = Math.max(1, sendProperties.getMaxAttempts());
        String safeMessage = truncate(errorMessage, 480);

        if (currentAttempt < maxAttempts) {
            long delayMs = computeRetryDelayMs(retryCount);
            log.warn("Send failed attempt {}/{} deliveryId={} to={} reason={} retryInMs={}",
                    currentAttempt, maxAttempts, payload.getDeliveryId(), payload.getTo(), safeMessage, delayMs);
            scheduleRetry(payload, retryCount + 1, delayMs);
            return;
        }

        log.error("Send failed after {} attempts, giving up deliveryId={} to={} reason={}",
                maxAttempts, payload.getDeliveryId(), payload.getTo(), safeMessage);
        markTaskFailed(payload, safeMessage);
    }

    /**
     * 计算第 retryCount 次重试前的等待时间（指数退避）。
     */
    private long computeRetryDelayMs(int retryCount) {
        double multiplier = Math.max(1.0, sendProperties.getRetryBackoffMultiplier());
        long interval = Math.max(0L, sendProperties.getRetryIntervalMs());
        long maxInterval = Math.max(interval, sendProperties.getMaxRetryIntervalMs());
        double delay = interval * Math.pow(multiplier, retryCount);
        return (long) Math.min(delay, maxInterval);
    }

    /**
     * 等待后重新投递到发送队列；不在此处标记 failed，任务保持 pending。
     */
    private void scheduleRetry(SendTaskPayload payload, int nextRetryCount, long delayMs) {
        payload.setRetryCount(nextRetryCount);
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Retry sleep interrupted for deliveryId={}", payload.getDeliveryId());
            }
        }
        rabbitTemplate.convertAndSend(
                sendProperties.getExchange(),
                sendProperties.getRoutingKey(),
                payload
        );
    }

    /** 发信成功：回写 delivery_task 与批次成功计数 */
    private void markTaskSuccess(SendTaskPayload payload) {
        LocalDateTime now = LocalDateTime.now();
        if (payload.getDeliveryId() != null) {
            DeliveryTask task = deliveryTaskMapper.selectById(payload.getDeliveryId());
            if (task != null) {
                task.setStatus("sent");
                task.setFailReason(null);
                task.setUpdateTime(now);
                deliveryTaskMapper.updateById(task);
            }
        }
        if (payload.getBatchId() != null) {
            CampaignBatch batch = campaignBatchMapper.selectById(payload.getBatchId());
            if (batch != null) {
                batch.setSuccessCount((batch.getSuccessCount() == null ? 0 : batch.getSuccessCount()) + 1);
                batch.setUpdateTime(now);
                campaignBatchMapper.updateById(batch);
            }
        }
    }

    /** 达到最大重试次数后标记失败，并更新批次失败计数 */
    private void markTaskFailed(SendTaskPayload payload, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        if (payload.getDeliveryId() != null) {
            DeliveryTask task = deliveryTaskMapper.selectById(payload.getDeliveryId());
            if (task != null) {
                task.setStatus("failed");
                task.setFailReason(errorMessage);
                task.setUpdateTime(now);
                deliveryTaskMapper.updateById(task);
            }
        }
        if (payload.getBatchId() != null) {
            CampaignBatch batch = campaignBatchMapper.selectById(payload.getBatchId());
            if (batch != null) {
                batch.setFailCount((batch.getFailCount() == null ? 0 : batch.getFailCount()) + 1);
                batch.setUpdateTime(now);
                campaignBatchMapper.updateById(batch);
            }
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }
}
