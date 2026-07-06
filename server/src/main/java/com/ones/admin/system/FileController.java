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
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.dto.FileMetadataQuery;
import com.ones.admin.system.dto.FileMetadataResponse;
import com.ones.admin.system.entity.SystemFileEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final FileAccessService fileAccessService;

    public FileController(
            FileStorageProperties fileStorageProperties,
            FileStorageService fileStorageService,
            FileMetadataService fileMetadataService,
            FileAccessService fileAccessService
    ) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageService = fileStorageService;
        this.fileMetadataService = fileMetadataService;
        this.fileAccessService = fileAccessService;
    }

    @PostMapping("/upload")
    @SaCheckPermission("system:file:upload")
    @Operation(operationId = "FileController_upload", summary = "上传文件")
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

    @GetMapping
    @SaCheckPermission("system:file:read")
    @Operation(operationId = "FileController_listMetadata", summary = "查询文件元数据列表")
    @ApiResourceMetadata(sinceVersion = "v0.0.82", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<PageResult<FileMetadataResponse>> listMetadata(@Valid FileMetadataQuery query) {
        return ApiResult.ok(fileMetadataService.queryPage(query));
    }

    @GetMapping("/{id:\\d+}/metadata")
    @Operation(operationId = "FileController_getMetadata", summary = "查询文件元数据")
    @ApiAccessPolicy(value = ApiAuthType.LOGIN, reason = "文件元数据按文件归属授权：管理员可查看全局文件，上传人可查看自己的未绑定临时文件，业务附件由业务策略校验")
    @ApiResourceMetadata(sinceVersion = "v0.0.85", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<FileMetadataResponse> getMetadata(@PathVariable Long id) {
        SystemFileEntity file = fileMetadataService.getRequiredEntity(id);
        fileAccessService.assertMetadataAllowed(file);
        return ApiResult.ok(fileMetadataService.toResponse(file));
    }

    @DeleteMapping("/{id:\\d+}")
    @SaCheckPermission("system:file:delete")
    @Operation(operationId = "FileController_deleteMetadata", summary = "删除文件元数据")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.81", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<FileMetadataResponse> deleteMetadata(@PathVariable Long id) {
        SystemFileEntity file = fileMetadataService.getRequiredEntity(id);
        fileAccessService.assertDeleteAllowed(file);
        return ApiResult.ok(fileMetadataService.delete(file));
    }

    @GetMapping("/{filename:.+}")
    @Operation(operationId = "FileController_download", summary = "访问文件")
    @ApiAccessPolicy(value = ApiAuthType.LOGIN, reason = "文件下载按元数据归属执行二次授权：管理员可访问全局未绑定文件，上传人可访问自己的未绑定临时文件，HR 合同附件要求合同权限和员工数据范围")
    @ApiResourceMetadata(sinceVersion = "v0.0.85", riskLevel = ApiRiskLevel.MEDIUM)
    public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {
        Optional<SystemFileEntity> metadata = fileMetadataService.findByStoredName(filename);
        if (metadata.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        fileAccessService.assertDownloadAllowed(metadata.get());
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
