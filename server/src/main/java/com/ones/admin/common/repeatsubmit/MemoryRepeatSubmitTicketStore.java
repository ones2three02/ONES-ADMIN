package com.ones.admin.common.repeatsubmit;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryRepeatSubmitTicketStore implements RepeatSubmitTicketStore {

    private final Map<String, Long> expiresAtByKey = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        clearExpired(now);
        long expiresAt = now + ttl.toMillis();
        Long existing = expiresAtByKey.putIfAbsent(key, expiresAt);
        if (existing == null) {
            return true;
        }
        if (existing <= now) {
            return expiresAtByKey.replace(key, existing, expiresAt);
        }
        return false;
    }

    private void clearExpired(long now) {
        Iterator<Map.Entry<String, Long>> iterator = expiresAtByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
