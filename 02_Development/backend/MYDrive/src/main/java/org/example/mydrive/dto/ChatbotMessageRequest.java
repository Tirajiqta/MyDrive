package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatbotMessageRequest(
        // User ID derived from authenticated user
        Long ticketId, // Optional, if message is part of an existing ticket
        @NotBlank(message = "Message cannot be empty")
        String message
) {}
