package com.ones.admin.hr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.FileAccessService;
import com.ones.admin.system.FileMetadataService;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.dto.FileMetadataResponse;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HrEmployeeDocumentService {

    private final HrEmployeeMapper employeeMapper;
    private final SystemFileMapper fileMapper;
    private final DataScopeService dataScopeService;
    private final FileAccessService fileAccessService;
    private final FileMetadataService fileMetadataService;

    public HrEmployeeDocumentService(
            HrEmployeeMapper employeeMapper,
            SystemFileMapper fileMapper,
            DataScopeService dataScopeService,
            FileAccessService fileAccessService,
            FileMetadataService fileMetadataService
    ) {
        this.employeeMapper = employeeMapper;
        this.fileMapper = fileMapper;
        this.dataScopeService = dataScopeService;
        this.fileAccessService = fileAccessService;
        this.fileMetadataService = fileMetadataService;
    }

    public List<FileMetadataResponse> listDocuments(Long employeeId) {
        getVisibleEmployee(employeeId);
        return fileMapper.selectList(new LambdaQueryWrapper<SystemFileEntity>()
                        .eq(SystemFileEntity::getBusinessType, HrEmployeeDocumentFileAccessPolicy.BUSINESS_TYPE)
                        .eq(SystemFileEntity::getBusinessId, String.valueOf(employeeId))
                        .eq(SystemFileEntity::getStatus, FileMetadataService.STATUS_ACTIVE)
                        .orderByDesc(SystemFileEntity::getCreatedAt)
                        .orderByDesc(SystemFileEntity::getId))
                .stream()
                .map(fileMetadataService::toResponse)
                .toList();
    }

    @Transactional
    public FileMetadataResponse bindDocument(Long employeeId, Long fileId) {
        getVisibleEmployee(employeeId);
        SystemFileEntity file = fileMetadataService.getRequiredEntity(fileId);
        validateUnbound(file);
        fileAccessService.assertMetadataAllowed(file);
        fileMetadataService.bindBusiness(
                fileId,
                HrEmployeeDocumentFileAccessPolicy.BUSINESS_TYPE,
                String.valueOf(employeeId)
        );
        return fileMetadataService.toResponse(fileMetadataService.getRequiredEntity(fileId));
    }

    @Transactional
    public FileMetadataResponse removeDocument(Long employeeId, Long fileId) {
        getVisibleEmployee(employeeId);
        SystemFileEntity file = fileMetadataService.getRequiredEntity(fileId);
        if (!isEmployeeDocument(file, employeeId)) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        fileMapper.update(null, new LambdaUpdateWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getId, fileId)
                .set(SystemFileEntity::getBusinessType, null)
                .set(SystemFileEntity::getBusinessId, null)
                .set(SystemFileEntity::getStatus, FileMetadataService.STATUS_DELETED)
                .set(SystemFileEntity::getDeletedAt, now)
                .set(SystemFileEntity::getUpdatedAt, now));
        return fileMetadataService.toResponse(fileMapper.selectById(fileId));
    }

    private HrEmployeeEntity getVisibleEmployee(Long employeeId) {
        HrEmployeeEntity employee = employeeId == null ? null : employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_NOT_FOUND);
        }
        if (!dataScopeService.canAccess(employee.getDeptId(), employee.getUserId())) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_DATA_SCOPE_DENIED);
        }
        return employee;
    }

    private void validateUnbound(SystemFileEntity file) {
        if (file.getBusinessType() == null && file.getBusinessId() == null) {
            return;
        }
        throw new BusinessException(SystemErrorCode.FILE_ALREADY_BOUND);
    }

    private boolean isEmployeeDocument(SystemFileEntity file, Long employeeId) {
        return HrEmployeeDocumentFileAccessPolicy.BUSINESS_TYPE.equals(file.getBusinessType())
                && String.valueOf(employeeId).equals(file.getBusinessId());
    }
}
