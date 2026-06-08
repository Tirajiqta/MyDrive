package org.example.mydrive.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.mydrive.dto.ActivateSubscriptionRequest;
import org.example.mydrive.dto.PlanResponse;
import org.example.mydrive.dto.UserSubscriptionResponse;
import org.example.mydrive.services.SubscriptionService;
import org.example.mydrive.utils.GeneralUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public List<PlanResponse> plans() {
        return subscriptionService.listActivePlans();
    }

    @GetMapping
    public UserSubscriptionResponse current() {
        return subscriptionService.getCurrentSubscription(GeneralUtils.getIdFromToken());
    }

    @PostMapping("/activate")
    public UserSubscriptionResponse activate(@Valid @RequestBody ActivateSubscriptionRequest req) {
        return subscriptionService.activatePlan(
                GeneralUtils.getIdFromToken(), req.plan(), req.stripeSessionId());
    }
}
