package org.example.mydrive.dto;

public record JwtResponse(
        String token,
        String type, // e.g., "Bearer"
        Long id,
        String username,
        String email,
        String preferredLanguageCode
) {}
