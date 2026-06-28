package com.ones.admin.system;

import com.ones.admin.common.web.ApiResult;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
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
public class FileController {

    private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

    @PostMapping("/upload")
    public ApiResult<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ApiResult.fail(400, "上传文件不能为空");
        }
        Files.createDirectories(uploadRoot);
        String storedName = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Path target = uploadRoot.resolve(storedName).normalize();
        file.transferTo(target);
        return ApiResult.ok(new FileUploadResponse("/api/system/files/" + storedName));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws MalformedURLException {
        Path file = uploadRoot.resolve(filename).normalize();
        if (!file.startsWith(uploadRoot) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    public record FileUploadResponse(String url) {
    }
}
