package org.example.mydrive.dto;

public record FolderTranslationResponse(
        Long id,
        LanguageResponse language,
        String name
) {}
