package com.ones.admin.system.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UserUpdateRequest(
        @NotBlank String displayName,
        Boolean enabled,
        List<String> roleCodes
) {
}
