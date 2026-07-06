package com.ones.admin.hr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.hr.dto.HrOverviewResponse;
import com.ones.admin.hr.entity.HrEmployeeContractEntity;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.entity.HrEmployeeLifecycleEventEntity;
import com.ones.admin.hr.mapper.HrEmployeeContractMapper;
import com.ones.admin.hr.mapper.HrEmployeeLifecycleEventMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeContext;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HrOverviewService {

    private static final int UPCOMING_DAYS = 30;
    private static final Map<String, String> EMPLOYMENT_STATUS_NAMES = orderedMap(
            "ACTIVE", "在职(正式)",
            "PROBATION", "试用期",
            "SUSPENDED", "停职",
            "RESIGNED", "已离职"
    );
    private static final Map<String, String> LIFECYCLE_EVENT_NAMES = orderedMap(
            "ONBOARD", "入职",
            "TRANSFER", "调岗",
            "REGULARIZE", "转正",
            "RESIGN", "离职"
    );

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeLifecycleEventMapper lifecycleEventMapper;
    private final SystemDeptMapper deptMapper;
    private final DataScopeService dataScopeService;

    public HrOverviewService(
            HrEmployeeMapper employeeMapper,
            HrEmployeeContractMapper contractMapper,
            HrEmployeeLifecycleEventMapper lifecycleEventMapper,
            SystemDeptMapper deptMapper,
            DataScopeService dataScopeService
    ) {
        this.employeeMapper = employeeMapper;
        this.contractMapper = contractMapper;
        this.lifecycleEventMapper = lifecycleEventMapper;
        this.deptMapper = deptMapper;
        this.dataScopeService = dataScopeService;
    }

    public HrOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        LocalDate upcomingDate = today.plusDays(UPCOMING_DAYS);
        LocalDateTime lifecycleCreatedAfter = LocalDateTime.now().minusDays(UPCOMING_DAYS);
        DataScopeContext dataScope = dataScopeService.currentContext();
        List<HrEmployeeEntity> employees = employeeMapper.selectList(new LambdaQueryWrapper<HrEmployeeEntity>()
                .orderByAsc(HrEmployeeEntity::getDeptId)
                .orderByAsc(HrEmployeeEntity::getId))
                .stream()
                .filter(employee -> dataScope.canAccess(employee.getDeptId(), employee.getUserId()))
                .toList();
        Set<Long> visibleEmployeeIds = employees.stream()
                .map(HrEmployeeEntity::getId)
                .collect(Collectors.toSet());
        List<HrEmployeeContractEntity> contracts = visibleEmployeeIds.isEmpty()
                ? List.of()
                : contractMapper.selectList(new LambdaQueryWrapper<HrEmployeeContractEntity>()
                .in(HrEmployeeContractEntity::getEmployeeId, visibleEmployeeIds));
        List<HrEmployeeLifecycleEventEntity> lifecycleEvents = lifecycleEventMapper.selectList(
                new LambdaQueryWrapper<HrEmployeeLifecycleEventEntity>()
                        .ge(HrEmployeeLifecycleEventEntity::getCreatedAt, lifecycleCreatedAfter)
        );
        if (!visibleEmployeeIds.isEmpty()) {
            lifecycleEvents = lifecycleEvents.stream()
                    .filter(event -> visibleEmployeeIds.contains(event.getEmployeeId()))
                    .toList();
        } else {
            lifecycleEvents = List.of();
        }

        return new HrOverviewResponse(
                employees.size(),
                countEmployeesByStatus(employees, "ACTIVE"),
                countEmployeesByStatus(employees, "PROBATION"),
                countEmployeesByStatus(employees, "RESIGNED"),
                countVisibleDepartments(dataScope),
                countContractsByStatus(contracts, Set.of("ACTIVE", "EXPIRING")),
                countExpiringContracts(contracts, today, upcomingDate),
                countProbationDueEmployees(employees, today, upcomingDate),
                upcomingDate,
                upcomingDate,
                employmentStatusStats(employees),
                departmentStats(employees),
                lifecycleEventStats(lifecycleEvents),
                LocalDateTime.now()
        );
    }

    private long countVisibleDepartments(DataScopeContext dataScope) {
        if (dataScope.isAll()) {
            return deptMapper.selectCount(new LambdaQueryWrapper<SystemDeptEntity>());
        }
        return dataScope.deptIds().size();
    }

    private long countEmployeesByStatus(List<HrEmployeeEntity> employees, String status) {
        return employees.stream()
                .filter(employee -> status.equals(employee.getEmploymentStatus()))
                .count();
    }

    private long countContractsByStatus(List<HrEmployeeContractEntity> contracts, Set<String> statuses) {
        return contracts.stream()
                .filter(contract -> statuses.contains(contract.getStatus()))
                .count();
    }

    private long countExpiringContracts(
            List<HrEmployeeContractEntity> contracts,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return contracts.stream()
                .filter(contract -> Set.of("ACTIVE", "EXPIRING").contains(contract.getStatus()))
                .filter(contract -> contract.getEndDate() != null)
                .filter(contract -> !contract.getEndDate().isBefore(startDate)
                        && !contract.getEndDate().isAfter(endDate))
                .count();
    }

    private long countProbationDueEmployees(
            List<HrEmployeeEntity> employees,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return employees.stream()
                .filter(employee -> "PROBATION".equals(employee.getEmploymentStatus()))
                .filter(employee -> employee.getProbationEndDate() != null)
                .filter(employee -> !employee.getProbationEndDate().isBefore(startDate)
                        && !employee.getProbationEndDate().isAfter(endDate))
                .count();
    }

    private List<HrOverviewResponse.MetricItem> employmentStatusStats(List<HrEmployeeEntity> employees) {
        Map<String, Long> grouped = employees.stream()
                .filter(employee -> employee.getEmploymentStatus() != null)
                .collect(Collectors.groupingBy(HrEmployeeEntity::getEmploymentStatus, Collectors.counting()));
        return EMPLOYMENT_STATUS_NAMES.entrySet()
                .stream()
                .map(entry -> new HrOverviewResponse.MetricItem(
                        entry.getKey(),
                        entry.getValue(),
                        grouped.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }

    private List<HrOverviewResponse.MetricItem> departmentStats(List<HrEmployeeEntity> employees) {
        Map<Long, Long> grouped = employees.stream()
                .filter(employee -> employee.getDeptId() != null)
                .collect(Collectors.groupingBy(HrEmployeeEntity::getDeptId, Collectors.counting()));
        if (grouped.isEmpty()) {
            return List.of();
        }
        Map<Long, String> deptNames = deptMapper.selectBatchIds(grouped.keySet())
                .stream()
                .collect(Collectors.toMap(SystemDeptEntity::getId, SystemDeptEntity::getName));
        return grouped.entrySet()
                .stream()
                .map(entry -> new HrOverviewResponse.MetricItem(
                        String.valueOf(entry.getKey()),
                        deptNames.getOrDefault(entry.getKey(), "未命名部门"),
                        entry.getValue()
                ))
                .sorted(Comparator.comparingLong(HrOverviewResponse.MetricItem::value).reversed()
                        .thenComparing(HrOverviewResponse.MetricItem::name))
                .limit(8)
                .toList();
    }

    private List<HrOverviewResponse.MetricItem> lifecycleEventStats(List<HrEmployeeLifecycleEventEntity> events) {
        Map<String, Long> grouped = events.stream()
                .filter(event -> event.getEventType() != null)
                .collect(Collectors.groupingBy(HrEmployeeLifecycleEventEntity::getEventType, Collectors.counting()));
        return LIFECYCLE_EVENT_NAMES.entrySet()
                .stream()
                .map(entry -> new HrOverviewResponse.MetricItem(
                        entry.getKey(),
                        entry.getValue(),
                        grouped.getOrDefault(entry.getKey(), 0L)
                ))
                .toList();
    }

    private static Map<String, String> orderedMap(String... entries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(entries[index], entries[index + 1]);
        }
        return result;
    }
}
