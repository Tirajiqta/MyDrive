package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FolderTranslationRequest(
        @NotNull(message = "Language ID cannot be null")
        Long languageId,

        @NotBlank(message = "Folder name cannot be empty for this translation")
        String name
) {}
