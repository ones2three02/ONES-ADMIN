package com.ones.admin.hr.dto;

import java.time.LocalDateTime;

public record HrJobGradeResponse(
        Long id,
        String gradeCode,
        String gradeName,
        Integer gradeRank,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
