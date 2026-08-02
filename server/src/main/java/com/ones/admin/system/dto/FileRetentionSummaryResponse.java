package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record FileRetentionSummaryResponse(
        int deletedFileRetentionDays,
        LocalDateTime purgeBefore,
        long expiredDeletedFileCount,
        long expiredDeletedFileSizeBytes
) {
}
