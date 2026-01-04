package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record ShareListItemResponse(
        Long shareId,
        String itemType,
        Long itemId,
        String permissionLevel,
        LocalDateTime expiresAt,
        LocalDateTime sharedDate,
        Boolean revoked
) {}