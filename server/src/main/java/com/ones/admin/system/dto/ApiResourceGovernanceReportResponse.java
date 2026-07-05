package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceGovernanceReportResponse(
        String applicationVersion,
        String generatedAt,
        int qualityScore,
        ReleaseReadiness releaseReadiness,
        List<QualityDimension> qualityDimensions,
        ApiResourceSummaryResponse summary,
        ApiResourceGovernanceResponse governance,
        ApiResourceGovernanceRuleResponse rules,
        ApiResourceManifestResponse manifest,
        ApiResourceManifestLatestGateResponse latestGate,
        List<ReferenceBenchmark> referenceBenchmarks,
        List<RecommendedAction> recommendedActions,
        List<OwnerActionSummary> ownerActionSummaries,
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

    public record ReleaseReadiness(
            String status,
            boolean ready,
            String priority,
            int qualityScore,
            boolean baselineAvailable,
            String gateStatus,
            long blockingCheckCount,
            long blockingActionCount,
            long openActionCount,
            String nextActionCode,
            String nextActionTitle,
            String message
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

    public record OwnerActionSummary(
            String owner,
            String status,
            String priority,
            long totalActionCount,
            long openActionCount,
            long blockingActionCount,
            long p0ActionCount,
            long p1ActionCount,
            long p2ActionCount,
            List<String> categories,
            String nextActionCode,
            String nextActionTitle,
            String recommendation
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
