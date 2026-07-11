package com.ones.admin.auth;

import java.time.Duration;

public interface LoginAttemptStore {

    long increment(String key, Duration ttl);

    boolean exists(String key);

    void put(String key, Duration ttl);

    void delete(String key);
}
