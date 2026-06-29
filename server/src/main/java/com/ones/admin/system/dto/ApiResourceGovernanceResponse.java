package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceGovernanceResponse(
        boolean passed,
        long total,
        long violationCount,
        long errorCount,
        long warningCount,
        String permissionCodePattern,
        List<Violation> violations
) {

    public record Violation(
            String ruleCode,
            String severity,
            String method,
            String path,
            String module,
            String summary,
            String message
    ) {
    }
}
