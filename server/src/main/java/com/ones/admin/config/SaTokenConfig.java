package com.ones.admin.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.system.audit.OperationAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    public static final List<String> LOGIN_EXCLUDE_PATH_PATTERNS = List.of(
            "/api/auth/login",
            "/api/health",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private final OperationAuditInterceptor operationAuditInterceptor;

    public SaTokenConfig(OperationAuditInterceptor operationAuditInterceptor) {
        this.operationAuditInterceptor = operationAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGIN_EXCLUDE_PATH_PATTERNS);
        registry.addInterceptor(operationAuditInterceptor)
                .addPathPatterns("/api/system/**");
    }
}
