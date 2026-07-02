package com.ones.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.web.TraceIdFilter;
import com.ones.admin.system.dto.UserCreateRequest;
import com.ones.admin.system.entity.SystemLoginLogEntity;
import com.ones.admin.system.entity.SystemOperationLogEntity;
import com.ones.admin.system.mapper.SystemLoginLogMapper;
import com.ones.admin.system.mapper.SystemOperationLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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

    @Autowired
    private SystemLoginLogMapper loginLogMapper;

    @Autowired
    private SystemOperationLogMapper operationLogMapper;

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

    @Test
    void auditRetentionCanPreviewAndCleanupExpiredLogs() throws Exception {
        String token = login();
        LocalDateTime now = LocalDateTime.now();
        String oldLoginTrace = "old-login-" + System.nanoTime();
        String freshLoginTrace = "fresh-login-" + System.nanoTime();
        String oldOperationTrace = "old-operation-" + System.nanoTime();
        String freshOperationTrace = "fresh-operation-" + System.nanoTime();
        insertLoginLog(oldLoginTrace, now.minusDays(400));
        insertLoginLog(freshLoginTrace, now.minusDays(1));
        insertOperationLog(oldOperationTrace, now.minusDays(400));
        insertOperationLog(freshOperationTrace, now.minusDays(1));

        mockMvc.perform(get("/api/system/audit/retention")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loginLogRetentionDays").value(180))
                .andExpect(jsonPath("$.data.operationLogRetentionDays").value(180))
                .andExpect(jsonPath("$.data.expiredLoginLogCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.expiredOperationLogCount", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/system/audit/retention/cleanup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.deletedLoginLogCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.deletedOperationLogCount", greaterThanOrEqualTo(1)));

        assertThat(countLoginLogsByTraceId(oldLoginTrace)).isZero();
        assertThat(countLoginLogsByTraceId(freshLoginTrace)).isEqualTo(1);
        assertThat(countOperationLogsByTraceId(oldOperationTrace)).isZero();
        assertThat(countOperationLogsByTraceId(freshOperationTrace)).isEqualTo(1);
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

    private void insertLoginLog(String traceId, LocalDateTime createdAt) {
        SystemLoginLogEntity log = new SystemLoginLogEntity();
        log.setUsername("retention_user");
        log.setUserId(1L);
        log.setSuccess(true);
        log.setIp("127.0.0.1");
        log.setUserAgent("JUnit");
        log.setTraceId(traceId);
        log.setCreatedAt(createdAt);
        loginLogMapper.insert(log);
    }

    private void insertOperationLog(String traceId, LocalDateTime createdAt) {
        SystemOperationLogEntity log = new SystemOperationLogEntity();
        log.setUserId(1L);
        log.setMethod("POST");
        log.setPath("/api/system/audit/retention/cleanup");
        log.setModule("系统管理-审计日志");
        log.setOperation("清理过期审计日志");
        log.setPermissionCode("system:audit:retention");
        log.setSuccess(true);
        log.setResponseCode(0);
        log.setTraceId(traceId);
        log.setIp("127.0.0.1");
        log.setUserAgent("JUnit");
        log.setDurationMs(12L);
        log.setCreatedAt(createdAt);
        operationLogMapper.insert(log);
    }

    private Long countLoginLogsByTraceId(String traceId) {
        return loginLogMapper.selectCount(new LambdaQueryWrapper<SystemLoginLogEntity>()
                .eq(SystemLoginLogEntity::getTraceId, traceId));
    }

    private Long countOperationLogsByTraceId(String traceId) {
        return operationLogMapper.selectCount(new LambdaQueryWrapper<SystemOperationLogEntity>()
                .eq(SystemOperationLogEntity::getTraceId, traceId));
    }
}
