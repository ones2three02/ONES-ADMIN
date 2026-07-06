package com.ones.admin.hr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrJobGradeSaveRequest;
import com.ones.admin.hr.dto.HrPositionSaveRequest;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemPermissionEntity;
import com.ones.admin.system.entity.SystemRolePermissionEntity;
import com.ones.admin.system.mapper.SystemPermissionMapper;
import com.ones.admin.system.mapper.SystemRolePermissionMapper;
import com.ones.admin.system.dto.DeptSaveRequest;
import com.ones.admin.system.dto.RoleSaveRequest;
import com.ones.admin.system.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HrDataScopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemPermissionMapper permissionMapper;

    @Autowired
    private SystemRolePermissionMapper rolePermissionMapper;

    @Test
    void hrEmployeeReadApisRespectRoleDataScope() throws Exception {
        String adminToken = login("admin", "admin123");
        String suffix = String.valueOf(System.nanoTime());

        long childDeptId = createDept(adminToken, "研发中心-" + suffix, 1L);
        createDept(adminToken, "研发一组-" + suffix, childDeptId);
        long roleId = createHrRole(adminToken, "HR_LIMIT_" + suffix, "DEPT_AND_CHILD");
        grantPermissions(roleId,
                "hr:employee:list",
                "hr:employee:detail",
                "hr:employee:export",
                "hr:contract:list",
                "hr:overview:view");
        String username = "hrscope" + suffix;
        long userId = createUser(adminToken, username, childDeptId, "HR_LIMIT_" + suffix);

        long visiblePositionId = createPosition(adminToken, "VP-" + suffix, childDeptId);
        long hiddenPositionId = createPosition(adminToken, "HP-" + suffix, 1L);
        long gradeId = createJobGrade(adminToken, "DS-" + suffix);

        long visibleEmployeeId = createEmployee(
                adminToken,
                "VE" + suffix,
                "可见员工",
                childDeptId,
                visiblePositionId,
                gradeId,
                userId
        );
        long hiddenEmployeeId = createEmployee(
                adminToken,
                "HE" + suffix,
                "总部员工",
                1L,
                hiddenPositionId,
                gradeId,
                null
        );
        createContract(adminToken, visibleEmployeeId, "VC" + suffix);
        createContract(adminToken, hiddenEmployeeId, "HC" + suffix);

        String hrToken = login(username, "hrscope123");

        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", "Bearer " + hrToken)
                        .param("keyword", suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].employeeNo").value("VE" + suffix));

        mockMvc.perform(get("/api/hr/employees/" + visibleEmployeeId)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("VE" + suffix));

        mockMvc.perform(get("/api/hr/employees/" + hiddenEmployeeId)
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HrErrorCode.EMPLOYEE_DATA_SCOPE_DENIED.code()));

        mockMvc.perform(get("/api/hr/employees/" + hiddenEmployeeId + "/contracts")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(HrErrorCode.EMPLOYEE_DATA_SCOPE_DENIED.code()));

        mockMvc.perform(get("/api/hr/contracts/expiring")
                        .header("Authorization", "Bearer " + hrToken)
                        .param("days", "365"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].employeeNo").value("VE" + suffix));

        String exportResponse = mockMvc.perform(get("/api/hr/employees/export")
                        .header("Authorization", "Bearer " + hrToken)
                        .param("keyword", suffix))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(exportResponse)
                .contains("VE" + suffix)
                .doesNotContain("HE" + suffix);

        mockMvc.perform(get("/api/hr/overview")
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCount").value(1));

        updateRoleDataScope(adminToken, roleId, "SELF");
        mockMvc.perform(get("/api/hr/employees")
                        .header("Authorization", "Bearer " + hrToken)
                        .param("keyword", suffix)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].employeeNo").value("VE" + suffix));
    }

    private long createDept(String token, String name, Long parentId) throws Exception {
        String response = mockMvc.perform(post("/api/system/dept")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeptSaveRequest(
                                parentId,
                                name,
                                "数据权限测试部门",
                                1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private long createHrRole(String token, String code, String dataScope) throws Exception {
        String response = mockMvc.perform(post("/api/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoleSaveRequest(
                                code,
                                "数据范围 HR",
                                dataScope,
                                "HRMS 数据范围测试角色",
                                1,
                                List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.dataScope").value(dataScope))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void updateRoleDataScope(String token, long roleId, String dataScope) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/system/role/" + roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dataScope", dataScope))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataScope").value(dataScope));
    }

    private void grantPermissions(long roleId, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            SystemPermissionEntity permission = permissionMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemPermissionEntity>()
                            .eq(SystemPermissionEntity::getCode, permissionCode)
                            .last("limit 1")
            );
            if (permission == null) {
                throw new IllegalStateException("权限不存在：" + permissionCode);
            }
            SystemRolePermissionEntity relation = new SystemRolePermissionEntity();
            relation.setRoleId(roleId);
            relation.setPermissionId(permission.getId());
            rolePermissionMapper.insert(relation);
        }
    }

    private long createUser(String token, String username, long deptId, String roleCode) throws Exception {
        String response = mockMvc.perform(post("/api/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateRequest(
                                username,
                                "数据范围 HR",
                                "hrscope123",
                                deptId,
                                "HRMS 数据范围测试用户",
                                true,
                                List.of(roleCode)
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private long createPosition(String token, String code, long deptId) throws Exception {
        String response = mockMvc.perform(post("/api/hr/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HrPositionSaveRequest(
                                code,
                                "数据范围岗位",
                                deptId,
                                "数据权限测试岗位",
                                true
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private long createJobGrade(String token, String code) throws Exception {
        String response = mockMvc.perform(post("/api/hr/job-grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HrJobGradeSaveRequest(
                                code,
                                "数据范围职级",
                                9,
                                true
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private long createEmployee(
            String token,
            String employeeNo,
            String realName,
            long deptId,
            long positionId,
            long gradeId,
            Long userId
    ) throws Exception {
        String response = mockMvc.perform(post("/api/hr/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HrEmployeeCreateRequest(
                                employeeNo,
                                realName,
                                null,
                                "MALE",
                                "13800138000",
                                employeeNo.toLowerCase() + "@ones.local",
                                null,
                                userId,
                                deptId,
                                positionId,
                                gradeId,
                                null,
                                "FULL_TIME",
                                "ACTIVE",
                                LocalDate.of(2026, 7, 1),
                                null,
                                "数据范围测试员工"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void createContract(String token, long employeeId, String contractNo) throws Exception {
        mockMvc.perform(post("/api/hr/employees/" + employeeId + "/contracts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractNo", contractNo,
                                "contractType", "FIXED_TERM",
                                "status", "ACTIVE",
                                "startDate", "2026-07-01",
                                "endDate", LocalDate.now().plusDays(90).toString()
                        ))))
                .andExpect(status().isOk());
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
