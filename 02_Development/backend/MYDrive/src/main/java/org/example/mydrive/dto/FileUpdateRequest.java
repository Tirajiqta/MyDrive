package org.example.mydrive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FileUpdateRequest(
        Long parentId, // For moving file

        @NotNull(message = "Translations cannot be null")
        @Size(min = 1, message = "At least one translation is required")
        List<FileTranslationRequest> translations // For renaming in specific locales
) {}