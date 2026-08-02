package com.ones.admin.system.dto;

import jakarta.validation.constraints.Size;

public record ApiResourceManifestSnapshotPublishRequest(
        boolean allowBreakingChanges,
        @Size(max = 512) String reviewReason
) {
}
