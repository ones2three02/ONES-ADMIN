package com.ones.admin.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeRegularizeRequest(
        @NotNull LocalDate regularizeDate,
        @Size(max = 255) String remark
) {
}
