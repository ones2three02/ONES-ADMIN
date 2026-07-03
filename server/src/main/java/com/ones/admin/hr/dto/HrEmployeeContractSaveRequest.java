package com.ones.admin.hr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeContractSaveRequest(
        @NotBlank @Size(max = 64) String contractNo,
        @NotBlank @Size(max = 32) String contractType,
        @NotBlank @Size(max = 32) String status,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Min(0) @Max(120) Integer probationMonths,
        LocalDate renewalRemindDate,
        Long attachmentFileId,
        @Size(max = 500) String remark
) {
}
