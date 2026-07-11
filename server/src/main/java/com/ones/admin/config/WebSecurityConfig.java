package com.ones.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(SecurityWebProperties.class)
public class WebSecurityConfig implements WebMvcConfigurer {

    private final SecurityWebProperties properties;

    public WebSecurityConfig(SecurityWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (properties.getAllowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(properties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Trace-Id", "X-Idempotency-Key")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
