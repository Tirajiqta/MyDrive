package org.example.mydrive.dto;

public record FileTranslationResponse(
        Long id,
        LanguageResponse language,
        String displayName
) {}
