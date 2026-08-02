package com.ones.admin.auth;

import com.ones.admin.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    @Test
    void blocksAccountAfterConfiguredFailures() {
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        properties.setAccountMaxAttempts(2);
        properties.setIpMaxAttempts(10);
        properties.setWindow(Duration.ofMinutes(5));
        properties.setBlockDuration(Duration.ofMinutes(15));
        LoginRateLimiter limiter = new LoginRateLimiter(properties, new MemoryLoginAttemptStore());

        limiter.recordFailure("Admin", "127.0.0.1");
        limiter.recordFailure("admin", "127.0.0.1");

        assertThatThrownBy(() -> limiter.checkAllowed(" admin ", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthErrorCode.LOGIN_RATE_LIMITED.message());
    }

    @Test
    void successfulLoginClearsAccountFailureState() {
        LoginRateLimitProperties properties = new LoginRateLimitProperties();
        properties.setAccountMaxAttempts(2);
        properties.setIpMaxAttempts(10);
        LoginRateLimiter limiter = new LoginRateLimiter(properties, new MemoryLoginAttemptStore());

        limiter.recordFailure("admin", "127.0.0.1");
        limiter.recordSuccess("admin");
        limiter.recordFailure("admin", "127.0.0.1");

        limiter.checkAllowed("admin", "127.0.0.1");
    }
}
