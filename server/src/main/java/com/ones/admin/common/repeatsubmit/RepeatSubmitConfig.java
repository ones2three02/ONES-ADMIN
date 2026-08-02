package com.ones.admin.common.repeatsubmit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RepeatSubmitProperties.class)
public class RepeatSubmitConfig {

    @Bean
    public RepeatSubmitTicketStore repeatSubmitTicketStore(
            RepeatSubmitProperties properties,
            StringRedisTemplate redisTemplate
    ) {
        if (properties.getStorage() == RepeatSubmitProperties.Storage.MEMORY) {
            return new MemoryRepeatSubmitTicketStore();
        }
        return new RedisRepeatSubmitTicketStore(redisTemplate);
    }
}
