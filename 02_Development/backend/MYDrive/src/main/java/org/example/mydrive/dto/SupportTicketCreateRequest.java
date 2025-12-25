package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketCreateRequest(
        // User ID derived from authenticated user, or null for anonymous
        @NotBlank(message = "Subject cannot be empty")
        @Size(max = 255, message = "Subject too long")
        String subject,

        @NotBlank(message = "Description cannot be empty")
        String description,

        String priority // Optional, default to MEDIUM
) {}
