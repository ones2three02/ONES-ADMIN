package com.ones.admin.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisLoginAttemptStoreTest {

    @Test
    void incrementUsesOneRedisScriptForCounterAndTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("login:key")), eq("30000")))
                .thenReturn(2L);

        RedisLoginAttemptStore store = new RedisLoginAttemptStore(redisTemplate);

        assertThat(store.increment("login:key", Duration.ofSeconds(30))).isEqualTo(2L);
    }
}
