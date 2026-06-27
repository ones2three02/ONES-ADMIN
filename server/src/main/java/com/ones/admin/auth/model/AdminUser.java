package com.ones.admin.auth.model;

import java.util.List;

public record AdminUser(
        Long id,
        String username,
        String displayName,
        String avatar,
        String passwordHash,
        List<String> roles,
        List<String> permissions,
        boolean enabled
) {
}
