package com.ones.admin.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.ones.admin.common.code.CommonErrorCode;
import com.ones.admin.common.exception.BusinessException;
import com.ones.admin.common.repeatsubmit.RepeatSubmit;
import com.ones.admin.common.web.ApiAccessPolicy;
import com.ones.admin.common.web.ApiAuthType;
import com.ones.admin.common.web.ApiLifecycleStatus;
import com.ones.admin.common.web.ApiResourceMetadata;
import com.ones.admin.common.web.ApiResult;
import com.ones.admin.common.web.ApiRiskLevel;
import com.ones.admin.common.web.PageResult;
import com.ones.admin.system.audit.AuditRequestContext;
import com.ones.admin.system.audit.OperationAuditService;
import com.ones.admin.system.dto.FileMetadataQuery;
import com.ones.admin.system.dto.FileMetadataResponse;
import com.ones.admin.system.dto.FileRetentionPurgeResponse;
import com.ones.admin.system.dto.FileRetentionSummaryResponse;
import com.ones.admin.system.entity.SystemFileEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OperationAuditService operationAuditService;

    public FileController(
            FileStorageProperties fileStorageProperties,
            FileStorageService fileStorageService,
            FileMetadataService fileMetadataService,
            FileAccessService fileAccessService,
            OperationAuditService operationAuditService
    ) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageService = fileStorageService;
        this.fileMetadataService = fileMetadataService;
        this.fileAccessService = fileAccessService;
        this.operationAuditService = operationAuditService;
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

    @GetMapping("/retention")
    @SaCheckPermission("system:file:purge")
    @Operation(operationId = "FileController_getRetention", summary = "查询文件清理策略")
    @ApiResourceMetadata(sinceVersion = "v0.0.86", riskLevel = ApiRiskLevel.MEDIUM)
    public ApiResult<FileRetentionSummaryResponse> getRetention() {
        return ApiResult.ok(fileMetadataService.summarizeRetention());
    }

    @PostMapping("/retention/purge")
    @SaCheckPermission("system:file:purge")
    @Operation(operationId = "FileController_purgeExpiredDeletedFiles", summary = "清理过期已删除文件")
    @RepeatSubmit
    @ApiResourceMetadata(sinceVersion = "v0.0.86", riskLevel = ApiRiskLevel.HIGH)
    public ApiResult<FileRetentionPurgeResponse> purgeExpiredDeletedFiles() {
        return ApiResult.ok(fileMetadataService.purgeExpiredDeletedFiles());
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
    public ResponseEntity<Resource> download(@PathVariable String filename, HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        Optional<SystemFileEntity> metadata = fileMetadataService.findByStoredName(filename);
        if (metadata.isEmpty()) {
            recordDownloadAudit(request, null, false, CommonErrorCode.NOT_FOUND.code(), "文件元数据不存在", startTime);
            return ResponseEntity.notFound().build();
        }
        SystemFileEntity file = metadata.get();
        try {
            fileAccessService.assertDownloadAllowed(file);
        } catch (BusinessException exception) {
            recordDownloadAudit(request, file, false, exception.getCode(), exception.getMessage(), startTime);
            throw exception;
        }
        FileStorageService.StoredResource storedResource;
        try {
            storedResource = fileStorageService.load(filename).orElse(null);
        } catch (IOException exception) {
            recordDownloadAudit(request, file, false, SystemErrorCode.FILE_STORAGE_FAILED.code(), "文件读取失败", startTime);
            throw exception;
        }
        if (storedResource == null) {
            recordDownloadAudit(request, file, false, SystemErrorCode.FILE_NOT_FOUND.code(), "文件对象不存在", startTime);
            return ResponseEntity.notFound().build();
        }
        recordDownloadAudit(request, file, true, CommonErrorCode.SUCCESS.code(), null, startTime);
        return ResponseEntity.ok()
                .contentType(storedResource.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""
                        + contentDispositionFilename(file.getOriginalName()) + "\"")
                .body(storedResource.resource());
    }

    private void recordDownloadAudit(
            HttpServletRequest request,
            SystemFileEntity file,
            boolean success,
            Integer responseCode,
            String errorMessage,
            long startTime
    ) {
        AuditRequestContext context = AuditRequestContext.from(request);
        operationAuditService.record(new OperationAuditService.OperationAuditRecord(
                currentUserId(),
                request.getMethod(),
                request.getRequestURI(),
                "系统管理-文件",
                "访问文件",
                downloadPermissionCode(file),
                success,
                responseCode,
                downloadAuditMessage(file, errorMessage),
                context.traceId(),
                context.ip(),
                context.userAgent(),
                System.currentTimeMillis() - startTime
        ));
    }

    private String downloadPermissionCode(SystemFileEntity file) {
        if (file == null || file.getBusinessType() == null) {
            return "system:file:read";
        }
        if (FileBusinessTypes.HR_EMPLOYEE_CONTRACT.equals(file.getBusinessType())) {
            return "hr:contract:list";
        }
        if (FileBusinessTypes.HR_EMPLOYEE_DOCUMENT.equals(file.getBusinessType())) {
            return "hr:employee:detail";
        }
        return "system:file:read";
    }

    private String downloadAuditMessage(SystemFileEntity file, String errorMessage) {
        StringBuilder message = new StringBuilder();
        if (file != null) {
            message.append("fileId=").append(file.getId())
                    .append(", businessType=").append(valueOrDash(file.getBusinessType()))
                    .append(", businessId=").append(valueOrDash(file.getBusinessId()))
                    .append(", originalName=").append(valueOrDash(file.getOriginalName()));
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            if (!message.isEmpty()) {
                message.append(", ");
            }
            message.append("error=").append(errorMessage);
        }
        return message.isEmpty() ? null : message.toString();
    }

    private Long currentUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static final class FileBusinessTypes {
        private static final String HR_EMPLOYEE_CONTRACT = "HR_EMPLOYEE_CONTRACT";
        private static final String HR_EMPLOYEE_DOCUMENT = "HR_EMPLOYEE_DOCUMENT";
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
            String storedName,
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
                    metadata.storedName(),
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
