package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanTranslationRequest(
        @NotNull(message = "Language ID cannot be null")
        Long languageId,

        @NotBlank(message = "Plan name cannot be empty for this translation")
        String name,

        String description
) {}
