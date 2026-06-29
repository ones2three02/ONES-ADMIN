package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceResponse(
        String method,
        String path,
        String module,
        String summary,
        List<String> permissionCodes,
        boolean permissionCodeStandard,
        List<String> invalidPermissionCodes,
        String permissionMode,
        String authType,
        boolean accessPolicyExplicit,
        String accessPolicyReason,
        boolean requiresPermission,
        boolean permissionRegistered,
        List<String> unregisteredPermissionCodes,
        boolean permissionAssignable,
        List<String> unassignablePermissionCodes,
        boolean permissionMissing,
        boolean writeOperation,
        String apiKey,
        String operationId,
        String handler,
        String owner,
        String sinceVersion,
        String lifecycle,
        String riskLevel,
        boolean deprecated
) {
}
