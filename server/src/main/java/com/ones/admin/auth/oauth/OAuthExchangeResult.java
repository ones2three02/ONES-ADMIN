package com.ones.admin.auth.oauth;

import com.ones.admin.auth.dto.LoginResponse;

public record OAuthExchangeResult(
        LoginResponse loginResponse,
        String provider,
        Long externalIdentityId
) {
}
