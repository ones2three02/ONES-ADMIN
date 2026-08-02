package com.ones.admin.auth.oauth.feishu;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "ones.auth.feishu")
public class FeishuAuthProperties {

    private boolean enabled;
    private String baseUrl = "https://open.feishu.cn";
    private String appId;
    private String appSecret;
    private String callbackUrl;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    @AssertTrue(message = "启用飞书登录时必须配置 App ID、App Secret 和回调地址")
    public boolean isConfigurationValid() {
        return !enabled || hasText(baseUrl) && hasText(appId) && hasText(appSecret) && hasText(callbackUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
