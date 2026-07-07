package com.ones.admin.hr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrEmployeeRegularizeRequest;
import com.ones.admin.hr.dto.HrEmployeeResignRequest;
import com.ones.admin.hr.dto.HrEmployeeTransferRequest;
import com.ones.admin.hr.dto.HrJobGradeSaveRequest;
import com.ones.admin.hr.dto.HrPositionSaveRequest;
import com.ones.admin.hr.entity.HrEmployeeDocumentEntity;
import com.ones.admin.hr.mapper.HrEmployeeDocumentMapper;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HrManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemFileMapper fileMapper;

    @Autowired
    private HrEmployeeDocumentMapper documentMapper;

    @Test
    void manageHrPhaseOneFoundation() throws Exception {
        String token = login();
        String suffix = String.valueOf(System.nanoTime());

        long positionId = createPosition(token, "DEV-" + suffix);
        updatePosition(token, positionId, "DEVOPS-" + suffix);
        long transferPositionId = createPosition(token, "ARCH-" + suffix, "架构师");
        long gradeId = createJobGrade(token, "P6-" + suffix);
        updateJobGrade(token, gradeId, "P7-" + suffix);
        long transferGradeId = createJobGrade(token, "P8-" + suffix, "P8", 8);

        String idCardNumber = "110101199001011234";
        String employeeResponse = createEmployee(token, suffix, positionId, gradeId, idCardNumber);
        assertThat(employeeResponse).doesNotContain(idCardNumber);
        long employeeId = objectMapper.readTree(employeeResponse).at("/data/id").asLong();
        String managerResponse = createEmployee(token, "MGR" + suffix, positionId, gradeId, "110101198801011234");
        long managerId = objectMapper.readTree(managerResponse).at("/data/id").asLong();
        String reportResponse = createEmployee(
                token,
                "RPT" + suffix,
                positionId,
                gradeId,
                "110101199501011234",
                employeeId
        );
        long reportEmployeeId = objectMapper.readTree(reportResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data.realName").value("张三"))
                .andExpect(jsonPath("$.data.deptName").value("ONES 总部"))
                .andExpect(jsonPath("$.data.positionName").value("运维工程师"))
                .andExpect(jsonPath("$.data.gradeName").value("P7"))
                .andExpect(jsonPath("$.data.idCardMasked").value("110****1234"));

        updateEmployeeProfile(token, employeeId, suffix, idCardNumber);
        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data.realName").value("张三丰"))
                .andExpect(jsonPath("$.data.preferredName").value("三丰"))
                .andExpect(jsonPath("$.data.mobile").value("13900139000"))
                .andExpect(jsonPath("$.data.email").value("zhangsanfeng" + suffix + "@ones.local"))
                .andExpect(jsonPath("$.data.idCardMasked").value("110****5678"))
                .andExpect(jsonPath("$.data.positionName").value("运维工程师"))
                .andExpect(jsonPath("$.data.gradeName").value("P7"))
                .andExpect(jsonPath("$.data.remark").value("基础档案信息更新"));

        updateEmployeeProfileWithoutIdCard(token, employeeId, suffix);
        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idCardMasked").value("110****5678"));

        long employeeDocumentFileId = createFileMetadata("employee-document-" + suffix + ".pdf");
        LocalDate activeDocumentExpireDate = LocalDate.now().plusDays(60);
        bindEmployeeDocument(token, employeeId, employeeDocumentFileId, activeDocumentExpireDate);
        assertEmployeeDocumentBound(employeeDocumentFileId, employeeId);
        LocalDate employeeDocumentExpireDate = LocalDate.now().plusDays(20);
        assertEmployeeDocumentMetadata(employeeDocumentFileId, employeeId, activeDocumentExpireDate);
        long expiringDocumentFileId = createFileMetadata("employee-document-expiring-" + suffix + ".pdf");
        bindEmployeeDocument(token, employeeId, expiringDocumentFileId, employeeDocumentExpireDate);
        mockMvc.perform(get("/api/hr/employees/" + employeeId + "/documents")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(expiringDocumentFileId))
                .andExpect(jsonPath("$.data[0].documentId", notNullValue()))
                .andExpect(jsonPath("$.data[0].employeeId").value(employeeId))
                .andExpect(jsonPath("$.data[0].employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data[0].realName").value("张三丰"))
                .andExpect(jsonPath("$.data[0].deptName").value("ONES 总部"))
                .andExpect(jsonPath("$.data[0].fileId").value(expiringDocumentFileId))
                .andExpect(jsonPath("$.data[0].documentType").value("CERTIFICATE"))
                .andExpect(jsonPath("$.data[0].issueDate").value("2026-07-01"))
                .andExpect(jsonPath("$.data[0].expireDate").value(employeeDocumentExpireDate.toString()))
                .andExpect(jsonPath("$.data[0].expired").value(false))
                .andExpect(jsonPath("$.data[0].expiringSoon").value(true))
                .andExpect(jsonPath("$.data[0].remark").value("职业资格证书"))
                .andExpect(jsonPath("$.data[0].originalName").value("employee-document-expiring-" + suffix + ".pdf"))
                .andExpect(jsonPath("$.data[0].businessType").value("HR_EMPLOYEE_DOCUMENT"))
                .andExpect(jsonPath("$.data[0].businessId").value(String.valueOf(employeeId)))
                .andExpect(jsonPath("$.data[1].id").value(employeeDocumentFileId))
                .andExpect(jsonPath("$.data[1].fileId").value(employeeDocumentFileId))
                .andExpect(jsonPath("$.data[1].expireDate").value(activeDocumentExpireDate.toString()))
                .andExpect(jsonPath("$.data[1].expiringSoon").value(false));
        mockMvc.perform(get("/api/hr/employees/documents/expiring")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(expiringDocumentFileId))
                .andExpect(jsonPath("$.data[0].employeeId").value(employeeId))
                .andExpect(jsonPath("$.data[0].employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data[0].realName").value("张三丰"))
                .andExpect(jsonPath("$.data[0].deptName").value("ONES 总部"))
                .andExpect(jsonPath("$.data[0].documentType").value("CERTIFICATE"))
                .andExpect(jsonPath("$.data[0].expireDate").value(employeeDocumentExpireDate.toString()))
                .andExpect(jsonPath("$.data[0].expiringSoon").value(true));
        String expiringDocumentExport = mockMvc.perform(get("/api/hr/employees/documents/expiring/export")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("ones-hr-expiring-documents.csv")))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(expiringDocumentExport)
                .startsWith("employeeNo,realName,deptName,documentType,originalName,issueDate,expireDate")
                .contains("E" + suffix)
                .contains("张三丰")
                .contains("ONES 总部")
                .contains("CERTIFICATE")
                .contains("employee-document-expiring-" + suffix + ".pdf")
                .contains(employeeDocumentExpireDate.toString())
                .doesNotContain("employee-document-" + suffix + ".pdf")
                .doesNotContain(activeDocumentExpireDate.toString());
        mockMvc.perform(get("/api/hr/employees/documents/expiring")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "366"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HrErrorCode.EMPLOYEE_DOCUMENT_EXPIRING_DAYS_INVALID.code()));
        removeEmployeeDocument(token, employeeId, employeeDocumentFileId);
        assertEmployeeDocumentDeleted(employeeDocumentFileId);
        assertThat(documentMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                .eq(HrEmployeeDocumentEntity::getFileId, employeeDocumentFileId))).isZero();
        mockMvc.perform(get("/api/hr/employees/" + employeeId + "/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(expiringDocumentFileId));

        transferEmployee(token, employeeId, transferPositionId, transferGradeId, managerId);
        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionName").value("架构师"))
                .andExpect(jsonPath("$.data.gradeName").value("P8"));

        regularizeEmployee(token, employeeId);
        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employmentStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.probationEndDate").value("2026-09-30"));

        long contractAttachmentFileId = createFileMetadata("contract-" + suffix + ".pdf");
        long contractId = createEmployeeContract(token, employeeId, suffix, contractAttachmentFileId);
        assertContractAttachmentBound(contractAttachmentFileId, contractId);
        rejectContractWithMissingAttachment(token, employeeId, suffix);
        updateEmployeeContract(token, employeeId, contractId, suffix);
        LocalDate expiringEndDate = LocalDate.now().plusDays(20);
        long expiringContractId = createEmployeeContract(token, employeeId, "EXP-" + suffix, expiringEndDate);
        mockMvc.perform(get("/api/hr/contracts/expiring")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(expiringContractId))
                .andExpect(jsonPath("$.data[0].employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data[0].contractNo").value("CEXP-" + suffix))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].endDate").value(expiringEndDate.toString()));

        terminateEmployeeContract(token, contractId, LocalDate.of(2028, 12, 31));
        mockMvc.perform(get("/api/hr/employees/" + employeeId + "/contracts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(expiringContractId))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[1].id").value(contractId))
                .andExpect(jsonPath("$.data[1].employeeId").value(employeeId))
                .andExpect(jsonPath("$.data[1].employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data[1].realName").value("张三丰"))
                .andExpect(jsonPath("$.data[1].contractNo").value("C" + suffix))
                .andExpect(jsonPath("$.data[1].contractType").value("FIXED_TERM"))
                .andExpect(jsonPath("$.data[1].status").value("TERMINATED"))
                .andExpect(jsonPath("$.data[1].startDate").value("2026-07-01"))
                .andExpect(jsonPath("$.data[1].endDate").value("2028-12-31"))
                .andExpect(jsonPath("$.data[1].probationMonths").value(3))
                .andExpect(jsonPath("$.data[1].renewalRemindDate").value("2029-11-30"))
                .andExpect(jsonPath("$.data[1].attachmentFileId").value(contractAttachmentFileId))
                .andExpect(jsonPath("$.data[1].remark").value("合同提前终止"));

        resignEmployee(token, employeeId);
        mockMvc.perform(get("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employmentStatus").value("RESIGNED"))
                .andExpect(jsonPath("$.data.leaveDate").value("2026-12-31"));

        mockMvc.perform(get("/api/hr/employees/" + employeeId + "/lifecycle-events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].eventType").value("RESIGN"))
                .andExpect(jsonPath("$.data[0].eventDate").value("2026-12-31"))
                .andExpect(jsonPath("$.data[0].beforeStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].afterStatus").value("RESIGNED"))
                .andExpect(jsonPath("$.data[0].summary").value("员工离职"))
                .andExpect(jsonPath("$.data[0].detail.reason").value("个人原因离职"))
                .andExpect(jsonPath("$.data[1].eventType").value("REGULARIZE"))
                .andExpect(jsonPath("$.data[2].eventType").value("TRANSFER"))
                .andExpect(jsonPath("$.data[2].detail.reason").value("组织架构调整"))
                .andExpect(jsonPath("$.data[3].eventType").value("ONBOARD"))
                .andExpect(jsonPath("$.data[3].detail.employeeNo").value("E" + suffix));

        mockMvc.perform(get("/api/hr/employees/" + employeeId + "/jobs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].employeeId").value(employeeId))
                .andExpect(jsonPath("$.data[0].positionName").value("架构师"))
                .andExpect(jsonPath("$.data[0].gradeName").value("P8"))
                .andExpect(jsonPath("$.data[0].employmentType").value("FULL_TIME"))
                .andExpect(jsonPath("$.data[0].effectiveDate").value("2026-08-01"))
                .andExpect(jsonPath("$.data[0].endDate").value("2026-12-31"))
                .andExpect(jsonPath("$.data[0].changeReason").value("组织架构调整"))
                .andExpect(jsonPath("$.data[1].employeeId").value(employeeId))
                .andExpect(jsonPath("$.data[1].positionName").value("运维工程师"))
                .andExpect(jsonPath("$.data[1].gradeName").value("P7"))
                .andExpect(jsonPath("$.data[1].effectiveDate").value("2026-07-01"))
                .andExpect(jsonPath("$.data[1].endDate").value("2026-07-31"))
                .andExpect(jsonPath("$.data[1].changeReason").value("员工入职初始化任职记录"));

        mockMvc.perform(get("/api/hr/employees/" + employeeId + "/org-context")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(employeeId))
                .andExpect(jsonPath("$.data.deptName").value("ONES 总部"))
                .andExpect(jsonPath("$.data.deptPath.length()").value(1))
                .andExpect(jsonPath("$.data.deptPath[0].name").value("ONES 总部"))
                .andExpect(jsonPath("$.data.manager.id").value(managerId))
                .andExpect(jsonPath("$.data.manager.realName").value("张三"))
                .andExpect(jsonPath("$.data.current.id").value(employeeId))
                .andExpect(jsonPath("$.data.current.realName").value("张三丰"))
                .andExpect(jsonPath("$.data.current.positionName").value("架构师"))
                .andExpect(jsonPath("$.data.directReportCount").value(1))
                .andExpect(jsonPath("$.data.directReports[0].id").value(reportEmployeeId))
                .andExpect(jsonPath("$.data.directReports[0].realName").value("张三"))
                .andExpect(jsonPath("$.data.directReports[0].positionName").value("运维工程师"));

        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HrEmployeeTransferRequest(
                                1L,
                                transferPositionId,
                                transferGradeId,
                                null,
                                "FULL_TIME",
                                LocalDate.of(2027, 1, 1),
                                "离职后调岗应失败"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HrErrorCode.EMPLOYEE_ALREADY_RESIGNED.code()));

        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "E" + suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].employeeNo").value("E" + suffix))
                .andExpect(jsonPath("$.data.list[0].mobile").value("139****9000"))
                .andExpect(jsonPath("$.data.list[0].email").value("z****" + suffix.charAt(suffix.length() - 1) + "@ones.local"));

        String exportResponse = mockMvc.perform(get("/api/hr/employees/export")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "E" + suffix))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("ones-hr-employees.csv")))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(exportResponse)
                .startsWith("employeeNo,realName,preferredName,gender,mobile,email,idCardMasked")
                .contains("E" + suffix)
                .contains("张三丰")
                .contains("ONES 总部")
                .contains("架构师")
                .contains("P8")
                .contains("139****9000")
                .contains("z****" + suffix.charAt(suffix.length() - 1) + "@ones.local")
                .contains("110****5678")
                .doesNotContain(idCardNumber)
                .doesNotContain("110101199001015678")
                .doesNotContain("13900139000")
                .doesNotContain("zhangsanfeng" + suffix + "@ones.local");

        String overviewResponse = mockMvc.perform(get("/api/hr/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode overview = objectMapper.readTree(overviewResponse).path("data");
        assertThat(overview.path("employeeCount").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.path("resignedEmployeeCount").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.path("departmentCount").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.path("activeContractCount").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.path("expiringContractCount").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.path("expiringDocumentCount").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(overview.path("expiredDocumentCount").asLong()).isGreaterThanOrEqualTo(0L);
        assertThat(overview.path("documentExpiringBefore").asText()).isEqualTo(LocalDate.now().plusDays(30).toString());
        assertThat(metricValue(overview.path("employmentStatusStats"), "RESIGNED")).isGreaterThanOrEqualTo(1L);
        assertThat(metricValue(overview.path("departmentStats"), "ONES 总部")).isGreaterThanOrEqualTo(1L);
        assertThat(metricValue(overview.path("lifecycleEventStats"), "ONBOARD")).isGreaterThanOrEqualTo(1L);
        assertThat(metricValue(overview.path("lifecycleEventStats"), "RESIGN")).isGreaterThanOrEqualTo(1L);
    }

    private long metricValue(JsonNode metrics, String codeOrName) {
        for (JsonNode item : metrics) {
            if (codeOrName.equals(item.path("code").asText()) || codeOrName.equals(item.path("name").asText())) {
                return item.path("value").asLong();
            }
        }
        return 0L;
    }

    private long createEmployeeContract(String token, long employeeId, String suffix) throws Exception {
        return createEmployeeContract(token, employeeId, suffix, LocalDate.of(2029, 6, 30));
    }

    private long createEmployeeContract(String token, long employeeId, String suffix, long attachmentFileId) throws Exception {
        return createEmployeeContract(token, employeeId, suffix, LocalDate.of(2029, 6, 30), attachmentFileId);
    }

    private long createEmployeeContract(
            String token,
            long employeeId,
            String suffix,
            LocalDate endDate
    ) throws Exception {
        return createEmployeeContract(token, employeeId, suffix, endDate, null);
    }

    private long createEmployeeContract(
            String token,
            long employeeId,
            String suffix,
            LocalDate endDate,
            Long attachmentFileId
    ) throws Exception {
        java.util.Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.putAll(Map.of(
                "contractNo", "C" + suffix,
                "contractType", "FIXED_TERM",
                "status", "ACTIVE",
                "startDate", "2026-07-01",
                "endDate", endDate.toString(),
                "probationMonths", 3,
                "renewalRemindDate", endDate.minusDays(30).toString(),
                "remark", "首签劳动合同"
        ));
        if (attachmentFileId != null) {
            request.put("attachmentFileId", attachmentFileId);
        }
        String response = mockMvc.perform(post("/api/hr/employees/" + employeeId + "/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.employeeId").value(employeeId))
                .andExpect(jsonPath("$.data.contractNo").value("C" + suffix))
                .andExpect(jsonPath("$.data.contractType").value("FIXED_TERM"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.startDate").value("2026-07-01"))
                .andExpect(jsonPath("$.data.endDate").value(endDate.toString()))
                .andExpect(jsonPath("$.data.renewalRemindDate").value(endDate.minusDays(30).toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        if (attachmentFileId != null) {
            assertThat(objectMapper.readTree(response).at("/data/attachmentFileId").asLong())
                    .isEqualTo(attachmentFileId);
        }
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void updateEmployeeContract(String token, long employeeId, long contractId, String suffix) throws Exception {
        Map<String, Object> request = Map.of(
                "contractNo", "C" + suffix,
                "contractType", "FIXED_TERM",
                "status", "ACTIVE",
                "startDate", "2026-07-01",
                "endDate", "2029-12-31",
                "probationMonths", 3,
                "renewalRemindDate", "2029-11-30",
                "remark", "合同续签信息更新"
        );
        mockMvc.perform(put("/api/hr/employees/" + employeeId + "/contracts/" + contractId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(contractId))
                .andExpect(jsonPath("$.data.contractNo").value("C" + suffix))
                .andExpect(jsonPath("$.data.endDate").value("2029-12-31"))
                .andExpect(jsonPath("$.data.renewalRemindDate").value("2029-11-30"))
                .andExpect(jsonPath("$.data.remark").value("合同续签信息更新"));
    }

    private void rejectContractWithMissingAttachment(String token, long employeeId, String suffix) throws Exception {
        java.util.Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.putAll(Map.of(
                "contractNo", "CMISSING-" + suffix,
                "contractType", "FIXED_TERM",
                "status", "ACTIVE",
                "startDate", "2026-07-01",
                "endDate", "2029-06-30",
                "probationMonths", 3,
                "renewalRemindDate", "2029-05-31",
                "remark", "缺失附件应失败"
        ));
        request.put("attachmentFileId", 9_999_999L);
        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.FILE_NOT_FOUND.code()));
    }

    private long createFileMetadata(String originalName) {
        SystemFileEntity file = new SystemFileEntity();
        file.setOriginalName(originalName);
        file.setStoredName(java.util.UUID.randomUUID() + ".pdf");
        file.setUrl("/api/system/files/" + file.getStoredName());
        file.setContentType(MediaType.APPLICATION_PDF_VALUE);
        file.setExtension("pdf");
        file.setSizeBytes(128L);
        file.setStorageType("LOCAL");
        file.setStatus("ACTIVE");
        file.setUploadedBy(1L);
        fileMapper.insert(file);
        return file.getId();
    }

    private void assertContractAttachmentBound(long fileId, long contractId) {
        SystemFileEntity file = fileMapper.selectById(fileId);
        assertThat(file).isNotNull();
        assertThat(file.getBusinessType()).isEqualTo("HR_EMPLOYEE_CONTRACT");
        assertThat(file.getBusinessId()).isEqualTo(String.valueOf(contractId));
    }

    private void bindEmployeeDocument(String token, long employeeId, long fileId) throws Exception {
        bindEmployeeDocument(token, employeeId, fileId, LocalDate.of(2026, 7, 20));
    }

    private void bindEmployeeDocument(String token, long employeeId, long fileId, LocalDate expireDate) throws Exception {
        Map<String, Object> request = Map.of(
                "documentType", "CERTIFICATE",
                "issueDate", "2026-07-01",
                "expireDate", expireDate.toString(),
                "remark", "职业资格证书"
        );
        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/documents/" + fileId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.fileId").value(fileId))
                .andExpect(jsonPath("$.data.documentType").value("CERTIFICATE"))
                .andExpect(jsonPath("$.data.businessType").value("HR_EMPLOYEE_DOCUMENT"))
                .andExpect(jsonPath("$.data.businessId").value(String.valueOf(employeeId)));
    }

    private void removeEmployeeDocument(String token, long employeeId, long fileId) throws Exception {
        mockMvc.perform(delete("/api/hr/employees/" + employeeId + "/documents/" + fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fileId))
                .andExpect(jsonPath("$.data.status").value("DELETED"))
                .andExpect(jsonPath("$.data.businessType").doesNotExist())
                .andExpect(jsonPath("$.data.businessId").doesNotExist());
    }

    private void assertEmployeeDocumentBound(long fileId, long employeeId) {
        SystemFileEntity file = fileMapper.selectById(fileId);
        assertThat(file).isNotNull();
        assertThat(file.getBusinessType()).isEqualTo("HR_EMPLOYEE_DOCUMENT");
        assertThat(file.getBusinessId()).isEqualTo(String.valueOf(employeeId));
    }

    private void assertEmployeeDocumentMetadata(long fileId, long employeeId) {
        assertEmployeeDocumentMetadata(fileId, employeeId, LocalDate.of(2026, 7, 20));
    }

    private void assertEmployeeDocumentMetadata(long fileId, long employeeId, LocalDate expireDate) {
        HrEmployeeDocumentEntity document = documentMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                .eq(HrEmployeeDocumentEntity::getFileId, fileId));
        assertThat(document).isNotNull();
        assertThat(document.getEmployeeId()).isEqualTo(employeeId);
        assertThat(document.getDocumentType()).isEqualTo("CERTIFICATE");
        assertThat(document.getIssueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(document.getExpireDate()).isEqualTo(expireDate);
        assertThat(document.getRemark()).isEqualTo("职业资格证书");
        assertThat(document.getCreatedBy()).isEqualTo(1L);
    }

    private void assertEmployeeDocumentDeleted(long fileId) {
        SystemFileEntity file = fileMapper.selectById(fileId);
        assertThat(file).isNotNull();
        assertThat(file.getBusinessType()).isNull();
        assertThat(file.getBusinessId()).isNull();
        assertThat(file.getStatus()).isEqualTo("DELETED");
        assertThat(file.getDeletedAt()).isNotNull();
    }

    private void terminateEmployeeContract(String token, long contractId, LocalDate terminateDate) throws Exception {
        Map<String, Object> request = Map.of(
                "terminateDate", terminateDate.toString(),
                "reason", "合同提前终止"
        );
        mockMvc.perform(post("/api/hr/contracts/" + contractId + "/terminate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(contractId))
                .andExpect(jsonPath("$.data.status").value("TERMINATED"))
                .andExpect(jsonPath("$.data.endDate").value(terminateDate.toString()))
                .andExpect(jsonPath("$.data.remark").value("合同提前终止"));
    }

    private long createPosition(String token, String code) throws Exception {
        return createPosition(token, code, "研发工程师");
    }

    private long createPosition(String token, String code, String name) throws Exception {
        String body = objectMapper.writeValueAsString(new HrPositionSaveRequest(
                code,
                name,
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
        return createJobGrade(token, code, "P6", 6);
    }

    private long createJobGrade(String token, String code, String name, int rank) throws Exception {
        String body = objectMapper.writeValueAsString(new HrJobGradeSaveRequest(
                code,
                name,
                rank,
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

    private void transferEmployee(String token, long employeeId, long positionId, long gradeId) throws Exception {
        transferEmployee(token, employeeId, positionId, gradeId, null);
    }

    private void transferEmployee(
            String token,
            long employeeId,
            long positionId,
            long gradeId,
            Long managerEmployeeId
    ) throws Exception {
        String body = objectMapper.writeValueAsString(new HrEmployeeTransferRequest(
                1L,
                positionId,
                gradeId,
                managerEmployeeId,
                "FULL_TIME",
                LocalDate.of(2026, 8, 1),
                "组织架构调整"
        ));
        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.positionName").value("架构师"))
                .andExpect(jsonPath("$.data.gradeName").value("P8"));
    }

    private void updateEmployeeProfile(
            String token,
            long employeeId,
            String suffix,
            String originalIdCardNumber
    ) throws Exception {
        Map<String, Object> request = Map.of(
                "realName", "张三丰",
                "preferredName", "三丰",
                "gender", "MALE",
                "mobile", "13900139000",
                "email", "zhangsanfeng" + suffix + "@ones.local",
                "idCardNumber", "110101199001015678",
                "userId", 2L,
                "probationEndDate", "2026-10-15",
                "remark", "基础档案信息更新"
        );
        String response = mockMvc.perform(put("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(employeeId))
                .andExpect(jsonPath("$.data.realName").value("张三丰"))
                .andExpect(jsonPath("$.data.idCardMasked").value("110****5678"))
                .andExpect(jsonPath("$.data.probationEndDate").value("2026-10-15"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(response).doesNotContain(originalIdCardNumber);
        assertThat(response).doesNotContain("110101199001015678");
    }

    private void updateEmployeeProfileWithoutIdCard(String token, long employeeId, String suffix) throws Exception {
        Map<String, Object> request = Map.of(
                "realName", "张三丰",
                "preferredName", "三丰",
                "gender", "MALE",
                "mobile", "13900139000",
                "email", "zhangsanfeng" + suffix + "@ones.local",
                "userId", 2L,
                "probationEndDate", "2026-10-15",
                "remark", "基础档案信息更新"
        );
        mockMvc.perform(put("/api/hr/employees/" + employeeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(employeeId))
                .andExpect(jsonPath("$.data.idCardMasked").value("110****5678"));
    }

    private void regularizeEmployee(String token, long employeeId) throws Exception {
        String body = objectMapper.writeValueAsString(new HrEmployeeRegularizeRequest(
                LocalDate.of(2026, 9, 30),
                "试用期表现达标"
        ));
        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/regularize")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employmentStatus").value("ACTIVE"));
    }

    private void resignEmployee(String token, long employeeId) throws Exception {
        String body = objectMapper.writeValueAsString(new HrEmployeeResignRequest(
                LocalDate.of(2026, 12, 31),
                "个人原因离职"
        ));
        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/resign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employmentStatus").value("RESIGNED"));
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

    private String createEmployee(
            String token,
            String suffix,
            long positionId,
            long gradeId,
            String idCardNumber,
            Long managerEmployeeId
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
                managerEmployeeId,
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
