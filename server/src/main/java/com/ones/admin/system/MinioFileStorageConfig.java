package com.ones.admin.system;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "ones.file", name = "storage-type", havingValue = "minio")
public class MinioFileStorageConfig {

    @Bean
    public MinioClient minioClient(FileStorageProperties properties) {
        FileStorageProperties.Minio minio = properties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
