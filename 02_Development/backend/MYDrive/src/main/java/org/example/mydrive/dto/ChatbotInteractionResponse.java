package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record ChatbotInteractionResponse(
        Long id,
        Long ticketId, // Null if not part of a ticket
        UserResponse user,
        String message,
        String response,
        LocalDateTime timestamp,
        Boolean isUserMessage
) {}
