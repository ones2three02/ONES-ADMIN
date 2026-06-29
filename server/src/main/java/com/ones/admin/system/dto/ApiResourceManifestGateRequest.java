package com.ones.admin.system.dto;

public record ApiResourceManifestGateRequest(
        ApiResourceManifestResponse previousManifest,
        boolean allowBreakingChanges,
        String reviewReason
) {
}
