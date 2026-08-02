package com.ones.admin.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(LoginRateLimitProperties.class)
public class LoginRateLimitConfig {

    @Bean
    public LoginAttemptStore loginAttemptStore(
            LoginRateLimitProperties properties,
            StringRedisTemplate redisTemplate
    ) {
        if (properties.getStorage() == LoginRateLimitProperties.Storage.MEMORY) {
            return new MemoryLoginAttemptStore();
        }
        return new RedisLoginAttemptStore(redisTemplate);
    }
}
