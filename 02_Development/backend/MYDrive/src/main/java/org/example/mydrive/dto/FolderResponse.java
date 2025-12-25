package org.example.mydrive.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FolderResponse(
        Long id,
        String canonicalName,
        String translatedName, // The name for the current locale
        Long parentId,
        UserResponse owner, // Could be just owner ID
        LocalDateTime creationDate,
        LocalDateTime lastModifiedDate,
        Boolean isDeleted,
        // If showing all translations for admin or debug
        List<FolderTranslationResponse> allTranslations
) {}
