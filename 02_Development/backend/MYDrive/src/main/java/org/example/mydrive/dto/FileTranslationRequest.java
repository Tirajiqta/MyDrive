package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileTranslationRequest(
        @NotNull(message = "Language ID cannot be null")
        Long languageId,

        @NotBlank(message = "Display name cannot be empty for this translation")
        String displayName
) {}
