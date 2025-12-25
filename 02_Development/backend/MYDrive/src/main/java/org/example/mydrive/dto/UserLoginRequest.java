package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank(message = "Username or Email cannot be empty")
        String usernameOrEmail, // Can be username or email

        @NotBlank(message = "Password cannot be empty")
        String password
) {}
