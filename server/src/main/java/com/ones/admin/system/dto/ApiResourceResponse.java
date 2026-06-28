package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceResponse(
        String method,
        String path,
        String module,
        String summary,
        List<String> permissionCodes,
        String permissionMode,
        boolean requiresPermission,
        boolean writeOperation
) {
}
