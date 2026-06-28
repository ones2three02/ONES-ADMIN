package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record SystemOverviewResponse(
        long userCount,
        long roleCount,
        long menuCount,
        long deptCount,
        long enabledUserCount,
        LocalDateTime generatedAt
) {
}
