package com.smartmail.delivery.mq;

import com.smartmail.delivery.service.PrepareSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 消费调度服务投递的活动触发消息，生成批次与投递任务并投递到发送队列。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignTriggerConsumer {

    private final PrepareSendService prepareSendService;

    @RabbitListener(queues = "${app.trigger.queue:smartmail.campaign.trigger}")
    public void onTrigger(CampaignTriggerPayload payload) {
        if (payload == null || payload.getCampaignId() == null) {
            log.warn("Invalid trigger payload: {}", payload);
            return;
        }
        log.info("Received campaign trigger: campaignId={}, tenantId={}", payload.getCampaignId(), payload.getTenantId());
        // #region agent log
        try {
            String line = "{\"hypothesisId\":\"B\",\"message\":\"trigger received\",\"data\":{\"campaignId\":" + payload.getCampaignId() + ",\"tenantId\":\"" + (payload.getTenantId() != null ? payload.getTenantId() : "") + "\"},\"timestamp\":" + System.currentTimeMillis() + "}";
            Files.write(Path.of("/tmp/debug-7f1483.log"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) { /* ignore */ }
        // #endregion
        try {
            prepareSendService.prepareAndEnqueue(
                    payload.getCampaignId(),
                    payload.getTenantId(),
                    payload.getScheduleId()
            );
        } catch (Exception e) {
            log.error("Failed to prepare and enqueue for campaignId={}", payload.getCampaignId(), e);
            throw e;
        }
    }
}
