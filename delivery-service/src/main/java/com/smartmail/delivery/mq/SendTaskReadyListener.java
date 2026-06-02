package com.smartmail.delivery.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在 prepare 事务提交后再将发送任务投递到 RabbitMQ，保证消费者能读到已提交的 delivery_task。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendTaskReadyListener {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 事务成功提交后批量入队，避免与消费者并发读写未提交数据产生竞态。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSendTaskReady(SendTaskReadyEvent event) {
        if (event.getPayloads().isEmpty()) {
            return;
        }
        for (SendTaskPayload payload : event.getPayloads()) {
            rabbitTemplate.convertAndSend(event.getExchange(), event.getRoutingKey(), payload);
        }
        log.info("Enqueued {} send tasks after commit, firstDeliveryId={}",
                event.getPayloads().size(),
                event.getPayloads().get(0).getDeliveryId());
    }
}
