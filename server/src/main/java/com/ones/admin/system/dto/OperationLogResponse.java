package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record OperationLogResponse(
        Long id,
        Long userId,
        String method,
        String path,
        String module,
        String operation,
        String permissionCode,
        Boolean success,
        Integer responseCode,
        String errorMessage,
        String traceId,
        String ip,
        String userAgent,
        Long durationMs,
        LocalDateTime createdAt
) {
}
