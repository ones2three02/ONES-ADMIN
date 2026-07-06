package com.ones.admin.system;

import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.system.dto.FileMetadataResponse;
import com.ones.admin.system.entity.SystemFileEntity;
import com.ones.admin.system.mapper.SystemFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class FileMetadataService {

    private final FileStorageProperties fileStorageProperties;
    private final SystemFileMapper fileMapper;

    public FileMetadataService(FileStorageProperties fileStorageProperties, SystemFileMapper fileMapper) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileMapper = fileMapper;
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
        fileMapper.insert(file);
        return toResponse(file);
    }

    public FileMetadataResponse getRequired(Long fileId) {
        return toResponse(getRequiredEntity(fileId));
    }

    public SystemFileEntity getRequiredEntity(Long fileId) {
        if (fileId == null) {
            throw new BusinessException(SystemErrorCode.FILE_NOT_FOUND);
        }
        SystemFileEntity file = fileMapper.selectById(fileId);
        if (file == null) {
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
        return Optional.ofNullable(fileMapper.selectOne(new LambdaQueryWrapper<SystemFileEntity>()
                .eq(SystemFileEntity::getStoredName, storedName)
                .last("limit 1")));
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
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
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
