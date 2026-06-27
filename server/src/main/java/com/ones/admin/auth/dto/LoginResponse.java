package com.ones.admin.auth.dto;

import java.util.List;

public record LoginResponse(
        TokenInfo token,
        UserProfile user,
        List<String> roles,
        List<String> permissions
) {

    public LoginResponse withToken(TokenInfo tokenInfo) {
        return new LoginResponse(tokenInfo, user, roles, permissions);
    }
}
