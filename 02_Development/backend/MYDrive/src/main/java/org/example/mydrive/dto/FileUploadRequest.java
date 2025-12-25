package org.example.mydrive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record FileUploadRequest(
        // MultipartFile handles the actual file binary
        @NotNull(message = "File cannot be null")
        MultipartFile file,

        Long parentId, // Null for root

        @NotNull(message = "Translations cannot be null")
        @Size(min = 1, message = "At least one translation is required")
        List<FileTranslationRequest> translations
) {}
