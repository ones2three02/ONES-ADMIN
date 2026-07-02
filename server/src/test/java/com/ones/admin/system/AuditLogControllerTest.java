package com.ones.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.web.TraceIdFilter;
import com.ones.admin.system.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        String failedUsername = "=ghost-audit";
        String failedBody = objectMapper.writeValueAsString(new LoginRequest(failedUsername, "wrong-password"));
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(failedBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.INVALID_CREDENTIALS.code()));

        String token = login();

        mockMvc.perform(get("/api/system/audit/login-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("username", failedUsername)
                        .param("success", "false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[*].username", hasItem(failedUsername)))
                .andExpect(jsonPath("$.data.list[*].success", hasItem(false)))
                .andExpect(jsonPath("$.data.list[*].traceId", hasItem(notNullValue())));

        mockMvc.perform(get("/api/system/audit/login-logs/export")
                        .param("username", failedUsername)
                        .param("success", "false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("ones-login-logs.csv")))
                .andExpect(content().string(containsString("id,username,userId,success")))
                .andExpect(content().string(containsString("'=ghost-audit")));
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

        String createTraceId = mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getHeader(TraceIdFilter.TRACE_ID_HEADER);

        mockMvc.perform(get("/api/system/audit/operation-logs")
                        .param("pageNum", "1")
                        .param("pageSize", "20")
                        .param("path", "/api/system/users")
                        .param("success", "true")
                        .param("traceId", createTraceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[*].path", hasItem("/api/system/users")))
                .andExpect(jsonPath("$.data.list[*].operation", hasItem("新增用户")))
                .andExpect(jsonPath("$.data.list[*].permissionCode", hasItem("system:user:create")));

        mockMvc.perform(get("/api/system/audit/operation-logs/export")
                        .param("path", "/api/system/users")
                        .param("success", "true")
                        .param("traceId", createTraceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("ones-operation-logs.csv")))
                .andExpect(content().string(containsString("id,userId,method,path,module,operation")))
                .andExpect(content().string(containsString("system:user:create")))
                .andExpect(content().string(containsString(createTraceId)));
    }

    @Test
    void rejectInvalidAuditPageSize() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/system/audit/login-logs")
                        .param("pageSize", "201")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("pageSize 每页数量不能超过 200"));
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
