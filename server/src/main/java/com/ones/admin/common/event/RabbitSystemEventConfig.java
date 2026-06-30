package com.ones.admin.common.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "ones.events", name = "broker", havingValue = "rabbitmq")
public class RabbitSystemEventConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange onesAdminEventExchange(SystemEventProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue operationAuditQueue(SystemEventProperties properties) {
        return QueueBuilder.durable(properties.getOperationAuditQueue()).build();
    }

    @Bean
    public Queue loginAuditQueue(SystemEventProperties properties) {
        return QueueBuilder.durable(properties.getLoginAuditQueue()).build();
    }

    @Bean
    public Binding operationAuditBinding(Queue operationAuditQueue, TopicExchange onesAdminEventExchange) {
        return BindingBuilder.bind(operationAuditQueue)
                .to(onesAdminEventExchange)
                .with("ones.audit.operation.*");
    }

    @Bean
    public Binding loginAuditBinding(Queue loginAuditQueue, TopicExchange onesAdminEventExchange) {
        return BindingBuilder.bind(loginAuditQueue)
                .to(onesAdminEventExchange)
                .with("ones.audit.login.*");
    }
}
