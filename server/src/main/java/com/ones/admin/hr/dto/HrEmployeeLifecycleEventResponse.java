package com.ones.admin.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record HrEmployeeLifecycleEventResponse(
        Long id,
        Long employeeId,
        String eventType,
        LocalDate eventDate,
        String beforeStatus,
        String afterStatus,
        String summary,
        Map<String, Object> detail,
        Long createdBy,
        LocalDateTime createdAt
) {
}
