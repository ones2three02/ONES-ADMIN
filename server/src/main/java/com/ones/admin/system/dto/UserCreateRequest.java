package com.ones.admin.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        @NotBlank @Size(min = 6) String password,
        Long deptId,
        String remark,
        Boolean enabled,
        List<String> roleCodes
) {
}
