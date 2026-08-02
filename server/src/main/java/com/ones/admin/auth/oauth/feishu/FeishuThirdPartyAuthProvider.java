package com.ones.admin.auth.oauth.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ones.admin.auth.AuthErrorCode;
import com.ones.admin.auth.oauth.ThirdPartyAuthProvider;
import com.ones.admin.auth.oauth.model.ExternalIdentityProfile;
import com.ones.admin.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class FeishuThirdPartyAuthProvider implements ThirdPartyAuthProvider {

    private static final String PROVIDER_ID = "feishu";

    private final FeishuAuthProperties properties;
    private final RestClient restClient;

    public FeishuThirdPartyAuthProvider(FeishuAuthProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public String displayName() {
        return "飞书";
    }

    @Override
    public boolean enabled() {
        return properties.isEnabled();
    }

    @Override
    public String authorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path("/open-apis/authen/v1/index")
                .queryParam("app_id", properties.getAppId())
                .queryParam("redirect_uri", properties.getCallbackUrl())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public ExternalIdentityProfile authenticate(String authorizationCode) {
        try {
            AppAccessTokenResponse appToken = restClient.post()
                    .uri("/open-apis/auth/v3/app_access_token/internal")
                    .body(new AppAccessTokenRequest(properties.getAppId(), properties.getAppSecret()))
                    .retrieve()
                    .body(AppAccessTokenResponse.class);
            requireSuccess(appToken == null ? -1 : appToken.code());
            if (appToken == null || !hasText(appToken.appAccessToken())) {
                throw providerFailure();
            }

            UserAccessTokenResponse userToken = restClient.post()
                    .uri("/open-apis/authen/v1/access_token")
                    .header("Authorization", "Bearer " + appToken.appAccessToken())
                    .body(new UserAccessTokenRequest("authorization_code", authorizationCode))
                    .retrieve()
                    .body(UserAccessTokenResponse.class);
            requireSuccess(userToken == null ? -1 : userToken.code());
            if (userToken == null || userToken.data() == null || !hasText(userToken.data().accessToken())) {
                throw providerFailure();
            }

            UserInfoResponse userInfo = restClient.get()
                    .uri("/open-apis/authen/v1/user_info")
                    .header("Authorization", "Bearer " + userToken.data().accessToken())
                    .retrieve()
                    .body(UserInfoResponse.class);
            requireSuccess(userInfo == null ? -1 : userInfo.code());
            if (userInfo == null || userInfo.data() == null) {
                throw providerFailure();
            }
            String subject = hasText(userInfo.data().unionId())
                    ? userInfo.data().unionId()
                    : userInfo.data().openId();
            if (!hasText(subject) || !hasText(userInfo.data().tenantKey())) {
                throw providerFailure();
            }
            return new ExternalIdentityProfile(
                    PROVIDER_ID,
                    userInfo.data().tenantKey(),
                    subject,
                    userInfo.data().name()
            );
        } catch (RestClientException exception) {
            throw providerFailure();
        }
    }

    private void requireSuccess(int code) {
        if (code != 0) {
            throw providerFailure();
        }
    }

    private BusinessException providerFailure() {
        return new BusinessException(AuthErrorCode.OAUTH_PROVIDER_FAILED);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record AppAccessTokenRequest(
            @JsonProperty("app_id") String appId,
            @JsonProperty("app_secret") String appSecret
    ) {
    }

    private record AppAccessTokenResponse(
            int code,
            String msg,
            @JsonProperty("app_access_token") String appAccessToken
    ) {
    }

    private record UserAccessTokenRequest(
            @JsonProperty("grant_type") String grantType,
            String code
    ) {
    }

    private record UserAccessTokenResponse(int code, String msg, UserAccessTokenData data) {
    }

    private record UserAccessTokenData(@JsonProperty("access_token") String accessToken) {
    }

    private record UserInfoResponse(int code, String msg, UserInfoData data) {
    }

    private record UserInfoData(
            String name,
            @JsonProperty("open_id") String openId,
            @JsonProperty("union_id") String unionId,
            @JsonProperty("tenant_key") String tenantKey
    ) {
    }
}
