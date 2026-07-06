package com.ones.admin.hr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.dto.HrEmployeeContractResponse;
import com.ones.admin.hr.dto.HrEmployeeContractSaveRequest;
import com.ones.admin.hr.dto.HrEmployeeContractTerminateRequest;
import com.ones.admin.hr.entity.HrEmployeeContractEntity;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeContractMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.FileMetadataService;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemFileEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HrEmployeeContractService {

    private static final String CONTRACT_ATTACHMENT_BUSINESS_TYPE = "HR_EMPLOYEE_CONTRACT";
    private static final Set<String> CONTRACT_TYPES = Set.of("FIXED_TERM", "OPEN_ENDED", "INTERNSHIP", "SERVICE");
    private static final Set<String> CONTRACT_STATUSES = Set.of("DRAFT", "ACTIVE", "EXPIRING", "TERMINATED");

    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeMapper employeeMapper;
    private final DataScopeService dataScopeService;
    private final FileMetadataService fileMetadataService;

    public HrEmployeeContractService(
            HrEmployeeContractMapper contractMapper,
            HrEmployeeMapper employeeMapper,
            DataScopeService dataScopeService,
            FileMetadataService fileMetadataService
    ) {
        this.contractMapper = contractMapper;
        this.employeeMapper = employeeMapper;
        this.dataScopeService = dataScopeService;
        this.fileMetadataService = fileMetadataService;
    }

    public List<HrEmployeeContractResponse> listContracts(Long employeeId) {
        HrEmployeeEntity employee = getVisibleEmployee(employeeId);
        return contractMapper.selectList(new LambdaQueryWrapper<HrEmployeeContractEntity>()
                        .eq(HrEmployeeContractEntity::getEmployeeId, employeeId)
                        .orderByDesc(HrEmployeeContractEntity::getStartDate)
                        .orderByDesc(HrEmployeeContractEntity::getId))
                .stream()
                .map(contract -> toResponse(contract, employee))
                .toList();
    }

    public List<HrEmployeeContractResponse> listExpiringContracts(Integer days) {
        int normalizedDays = days == null ? 30 : days;
        if (normalizedDays < 0 || normalizedDays > 365) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_EXPIRING_DAYS_INVALID);
        }
        LocalDate today = LocalDate.now();
        List<HrEmployeeContractEntity> contracts = contractMapper.selectList(new LambdaQueryWrapper<HrEmployeeContractEntity>()
                .in(HrEmployeeContractEntity::getStatus, Set.of("ACTIVE", "EXPIRING"))
                .isNotNull(HrEmployeeContractEntity::getEndDate)
                .ge(HrEmployeeContractEntity::getEndDate, today)
                .le(HrEmployeeContractEntity::getEndDate, today.plusDays(normalizedDays))
                .orderByAsc(HrEmployeeContractEntity::getEndDate)
                .orderByAsc(HrEmployeeContractEntity::getId));
        Map<Long, HrEmployeeEntity> employees = employeesById(contracts.stream()
                .map(HrEmployeeContractEntity::getEmployeeId)
                .collect(Collectors.toCollection(HashSet::new)));
        return contracts.stream()
                .filter(contract -> {
                    HrEmployeeEntity employee = employees.get(contract.getEmployeeId());
                    return employee != null && dataScopeService.canAccess(employee.getDeptId(), employee.getUserId());
                })
                .map(contract -> toResponse(contract, employees.get(contract.getEmployeeId())))
                .toList();
    }

    @Transactional
    public HrEmployeeContractResponse createContract(Long employeeId, HrEmployeeContractSaveRequest request) {
        HrEmployeeEntity employee = getVisibleEmployee(employeeId);
        String contractNo = normalizeCode(request.contractNo());
        assertContractNoAvailable(contractNo, null);

        HrEmployeeContractEntity contract = new HrEmployeeContractEntity();
        contract.setEmployeeId(employee.getId());
        contract.setContractNo(contractNo);
        fillContract(contract, request, null);
        contractMapper.insert(contract);
        bindContractAttachment(contract);
        return toResponse(contractMapper.selectById(contract.getId()), employee);
    }

    @Transactional
    public HrEmployeeContractResponse updateContract(
            Long employeeId,
            Long contractId,
            HrEmployeeContractSaveRequest request
    ) {
        HrEmployeeEntity employee = getVisibleEmployee(employeeId);
        HrEmployeeContractEntity contract = getRequiredContract(employeeId, contractId);
        String contractNo = normalizeCode(request.contractNo());
        assertContractNoAvailable(contractNo, contractId);

        contract.setContractNo(contractNo);
        Long previousAttachmentFileId = contract.getAttachmentFileId();
        fillContract(contract, request, contract.getId());
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        syncContractAttachment(previousAttachmentFileId, contract);
        return toResponse(contractMapper.selectById(contractId), employee);
    }

    @Transactional
    public HrEmployeeContractResponse terminateContract(
            Long contractId,
            HrEmployeeContractTerminateRequest request
    ) {
        HrEmployeeContractEntity contract = getRequiredContract(contractId);
        getVisibleEmployee(contract.getEmployeeId());
        if ("TERMINATED".equals(contract.getStatus())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_ALREADY_TERMINATED);
        }
        if (request.terminateDate().isBefore(contract.getStartDate())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_DATE_INVALID);
        }

        contract.setStatus("TERMINATED");
        contract.setEndDate(request.terminateDate());
        contract.setRemark(normalizeNullable(request.reason()));
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        return toResponse(contractMapper.selectById(contractId), getVisibleEmployee(contract.getEmployeeId()));
    }

    private void fillContract(HrEmployeeContractEntity contract, HrEmployeeContractSaveRequest request, Long contractId) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_DATE_INVALID);
        }
        validateAttachmentFile(request.attachmentFileId(), contractId);
        contract.setContractType(normalizeEnum(request.contractType(), CONTRACT_TYPES));
        contract.setStatus(normalizeEnum(request.status(), CONTRACT_STATUSES));
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setProbationMonths(request.probationMonths());
        contract.setRenewalRemindDate(request.renewalRemindDate());
        contract.setAttachmentFileId(request.attachmentFileId());
        contract.setRemark(normalizeNullable(request.remark()));
    }

    private void validateAttachmentFile(Long attachmentFileId, Long contractId) {
        if (attachmentFileId == null) {
            return;
        }
        SystemFileEntity file = fileMetadataService.getRequiredEntity(attachmentFileId);
        String businessType = file.getBusinessType();
        String businessId = file.getBusinessId();
        if (businessType == null && businessId == null) {
            return;
        }
        if (CONTRACT_ATTACHMENT_BUSINESS_TYPE.equals(businessType)
                && contractId != null
                && String.valueOf(contractId).equals(businessId)) {
            return;
        }
        throw new BusinessException(SystemErrorCode.FILE_ALREADY_BOUND);
    }

    private void bindContractAttachment(HrEmployeeContractEntity contract) {
        if (contract.getAttachmentFileId() == null) {
            return;
        }
        fileMetadataService.bindBusiness(
                contract.getAttachmentFileId(),
                CONTRACT_ATTACHMENT_BUSINESS_TYPE,
                String.valueOf(contract.getId())
        );
    }

    private void syncContractAttachment(Long previousAttachmentFileId, HrEmployeeContractEntity contract) {
        Long currentAttachmentFileId = contract.getAttachmentFileId();
        if (java.util.Objects.equals(previousAttachmentFileId, currentAttachmentFileId)) {
            return;
        }
        fileMetadataService.clearBusinessIfMatched(
                previousAttachmentFileId,
                CONTRACT_ATTACHMENT_BUSINESS_TYPE,
                String.valueOf(contract.getId())
        );
        bindContractAttachment(contract);
    }

    private HrEmployeeEntity getRequiredEmployee(Long employeeId) {
        HrEmployeeEntity employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return employee;
    }

    private HrEmployeeEntity getVisibleEmployee(Long employeeId) {
        HrEmployeeEntity employee = getRequiredEmployee(employeeId);
        if (!dataScopeService.canAccess(employee.getDeptId(), employee.getUserId())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_DATA_SCOPE_DENIED);
        }
        return employee;
    }

    private HrEmployeeContractEntity getRequiredContract(Long employeeId, Long contractId) {
        HrEmployeeContractEntity contract = contractMapper.selectOne(new LambdaQueryWrapper<HrEmployeeContractEntity>()
                .eq(HrEmployeeContractEntity::getId, contractId)
                .eq(HrEmployeeContractEntity::getEmployeeId, employeeId)
                .last("limit 1"));
        if (contract == null) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_NOT_FOUND);
        }
        return contract;
    }

    private HrEmployeeContractEntity getRequiredContract(Long contractId) {
        HrEmployeeContractEntity contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_NOT_FOUND);
        }
        return contract;
    }

    private Map<Long, HrEmployeeEntity> employeesById(Collection<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return employeeMapper.selectByIds(employeeIds)
                .stream()
                .collect(Collectors.toMap(HrEmployeeEntity::getId, Function.identity()));
    }

    private void assertContractNoAvailable(String contractNo, Long exceptId) {
        LambdaQueryWrapper<HrEmployeeContractEntity> wrapper = new LambdaQueryWrapper<HrEmployeeContractEntity>()
                .eq(HrEmployeeContractEntity::getContractNo, contractNo);
        if (exceptId != null) {
            wrapper.ne(HrEmployeeContractEntity::getId, exceptId);
        }
        if (contractMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_NO_EXISTS);
        }
    }

    private HrEmployeeContractResponse toResponse(HrEmployeeContractEntity contract, HrEmployeeEntity employee) {
        return new HrEmployeeContractResponse(
                contract.getId(),
                contract.getEmployeeId(),
                employee.getEmployeeNo(),
                employee.getRealName(),
                contract.getContractNo(),
                contract.getContractType(),
                contract.getStatus(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getProbationMonths(),
                contract.getRenewalRemindDate(),
                contract.getAttachmentFileId(),
                contract.getRemark(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }

    private String normalizeEnum(String value, Set<String> allowedValues) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(HrErrorCode.HR_ENUM_INVALID);
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
