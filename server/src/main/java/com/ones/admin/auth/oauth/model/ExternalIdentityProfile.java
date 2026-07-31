package com.ones.admin.auth.oauth.model;

public record ExternalIdentityProfile(
        String provider,
        String tenantKey,
        String externalSubject,
        String displayName
) {
}
