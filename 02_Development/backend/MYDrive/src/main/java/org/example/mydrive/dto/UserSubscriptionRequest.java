package org.example.mydrive.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UserSubscriptionRequest(
        // userId might be inferred from authenticated user, or explicitly passed by admin
        // @NotNull(message = "User ID cannot be null")
        // Long userId,

        @NotNull(message = "Plan ID cannot be null")
        Long planId,

        // For initial subscription, payment details might be separate or embedded
        // String paymentMethodToken, // e.g., Stripe token
        // Other payment specific fields...

        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDateTime startDate, // Optional, can be derived
        LocalDateTime endDate // Optional, for fixed terms
) {}
