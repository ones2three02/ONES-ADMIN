package com.ones.admin.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.OnesAdminApplication;
import com.ones.admin.auth.dto.LoginRequest;
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

    private List<String> ruleCodes(List<JsonNode> violations) {
        return violations.stream()
                .map(violation -> violation.path("ruleCode").asText())
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
    }
}
