package com.smartmail.delivery.mq;

import com.smartmail.delivery.service.PrepareSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
