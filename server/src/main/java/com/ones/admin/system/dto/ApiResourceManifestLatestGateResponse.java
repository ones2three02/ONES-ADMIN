package com.ones.admin.system.dto;

public record ApiResourceManifestLatestGateResponse(
        boolean baselineAvailable,
        Long baselineSnapshotId,
        String baselineVersion,
        String baselineChecksum,
        ApiResourceManifestGateResponse gate
) {
}
