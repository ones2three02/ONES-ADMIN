package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record FileRetentionPurgeResponse(
        int deletedFileRetentionDays,
        LocalDateTime purgeBefore,
        long purgedFileCount,
        long purgedFileSizeBytes
) {
}
