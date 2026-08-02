package com.ones.admin.system.dto;

public record ApiResourceManifestSnapshotPublishResponse(
        boolean saved,
        ApiResourceManifestSnapshotResponse snapshot,
        ApiResourceManifestGateResponse gate
) {
}
