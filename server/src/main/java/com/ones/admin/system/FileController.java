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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/files")
@Tag(name = "系统管理-文件")
@ApiResourceMetadata(
        owner = "系统平台组",
        sinceVersion = "v0.0.3",
        lifecycle = ApiLifecycleStatus.ACTIVE,
        riskLevel = ApiRiskLevel.MEDIUM
)
public class FileController {

    private final FileStorageProperties fileStorageProperties;

    public FileController(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
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
        Path uploadRoot = fileStorageProperties.normalizedUploadRoot();
        Files.createDirectories(uploadRoot);
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(SystemErrorCode.FILE_STORAGE_PATH_INVALID);
        }
        file.transferTo(target);
        return ApiResult.ok(new FileUploadResponse(publicUrl(storedName)));
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "访问文件")
    @ApiAccessPolicy(value = ApiAuthType.LOGIN, reason = "文件访问依赖登录态保护，文件级授权后续随文件元数据表补齐")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws MalformedURLException {
        Path uploadRoot = fileStorageProperties.normalizedUploadRoot();
        Path file = uploadRoot.resolve(filename).normalize();
        if (!file.startsWith(uploadRoot) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
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

    private String publicUrl(String storedName) {
        String prefix = fileStorageProperties.getPublicUrlPrefix();
        if (prefix.endsWith("/")) {
            return prefix + storedName;
        }
        return prefix + "/" + storedName;
    }

    public record FileUploadResponse(String url) {
    }
}
