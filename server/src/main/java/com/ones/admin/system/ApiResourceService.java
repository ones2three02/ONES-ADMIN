package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.config.SaTokenConfig;
import com.ones.admin.system.dto.ApiResourceGovernanceResponse;
import com.ones.admin.system.dto.ApiResourceQuery;
import com.ones.admin.system.dto.ApiResourceResponse;
import com.ones.admin.system.dto.ApiResourceSummaryResponse;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ApiResourceService {

    private static final String PERMISSION_CODE_PATTERN_TEXT =
            "^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$";
    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile(PERMISSION_CODE_PATTERN_TEXT);

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final SystemPermissionMapper permissionMapper;
    private final SystemMenuMapper menuMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiResourceService(
            RequestMappingHandlerMapping requestMappingHandlerMapping,
            SystemPermissionMapper permissionMapper,
            SystemMenuMapper menuMapper
    ) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
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

    public ApiResourceSummaryResponse summarize() {
        List<ApiResourceResponse> resources = listApiResources();
        List<ApiResourceSummaryResponse.AuthTypeStat> authTypes = resources.stream()
                .collect(Collectors.groupingBy(ApiResourceResponse::authType, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ApiResourceSummaryResponse.AuthTypeStat(entry.getKey(), entry.getValue()))
                .toList();
        List<ApiResourceSummaryResponse.ModuleStat> modules = resources.stream()
                .collect(Collectors.groupingBy(resource -> blankToDefault(resource.module(), "未分组")))
                .entrySet()
                .stream()
                .map(entry -> toModuleStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ApiResourceSummaryResponse.ModuleStat::module))
                .toList();
        return new ApiResourceSummaryResponse(
                resources.size(),
                resources.stream().filter(ApiResourceResponse::writeOperation).count(),
                resources.stream().filter(ApiResourceResponse::permissionMissing).count(),
                resources.stream().filter(ApiResourceResponse::accessPolicyExplicit).count(),
                authTypes,
                modules
        );
    }

    public ApiResourceGovernanceResponse checkGovernance() {
        List<ApiResourceResponse> resources = listApiResources();
        List<ApiResourceGovernanceResponse.Violation> violations = resources.stream()
                .flatMap(resource -> governanceViolations(resource).stream())
                .toList();
        long errorCount = countSeverity(violations, "ERROR");
        long warningCount = countSeverity(violations, "WARN");
        return new ApiResourceGovernanceResponse(
                errorCount == 0,
                resources.size(),
                violations.size(),
                errorCount,
                warningCount,
                PERMISSION_CODE_PATTERN_TEXT,
                violations
        );
    }

    public List<ApiResourceResponse> listApiResources() {
        Set<String> registeredPermissionCodes = new LinkedHashSet<>(permissionMapper.selectAllCodes());
        Set<String> assignablePermissionCodes = new LinkedHashSet<>(menuMapper.selectAssignablePermissionCodes());
        return requestMappingHandlerMapping.getHandlerMethods()
                .entrySet()
                .stream()
                .flatMap(entry -> toResponses(
                        entry.getKey(),
                        entry.getValue(),
                        registeredPermissionCodes,
                        assignablePermissionCodes
                ).stream())
                .sorted(Comparator.comparing(ApiResourceResponse::path)
                        .thenComparing(ApiResourceResponse::method))
                .toList();
    }

    private List<ApiResourceResponse> toResponses(
            RequestMappingInfo info,
            HandlerMethod handlerMethod,
            Set<String> registeredPermissionCodes,
            Set<String> assignablePermissionCodes
    ) {
        List<ApiResourceResponse> responses = new ArrayList<>();
        PermissionMetadata permissionMetadata = permissionMetadata(handlerMethod);
        AccessPolicyMetadata accessPolicyMetadata = accessPolicyMetadata(handlerMethod);
        List<String> unregisteredPermissionCodes = missingPermissionCodes(
                permissionMetadata.codes(),
                registeredPermissionCodes
        );
        List<String> unassignablePermissionCodes = missingPermissionCodes(
                permissionMetadata.codes(),
                assignablePermissionCodes
        );
        List<String> invalidPermissionCodes = invalidPermissionCodes(permissionMetadata.codes());
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
                        invalidPermissionCodes.isEmpty(),
                        invalidPermissionCodes,
                        permissionMetadata.mode(),
                        authType.name(),
                        accessPolicyMetadata.explicit(),
                        accessPolicyMetadata.reason(),
                        !permissionMetadata.codes().isEmpty(),
                        unregisteredPermissionCodes.isEmpty(),
                        unregisteredPermissionCodes,
                        unassignablePermissionCodes.isEmpty(),
                        unassignablePermissionCodes,
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

    private List<ApiResourceGovernanceResponse.Violation> governanceViolations(ApiResourceResponse resource) {
        List<ApiResourceGovernanceResponse.Violation> violations = new ArrayList<>();
        if (resource.permissionMissing()) {
            violations.add(toViolation(
                    resource,
                    "API_PERMISSION_MISSING",
                    "ERROR",
                    "系统接口缺少权限点或显式访问策略"
            ));
        }
        if (!resource.invalidPermissionCodes().isEmpty()) {
            violations.add(toViolation(
                    resource,
                    "PERMISSION_CODE_INVALID_FORMAT",
                    "ERROR",
                    "接口权限码命名不符合规范：" + String.join(",", resource.invalidPermissionCodes())
            ));
        }
        if (!resource.unregisteredPermissionCodes().isEmpty()) {
            violations.add(toViolation(
                    resource,
                    "PERMISSION_CODE_UNREGISTERED",
                    "ERROR",
                    "接口权限点未在系统权限表注册：" + String.join(",", resource.unregisteredPermissionCodes())
            ));
        }
        if (resource.path().startsWith("/api/system/")
                && resource.writeOperation()
                && !resource.requiresPermission()) {
            violations.add(toViolation(
                    resource,
                    "SYSTEM_WRITE_API_WITHOUT_PERMISSION",
                    "ERROR",
                    "系统写接口必须配置权限点"
            ));
        }
        if (!resource.unassignablePermissionCodes().isEmpty()) {
            violations.add(toViolation(
                    resource,
                    "PERMISSION_CODE_UNASSIGNABLE",
                    "WARN",
                    "接口权限点未挂载到菜单权限树，角色页面无法授权：" + String.join(",", resource.unassignablePermissionCodes())
            ));
        }
        if (!hasText(resource.module())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_MODULE_TAG",
                    "WARN",
                    "接口缺少 OpenAPI 模块标签"
            ));
        }
        if (!hasText(resource.summary())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_OPERATION_SUMMARY",
                    "WARN",
                    "接口缺少 OpenAPI 摘要"
            ));
        }
        return violations;
    }

    private ApiResourceGovernanceResponse.Violation toViolation(
            ApiResourceResponse resource,
            String ruleCode,
            String severity,
            String message
    ) {
        return new ApiResourceGovernanceResponse.Violation(
                ruleCode,
                severity,
                resource.method(),
                resource.path(),
                resource.module(),
                resource.summary(),
                message
        );
    }

    private long countSeverity(List<ApiResourceGovernanceResponse.Violation> violations, String severity) {
        return violations.stream()
                .filter(violation -> severity.equals(violation.severity()))
                .count();
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

    private List<String> missingPermissionCodes(List<String> permissionCodes, Set<String> existingCodes) {
        return permissionCodes.stream()
                .filter(permission -> !existingCodes.contains(permission))
                .toList();
    }

    private List<String> invalidPermissionCodes(List<String> permissionCodes) {
        return permissionCodes.stream()
                .filter(permission -> !PERMISSION_CODE_PATTERN.matcher(permission).matches())
                .toList();
    }

    private boolean equalsIfPresent(Boolean expected, boolean actual) {
        return expected == null || expected == actual;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private ApiResourceSummaryResponse.ModuleStat toModuleStat(String module, List<ApiResourceResponse> resources) {
        return new ApiResourceSummaryResponse.ModuleStat(
                module,
                resources.size(),
                countAuthType(resources, ApiAuthType.PUBLIC),
                countAuthType(resources, ApiAuthType.LOGIN),
                countAuthType(resources, ApiAuthType.PERMISSION),
                resources.stream().filter(ApiResourceResponse::writeOperation).count(),
                resources.stream().filter(ApiResourceResponse::permissionMissing).count()
        );
    }

    private long countAuthType(List<ApiResourceResponse> resources, ApiAuthType authType) {
        return resources.stream()
                .filter(resource -> authType.name().equals(resource.authType()))
                .count();
    }

    private String blankToDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private record PermissionMetadata(List<String> codes, String mode) {
    }

    private record AccessPolicyMetadata(ApiAuthType type, boolean explicit, String reason) {
    }
}
