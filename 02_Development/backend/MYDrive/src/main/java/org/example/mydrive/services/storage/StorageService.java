package org.example.mydrive.services.storage;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction over where file blobs physically live. Two implementations are
 * provided and selected via the {@code app.storage.backend} property:
 * <ul>
 *   <li>{@code local} (default) — blobs on the local filesystem;</li>
 *   <li>{@code fileserver} — blobs delegated to the standalone Go file-server.</li>
 * </ul>
 * Blobs are addressed by an opaque storage key (the file's {@code uniqueName}).
 */
public interface StorageService {

    void store(String key, InputStream content, long size) throws IOException;

    Resource load(String key);

    void delete(String key);
}
