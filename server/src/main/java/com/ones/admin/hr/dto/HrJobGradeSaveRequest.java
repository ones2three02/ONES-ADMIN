package com.ones.admin.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HrJobGradeSaveRequest(
        @NotBlank @Size(max = 64) String gradeCode,
        @NotBlank @Size(max = 100) String gradeName,
        @NotNull Integer gradeRank,
        Boolean enabled
) {
}
