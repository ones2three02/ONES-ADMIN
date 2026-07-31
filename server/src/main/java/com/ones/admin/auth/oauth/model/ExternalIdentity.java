package com.ones.admin.auth.oauth.model;

import com.ones.admin.auth.oauth.ExternalIdentityStatus;

import java.time.LocalDateTime;

public record ExternalIdentity(
        Long id,
        String provider,
        String tenantKey,
        String externalSubject,
        Long userId,
        ExternalIdentityStatus status,
        LocalDateTime lastLoginAt
) {
}
