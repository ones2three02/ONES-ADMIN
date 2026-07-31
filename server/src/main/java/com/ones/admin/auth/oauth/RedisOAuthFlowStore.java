package com.ones.admin.auth.oauth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class RedisOAuthFlowStore implements OAuthFlowStore {

    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if value then
                redis.call('DEL', KEYS[1])
            end
            return value
            """, String.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisOAuthFlowStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void saveState(String provider, String state, Duration ttl) {
        redisTemplate.opsForValue().set(stateKey(provider, state), "1", ttl);
    }

    @Override
    public boolean consumeState(String provider, String state) {
        return redisTemplate.execute(CONSUME_SCRIPT, List.of(stateKey(provider, state))) != null;
    }

    @Override
    public void saveTicket(String ticket, OAuthLoginTicket loginTicket, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(ticketKey(ticket), objectMapper.writeValueAsString(loginTicket), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OAuth 登录票据序列化失败", exception);
        }
    }

    @Override
    public Optional<OAuthLoginTicket> consumeTicket(String ticket) {
        String value = redisTemplate.execute(CONSUME_SCRIPT, List.of(ticketKey(ticket)));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, OAuthLoginTicket.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OAuth 登录票据解析失败", exception);
        }
    }

    private String stateKey(String provider, String state) {
        return keyPrefix + ":state:" + provider + ":" + state;
    }

    private String ticketKey(String ticket) {
        return keyPrefix + ":ticket:" + ticket;
    }
}
