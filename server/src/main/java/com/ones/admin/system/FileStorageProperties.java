package com.ones.admin.system;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "ones.file")
public class FileStorageProperties {

    private StorageType storageType = StorageType.LOCAL;
    private Path uploadRoot = Path.of("uploads");
    private String publicUrlPrefix = "/api/system/files";
    private DataSize maxSize = DataSize.ofMegabytes(20);
    private Minio minio = new Minio();
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

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

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

    public Minio getMinio() {
        return minio;
    }

    public void setMinio(Minio minio) {
        this.minio = minio;
    }

    public Path normalizedUploadRoot() {
        return uploadRoot.toAbsolutePath().normalize();
    }

    public enum StorageType {
        LOCAL,
        MINIO
    }

    public static class Minio {

        private String endpoint = "http://localhost:9000";
        private String accessKey = "";
        private String secretKey = "";
        private String bucket = "ones-admin";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }
}
