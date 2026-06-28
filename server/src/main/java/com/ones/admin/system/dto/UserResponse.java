package com.ones.admin.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        Long deptId,
        String avatar,
        String remark,
        boolean enabled,
        LocalDateTime lastLoginAt,
        LocalDateTime lockedUntil,
        LocalDateTime createdAt,
        List<String> roles
) {
}
