package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceManifestGateResponse(
        boolean passed,
        String status,
        String previousVersion,
        String currentVersion,
        long governanceErrorCount,
        long governanceWarningCount,
        long breakingChangeCount,
        boolean requiredManualReview,
        boolean reviewReasonRequired,
        List<String> reasons,
        ApiResourceManifestDiffResponse diff
) {
}
