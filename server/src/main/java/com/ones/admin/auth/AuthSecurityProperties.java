package com.ones.admin.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "ones.security.login")
public class AuthSecurityProperties {

    @Min(1)
    private int maxFailedLoginCount = 5;

    @NotNull
    private Duration lockDuration = Duration.ofMinutes(15);

    public int getMaxFailedLoginCount() {
        return maxFailedLoginCount;
    }

    public void setMaxFailedLoginCount(int maxFailedLoginCount) {
        this.maxFailedLoginCount = maxFailedLoginCount;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }
}
