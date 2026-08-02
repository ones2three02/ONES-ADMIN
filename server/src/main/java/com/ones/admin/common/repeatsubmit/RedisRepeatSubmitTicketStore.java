package com.ones.admin.common.repeatsubmit;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

public class RedisRepeatSubmitTicketStore implements RepeatSubmitTicketStore {

    private final StringRedisTemplate redisTemplate;

    public RedisRepeatSubmitTicketStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(String key, Duration ttl) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(locked);
    }
}
