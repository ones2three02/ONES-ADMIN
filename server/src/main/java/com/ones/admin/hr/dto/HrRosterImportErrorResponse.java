package com.ones.admin.hr.dto;

import java.time.LocalDateTime;

public record HrRosterImportErrorResponse(
        Long id,
        Long batchId,
        Integer rowNumber,
        String employeeNo,
        String fieldName,
        String errorMessage,
        String rawJson,
        LocalDateTime createdAt
) {
}
