package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record AuditRetentionCleanupResponse(
        int loginLogRetentionDays,
        int operationLogRetentionDays,
        LocalDateTime loginLogExpireBefore,
        LocalDateTime operationLogExpireBefore,
        long deletedLoginLogCount,
        long deletedOperationLogCount
) {
}
