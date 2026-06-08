package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLanguageRequest(
        @NotBlank(message = "Language code cannot be empty")
        String code
) {}
