package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record ShareCreateResponse(
        Long shareId,
        String token,           // raw token, returned once
        String itemType,
        Long itemId,
        String permissionLevel,
        LocalDateTime expiresAt,
        LocalDateTime sharedDate
) {}
