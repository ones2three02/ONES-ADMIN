package com.ones.admin.hr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.dto.HrEmployeeContractResponse;
import com.ones.admin.hr.dto.HrEmployeeContractSaveRequest;
import com.ones.admin.hr.entity.HrEmployeeContractEntity;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeContractMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HrEmployeeContractService {

    private static final Set<String> CONTRACT_TYPES = Set.of("FIXED_TERM", "OPEN_ENDED", "INTERNSHIP", "SERVICE");
    private static final Set<String> CONTRACT_STATUSES = Set.of("DRAFT", "ACTIVE", "EXPIRING", "TERMINATED");

    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeMapper employeeMapper;

    public HrEmployeeContractService(
            HrEmployeeContractMapper contractMapper,
            HrEmployeeMapper employeeMapper
    ) {
        this.contractMapper = contractMapper;
        this.employeeMapper = employeeMapper;
    }

    public List<HrEmployeeContractResponse> listContracts(Long employeeId) {
        HrEmployeeEntity employee = getRequiredEmployee(employeeId);
        return contractMapper.selectList(new LambdaQueryWrapper<HrEmployeeContractEntity>()
                        .eq(HrEmployeeContractEntity::getEmployeeId, employeeId)
                        .orderByDesc(HrEmployeeContractEntity::getStartDate)
                        .orderByDesc(HrEmployeeContractEntity::getId))
                .stream()
                .map(contract -> toResponse(contract, employee))
                .toList();
    }

    @Transactional
    public HrEmployeeContractResponse createContract(Long employeeId, HrEmployeeContractSaveRequest request) {
        HrEmployeeEntity employee = getRequiredEmployee(employeeId);
        String contractNo = normalizeCode(request.contractNo());
        assertContractNoAvailable(contractNo, null);

        HrEmployeeContractEntity contract = new HrEmployeeContractEntity();
        contract.setEmployeeId(employee.getId());
        contract.setContractNo(contractNo);
        fillContract(contract, request);
        contractMapper.insert(contract);
        return toResponse(contractMapper.selectById(contract.getId()), employee);
    }

    @Transactional
    public HrEmployeeContractResponse updateContract(
            Long employeeId,
            Long contractId,
            HrEmployeeContractSaveRequest request
    ) {
        HrEmployeeEntity employee = getRequiredEmployee(employeeId);
        HrEmployeeContractEntity contract = getRequiredContract(employeeId, contractId);
        String contractNo = normalizeCode(request.contractNo());
        assertContractNoAvailable(contractNo, contractId);

        contract.setContractNo(contractNo);
        fillContract(contract, request);
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        return toResponse(contractMapper.selectById(contractId), employee);
    }

    private void fillContract(HrEmployeeContractEntity contract, HrEmployeeContractSaveRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_CONTRACT_DATE_INVALID);
        }
        contract.setContractType(normalizeEnum(request.contractType(), CONTRACT_TYPES));
        contract.setStatus(normalizeEnum(request.status(), CONTRACT_STATUSES));
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setProbationMonths(request.probationMonths());
        contract.setRenewalRemindDate(request.renewalRemindDate());
        contract.setAttachmentFileId(request.attachmentFileId());
        contract.setRemark(normalizeNullable(request.remark()));
    }

    private HrEmployeeEntity getRequiredEmployee(Long employeeId) {
        HrEmployeeEntity employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_NOT_FOUND);
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
