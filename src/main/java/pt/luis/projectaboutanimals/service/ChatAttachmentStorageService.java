package pt.luis.projectaboutanimals.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface ChatAttachmentStorageService {

    record StoredFile(String originalName, String contentType, long sizeBytes, String storageKey) {}

    StoredFile store(MultipartFile file) throws IOException;

    Path resolve(String storageKey);

    default void deleteIfExists(String storageKey) throws IOException {
        // opcional
    }
}
