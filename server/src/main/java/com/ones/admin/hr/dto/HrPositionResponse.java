package com.ones.admin.hr.dto;

import java.time.LocalDateTime;

public record HrPositionResponse(
        Long id,
        String positionCode,
        String positionName,
        Long deptId,
        String deptName,
        String description,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
