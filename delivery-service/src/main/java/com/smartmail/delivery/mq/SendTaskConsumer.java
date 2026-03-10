package com.smartmail.delivery.mq;

import com.smartmail.delivery.channel.SendRequest;
import com.smartmail.delivery.channel.SendResult;
import com.smartmail.delivery.channel.SendStrategy;
import com.smartmail.delivery.entity.CampaignBatch;
import com.smartmail.delivery.entity.DeliveryTask;
import com.smartmail.delivery.mapper.CampaignBatchMapper;
import com.smartmail.common.tenant.TenantContext;
import com.smartmail.delivery.mapper.DeliveryTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 消费发送队列：调用发送通道发送邮件，并回写 delivery_task 状态与 campaign_batch 计数。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendTaskConsumer {

    public static final String QUEUE_SEND = "smartmail.send.task";
    public static final String QUEUE_SEND_DLQ = "smartmail.send.task.dlq";

    private final SendStrategy sendStrategy;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final CampaignBatchMapper campaignBatchMapper;

    @RabbitListener(queues = QUEUE_SEND)
    @Transactional(rollbackFor = Exception.class)
    public void handleSendTask(SendTaskPayload payload) {
        TenantContext.setTenantId(payload.getTenantId() != null ? payload.getTenantId() : "default");
        try {
            doHandleSendTask(payload);
        } finally {
            TenantContext.clear();
        }
    }

    private void doHandleSendTask(SendTaskPayload payload) {
        if (payload.getDeliveryId() == null) {
            log.warn("SendTaskPayload missing deliveryId, skip status update");
        }
        var sender = sendStrategy.select(payload.getTenantId(), payload.getChannel());
        if (sender == null) {
            log.warn("No sender available for tenant {}", payload.getTenantId());
            return;
        }
        SendRequest req = SendRequest.builder()
                .to(payload.getTo())
                .subject(payload.getSubject())
                .htmlBody(payload.getHtmlBody())
                .from(payload.getFrom())
                .build();
        SendResult result = sender.send(req);
        LocalDateTime now = LocalDateTime.now();
        boolean success = result.isSuccess();
        if (!success) {
            log.warn("Send failed for {}: {}", payload.getTo(), result.getErrorMessage());
        }
        if (payload.getDeliveryId() != null) {
            DeliveryTask task = deliveryTaskMapper.selectById(payload.getDeliveryId());
            if (task != null) {
                task.setStatus(success ? "sent" : "failed");
                task.setFailReason(success ? null : result.getErrorMessage());
                task.setUpdateTime(now);
                deliveryTaskMapper.updateById(task);
            }
        }
        if (payload.getBatchId() != null && success) {
            CampaignBatch batch = campaignBatchMapper.selectById(payload.getBatchId());
            if (batch != null) {
                batch.setSuccessCount((batch.getSuccessCount() == null ? 0 : batch.getSuccessCount()) + 1);
                batch.setUpdateTime(now);
                campaignBatchMapper.updateById(batch);
            }
        }
        if (payload.getBatchId() != null && !success) {
            CampaignBatch batch = campaignBatchMapper.selectById(payload.getBatchId());
            if (batch != null) {
                batch.setFailCount((batch.getFailCount() == null ? 0 : batch.getFailCount()) + 1);
                batch.setUpdateTime(now);
                campaignBatchMapper.updateById(batch);
            }
        }
    }
}
