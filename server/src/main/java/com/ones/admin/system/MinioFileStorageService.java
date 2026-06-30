package com.ones.admin.system;

import com.ones.admin.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "ones.file", name = "storage-type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;
    private final MinioClient minioClient;

    public MinioFileStorageService(FileStorageProperties properties, MinioClient minioClient) {
        this.properties = properties;
        this.minioClient = minioClient;
    }

    @Override
    public StoredFile store(MultipartFile file, String storedName) throws IOException {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(storedName)
                    .stream(file.getInputStream(), file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
            return new StoredFile(storedName, publicUrl(storedName));
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(SystemErrorCode.FILE_STORAGE_FAILED, "文件上传到 MinIO 失败");
        }
    }

    @Override
    public Optional<StoredResource> load(String storedName) throws IOException {
        try {
            InputStreamResource resource = new InputStreamResource(minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket())
                    .object(storedName)
                    .build()));
            MediaType mediaType = MediaTypeFactory.getMediaType(storedName)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return Optional.of(new StoredResource(resource, mediaType, storedName));
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code())) {
                return Optional.empty();
            }
            throw new BusinessException(SystemErrorCode.FILE_STORAGE_FAILED, "读取 MinIO 文件失败");
        } catch (Exception exception) {
            throw new BusinessException(SystemErrorCode.FILE_STORAGE_FAILED, "读取 MinIO 文件失败");
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
        }
    }

    private String bucket() {
        return properties.getMinio().getBucket();
    }

    private String publicUrl(String storedName) {
        String prefix = properties.getPublicUrlPrefix();
        if (prefix.endsWith("/")) {
            return prefix + storedName;
        }
        return prefix + "/" + storedName;
    }
}
