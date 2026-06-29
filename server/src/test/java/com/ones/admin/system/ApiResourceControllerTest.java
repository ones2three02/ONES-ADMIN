package com.ones.admin.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.system.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        assertThat(userListResource.path("module").asText()).isEqualTo("系统管理-用户");
        assertThat(userListResource.path("summary").asText()).isEqualTo("查询用户列表");
        assertThat(userListResource.path("permissionMode").asText()).isEqualTo("AND");
        assertThat(userListResource.path("authType").asText()).isEqualTo("PERMISSION");
        assertThat(userListResource.path("accessPolicyExplicit").asBoolean()).isFalse();
        assertThat(userListResource.path("requiresPermission").asBoolean()).isTrue();
        assertThat(userListResource.path("permissionMissing").asBoolean()).isFalse();
        assertThat(userListResource.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(userListResource)).containsExactly("system:user:list");

        JsonNode userCreateResource = findResource(resources, "POST", "/api/system/users");
        assertThat(userCreateResource.path("summary").asText()).isEqualTo("新增用户");
        assertThat(userCreateResource.path("writeOperation").asBoolean()).isTrue();
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode summary = objectMapper.readTree(summaryResponse).path("data");
        assertThat(summary.path("total").asLong()).isEqualTo(listTotal);
        assertThat(summary.path("writeOperationCount").asLong()).isGreaterThan(0);
        assertThat(summary.path("explicitAccessPolicyCount").asLong()).isGreaterThan(0);
        assertThat(findAuthTypeCount(summary, "PERMISSION")).isGreaterThan(0);
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode governance = objectMapper.readTree(governanceResponse).path("data");
        assertThat(governance.path("total").asLong()).isGreaterThan(0);
        assertThat(governance.path("violationCount").asLong())
                .isEqualTo(governance.path("warningCount").asLong());
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
