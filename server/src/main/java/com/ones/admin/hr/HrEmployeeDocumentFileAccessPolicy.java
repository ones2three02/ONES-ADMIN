package com.ones.admin.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.FileBusinessAccessPolicy;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemFileEntity;
import org.springframework.stereotype.Component;

@Component
public class HrEmployeeDocumentFileAccessPolicy implements FileBusinessAccessPolicy {

    static final String BUSINESS_TYPE = "HR_EMPLOYEE_DOCUMENT";
    private static final String EMPLOYEE_DETAIL_PERMISSION = "hr:employee:detail";

    private final DataScopeService dataScopeService;
    private final HrEmployeeMapper employeeMapper;

    public HrEmployeeDocumentFileAccessPolicy(
            DataScopeService dataScopeService,
            HrEmployeeMapper employeeMapper
    ) {
        this.dataScopeService = dataScopeService;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public void assertDownloadAllowed(SystemFileEntity file) {
        if (!StpUtil.hasPermission(EMPLOYEE_DETAIL_PERMISSION)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        Long employeeId = parseBusinessId(file.getBusinessId());
        HrEmployeeEntity employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        if (!dataScopeService.canAccess(employee.getDeptId(), employee.getUserId())) {
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
