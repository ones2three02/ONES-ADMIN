package com.ones.admin.system;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String storedName) throws IOException;

    Optional<StoredResource> load(String storedName) throws IOException;

    record StoredFile(String storedName, String url) {
    }

    record StoredResource(Resource resource, MediaType mediaType, String filename) {
    }
}
