package com.ones.admin.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.hr.dto.HrEmployeeCreateRequest;
import com.ones.admin.hr.dto.HrEmployeeQuery;
import com.ones.admin.hr.dto.HrEmployeeResponse;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.entity.HrEmployeeJobEntity;
import com.ones.admin.hr.entity.HrEmployeeLifecycleEventEntity;
import com.ones.admin.hr.entity.HrJobGradeEntity;
import com.ones.admin.hr.entity.HrPositionEntity;
import com.ones.admin.hr.mapper.HrEmployeeJobMapper;
import com.ones.admin.hr.mapper.HrEmployeeLifecycleEventMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.hr.mapper.HrJobGradeMapper;
import com.ones.admin.hr.mapper.HrPositionMapper;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class HrEmployeeService {

    private static final Set<String> EMPLOYMENT_TYPES = Set.of("FULL_TIME", "PART_TIME", "INTERN", "OUTSOURCED");
    private static final Set<String> EMPLOYMENT_STATUSES = Set.of("ACTIVE", "PROBATION", "SUSPENDED", "RESIGNED");

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeJobMapper employeeJobMapper;
    private final HrEmployeeLifecycleEventMapper lifecycleEventMapper;
    private final HrPositionMapper positionMapper;
    private final HrJobGradeMapper jobGradeMapper;
    private final SystemDeptMapper deptMapper;
    private final ObjectMapper objectMapper;

    public HrEmployeeService(
            HrEmployeeMapper employeeMapper,
            HrEmployeeJobMapper employeeJobMapper,
            HrEmployeeLifecycleEventMapper lifecycleEventMapper,
            HrPositionMapper positionMapper,
            HrJobGradeMapper jobGradeMapper,
            SystemDeptMapper deptMapper,
            ObjectMapper objectMapper
    ) {
        this.employeeMapper = employeeMapper;
        this.employeeJobMapper = employeeJobMapper;
        this.lifecycleEventMapper = lifecycleEventMapper;
        this.positionMapper = positionMapper;
        this.jobGradeMapper = jobGradeMapper;
        this.deptMapper = deptMapper;
        this.objectMapper = objectMapper;
    }

    public PageResult<HrEmployeeResponse> queryEmployees(HrEmployeeQuery query) {
        IPage<HrEmployeeEntity> page = employeeMapper.selectPage(query.toMyBatisPage(), buildEmployeeQuery(query));
        List<HrEmployeeResponse> records = page.getRecords()
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(page, records);
    }

    public HrEmployeeResponse getEmployee(Long id) {
        return toResponse(getRequiredEmployee(id));
    }

    @Transactional
    public HrEmployeeResponse createEmployee(HrEmployeeCreateRequest request) {
        String employeeNo = normalizeCode(request.employeeNo());
        assertEmployeeNoAvailable(employeeNo);
        Long deptId = requireEnabledDept(request.deptId());
        Long positionId = requireEnabledPosition(request.positionId());
        Long gradeId = requireEnabledGrade(request.gradeId());
        Long managerEmployeeId = requireActiveManager(request.managerEmployeeId(), null);
        String employmentType = normalizeEnum(request.employmentType(), "FULL_TIME", EMPLOYMENT_TYPES);
        String employmentStatus = normalizeEnum(request.employmentStatus(), "PROBATION", EMPLOYMENT_STATUSES);

        HrEmployeeEntity employee = new HrEmployeeEntity();
        employee.setEmployeeNo(employeeNo);
        employee.setRealName(request.realName().trim());
        employee.setPreferredName(normalizeNullable(request.preferredName()));
        employee.setGender(normalizeNullable(request.gender()));
        employee.setMobile(normalizeNullable(request.mobile()));
        employee.setEmail(normalizeNullable(request.email()));
        employee.setIdCardMasked(maskIdCard(request.idCardNumber()));
        employee.setIdCardEncrypted(null);
        employee.setUserId(request.userId());
        employee.setDeptId(deptId);
        employee.setPositionId(positionId);
        employee.setGradeId(gradeId);
        employee.setManagerEmployeeId(managerEmployeeId);
        employee.setEmploymentType(employmentType);
        employee.setEmploymentStatus(employmentStatus);
        employee.setHireDate(request.hireDate());
        employee.setProbationEndDate(request.probationEndDate());
        employee.setRemark(normalizeNullable(request.remark()));
        employeeMapper.insert(employee);

        createInitialJob(employee);
        createLifecycleEvent(employee);
        return toResponse(employeeMapper.selectById(employee.getId()));
    }

    private LambdaQueryWrapper<HrEmployeeEntity> buildEmployeeQuery(HrEmployeeQuery query) {
        LambdaQueryWrapper<HrEmployeeEntity> wrapper = new LambdaQueryWrapper<HrEmployeeEntity>()
                .orderByDesc(HrEmployeeEntity::getCreatedAt)
                .orderByDesc(HrEmployeeEntity::getId);
        if (hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(nested -> nested
                    .like(HrEmployeeEntity::getEmployeeNo, keyword)
                    .or()
                    .like(HrEmployeeEntity::getRealName, keyword)
                    .or()
                    .like(HrEmployeeEntity::getMobile, keyword)
                    .or()
                    .like(HrEmployeeEntity::getEmail, keyword));
        }
        if (query.getDeptId() != null) {
            wrapper.eq(HrEmployeeEntity::getDeptId, query.getDeptId());
        }
        if (query.getPositionId() != null) {
            wrapper.eq(HrEmployeeEntity::getPositionId, query.getPositionId());
        }
        if (query.getGradeId() != null) {
            wrapper.eq(HrEmployeeEntity::getGradeId, query.getGradeId());
        }
        if (hasText(query.getEmploymentStatus())) {
            wrapper.eq(HrEmployeeEntity::getEmploymentStatus,
                    normalizeEnum(query.getEmploymentStatus(), null, EMPLOYMENT_STATUSES));
        }
        return wrapper;
    }

    private void createInitialJob(HrEmployeeEntity employee) {
        HrEmployeeJobEntity job = new HrEmployeeJobEntity();
        job.setEmployeeId(employee.getId());
        job.setDeptId(employee.getDeptId());
        job.setPositionId(employee.getPositionId());
        job.setGradeId(employee.getGradeId());
        job.setManagerEmployeeId(employee.getManagerEmployeeId());
        job.setEmploymentType(employee.getEmploymentType());
        job.setEffectiveDate(employee.getHireDate());
        job.setChangeReason("员工入职初始化任职记录");
        employeeJobMapper.insert(job);
    }

    private void createLifecycleEvent(HrEmployeeEntity employee) {
        HrEmployeeLifecycleEventEntity event = new HrEmployeeLifecycleEventEntity();
        event.setEmployeeId(employee.getId());
        event.setEventType("ONBOARD");
        event.setEventDate(employee.getHireDate());
        event.setAfterStatus(employee.getEmploymentStatus());
        event.setSummary("员工入职");
        event.setDetailJson(writeLifecycleDetail(employee));
        event.setCreatedBy(currentUserId());
        lifecycleEventMapper.insert(event);
    }

    private String writeLifecycleDetail(HrEmployeeEntity employee) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("employeeNo", employee.getEmployeeNo());
        detail.put("realName", employee.getRealName());
        detail.put("deptId", employee.getDeptId());
        detail.put("positionId", employee.getPositionId());
        detail.put("gradeId", employee.getGradeId());
        detail.put("employmentType", employee.getEmploymentType());
        detail.put("employmentStatus", employee.getEmploymentStatus());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("员工生命周期事件序列化失败");
        }
    }

    private void assertEmployeeNoAvailable(String employeeNo) {
        Long count = employeeMapper.selectCount(new LambdaQueryWrapper<HrEmployeeEntity>()
                .eq(HrEmployeeEntity::getEmployeeNo, employeeNo));
        if (count > 0) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_NO_EXISTS);
        }
    }

    private HrEmployeeEntity getRequiredEmployee(Long id) {
        HrEmployeeEntity employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employee;
    }

    private Long requireEnabledDept(Long deptId) {
        SystemDeptEntity dept = deptId == null ? null : deptMapper.selectById(deptId);
        if (dept == null || !Boolean.TRUE.equals(dept.getEnabled())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_DEPT_NOT_AVAILABLE);
        }
        return deptId;
    }

    private Long requireEnabledPosition(Long positionId) {
        if (positionId == null) {
            return null;
        }
        HrPositionEntity position = positionMapper.selectById(positionId);
        if (position == null || !Boolean.TRUE.equals(position.getEnabled())) {
            throw new BusinessException(HrErrorCode.POSITION_NOT_AVAILABLE);
        }
        return positionId;
    }

    private Long requireEnabledGrade(Long gradeId) {
        if (gradeId == null) {
            return null;
        }
        HrJobGradeEntity grade = jobGradeMapper.selectById(gradeId);
        if (grade == null || !Boolean.TRUE.equals(grade.getEnabled())) {
            throw new BusinessException(HrErrorCode.JOB_GRADE_NOT_AVAILABLE);
        }
        return gradeId;
    }

    private Long requireActiveManager(Long managerEmployeeId, Long currentEmployeeId) {
        if (managerEmployeeId == null) {
            return null;
        }
        if (managerEmployeeId.equals(currentEmployeeId)) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_MANAGER_CANNOT_BE_SELF);
        }
        HrEmployeeEntity manager = employeeMapper.selectById(managerEmployeeId);
        if (manager == null || "RESIGNED".equals(manager.getEmploymentStatus())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_MANAGER_NOT_AVAILABLE);
        }
        return managerEmployeeId;
    }

    private HrEmployeeResponse toResponse(HrEmployeeEntity employee) {
        SystemDeptEntity dept = employee.getDeptId() == null ? null : deptMapper.selectById(employee.getDeptId());
        HrPositionEntity position = employee.getPositionId() == null ? null : positionMapper.selectById(employee.getPositionId());
        HrJobGradeEntity grade = employee.getGradeId() == null ? null : jobGradeMapper.selectById(employee.getGradeId());
        HrEmployeeEntity manager = employee.getManagerEmployeeId() == null
                ? null
                : employeeMapper.selectById(employee.getManagerEmployeeId());
        return new HrEmployeeResponse(
                employee.getId(),
                employee.getEmployeeNo(),
                employee.getRealName(),
                employee.getPreferredName(),
                employee.getGender(),
                employee.getMobile(),
                employee.getEmail(),
                employee.getIdCardMasked(),
                employee.getUserId(),
                employee.getDeptId(),
                dept == null ? null : dept.getName(),
                employee.getPositionId(),
                position == null ? null : position.getPositionName(),
                employee.getGradeId(),
                grade == null ? null : grade.getGradeName(),
                employee.getManagerEmployeeId(),
                manager == null ? null : manager.getRealName(),
                employee.getEmploymentType(),
                employee.getEmploymentStatus(),
                employee.getHireDate(),
                employee.getProbationEndDate(),
                employee.getLeaveDate(),
                employee.getRemark(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String normalizeEnum(String value, String defaultValue, Set<String> allowedValues) {
        String normalized = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
        if (normalized == null || !allowedValues.contains(normalized)) {
            throw new BusinessException(HrErrorCode.HR_ENUM_INVALID);
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String maskIdCard(String idCardNumber) {
        if (!hasText(idCardNumber)) {
            return null;
        }
        String normalized = idCardNumber.trim();
        if (normalized.length() <= 4) {
            return "****";
        }
        String suffix = normalized.substring(normalized.length() - 4);
        int prefixLength = Math.min(3, normalized.length() - 4);
        return normalized.substring(0, prefixLength) + "****" + suffix;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
