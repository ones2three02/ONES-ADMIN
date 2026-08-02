package com.ones.admin.hr.dto;

import java.util.List;

public record HrEmployeeOrgContextResponse(
        Long employeeId,
        Long deptId,
        String deptName,
        List<DeptNode> deptPath,
        EmployeeNode manager,
        EmployeeNode current,
        List<EmployeeNode> directReports,
        long directReportCount
) {

    public record DeptNode(
            Long id,
            String name
    ) {
    }

    public record EmployeeNode(
            Long id,
            String employeeNo,
            String realName,
            Long deptId,
            String deptName,
            Long positionId,
            String positionName,
            Long gradeId,
            String gradeName,
            String employmentStatus
    ) {
    }
}
