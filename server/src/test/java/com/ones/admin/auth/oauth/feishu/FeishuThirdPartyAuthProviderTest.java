package com.ones.admin.auth.oauth.feishu;

import com.ones.admin.auth.oauth.model.ExternalIdentityProfile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FeishuThirdPartyAuthProviderTest {

    @Test
    void authorizationUrlAndIdentityExchangeFollowFeishuProtocol() {
        FeishuAuthProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FeishuThirdPartyAuthProvider provider = new FeishuThirdPartyAuthProvider(properties, builder);

        String authorizationUrl = provider.authorizationUrl("state-token");
        assertThat(authorizationUrl)
                .contains("/open-apis/authen/v1/index")
                .contains("app_id=cli_test")
                .contains("state=state-token")
                .doesNotContain("secret-value");

        server.expect(once(), requestTo("https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"ok","app_access_token":"app-token"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://open.feishu.cn/open-apis/authen/v1/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"ok","data":{"access_token":"user-token"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://open.feishu.cn/open-apis/authen/v1/user_info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"ok","data":{"name":"张三","open_id":"ou_open","union_id":"on_union","tenant_key":"tenant-1"}}
                        """, MediaType.APPLICATION_JSON));

        ExternalIdentityProfile profile = provider.authenticate("authorization-code");

        assertThat(profile).isEqualTo(new ExternalIdentityProfile(
                "feishu", "tenant-1", "on_union", "张三"
        ));
        server.verify();
    }

    @Test
    void enabledProviderRequiresCredentialsAndCallback() {
        FeishuAuthProperties properties = new FeishuAuthProperties();
        properties.setEnabled(true);

        assertThat(properties.isConfigurationValid()).isFalse();
    }

    private FeishuAuthProperties properties() {
        FeishuAuthProperties properties = new FeishuAuthProperties();
        properties.setEnabled(true);
        properties.setAppId("cli_test");
        properties.setAppSecret("secret-value");
        properties.setCallbackUrl("https://admin.example.com/auth/oauth/feishu/callback");
        return properties;
    }
}
