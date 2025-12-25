package org.example.mydrive.dto;

public record SupportTicketUpdateRequest(
        String status,
        String priority,
        Long assignedAgentId,
        String githubIssueUrl, // Can be updated by backend after creation
        Long githubIssueId
) {}
