package org.example.mydrive.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;

/**
 * Storage backend that delegates blob storage to the standalone Go file-server
 * over HTTP. Enabled with {@code app.storage.backend=fileserver}.
 */
@Service
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "fileserver")
public class FileServerStorageService implements StorageService {

    private final RestClient restClient;

    public FileServerStorageService(
            @Value("${app.storage.fileserver.url:http://localhost:9000}") String baseUrl,
            @Value("${app.storage.fileserver.token:}") String token) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (!token.isBlank()) {
            builder.defaultHeader("X-Internal-Token", token);
        }
        this.restClient = builder.build();
    }

    @Override
    public void store(String key, InputStream content, long size) {
        // Stream the body straight through without buffering the whole file.
        InputStreamResource body = new InputStreamResource(content) {
            @Override
            public long contentLength() {
                return size;
            }
        };
        restClient.put()
                .uri("/files/{key}", key)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Resource load(String key) {
        byte[] bytes = restClient.get()
                .uri("/files/{key}", key)
                .retrieve()
                .body(byte[].class);
        return new ByteArrayResource(bytes != null ? bytes : new byte[0]);
    }

    @Override
    public void delete(String key) {
        restClient.delete()
                .uri("/files/{key}", key)
                .retrieve()
                .toBodilessEntity();
    }
}
