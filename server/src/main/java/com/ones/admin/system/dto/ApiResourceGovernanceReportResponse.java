package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceGovernanceReportResponse(
        String applicationVersion,
        String generatedAt,
        int qualityScore,
        List<QualityDimension> qualityDimensions,
        ApiResourceSummaryResponse summary,
        ApiResourceGovernanceResponse governance,
        ApiResourceGovernanceRuleResponse rules,
        ApiResourceManifestResponse manifest,
        ApiResourceManifestLatestGateResponse latestGate,
        List<ReferenceBenchmark> referenceBenchmarks,
        List<RecommendedAction> recommendedActions,
        List<ActionItem> actionItems
) {

    public record ReferenceBenchmark(
            String project,
            String category,
            String url,
            String lesson
    ) {
    }

    public record QualityDimension(
            String dimensionCode,
            String title,
            String category,
            int score,
            long violationCount,
            long errorCount,
            long warningCount,
            boolean passed,
            String benchmark,
            String recommendation
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

    public record ActionItem(
            String actionCode,
            String priority,
            String category,
            String title,
            String description,
            String sourceType,
            String sourceCode,
            String owner,
            String module,
            String apiKey,
            boolean blocking,
            String status,
            String verification
    ) {
    }
}
