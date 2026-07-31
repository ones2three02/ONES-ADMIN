package com.ones.admin.auth.oauth;

@FunctionalInterface
public interface OAuthTokenGenerator {
    String generate();
}
