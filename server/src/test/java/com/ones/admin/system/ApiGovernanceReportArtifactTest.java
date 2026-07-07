package com.ones.admin.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiGovernanceReportArtifactTest {

    private static final Path REPORT_ARTIFACT =
            Path.of("..", ".ci-artifacts", "api-governance-report.json").normalize();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void writeApiGovernanceReportArtifactForCi() throws Exception {
        String token = login();

        String response = mockMvc.perform(get("/api/system/api-resources/governance/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.applicationVersion").value("v0.0.137"))
                .andExpect(jsonPath("$.data.governance.passed").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode report = objectMapper.readTree(response).path("data");
        assertThat(report.path("qualityScore").asInt()).isEqualTo(100);
        assertThat(report.path("manifest").path("checksum").asText()).matches("^[a-f0-9]{64}$");
        assertThat(report.path("releaseReadiness").path("status").asText()).isNotBlank();

        Files.createDirectories(REPORT_ARTIFACT.getParent());
        String formattedReport = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        Files.writeString(REPORT_ARTIFACT, formattedReport + System.lineSeparator(), StandardCharsets.UTF_8);

        assertThat(Files.exists(REPORT_ARTIFACT)).isTrue();
        assertThat(Files.size(REPORT_ARTIFACT)).isGreaterThan(0);
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
