package com.ones.admin.auth.oauth;

import java.time.Duration;
import java.util.Optional;

public interface OAuthFlowStore {

    void saveState(String provider, String state, Duration ttl);

    boolean consumeState(String provider, String state);

    void saveTicket(String ticket, OAuthLoginTicket loginTicket, Duration ttl);

    Optional<OAuthLoginTicket> consumeTicket(String ticket);
}
