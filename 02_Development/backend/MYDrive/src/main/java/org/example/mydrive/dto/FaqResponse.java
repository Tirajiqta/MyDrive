package org.example.mydrive.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FaqResponse(
        Long id,
        String internalQuestionKey,
        String translatedQuestion, // For current locale
        String translatedAnswer,   // For current locale
        String translatedCategory,
        String translatedKeywords,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FaqTranslationResponse> allTranslations // For admin or debug
) {}
