package org.example.mydrive.dto;

import java.time.LocalDateTime;

public record DocumentContentResponse(
        Long fileId,
        String content,
        LocalDateTime lastUpdated,
        String lastEditorUsername
) {}
