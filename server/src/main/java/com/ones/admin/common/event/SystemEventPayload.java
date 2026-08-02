package com.ones.admin.common.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SystemEventPayload(
        String eventType,
        String routingKey,
        String traceId,
        Instant occurredAt,
        Map<String, Object> payload
) {

    public SystemEventPayload {
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static SystemEventPayload of(
            String eventType,
            String routingKey,
            String traceId,
            Map<String, Object> payload
    ) {
        return new SystemEventPayload(eventType, routingKey, traceId, Instant.now(), payload);
    }
}
