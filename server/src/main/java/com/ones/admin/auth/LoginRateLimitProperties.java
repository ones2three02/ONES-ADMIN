package com.ones.admin.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ones.security.login-rate-limit")
public class LoginRateLimitProperties {

    private boolean enabled = true;
    private Storage storage = Storage.REDIS;
    @Min(1)
    private int accountMaxAttempts = 10;
    @Min(1)
    private int ipMaxAttempts = 30;
    @NotNull
    private Duration window = Duration.ofMinutes(5);
    @NotNull
    private Duration blockDuration = Duration.ofMinutes(15);
    private String keyPrefix = "ones:security:login";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public int getAccountMaxAttempts() { return accountMaxAttempts; }
    public void setAccountMaxAttempts(int accountMaxAttempts) { this.accountMaxAttempts = accountMaxAttempts; }
    public int getIpMaxAttempts() { return ipMaxAttempts; }
    public void setIpMaxAttempts(int ipMaxAttempts) { this.ipMaxAttempts = ipMaxAttempts; }
    public Duration getWindow() { return window; }
    public void setWindow(Duration window) { this.window = window; }
    public Duration getBlockDuration() { return blockDuration; }
    public void setBlockDuration(Duration blockDuration) { this.blockDuration = blockDuration; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public enum Storage { REDIS, MEMORY }
}
