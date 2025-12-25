package org.example.mydrive.dto;

public record PlanTranslationResponse(
        Long id,
        LanguageResponse language,
        String name,
        String description
) {}
