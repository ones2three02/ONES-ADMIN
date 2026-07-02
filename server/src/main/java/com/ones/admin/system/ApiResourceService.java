package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.config.SaTokenConfig;
import com.ones.admin.system.dto.ApiResourceManifestSnapshotPublishRequest;
import com.ones.admin.system.dto.ApiResourceManifestSnapshotPublishResponse;
import com.ones.admin.system.dto.ApiResourceManifestSnapshotQuery;
import com.ones.admin.system.dto.ApiResourceManifestSnapshotResponse;
import com.ones.admin.system.dto.ApiResourceGovernanceResponse;
import com.ones.admin.system.dto.ApiResourceGovernanceReportResponse;
import com.ones.admin.system.dto.ApiResourceGovernanceRuleResponse;
import com.ones.admin.system.dto.ApiResourceManifestDiffResponse;
import com.ones.admin.system.dto.ApiResourceManifestGateRequest;
import com.ones.admin.system.dto.ApiResourceManifestGateResponse;
import com.ones.admin.system.dto.ApiResourceManifestLatestGateRequest;
import com.ones.admin.system.dto.ApiResourceManifestLatestGateResponse;
import com.ones.admin.system.dto.ApiResourceManifestResponse;
import com.ones.admin.system.dto.ApiResourceQuery;
import com.ones.admin.system.dto.ApiResourceResponse;
import com.ones.admin.system.dto.ApiResourceSummaryResponse;
import com.ones.admin.system.entity.SystemApiManifestSnapshotEntity;
import com.ones.admin.system.mapper.SystemApiManifestSnapshotMapper;
import com.ones.admin.system.mapper.SystemMenuMapper;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private static final String API_VERSION_PATTERN_TEXT =
            "^v\\d+\\.\\d+\\.\\d+$";
    private static final String CHECKSUM_ALGORITHM = "SHA-256";
    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile(PERMISSION_CODE_PATTERN_TEXT);
    private static final Pattern OPERATION_ID_PATTERN = Pattern.compile(OPERATION_ID_PATTERN_TEXT);
    private static final Pattern API_VERSION_PATTERN = Pattern.compile(API_VERSION_PATTERN_TEXT);
    private static final String CSV_HEADER = "apiKey,operationId,method,path,handler,module,summary,authType,permissionCodes,"
            + "permissionMode,requiresPermission,permissionRegistered,permissionAssignable,permissionMissing,"
            + "writeOperation,repeatSubmitProtected,deprecated,owner,audience,sinceVersion,lifecycle,riskLevel,"
            + "sunsetVersion,replacementApiKey,"
            + "accessPolicyExplicit,accessPolicyReason";
    private static final List<GovernanceRule> GOVERNANCE_RULES = List.of(
            new GovernanceRule(
                    "API_PERMISSION_MISSING",
                    "ERROR",
                    "SECURITY",
                    "系统接口缺少权限点或显式访问策略",
                    "为接口补充 @SaCheckPermission 权限点；如果确实只需要登录态访问，补充 @ApiAccessPolicy 并说明原因"
            ),
            new GovernanceRule(
                    "PUBLIC_API_WITHOUT_ACCESS_POLICY",
                    "ERROR",
                    "SECURITY",
                    "公开接口缺少显式访问策略",
                    "为公开接口补充 @ApiAccessPolicy(ApiAuthType.PUBLIC, reason = \"...\")，说明开放原因、调用方和安全补偿措施"
            ),
            new GovernanceRule(
                    "PUBLIC_API_NOT_IN_RUNTIME_WHITELIST",
                    "ERROR",
                    "SECURITY",
                    "公开接口未加入运行时白名单",
                    "将公开接口加入 SaTokenConfig.LOGIN_EXCLUDE_PATH_PATTERNS，或改为 LOGIN/PERMISSION 访问策略，确保接口目录和运行时拦截器一致"
            ),
            new GovernanceRule(
                    "API_METHOD_NOT_EXPLICIT",
                    "ERROR",
                    "CONTRACT",
                    "接口未显式声明 HTTP 方法",
                    "使用 @GetMapping、@PostMapping、@PutMapping、@DeleteMapping，或在 @RequestMapping(method = ...) 中声明明确方法，避免 ALL 泛匹配破坏接口契约和网关路由"
            ),
            new GovernanceRule(
                    "PERMISSION_CODE_INVALID_FORMAT",
                    "ERROR",
                    "SECURITY",
                    "接口权限码命名不符合规范",
                    "按 permissionCodePattern 调整权限码，建议使用 模块:资源:动作 形式，例如 system:user:list"
            ),
            new GovernanceRule(
                    "OPERATION_ID_MISSING",
                    "ERROR",
                    "CONTRACT",
                    "接口缺少稳定 operationId",
                    "在 @Operation 中补充稳定 operationId，建议使用 Controller_动作 形式"
            ),
            new GovernanceRule(
                    "OPERATION_ID_INVALID_FORMAT",
                    "ERROR",
                    "CONTRACT",
                    "接口 operationId 命名不符合规范",
                    "按 operationIdPattern 调整 operationId，建议使用 Controller_动作 形式，避免点号、短横线和空格"
            ),
            new GovernanceRule(
                    "PERMISSION_CODE_UNREGISTERED",
                    "ERROR",
                    "SECURITY",
                    "接口权限点未在系统权限表注册",
                    "在系统权限初始化或权限迁移中注册该权限码，并确认超级管理员角色可获得授权"
            ),
            new GovernanceRule(
                    "SYSTEM_WRITE_API_WITHOUT_PERMISSION",
                    "ERROR",
                    "SECURITY",
                    "系统写接口未配置权限点",
                    "系统写接口必须补充 @SaCheckPermission，或通过 @ApiAccessPolicy 明确例外并接受安全评审"
            ),
            new GovernanceRule(
                    "HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT",
                    "ERROR",
                    "SECURITY",
                    "非公开高风险写接口缺少重复提交防护",
                    "为非公开高风险写接口补充 @RepeatSubmit，并结合业务幂等键、唯一约束或审批流保证重复请求不会造成脏数据；公开登录接口由登录失败锁定策略保护"
            ),
            new GovernanceRule(
                    "PERMISSION_CODE_UNASSIGNABLE",
                    "WARN",
                    "SECURITY",
                    "接口权限点未挂载到菜单权限树",
                    "将权限点挂载到菜单权限树的 button 节点，确保角色授权页面可以分配该能力"
            ),
            new GovernanceRule(
                    "DEPRECATED_API",
                    "WARN",
                    "LIFECYCLE",
                    "接口已标记废弃",
                    "补充迁移说明、下线版本和替代接口，并在发布计划中跟踪调用方迁移"
            ),
            new GovernanceRule(
                    "DEPRECATED_API_MISSING_SUNSET_VERSION",
                    "WARN",
                    "LIFECYCLE",
                    "废弃接口缺少计划下线版本",
                    "在 @ApiResourceMetadata 中补充 sunsetVersion，记录计划下线版本，例如 v1.2.0"
            ),
            new GovernanceRule(
                    "DEPRECATED_API_SUNSET_VERSION_INVALID_FORMAT",
                    "WARN",
                    "LIFECYCLE",
                    "废弃接口计划下线版本格式不符合规范",
                    "按 apiVersionPattern 调整 sunsetVersion，统一使用 v主版本.次版本.修订版本，例如 v1.2.3"
            ),
            new GovernanceRule(
                    "DEPRECATED_API_MISSING_REPLACEMENT",
                    "WARN",
                    "LIFECYCLE",
                    "废弃接口缺少替代接口",
                    "在 @ApiResourceMetadata 中补充 replacementApiKey，记录替代接口 apiKey，例如 GET /api/system/users"
            ),
            new GovernanceRule(
                    "LIFECYCLE_DEPRECATED_WITHOUT_DEPRECATED_FLAG",
                    "WARN",
                    "LIFECYCLE",
                    "接口生命周期为 DEPRECATED 但未同步 OpenAPI 废弃标记",
                    "在 @Operation 中设置 deprecated = true，或使用 @Deprecated，确保 OpenAPI、API Catalog 和运行时治理状态一致"
            ),
            new GovernanceRule(
                    "REMOVED_API_STILL_MAPPED",
                    "ERROR",
                    "LIFECYCLE",
                    "生命周期为 REMOVED 的接口仍在运行时暴露",
                    "删除运行时路由或将接口恢复为 DEPRECATED/ACTIVE；REMOVED 只能用于已下线且不再暴露的接口资产记录"
            ),
            new GovernanceRule(
                    "MISSING_API_OWNER",
                    "WARN",
                    "CATALOG",
                    "接口缺少负责人元数据",
                    "在 @ApiResourceMetadata 中补充 owner，明确接口资产负责人"
            ),
            new GovernanceRule(
                    "MISSING_API_AUDIENCE",
                    "WARN",
                    "CATALOG",
                    "接口缺少受众元数据",
                    "在 @ApiResourceMetadata 中补充 audience，明确接口主要调用方，例如 ADMIN_PORTAL、OPS_PLATFORM、INTEGRATION_CLIENT"
            ),
            new GovernanceRule(
                    "MISSING_API_SINCE_VERSION",
                    "WARN",
                    "CATALOG",
                    "接口缺少引入版本元数据",
                    "在 @ApiResourceMetadata 中补充 sinceVersion，记录接口首次引入版本"
            ),
            new GovernanceRule(
                    "API_SINCE_VERSION_INVALID_FORMAT",
                    "WARN",
                    "CATALOG",
                    "接口引入版本格式不符合规范",
                    "按 apiVersionPattern 调整 sinceVersion，统一使用 v主版本.次版本.修订版本，例如 v1.2.3"
            ),
            new GovernanceRule(
                    "MISSING_API_LIFECYCLE",
                    "WARN",
                    "LIFECYCLE",
                    "接口缺少生命周期元数据",
                    "在 @ApiResourceMetadata 中补充 lifecycle，明确 ACTIVE、DEPRECATED 等生命周期状态"
            ),
            new GovernanceRule(
                    "MISSING_API_RISK_LEVEL",
                    "WARN",
                    "CATALOG",
                    "接口缺少风险级别元数据",
                    "在 @ApiResourceMetadata 中补充 riskLevel，写接口和敏感接口建议标记为 HIGH"
            ),
            new GovernanceRule(
                    "MISSING_MODULE_TAG",
                    "WARN",
                    "DOCUMENTATION",
                    "接口缺少 OpenAPI 模块标签",
                    "在 Controller 上补充 @Tag，确保接口可归属到清晰模块"
            ),
            new GovernanceRule(
                    "MISSING_OPERATION_SUMMARY",
                    "WARN",
                    "DOCUMENTATION",
                    "接口缺少 OpenAPI 摘要",
                    "在方法上补充 @Operation(summary = \"...\")，用业务语义描述接口用途"
            ),
            new GovernanceRule(
                    "OPERATION_ID_DUPLICATED",
                    "ERROR",
                    "CONTRACT",
                    "接口 operationId 不唯一",
                    "为重复接口分配唯一 operationId，建议使用具体 Controller_动作 命名，避免客户端生成和接口目录引用冲突"
            )
    );
    private static final Map<String, GovernanceRule> GOVERNANCE_RULE_BY_CODE = GOVERNANCE_RULES.stream()
            .collect(Collectors.toUnmodifiableMap(GovernanceRule::ruleCode, Function.identity()));

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final SystemPermissionMapper permissionMapper;
    private final SystemMenuMapper menuMapper;
    private final SystemApiManifestSnapshotMapper manifestSnapshotMapper;
    private final ObjectMapper objectMapper;
    private final String applicationVersion;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiResourceService(
            RequestMappingHandlerMapping requestMappingHandlerMapping,
            SystemPermissionMapper permissionMapper,
            SystemMenuMapper menuMapper,
            SystemApiManifestSnapshotMapper manifestSnapshotMapper,
            ObjectMapper objectMapper,
            @Value("${ones.version:v0.0.48}") String applicationVersion
    ) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
        this.manifestSnapshotMapper = manifestSnapshotMapper;
        this.objectMapper = objectMapper;
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
        List<ApiResourceSummaryResponse.OwnerStat> owners = resources.stream()
                .collect(Collectors.groupingBy(resource -> blankToDefault(resource.owner(), "未归属")))
                .entrySet()
                .stream()
                .map(entry -> toOwnerStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingLong(ApiResourceSummaryResponse.OwnerStat::total)
                        .reversed()
                        .thenComparing(ApiResourceSummaryResponse.OwnerStat::owner))
                .toList();
        List<ApiResourceSummaryResponse.AudienceStat> audiences = resources.stream()
                .collect(Collectors.groupingBy(resource -> blankToDefault(resource.audience(), "未声明")))
                .entrySet()
                .stream()
                .map(entry -> toAudienceStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingLong(ApiResourceSummaryResponse.AudienceStat::total)
                        .reversed()
                        .thenComparing(ApiResourceSummaryResponse.AudienceStat::audience))
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
                owners,
                audiences,
                modules
        );
    }

    public ApiResourceGovernanceRuleResponse listGovernanceRules() {
        List<ApiResourceGovernanceRuleResponse.Rule> rules = GOVERNANCE_RULES.stream()
                .map(rule -> new ApiResourceGovernanceRuleResponse.Rule(
                        rule.ruleCode(),
                        rule.severity(),
                        "ERROR".equals(rule.severity()),
                        rule.category(),
                        rule.description(),
                        rule.remediation()
                ))
                .toList();
        return new ApiResourceGovernanceRuleResponse(
                PERMISSION_CODE_PATTERN_TEXT,
                OPERATION_ID_PATTERN_TEXT,
                API_VERSION_PATTERN_TEXT,
                rules
        );
    }

    public ApiResourceGovernanceReportResponse generateGovernanceReport() {
        return new ApiResourceGovernanceReportResponse(
                applicationVersion,
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                summarize(),
                checkGovernance(),
                listGovernanceRules(),
                generateManifest(),
                gateManifestWithLatestSnapshot(new ApiResourceManifestLatestGateRequest(false, null))
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

    @Transactional
    public ApiResourceManifestSnapshotPublishResponse publishManifestSnapshot(
            ApiResourceManifestSnapshotPublishRequest request
    ) {
        ApiResourceManifestSnapshotResponse latestSnapshot = latestManifestSnapshot();
        ApiResourceManifestGateRequest gateRequest = new ApiResourceManifestGateRequest(
                latestSnapshot == null ? null : latestSnapshot.manifest(),
                request != null && request.allowBreakingChanges(),
                request == null ? null : request.reviewReason()
        );
        ApiResourceManifestGateResponse gate = gateManifest(gateRequest);
        if (!gate.passed()) {
            return new ApiResourceManifestSnapshotPublishResponse(false, null, gate);
        }

        ApiResourceManifestResponse manifest = generateManifest();
        SystemApiManifestSnapshotEntity existing = manifestSnapshotMapper.selectOne(
                new LambdaQueryWrapper<SystemApiManifestSnapshotEntity>()
                        .eq(SystemApiManifestSnapshotEntity::getApplicationVersion, manifest.applicationVersion())
                        .eq(SystemApiManifestSnapshotEntity::getChecksum, manifest.checksum())
                        .last("limit 1")
        );
        if (existing != null) {
            return new ApiResourceManifestSnapshotPublishResponse(
                    false,
                    toSnapshotResponse(existing, true),
                    gate
            );
        }

        SystemApiManifestSnapshotEntity snapshot = new SystemApiManifestSnapshotEntity();
        snapshot.setApplicationVersion(manifest.applicationVersion());
        snapshot.setChecksumAlgorithm(manifest.checksumAlgorithm());
        snapshot.setChecksum(manifest.checksum());
        snapshot.setResourceCount(manifest.total());
        snapshot.setManifestJson(writeManifest(manifest));
        snapshot.setPublishStatus(gate.status());
        snapshot.setReviewReason(trimToNull(request == null ? null : request.reviewReason()));
        snapshot.setCreatedAt(java.time.LocalDateTime.now());
        manifestSnapshotMapper.insert(snapshot);
        return new ApiResourceManifestSnapshotPublishResponse(
                true,
                toSnapshotResponse(snapshot, true),
                gate
        );
    }

    public PageResult<ApiResourceManifestSnapshotResponse> queryManifestSnapshots(
            ApiResourceManifestSnapshotQuery query
    ) {
        IPage<SystemApiManifestSnapshotEntity> page = manifestSnapshotMapper.selectPage(
                query.toMyBatisPage(),
                buildSnapshotQueryWrapper(query)
        );
        List<ApiResourceManifestSnapshotResponse> records = page.getRecords()
                .stream()
                .map(snapshot -> toSnapshotResponse(snapshot, false))
                .toList();
        return PageResult.of(page, records);
    }

    public ApiResourceManifestSnapshotResponse latestManifestSnapshot() {
        SystemApiManifestSnapshotEntity latest = manifestSnapshotMapper.selectOne(
                new LambdaQueryWrapper<SystemApiManifestSnapshotEntity>()
                        .orderByDesc(SystemApiManifestSnapshotEntity::getId)
                        .last("limit 1")
        );
        return latest == null ? null : toSnapshotResponse(latest, true);
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
                buildGateChecks(request, governance, diff),
                reasons,
                diff
        );
    }

    private List<ApiResourceManifestGateResponse.Check> buildGateChecks(
            ApiResourceManifestGateRequest request,
            ApiResourceGovernanceResponse governance,
            ApiResourceManifestDiffResponse diff
    ) {
        boolean governancePassed = governance.errorCount() == 0;
        boolean breakingPassed = diff.breakingChangeCount() == 0
                || allowBreakingChanges(request) && hasText(request.reviewReason());
        boolean warningPassed = governance.warningCount() == 0;
        return List.of(
                new ApiResourceManifestGateResponse.Check(
                        "API_GOVERNANCE_ERROR",
                        "ERROR",
                        governancePassed,
                        true,
                        governancePassed
                                ? "接口治理错误检查通过"
                                : "接口治理存在 " + governance.errorCount() + " 个错误",
                        governancePassed
                                ? "无需处理"
                                : "调用 /api/system/api-resources/governance 查看 violations，并按 /governance/rules 修复 ERROR 级规则"
                ),
                new ApiResourceManifestGateResponse.Check(
                        "BREAKING_CHANGE_REVIEW",
                        "ERROR",
                        breakingPassed,
                        true,
                        breakingReviewMessage(request, diff),
                        breakingReviewRemediation(request, diff)
                ),
                new ApiResourceManifestGateResponse.Check(
                        "GOVERNANCE_WARNING_TRACKING",
                        "WARN",
                        warningPassed,
                        false,
                        warningPassed
                                ? "接口治理警告检查通过"
                                : "接口治理存在 " + governance.warningCount() + " 个警告",
                        warningPassed
                                ? "无需处理"
                                : "按 /api/system/api-resources/governance 和 /governance/rules 跟踪 WARN 级规则"
                ),
                new ApiResourceManifestGateResponse.Check(
                        "MANIFEST_DIFF_ARCHIVE",
                        "INFO",
                        true,
                        false,
                        diff.changed() ? "接口契约存在变更，建议归档差异明细" : "接口契约未变化",
                        "将 diff.changes 归档到 Jenkins 构建产物，便于版本审计和回滚排查"
                )
        );
    }

    private String breakingReviewMessage(
            ApiResourceManifestGateRequest request,
            ApiResourceManifestDiffResponse diff
    ) {
        if (diff.breakingChangeCount() == 0) {
            return "未发现破坏性接口契约变更";
        }
        if (!allowBreakingChanges(request)) {
            return "存在 " + diff.breakingChangeCount() + " 个破坏性接口契约变更，尚未允许人工确认";
        }
        if (!hasText(request.reviewReason())) {
            return "存在 " + diff.breakingChangeCount() + " 个破坏性接口契约变更，但缺少人工确认原因";
        }
        return "存在 " + diff.breakingChangeCount() + " 个破坏性接口契约变更，已记录人工确认原因";
    }

    private String breakingReviewRemediation(
            ApiResourceManifestGateRequest request,
            ApiResourceManifestDiffResponse diff
    ) {
        if (diff.breakingChangeCount() == 0) {
            return "无需处理";
        }
        if (!allowBreakingChanges(request)) {
            return "优先保持接口兼容；如确认可接受，设置 allowBreakingChanges=true 并填写 reviewReason";
        }
        if (!hasText(request.reviewReason())) {
            return "补充 reviewReason，记录架构负责人、影响范围和调用方迁移计划";
        }
        return "保留人工确认记录，并同步调用方迁移计划";
    }

    public ApiResourceManifestLatestGateResponse gateManifestWithLatestSnapshot(
            ApiResourceManifestLatestGateRequest request
    ) {
        ApiResourceManifestSnapshotResponse latestSnapshot = latestManifestSnapshot();
        ApiResourceManifestGateRequest gateRequest = new ApiResourceManifestGateRequest(
                latestSnapshot == null ? null : latestSnapshot.manifest(),
                request != null && request.allowBreakingChanges(),
                request == null ? null : request.reviewReason()
        );
        return new ApiResourceManifestLatestGateResponse(
                latestSnapshot != null,
                latestSnapshot == null ? null : latestSnapshot.id(),
                latestSnapshot == null ? null : latestSnapshot.applicationVersion(),
                latestSnapshot == null ? null : latestSnapshot.checksum(),
                gateManifest(gateRequest)
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
        addFieldChange(changes, "repeatSubmitProtected",
                String.valueOf(previous.repeatSubmitProtected()),
                String.valueOf(current.repeatSubmitProtected()));
        addFieldChange(changes, "owner", previous.owner(), current.owner());
        addFieldChange(changes, "audience", previous.audience(), current.audience());
        addFieldChange(changes, "sinceVersion", previous.sinceVersion(), current.sinceVersion());
        addFieldChange(changes, "lifecycle", previous.lifecycle(), current.lifecycle());
        addFieldChange(changes, "riskLevel", previous.riskLevel(), current.riskLevel());
        addFieldChange(changes, "sunsetVersion", previous.sunsetVersion(), current.sunsetVersion());
        addFieldChange(changes, "replacementApiKey", previous.replacementApiKey(), current.replacementApiKey());
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
                || "writeOperation".equals(fieldName)
                || "repeatSubmitProtected".equals(fieldName);
    }

    private long countChangeType(List<ApiResourceManifestDiffResponse.Change> changes, String changeType) {
        return changes.stream()
                .filter(change -> changeType.equals(change.changeType()))
                .count();
    }

    private LambdaQueryWrapper<SystemApiManifestSnapshotEntity> buildSnapshotQueryWrapper(
            ApiResourceManifestSnapshotQuery query
    ) {
        LambdaQueryWrapper<SystemApiManifestSnapshotEntity> wrapper =
                new LambdaQueryWrapper<SystemApiManifestSnapshotEntity>()
                        .orderByDesc(SystemApiManifestSnapshotEntity::getId);
        if (hasText(query.getApplicationVersion())) {
            wrapper.eq(
                    SystemApiManifestSnapshotEntity::getApplicationVersion,
                    query.getApplicationVersion().trim()
            );
        }
        if (hasText(query.getChecksum())) {
            wrapper.eq(SystemApiManifestSnapshotEntity::getChecksum, query.getChecksum().trim());
        }
        return wrapper;
    }

    private ApiResourceManifestSnapshotResponse toSnapshotResponse(
            SystemApiManifestSnapshotEntity snapshot,
            boolean includeManifest
    ) {
        return new ApiResourceManifestSnapshotResponse(
                snapshot.getId(),
                snapshot.getApplicationVersion(),
                snapshot.getChecksumAlgorithm(),
                snapshot.getChecksum(),
                snapshot.getResourceCount() == null ? 0 : snapshot.getResourceCount(),
                snapshot.getPublishStatus(),
                snapshot.getReviewReason(),
                snapshot.getCreatedAt(),
                includeManifest ? readManifest(snapshot.getManifestJson()) : null
        );
    }

    private String writeManifest(ApiResourceManifestResponse manifest) {
        try {
            return objectMapper.writeValueAsString(manifest);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("接口资源 Manifest 序列化失败");
        }
    }

    private ApiResourceManifestResponse readManifest(String manifestJson) {
        try {
            return objectMapper.readValue(manifestJson, ApiResourceManifestResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("接口资源 Manifest 快照解析失败");
        }
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
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
                API_VERSION_PATTERN_TEXT,
                summarizeRules(violations),
                summarizeCategories(violations),
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
        boolean repeatSubmitProtected = repeatSubmitProtected(handlerMethod);
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
                        repeatSubmitProtected,
                        apiKey(method, path),
                        operationId(handlerMethod),
                        handlerName(handlerMethod),
                        resourceMetadata.owner(),
                        resourceMetadata.audience(),
                        resourceMetadata.sinceVersion(),
                        resourceMetadata.lifecycle(),
                        resourceMetadata.riskLevel(),
                        resourceMetadata.sunsetVersion(),
                        resourceMetadata.replacementApiKey(),
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
                resource.repeatSubmitProtected(),
                resource.owner(),
                resource.audience(),
                resource.sinceVersion(),
                resource.lifecycle(),
                resource.riskLevel(),
                resource.sunsetVersion(),
                resource.replacementApiKey(),
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
                String.valueOf(resource.repeatSubmitProtected()),
                nullToEmpty(resource.owner()),
                nullToEmpty(resource.audience()),
                nullToEmpty(resource.sinceVersion()),
                nullToEmpty(resource.lifecycle()),
                nullToEmpty(resource.riskLevel()),
                nullToEmpty(resource.sunsetVersion()),
                nullToEmpty(resource.replacementApiKey()),
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
                .append(resource.repeatSubmitProtected()).append(',')
                .append(resource.deprecated()).append(',')
                .append(csvValue(resource.owner())).append(',')
                .append(csvValue(resource.audience())).append(',')
                .append(csvValue(resource.sinceVersion())).append(',')
                .append(csvValue(resource.lifecycle())).append(',')
                .append(csvValue(resource.riskLevel())).append(',')
                .append(csvValue(resource.sunsetVersion())).append(',')
                .append(csvValue(resource.replacementApiKey())).append(',')
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

    private boolean repeatSubmitProtected(HandlerMethod handlerMethod) {
        return AnnotationUtils.findAnnotation(handlerMethod.getMethod(), RepeatSubmit.class) != null;
    }

    private ResourceMetadata resourceMetadata(HandlerMethod handlerMethod) {
        ApiResourceMetadata classMetadata =
                AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), ApiResourceMetadata.class);
        ApiResourceMetadata methodMetadata =
                AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiResourceMetadata.class);
        return new ResourceMetadata(
                firstText(methodMetadata == null ? null : methodMetadata.owner(),
                        classMetadata == null ? null : classMetadata.owner()),
                firstText(methodMetadata == null ? null : methodMetadata.audience(),
                        classMetadata == null ? null : classMetadata.audience()),
                firstText(methodMetadata == null ? null : methodMetadata.sinceVersion(),
                        classMetadata == null ? null : classMetadata.sinceVersion()),
                lifecycleName(methodMetadata, classMetadata),
                riskLevelName(methodMetadata, classMetadata),
                firstText(methodMetadata == null ? null : methodMetadata.sunsetVersion(),
                        classMetadata == null ? null : classMetadata.sunsetVersion()),
                firstText(methodMetadata == null ? null : methodMetadata.replacementApiKey(),
                        classMetadata == null ? null : classMetadata.replacementApiKey())
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
        if ("PUBLIC".equals(resource.authType()) && !resource.accessPolicyExplicit()) {
            violations.add(toViolation(
                    resource,
                    "PUBLIC_API_WITHOUT_ACCESS_POLICY",
                    "ERROR",
                    "公开接口必须显式声明访问策略和开放原因"
            ));
        }
        if ("PUBLIC".equals(resource.authType()) && !isPublicPath(resource.path())) {
            violations.add(toViolation(
                    resource,
                    "PUBLIC_API_NOT_IN_RUNTIME_WHITELIST",
                    "ERROR",
                    "公开接口访问策略未同步到运行时白名单"
            ));
        }
        if ("ALL".equals(resource.method())) {
            violations.add(toViolation(
                    resource,
                    "API_METHOD_NOT_EXPLICIT",
                    "ERROR",
                    "接口必须显式声明 HTTP 方法"
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
        if ("HIGH".equals(resource.riskLevel())
                && resource.writeOperation()
                && !"PUBLIC".equals(resource.authType())
                && !resource.repeatSubmitProtected()) {
            violations.add(toViolation(
                    resource,
                    "HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT",
                    "ERROR",
                    "非公开高风险写接口缺少重复提交防护"
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
            if (!hasText(resource.sunsetVersion())) {
                violations.add(toViolation(
                        resource,
                        "DEPRECATED_API_MISSING_SUNSET_VERSION",
                        "WARN",
                        "废弃接口缺少计划下线版本"
                ));
            } else if (!API_VERSION_PATTERN.matcher(resource.sunsetVersion()).matches()) {
                violations.add(toViolation(
                        resource,
                        "DEPRECATED_API_SUNSET_VERSION_INVALID_FORMAT",
                        "WARN",
                        "废弃接口计划下线版本格式不符合规范：" + resource.sunsetVersion()
                ));
            }
            if (!hasText(resource.replacementApiKey())) {
                violations.add(toViolation(
                        resource,
                        "DEPRECATED_API_MISSING_REPLACEMENT",
                        "WARN",
                        "废弃接口缺少替代接口"
                ));
            }
        }
        if ("DEPRECATED".equals(resource.lifecycle()) && !resource.deprecated()) {
            violations.add(toViolation(
                    resource,
                    "LIFECYCLE_DEPRECATED_WITHOUT_DEPRECATED_FLAG",
                    "WARN",
                    "接口生命周期为 DEPRECATED，但 OpenAPI 未标记 deprecated"
            ));
        }
        if ("REMOVED".equals(resource.lifecycle())) {
            violations.add(toViolation(
                    resource,
                    "REMOVED_API_STILL_MAPPED",
                    "ERROR",
                    "生命周期为 REMOVED 的接口仍被运行时路由暴露"
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
        if (!hasText(resource.audience())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_API_AUDIENCE",
                    "WARN",
                    "接口缺少受众元数据"
            ));
        }
        if (!hasText(resource.sinceVersion())) {
            violations.add(toViolation(
                    resource,
                    "MISSING_API_SINCE_VERSION",
                    "WARN",
                    "接口缺少引入版本元数据"
            ));
        } else if (!API_VERSION_PATTERN.matcher(resource.sinceVersion()).matches()) {
            violations.add(toViolation(
                    resource,
                    "API_SINCE_VERSION_INVALID_FORMAT",
                    "WARN",
                    "接口引入版本格式不符合规范：" + resource.sinceVersion()
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
                message,
                remediation(ruleCode)
        );
    }

    private String remediation(String ruleCode) {
        GovernanceRule rule = GOVERNANCE_RULE_BY_CODE.get(ruleCode);
        if (rule == null) {
            return "根据 ruleCode 对应的接口治理规范补充元数据、权限配置或发布说明";
        }
        return rule.remediation();
    }

    private record GovernanceRule(
            String ruleCode,
            String severity,
            String category,
            String description,
            String remediation
    ) {
    }

    private long countSeverity(List<ApiResourceGovernanceResponse.Violation> violations, String severity) {
        return violations.stream()
                .filter(violation -> severity.equals(violation.severity()))
                .count();
    }

    private List<ApiResourceGovernanceResponse.RuleSummary> summarizeRules(
            List<ApiResourceGovernanceResponse.Violation> violations
    ) {
        return violations.stream()
                .collect(Collectors.groupingBy(ApiResourceGovernanceResponse.Violation::ruleCode, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> toRuleSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt((ApiResourceGovernanceResponse.RuleSummary summary) ->
                                severityRank(summary.severity()))
                        .thenComparing(Comparator
                                .comparingLong(ApiResourceGovernanceResponse.RuleSummary::count)
                                .reversed())
                        .thenComparing(ApiResourceGovernanceResponse.RuleSummary::ruleCode))
                .toList();
    }

    private ApiResourceGovernanceResponse.RuleSummary toRuleSummary(String ruleCode, long count) {
        GovernanceRule rule = GOVERNANCE_RULE_BY_CODE.get(ruleCode);
        String severity = rule == null ? "WARN" : rule.severity();
        String category = rule == null ? "UNKNOWN" : rule.category();
        String description = rule == null ? "未登记接口治理规则：" + ruleCode : rule.description();
        String remediation = rule == null ? remediation(ruleCode) : rule.remediation();
        return new ApiResourceGovernanceResponse.RuleSummary(
                ruleCode,
                severity,
                category,
                "ERROR".equals(severity),
                count,
                description,
                remediation
        );
    }

    private List<ApiResourceGovernanceResponse.CategorySummary> summarizeCategories(
            List<ApiResourceGovernanceResponse.Violation> violations
    ) {
        return violations.stream()
                .collect(Collectors.groupingBy(violation -> categoryOf(violation.ruleCode())))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ApiResourceGovernanceResponse.Violation> categoryViolations = entry.getValue();
                    return new ApiResourceGovernanceResponse.CategorySummary(
                            entry.getKey(),
                            categoryViolations.size(),
                            countSeverity(categoryViolations, "ERROR"),
                            countSeverity(categoryViolations, "WARN")
                    );
                })
                .sorted(Comparator
                        .comparingLong(ApiResourceGovernanceResponse.CategorySummary::errorCount)
                        .reversed()
                        .thenComparing(Comparator
                                .comparingLong(ApiResourceGovernanceResponse.CategorySummary::violationCount)
                                .reversed())
                        .thenComparing(ApiResourceGovernanceResponse.CategorySummary::category))
                .toList();
    }

    private String categoryOf(String ruleCode) {
        GovernanceRule rule = GOVERNANCE_RULE_BY_CODE.get(ruleCode);
        return rule == null ? "UNKNOWN" : rule.category();
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "ERROR" -> 0;
            case "WARN" -> 1;
            default -> 2;
        };
    }

    private boolean matches(ApiResourceQuery query, ApiResourceResponse resource) {
        return equalsIgnoreCaseIfPresent(query.getMethod(), resource.method())
                && containsIfPresent(query.getPath(), resource.path())
                && containsIfPresent(query.getModule(), resource.module())
                && permissionContainsIfPresent(query.getPermissionCode(), resource.permissionCodes())
                && containsIfPresent(query.getHandler(), resource.handler())
                && equalsIgnoreCaseIfPresent(query.getAuthType(), resource.authType())
                && containsIfPresent(query.getOwner(), resource.owner())
                && containsIfPresent(query.getAudience(), resource.audience())
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

    private ApiResourceSummaryResponse.OwnerStat toOwnerStat(String owner, List<ApiResourceResponse> resources) {
        return new ApiResourceSummaryResponse.OwnerStat(
                owner,
                resources.size(),
                resources.stream().filter(ApiResourceResponse::writeOperation).count(),
                resources.stream().filter(ApiResourceResponse::permissionMissing).count(),
                resources.stream().filter(resource -> ApiRiskLevel.HIGH.name().equals(resource.riskLevel())).count()
        );
    }

    private ApiResourceSummaryResponse.AudienceStat toAudienceStat(String audience, List<ApiResourceResponse> resources) {
        return new ApiResourceSummaryResponse.AudienceStat(
                audience,
                resources.size(),
                countAuthType(resources, ApiAuthType.PUBLIC),
                countAuthType(resources, ApiAuthType.PERMISSION),
                resources.stream().filter(ApiResourceResponse::writeOperation).count()
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
            String audience,
            String sinceVersion,
            String lifecycle,
            String riskLevel,
            String sunsetVersion,
            String replacementApiKey
    ) {
    }
}
