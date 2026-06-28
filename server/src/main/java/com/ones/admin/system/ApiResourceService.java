package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ones.admin.system.dto.ApiResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ApiResourceService {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public ApiResourceService(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    public List<ApiResourceResponse> listApiResources() {
        return requestMappingHandlerMapping.getHandlerMethods()
                .entrySet()
                .stream()
                .flatMap(entry -> toResponses(entry.getKey(), entry.getValue()).stream())
                .sorted(Comparator.comparing(ApiResourceResponse::path)
                        .thenComparing(ApiResourceResponse::method))
                .toList();
    }

    private List<ApiResourceResponse> toResponses(RequestMappingInfo info, HandlerMethod handlerMethod) {
        List<ApiResourceResponse> responses = new ArrayList<>();
        PermissionMetadata permissionMetadata = permissionMetadata(handlerMethod);
        for (String path : paths(info)) {
            if (!path.startsWith("/api/")) {
                continue;
            }
            for (String method : methods(info)) {
                responses.add(new ApiResourceResponse(
                        method,
                        path,
                        moduleName(handlerMethod),
                        operationSummary(handlerMethod),
                        permissionMetadata.codes(),
                        permissionMetadata.mode(),
                        !permissionMetadata.codes().isEmpty(),
                        isWriteOperation(method)
                ));
            }
        }
        return responses;
    }

    private Set<String> paths(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() != null) {
            return info.getPathPatternsCondition().getPatternValues();
        }
        if (info.getPatternsCondition() != null) {
            return info.getPatternsCondition().getPatterns();
        }
        return Set.of();
    }

    private List<String> methods(RequestMappingInfo info) {
        Set<RequestMethod> requestMethods = info.getMethodsCondition().getMethods();
        if (requestMethods.isEmpty()) {
            return List.of("ALL");
        }
        return requestMethods.stream()
                .map(RequestMethod::name)
                .sorted()
                .toList();
    }

    private String moduleName(HandlerMethod handlerMethod) {
        Tag tag = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Tag.class);
        return tag == null ? null : tag.name();
    }

    private String operationSummary(HandlerMethod handlerMethod) {
        Operation operation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
        return operation == null ? handlerMethod.getMethod().getName() : operation.summary();
    }

    private PermissionMetadata permissionMetadata(HandlerMethod handlerMethod) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        SaCheckPermission classPermission = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), SaCheckPermission.class);
        SaCheckPermission methodPermission = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), SaCheckPermission.class);
        addPermissionCodes(codes, classPermission);
        addPermissionCodes(codes, methodPermission);
        return new PermissionMetadata(List.copyOf(codes), permissionMode(classPermission, methodPermission));
    }

    private void addPermissionCodes(Set<String> codes, SaCheckPermission permission) {
        if (permission == null) {
            return;
        }
        for (String code : permission.value()) {
            if (code != null && !code.isBlank()) {
                codes.add(code);
            }
        }
    }

    private String permissionMode(SaCheckPermission classPermission, SaCheckPermission methodPermission) {
        SaCheckPermission permission = methodPermission == null ? classPermission : methodPermission;
        if (permission == null || permission.value().length == 0) {
            return null;
        }
        SaMode mode = permission.mode();
        return mode == SaMode.AND ? "AND" : "OR";
    }

    private boolean isWriteOperation(String method) {
        return "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    private record PermissionMetadata(List<String> codes, String mode) {
    }
}
