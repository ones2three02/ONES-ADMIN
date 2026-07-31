package com.ones.admin.auth.oauth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryOAuthFlowStore implements OAuthFlowStore {

    private final Clock clock;
    private final Map<String, ExpiringValue<Boolean>> states = new ConcurrentHashMap<>();
    private final Map<String, ExpiringValue<OAuthLoginTicket>> tickets = new ConcurrentHashMap<>();

    public MemoryOAuthFlowStore() {
        this(Clock.systemUTC());
    }

    MemoryOAuthFlowStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void saveState(String provider, String state, Duration ttl) {
        states.put(stateKey(provider, state), new ExpiringValue<>(Boolean.TRUE, expiresAt(ttl)));
    }

    @Override
    public boolean consumeState(String provider, String state) {
        ExpiringValue<Boolean> value = states.remove(stateKey(provider, state));
        return value != null && value.expiresAt().isAfter(clock.instant());
    }

    @Override
    public void saveTicket(String ticket, OAuthLoginTicket loginTicket, Duration ttl) {
        tickets.put(ticket, new ExpiringValue<>(loginTicket, expiresAt(ttl)));
    }

    @Override
    public Optional<OAuthLoginTicket> consumeTicket(String ticket) {
        ExpiringValue<OAuthLoginTicket> value = tickets.remove(ticket);
        if (value == null || !value.expiresAt().isAfter(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(value.value());
    }

    private String stateKey(String provider, String state) {
        return provider + ":" + state;
    }

    private Instant expiresAt(Duration ttl) {
        return clock.instant().plus(ttl);
    }

    private record ExpiringValue<T>(T value, Instant expiresAt) {
    }
}
