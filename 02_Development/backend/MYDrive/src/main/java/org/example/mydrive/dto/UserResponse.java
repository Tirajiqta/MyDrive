package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        Long currentStorageUsedBytes,
        Integer storageLimitGB, // From user's plan
        String preferredLanguageCode
) {}
