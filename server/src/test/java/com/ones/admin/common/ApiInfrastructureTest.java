package com.ones.admin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.web.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiInfrastructureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthResponseContainsTraceId() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(TraceIdFilter.TRACE_ID_HEADER, "test-trace-123456"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, "test-trace-123456"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.traceId").value("test-trace-123456"));
    }

    @Test
    void validationErrorContainsFieldMessage() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("", ""));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(TraceIdFilter.TRACE_ID_HEADER, "validation-trace-123456")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, "validation-trace-123456"))
                .andExpect(jsonPath("$.code").value(CommonErrorCode.PARAM_ERROR.code()))
                .andExpect(jsonPath("$.message", containsString("username")))
                .andExpect(jsonPath("$.traceId").value("validation-trace-123456"));
    }

    @Test
    void openApiDocsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ONES-ADMIN 后端接口"))
                .andExpect(jsonPath("$.info.version").value("v0.0.90"));
    }
}
