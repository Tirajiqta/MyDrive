package org.example.mydrive.dto;

public record FileCreateRequest(
        String name,
        Long folderId,
        String mimeType,
        Long sizeBytes,
        String storageKey
) {}
