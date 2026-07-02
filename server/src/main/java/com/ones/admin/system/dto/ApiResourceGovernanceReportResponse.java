package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceGovernanceReportResponse(
        String applicationVersion,
        String generatedAt,
        ApiResourceSummaryResponse summary,
        ApiResourceGovernanceResponse governance,
        ApiResourceGovernanceRuleResponse rules,
        ApiResourceManifestResponse manifest,
        ApiResourceManifestLatestGateResponse latestGate,
        List<ReferenceBenchmark> referenceBenchmarks,
        List<RecommendedAction> recommendedActions
) {

    public record ReferenceBenchmark(
            String project,
            String category,
            String url,
            String lesson
    ) {
    }

    public record RecommendedAction(
            String actionCode,
            String priority,
            String category,
            String title,
            String description,
            String verification
    ) {
    }
}
