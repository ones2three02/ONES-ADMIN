package com.ones.admin.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.auth.dto.LoginRequest;
import com.ones.admin.auth.oauth.model.ExternalIdentityProfile;
import com.ones.admin.system.entity.SystemLoginLogEntity;
import com.ones.admin.system.mapper.SystemLoginLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemLoginLogMapper loginLogMapper;

    @MockitoBean
    private ThirdPartyAuthProviderRegistry providerRegistry;

    @BeforeEach
    void setUpProvider() {
        ThirdPartyAuthProvider provider = new ThirdPartyAuthProvider() {
            @Override
            public String id() { return "feishu"; }
            @Override
            public String displayName() { return "飞书"; }
            @Override
            public boolean enabled() { return true; }
            @Override
            public String authorizationUrl(String state) {
                return "https://open.feishu.cn/open-apis/authen/v1/index?state=" + state;
            }
            @Override
            public ExternalIdentityProfile authenticate(String authorizationCode) {
                return new ExternalIdentityProfile("feishu", "tenant-test", "union-test", "飞书测试用户");
            }
        };
        when(providerRegistry.all()).thenReturn(List.of(provider));
        when(providerRegistry.requireEnabled("feishu")).thenReturn(provider);
    }

    @Test
    void preBoundFeishuIdentityCompletesLoginAndRejectsReplay() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("feishu"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].appSecret").doesNotExist());

        String adminToken = passwordLogin();
        String bindBody = objectMapper.writeValueAsString(
                new ExternalIdentityBindRequest("feishu", "tenant-test", "union-test")
        );
        mockMvc.perform(post("/api/system/users/1/external-identities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bindBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("feishu"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        String authorizeResponse = mockMvc.perform(get("/api/auth/oauth/feishu/authorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300))
                .andReturn().getResponse().getContentAsString();
        String authorizationUrl = objectMapper.readTree(authorizeResponse).at("/data/authorizationUrl").asText();
        String state = UriComponentsBuilder.fromUriString(authorizationUrl).build().getQueryParams().getFirst("state");
        assertThat(state).isNotBlank();

        String callbackResponse = mockMvc.perform(get("/api/auth/oauth/feishu/callback")
                        .param("code", "valid-code")
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresInSeconds").value(60))
                .andReturn().getResponse().getContentAsString();
        String ticket = objectMapper.readTree(callbackResponse).at("/data/ticket").asText();

        String exchangeBody = objectMapper.writeValueAsString(new OAuthExchangeRequest(ticket));
        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchangeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.token.tokenValue").isNotEmpty());

        mockMvc.perform(get("/api/auth/oauth/feishu/callback")
                        .param("code", "valid-code")
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4211));
        mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchangeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4214));

        List<SystemLoginLogEntity> oauthLogs = loginLogMapper.selectList(null).stream()
                .filter(log -> "OAUTH".equals(log.getAuthMethod()))
                .toList();
        assertThat(oauthLogs).anyMatch(log -> Boolean.TRUE.equals(log.getSuccess())
                && "feishu".equals(log.getProvider())
                && log.getExternalIdentityId() != null);
    }

    private String passwordLogin() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.at("/data/token/tokenValue").asText();
    }
}
