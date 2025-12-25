package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareItemRequest(
        @NotNull(message = "Item ID cannot be null")
        Long itemId,

        @NotBlank(message = "Item type cannot be empty")
        String itemType, // "FILE" or "FOLDER"

        Long sharedWithUserId, // Null for public link sharing

        @NotBlank(message = "Permission level cannot be empty")
        String permissionLevel, // "VIEW", "EDIT"

        Boolean generatePublicLink, // True to generate a public share link
        String linkAccessType // e.g., "ANYONE_WITH_LINK_VIEW"
) {}
