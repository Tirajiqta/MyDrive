package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record SupportTicketResponse(
        Long id,
        UserResponse user,
        String subject,
        String description,
        String status,
        String priority,
        UserResponse assignedAgent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String githubIssueUrl,
        Long githubIssueId
) {}
