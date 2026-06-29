package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.config.SaTokenConfig;
import com.ones.admin.system.dto.ApiResourceQuery;
import com.ones.admin.system.dto.ApiResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
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
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiResourceService(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    public PageResult<ApiResourceResponse> queryPage(ApiResourceQuery query) {
        List<ApiResourceResponse> records = listApiResources()
                .stream()
                .filter(resource -> matches(query, resource))
                .toList();
        int fromIndex = (int) Math.min((query.getPageNum() - 1) * query.getPageSize(), records.size());
        int toIndex = (int) Math.min(fromIndex + query.getPageSize(), records.size());
        return PageResult.of(query.getPageNum(), query.getPageSize(), records.size(), records.subList(fromIndex, toIndex));
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
        AccessPolicyMetadata accessPolicyMetadata = accessPolicyMetadata(handlerMethod);
        for (String path : paths(info)) {
            if (!path.startsWith("/api/")) {
                continue;
            }
            for (String method : methods(info)) {
                ApiAuthType authType = authType(path, permissionMetadata, accessPolicyMetadata);
                boolean permissionMissing = isPermissionMissing(path, authType, accessPolicyMetadata);
                responses.add(new ApiResourceResponse(
                        method,
                        path,
                        moduleName(handlerMethod),
                        operationSummary(handlerMethod),
                        permissionMetadata.codes(),
                        permissionMetadata.mode(),
                        authType.name(),
                        accessPolicyMetadata.explicit(),
                        accessPolicyMetadata.reason(),
                        !permissionMetadata.codes().isEmpty(),
                        permissionMissing,
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

    private AccessPolicyMetadata accessPolicyMetadata(HandlerMethod handlerMethod) {
        ApiAccessPolicy classPolicy = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), ApiAccessPolicy.class);
        ApiAccessPolicy methodPolicy = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiAccessPolicy.class);
        ApiAccessPolicy policy = methodPolicy == null ? classPolicy : methodPolicy;
        if (policy == null) {
            return new AccessPolicyMetadata(null, false, null);
        }
        String reason = policy.reason().isBlank() ? null : policy.reason();
        return new AccessPolicyMetadata(policy.value(), true, reason);
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

    private ApiAuthType authType(
            String path,
            PermissionMetadata permissionMetadata,
            AccessPolicyMetadata accessPolicyMetadata
    ) {
        if (isPublicPath(path)) {
            return ApiAuthType.PUBLIC;
        }
        if (!permissionMetadata.codes().isEmpty()) {
            return ApiAuthType.PERMISSION;
        }
        if (accessPolicyMetadata.type() != null) {
            return accessPolicyMetadata.type();
        }
        return ApiAuthType.LOGIN;
    }

    private boolean isPublicPath(String path) {
        return SaTokenConfig.LOGIN_EXCLUDE_PATH_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isPermissionMissing(
            String path,
            ApiAuthType authType,
            AccessPolicyMetadata accessPolicyMetadata
    ) {
        return authType == ApiAuthType.LOGIN
                && path.startsWith("/api/system/")
                && !accessPolicyMetadata.explicit();
    }

    private boolean isWriteOperation(String method) {
        return "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
    }

    private boolean matches(ApiResourceQuery query, ApiResourceResponse resource) {
        return equalsIgnoreCaseIfPresent(query.getMethod(), resource.method())
                && containsIfPresent(query.getPath(), resource.path())
                && containsIfPresent(query.getModule(), resource.module())
                && permissionContainsIfPresent(query.getPermissionCode(), resource.permissionCodes())
                && equalsIgnoreCaseIfPresent(query.getAuthType(), resource.authType())
                && equalsIfPresent(query.getWriteOperation(), resource.writeOperation())
                && equalsIfPresent(query.getPermissionMissing(), resource.permissionMissing());
    }

    private boolean equalsIgnoreCaseIfPresent(String expected, String actual) {
        return !hasText(expected) || (actual != null && actual.equalsIgnoreCase(expected.trim()));
    }

    private boolean containsIfPresent(String expected, String actual) {
        return !hasText(expected) || (actual != null && actual.toLowerCase().contains(expected.trim().toLowerCase()));
    }

    private boolean permissionContainsIfPresent(String expected, List<String> actual) {
        return !hasText(expected) || actual.stream()
                .anyMatch(permission -> permission.toLowerCase().contains(expected.trim().toLowerCase()));
    }

    private boolean equalsIfPresent(Boolean expected, boolean actual) {
        return expected == null || expected == actual;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private record PermissionMetadata(List<String> codes, String mode) {
    }

    private record AccessPolicyMetadata(ApiAuthType type, boolean explicit, String reason) {
    }
}
