package com.ones.admin.system;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ones.bootstrap")
public class SystemBootstrapProperties {

    private boolean enabled;
    private String adminUsername = "admin";
    private String adminDisplayName = "系统管理员";
    private String adminPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminDisplayName() {
        return adminDisplayName;
    }

    public void setAdminDisplayName(String adminDisplayName) {
        this.adminDisplayName = adminDisplayName;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String requireAdminPassword() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "启用系统初始化时必须通过 ONES_BOOTSTRAP_ADMIN_PASSWORD 提供初始管理员密码"
            );
        }
        return adminPassword;
    }
}
