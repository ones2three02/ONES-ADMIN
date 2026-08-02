package com.ones.admin.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ones.events", name = "broker", havingValue = "none", matchIfMissing = true)
public class NoopSystemEventPublisher implements SystemEventPublisher {

    @Override
    public void publish(SystemEventPayload event) {
        // 默认关闭事件外发，避免本地和测试环境强依赖消息中间件。
    }
}
