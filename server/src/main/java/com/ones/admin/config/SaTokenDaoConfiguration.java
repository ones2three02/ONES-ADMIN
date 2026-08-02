package com.ones.admin.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SaTokenDaoConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "ones.security.session", name = "storage", havingValue = "memory")
    public SaTokenDao memorySaTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }
}
