package com.ones.admin.system;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "ones.file")
public class FileStorageProperties {

    private Path uploadRoot = Path.of("uploads");
    private String publicUrlPrefix = "/api/system/files";
    private DataSize maxSize = DataSize.ofMegabytes(20);
    private List<String> allowedExtensions = List.of(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp",
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "txt",
            "csv",
            "zip"
    );

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public void setUploadRoot(Path uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    public String getPublicUrlPrefix() {
        return publicUrlPrefix;
    }

    public void setPublicUrlPrefix(String publicUrlPrefix) {
        this.publicUrlPrefix = publicUrlPrefix;
    }

    public DataSize getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(DataSize maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Path normalizedUploadRoot() {
        return uploadRoot.toAbsolutePath().normalize();
    }
}
