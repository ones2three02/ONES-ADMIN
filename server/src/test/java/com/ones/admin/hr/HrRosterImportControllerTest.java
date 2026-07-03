package com.ones.admin.hr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.hr.dto.HrJobGradeSaveRequest;
import com.ones.admin.hr.dto.HrPositionSaveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HrRosterImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importRosterAndTrackBatchErrors() throws Exception {
        String token = login();
        String suffix = String.valueOf(System.nanoTime());
        long positionId = createPosition(token, "IMP-" + suffix);
        long gradeId = createJobGrade(token, "IP6-" + suffix);

        mockMvc.perform(get("/api/hr/roster-import/template")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("ones-hr-roster-template.csv")))
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .contains("employeeNo,realName,deptId,positionId,gradeId,employmentType,employmentStatus,hireDate"));

        String successEmployeeNo = "EI" + suffix;
        String failedEmployeeNo = "EF" + suffix;
        String csv = """
                employeeNo,realName,deptId,positionId,gradeId,employmentType,employmentStatus,hireDate,probationEndDate,mobile,email,gender,preferredName,remark
                %s,李四,1,%d,%d,FULL_TIME,PROBATION,2026-07-03,2026-10-03,13900139000,lisi%s@ones.local,MALE,四四,导入成功员工
                %s,,1,%d,%d,FULL_TIME,PROBATION,2026-07-03,2026-10-03,13900139001,fail%s@ones.local,FEMALE,,缺少姓名
                """.formatted(successEmployeeNo, positionId, gradeId, suffix, failedEmployeeNo, positionId, gradeId, suffix);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        String importResponse = mockMvc.perform(multipart("/api/hr/roster-import/batches")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.batchNo", notNullValue()))
                .andExpect(jsonPath("$.data.fileName").value("employees.csv"))
                .andExpect(jsonPath("$.data.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.completedAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode batch = objectMapper.readTree(importResponse).path("data");
        long batchId = batch.path("id").asLong();

        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", successEmployeeNo)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].employeeNo").value(successEmployeeNo))
                .andExpect(jsonPath("$.data.list[0].realName").value("李四"));

        mockMvc.perform(get("/api/hr/roster-import/batches")
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list[0].id").value(batchId))
                .andExpect(jsonPath("$.data.list[0].status").value("PARTIAL_SUCCESS"));

        mockMvc.perform(get("/api/hr/roster-import/batches/" + batchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(batchId))
                .andExpect(jsonPath("$.data.batchNo").value(batch.path("batchNo").asText()));

        mockMvc.perform(get("/api/hr/roster-import/batches/" + batchId + "/errors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].rowNumber").value(3))
                .andExpect(jsonPath("$.data[0].employeeNo").value(failedEmployeeNo))
                .andExpect(jsonPath("$.data[0].fieldName").value("realName"))
                .andExpect(jsonPath("$.data[0].errorMessage").value("员工姓名不能为空"))
                .andExpect(jsonPath("$.data[0].rawJson", containsString(failedEmployeeNo)));
    }

    private long createPosition(String token, String code) throws Exception {
        String body = objectMapper.writeValueAsString(new HrPositionSaveRequest(
                code,
                "导入测试岗位",
                1L,
                "花名册导入测试",
                true
        ));
        String response = mockMvc.perform(post("/api/hr/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private long createJobGrade(String token, String code) throws Exception {
        String body = objectMapper.writeValueAsString(new HrJobGradeSaveRequest(
                code,
                "IP6",
                6,
                true
        ));
        String response = mockMvc.perform(post("/api/hr/job-grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
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
