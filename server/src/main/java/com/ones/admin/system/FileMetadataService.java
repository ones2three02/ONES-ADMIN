package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.FileMetadataQuery;
import com.ones.admin.system.dto.FileMetadataResponse;
import com.ones.admin.system.dto.FileRetentionPurgeResponse;
import com.ones.admin.system.dto.FileRetentionSummaryResponse;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class FileMetadataService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELETED = "DELETED";
    public static final String STATUS_PURGED = "PURGED";

    private final FileStorageProperties fileStorageProperties;
    private final FileRetentionProperties fileRetentionProperties;
    private final SystemFileMapper fileMapper;
    private final FileStorageService fileStorageService;

    public FileMetadataService(
            FileStorageProperties fileStorageProperties,
            FileRetentionProperties fileRetentionProperties,
            SystemFileMapper fileMapper,
            FileStorageService fileStorageService
    ) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileRetentionProperties = fileRetentionProperties;
        this.fileMapper = fileMapper;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public FileMetadataResponse create(
            String originalName,
            String storedName,
            String url,
            String contentType,
            String extension,
            long sizeBytes
    ) {
        SystemFileEntity file = new SystemFileEntity();
        file.setOriginalName(originalName);
        file.setStoredName(storedName);
        file.setUrl(url);
        file.setContentType(contentType);
        file.setExtension(extension);
        file.setSizeBytes(sizeBytes);
        file.setStorageType(fileStorageProperties.getStorageType().name());
        file.setBucket(bucketName());
        file.setUploadedBy(currentUserId());
        file.setStatus(STATUS_ACTIVE);
        fileMapper.insert(file);
        return toResponse(file);
    }

    public FileMetadataResponse getRequired(Long fileId) {
        return toResponse(getRequiredEntity(fileId));
    }

    public PageResult<FileMetadataResponse> queryPage(FileMetadataQuery query) {
        IPage<SystemFileEntity> page = fileMapper.selectPage(
                query.toMyBatisPage(),
                buildQueryWrapper(query)
        );
        return PageResult.of(
                page,
                page.getRecords().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    public SystemFileEntity getRequiredEntity(Long fileId) {
        if (fileId == null) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        SystemFileEntity file = fileMapper.selectById(fileId);
        if (file == null || !isActive(file)) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    public SystemFileEntity getRequiredByStoredName(String storedName) {
        return findByStoredName(storedName)
                .orElseThrow(() -> new BusinessException(SystemErrorCode.FILE_NOT_FOUND));
    }

    public Optional<SystemFileEntity> findByStoredName(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return Optional.empty();
        }
        SystemFileEntity file = fileMapper.selectOne(new LambdaQueryWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getStoredName, storedName)
                .last("limit 1"));
        if (file == null || !isActive(file)) {
            return Optional.empty();
        }
        return Optional.of(file);
    }

    @Transactional
    public void bindBusiness(Long fileId, String businessType, String businessId) {
        SystemFileEntity file = getRequiredEntity(fileId);
        if (sameBusiness(file, businessType, businessId)) {
            return;
        }
        if (file.getBusinessType() != null || file.getBusinessId() != null) {
            throw new BusinessException(SystemErrorCode.FILE_ALREADY_BOUND);
        }
        file.setBusinessType(trimToNull(businessType));
        file.setBusinessId(trimToNull(businessId));
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(file);
    }

    @Transactional
    public FileMetadataResponse delete(Long fileId) {
        SystemFileEntity file = getRequiredEntity(fileId);
        return delete(file);
    }

    @Transactional
    public FileMetadataResponse delete(SystemFileEntity file) {
        if (file.getBusinessType() != null || file.getBusinessId() != null) {
            throw new BusinessException(SystemErrorCode.FILE_IN_USE);
        }
        LocalDateTime now = LocalDateTime.now();
        file.setStatus(STATUS_DELETED);
        file.setDeletedAt(now);
        file.setUpdatedAt(now);
        fileMapper.updateById(file);
        return toResponse(file);
    }

    public FileRetentionSummaryResponse summarizeRetention() {
        LocalDateTime purgeBefore = purgeBefore(LocalDateTime.now());
        List<SystemFileEntity> files = expiredDeletedFiles(purgeBefore);
        return new FileRetentionSummaryResponse(
                fileRetentionProperties.getDeletedFileDays(),
                purgeBefore,
                files.size(),
                totalSize(files)
        );
    }

    @Transactional
    public FileRetentionPurgeResponse purgeExpiredDeletedFiles() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime purgeBefore = purgeBefore(now);
        List<SystemFileEntity> files = expiredDeletedFiles(purgeBefore);
        long totalSize = totalSize(files);
        for (SystemFileEntity file : files) {
            try {
                fileStorageService.delete(file.getStoredName());
            } catch (IOException exception) {
                throw new BusinessException(SystemErrorCode.FILE_STORAGE_FAILED, "清理文件物理对象失败");
            }
            file.setStatus(STATUS_PURGED);
            file.setUpdatedAt(now);
            fileMapper.updateById(file);
        }
        return new FileRetentionPurgeResponse(
                fileRetentionProperties.getDeletedFileDays(),
                purgeBefore,
                files.size(),
                totalSize
        );
    }

    @Transactional
    public void clearBusinessIfMatched(Long fileId, String businessType, String businessId) {
        if (fileId == null) {
            return;
        }
        SystemFileEntity file = fileMapper.selectById(fileId);
        if (file == null || !sameBusiness(file, businessType, businessId)) {
            return;
        }
        file.setBusinessType(null);
        file.setBusinessId(null);
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(file);
    }

    public FileMetadataResponse toResponse(SystemFileEntity file) {
        return new FileMetadataResponse(
                file.getId(),
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

    private LambdaQueryWrapper<SystemFileEntity> buildQueryWrapper(FileMetadataQuery query) {
        LambdaQueryWrapper<SystemFileEntity> wrapper = new LambdaQueryWrapper<SystemFileEntity>()
                .orderByDesc(SystemFileEntity::getCreatedAt)
                .orderByDesc(SystemFileEntity::getId);
        if (hasText(query.getOriginalName())) {
            wrapper.like(SystemFileEntity::getOriginalName, query.getOriginalName().trim());
        }
        if (hasText(query.getExtension())) {
            wrapper.eq(SystemFileEntity::getExtension, query.getExtension().trim().toLowerCase(Locale.ROOT));
        }
        if (hasText(query.getStorageType())) {
            wrapper.eq(SystemFileEntity::getStorageType, query.getStorageType().trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(query.getStatus())) {
            wrapper.eq(SystemFileEntity::getStatus, query.getStatus().trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(query.getBusinessType())) {
            wrapper.eq(SystemFileEntity::getBusinessType, query.getBusinessType().trim());
        }
        if (hasText(query.getBusinessId())) {
            wrapper.eq(SystemFileEntity::getBusinessId, query.getBusinessId().trim());
        }
        if (query.getUploadedBy() != null) {
            wrapper.eq(SystemFileEntity::getUploadedBy, query.getUploadedBy());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SystemFileEntity::getCreatedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SystemFileEntity::getCreatedAt, query.getEndTime());
        }
        return wrapper;
    }

    private LocalDateTime purgeBefore(LocalDateTime now) {
        return now.minusDays(fileRetentionProperties.getDeletedFileDays());
    }

    private List<SystemFileEntity> expiredDeletedFiles(LocalDateTime purgeBefore) {
        return fileMapper.selectList(new LambdaQueryWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getStatus, STATUS_DELETED)
                .lt(SystemFileEntity::getDeletedAt, purgeBefore)
                .isNull(SystemFileEntity::getBusinessType)
                .isNull(SystemFileEntity::getBusinessId)
                .orderByAsc(SystemFileEntity::getDeletedAt)
                .orderByAsc(SystemFileEntity::getId));
    }

    private long totalSize(List<SystemFileEntity> files) {
        return files.stream()
                .map(SystemFileEntity::getSizeBytes)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    private String bucketName() {
        return FileStorageProperties.StorageType.MINIO.equals(fileStorageProperties.getStorageType())
                ? fileStorageProperties.getMinio().getBucket()
                : null;
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private boolean isActive(SystemFileEntity file) {
        return file.getStatus() == null || STATUS_ACTIVE.equals(file.getStatus());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private boolean sameBusiness(SystemFileEntity file, String businessType, String businessId) {
        return Objects.equals(file.getBusinessType(), trimToNull(businessType))
                && Objects.equals(file.getBusinessId(), trimToNull(businessId));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
