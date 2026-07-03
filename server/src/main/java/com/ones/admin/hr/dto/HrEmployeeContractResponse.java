package com.ones.admin.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HrEmployeeContractResponse(
        Long id,
        Long employeeId,
        String employeeNo,
        String realName,
        String contractNo,
        String contractType,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        Integer probationMonths,
        LocalDate renewalRemindDate,
        Long attachmentFileId,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
