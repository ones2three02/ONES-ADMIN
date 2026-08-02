package com.ones.admin.hr;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.web.CsvExportUtils;
import com.ones.admin.hr.dto.HrEmployeeDocumentBindRequest;
import com.ones.admin.hr.dto.HrEmployeeDocumentResponse;
import com.ones.admin.hr.entity.HrEmployeeDocumentEntity;
import com.ones.admin.hr.entity.HrEmployeeEntity;
import com.ones.admin.hr.mapper.HrEmployeeDocumentMapper;
import com.ones.admin.hr.mapper.HrEmployeeMapper;
import com.ones.admin.system.DataScopeContext;
import com.ones.admin.system.DataScopeService;
import com.ones.admin.system.FileAccessService;
import com.ones.admin.system.FileMetadataService;
import com.ones.admin.system.SystemErrorCode;
import com.ones.admin.system.entity.SystemDeptEntity;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemDeptMapper;
import com.ones.admin.system.mapper.SystemFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HrEmployeeDocumentService {

    private static final String DEFAULT_DOCUMENT_TYPE = "OTHER";
    private static final int EXPIRING_SOON_DAYS = 30;
    private static final String EXPIRING_DOCUMENT_CSV_HEADER = "employeeNo,realName,deptName,documentType,"
            + "originalName,issueDate,expireDate,expired,expiringSoon,remark,fileId,documentId";

    private final HrEmployeeMapper employeeMapper;
    private final HrEmployeeDocumentMapper documentMapper;
    private final SystemFileMapper fileMapper;
    private final SystemDeptMapper deptMapper;
    private final DataScopeService dataScopeService;
    private final FileAccessService fileAccessService;
    private final FileMetadataService fileMetadataService;

    public HrEmployeeDocumentService(
            HrEmployeeMapper employeeMapper,
            HrEmployeeDocumentMapper documentMapper,
            SystemFileMapper fileMapper,
            SystemDeptMapper deptMapper,
            DataScopeService dataScopeService,
            FileAccessService fileAccessService,
            FileMetadataService fileMetadataService
    ) {
        this.employeeMapper = employeeMapper;
        this.documentMapper = documentMapper;
        this.fileMapper = fileMapper;
        this.deptMapper = deptMapper;
        this.dataScopeService = dataScopeService;
        this.fileAccessService = fileAccessService;
        this.fileMetadataService = fileMetadataService;
    }

    public List<HrEmployeeDocumentResponse> listDocuments(Long employeeId) {
        HrEmployeeEntity employee = getVisibleEmployee(employeeId);
        List<HrEmployeeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                        .eq(HrEmployeeDocumentEntity::getEmployeeId, employeeId)
                        .orderByAsc(HrEmployeeDocumentEntity::getExpireDate)
                        .orderByDesc(HrEmployeeDocumentEntity::getId))
                .stream()
                .toList();
        List<HrEmployeeDocumentResponse> responses = new ArrayList<>();
        Set<Long> documentedFileIds = new HashSet<>();
        for (HrEmployeeDocumentEntity document : documents) {
            responses.add(toResponse(document, employee));
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
                responses.add(toLegacyResponse(employee, legacyFile));
            }
        }
        return responses;
    }

    public List<HrEmployeeDocumentResponse> listExpiringDocuments(Integer days) {
        int normalizedDays = days == null ? EXPIRING_SOON_DAYS : days;
        if (normalizedDays < 0 || normalizedDays > 365) {
            throw new BusinessException(HrErrorCode.EMPLOYEE_DOCUMENT_EXPIRING_DAYS_INVALID);
        }
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(normalizedDays);
        List<HrEmployeeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                .isNotNull(HrEmployeeDocumentEntity::getExpireDate)
                .ge(HrEmployeeDocumentEntity::getExpireDate, today)
                .le(HrEmployeeDocumentEntity::getExpireDate, endDate)
                .orderByAsc(HrEmployeeDocumentEntity::getExpireDate)
                .orderByAsc(HrEmployeeDocumentEntity::getId));
        Set<Long> activeFileIds = activeFileIds(documents);
        Map<Long, HrEmployeeEntity> employees = employeesById(documents.stream()
                .map(HrEmployeeDocumentEntity::getEmployeeId)
                .collect(Collectors.toSet()));
        DataScopeContext dataScope = dataScopeService.currentContext();
        return documents.stream()
                .filter(document -> activeFileIds.contains(document.getFileId()))
                .filter(document -> {
                    HrEmployeeEntity employee = employees.get(document.getEmployeeId());
                    return employee != null && dataScope.canAccess(employee.getDeptId(), employee.getUserId());
                })
                .map(document -> toResponse(document, employees.get(document.getEmployeeId())))
                .toList();
    }

    public String exportExpiringDocuments(Integer days) {
        StringBuilder csv = new StringBuilder(EXPIRING_DOCUMENT_CSV_HEADER).append('\n');
        listExpiringDocuments(days)
                .forEach(document -> csv.append(CsvExportUtils.row(
                        document.employeeNo(),
                        document.realName(),
                        document.deptName(),
                        document.documentType(),
                        document.originalName(),
                        document.issueDate(),
                        document.expireDate(),
                        document.expired(),
                        document.expiringSoon(),
                        document.remark(),
                        document.fileId(),
                        document.documentId()
                )).append('\n'));
        return csv.toString();
    }

    @Transactional
    public HrEmployeeDocumentResponse bindDocument(
            Long employeeId,
            Long fileId,
            HrEmployeeDocumentBindRequest request
    ) {
        HrEmployeeEntity employee = getVisibleEmployee(employeeId);
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
        return toResponse(documentMapper.selectById(document.getId()), employee);
    }

    @Transactional
    public HrEmployeeDocumentResponse removeDocument(Long employeeId, Long fileId) {
        HrEmployeeEntity employee = getVisibleEmployee(employeeId);
        SystemFileEntity file = fileMetadataService.getRequiredEntity(fileId);
        if (!isEmployeeDocument(file, employeeId)) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        HrEmployeeDocumentEntity document = findDocumentByFile(fileId);
        HrEmployeeDocumentResponse response = document == null
                ? toLegacyResponse(employee, file)
                : toResponse(document, file, employee);
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

    long countExpiringDocuments(Set<Long> visibleEmployeeIds, LocalDate startDate, LocalDate endDate) {
        if (visibleEmployeeIds.isEmpty()) {
            return 0L;
        }
        List<HrEmployeeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                .in(HrEmployeeDocumentEntity::getEmployeeId, visibleEmployeeIds)
                .isNotNull(HrEmployeeDocumentEntity::getExpireDate)
                .ge(HrEmployeeDocumentEntity::getExpireDate, startDate)
                .le(HrEmployeeDocumentEntity::getExpireDate, endDate));
        return countActiveDocumentFiles(documents);
    }

    long countExpiredDocuments(Set<Long> visibleEmployeeIds, LocalDate today) {
        if (visibleEmployeeIds.isEmpty()) {
            return 0L;
        }
        List<HrEmployeeDocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<HrEmployeeDocumentEntity>()
                .in(HrEmployeeDocumentEntity::getEmployeeId, visibleEmployeeIds)
                .isNotNull(HrEmployeeDocumentEntity::getExpireDate)
                .lt(HrEmployeeDocumentEntity::getExpireDate, today));
        return countActiveDocumentFiles(documents);
    }

    private long countActiveDocumentFiles(List<HrEmployeeDocumentEntity> documents) {
        return activeFileIds(documents).size();
    }

    private Set<Long> activeFileIds(List<HrEmployeeDocumentEntity> documents) {
        if (documents.isEmpty()) {
            return Set.of();
        }
        Set<Long> fileIds = documents.stream()
                .map(HrEmployeeDocumentEntity::getFileId)
                .collect(Collectors.toSet());
        return fileMapper.selectList(new LambdaQueryWrapper<SystemFileEntity>()
                        .in(SystemFileEntity::getId, fileIds)
                        .eq(SystemFileEntity::getStatus, FileMetadataService.STATUS_ACTIVE))
                .stream()
                .map(SystemFileEntity::getId)
                .collect(Collectors.toSet());
    }

    private HrEmployeeDocumentResponse toResponse(HrEmployeeDocumentEntity document, HrEmployeeEntity employee) {
        SystemFileEntity file = fileMapper.selectById(document.getFileId());
        if (file == null || !FileMetadataService.STATUS_ACTIVE.equals(file.getStatus())) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        return toResponse(document, file, employee);
    }

    private HrEmployeeDocumentResponse toResponse(
            HrEmployeeDocumentEntity document,
            SystemFileEntity file,
            HrEmployeeEntity employee
    ) {
        LocalDate today = LocalDate.now();
        LocalDate expireDate = document.getExpireDate();
        boolean expired = expireDate != null && expireDate.isBefore(today);
        boolean expiringSoon = expireDate != null
                && !expired
                && !expireDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS));
        SystemDeptEntity dept = employee.getDeptId() == null ? null : deptMapper.selectById(employee.getDeptId());
        return new HrEmployeeDocumentResponse(
                file.getId(),
                document.getId(),
                document.getEmployeeId(),
                employee.getEmployeeNo(),
                employee.getRealName(),
                dept == null ? null : dept.getName(),
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

    private HrEmployeeDocumentResponse toLegacyResponse(HrEmployeeEntity employee, SystemFileEntity file) {
        HrEmployeeDocumentEntity document = new HrEmployeeDocumentEntity();
        document.setEmployeeId(employee.getId());
        document.setFileId(file.getId());
        document.setDocumentType(DEFAULT_DOCUMENT_TYPE);
        return toResponse(document, file, employee);
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

    private Map<Long, HrEmployeeEntity> employeesById(Collection<Long> employeeIds) {
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(employeeIds)
                .stream()
                .collect(Collectors.toMap(HrEmployeeEntity::getId, Function.identity()));
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
