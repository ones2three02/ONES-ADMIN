package com.ones.admin.system.audit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "ones.audit.retention")
public class AuditRetentionProperties {

    @Min(1)
    private int loginLogDays = 180;

    @Min(1)
    private int operationLogDays = 180;

    public int getLoginLogDays() {
        return loginLogDays;
    }

    public void setLoginLogDays(int loginLogDays) {
        this.loginLogDays = loginLogDays;
    }

    public int getOperationLogDays() {
        return operationLogDays;
    }

    public void setOperationLogDays(int operationLogDays) {
        this.operationLogDays = operationLogDays;
    }
}
