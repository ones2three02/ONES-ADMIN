package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record AuditRetentionSummaryResponse(
        int loginLogRetentionDays,
        int operationLogRetentionDays,
        LocalDateTime loginLogExpireBefore,
        LocalDateTime operationLogExpireBefore,
        long expiredLoginLogCount,
        long expiredOperationLogCount
) {
}
