package com.ones.admin.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(OAuthFlowProperties.class)
public class OAuthFlowConfig {

    @Bean
    public OAuthFlowStore oauthFlowStore(
            OAuthFlowProperties properties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        if (properties.getStorage() == OAuthFlowProperties.Storage.MEMORY) {
            return new MemoryOAuthFlowStore();
        }
        return new RedisOAuthFlowStore(redisTemplate, objectMapper, properties.getKeyPrefix());
    }
}
