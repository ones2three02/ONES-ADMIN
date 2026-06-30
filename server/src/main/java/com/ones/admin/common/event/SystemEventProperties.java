package com.ones.admin.common.event;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ones.events")
public class SystemEventProperties {

    private Broker broker = Broker.NONE;
    private String exchange = "ones.admin.events";
    private String operationAuditQueue = "ones.admin.audit.operation";
    private String loginAuditQueue = "ones.admin.audit.login";

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getOperationAuditQueue() {
        return operationAuditQueue;
    }

    public void setOperationAuditQueue(String operationAuditQueue) {
        this.operationAuditQueue = operationAuditQueue;
    }

    public String getLoginAuditQueue() {
        return loginAuditQueue;
    }

    public void setLoginAuditQueue(String loginAuditQueue) {
        this.loginAuditQueue = loginAuditQueue;
    }

    public enum Broker {
        NONE,
        RABBITMQ
    }
}
