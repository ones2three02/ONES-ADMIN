package com.ones.admin.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.entity.HrEmployeeContractEntity;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeContractMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.FileBusinessAccessPolicy;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemFileEntity;
import org.springframework.stereotype.Component;

@Component
public class HrContractFileAccessPolicy implements FileBusinessAccessPolicy {

    private static final String BUSINESS_TYPE = "HR_EMPLOYEE_CONTRACT";
    private static final String CONTRACT_LIST_PERMISSION = "hr:contract:list";

    private final DataScopeService dataScopeService;
    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeMapper employeeMapper;

    public HrContractFileAccessPolicy(
            DataScopeService dataScopeService,
            HrEmployeeContractMapper contractMapper,
            HrEmployeeMapper employeeMapper
    ) {
        this.dataScopeService = dataScopeService;
        this.contractMapper = contractMapper;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public void assertDownloadAllowed(SystemFileEntity file) {
        if (!StpUtil.hasPermission(CONTRACT_LIST_PERMISSION)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        Long contractId = parseBusinessId(file.getBusinessId());
        HrEmployeeContractEntity contract = contractId == null ? null : contractMapper.selectById(contractId);
        if (contract == null || !file.getId().equals(contract.getAttachmentFileId())) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        HrEmployeeEntity employee = employeeMapper.selectById(contract.getEmployeeId());
        if (employee == null || !dataScopeService.canAccess(employee.getDeptId(), employee.getUserId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    @Override
    public boolean supports(SystemFileEntity file) {
        return file != null && BUSINESS_TYPE.equals(file.getBusinessType());
    }

    private Long parseBusinessId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
