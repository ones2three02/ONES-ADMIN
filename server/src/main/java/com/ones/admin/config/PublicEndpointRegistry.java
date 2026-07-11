package com.ones.admin.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PublicEndpointRegistry {

    public static final List<String> CORE_PUBLIC_PATH_PATTERNS = List.of(
            "/api/auth/login",
            "/api/health"
    );
    public static final List<String> API_DOC_PATH_PATTERNS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );
    public static final List<String> ACTUATOR_PATH_PATTERNS = List.of("/actuator/**");

    private final SecurityPublicEndpointProperties properties;

    public PublicEndpointRegistry(SecurityPublicEndpointProperties properties) {
        this.properties = properties;
    }

    public List<String> publicPathPatterns() {
        List<String> patterns = new ArrayList<>(CORE_PUBLIC_PATH_PATTERNS);
        if (properties.isApiDocsPublic()) {
            patterns.addAll(API_DOC_PATH_PATTERNS);
        }
        if (properties.isActuatorPublic()) {
            patterns.addAll(ACTUATOR_PATH_PATTERNS);
        }
        return List.copyOf(patterns);
    }

    public List<String> protectedPathPatterns() {
        List<String> patterns = new ArrayList<>();
        patterns.add("/api/**");
        patterns.addAll(API_DOC_PATH_PATTERNS);
        patterns.addAll(ACTUATOR_PATH_PATTERNS);
        return List.copyOf(patterns);
    }
}
