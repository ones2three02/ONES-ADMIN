package com.ones.admin.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.system.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listApiResourcesContainsPermissionMetadata() throws Exception {
        String token = login("admin", "admin123");

        String response = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "200")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode resources = objectMapper.readTree(response).at("/data/list");
        JsonNode userListResource = findResource(resources, "GET", "/api/system/users");
        assertThat(userListResource.path("apiKey").asText()).isEqualTo("GET /api/system/users");
        assertThat(userListResource.path("operationId").asText()).isEqualTo("UserController_listUsers");
        assertThat(userListResource.path("handler").asText())
                .isEqualTo("com.ones.admin.system.UserController#listUsers");
        assertThat(userListResource.path("deprecated").asBoolean()).isFalse();
        assertThat(userListResource.path("owner").asText()).isEqualTo("系统平台组");
        assertThat(userListResource.path("audience").asText()).isEqualTo("ADMIN_PORTAL");
        assertThat(userListResource.path("sinceVersion").asText()).isEqualTo("v0.0.1");
        assertThat(userListResource.path("lifecycle").asText()).isEqualTo("ACTIVE");
        assertThat(userListResource.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(userListResource.path("module").asText()).isEqualTo("系统管理-用户");
        assertThat(userListResource.path("summary").asText()).isEqualTo("查询用户列表");
        assertThat(userListResource.path("permissionMode").asText()).isEqualTo("AND");
        assertThat(userListResource.path("authType").asText()).isEqualTo("PERMISSION");
        assertThat(userListResource.path("permissionCodeStandard").asBoolean()).isTrue();
        assertThat(userListResource.path("invalidPermissionCodes").isEmpty()).isTrue();
        assertThat(userListResource.path("accessPolicyExplicit").asBoolean()).isFalse();
        assertThat(userListResource.path("requiresPermission").asBoolean()).isTrue();
        assertThat(userListResource.path("permissionRegistered").asBoolean()).isTrue();
        assertThat(userListResource.path("unregisteredPermissionCodes").isEmpty()).isTrue();
        assertThat(userListResource.path("permissionAssignable").asBoolean()).isTrue();
        assertThat(userListResource.path("unassignablePermissionCodes").isEmpty()).isTrue();
        assertThat(userListResource.path("permissionMissing").asBoolean()).isFalse();
        assertThat(userListResource.path("writeOperation").asBoolean()).isFalse();
        assertThat(userListResource.path("repeatSubmitProtected").asBoolean()).isFalse();
        assertThat(permissionCodes(userListResource)).containsExactly("system:user:list");

        JsonNode userCreateResource = findResource(resources, "POST", "/api/system/users");
        assertThat(userCreateResource.path("operationId").asText()).isEqualTo("UserController_createUser");
        assertThat(userCreateResource.path("summary").asText()).isEqualTo("新增用户");
        assertThat(userCreateResource.path("writeOperation").asBoolean()).isTrue();
        assertThat(userCreateResource.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(userCreateResource.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(userCreateResource.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(permissionCodes(userCreateResource)).containsExactly("system:user:create");

        JsonNode refreshTokenResource = findResource(resources, "POST", "/api/auth/refresh");
        assertThat(refreshTokenResource.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(refreshTokenResource.path("operationAuditProtected").asBoolean()).isTrue();
        JsonNode logoutResource = findResource(resources, "POST", "/api/auth/logout");
        assertThat(logoutResource.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(logoutResource.path("operationAuditProtected").asBoolean()).isTrue();
        JsonNode timezoneResource = findResource(resources, "POST", "/api/timezone/setTimezone");
        assertThat(timezoneResource.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(timezoneResource.path("operationAuditProtected").asBoolean()).isTrue();

        JsonNode fileMetadataResource = findResource(resources, "GET", "/api/system/files/{id:\\d+}/metadata");
        assertThat(fileMetadataResource.path("operationId").asText()).isEqualTo("FileController_getMetadata");
        assertThat(fileMetadataResource.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(fileMetadataResource.path("accessPolicyExplicit").asBoolean()).isTrue();
        assertThat(fileMetadataResource.path("accessPolicyReason").asText()).contains("上传人");
        assertThat(fileMetadataResource.path("permissionMissing").asBoolean()).isFalse();
        assertThat(permissionCodes(fileMetadataResource)).isEmpty();
    }

    @Test
    void filterApiResourcesByAuthTypeAndPermissionMissing() throws Exception {
        String token = login("admin", "admin123");

        String publicResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("authType", "PUBLIC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].path").value("/api/auth/login"))
                .andExpect(jsonPath("$.data.list[0].authType").value("PUBLIC"))
                .andExpect(jsonPath("$.data.list[0].permissionMissing").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode publicResources = objectMapper.readTree(publicResponse).at("/data/list");
        JsonNode loginResource = findResource(publicResources, "POST", "/api/auth/login");
        assertThat(loginResource.path("accessPolicyExplicit").asBoolean()).isTrue();
        assertThat(loginResource.path("accessPolicyReason").asText()).contains("登录");
        JsonNode healthResource = findResource(publicResources, "GET", "/api/health");
        assertThat(healthResource.path("accessPolicyExplicit").asBoolean()).isTrue();
        assertThat(healthResource.path("accessPolicyReason").asText()).contains("健康检查");

        String loginOnlyResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("authType", "LOGIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginOnlyResources = objectMapper.readTree(loginOnlyResponse).at("/data/list");
        JsonNode currentMenuResource = findResource(loginOnlyResources, "GET", "/api/system/menus");
        assertThat(currentMenuResource.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(currentMenuResource.path("accessPolicyExplicit").asBoolean()).isTrue();
        assertThat(currentMenuResource.path("permissionMissing").asBoolean()).isFalse();
        assertThat(currentMenuResource.path("accessPolicyReason").asText()).contains("当前用户菜单");

        mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("permissionMissing", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.empty").value(true));

        mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("handler", "UserController#listUsers")
                        .param("deprecated", "false")
                        .param("owner", "系统平台")
                        .param("audience", "ADMIN_PORTAL")
                        .param("lifecycle", "ACTIVE")
                        .param("riskLevel", "MEDIUM")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].apiKey").value("GET /api/system/users"))
                .andExpect(jsonPath("$.data.list[0].deprecated").value(false));

        mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("handler", "UserController#listUsers")
                        .param("audience", "ARCHITECTURE_GOVERNANCE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void filterApiResourcesByPermissionCode() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("permissionCode", "system:user:create")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].method").value("POST"))
                .andExpect(jsonPath("$.data.list[0].path").value("/api/system/users"));

        mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("permissionCode", "system:file:upload")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].method").value("POST"))
                .andExpect(jsonPath("$.data.list[0].path").value("/api/system/files/upload"));
    }

    @Test
    void summarizeApiResources() throws Exception {
        String token = login("admin", "admin123");

        String listResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "200")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long listTotal = objectMapper.readTree(listResponse).at("/data/total").asLong();

        String summaryResponse = mockMvc.perform(get("/api/system/api-resources/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permissionMissingCount").value(0))
                .andExpect(jsonPath("$.data.deprecatedCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode summary = objectMapper.readTree(summaryResponse).path("data");
        assertThat(summary.path("total").asLong()).isEqualTo(listTotal);
        assertThat(summary.path("writeOperationCount").asLong()).isGreaterThan(0);
        assertThat(summary.path("explicitAccessPolicyCount").asLong()).isGreaterThan(0);
        assertThat(findAuthTypeCount(summary, "PERMISSION")).isGreaterThan(0);
        assertThat(findLifecycleCount(summary, "ACTIVE")).isEqualTo(listTotal);
        assertThat(findRiskLevelCount(summary, "HIGH")).isGreaterThan(0);
        assertThat(findModule(summary, "系统管理-接口资源").path("permissionCount").asLong()).isGreaterThan(0);
    }

    @Test
    void checkApiResourceGovernance() throws Exception {
        String token = login("admin", "admin123");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.errorCount").value(0))
                .andExpect(jsonPath("$.data.warningCount").value(0))
                .andExpect(jsonPath("$.data.permissionCodePattern")
                        .value("^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$"))
                .andExpect(jsonPath("$.data.operationIdPattern")
                        .value("^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$"))
                .andExpect(jsonPath("$.data.apiVersionPattern").value("^v\\d+\\.\\d+\\.\\d+$"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode governance = objectMapper.readTree(governanceResponse).path("data");
        assertThat(governance.path("total").asLong()).isGreaterThan(0);
        assertThat(governance.path("violationCount").asLong())
                .isEqualTo(governance.path("warningCount").asLong());
        assertThat(governance.path("ruleSummaries").isArray()).isTrue();
        assertThat(governance.path("categorySummaries").isArray()).isTrue();
        assertThat(ruleCodes(governance.path("violations")))
                .doesNotContain(
                        "PUBLIC_API_WITHOUT_ACCESS_POLICY",
                        "OPERATION_ID_INVALID_FORMAT",
                        "OPERATION_ID_DUPLICATED"
                );
    }

    @Test
    void listApiResourceGovernanceRules() throws Exception {
        String token = login("admin", "admin123");

        String rulesResponse = mockMvc.perform(get("/api/system/api-resources/governance/rules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permissionCodePattern")
                        .value("^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$"))
                .andExpect(jsonPath("$.data.operationIdPattern")
                        .value("^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$"))
                .andExpect(jsonPath("$.data.apiVersionPattern").value("^v\\d+\\.\\d+\\.\\d+$"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode rules = objectMapper.readTree(rulesResponse).at("/data/rules");
        assertThat(rules).hasSizeGreaterThanOrEqualTo(15);
        JsonNode permissionMissingRule = findRule(rules, "API_PERMISSION_MISSING");
        assertThat(permissionMissingRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(permissionMissingRule.path("blocking").asBoolean()).isTrue();
        assertThat(permissionMissingRule.path("category").asText()).isEqualTo("SECURITY");
        assertThat(permissionMissingRule.path("remediation").asText()).contains("@SaCheckPermission");

        JsonNode publicAccessPolicyRule = findRule(rules, "PUBLIC_API_WITHOUT_ACCESS_POLICY");
        assertThat(publicAccessPolicyRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(publicAccessPolicyRule.path("blocking").asBoolean()).isTrue();
        assertThat(publicAccessPolicyRule.path("category").asText()).isEqualTo("SECURITY");
        assertThat(publicAccessPolicyRule.path("remediation").asText()).contains("@ApiAccessPolicy");

        JsonNode publicReasonRule = findRule(rules, "PUBLIC_API_ACCESS_POLICY_REASON_MISSING");
        assertThat(publicReasonRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(publicReasonRule.path("blocking").asBoolean()).isTrue();
        assertThat(publicReasonRule.path("category").asText()).isEqualTo("SECURITY");
        assertThat(publicReasonRule.path("remediation").asText()).contains("reason");

        JsonNode publicRuntimeWhitelistRule = findRule(rules, "PUBLIC_API_NOT_IN_RUNTIME_WHITELIST");
        assertThat(publicRuntimeWhitelistRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(publicRuntimeWhitelistRule.path("blocking").asBoolean()).isTrue();
        assertThat(publicRuntimeWhitelistRule.path("category").asText()).isEqualTo("SECURITY");
        assertThat(publicRuntimeWhitelistRule.path("remediation").asText()).contains("LOGIN_EXCLUDE_PATH_PATTERNS");

        JsonNode explicitMethodRule = findRule(rules, "API_METHOD_NOT_EXPLICIT");
        assertThat(explicitMethodRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(explicitMethodRule.path("blocking").asBoolean()).isTrue();
        assertThat(explicitMethodRule.path("category").asText()).isEqualTo("CONTRACT");
        assertThat(explicitMethodRule.path("remediation").asText()).contains("@GetMapping");

        JsonNode deprecatedRule = findRule(rules, "DEPRECATED_API");
        assertThat(deprecatedRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(deprecatedRule.path("blocking").asBoolean()).isFalse();
        assertThat(deprecatedRule.path("category").asText()).isEqualTo("LIFECYCLE");
        JsonNode missingSunsetRule = findRule(rules, "DEPRECATED_API_MISSING_SUNSET_VERSION");
        assertThat(missingSunsetRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(missingSunsetRule.path("remediation").asText()).contains("sunsetVersion");
        JsonNode missingReplacementRule = findRule(rules, "DEPRECATED_API_MISSING_REPLACEMENT");
        assertThat(missingReplacementRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(missingReplacementRule.path("remediation").asText()).contains("replacementApiKey");
        JsonNode deprecatedLifecycleRule = findRule(rules, "LIFECYCLE_DEPRECATED_WITHOUT_DEPRECATED_FLAG");
        assertThat(deprecatedLifecycleRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(deprecatedLifecycleRule.path("remediation").asText()).contains("@Operation");
        JsonNode removedStillMappedRule = findRule(rules, "REMOVED_API_STILL_MAPPED");
        assertThat(removedStillMappedRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(removedStillMappedRule.path("blocking").asBoolean()).isTrue();
        assertThat(removedStillMappedRule.path("remediation").asText()).contains("删除运行时路由");
        JsonNode invalidSinceVersionRule = findRule(rules, "API_SINCE_VERSION_INVALID_FORMAT");
        assertThat(invalidSinceVersionRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(invalidSinceVersionRule.path("category").asText()).isEqualTo("CATALOG");
        assertThat(invalidSinceVersionRule.path("remediation").asText()).contains("apiVersionPattern");
        JsonNode missingAudienceRule = findRule(rules, "MISSING_API_AUDIENCE");
        assertThat(missingAudienceRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(missingAudienceRule.path("category").asText()).isEqualTo("CATALOG");
        assertThat(missingAudienceRule.path("remediation").asText()).contains("audience");
        JsonNode invalidSunsetVersionRule = findRule(rules, "DEPRECATED_API_SUNSET_VERSION_INVALID_FORMAT");
        assertThat(invalidSunsetVersionRule.path("severity").asText()).isEqualTo("WARN");
        assertThat(invalidSunsetVersionRule.path("category").asText()).isEqualTo("LIFECYCLE");
        assertThat(invalidSunsetVersionRule.path("remediation").asText()).contains("sunsetVersion");
        JsonNode repeatSubmitRule = findRule(rules, "HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT");
        assertThat(repeatSubmitRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(repeatSubmitRule.path("blocking").asBoolean()).isTrue();
        assertThat(repeatSubmitRule.path("category").asText()).isEqualTo("SECURITY");
        assertThat(repeatSubmitRule.path("remediation").asText()).contains("@RepeatSubmit");
        JsonNode operationAuditRule = findRule(rules, "WRITE_API_WITHOUT_OPERATION_AUDIT");
        assertThat(operationAuditRule.path("severity").asText()).isEqualTo("ERROR");
        assertThat(operationAuditRule.path("blocking").asBoolean()).isTrue();
        assertThat(operationAuditRule.path("category").asText()).isEqualTo("AUDIT");
        assertThat(operationAuditRule.path("remediation").asText()).contains("OperationAuditInterceptor");
    }

    @Test
    void generateApiResourceGovernanceReportForCiAndFrontend() throws Exception {
        String token = login("admin", "admin123");

        String response = mockMvc.perform(get("/api/system/api-resources/governance/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationVersion").value("v0.0.120"))
                .andExpect(jsonPath("$.data.summary.total").value(102))
                .andExpect(jsonPath("$.data.governance.passed").value(true))
                .andExpect(jsonPath("$.data.governance.total").value(102))
                .andExpect(jsonPath("$.data.manifest.total").value(102))
                .andExpect(jsonPath("$.data.latestGate.gate.currentVersion").value("v0.0.120"))
                .andExpect(jsonPath("$.data.latestGate.gate.checks[0].checkCode").value("API_GOVERNANCE_ERROR"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode report = objectMapper.readTree(response).path("data");
        assertThat(report.path("generatedAt").asText()).isNotBlank();
        assertThat(report.path("qualityScore").asInt()).isEqualTo(100);
        JsonNode releaseReadiness = report.path("releaseReadiness");
        assertThat(List.of(
                "BASELINE_REQUIRED",
                "BLOCKED",
                "MANUAL_REVIEW_REQUIRED",
                "READY",
                "READY_WITH_WARNINGS",
                "UNKNOWN"
        )).contains(releaseReadiness.path("status").asText());
        assertThat(releaseReadiness.path("ready").isBoolean()).isTrue();
        assertThat(List.of("P0", "P1", "P2")).contains(releaseReadiness.path("priority").asText());
        assertThat(releaseReadiness.path("qualityScore").asInt()).isEqualTo(100);
        assertThat(releaseReadiness.path("baselineAvailable").isBoolean()).isTrue();
        assertThat(releaseReadiness.path("gateStatus").asText()).isNotBlank();
        assertThat(releaseReadiness.path("blockingCheckCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(releaseReadiness.path("blockingActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(releaseReadiness.path("openActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(releaseReadiness.path("message").asText()).isNotBlank();
        assertThat(report.path("qualityDimensions").isArray()).isTrue();
        assertThat(report.path("qualityDimensions").size()).isGreaterThanOrEqualTo(6);
        JsonNode securityDimension = findQualityDimension(report.path("qualityDimensions"), "SECURITY");
        assertThat(securityDimension.path("score").asInt()).isEqualTo(100);
        assertThat(securityDimension.path("passed").asBoolean()).isTrue();
        assertThat(securityDimension.path("benchmark").asText()).contains("Kong");
        JsonNode auditDimension = findQualityDimension(report.path("qualityDimensions"), "AUDIT");
        assertThat(auditDimension.path("score").asInt()).isEqualTo(100);
        assertThat(auditDimension.path("benchmark").asText()).contains("Gravitee");
        JsonNode contractDimension = findQualityDimension(report.path("qualityDimensions"), "CONTRACT");
        assertThat(contractDimension.path("score").asInt()).isEqualTo(100);
        assertThat(contractDimension.path("benchmark").asText()).contains("Spectral");
        assertThat(report.at("/rules/rules").size()).isGreaterThanOrEqualTo(15);
        assertThat(report.path("manifest").path("checksum").asText()).matches("^[a-f0-9]{64}$");
        JsonNode systemOwner = findOwnerStat(report.path("summary").path("owners"), "系统平台组");
        assertThat(systemOwner.path("total").asLong()).isGreaterThanOrEqualTo(20);
        assertThat(systemOwner.path("writeOperationCount").asLong()).isGreaterThanOrEqualTo(1);
        JsonNode adminPortalAudience = findAudienceStat(report.path("summary").path("audiences"), "ADMIN_PORTAL");
        assertThat(adminPortalAudience.path("total").asLong()).isGreaterThanOrEqualTo(20);
        assertThat(adminPortalAudience.path("publicCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(report.at("/governance/ruleSummaries").isArray()).isTrue();
        assertThat(report.at("/governance/categorySummaries").isArray()).isTrue();
        JsonNode reportResource = findResource(
                report.path("manifest").path("resources"),
                "GET",
                "/api/system/api-resources/governance/report"
        );
        assertThat(reportResource.path("operationId").asText())
                .isEqualTo("ApiResourceController_generateApiResourceGovernanceReport");
        assertThat(permissionCodes(reportResource)).containsExactly("system:api:list");
        assertThat(report.path("referenceBenchmarks").isArray()).isTrue();
        assertThat(report.path("referenceBenchmarks").size()).isGreaterThanOrEqualTo(6);
        JsonNode backstage = findReferenceBenchmark(report.path("referenceBenchmarks"), "Backstage");
        assertThat(backstage.path("category").asText()).isEqualTo("API_CATALOG");
        assertThat(backstage.path("url").asText()).isEqualTo("https://github.com/backstage/backstage");
        JsonNode frappeHr = findReferenceBenchmark(report.path("referenceBenchmarks"), "Frappe HR");
        assertThat(frappeHr.path("category").asText()).isEqualTo("HRMS");
        assertThat(frappeHr.path("url").asText()).isEqualTo("https://github.com/frappe/hrms");
        assertThat(report.path("recommendedActions").isArray()).isTrue();
        assertThat(report.path("recommendedActions").size()).isGreaterThanOrEqualTo(1);
        assertThat(actionCodes(report.path("recommendedActions"))).doesNotContain("DESIGN_HRMS_PHASE_ONE");
        assertThat(actionCodes(report.path("recommendedActions"))).doesNotContain("IMPLEMENT_HRMS_ROSTER_FRONTEND");
        JsonNode archiveAction = findRecommendedAction(
                report.path("recommendedActions"),
                "ARCHIVE_MANIFEST_SNAPSHOT"
        );
        assertThat(archiveAction.path("category").asText()).isEqualTo("RELEASE");
        assertThat(report.path("ownerActionSummaries").isArray()).isTrue();
        assertThat(report.path("ownerActionSummaries").size()).isGreaterThanOrEqualTo(1);
        JsonNode governanceOwnerSummary = findOwnerActionSummary(report.path("ownerActionSummaries"), "架构治理组");
        assertThat(List.of("BLOCKED", "DONE", "TRACKING"))
                .contains(governanceOwnerSummary.path("status").asText());
        assertThat(List.of("P0", "P1", "P2"))
                .contains(governanceOwnerSummary.path("priority").asText());
        assertThat(governanceOwnerSummary.path("totalActionCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(governanceOwnerSummary.path("openActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(governanceOwnerSummary.path("blockingActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(governanceOwnerSummary.path("p0ActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(governanceOwnerSummary.path("p1ActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(governanceOwnerSummary.path("p2ActionCount").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(governanceOwnerSummary.path("categories").isArray()).isTrue();
        assertThat(governanceOwnerSummary.path("recommendation").asText()).contains("架构治理组");
        assertThat(report.path("actionItems").isArray()).isTrue();
        assertThat(actionCodes(report.path("actionItems"))).doesNotContain("IMPLEMENT_HRMS_ROSTER_FRONTEND");
        JsonNode baselineAction = findActionItem(report.path("actionItems"), "PUBLISH_API_MANIFEST_BASELINE");
        assertThat(baselineAction.path("priority").asText()).isEqualTo("P1");
        assertThat(baselineAction.path("sourceType").asText()).isEqualTo("GATE");
        assertThat(List.of("NO_BASELINE_SNAPSHOT", "BASELINE_SNAPSHOT_AVAILABLE"))
                .contains(baselineAction.path("sourceCode").asText());
        assertThat(baselineAction.path("owner").asText()).isEqualTo("架构治理组");
        assertThat(baselineAction.path("blocking").asBoolean()).isFalse();
        assertThat(List.of("OPEN", "DONE")).contains(baselineAction.path("status").asText());
        JsonNode diffArchiveAction = findActionItem(report.path("actionItems"), "ARCHIVE_MANIFEST_DIFF");
        assertThat(diffArchiveAction.path("sourceType").asText()).isEqualTo("GATE_CHECK");
        assertThat(diffArchiveAction.path("sourceCode").asText()).isEqualTo("MANIFEST_DIFF_ARCHIVE");
        assertThat(diffArchiveAction.path("category").asText()).isEqualTo("RELEASE");
    }

    @Test
    void exportApiResourcesAsCsv() throws Exception {
        String token = login("admin", "admin123");

        String response = mockMvc.perform(get("/api/system/api-resources/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("ones-api-resources.csv")))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).startsWith(
                "apiKey,operationId,method,path,handler,module,summary,authType,permissionCodes,permissionMode,"
                        + "requiresPermission,permissionRegistered,permissionAssignable,permissionMissing,"
                        + "writeOperation,repeatSubmitProtected,operationAuditProtected,deprecated,owner,audience,"
                        + "sinceVersion,lifecycle,riskLevel,"
                        + "sunsetVersion,replacementApiKey,"
                        + "accessPolicyExplicit,accessPolicyReason\n"
        );
        assertThat(response).contains(
                "GET /api/system/users,UserController_listUsers,GET,/api/system/users,"
                        + "com.ones.admin.system.UserController#listUsers"
        );
        assertThat(response).contains("system:user:list");
        assertThat(response).contains(
                "GET /api/system/api-resources/export,ApiResourceController_exportApiResources,"
                        + "GET,/api/system/api-resources/export,"
                        + "com.ones.admin.system.ApiResourceController#exportApiResources"
        );
    }

    @Test
    void generateApiResourceManifest() throws Exception {
        String token = login("admin", "admin123");

        String firstResponse = mockMvc.perform(get("/api/system/api-resources/manifest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationVersion").value("v0.0.120"))
                .andExpect(jsonPath("$.data.checksumAlgorithm").value("SHA-256"))
                .andExpect(jsonPath("$.data.total").value(102))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(get("/api/system/api-resources/manifest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstManifest = objectMapper.readTree(firstResponse).path("data");
        JsonNode secondManifest = objectMapper.readTree(secondResponse).path("data");
        assertThat(firstManifest.path("checksum").asText()).matches("^[a-f0-9]{64}$");
        assertThat(secondManifest.path("checksum").asText()).isEqualTo(firstManifest.path("checksum").asText());
        assertThat(firstManifest.path("resources").size()).isEqualTo(firstManifest.path("total").asInt());

        JsonNode userList = findResource(firstManifest.path("resources"), "GET", "/api/system/users");
        assertThat(userList.path("apiKey").asText()).isEqualTo("GET /api/system/users");
        assertThat(userList.path("operationId").asText()).isEqualTo("UserController_listUsers");
        assertThat(userList.path("handler").asText()).isEqualTo("com.ones.admin.system.UserController#listUsers");
        assertThat(userList.path("authType").asText()).isEqualTo("PERMISSION");
        assertThat(userList.path("owner").asText()).isEqualTo("系统平台组");
        assertThat(userList.path("audience").asText()).isEqualTo("ADMIN_PORTAL");
        assertThat(userList.path("sinceVersion").asText()).isEqualTo("v0.0.1");
        assertThat(userList.path("lifecycle").asText()).isEqualTo("ACTIVE");
        assertThat(userList.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(userList.path("repeatSubmitProtected").asBoolean()).isFalse();
        assertThat(userList.path("operationAuditProtected").asBoolean()).isFalse();
        assertThat(permissionCodes(userList)).containsExactly("system:user:list");

        JsonNode userCreate = findResource(firstManifest.path("resources"), "POST", "/api/system/users");
        assertThat(userCreate.path("writeOperation").asBoolean()).isTrue();
        assertThat(userCreate.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(userCreate.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(userCreate.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(userCreate)).containsExactly("system:user:create");

        JsonNode refreshToken = findResource(firstManifest.path("resources"), "POST", "/api/auth/refresh");
        assertThat(refreshToken.path("operationAuditProtected").asBoolean()).isTrue();
        JsonNode logout = findResource(firstManifest.path("resources"), "POST", "/api/auth/logout");
        assertThat(logout.path("operationAuditProtected").asBoolean()).isTrue();
        JsonNode timezone = findResource(firstManifest.path("resources"), "POST", "/api/timezone/setTimezone");
        assertThat(timezone.path("operationAuditProtected").asBoolean()).isTrue();

        JsonNode fileMetadata = findResource(firstManifest.path("resources"),
                "GET", "/api/system/files/{id:\\d+}/metadata");
        assertThat(fileMetadata.path("operationId").asText()).isEqualTo("FileController_getMetadata");
        assertThat(fileMetadata.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(fileMetadata.path("sinceVersion").asText()).isEqualTo("v0.0.85");
        assertThat(fileMetadata.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(permissionCodes(fileMetadata)).isEmpty();

        JsonNode fileDownload = findResource(firstManifest.path("resources"),
                "GET", "/api/system/files/{filename:.+}");
        assertThat(fileDownload.path("operationId").asText()).isEqualTo("FileController_download");
        assertThat(fileDownload.path("authType").asText()).isEqualTo("LOGIN");
        assertThat(fileDownload.path("sinceVersion").asText()).isEqualTo("v0.0.85");

        JsonNode fileRetention = findResource(firstManifest.path("resources"),
                "GET", "/api/system/files/retention");
        assertThat(fileRetention.path("operationId").asText()).isEqualTo("FileController_getRetention");
        assertThat(fileRetention.path("sinceVersion").asText()).isEqualTo("v0.0.86");
        assertThat(fileRetention.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(fileRetention.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(fileRetention)).containsExactly("system:file:purge");

        JsonNode filePurge = findResource(firstManifest.path("resources"),
                "POST", "/api/system/files/retention/purge");
        assertThat(filePurge.path("operationId").asText()).isEqualTo("FileController_purgeExpiredDeletedFiles");
        assertThat(filePurge.path("sinceVersion").asText()).isEqualTo("v0.0.86");
        assertThat(filePurge.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(filePurge.path("writeOperation").asBoolean()).isTrue();
        assertThat(filePurge.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(filePurge.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(filePurge)).containsExactly("system:file:purge");

        JsonNode employeeExport = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/employees/export");
        assertThat(employeeExport.path("operationId").asText()).isEqualTo("HrEmployeeController_exportEmployees");
        assertThat(employeeExport.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(employeeExport.path("sinceVersion").asText()).isEqualTo("v0.0.69");
        assertThat(employeeExport.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(employeeExport.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(employeeExport)).containsExactly("hr:employee:export");

        JsonNode employeeJobs = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/employees/{id}/jobs");
        assertThat(employeeJobs.path("operationId").asText()).isEqualTo("HrEmployeeController_listEmployeeJobs");
        assertThat(employeeJobs.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(employeeJobs.path("sinceVersion").asText()).isEqualTo("v0.0.88");
        assertThat(employeeJobs.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(employeeJobs.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(employeeJobs)).containsExactly("hr:employee:detail");

        JsonNode employeeOrgContext = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/employees/{id}/org-context");
        assertThat(employeeOrgContext.path("operationId").asText())
                .isEqualTo("HrEmployeeController_getEmployeeOrgContext");
        assertThat(employeeOrgContext.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(employeeOrgContext.path("sinceVersion").asText()).isEqualTo("v0.0.89");
        assertThat(employeeOrgContext.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(employeeOrgContext.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(employeeOrgContext)).containsExactly("hr:employee:detail");

        JsonNode employeeDocuments = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/employees/{id}/documents");
        assertThat(employeeDocuments.path("operationId").asText())
                .isEqualTo("HrEmployeeController_listEmployeeDocuments");
        assertThat(employeeDocuments.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(employeeDocuments.path("sinceVersion").asText()).isEqualTo("v0.0.91");
        assertThat(employeeDocuments.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(employeeDocuments.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(employeeDocuments)).containsExactly("hr:employee:detail");

        JsonNode expiringEmployeeDocuments = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/employees/documents/expiring");
        assertThat(expiringEmployeeDocuments.path("operationId").asText())
                .isEqualTo("HrEmployeeController_listExpiringEmployeeDocuments");
        assertThat(expiringEmployeeDocuments.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(expiringEmployeeDocuments.path("sinceVersion").asText()).isEqualTo("v0.0.92");
        assertThat(expiringEmployeeDocuments.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(expiringEmployeeDocuments.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(expiringEmployeeDocuments)).containsExactly("hr:employee:detail");

        JsonNode bindEmployeeDocument = findResource(firstManifest.path("resources"),
                "POST", "/api/hr/employees/{id}/documents/{fileId}");
        assertThat(bindEmployeeDocument.path("operationId").asText())
                .isEqualTo("HrEmployeeController_bindEmployeeDocument");
        assertThat(bindEmployeeDocument.path("sinceVersion").asText()).isEqualTo("v0.0.91");
        assertThat(bindEmployeeDocument.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(bindEmployeeDocument.path("writeOperation").asBoolean()).isTrue();
        assertThat(bindEmployeeDocument.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(bindEmployeeDocument.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(bindEmployeeDocument)).containsExactly("hr:employee:update");

        JsonNode removeEmployeeDocument = findResource(firstManifest.path("resources"),
                "DELETE", "/api/hr/employees/{id}/documents/{fileId}");
        assertThat(removeEmployeeDocument.path("operationId").asText())
                .isEqualTo("HrEmployeeController_removeEmployeeDocument");
        assertThat(removeEmployeeDocument.path("sinceVersion").asText()).isEqualTo("v0.0.90");
        assertThat(removeEmployeeDocument.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(removeEmployeeDocument.path("writeOperation").asBoolean()).isTrue();
        assertThat(removeEmployeeDocument.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(removeEmployeeDocument.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(removeEmployeeDocument)).containsExactly("hr:employee:update");

        JsonNode hrOverview = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/overview");
        assertThat(hrOverview.path("operationId").asText()).isEqualTo("HrOverviewController_getOverview");
        assertThat(hrOverview.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(hrOverview.path("sinceVersion").asText()).isEqualTo("v0.0.70");
        assertThat(hrOverview.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(hrOverview.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(hrOverview)).containsExactly("hr:overview:view");

        JsonNode contractList = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/employees/{employeeId}/contracts");
        assertThat(contractList.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(contractList.path("sinceVersion").asText()).isEqualTo("v0.0.55");
        assertThat(contractList.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(permissionCodes(contractList)).containsExactly("hr:contract:list");

        JsonNode contractCreate = findResource(firstManifest.path("resources"),
                "POST", "/api/hr/employees/{employeeId}/contracts");
        assertThat(contractCreate.path("writeOperation").asBoolean()).isTrue();
        assertThat(contractCreate.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(contractCreate.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(contractCreate.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(contractCreate)).containsExactly("hr:contract:create");

        JsonNode contractUpdate = findResource(firstManifest.path("resources"),
                "PUT", "/api/hr/employees/{employeeId}/contracts/{contractId}");
        assertThat(contractUpdate.path("writeOperation").asBoolean()).isTrue();
        assertThat(contractUpdate.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(contractUpdate.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(contractUpdate.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(contractUpdate)).containsExactly("hr:contract:update");

        JsonNode expiringContracts = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/contracts/expiring");
        assertThat(expiringContracts.path("sinceVersion").asText()).isEqualTo("v0.0.56");
        assertThat(expiringContracts.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(expiringContracts.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(expiringContracts)).containsExactly("hr:contract:list");

        JsonNode expiringContractExport = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/contracts/expiring/export");
        assertThat(expiringContractExport.path("operationId").asText())
                .isEqualTo("HrEmployeeContractController_exportExpiringContracts");
        assertThat(expiringContractExport.path("sinceVersion").asText()).isEqualTo("v0.0.99");
        assertThat(expiringContractExport.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(expiringContractExport.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(expiringContractExport)).containsExactly("hr:contract:list");

        JsonNode contractAttachmentMetadata = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/contracts/{contractId}/attachment/metadata");
        assertThat(contractAttachmentMetadata.path("operationId").asText())
                .isEqualTo("HrEmployeeContractController_getAttachmentMetadata");
        assertThat(contractAttachmentMetadata.path("sinceVersion").asText()).isEqualTo("v0.0.84");
        assertThat(contractAttachmentMetadata.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(contractAttachmentMetadata.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(contractAttachmentMetadata)).containsExactly("hr:contract:list");

        JsonNode terminateContract = findResource(firstManifest.path("resources"),
                "POST", "/api/hr/contracts/{contractId}/terminate");
        assertThat(terminateContract.path("sinceVersion").asText()).isEqualTo("v0.0.56");
        assertThat(terminateContract.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(terminateContract.path("writeOperation").asBoolean()).isTrue();
        assertThat(terminateContract.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(terminateContract)).containsExactly("hr:contract:terminate");

        JsonNode rosterTemplate = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/roster-import/template");
        assertThat(rosterTemplate.path("owner").asText()).isEqualTo("人力平台组");
        assertThat(rosterTemplate.path("sinceVersion").asText()).isEqualTo("v0.0.58");
        assertThat(rosterTemplate.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(rosterTemplate.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(rosterTemplate)).containsExactly("hr:roster:import");

        JsonNode rosterImport = findResource(firstManifest.path("resources"),
                "POST", "/api/hr/roster-import/batches");
        assertThat(rosterImport.path("sinceVersion").asText()).isEqualTo("v0.0.58");
        assertThat(rosterImport.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(rosterImport.path("writeOperation").asBoolean()).isTrue();
        assertThat(rosterImport.path("repeatSubmitProtected").asBoolean()).isTrue();
        assertThat(rosterImport.path("operationAuditProtected").asBoolean()).isTrue();
        assertThat(permissionCodes(rosterImport)).containsExactly("hr:roster:import");

        JsonNode rosterBatches = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/roster-import/batches");
        assertThat(rosterBatches.path("sinceVersion").asText()).isEqualTo("v0.0.58");
        assertThat(rosterBatches.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(permissionCodes(rosterBatches)).containsExactly("hr:roster:list");

        JsonNode rosterBatchDetail = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/roster-import/batches/{id}");
        assertThat(rosterBatchDetail.path("sinceVersion").asText()).isEqualTo("v0.0.58");
        assertThat(permissionCodes(rosterBatchDetail)).containsExactly("hr:roster:list");

        JsonNode rosterErrors = findResource(firstManifest.path("resources"),
                "GET", "/api/hr/roster-import/batches/{id}/errors");
        assertThat(rosterErrors.path("sinceVersion").asText()).isEqualTo("v0.0.58");
        assertThat(permissionCodes(rosterErrors)).containsExactly("hr:roster:list");

        JsonNode manifest = findResource(firstManifest.path("resources"), "GET", "/api/system/api-resources/manifest");
        assertThat(manifest.path("handler").asText())
                .isEqualTo("com.ones.admin.system.ApiResourceController#generateApiResourceManifest");
        assertThat(permissionCodes(manifest)).containsExactly("system:api:list");
        assertThat(manifest.path("writeOperation").asBoolean()).isFalse();
    }

    @Test
    void publishAndListApiResourceManifestSnapshots() throws Exception {
        String token = login("admin", "admin123");
        ObjectNode publishRequest = publishRequest();

        String publishResponse = mockMvc.perform(post("/api/system/api-resources/manifest/snapshots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.gate.passed").value(true))
                .andExpect(jsonPath("$.data.gate.status").value("PASSED_WITH_CHANGES"))
                .andExpect(jsonPath("$.data.snapshot.applicationVersion").value("v0.0.120"))
                .andExpect(jsonPath("$.data.snapshot.checksumAlgorithm").value("SHA-256"))
                .andExpect(jsonPath("$.data.snapshot.total").value(102))
                .andExpect(jsonPath("$.data.snapshot.manifest.total").value(102))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode snapshot = objectMapper.readTree(publishResponse).at("/data/snapshot");
        long snapshotId = snapshot.path("id").asLong();
        String checksum = snapshot.path("checksum").asText();
        assertThat(snapshotId).isPositive();
        assertThat(checksum).matches("^[a-f0-9]{64}$");

        mockMvc.perform(get("/api/system/api-resources/manifest/snapshots")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(snapshotId))
                .andExpect(jsonPath("$.data.list[0].checksum").value(checksum))
                .andExpect(jsonPath("$.data.list[0].manifest").doesNotExist());

        mockMvc.perform(get("/api/system/api-resources/manifest/snapshots/latest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(snapshotId))
                .andExpect(jsonPath("$.data.checksum").value(checksum))
                .andExpect(jsonPath("$.data.manifest.checksum").value(checksum));

        Thread.sleep(600);
        mockMvc.perform(post("/api/system/api-resources/manifest/snapshots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.snapshot.id").value(snapshotId))
                .andExpect(jsonPath("$.data.snapshot.checksum").value(checksum));
    }

    @Test
    void gateLatestApiResourceManifestSnapshotUsesPublishedBaseline() throws Exception {
        String token = login("admin", "admin123");
        ObjectNode request = publishRequest();

        String publishResponse = mockMvc.perform(post("/api/system/api-resources/manifest/snapshots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode snapshot = objectMapper.readTree(publishResponse).at("/data/snapshot");
        long snapshotId = snapshot.path("id").asLong();
        String checksum = snapshot.path("checksum").asText();

        mockMvc.perform(post("/api/system/api-resources/manifest/gate/latest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.baselineAvailable").value(true))
                .andExpect(jsonPath("$.data.baselineSnapshotId").value(snapshotId))
                .andExpect(jsonPath("$.data.baselineChecksum").value(checksum))
                .andExpect(jsonPath("$.data.gate.passed").value(true))
                .andExpect(jsonPath("$.data.gate.status").value("PASSED"))
                .andExpect(jsonPath("$.data.gate.diff.changed").value(false))
                .andExpect(jsonPath("$.data.gate.diff.previousChecksum").value(checksum))
                .andExpect(jsonPath("$.data.gate.diff.currentChecksum").value(checksum));
    }

    @Test
    void diffApiResourceManifest() throws Exception {
        String token = login("admin", "admin123");
        String currentResponse = mockMvc.perform(get("/api/system/api-resources/manifest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ObjectNode previousManifest = previousManifestWithBreakingChanges(currentResponse);

        String diffResponse = mockMvc.perform(post("/api/system/api-resources/manifest/diff")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previousManifest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.previousVersion").value("v0.0.14"))
                .andExpect(jsonPath("$.data.currentVersion").value("v0.0.120"))
                .andExpect(jsonPath("$.data.addedCount").value(1))
                .andExpect(jsonPath("$.data.removedCount").value(1))
                .andExpect(jsonPath("$.data.modifiedCount").value(1))
                .andExpect(jsonPath("$.data.breakingChangeCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode changes = objectMapper.readTree(diffResponse).at("/data/changes");
        JsonNode added = findChange(changes, "ADDED", "GET /api/system/api-resources/manifest");
        assertThat(added.path("severity").asText()).isEqualTo("INFO");
        assertThat(added.path("breakingChange").asBoolean()).isFalse();
        JsonNode removed = findChange(changes, "REMOVED", "GET /api/legacy/removed");
        assertThat(removed.path("severity").asText()).isEqualTo("ERROR");
        assertThat(removed.path("breakingChange").asBoolean()).isTrue();
        JsonNode modified = findChange(changes, "MODIFIED", "GET /api/system/users");
        assertThat(modified.path("severity").asText()).isEqualTo("ERROR");
        assertThat(modified.path("breakingChange").asBoolean()).isTrue();
        assertThat(fieldNames(modified)).containsExactly("operationId", "authType", "permissionCodes");
    }

    @Test
    void gateApiResourceManifestRelease() throws Exception {
        String token = login("admin", "admin123");
        String currentResponse = mockMvc.perform(get("/api/system/api-resources/manifest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ObjectNode previousManifest = previousManifestWithBreakingChanges(currentResponse);
        ObjectNode blockedRequest = objectMapper.createObjectNode();
        blockedRequest.set("previousManifest", previousManifest);
        blockedRequest.put("allowBreakingChanges", false);

        String blockedResponse = mockMvc.perform(post("/api/system/api-resources/manifest/gate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.requiredManualReview").value(true))
                .andExpect(jsonPath("$.data.reviewReasonRequired").value(true))
                .andExpect(jsonPath("$.data.breakingChangeCount").value(2))
                .andExpect(jsonPath("$.data.diff.breakingChangeCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode blockedChecks = objectMapper.readTree(blockedResponse).at("/data/checks");
        JsonNode blockedGovernanceCheck = findGateCheck(blockedChecks, "API_GOVERNANCE_ERROR");
        assertThat(blockedGovernanceCheck.path("passed").asBoolean()).isTrue();
        assertThat(blockedGovernanceCheck.path("blocking").asBoolean()).isTrue();
        JsonNode blockedBreakingCheck = findGateCheck(blockedChecks, "BREAKING_CHANGE_REVIEW");
        assertThat(blockedBreakingCheck.path("severity").asText()).isEqualTo("ERROR");
        assertThat(blockedBreakingCheck.path("passed").asBoolean()).isFalse();
        assertThat(blockedBreakingCheck.path("blocking").asBoolean()).isTrue();
        assertThat(blockedBreakingCheck.path("remediation").asText()).contains("allowBreakingChanges");

        ObjectNode missingReasonRequest = blockedRequest.deepCopy();
        missingReasonRequest.put("allowBreakingChanges", true);
        mockMvc.perform(post("/api/system/api-resources/manifest/gate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingReasonRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.status").value("REVIEW_REASON_REQUIRED"))
                .andExpect(jsonPath("$.data.requiredManualReview").value(true))
                .andExpect(jsonPath("$.data.reviewReasonRequired").value(true));

        ObjectNode approvedRequest = missingReasonRequest.deepCopy();
        approvedRequest.put("reviewReason", "已由架构负责人确认本次破坏性接口变更");
        String approvedResponse = mockMvc.perform(post("/api/system/api-resources/manifest/gate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.status").value("MANUAL_APPROVED"))
                .andExpect(jsonPath("$.data.reviewReasonRequired").value(false))
                .andExpect(jsonPath("$.data.reasons[0]").value("存在 2 个破坏性接口契约变更，已记录人工确认原因"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode approvedChecks = objectMapper.readTree(approvedResponse).at("/data/checks");
        JsonNode approvedBreakingCheck = findGateCheck(approvedChecks, "BREAKING_CHANGE_REVIEW");
        assertThat(approvedBreakingCheck.path("passed").asBoolean()).isTrue();
        assertThat(approvedBreakingCheck.path("blocking").asBoolean()).isTrue();
        assertThat(approvedBreakingCheck.path("message").asText()).contains("已记录人工确认原因");
    }

    @Test
    void rejectUserWithoutApiResourcePermission() throws Exception {
        String adminToken = login("admin", "admin123");
        String username = "api_operator_" + System.nanoTime();
        String createBody = objectMapper.writeValueAsString(new UserCreateRequest(
                username,
                "接口权限测试用户",
                "operator123",
                1L,
                "用于验证接口资源权限",
                true,
                List.of("OPERATOR")
        ));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String operatorToken = login(username, "operator123");
        mockMvc.perform(get("/api/system/api-resources")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/governance/rules")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/governance/report")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/export")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/manifest")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(post("/api/system/api-resources/manifest/diff")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(post("/api/system/api-resources/manifest/gate")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(post("/api/system/api-resources/manifest/gate/latest")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(post("/api/system/api-resources/manifest/snapshots")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/manifest/snapshots")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/system/api-resources/manifest/snapshots/latest")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.FORBIDDEN.code()));
    }

    private ObjectNode previousManifestWithBreakingChanges(String currentResponse) throws Exception {
        ObjectNode previousManifest = (ObjectNode) objectMapper.readTree(currentResponse).path("data");
        previousManifest.put("applicationVersion", "v0.0.14");
        previousManifest.put("checksum", "previous-checksum");
        ArrayNode previousResources = objectMapper.createArrayNode();
        for (JsonNode resource : previousManifest.path("resources")) {
            ObjectNode resourceNode = resource.deepCopy();
            if ("GET".equals(resourceNode.path("method").asText())
                    && "/api/system/api-resources/manifest".equals(resourceNode.path("path").asText())) {
                continue;
            }
            if ("GET".equals(resourceNode.path("method").asText())
                    && "/api/system/users".equals(resourceNode.path("path").asText())) {
                resourceNode.put("operationId", "LegacyUserController_listUsers");
                resourceNode.put("authType", "LOGIN");
                resourceNode.set("permissionCodes", objectMapper.createArrayNode());
            }
            previousResources.add(resourceNode);
        }
        previousResources.addObject()
                .put("apiKey", "GET /api/legacy/removed")
                .put("operationId", "LegacyController_removed")
                .put("method", "GET")
                .put("path", "/api/legacy/removed")
                .put("handler", "com.ones.admin.LegacyController#removed")
                .put("authType", "LOGIN")
                .putNull("permissionMode")
                .put("writeOperation", false)
                .put("owner", "legacy")
                .put("sinceVersion", "v0.0.1")
                .put("lifecycle", "ACTIVE")
                .put("riskLevel", "LOW")
                .put("deprecated", false)
                .set("permissionCodes", objectMapper.createArrayNode());
        previousManifest.set("resources", previousResources);
        previousManifest.put("total", previousResources.size());
        return previousManifest;
    }

    private ObjectNode publishRequest() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("reviewReason", "接口 Manifest 发布测试-" + System.nanoTime());
        return request;
    }

    private JsonNode findResource(JsonNode resources, String method, String path) {
        return StreamSupport.stream(resources.spliterator(), false)
                .filter(resource -> method.equals(resource.path("method").asText())
                        && path.equals(resource.path("path").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到接口资源：" + method + " " + path));
    }

    private List<String> permissionCodes(JsonNode resource) {
        return StreamSupport.stream(resource.path("permissionCodes").spliterator(), false)
                .map(JsonNode::asText)
                .toList();
    }

    private JsonNode findChange(JsonNode changes, String changeType, String apiKey) {
        return StreamSupport.stream(changes.spliterator(), false)
                .filter(change -> changeType.equals(change.path("changeType").asText())
                        && apiKey.equals(change.path("apiKey").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到接口变更：" + changeType + " " + apiKey));
    }

    private JsonNode findRule(JsonNode rules, String ruleCode) {
        return StreamSupport.stream(rules.spliterator(), false)
                .filter(rule -> ruleCode.equals(rule.path("ruleCode").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到接口治理规则：" + ruleCode));
    }

    private JsonNode findGateCheck(JsonNode checks, String checkCode) {
        return StreamSupport.stream(checks.spliterator(), false)
                .filter(check -> checkCode.equals(check.path("checkCode").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到 Manifest Gate 检查项：" + checkCode));
    }

    private List<String> fieldNames(JsonNode change) {
        return StreamSupport.stream(change.path("changedFields").spliterator(), false)
                .map(field -> field.path("fieldName").asText())
                .toList();
    }

    private List<String> ruleCodes(JsonNode violations) {
        return StreamSupport.stream(violations.spliterator(), false)
                .map(violation -> violation.path("ruleCode").asText())
                .toList();
    }

    private long findAuthTypeCount(JsonNode summary, String authType) {
        return StreamSupport.stream(summary.path("authTypes").spliterator(), false)
                .filter(node -> authType.equals(node.path("authType").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到认证级别：" + authType))
                .path("count")
                .asLong();
    }

    private JsonNode findModule(JsonNode summary, String module) {
        return StreamSupport.stream(summary.path("modules").spliterator(), false)
                .filter(node -> module.equals(node.path("module").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到模块：" + module));
    }

    private JsonNode findOwnerStat(JsonNode owners, String owner) {
        return StreamSupport.stream(owners.spliterator(), false)
                .filter(node -> owner.equals(node.path("owner").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到接口负责人：" + owner));
    }

    private JsonNode findAudienceStat(JsonNode audiences, String audience) {
        return StreamSupport.stream(audiences.spliterator(), false)
                .filter(node -> audience.equals(node.path("audience").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到接口调用方：" + audience));
    }

    private JsonNode findReferenceBenchmark(JsonNode benchmarks, String project) {
        return StreamSupport.stream(benchmarks.spliterator(), false)
                .filter(node -> project.equals(node.path("project").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到参考基准：" + project));
    }

    private JsonNode findRecommendedAction(JsonNode actions, String actionCode) {
        return StreamSupport.stream(actions.spliterator(), false)
                .filter(node -> actionCode.equals(node.path("actionCode").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到推荐动作：" + actionCode));
    }

    private JsonNode findActionItem(JsonNode actions, String actionCode) {
        return StreamSupport.stream(actions.spliterator(), false)
                .filter(node -> actionCode.equals(node.path("actionCode").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到治理动作项：" + actionCode));
    }

    private JsonNode findOwnerActionSummary(JsonNode summaries, String owner) {
        return StreamSupport.stream(summaries.spliterator(), false)
                .filter(node -> owner.equals(node.path("owner").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到负责人治理摘要：" + owner));
    }

    private JsonNode findQualityDimension(JsonNode dimensions, String dimensionCode) {
        return StreamSupport.stream(dimensions.spliterator(), false)
                .filter(node -> dimensionCode.equals(node.path("dimensionCode").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到治理评分维度：" + dimensionCode));
    }

    private List<String> actionCodes(JsonNode actions) {
        return StreamSupport.stream(actions.spliterator(), false)
                .map(node -> node.path("actionCode").asText())
                .toList();
    }

    private long findLifecycleCount(JsonNode summary, String lifecycle) {
        return StreamSupport.stream(summary.path("lifecycles").spliterator(), false)
                .filter(node -> lifecycle.equals(node.path("lifecycle").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到生命周期：" + lifecycle))
                .path("count")
                .asLong();
    }

    private long findRiskLevelCount(JsonNode summary, String riskLevel) {
        return StreamSupport.stream(summary.path("riskLevels").spliterator(), false)
                .filter(node -> riskLevel.equals(node.path("riskLevel").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到风险级别：" + riskLevel))
                .path("count")
                .asLong();
    }

    private String login(String username, String password) throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest(username, password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/token/tokenValue").asText();
    }
}
