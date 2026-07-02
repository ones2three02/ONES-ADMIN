package com.ones.admin.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HrEmployeeResponse(
        Long id,
        String employeeNo,
        String realName,
        String preferredName,
        String gender,
        String mobile,
        String email,
        String idCardMasked,
        Long userId,
        Long deptId,
        String deptName,
        Long positionId,
        String positionName,
        Long gradeId,
        String gradeName,
        Long managerEmployeeId,
        String managerName,
        String employmentType,
        String employmentStatus,
        LocalDate hireDate,
        LocalDate probationEndDate,
        LocalDate leaveDate,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
