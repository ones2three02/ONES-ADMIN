package com.ones.admin.system.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleSaveRequest(
        @Size(max = 64) String code,
        @Size(max = 64) String name,
        @Size(max = 32) String dataScope,
        @Size(max = 255) String remark,
        Integer status,
        List<String> permissions
) {
}
