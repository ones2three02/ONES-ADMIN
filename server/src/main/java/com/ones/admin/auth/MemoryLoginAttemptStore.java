package com.ones.admin.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoryLoginAttemptStore implements LoginAttemptStore {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public long increment(String key, Duration ttl) {
        Instant now = Instant.now();
        Entry entry = entries.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt().isBefore(now)) {
                return new Entry(1, now.plus(ttl));
            }
            return new Entry(current.value() + 1, current.expiresAt());
        });
        return entry.value();
    }

    @Override
    public boolean exists(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key, entry);
            return false;
        }
        return true;
    }

    @Override
    public void put(String key, Duration ttl) {
        entries.put(key, new Entry(1, Instant.now().plus(ttl)));
    }

    @Override
    public void delete(String key) {
        entries.remove(key);
    }

    private record Entry(long value, Instant expiresAt) {
    }
}
