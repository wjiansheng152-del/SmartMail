package com.smartmail.scheduler.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 调度服务 RabbitMQ 配置：活动触发队列与交换机，供定时任务投递 CampaignTriggerPayload。
 */
@Configuration
public class SchedulerRabbitConfig {

    @Value("${app.trigger.exchange:smartmail.trigger}")
    private String exchangeName;
    @Value("${app.trigger.queue:smartmail.campaign.trigger}")
    private String queueName;
    @Value("${app.trigger.routing-key:campaign.trigger}")
    private String routingKey;

    @Bean
    public DirectExchange triggerExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue triggerQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding triggerBinding() {
        return BindingBuilder.bind(triggerQueue()).to(triggerExchange()).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
