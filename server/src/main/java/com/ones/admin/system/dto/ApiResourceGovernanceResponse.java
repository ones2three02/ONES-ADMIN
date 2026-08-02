package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceGovernanceResponse(
        boolean passed,
        long total,
        long violationCount,
        long errorCount,
        long warningCount,
        String permissionCodePattern,
        String operationIdPattern,
        String apiVersionPattern,
        List<RuleSummary> ruleSummaries,
        List<CategorySummary> categorySummaries,
        List<Violation> violations
) {

    public record RuleSummary(
            String ruleCode,
            String severity,
            String category,
            boolean blocking,
            long count,
            String description,
            String remediation
    ) {
    }

    public record CategorySummary(
            String category,
            long violationCount,
            long errorCount,
            long warningCount
    ) {
    }

    public record Violation(
            String ruleCode,
            String severity,
            String method,
            String path,
            String operationId,
            String module,
            String summary,
            String message,
            String remediation
    ) {
    }
}
