package com.ones.admin.hr.dto;

import java.time.LocalDateTime;

public record HrRosterImportBatchResponse(
        Long id,
        String batchNo,
        String fileName,
        String status,
        Integer totalCount,
        Integer successCount,
        Integer failedCount,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
