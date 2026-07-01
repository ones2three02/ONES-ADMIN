package com.ones.admin.system.dto;

import java.util.List;

public record ApiResourceManifestResponse(
        String applicationVersion,
        String checksumAlgorithm,
        String checksum,
        long total,
        List<Resource> resources
) {

    public record Resource(
            String apiKey,
            String operationId,
            String method,
            String path,
            String handler,
            String authType,
            List<String> permissionCodes,
            String permissionMode,
            boolean writeOperation,
            boolean repeatSubmitProtected,
            String owner,
            String audience,
            String sinceVersion,
            String lifecycle,
            String riskLevel,
            String sunsetVersion,
            String replacementApiKey,
            boolean deprecated
    ) {
    }
}
