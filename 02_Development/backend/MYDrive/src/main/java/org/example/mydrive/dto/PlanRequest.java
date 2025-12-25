package org.example.mydrive.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record PlanRequest(
        @NotBlank(message = "Internal name cannot be empty")
        String internalName,

        @NotNull(message = "Storage limit cannot be null")
        @Min(value = 0, message = "Storage limit must be positive or zero")
        Integer storageLimitGB,

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.00", message = "Price must be positive or zero")
        BigDecimal pricePerMonth,

        @NotBlank(message = "Currency cannot be empty")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g., USD)")
        String currency,

        @NotNull(message = "Active status cannot be null")
        Boolean isActive,

        @NotNull(message = "Translations cannot be null")
        @Size(min = 1, message = "At least one translation is required")
        List<PlanTranslationRequest> translations
) {}
