package com.ones.admin.auth.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExternalIdentityBindRequest(
        @NotBlank @Size(max = 32) String provider,
        @NotBlank @Size(max = 128) String tenantKey,
        @NotBlank @Size(max = 128) String externalSubject
) {
}
