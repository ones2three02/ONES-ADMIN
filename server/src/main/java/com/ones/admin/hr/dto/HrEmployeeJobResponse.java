package com.ones.admin.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HrEmployeeJobResponse(
        Long id,
        Long employeeId,
        Long deptId,
        String deptName,
        Long positionId,
        String positionName,
        Long gradeId,
        String gradeName,
        Long managerEmployeeId,
        String managerName,
        String employmentType,
        LocalDate effectiveDate,
        LocalDate endDate,
        String changeReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
