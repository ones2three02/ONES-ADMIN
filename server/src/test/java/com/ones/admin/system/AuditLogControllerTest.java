package com.ones.admin.system;

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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginAttemptsAreAudited() throws Exception {
        String failedUsername = "ghost-audit";
        String failedBody = objectMapper.writeValueAsString(new LoginRequest(failedUsername, "wrong-password"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonErrorCode.BUSINESS_ERROR.code()));

        String token = login();

        mockMvc.perform(get("/api/system/audit/login-logs")
                        .param("limit", "200")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].username", hasItem(failedUsername)))
                .andExpect(jsonPath("$.data[*].success", hasItem(false)))
                .andExpect(jsonPath("$.data[*].traceId", hasItem(notNullValue())));
    }

    @Test
    void systemWriteOperationsAreAudited() throws Exception {
        String token = login();
        String username = "audit_user";
        String createBody = objectMapper.writeValueAsString(new UserCreateRequest(
                username,
                "审计测试用户",
                "operator123",
                1L,
                "用于验证操作日志",
                true,
                List.of("OPERATOR")
        ));

        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/system/audit/operation-logs")
                        .param("limit", "200")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].path", hasItem("/api/system/users")))
                .andExpect(jsonPath("$.data[*].operation", hasItem("新增用户")))
                .andExpect(jsonPath("$.data[*].permissionCode", hasItem("system:user:create")));
    }

    private String login() throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"));
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
