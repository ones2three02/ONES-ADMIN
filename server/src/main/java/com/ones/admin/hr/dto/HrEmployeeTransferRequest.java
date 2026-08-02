package com.ones.admin.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeTransferRequest(
        @NotNull Long deptId,
        Long positionId,
        Long gradeId,
        Long managerEmployeeId,
        @Size(max = 32) String employmentType,
        @NotNull LocalDate effectiveDate,
        @Size(max = 255) String changeReason
) {
}
