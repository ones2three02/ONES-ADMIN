package com.ones.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.system.dto.RoleSaveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void manageRoleDataScope() throws Exception {
        String token = login("admin", "admin123");
        String suffix = String.valueOf(System.nanoTime());

        mockMvc.perform(get("/api/system/role/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].dataScope", hasItem("ALL")))
                .andExpect(jsonPath("$.data[*].dataScope", hasItem("DEPT_AND_CHILD")));

        String createBody = objectMapper.writeValueAsString(new RoleSaveRequest(
                "HR_LIMITED_" + suffix,
                "受限 HR",
                "DEPT",
                "测试数据权限",
                1,
                List.of()
        ));
        String createResponse = mockMvc.perform(post("/api/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataScope").value("DEPT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long roleId = objectMapper.readTree(createResponse).at("/data/id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "dataScope", "SELF",
                "status", 1
        ));
        mockMvc.perform(put("/api/system/role/" + roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataScope").value("SELF"));

        mockMvc.perform(put("/api/system/role/" + roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dataScope", "WRONG_SCOPE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SystemErrorCode.ROLE_DATA_SCOPE_INVALID.code()));
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
