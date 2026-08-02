package com.ones.admin.system.dto;

import java.time.LocalDateTime;

public record FileMetadataResponse(
        Long id,
        String originalName,
        String storedName,
        String url,
        String contentType,
        String extension,
        Long sizeBytes,
        String storageType,
        String bucket,
        String businessType,
        String businessId,
        Long uploadedBy,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}
