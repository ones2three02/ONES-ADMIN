package com.ones.admin.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.OnesAdminApplication;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        OnesAdminApplication.class,
        ApiResourceOperationIdGovernanceTest.OperationIdGovernanceFixtureController.class
})
@AutoConfigureMockMvc
class ApiResourceOperationIdGovernanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectInvalidAndDuplicatedOperationId() throws Exception {
        String token = login("admin", "admin123");

        String response = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.operationIdPattern")
                        .value("^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode governance = objectMapper.readTree(response).path("data");
        List<JsonNode> operationViolations = StreamSupport.stream(governance.path("violations").spliterator(), false)
                .filter(violation -> violation.path("ruleCode").asText().startsWith("OPERATION_ID_"))
                .toList();
        assertThat(operationViolations).hasSize(3);
        assertThat(ruleCodes(operationViolations))
                .containsExactlyInAnyOrder(
                        "OPERATION_ID_DUPLICATED",
                        "OPERATION_ID_DUPLICATED",
                        "OPERATION_ID_INVALID_FORMAT"
                );
        assertThat(operationViolations)
                .allSatisfy(violation -> assertThat(violation.path("severity").asText()).isEqualTo("ERROR"));
        assertThat(operationViolations)
                .allSatisfy(violation -> assertThat(violation.path("remediation").asText()).isNotBlank());
        assertThat(operationViolations)
                .filteredOn(violation -> "OPERATION_ID_INVALID_FORMAT".equals(violation.path("ruleCode").asText()))
                .allSatisfy(violation -> assertThat(violation.path("remediation").asText())
                        .contains("Controller_动作"));
        assertThat(operationViolations)
                .filteredOn(violation -> "OPERATION_ID_DUPLICATED".equals(violation.path("ruleCode").asText()))
                .allSatisfy(violation -> assertThat(violation.path("remediation").asText())
                        .contains("唯一 operationId"));
        assertThat(operationViolations)
                .extracting(violation -> violation.path("operationId").asText())
                .contains("Duplicated_operationId", "bad.operation.id");
    }

