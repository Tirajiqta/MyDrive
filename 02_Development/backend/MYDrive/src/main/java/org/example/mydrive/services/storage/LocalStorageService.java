package org.example.mydrive.services.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Default storage backend: blobs are written to {@code app.storage.path} on the
 * local filesystem, keyed by their uniqueName.
 */
@Service
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${app.storage.path}")
    private String storagePath;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Path.of(storagePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage directory: " + storagePath, e);
        }
    }

    @Override
    public void store(String key, InputStream content, long size) throws IOException {
        Files.copy(content, Path.of(storagePath, key));
    }

    @Override
    public Resource load(String key) {
        return new PathResource(Path.of(storagePath, key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(Path.of(storagePath, key));
        } catch (IOException ignored) {
            // Best effort; the DB record is already flagged deleted.
        }
    }
}
