package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        Long currentStorageUsedBytes,
        Long storageLimitGB, // From user's plan
        String preferredLanguageCode
) {}
