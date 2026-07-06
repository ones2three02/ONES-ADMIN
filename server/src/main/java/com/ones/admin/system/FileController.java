package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.system.dto.FileMetadataResponse;
import com.ones.admin.system.entity.SystemFileEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/files")
@Tag(name = "系统管理-文件")
@ApiResourceMetadata(
        owner = "系统平台组",
        audience = "ADMIN_PORTAL",
        sinceVersion = "v0.0.3",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class FileController {

    private final FileStorageProperties fileStorageProperties;
    private final FileStorageService fileStorageService;
    private final FileMetadataService fileMetadataService;

    public FileController(
            FileStorageProperties fileStorageProperties,
            FileStorageService fileStorageService,
            FileMetadataService fileMetadataService
    ) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageService = fileStorageService;
        this.fileMetadataService = fileMetadataService;
    }

    @PostMapping("/upload")
    @SaCheckPermission("system:file:upload")
    @Operation(summary = "上传文件")
    @RepeatSubmit
    @ApiResourceMetadata(riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(SystemErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > fileStorageProperties.getMaxSize().toBytes()) {
            throw new BusinessException(SystemErrorCode.FILE_TOO_LARGE);
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!isAllowedExtension(extension)) {
            throw new BusinessException(SystemErrorCode.FILE_EXTENSION_NOT_ALLOWED);
        }
        String storedName = UUID.randomUUID() + "." + extension;
        FileStorageService.StoredFile storedFile = fileStorageService.store(file, storedName);
        FileMetadataResponse metadata = fileMetadataService.create(
                safeFilename(file.getOriginalFilename()),
                storedFile.storedName(),
                storedFile.url(),
                file.getContentType(),
                extension,
                file.getSize()
        );
        return ApiResult.ok(FileUploadResponse.from(metadata));
    }

    @GetMapping("/{id:\\d+}/metadata")
    @SaCheckPermission("system:file:read")
    @Operation(summary = "查询文件元数据")
    @ApiResourceMetadata(sinceVersion = "v0.0.80", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<FileMetadataResponse> getMetadata(@PathVariable Long id) {
        return ApiResult.ok(fileMetadataService.getRequired(id));
    }

    @DeleteMapping("/{id:\\d+}")
    @SaCheckPermission("system:file:delete")
    @Operation(summary = "删除文件元数据")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.81", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<FileMetadataResponse> deleteMetadata(@PathVariable Long id) {
        return ApiResult.ok(fileMetadataService.delete(id));
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "访问文件")
    @ApiAccessPolicy(value = ApiAuthType.LOGIN, reason = "文件访问依赖登录态保护，文件级授权后续随文件元数据表补齐")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {
        Optional<SystemFileEntity> metadata = fileMetadataService.findByStoredName(filename);
        if (metadata.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        FileStorageService.StoredResource storedResource = fileStorageService.load(filename)
                .orElse(null);
        if (storedResource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(storedResource.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""
                        + contentDispositionFilename(metadata
                        .map(SystemFileEntity::getOriginalName)
                        .orElse(storedResource.filename())) + "\"")
                .body(storedResource.resource());
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file." + UUID.randomUUID();
        }
        String filename = Path.of(originalFilename).getFileName().toString()
                .replace('\r', '_')
                .replace('\n', '_')
                .replace('"', '_');
        if (filename.isBlank()) {
            return "file." + UUID.randomUUID();
        }
        return filename;
    }

    private String contentDispositionFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replace('\r', '_')
                .replace('\n', '_')
                .replace('"', '_');
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String filename = Path.of(originalFilename).getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedExtension(String extension) {
        return fileStorageProperties.getAllowedExtensions()
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(extension));
    }

    public record FileUploadResponse(
            Long id,
            String url,
            String originalName,
            Long sizeBytes,
            String contentType,
            String extension,
            String storageType,
            String status
    ) {

        private static FileUploadResponse from(FileMetadataResponse metadata) {
            return new FileUploadResponse(
                    metadata.id(),
                    metadata.url(),
                    metadata.originalName(),
                    metadata.sizeBytes(),
                    metadata.contentType(),
                    metadata.extension(),
                    metadata.storageType(),
                    metadata.status()
            );
        }
    }
}
