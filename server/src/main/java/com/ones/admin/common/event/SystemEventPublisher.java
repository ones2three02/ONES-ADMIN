package com.ones.admin.common.event;

public interface SystemEventPublisher {

    void publish(SystemEventPayload event);
}
