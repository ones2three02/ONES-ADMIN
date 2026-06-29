package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceSummaryResponse(
        long total,
        long writeOperationCount,
        long permissionMissingCount,
        long explicitAccessPolicyCount,
        List<AuthTypeStat> authTypes,
        List<ModuleStat> modules
) {

    public record AuthTypeStat(
            String authType,
            long count
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
