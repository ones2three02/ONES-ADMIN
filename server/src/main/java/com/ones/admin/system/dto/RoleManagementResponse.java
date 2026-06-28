package com.ones.admin.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RoleManagementResponse(
        String id,
        String code,
        String name,
        String remark,
        int status,
        LocalDateTime createTime,
        List<String> permissions
) {
}
