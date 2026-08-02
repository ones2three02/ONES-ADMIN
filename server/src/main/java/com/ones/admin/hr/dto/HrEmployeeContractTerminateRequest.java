package com.ones.admin.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeContractTerminateRequest(
        @NotNull LocalDate terminateDate,
        @Size(max = 500) String reason
) {
}
