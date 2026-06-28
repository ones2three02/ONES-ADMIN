package com.ones.admin;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestSaTokenDaoConfiguration {

    @Bean
    @Primary
    public SaTokenDao testSaTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }
}
