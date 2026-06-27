package com.ones.admin.system.dto;

public record RoleResponse(
        Long id,
        String code,
        String name,
        boolean enabled
) {
}
