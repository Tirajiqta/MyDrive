package org.example.mydrive.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sent by the frontend after Stripe Checkout confirms a completed payment.
 * {@code plan} is the frontend plan id ("pro" / "business") or a backend
 * internal plan name ("PRO_PLAN"). {@code stripeSessionId} is kept for audit /
 * future webhook reconciliation.
 */
public record ActivateSubscriptionRequest(
        @NotBlank(message = "Plan cannot be empty")
        String plan,
        String stripeSessionId
) {}
