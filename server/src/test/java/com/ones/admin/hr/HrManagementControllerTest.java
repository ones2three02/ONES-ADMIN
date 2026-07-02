package com.ones.admin.hr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrJobGradeSaveRequest;
import com.ones.admin.hr.dto.HrPositionSaveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HrManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void manageHrPhaseOneFoundation() throws Exception {
        String token = login();
        String suffix = String.valueOf(System.nanoTime());

        long positionId = createPosition(token, "DEV-" + suffix);
        updatePosition(token, positionId, "DEVOPS-" + suffix);
        long gradeId = createJobGrade(token, "P6-" + suffix);
        updateJobGrade(token, gradeId, "P7-" + suffix);

        String idCardNumber = "110101199001011234";
        String employeeResponse = createEmployee(token, suffix, positionId, gradeId, idCardNumber);
        assertThat(employeeResponse).doesNotContain(idCardNumber);
        long employeeId = objectMapper.readTree(employeeResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data.realName").value("张三"))
                .andExpect(jsonPath("$.data.deptName").value("ONES 总部"))
                .andExpect(jsonPath("$.data.positionName").value("运维工程师"))
                .andExpect(jsonPath("$.data.gradeName").value("P7"))
                .andExpect(jsonPath("$.data.idCardMasked").value("110****1234"));

        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "E" + suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].employeeNo").value("E" + suffix));
    }

    private long createPosition(String token, String code) throws Exception {
        String body = objectMapper.writeValueAsString(new HrPositionSaveRequest(
                code,
                "研发工程师",
                1L,
                "负责产品研发",
                true
        ));
        String response = mockMvc.perform(post("/api/hr/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.positionCode").value(code))
                .andExpect(jsonPath("$.data.deptName").value("ONES 总部"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void updatePosition(String token, long positionId, String code) throws Exception {
        String body = objectMapper.writeValueAsString(new HrPositionSaveRequest(
                code,
                "运维工程师",
                1L,
                "负责平台运维",
                true
        ));
        mockMvc.perform(put("/api/hr/positions/" + positionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionName").value("运维工程师"));
    }

    private long createJobGrade(String token, String code) throws Exception {
        String body = objectMapper.writeValueAsString(new HrJobGradeSaveRequest(
                code,
                "P6",
                6,
                true
        ));
        String response = mockMvc.perform(post("/api/hr/job-grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.gradeCode").value(code))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void updateJobGrade(String token, long gradeId, String code) throws Exception {
        String body = objectMapper.writeValueAsString(new HrJobGradeSaveRequest(
                code,
                "P7",
                7,
                true
        ));
        mockMvc.perform(put("/api/hr/job-grades/" + gradeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeName").value("P7"));
    }

    private String createEmployee(
            String token,
            String suffix,
            long positionId,
            long gradeId,
            String idCardNumber
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HrEmployeeCreateRequest(
                "E" + suffix,
                "张三",
                "三三",
                "MALE",
                "13800138000",
                "zhangsan" + suffix + "@ones.local",
                idCardNumber,
                null,
                1L,
                positionId,
                gradeId,
                null,
                "FULL_TIME",
                "PROBATION",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 10, 1),
                "HRMS Phase 1A 测试员工"
        ));
        return mockMvc.perform(post("/api/hr/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data.employmentStatus").value("PROBATION"))
                .andReturn()
                .getResponse()
                .getContentAsString();
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
