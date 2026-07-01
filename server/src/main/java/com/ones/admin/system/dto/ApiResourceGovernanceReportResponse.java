package com.ones.admin.system.dto;

public record ApiResourceGovernanceReportResponse(
        String applicationVersion,
        String generatedAt,
        ApiResourceSummaryResponse summary,
        ApiResourceGovernanceResponse governance,
        ApiResourceGovernanceRuleResponse rules,
        ApiResourceManifestResponse manifest,
        ApiResourceManifestLatestGateResponse latestGate
) {
}
