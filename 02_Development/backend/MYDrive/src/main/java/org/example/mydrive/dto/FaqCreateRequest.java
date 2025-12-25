package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FaqCreateRequest(
        @NotBlank(message = "Internal question key cannot be empty")
        String internalQuestionKey,

        @NotNull(message = "Translations cannot be null")
        @Size(min = 1, message = "At least one translation is required")
        List<FaqTranslationRequest> translations
) {}
