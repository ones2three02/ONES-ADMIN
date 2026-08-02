package com.ones.admin.auth.oauth;

import java.time.LocalDateTime;

public record ExternalIdentityResponse(
        Long id,
        String provider,
        String tenantKey,
        String externalSubject,
        Long userId,
        ExternalIdentityStatus status,
        LocalDateTime lastLoginAt
) {
}
