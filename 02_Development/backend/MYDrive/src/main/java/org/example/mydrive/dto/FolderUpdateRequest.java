package org.example.mydrive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FolderUpdateRequest(
        Long parentId, // For moving folders

        String canonicalName, // Optional update

        @NotNull(message = "Translations cannot be null")
        @Size(min = 1, message = "At least one translation is required")
        List<FolderTranslationRequest> translations // For renaming in specific locales
) {}
