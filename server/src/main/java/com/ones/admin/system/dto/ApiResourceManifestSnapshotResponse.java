package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record ApiResourceManifestSnapshotResponse(
        Long id,
        String applicationVersion,
        String checksumAlgorithm,
        String checksum,
        long total,
        String publishStatus,
        String reviewReason,
        LocalDateTime createdAt,
        ApiResourceManifestResponse manifest
) {
}
