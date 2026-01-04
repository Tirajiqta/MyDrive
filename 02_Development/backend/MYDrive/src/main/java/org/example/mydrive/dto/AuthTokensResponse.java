package org.example.mydrive.dto;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
