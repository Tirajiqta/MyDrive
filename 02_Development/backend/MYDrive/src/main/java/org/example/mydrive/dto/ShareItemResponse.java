package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record ShareItemResponse(
        Long id,
        Long itemId,
        String itemType,
        UserResponse owner,
        UserResponse sharedWithUser,
        String permissionLevel,
        LocalDateTime sharedDate,
        String shareLinkToken, // Will be present for public links
        String linkAccessType
) {}
