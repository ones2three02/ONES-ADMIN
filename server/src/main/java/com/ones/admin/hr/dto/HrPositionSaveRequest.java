package com.ones.admin.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HrPositionSaveRequest(
        @NotBlank @Size(max = 64) String positionCode,
        @NotBlank @Size(max = 100) String positionName,
        Long deptId,
        @Size(max = 500) String description,
        Boolean enabled
) {
}
