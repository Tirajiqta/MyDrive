package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ShareCreateRequest(
        @NotNull Long itemId,
        @NotBlank String itemType,        // FILE/FOLDER
        @NotBlank String permissionLevel, // VIEW/EDIT
        LocalDateTime expiresAt           // optional
) {}
