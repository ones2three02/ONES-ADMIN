package com.ones.admin.auth.oauth;

public record OAuthLoginTicket(
        Long userId,
        Long externalIdentityId,
        String provider
) {
}
