package com.ones.admin.system.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String avatar,
        boolean enabled,
        List<String> roles
) {
}
