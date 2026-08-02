package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceSummaryResponse(
        long total,
        long writeOperationCount,
        long permissionMissingCount,
        long explicitAccessPolicyCount,
        long deprecatedCount,
        List<AuthTypeStat> authTypes,
        List<LifecycleStat> lifecycles,
        List<RiskLevelStat> riskLevels,
        List<OwnerStat> owners,
        List<AudienceStat> audiences,
        List<ModuleStat> modules
) {

    public record AuthTypeStat(
            String authType,
            long count
    ) {
    }

    public record LifecycleStat(
            String lifecycle,
            long count
    ) {
    }

    public record RiskLevelStat(
            String riskLevel,
            long count
    ) {
    }

    public record OwnerStat(
            String owner,
            long total,
            long writeOperationCount,
            long permissionMissingCount,
            long highRiskCount
    ) {
    }

    public record AudienceStat(
            String audience,
            long total,
            long publicCount,
            long permissionCount,
            long writeOperationCount
    ) {
    }

    public record ModuleStat(
            String module,
            long total,
            long publicCount,
            long loginCount,
            long permissionCount,
            long writeOperationCount,
            long permissionMissingCount
    ) {
    }
}
