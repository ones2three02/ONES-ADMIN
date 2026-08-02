package com.ones.admin.auth.oauth;

public record OAuthCallbackResult(String ticket, long expiresInSeconds) {
}
