package com.ones.admin.auth;

import com.ones.admin.common.security.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class LoginRateLimiter {

    private final LoginRateLimitProperties properties;
    private final LoginAttemptStore store;

    public LoginRateLimiter(LoginRateLimitProperties properties, LoginAttemptStore store) {
        this.properties = properties;
        this.store = store;
    }

    public void checkAllowed(String username, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }
        if (store.exists(accountBlockKey(username)) || store.exists(ipBlockKey(clientIp))) {
            throw new RateLimitExceededException(AuthErrorCode.LOGIN_RATE_LIMITED);
        }
    }

    public void recordFailure(String username, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }
        long accountAttempts = store.increment(accountCountKey(username), properties.getWindow());
        if (accountAttempts >= properties.getAccountMaxAttempts()) {
            store.put(accountBlockKey(username), properties.getBlockDuration());
        }
        long ipAttempts = store.increment(ipCountKey(clientIp), properties.getWindow());
        if (ipAttempts >= properties.getIpMaxAttempts()) {
            store.put(ipBlockKey(clientIp), properties.getBlockDuration());
        }
    }

    public void recordSuccess(String username) {
        if (!properties.isEnabled()) {
            return;
        }
        store.delete(accountCountKey(username));
        store.delete(accountBlockKey(username));
    }

    private String accountCountKey(String username) { return key("account", "count", normalizeUsername(username)); }
    private String accountBlockKey(String username) { return key("account", "block", normalizeUsername(username)); }
    private String ipCountKey(String clientIp) { return key("ip", "count", clientIp); }
    private String ipBlockKey(String clientIp) { return key("ip", "block", clientIp); }

    private String key(String dimension, String state, String value) {
        return properties.getKeyPrefix() + ":" + dimension + ":" + state + ":" + sha256(value == null ? "unknown" : value);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 缺少 SHA-256 算法", exception);
        }
    }
}
