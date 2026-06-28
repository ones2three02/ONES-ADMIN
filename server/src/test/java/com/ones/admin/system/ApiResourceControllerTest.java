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
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode resources = objectMapper.readTree(response).path("data");
        JsonNode userListResource = findResource(resources, "GET", "/api/system/users");
        assertThat(userListResource.path("module").asText()).isEqualTo("系统管理-用户");
        assertThat(userListResource.path("summary").asText()).isEqualTo("查询用户列表");
        assertThat(userListResource.path("permissionMode").asText()).isEqualTo("AND");
        assertThat(userListResource.path("requiresPermission").asBoolean()).isTrue();
        assertThat(userListResource.path("writeOperation").asBoolean()).isFalse();
        assertThat(permissionCodes(userListResource)).containsExactly("system:user:list");

        JsonNode userCreateResource = findResource(resources, "POST", "/api/system/users");
        assertThat(userCreateResource.path("summary").asText()).isEqualTo("新增用户");
        assertThat(userCreateResource.path("writeOperation").asBoolean()).isTrue();
        assertThat(permissionCodes(userCreateResource)).containsExactly("system:user:create");
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
