package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record ShareInfoResponse(
        Long shareId,
        String itemType,
        Long itemId,
        String permissionLevel,
        LocalDateTime expiresAt,
        LocalDateTime sharedDate
) {}
