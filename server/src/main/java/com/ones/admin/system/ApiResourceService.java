package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.config.SaTokenConfig;
import com.ones.admin.system.dto.ApiResourceGovernanceResponse;
import com.ones.admin.system.dto.ApiResourceManifestDiffResponse;
import com.ones.admin.system.dto.ApiResourceManifestGateRequest;
import com.ones.admin.system.dto.ApiResourceManifestGateResponse;
import com.ones.admin.system.dto.ApiResourceManifestResponse;
import com.ones.admin.system.dto.ApiResourceQuery;
import com.ones.admin.system.dto.ApiResourceResponse;
import com.ones.admin.system.dto.ApiResourceSummaryResponse;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ApiResourceService {

    private static final String PERMISSION_CODE_PATTERN_TEXT =
            "^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$";
    private static final String OPERATION_ID_PATTERN_TEXT =
            "^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$";
    private static final String CHECKSUM_ALGORITHM = "SHA-256";
    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile(PERMISSION_CODE_PATTERN_TEXT);
    private static final Pattern OPERATION_ID_PATTERN = Pattern.compile(OPERATION_ID_PATTERN_TEXT);
    private static final String CSV_HEADER = "apiKey,operationId,method,path,handler,module,summary,authType,permissionCodes,"
            + "permissionMode,requiresPermission,permissionRegistered,permissionAssignable,permissionMissing,"
            + "writeOperation,deprecated,owner,sinceVersion,lifecycle,riskLevel,"
            + "accessPolicyExplicit,accessPolicyReason";

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final SystemPermissionMapper permissionMapper;
    private final SystemMenuMapper menuMapper;
    private final String applicationVersion;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiResourceService(
            RequestMappingHandlerMapping requestMappingHandlerMapping,
            SystemPermissionMapper permissionMapper,
            SystemMenuMapper menuMapper,
            @Value("${ones.version:v0.0.19}") String applicationVersion
    ) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
        this.applicationVersion = applicationVersion;
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
        List<ApiResourceSummaryResponse.LifecycleStat> lifecycles = resources.stream()
                .collect(Collectors.groupingBy(
                        resource -> blankToDefault(resource.lifecycle(), "UNSPECIFIED"),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ApiResourceSummaryResponse.LifecycleStat(entry.getKey(), entry.getValue()))
                .toList();
        List<ApiResourceSummaryResponse.RiskLevelStat> riskLevels = resources.stream()
                .collect(Collectors.groupingBy(
                        resource -> blankToDefault(resource.riskLevel(), "UNSPECIFIED"),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ApiResourceSummaryResponse.RiskLevelStat(entry.getKey(), entry.getValue()))
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
                resources.stream().filter(ApiResourceResponse::deprecated).count(),
                authTypes,
                lifecycles,
                riskLevels,
                modules
        );
    }

    public ApiResourceManifestResponse generateManifest() {
        List<ApiResourceManifestResponse.Resource> resources = listApiResources().stream()
                .map(this::toManifestResource)
                .toList();
        return new ApiResourceManifestResponse(
                applicationVersion,
                CHECKSUM_ALGORITHM,
                checksum(resources),
                resources.size(),
                resources
        );
    }

    public ApiResourceManifestDiffResponse diffManifest(ApiResourceManifestResponse previousManifest) {
        ApiResourceManifestResponse currentManifest = generateManifest();
        List<ApiResourceManifestResponse.Resource> previousResources = resourcesOf(previousManifest);
        List<ApiResourceManifestResponse.Resource> currentResources = resourcesOf(currentManifest);
        Map<String, ApiResourceManifestResponse.Resource> previousByKey = resourcesByKey(previousResources);
        Map<String, ApiResourceManifestResponse.Resource> currentByKey = resourcesByKey(currentResources);
        List<ApiResourceManifestDiffResponse.Change> changes = new ArrayList<>();
        for (ApiResourceManifestResponse.Resource current : currentResources) {
            if (!previousByKey.containsKey(current.apiKey())) {
                changes.add(toAddedChange(current));
            }
        }
        for (ApiResourceManifestResponse.Resource previous : previousResources) {
            ApiResourceManifestResponse.Resource current = currentByKey.get(previous.apiKey());
            if (current == null) {
                changes.add(toRemovedChange(previous));
                continue;
            }
            List<ApiResourceManifestDiffResponse.FieldChange> fieldChanges = diffFields(previous, current);
            if (!fieldChanges.isEmpty()) {
                changes.add(toModifiedChange(previous, current, fieldChanges));
            }
        }
        changes = changes.stream()
                .sorted(Comparator.comparing(ApiResourceManifestDiffResponse.Change::apiKey)
                        .thenComparing(ApiResourceManifestDiffResponse.Change::changeType))
                .toList();
        long addedCount = countChangeType(changes, "ADDED");
        long removedCount = countChangeType(changes, "REMOVED");
        long modifiedCount = countChangeType(changes, "MODIFIED");
        long breakingChangeCount = changes.stream()
                .filter(ApiResourceManifestDiffResponse.Change::breakingChange)
                .count();
        return new ApiResourceManifestDiffResponse(
                previousManifest == null ? null : previousManifest.applicationVersion(),
                currentManifest.applicationVersion(),
                previousManifest == null ? null : previousManifest.checksum(),
                currentManifest.checksum(),
                !changes.isEmpty(),
                addedCount,
                removedCount,
                modifiedCount,
                breakingChangeCount,
                changes
        );
    }

    public ApiResourceManifestGateResponse gateManifest(ApiResourceManifestGateRequest request) {
        ApiResourceManifestResponse previousManifest = request == null ? null : request.previousManifest();
        ApiResourceManifestDiffResponse diff = diffManifest(previousManifest);
        ApiResourceGovernanceResponse governance = checkGovernance();
        List<String> reasons = new ArrayList<>();
        boolean requiredManualReview = diff.breakingChangeCount() > 0;
        boolean reviewReasonRequired = false;
        boolean passed;
        String status;
        if (governance.errorCount() > 0) {
            passed = false;
            status = "BLOCKED";
            reasons.add("接口治理存在 " + governance.errorCount() + " 个错误，必须修复后才能发布");
        } else if (requiredManualReview && !allowBreakingChanges(request)) {
            passed = false;
            status = "BLOCKED";
            reviewReasonRequired = true;
            reasons.add("存在 " + diff.breakingChangeCount() + " 个破坏性接口契约变更，需要人工确认");
        } else if (requiredManualReview && !hasText(request.reviewReason())) {
            passed = false;
            status = "REVIEW_REASON_REQUIRED";
            reviewReasonRequired = true;
            reasons.add("破坏性接口契约变更已允许，但缺少人工确认原因");
        } else if (requiredManualReview) {
            passed = true;
            status = "MANUAL_APPROVED";
            reasons.add("存在 " + diff.breakingChangeCount() + " 个破坏性接口契约变更，已记录人工确认原因");
        } else {
            passed = true;
            status = diff.changed() ? "PASSED_WITH_CHANGES" : "PASSED";
            reasons.add(diff.changed() ? "存在非破坏性接口契约变化，允许发布" : "接口契约未变化，允许发布");
        }
        if (governance.warningCount() > 0) {
            reasons.add("接口治理存在 " + governance.warningCount() + " 个警告，建议跟踪处理");
        }
        return new ApiResourceManifestGateResponse(
                passed,
                status,
                diff.previousVersion(),
                diff.currentVersion(),
                governance.errorCount(),
                governance.warningCount(),
                diff.breakingChangeCount(),
                requiredManualReview,
                reviewReasonRequired,
                reasons,
                diff
        );
    }

    private boolean allowBreakingChanges(ApiResourceManifestGateRequest request) {
        return request != null && request.allowBreakingChanges();
    }

    private List<ApiResourceManifestResponse.Resource> resourcesOf(ApiResourceManifestResponse manifest) {
        if (manifest == null || manifest.resources() == null) {
            return List.of();
        }
        return manifest.resources().stream()
                .filter(resource -> resource != null && hasText(resource.apiKey()))
                .toList();
    }

    private Map<String, ApiResourceManifestResponse.Resource> resourcesByKey(
            List<ApiResourceManifestResponse.Resource> resources
    ) {
        return resources.stream()
                .collect(Collectors.toMap(
                        ApiResourceManifestResponse.Resource::apiKey,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    private ApiResourceManifestDiffResponse.Change toAddedChange(ApiResourceManifestResponse.Resource current) {
        return new ApiResourceManifestDiffResponse.Change(
                "ADDED",
                "INFO",
                current.apiKey(),
                current.method(),
                current.path(),
                false,
                List.of(),
                "新增接口资源"
        );
    }

    private ApiResourceManifestDiffResponse.Change toRemovedChange(ApiResourceManifestResponse.Resource previous) {
        return new ApiResourceManifestDiffResponse.Change(
                "REMOVED",
                "ERROR",
                previous.apiKey(),
                previous.method(),
                previous.path(),
                true,
                List.of(),
                "接口资源已删除，属于破坏性变更"
        );
    }

    private ApiResourceManifestDiffResponse.Change toModifiedChange(
            ApiResourceManifestResponse.Resource previous,
            ApiResourceManifestResponse.Resource current,
            List<ApiResourceManifestDiffResponse.FieldChange> fieldChanges
    ) {
        boolean breaking = fieldChanges.stream()
                .map(ApiResourceManifestDiffResponse.FieldChange::fieldName)
                .anyMatch(this::isBreakingField);
        return new ApiResourceManifestDiffResponse.Change(
                "MODIFIED",
                breaking ? "ERROR" : "WARN",
                current.apiKey(),
                current.method(),
                current.path(),
                breaking,
                fieldChanges,
                breaking ? "接口契约发生破坏性变更" : "接口元数据发生变化"
        );
    }

    private List<ApiResourceManifestDiffResponse.FieldChange> diffFields(
            ApiResourceManifestResponse.Resource previous,
            ApiResourceManifestResponse.Resource current
    ) {
        List<ApiResourceManifestDiffResponse.FieldChange> changes = new ArrayList<>();
        addFieldChange(changes, "operationId", previous.operationId(), current.operationId());
        addFieldChange(changes, "handler", previous.handler(), current.handler());
        addFieldChange(changes, "authType", previous.authType(), current.authType());
        addFieldChange(changes, "permissionCodes",
                String.join("|", permissionCodesOf(previous)),
                String.join("|", permissionCodesOf(current)));
        addFieldChange(changes, "permissionMode", previous.permissionMode(), current.permissionMode());
        addFieldChange(changes, "writeOperation",
                String.valueOf(previous.writeOperation()),
                String.valueOf(current.writeOperation()));
        addFieldChange(changes, "owner", previous.owner(), current.owner());
        addFieldChange(changes, "sinceVersion", previous.sinceVersion(), current.sinceVersion());
        addFieldChange(changes, "lifecycle", previous.lifecycle(), current.lifecycle());
        addFieldChange(changes, "riskLevel", previous.riskLevel(), current.riskLevel());
        addFieldChange(changes, "deprecated",
                String.valueOf(previous.deprecated()),
                String.valueOf(current.deprecated()));
        return changes;
    }

    private List<String> permissionCodesOf(ApiResourceManifestResponse.Resource resource) {
        if (resource.permissionCodes() == null) {
            return List.of();
        }
        return resource.permissionCodes();
    }

    private void addFieldChange(
            List<ApiResourceManifestDiffResponse.FieldChange> changes,
            String fieldName,
            String previousValue,
            String currentValue
    ) {
        if (Objects.equals(nullToEmpty(previousValue), nullToEmpty(currentValue))) {
            return;
        }
        changes.add(new ApiResourceManifestDiffResponse.FieldChange(
                fieldName,
                previousValue,
                currentValue
        ));
    }

    private boolean isBreakingField(String fieldName) {
        return "authType".equals(fieldName)
                || "operationId".equals(fieldName)
                || "permissionCodes".equals(fieldName)
                || "permissionMode".equals(fieldName)
                || "writeOperation".equals(fieldName);
    }

    private long countChangeType(List<ApiResourceManifestDiffResponse.Change> changes, String changeType) {
        return changes.stream()
                .filter(change -> changeType.equals(change.changeType()))
                .count();
    }

    public String exportCsv() {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        listApiResources().forEach(resource -> appendCsvRow(csv, resource));
        return csv.toString();
    }

    public ApiResourceGovernanceResponse checkGovernance() {
        List<ApiResourceResponse> resources = listApiResources();
        List<ApiResourceGovernanceResponse.Violation> violations = new ArrayList<>(resources.stream()
                .flatMap(resource -> governanceViolations(resource).stream())
                .toList());
        violations.addAll(duplicatedOperationIdViolations(resources));
        long errorCount = countSeverity(violations, "ERROR");
        long warningCount = countSeverity(violations, "WARN");
        return new ApiResourceGovernanceResponse(
                errorCount == 0,
                resources.size(),
                violations.size(),
                errorCount,
                warningCount,
                PERMISSION_CODE_PATTERN_TEXT,
                OPERATION_ID_PATTERN_TEXT,
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
        ResourceMetadata resourceMetadata = resourceMetadata(handlerMethod);
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
                        isWriteOperation(method),
                        apiKey(method, path),
                        operationId(handlerMethod),
                        handlerName(handlerMethod),
                        resourceMetadata.owner(),
                        resourceMetadata.sinceVersion(),
                        resourceMetadata.lifecycle(),
                        resourceMetadata.riskLevel(),
                        deprecated(handlerMethod)
                ));
            }
        }
        return responses;
    }

    private ApiResourceManifestResponse.Resource toManifestResource(ApiResourceResponse resource) {
        return new ApiResourceManifestResponse.Resource(
                resource.apiKey(),
                resource.operationId(),
                resource.method(),
                resource.path(),
                resource.handler(),
                resource.authType(),
                resource.permissionCodes(),
                resource.permissionMode(),
                resource.writeOperation(),
                resource.owner(),
                resource.sinceVersion(),
                resource.lifecycle(),
                resource.riskLevel(),
                resource.deprecated()
        );
    }

    private String checksum(List<ApiResourceManifestResponse.Resource> resources) {
        try {
            MessageDigest digest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
            for (ApiResourceManifestResponse.Resource resource : resources) {
                digest.update(canonicalManifestLine(resource).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("接口资源 Manifest 指纹算法不可用：" + CHECKSUM_ALGORITHM, exception);
        }
    }

    private String canonicalManifestLine(ApiResourceManifestResponse.Resource resource) {
        return String.join("\u001F",
                nullToEmpty(resource.apiKey()),
                nullToEmpty(resource.operationId()),
                nullToEmpty(resource.method()),
                nullToEmpty(resource.path()),
                nullToEmpty(resource.handler()),
                nullToEmpty(resource.authType()),
                String.join("|", resource.permissionCodes()),
                nullToEmpty(resource.permissionMode()),
                String.valueOf(resource.writeOperation()),
                nullToEmpty(resource.owner()),
                nullToEmpty(resource.sinceVersion()),
                nullToEmpty(resource.lifecycle()),
                nullToEmpty(resource.riskLevel()),
                String.valueOf(resource.deprecated())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void appendCsvRow(StringBuilder csv, ApiResourceResponse resource) {
        csv.append(csvValue(resource.apiKey())).append(',')
                .append(csvValue(resource.operationId())).append(',')
                .append(csvValue(resource.method())).append(',')
                .append(csvValue(resource.path())).append(',')
                .append(csvValue(resource.handler())).append(',')
                .append(csvValue(resource.module())).append(',')
                .append(csvValue(resource.summary())).append(',')
                .append(csvValue(resource.authType())).append(',')
                .append(csvValue(String.join("|", resource.permissionCodes()))).append(',')
                .append(csvValue(resource.permissionMode())).append(',')
                .append(resource.requiresPermission()).append(',')
                .append(resource.permissionRegistered()).append(',')
                .append(resource.permissionAssignable()).append(',')
                .append(resource.permissionMissing()).append(',')
                .append(resource.writeOperation()).append(',')
                .append(resource.deprecated()).append(',')
                .append(csvValue(resource.owner())).append(',')
                .append(csvValue(resource.sinceVersion())).append(',')
                .append(csvValue(resource.lifecycle())).append(',')
                .append(csvValue(resource.riskLevel())).append(',')
                .append(resource.accessPolicyExplicit()).append(',')
                .append(csvValue(resource.accessPolicyReason()))
                .append('\n');
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        String safeValue = escapeCsvFormula(value);
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private String escapeCsvFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') {
            return "'" + value;
        }
        return value;
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

    private String handlerName(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName();
    }

    private String operationId(HandlerMethod handlerMethod) {
        Operation operation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
        if (operation != null && hasText(operation.operationId())) {
            return operation.operationId().trim();
        }
        return handlerMethod.getBeanType().getSimpleName() + "_" + handlerMethod.getMethod().getName();
    }

    private String apiKey(String method, String path) {
        return method + " " + path;
    }

    private boolean deprecated(HandlerMethod handlerMethod) {
        Operation operation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
        return (operation != null && operation.deprecated())
                || AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Deprecated.class) != null
                || AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Deprecated.class) != null;
    }

    private ResourceMetadata resourceMetadata(HandlerMethod handlerMethod) {
        ApiResourceMetadata classMetadata =
                AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), ApiResourceMetadata.class);
        ApiResourceMetadata methodMetadata =
                AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiResourceMetadata.class);
        return new ResourceMetadata(
                firstText(methodMetadata == null ? null : methodMetadata.owner(),
                        classMetadata == null ? null : classMetadata.owner()),
                firstText(methodMetadata == null ? null : methodMetadata.sinceVersion(),
                        classMetadata == null ? null : classMetadata.sinceVersion()),
                lifecycleName(methodMetadata, classMetadata),
                riskLevelName(methodMetadata, classMetadata)
        );
    }

    private String lifecycleName(ApiResourceMetadata methodMetadata, ApiResourceMetadata classMetadata) {
        ApiLifecycleStatus lifecycle = firstLifecycle(
                methodMetadata == null ? null : methodMetadata.lifecycle(),
                classMetadata == null ? null : classMetadata.lifecycle()
        );
        return lifecycle == null ? null : lifecycle.name();
    }

    private String riskLevelName(ApiResourceMetadata methodMetadata, ApiResourceMetadata classMetadata) {
        ApiRiskLevel riskLevel = firstRiskLevel(
                methodMetadata == null ? null : methodMetadata.riskLevel(),
                classMetadata == null ? null : classMetadata.riskLevel()
        );
        return riskLevel == null ? null : riskLevel.name();
    }

    private ApiLifecycleStatus firstLifecycle(ApiLifecycleStatus first, ApiLifecycleStatus second) {
        if (first != null && first != ApiLifecycleStatus.UNSPECIFIED) {
            return first;
        }
        if (second != null && second != ApiLifecycleStatus.UNSPECIFIED) {
            return second;
        }
        return null;
    }

    private ApiRiskLevel firstRiskLevel(ApiRiskLevel first, ApiRiskLevel second) {
        if (first != null && first != ApiRiskLevel.UNSPECIFIED) {
            return first;
        }
        if (second != null && second != ApiRiskLevel.UNSPECIFIED) {
            return second;
        }
        return null;
    }

    private String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        if (hasText(second)) {
            return second.trim();
        }
        return null;
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
        if (!hasText(resource.operationId())) {
            violations.add(toViolation(
                    resource,
                    "OPERATION_ID_MISSING",
                    "ERROR",
                    "接口缺少稳定 operationId"
            ));
        } else if (!OPERATION_ID_PATTERN.matcher(resource.operationId()).matches()) {
            violations.add(toViolation(
                    resource,
                    "OPERATION_ID_INVALID_FORMAT",
                    "ERROR",
                    "接口 operationId 命名不符合规范：" + resource.operationId()
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
        if (resource.deprecated()) {
            violations.add(toViolation(
                    resource,
                    "DEPRECATED_API",
                    "WARN",
                    "接口已标记废弃，需确认迁移说明和下线计划"
            ));
        }
        if (!hasText(resource.owner())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_API_OWNER",
                    "WARN",
                    "接口缺少负责人元数据"
            ));
        }
        if (!hasText(resource.sinceVersion())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_API_SINCE_VERSION",
                    "WARN",
                    "接口缺少引入版本元数据"
            ));
        }
        if (!hasText(resource.lifecycle())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_API_LIFECYCLE",
                    "WARN",
                    "接口缺少生命周期元数据"
            ));
        }
        if (!hasText(resource.riskLevel())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_API_RISK_LEVEL",
                    "WARN",
                    "接口缺少风险级别元数据"
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

    private List<ApiResourceGovernanceResponse.Violation> duplicatedOperationIdViolations(
            List<ApiResourceResponse> resources
    ) {
        Map<String, List<ApiResourceResponse>> resourcesByOperationId = resources.stream()
                .filter(resource -> hasText(resource.operationId()))
                .collect(Collectors.groupingBy(ApiResourceResponse::operationId));
        return resourcesByOperationId.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .map(resource -> toViolation(
                                resource,
                                "OPERATION_ID_DUPLICATED",
                                "ERROR",
                                "接口 operationId 必须全局唯一：" + entry.getKey()
                        )))
                .toList();
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
                resource.operationId(),
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
                && containsIfPresent(query.getHandler(), resource.handler())
                && equalsIgnoreCaseIfPresent(query.getAuthType(), resource.authType())
                && containsIfPresent(query.getOwner(), resource.owner())
                && equalsIgnoreCaseIfPresent(query.getLifecycle(), resource.lifecycle())
                && equalsIgnoreCaseIfPresent(query.getRiskLevel(), resource.riskLevel())
                && equalsIfPresent(query.getWriteOperation(), resource.writeOperation())
                && equalsIfPresent(query.getPermissionMissing(), resource.permissionMissing())
                && equalsIfPresent(query.getDeprecated(), resource.deprecated());
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

    private record ResourceMetadata(
            String owner,
            String sinceVersion,
            String lifecycle,
            String riskLevel
    ) {
    }
}
