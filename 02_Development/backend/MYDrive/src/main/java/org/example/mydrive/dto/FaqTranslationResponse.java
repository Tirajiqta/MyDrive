package org.example.mydrive.dto;

public record FaqTranslationResponse(
        Long id,
        LanguageResponse language,
        String question,
        String answer,
        String category,
        String keywords
) {}
