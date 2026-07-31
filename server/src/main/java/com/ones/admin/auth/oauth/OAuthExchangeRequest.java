package com.ones.admin.auth.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OAuthExchangeRequest(@NotBlank @Size(max = 256) String ticket) {
}
