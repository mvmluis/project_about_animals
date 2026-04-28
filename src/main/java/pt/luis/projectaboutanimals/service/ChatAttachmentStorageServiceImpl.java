package pt.luis.projectaboutanimals.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChatAttachmentStorageServiceImpl implements ChatAttachmentStorageService {

    private final Path baseDir;

    public ChatAttachmentStorageServiceImpl(
            @Value("${app.chat.upload-dir:uploads/chat}") String uploadDir
    ) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file) throws IOException {
        Objects.requireNonNull(file, "file");

        String original = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), "file"));
        String contentType = Objects.toString(file.getContentType(), "application/octet-stream");
        long size = file.getSize();

        // nome físico no disco
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) ext = original.substring(dot);

        String storageKey = UUID.randomUUID() + ext;

        Files.createDirectories(baseDir);

        Path target = baseDir.resolve(storageKey).normalize();

        // defesa contra path traversal (mesmo com cleanPath)
        if (!target.startsWith(baseDir)) {
            throw new IOException("Caminho inválido para upload.");
        }

        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredFile(original, contentType, size, storageKey);
    }

    @Override
    public Path resolve(String storageKey) {
        Path p = baseDir.resolve(storageKey).normalize();
        if (!p.startsWith(baseDir)) {
            throw new IllegalArgumentException("storageKey inválido.");
        }
        return p;
    }

    @Override
    public void deleteIfExists(String storageKey) throws IOException {
        Path p = resolve(storageKey);
        Files.deleteIfExists(p);
    }
}
