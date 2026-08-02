package com.ones.admin.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.system.audit.OperationAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private final OperationAuditInterceptor operationAuditInterceptor;
    private final PublicEndpointRegistry publicEndpointRegistry;

    public SaTokenConfig(
            OperationAuditInterceptor operationAuditInterceptor,
            PublicEndpointRegistry publicEndpointRegistry
    ) {
        this.operationAuditInterceptor = operationAuditInterceptor;
        this.publicEndpointRegistry = publicEndpointRegistry;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns(publicEndpointRegistry.protectedPathPatterns())
                .excludePathPatterns(publicEndpointRegistry.publicPathPatterns());
        registry.addInterceptor(operationAuditInterceptor)
                .addPathPatterns("/api/system/**", "/api/hr/**");
    }
}
