package com.ones.admin.system;

import com.ones.admin.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "ones.file", name = "storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    public LocalFileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredFile store(MultipartFile file, String storedName) throws IOException {
        Path uploadRoot = properties.normalizedUploadRoot();
        Files.createDirectories(uploadRoot);
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(SystemErrorCode.FILE_STORAGE_PATH_INVALID);
        }
        file.transferTo(target);
        return new StoredFile(storedName, publicUrl(storedName));
    }

    @Override
    public Optional<StoredResource> load(String storedName) throws MalformedURLException {
        Path uploadRoot = properties.normalizedUploadRoot();
        Path file = uploadRoot.resolve(storedName).normalize();
        if (!file.startsWith(uploadRoot) || !Files.exists(file)) {
            return Optional.empty();
        }
        Resource resource = new UrlResource(file.toUri());
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return Optional.of(new StoredResource(resource, mediaType, resource.getFilename()));
    }

    @Override
    public void delete(String storedName) throws IOException {
        Path uploadRoot = properties.normalizedUploadRoot();
        Path file = uploadRoot.resolve(storedName).normalize();
        if (!file.startsWith(uploadRoot)) {
            throw new BusinessException(SystemErrorCode.FILE_STORAGE_PATH_INVALID);
        }
        Files.deleteIfExists(file);
    }

    private String publicUrl(String storedName) {
        String prefix = properties.getPublicUrlPrefix();
        if (prefix.endsWith("/")) {
            return prefix + storedName;
        }
        return prefix + "/" + storedName;
    }
}