    @Test
    void deprecatedApiRequiresSunsetVersionAndReplacementApiKey() throws Exception {
        String token = login("admin", "admin123");

        String resourceResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "200")
                        .param("path", "/api/test/lifecycle/deprecated-with-plan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].deprecated").value(true))
                .andExpect(jsonPath("$.data.list[0].lifecycle").value("DEPRECATED"))
                .andExpect(jsonPath("$.data.list[0].sunsetVersion").value("v9.9.9"))
                .andExpect(jsonPath("$.data.list[0].replacementApiKey")
                        .value("GET /api/test/lifecycle/replacement"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode plannedDeprecatedApi = objectMapper.readTree(resourceResponse).at("/data/list/0");
        assertThat(plannedDeprecatedApi.path("operationId").asText())
                .isEqualTo("LifecycleFixture_deprecatedWithPlan");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        assertThat(ruleCodes(violationsForPath(violations, "/api/test/lifecycle/deprecated-with-plan")))
                .contains("DEPRECATED_API")
                .doesNotContain(
                        "DEPRECATED_API_MISSING_SUNSET_VERSION",
                        "DEPRECATED_API_MISSING_REPLACEMENT"
                );
        assertThat(ruleCodes(violationsForPath(violations, "/api/test/lifecycle/deprecated-without-plan")))
                .contains(
                        "DEPRECATED_API",
                        "DEPRECATED_API_MISSING_SUNSET_VERSION",
                        "DEPRECATED_API_MISSING_REPLACEMENT"
                );

        String manifestResponse = mockMvc.perform(get("/api/system/api-resources/manifest")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode manifestResources = objectMapper.readTree(manifestResponse).at("/data/resources");
        JsonNode deprecatedManifest = findResource(manifestResources, "GET", "/api/test/lifecycle/deprecated-with-plan");
        assertThat(deprecatedManifest.path("sunsetVersion").asText()).isEqualTo("v9.9.9");
        assertThat(deprecatedManifest.path("replacementApiKey").asText())
                .isEqualTo("GET /api/test/lifecycle/replacement");
    }

    @Test
    void versionMetadataMustUseProductVersionPattern() throws Exception {
        String token = login("admin", "admin123");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.apiVersionPattern").value("^v\\d+\\.\\d+\\.\\d+$"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        JsonNode invalidSinceVersion = findViolation(
                violations,
                "/api/test/catalog/invalid-since-version",
                "API_SINCE_VERSION_INVALID_FORMAT"
        );
        assertThat(invalidSinceVersion.path("severity").asText()).isEqualTo("WARN");
        assertThat(invalidSinceVersion.path("message").asText()).contains("0.0.18");
        assertThat(invalidSinceVersion.path("remediation").asText()).contains("v1.2.3");

        JsonNode invalidSunsetVersion = findViolation(
                violations,
                "/api/test/lifecycle/deprecated-invalid-sunset-version",
                "DEPRECATED_API_SUNSET_VERSION_INVALID_FORMAT"
        );
        assertThat(invalidSunsetVersion.path("severity").asText()).isEqualTo("WARN");
        assertThat(invalidSunsetVersion.path("message").asText()).contains("9.9.9");
        assertThat(invalidSunsetVersion.path("remediation").asText()).contains("sunsetVersion");
    }

    @Test
    void apiLifecycleMustBeConsistentWithRuntimeRouteAndOpenApiMetadata() throws Exception {
        String token = login("admin", "admin123");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        JsonNode deprecatedMetadataOnlyViolation = findViolation(
                violations,
                "/api/test/lifecycle/deprecated-metadata-only",
                "LIFECYCLE_DEPRECATED_WITHOUT_DEPRECATED_FLAG"
        );
        assertThat(deprecatedMetadataOnlyViolation.path("severity").asText()).isEqualTo("WARN");
        assertThat(deprecatedMetadataOnlyViolation.path("remediation").asText()).contains("@Operation");

        JsonNode removedStillMappedViolation = findViolation(
                violations,
                "/api/test/lifecycle/removed-still-mapped",
                "REMOVED_API_STILL_MAPPED"
        );
        assertThat(removedStillMappedViolation.path("severity").asText()).isEqualTo("ERROR");
        assertThat(removedStillMappedViolation.path("remediation").asText()).contains("删除运行时路由");
    }

    @Test
    void highRiskWriteApiRequiresRepeatSubmitProtection() throws Exception {
        String token = login("admin", "admin123");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        JsonNode violation = findViolation(
                violations,
                "/api/test/security/high-risk-without-repeat-submit",
                "HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT"
        );
        assertThat(violation.path("severity").asText()).isEqualTo("ERROR");
        assertThat(violation.path("remediation").asText()).contains("@RepeatSubmit");
    }

    @Test
    void publicAccessPolicyMustMatchRuntimeWhitelist() throws Exception {
        String token = login("admin", "admin123");

        String resourceResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "200")
                        .param("path", "/api/test/security/public-without-runtime-whitelist")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].authType").value("PUBLIC"))
                .andExpect(jsonPath("$.data.list[0].accessPolicyExplicit").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode publicPolicyResource = objectMapper.readTree(resourceResponse).at("/data/list/0");
        assertThat(publicPolicyResource.path("accessPolicyReason").asText()).contains("测试公开策略");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        JsonNode violation = findViolation(
                violations,
                "/api/test/security/public-without-runtime-whitelist",
                "PUBLIC_API_NOT_IN_RUNTIME_WHITELIST"
        );
        assertThat(violation.path("severity").asText()).isEqualTo("ERROR");
        assertThat(violation.path("remediation").asText()).contains("LOGIN_EXCLUDE_PATH_PATTERNS");
    }

    @Test
    void publicAccessPolicyRequiresAuditReason() throws Exception {
        String token = login("admin", "admin123");

        String resourceResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "200")
                        .param("path", "/api/test/security/public-without-audit-reason")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].authType").value("PUBLIC"))
                .andExpect(jsonPath("$.data.list[0].accessPolicyExplicit").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode publicPolicyResource = objectMapper.readTree(resourceResponse).at("/data/list/0");
        assertThat(publicPolicyResource.path("accessPolicyReason").isNull()).isTrue();

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        JsonNode violation = findViolation(
                violations,
                "/api/test/security/public-without-audit-reason",
                "PUBLIC_API_ACCESS_POLICY_REASON_MISSING"
        );
        assertThat(violation.path("severity").asText()).isEqualTo("ERROR");
        assertThat(violation.path("remediation").asText()).contains("reason");
    }

    @Test
    void apiMethodMustBeExplicit() throws Exception {
        String token = login("admin", "admin123");

        String resourceResponse = mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "200")
                        .param("path", "/api/test/contract/all-method")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].method").value("ALL"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode ambiguousMethodResource = objectMapper.readTree(resourceResponse).at("/data/list/0");
        assertThat(ambiguousMethodResource.path("operationId").asText())
                .isEqualTo("ContractFixture_allMethod");

        String governanceResponse = mockMvc.perform(get("/api/system/api-resources/governance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode violations = objectMapper.readTree(governanceResponse).at("/data/violations");
        JsonNode violation = findViolation(
                violations,
                "/api/test/contract/all-method",
                "API_METHOD_NOT_EXPLICIT"
        );
        assertThat(violation.path("severity").asText()).isEqualTo("ERROR");
        assertThat(violation.path("remediation").asText()).contains("@GetMapping");
    }

    private List<String> ruleCodes(List<JsonNode> violations) {
        return violations.stream()
                .map(violation -> violation.path("ruleCode").asText())
                .toList();
    }

    private List<JsonNode> violationsForPath(JsonNode violations, String path) {
        return StreamSupport.stream(violations.spliterator(), false)
                .filter(violation -> path.equals(violation.path("path").asText()))
                .toList();
    }

    private JsonNode findResource(JsonNode resources, String method, String path) {
        return StreamSupport.stream(resources.spliterator(), false)
                .filter(resource -> method.equals(resource.path("method").asText())
                        && path.equals(resource.path("path").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到接口资源：" + method + " " + path));
    }

    private JsonNode findViolation(JsonNode violations, String path, String ruleCode) {
        return StreamSupport.stream(violations.spliterator(), false)
                .filter(violation -> path.equals(violation.path("path").asText())
                        && ruleCode.equals(violation.path("ruleCode").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到治理违规：" + path + " " + ruleCode));
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

    @RestController
    @Tag(name = "测试-接口治理")
    @ApiResourceMetadata(
            owner = "测试平台组",
            sinceVersion = "v0.0.18",
            lifecycle = ApiLifecycleStatus.ACTIVE,
            riskLevel = ApiRiskLevel.LOW
    )
    static class OperationIdGovernanceFixtureController {

        @GetMapping("/api/test/operation-id/one")
        @Operation(summary = "重复 operationId 测试一", operationId = "Duplicated_operationId")
        public ApiResult<String> duplicatedOne() {
            return ApiResult.ok("one");
        }

        @GetMapping("/api/test/operation-id/two")
        @Operation(summary = "重复 operationId 测试二", operationId = "Duplicated_operationId")
        public ApiResult<String> duplicatedTwo() {
            return ApiResult.ok("two");
        }

        @GetMapping("/api/test/operation-id/invalid")
        @Operation(summary = "非法 operationId 测试", operationId = "bad.operation.id")
        public ApiResult<String> invalid() {
            return ApiResult.ok("invalid");
        }

        @GetMapping("/api/test/lifecycle/replacement")
        @Operation(summary = "废弃接口替代接口", operationId = "LifecycleFixture_replacement")
        public ApiResult<String> replacement() {
            return ApiResult.ok("replacement");
        }

        @GetMapping("/api/test/lifecycle/deprecated-with-plan")
        @Operation(
                summary = "有下线计划的废弃接口",
                operationId = "LifecycleFixture_deprecatedWithPlan",
                deprecated = true
        )
        @ApiResourceMetadata(
                lifecycle = ApiLifecycleStatus.DEPRECATED,
                sunsetVersion = "v9.9.9",
                replacementApiKey = "GET /api/test/lifecycle/replacement"
        )
        public ApiResult<String> deprecatedWithPlan() {
            return ApiResult.ok("deprecated-with-plan");
        }

        @GetMapping("/api/test/lifecycle/deprecated-without-plan")
        @Operation(
                summary = "缺少下线计划的废弃接口",
                operationId = "LifecycleFixture_deprecatedWithoutPlan",
                deprecated = true
        )
        @ApiResourceMetadata(lifecycle = ApiLifecycleStatus.DEPRECATED)
        public ApiResult<String> deprecatedWithoutPlan() {
            return ApiResult.ok("deprecated-without-plan");
        }

        @GetMapping("/api/test/lifecycle/deprecated-metadata-only")
        @Operation(
                summary = "仅生命周期标记废弃但 OpenAPI 未废弃",
                operationId = "LifecycleFixture_deprecatedMetadataOnly"
        )
        @ApiResourceMetadata(
                lifecycle = ApiLifecycleStatus.DEPRECATED,
                sunsetVersion = "v9.9.9",
                replacementApiKey = "GET /api/test/lifecycle/replacement"
        )
        public ApiResult<String> deprecatedMetadataOnly() {
            return ApiResult.ok("deprecated-metadata-only");
        }

        @GetMapping("/api/test/lifecycle/deprecated-invalid-sunset-version")
        @Operation(
                summary = "废弃接口下线版本格式非法",
                operationId = "LifecycleFixture_deprecatedInvalidSunsetVersion",
                deprecated = true
        )
        @ApiResourceMetadata(
                lifecycle = ApiLifecycleStatus.DEPRECATED,
                sunsetVersion = "9.9.9",
                replacementApiKey = "GET /api/test/lifecycle/replacement"
        )
        public ApiResult<String> deprecatedInvalidSunsetVersion() {
            return ApiResult.ok("deprecated-invalid-sunset-version");
        }

        @GetMapping("/api/test/lifecycle/removed-still-mapped")
        @Operation(summary = "已移除但仍暴露的接口", operationId = "LifecycleFixture_removedStillMapped")
        @ApiResourceMetadata(lifecycle = ApiLifecycleStatus.REMOVED)
        public ApiResult<String> removedStillMapped() {
            return ApiResult.ok("removed-still-mapped");
        }

        @PostMapping("/api/test/security/high-risk-without-repeat-submit")
        @Operation(
                summary = "高风险写接口缺少重复提交防护",
                operationId = "SecurityFixture_highRiskWithoutRepeatSubmit"
        )
        @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
        public ApiResult<String> highRiskWithoutRepeatSubmit() {
            return ApiResult.ok("high-risk-without-repeat-submit");
        }

        @GetMapping("/api/test/security/public-without-runtime-whitelist")
        @Operation(
                summary = "公开策略未同步运行时白名单",
                operationId = "SecurityFixture_publicWithoutRuntimeWhitelist"
        )
        @ApiAccessPolicy(value = ApiAuthType.PUBLIC, reason = "测试公开策略缺少运行时白名单同步")
        public ApiResult<String> publicWithoutRuntimeWhitelist() {
            return ApiResult.ok("public-without-runtime-whitelist");
        }

        @GetMapping("/api/test/security/public-without-audit-reason")
        @Operation(
                summary = "公开策略缺少开放原因",
                operationId = "SecurityFixture_publicWithoutAuditReason"
        )
        @ApiAccessPolicy(ApiAuthType.PUBLIC)
        public ApiResult<String> publicWithoutAuditReason() {
            return ApiResult.ok("public-without-audit-reason");
        }

        @GetMapping("/api/test/catalog/invalid-since-version")
        @Operation(
                summary = "接口引入版本格式非法",
                operationId = "CatalogFixture_invalidSinceVersion"
        )
        @ApiResourceMetadata(sinceVersion = "0.0.18")
        public ApiResult<String> invalidSinceVersion() {
            return ApiResult.ok("invalid-since-version");
        }

        @RequestMapping("/api/test/contract/all-method")
        @Operation(summary = "未显式声明 HTTP 方法", operationId = "ContractFixture_allMethod")
        public ApiResult<String> allMethod() {
            return ApiResult.ok("all-method");
        }
    }
}
