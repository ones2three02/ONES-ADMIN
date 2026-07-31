package com.ones.admin.auth.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ones.security.oauth")
public class OAuthFlowProperties {

    private Storage storage = Storage.REDIS;
    @NotNull
    private Duration stateTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration ticketTtl = Duration.ofSeconds(60);
    @NotBlank
    private String keyPrefix = "ones:security:oauth";

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Duration getStateTtl() {
        return stateTtl;
    }

    public void setStateTtl(Duration stateTtl) {
        this.stateTtl = stateTtl;
    }

    public Duration getTicketTtl() {
        return ticketTtl;
    }

    public void setTicketTtl(Duration ticketTtl) {
        this.ticketTtl = ticketTtl;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @AssertTrue(message = "OAuth state 和登录票据有效期必须大于 0")
    public boolean isTtlValid() {
        return stateTtl != null && !stateTtl.isZero() && !stateTtl.isNegative()
                && ticketTtl != null && !ticketTtl.isZero() && !ticketTtl.isNegative();
    }

    public enum Storage {
        REDIS,
        MEMORY
    }
}
