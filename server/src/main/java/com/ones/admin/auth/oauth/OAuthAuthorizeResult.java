package com.ones.admin.auth.oauth;

public record OAuthAuthorizeResult(String authorizationUrl, long expiresInSeconds) {
}
