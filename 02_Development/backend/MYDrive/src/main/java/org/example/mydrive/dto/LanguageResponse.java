package org.example.mydrive.dto;

public record LanguageResponse(
        Long id,
        String code,
        String name,
        Boolean isActive
) {}
