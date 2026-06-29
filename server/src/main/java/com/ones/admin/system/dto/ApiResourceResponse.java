package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceResponse(
        String method,
        String path,
        String module,
        String summary,
        List<String> permissionCodes,
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
        boolean writeOperation
) {
}
