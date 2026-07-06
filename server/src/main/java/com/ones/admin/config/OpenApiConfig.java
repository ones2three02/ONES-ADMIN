package com.ones.admin.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "BearerAuth";

    private final String applicationVersion;

    public OpenApiConfig(@Value("${ones.version:v0.0.73}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Bean
    public OpenAPI onesAdminOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ONES-ADMIN 后端接口")
                        .version(applicationVersion)
                        .description("企业级后台管理系统接口文档，基于 Spring Boot 3、Sa-Token、MyBatis-Plus 构建。"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Sa-Token")
                                .name(HttpHeaders.AUTHORIZATION)))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证接口")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统管理接口")
                .pathsToMatch("/api/system/**", "/api/timezone/**")
                .build();
    }

    @Bean
    public GroupedOpenApi hrApi() {
        return GroupedOpenApi.builder()
                .group("HRMS接口")
                .pathsToMatch("/api/hr/**")
                .build();
    }

    @Bean
    public OperationCustomizer saTokenOperationCustomizer() {
        return (operation, handlerMethod) -> {
            List<String> descriptions = new ArrayList<>();
            if (operation.getDescription() != null && !operation.getDescription().isBlank()) {
                descriptions.add(operation.getDescription());
            }
            permissionDescription(handlerMethod).forEach(descriptions::add);
            if (!descriptions.isEmpty()) {
                operation.setDescription(String.join("<br/>", descriptions));
            }
            return operation;
        };
    }

    private List<String> permissionDescription(HandlerMethod handlerMethod) {
        List<String> descriptions = new ArrayList<>();
        SaCheckPermission classPermission = handlerMethod.getBeanType().getAnnotation(SaCheckPermission.class);
        if (classPermission != null) {
            descriptions.add("类权限：" + formatPermission(classPermission.value(), classPermission.mode()));
        }
        SaCheckPermission methodPermission = handlerMethod.getMethodAnnotation(SaCheckPermission.class);
        if (methodPermission != null) {
            descriptions.add("方法权限：" + formatPermission(methodPermission.value(), methodPermission.mode()));
        }
        return descriptions;
    }

    private String formatPermission(String[] permissions, SaMode mode) {
        String separator = mode == SaMode.AND ? " 且 " : " 或 ";
        return String.join(separator, permissions);
    }
}
