package com.ones.admin.system;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Proxy;

@Configuration
@ConditionalOnProperty(prefix = "ones.file", name = "storage-type", havingValue = "minio")
public class MinioFileStorageConfig {

    @Bean
    public MinioClient minioClient(FileStorageProperties properties) {
        FileStorageProperties.Minio minio = properties.getMinio();
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey());
        if (!minio.isProxyEnabled()) {
            builder.httpClient(new OkHttpClient.Builder()
                    .proxy(Proxy.NO_PROXY)
                    .build());
        }
        return builder.build();
    }
}
