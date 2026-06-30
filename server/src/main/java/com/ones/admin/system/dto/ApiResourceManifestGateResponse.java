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
        List<Check> checks,
        List<String> reasons,
        ApiResourceManifestDiffResponse diff
) {

    public record Check(
            String checkCode,
            String severity,
            boolean passed,
            boolean blocking,
            String message,
            String remediation
    ) {
    }
}
