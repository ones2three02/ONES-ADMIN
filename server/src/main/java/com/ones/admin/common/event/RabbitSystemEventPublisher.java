package com.ones.admin.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ones.events", name = "broker", havingValue = "rabbitmq")
public class RabbitSystemEventPublisher implements SystemEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitSystemEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final SystemEventProperties properties;

    public RabbitSystemEventPublisher(RabbitTemplate rabbitTemplate, SystemEventProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(SystemEventPayload event) {
        try {
            rabbitTemplate.convertAndSend(properties.getExchange(), event.routingKey(), event);
        } catch (RuntimeException exception) {
            log.warn("系统事件发布失败：eventType={}, routingKey={}", event.eventType(), event.routingKey(), exception);
        }
    }
}
