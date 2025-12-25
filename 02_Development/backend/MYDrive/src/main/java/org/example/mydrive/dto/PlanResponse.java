package org.example.mydrive.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlanResponse(
        Long id,
        String internalName,
        Integer storageLimitGB,
        BigDecimal pricePerMonth,
        String currency,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // For a specific locale, you might return only one translation
        PlanTranslationResponse currentTranslation,
        // Or, for admin views, all translations
        List<PlanTranslationResponse> allTranslations
) {}