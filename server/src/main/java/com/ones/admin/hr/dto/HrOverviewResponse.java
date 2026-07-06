package com.ones.admin.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HrOverviewResponse(
        long employeeCount,
        long activeEmployeeCount,
        long probationEmployeeCount,
        long resignedEmployeeCount,
        long departmentCount,
        long activeContractCount,
        long expiringContractCount,
        long probationDueCount,
        LocalDate contractExpiringBefore,
        LocalDate probationDueBefore,
        List<MetricItem> employmentStatusStats,
        List<MetricItem> departmentStats,
        List<MetricItem> lifecycleEventStats,
        LocalDateTime generatedAt
) {

    public record MetricItem(
            String code,
            String name,
            long value
    ) {
    }
}
