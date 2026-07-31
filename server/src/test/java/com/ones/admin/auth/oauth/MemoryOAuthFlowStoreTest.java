package com.ones.admin.auth.oauth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryOAuthFlowStoreTest {

    @Test
    void stateAndTicketAreOneTimeValues() {
        MemoryOAuthFlowStore store = new MemoryOAuthFlowStore();
        OAuthLoginTicket ticket = new OAuthLoginTicket(1L, 2L, "feishu");

        store.saveState("feishu", "state", Duration.ofMinutes(5));
        store.saveTicket("ticket", ticket, Duration.ofMinutes(1));

        assertThat(store.consumeState("feishu", "state")).isTrue();
        assertThat(store.consumeState("feishu", "state")).isFalse();
        assertThat(store.consumeTicket("ticket")).contains(ticket);
        assertThat(store.consumeTicket("ticket")).isEmpty();
    }
}
