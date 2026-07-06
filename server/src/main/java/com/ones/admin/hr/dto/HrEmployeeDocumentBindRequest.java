package com.ones.admin.hr.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record HrEmployeeDocumentBindRequest(
        @Size(max = 64) String documentType,
        LocalDate issueDate,
        LocalDate expireDate,
        @Size(max = 500) String remark
) {
}
