package com.ones.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ones.security.public-endpoints")
public class SecurityPublicEndpointProperties {

    private boolean apiDocsPublic;
    private boolean actuatorPublic;

    public boolean isApiDocsPublic() {
        return apiDocsPublic;
    }

    public void setApiDocsPublic(boolean apiDocsPublic) {
        this.apiDocsPublic = apiDocsPublic;
    }

    public boolean isActuatorPublic() {
        return actuatorPublic;
    }

    public void setActuatorPublic(boolean actuatorPublic) {
        this.actuatorPublic = actuatorPublic;
    }
}
