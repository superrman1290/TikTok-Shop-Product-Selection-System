package com.tiktokinsight.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalObjectStorage implements ObjectStorage {

    private static final Set<String> SAFE_EXTENSIONS = Set.of(
            ".csv", ".json", ".jpg", ".jpeg", ".png", ".webp"
    );

    private final Path root;

    public LocalObjectStorage(StorageProperties properties) {
        this.root = properties.path().toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new StorageException("Unable to initialize local object storage", exception);
        }
    }

    @Override
    public StoredObject put(StorageUploadRequest request) {
        String objectKey = UUID.randomUUID() + safeExtension(request.originalFilename());
        Path target = resolve(objectKey);
        try (InputStream content = request.content()) {
            long size = Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredObject(objectKey, size, request.contentType());
        } catch (IOException exception) {
            throw new StorageException("Unable to write object", exception);
        }
    }

    @Override
    public InputStream get(String objectKey) {
        try {
            return Files.newInputStream(resolve(objectKey));
        } catch (IOException exception) {
            throw new StorageException("Unable to read object", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
        } catch (IOException exception) {
            throw new StorageException("Unable to delete object", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.isRegularFile(resolve(objectKey));
    }

    @Override
    public boolean isAvailable() {
        return Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root);
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey must not be blank");
        }
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid object key");
        }
        return resolved;
    }

    private String safeExtension(String originalFilename) {
        String lowerName = originalFilename.toLowerCase(Locale.ROOT);
        return SAFE_EXTENSIONS.stream()
                .filter(lowerName::endsWith)
                .findFirst()
                .orElse("");
    }
}
