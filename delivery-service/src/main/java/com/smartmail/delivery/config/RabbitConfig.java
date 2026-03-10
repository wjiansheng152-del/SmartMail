package com.smartmail.delivery.config;

import com.smartmail.delivery.mq.SendTaskConsumer;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_SEND = "smartmail.send";
    public static final String ROUTING_SEND = "send.task";
    public static final String ROUTING_DLQ = "send.task.dlq";

    /** 活动触发队列（与 scheduler 一致，便于单独启动时队列存在） */
    public static final String QUEUE_TRIGGER = "smartmail.campaign.trigger";
    public static final String EXCHANGE_TRIGGER = "smartmail.trigger";
    public static final String ROUTING_TRIGGER = "campaign.trigger";

    @Bean
    public DirectExchange sendExchange() {
        return new DirectExchange(EXCHANGE_SEND, true, false);
    }

    @Bean
    public Queue sendQueue() {
        return QueueBuilder.durable(SendTaskConsumer.QUEUE_SEND)
                .withArgument("x-dead-letter-exchange", EXCHANGE_SEND)
                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
                .build();
    }

    @Bean
    public Queue sendDlq() {
        return QueueBuilder.durable(SendTaskConsumer.QUEUE_SEND_DLQ).build();
    }

    @Bean
    public Binding sendBinding() {
        return BindingBuilder.bind(sendQueue()).to(sendExchange()).with(ROUTING_SEND);
    }

    @Bean
    public Binding sendDlqBinding() {
        return BindingBuilder.bind(sendDlq()).to(sendExchange()).with(ROUTING_DLQ);
    }

    @Bean
    public DirectExchange triggerExchange() {
        return new DirectExchange(EXCHANGE_TRIGGER, true, false);
    }

    @Bean
    public Queue triggerQueue() {
        return new Queue(QUEUE_TRIGGER, true);
    }

    @Bean
    public Binding triggerBinding() {
        return BindingBuilder.bind(triggerQueue()).to(triggerExchange()).with(ROUTING_TRIGGER);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
