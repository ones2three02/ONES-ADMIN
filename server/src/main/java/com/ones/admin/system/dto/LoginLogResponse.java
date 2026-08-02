package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record LoginLogResponse(
        Long id,
        String username,
        Long userId,
        Boolean success,
        String failureReason,
        String ip,
        String userAgent,
        String authMethod,
        String provider,
        Long externalIdentityId,
        String traceId,
        LocalDateTime createdAt
) {
}
