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
                        .param("pageSize", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(100))
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
        assertThat(permissionCodes(userListResource)).containsExactly("system:user:list");

        JsonNode userCreateResource = findResource(resources, "POST", "/api/system/users");
        assertThat(userCreateResource.path("operationId").asText()).isEqualTo("UserController_createUser");
        assertThat(userCreateResource.path("summary").asText()).isEqualTo("新增用户");
        assertThat(userCreateResource.path("writeOperation").asBoolean()).isTrue();
        assertThat(userCreateResource.path("riskLevel").asText()).isEqualTo("HIGH");
        assertThat(permissionCodes(userCreateResource)).containsExactly("system:user:create");
    }

    @Test
    void filterApiResourcesByAuthTypeAndPermissionMissing() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/system/api-resources")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("authType", "PUBLIC")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].path").value("/api/auth/login"))
                .andExpect(jsonPath("$.data.list[0].authType").value("PUBLIC"))
                .andExpect(jsonPath("$.data.list[0].permissionMissing").value(false));

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
                        .param("lifecycle", "ACTIVE")
                        .param("riskLevel", "MEDIUM")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].apiKey").value("GET /api/system/users"))
                .andExpect(jsonPath("$.data.list[0].deprecated").value(false));
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode governance = objectMapper.readTree(governanceResponse).path("data");
        assertThat(governance.path("total").asLong()).isGreaterThan(0);
        assertThat(governance.path("violationCount").asLong())
                .isEqualTo(governance.path("warningCount").asLong());
        assertThat(ruleCodes(governance.path("violations")))
                .doesNotContain("OPERATION_ID_INVALID_FORMAT", "OPERATION_ID_DUPLICATED");
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
                        + "writeOperation,deprecated,owner,sinceVersion,lifecycle,riskLevel,"
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
                .andExpect(jsonPath("$.data.applicationVersion").value("v0.0.22"))
                .andExpect(jsonPath("$.data.checksumAlgorithm").value("SHA-256"))
                .andExpect(jsonPath("$.data.total").value(47))
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
        assertThat(userList.path("sinceVersion").asText()).isEqualTo("v0.0.1");
        assertThat(userList.path("lifecycle").asText()).isEqualTo("ACTIVE");
        assertThat(userList.path("riskLevel").asText()).isEqualTo("MEDIUM");
        assertThat(permissionCodes(userList)).containsExactly("system:user:list");

        JsonNode manifest = findResource(firstManifest.path("resources"), "GET", "/api/system/api-resources/manifest");
        assertThat(manifest.path("handler").asText())
                .isEqualTo("com.ones.admin.system.ApiResourceController#generateApiResourceManifest");
        assertThat(permissionCodes(manifest)).containsExactly("system:api:list");
        assertThat(manifest.path("writeOperation").asBoolean()).isFalse();
    }

    @Test
    void publishAndListApiResourceManifestSnapshots() throws Exception {
        String token = login("admin", "admin123");
        ObjectNode publishRequest = objectMapper.createObjectNode();

        String publishResponse = mockMvc.perform(post("/api/system/api-resources/manifest/snapshots")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.saved").value(true))
                .andExpect(jsonPath("$.data.gate.passed").value(true))
                .andExpect(jsonPath("$.data.gate.status").value("PASSED_WITH_CHANGES"))
                .andExpect(jsonPath("$.data.snapshot.applicationVersion").value("v0.0.22"))
                .andExpect(jsonPath("$.data.snapshot.checksumAlgorithm").value("SHA-256"))
                .andExpect(jsonPath("$.data.snapshot.total").value(47))
                .andExpect(jsonPath("$.data.snapshot.manifest.total").value(47))
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
        ObjectNode request = objectMapper.createObjectNode();

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
                .andExpect(jsonPath("$.data.currentVersion").value("v0.0.22"))
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

        mockMvc.perform(post("/api/system/api-resources/manifest/gate")
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
                .andExpect(jsonPath("$.data.diff.breakingChangeCount").value(2));

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
        mockMvc.perform(post("/api/system/api-resources/manifest/gate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.status").value("MANUAL_APPROVED"))
                .andExpect(jsonPath("$.data.reviewReasonRequired").value(false))
                .andExpect(jsonPath("$.data.reasons[0]").value("存在 2 个破坏性接口契约变更，已记录人工确认原因"));
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
