package org.example.mydrive.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FileResponse(
        Long id,
        String originalFileName,
        String translatedDisplayName, // The display name for the current locale
        String type,
        Long size, // In bytes
        Long parentId,
        UserResponse owner, // Could be just owner ID
        LocalDateTime uploadDate,
        LocalDateTime lastModifiedDate,
        Boolean isDeleted
) {}