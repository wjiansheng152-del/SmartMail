package com.smartmail.delivery.mq;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 投递任务入库事务提交后再投递 MQ 的领域事件。
 * <p>
 * 避免在 {@code @Transactional} 未提交前发送消息，导致消费者 {@code selectById} 查不到记录、状态无法回写。
 */
@Getter
public class SendTaskReadyEvent extends ApplicationEvent {

    /** 待投递到发送队列的消息体列表 */
    private final List<SendTaskPayload> payloads;

    /** 发送交换机 */
    private final String exchange;

    /** 发送路由键 */
    private final String routingKey;

    /**
     * @param source      事件发布方
     * @param payloads    待发任务 payload
     * @param exchange    RabbitMQ 交换机
     * @param routingKey  RabbitMQ 路由键
     */
    public SendTaskReadyEvent(Object source, List<SendTaskPayload> payloads, String exchange, String routingKey) {
        super(source);
        this.payloads = payloads == null ? List.of() : List.copyOf(payloads);
        this.exchange = exchange;
        this.routingKey = routingKey;
    }
}
