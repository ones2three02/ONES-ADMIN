package com.ones.admin.common.repeatsubmit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ones.security.repeat-submit")
public class RepeatSubmitProperties {

    private boolean enabled = true;
    private Storage storage = Storage.REDIS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public enum Storage {
        REDIS,
        MEMORY
    }
}
