package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record UserSubscriptionResponse(
        Long id,
        UserResponse user, // Might be simplified or just user ID
        PlanResponse plan, // Might be simplified to PlanName, StorageLimit
        LocalDateTime startDate,
        LocalDateTime endDate,
        String status,
        LocalDateTime renewalDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
