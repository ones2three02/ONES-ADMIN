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
import com.ones.admin.system.audit.OperationAuditInterceptor;
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
            + "writeOperation,repeatSubmitProtected,operationAuditProtected,deprecated,owner,audience,"
            + "sinceVersion,lifecycle,riskLevel,"
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
                    "PUBLIC_API_ACCESS_POLICY_REASON_MISSING",
                    "ERROR",
                    "SECURITY",
                    "公开接口缺少开放原因",
                    "为公开接口的 @ApiAccessPolicy 补充 reason，说明开放原因、调用方和安全补偿措施"
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
                    "WRITE_API_WITHOUT_OPERATION_AUDIT",
                    "ERROR",
                    "AUDIT",
                    "非公开写接口缺少操作审计覆盖",
                    "将写接口路径纳入 OperationAuditInterceptor 审计路径，或确认该接口不应作为非公开写接口暴露"
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
    private static final List<ApiResourceGovernanceReportResponse.ReferenceBenchmark> REFERENCE_BENCHMARKS = List.of(
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "Backstage",
                    "API_CATALOG",
                    "https://github.com/backstage/backstage",
                    "借鉴 API Catalog 的 Owner、Lifecycle、可发现性和开发者门户视图"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "Gravitee API Management",
                    "API_MANAGEMENT",
                    "https://github.com/gravitee-io/gravitee-api-management",
                    "借鉴 API 生命周期、集中发布治理和管理端/消费端边界"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "Kong",
                    "API_GATEWAY",
                    "https://github.com/Kong/kong",
                    "借鉴网关策略、插件化治理和运行时策略资产化"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "Apache APISIX",
                    "API_GATEWAY",
                    "https://github.com/apache/apisix",
                    "借鉴云原生网关、路由策略和插件化治理"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "Tyk",
                    "API_GATEWAY",
                    "https://github.com/TykTechnologies/tyk",
                    "借鉴多协议 API 网关、鉴权、限流和调用侧治理"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "Frappe HR",
                    "HRMS",
                    "https://github.com/frappe/hrms",
                    "HRMS 按员工生命周期拆模块，覆盖员工、考勤、排班、请假、绩效、薪酬"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "OrangeHRM",
                    "HRMS",
                    "https://github.com/orangehrm/orangehrm",
                    "HRMS 一期应具备员工管理、报表分析、招聘、入职、请假和时间追踪扩展边界"
            ),
            new ApiResourceGovernanceReportResponse.ReferenceBenchmark(
                    "IceHrm",
                    "HRMS",
                    "https://github.com/gamonoid/icehrm",
                    "借鉴中小企业 HRIS 的实用路线，先主数据与请假考勤，再扩薪酬报表"
            )
    );
    private static final List<QualityDimensionDefinition> QUALITY_DIMENSION_DEFINITIONS = List.of(
            new QualityDimensionDefinition(
                    "SECURITY",
                    "安全与访问策略",
                    "SECURITY",
                    "Kong / APISIX / Tyk 将认证、授权、限流和风险策略前置到 API 网关与策略层",
                    "优先修复权限、公开接口白名单、访问策略原因和重复提交防护，确保接口默认安全"
            ),
            new QualityDimensionDefinition(
                    "AUDIT",
                    "审计追踪",
                    "AUDIT",
                    "Gravitee API Management 强调 API 生命周期中的可观测、审计与策略追踪",
                    "确保非公开写接口进入 OperationAuditInterceptor 覆盖范围，并能通过 TraceId 关联操作日志"
            ),
            new QualityDimensionDefinition(
                    "CONTRACT",
                    "接口契约稳定性",
                    "CONTRACT",
                    "Spectral 与 oasdiff 分别提供 OpenAPI 规则校验和破坏性变更识别能力",
                    "补齐稳定 operationId、显式 HTTP 方法、OpenAPI 元数据和 Manifest Diff，避免客户端生成和网关路由不稳定"
            ),
            new QualityDimensionDefinition(
                    "LIFECYCLE",
                    "生命周期治理",
                    "LIFECYCLE",
                    "Gravitee API Management 按创建、发布、废弃、下线管理 API 生命周期",
                    "为废弃接口维护 sunsetVersion、replacementApiKey 和 OpenAPI deprecated 标记，避免无计划下线"
            ),
            new QualityDimensionDefinition(
                    "CATALOG",
                    "接口资产目录",
                    "CATALOG",
                    "Backstage API Catalog 强调 Owner、Lifecycle 和可发现性，支撑开发者门户和服务治理",
                    "补齐 owner、audience、sinceVersion 和 riskLevel，让接口资产可归属、可检索、可治理"
            ),
            new QualityDimensionDefinition(
                    "DOCUMENTATION",
                    "接口文档可发现性",
                    "DOCUMENTATION",
                    "Backstage Developer Portal 通过统一目录降低跨团队 API 发现和理解成本",
                    "补齐 @Tag 和 @Operation(summary)，保证 Swagger、接口目录和前端接口管理页语义一致"
            )
    );

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
            @Value("${ones.version:v0.0.93}") String applicationVersion
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
        ApiResourceSummaryResponse summary = summarize();
        ApiResourceGovernanceResponse governance = checkGovernance();
        ApiResourceGovernanceRuleResponse rules = listGovernanceRules();
        ApiResourceManifestResponse manifest = generateManifest();
        ApiResourceManifestLatestGateResponse latestGate = gateManifestWithLatestSnapshot(
                new ApiResourceManifestLatestGateRequest(false, null)
        );
        List<ApiResourceGovernanceReportResponse.QualityDimension> qualityDimensions =
                buildQualityDimensions(governance);
        int qualityScore = qualityScore(qualityDimensions);
        List<ApiResourceGovernanceReportResponse.RecommendedAction> recommendedActions =
                recommendActions(summary, governance, latestGate);
        List<ApiResourceGovernanceReportResponse.ActionItem> actionItems =
                buildActionItems(summary, governance, latestGate);
        List<ApiResourceGovernanceReportResponse.OwnerActionSummary> ownerActionSummaries =
                buildOwnerActionSummaries(actionItems);
        return new ApiResourceGovernanceReportResponse(
                applicationVersion,
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                qualityScore,
                buildReleaseReadiness(qualityScore, latestGate, actionItems),
                qualityDimensions,
                summary,
                governance,
                rules,
                manifest,
                latestGate,
                REFERENCE_BENCHMARKS,
                recommendedActions,
                ownerActionSummaries,
                actionItems
        );
    }

    private ApiResourceGovernanceReportResponse.ReleaseReadiness buildReleaseReadiness(
            int qualityScore,
            ApiResourceManifestLatestGateResponse latestGate,
            List<ApiResourceGovernanceReportResponse.ActionItem> actionItems
    ) {
        long openActionCount = actionItems.stream()
                .filter(item -> "OPEN".equals(item.status()))
                .count();
        long blockingActionCount = actionItems.stream()
                .filter(item -> item.blocking() && "OPEN".equals(item.status()))
                .count();
        if (latestGate == null || latestGate.gate() == null) {
            return new ApiResourceGovernanceReportResponse.ReleaseReadiness(
                    "UNKNOWN",
                    false,
                    "P1",
                    qualityScore,
                    false,
                    null,
                    1,
                    blockingActionCount,
                    openActionCount,
                    "RUN_MANIFEST_GATE",
                    "执行 Manifest 发布门禁",
                    "尚未生成 Manifest 发布门禁结果，无法判断接口发布就绪状态"
            );
        }

        ApiResourceManifestGateResponse gate = latestGate.gate();
        long blockingCheckCount = gate.checks() == null ? 0 : gate.checks().stream()
                .filter(check -> check.blocking() && !check.passed())
                .count();
        if (!gate.passed()) {
            ApiResourceManifestGateResponse.Check firstBlockingCheck = firstFailedBlockingCheck(gate);
            boolean reviewRequired = gate.reviewReasonRequired()
                    || "REVIEW_REASON_REQUIRED".equals(gate.status());
            return new ApiResourceGovernanceReportResponse.ReleaseReadiness(
                    reviewRequired ? "MANUAL_REVIEW_REQUIRED" : "BLOCKED",
                    false,
                    "P0",
                    qualityScore,
                    latestGate.baselineAvailable(),
                    gate.status(),
                    blockingCheckCount,
                    blockingActionCount,
                    openActionCount,
                    reviewRequired
                            ? "REVIEW_BREAKING_CHANGE"
                            : firstBlockingCheck == null ? "REVIEW_MANIFEST_GATE" : firstBlockingCheck.checkCode(),
                    reviewRequired
                            ? "补充破坏性变更人工确认"
                            : firstBlockingCheck == null ? "处理 Manifest 发布门禁阻断" : firstBlockingCheck.message(),
                    reviewRequired
                            ? "接口存在破坏性契约变更，需要记录架构负责人、影响范围和调用方迁移计划"
                            : "Manifest 发布门禁存在阻断项，必须处理失败检查后再发布"
            );
        }

        if (!latestGate.baselineAvailable()) {
            return new ApiResourceGovernanceReportResponse.ReleaseReadiness(
                    "BASELINE_REQUIRED",
                    false,
                    "P1",
                    qualityScore,
                    false,
                    gate.status(),
                    blockingCheckCount,
                    blockingActionCount,
                    openActionCount,
                    "PUBLISH_API_MANIFEST_BASELINE",
                    "发布接口 Manifest 基线快照",
                    "当前环境缺少 Manifest 基线快照，应先归档当前接口契约，后续发布才能稳定做版本差异门禁"
            );
        }

        ApiResourceGovernanceReportResponse.ActionItem nextAction = firstOpenActionItem(actionItems);
        if (qualityScore < 95 || gate.governanceWarningCount() > 0) {
            return new ApiResourceGovernanceReportResponse.ReleaseReadiness(
                    "READY_WITH_WARNINGS",
                    true,
                    nextAction == null ? "P1" : nextAction.priority(),
                    qualityScore,
                    true,
                    gate.status(),
                    blockingCheckCount,
                    blockingActionCount,
                    openActionCount,
                    nextAction == null ? "TRACK_GOVERNANCE_WARNINGS" : nextAction.actionCode(),
                    nextAction == null ? "跟踪接口治理警告" : nextAction.title(),
                    "接口发布门禁已通过，但仍有治理警告或非阻断动作需要进入迭代跟踪"
            );
        }

        return new ApiResourceGovernanceReportResponse.ReleaseReadiness(
                "READY",
                true,
                nextAction == null ? "P2" : nextAction.priority(),
                qualityScore,
                true,
                gate.status(),
                blockingCheckCount,
                blockingActionCount,
                openActionCount,
                nextAction == null ? null : nextAction.actionCode(),
                nextAction == null ? null : nextAction.title(),
                openActionCount > 0
                        ? "接口发布门禁已通过，仍有非阻断动作需要持续跟踪"
                        : "接口治理、契约门禁和基线状态均满足发布要求"
        );
    }

    private ApiResourceManifestGateResponse.Check firstFailedBlockingCheck(ApiResourceManifestGateResponse gate) {
        if (gate.checks() == null) {
            return null;
        }
        return gate.checks()
                .stream()
                .filter(check -> check.blocking() && !check.passed())
                .findFirst()
                .orElse(null);
    }

    private ApiResourceGovernanceReportResponse.ActionItem firstOpenActionItem(
            List<ApiResourceGovernanceReportResponse.ActionItem> actionItems
    ) {
        return actionItems.stream()
                .filter(item -> "OPEN".equals(item.status()))
                .sorted(Comparator
                        .comparingInt((ApiResourceGovernanceReportResponse.ActionItem item) -> priorityRank(item.priority()))
                        .thenComparing(ApiResourceGovernanceReportResponse.ActionItem::actionCode))
                .findFirst()
                .orElse(null);
    }

    private int priorityRank(String priority) {
        return switch (priority) {
            case "P0" -> 0;
            case "P1" -> 1;
            case "P2" -> 2;
            default -> 9;
        };
    }

    private List<ApiResourceGovernanceReportResponse.QualityDimension> buildQualityDimensions(
            ApiResourceGovernanceResponse governance
    ) {
        Map<String, ApiResourceGovernanceResponse.CategorySummary> summariesByCategory =
                governance.categorySummaries()
                        .stream()
                        .collect(Collectors.toMap(
                                ApiResourceGovernanceResponse.CategorySummary::category,
                                Function.identity(),
                                (first, second) -> first
                        ));
        return QUALITY_DIMENSION_DEFINITIONS.stream()
                .map(definition -> {
                    ApiResourceGovernanceResponse.CategorySummary summary =
                            summariesByCategory.get(definition.category());
                    long violationCount = summary == null ? 0 : summary.violationCount();
                    long errorCount = summary == null ? 0 : summary.errorCount();
                    long warningCount = summary == null ? 0 : summary.warningCount();
                    int score = dimensionScore(errorCount, warningCount);
                    return new ApiResourceGovernanceReportResponse.QualityDimension(
                            definition.dimensionCode(),
                            definition.title(),
                            definition.category(),
                            score,
                            violationCount,
                            errorCount,
                            warningCount,
                            errorCount == 0,
                            definition.benchmark(),
                            definition.recommendation()
                    );
                })
                .toList();
    }

    private int qualityScore(List<ApiResourceGovernanceReportResponse.QualityDimension> dimensions) {
        if (dimensions.isEmpty()) {
            return 100;
        }
        double average = dimensions.stream()
                .mapToInt(ApiResourceGovernanceReportResponse.QualityDimension::score)
                .average()
                .orElse(100);
        return (int) Math.round(average);
    }

    private int dimensionScore(long errorCount, long warningCount) {
        long penalty = errorCount * 25 + warningCount * 5;
        return (int) Math.max(0, 100 - penalty);
    }

    private List<ApiResourceGovernanceReportResponse.RecommendedAction> recommendActions(
            ApiResourceSummaryResponse summary,
            ApiResourceGovernanceResponse governance,
            ApiResourceManifestLatestGateResponse latestGate
    ) {
        List<ApiResourceGovernanceReportResponse.RecommendedAction> actions = new ArrayList<>();
        if (governance.errorCount() > 0) {
            actions.add(new ApiResourceGovernanceReportResponse.RecommendedAction(
                    "FIX_BLOCKING_GOVERNANCE_ERRORS",
                    "P0",
                    "API_GOVERNANCE",
                    "修复阻断级接口治理错误",
                    "当前接口治理存在 " + governance.errorCount() + " 个 ERROR 级问题，发布前必须逐项修复。",
                    "GET /api/system/api-resources/governance 返回 passed=true"
            ));
        }
        if (governance.warningCount() > 0) {
            actions.add(new ApiResourceGovernanceReportResponse.RecommendedAction(
                    "TRACK_GOVERNANCE_WARNINGS",
                    "P1",
                    "API_GOVERNANCE",
                    "跟踪接口治理警告",
                    "当前接口治理存在 " + governance.warningCount() + " 个 WARN 级问题，应纳入迭代看板持续收敛。",
                    "GET /api/system/api-resources/governance 的 warningCount 持续下降"
            ));
        }
        if (summary.permissionMissingCount() > 0) {
            actions.add(new ApiResourceGovernanceReportResponse.RecommendedAction(
                    "REGISTER_MISSING_PERMISSIONS",
                    "P0",
                    "PERMISSION",
                    "补齐接口权限点",
                    "当前仍有 " + summary.permissionMissingCount() + " 个接口缺少权限点或显式访问策略，需要补齐权限注册和授权树挂载。",
                    "GET /api/system/api-resources/summary 返回 permissionMissingCount=0"
            ));
        }
        if (latestGate != null && latestGate.gate() != null && !latestGate.gate().passed()) {
            actions.add(new ApiResourceGovernanceReportResponse.RecommendedAction(
                    "REVIEW_MANIFEST_GATE",
                    "P0",
                    "RELEASE",
                    "处理 Manifest 发布门禁阻断",
                    "最新快照门禁状态为 " + latestGate.gate().status() + "，需要处理治理错误或破坏性接口契约变更。",
                    "POST /api/system/api-resources/manifest/gate/latest 返回 gate.passed=true"
            ));
        }
        actions.add(new ApiResourceGovernanceReportResponse.RecommendedAction(
                "ARCHIVE_MANIFEST_SNAPSHOT",
                "P1",
                "RELEASE",
                "归档接口 Manifest 快照",
                "发布通过后将接口 Manifest 以应用版本资产落库，便于下一次发布做 Diff 与 Gate。",
                "POST /api/system/api-resources/manifest/snapshots 返回 published=true 或既有快照"
        ));
        return actions;
    }

    private List<ApiResourceGovernanceReportResponse.ActionItem> buildActionItems(
            ApiResourceSummaryResponse summary,
            ApiResourceGovernanceResponse governance,
            ApiResourceManifestLatestGateResponse latestGate
    ) {
        List<ApiResourceGovernanceReportResponse.ActionItem> items = new ArrayList<>();
        items.addAll(governance.violations().stream()
                .map(this::toGovernanceActionItem)
                .toList());
        if (latestGate != null) {
            boolean baselineAvailable = latestGate.baselineAvailable();
            items.add(new ApiResourceGovernanceReportResponse.ActionItem(
                    "PUBLISH_API_MANIFEST_BASELINE",
                    "P1",
                    "RELEASE",
                    baselineAvailable ? "确认接口 Manifest 基线快照" : "发布接口 Manifest 基线快照",
                    baselineAvailable
                            ? "当前环境已有接口 Manifest 快照，可作为后续发布门禁对比基线；发布后仍需确认快照已归档到当前版本。"
                            : "当前环境还没有可用于对比的接口 Manifest 快照，需要先归档当前接口契约作为后续发布门禁基线。",
                    "GATE",
                    baselineAvailable ? "BASELINE_SNAPSHOT_AVAILABLE" : "NO_BASELINE_SNAPSHOT",
                    "架构治理组",
                    "接口资源",
                    "POST /api/system/api-resources/manifest/snapshots",
                    false,
                    baselineAvailable ? "DONE" : "OPEN",
                    "POST /api/system/api-resources/manifest/snapshots 返回 saved=true 或既有快照"
            ));
        }
        if (latestGate != null && latestGate.gate() != null && latestGate.gate().diff() != null) {
            boolean changed = latestGate.gate().diff().changed();
            items.add(new ApiResourceGovernanceReportResponse.ActionItem(
                    "ARCHIVE_MANIFEST_DIFF",
                    "P2",
                    "RELEASE",
                    changed ? "归档接口契约变更差异" : "确认接口契约无变更",
                    changed
                            ? "当前 Manifest Gate 已识别接口契约变更，需要将 diff.changes 归档到 Jenkins 构建产物，便于版本审计和回滚排查。"
                            : "当前 Manifest Gate 未识别接口契约变更，仍应在 Jenkins 报告中记录 checksum 和门禁结果。",
                    "GATE_CHECK",
                    "MANIFEST_DIFF_ARCHIVE",
                    "架构治理组",
                    "接口资源",
                    "POST /api/system/api-resources/manifest/gate/latest",
                    false,
                    changed ? "OPEN" : "DONE",
                    changed
                            ? "Jenkins 构建产物中包含 Manifest Diff 明细和当前 checksum"
                            : "Jenkins 构建产物中包含当前 checksum 和 Manifest Gate 通过记录"
            ));
        }
        return items;
    }

    private List<ApiResourceGovernanceReportResponse.OwnerActionSummary> buildOwnerActionSummaries(
            List<ApiResourceGovernanceReportResponse.ActionItem> actionItems
    ) {
        return actionItems.stream()
                .collect(Collectors.groupingBy(item -> blankToDefault(item.owner(), "未归属")))
                .entrySet()
                .stream()
                .map(entry -> toOwnerActionSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt((ApiResourceGovernanceReportResponse.OwnerActionSummary summary) ->
                                priorityRank(summary.priority()))
                        .thenComparing(Comparator
                                .comparingLong(ApiResourceGovernanceReportResponse.OwnerActionSummary::blockingActionCount)
                                .reversed())
                        .thenComparing(Comparator
                                .comparingLong(ApiResourceGovernanceReportResponse.OwnerActionSummary::openActionCount)
                                .reversed())
                        .thenComparing(ApiResourceGovernanceReportResponse.OwnerActionSummary::owner))
                .toList();
    }

    private ApiResourceGovernanceReportResponse.OwnerActionSummary toOwnerActionSummary(
            String owner,
            List<ApiResourceGovernanceReportResponse.ActionItem> actionItems
    ) {
        long openActionCount = actionItems.stream()
                .filter(item -> "OPEN".equals(item.status()))
                .count();
        long blockingActionCount = actionItems.stream()
                .filter(item -> item.blocking() && "OPEN".equals(item.status()))
                .count();
        long p0ActionCount = countOpenPriority(actionItems, "P0");
        long p1ActionCount = countOpenPriority(actionItems, "P1");
        long p2ActionCount = countOpenPriority(actionItems, "P2");
        ApiResourceGovernanceReportResponse.ActionItem nextAction = firstOpenActionItem(actionItems);
        String priority = nextAction == null ? "P2" : nextAction.priority();
        String status = blockingActionCount > 0 ? "BLOCKED" : openActionCount > 0 ? "TRACKING" : "DONE";
        List<String> categories = distinctCategories(actionItems);
        return new ApiResourceGovernanceReportResponse.OwnerActionSummary(
                owner,
                status,
                priority,
                actionItems.size(),
                openActionCount,
                blockingActionCount,
                p0ActionCount,
                p1ActionCount,
                p2ActionCount,
                categories,
                nextAction == null ? null : nextAction.actionCode(),
                nextAction == null ? null : nextAction.title(),
                ownerActionRecommendation(owner, status, nextAction)
        );
    }

    private long countOpenPriority(
            List<ApiResourceGovernanceReportResponse.ActionItem> actionItems,
            String priority
    ) {
        return actionItems.stream()
                .filter(item -> "OPEN".equals(item.status()))
                .filter(item -> priority.equals(item.priority()))
                .count();
    }

    private List<String> distinctCategories(List<ApiResourceGovernanceReportResponse.ActionItem> actionItems) {
        List<String> openCategories = actionItems.stream()
                .filter(item -> "OPEN".equals(item.status()))
                .map(ApiResourceGovernanceReportResponse.ActionItem::category)
                .filter(this::hasText)
                .distinct()
                .sorted()
                .toList();
        if (!openCategories.isEmpty()) {
            return openCategories;
        }
        return actionItems.stream()
                .map(ApiResourceGovernanceReportResponse.ActionItem::category)
                .filter(this::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    private String ownerActionRecommendation(
            String owner,
            String status,
            ApiResourceGovernanceReportResponse.ActionItem nextAction
    ) {
        if ("BLOCKED".equals(status)) {
            return owner + " 需要优先处理阻断级接口治理动作，修复后重新执行 Manifest Gate";
        }
        if ("TRACKING".equals(status) && nextAction != null) {
            return owner + " 下一步处理：" + nextAction.title();
        }
        return owner + " 当前无未完成接口治理动作，保持版本发布归档和周期复核";
    }

    private ApiResourceGovernanceReportResponse.ActionItem toGovernanceActionItem(
            ApiResourceGovernanceResponse.Violation violation
    ) {
        boolean blocking = "ERROR".equals(violation.severity());
        return new ApiResourceGovernanceReportResponse.ActionItem(
                "FIX_" + violation.ruleCode(),
                blocking ? "P0" : "P1",
                categoryOf(violation.ruleCode()),
                violation.summary(),
                violation.message(),
                "GOVERNANCE_RULE",
                violation.ruleCode(),
                ownerForModule(violation.module()),
                violation.module(),
                violation.method() + " " + violation.path(),
                blocking,
                "OPEN",
                violation.remediation()
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
        addFieldChange(changes, "operationAuditProtected",
                String.valueOf(previous.operationAuditProtected()),
                String.valueOf(current.operationAuditProtected()));
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
                || "repeatSubmitProtected".equals(fieldName)
                || "operationAuditProtected".equals(fieldName);
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
                boolean writeOperation = isWriteOperation(method);
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
                        writeOperation,
                        repeatSubmitProtected,
                        operationAuditProtected(writeOperation, path, authType),
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
                resource.operationAuditProtected(),
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
                String.valueOf(resource.operationAuditProtected()),
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
                .append(resource.operationAuditProtected()).append(',')
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

    private boolean operationAuditProtected(boolean writeOperation, String path, ApiAuthType authType) {
        return writeOperation
                && authType != ApiAuthType.PUBLIC
                && OperationAuditInterceptor.isAuditedApiPath(path);
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
        if ("PUBLIC".equals(resource.authType())
                && resource.accessPolicyExplicit()
                && !hasText(resource.accessPolicyReason())) {
            violations.add(toViolation(
                    resource,
                    "PUBLIC_API_ACCESS_POLICY_REASON_MISSING",
                    "ERROR",
                    "公开接口必须说明开放原因、调用方和安全补偿措施"
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
        if (resource.writeOperation()
                && !"PUBLIC".equals(resource.authType())
                && !resource.operationAuditProtected()) {
            violations.add(toViolation(
                    resource,
                    "WRITE_API_WITHOUT_OPERATION_AUDIT",
                    "ERROR",
                    "非公开写接口缺少操作审计覆盖"
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

    private record QualityDimensionDefinition(
            String dimensionCode,
            String title,
            String category,
            String benchmark,
            String recommendation
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

    private String ownerForModule(String module) {
        if (!hasText(module)) {
            return "架构治理组";
        }
        if (module.startsWith("HRMS")) {
            return "人力平台组";
        }
        if (module.startsWith("系统管理-审计")) {
            return "审计与安全组";
        }
        if (module.startsWith("认证")) {
            return "认证与安全组";
        }
        if (module.startsWith("系统管理") || module.startsWith("系统健康")) {
            return "系统平台组";
        }
        return "架构治理组";
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
