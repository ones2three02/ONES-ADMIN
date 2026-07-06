package com.ones.admin.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.hr.dto.HrEmployeeDocumentBindRequest;
import com.ones.admin.hr.dto.HrEmployeeDocumentResponse;
import com.ones.admin.hr.entity.HrEmployeeDocumentEntity;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeDocumentMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.FileAccessService;
import com.ones.admin.system.FileMetadataService;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HrEmployeeDocumentService {

    private static final String DEFAULT_DOCUMENT_TYPE = "OTHER";
    private static final int EXPIRING_SOON_DAYS = 30;

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeDocumentMapper documentMapper;
    private final SystemFileMapper fileMapper;
    private final DataScopeService dataScopeService;
    private final FileAccessService fileAccessService;
    private final FileMetadataService fileMetadataService;

    public HrEmployeeDocumentService(
            HrEmployeeMapper employeeMapper,
            HrEmployeeDocumentMapper documentMapper,
            SystemFileMapper fileMapper,
            DataScopeService dataScopeService,
            FileAccessService fileAccessService,
            FileMetadataService fileMetadataService
    ) {
        this.employeeMapper = employeeMapper;
        this.documentMapper = documentMapper;
        this.fileMapper = fileMapper;
        this.dataScopeService = dataScopeService;
        this.fileAccessService = fileAccessService;
        this.fileMetadataService = fileMetadataService;
    }

    public List<HrEmployeeDocumentResponse> listDocuments(Long employeeId) {
        getVisibleEmployee(employeeId);
        List<HrEmployeeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                        .eq(HrEmployeeDocumentEntity::getEmployeeId, employeeId)
                        .orderByAsc(HrEmployeeDocumentEntity::getExpireDate)
                        .orderByDesc(HrEmployeeDocumentEntity::getId))
                .stream()
                .toList();
        List<HrEmployeeDocumentResponse> responses = new ArrayList<>();
        Set<Long> documentedFileIds = new HashSet<>();
        for (HrEmployeeDocumentEntity document : documents) {
            responses.add(toResponse(document));
            documentedFileIds.add(document.getFileId());
        }
        List<SystemFileEntity> legacyFiles = fileMapper.selectList(new LambdaQueryWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getBusinessType, HrEmployeeDocumentFileAccessPolicy.BUSINESS_TYPE)
                .eq(SystemFileEntity::getBusinessId, String.valueOf(employeeId))
                .eq(SystemFileEntity::getStatus, FileMetadataService.STATUS_ACTIVE)
                .orderByDesc(SystemFileEntity::getCreatedAt)
                .orderByDesc(SystemFileEntity::getId));
        for (SystemFileEntity legacyFile : legacyFiles) {
            if (!documentedFileIds.contains(legacyFile.getId())) {
                responses.add(toLegacyResponse(employeeId, legacyFile));
            }
        }
        return responses;
    }

    @Transactional
    public HrEmployeeDocumentResponse bindDocument(
            Long employeeId,
            Long fileId,
            HrEmployeeDocumentBindRequest request
    ) {
        getVisibleEmployee(employeeId);
        SystemFileEntity file = fileMetadataService.getRequiredEntity(fileId);
        validateUnbound(file);
        fileAccessService.assertMetadataAllowed(file);
        fileMetadataService.bindBusiness(
                fileId,
                HrEmployeeDocumentFileAccessPolicy.BUSINESS_TYPE,
                String.valueOf(employeeId)
        );
        HrEmployeeDocumentEntity document = new HrEmployeeDocumentEntity();
        document.setEmployeeId(employeeId);
        document.setFileId(fileId);
        document.setDocumentType(normalizeDocumentType(request == null ? null : request.documentType()));
        document.setIssueDate(request == null ? null : request.issueDate());
        document.setExpireDate(request == null ? null : request.expireDate());
        document.setRemark(trimToNull(request == null ? null : request.remark()));
        document.setCreatedBy(currentUserId());
        documentMapper.insert(document);
        return toResponse(documentMapper.selectById(document.getId()));
    }

    @Transactional
    public HrEmployeeDocumentResponse removeDocument(Long employeeId, Long fileId) {
        getVisibleEmployee(employeeId);
        SystemFileEntity file = fileMetadataService.getRequiredEntity(fileId);
        if (!isEmployeeDocument(file, employeeId)) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        HrEmployeeDocumentEntity document = findDocumentByFile(fileId);
        HrEmployeeDocumentResponse response = document == null
                ? toLegacyResponse(employeeId, file)
                : toResponse(document, file);
        LocalDateTime now = LocalDateTime.now();
        fileMapper.update(null, new LambdaUpdateWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getId, fileId)
                .set(SystemFileEntity::getBusinessType, null)
                .set(SystemFileEntity::getBusinessId, null)
                .set(SystemFileEntity::getStatus, FileMetadataService.STATUS_DELETED)
                .set(SystemFileEntity::getDeletedAt, now)
                .set(SystemFileEntity::getUpdatedAt, now));
        if (document != null) {
            documentMapper.deleteById(document.getId());
        }
        SystemFileEntity deletedFile = fileMapper.selectById(fileId);
        return response.withFile(
                deletedFile.getBusinessType(),
                deletedFile.getBusinessId(),
                deletedFile.getStatus(),
                deletedFile.getUpdatedAt(),
                deletedFile.getDeletedAt()
        );
    }

    private HrEmployeeDocumentResponse toResponse(HrEmployeeDocumentEntity document) {
        SystemFileEntity file = fileMapper.selectById(document.getFileId());
        if (file == null || !FileMetadataService.STATUS_ACTIVE.equals(file.getStatus())) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        return toResponse(document, file);
    }

    private HrEmployeeDocumentResponse toResponse(HrEmployeeDocumentEntity document, SystemFileEntity file) {
        LocalDate today = LocalDate.now();
        LocalDate expireDate = document.getExpireDate();
        boolean expired = expireDate != null && expireDate.isBefore(today);
        boolean expiringSoon = expireDate != null
                && !expired
                && !expireDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS));
        return new HrEmployeeDocumentResponse(
                file.getId(),
                document.getId(),
                document.getEmployeeId(),
                document.getFileId(),
                document.getDocumentType(),
                document.getIssueDate(),
                document.getExpireDate(),
                expired,
                expiringSoon,
                document.getRemark(),
                file.getOriginalName(),
                file.getStoredName(),
                file.getUrl(),
                file.getContentType(),
                file.getExtension(),
                file.getSizeBytes(),
                file.getStorageType(),
                file.getBucket(),
                file.getBusinessType(),
                file.getBusinessId(),
                file.getUploadedBy(),
                file.getStatus(),
                file.getCreatedAt(),
                file.getUpdatedAt(),
                file.getDeletedAt()
        );
    }

    private HrEmployeeDocumentResponse toLegacyResponse(Long employeeId, SystemFileEntity file) {
        HrEmployeeDocumentEntity document = new HrEmployeeDocumentEntity();
        document.setEmployeeId(employeeId);
        document.setFileId(file.getId());
        document.setDocumentType(DEFAULT_DOCUMENT_TYPE);
        return toResponse(document, file);
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

    private HrEmployeeDocumentEntity findDocumentByFile(Long fileId) {
        return documentMapper.selectOne(new LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                .eq(HrEmployeeDocumentEntity::getFileId, fileId)
                .last("limit 1"));
    }

    private String normalizeDocumentType(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return DEFAULT_DOCUMENT_TYPE;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }
}
