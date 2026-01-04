package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FolderCreateRequest(
        // Owner ID derived from authenticated user
        Long parentId, // Null for root folder

        @NotBlank(message = "Canonical name cannot be empty")
        String canonicalName // Base name for internal use

) {}
