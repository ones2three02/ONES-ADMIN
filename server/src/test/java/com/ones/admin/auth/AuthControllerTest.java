package com.ones.admin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginThenReadCurrentUserAndMenus() throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"));

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.token.tokenValue", not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String tokenValue = objectMapper.readTree(token).at("/data/token/tokenValue").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        mockMvc.perform(get("/api/auth/roles").header("Authorization", "Bearer " + tokenValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("SUPER_ADMIN"));

        mockMvc.perform(get("/api/auth/codes").header("Authorization", "Bearer " + tokenValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("dashboard:view"));

        mockMvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer " + tokenValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not("")));

        mockMvc.perform(get("/api/system/menus").header("Authorization", "Bearer " + tokenValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("工作台"))
                .andExpect(jsonPath("$.data[0].children[0].path", containsString("/dashboard/overview")));

        mockMvc.perform(get("/api/system/menus/routes").header("Authorization", "Bearer " + tokenValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].component").value("BasicLayout"))
                .andExpect(jsonPath("$..path", hasItem("/system/api-resources")))
                .andExpect(jsonPath("$..path", hasItem("/system/audit")));
    }
}
