package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FaqTranslationRequest(
        @NotNull(message = "Language ID cannot be null")
        Long languageId,

        @NotBlank(message = "Question cannot be empty for this translation")
        String question,

        @NotBlank(message = "Answer cannot be empty for this translation")
        String answer,

        String category,
        String keywords
) {}
