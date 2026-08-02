package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceManifestDiffResponse(
        String previousVersion,
        String currentVersion,
        String previousChecksum,
        String currentChecksum,
        boolean changed,
        long addedCount,
        long removedCount,
        long modifiedCount,
        long breakingChangeCount,
        List<Change> changes
) {

    public record Change(
            String changeType,
            String severity,
            String apiKey,
            String method,
            String path,
            boolean breakingChange,
            List<FieldChange> changedFields,
            String message
    ) {
    }

    public record FieldChange(
            String fieldName,
            String previousValue,
            String currentValue
    ) {
    }
}
