package com.ones.admin.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeResignRequest(
        @NotNull LocalDate leaveDate,
        @Size(max = 255) String resignationReason
) {
}
